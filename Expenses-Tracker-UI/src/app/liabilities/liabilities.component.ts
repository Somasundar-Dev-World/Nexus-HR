import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { FinanceService, Liability } from '../finance.service';

@Component({
  selector: 'app-liabilities',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './liabilities.component.html',
  styleUrl: './liabilities.component.css'
})
export class LiabilitiesComponent implements OnInit {
  liabilities: Liability[] = [];
  isLoading = true;
  isSaving = false;
  showAddForm = false;
  editingId: number | null = null;
  
  newLiability: Liability = this.getDefaultLiability();

  constructor(private financeService: FinanceService) {}

  ngOnInit() {
    this.loadLiabilities();
  }

  getDefaultLiability(): Liability {
    return { name: '', amount: 0, institutionName: '', accountNumber: '', note: '' };
  }

  loadLiabilities() {
    this.isLoading = true;
    this.financeService.getLiabilities().subscribe({
      next: (data) => {
        this.liabilities = data;
        this.isLoading = false;
      },
      error: (err) => {
        console.error(err);
        this.isLoading = false;
      }
    });
  }

  saveLiability() {
    this.isSaving = true;
    if (this.editingId) {
      this.financeService.updateLiability(this.editingId, this.newLiability).subscribe({
        next: () => { this.isSaving = false; this.resetForm(); this.loadLiabilities(); },
        error: (err) => { console.error(err); this.isSaving = false; }
      });
    } else {
      this.financeService.addLiability(this.newLiability).subscribe({
        next: () => { this.isSaving = false; this.resetForm(); this.loadLiabilities(); },
        error: (err) => { console.error(err); this.isSaving = false; }
      });
    }
  }

  edit(l: Liability) {
    this.editingId = l.id!;
    this.newLiability = { ...l };
    this.showAddForm = true;
  }

  resetForm() {
    this.showAddForm = false;
    this.editingId = null;
    this.newLiability = this.getDefaultLiability();
  }

  toggleAddForm() {
    this.resetForm();
    this.showAddForm = true;
  }

  delete(id: number | undefined) {
    if (!id) return;
    this.financeService.deleteLiability(id).subscribe(() => this.loadLiabilities());
  }
}
