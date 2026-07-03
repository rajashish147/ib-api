import { AfterViewInit, ChangeDetectionStrategy, Component, ElementRef, Input, OnChanges, OnDestroy, SimpleChanges, ViewChild } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Chart, ChartConfiguration, ChartData, registerables } from 'chart.js';
import { MATERIAL_IMPORTS } from '../material.imports';

Chart.register(...registerables);

@Component({
  selector: 'app-chart-card',
  standalone: true,
  imports: [CommonModule, ...MATERIAL_IMPORTS],
  template: `
    <mat-card class="chart-card surface card">
      <div class="header">
        <div>
          <div class="title">{{ title }}</div>
          <div class="subtitle">{{ subtitle }}</div>
        </div>
      </div>
      <canvas #canvas></canvas>
    </mat-card>
  `,
  styles: [`
    .chart-card { min-height: 320px; padding: 1rem 1.1rem 1.25rem; display: flex; flex-direction: column; gap: 1rem; }
    .header { display: flex; justify-content: space-between; align-items: start; }
    .title { font-weight: 700; font-size: 1rem; }
    .subtitle { color: var(--app-text-muted); font-size: 0.85rem; margin-top: 0.25rem; }
    canvas { width: 100% !important; height: 240px !important; }
  `],
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class ChartCardComponent implements AfterViewInit, OnChanges, OnDestroy {
  @Input({ required: true }) title = '';
  @Input() subtitle = '';
  @Input({ required: true }) chartData!: ChartData<'line' | 'doughnut' | 'bar'>;
  @Input() chartType: 'line' | 'doughnut' | 'bar' = 'line';

  @ViewChild('canvas', { static: true }) canvas?: ElementRef<HTMLCanvasElement>;

  private chart: Chart | null = null;

  ngAfterViewInit(): void {
    this.renderChart();
  }

  ngOnChanges(changes: SimpleChanges): void {
    if (changes['chartData'] || changes['chartType']) {
      this.renderChart();
    }
  }

  ngOnDestroy(): void {
    this.chart?.destroy();
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
              x: { ticks: { color: getComputedStyle(document.documentElement).getPropertyValue('--app-text-muted') }, grid: { color: 'rgba(148, 163, 184, 0.12)' } },
              y: { ticks: { color: getComputedStyle(document.documentElement).getPropertyValue('--app-text-muted') }, grid: { color: 'rgba(148, 163, 184, 0.12)' } }
            }
      }
    };

    this.chart = new Chart(this.canvas.nativeElement, config);
  }
}