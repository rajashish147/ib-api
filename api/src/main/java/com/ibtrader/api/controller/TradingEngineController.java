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

    @Value("${ib.accounts.default.paper}")
    private String defaultAccountId;

    private final TradingEngineOrchestrator tradingEngineOrchestrator;
    private final EngineState engineState;

    @PostMapping("/trigger")
    public ResponseEntity<Map<String, String>> triggerPipeline() {
        // In a real application, accountId might come from a user session or JWT token
        String accountId = defaultAccountId;
        
        // Execute asynchronously or synchronously depending on the Orchestrator
        // For now, we trigger it synchronously (or it spawns a thread)
        try {
            tradingEngineOrchestrator.executePipeline(accountId);
            return ResponseEntity.ok(Map.of(
                "status", "success",
                "message", "Trading Pipeline triggered for account " + accountId
            ));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of(
                "status", "error",
                "message", e.getMessage()
            ));
        }
    }

    @PostMapping("/pause")
    public ResponseEntity<Map<String, String>> pauseEngine() {
        engineState.pause();
        return ResponseEntity.ok(Map.of("status", "paused"));
    }

    @PostMapping("/resume")
    public ResponseEntity<Map<String, String>> resumeEngine() {
        engineState.resume();
        return ResponseEntity.ok(Map.of("status", "running"));
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
