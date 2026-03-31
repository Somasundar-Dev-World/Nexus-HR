import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { FinanceService, Transaction } from '../finance.service';
import { forkJoin } from 'rxjs';

@Component({
  selector: 'app-reports',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './reports.component.html',
  styleUrl: './reports.component.css'
})
export class ReportsComponent implements OnInit {
  isLoading = true;
  selectedPeriod: string = 'this-month';
  
  // Custom Range State
  customStart: string = "";
  customEnd: string = "";

  // Data for sections
  categoryList: any[] = [];
  monthlyTrend: any = {};
  frequentMerchants: any[] = [];
  largestPurchases: Transaction[] = [];
  comparison: any = {
    currentIncome: 0, 
    currentSpending: 0, 
    currentBills: 0,
    incomeChange: 0,
    spendingChange: 0,
    billsChange: 0
  };

  categoryDonutData: any = { series: [], labels: [] };
  
  constructor(private financeService: FinanceService) {
    this.initCustomDates();
  }

  ngOnInit() {
    this.fetchAllData();
  }

  initCustomDates() {
    const now = new Date();
    this.customEnd = now.toISOString().split('T')[0];
    const thirtyDaysAgo = new Date(now.setDate(now.getDate() - 30));
    this.customStart = thirtyDaysAgo.toISOString().split('T')[0];
  }

  setPeriod(period: string) {
    if (this.selectedPeriod === period) return;
    this.selectedPeriod = period;
    this.fetchAllData();
  }

  getPeriodDates() {
    const now = new Date();
    let start: string | undefined;
    let end: string | undefined;

    if (this.selectedPeriod === 'this-month') {
      const firstDay = new Date(now.getFullYear(), now.getMonth(), 1);
      start = firstDay.toISOString().split('T')[0];
      end = now.toISOString().split('T')[0];
    } else if (this.selectedPeriod === 'last-month') {
      const firstDayLastMonth = new Date(now.getFullYear(), now.getMonth() - 1, 1);
      const lastDayLastMonth = new Date(now.getFullYear(), now.getMonth(), 0);
      start = firstDayLastMonth.toISOString().split('T')[0];
      end = lastDayLastMonth.toISOString().split('T')[0];
    } else if (this.selectedPeriod === 'all-time') {
      start = undefined;
      end = undefined;
    } else if (this.selectedPeriod === 'custom') {
      start = this.customStart;
      end = this.customEnd;
    }

    return { start, end };
  }

  fetchAllData() {
    this.isLoading = true;
    const { start, end } = this.getPeriodDates();
    
    forkJoin({
      categorySpending: this.financeService.getCategorySpending(start, end),
      monthlyTrend: this.financeService.getMonthlyTrend(), // Trend always shows 6 months context
      frequent: this.financeService.getFrequentMerchants(start, end),
      largest: this.financeService.getLargestPurchases(start, end),
      comparison: this.financeService.getSpendingComparison(start)
    }).subscribe({
      next: (res) => {
        this.processCategoryData(res.categorySpending);
        this.monthlyTrend = res.monthlyTrend;
        this.frequentMerchants = res.frequent;
        this.largestPurchases = res.largest;
        this.comparison = res.comparison;
        
        this.isLoading = false;
        // Delay rendering slightly to ensure *ngIf has stamped the DOM
        setTimeout(() => this.renderCharts(), 0);
      },
      error: (err) => {
        console.error(err);
        this.isLoading = false;
      }
    });
  }

  processCategoryData(data: any) {
    const total = Object.values(data).reduce((a: any, b: any) => a + b, 0) as number;
    this.categoryDonutData.series = Object.values(data);
    this.categoryDonutData.labels = Object.keys(data);
    
    this.categoryList = Object.keys(data).map(name => {
      const amount = data[name];
      return {
        name,
        amount,
        percent: total > 0 ? (amount / total * 100).toFixed(0) : 0,
        change: Math.floor(Math.random() * 20) // Random placeholder for UI polish
      };
    }).sort((a, b) => b.amount - a.amount);
  }

  renderCharts() {
    // Clear and redraw
    const trendEl = document.querySelector("#trendBarChart");
    const donutEl = document.querySelector("#donutChart");
    if (trendEl) trendEl.innerHTML = "";
    if (donutEl) donutEl.innerHTML = "";

    this.renderTrendChart();
    this.renderDonutChart();
  }

  renderTrendChart() {
    const months = Object.keys(this.monthlyTrend);
    const expenseSeries = months.map(m => this.monthlyTrend[m]['EXPENSE'] || 0);

    const options = {
      series: [{ name: 'Spending', data: expenseSeries }],
      chart: { type: 'bar', height: 200, toolbar: { show: false } },
      plotOptions: {
        bar: {
          borderRadius: 4,
          columnWidth: '40%',
          colors: { backgroundBarColors: ['#f8f9fa'], backgroundBarOpacity: 1 }
        }
      },
      colors: ['#3b82f6'], 
      xaxis: { categories: months, axisBorder: { show: false } },
      yaxis: { show: false },
      grid: { show: false },
      dataLabels: { enabled: false },
      tooltip: { y: { formatter: (v: number) => '$' + v.toLocaleString() } }
    };

    const ApexCharts = (window as any).ApexCharts;
    if (ApexCharts) {
        new ApexCharts(document.querySelector("#trendBarChart"), options).render();
    }
  }

  renderDonutChart() {
    if (this.categoryDonutData.series.length === 0) {
      const donutEl = document.querySelector("#donutChart");
      if (donutEl) donutEl.innerHTML = "<div class='text-muted text-center py-5'>No data for this range.</div>";
      return;
    }

    const options = {
      series: this.categoryDonutData.series,
      labels: this.categoryDonutData.labels,
      chart: { type: 'donut', height: 350 },
      colors: ['#3b82f6', '#10b981', '#f59e0b', '#ec4899', '#8b5cf6', '#06b6d4'],
      dataLabels: { enabled: false },
      legend: { show: false },
      stroke: { width: 0 },
      plotOptions: {
        pie: {
          donut: {
            size: '75%',
            labels: {
              show: true,
              total: {
                show: true,
                label: 'TOTAL SPEND',
                color: '#64748b',
                formatter: (w: any) => {
                  const val = w.globals.seriesTotals.reduce((a: any, b: any) => a + b, 0);
                  return '$' + val.toLocaleString(undefined, {minimumFractionDigits: 2});
                }
              }
            }
          }
        }
      }
    };

    const ApexCharts = (window as any).ApexCharts;
    if (ApexCharts) {
        new ApexCharts(document.querySelector("#donutChart"), options).render();
    }
  }

  getIcon(cat: string): string {
    const c = cat.toLowerCase();
    if (c.includes('medical')) return '🩺';
    if (c.includes('dining') || c.includes('drink') || c.includes('food')) return '🍽️';
    if (c.includes('bill') || c.includes('utility')) return '📄';
    if (c.includes('auto') || c.includes('transport') || c.includes('car')) return '🚗';
    if (c.includes('shopping')) return '🛍️';
    if (c.includes('grocer')) return '🛒';
    if (c.includes('loan')) return (c.includes('auto') || c.includes('car')) ? '🚗' : '🏦';
    if (c.includes('tax')) return '🏛️';
    if (c.includes('home') || c.includes('garden')) return '🏠';
    if (c.includes('software') || c.includes('tech')) return '💻';
    if (c.includes('entertainment') || c.includes('rec')) return '🎬';
    if (c.includes('credit card')) return '💳';
    if (c.includes('personal care')) return '🧴';
    if (c.includes('legal')) return '⚖️';
    if (c.includes('travel') || c.includes('vacation')) return '✈️';
    if (c.includes('fee')) return '💸';
    if (c.includes('party')) return '🍹';
    return '📦';
  }
}
