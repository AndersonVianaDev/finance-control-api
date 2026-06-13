package com.andersonvianadev.finance_control_api.domain.models;

import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Data
@Entity
@Table(name = "tb_expenses", uniqueConstraints = @UniqueConstraint(
        name = "uk_expense_dedup",
        columnNames = {"owner_id", "category_id", "transaction_date", "price", "description"}
))
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class Expense extends BaseTransaction {

    @ManyToOne(optional = false)
    @JoinColumn(name = "category_id", nullable = false)
    private Category category;

    @ManyToOne
    @JoinColumn(name = "installment_plan_id")
    private InstallmentPlan installmentPlan;

    private Integer installmentNumber;
}
