import { HttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { BACKEND_CONFIG } from '../config/backend-config';
import { PortfolioApiService } from './portfolio-api.service';

describe('PortfolioApiService', () => {
  it('requests the portfolio summary from the REST API', () => {
    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        { provide: BACKEND_CONFIG, useValue: { apiBaseUrl: '/api/v1' } },
        PortfolioApiService
      ]
    });

    const service = TestBed.inject(PortfolioApiService);
    const httpMock = TestBed.inject(HttpTestingController);

    service.getPortfolio('ACC-1').subscribe();

    const request = httpMock.expectOne('/api/v1/portfolio?accountId=ACC-1');
    expect(request.request.method).toBe('GET');
    request.flush({});
    httpMock.verify();
  });
});