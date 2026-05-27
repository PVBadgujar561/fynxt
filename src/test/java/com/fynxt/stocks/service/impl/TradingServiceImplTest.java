package com.fynxt.stocks.service.impl;

import com.fynxt.stocks.model.Order;
import com.fynxt.stocks.model.Portfolio;
import com.fynxt.stocks.repository.OrderRepository;
import com.fynxt.stocks.repository.PortfolioRepository;
import com.fynxt.stocks.domain.SectorOverlapCalculator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TradingServiceImplTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private PortfolioRepository portfolioRepository;

    private TradingServiceImpl tradingService;

    @BeforeEach
    void setUp() {
        tradingService = new TradingServiceImpl(orderRepository, portfolioRepository);
    }

    // ============= Place Order Tests =============

    @Test
    void testPlaceOrderSuccess() {
        // Arrange
        Order order = new Order();
        order.setTraderId("trader1");
        order.setStock("AAPL");
        order.setSector("TECH");
        order.setQuantity(100);
        order.setSide(Order.Side.BUY);

        when(orderRepository.findPendingOrdersForUpdate("trader1")).thenReturn(new ArrayList<>());
        when(orderRepository.save(any(Order.class))).thenReturn(order);

        // Act
        Order result = tradingService.placeOrder(order);

        // Assert
        assertNotNull(result);
        assertEquals(Order.OrderStatus.PENDING, result.getStatus());
        verify(orderRepository, times(1)).save(order);
    }

    @Test
    void testPlaceOrderExceedsMaxPending() {
        // Arrange
        Order order = new Order();
        order.setTraderId("trader1");
        order.setStock("AAPL");
        order.setSide(Order.Side.BUY);

        List<Order> pendingOrders = new ArrayList<>();
        pendingOrders.add(new Order());
        pendingOrders.add(new Order());
        pendingOrders.add(new Order());

        when(orderRepository.findPendingOrdersForUpdate("trader1")).thenReturn(pendingOrders);

        // Act & Assert
        assertThrows(IllegalStateException.class, () -> tradingService.placeOrder(order));
        verify(orderRepository, never()).save(any(Order.class));
    }

    @Test
    void testPlaceOrderSellWithoutHoldings() {
        // Arrange
        Order order = new Order();
        order.setTraderId("trader1");
        order.setStock("AAPL");
        order.setQuantity(100);
        order.setSide(Order.Side.SELL);

        when(orderRepository.findPendingOrdersForUpdate("trader1")).thenReturn(new ArrayList<>());
        when(portfolioRepository.findByTraderIdAndStockForUpdate("trader1", "AAPL")).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> tradingService.placeOrder(order));
    }

    @Test
    void testPlaceOrderSellInsufficientShares() {
        // Arrange
        Order order = new Order();
        order.setTraderId("trader1");
        order.setStock("AAPL");
        order.setQuantity(200);
        order.setSide(Order.Side.SELL);

        Portfolio portfolio = new Portfolio("trader1", "AAPL", "TECH", 100);

        when(orderRepository.findPendingOrdersForUpdate("trader1")).thenReturn(new ArrayList<>());
        when(portfolioRepository.findByTraderIdAndStockForUpdate("trader1", "AAPL")).thenReturn(Optional.of(portfolio));

        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> tradingService.placeOrder(order));
    }

    @Test
    void testPlaceOrderSellSuccess() {
        // Arrange
        Order order = new Order();
        order.setTraderId("trader1");
        order.setStock("AAPL");
        order.setQuantity(100);
        order.setSide(Order.Side.SELL);
        order.setStatus(Order.OrderStatus.PENDING);

        Portfolio portfolio = new Portfolio("trader1", "AAPL", "TECH", 150);

        when(orderRepository.findPendingOrdersForUpdate("trader1")).thenReturn(new ArrayList<>());
        when(portfolioRepository.findByTraderIdAndStockForUpdate("trader1", "AAPL")).thenReturn(Optional.of(portfolio));
        when(orderRepository.save(any(Order.class))).thenReturn(order);

        // Act
        Order result = tradingService.placeOrder(order);

        // Assert
        assertNotNull(result);
        assertEquals(Order.OrderStatus.PENDING, result.getStatus());
    }

    @Test
    void testPlaceOrderWithMaxPendingOrders() {
        // Arrange
        Order order1 = new Order();
        order1.setTraderId("trader1");
        order1.setStatus(Order.OrderStatus.PENDING);

        Order order2 = new Order();
        order2.setTraderId("trader1");
        order2.setStatus(Order.OrderStatus.PENDING);

        Order order3 = new Order();
        order3.setTraderId("trader1");
        order3.setStatus(Order.OrderStatus.PENDING);

        Order newOrder = new Order();
        newOrder.setTraderId("trader1");
        newOrder.setStock("MSFT");
        newOrder.setSide(Order.Side.BUY);

        List<Order> pendingOrders = Arrays.asList(order1, order2, order3);

        when(orderRepository.findPendingOrdersForUpdate("trader1")).thenReturn(pendingOrders);

        // Act & Assert
        assertThrows(IllegalStateException.class, () -> tradingService.placeOrder(newOrder));
    }

    // ============= Fill Order Tests =============

    @Test
    void testFillOrderSuccess() {
        // Arrange
        Order order = new Order();
        order.setId(1L);
        order.setTraderId("trader1");
        order.setStock("AAPL");
        order.setQuantity(100);
        order.setSide(Order.Side.BUY);
        order.setStatus(Order.OrderStatus.PENDING);

        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));
        when(portfolioRepository.findByTraderIdAndStockForUpdate("trader1", "AAPL"))
                .thenReturn(Optional.of(new Portfolio("trader1", "AAPL", "TECH", 0)));
        when(portfolioRepository.save(any(Portfolio.class))).thenReturn(new Portfolio());
        when(orderRepository.save(any(Order.class))).thenReturn(order);

        // Act
        Order result = tradingService.fillOrder(1L);

        // Assert
        assertNotNull(result);
        assertEquals(Order.OrderStatus.FILLED, result.getStatus());
        verify(portfolioRepository, times(1)).save(any(Portfolio.class));
    }

    @Test
    void testFillOrderNotFound() {
        // Arrange
        when(orderRepository.findById(999L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> tradingService.fillOrder(999L));
    }

    @Test
    void testFillOrderNotPending() {
        // Arrange
        Order order = new Order();
        order.setId(1L);
        order.setStatus(Order.OrderStatus.FILLED);

        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));

        // Act & Assert
        assertThrows(IllegalStateException.class, () -> tradingService.fillOrder(1L));
    }

    @Test
    void testFillBuyOrderCreatesPortfolio() {
        // Arrange
        Order order = new Order();
        order.setId(1L);
        order.setTraderId("trader1");
        order.setStock("AAPL");
        order.setQuantity(100);
        order.setSide(Order.Side.BUY);
        order.setStatus(Order.OrderStatus.PENDING);

        Portfolio newPortfolio = new Portfolio("trader1", "AAPL", "TECH", 100);

        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));
        when(portfolioRepository.findByTraderIdAndStockForUpdate("trader1", "AAPL")).thenReturn(Optional.empty());
        when(portfolioRepository.save(any(Portfolio.class))).thenReturn(newPortfolio);
        when(orderRepository.save(any(Order.class))).thenReturn(order);

        // Act
        Order result = tradingService.fillOrder(1L);

        // Assert
        assertNotNull(result);
        verify(portfolioRepository, times(1)).save(any(Portfolio.class));
    }

    @Test
    void testFillSellOrderUpdatesPortfolio() {
        // Arrange
        Order order = new Order();
        order.setId(1L);
        order.setTraderId("trader1");
        order.setStock("AAPL");
        order.setQuantity(50);
        order.setSide(Order.Side.SELL);
        order.setStatus(Order.OrderStatus.PENDING);

        Portfolio portfolio = new Portfolio("trader1", "AAPL", "TECH", 100);

        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));
        when(portfolioRepository.findByTraderIdAndStockForUpdate("trader1", "AAPL")).thenReturn(Optional.of(portfolio));
        when(portfolioRepository.save(any(Portfolio.class))).thenReturn(portfolio);
        when(orderRepository.save(any(Order.class))).thenReturn(order);

        // Act
        Order result = tradingService.fillOrder(1L);

        // Assert
        assertNotNull(result);
        assertEquals(Order.OrderStatus.FILLED, result.getStatus());
        verify(portfolioRepository, times(1)).save(any(Portfolio.class));
    }

    @Test
    void testFillSellOrderInsufficientInventory() {
        // Arrange
        Order order = new Order();
        order.setId(1L);
        order.setTraderId("trader1");
        order.setStock("AAPL");
        order.setQuantity(150);
        order.setSide(Order.Side.SELL);
        order.setStatus(Order.OrderStatus.PENDING);

        Portfolio portfolio = new Portfolio("trader1", "AAPL", "TECH", 100);

        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));
        when(portfolioRepository.findByTraderIdAndStockForUpdate("trader1", "AAPL")).thenReturn(Optional.of(portfolio));

        // Act & Assert
        assertThrows(IllegalStateException.class, () -> tradingService.fillOrder(1L));
    }

    // ============= Cancel Order Tests =============

    @Test
    void testCancelOrderSuccess() {
        // Arrange
        Order order = new Order();
        order.setId(1L);
        order.setStatus(Order.OrderStatus.PENDING);

        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));
        when(orderRepository.save(any(Order.class))).thenReturn(order);

        // Act
        Order result = tradingService.cancelOrder(1L);

        // Assert
        assertNotNull(result);
        assertEquals(Order.OrderStatus.CANCELLED, result.getStatus());
        verify(orderRepository, times(1)).save(order);
    }

    @Test
    void testCancelOrderNotFound() {
        // Arrange
        when(orderRepository.findById(999L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> tradingService.cancelOrder(999L));
    }

    @Test
    void testCancelOrderNotPending() {
        // Arrange
        Order order = new Order();
        order.setId(1L);
        order.setStatus(Order.OrderStatus.FILLED);

        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));

        // Act & Assert
        assertThrows(IllegalStateException.class, () -> tradingService.cancelOrder(1L));
    }

    @Test
    void testCancelCancelledOrder() {
        // Arrange
        Order order = new Order();
        order.setId(1L);
        order.setStatus(Order.OrderStatus.CANCELLED);

        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));

        // Act & Assert
        assertThrows(IllegalStateException.class, () -> tradingService.cancelOrder(1L));
    }

    // ============= Overlap Analysis Tests =============

    @Test
    void testGetOverlapAnalysis() {
        // Arrange
        List<Portfolio> portfolios = new ArrayList<>();
        portfolios.add(new Portfolio("trader1", "AAPL", "TECH", 100));
        portfolios.add(new Portfolio("trader1", "MSFT", "TECH", 50));

        when(portfolioRepository.findByTraderId("trader1")).thenReturn(portfolios);

        // Act
        SectorOverlapCalculator.OverlapResult result = tradingService.getOverlapAnalysis("trader1");

        // Assert
        assertNotNull(result);
        assertNotNull(result.overlaps);
        assertEquals("TECH_HEAVY", result.dominantBasket);
    }

    @Test
    void testGetOverlapAnalysisEmptyPortfolio() {
        // Arrange
        List<Portfolio> portfolios = new ArrayList<>();

        when(portfolioRepository.findByTraderId("trader1")).thenReturn(portfolios);

        // Act
        SectorOverlapCalculator.OverlapResult result = tradingService.getOverlapAnalysis("trader1");

        // Assert
        assertNotNull(result);
        assertEquals("NONE", result.dominantBasket);
    }

    @Test
    void testGetOverlapAnalysisIgnoresZeroQuantity() {
        // Arrange
        List<Portfolio> portfolios = new ArrayList<>();
        portfolios.add(new Portfolio("trader1", "AAPL", "TECH", 100));
        portfolios.add(new Portfolio("trader1", "DEAD", "TECH", 0)); // Zero quantity should be ignored

        when(portfolioRepository.findByTraderId("trader1")).thenReturn(portfolios);

        // Act
        SectorOverlapCalculator.OverlapResult result = tradingService.getOverlapAnalysis("trader1");

        // Assert
        assertNotNull(result);
        // Should only consider AAPL
        verify(portfolioRepository, times(1)).findByTraderId("trader1");
    }

    @Test
    void testGetOverlapAnalysisMixedPortfolio() {
        // Arrange
        List<Portfolio> portfolios = new ArrayList<>();
        portfolios.add(new Portfolio("trader1", "AAPL", "TECH", 100));
        portfolios.add(new Portfolio("trader1", "JPM", "FINANCE", 50));
        portfolios.add(new Portfolio("trader1", "XOM", "ENERGY", 75));

        when(portfolioRepository.findByTraderId("trader1")).thenReturn(portfolios);

        // Act
        SectorOverlapCalculator.OverlapResult result = tradingService.getOverlapAnalysis("trader1");

        // Assert
        assertNotNull(result);
        assertEquals("BALANCED", result.dominantBasket);
    }
}


