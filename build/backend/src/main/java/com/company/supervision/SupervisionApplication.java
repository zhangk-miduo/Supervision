package com.company.supervision;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan("com.company.supervision.infrastructure.repository")
public class SupervisionApplication {

    public static void main(String[] args) {
        SpringApplication.run(SupervisionApplication.class, args);
    }
}
