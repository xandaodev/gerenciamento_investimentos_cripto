# 🪙 CryptoPortfolio Manager (Binance Edition)

<p align="left"> 
  <img src="https://img.shields.io/badge/Java-ED8B00?style=for-the-badge&logo=openjdk&logoColor=white" alt="Java"> 
  <img src="https://img.shields.io/badge/API-Binance-yellow?style=for-the-badge" alt="Binance API">
  <img src="https://img.shields.io/badge/Status-Versão%20Estável-green?style=for-the-badge" alt="Status"> 
</p>

O **CryptoPortfolio Manager** é uma aplicação Java de alta performance para gestão centralizada de ativos digitais. O sistema permite consolidar investimentos e acompanhar a performance da carteira em tempo real.

## ✨ Evolução e Funcionalidades

- **Integração com Binance API:** Migração da CoinGecko para Binance, garantindo consultas instantâneas e maior limite de requisições.
- **Tratamento Inteligente de Ativos:** Sistema robusto que traduz automaticamente nomes de moedas (ex: "Bitcoin") para Tickers de mercado (ex: "BTC").
- **Dashboard de Patrimônio:** Cálculo automático de saldo, preço médio e PNL (Profit and Loss) de cada ativo.
- **Interface de Terminal Moderna:** Resumo executivo com indicadores visuais de performance financeira (▲/▼).
- **Persistência de Dados:** Histórico de transações e lucros realizados armazenados em arquivos CSV.

## 🛠️ Tecnologias Utilizadas

- **Linguagem:** Java (JDK 17+).
- **HTTP Client:** Java Native HttpClient para consumo de APIs REST.
- **Arquitetura:** Organizada em pacotes (Model, Repository, Service, Main) para facilitar a manutenção e escalabilidade.