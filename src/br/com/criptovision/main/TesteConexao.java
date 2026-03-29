package br.com.criptovision.main;

import java.sql.Connection;
import br.com.criptovision.database.ConexaoDB;

public class TesteConexao {
    public static void main(String[] args) {
        try{
            System.out.println("iniciado teste de conxao");
            Connection conn = ConexaoDB.getConexao();
            if(conn != null){
                System.out.println("conexao estabelecida com sucesso");
                System.out.println("o java leu as senhas protegidas e conseguiu falar com o mysql local");
                conn.close();
            }
        }catch(Exception e){
            System.err.println("erro no teste: " + e.getMessage());
        }
    }
}