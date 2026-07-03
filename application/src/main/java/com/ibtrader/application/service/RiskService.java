package com.ibtrader.application.service;

import com.ibtrader.domain.event.RiskLimitUpdatedEvent;
import com.ibtrader.domain.exception.InvalidRiskLimitValueException;
import com.ibtrader.domain.model.risk.LimitType;
import com.ibtrader.domain.model.risk.RiskLimit;
import com.ibtrader.domain.port.inbound.UpdateRiskLimitUseCase;
import com.ibtrader.domain.port.outbound.DomainEventPublisher;
import com.ibtrader.domain.port.outbound.RiskLimitRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Optional;

/**
 * Application service for managing {@link RiskLimit} configurations.
 * Implements the {@link UpdateRiskLimitUseCase} inbound port.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class RiskService implements UpdateRiskLimitUseCase {

    private final RiskLimitRepository riskLimitRepository;
    private final DomainEventPublisher domainEventPublisher;

    @Override
    public RiskLimit execute(Command command) {
        if (command == null) {
            throw new IllegalArgumentException("Command must not be null");
        }

        log.info("Executing risk limit update for type: {}", command.limitType());

        if (command.newValue() == null) {
            throw new IllegalArgumentException("newValue must not be null");
        }

        // Validate range
        if (command.newValue().compareTo(BigDecimal.ZERO) < 0) {
            log.warn("Invalid risk limit value (negative): {} for limit type {}", command.newValue(), command.limitType());
            throw new InvalidRiskLimitValueException(command.limitType(), command.newValue());
        }

        if (command.limitType().isPercentage() && command.limitType() != LimitType.MAX_LEVERAGE) {
            if (command.newValue().compareTo(BigDecimal.ONE) > 0) {
                log.warn("Invalid risk limit value (exceeds 100%): {} for percentage-based limit {}", 
                         command.newValue(), command.limitType());
                throw new InvalidRiskLimitValueException(command.limitType(), command.newValue());
            }
        }

        Optional<RiskLimit> existingLimitOpt = riskLimitRepository.findByLimitType(command.limitType());

        RiskLimit limit;
        if (existingLimitOpt.isPresent()) {
            limit = existingLimitOpt.get().withValue(command.newValue());
            if (command.enabled()) {
                limit = limit.enable();
            } else {
                limit = limit.disable();
            }
        } else {
            limit = RiskLimit.of(command.limitType(), command.newValue());
            if (!command.enabled()) {
                limit = limit.disable();
            }
        }

        RiskLimit savedLimit = riskLimitRepository.save(limit);

        log.info("Risk limit {} successfully updated to value {} and enabled state: {}", 
                savedLimit.getLimitType(), savedLimit.getValue(), savedLimit.isEnabled());

        domainEventPublisher.publish(new RiskLimitUpdatedEvent(
                savedLimit.getId(),
                savedLimit.getLimitType(),
                savedLimit.getValue(),
                savedLimit.isEnabled()
        ));

        return savedLimit;
    }
}
