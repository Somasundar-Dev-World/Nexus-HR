import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule, ReactiveFormsModule, FormBuilder, FormGroup, FormArray, Validators } from '@angular/forms';
import { OmniTrackerService, Tracker, TrackerEntry } from '../omni-tracker.service';
import { FinanceService } from '../finance.service';

@Component({
  selector: 'app-omni-dashboard',
  standalone: true,
  imports: [CommonModule, FormsModule, ReactiveFormsModule],
  templateUrl: './omni-dashboard.component.html',
  styleUrl: './omni-dashboard.component.css'
})
export class OmniDashboardComponent implements OnInit {
  trackers: Tracker[] = [];
  selectedTracker?: Tracker;
  entries: TrackerEntry[] = [];
  categories: string[] = [];

  viewMode: 'GRID' | 'DETAIL' = 'GRID';

  // Reactive forms (used for non-FINANCE trackers)
  trackerForm: FormGroup;
  entryForm: FormGroup;
  newEntryNote = '';

  // Flat transaction-style model for FINANCE trackers
  financeEntry: any = {};

  isLoading = true;
  isAddingTracker = false;
  isAddingEntry = false;
  editingEntryId: number | null = null;
  deleteConfirm: { type: 'TRACKER' | 'ENTRY', id: number } | null = null;

  readonly FINANCE_CATEGORIES = [
    'Food & Dining', 'Groceries', 'Transportation', 'Rent', 'Utilities',
    'Entertainment', 'Healthcare', 'Insurance', 'Shopping', 'Travel',
    'Education', 'Subscriptions', 'Savings', 'Investment', 'Salary', 'Freelance', 'Other'
  ];

  constructor(
    private omniService: OmniTrackerService,
    private financeService: FinanceService,
    private fb: FormBuilder
  ) {
    this.trackerForm = this.fb.group({
      name: ['', Validators.required],
      type: ['FINANCE'],
      fieldDefinitions: this.fb.array([])
    });
    this.entryForm = this.fb.group({ fieldValues: this.fb.group({}) });
  }

  ngOnInit() {
    this.loadData();
    this.loadCategories();
  }

  loadData() {
    this.isLoading = true;
    this.omniService.getTrackers().subscribe({
      next: (data) => { this.trackers = data; this.isLoading = false; },
      error: () => this.isLoading = false
    });
  }

  loadCategories() {
    this.financeService.getCategories().subscribe({
      next: (cats: any[]) => {
        const names = cats.map((c: any) => c.name || c);
        this.categories = [...new Set([...this.FINANCE_CATEGORIES, ...names])];
      },
      error: () => { this.categories = [...this.FINANCE_CATEGORIES]; }
    });
  }

  // ── Navigation ──────────────────────────────────────────────
  goToDetail(tracker: Tracker) {
    this.selectedTracker = tracker;
    this.entries = [];
    this.omniService.getEntries(tracker.id!).subscribe(data => this.entries = data);
    this.viewMode = 'DETAIL';
  }

  goBack() {
    this.viewMode = 'GRID';
    this.selectedTracker = undefined;
    this.entries = [];
  }

  isFinanceTracker(): boolean {
    return this.selectedTracker?.type === 'FINANCE';
  }

  // ── Create Tracker ───────────────────────────────────────────
  get fieldDefinitions() { return this.trackerForm.get('fieldDefinitions') as FormArray; }

  addField() {
    this.fieldDefinitions.push(this.fb.group({
      name: ['', Validators.required],
      type: ['NUMBER', Validators.required],
      unit: ['']
    }));
  }

  removeField(index: number) { this.fieldDefinitions.removeAt(index); }

  openAddTracker() {
    this.trackerForm.reset({ type: 'FINANCE' });
    this.fieldDefinitions.clear();
    this.addField();
    this.isAddingTracker = true;
  }

  createTracker() {
    if (this.trackerForm.invalid) return;
    const val = this.trackerForm.value;
    // For FINANCE trackers, no field definitions needed — they use the fixed transaction form
    if (val.type === 'FINANCE') val.fieldDefinitions = [];
    this.omniService.createTracker(val).subscribe(tracker => {
      this.trackers.push(tracker);
      this.isAddingTracker = false;
    });
  }

  // ── Log Entries ──────────────────────────────────────────────
  resetFinanceEntry() {
    this.financeEntry = {
      type: 'EXPENSE',
      amount: null,
      date: new Date().toISOString().split('T')[0],
      category: '',
      description: '',
      institutionName: '',
      accountName: '',
      accountNumber: '',
      transactionTags: '',
      taxDeductible: false,
      note: ''
    };
  }

  openAddEntry() {
    if (!this.selectedTracker) return;
    this.editingEntryId = null;

    if (this.isFinanceTracker()) {
      this.resetFinanceEntry();
    } else {
      const fg = this.fb.group({});
      this.selectedTracker.fieldDefinitions?.forEach(f =>
        fg.addControl(f.name, this.fb.control('', Validators.required))
      );
      this.entryForm.setControl('fieldValues', fg);
      this.newEntryNote = '';
    }
    this.isAddingEntry = true;
  }

  editLog(entry: TrackerEntry) {
    if (!this.selectedTracker) return;
    this.editingEntryId = entry.id!;

    if (this.isFinanceTracker()) {
      this.financeEntry = { ...entry.fieldValues, note: entry.note || '' };
      // Ensure date field is present
      if (!this.financeEntry.date) this.financeEntry.date = entry.date?.split('T')[0] || '';
    } else {
      const fg = this.fb.group({});
      this.selectedTracker.fieldDefinitions?.forEach(f => {
        fg.addControl(f.name, this.fb.control(entry.fieldValues?.[f.name] || '', Validators.required));
      });
      this.entryForm.setControl('fieldValues', fg);
      this.newEntryNote = entry.note || '';
    }
    this.isAddingEntry = true;
  }

  addEntry() {
    if (!this.selectedTracker) return;

    let entryData: TrackerEntry;

    if (this.isFinanceTracker()) {
      const { note, ...financeFields } = this.financeEntry;
      entryData = {
        trackerId: this.selectedTracker.id!,
        fieldValues: financeFields,
        note: note || '',
        date: financeFields.date ? new Date(financeFields.date).toISOString() : new Date().toISOString()
      };
    } else {
      if (this.entryForm.invalid) return;
      entryData = {
        trackerId: this.selectedTracker.id!,
        fieldValues: this.entryForm.get('fieldValues')?.value,
        note: this.newEntryNote,
        date: new Date().toISOString()
      };
    }

    if (this.editingEntryId) {
      this.omniService.updateEntry(this.editingEntryId, entryData).subscribe(saved => {
        const i = this.entries.findIndex(e => e.id === saved.id);
        if (i !== -1) this.entries[i] = saved;
        this.isAddingEntry = false;
        this.editingEntryId = null;
      });
    } else {
      this.omniService.addEntry(entryData).subscribe(saved => {
        this.entries.unshift(saved);
        this.isAddingEntry = false;
      });
    }
  }

  isFinanceEntryValid(): boolean {
    return !!(this.financeEntry.type && this.financeEntry.amount && this.financeEntry.date && this.financeEntry.description);
  }

  // ── Delete ───────────────────────────────────────────────────
  deleteLog(id: number) { this.deleteConfirm = { type: 'ENTRY', id }; }

  deleteTrackerIcon(id: number, event: Event) {
    event.stopPropagation();
    this.deleteConfirm = { type: 'TRACKER', id };
  }

  cancelDelete() { this.deleteConfirm = null; }

  confirmDelete() {
    if (!this.deleteConfirm) return;
    if (this.deleteConfirm.type === 'TRACKER') {
      this.omniService.deleteTracker(this.deleteConfirm.id).subscribe(() => {
        this.trackers = this.trackers.filter(t => t.id !== this.deleteConfirm?.id);
        this.deleteConfirm = null;
        if (this.viewMode === 'DETAIL') this.goBack();
      });
    } else {
      this.omniService.deleteEntry(this.deleteConfirm.id).subscribe(() => {
        this.entries = this.entries.filter(e => e.id !== this.deleteConfirm?.id);
        this.deleteConfirm = null;
      });
    }
  }

  // ── Helpers ──────────────────────────────────────────────────
  getIcon(type: string): string {
    switch(type) {
      case 'FINANCE': return '🏦';
      case 'HEALTH': return '🍎';
      case 'STOCK': return '📈';
      default: return '⚙️';
    }
  }

  getGradient(type: string): string {
    switch(type) {
      case 'FINANCE': return 'linear-gradient(135deg, #667eea 0%, #764ba2 100%)';
      case 'HEALTH': return 'linear-gradient(135deg, #11998e 0%, #38ef7d 100%)';
      case 'STOCK': return 'linear-gradient(135deg, #f7971e 0%, #ffd200 100%)';
      default: return 'linear-gradient(135deg, #fc4a1a 0%, #f7b733 100%)';
    }
  }

  getFieldKeys(entry: TrackerEntry): string[] {
    if (!entry.fieldValues) return [];
    const excl = ['note', 'taxDeductible'];
    return Object.keys(entry.fieldValues).filter(k => !excl.includes(k) && entry.fieldValues[k]);
  }

  getLatestValue(): string {
    if (!this.entries.length) return '—';
    if (this.isFinanceTracker()) {
      const fv = this.entries[0]?.fieldValues;
      if (fv?.amount) return `$${parseFloat(fv.amount).toFixed(2)}`;
      return '—';
    }
    if (!this.selectedTracker?.fieldDefinitions?.length) return '—';
    const key = this.selectedTracker.fieldDefinitions[0].name;
    const val = this.entries[0]?.fieldValues?.[key];
    const unit = this.selectedTracker.fieldDefinitions[0].unit || '';
    return val ? `${val} ${unit}`.trim() : '—';
  }

  formatFieldValue(key: string, val: any): string {
    if (key === 'amount') return `$${parseFloat(val).toFixed(2)}`;
    if (key === 'type') return val === 'INCOME' ? '📈 Income' : '📉 Expense';
    return String(val);
  }
}
