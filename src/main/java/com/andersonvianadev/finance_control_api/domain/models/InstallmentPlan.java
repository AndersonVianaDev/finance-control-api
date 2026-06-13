package com.andersonvianadev.finance_control_api.domain.models;

import com.andersonvianadev.finance_control_api.domain.models.enums.InstallmentStatus;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Entity
@Table(name = "tb_installment_plans")
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InstallmentPlan {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "owner_id", nullable = false)
    private User owner;

    @ManyToOne(optional = false)
    @JoinColumn(name = "category_id", nullable = false)
    private Category category;

    private String description;
    private BigDecimal totalAmount;
    private BigDecimal installmentAmount;
    private Integer totalInstallments;

    @Builder.Default
    private Integer paidInstallments = 0;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    private InstallmentStatus status = InstallmentStatus.ACTIVE;

    private LocalDate firstDueDate;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @PrePersist
    public void addCreatedAt() {
        createdAt = LocalDateTime.now();
    }

    @PreUpdate
    public void addUpdatedAt() {
        updatedAt = LocalDateTime.now();
    }
}
