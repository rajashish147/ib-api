package com.ibtrader.api.controller;

import com.ibtrader.api.dto.AssetRequestDto;
import com.ibtrader.api.dto.AssetResponseDto;
import com.ibtrader.domain.model.asset.Asset;
import com.ibtrader.domain.model.asset.AssetClass;
import com.ibtrader.domain.port.inbound.RegisterAssetUseCase;
import com.ibtrader.domain.port.outbound.AssetRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/**
 * REST controller exposing the asset registry.
 *
 * <p>Assets are the tradeable instruments (stocks, ETFs, futures, etc.) known to the
 * platform. This controller reads directly from {@link AssetRepository} (a simple,
 * read-mostly outbound port with no orchestration needed) and delegates writes to
 * {@link RegisterAssetUseCase}, consistent with how {@code PortfolioController} reads
 * directly from its use cases. CORS is handled globally by {@code CorsConfig}.</p>
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/assets")
@RequiredArgsConstructor
public class AssetController {

    private final AssetRepository assetRepository;
    private final RegisterAssetUseCase registerAssetUseCase;

    /**
     * Returns all registered assets, optionally filtered to only enabled ones.
     *
     * @param enabledOnly when {@code true}, returns only assets enabled for trading
     */
    @GetMapping
    public ResponseEntity<List<AssetResponseDto>> getAssets(
            @RequestParam(name = "enabledOnly", defaultValue = "false") boolean enabledOnly) {

        List<Asset> assets = enabledOnly ? assetRepository.findAllEnabled() : assetRepository.findAll();
        return ResponseEntity.ok(assets.stream().map(AssetResponseDto::from).toList());
    }

    /**
     * Returns a single asset by its domain ID.
     */
    @GetMapping("/{id}")
    public ResponseEntity<AssetResponseDto> getAssetById(@PathVariable UUID id) {
        return assetRepository.findById(id)
                .map(AssetResponseDto::from)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    /**
     * Returns a single asset by ticker symbol (case-insensitive).
     */
    @GetMapping("/symbol/{symbol}")
    public ResponseEntity<AssetResponseDto> getAssetBySymbol(@PathVariable String symbol) {
        return assetRepository.findBySymbol(symbol.toUpperCase())
                .map(AssetResponseDto::from)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    /**
     * Registers a new tradeable asset.
     */
    @PostMapping
    public ResponseEntity<AssetResponseDto> registerAsset(@Valid @RequestBody AssetRequestDto request) {
        RegisterAssetUseCase.Command command = new RegisterAssetUseCase.Command(
                request.symbol(),
                request.exchange(),
                request.currency(),
                AssetClass.valueOf(request.assetClass().toUpperCase()),
                request.multiplier());

        Asset registered = registerAssetUseCase.execute(command);
        log.info("Registered asset {} ({})", registered.getSymbol(), registered.getId());
        return ResponseEntity.status(HttpStatus.CREATED).body(AssetResponseDto.from(registered));
    }
}
