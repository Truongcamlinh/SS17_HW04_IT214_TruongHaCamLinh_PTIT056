package com.storex.flashsale.service;

import com.storex.flashsale.entity.Product;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cache.CacheManager;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class FlashSaleProductServiceTest {
    @Autowired
    private FlashSaleProductService productService;

    @Autowired
    private CacheManager cacheManager;

    @BeforeEach
    void clearCacheAndCounter() {
        cacheManager.getCache("flash-sale").clear();
        productService.resetDatabaseFetchCount();
    }

    @Test
    void fiftyConcurrentRequestsOnlyFetchDatabaseOnce() {
        long startedAt = System.nanoTime();

        ExecutorService executor = Executors.newFixedThreadPool(50);
        try {
            List<CompletableFuture<Product>> requests = IntStream.range(0, 50)
                    .mapToObj(index -> CompletableFuture.supplyAsync(
                            () -> productService.getProductById(1L), executor))
                    .toList();

            CompletableFuture.allOf(requests.toArray(CompletableFuture[]::new)).join();
            assertThat(requests).allSatisfy(request ->
                    assertThat(request.join().getId()).isEqualTo(1L));
        } finally {
            executor.shutdownNow();
        }

        Duration elapsed = Duration.ofNanos(System.nanoTime() - startedAt);
        assertThat(productService.getDatabaseFetchCount()).isEqualTo(1);
        assertThat(elapsed).isBetween(Duration.ofMillis(1900), Duration.ofSeconds(4));
        System.out.printf(
                "KẾT QUẢ: 50/50 request thành công, số lần truy vấn DB = %d, thời gian = %d ms%n",
                productService.getDatabaseFetchCount(), elapsed.toMillis());
    }
}
