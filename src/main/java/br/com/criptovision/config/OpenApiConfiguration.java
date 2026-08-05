package br.com.criptovision.config;

import io.swagger.v3.oas.annotations.ExternalDocumentation;
import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Contact;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.context.annotation.Configuration;

@Configuration
@OpenAPIDefinition(
    info = @Info(
        title = "CriptoVision API",
        version = "1.0.0",
        description = "API REST para cadastro de usuários, autenticação JWT, "
            + "registro de transações e análise de carteiras de criptomoedas.",
        contact = @Contact(
            name = "Alexandre Vital",
            url = "https://github.com/xandaodev"
        )
    ),
    tags = {
        @Tag(
            name = "Autenticação",
            description = "Cadastro de usuários e emissão de tokens JWT."
        ),
        @Tag(
            name = "Transações",
            description = "Cadastro, consulta, alteração e exclusão de transações."
        ),
        @Tag(
            name = "Carteira",
            description = "Resumo, patrimônio e simulações da carteira autenticada."
        ),
        @Tag(
            name = "Análises",
            description = "Indicadores agregados das transações do usuário autenticado."
        )
    },
    externalDocs = @ExternalDocumentation(
        description = "Código-fonte do CriptoVision",
        url = "https://github.com/xandaodev/gerenciamento_investimentos_cripto"
    )
)
@SecurityScheme(
    name = OpenApiConfiguration.BEARER_AUTH,
    description = "Informe o token JWT retornado por POST /auth/login.",
    type = SecuritySchemeType.HTTP,
    scheme = "bearer",
    bearerFormat = "JWT"
)
public class OpenApiConfiguration {

    public static final String BEARER_AUTH = "bearerAuth";

}
