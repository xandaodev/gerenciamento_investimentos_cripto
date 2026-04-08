# 🚀 Gerenciador de Investimentos Cripto

<p align="left"> 
  <img src="https://img.shields.io/badge/Java-ED8B00?style=for-the-badge&logo=openjdk&logoColor=white" alt="Java"> 
  <img src="https://img.shields.io/badge/SQL-4479A1?style=for-the-badge&logo=mysql&logoColor=white" alt="SQL"> 
  <img src="https://img.shields.io/badge/API_Binance-F3BA2F?style=for-the-badge&logo=binance&logoColor=white" alt="Binance API">
  <img src="https://img.shields.io/badge/Status-Em%20Evolu%C3%A7%C3%A3o-green?style=for-the-badge" alt="Status"> 
</p>

O **Gerenciador de Investimentos Cripto** é uma solução Back-End desenvolvida em **Java** para investidores que desejam centralizar e automatizar a gestão de seus ativos digitais. O sistema rastreia rentabilidade, calcula métricas financeiras precisas e fornece dados em tempo real através da integração direta com a API da Binance.

## 📖 A História do Projeto: Por que eu criei?

Como um entusiasta do mercado financeiro e investidor em criptoativos, eu precisava de uma ferramenta robusta para monitorar meu Preço Médio (PM) e meu Lucro/Prejuízo (PNL) sem depender de planilhas manuais sujeitas a erros.

Para calcular o Preço Médio das minhas criptomoedas, projetar simulações de lucros e saber meu PNL em cada uma delas, era todo um trabalho de anotação, cálculos feitos com ajuda de IAs, uma grande dor de cabeça para ter resultados sem nenhuma precisão.

Nasceu então a necessidade de criar meu próprio ecossistema. O projeto me ajudou não apenas a ter controle absoluto sobre minha carteira, mas tornou-se meu principal laboratório prático para aplicar conceitos de Ciência da Computação, estruturação de dados e engenharia de software no mundo real.

## 🌱 Evolução e Desafios Superados (A Jornada para o SQL)

O desenvolvimento deste sistema foi marcado por ciclos de melhoria contínua:

* **V1 (A Origem - Arquivos CSV):** Inicialmente, construí o sistema utilizando persistência local em arquivos `.csv`. Foi um excelente desafio para entender I/O em Java, mas logo esbarrei em problemas de concorrência, lentidão na busca de dados históricos e dificuldade de manipulação estruturada.
* **V2 (O Salto - Banco de Dados Relacional):** Para escalar o projeto e garantir a integridade das transações, refatorei toda a camada de persistência. Substituí a leitura de arquivos estáticos por uma arquitetura conectada a um **Banco de Dados Relacional (SQL)**. 
* **O Desafio da Matemática Financeira:** Garantir que o cálculo de *Dollar Cost Averaging* (DCA) e PNL estivessem corretos ao centavo exigiu a criação de uma suíte de **Testes Unitários com JUnit**, garantindo a confiabilidade das regras de negócio.

## 🚀 Funcionalidades Principais

* **📈 Cotações em Tempo Real:** Integração assíncrona com a API da Binance para buscar preços atualizados de qualquer par de moedas.
* **📊 Dashboard de Patrimônio:** Visão consolidada do saldo por ativo, preço médio de compra e valor da carteira em USD e BRL.
* **💰 Motor de Cálculo de PNL e DCA:** Monitoramento automatizado de Lucro/Prejuízo e simulador integrado para prever o impacto de novos aportes no *break-even*.
* **🗄️ Persistência Relacional:** Todo o histórico de transações, aportes e lucros agora é gerenciado de forma segura e estruturada via Banco de Dados (SQL).
* **🔄 Normalização Inteligente:** Tradutor de strings que reconhece nomes comuns (ex: "Bitcoin") e converte para tickers ("BTC").

## 🛠️ Tecnologias e Arquitetura

Este projeto demonstra a aplicação de práticas sólidas de desenvolvimento Back-End:

* **Linguagem:** Java (JDK 17+)
* **Banco de Dados:** SQL (JDBC / Integração Relacional)
* **Arquitetura em Camadas:** Código limpo e desacoplado dividido em `Model`, `Repository`, `Service`, `Main`, `Util` e `Test`.
* **Consumo de APIs REST:** Utilização do `HttpClient` nativo do Java para requisições e manipulação de respostas JSON.
* **Qualidade de Código:** Testes de Unidade (JUnit 4) focados nas lógicas críticas.

## 📋 Como Executar

1.  **Pré-requisitos:** JDK 17+ e um servidor SQL rodando localmente.
2.  **Configuração do Banco:** Execute o script `schema.sql` (incluso no repositório) para criar as tabelas necessárias no seu banco de dados.
3.  **Execução:**
    ```bash
    java -jar CriptoResumo.jar
    ```

---
⭐ *Desenvolvido por **Alexandre (Xandão) Vital** - Estudante de Ciência da Computação na UFSJ, em busca de desafios como Desenvolvedor Back-End.*
