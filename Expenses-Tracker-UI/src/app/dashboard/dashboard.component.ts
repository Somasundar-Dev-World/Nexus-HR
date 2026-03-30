import { Component, OnInit } from '@angular/core';
import { FinanceService, FinancialSummary } from '../finance.service';
import { CommonModule } from '@angular/common';

@Component({
  selector: 'app-dashboard',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './dashboard.component.html',
  styleUrl: './dashboard.component.css'
})
export class DashboardComponent implements OnInit {
  summary: FinancialSummary | null = null;
  isLoading = true;

  constructor(private financeService: FinanceService) {}

  ngOnInit() {
    this.financeService.getSummary().subscribe({
      next: (data) => {
        this.summary = data;
        this.isLoading = false;
      },
      error: (err) => {
        console.error(err);
        this.isLoading = false;
      }
    });
  }
}
