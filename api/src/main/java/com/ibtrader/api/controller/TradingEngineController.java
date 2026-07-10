package com.ibtrader.api.controller;

import com.ibtrader.application.EngineState;
import com.ibtrader.application.TradingEngineOrchestrator;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/engine")
@RequiredArgsConstructor
public class TradingEngineController {

    @Value("${app.ib.accounts.default.paper:DUP854695}")
    private String defaultAccountId;

    private final TradingEngineOrchestrator tradingEngineOrchestrator;
    private final EngineState engineState;

    @PostMapping("/trigger")
    public ResponseEntity<Map<String, String>> triggerPipeline() {
        String accountId = defaultAccountId;
        tradingEngineOrchestrator.executePipeline(accountId);
        return ResponseEntity.ok(Map.of(
            "status", "success",
            "message", "Trading pipeline triggered for account " + accountId
        ));
    }

    @PostMapping("/pause")
    public ResponseEntity<Map<String, String>> pauseEngine() {
        engineState.pause();
        return ResponseEntity.ok(Map.of("status", "paused", "message", "Engine paused successfully"));
    }

    @PostMapping("/resume")
    public ResponseEntity<Map<String, String>> resumeEngine() {
        engineState.resume();
        return ResponseEntity.ok(Map.of("status", "running", "message", "Engine resumed successfully"));
    }

    @GetMapping("/status")
    public ResponseEntity<Map<String, String>> getStatus() {
        String status = engineState.isRunning() ? "running" : "paused";
        return ResponseEntity.ok(Map.of(
            "status", status,
            "message", "Engine is currently " + status
        ));
    }
}
