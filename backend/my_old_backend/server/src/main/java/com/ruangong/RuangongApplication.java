package com.ruangong;

import com.ruangong.config.JwtProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties(JwtProperties.class)
public class RuangongApplication {

    public static void main(String[] args) {
        SpringApplication.run(RuangongApplication.class, args);
    }
}
