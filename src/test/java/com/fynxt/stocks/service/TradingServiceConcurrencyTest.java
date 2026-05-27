package com.fynxt.stocks.service;

import com.fynxt.stocks.model.Order;
import com.fynxt.stocks.model.Portfolio;
import com.fynxt.stocks.repository.OrderRepository;
import com.fynxt.stocks.repository.PortfolioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest
public class TradingServiceConcurrencyTest {

    @Autowired
    private TradingService tradingService;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private PortfolioRepository portfolioRepository;

    @Autowired
    private org.springframework.jdbc.core.JdbcTemplate jdbcTemplate;

    @BeforeEach
    void setUp() {
        orderRepository.deleteAll();
        portfolioRepository.deleteAll();

        // Clean up and ensure master verification row exists for lock targeting
        jdbcTemplate.execute("DELETE FROM traders WHERE id = 'CONCUR_T01'");
        jdbcTemplate.execute("INSERT INTO traders (id) VALUES ('CONCUR_T01')");
    }

    @Test
    @Transactional
    void testConcurrentOrderPlacement_EnforcesMaxThreePendingRule() throws InterruptedException {
        int numberOfThreads = 10;
        ExecutorService executorService = Executors.newFixedThreadPool(numberOfThreads);
        CountDownLatch latch = new CountDownLatch(1); // Synchronizes thread takeoff

        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failureCount = new AtomicInteger(0);

        // Run 10 threads trying to place an order for the exact same trader at the same time
        for (int i = 0; i < numberOfThreads; i++) {
            executorService.submit(() -> {
                try {
                    latch.await(); // Wait for the green light signal
                    Order order = new Order();
                    order.setTraderId("CONCUR_T01");
                    order.setStock("AAPL");
                    order.setSector("TECH");
                    order.setQuantity(5);
                    order.setSide(Order.Side.BUY);

                    tradingService.placeOrder(order);
                    successCount.incrementAndGet();
                } catch (Exception e) {
                    failureCount.incrementAndGet();
                }
            });
        }

        latch.countDown(); // Reboots all threads simultaneously to induce a race condition
        executorService.shutdown();

        // Wait for all threads to finish processing executions
        while (!executorService.isTerminated()) {
            Thread.sleep(10);
        }

        // Assertions: Pessimistic locking must strictly limit successful creations to 3
        assertEquals(3, successCount.get(), "Exactly 3 orders should successfully enter PENDING state.");
        assertEquals(7, failureCount.get(), "The other 7 concurrent requests must be rejected safely.");

        List<Order> totalPending = orderRepository.findPendingOrdersForUpdate("CONCUR_T01");
        assertEquals(3, totalPending.size());
    }
}