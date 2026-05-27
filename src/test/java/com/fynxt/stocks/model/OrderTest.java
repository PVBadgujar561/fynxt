package com.fynxt.stocks.model;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import java.time.LocalDateTime;

class OrderTest {

    @Test
    void testOrderCreationWithEnums() {
        // Arrange & Act
        Order order = new Order();
        order.setId(1L);
        order.setTraderId("trader123");
        order.setStock("AAPL");
        order.setSector("TECH");
        order.setQuantity(100);
        order.setSide(Order.Side.BUY);
        order.setStatus(Order.OrderStatus.PENDING);

        // Assert
        assertEquals(1L, order.getId());
        assertEquals("trader123", order.getTraderId());
        assertEquals("AAPL", order.getStock());
        assertEquals("TECH", order.getSector());
        assertEquals(100, order.getQuantity());
        assertEquals(Order.Side.BUY, order.getSide());
        assertEquals(Order.OrderStatus.PENDING, order.getStatus());
        assertNotNull(order.getCreatedAt());
    }

    @Test
    void testOrderSideEnum() {
        // Act & Assert
        assertEquals(Order.Side.BUY, Order.Side.valueOf("BUY"));
        assertEquals(Order.Side.SELL, Order.Side.valueOf("SELL"));
    }

    @Test
    void testOrderStatusEnum() {
        // Act & Assert
        assertEquals(Order.OrderStatus.PENDING, Order.OrderStatus.valueOf("PENDING"));
        assertEquals(Order.OrderStatus.FILLED, Order.OrderStatus.valueOf("FILLED"));
        assertEquals(Order.OrderStatus.CANCELLED, Order.OrderStatus.valueOf("CANCELLED"));
    }

    @Test
    void testOrderStatusUpdate() {
        // Arrange
        Order order = new Order();
        order.setSide(Order.Side.BUY);
        order.setStatus(Order.OrderStatus.PENDING);

        // Act
        order.setStatus(Order.OrderStatus.FILLED);

        // Assert
        assertEquals(Order.OrderStatus.FILLED, order.getStatus());
    }

    @Test
    void testOrderSellSide() {
        // Arrange & Act
        Order order = new Order();
        order.setSide(Order.Side.SELL);
        order.setQuantity(50);

        // Assert
        assertEquals(Order.Side.SELL, order.getSide());
        assertEquals(50, order.getQuantity());
    }

    @Test
    void testMultipleOrders() {
        // Arrange & Act
        Order order1 = new Order();
        order1.setTraderId("trader1");
        order1.setStock("AAPL");
        order1.setQuantity(100);

        Order order2 = new Order();
        order2.setTraderId("trader2");
        order2.setStock("MSFT");
        order2.setQuantity(50);

        // Assert
        assertNotEquals(order1.getTraderId(), order2.getTraderId());
        assertNotEquals(order1.getStock(), order2.getStock());
    }
}

