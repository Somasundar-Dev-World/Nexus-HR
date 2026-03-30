import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { FinanceService, Category } from '../finance.service';

@Component({
  selector: 'app-categories',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink],
  templateUrl: './categories.component.html',
  styleUrl: '../transactions/transactions.component.css' // Reuse similar layout
})
export class CategoriesComponent implements OnInit {
  categories: Category[] = [];
  isLoading = true;
  showAddForm = false;
  editingId: number | null = null;
  
  newCategory: Category = { name: '' };

  constructor(private financeService: FinanceService) {}

  ngOnInit() {
    this.loadCategories();
  }

  loadCategories() {
    this.isLoading = true;
    this.financeService.getCategories().subscribe({
      next: (data) => {
        this.categories = data;
        this.isLoading = false;
      },
      error: (err) => {
        console.error(err);
        this.isLoading = false;
      }
    });
  }

  saveCategory() {
    if (this.editingId) {
      this.financeService.updateCategory(this.editingId, this.newCategory).subscribe({
        next: () => {
          this.resetForm();
          this.loadCategories();
        },
        error: (err) => console.error(err)
      });
    } else {
      this.financeService.addCategory(this.newCategory).subscribe({
        next: () => {
          this.resetForm();
          this.loadCategories();
        },
        error: (err) => console.error(err)
      });
    }
  }

  edit(c: Category) {
    this.editingId = c.id!;
    this.newCategory = { ...c };
    this.showAddForm = true;
  }

  resetForm() {
    this.showAddForm = false;
    this.editingId = null;
    this.newCategory = { name: '' };
  }

  toggleAddForm() {
    this.resetForm();
    this.showAddForm = true;
  }

  delete(id: number | undefined) {
    if (!id) return;
    this.financeService.deleteCategory(id).subscribe(() => this.loadCategories());
  }
}
