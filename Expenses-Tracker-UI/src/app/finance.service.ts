import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../environments/environment';

export interface Transaction { 
  id?: number; 
  date: string;
  originalDate?: string;
  accountType?: string;
  accountName?: string;
  accountNumber?: string;
  institutionName?: string;
  name?: string;
  customName?: string;
  amount: number; 
  description: string; 
  type: 'INCOME' | 'EXPENSE'; 
  category?: string; 
  note?: string;
  ignoredFrom?: string;
  taxDeductible?: boolean;
  transactionTags?: string;
}

export interface Category { id?: number; name: string; }

export interface Asset { 
  id?: number; 
  name: string; 
  currentValue: number; 
  institutionName?: string;
  accountNumber?: string;
  note?: string;
}

export interface Liability { 
  id?: number; 
  name: string; 
  amount: number; 
  institutionName?: string;
  accountNumber?: string;
  note?: string;
}

export interface FinancialSummary { 
  totalIncome: number; 
  totalExpense: number; 
  totalAssets: number; 
  totalLiabilities: number; 
  netWorth: number; 
  predictedNetWorthNextMonth: number; 
}

@Injectable({ providedIn: 'root' })
export class FinanceService {
  private apiUrl = environment.apiUrl;

  constructor(private http: HttpClient) { }

  getSummary(): Observable<FinancialSummary> { return this.http.get<FinancialSummary>(`${this.apiUrl}/reports/summary`); }
  getCategorySpending(): Observable<any> { return this.http.get<any>(`${this.apiUrl}/reports/category-spending`); }
  getMonthlyTrend(): Observable<any> { return this.http.get<any>(`${this.apiUrl}/reports/monthly-trend`); }
  getFrequentMerchants(): Observable<any[]> { return this.http.get<any[]>(`${this.apiUrl}/reports/frequent-merchants`); }
  getLargestPurchases(): Observable<Transaction[]> { return this.http.get<Transaction[]>(`${this.apiUrl}/reports/largest-purchases`); }
  getSpendingComparison(): Observable<any> { return this.http.get<any>(`${this.apiUrl}/reports/spending-comparison`); }
  
  getTransactions(): Observable<Transaction[]> { return this.http.get<Transaction[]>(`${this.apiUrl}/transactions`); }
  addTransaction(t: Transaction): Observable<Transaction> { return this.http.post<Transaction>(`${this.apiUrl}/transactions`, t); }
  bulkAddTransactions(ts: Transaction[]): Observable<Transaction[]> { return this.http.post<Transaction[]>(`${this.apiUrl}/transactions/bulk`, ts); }
  updateTransaction(id: number, t: Transaction): Observable<Transaction> { return this.http.put<Transaction>(`${this.apiUrl}/transactions/${id}`, t); }
  deleteTransaction(id: number): Observable<any> { return this.http.delete(`${this.apiUrl}/transactions/${id}`); }
  
  getCategories(): Observable<Category[]> { return this.http.get<Category[]>(`${this.apiUrl}/categories`); }
  addCategory(c: Category): Observable<Category> { return this.http.post<Category>(`${this.apiUrl}/categories`, c); }
  updateCategory(id: number, c: Category): Observable<Category> { return this.http.put<Category>(`${this.apiUrl}/categories/${id}`, c); }
  deleteCategory(id: number): Observable<any> { return this.http.delete(`${this.apiUrl}/categories/${id}`); }
  
  getAssets(): Observable<Asset[]> { return this.http.get<Asset[]>(`${this.apiUrl}/assets`); }
  addAsset(a: Asset): Observable<Asset> { return this.http.post<Asset>(`${this.apiUrl}/assets`, a); }
  updateAsset(id: number, a: Asset): Observable<Asset> { return this.http.put<Asset>(`${this.apiUrl}/assets/${id}`, a); }
  deleteAsset(id: number): Observable<any> { return this.http.delete(`${this.apiUrl}/assets/${id}`); }
  
  getLiabilities(): Observable<Liability[]> { return this.http.get<Liability[]>(`${this.apiUrl}/liabilities`); }
  addLiability(l: Liability): Observable<Liability> { return this.http.post<Liability>(`${this.apiUrl}/liabilities`, l); }
  updateLiability(id: number, l: Liability): Observable<Liability> { return this.http.put<Liability>(`${this.apiUrl}/liabilities/${id}`, l); }
  deleteLiability(id: number): Observable<any> { return this.http.delete(`${this.apiUrl}/liabilities/${id}`); }
}
