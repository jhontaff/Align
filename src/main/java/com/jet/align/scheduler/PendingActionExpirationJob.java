package com.jet.align.scheduler;

import com.jet.align.ai.tool.PendingActionService;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PendingActionExpirationJob {

    private final PendingActionService pendingActionService;

    // Cada hora: acota a ~1 h el delay entre cruzar el TTL (24 h) y quedar EXPIRED.
    // Suficiente resolución para housekeeping; nada hoy lee este estado en tiempo real.
    @Scheduled(fixedRate = 60 * 60 * 1000)
    public void run() {
        pendingActionService.expireStale();
    }
}

