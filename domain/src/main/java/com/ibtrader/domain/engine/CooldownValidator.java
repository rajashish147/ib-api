package com.ibtrader.domain.engine;

import com.ibtrader.domain.model.strategy.EvaluationHistory;
import com.ibtrader.domain.model.strategy.TradingStrategy;
import com.ibtrader.domain.port.outbound.EvaluationHistoryRepository;
import lombok.RequiredArgsConstructor;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.logging.Logger;

/**
 * Validates if a strategy is allowed to be evaluated based on its cooldown rules.
 */
@RequiredArgsConstructor
public class CooldownValidator {

    private static final Logger LOG = Logger.getLogger(CooldownValidator.class.getName());

    private final EvaluationHistoryRepository<EvaluationHistory> evaluationHistoryRepository;

    /**
     * Checks if the strategy can execute based on its cooldown configuration.
     *
     * @param strategy The strategy to check
     * @return true if the strategy can execute, false if it is in cooldown
     */
    public boolean canExecute(TradingStrategy strategy) {
        if (strategy.getCooldownMinutes() <= 0) {
            return true;
        }

        Optional<EvaluationHistory> lastHistory = evaluationHistoryRepository.findLastByStrategyId(strategy.getId());
        
        if (lastHistory.isEmpty()) {
            return true; // Never executed
        }

        Instant lastTime = lastHistory.get().getEvaluationTime();
        if (lastTime == null) {
            return true; // No time recorded
        }

        Instant nextAllowedTime = lastTime.plus(strategy.getCooldownMinutes(), ChronoUnit.MINUTES);
        Instant now = Instant.now();

        if (now.isBefore(nextAllowedTime)) {
            LOG.info(String.format("Strategy %s is in cooldown. Last executed at %s, next allowed at %s", 
                     strategy.getId(), lastTime, nextAllowedTime));
            return false;
        }

        return true;
    }
}
