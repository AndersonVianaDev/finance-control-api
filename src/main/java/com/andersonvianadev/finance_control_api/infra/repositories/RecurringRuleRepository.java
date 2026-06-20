package com.andersonvianadev.finance_control_api.infra.repositories;

import com.andersonvianadev.finance_control_api.domain.models.RecurringRule;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface RecurringRuleRepository extends JpaRepository<RecurringRule, UUID> {

    boolean existsRecurringRuleByOwnerIdAndCategoryIdAndPriceAndTransactionDateAndDescription(
            UUID ownerId,
            UUID categoryId,
            BigDecimal price,
            LocalDateTime transactionDate,
            String description
    );

    List<RecurringRule> findAllByIsActiveTrue();
    Optional<RecurringRule> findByOwnerIdAndId(UUID ownerId, UUID id);
    Page<RecurringRule> findAllByOwnerId(UUID ownerId, Pageable pageable);
    boolean existsRecurringRuleByOwnerIdAndCategoryIdAndPriceAndTransactionDateAndDescriptionAndIdNot(
            UUID ownerId,
            UUID categoryId,
            BigDecimal price,
            LocalDateTime transactionDate,
            String description,
            UUID id
    );
}
