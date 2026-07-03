package com.ibtrader.infrastructure.adapter;

import com.ibtrader.domain.port.outbound.ExecutionPolicyRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Adapter for ExecutionPolicyRepository.
 * In a real implementation, this would load execution policies from the database.
 */
@Repository
@Slf4j
public class ExecutionPolicyRepositoryAdapter implements ExecutionPolicyRepository {

    @Override
    public Optional<String> getPolicy(String policyName) {
        log.info("Fetching execution policy: {}", policyName);
        // Mock returning the policy name itself or empty
        if (policyName == null || policyName.isBlank()) {
            return Optional.of("IMMEDIATE");
        }
        return Optional.of(policyName);
    }
}
