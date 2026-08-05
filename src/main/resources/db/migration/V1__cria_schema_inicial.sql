CREATE TABLE usuarios
(
    id    BIGINT       NOT NULL AUTO_INCREMENT,
    login VARCHAR(255) NULL,
    senha VARCHAR(255) NULL,

    PRIMARY KEY (id),
    CONSTRAINT uk_usuarios_login UNIQUE (login)
) ENGINE = InnoDB
  DEFAULT CHARACTER SET utf8mb4
  COLLATE utf8mb4_0900_ai_ci;


CREATE TABLE transacoes
(
    id             BIGINT                   NOT NULL AUTO_INCREMENT,
    ticker         VARCHAR(255)             NULL,
    quantidade     DECIMAL(38, 2)           NULL,
    preco_unitario DECIMAL(38, 2)           NULL,
    data           DATETIME(6)              NULL,
    tipo           ENUM ('COMPRA', 'VENDA') NULL,
    usuario_id     BIGINT                   NOT NULL,

    PRIMARY KEY (id),

    INDEX idx_transacoes_usuario_id (usuario_id),

    CONSTRAINT fk_transacoes_usuario
        FOREIGN KEY (usuario_id)
            REFERENCES usuarios (id)
) ENGINE = InnoDB
  DEFAULT CHARACTER SET utf8mb4
  COLLATE utf8mb4_0900_ai_ci;
