package com.fynxt.stocks.model;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class PortfolioTest {

    @Test
    void testPortfolioCreationWithConstructor() {
        // Arrange & Act
        Portfolio portfolio = new Portfolio("trader123", "AAPL", "TECH", 100);

        // Assert
        assertEquals("trader123", portfolio.getTraderId());
        assertEquals("AAPL", portfolio.getStock());
        assertEquals("TECH", portfolio.getSector());
        assertEquals(100, portfolio.getQuantity());
    }

    @Test
    void testPortfolioCreationWithSetters() {
        // Arrange & Act
        Portfolio portfolio = new Portfolio();
        portfolio.setId(1L);
        portfolio.setTraderId("trader456");
        portfolio.setStock("MSFT");
        portfolio.setSector("TECH");
        portfolio.setQuantity(50);

        // Assert
        assertEquals(1L, portfolio.getId());
        assertEquals("trader456", portfolio.getTraderId());
        assertEquals("MSFT", portfolio.getStock());
        assertEquals("TECH", portfolio.getSector());
        assertEquals(50, portfolio.getQuantity());
    }

    @Test
    void testPortfolioQuantityUpdate() {
        // Arrange
        Portfolio portfolio = new Portfolio("trader999", "JPM", "FINANCE", 200);

        // Act
        portfolio.setQuantity(250);

        // Assert
        assertEquals(250, portfolio.getQuantity());
    }

    @Test
    void testPortfolioZeroQuantity() {
        // Arrange & Act
        Portfolio portfolio = new Portfolio("trader123", "DELTA", "TECH", 0);

        // Assert
        assertEquals(0, portfolio.getQuantity());
    }

    @Test
    void testPortfolioNegativeQuantity() {
        // Arrange & Act
        Portfolio portfolio = new Portfolio("trader123", "DELTA", "TECH", -10);

        // Assert
        assertEquals(-10, portfolio.getQuantity());
    }

    @Test
    void testPortfolioSectorTypes() {
        // Arrange & Act
        Portfolio techPortfolio = new Portfolio("trader1", "AAPL", "TECH", 100);
        Portfolio financePortfolio = new Portfolio("trader2", "JPM", "FINANCE", 50);
        Portfolio energyPortfolio = new Portfolio("trader3", "XOM", "ENERGY", 75);

        // Assert
        assertEquals("TECH", techPortfolio.getSector());
        assertEquals("FINANCE", financePortfolio.getSector());
        assertEquals("ENERGY", energyPortfolio.getSector());
    }

    @Test
    void testMultiplePortfoliosSameTrader() {
        // Arrange & Act
        Portfolio portfolio1 = new Portfolio("trader1", "AAPL", "TECH", 100);
        Portfolio portfolio2 = new Portfolio("trader1", "MSFT", "TECH", 50);
        Portfolio portfolio3 = new Portfolio("trader1", "JPM", "FINANCE", 80);

        // Assert
        assertEquals("trader1", portfolio1.getTraderId());
        assertEquals("trader1", portfolio2.getTraderId());
        assertEquals("trader1", portfolio3.getTraderId());
        assertNotEquals(portfolio1.getStock(), portfolio2.getStock());
    }

    @Test
    void testPortfolioEquality() {
        // Arrange
        Portfolio portfolio1 = new Portfolio("trader1", "AAPL", "TECH", 100);
        Portfolio portfolio2 = new Portfolio("trader1", "AAPL", "TECH", 100);

        // Act & Assert
        assertEquals(portfolio1.getTraderId(), portfolio2.getTraderId());
        assertEquals(portfolio1.getStock(), portfolio2.getStock());
        assertEquals(portfolio1.getQuantity(), portfolio2.getQuantity());
    }

    @Test
    void testPortfolioLargeQuantity() {
        // Arrange & Act
        Portfolio portfolio = new Portfolio("trader123", "AAPL", "TECH", 1000000);

        // Assert
        assertEquals(1000000, portfolio.getQuantity());
    }
}

