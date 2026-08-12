package com.fever.plans;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@SpringBootApplication
public class FeverPlansApplication {
    public static void main(String[] args) {
        SpringApplication.run(FeverPlansApplication.class, args);
    }
}
