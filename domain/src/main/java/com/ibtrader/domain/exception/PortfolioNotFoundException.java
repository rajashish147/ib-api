package com.ibtrader.domain.exception;

public final class PortfolioNotFoundException extends DomainException {
    
    public static final String ERROR_CODE = "PORTFOLIO_NOT_FOUND";
    
    private final String accountId;
    
    public PortfolioNotFoundException(String accountId) {
        super(ERROR_CODE, "Portfolio not found for account: " + accountId);
        this.accountId = accountId;
    }
    
    public String getAccountId() {
        return accountId;
    }
}
