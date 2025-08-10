package com.example.timetoeat;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@SpringBootApplication
@EnableJpaAuditing
public class TimeToEatApplication {

    public static void main(String[] args) {
        SpringApplication.run(TimeToEatApplication.class, args);
    }

}
