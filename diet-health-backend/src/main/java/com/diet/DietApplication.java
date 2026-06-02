package com.diet;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@MapperScan("com.diet.mapper")
@EnableScheduling
public class DietApplication {
    public static void main(String[] args) {
        SpringApplication.run(DietApplication.class, args);
    }
}
