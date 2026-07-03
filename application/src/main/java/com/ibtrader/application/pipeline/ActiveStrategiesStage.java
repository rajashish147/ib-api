package com.ibtrader.application.pipeline;

import com.ibtrader.domain.model.strategy.TradingStrategy;
import com.ibtrader.domain.port.outbound.StrategyRepository;
import com.ibtrader.domain.port.outbound.StrategyExecutionHistoryPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class ActiveStrategiesStage implements PipelineStage {

    private final StrategyRepository strategyRepository;
    private final StrategyExecutionHistoryPort executionHistoryPort;

    @Override
    public void execute(PipelineContext context) {
        List<TradingStrategy> activeStrategies = strategyRepository.findActiveStrategies();
        
        // Filter by cooldown
        Instant now = Instant.now();
        List<TradingStrategy> strategiesToRun = activeStrategies.stream()
                .filter(strategy -> {
                    if (strategy.getCooldownMinutes() <= 0) return true;
                    
                    return executionHistoryPort.findLastSuccessfulExecution(strategy.getId())
                            .map(history -> {
                                Instant nextAllowedTime = history.getExecutedAt()
                                        .plus(strategy.getCooldownMinutes(), ChronoUnit.MINUTES);
                                if (now.isBefore(nextAllowedTime)) {
                                    log.debug("Strategy {} is in cooldown until {}", strategy.getId(), nextAllowedTime);
                                    return false;
                                }
                                return true;
                            })
                            .orElse(true);
                })
                .collect(Collectors.toList());

        if (strategiesToRun.isEmpty()) {
            log.info("No active strategies ready to run (all in cooldown or disabled).");
        }
        context.setActiveStrategies(strategiesToRun);
    }

    @Override
    public int getOrder() {
        return 20;
    }
}
