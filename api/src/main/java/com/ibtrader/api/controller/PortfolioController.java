package com.ibtrader.api.controller;

import com.ibtrader.domain.model.portfolio.Portfolio;
import com.ibtrader.domain.model.portfolio.PortfolioSnapshot;
import com.ibtrader.domain.port.inbound.GetPortfolioSummaryUseCase;
import com.ibtrader.domain.port.inbound.GetSnapshotHistoryUseCase;
import com.ibtrader.domain.port.inbound.ReconcilePositionsUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/portfolio")
@RequiredArgsConstructor
public class PortfolioController {

    @Value("${app.ib.accounts.default.paper:DUP854695}")
    private String defaultAccountId;

    private final GetPortfolioSummaryUseCase getPortfolioSummaryUseCase;
    private final GetSnapshotHistoryUseCase getSnapshotHistoryUseCase;
    private final ReconcilePositionsUseCase reconcilePositionsUseCase;

    @GetMapping
    public ResponseEntity<Portfolio> getPortfolio(@RequestParam(required = false) String accountId) {
        String resolvedAccountId = resolveAccountId(accountId);
        Portfolio portfolio = getPortfolioSummaryUseCase.execute(new GetPortfolioSummaryUseCase.Query(resolvedAccountId));
        return ResponseEntity.ok(portfolio);
    }

    @GetMapping("/snapshots")
    public ResponseEntity<List<PortfolioSnapshot>> getSnapshotHistory(
            @RequestParam(required = false) String accountId,
            @RequestParam(defaultValue = "50") int limit) {

        String resolvedAccountId = resolveAccountId(accountId);
        return ResponseEntity.ok(getSnapshotHistoryUseCase.execute(new GetSnapshotHistoryUseCase.Query(resolvedAccountId, limit)));
    }

    @PostMapping("/reconcile")
    public ResponseEntity<Map<String, String>> reconcilePositions(@RequestParam(required = false) String accountId) {
        String resolvedAccountId = resolveAccountId(accountId);
        reconcilePositionsUseCase.execute(new ReconcilePositionsUseCase.Command(resolvedAccountId));
        return ResponseEntity.accepted().body(Map.of(
                "message", "Position reconciliation triggered for account " + resolvedAccountId,
                "status", "ACCEPTED"
        ));
    }

    private String resolveAccountId(String accountId) {
        return (accountId == null || accountId.isEmpty()) ? defaultAccountId : accountId;
    }
}
