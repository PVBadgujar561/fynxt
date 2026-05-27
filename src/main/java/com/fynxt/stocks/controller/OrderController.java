package com.fynxt.stocks.controller;

import com.fynxt.stocks.domain.SectorOverlapCalculator;
import com.fynxt.stocks.model.Order;
import com.fynxt.stocks.model.Portfolio;
import com.fynxt.stocks.repository.PortfolioRepository;
import com.fynxt.stocks.service.TradingService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api")
public class OrderController {

    private final TradingService tradingService;
    private final PortfolioRepository portfolioRepository; // Injected directly for quick reading endpoints

    public OrderController(TradingService tradingService, PortfolioRepository portfolioRepository) {
        this.tradingService = tradingService;
        this.portfolioRepository = portfolioRepository;
    }

    /**
     * 1. Place an Order
     * Endpoint: POST /api/orders
     */
    @PostMapping("/orders")
    public ResponseEntity<Order> placeOrder(@RequestBody Order order) {
        Order savedOrder = tradingService.placeOrder(order);
        return new ResponseEntity<>(savedOrder, HttpStatus.CREATED);
    }

    /**
     * 2. Fill an Order
     * Endpoint: POST /api/orders/{id}/fill
     */
    @PostMapping("/orders/{id}/fill")
    public ResponseEntity<Order> fillOrder(@PathVariable Long id) {
        Order filledOrder = tradingService.fillOrder(id);
        return ResponseEntity.ok(filledOrder);
    }

    /**
     * 3. Cancel an Order
     * Endpoint: POST /api/orders/{id}/cancel
     */
    @PostMapping("/orders/{id}/cancel")
    public ResponseEntity<Order> cancelOrder(@PathVariable Long id) {
        Order cancelledOrder = tradingService.cancelOrder(id);
        return ResponseEntity.ok(cancelledOrder);
    }

    /**
     * 4. Get Portfolio
     * Endpoint: GET /api/portfolios/{traderId}
     */
    @GetMapping("/portfolios/{traderId}")
    public ResponseEntity<Map<String, Object>> getPortfolio(@PathVariable String traderId) {
        List<Portfolio> holdings = portfolioRepository.findByTraderId(traderId);

        // Map positions: { "AAPL": 150, "TSLA": 80 }
        Map<String, Integer> positions = holdings.stream()
                .filter(p -> p.getQuantity() > 0)
                .collect(Collectors.toMap(Portfolio::getStock, Portfolio::getQuantity));

        // Group & sum sector breakdown: { "TECH": 230 }
        Map<String, Integer> sectorBreakdown = holdings.stream()
                .filter(p -> p.getQuantity() > 0)
                .collect(Collectors.groupingBy(
                        Portfolio::getSector,
                        Collectors.summingInt(Portfolio::getQuantity)
                ));

        Map<String, Object> response = new HashMap<>();
        response.put("traderId", traderId);
        response.put("positions", positions);
        response.put("sectorBreakdown", sectorBreakdown);

        return ResponseEntity.ok(response);
    }

    /**
     * 5. Sector Overlap Analysis
     * Endpoint: GET /api/portfolios/{traderId}/overlap
     */
    @GetMapping("/portfolios/{traderId}/overlap")
    public ResponseEntity<SectorOverlapCalculator.OverlapResult> getSectorOverlap(@PathVariable String traderId) {
        SectorOverlapCalculator.OverlapResult result = tradingService.getOverlapAnalysis(traderId);
        return ResponseEntity.ok(result);
    }

    /**
     * 6. Add Directly to Portfolio
     * Endpoint: POST /api/portfolios/{traderId}/items
     */
    @PostMapping("/portfolios/{traderId}/items")
    public ResponseEntity<Portfolio> addToPortfolio(@PathVariable String traderId, @RequestBody Portfolio input) {
        // Enforce the URL path parameter into the entity mapping structure safely
        input.setTraderId(traderId);

        // Find if item already exists to update it, or initialize a clean save
        Portfolio targetPortfolio = portfolioRepository.findByTraderIdAndStockForUpdate(traderId, input.getStock())
                .orElse(input);

        if (targetPortfolio.getId() != null) {
            targetPortfolio.setQuantity(targetPortfolio.getQuantity() + input.getQuantity());
        }

        Portfolio savedPortfolio = portfolioRepository.save(targetPortfolio);
        return new ResponseEntity<>(savedPortfolio, HttpStatus.CREATED);
    }

    // --- Global Exception Handler inside Controller for Clean Responses ---
    @ExceptionHandler({IllegalStateException.class, IllegalArgumentException.class})
    public ResponseEntity<Map<String, String>> handleBusinessExceptions(RuntimeException ex) {
        Map<String, String> errorDetails = new HashMap<>();
        errorDetails.put("status", "REJECTED");
        errorDetails.put("message", ex.getMessage());
        return new ResponseEntity<>(errorDetails, HttpStatus.BAD_REQUEST);
    }
}