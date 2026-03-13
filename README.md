# 🪙 Gerenciador de Investimentos Cripto

<p align="left"> 
  <img src="https://img.shields.io/badge/Java-ED8B00?style=for-the-badge&logo=openjdk&logoColor=white" alt="Java"> 
  <img src="https://img.shields.io/badge/API-Binance-F3BA2F?style=for-the-badge&logo=binance&logoColor=white" alt="Binance API">
  <img src="https://img.shields.io/badge/Status-Em%20Desenvolvimento-green?style=for-the-badge" alt="Status"> 
</p>

O **Gerenciador de Investimentos Cripto** é uma solução robusta desenvolvida em **Java** para investidores que desejam centralizar a gestão de seus ativos digitais. O sistema automatiza o rastreamento de rentabilidade e fornece dados em tempo real através da integração direta com a API da Binance.

## 🚀 Funcionalidades Principais

O projeto oferece um conjunto de ferramentas essenciais para o monitoramento financeiro de precisão:

* **📈 Cotações em Tempo Real:** Integração com a API da Binance para buscar preços atualizados de qualquer par de moedas disponível no mercado.
* **📊 Dashboard de Patrimônio:** Visão consolidada do saldo por ativo, preço médio de compra e valor total da carteira em dólares (USD) e reais (BRL).
* **💰 Cálculo de PNL Automático:** Monitoramento de Lucro e Prejuízo (*Profit and Loss*) por moeda e global, permitindo visualizar a performance histórica da carteira.
* **⚖️ Simulador de DCA (Dollar Cost Averaging):** Ferramenta para simular novos aportes e prever o impacto no preço médio atual e no ponto de equilíbrio (*break-even*).
* **📂 Relatórios Profissionais:** Geração automática de extratos em formato `.txt` com formatação tabular e data/hora de emissão.
* **🔄 Normalização de Ativos:** Tradutor inteligente que reconhece nomes comuns (ex: "Bitcoin", "Ethereum") e os converte automaticamente para tickers de mercado ("BTC", "ETH").
* **🧪 Qualidade Garantida:** Suite de testes unitários desenvolvida com **JUnit** para validar a integridade dos cálculos matemáticos do sistema.

## 🛠️ Tecnologias e Conceitos de Engenharia

Este projeto demonstra a aplicação de práticas sólidas de desenvolvimento de software:

* **Arquitetura em Camadas:** Organização lógica dividida em `Model`, `Repository`, `Service`, `Main`, `Util` e `Test`.
* **Consumo de APIs REST:** Utilização do `HttpClient` nativo do Java para requisições assíncronas e tratamento de respostas JSON.
* **Persistência Local:** Gerenciamento de dados via arquivos CSV, garantindo histórico de transações e backups automáticos ao iniciar o sistema.
* **Testes de Unidade:** Cobertura de lógica de negócio crítica (cálculo de PM e PNL) utilizando JUnit 4.

## 📋 Como Executar

1.  **Pré-requisitos:** Certifique-se de possuir o JDK 17 ou superior instalado.
2.  **Configuração:** O sistema utiliza os arquivos `transacoes.csv` e `lucros.csv` para armazenar os dados.
3.  **Execução:**
    ```bash
    java -jar CriptoResumo.jar
    ```

## 🔐 Segurança e Privacidade

A privacidade dos dados financeiros é um pilar fundamental deste projeto:
* **Armazenamento Local:** Nenhuma informação de investimento sai do seu computador; os dados são gravados apenas em arquivos locais.
* **API Pública:** O sistema utiliza apenas a API pública de preços da Binance, não exigindo chaves privadas ou senhas de corretoras para funcionar.

---
⭐ *Desenvolvido por **Alexandre (Xandão)** - Aluno de Ciência da Computação na UFSJ.*