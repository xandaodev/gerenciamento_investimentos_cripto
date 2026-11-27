package br.com.criptovision.integracao;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.Container.ExecResult;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.images.builder.ImageFromDockerfile;
import org.testcontainers.mysql.MySQLContainer;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * O serviço de backup (backup/Dockerfile e backup/backup.sh) contra um MySQL 8
 * que exige TLS, com o schema das migrations do Flyway, e um bucket S3 de teste
 * servido pelo próprio rclone.
 */
class BackupMysqlIT {

    private static final String SENHA_DO_BANCO = "senha#do\"banco";
    private static final String SENHA_DO_BACKUP = "senha-do-backup-com-mais-de-vinte";
    private static final BigDecimal PRECO_PEPE = new BigDecimal("0.000001234500000001");
    private static final Pattern NOME_DO_BACKUP =
        Pattern.compile("Backup conferido: (criptovision-\\S+\\.sql\\.gz\\.gpg)");

    private static final Network REDE = Network.newNetwork();

    private static final MySQLContainer MYSQL = new MySQLContainer("mysql:8.0")
        .withNetwork(REDE)
        .withNetworkAliases("db")
        .withDatabaseName("criptovision")
        .withUsername("criptovision")
        .withPassword(SENHA_DO_BANCO)
        .withCommand("--require-secure-transport=ON")
        .withUrlParam("sslMode", "REQUIRED")
        .withUrlParam("allowPublicKeyRetrieval", "false");

    // bucket S3 de teste: o próprio rclone serve a pasta /dados como S3
    private static final GenericContainer<?> S3 = new GenericContainer<>("rclone/rclone:1.76")
        .withNetwork(REDE)
        .withNetworkAliases("s3")
        .withExposedPorts(8080)
        .withCreateContainerCmdModifier(comando -> comando.withEntrypoint("sh"))
        .withCommand("-c",
            "mkdir -p /dados/backups && exec rclone serve s3 /dados --addr :8080 --auth-key chave,segredo")
        .waitingFor(Wait.forListeningPort());

    private static final GenericContainer<?> BACKUP = new GenericContainer<>(
            new ImageFromDockerfile().withFileFromPath(".", Path.of("backup")))
        .withNetwork(REDE)
        .withEnv("DB_USER", "criptovision")
        .withEnv("DB_PASSWORD", SENHA_DO_BANCO)
        .withEnv("BACKUP_SENHA", SENHA_DO_BACKUP)
        .withEnv("BACKUP_S3_BUCKET", "backups")
        .withEnv("BACKUP_S3_ENDPOINT", "http://s3:8080")
        .withEnv("BACKUP_S3_ACCESS_KEY_ID", "chave")
        .withEnv("BACKUP_S3_SECRET_ACCESS_KEY", "segredo")
        .withEnv("BACKUP_S3_REGIAO", "us-east-1")
        // o agendador não roda: cada teste chama os comandos que precisa
        .withCreateContainerCmdModifier(comando -> comando.withEntrypoint("sleep"))
        .withCommand("infinity");

    @BeforeAll
    static void prepararBancoComDados() throws Exception {
        S3.start();
        MYSQL.start();

        Flyway.configure()
            .dataSource(MYSQL.getJdbcUrl(), MYSQL.getUsername(), MYSQL.getPassword())
            .locations("classpath:db/migration")
            .load()
            .migrate();

        BACKUP.start();
    }

    // cada teste começa com os mesmos dados, para que a falha de um não contamine os outros
    @BeforeEach
    void restaurarDadosIniciais() throws Exception {
        executarSql("""
            DELETE FROM transacoes;
            DELETE FROM usuarios;
            INSERT INTO usuarios (id, login, senha) VALUES (1, 'alexandre', 'hash-bcrypt');
            INSERT INTO transacoes (ticker, quantidade, preco_unitario, tipo, usuario_id)
                VALUES ('PEPE', 1000000, 0.000001234500000001, 'COMPRA', 1),
                       ('BTC', 0.5, 65000, 'COMPRA', 1);
            """);
    }

    @AfterAll
    static void encerrar() {
        BACKUP.stop();
        MYSQL.stop();
        S3.stop();
        REDE.close();
    }

    @Test
    void deveGerarBackupCifradoNoServidorENoBucket() throws Exception {
        String nome = fazerBackup();

        byte[] noServidor = BACKUP.copyFileFromContainer(
            "/backups/" + nome, entrada -> entrada.readAllBytes());
        byte[] noBucket = S3.copyFileFromContainer(
            "/dados/backups/criptovision/" + nome, entrada -> entrada.readAllBytes());

        assertThat(noBucket).isEqualTo(noServidor);
        // nem gzip aberto (1f 8b) nem texto do banco: só o pacote cifrado do gpg
        assertThat(noServidor[0] & 0xff).isNotEqualTo(0x1f);
        assertThat(new String(noServidor, StandardCharsets.ISO_8859_1))
            .doesNotContain("INSERT INTO", "alexandre", "PEPE");
    }

    @Test
    void deveTestarARestauracaoNumMysqlTemporario() throws Exception {
        String nome = fazerBackup();

        ExecResult teste = backup("testar", nome);

        assertThat(teste.getExitCode()).as(teste.getStderr()).isZero();
        assertThat(teste.getStdout())
            .contains("Restauração testada com sucesso: 1 usuários, 2 transações, migrations até a versão 3");
    }

    @Test
    void deveRestaurarOsDadosComValoresExatos() throws Exception {
        String nome = fazerBackup();
        executarSql("DELETE FROM transacoes;");

        ExecResult restauracao = backup("restaurar", nome, "--confirmar");

        assertThat(restauracao.getExitCode()).as(restauracao.getStderr()).isZero();
        assertThat(consultarPrecoDoPepe()).isEqualByComparingTo(PRECO_PEPE);
    }

    @Test
    void naoDeveSobrescreverUmBackupComOMesmoNome() throws Exception {
        // ocupa os nomes dos próximos segundos com arquivos vazios: o backup novo
        // precisa escolher outro nome, sem tocar em nenhum deles. Sem essa proteção,
        // o backup de segurança do "restaurar" podia destruir o backup a ser restaurado
        ExecResult ocupados = BACKUP.execInContainer("sh", "-c", """
            for i in 0 1 2 3 4 5 6 7 8 9; do
              touch /backups/criptovision-$(date -u -d "+$i seconds" +%Y%m%dT%H%M%SZ).sql.gz.gpg
            done""");
        assertThat(ocupados.getExitCode()).isZero();

        try {
            String nome = fazerBackup();

            ExecResult intactos = BACKUP.execInContainer("sh", "-c",
                "find /backups -name 'criptovision-*.sql.gz.gpg' -empty | wc -l");
            assertThat(intactos.getStdout().trim()).isEqualTo("10");
            assertThat(BACKUP.execInContainer("test", "-s", "/backups/" + nome).getExitCode())
                .as("o backup novo tem conteúdo").isZero();
        } finally {
            // os arquivos vazios atrapalhariam os testes que usam o backup mais recente
            BACKUP.execInContainer("sh", "-c", "find /backups -name '*.gpg' -empty -delete");
        }
    }

    @Test
    void deveExigirConfirmacaoParaRestaurar() throws Exception {
        String nome = fazerBackup();

        ExecResult restauracao = backup("restaurar", nome);

        assertThat(restauracao.getExitCode()).isNotZero();
        assertThat(restauracao.getStderr()).contains("--confirmar");
    }

    @Test
    void naoDeveDecifrarComOutraSenha() throws Exception {
        String nome = fazerBackup();

        ExecResult teste = BACKUP.execInContainer("sh", "-c",
            "BACKUP_SENHA=outra-senha-com-mais-de-vinte-caracteres backup testar " + nome);

        assertThat(teste.getExitCode()).isNotZero();
        assertThat(teste.getStderr()).contains("A BACKUP_SENHA é a mesma usada para criá-lo?");
    }

    @Test
    void deveRestaurarDoBucketQuandoOServidorPerdeuOsBackups() throws Exception {
        String nome = fazerBackup();
        BACKUP.execInContainer("sh", "-c", "rm -f /backups/*.gpg");

        ExecResult teste = backup("testar", nome);

        assertThat(teste.getExitCode()).as(teste.getStderr()).isZero();
        assertThat(teste.getStderr()).contains("Baixando " + nome + " do bucket");
        assertThat(teste.getStdout()).contains("Restauração testada com sucesso");
    }

    @Test
    void deveRecusarRodarSemASenhaDosBackups() throws Exception {
        ExecResult backup = BACKUP.execInContainer("sh", "-c", "BACKUP_SENHA= backup agora");

        assertThat(backup.getExitCode()).isNotZero();
        assertThat(backup.getStderr()).contains("Defina BACKUP_SENHA");
    }

    private static String fazerBackup() throws Exception {
        ExecResult backup = backup("agora");
        assertThat(backup.getExitCode()).as(backup.getStderr()).isZero();
        assertThat(backup.getStdout()).contains("Enviado para o bucket");

        Matcher nome = NOME_DO_BACKUP.matcher(backup.getStdout());
        assertThat(nome.find()).as(backup.getStdout()).isTrue();
        return nome.group(1);
    }

    private static ExecResult backup(String... argumentos) throws Exception {
        String[] comando = new String[argumentos.length + 1];
        comando[0] = "backup";
        System.arraycopy(argumentos, 0, comando, 1, argumentos.length);
        return BACKUP.execInContainer(comando);
    }

    private static void executarSql(String sql) throws Exception {
        try (Connection conexao = conectar(); Statement comando = conexao.createStatement()) {
            for (String instrucao : sql.split(";")) {
                if (!instrucao.isBlank()) {
                    comando.execute(instrucao);
                }
            }
        }
    }

    private static BigDecimal consultarPrecoDoPepe() throws Exception {
        try (Connection conexao = conectar();
             Statement comando = conexao.createStatement();
             ResultSet resultado = comando.executeQuery(
                 "SELECT preco_unitario FROM transacoes WHERE ticker = 'PEPE'")) {
            assertThat(resultado.next()).isTrue();
            return resultado.getBigDecimal(1);
        }
    }

    private static Connection conectar() throws Exception {
        return DriverManager.getConnection(
            MYSQL.getJdbcUrl(), MYSQL.getUsername(), MYSQL.getPassword());
    }
}
