import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { FinanceService, Transaction } from '../finance.service';

@Component({
  selector: 'app-transactions',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './transactions.component.html',
  styleUrl: './transactions.component.css'
})
export class TransactionsComponent implements OnInit {
  transactions: Transaction[] = [];
  isLoading = true;
  showAddForm = false;
  editingId: number | null = null;
  
  defaultCategories = ['Food & Dining', 'Transportation', 'Utilities', 'Entertainment', 'Health & Fitness', 'Shopping', 'Other'];
  customCategories: string[] = [];
  suggestedCategories: string[] = [...this.defaultCategories];

  newTransaction: Transaction = {
    description: '',
    amount: 0,
    date: new Date().toISOString().split('T')[0],
    type: 'EXPENSE',
    category: ''
  };

  constructor(private financeService: FinanceService, private router: Router) {}

  ngOnInit() {
    this.loadTransactions();
  }

  loadTransactions() {
    this.isLoading = true;
    this.financeService.getTransactions().subscribe({
      next: (data) => {
        this.transactions = data.sort((a,b) => new Date(b.date).getTime() - new Date(a.date).getTime());
        this.isLoading = false;
        this.updateSuggestedCategories();
      },
      error: (err) => {
        console.error(err);
        this.isLoading = false;
      }
    });

    this.financeService.getCategories().subscribe({
        next: (cats) => {
            this.customCategories = cats.map(c => c.name);
            this.updateSuggestedCategories();
        }
    });
  }

  updateSuggestedCategories() {
    const historical = this.transactions
      .filter(t => t.type === 'EXPENSE' && t.category)
      .map(t => t.category!);
    
    // Combine defaults, explicitly added custom categories, and historical ones
    this.suggestedCategories = Array.from(new Set([...this.defaultCategories, ...this.customCategories, ...historical]));
  }

  onCategoryChange(val: string) {
    if (val === '__CUSTOM__') {
      this.router.navigate(['/categories']);
    }
  }

  saveTransaction() {
    if (this.editingId) {
      this.financeService.updateTransaction(this.editingId, this.newTransaction).subscribe({
        next: () => {
          this.resetForm();
          this.loadTransactions();
        },
        error: (err) => console.error(err)
      });
    } else {
      this.financeService.addTransaction(this.newTransaction).subscribe({
        next: () => {
          this.resetForm();
          this.loadTransactions();
        },
        error: (err) => console.error(err)
      });
    }
  }

  edit(t: Transaction) {
    this.editingId = t.id!;
    this.newTransaction = { ...t };
    // ensure date is YYYY-MM-DD
    if (this.newTransaction.date) {
        this.newTransaction.date = new Date(this.newTransaction.date).toISOString().split('T')[0];
    }
    this.showAddForm = true;
  }

  resetForm() {
    this.showAddForm = false;
    this.editingId = null;
    this.newTransaction = { description: '', amount: 0, date: new Date().toISOString().split('T')[0], type: 'EXPENSE', category: '' };
  }

  toggleAddForm() {
    this.resetForm();
    this.showAddForm = true;
  }

  delete(id: number | undefined) {
    if (!id) return;
    this.financeService.deleteTransaction(id).subscribe(() => this.loadTransactions());
  }
}
