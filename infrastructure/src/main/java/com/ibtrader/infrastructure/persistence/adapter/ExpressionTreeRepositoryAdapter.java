package com.ibtrader.infrastructure.persistence.adapter;

import com.ibtrader.domain.model.strategy.ExpressionNode;
import com.ibtrader.domain.port.outbound.ExpressionTreeRepository;
import com.ibtrader.infrastructure.persistence.repository.ExpressionNodeJpaRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Repository
@Profile("!demo")
@RequiredArgsConstructor
public class ExpressionTreeRepositoryAdapter implements ExpressionTreeRepository {

    private final ExpressionNodeJpaRepository jpaRepository;

    @Override
    public ExpressionNode save(ExpressionNode node) { 
        return node; 
    }

    @Override
    public Optional<ExpressionNode> findById(UUID id) { 
        return Optional.empty(); 
    }

    @Override
    public Optional<ExpressionNode> findByStrategyId(UUID strategyId) { 
        return Optional.empty(); 
    }

    @Override
    public List<ExpressionNode> findAll() { 
        return List.of(); 
    }

    @Override
    public void deleteById(UUID id) {
    }
}
