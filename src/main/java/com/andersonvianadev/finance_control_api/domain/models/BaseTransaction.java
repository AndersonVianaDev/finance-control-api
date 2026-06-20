package com.andersonvianadev.finance_control_api.domain.models;

import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@SuperBuilder
@MappedSuperclass
@NoArgsConstructor
@AllArgsConstructor
public class BaseTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    private LocalDateTime transactionDate;
    private BigDecimal price;
    private String description;

    @ManyToOne
    @JoinColumn(name = "owner_id")
    private User owner;

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
