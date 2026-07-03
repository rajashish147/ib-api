package com.ibtrader.application.service;

import com.ibtrader.domain.event.PortfolioSnapshotTakenEvent;
import com.ibtrader.domain.exception.PortfolioNotFoundException;
import com.ibtrader.domain.model.portfolio.Portfolio;
import com.ibtrader.domain.model.portfolio.PortfolioSnapshot;
import com.ibtrader.domain.model.portfolio.Position;
import com.ibtrader.domain.port.inbound.GetPortfolioSummaryUseCase;
import com.ibtrader.domain.port.inbound.GetPositionsUseCase;
import com.ibtrader.domain.port.inbound.ReconcilePositionsUseCase;
import com.ibtrader.domain.port.inbound.GetSnapshotHistoryUseCase;
import com.ibtrader.domain.port.inbound.TakePortfolioSnapshotUseCase;
import com.ibtrader.domain.port.outbound.AccountDataPort;
import com.ibtrader.domain.port.outbound.DomainEventPublisher;
import com.ibtrader.domain.port.outbound.PortfolioRepository;
import com.ibtrader.domain.port.outbound.PortfolioSnapshotRepository;
import com.ibtrader.domain.port.outbound.PositionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

/**
 * Application service for portfolio management.
 * 
 * <p>Implements inbound use cases for retrieving portfolio summaries, positions,
 * initiating position reconciliation, and capturing portfolio snapshots.
 * Delegates data retrieval to outbound repository ports and publishes domain
 * events when snapshots are taken.</p>
 */
@Slf4j
@RequiredArgsConstructor
@Service
public class PortfolioService implements GetPortfolioSummaryUseCase, GetPositionsUseCase,
        ReconcilePositionsUseCase, TakePortfolioSnapshotUseCase, GetSnapshotHistoryUseCase {

    private final PortfolioRepository portfolioRepository;
    private final PortfolioSnapshotRepository portfolioSnapshotRepository;
    private final PositionRepository positionRepository;

    @Override
    public List<PortfolioSnapshot> execute(GetSnapshotHistoryUseCase.Query query) {
        return portfolioSnapshotRepository.findByAccountId(query.accountId(), query.limit());
    }
    private final AccountDataPort accountDataPort;
    private final DomainEventPublisher domainEventPublisher;

    @Override
    public Portfolio execute(GetPortfolioSummaryUseCase.Query query) {
        log.debug("Fetching portfolio summary for account: {}", query.accountId());
        if (query == null || query.accountId() == null || query.accountId().isBlank()) {
            throw new IllegalArgumentException("Query accountId must not be blank");
        }
        return portfolioRepository.findByAccountId(query.accountId())
                .orElseThrow(() -> new PortfolioNotFoundException(query.accountId()));
    }

    @Override
    public List<Position> execute(GetPositionsUseCase.Query query) {
        log.debug("Fetching positions for account: {}", query.accountId());
        if (query == null || query.accountId() == null || query.accountId().isBlank()) {
            throw new IllegalArgumentException("Query accountId must not be blank");
        }
        Portfolio portfolio = portfolioRepository.findByAccountId(query.accountId())
                .orElseThrow(() -> new PortfolioNotFoundException(query.accountId()));
        
        return positionRepository.findByPortfolioId(portfolio.getId());
    }

    @Override
    public void execute(ReconcilePositionsUseCase.Command command) {
        log.info("Initiating position reconciliation for account: {}", command.accountId());
        if (command == null || command.accountId() == null || command.accountId().isBlank()) {
            throw new IllegalArgumentException("Command accountId must not be blank");
        }
        
        // Ensure portfolio exists before requesting positions
        portfolioRepository.findByAccountId(command.accountId())
                .orElseThrow(() -> new PortfolioNotFoundException(command.accountId()));
                
        // Triggers the infrastructure adapter to fetch positions asynchronously.
        // The adapter is responsible for routing the callback to the reconciliation handler.
        accountDataPort.requestPositions();
    }

    @Override
    public PortfolioSnapshot execute(TakePortfolioSnapshotUseCase.Command command) {
        log.info("Taking portfolio snapshot for account: {}", command.accountId());
        if (command == null || command.accountId() == null || command.accountId().isBlank()) {
            throw new IllegalArgumentException("Command accountId must not be blank");
        }
        
        Portfolio portfolio = portfolioRepository.findByAccountId(command.accountId())
                .orElseThrow(() -> new PortfolioNotFoundException(command.accountId()));

        PortfolioSnapshot snapshot = portfolio.takeSnapshot();
        
        PortfolioSnapshot savedSnapshot = portfolioSnapshotRepository.save(snapshot);
        
        BigDecimal netLiquidationValue = savedSnapshot.getNetLiquidationValue() != null 
                ? savedSnapshot.getNetLiquidationValue().getAmount() : BigDecimal.ZERO;
        BigDecimal totalCashValue = savedSnapshot.getTotalCashValue() != null 
                ? savedSnapshot.getTotalCashValue().getAmount() : BigDecimal.ZERO;
        BigDecimal unrealizedPnL = savedSnapshot.getUnrealizedPnL() != null 
                ? savedSnapshot.getUnrealizedPnL().getAmount() : BigDecimal.ZERO;
        BigDecimal realizedPnL = savedSnapshot.getRealizedPnL() != null 
                ? savedSnapshot.getRealizedPnL().getAmount() : BigDecimal.ZERO;
        
        PortfolioSnapshotTakenEvent event = PortfolioSnapshotTakenEvent.builder()
                .portfolioId(portfolio.getId())
                .accountId(portfolio.getAccountId())
                .netLiquidationValue(netLiquidationValue)
                .totalCashValue(totalCashValue)
                .unrealizedPnL(unrealizedPnL)
                .realizedPnL(realizedPnL)
                .positionCount(savedSnapshot.getPositionCount())
                .sequenceNumber(portfolio.getVersion())
                .build();
                
        domainEventPublisher.publish(event);
        
        return savedSnapshot;
    }
}
