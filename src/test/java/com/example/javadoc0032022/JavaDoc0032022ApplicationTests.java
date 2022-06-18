package com.example.javadoc0032022;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

@SpringBootTest
class JavaDoc0032022ApplicationTests {

    @Test
    void contextLoads() {
        String BASE_URL = ServletUriComponentsBuilder.fromCurrentContextPath().build().toUriString();
        System.out.println(BASE_URL);
    }

}
