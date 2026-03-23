package com.example.ngerShop_be.modules.product.util;

import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Random;

@Component
public class GeneratorUtil {
    public static String generatorReference() {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyMMddHHmmss");
        String timestamp = LocalDateTime.now().format(formatter);
        int randomNum = new Random().nextInt(900) + 100; // Tạo số ngẫu nhiên từ 100-999
        return "ORD-" + timestamp + "-" + randomNum;
    }
}
