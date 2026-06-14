package com.andersonvianadev.finance_control_api.domain.models;

import com.andersonvianadev.finance_control_api.domain.models.enums.RecurringType;
import com.andersonvianadev.finance_control_api.domain.models.enums.TransactionPeriodType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Data
@Entity
@Table(name = "tb_recurring_rules")
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class RecurringRule extends BaseTransaction {

    @ManyToOne
    @JoinColumn(name = "category_id", nullable = false)
    private Category category;

    @Enumerated(EnumType.STRING)
    private TransactionPeriodType transactionPeriodType;

    @Enumerated(EnumType.STRING)
    private RecurringType recurringType;

    @Builder.Default
    private Boolean isActive = true;
}
