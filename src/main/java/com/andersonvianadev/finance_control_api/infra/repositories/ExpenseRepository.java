package com.andersonvianadev.finance_control_api.infra.repositories;

import com.andersonvianadev.finance_control_api.domain.models.Expense;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ExpenseRepository extends JpaRepository<Expense, UUID> {

    @Query(value = """
            SELECT EXISTS (
                SELECT 1 FROM tb_expenses
                WHERE owner_id = :ownerId
                  AND category_id = :categoryId
                  AND price = :price
                  AND transaction_date = :transactionDate
                  AND description = :description
            )
            """, nativeQuery = true)
    boolean existsDuplicate(
            @Param("ownerId") UUID ownerId,
            @Param("categoryId") UUID categoryId,
            @Param("price") BigDecimal price,
            @Param("transactionDate") LocalDateTime transactionDate,
            @Param("description") String description
    );

    @Query(value = """
            SELECT EXISTS (
                SELECT 1 FROM tb_expenses
                WHERE owner_id = :ownerId
                  AND category_id = :categoryId
                  AND price = :price
                  AND transaction_date = :transactionDate
                  AND description = :description
                  AND id != :excludeId
            )
            """, nativeQuery = true)
    boolean existsDuplicateExcluding(
            @Param("ownerId") UUID ownerId,
            @Param("categoryId") UUID categoryId,
            @Param("price") BigDecimal price,
            @Param("transactionDate") LocalDateTime transactionDate,
            @Param("description") String description,
            @Param("excludeId") UUID excludeId
    );

    @Query("SELECT COALESCE(SUM(e.price), 0) FROM Expense e " +
            "WHERE e.owner.id = :ownerId " +
            "AND e.category.id = :categoryId " +
            "AND e.transactionDate >= :startDate " +
            "AND e.transactionDate < :endDate")
    BigDecimal sumByOwnerAndCategoryAndDateRange(
            @Param("ownerId") UUID ownerId,
            @Param("categoryId") UUID categoryId,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate);

    Optional<Expense> findExpenseByOwnerIdAndId(UUID ownerId, UUID id);

    Page<Expense> findByOwnerId(UUID ownerId, Pageable pageable);

    List<Expense> findByInstallmentPlanIdOrderByInstallmentNumberAsc(UUID installmentPlanId);

    @Modifying
    @Query("DELETE FROM Expense e WHERE e.installmentPlan.id = :planId")
    void deleteByInstallmentPlanId(@Param("planId") UUID planId);

    Page<Expense> findByOwnerIdAndTransactionDateBetween(
            UUID ownerId, LocalDateTime start,
            LocalDateTime finish, Pageable pageable
    );

    @Query("SELECT COALESCE(SUM(e.price), 0) FROM Expense e " +
            "WHERE e.owner.id = :ownerId " +
            "AND e.transactionDate >= :start AND e.transactionDate < :finish")
    BigDecimal sumByOwnerAndDateRange(
            @Param("ownerId") UUID ownerId,
            @Param("start") LocalDateTime start,
            @Param("finish") LocalDateTime finish);

    @Query("SELECT COUNT(e) FROM Expense e " +
            "WHERE e.owner.id = :ownerId " +
            "AND e.transactionDate >= :start AND e.transactionDate < :finish")
    Long countByOwnerAndDateRange(
            @Param("ownerId") UUID ownerId,
            @Param("start") LocalDateTime start,
            @Param("finish") LocalDateTime finish);

    @Query("SELECT e FROM Expense e " +
            "WHERE e.owner.id = :ownerId " +
            "AND e.transactionDate >= :start AND e.transactionDate < :finish")
    List<Expense> findScheduledByOwnerAndDateRange(
            @Param("ownerId") UUID ownerId,
            @Param("start") LocalDateTime start,
            @Param("finish") LocalDateTime finish);
}
