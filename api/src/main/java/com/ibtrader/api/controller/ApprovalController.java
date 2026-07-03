package com.ibtrader.api.controller;

import com.ibtrader.domain.model.strategy.RebalancePlan;
import com.ibtrader.domain.port.inbound.ExecuteRebalancePlanUseCase;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/v1/orders")
@RequiredArgsConstructor
public class ApprovalController {

    private final ExecuteRebalancePlanUseCase executeRebalancePlanUseCase;

    @GetMapping("/pending-approval")
    public ResponseEntity<List<RebalancePlan>> getPendingApproval() {
        log.info("Fetching orders/plans pending manual approval");
        // Implementation would query repository for PENDING_APPROVAL plans.
        return ResponseEntity.ok(Collections.emptyList());
    }

    @PostMapping("/{planId}/approve")
    public ResponseEntity<Void> approvePlan(@PathVariable UUID planId) {
        log.info("Approving plan: {}", planId);
        executeRebalancePlanUseCase.execute(new ExecuteRebalancePlanUseCase.Command(planId));
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{planId}/reject")
    public ResponseEntity<Void> rejectPlan(@PathVariable UUID planId) {
        log.info("Rejecting plan: {}", planId);
        // Implementation would mark the plan as CANCELLED or REJECTED.
        return ResponseEntity.ok().build();
    }
}
