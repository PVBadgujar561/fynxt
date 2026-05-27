package com.fynxt.stocks;

import com.fynxt.stocks.model.Order;
import com.fynxt.stocks.model.Portfolio;
import com.fynxt.stocks.repository.OrderRepository;
import com.fynxt.stocks.repository.PortfolioRepository;
import com.fynxt.stocks.service.TradingService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * End-to-end integration tests for the trading system.
 * Tests complete workflows across multiple components.
 */
@SpringBootTest
@Transactional
class TradingSystemE2ETest {

    @Autowired
    private TradingService tradingService;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private PortfolioRepository portfolioRepository;

    @BeforeEach
    void setUp() {
        // Clear repositories before each test
        orderRepository.deleteAll();
        portfolioRepository.deleteAll();
    }

    // ============= Buy Order Workflow Tests =============

    @Test
    void testCompleteBuyOrderWorkflow() {
        // Arrange: Create and place a BUY order
        Order buyOrder = new Order();
        buyOrder.setTraderId("trader1");
        buyOrder.setStock("AAPL");
        buyOrder.setSector("TECH");
        buyOrder.setQuantity(100);
        buyOrder.setSide(Order.Side.BUY);

        // Act: Place the order
        Order placedOrder = tradingService.placeOrder(buyOrder);
        assertNotNull(placedOrder.getId());
        assertEquals(Order.OrderStatus.PENDING, placedOrder.getStatus());

        // Act: Fill the order
        Order filledOrder = tradingService.fillOrder(placedOrder.getId());
        assertEquals(Order.OrderStatus.FILLED, filledOrder.getStatus());

        // Assert: Verify portfolio was updated
        Portfolio portfolio = portfolioRepository.findByTraderIdAndStockForUpdate("trader1", "AAPL")
                .orElseThrow(() -> new AssertionError("Portfolio not found"));
        assertEquals(100, portfolio.getQuantity());
    }

    @Test
    void testMultipleBuyOrdersFromDifferentTraders() {
        // Arrange
        Order order1 = new Order();
        order1.setTraderId("trader1");
        order1.setStock("AAPL");
        order1.setQuantity(100);
        order1.setSide(Order.Side.BUY);

        Order order2 = new Order();
        order2.setTraderId("trader2");
        order2.setStock("MSFT");
        order2.setQuantity(50);
        order2.setSide(Order.Side.BUY);

        // Act
        Order placed1 = tradingService.placeOrder(order1);
        Order placed2 = tradingService.placeOrder(order2);

        tradingService.fillOrder(placed1.getId());
        tradingService.fillOrder(placed2.getId());

        // Assert
        Portfolio portfolio1 = portfolioRepository.findByTraderIdAndStockForUpdate("trader1", "AAPL").orElseThrow();
        Portfolio portfolio2 = portfolioRepository.findByTraderIdAndStockForUpdate("trader2", "MSFT").orElseThrow();

        assertEquals("trader1", portfolio1.getTraderId());
        assertEquals("trader2", portfolio2.getTraderId());
        assertEquals(100, portfolio1.getQuantity());
        assertEquals(50, portfolio2.getQuantity());
    }

    // ============= Sell Order Workflow Tests =============

    @Test
    void testCompleteSellOrderWorkflow() {
        // Arrange: First, establish a position by buying
        Order buyOrder = new Order();
        buyOrder.setTraderId("trader1");
        buyOrder.setStock("AAPL");
        buyOrder.setQuantity(100);
        buyOrder.setSide(Order.Side.BUY);

        Order placedBuy = tradingService.placeOrder(buyOrder);
        tradingService.fillOrder(placedBuy.getId());

        // Act: Place a SELL order
        Order sellOrder = new Order();
        sellOrder.setTraderId("trader1");
        sellOrder.setStock("AAPL");
        sellOrder.setQuantity(50);
        sellOrder.setSide(Order.Side.SELL);

        Order placedSell = tradingService.placeOrder(sellOrder);
        Order filledSell = tradingService.fillOrder(placedSell.getId());

        // Assert
        assertEquals(Order.OrderStatus.FILLED, filledSell.getStatus());
        Portfolio portfolio = portfolioRepository.findByTraderIdAndStockForUpdate("trader1", "AAPL").orElseThrow();
        assertEquals(50, portfolio.getQuantity()); // 100 - 50 = 50
    }

    @Test
    void testSellOrderFailsWithoutHoldings() {
        // Arrange
        Order sellOrder = new Order();
        sellOrder.setTraderId("trader1");
        sellOrder.setStock("AAPL");
        sellOrder.setQuantity(100);
        sellOrder.setSide(Order.Side.SELL);

        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> tradingService.placeOrder(sellOrder));
    }

    @Test
    void testSellOrderFailsWithInsufficientShares() {
        // Arrange: Buy 100 shares
        Order buyOrder = new Order();
        buyOrder.setTraderId("trader1");
        buyOrder.setStock("AAPL");
        buyOrder.setQuantity(100);
        buyOrder.setSide(Order.Side.BUY);

        Order placedBuy = tradingService.placeOrder(buyOrder);
        tradingService.fillOrder(placedBuy.getId());

        // Act & Assert: Try to sell 150 shares
        Order sellOrder = new Order();
        sellOrder.setTraderId("trader1");
        sellOrder.setStock("AAPL");
        sellOrder.setQuantity(150);
        sellOrder.setSide(Order.Side.SELL);

        assertThrows(IllegalArgumentException.class, () -> tradingService.placeOrder(sellOrder));
    }

    // ============= Order Cancellation Workflow Tests =============

    @Test
    void testCancelPendingOrder() {
        // Arrange
        Order order = new Order();
        order.setTraderId("trader1");
        order.setStock("AAPL");
        order.setSector("TECH");
        order.setQuantity(100);
        order.setSide(Order.Side.BUY);

        Order placedOrder = tradingService.placeOrder(order);

        // Act
        Order cancelledOrder = tradingService.cancelOrder(placedOrder.getId());

        // Assert
        assertEquals(Order.OrderStatus.CANCELLED, cancelledOrder.getStatus());
    }

    @Test
    void testCancelFilledOrderFails() {
        // Arrange
        Order order = new Order();
        order.setTraderId("trader1");
        order.setStock("AAPL");
        order.setSector("TECH");
        order.setQuantity(100);
        order.setSide(Order.Side.BUY);

        Order placedOrder = tradingService.placeOrder(order);
        Order filledOrder = tradingService.fillOrder(placedOrder.getId());

        // Act & Assert
        assertThrows(IllegalStateException.class, () -> tradingService.cancelOrder(filledOrder.getId()));
    }

    // ============= Max Pending Orders Constraint Tests =============

    @Test
    void testMaxThreePendingOrdersConstraint() {
        // Arrange
        Order order1 = new Order();
        order1.setTraderId("trader1");
        order1.setStock("AAPL");
        order1.setQuantity(100);
        order1.setSide(Order.Side.BUY);

        Order order2 = new Order();
        order2.setTraderId("trader1");
        order2.setStock("MSFT");
        order2.setQuantity(100);
        order2.setSide(Order.Side.BUY);

        Order order3 = new Order();
        order3.setTraderId("trader1");
        order3.setStock("GOOGL");
        order3.setQuantity(100);
        order3.setSide(Order.Side.BUY);

        Order order4 = new Order();
        order4.setTraderId("trader1");
        order4.setStock("TSLA");
        order4.setQuantity(100);
        order4.setSide(Order.Side.BUY);

        // Act
        tradingService.placeOrder(order1);
        tradingService.placeOrder(order2);
        tradingService.placeOrder(order3);

        // Assert
        assertThrows(IllegalStateException.class, () -> tradingService.placeOrder(order4));
    }

    @Test
    void testThirdOrderSucceedsAfterOneIsFilled() {
        // Arrange: Place 3 orders
        Order order1 = new Order();
        order1.setTraderId("trader1");
        order1.setStock("AAPL");
        order1.setQuantity(100);
        order1.setSide(Order.Side.BUY);

        Order order2 = new Order();
        order2.setTraderId("trader1");
        order2.setStock("MSFT");
        order2.setQuantity(100);
        order2.setSide(Order.Side.BUY);

        Order order3 = new Order();
        order3.setTraderId("trader1");
        order3.setStock("GOOGL");
        order3.setQuantity(100);
        order3.setSide(Order.Side.BUY);

        Order order4 = new Order();
        order4.setTraderId("trader1");
        order4.setStock("TSLA");
        order4.setQuantity(100);
        order4.setSide(Order.Side.BUY);

        Order placed1 = tradingService.placeOrder(order1);
        Order placed2 = tradingService.placeOrder(order2);
        Order placed3 = tradingService.placeOrder(order3);

        // Act: Fill the first order
        tradingService.fillOrder(placed1.getId());

        // Now we should be able to place a 4th order
        Order placed4 = tradingService.placeOrder(order4);

        // Assert
        assertNotNull(placed4.getId());
        assertEquals(Order.OrderStatus.PENDING, placed4.getStatus());
    }

    // ============= Portfolio Accumulation Tests =============

    @Test
    void testPortfolioAccumulatesMultipleOrders() {
        // Arrange
        Order order1 = new Order();
        order1.setTraderId("trader1");
        order1.setStock("AAPL");
        order1.setQuantity(100);
        order1.setSide(Order.Side.BUY);

        Order order2 = new Order();
        order2.setTraderId("trader1");
        order2.setStock("AAPL");
        order2.setQuantity(50);
        order2.setSide(Order.Side.BUY);

        // Act
        Order placed1 = tradingService.placeOrder(order1);
        Order placed2 = tradingService.placeOrder(order2);

        tradingService.fillOrder(placed1.getId());
        tradingService.fillOrder(placed2.getId());

        // Assert
        Portfolio portfolio = portfolioRepository.findByTraderIdAndStockForUpdate("trader1", "AAPL").orElseThrow();
        assertEquals(150, portfolio.getQuantity()); // 100 + 50
    }

    // ============= Sector Overlap Analysis Tests =============

    @Test
    void testOverlapAnalysisAfterOrders() {
        // Arrange: Place and fill multiple orders to build a portfolio
        Order order1 = new Order();
        order1.setTraderId("trader1");
        order1.setStock("AAPL");
        order1.setQuantity(100);
        order1.setSide(Order.Side.BUY);

        Order order2 = new Order();
        order2.setTraderId("trader1");
        order2.setStock("MSFT");
        order2.setQuantity(100);
        order2.setSide(Order.Side.BUY);

        Order placed1 = tradingService.placeOrder(order1);
        Order placed2 = tradingService.placeOrder(order2);

        tradingService.fillOrder(placed1.getId());
        tradingService.fillOrder(placed2.getId());

        // Act
        var overlapResult = tradingService.getOverlapAnalysis("trader1");

        // Assert
        assertNotNull(overlapResult);
        assertEquals("TECH_HEAVY", overlapResult.dominantBasket);
        assertNotNull(overlapResult.riskFlag);
    }

    // ============= Mixed Operations Tests =============

    @Test
    void testComplexTradeSequence() {
        // Arrange & Act: Complex sequence of trades
        // 1. Buy 100 AAPL
        Order buyAApl = new Order();
        buyAApl.setTraderId("trader1");
        buyAApl.setStock("AAPL");
        buyAApl.setQuantity(100);
        buyAApl.setSide(Order.Side.BUY);
        Order placedBuyAAPL = tradingService.placeOrder(buyAApl);
        tradingService.fillOrder(placedBuyAAPL.getId());

        // 2. Buy 50 MSFT
        Order buyMSFT = new Order();
        buyMSFT.setTraderId("trader1");
        buyMSFT.setStock("MSFT");
        buyMSFT.setQuantity(50);
        buyMSFT.setSide(Order.Side.BUY);
        Order placedBuyMSFT = tradingService.placeOrder(buyMSFT);
        tradingService.fillOrder(placedBuyMSFT.getId());

        // 3. Sell 30 AAPL
        Order sellAAPL = new Order();
        sellAAPL.setTraderId("trader1");
        sellAAPL.setStock("AAPL");
        sellAAPL.setQuantity(30);
        sellAAPL.setSide(Order.Side.SELL);
        Order placedSellAAPL = tradingService.placeOrder(sellAAPL);
        tradingService.fillOrder(placedSellAAPL.getId());

        // Assert: Verify final portfolio state
        Portfolio aaplPortfolio = portfolioRepository.findByTraderIdAndStockForUpdate("trader1", "AAPL").orElseThrow();
        Portfolio msftPortfolio = portfolioRepository.findByTraderIdAndStockForUpdate("trader1", "MSFT").orElseThrow();

        assertEquals(70, aaplPortfolio.getQuantity()); // 100 - 30
        assertEquals(50, msftPortfolio.getQuantity());
    }

    @Test
    void testOrderHistoryTracking() {
        // Arrange
        Order buyOrder = new Order();
        buyOrder.setTraderId("trader1");
        buyOrder.setStock("AAPL");
        buyOrder.setQuantity(100);
        buyOrder.setSide(Order.Side.BUY);

        // Act
        Order placed = tradingService.placeOrder(buyOrder);
        Order filled = tradingService.fillOrder(placed.getId());

        // Assert: Verify order is persisted
        Order retrieved = orderRepository.findById(filled.getId()).orElseThrow();
        assertEquals(Order.OrderStatus.FILLED, retrieved.getStatus());
        assertEquals("trader1", retrieved.getTraderId());
        assertEquals(100, retrieved.getQuantity());
    }

    @Test
    void testIsolationBetweenTraders() {
        // Arrange
        Order order1 = new Order();
        order1.setTraderId("trader1");
        order1.setStock("AAPL");
        order1.setQuantity(100);
        order1.setSide(Order.Side.BUY);

        Order order2 = new Order();
        order2.setTraderId("trader2");
        order2.setStock("MSFT");
        order2.setQuantity(100);
        order2.setSide(Order.Side.BUY);

        // Act
        Order placed1 = tradingService.placeOrder(order1);
        Order placed2 = tradingService.placeOrder(order2);

        tradingService.fillOrder(placed1.getId());
        tradingService.fillOrder(placed2.getId());

        // Assert: Verify traders don't interfere with each other
        Portfolio portfolio1 = portfolioRepository.findByTraderIdAndStockForUpdate("trader1", "AAPL").orElseThrow();
        Portfolio portfolio2 = portfolioRepository.findByTraderIdAndStockForUpdate("trader2", "MSFT").orElseThrow();

        assertEquals("trader1", portfolio1.getTraderId());
        assertEquals("trader2", portfolio2.getTraderId());

        // Trader1 shouldn't have MSFT
        assertTrue(portfolioRepository.findByTraderIdAndStockForUpdate("trader1", "MSFT").isEmpty());
        // Trader2 shouldn't have AAPL
        assertTrue(portfolioRepository.findByTraderIdAndStockForUpdate("trader2", "AAPL").isEmpty());
    }
}

