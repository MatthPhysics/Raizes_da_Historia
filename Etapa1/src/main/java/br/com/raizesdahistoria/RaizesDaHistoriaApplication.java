package br.com.raizesdahistoria;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class RaizesDaHistoriaApplication {

    public static void main(String[] args) {
        SpringApplication.run(RaizesDaHistoriaApplication.class, args);
    }
}
