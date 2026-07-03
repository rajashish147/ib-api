package com.ibtrader.application.service;

import com.ibtrader.domain.event.AssetRegisteredEvent;
import com.ibtrader.domain.exception.AssetAlreadyRegisteredException;
import com.ibtrader.domain.model.asset.Asset;
import com.ibtrader.domain.port.inbound.RegisterAssetUseCase;
import com.ibtrader.domain.port.outbound.AssetRepository;
import com.ibtrader.domain.port.outbound.DomainEventPublisher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Application service for managing {@link Asset} aggregates.
 * Implements the {@link RegisterAssetUseCase} inbound port.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AssetService implements RegisterAssetUseCase {

    private final AssetRepository assetRepository;
    private final DomainEventPublisher domainEventPublisher;

    @Override
    public Asset execute(Command command) {
        if (command == null) {
            throw new IllegalArgumentException("Command must not be null");
        }

        log.info("Executing asset registration for symbol: {}", command.symbol());

        String normalizedSymbol = command.symbol().toUpperCase();

        if (assetRepository.findBySymbol(normalizedSymbol).isPresent()) {
            log.warn("Asset registration failed. Symbol already exists: {}", normalizedSymbol);
            throw new AssetAlreadyRegisteredException(normalizedSymbol);
        }

        Asset asset = Asset.create(
                command.symbol(),
                command.exchange(),
                command.currency(),
                command.assetClass()
        );

        if (command.multiplier() != null) {
            asset = asset.toBuilder().multiplier(command.multiplier()).build();
        }

        Asset savedAsset = assetRepository.save(asset);

        log.info("Asset registered successfully with ID: {}", savedAsset.getId());

        domainEventPublisher.publish(new AssetRegisteredEvent(
                savedAsset.getId(),
                savedAsset.getSymbol(),
                savedAsset.getExchange(),
                savedAsset.getCurrency(),
                savedAsset.getAssetClass()
        ));

        return savedAsset;
    }
}
