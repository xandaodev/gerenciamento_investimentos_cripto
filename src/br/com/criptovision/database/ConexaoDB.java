package br.com.criptovision.database;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import br.com.criptovision.database.ConexaoDB; // Ajuste se necessário
import br.com.criptovision.util.ConfigLoader;
import br.com.criptovision.exception.BancoDeDadosException;

public class ConexaoDB {
    public static Connection getConexao(){
        try{
            // lê as informações do arquivo config.properties
            String url = ConfigLoader.get("db.url");
            String user = ConfigLoader.get("db.user");
            String pass = ConfigLoader.get("db.password");

            return DriverManager.getConnection(url, user, pass);
        }catch(SQLException e){
            throw new BancoDeDadosException("Erro ao conectar com o MySQL: " + e.getMessage(), e);
        }
    }
}