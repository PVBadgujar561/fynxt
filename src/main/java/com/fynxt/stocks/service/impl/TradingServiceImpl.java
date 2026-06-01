package com.fynxt.stocks.service.impl;

import com.fynxt.stocks.domain.SectorOverlapCalculator;
import com.fynxt.stocks.model.*;
import com.fynxt.stocks.repository.*;
import com.fynxt.stocks.service.TradingService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;

@Service
public class TradingServiceImpl implements TradingService {

    private final OrderRepository orderRepository;
    private final PortfolioRepository portfolioRepository;

    // Thread-safe lock registry per trader to serialize incoming concurrent orders
    private final ConcurrentHashMap<String, ReentrantLock> traderLocks = new ConcurrentHashMap<>();

    public TradingServiceImpl(OrderRepository orderRepository, PortfolioRepository portfolioRepository) {
        this.orderRepository = orderRepository;
        this.portfolioRepository = portfolioRepository;
    }

    @Transactional
    @Override
    public Order placeOrder(Order order) {
        // Get or create a fair lock for this specific trader
        ReentrantLock lock = traderLocks.computeIfAbsent(order.getTraderId(), k -> new ReentrantLock(true));
        lock.lock(); // Block concurrent threads for this trader right here

        try {
            // 1. Enforce max 3 pending orders rule safely
            List<Order> pendingOrders = orderRepository.findPendingOrdersForUpdate(order.getTraderId());
            if (pendingOrders.size() >= 3) {
                throw new IllegalStateException("Trader cannot have more than 3 pending orders.");
            }

            // 2. Enforce sell balance rule safely
            if (order.getSide() == Order.Side.SELL) {
                Portfolio portfolio = portfolioRepository.findByTraderIdAndStockForUpdate(order.getTraderId(), order.getStock())
                        .orElseThrow(() -> new IllegalArgumentException("No stock holdings found to sell."));

                if (portfolio.getQuantity() < order.getQuantity()) {
                    throw new IllegalArgumentException("Insufficient shares available to place this SELL order.");
                }
            }

            order.setStatus(Order.OrderStatus.PENDING);
            return orderRepository.save(order);
        } finally {
            lock.unlock(); // Always release the lock!
        }
    }

    @Override
    @Transactional
    public Order fillOrder(Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Order not found."));

        if (order.getStatus() != Order.OrderStatus.PENDING) {
            throw new IllegalStateException("Only PENDING orders can be filled.");
        }

        Portfolio portfolio = portfolioRepository.findByTraderIdAndStockForUpdate(order.getTraderId(), order.getStock())
                .orElseGet(() -> {
                    String sector = (order.getSector() != null) ? order.getSector() : "TECH"; // Safe fallback
                    return new Portfolio(order.getTraderId(), order.getStock(), sector, 0);
                });
        // Adjust assets based on transactional side
        if (order.getSide() == Order.Side.BUY) {
            portfolio.setQuantity(portfolio.getQuantity() + order.getQuantity());
        } else { // SELL
            if (portfolio.getQuantity() < order.getQuantity()) {
                throw new IllegalStateException("Concurrent error: Insufficient stock inventory to fill SELL order.");
            }
            portfolio.setQuantity(portfolio.getQuantity() - order.getQuantity());
        }

        portfolioRepository.save(portfolio);
        order.setStatus(Order.OrderStatus.FILLED);
        return orderRepository.save(order);
    }

    @Override
    @Transactional
    public Order cancelOrder(Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Order not found."));

        if (order.getStatus() != Order.OrderStatus.PENDING) {
            throw new IllegalStateException("Only PENDING orders can be cancelled. Current status: " + order.getStatus());
        }

        order.setStatus(Order.OrderStatus.CANCELLED);
        return orderRepository.save(order);
    }

    @Transactional(readOnly = true)
    public SectorOverlapCalculator.OverlapResult getOverlapAnalysis(String traderId) {
        List<Portfolio> portfolios = portfolioRepository.findByTraderId(traderId);
        Set<String> customPortfolioStocks = portfolios.stream()
                .filter(p -> p.getQuantity() > 0)
                .map(Portfolio::getStock)
                .collect(Collectors.toSet());

        return SectorOverlapCalculator.calculate(customPortfolioStocks);
    }
}
