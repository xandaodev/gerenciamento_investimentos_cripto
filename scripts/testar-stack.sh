#!/usr/bin/env bash
# Teste de fumaça da imagem de produção: sobe o db e a api do docker-compose.yml
# num projeto isolado (nomes, portas e volumes próprios, com um .env gerado na
# hora) e confere o que a produção depende:
#
#   - a imagem constrói do zero, sem Java na máquina;
#   - a API só sobe depois do MySQL ficar saudável (nenhum reinício na subida);
#   - os dois containers ficam "healthy", e /saude responde UP;
#   - a API roda sem root e não consegue alterar os próprios arquivos;
#   - a JVM é o PID 1, com o heap limitado pela memória do container;
#   - a porta de gerenciamento não é publicada;
#   - os logs saem em JSON, com o ID da requisição;
#   - "docker stop" encerra a JVM pelo SIGTERM (graceful shutdown), sem SIGKILL.
#
# Uso: scripts/testar-stack.sh
# Precisa de Docker com Compose 2.24 ou mais novo. Não usa o .env do projeto nem
# toca nos containers da stack normal. No fim, apaga containers, volumes e imagem.
set -euo pipefail

PROJETO=${PROJETO:-criptovision-teste-stack}
PORTA_API=${PORTA_API:-18080}
PORTA_DB=${PORTA_DB:-13307}

raiz=$(cd "$(dirname "$0")/.." && pwd)
tmp=$(mktemp -d)
falhas=0

# o perfil "producao" traria o Caddy e o backup, que não fazem parte deste teste
unset COMPOSE_PROFILES

dc() {
    docker compose -p "$PROJETO" --project-directory "$raiz" \
        --env-file "$tmp/.env" \
        -f "$raiz/docker-compose.yml" -f "$tmp/override.yml" "$@"
}

limpar() {
    dc down -v --rmi local --remove-orphans >/dev/null 2>&1 || true
    rm -rf "$tmp"
}
trap limpar EXIT

ok() { echo "  ok      $1"; }
falhou() { echo "  FALHOU  $1"; falhas=$((falhas + 1)); }

conferir() {
    local descricao=$1 esperado=$2 obtido=$3
    if [ "$obtido" = "$esperado" ]; then
        ok "$descricao"
    else
        falhou "$descricao (esperado: $esperado, obtido: $obtido)"
    fi
}

# segredos descartáveis, só para este teste
cat > "$tmp/.env" <<EOF
MYSQL_ROOT_PASSWORD=$(openssl rand -hex 24)
DB_USER=criptovision
DB_PASSWORD=$(openssl rand -hex 24)
JWT_SECRET=$(openssl rand -base64 48 | tr -d '\n')
EOF

# sem nomes fixos de container e com outras portas, para não colidir com a
# stack normal que estiver rodando na mesma máquina
cat > "$tmp/override.yml" <<EOF
services:
  db:
    container_name: !reset null
    ports: !override
      - "127.0.0.1:$PORTA_DB:3306"
  api:
    container_name: !reset null
    ports: !override
      - "127.0.0.1:$PORTA_API:8080"
EOF

echo "Construindo a imagem e subindo db + api (projeto $PROJETO)..."
if ! dc up -d --build --wait --wait-timeout 300 db api; then
    echo "A stack não ficou saudável em 5 minutos."
    dc ps -a
    dc logs --tail 80 db api
    exit 1
fi

api=$(dc ps -q api)
db=$(dc ps -q db)

echo "Conferindo:"

conferir "MySQL saudável" "healthy" \
    "$(docker inspect -f '{{.State.Health.Status}}' "$db")"
conferir "API saudável (HEALTHCHECK da imagem)" "healthy" \
    "$(docker inspect -f '{{.State.Health.Status}}' "$api")"

# sem o depends_on com service_healthy, a API subia antes do MySQL,
# falhava ao conectar e só funcionava depois de o Docker reiniciá-la
conferir "API subiu de primeira, depois do MySQL" "0" \
    "$(docker inspect -f '{{.RestartCount}}' "$api")"

conferir "/saude responde UP" '{"status":"UP"}' \
    "$(curl -fsS "http://127.0.0.1:$PORTA_API/saude" || echo "sem resposta")"

conferir "API roda sem root (UID 10001)" "10001" \
    "$(dc exec -T api id -u)"
conferir "API não altera os próprios arquivos" "protegido" \
    "$(dc exec -T api sh -c 'touch /app/teste 2>/dev/null && echo gravou || echo protegido')"

comando=$(dc exec -T api sh -c "tr '\0' ' ' < /proc/1/cmdline")
case "$comando" in
    java*-XX:MaxRAMPercentage=75*) ok "JVM é o PID 1, com MaxRAMPercentage=75" ;;
    *) falhou "JVM não é o PID 1 ou está sem MaxRAMPercentage: $comando" ;;
esac

# o heap máximo deve acompanhar o limite do container: 75% dele
limite=$(docker inspect -f '{{.HostConfig.Memory}}' "$api")
heap=$(dc exec -T api wget -qO- \
    "http://127.0.0.1:8081/actuator/metrics/jvm.memory.max?tag=area:heap" \
    | sed -n 's/.*"value":\([0-9.E+-]*\).*/\1/p')
if [ "$limite" -gt 0 ] && awk -v h="$heap" -v l="$limite" \
    'BEGIN { exit !(h > 0.6 * l && h <= 0.76 * l) }'; then
    ok "heap máximo de $(awk -v h="$heap" 'BEGIN { printf "%.0f", h / 1048576 }') MiB para um limite de $((limite / 1048576)) MiB"
else
    falhou "heap máximo ($heap) fora de 60-75% do limite do container ($limite)"
fi

conferir "porta de gerenciamento não publicada" "8080/tcp -> 127.0.0.1:$PORTA_API" \
    "$(docker port "$api" | tr -d '\r')"

# 6 senhas erradas na mesma conta: a sexta é bloqueada e gera um log de aviso
for i in 1 2 3 4 5 6; do
    curl -s -o /dev/null -H "X-Request-Id: teste-stack-$i" \
        -H "Content-Type: application/json" \
        -d '{"login": "conta.teste.stack", "senha": "senha-errada"}' \
        "http://127.0.0.1:$PORTA_API/auth/login"
done
linha=$(dc logs --no-log-prefix api | grep '"requestId":"teste-stack-' | head -1 || true)
case "$linha" in
    '{"@timestamp"'*) ok "logs em JSON com o requestId" ;;
    *) falhou "nenhuma linha de log em JSON com o requestId das requisições do teste" ;;
esac

# java como PID 1 recebe o SIGTERM: sai com 143. Um 137 seria o SIGKILL do
# Docker depois do prazo, sem graceful shutdown
dc stop -t 40 api >/dev/null 2>&1
conferir "docker stop encerra a JVM pelo SIGTERM" "143" \
    "$(docker inspect -f '{{.State.ExitCode}}' "$api")"
if dc logs --no-log-prefix api | grep -q "Graceful shutdown complete"; then
    ok "graceful shutdown concluído"
else
    falhou "o log não mostra o graceful shutdown"
fi

if [ "$falhas" -gt 0 ]; then
    echo "$falhas verificação(ões) falharam."
    exit 1
fi
echo "Tudo certo."
