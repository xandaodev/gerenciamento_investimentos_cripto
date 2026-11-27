-- Versão dos tokens JWT de cada usuário. O token carrega a versão com que foi
-- emitido, e o filtro de segurança só o aceita se ela for igual à do banco.
-- Incrementar a versão (logout ou troca de senha) revoga todos os tokens do usuário.
ALTER TABLE usuarios
    ADD COLUMN versao_token BIGINT NOT NULL DEFAULT 0;
