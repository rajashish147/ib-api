import { AfterViewInit, ChangeDetectionStrategy, Component, ElementRef, Input, OnChanges, OnDestroy, SimpleChanges, ViewChild } from '@angular/core';
import { Chart, ChartConfiguration, ChartData, registerables } from 'chart.js';
import { MATERIAL_IMPORTS } from '../material.imports';

Chart.register(...registerables);

@Component({
  selector: 'app-chart-card',
  standalone: true,
  imports: [...MATERIAL_IMPORTS],
  template: `
    <mat-card class="chart-card surface card">
      <div class="header">
        <div>
          <div class="title">{{ title }}</div>
          <div class="subtitle">{{ subtitle }}</div>
        </div>
      </div>
      <div class="canvas-wrap">
        <canvas #canvas></canvas>
        @if (error) {
          <div class="overlay">
            <mat-icon class="overlay-icon error-icon">error_outline</mat-icon>
            <div class="overlay-text">Could not load chart data</div>
          </div>
        } @else if (isEmpty()) {
          <div class="overlay">
            <mat-icon class="overlay-icon">bar_chart</mat-icon>
            <div class="overlay-text">No data available yet</div>
          </div>
        }
      </div>
    </mat-card>
  `,
  styles: [`
    .chart-card { min-height: 320px; padding: 1rem 1.1rem 1.25rem; display: flex; flex-direction: column; gap: 1rem; }
    .header { display: flex; justify-content: space-between; align-items: start; }
    .title { font-weight: 700; font-size: 1rem; }
    .subtitle { color: var(--app-text-muted); font-size: 0.85rem; margin-top: 0.25rem; }
    .canvas-wrap { position: relative; flex: 1; min-height: 240px; }
    canvas { width: 100% !important; height: 100% !important; }
    .overlay {
      position: absolute; inset: 0; display: flex; flex-direction: column; align-items: center; justify-content: center;
      gap: 0.5rem; background: color-mix(in srgb, var(--app-surface-elevated) 80%, transparent);
      border-radius: var(--radius-md); backdrop-filter: blur(4px);
    }
    .overlay-icon { font-size: 2.5rem; width: 2.5rem; height: 2.5rem; color: var(--app-text-muted); }
    .error-icon { color: var(--app-negative) !important; }
    .overlay-text { color: var(--app-text-muted); font-size: 0.9rem; }
  `],
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class ChartCardComponent implements AfterViewInit, OnChanges, OnDestroy {
  @Input({ required: true }) title = '';
  @Input() subtitle = '';
  @Input({ required: true }) chartData!: ChartData<'line' | 'doughnut' | 'bar'>;
  @Input() chartType: 'line' | 'doughnut' | 'bar' = 'line';
  @Input() error = false;

  @ViewChild('canvas', { static: true }) canvas?: ElementRef<HTMLCanvasElement>;

  private chart: Chart | null = null;

  ngAfterViewInit(): void {
    this.renderChart();
  }

  ngOnChanges(changes: SimpleChanges): void {
    if (changes['chartData'] || changes['chartType'] || changes['error']) {
      this.renderChart();
    }
  }

  ngOnDestroy(): void {
    this.chart?.destroy();
  }

  isEmpty(): boolean {
    if (!this.chartData?.datasets?.length) return true;
    return this.chartData.datasets.every((ds) => !ds.data?.length);
  }

  private renderChart(): void {
    if (!this.canvas?.nativeElement || !this.chartData) {
      return;
    }

    this.chart?.destroy();

    const config: ChartConfiguration<typeof this.chartType> = {
      type: this.chartType,
      data: this.chartData,
      options: {
        responsive: true,
        maintainAspectRatio: false,
        plugins: {
          legend: {
            labels: { color: getComputedStyle(document.documentElement).getPropertyValue('--app-text') }
          }
        },
        scales: this.chartType === 'doughnut'
          ? undefined
          : {
              x: { ticks: { color: getComputedStyle(document.documentElement).getPropertyValue('--app-text-muted') }, grid: { color: getComputedStyle(document.documentElement).getPropertyValue('--app-border') } },
              y: { ticks: { color: getComputedStyle(document.documentElement).getPropertyValue('--app-text-muted') }, grid: { color: getComputedStyle(document.documentElement).getPropertyValue('--app-border') } }
            }
      }
    };

    this.chart = new Chart(this.canvas.nativeElement, config);
  }
}
