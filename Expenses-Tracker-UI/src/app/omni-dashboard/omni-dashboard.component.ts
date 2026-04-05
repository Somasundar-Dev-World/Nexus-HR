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

  // View state — 'APP_GRID' is apps, 'APP_DASHBOARD' is app-level graphs/stats, 'TRACKER_GRID' is grid inside app, 'DETAIL' is logs
  viewMode: 'APP_GRID' | 'APP_DASHBOARD' | 'TRACKER_GRID' | 'DETAIL' = 'APP_GRID';

  appStats = {
    archetype: 'GENERIC' as 'FINANCE' | 'HEALTH' | 'GENERIC',
    primaryMetrics: [] as { label: string, value: string, icon: string, trend?: string }[],
    recentLogs: [] as any[],
    breakdown: [] as { name: string, value: number, percent: number }[]
  };

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
    },
    {
      name: 'Global Carbon Footprint',
      type: 'CUSTOM',
      icon: '🌍',
      fields: [
        { name: 'Emission Type', type: 'SELECT', options: 'Scope 1 (Direct), Scope 2 (Energy), Scope 3 (Value Chain)' },
        { name: 'Carbon Metric Tons (CO2e)', type: 'NUMBER', unit: 'mt' },
        { name: 'Energy Source', type: 'SELECT', options: 'Grid Mix, Solar, Wind, Natural Gas, Diesel' },
        { name: 'Mitigation Offset Ratio (%)', type: 'RATING' },
        { name: 'Audit Ref ID', type: 'TEXT' }
      ]
    },
    {
      name: 'DEI & Social Impact Index',
      type: 'HEALTH',
      icon: '🤝',
      fields: [
        { name: 'Focus Area', type: 'SELECT', options: 'Representational Diversity, Pay Equity, Inclusive Culture, Community Impact' },
        { name: 'Progress Metric (%)', type: 'NUMBER', unit: '%' },
        { name: 'Equity Ratio (Target 1.0)', type: 'NUMBER' },
        { name: 'Culture Sentiment Score', type: 'RATING' },
        { name: 'Strategy Notes', type: 'LONG_TEXT' }
      ]
    },
    {
      name: 'AI Ethical Governance Hub',
      type: 'CUSTOM',
      icon: '🤖',
      fields: [
        { name: 'Model Name / LLM', type: 'TEXT' },
        { name: 'Bias Detection Score', type: 'RATING' },
        { name: 'Hallucination Mitigation (%)', type: 'NUMBER', unit: '%' },
        { name: 'Compliance Level', type: 'SELECT', options: 'EU AI Act Ready, NIST Aligned, Internal Guardrails Only' },
        { name: 'Transparency Report Link', type: 'TEXT' }
      ]
    },
    {
      name: 'Rocket Subscription Manager',
      type: 'FINANCE',
      icon: '💳',
      fields: [
        { name: 'Service Name', type: 'TEXT' },
        { name: 'Monthly Cost', type: 'CURRENCY' },
        { name: 'Next Bill Date', type: 'DATE' },
        { name: 'Status', type: 'SELECT', options: 'Active, Cancelled, To Review' },
        { name: 'Value Score', type: 'RATING' }
      ]
    },
    {
      name: 'Bills & Recurring Spends',
      type: 'FINANCE',
      icon: '🧾',
      fields: [
        { name: 'Bill Name', type: 'TEXT' },
        { name: 'Category', type: 'SELECT', options: 'Rent/Mortgage, Utilities, Insurance, Internet/Mobile, Other' },
        { name: 'Due Date', type: 'DATE' },
        { name: 'Amount', type: 'CURRENCY' },
        { name: 'Paid Status', type: 'BOOLEAN' }
      ]
    },
    {
      name: 'Net Worth Observatory',
      type: 'FINANCE',
      icon: '🏦',
      fields: [
        { name: 'Account / Asset', type: 'TEXT' },
        { name: 'Category', type: 'SELECT', options: 'Cash, Savings, Investment, Crypto, Real Estate, Debt' },
        { name: 'Current Balance', type: 'CURRENCY' },
        { name: 'Growth (%)', type: 'NUMBER', unit: '%' },
        { name: 'Last Updated', type: 'DATE' }
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
      icon: ['⚙️'],
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
        if (this.apps.length === 0) {
          this.omniService.getTrackers().subscribe(legacyTrackers => {
            if (legacyTrackers.length > 0) {
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

  calculateAppStats(appId: number) {
    if (this.trackers.length === 0) {
      this.appStats = { archetype: 'GENERIC', primaryMetrics: [], recentLogs: [], breakdown: [] };
      return;
    }

    const typeCounts = { FINANCE: 0, HEALTH: 0, STOCK: 0, CUSTOM: 0 };
    this.trackers.forEach(t => typeCounts[t.type as keyof typeof typeCounts]++);
    
    let archetype: 'FINANCE' | 'HEALTH' | 'GENERIC' = 'GENERIC';
    if (typeCounts.FINANCE >= typeCounts.HEALTH && typeCounts.FINANCE > 0) archetype = 'FINANCE';
    else if (typeCounts.HEALTH > typeCounts.FINANCE) archetype = 'HEALTH';

    this.appStats.archetype = archetype;
    const allLogs: any[] = [];
    let processedTrackers = 0;
    let totalFinance = 0;
    let totalHealthScore = 0;
    let healthMetricCount = 0;

    this.trackers.forEach(tracker => {
      this.omniService.getEntries(tracker.id!).subscribe(entries => {
        processedTrackers++;
        const identityField = tracker.fieldDefinitions?.[0]?.name;
        const latestByIdentity: { [id: string]: TrackerEntry } = {};
        
        entries.forEach(e => {
          const idValue = identityField ? e.fieldValues[identityField] : 'default';
          if (!latestByIdentity[idValue]) latestByIdentity[idValue] = e;
          allLogs.push({ ...e, trackerName: tracker.name, trackerIcon: tracker.icon || this.getIcon(tracker.type) });
        });

        Object.values(latestByIdentity).forEach(e => {
          Object.keys(e.fieldValues || {}).forEach(k => {
            const field = tracker.fieldDefinitions.find(f => f.name === k);
            const valNum = parseFloat(e.fieldValues[k]);
            if (field?.type === 'CURRENCY' || k.toLowerCase().includes('balance') || k.toLowerCase().includes('cost')) totalFinance += valNum || 0;
            if (field?.type === 'RATING') { totalHealthScore += valNum || 0; healthMetricCount++; }
          });
        });

        if (processedTrackers === this.trackers.length) {
          if (archetype === 'FINANCE') {
            this.appStats.primaryMetrics = [
              { label: 'Total Value Hub', value: `$${totalFinance.toLocaleString()}`, icon: '💰', trend: 'Monthly' },
              { label: 'Asset Classes', value: `${this.trackers.length}`, icon: '📊' },
              { label: 'Financial Health', value: 'Prime', icon: '✨' }
            ];
          } else if (archetype === 'HEALTH') {
            const avg = healthMetricCount > 0 ? (totalHealthScore / healthMetricCount).toFixed(1) : '—';
            this.appStats.primaryMetrics = [
              { label: 'Avg Health Score', value: `⭐ ${avg}`, icon: '🧬' },
              { label: 'Recovery Status', value: 'Optimal', icon: '🛌' },
              { label: 'Logs Analyzed', value: `${allLogs.length}`, icon: '📋' }
            ];
          } else {
            this.appStats.primaryMetrics = [
              { label: 'Operational Hub', value: `${this.trackers.length}`, icon: '⚙️' },
              { label: 'Total Activity', value: `${allLogs.length}`, icon: '🔄' },
              { label: 'System status', value: 'Online', icon: '🌐' }
            ];
          }
          this.appStats.recentLogs = allLogs.sort((a,b) => new Date(b.date).getTime() - new Date(a.date).getTime()).slice(0, 5);
        }
      });
    });
  }

  // ── Navigation ──────────────────────────────────────────────
  selectApp(app: OmniApp) {
    this.selectedApp = app;
    this.isLoading = true;
    this.omniService.getTrackers(app.id).subscribe(data => {
      this.trackers = data;
      this.calculateAppStats(app.id!);
      this.viewMode = 'APP_DASHBOARD';
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
      this.viewMode = 'APP_DASHBOARD';
      this.selectedTracker = undefined;
      this.entries = [];
    } else if (this.viewMode === 'TRACKER_GRID' || this.viewMode === 'APP_DASHBOARD') {
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
      type: tpl.type,
      icon: tpl.icon
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
      this.calculateAppStats(this.selectedApp!.id!); // Refresh stats
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
