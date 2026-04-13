/**
 * v1.0.1 - Rich AI Chat with ApexCharts & PDF Export
 */
import { Component, OnInit, OnDestroy, ViewChild, ElementRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule, ReactiveFormsModule, FormBuilder, FormGroup, FormArray, Validators } from '@angular/forms';
import { OmniTrackerService, Tracker, TrackerEntry, SmartInsight, AiReport } from '../omni-tracker.service';
import { OmniApp } from '../omni-app.model';

declare var Plaid: any;

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
  aiInsights: SmartInsight[] = [];
  loadingInsights = false;

  // View state — 'APP_GRID' is apps, 'APP_DASHBOARD' is app-level graphs/stats, 'TRACKER_GRID' is grid inside app, 'DETAIL' is logs, 'INTELLIGENCE_REPORTS' is dynamic AI reports, 'REPORT_VIEW' is rendered chart
  viewMode: 'APP_GRID' | 'APP_DASHBOARD' | 'TRACKER_GRID' | 'DETAIL' | 'INTELLIGENCE_REPORTS' | 'REPORT_VIEW' = 'APP_GRID';

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
  editingAppId: number | null = null;
  editingTrackerId: number | null = null;
  deleteConfirm: { type: 'APP' | 'TRACKER' | 'ENTRY', id: number } | null = null;

  // AI Import state
  selectedFile: File | null = null;
  importTrackerName = '';
  isImporting = false;
  importResult: { entryCount: number, skippedCount: number, fileName?: string, skippedRecords?: any[] } | null = null;
  isImportModalOpen = false;
  showDeleteTrackerConfirm = false;

  // Modern Success Modal
  showSuccessModal = false;
  successData: any = null;

  // Deep Research State
  isDeepInsightOpen = false;
  isDeepInsightLoading = false;
  
  // Intelligence Reporting State
  savedReports: AiReport[] = [];
  reportArchitectMode = false;
  isArchitecting = false;
  architectSelectedTrackers: number[] = [];
  architectSuggestions: any[] = [];
  reportNameInput: string = '';
  activeReportId: number | null = null;
  activeReportResult: any = null;
  activeReportMeta: AiReport | null = null;
  isReportLoading = false;
  architectError: string | null = null;

  // AI Chat State
  isChatOpen = false;
  isChatLoading = false;
  chatInputText = '';
  chatMessages: { role: string, content: string, richBlocks?: any[], id?: string }[] = [];
  isChatMaximized = false;
  deepInsightReport: any = null;
  deepInsightLoadingSteps = [
    { label: 'Scanning all tracker records...', done: false },
    { label: 'Computing field statistics...', done: false },
    { label: 'Detecting anomalies & trends...', done: false },
    { label: 'Generating AI forecast...', done: false },
    { label: 'Assembling deep report...', done: false },
  ];
  deepInsightStepIndex = 0;
  private deepInsightStepTimer: any;

  // Plaid Mapping State
  isPlaidMappingModalOpen = false;
  isAutoMapping = false;
  isAutoMapped = false;
  connectedInstitution = '';
  plaidMapping: { [plaidField: string]: string } = { amount: '', name: '', date: '' };
  plaidStandardFields = [
    { id: 'amount', label: 'Transaction Amount' },
    { id: 'name', label: 'Merchant Name' },
    { id: 'date', label: 'Transaction Date' }
  ];

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
    this.aiInsights = [];
    this.omniService.getTrackers(app.id).subscribe(data => {
      this.trackers = data;
      this.calculateAppStats(app.id!);
      this.fetchAiInsights(app.id!);
      this.viewMode = 'APP_DASHBOARD';
      this.isLoading = false;
    });
  }

  fetchAiInsights(appId: number, refresh: boolean = false) {
    this.loadingInsights = true;
    this.omniService.getAiInsights(appId, refresh).subscribe({
      next: (insights) => {
        this.aiInsights = insights.sort((a, b) => a.priority - b.priority);
        this.loadingInsights = false;
      },
      error: (err) => {
        console.error('Failed to fetch AI insights', err);
        this.loadingInsights = false;
      }
    });
  }

  refreshAI() {
    if (this.selectedApp?.id) {
      this.fetchAiInsights(this.selectedApp.id, true);
    }
  }

  showReasoning(insight: SmartInsight) {
    if (insight.reasoning) {
      // Use Success Modal for reasoning too, or a specialized one
      this.successData = {
        title: 'AI Insights Reasoning',
        message: insight.reasoning,
        icon: '🔬',
        isReasoning: true
      };
      this.showSuccessModal = true;
    }
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
      this.showSuccessModal = false;
    }
  }

  onSuccessAction() {
    if (this.successData?.tracker) {
      this.goToDetail(this.successData.tracker);
    }
    this.showSuccessModal = false;
  }

  // ── Create App ───────────────────────────────────────────────
  openAddApp() {
    this.appForm.reset({ icon: '🚀' });
    this.editingAppId = null;
    this.isAddingApp = true;
  }

  openEditApp(app: OmniApp) {
    this.appForm.patchValue({
      name: app.name,
      icon: app.icon,
      description: app.description
    });
    this.editingAppId = app.id!;
    this.isAddingApp = true;
  }

  createApp() {
    if (this.appForm.invalid) return;
    
    if (this.editingAppId) {
      this.omniService.updateApp(this.editingAppId, this.appForm.value).subscribe(updated => {
        const idx = this.apps.findIndex(a => a.id === updated.id);
        if (idx !== -1) this.apps[idx] = updated;
        if (this.selectedApp?.id === updated.id) this.selectedApp = updated;
        this.isAddingApp = false;
        this.editingAppId = null;
      });
    } else {
      this.omniService.createApp(this.appForm.value).subscribe(app => {
        this.apps.push(app);
        this.isAddingApp = false;
      });
    }
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
    this.trackerForm.reset({ type: 'FINANCE', icon: '⚙️' });
    this.fieldDefinitions.clear();
    this.selectedTemplateIndex = null;
    this.editingTrackerId = null;
    this.addField();
    this.isAddingTracker = true;
  }

  openEditTracker(tracker: Tracker) {
    this.trackerForm.patchValue({
      name: tracker.name,
      type: tracker.type,
      icon: tracker.icon || '⚙️'
    });
    this.fieldDefinitions.clear();
    tracker.fieldDefinitions.forEach(f => this.addField(f));
    this.editingTrackerId = tracker.id!;
    this.isAddingTracker = true;
  }

  createTracker() {
    if (this.trackerForm.invalid || !this.selectedApp) return;
    const trackerData = { ...this.trackerForm.value, appId: this.selectedApp.id };
    
    if (this.editingTrackerId) {
      this.omniService.updateTracker(this.editingTrackerId, trackerData).subscribe(updated => {
        const idx = this.trackers.findIndex(t => t.id === updated.id);
        if (idx !== -1) this.trackers[idx] = updated;
        if (this.selectedTracker?.id === updated.id) {
          this.selectedTracker = updated;
          this.entries = []; // Re-fetch or clear to be safe
          this.omniService.getEntries(updated.id!).subscribe(data => this.entries = data);
        }
        this.isAddingTracker = false;
        this.editingTrackerId = null;
        this.calculateAppStats(this.selectedApp!.id!);
      });
    } else {
      this.omniService.createTracker(trackerData).subscribe(tracker => {
        this.trackers.push(tracker);
        this.isAddingTracker = false;
        this.calculateAppStats(this.selectedApp!.id!); // Refresh stats
      });
    }
  }

  // --- AI Tracker Import ---
  onFileSelected(event: any) {
    const file = event.target.files[0];
    if (file) {
      this.selectedFile = file;
    }
  }

  autoImportTracker() {
    if (!this.selectedFile || !this.selectedApp) return;
    
    this.isImporting = true;
    this.omniService.importTracker(this.selectedFile, this.selectedApp.id!, this.importTrackerName)
      .subscribe({
        next: (res) => {
          this.trackers.push(res.tracker);
          this.isAddingTracker = false;
          this.isImporting = false;
          this.selectedFile = null;
          this.importTrackerName = '';
          this.calculateAppStats(this.selectedApp!.id!);
          
          // Show Modern Success Modal instead of alert
          this.successData = {
            title: 'Import Successful',
            message: `Successfully created the "${res.tracker.name}" tracker.`,
            count: res.entryCount,
            icon: res.tracker.icon || '📄',
            tracker: res.tracker
          };
          this.showSuccessModal = true;
        },
        error: (err) => {
          console.error(err);
          this.isImporting = false;
          this.successData = {
            title: 'Import Failed',
            message: 'Failed to import document: ' + (err.error || err.message),
            icon: '❌',
            isError: true
          };
          this.showSuccessModal = true;
        }
      });
  }

  onEntriesFileSelected(event: any) {
    const files: File[] = Array.from(event.target.files || []);
    if (files.length > 0 && this.selectedTracker) {
      this.isImporting = true;
      this.omniService.importEntries(files, this.selectedTracker.id!)
        .subscribe({
          next: (res) => {
            this.isImporting = false;
            // Refetch entries
            this.omniService.getEntries(this.selectedTracker!.id!).subscribe(data => {
              this.entries = data;
              this.calculateAppStats(this.selectedApp!.id!);
              const fileNameDesc = files.length === 1 ? files[0].name : `${files.length} files`;
              this.importResult = { 
                entryCount: res.entryCount, 
                skippedCount: res.skippedCount || 0, 
                fileName: fileNameDesc,
                skippedRecords: res.skippedRecords || [] 
              };
              this.isImportModalOpen = true;
            });
          },
          error: (err) => {
            console.error(err);
            this.isImporting = false;
            this.successData = {
              title: 'Upload Failed',
              message: 'Failed to import entries: ' + (err.error || err.message),
              icon: '❌',
              isError: true
            };
            this.showSuccessModal = true;
          }
        });
      // reset file input
      event.target.value = null;
    }
  }

  closeImportResult() {
    this.importResult = null;
    this.isImportModalOpen = false;
    this.showSkippedRecords = false;
  }

  showSkippedRecords = false;
  toggleSkippedRecords() {
    this.showSkippedRecords = !this.showSkippedRecords;
  }

  formatSkippedRecord(record: any): string {
    if (!record) return '';
    try {
      return JSON.stringify(record).replace(/["{}]/g, '').replace(/,/g, ', ');
    } catch {
      return 'Invalid Record Data';
    }
  }

  // ── Delete Current Tracker ───────────────────────────────────
  confirmDeleteTracker() {
    if (!this.selectedTracker) return;
    this.omniService.deleteTracker(this.selectedTracker.id!).subscribe({
      next: () => {
        this.showDeleteTrackerConfirm = false;
        this.selectedTracker = undefined;
        this.entries = [];
        this.importResult = null;
        this.isImportModalOpen = false;
        this.goBack();
      },
      error: (err) => {
        console.error(err);
        alert('Failed to delete tracker.');
      }
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

  // ── Deep Research ─────────────────────────────────────────────
  loadDeepInsight(forceRefresh: boolean = false) {
    if (!this.selectedApp) return;
    this.isDeepInsightOpen = true;
    this.isDeepInsightLoading = true;
    this.deepInsightReport = null;
    this.deepInsightStepIndex = 0;
    this.deepInsightLoadingSteps.forEach(s => s.done = false);

    // Animate loading steps
    this.deepInsightStepTimer = setInterval(() => {
      if (this.deepInsightStepIndex < this.deepInsightLoadingSteps.length) {
        this.deepInsightLoadingSteps[this.deepInsightStepIndex].done = true;
        this.deepInsightStepIndex++;
      }
    }, 1800);

    this.omniService.getDeepInsight(this.selectedApp.id!, forceRefresh).subscribe({
      next: (report) => {
        clearInterval(this.deepInsightStepTimer);
        this.deepInsightLoadingSteps.forEach(s => s.done = true);
        setTimeout(() => {
          this.isDeepInsightLoading = false;
          this.deepInsightReport = report;
        }, 600);
      },
      error: (err) => {
        clearInterval(this.deepInsightStepTimer);
        this.isDeepInsightLoading = false;
        this.deepInsightReport = {
          executiveSummary: 'Failed to generate deep insight: ' + (err?.error?.message || err.message || 'Unknown error'),
          overallScore: 0, scoreLabel: 'Error', sections: []
        };
      }
    });
  }

  closeDeepInsight() {
    clearInterval(this.deepInsightStepTimer);
    this.isDeepInsightOpen = false;
    this.deepInsightReport = null;
  }

  getScoreColor(score: number): string {
    if (score >= 80) return '#10b981';
    if (score >= 60) return '#f59e0b';
    if (score >= 40) return '#f97316';
    return '#ef4444';
  }

  getSectionColorClass(color: string): string {
    const map: any = {
      success: 'deep-card-success',
      warning: 'deep-card-warning',
      danger: 'deep-card-danger',
      primary: 'deep-card-primary',
      magic: 'deep-card-magic',
      info: 'deep-card-info',
    };
    return map[color] || 'deep-card-primary';
  }

  // ── AI Chat ──────────────────────────────────────────────────
  toggleChat() {
    this.isChatOpen = !this.isChatOpen;
    if (!this.isChatOpen) {
      this.isChatMaximized = false;
    }
    // Scroll chat to bottom when opened
    if (this.isChatOpen) {
      setTimeout(() => this.scrollToBottom(), 100);
    }
  }

  toggleMaximizeChat() {
    this.isChatMaximized = !this.isChatMaximized;
  }

  sendChatMessage() {
    if (!this.chatInputText.trim() || !this.selectedApp) return;

    const userMessage = this.chatInputText.trim();
    this.chatMessages.push({ role: 'user', content: userMessage });
    this.chatInputText = '';
    this.isChatLoading = true;
    this.scrollToBottom();

    // The history excludes the new message we just pushed
    const history = this.chatMessages.slice(0, -1);

    this.omniService.chatWithApp(this.selectedApp.id!, { history, message: userMessage }).subscribe({
      next: (res) => {
        this.isChatLoading = false;
        const reply = res.reply || 'No response.';
        const richBlocks = this.parseRichMessage(reply);
        const msgId = 'msg-' + Math.random().toString(36).substr(2, 9);
        this.chatMessages.push({ role: 'assistant', content: reply, richBlocks, id: msgId });
        
        // Handle Charts post-push with multiple retry attempts if needed
        this.attemptChartInit(richBlocks, 5);
        
        this.scrollToBottom();
      },
      error: (err) => {
        this.isChatLoading = false;
        this.chatMessages.push({ role: 'assistant', content: 'Connection failed. Please try again.' });
        this.scrollToBottom();
      }
    });
  }

  parseRichMessage(text: string): any[] {
    const blocks: any[] = [];
    let remaining = text;

    // Pattern for tags: [TAG] content [/TAG] (optional closing)
    const tags = ['SUMMARY', 'TABLE', 'INSIGHT', 'CHART'];
    
    // We'll use a more advanced regex or a sequential scanner
    // For simplicity, let's use a robust sequential scanner for these markers
    
    const parts = text.split(/(\[SUMMARY\]|\[TABLE\]|\[INSIGHT\]|\[CHART.*?\])/i);
    let currentType = 'TEXT';
    
    parts.forEach(part => {
      if (!part) return;

      const upper = part.toUpperCase();
      if (upper === '[SUMMARY]') { currentType = 'SUMMARY'; return; }
      if (upper === '[TABLE]') { currentType = 'TABLE'; return; }
      if (upper === '[INSIGHT]') { currentType = 'INSIGHT'; return; }
      if (upper.startsWith('[CHART')) {
        const match = part.match(/type=['"](.*?)['"]/i);
        currentType = 'CHART:' + (match ? match[1].toLowerCase() : 'bar');
        return;
      }

      // Normal content
      let content = part.trim();
      
      // Strip any lingering closing tags efficiently
      content = content.replace(/\[\/(SUMMARY|TABLE|INSIGHT|CHART)\]/gi, '').trim();

      if (!content || content === 'N/A' || content === 'null') return;

      if (currentType === 'TEXT') {
        blocks.push({ type: 'TEXT', content });
      } else if (currentType === 'SUMMARY') {
        blocks.push({ type: 'SUMMARY', content });
      } else if (currentType === 'TABLE') {
        blocks.push({ type: 'TABLE', rows: this.parseMarkdownTable(content) });
      } else if (currentType === 'INSIGHT') {
        blocks.push({ type: 'INSIGHT', content });
      } else if (currentType.startsWith('CHART:')) {
        try {
          const chartData = JSON.parse(content);
          // Only push if we have valid data series
          if (chartData && chartData.series && chartData.series.length > 0) {
            blocks.push({ 
              type: 'CHART', 
              chartType: currentType.split(':')[1], 
              data: chartData,
              id: 'chart-' + Math.random().toString(36).substr(2, 9)
            });
          } else {
            console.warn('AI returned empty chart data, skipping block.');
          }
        } catch (e) {
          blocks.push({ type: 'TEXT', content: '[Data unavailable]' });
        }
      }
      currentType = 'TEXT'; // Reset
    });

    return blocks;
  }

  parseMarkdownTable(md: string) {
    const lines = md.split('\n').filter(l => l.includes('|'));
    if (lines.length < 2) return null;

    const headers = lines[0].split('|').map(h => h.trim()).filter(h => h);
    const rows = lines.slice(2).map(line => {
      return line.split('|').map(c => c.trim()).filter(c => c);
    });

    return { headers, rows };
  }

  attemptChartInit(blocks: any[], retries: number) {
    if (retries <= 0) return;
    
    setTimeout(() => {
      const allExist = blocks.filter(b => b.type === 'CHART').every(b => document.getElementById(b.id));
      if (allExist) {
        this.initializeCharts(blocks);
      } else {
        this.attemptChartInit(blocks, retries - 1);
      }
    }, 200);
  }

  initializeCharts(blocks: any[]) {
    blocks.forEach(block => {
      if (block.type === 'CHART') {
        const el = document.getElementById(block.id);
        if (el && (window as any).ApexCharts) {
          try {
            // SMART SERIES MAPPING: Detect if AI returned a single array or a complex multi-series list
            let chartSeries = [];
            if (block.chartType === 'pie') {
              chartSeries = block.data.series;
            } else {
              // If first element is an object with 'data', use series as is
              if (block.data.series && block.data.series.length > 0 && typeof block.data.series[0] === 'object') {
                chartSeries = block.data.series;
              } else {
                // Otherwise wrap it (for legacy simple series)
                chartSeries = [{ name: 'Value', data: block.data.series }];
              }
            }

            const options = {
              chart: {
                type: block.chartType,
                height: 250,
                foreColor: '#94a3b8',
                toolbar: { show: false },
                background: 'transparent'
              },
              series: chartSeries,
              labels: block.data.labels,
              theme: { mode: 'dark' },
              colors: ['#3b82f6', '#10b981', '#f59e0b', '#ef4444', '#8b5cf6'],
              stroke: { curve: 'smooth', width: 2 },
              grid: { borderColor: 'rgba(255,255,255,0.05)' }
            };
            const chart = new (window as any).ApexCharts(el, options);
            chart.render();
          } catch (e) {
            console.error('ApexChart Init Failed', e);
            if (el) el.style.display = 'none';
          }
        }
      }
    });
  }

  formatMarkdown(text: string | number): string {
    if (text === undefined || text === null || text === '') return '—';
    let html = String(text);
    // Bold (more robust regex for word boundaries)
    html = html.replace(/\*\*([\s\S]*?)\*\*/g, '<strong>$1</strong>');
    // Italic
    html = html.replace(/\*([\s\S]*?)\*/g, '<em>$1</em>');
    // Headers
    html = html.replace(/^### (.*$)/gm, '<h3 class="chat-h3">$1</h3>');
    // Inline Code
    html = html.replace(/`(.*?)`/g, '<code class="chat-code">$1</code>');
    // Bullets (handles any number of spaces after *)
    html = html.replace(/^\*\s+(.*$)/gm, '<div class="chat-bullet"><span>•</span> $1</div>');
    // Newlines
    html = html.replace(/\n/g, '<br/>');

    return html;
  }

  scrollToBottom() {
    setTimeout(() => {
      const chatBody = document.querySelector('.chat-body');
      if (chatBody) {
        chatBody.scrollTop = chatBody.scrollHeight;
      }
    }, 50);
  }

  // --- Intelligence Reporting ---

  goToReports() {
    this.viewMode = 'INTELLIGENCE_REPORTS';
    this.loadReports();
  }

  loadReports() {
    if (!this.selectedApp?.id) return;
    this.omniService.getReportsByApp(this.selectedApp.id).subscribe(reports => {
      this.savedReports = reports;
    });
  }

  openReportArchitect() {
    this.reportArchitectMode = true;
    this.architectSelectedTrackers = [];
    this.architectSuggestions = [];
    this.reportNameInput = '';
  }

  toggleArchitectTrackerSelection(id: number) {
    const idx = this.architectSelectedTrackers.indexOf(id);
    if (idx > -1) this.architectSelectedTrackers.splice(idx, 1);
    else this.architectSelectedTrackers.push(id);
  }

  generateReportSuggestions() {
    if (this.architectSelectedTrackers.length === 0 || !this.selectedApp?.id) return;
    this.isArchitecting = true;
    this.architectError = null;
    this.omniService.suggestReports(this.selectedApp.id, this.architectSelectedTrackers).subscribe({
      next: (suggestions) => {
        this.architectSuggestions = suggestions;
        this.isArchitecting = false;
      },
      error: (err) => {
        this.isArchitecting = false;
        const msg: string = err?.error?.message || err?.message || 'Unknown error occurred.';
        if (msg.toLowerCase().includes('quota') || msg.toLowerCase().includes('rate limit') || msg.toLowerCase().includes('wait')) {
          this.architectError = 'quota';
        } else {
          this.architectError = msg;
        }
      }
    });
  }

  saveArchitectReport(suggestion: any) {
    if (!this.selectedApp?.id) return;
    const report: AiReport = {
      name: this.reportNameInput || suggestion.name,
      description: suggestion.description,
      appId: this.selectedApp.id,
      visualType: suggestion.visualType,
      querySpec: suggestion.querySpec,
      config: suggestion.config
    };
    this.omniService.saveReport(report).subscribe(() => {
      this.reportArchitectMode = false;
      this.loadReports();
    });
  }

  runReport(id: number | undefined) {
    if (!id) return;
    this.isReportLoading = true;
    this.activeReportId = id;
    this.activeReportResult = null;
    this.activeReportMeta = this.savedReports.find(r => r.id === id) || null;
    this.omniService.executeReport(id).subscribe({
      next: (result) => {
        this.activeReportResult = result;
        this.isReportLoading = false;
        this.viewMode = 'REPORT_VIEW';
        setTimeout(() => this.initializeReportChart(), 300);
      },
      error: (err) => {
        console.error(err);
        this.isReportLoading = false;
        alert('Failed to execute intelligence report.');
      }
    });
  }

  closeReportDetail() {
    this.activeReportId = null;
    this.activeReportResult = null;
    this.activeReportMeta = null;
    this.viewMode = 'INTELLIGENCE_REPORTS';
  }

  deleteReport(id: number | undefined) {
    if (!id) return;
    if (confirm('Delete this report?')) {
      this.omniService.deleteReport(id).subscribe(() => this.loadReports());
    }
  }

  private initializeReportChart() {
    if (!this.activeReportResult || !this.activeReportResult.labels) return;
    
    const ApexCharts = (window as any).ApexCharts;
    if (!ApexCharts) return;

    const chartEl = document.querySelector('#report-main-chart');
    if (!chartEl) {
      setTimeout(() => this.initializeReportChart(), 200);
      return;
    }

    // Destroy any previous chart instance
    if ((chartEl as any).__apexcharts) {
      (chartEl as any).__apexcharts.destroy();
    }

    const type = (this.activeReportResult.visualType || 'bar').toLowerCase();
    const config = this.activeReportResult.config || {};
    const isPie = type === 'pie' || type === 'donut';
    const isRadar = type === 'radar';
    const labelCount = this.activeReportResult.labels?.length || 0;
    const isHorizontal = labelCount > 15 && !isPie && !isRadar;

    // PIE charts need a flat number array; others need the nested series
    const seriesData = isPie
      ? (this.activeReportResult.series[0]?.data || [])
      : this.activeReportResult.series;

    // Dynamic height for large lists (like 66 instruments)
    let dynamicHeight = 420;
    if (isHorizontal) {
      dynamicHeight = Math.max(420, labelCount * 30 + 100);
    }

    const options: any = {
      series: seriesData,
      chart: {
        type: isPie ? 'pie' : (isRadar ? 'radar' : (type === 'metric_grid' ? 'bar' : type)),
        height: dynamicHeight,
        background: 'transparent',
        foreColor: '#94a3b8',
        toolbar: { show: true },
        animations: { enabled: true, easing: 'easeinout', speed: 800 }
      },
      colors: ['#6366f1', '#10b981', '#f59e0b', '#ef4444', '#8b5cf6', '#06b6d4', '#f97316', '#ec4899'],
      plotOptions: {
        bar: { 
          borderRadius: 4, 
          columnWidth: '55%',
          horizontal: isHorizontal,
          dataLabels: { position: isHorizontal ? 'right' : 'top' }
        }
      },
      dataLabels: { 
        enabled: isPie || (labelCount < 30 && !isRadar),
        formatter: (val: any) => val.toLocaleString(),
        style: { fontSize: '10px', colors: ['#fff'] }
      },
      stroke: { curve: 'smooth', width: isPie ? 0 : 3 },
      labels: isPie ? this.activeReportResult.labels : undefined,
      xaxis: isPie ? undefined : {
        type: 'category',
        categories: isHorizontal ? undefined : this.activeReportResult.labels,
        title: { text: isHorizontal ? config.yAxis : config.xAxis, style: { color: '#64748b' } },
        labels: { 
          style: { colors: '#64748b', fontSize: '11px' },
          rotate: -45
        }
      },
      yaxis: isPie ? undefined : {
        categories: isHorizontal ? this.activeReportResult.labels : undefined,
        title: { text: isHorizontal ? config.xAxis : config.yAxis, style: { color: '#64748b' } },
        labels: { 
          style: { colors: '#64748b' },
          formatter: (val: any) => val.toLocaleString()
        }
      },
      tooltip: {
        theme: 'dark',
        y: { formatter: (val: any) => '$' + val.toLocaleString() }
      },
      legend: {
        position: isPie ? 'bottom' : 'top',
        labels: { colors: '#94a3b8' }
      },
      grid: { borderColor: 'rgba(255,255,255,0.05)' },
      theme: { mode: 'dark' }
    };

    const chart = new ApexCharts(chartEl, options);
    (chartEl as any).__apexcharts = chart;
    chart.render();
  }

  goBackToDashboard() {
    this.viewMode = 'APP_DASHBOARD';
  }

  async exportToPDF(elementId: string, filename: string) {
    const element = document.getElementById(elementId);
    if (!element) return;

    // Use global access to h2c and jspdf from CDN
    const html2canvas = (window as any).html2canvas;
    const { jsPDF } = (window as any).jspdf;

    if (!html2canvas || !jsPDF) {
      alert('PDF libraries are still loading. Please try again in 2 seconds.');
      return;
    }

    try {
      // Temporarily hide export buttons during capture
      const buttons = element.querySelectorAll('.export-ignore');
      buttons.forEach((b: any) => b.style.opacity = '0');

      const canvas = await html2canvas(element, {
        scale: 2, // High-res
        useCORS: true,
        backgroundColor: '#0f172a', // Match theme
        logging: false
      });

      // Show buttons back
      buttons.forEach((b: any) => b.style.opacity = '1');

      const imgData = canvas.toDataURL('image/png');
      const pdf = new jsPDF('p', 'mm', 'a4');
      
      const imgProps = pdf.getImageProperties(imgData);
      const pdfWidth = pdf.internal.pageSize.getWidth();
      const pdfHeight = (imgProps.height * pdfWidth) / imgProps.width;

      pdf.addImage(imgData, 'PNG', 0, 0, pdfWidth, pdfHeight);
      pdf.save(`${filename}.pdf`);
    } catch (err) {
      console.error('PDF Export Error', err);
      alert('Failed to generate PDF. Please check console.');
    }
  }

  // ── Delete ───────────────────────────────────────────────────
  deleteLog(id: number) { this.deleteConfirm = { type: 'ENTRY', id }; }

  deleteAllLogs() {
    if (!this.selectedTracker) return;
    if (confirm(`Are you sure you want to DELETE ALL logs in ${this.selectedTracker.name}?\n\nThis action cannot be undone.`)) {
      this.omniService.deleteAllEntries(this.selectedTracker.id!).subscribe({
        next: () => {
          this.entries = [];
          this.calculateAppStats(this.selectedApp!.id!);
          alert('All logs have been cleared.');
        },
        error: (err) => {
          console.error(err);
          alert('Failed to delete logs.');
        }
      });
    }
  }

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

  getIcon(type: string): string {
    switch(type) {
      case 'FINANCE': return '🏦';
      case 'HEALTH': return '🍎';
      case 'STOCK': return '📈';
      default: return '⚙️';
    }
  }

  // ── Plaid Integrations ───────────────────────────────────────
  connectPlaid() {
    if (!this.selectedTracker) return;
    this.isLoading = true;
    this.omniService.createPlaidLinkToken().subscribe({
      next: (res: any) => {
        this.isLoading = false;
        const handler = Plaid.create({
          token: res.link_token,
          onSuccess: (public_token: string, metadata: any) => {
            const institutionName = metadata?.institution?.name || 'Your Bank';
            this.connectedInstitution = institutionName;
            console.log("Plaid connected to: " + institutionName + ", exchanging token...");
            this.omniService.exchangePlaidToken(this.selectedTracker!.id!, public_token, institutionName).subscribe({
              next: () => {
                // Now call AI to auto-suggest field mappings
                this.isAutoMapping = true;
                this.isPlaidMappingModalOpen = true;
                this.omniService.getSuggestedPlaidMapping(this.selectedTracker!.id!).subscribe({
                  next: (suggestedMapping) => {
                    this.isAutoMapping = false;
                    // Pre-fill any matched mappings from AI
                    if (suggestedMapping && Object.keys(suggestedMapping).length > 0) {
                      this.plaidMapping = { ...this.plaidMapping, ...suggestedMapping };
                      this.isAutoMapped = true;
                    }
                  },
                  error: () => {
                    this.isAutoMapping = false; // Silently fail — user can still map manually
                  }
                });
              },
              error: err => {
                console.error(err);
                alert('Failed to exchange token.');
              }
            });
          },
          onLoad: () => {},
          onExit: (err: any, metadata: any) => { console.log('Exit Plaid', err); }
        });
        handler.open();
      },
      error: (err) => {
        this.isLoading = false;
        console.error(err);
        alert('Make sure your Plaid API Sandbox keys are set in your Profile Settings.');
      }
    });
  }

  savePlaidMapping() {
    if (!this.selectedTracker) return;
    this.isLoading = true;
    this.omniService.setPlaidMapping(this.selectedTracker.id!, this.plaidMapping).subscribe({
      next: () => {
        // Sync immediately after mapping
        this.omniService.syncPlaidTransactions(this.selectedTracker!.id!).subscribe({
          next: (res: any) => {
            this.isLoading = false;
            this.isPlaidMappingModalOpen = false;
            this.importResult = { entryCount: res.addedCount, skippedCount: 0 };
            this.calculateAppStats(this.selectedApp!.id!); // Refresh data
            // Refresh entries if on detail view
            this.omniService.getEntries(this.selectedTracker!.id!).subscribe(entries => this.entries = entries);
          },
          error: (err) => {
            this.isLoading = false;
            console.error(err);
            alert('Failed to sync bank transactions.');
          }
        });
      },
      error: (err) => {
        this.isLoading = false;
        console.error(err);
        alert('Failed to save mapping.');
      }
    });
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
    if (val === undefined || val === null || val === '' || val === 'null' || val === 'undefined') return '—';
    
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
