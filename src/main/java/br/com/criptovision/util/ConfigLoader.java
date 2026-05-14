package br.com.criptovision.util;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;
import br.com.criptovision.exception.CriptoException;

public class ConfigLoader {
    private static Properties props = new Properties();
    static{
        try(FileInputStream fis = new FileInputStream("config.properties")){
            props.load(fis);
        }catch(IOException e){
            throw new CriptoException("Erro ao carregar arquivo de configuração config.properties", e);
        }
    }

    public static String get(String chave){
        return props.getProperty(chave);
    }
}