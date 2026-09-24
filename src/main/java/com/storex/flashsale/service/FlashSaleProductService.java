package com.storex.flashsale.service;

import com.storex.flashsale.entity.Product;
import com.storex.flashsale.repository.ProductRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.NoSuchElementException;
import java.util.concurrent.atomic.AtomicInteger;

@Service
public class FlashSaleProductService {
    private static final Logger log = LoggerFactory.getLogger(FlashSaleProductService.class);

    private final ProductRepository productRepository;
    private final AtomicInteger databaseFetchCount = new AtomicInteger();

    public FlashSaleProductService(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    @Cacheable(value = "flash-sale", key = "#id", sync = true)
    public Product getProductById(Long id) {
        log.info("Fetching from Database for product {}", id);
        databaseFetchCount.incrementAndGet();
        simulateComplexQuery();
        return productRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Không tìm thấy sản phẩm " + id));
    }

    private void simulateComplexQuery() {
        try {
            Thread.sleep(2000);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Luồng truy vấn bị gián đoạn", exception);
        }
    }

    public int getDatabaseFetchCount() { return databaseFetchCount.get(); }
    public void resetDatabaseFetchCount() { databaseFetchCount.set(0); }
}
