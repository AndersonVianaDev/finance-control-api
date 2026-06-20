package com.andersonvianadev.finance_control_api.infra.scheduler;

import com.andersonvianadev.finance_control_api.domain.services.IRecurringRuleService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class RecurringScheduler {

    private final IRecurringRuleService recurringRuleService;

    @Scheduled(cron = "0 0 0 * * *")
    public void generateRecurringOccurrences() {
        log.info("Recurring scheduler triggered");
        recurringRuleService.processRecurringOccurrences();
    }
}
