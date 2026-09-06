package com.roost;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class RoostApplication {

    public static void main(String[] args) {
        SpringApplication.run(RoostApplication.class, args);
    }
}
