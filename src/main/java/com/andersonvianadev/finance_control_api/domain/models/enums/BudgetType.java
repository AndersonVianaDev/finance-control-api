package com.andersonvianadev.finance_control_api.domain.models.enums;

import com.andersonvianadev.finance_control_api.domain.models.dtos.PeriodDTO;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.temporal.TemporalAdjusters;

public enum BudgetType {
    WEEKLY {
        @Override
        public PeriodDTO getPeriod() {
            LocalDate today = LocalDate.now();

            LocalDate startOfWeek = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
            LocalDate endOfWeek   = today.with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY));

            return new PeriodDTO(
                    startOfWeek.atStartOfDay(),
                    endOfWeek.atTime(LocalTime.MAX)
            );
        }
    },

    MONTHLY {
        @Override
        public PeriodDTO getPeriod() {
            LocalDate today = LocalDate.now();

            LocalDate startOfMonth = today.withDayOfMonth(1);
            LocalDate endOfMonth   = today.withDayOfMonth(today.lengthOfMonth());

            return new PeriodDTO(
                    startOfMonth.atStartOfDay(),
                    endOfMonth.atTime(LocalTime.MAX)
            );
        }
    };

    public abstract PeriodDTO getPeriod();
}
