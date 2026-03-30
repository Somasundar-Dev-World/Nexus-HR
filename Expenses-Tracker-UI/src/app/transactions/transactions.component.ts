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
  isSaving = false;
  showAddForm = false;
  editingId: number | null = null;
  
  // CSV Preview
  csvPreview: Transaction[] = [];
  showPreview = false;
  uploadStatus: string | null = null;

  defaultCategories = ['Food & Dining', 'Transportation', 'Utilities', 'Entertainment', 'Health & Fitness', 'Shopping', 'Other'];
  customCategories: string[] = [];
  suggestedCategories: string[] = [...this.defaultCategories];

  newTransaction: Transaction = this.getDefaultTransaction();

  constructor(private financeService: FinanceService, private router: Router) {}

  ngOnInit() {
    this.loadTransactions();
  }

  getDefaultTransaction(): Transaction {
    return {
      description: '',
      amount: 0,
      date: new Date().toISOString().split('T')[0],
      type: 'EXPENSE',
      category: '',
      name: '',
      accountName: '',
      institutionName: '',
      note: ''
    };
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
    
    this.suggestedCategories = Array.from(new Set([...this.defaultCategories, ...this.customCategories, ...historical]));
  }

  onCategoryChange(val: string) {
    if (val === '__CUSTOM__') {
      this.router.navigate(['/categories']);
    }
  }

  saveTransaction() {
    this.isSaving = true;
    if (this.editingId) {
      this.financeService.updateTransaction(this.editingId, this.newTransaction).subscribe({
        next: () => { this.isSaving = false; this.resetForm(); this.loadTransactions(); },
        error: (err) => { console.error(err); this.isSaving = false; }
      });
    } else {
      this.financeService.addTransaction(this.newTransaction).subscribe({
        next: () => { this.isSaving = false; this.resetForm(); this.loadTransactions(); },
        error: (err) => { console.error(err); this.isSaving = false; }
      });
    }
  }

  onFileSelected(event: any) {
    const file: File = event.target.files[0];
    if (file) {
      const papa = (window as any).Papa;
      if (!papa) {
          alert('CSV Parser not loaded. Please refresh.');
          return;
      }

      papa.parse(file, {
        header: true,
        skipEmptyLines: true,
        complete: (results: any) => {
          this.processCsvData(results.data);
        }
      });
    }
  }

  processCsvData(data: any[]) {
    const parsed: Transaction[] = data.map(row => {
      const amount = parseFloat(row.Amount) || 0;
      return {
        date: row.Date || new Date().toISOString().split('T')[0],
        originalDate: row['Original Date'],
        accountType: row['Account Type'],
        accountName: row['Account Name'],
        accountNumber: row['Account Number'],
        institutionName: row['Institution Name'],
        name: row.Name,
        customName: row['Custom Name'],
        amount: Math.abs(amount),
        description: row.Description || row.Name || 'Imported Transaction',
        type: amount < 0 ? 'INCOME' : 'EXPENSE',
        category: row.Category,
        note: row.Note,
        ignoredFrom: row['Ignored From'],
        taxDeductible: row['Tax Deductible']?.toLowerCase() === 'true',
        transactionTags: row['Transaction Tags']
      };
    });

    this.csvPreview = parsed;
    this.showPreview = true;
    this.uploadStatus = null;
  }

  uploadBulk() {
    this.isSaving = true;
    const totalToUpload = this.csvPreview.length;
    this.financeService.bulkAddTransactions(this.csvPreview).subscribe({
      next: (saved) => {
        this.isSaving = false;
        this.showPreview = false;
        this.csvPreview = [];
        this.loadTransactions();
        this.uploadStatus = `Successfully processed ${totalToUpload} transactions (duplicates were merged).`;
        setTimeout(() => this.uploadStatus = null, 5000);
      },
      error: (err) => {
        console.error(err);
        this.isSaving = false;
        this.uploadStatus = "Error uploading transactions. Please check your file.";
      }
    });
  }

  edit(t: Transaction) {
    this.editingId = t.id!;
    this.newTransaction = { ...t };
    if (this.newTransaction.date) {
        this.newTransaction.date = new Date(this.newTransaction.date).toISOString().split('T')[0];
    }
    this.showAddForm = true;
  }

  resetForm() {
    this.showAddForm = false;
    this.editingId = null;
    this.newTransaction = this.getDefaultTransaction();
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
