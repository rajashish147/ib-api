package com.ibtrader.api.controller;

import com.ibtrader.api.dto.StrategyRequestDto;
import com.ibtrader.api.dto.StrategyResponseDto;
import com.ibtrader.api.mapper.StrategyApiMapper;
import com.ibtrader.domain.model.strategy.TradingStrategy;
import com.ibtrader.domain.port.inbound.ManageStrategyUseCase;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/**
 * REST controller for managing basket trading strategies.
 *
 * <p>Provides CRUD operations for strategies along with
 * enable/disable toggles. CORS is handled globally by {@code CorsConfig}.</p>
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/strategies")
@RequiredArgsConstructor
public class StrategyController {

    private final ManageStrategyUseCase manageStrategyUseCase;
    private final StrategyApiMapper strategyApiMapper;

    /**
     * Returns all active (enabled) trading strategies.
     */
    @GetMapping
    public ResponseEntity<List<StrategyResponseDto>> getActiveStrategies() {
        return ResponseEntity.ok(toResponseList(manageStrategyUseCase.getActiveStrategies()));
    }

    /**
     * Returns all strategies (enabled and disabled).
     */
    @GetMapping("/all")
    public ResponseEntity<List<StrategyResponseDto>> getAllStrategies() {
        return ResponseEntity.ok(toResponseList(manageStrategyUseCase.getAllStrategies()));
    }

    /**
     * Returns a specific strategy by ID.
     */
    @GetMapping("/{id}")
    public ResponseEntity<StrategyResponseDto> getStrategyById(@PathVariable UUID id) {
        return ResponseEntity.ok(StrategyResponseDto.from(manageStrategyUseCase.getStrategyById(id)));
    }

    /**
     * Creates a new basket trading strategy.
     */
    @PostMapping
    public ResponseEntity<StrategyResponseDto> createStrategy(@Valid @RequestBody StrategyRequestDto request) {
        TradingStrategy strategy = strategyApiMapper.toDomain(request);
        log.info("Creating strategy: {} with {} basket targets", strategy.getName(),
                strategy.getTargets() != null ? strategy.getTargets().size() : 0);
        TradingStrategy saved = manageStrategyUseCase.createStrategy(strategy);
        return ResponseEntity.ok(StrategyResponseDto.from(saved));
    }

    /**
     * Updates an existing strategy by ID.
     */
    @PutMapping("/{id}")
    public ResponseEntity<StrategyResponseDto> updateStrategy(
            @PathVariable UUID id,
            @Valid @RequestBody StrategyRequestDto request) {

        TradingStrategy existing = manageStrategyUseCase.getStrategyById(id);
        TradingStrategy toUpdate = strategyApiMapper.toDomain(request, existing);
        TradingStrategy updated = manageStrategyUseCase.updateStrategy(id, toUpdate);
        log.info("Updated strategy: {}", id);
        return ResponseEntity.ok(StrategyResponseDto.from(updated));
    }

    /**
     * Enables a strategy (sets enabled=true).
     */
    @PutMapping("/{id}/enable")
    public ResponseEntity<StrategyResponseDto> enableStrategy(@PathVariable UUID id) {
        TradingStrategy enabled = manageStrategyUseCase.toggleStrategy(id, true);
        log.info("Enabling strategy: {}", id);
        return ResponseEntity.ok(StrategyResponseDto.from(enabled));
    }

    /**
     * Disables a strategy (sets enabled=false). The engine will ignore disabled strategies.
     */
    @PutMapping("/{id}/disable")
    public ResponseEntity<StrategyResponseDto> disableStrategy(@PathVariable UUID id) {
        TradingStrategy disabled = manageStrategyUseCase.toggleStrategy(id, false);
        log.info("Disabling strategy: {}", id);
        return ResponseEntity.ok(StrategyResponseDto.from(disabled));
    }

    /**
     * Deletes a strategy permanently by ID.
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteStrategy(@PathVariable UUID id) {
        manageStrategyUseCase.deleteStrategy(id);
        log.info("Deleted strategy: {}", id);
        return ResponseEntity.noContent().build();
    }

    private static List<StrategyResponseDto> toResponseList(List<TradingStrategy> strategies) {
        return strategies.stream()
                .map(StrategyResponseDto::from)
                .toList();
    }
}
