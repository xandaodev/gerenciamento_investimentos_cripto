# Imagem do serviço de backup do MySQL.
#
# Parte da imagem do próprio MySQL, para que o mysqldump seja da mesma versão do
# servidor; ela já traz o gpg. O rclone, que envia os backups para o bucket, vem
# da imagem oficial do projeto (binário estático).

FROM rclone/rclone:1.76 AS rclone

FROM mysql:8.0

COPY --from=rclone /usr/local/bin/rclone /usr/local/bin/rclone
COPY backup.sh /usr/local/bin/backup
RUN chmod 755 /usr/local/bin/backup

VOLUME /backups

# "unhealthy" no docker compose ps se o último backup tiver mais de 26 horas
HEALTHCHECK --interval=30m --timeout=30s --start-period=10m --retries=1 \
    CMD ["backup", "saude"]

ENTRYPOINT ["backup"]
CMD ["agendar"]
