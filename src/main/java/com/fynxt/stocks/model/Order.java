package com.fynxt.stocks.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@Entity
@Table(name = "orders")
public class Order {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String traderId;
    private String stock;
    private String sector;
    private int quantity;

    @Enumerated(EnumType.STRING)
    private Side side; // BUY, SELL

    @Enumerated(EnumType.STRING)
    private OrderStatus status; // PENDING, FILLED, CANCELLED

    private LocalDateTime createdAt = LocalDateTime.now();

    // Enums
    public enum Side { BUY, SELL }
    public enum OrderStatus { PENDING, FILLED, CANCELLED }

}
