import { Component, OnInit, AfterViewInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FinanceService } from '../finance.service';

@Component({
  selector: 'app-reports',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './reports.component.html',
  styleUrl: './reports.component.css'
})
export class ReportsComponent implements OnInit, AfterViewInit {
  isLoading = true;
  categoryData: any = {};
  trendData: any = {};
  
  summary = {
    topCategory: '-',
    avgMonthlySpend: 0,
    savingsRate: 0
  };

  constructor(private financeService: FinanceService) {}

  ngOnInit() {
    this.loadReportData();
  }

  ngAfterViewInit() {
    // We'll init charts after data loads
  }

  loadReportData() {
    this.isLoading = true;
    
    // Load Category Spending
    this.financeService.getCategorySpending().subscribe({
      next: (data) => {
        this.categoryData = data;
        this.calculateSummary();
        this.renderCategoryChart();
      },
      error: (err) => console.error(err)
    });

    // Load Monthly Trend
    this.financeService.getMonthlyTrend().subscribe({
      next: (data) => {
        this.trendData = data;
        this.renderTrendChart();
        this.isLoading = false;
      },
      error: (err) => {
        console.error(err);
        this.isLoading = false;
      }
    });
  }

  calculateSummary() {
    let max = 0;
    let total = 0;
    for (const cat in this.categoryData) {
      if (this.categoryData[cat] > max) {
        max = this.categoryData[cat];
        this.summary.topCategory = cat;
      }
      total += this.categoryData[cat];
    }
    // Simple avg calculation based on available data
    this.summary.avgMonthlySpend = total / 1; // Simplification for now
  }

  renderCategoryChart() {
    const labels = Object.keys(this.categoryData);
    const series = Object.values(this.categoryData);

    if (labels.length === 0) return;

    const options = {
      series: series,
      chart: {
        type: 'donut',
        height: 350
      },
      labels: labels,
      colors: ['#6366f1', '#8b5cf6', '#a855f7', '#d946ef', '#f43f5e', '#f97316', '#eab308'],
      plotOptions: {
        pie: {
          donut: {
            size: '70%',
            labels: {
              show: true,
              total: {
                show: true,
                label: 'Total Spent',
                formatter: (w: any) => {
                  const total = w.globals.seriesTotals.reduce((a: number, b: number) => a + b, 0);
                  return '$' + total.toLocaleString(undefined, {minimumFractionDigits: 2, maximumFractionDigits: 2});
                }
              }
            }
          }
        }
      },
      dataLabels: { enabled: false },
      legend: { position: 'bottom' },
      theme: { mode: 'light' }
    };

    const ApexCharts = (window as any).ApexCharts;
    if (ApexCharts) {
      const chart = new ApexCharts(document.querySelector("#categoryChart"), options);
      chart.render();
    }
  }

  renderTrendChart() {
    const months = Object.keys(this.trendData);
    const incomeSeries: number[] = [];
    const expenseSeries: number[] = [];

    months.forEach(m => {
      incomeSeries.push(this.trendData[m]['INCOME'] || 0);
      expenseSeries.push(this.trendData[m]['EXPENSE'] || 0);
    });

    const options = {
      series: [
        { name: 'Income', data: incomeSeries },
        { name: 'Expense', data: expenseSeries }
      ],
      chart: {
        type: 'bar',
        height: 350,
        toolbar: { show: false },
        zoom: { enabled: false }
      },
      plotOptions: {
        bar: {
          horizontal: false,
          columnWidth: '55%',
          borderRadius: 6,
          borderRadiusApplication: 'end'
        }
      },
      colors: ['#10b981', '#f43f5e'],
      dataLabels: { enabled: false },
      stroke: { show: true, width: 2, colors: ['transparent'] },
      xaxis: { categories: months },
      yaxis: {
        min: 0,
        labels: {
          formatter: (val: number) => '$' + val.toLocaleString()
        }
      },
      fill: { opacity: 1 },
      grid: {
        borderColor: '#f1f1f1',
        strokeDashArray: 4
      },
      tooltip: {
        y: { formatter: (val: number) => '$' + val.toLocaleString() }
      }
    };

    const ApexCharts = (window as any).ApexCharts;
    if (ApexCharts) {
      const chart = new ApexCharts(document.querySelector("#trendChart"), options);
      chart.render();
    }
  }
}
