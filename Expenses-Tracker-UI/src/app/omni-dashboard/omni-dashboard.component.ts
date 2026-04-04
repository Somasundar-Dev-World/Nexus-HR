import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule, ReactiveFormsModule, FormBuilder, FormGroup, FormArray, Validators } from '@angular/forms';
import { OmniTrackerService, Tracker, TrackerEntry } from '../omni-tracker.service';
import { OmniApp } from '../omni-app.model';

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
  apps: OmniApp[] = [];
  selectedApp?: OmniApp;
  entries: TrackerEntry[] = [];

  // View state — 'APP_GRID' is apps, 'TRACKER_GRID' is trackers inside app, 'DETAIL' is logs
  viewMode: 'APP_GRID' | 'TRACKER_GRID' | 'DETAIL' = 'APP_GRID';

  // Reactive forms
  appForm: FormGroup;
  trackerForm: FormGroup;
  entryForm: FormGroup;
  newEntryNote = '';

  isLoading = true;
  isAddingApp = false;
  isAddingTracker = false;
  isAddingEntry = false;
  editingEntryId: number | null = null;
  deleteConfirm: { type: 'APP' | 'TRACKER' | 'ENTRY', id: number } | null = null;

  readonly TRACKER_TEMPLATES = [
    {
      name: 'Biohacker Recovery',
      type: 'HEALTH',
      icon: '🧬',
      fields: [
        { name: 'Sleep Quality', type: 'RATING' },
        { name: 'Resting HR', type: 'NUMBER', unit: 'BPM' },
        { name: 'Wake Time', type: 'TIME' },
        { name: 'Bedtime', type: 'TIME' },
        { name: 'Morning Feel', type: 'SELECT', options: 'Refreshed, Neutral, Tired, Sick' }
      ]
    },
    {
      name: 'Deep Work Diary',
      type: 'CUSTOM',
      icon: '🧠',
      fields: [
        { name: 'Focus Level', type: 'RATING' },
        { name: 'Deep Work Mins', type: 'NUMBER', unit: 'min' },
        { name: 'Start Time', type: 'TIME' },
        { name: 'Session Goal', type: 'LONG_TEXT' }
      ]
    },
    {
      name: 'Mindset & Mood',
      type: 'HEALTH',
      icon: '✨',
      fields: [
        { name: 'Happiness', type: 'RATING' },
        { name: 'Meditation', type: 'BOOLEAN' },
        { name: 'Dominant Emotion', type: 'SELECT', options: 'Calm, Joyful, Anxious, Angry, Sad' },
        { name: 'Journal', type: 'LONG_TEXT' }
      ]
    },
    {
      name: 'Financial Discipline',
      type: 'FINANCE',
      icon: '💰',
      fields: [
        { name: 'Amount Saved', type: 'CURRENCY' },
        { name: 'Spend Type', type: 'SELECT', options: 'Need, Want' },
        { name: 'No Spend Day', type: 'BOOLEAN' },
        { name: 'Temptation Note', type: 'TEXT' }
      ]
    },
    {
      name: 'Pet Health Log',
      type: 'CUSTOM',
      icon: '🐕',
      fields: [
        { name: 'Walk Mins', type: 'NUMBER', unit: 'min' },
        { name: 'Meal Given', type: 'BOOLEAN' },
        { name: 'Energy Level', type: 'RATING' },
        { name: 'Incident Notes', type: 'LONG_TEXT' }
      ]
    },
    {
      name: 'Daily Expense Log',
      type: 'FINANCE',
      icon: '🧾',
      fields: [
        { name: 'Amount', type: 'CURRENCY' },
        { name: 'Category', type: 'SELECT', options: 'Food, Transport, Bills, Shopping, Entertainment, Health, Other' },
        { name: 'Payment Mode', type: 'SELECT', options: 'Cash, Credit Card, Debit Card, UPI' },
        { name: 'Note', type: 'TEXT' }
      ]
    },
    {
      name: 'Wealth Portfolio',
      type: 'FINANCE',
      icon: '💹',
      fields: [
        { name: 'Ticker Symbol', type: 'TEXT' },
        { name: 'Asset Class', type: 'SELECT', options: 'Stocks, Crypto, Mutual Funds, Real Estate, Gold, Cash' },
        { name: 'Buy Price', type: 'CURRENCY' },
        { name: 'Quantity', type: 'NUMBER' },
        { name: 'Conviction', type: 'RATING' },
        { name: 'Investment Thesis', type: 'LONG_TEXT' }
      ]
    }
  ];

  selectedTemplateIndex: number | null = null;

  constructor(private omniService: OmniTrackerService, private fb: FormBuilder) {
    this.appForm = this.fb.group({
      name: ['', Validators.required],
      icon: ['🚀', Validators.required],
      description: ['']
    });
    this.trackerForm = this.fb.group({
      name: ['', Validators.required],
      type: ['FINANCE'],
      fieldDefinitions: this.fb.array([])
    });
    this.entryForm = this.fb.group({ fieldValues: this.fb.group({}) });
  }

  ngOnInit() { this.loadData(); }

  loadData() {
    this.isLoading = true;
    this.omniService.getApps().subscribe({
      next: (appsData) => { 
        this.apps = appsData; 
        
        // --- Migration Logic ---
        // If we have no apps, check if there are legacy "app-less" trackers
        if (this.apps.length === 0) {
          this.omniService.getTrackers().subscribe(legacyTrackers => {
            if (legacyTrackers.length > 0) {
              // Create a default app for them
              this.omniService.createApp({ name: 'Omni Hub', icon: '🏠', description: 'Your original trackers' })
                .subscribe(newApp => {
                  this.apps.push(newApp);
                  this.isLoading = false;
                });
            } else {
              this.isLoading = false;
            }
          });
        } else {
          this.isLoading = false;
        }
      },
      error: () => this.isLoading = false
    });
  }

  // ── Navigation ──────────────────────────────────────────────
  selectApp(app: OmniApp) {
    this.selectedApp = app;
    this.isLoading = true;
    this.omniService.getTrackers(app.id).subscribe(data => {
      this.trackers = data;
      this.viewMode = 'TRACKER_GRID';
      this.isLoading = false;
    });
  }

  goToDetail(tracker: Tracker) {
    this.selectedTracker = tracker;
    this.entries = [];
    this.omniService.getEntries(tracker.id!).subscribe(data => this.entries = data);
    this.viewMode = 'DETAIL';
  }

  goBack() {
    if (this.viewMode === 'DETAIL') {
      this.viewMode = 'TRACKER_GRID';
      this.selectedTracker = undefined;
      this.entries = [];
    } else if (this.viewMode === 'TRACKER_GRID') {
      this.viewMode = 'APP_GRID';
      this.selectedApp = undefined;
      this.trackers = [];
    }
  }

  // ── Create App ───────────────────────────────────────────────
  openAddApp() {
    this.appForm.reset({ icon: '🚀' });
    this.isAddingApp = true;
  }

  createApp() {
    if (this.appForm.invalid) return;
    this.omniService.createApp(this.appForm.value).subscribe(app => {
      this.apps.push(app);
      this.isAddingApp = false;
    });
  }

  // ── Create Tracker ───────────────────────────────────────────
  get fieldDefinitions() { return this.trackerForm.get('fieldDefinitions') as FormArray; }

  addField(initialValues: any = {}) {
    this.fieldDefinitions.push(this.fb.group({
      name: [initialValues.name || '', Validators.required],
      type: [initialValues.type || 'NUMBER', Validators.required],
      unit: [initialValues.unit || ''],
      options: [initialValues.options || ''] // Comma-separated for SELECT type
    }));
  }

  removeField(index: number) { this.fieldDefinitions.removeAt(index); }

  applyTemplate(index: number) {
    this.selectedTemplateIndex = index;
    const tpl = this.TRACKER_TEMPLATES[index];
    
    this.trackerForm.patchValue({
      name: tpl.name,
      type: tpl.type
    });

    this.fieldDefinitions.clear();
    tpl.fields.forEach(f => this.addField(f));
  }

  openAddTracker() {
    this.trackerForm.reset({ type: 'FINANCE' });
    this.fieldDefinitions.clear();
    this.selectedTemplateIndex = null;
    this.addField();
    this.isAddingTracker = true;
  }

  createTracker() {
    if (this.trackerForm.invalid || !this.selectedApp) return;
    const trackerData = { ...this.trackerForm.value, appId: this.selectedApp.id };
    this.omniService.createTracker(trackerData).subscribe(tracker => {
      this.trackers.push(tracker);
      this.isAddingTracker = false;
    });
  }

  // ── Log Entries ──────────────────────────────────────────────
  openAddEntry() {
    if (!this.selectedTracker) return;
    const fg = this.fb.group({});
    this.selectedTracker.fieldDefinitions?.forEach(f =>
      fg.addControl(f.name, this.fb.control('', Validators.required))
    );
    this.entryForm.setControl('fieldValues', fg);
    this.newEntryNote = '';
    this.editingEntryId = null;
    this.isAddingEntry = true;
  }

  editLog(entry: TrackerEntry) {
    if (!this.selectedTracker) return;
    const fg = this.fb.group({});
    this.selectedTracker.fieldDefinitions?.forEach(f => {
      fg.addControl(f.name, this.fb.control(entry.fieldValues?.[f.name] || '', Validators.required));
    });
    this.entryForm.setControl('fieldValues', fg);
    this.newEntryNote = entry.note || '';
    this.editingEntryId = entry.id!;
    this.isAddingEntry = true;
  }

  addEntry() {
    if (!this.selectedTracker || this.entryForm.invalid) return;
    const entryData: TrackerEntry = {
      trackerId: this.selectedTracker.id!,
      fieldValues: this.entryForm.get('fieldValues')?.value,
      note: this.newEntryNote,
      date: new Date().toISOString()
    };
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

  // ── Delete ───────────────────────────────────────────────────
  deleteLog(id: number) { this.deleteConfirm = { type: 'ENTRY', id }; }

  deleteAppIcon(id: number, event: Event) {
    event.stopPropagation();
    this.deleteConfirm = { type: 'APP', id };
  }

  deleteTrackerIcon(id: number, event: Event) {
    event.stopPropagation();
    this.deleteConfirm = { type: 'TRACKER', id };
  }

  cancelDelete() { this.deleteConfirm = null; }

  confirmDelete() {
    if (!this.deleteConfirm) return;
    const { type, id } = this.deleteConfirm;

    if (type === 'APP') {
      this.omniService.deleteApp(id).subscribe(() => {
        this.apps = this.apps.filter(a => a.id !== id);
        this.deleteConfirm = null;
        if (this.selectedApp?.id === id) this.goBack();
      });
    } else if (type === 'TRACKER') {
      this.omniService.deleteTracker(id).subscribe(() => {
        this.trackers = this.trackers.filter(t => t.id !== id);
        this.deleteConfirm = null;
        if (this.selectedTracker?.id === id) this.goBack();
      });
    } else {
      this.omniService.deleteEntry(id).subscribe(() => {
        this.entries = this.entries.filter(e => e.id !== id);
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
    return entry.fieldValues ? Object.keys(entry.fieldValues) : [];
  }

  getOptions(optionsStr: string): string[] {
    if (!optionsStr) return [];
    return optionsStr.split(',').map(s => s.trim()).filter(s => s !== '');
  }

  formatFieldValue(val: any, fieldDef: any): string {
    if (val === undefined || val === null || val === '') return '—';
    
    switch(fieldDef.type) {
      case 'CURRENCY':
        return `$${parseFloat(val).toFixed(2)}`;
      case 'RATING':
        return '⭐'.repeat(parseInt(val)) || '—';
      case 'BOOLEAN':
        return val === 'true' || val === true ? '✅ Yes' : '❌ No';
      default:
        return fieldDef.unit ? `${val} ${fieldDef.unit}` : `${val}`;
    }
  }

  getFieldDef(key: string): any {
    return this.selectedTracker?.fieldDefinitions.find(f => f.name === key);
  }

  getLatestValue(): string {
    if (!this.entries.length || !this.selectedTracker?.fieldDefinitions?.length) return '—';
    const fieldDef = this.selectedTracker.fieldDefinitions[0];
    const val = this.entries[0]?.fieldValues?.[fieldDef.name];
    return this.formatFieldValue(val, fieldDef);
  }
}
