package br.com.criptovision;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class OpenApiDocumentationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void deveExporOpenApiComJwtRotasEContratosDocumentados()
        throws Exception {

        mockMvc.perform(get("/v3/api-docs"))
            .andExpect(status().isOk())
            .andExpect(content().contentTypeCompatibleWith(
                MediaType.APPLICATION_JSON
            ))
            .andExpect(jsonPath("$.info.title")
                .value("CriptoVision API"))
            .andExpect(jsonPath("$.info.version")
                .value("1.0.0"))
            .andExpect(jsonPath(
                "$.components.securitySchemes.bearerAuth.type"
            ).value("http"))
            .andExpect(jsonPath(
                "$.components.securitySchemes.bearerAuth.scheme"
            ).value("bearer"))
            .andExpect(jsonPath(
                "$.components.securitySchemes.bearerAuth.bearerFormat"
            ).value("JWT"))
            .andExpect(jsonPath(
                "$.paths['/auth/login'].post.summary"
            ).value("Autenticar usuário"))
            .andExpect(jsonPath(
                "$.paths['/auth/register'].post.responses['201']"
            ).exists())
            .andExpect(jsonPath(
                "$.paths['/transacoes'].get.security[0].bearerAuth"
            ).isArray())
            .andExpect(jsonPath(
                "$.paths['/transacoes/{id}'].put.responses['409']"
            ).exists())
            .andExpect(jsonPath(
                "$.components.schemas.ProblemaApi"
            ).exists());
    }
}
