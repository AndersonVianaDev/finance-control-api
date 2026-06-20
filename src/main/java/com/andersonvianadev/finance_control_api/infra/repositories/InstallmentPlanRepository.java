package com.andersonvianadev.finance_control_api.infra.repositories;

import com.andersonvianadev.finance_control_api.domain.models.InstallmentPlan;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface InstallmentPlanRepository extends JpaRepository<InstallmentPlan, UUID> {
    Optional<InstallmentPlan> findByOwnerIdAndId(UUID ownerId, UUID id);
    Page<InstallmentPlan> findByOwnerId(UUID ownerId, Pageable pageable);
    Page<InstallmentPlan> findByOwnerIdAndFirstDueDateBetween(
            UUID ownerId, LocalDate start,
            LocalDate finish, Pageable pageable
    );
}
