#!/usr/bin/env bash
#
# Backup do MySQL do CriptoVision.
#
# Cada backup é um mysqldump comprimido com gzip e cifrado com gpg (AES-256) usando
# BACKUP_SENHA. O arquivo só é aceito depois de ser decifrado e conferido. Os backups
# ficam no volume /backups e, se BACKUP_S3_BUCKET estiver definido, também num bucket
# S3 fora do servidor. A senha nunca vai para o bucket: sem ela, ninguém lê os backups,
# nem quem tiver acesso ao bucket.
#
# Uso, dentro do container "backup" (docker compose exec backup backup <comando>):
#   agendar                      roda para sempre: um backup por dia em BACKUP_HORARIO (UTC)
#                                e um teste de restauração por semana. É o comando padrão
#   agora                        faz um backup agora
#   listar                       lista os backups do servidor e os do bucket
#   testar [ARQUIVO]             restaura num MySQL temporário e confere os dados
#                                (padrão: o backup mais recente)
#   restaurar ARQUIVO --confirmar
#                                restaura no banco da aplicação, substituindo os dados atuais.
#                                Antes, faz um backup do estado atual
#   saude                        sai com erro se o último backup tiver mais de 26 horas
#                                (usado pelo HEALTHCHECK)

set -Eeuo pipefail

readonly PASTA=/backups
readonly PREFIXO=criptovision
readonly EXTENSAO=.sql.gz.gpg
readonly BANCO=criptovision
readonly HOST_BANCO="${BACKUP_DB_HOST:-db}"
readonly HORARIO="${BACKUP_HORARIO:-03:00}"
readonly RETENCAO_LOCAL_DIAS="${BACKUP_RETENCAO_DIAS:-14}"
readonly RETENCAO_REMOTA_DIAS="${BACKUP_RETENCAO_REMOTA_DIAS:-30}"
readonly TESTE_SEMANAL="${BACKUP_TESTE_SEMANAL:-true}"
readonly IDADE_MAXIMA_MINUTOS=$((26 * 60))
readonly TAMANHO_MINIMO_SENHA=20

DESTINO=""

log() {
    printf '%s [backup] %s\n' "$(date -u +%Y-%m-%dT%H:%M:%SZ)" "$*"
}

falhar() {
    log "ERRO: $*" >&2
    exit 1
}

# ---------------------------------------------------------------------------
# Configuração
# ---------------------------------------------------------------------------

validar_configuracao() {
    [[ -n "${BACKUP_SENHA:-}" ]] \
        || falhar "Defina BACKUP_SENHA no .env. É a senha que cifra os backups: guarde uma cópia fora do servidor, porque sem ela nenhum backup pode ser restaurado."
    (( ${#BACKUP_SENHA} >= TAMANHO_MINIMO_SENHA )) \
        || falhar "BACKUP_SENHA precisa ter ao menos ${TAMANHO_MINIMO_SENHA} caracteres. Gere uma com: openssl rand -base64 32"
    [[ -n "${DB_USER:-}" && -n "${DB_PASSWORD:-}" ]] \
        || falhar "Defina DB_USER e DB_PASSWORD no .env."
    [[ "$HORARIO" =~ ^([01][0-9]|2[0-3]):[0-5][0-9]$ ]] \
        || falhar "BACKUP_HORARIO deve estar no formato HH:MM, em UTC: \"$HORARIO\""
    [[ "$RETENCAO_LOCAL_DIAS" =~ ^[1-9][0-9]*$ ]] \
        || falhar "BACKUP_RETENCAO_DIAS deve ser um número de dias maior que zero: \"$RETENCAO_LOCAL_DIAS\""
    [[ "$RETENCAO_REMOTA_DIAS" =~ ^[0-9]+$ ]] \
        || falhar "BACKUP_RETENCAO_REMOTA_DIAS deve ser um número de dias (0 desliga a limpeza no bucket): \"$RETENCAO_REMOTA_DIAS\""

    if bucket_configurado; then
        configurar_bucket
    fi
}

bucket_configurado() {
    [[ -n "${BACKUP_S3_BUCKET:-}" ]]
}

# O remoto do rclone é definido só por variáveis de ambiente: as chaves não ficam
# em nenhum arquivo de configuração
configurar_bucket() {
    local variavel
    for variavel in BACKUP_S3_ENDPOINT BACKUP_S3_ACCESS_KEY_ID BACKUP_S3_SECRET_ACCESS_KEY; do
        [[ -n "${!variavel:-}" ]] \
            || falhar "BACKUP_S3_BUCKET está definido, mas falta $variavel no .env."
    done

    # sem arquivo de configuração: evita o aviso "Config file not found" a cada comando
    export RCLONE_CONFIG=/dev/null
    export RCLONE_CONFIG_BUCKET_TYPE=s3
    export RCLONE_CONFIG_BUCKET_PROVIDER="${BACKUP_S3_PROVEDOR:-Other}"
    export RCLONE_CONFIG_BUCKET_ENDPOINT="$BACKUP_S3_ENDPOINT"
    export RCLONE_CONFIG_BUCKET_REGION="${BACKUP_S3_REGIAO:-auto}"
    export RCLONE_CONFIG_BUCKET_ACCESS_KEY_ID="$BACKUP_S3_ACCESS_KEY_ID"
    export RCLONE_CONFIG_BUCKET_SECRET_ACCESS_KEY="$BACKUP_S3_SECRET_ACCESS_KEY"
    # a credencial só enxerga o próprio bucket: não tenta criar nem listar buckets
    export RCLONE_CONFIG_BUCKET_NO_CHECK_BUCKET=true

    DESTINO="bucket:${BACKUP_S3_BUCKET}/${BACKUP_S3_PASTA:-criptovision}"
}

OPCOES_MYSQL=()
GNUPGHOME=""

preparar_ambiente() {
    # A senha vai pela variável MYSQL_PWD, e não por um arquivo de opções do MySQL:
    # o leitor desses arquivos trata "#" como comentário mesmo entre aspas escapadas
    # e cortaria senhas com esse caractere. Ela também não vai na linha de comando,
    # onde apareceria no ps. A senha já está no ambiente deste container (DB_PASSWORD),
    # então não fica mais exposta
    export MYSQL_PWD="$DB_PASSWORD"
    OPCOES_MYSQL=(--user="$DB_USER" --host="$HOST_BANCO" --ssl-mode=REQUIRED)

    GNUPGHOME=$(mktemp -d)
    chmod 700 "$GNUPGHOME"
    export GNUPGHOME

    trap limpar_ambiente EXIT
}

limpar_ambiente() {
    [[ -n "$GNUPGHOME" ]] && rm -rf "$GNUPGHOME"
    return 0
}

# o MySQL temporário do teste de restauração tem root sem senha: a MYSQL_PWD do
# banco da aplicação não pode ir junto
mysql_temporario() {
    env -u MYSQL_PWD "$@"
}

# ---------------------------------------------------------------------------
# Cifra
# ---------------------------------------------------------------------------

cifrar() {
    gpg --batch --quiet --yes --pinentry-mode loopback \
        --passphrase-fd 3 \
        --symmetric --cipher-algo AES256 \
        --s2k-digest-algo SHA512 --s2k-mode 3 --s2k-count 65011712 \
        --compress-algo none \
        --output "$1" \
        3<<< "$BACKUP_SENHA"
}

decifrar() {
    gpg --batch --quiet --pinentry-mode loopback \
        --passphrase-fd 3 \
        --decrypt "$1" \
        3<<< "$BACKUP_SENHA"
}

# Um backup só vale se puder ser decifrado com a senha atual e se o mysqldump
# tiver terminado: a última linha de um dump completo é "-- Dump completed on ..."
conferir() {
    local arquivo=$1 ultima_linha

    if ! ultima_linha=$(decifrar "$arquivo" | gunzip | tail -n 1); then
        falhar "Não foi possível decifrar e descompactar $(basename "$arquivo"). A BACKUP_SENHA é a mesma usada para criá-lo?"
    fi

    [[ "$ultima_linha" == "-- Dump completed"* ]] \
        || falhar "$(basename "$arquivo") está incompleto: o mysqldump não terminou."
}

# ---------------------------------------------------------------------------
# Backup
# ---------------------------------------------------------------------------

fazer_backup() {
    local nome temporario
    # nome temporário único por processo: dois backups simultâneos (o agendado e
    # um "backup agora") não apagam o arquivo um do outro
    temporario=$(mktemp "$PASTA/.backup-em-andamento.XXXXXX.parcial")
    find "$PASTA" -maxdepth 1 -name '.backup-em-andamento.*.parcial' -mmin +60 -delete

    log "Gerando backup"

    # --single-transaction: cópia consistente sem travar as tabelas (InnoDB)
    # --no-tablespaces: dispensa o privilégio PROCESS, que o usuário da aplicação não tem
    if ! mysqldump "${OPCOES_MYSQL[@]}" \
            --single-transaction --routines --triggers --events \
            --no-tablespaces --set-gtid-purged=OFF \
            --databases "$BANCO" \
        | gzip -9 \
        | cifrar "$temporario"; then
        rm -f "$temporario"
        falhar "O mysqldump falhou: nenhum backup foi gerado."
    fi

    conferir "$temporario"
    nome=$(publicar "$temporario")
    log "Backup conferido: $nome ($(du -h "$PASTA/$nome" | cut -f1))"

    aplicar_retencao_local

    if bucket_configurado; then
        enviar_para_o_bucket "$nome"
    else
        log "AVISO: BACKUP_S3_BUCKET não está definido. O backup ficou só neste servidor e se perde junto com ele."
    fi
}

# Dá ao backup conferido o nome definitivo sem nunca sobrescrever outro backup.
# O nome tem precisão de segundos: sem esta proteção, o backup de segurança que o
# "restaurar" faz poderia sair no mesmo segundo e destruir o backup a ser restaurado.
# O ln é atômico e falha se o nome já existir; nesse caso, tenta o segundo seguinte
publicar() {
    local temporario=$1 nome
    while true; do
        nome="${PREFIXO}-$(date -u +%Y%m%dT%H%M%SZ)${EXTENSAO}"
        if ln "$temporario" "$PASTA/$nome" 2> /dev/null; then
            rm -f "$temporario"
            printf '%s\n' "$nome"
            return
        fi
        sleep 1
    done
}

aplicar_retencao_local() {
    local removido
    while IFS= read -r removido; do
        log "Removido do servidor (mais de ${RETENCAO_LOCAL_DIAS} dias): $(basename "$removido")"
    done < <(find "$PASTA" -maxdepth 1 -type f -name "${PREFIXO}-*${EXTENSAO}" \
                -mmin +$((RETENCAO_LOCAL_DIAS * 24 * 60)) -print -delete)
}

enviar_para_o_bucket() {
    local nome=$1

    rclone copyto --quiet "$PASTA/$nome" "$DESTINO/$nome" \
        || falhar "Não foi possível enviar $nome para o bucket. O backup continua no servidor."
    log "Enviado para o bucket: $DESTINO/$nome"

    if (( RETENCAO_REMOTA_DIAS > 0 )); then
        rclone delete --quiet "$DESTINO" \
            --include "${PREFIXO}-*${EXTENSAO}" \
            --min-age "${RETENCAO_REMOTA_DIAS}d" \
            || log "AVISO: não foi possível remover do bucket os backups com mais de ${RETENCAO_REMOTA_DIAS} dias."
    fi
}

# ---------------------------------------------------------------------------
# Consulta e restauração
# ---------------------------------------------------------------------------

backup_mais_recente_local() {
    find "$PASTA" -maxdepth 1 -type f -name "${PREFIXO}-*${EXTENSAO}" -printf '%f\n' \
        | sort | tail -n 1
}

backup_mais_recente_no_bucket() {
    bucket_configurado || return 0
    rclone lsf "$DESTINO" --include "${PREFIXO}-*${EXTENSAO}" 2>/dev/null | sort | tail -n 1
}

listar() {
    log "Backups neste servidor ($PASTA):"
    find "$PASTA" -maxdepth 1 -type f -name "${PREFIXO}-*${EXTENSAO}" -printf '  %f  %s bytes\n' | sort

    if bucket_configurado; then
        log "Backups no bucket ($DESTINO):"
        rclone lsl "$DESTINO" --include "${PREFIXO}-*${EXTENSAO}" | sort -k4
    else
        log "Nenhum bucket configurado (BACKUP_S3_BUCKET)."
    fi
}

# Aceita o nome de um backup do servidor ou do bucket. Imprime o caminho local
obter_backup() {
    local nome
    nome=$(basename "$1")

    [[ "$nome" == "${PREFIXO}-"*"${EXTENSAO}" ]] \
        || falhar "\"$1\" não é um backup do CriptoVision (${PREFIXO}-AAAAMMDDTHHMMSSZ${EXTENSAO})."

    if [[ -f "$PASTA/$nome" ]]; then
        printf '%s\n' "$PASTA/$nome"
        return
    fi

    bucket_configurado \
        || falhar "$nome não está neste servidor, e nenhum bucket está configurado."

    log "Baixando $nome do bucket" >&2
    rclone copyto --quiet "$DESTINO/$nome" "$PASTA/$nome" \
        || falhar "$nome não foi encontrado nem no servidor nem no bucket."
    printf '%s\n' "$PASTA/$nome"
}

escolher_backup() {
    if [[ -n "${1:-}" ]]; then
        obter_backup "$1"
        return
    fi

    local nome
    nome=$(backup_mais_recente_local)
    [[ -n "$nome" ]] || nome=$(backup_mais_recente_no_bucket)
    [[ -n "$nome" ]] || falhar "Nenhum backup encontrado, nem no servidor nem no bucket."
    obter_backup "$nome"
}

# Restaura num MySQL temporário, criado e destruído dentro deste container,
# e confere se o banco restaurado tem as tabelas e as migrations esperadas
testar() {
    local arquivo datadir socket resumo
    arquivo=$(escolher_backup "${1:-}")
    log "Testando a restauração de $(basename "$arquivo")"
    conferir "$arquivo"

    datadir=$(mktemp -d /tmp/mysql-teste.XXXXXX)
    socket="$datadir/mysqld.sock"
    chown mysql:mysql "$datadir"

    mysqld --no-defaults --initialize-insecure --user=mysql --datadir="$datadir/dados" \
        > "$datadir/inicializacao.log" 2>&1 \
        || { cat "$datadir/inicializacao.log" >&2; rm -rf "$datadir"; falhar "Não foi possível criar o MySQL temporário."; }

    mysqld --no-defaults --user=mysql --datadir="$datadir/dados" --socket="$socket" \
        --pid-file="$datadir/mysqld.pid" --skip-networking --mysqlx=OFF \
        --skip-log-bin --performance-schema=OFF --innodb-buffer-pool-size=32M \
        > "$datadir/mysqld.log" 2>&1 &

    local _
    for _ in $(seq 1 60); do
        mysql_temporario mysqladmin --no-defaults --socket="$socket" -uroot ping > /dev/null 2>&1 && break
        sleep 1
    done

    if ! { decifrar "$arquivo" | gunzip | mysql_temporario mysql --no-defaults --socket="$socket" -uroot; }; then
        mysql_temporario mysqladmin --no-defaults --socket="$socket" -uroot shutdown > /dev/null 2>&1 || true
        rm -rf "$datadir"
        falhar "A restauração de $(basename "$arquivo") no MySQL temporário falhou."
    fi

    local consulta_ok=true
    resumo=$(mysql_temporario mysql --no-defaults --socket="$socket" -uroot -N -B "$BANCO" -e "
        SELECT CONCAT(
            (SELECT COUNT(*) FROM usuarios), ' usuários, ',
            (SELECT COUNT(*) FROM transacoes), ' transações, migrations até a versão ',
            (SELECT MAX(CAST(version AS UNSIGNED)) FROM flyway_schema_history WHERE success = 1))" 2>&1) \
        || consulta_ok=false

    mysql_temporario mysqladmin --no-defaults --socket="$socket" -uroot shutdown > /dev/null 2>&1 || true
    wait || true
    rm -rf "$datadir"

    [[ "$consulta_ok" == true && -n "$resumo" ]] \
        || falhar "O banco restaurado não tem as tabelas esperadas (usuarios, transacoes, flyway_schema_history). Resposta do MySQL: ${resumo:-(vazia)}"

    log "Restauração testada com sucesso: $resumo"
}

restaurar() {
    local alvo=${1:-} confirmacao=${2:-} arquivo
    [[ -n "$alvo" ]] || falhar "Informe qual backup restaurar. Veja os disponíveis com: backup listar"
    [[ "$confirmacao" == "--confirmar" ]] \
        || falhar "A restauração substitui todos os dados atuais do banco ${BANCO}. Pare a API (docker compose stop api) e repita com: backup restaurar $(basename "$alvo") --confirmar"

    arquivo=$(obter_backup "$alvo")
    conferir "$arquivo"

    log "Fazendo um backup do estado atual antes de restaurar"
    fazer_backup

    log "Restaurando $(basename "$arquivo") no banco ${BANCO} (${HOST_BANCO})"
    decifrar "$arquivo" | gunzip | mysql "${OPCOES_MYSQL[@]}" \
        || falhar "A restauração falhou. O backup feito agora há pouco tem o estado anterior."

    log "Restauração concluída. Suba a API de novo: docker compose start api"
}

# return, e não exit: o agendar também chama esta função para decidir se faz
# um backup ao subir, e um exit encerraria o agendador
saude() {
    find "$PASTA" -maxdepth 1 -type f -name "${PREFIXO}-*${EXTENSAO}" \
        -mmin -"$IDADE_MAXIMA_MINUTOS" | grep -q . \
        || { echo "Nenhum backup nas últimas 26 horas."; return 1; }
}

# ---------------------------------------------------------------------------
# Agendamento
# ---------------------------------------------------------------------------

segundos_ate_o_proximo_horario() {
    local agora proximo
    agora=$(date -u +%s)
    proximo=$(date -u -d "today $HORARIO" +%s)
    (( proximo > agora )) || proximo=$(date -u -d "tomorrow $HORARIO" +%s)
    echo $((proximo - agora))
}

agendar() {
    log "Agendado: um backup por dia às $HORARIO UTC. Retenção: ${RETENCAO_LOCAL_DIAS} dias no servidor$(bucket_configurado && echo " e ${RETENCAO_REMOTA_DIAS} dias em $DESTINO")."

    # ao subir, já faz um backup se não houver um recente: valida toda a
    # configuração (banco, senha, bucket) no momento do deploy
    if ! saude > /dev/null 2>&1; then
        ( fazer_backup ) || log "ERRO: o backup inicial falhou. Nova tentativa no horário agendado."
    fi

    while true; do
        sleep "$(segundos_ate_o_proximo_horario)"

        ( fazer_backup ) || log "ERRO: o backup de hoje falhou. Veja as mensagens acima."

        if [[ "$TESTE_SEMANAL" == "true" && "$(date -u +%u)" == "7" ]]; then
            ( testar ) || log "ERRO: o teste semanal de restauração falhou. Veja as mensagens acima."
        fi
    done
}

# ---------------------------------------------------------------------------

principal() {
    local comando=${1:-agendar}
    shift || true

    if [[ "$comando" == "saude" ]]; then
        saude
        return
    fi

    validar_configuracao
    preparar_ambiente
    mkdir -p "$PASTA"

    case "$comando" in
        agendar)   agendar ;;
        agora)     fazer_backup ;;
        listar)    listar ;;
        testar)    testar "${1:-}" ;;
        restaurar) restaurar "${1:-}" "${2:-}" ;;
        *)         falhar "Comando desconhecido: $comando. Use agendar, agora, listar, testar ou restaurar." ;;
    esac
}

principal "$@"
