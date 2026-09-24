package com.storex.flashsale;

import com.storex.flashsale.entity.Product;
import com.storex.flashsale.repository.ProductRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;

import java.math.BigDecimal;

@EnableCaching
@SpringBootApplication
public class FlashSaleApplication {
    public static void main(String[] args) {
        SpringApplication.run(FlashSaleApplication.class, args);
    }

    @Bean
    CommandLineRunner seed(ProductRepository repository) {
        return args -> repository.save(
                new Product(1L, "Điện thoại Flash Sale", new BigDecimal("9990000")));
    }
}
