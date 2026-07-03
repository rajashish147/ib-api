package com.ibtrader.domain.exception;

/**
 * Exception thrown when attempting to register an asset that already exists.
 */
public class AssetAlreadyRegisteredException extends DomainException {

    public AssetAlreadyRegisteredException(String symbol) {
        super("ASSET_ALREADY_REGISTERED", String.format("Asset with symbol '%s' is already registered.", symbol));
    }
}
