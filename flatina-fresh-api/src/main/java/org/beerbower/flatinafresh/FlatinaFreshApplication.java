package org.beerbower.flatinafresh;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class FlatinaFreshApplication {
    public static void main(String[] args) {
        SpringApplication.run(FlatinaFreshApplication.class, args);
    }
}
