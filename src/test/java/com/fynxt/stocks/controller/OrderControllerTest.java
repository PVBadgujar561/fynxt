package com.fynxt.stocks.controller;

import com.fynxt.stocks.model.Order;
import com.fynxt.stocks.model.Portfolio;
import com.fynxt.stocks.domain.SectorOverlapCalculator;
import com.fynxt.stocks.repository.PortfolioRepository;
import com.fynxt.stocks.service.TradingService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderControllerTest {

    @Mock
    private TradingService tradingService;

    @Mock
    private PortfolioRepository portfolioRepository;

    private OrderController orderController;

    @BeforeEach
    void setUp() {
        orderController = new OrderController(tradingService, portfolioRepository);
    }

    // ============= Place Order Endpoint Tests =============

    @Test
    void testPlaceOrderEndpoint() {
        // Arrange
        Order order = new Order();
        order.setTraderId("trader1");
        order.setStock("AAPL");
        order.setQuantity(100);
        order.setSide(Order.Side.BUY);
        order.setStatus(Order.OrderStatus.PENDING);
        order.setId(1L);

        when(tradingService.placeOrder(any(Order.class))).thenReturn(order);

        // Act
        ResponseEntity<Order> response = orderController.placeOrder(order);

        // Assert
        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(1L, response.getBody().getId());
        verify(tradingService, times(1)).placeOrder(any(Order.class));
    }

    @Test
    void testPlaceOrderEndpointException() {
        // Arrange
        Order order = new Order();
        order.setTraderId("trader1");
        order.setSide(Order.Side.SELL);

        when(tradingService.placeOrder(any(Order.class))).thenThrow(
                new IllegalArgumentException("No stock holdings found to sell."));

        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> orderController.placeOrder(order));
    }

    // ============= Fill Order Endpoint Tests =============

    @Test
    void testFillOrderEndpoint() {
        // Arrange
        Order order = new Order();
        order.setId(1L);
        order.setStatus(Order.OrderStatus.FILLED);

        when(tradingService.fillOrder(1L)).thenReturn(order);

        // Act
        ResponseEntity<Order> response = orderController.fillOrder(1L);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(Order.OrderStatus.FILLED, response.getBody().getStatus());
        verify(tradingService, times(1)).fillOrder(1L);
    }

    @Test
    void testFillOrderNotFound() {
        // Arrange
        when(tradingService.fillOrder(999L)).thenThrow(
                new IllegalArgumentException("Order not found."));

        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> orderController.fillOrder(999L));
    }

    // ============= Cancel Order Endpoint Tests =============

    @Test
    void testCancelOrderEndpoint() {
        // Arrange
        Order order = new Order();
        order.setId(1L);
        order.setStatus(Order.OrderStatus.CANCELLED);

        when(tradingService.cancelOrder(1L)).thenReturn(order);

        // Act
        ResponseEntity<Order> response = orderController.cancelOrder(1L);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(Order.OrderStatus.CANCELLED, response.getBody().getStatus());
        verify(tradingService, times(1)).cancelOrder(1L);
    }

    @Test
    void testCancelOrderNotFound() {
        // Arrange
        when(tradingService.cancelOrder(999L)).thenThrow(
                new IllegalArgumentException("Order not found."));

        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> orderController.cancelOrder(999L));
    }

    // ============= Get Portfolio Endpoint Tests =============

    @Test
    void testGetPortfolioEndpoint() {
        // Arrange
        List<Portfolio> holdings = new ArrayList<>();
        holdings.add(new Portfolio("trader1", "AAPL", "TECH", 100));
        holdings.add(new Portfolio("trader1", "JPM", "FINANCE", 50));

        when(portfolioRepository.findByTraderId("trader1")).thenReturn(holdings);

        // Act
        ResponseEntity<Map<String, Object>> response = orderController.getPortfolio("trader1");

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("trader1", response.getBody().get("traderId"));
        assertNotNull(response.getBody().get("positions"));
        assertNotNull(response.getBody().get("sectorBreakdown"));
        verify(portfolioRepository, times(1)).findByTraderId("trader1");
    }

    @Test
    void testGetPortfolioPositions() {
        // Arrange
        List<Portfolio> holdings = new ArrayList<>();
        holdings.add(new Portfolio("trader1", "AAPL", "TECH", 100));
        holdings.add(new Portfolio("trader1", "MSFT", "TECH", 50));

        when(portfolioRepository.findByTraderId("trader1")).thenReturn(holdings);

        // Act
        ResponseEntity<Map<String, Object>> response = orderController.getPortfolio("trader1");

        // Assert
        Map<String, Integer> positions = (Map<String, Integer>) response.getBody().get("positions");
        assertEquals(100, positions.get("AAPL"));
        assertEquals(50, positions.get("MSFT"));
    }

    @Test
    void testGetPortfolioSectorBreakdown() {
        // Arrange
        List<Portfolio> holdings = new ArrayList<>();
        holdings.add(new Portfolio("trader1", "AAPL", "TECH", 100));
        holdings.add(new Portfolio("trader1", "MSFT", "TECH", 50));
        holdings.add(new Portfolio("trader1", "JPM", "FINANCE", 75));

        when(portfolioRepository.findByTraderId("trader1")).thenReturn(holdings);

        // Act
        ResponseEntity<Map<String, Object>> response = orderController.getPortfolio("trader1");

        // Assert
        Map<String, Integer> sectorBreakdown = (Map<String, Integer>) response.getBody().get("sectorBreakdown");
        assertEquals(150, sectorBreakdown.get("TECH"));
        assertEquals(75, sectorBreakdown.get("FINANCE"));
    }

    @Test
    void testGetPortfolioIgnoresZeroQuantity() {
        // Arrange
        List<Portfolio> holdings = new ArrayList<>();
        holdings.add(new Portfolio("trader1", "AAPL", "TECH", 100));
        holdings.add(new Portfolio("trader1", "DEAD", "TECH", 0)); // Zero quantity

        when(portfolioRepository.findByTraderId("trader1")).thenReturn(holdings);

        // Act
        ResponseEntity<Map<String, Object>> response = orderController.getPortfolio("trader1");

        // Assert
        Map<String, Integer> positions = (Map<String, Integer>) response.getBody().get("positions");
        assertEquals(1, positions.size());
        assertTrue(positions.containsKey("AAPL"));
        assertFalse(positions.containsKey("DEAD"));
    }

    @Test
    void testGetPortfolioEmpty() {
        // Arrange
        when(portfolioRepository.findByTraderId("trader1")).thenReturn(new ArrayList<>());

        // Act
        ResponseEntity<Map<String, Object>> response = orderController.getPortfolio("trader1");

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        Map<String, Integer> positions = (Map<String, Integer>) response.getBody().get("positions");
        assertTrue(positions.isEmpty());
    }

    // ============= Get Sector Overlap Endpoint Tests =============

    @Test
    void testGetSectorOverlapEndpoint() {
        // Arrange
        List<Map<String, Object>> overlaps = new ArrayList<>();
        SectorOverlapCalculator.OverlapResult result = new SectorOverlapCalculator.OverlapResult(
                overlaps, "TECH_HEAVY", "HIGH"
        );

        when(tradingService.getOverlapAnalysis("trader1")).thenReturn(result);

        // Act
        ResponseEntity<SectorOverlapCalculator.OverlapResult> response = orderController.getSectorOverlap("trader1");

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("TECH_HEAVY", response.getBody().dominantBasket);
        assertEquals("HIGH", response.getBody().riskFlag);
        verify(tradingService, times(1)).getOverlapAnalysis("trader1");
    }

    @Test
    void testGetSectorOverlapLowRisk() {
        // Arrange
        List<Map<String, Object>> overlaps = new ArrayList<>();
        SectorOverlapCalculator.OverlapResult result = new SectorOverlapCalculator.OverlapResult(
                overlaps, "NONE", "LOW"
        );

        when(tradingService.getOverlapAnalysis("trader1")).thenReturn(result);

        // Act
        ResponseEntity<SectorOverlapCalculator.OverlapResult> response = orderController.getSectorOverlap("trader1");

        // Assert
        assertEquals("NONE", response.getBody().dominantBasket);
        assertEquals("LOW", response.getBody().riskFlag);
    }

    // ============= Add to Portfolio Endpoint Tests =============

    @Test
    void testAddToPortfolioNewStock() {
        // Arrange
        Portfolio input = new Portfolio();
        input.setStock("AAPL");
        input.setSector("TECH");
        input.setQuantity(100);

        Portfolio savedPortfolio = new Portfolio("trader1", "AAPL", "TECH", 100);
        savedPortfolio.setId(1L);

        when(portfolioRepository.findByTraderIdAndStockForUpdate("trader1", "AAPL"))
                .thenReturn(Optional.empty());
        when(portfolioRepository.save(any(Portfolio.class))).thenReturn(savedPortfolio);

        // Act
        ResponseEntity<Portfolio> response = orderController.addToPortfolio("trader1", input);

        // Assert
        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("trader1", response.getBody().getTraderId());
        assertEquals("AAPL", response.getBody().getStock());
        assertEquals(100, response.getBody().getQuantity());
        verify(portfolioRepository, times(1)).save(any(Portfolio.class));
    }

    @Test
    void testAddToPortfolioExistingStock() {
        // Arrange
        Portfolio input = new Portfolio();
        input.setStock("AAPL");
        input.setQuantity(50);

        Portfolio existing = new Portfolio("trader1", "AAPL", "TECH", 100);
        existing.setId(1L);

        Portfolio updated = new Portfolio("trader1", "AAPL", "TECH", 150);
        updated.setId(1L);

        when(portfolioRepository.findByTraderIdAndStockForUpdate("trader1", "AAPL"))
                .thenReturn(Optional.of(existing));
        when(portfolioRepository.save(any(Portfolio.class))).thenReturn(updated);

        // Act
        ResponseEntity<Portfolio> response = orderController.addToPortfolio("trader1", input);

        // Assert
        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(150, response.getBody().getQuantity());
        verify(portfolioRepository, times(1)).save(any(Portfolio.class));
    }

    @Test
    void testAddToPortfolioEnforcesTraderIdFromUrl() {
        // Arrange
        Portfolio input = new Portfolio();
        input.setStock("AAPL");
        input.setTraderId("wrongTrader"); // This should be overridden
        input.setQuantity(100);

        Portfolio savedPortfolio = new Portfolio("trader1", "AAPL", "TECH", 100);
        savedPortfolio.setId(1L);

        when(portfolioRepository.findByTraderIdAndStockForUpdate("trader1", "AAPL"))
                .thenReturn(Optional.empty());
        when(portfolioRepository.save(any(Portfolio.class))).thenReturn(savedPortfolio);

        // Act
        ResponseEntity<Portfolio> response = orderController.addToPortfolio("trader1", input);

        // Assert
        assertEquals("trader1", response.getBody().getTraderId());
    }

    // ============= Exception Handler Tests =============

    @Test
    void testExceptionHandlerIllegalStateException() {
        // Arrange
        when(tradingService.placeOrder(any(Order.class))).thenThrow(
                new IllegalStateException("Trader cannot have more than 3 pending orders."));

        Order order = new Order();

        // Act
        assertThrows(IllegalStateException.class, () -> orderController.placeOrder(order));
    }

    @Test
    void testExceptionHandlerIllegalArgumentException() {
        // Arrange
        when(tradingService.placeOrder(any(Order.class))).thenThrow(
                new IllegalArgumentException("Invalid input"));

        Order order = new Order();

        // Act
        assertThrows(IllegalArgumentException.class, () -> orderController.placeOrder(order));
    }

    @Test
    void testAddToPortfolioMultipleTimes() {
        // Arrange
        Portfolio input1 = new Portfolio();
        input1.setStock("AAPL");
        input1.setQuantity(100);

        Portfolio existing = new Portfolio("trader1", "AAPL", "TECH", 0);
        existing.setId(1L);

        Portfolio updated = new Portfolio("trader1", "AAPL", "TECH", 100);
        updated.setId(1L);

        when(portfolioRepository.findByTraderIdAndStockForUpdate("trader1", "AAPL"))
                .thenReturn(Optional.of(existing))
                .thenReturn(Optional.of(updated));
        when(portfolioRepository.save(any(Portfolio.class))).thenReturn(updated);

        // Act
        ResponseEntity<Portfolio> response1 = orderController.addToPortfolio("trader1", input1);

        // Assert
        assertEquals(HttpStatus.CREATED, response1.getStatusCode());
        assertEquals(100, response1.getBody().getQuantity());
    }
}

