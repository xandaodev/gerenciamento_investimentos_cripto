# 🚀 CriptoVision API - Gerenciador de Investimentos

<p align="left"> 
  <img src="https://img.shields.io/badge/Java-ED8B00?style=for-the-badge&logo=openjdk&logoColor=white" alt="Java"> 
  <img src="https://img.shields.io/badge/Spring_Boot-6DB33F?style=for-the-badge&logo=spring&logoColor=white" alt="Spring Boot"> 
  <img src="https://img.shields.io/badge/SQL-4479A1?style=for-the-badge&logo=mysql&logoColor=white" alt="SQL"> 
  <img src="https://img.shields.io/badge/API_Binance-F3BA2F?style=for-the-badge&logo=binance&logoColor=white" alt="Binance API">
  <img src="https://img.shields.io/badge/Swagger-85EA2D?style=for-the-badge&logo=swagger&logoColor=black" alt="Swagger">
  <img src="https://img.shields.io/badge/Status-Em%20Evolu%C3%A7%C3%A3o-green?style=for-the-badge" alt="Status"> 
</p>

O **CriptoVision API** é uma solução Back-End robusta desenvolvida em **Java + Spring Boot** para investidores que desejam centralizar e automatizar a gestão de seus ativos digitais. O sistema rastreia rentabilidade, calcula métricas financeiras com precisão absoluta e fornece dados em tempo real através da integração direta com a API da Binance.

## 📖 A História do Projeto: Por que eu criei?

Como um entusiasta do mercado financeiro e investidor em criptoativos, eu precisava de uma ferramenta robusta para monitorar meu Preço Médio (PM) e meu Lucro/Prejuízo (PNL) sem depender de planilhas manuais sujeitas a erros.

Para calcular o Preço Médio das minhas criptomoedas, projetar simulações de lucros e saber meu PNL em cada uma delas, era todo um trabalho de anotação, cálculos feitos com ajuda de IAs, uma grande dor de cabeça para ter resultados sem nenhuma precisão.

Nasceu então a necessidade de criar meu próprio ecossistema. O projeto me ajudou não apenas a ter controle absoluto sobre minha carteira, mas tornou-se meu principal laboratório prático para aplicar conceitos de Ciência da Computação, estruturação de dados e padrões de Engenharia de Software no mundo real.

## 🌱 Evolução e Desafios Superados (A Jornada para a API REST)

O desenvolvimento deste sistema foi marcado por ciclos de melhoria contínua e refatorações profundas:

* **V1 (A Origem - Arquivos CSV):** Inicialmente, construí o sistema utilizando persistência local em arquivos `.csv`. Foi um excelente desafio para entender fluxos de I/O em Java, mas logo esbarrei em problemas de concorrência e manipulação estruturada.
* **V2 (O Salto SQL - JDBC Manual):** Para escalar o projeto, refatorei a camada de persistência para uma arquitetura conectada a um banco MySQL. Criei DAOs do zero para lidar com as queries e garantir a integridade das transações.
* **V3 (API REST e Padrões do Mercado - Atual):** A grande revolução do projeto. Migrei a aplicação para o ecossistema **Spring Boot**, transformando-a em uma API RESTful completa.
    * 🧮 **Precisão Financeira:** Substituição de tipos primitivos por `BigDecimal`, blindando o motor de cálculos contra imprecisões de arredondamento.
    * 🛡️ **Resiliência:** Implementação de um `@ControllerAdvice` (Global Exception Handler) para interceptar regras de negócio violadas (ex: Saldo Insuficiente) e retornar JSONs padronizados (HTTP 400).
    * 🔒 **Segurança e ORM:** Adoção do Spring Data JPA (Hibernate) para mapeamento objeto-relacional e Spring Security para proteção dos endpoints.

## 🚀 Funcionalidades Principais

* **📈 Cotações em Tempo Real:** Integração assíncrona (via `HttpClient` nativo do Java) com a API da Binance para buscar preços atualizados de qualquer par de moedas.
* **📊 Dashboard e DTOs:** Resumo consolidado mapeado em DTOs, entregando saldo por ativo, preço médio e valor total da carteira em USD/BRL no formato JSON.
* **💰 Motor de Cálculo de PNL e DCA:** Monitoramento automatizado de Lucro/Prejuízo e simulador integrado para prever o impacto de novos aportes no *break-even*.
* **📖 Documentação Interativa:** Interface visual gerada dinamicamente com **Swagger/OpenAPI** para testar as rotas da API em tempo real.
* **🔄 Normalização Inteligente:** Tradutor de strings que reconhece nomes comuns (ex: "Bitcoin") e converte para tickers ("BTC").

## 🛠️ Tecnologias e Stack

Este projeto demonstra a aplicação de práticas sólidas de desenvolvimento Back-End moderno:

* **Linguagem:** Java (JDK 17+)
* **Framework Principal:** Spring Boot 3
* **Persistência:** Spring Data JPA / Hibernate
* **Banco de Dados:** MySQL
* **Segurança:** Spring Security (Basic Auth)
* **Documentação:** Springdoc OpenAPI (Swagger)
* **Qualidade:** Testes de Unidade focados em regras de negócio críticas.

## 📋 Como Executar e Testar

1.  **Pré-requisitos:** JDK 17+, Maven e um servidor MySQL rodando localmente.
2.  **Configuração do Banco:** Configure as credenciais no arquivo `src/main/resources/application.properties`. O Hibernate cuidará da criação das tabelas automaticamente (`ddl-auto=update`).
3.  **Execução:**
    Na raiz do projeto, rode o comando:
    ```bash
    ./mvnw spring-boot:run
    ```
4.  **Acessando a API:**
    Abra o navegador e acesse a documentação do Swagger para testar todos os endpoints interativamente:
    👉 `http://localhost:8080/swagger-ui.html`

---
⭐ *Desenvolvido por **Alexandre Vital** - Estudante de Ciência da Computação na UFSJ, em busca de desafios como Desenvolvedor Back-End.*