package com.rbdip.bookstore.migration;

import static org.assertj.core.api.Assertions.assertThat;

import com.rbdip.bookstore.order.CreateOrderRequest;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = "spring.flyway.target=3")
class MigrationLoadTest {

    private static final int WORKERS = 4;
    private static final int REQUESTS_PER_WORKER = 40;

    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("bookstore_load")
            .withUsername("bookstore")
            .withPassword("bookstore");

    @Autowired
    private TestRestTemplate restTemplate;

    @DynamicPropertySource
    static void configureDatasource(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
    }

    @Test
    void servesOrdersWhileNameMigrationIsApplied() throws Exception {
        Long productId = createProduct();
        CountDownLatch ready = new CountDownLatch(WORKERS);
        CountDownLatch start = new CountDownLatch(1);
        AtomicInteger completed = new AtomicInteger();
        List<Throwable> errors = new CopyOnWriteArrayList<>();
        ExecutorService pool = Executors.newFixedThreadPool(WORKERS);
        List<Future<?>> futures = new ArrayList<>();

        try {
            for (int worker = 0; worker < WORKERS; worker++) {
                futures.add(pool.submit(() -> sendOrders(productId, ready, start, completed, errors)));
            }
            assertThat(ready.await(5, TimeUnit.SECONDS)).isTrue();
            start.countDown();
            awaitCompletedRequests(completed);
            migrateToLatestVersion();
            for (Future<?> future : futures) {
                future.get(30, TimeUnit.SECONDS);
            }
        } finally {
            pool.shutdownNow();
        }

        assertThat(errors).isEmpty();
        assertThat(completed).hasValue(WORKERS * REQUESTS_PER_WORKER);
        assertThat(createOrder(productId).getStatusCode().is2xxSuccessful()).isTrue();
    }

    private void sendOrders(
            Long productId,
            CountDownLatch ready,
            CountDownLatch start,
            AtomicInteger completed,
            List<Throwable> errors) {
        ready.countDown();
        try {
            start.await();
            for (int request = 0; request < REQUESTS_PER_WORKER; request++) {
                ResponseEntity<Map> response = createOrder(productId);
                if (!response.getStatusCode().is2xxSuccessful()) {
                    errors.add(new IllegalStateException(response.getStatusCode().toString()));
                }
                completed.incrementAndGet();
            }
        } catch (Throwable error) {
            errors.add(error);
        }
    }

    private void awaitCompletedRequests(AtomicInteger completed) throws InterruptedException {
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5);
        while (completed.get() < WORKERS && System.nanoTime() < deadline) {
            Thread.sleep(10);
        }
        assertThat(completed.get()).isGreaterThanOrEqualTo(WORKERS);
    }

    private void migrateToLatestVersion() {
        Flyway.configure()
                .dataSource(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword())
                .locations("classpath:db/migration")
                .load()
                .migrate();
    }

    private Long createProduct() {
        ResponseEntity<Map> response = restTemplate.postForEntity(
                "/products", Map.of("name", "Continuous Delivery", "price", new BigDecimal("60.00")), Map.class);
        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        return Long.valueOf(response.getBody().get("id").toString());
    }

    private ResponseEntity<Map> createOrder(Long productId) {
        CreateOrderRequest request = new CreateOrderRequest(
                "Ivan Petrov",
                "Moscow",
                null,
                "regular",
                null,
                List.of(new CreateOrderRequest.Item(productId, 1)));
        return restTemplate.postForEntity("/orders", request, Map.class);
    }
}
