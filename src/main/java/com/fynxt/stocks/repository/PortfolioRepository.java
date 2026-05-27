package com.fynxt.stocks.repository;

import com.fynxt.stocks.model.Portfolio;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import java.util.List;
import java.util.Optional;

public interface PortfolioRepository extends JpaRepository<Portfolio, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM Portfolio p WHERE p.traderId = :traderId AND p.stock = :stock")
    Optional<Portfolio> findByTraderIdAndStockForUpdate(String traderId, String stock);

    List<Portfolio> findByTraderId(String traderId);
}