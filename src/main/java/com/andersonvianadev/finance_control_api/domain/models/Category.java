package com.andersonvianadev.finance_control_api.domain.models;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Table(name = "tb_categories", uniqueConstraints = @UniqueConstraint(
        columnNames = {"name", "owner_id"}
))
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Category {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, length = 30)
    private String name;

    @Column(length = 50)
    private String description;

    @Column(length = 20)
    private String icon;

    @ManyToOne
    @JoinColumn(name = "owner_id")
    private User owner;

    @PrePersist
    @PreUpdate
    private void normalizeFields() {
        if (name != null) name = name.toLowerCase();
    }
}
