package com.ibtrader.api.controller;

import com.ibtrader.domain.model.portfolio.Portfolio;
import com.ibtrader.domain.model.portfolio.PortfolioSnapshot;
import com.ibtrader.domain.port.inbound.GetPortfolioSummaryUseCase;
import com.ibtrader.domain.port.inbound.GetSnapshotHistoryUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/portfolio")
@RequiredArgsConstructor
public class PortfolioController {

    @Value("${ib.accounts.default.paper}")
    private String defaultAccountId;

    private final GetPortfolioSummaryUseCase getPortfolioSummaryUseCase;
    private final GetSnapshotHistoryUseCase getSnapshotHistoryUseCase;

    @GetMapping
    public ResponseEntity<Portfolio> getPortfolio(@RequestParam(required = false) String accountId) {
        if (accountId == null || accountId.isEmpty()) {
            accountId = defaultAccountId;
        }
        try {
            Portfolio portfolio = getPortfolioSummaryUseCase.execute(new GetPortfolioSummaryUseCase.Query(accountId));
            return ResponseEntity.ok(portfolio);
        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/snapshots")
    public ResponseEntity<List<PortfolioSnapshot>> getSnapshotHistory(
            @RequestParam(required = false) String accountId,
            @RequestParam(defaultValue = "50") int limit) {

        if (accountId == null || accountId.isEmpty()) {
            accountId = defaultAccountId;
        }
        return ResponseEntity.ok(getSnapshotHistoryUseCase.execute(new GetSnapshotHistoryUseCase.Query(accountId, limit)));
    }
}
