package com.fynxt.stocks.repository;

import com.fynxt.stocks.model.Order;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import java.util.List;

public interface OrderRepository extends JpaRepository<Order, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT o FROM Order o WHERE o.traderId = :traderId AND o.status = 'PENDING'")
    List<Order> findPendingOrdersForUpdate(String traderId);

    // Native query fallback to guarantee an absolute transaction block on Postgres for the trader namespace
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query(value = "SELECT id FROM traders WHERE id = :traderId", nativeQuery = true)
    String lockTraderIdNamespace(String traderId);
}