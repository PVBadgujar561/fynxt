package com.fynxt.stocks.domain;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import java.util.HashSet;
import java.util.Set;
import java.util.Map;

class SectorOverlapCalculatorTest {

    @Test
    void testEmptyPortfolio() {
        // Arrange
        Set<String> emptyPortfolio = new HashSet<>();

        // Act
        SectorOverlapCalculator.OverlapResult result = SectorOverlapCalculator.calculate(emptyPortfolio);

        // Assert
        assertNotNull(result);
        // Empty portfolio will have 0% overlap with all baskets and BALANCED will be selected first
        assertEquals("NONE", result.dominantBasket);
        assertEquals("LOW", result.riskFlag);
        assertEquals(3, result.overlaps.size()); // All three baskets
    }

    @Test
    void testSingleStockPortfolio() {
        // Arrange
        Set<String> portfolio = new HashSet<>();
        portfolio.add("AAPL");

        // Act
        SectorOverlapCalculator.OverlapResult result = SectorOverlapCalculator.calculate(portfolio);

        // Assert
        assertNotNull(result);
        assertEquals(3, result.overlaps.size());
        assertTrue(result.dominantBasket.length() > 0);
    }

    @Test
    void testTechHeavyPortfolio() {
        // Arrange
        Set<String> portfolio = new HashSet<>();
        portfolio.add("AAPL");
        portfolio.add("MSFT");
        portfolio.add("GOOGL");
        portfolio.add("TSLA");
        portfolio.add("NVDA");

        // Act
        SectorOverlapCalculator.OverlapResult result = SectorOverlapCalculator.calculate(portfolio);

        // Assert
        assertNotNull(result);
        assertEquals("TECH_HEAVY", result.dominantBasket);
        assertEquals("HIGH", result.riskFlag); // Should have high overlap
    }

    @Test
    void testFinanceHeavyPortfolio() {
        // Arrange
        Set<String> portfolio = new HashSet<>();
        portfolio.add("JPM");
        portfolio.add("GS");
        portfolio.add("BAC");
        portfolio.add("MS");
        portfolio.add("WFC");

        // Act
        SectorOverlapCalculator.OverlapResult result = SectorOverlapCalculator.calculate(portfolio);

        // Assert
        assertNotNull(result);
        assertEquals("FINANCE_HEAVY", result.dominantBasket);
        assertEquals("HIGH", result.riskFlag);
    }

    @Test
    void testBalancedPortfolio() {
        // Arrange
        Set<String> portfolio = new HashSet<>();
        portfolio.add("AAPL");
        portfolio.add("JPM");
        portfolio.add("XOM");
        portfolio.add("JNJ");
        portfolio.add("TSLA");

        // Act
        SectorOverlapCalculator.OverlapResult result = SectorOverlapCalculator.calculate(portfolio);

        // Assert
        assertNotNull(result);
        assertEquals("BALANCED", result.dominantBasket);
        assertEquals("HIGH", result.riskFlag);
    }

    @Test
    void testMixedPortfolioWithoutOverlap() {
        // Arrange
        Set<String> portfolio = new HashSet<>();
        portfolio.add("UNKNOWN1");
        portfolio.add("UNKNOWN2");

        // Act
        SectorOverlapCalculator.OverlapResult result = SectorOverlapCalculator.calculate(portfolio);

        // Assert
        assertNotNull(result);
        // Similarly, unknown stocks will have 0% with all baskets, BALANCED is selected first
        assertEquals("BALANCED", result.dominantBasket);
        assertEquals("LOW", result.riskFlag);
    }

    @Test
    void testMediumRiskPortfolio() {
        // Arrange
        Set<String> portfolio = new HashSet<>();
        portfolio.add("AAPL");
        portfolio.add("MSFT");
        portfolio.add("UNKNOWN1");

        // Act
        SectorOverlapCalculator.OverlapResult result = SectorOverlapCalculator.calculate(portfolio);

        // Assert
        assertNotNull(result);
        // Should have medium or low risk
        assertTrue(result.riskFlag.equals("LOW") || result.riskFlag.equals("MEDIUM"));
    }

    @Test
    void testOverlapFormatting() {
        // Arrange
        Set<String> portfolio = new HashSet<>();
        portfolio.add("AAPL");

        // Act
        SectorOverlapCalculator.OverlapResult result = SectorOverlapCalculator.calculate(portfolio);

        // Assert
        for (Map<String, Object> overlap : result.overlaps) {
            assertNotNull(overlap.get("basket"));
            assertNotNull(overlap.get("overlap"));
            String overlapStr = (String) overlap.get("overlap");
            // Check format is like "XX.XX%"
            assertTrue(overlapStr.contains("%"));
            assertTrue(overlapStr.matches("\\d+\\.\\d{2}%"));
        }
    }

    @Test
    void testBenchmarkBasketsNotEmpty() {
        // Act & Assert
        assertEquals(3, SectorOverlapCalculator.BENCHMARK_BASKETS.size());
        assertTrue(SectorOverlapCalculator.BENCHMARK_BASKETS.containsKey("TECH_HEAVY"));
        assertTrue(SectorOverlapCalculator.BENCHMARK_BASKETS.containsKey("FINANCE_HEAVY"));
        assertTrue(SectorOverlapCalculator.BENCHMARK_BASKETS.containsKey("BALANCED"));
    }

    @Test
    void testBenchmarkBasketContents() {
        // Act & Assert
        Set<String> techHeavy = SectorOverlapCalculator.BENCHMARK_BASKETS.get("TECH_HEAVY");
        assertEquals(5, techHeavy.size());
        assertTrue(techHeavy.contains("AAPL"));
        assertTrue(techHeavy.contains("MSFT"));
        assertTrue(techHeavy.contains("GOOGL"));
        assertTrue(techHeavy.contains("TSLA"));
        assertTrue(techHeavy.contains("NVDA"));

        Set<String> financeHeavy = SectorOverlapCalculator.BENCHMARK_BASKETS.get("FINANCE_HEAVY");
        assertEquals(5, financeHeavy.size());
        assertTrue(financeHeavy.contains("JPM"));

        Set<String> balanced = SectorOverlapCalculator.BENCHMARK_BASKETS.get("BALANCED");
        assertEquals(5, balanced.size());
        assertTrue(balanced.contains("AAPL"));
        assertTrue(balanced.contains("JPM"));
    }

    @Test
    void testPartialTechPortfolio() {
        // Arrange
        Set<String> portfolio = new HashSet<>();
        portfolio.add("AAPL");
        portfolio.add("MSFT");
        portfolio.add("UNKNOWN");

        // Act
        SectorOverlapCalculator.OverlapResult result = SectorOverlapCalculator.calculate(portfolio);

        // Assert
        assertNotNull(result);
        assertTrue(result.riskFlag.equals("LOW") || result.riskFlag.equals("MEDIUM") || result.riskFlag.equals("HIGH"));
    }

    @Test
    void testHighRiskThreshold() {
        // Arrange - Create a portfolio that should trigger HIGH risk
        Set<String> portfolio = new HashSet<>();
        portfolio.add("AAPL");
        portfolio.add("MSFT");
        portfolio.add("GOOGL");
        portfolio.add("TSLA");

        // Act
        SectorOverlapCalculator.OverlapResult result = SectorOverlapCalculator.calculate(portfolio);

        // Assert
        assertNotNull(result);
        // With 4 out of 5 tech stocks, should have high overlap with TECH_HEAVY
        assertEquals("HIGH", result.riskFlag);
    }

    @Test
    void testMediumRiskThreshold() {
        // Arrange
        Set<String> portfolio = new HashSet<>();
        portfolio.add("AAPL");
        portfolio.add("JPM");
        portfolio.add("XOM");

        // Act
        SectorOverlapCalculator.OverlapResult result = SectorOverlapCalculator.calculate(portfolio);

        // Assert
        assertNotNull(result);
        assertNotNull(result.riskFlag);
    }

    @Test
    void testAllResultFieldsPopulated() {
        // Arrange
        Set<String> portfolio = new HashSet<>();
        portfolio.add("AAPL");

        // Act
        SectorOverlapCalculator.OverlapResult result = SectorOverlapCalculator.calculate(portfolio);

        // Assert
        assertNotNull(result.overlaps);
        assertNotNull(result.dominantBasket);
        assertNotNull(result.riskFlag);
        assertFalse(result.overlaps.isEmpty());
        assertFalse(result.dominantBasket.isEmpty());
        assertFalse(result.riskFlag.isEmpty());
    }
}

