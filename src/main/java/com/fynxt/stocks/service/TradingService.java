package com.fynxt.stocks.service;

import com.fynxt.stocks.domain.SectorOverlapCalculator;
import com.fynxt.stocks.model.Order;

public interface TradingService {
    public Order placeOrder(Order order);
    public Order fillOrder(Long orderId);
    public Order cancelOrder(Long orderId);
    public SectorOverlapCalculator.OverlapResult getOverlapAnalysis(String traderId);
}
