package com.w.esshop;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan("com.w.esshop.mapper")
public class EsshopApplication {

    public static void main(String[] args) {
        SpringApplication.run(EsshopApplication.class, args);
    }

}
