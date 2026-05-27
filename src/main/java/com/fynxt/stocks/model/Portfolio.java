package com.fynxt.stocks.model;

import jakarta.persistence.*;
import lombok.*;

@Data
@Entity
@Table(name = "portfolios")
public class Portfolio {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id; // Internal database primary key

    @Column(name = "trader_id", nullable = false)
    private String traderId;

    @Column(nullable = false)
    private String stock;

    @Column(nullable = false)
    private String sector;

    @Column(nullable = false)
    private int quantity;

    public Portfolio() {
    }

    public Portfolio(String traderId, String stock, String sector, int quantity) {
        this.traderId = traderId;
        this.stock = stock;
        this.sector = sector;
        this.quantity = quantity;
    }
}