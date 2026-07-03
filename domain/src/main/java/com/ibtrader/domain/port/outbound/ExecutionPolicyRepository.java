package com.ibtrader.domain.port.outbound;

import java.util.Optional;

/**
 * Outbound port for retrieving execution policies.
 */
public interface ExecutionPolicyRepository {
    
    /**
     * Retrieves the execution policy by its name or ID.
     * @param policyName The name or ID of the policy
     * @return The policy configuration, or Optional.empty() if not found
     */
    Optional<String> getPolicy(String policyName);
}
