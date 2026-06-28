package com.andersonvianadev.finance_control_api.infra.repositories;

import com.andersonvianadev.finance_control_api.domain.models.Income;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface IncomeRepository extends JpaRepository<Income, UUID> {

    @Query(value = """
            SELECT EXISTS (
                SELECT 1 FROM tb_incomes
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

    Optional<Income> findByOwnerIdAndId(UUID ownerId, UUID id);
}
