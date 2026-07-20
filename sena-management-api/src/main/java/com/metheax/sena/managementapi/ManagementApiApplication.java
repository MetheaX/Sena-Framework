package com.metheax.sena.managementapi;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication(scanBasePackages = {"com.metheax.sena"})
@EnableJpaRepositories(basePackages =  {"com.metheax.sena"})
@EntityScan(basePackages = {"com.metheax.sena"})
@ComponentScan(basePackages = {"com.metheax.sena"})
public class ManagementApiApplication {

    public static void main(String[] args) {
        SpringApplication.run(ManagementApiApplication.class, args);
    }

}
