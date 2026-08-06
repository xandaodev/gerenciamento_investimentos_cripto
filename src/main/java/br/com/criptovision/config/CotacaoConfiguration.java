package br.com.criptovision.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

@Configuration
public class CotacaoConfiguration {

    @Bean
    public Clock clock() {
        return Clock.systemUTC();
    }
}
