package br.com.criptovision.util;


import java.util.Scanner;

//CLASSE UTILITARIA

//aqui a gente trata os erros de digitação

public class InputUtils {

    // cria um unico scanner pra classe toda usar
    private static Scanner leitor = new Scanner(System.in);

    
     // lê um número decimal double com segurança, aceita virgula ou ponto e nao deixa o programa travar se digitar letra
    
    public static double lerDouble(String mensagem){
        while (true){ // loop infinito até o usuário acertar
            try{
                System.out.print(mensagem);
                String entrada = leitor.nextLine().trim().replace(",", ".");
                return Double.parseDouble(entrada); 
            }catch(NumberFormatException e){
                System.out.println("ERRO: Digite um número válido (ex: 10.48).");
            }
        }
    }

    
    // lê um número inteiro (usado para as opções do menu)
     
    public static int lerInt(String mensagem){
        while(true){
            try {
                System.out.print(mensagem);
                String entrada = leitor.nextLine().trim();
                return Integer.parseInt(entrada);
            }catch(NumberFormatException e){
                System.out.println("ERRO: Digite um número inteiro válido.");
            }
        }
    }

    // lê uma string, um texto simples, um ticker, um nome
    public static String lerString(String mensagem){
        System.out.print(mensagem);
        return leitor.nextLine().trim().toUpperCase();
    }
}