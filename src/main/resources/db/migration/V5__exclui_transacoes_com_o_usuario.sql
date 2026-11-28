-- Exclusão de conta (LGPD): ao apagar um usuário, as transações dele saem junto.
-- O ContaService já apaga as transações antes do usuário; o ON DELETE CASCADE
-- garante o mesmo numa exclusão feita direto no banco, sem deixar transações
-- órfãs nem bloquear a exclusão. O índice idx_transacoes_usuario_id continua.
ALTER TABLE transacoes DROP FOREIGN KEY fk_transacoes_usuario;

ALTER TABLE transacoes
    ADD CONSTRAINT fk_transacoes_usuario
        FOREIGN KEY (usuario_id)
            REFERENCES usuarios (id)
            ON DELETE CASCADE;
