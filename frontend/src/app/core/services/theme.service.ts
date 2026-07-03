import { Injectable, computed, signal, effect } from '@angular/core';

export type ThemeMode = 'dark' | 'light';

@Injectable({ providedIn: 'root' })
export class ThemeService {
  private readonly storageKey = 'ibkr.theme';
  private readonly modeSignal = signal<ThemeMode>(this.loadMode());

  readonly mode = computed(() => this.modeSignal());

  constructor() {
    effect(() => {
      document.documentElement.dataset['theme'] = this.modeSignal();
      localStorage.setItem(this.storageKey, this.modeSignal());
    });
  }

  toggle(): void {
    this.modeSignal.update((mode) => (mode === 'dark' ? 'light' : 'dark'));
  }

  setMode(mode: ThemeMode): void {
    this.modeSignal.set(mode);
  }

  private loadMode(): ThemeMode {
    const stored = localStorage.getItem(this.storageKey);
    return stored === 'light' ? 'light' : 'dark';
  }
}