package com.ibtrader.infrastructure.persistence.adapter;

import com.ibtrader.domain.model.order.Order;
import com.ibtrader.domain.model.order.OrderStatus;
import com.ibtrader.domain.port.outbound.OrderRepository;
import com.ibtrader.infrastructure.persistence.entity.OrderEntity;
import com.ibtrader.infrastructure.persistence.mapper.OrderMapper;
import com.ibtrader.infrastructure.persistence.repository.OrderJpaRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Outbound adapter for OrderRepository.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OrderRepositoryAdapter implements OrderRepository {

    private final OrderJpaRepository jpaRepository;
    private final OrderMapper mapper;

    @Override
    @Transactional(readOnly = true)
    public Optional<Order> findById(UUID id) {
        return jpaRepository.findById(id).map(mapper::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Order> findByIbOrderId(int ibOrderId) {
        return jpaRepository.findByIbOrderId(ibOrderId).map(mapper::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Order> findByStatus(OrderStatus status) {
        return jpaRepository.findByStatus(status.name()).stream()
                .map(mapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<Order> findByRebalancePlanId(UUID planId) {
        return jpaRepository.findByRebalancePlanId(planId).stream()
                .map(mapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<Order> findOpenOrders() {
        return jpaRepository.findByStatusIn(List.of(
                OrderStatus.SUBMITTED.name(),
                OrderStatus.PARTIALLY_FILLED.name()
        )).stream()
                .map(mapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public Order save(Order order) {
        OrderEntity entity = mapper.toEntity(order);
        OrderEntity saved = jpaRepository.save(entity);
        return mapper.toDomain(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Order> findAll(int page, int size) {
        return jpaRepository.findAll(PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"))).stream()
                .map(mapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<Order> findByAssetId(UUID assetId) {
        return jpaRepository.findByAssetId(assetId).stream()
                .map(mapper::toDomain)
                .collect(Collectors.toList());
    }
}
