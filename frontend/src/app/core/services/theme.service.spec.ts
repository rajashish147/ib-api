import { TestBed } from '@angular/core/testing';
import { ThemeService } from './theme.service';

describe('ThemeService', () => {
  it('toggles between light and dark modes', () => {
    TestBed.configureTestingModule({ providers: [ThemeService] });
    const service = TestBed.inject(ThemeService);
    const original = service.mode();

    service.toggle();

    expect(service.mode()).not.toBe(original);
  });
});