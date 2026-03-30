import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { FinanceService, Asset } from '../finance.service';

@Component({
  selector: 'app-assets',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './assets.component.html',
  styleUrl: './assets.component.css'
})
export class AssetsComponent implements OnInit {
  assets: Asset[] = [];
  isLoading = true;
  isSaving = false;
  showAddForm = false;
  editingId: number | null = null;
  
  newAsset: Asset = { name: '', currentValue: 0 };

  constructor(private financeService: FinanceService) {}

  ngOnInit() {
    this.loadAssets();
  }

  loadAssets() {
    this.isLoading = true;
    this.financeService.getAssets().subscribe({
      next: (data) => {
        this.assets = data;
        this.isLoading = false;
      },
      error: (err) => {
        console.error(err);
        this.isLoading = false;
      }
    });
  }

  saveAsset() {
    this.isSaving = true;
    if (this.editingId) {
      this.financeService.updateAsset(this.editingId, this.newAsset).subscribe({
        next: () => { this.isSaving = false; this.resetForm(); this.loadAssets(); },
        error: (err) => { console.error(err); this.isSaving = false; }
      });
    } else {
      this.financeService.addAsset(this.newAsset).subscribe({
        next: () => { this.isSaving = false; this.resetForm(); this.loadAssets(); },
        error: (err) => { console.error(err); this.isSaving = false; }
      });
    }
  }

  edit(a: Asset) {
    this.editingId = a.id!;
    this.newAsset = { ...a };
    this.showAddForm = true;
  }

  resetForm() {
    this.showAddForm = false;
    this.editingId = null;
    this.newAsset = { name: '', currentValue: 0 };
  }

  toggleAddForm() {
    this.resetForm();
    this.showAddForm = true;
  }

  delete(id: number | undefined) {
    if (!id) return;
    this.financeService.deleteAsset(id).subscribe(() => this.loadAssets());
  }
}
