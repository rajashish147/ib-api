import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { ApiBaseService } from '../api/api-base.service';

export interface AssetDto {
  readonly id: string;
  readonly symbol: string;
  readonly exchange: string | null;
  readonly currency: string | null;
  readonly assetClass: string | null;
}

export interface RegisterAssetRequest {
  readonly symbol: string;
  readonly exchange?: string | null;
  readonly currency?: string | null;
  readonly assetClass?: string | null;
}

@Injectable({ providedIn: 'root' })
export class AssetApiService extends ApiBaseService {
  getAssets(): Observable<readonly AssetDto[]> {
    return this.get<readonly AssetDto[]>('/assets');
  }

  registerAsset(req: RegisterAssetRequest): Observable<AssetDto> {
    return this.post<AssetDto>('/assets', req);
  }
}
