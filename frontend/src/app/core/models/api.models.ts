export interface MoneyDto {
  readonly amount: string | number;
  readonly currency: string;
}

export interface MarketDataQuoteDto {
  readonly assetId: string;
  readonly symbol: string;
  readonly currency: string | null;
  readonly exchange: string | null;
  readonly assetClass: string | null;
  readonly lastPrice: number | null;
  readonly priceAt: string | null;
  readonly stale: boolean;
}

export interface PositionDto {
  readonly id: string;
  readonly portfolioId: string;
  readonly assetId: string;
  readonly symbol: string;
  readonly assetClass?: string | null;
  readonly quantity: string | number;
  readonly averageCost: MoneyDto;
  readonly marketPrice: MoneyDto;
  readonly marketValue: MoneyDto;
  readonly unrealizedPnL: MoneyDto;
  readonly realizedPnL: MoneyDto;
  readonly lastUpdated: string;
}

export interface PortfolioDto {
  readonly id: string;
  readonly accountId: string;
  readonly netLiquidationValue: MoneyDto;
  readonly totalCashValue: MoneyDto;
  readonly availableFunds: MoneyDto;
  readonly buyingPower: MoneyDto;
  readonly maintenanceMargin: MoneyDto;
  readonly initialMargin: MoneyDto;
  readonly unrealizedPnL: MoneyDto;
  readonly realizedPnL: MoneyDto;
  readonly positions: readonly PositionDto[];
  readonly lastUpdated: string;
  readonly version: number;
}

export interface PortfolioSnapshotDto {
  readonly id: string;
  readonly portfolioId: string;
  readonly accountId: string;
  readonly netLiquidationValue: MoneyDto;
  readonly totalCashValue: MoneyDto;
  readonly availableFunds: MoneyDto;
  readonly buyingPower: MoneyDto;
  readonly maintenanceMargin: MoneyDto;
  readonly unrealizedPnL: MoneyDto;
  readonly realizedPnL: MoneyDto;
  readonly positionCount: number;
  readonly capturedAt: string;
}

export interface BasketTargetDto {
  readonly id: string;
  readonly symbol: string;
  readonly assetClass: string;
  readonly quantity: string | number;
}

export interface BasketTargetRequestDto {
  readonly id?: string;
  readonly symbol: string;
  readonly assetClass: string;
  readonly quantity: string | number;
}

export interface StrategyDto {
  readonly id: string;
  readonly versionId: string;
  readonly name: string;
  readonly description: string;
  readonly priority: number;
  readonly enabled: boolean;
  readonly cooldownMinutes: number;
  readonly riskProfile: string;
  readonly executionMode: string;
  readonly buyThreshold: string | number | null;
  readonly sellThreshold: string | number | null;
  readonly state: string | null;
  readonly targets: readonly BasketTargetDto[];
}

export interface StrategyRequestDto {
  readonly name: string;
  readonly description?: string | null;
  readonly priority?: number | null;
  readonly enabled?: boolean | null;
  readonly cooldownMinutes?: number | null;
  readonly riskProfile?: string | null;
  readonly executionMode?: string | null;
  readonly buyThreshold?: string | number | null;
  readonly sellThreshold?: string | number | null;
  readonly targets?: readonly BasketTargetRequestDto[] | null;
}

export interface RebalancePlanDto {
  readonly id: string;
  readonly strategyId: string;
  readonly triggerType: string;
  readonly mode: string;
  readonly portfolioNlvAtTrigger: MoneyDto;
  readonly availableBudget: MoneyDto;
  readonly status: string;
  readonly items: readonly unknown[];
  readonly notes?: string | null;
  readonly createdAt: string;
  readonly executedAt?: string | null;
  readonly completedAt?: string | null;
  readonly version: number;
}

export interface EngineStatusDto {
  readonly status: string;
  readonly message: string;
}

export interface ApiMessageDto {
  readonly status: string;
  readonly message: string;
}

export interface HealthDto {
  readonly status: string;
  readonly components?: Record<string, unknown>;
}

export interface MetricMeasureDto {
  readonly statistic: string;
  readonly value: number;
}

export interface MetricDto {
  readonly name: string;
  readonly measurements: readonly MetricMeasureDto[];
}