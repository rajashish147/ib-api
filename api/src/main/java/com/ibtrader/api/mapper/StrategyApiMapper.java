package com.ibtrader.api.mapper;

import com.ibtrader.api.dto.BasketTargetDto;
import com.ibtrader.api.dto.StrategyRequestDto;
import com.ibtrader.domain.model.strategy.BasketTarget;
import com.ibtrader.domain.model.strategy.TradingStrategy;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;
import org.mapstruct.ReportingPolicy;

@Mapper(
    componentModel = "spring", 
    unmappedTargetPolicy = ReportingPolicy.IGNORE,
    nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE
)
public interface StrategyApiMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "versionId", ignore = true)
    @Mapping(target = "state", ignore = true)
    TradingStrategy toDomain(StrategyRequestDto request, @MappingTarget TradingStrategy existing);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "versionId", ignore = true)
    @Mapping(target = "state", ignore = true)
    TradingStrategy toDomain(StrategyRequestDto request);

    default BasketTarget map(BasketTargetDto dto) {
        if (dto == null) {
            return null;
        }
        return dto.toDomain();
    }
}
