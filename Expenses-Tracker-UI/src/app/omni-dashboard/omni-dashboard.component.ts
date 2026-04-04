import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule, ReactiveFormsModule, FormBuilder, FormGroup, FormArray, Validators } from '@angular/forms';
import { OmniTrackerService, Tracker, TrackerEntry } from '../omni-tracker.service';

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
  
  // Reactive forms for dynamic trackers
  trackerForm: FormGroup;
  entryForm: FormGroup;

  newEntryNote: string = '';

  isLoading = true;
  isAddingTracker = false;
  isAddingEntry = false;
  editingEntryId: number | null = null;
  deleteConfirm: { type: 'TRACKER' | 'ENTRY', id: number } | null = null;

  constructor(private omniService: OmniTrackerService, private fb: FormBuilder) {
    this.trackerForm = this.fb.group({
      name: ['', Validators.required],
      type: ['FINANCE'],
      fieldDefinitions: this.fb.array([])
    });

    this.entryForm = this.fb.group({
      fieldValues: this.fb.group({})
    });
  }

  ngOnInit() {
    this.loadData();
  }

  loadData() {
    this.isLoading = true;
    this.omniService.getTrackers().subscribe({
      next: (data) => {
        this.trackers = data;
        this.isLoading = false;
      },
      error: () => this.isLoading = false
    });
  }

  selectTracker(tracker: Tracker) {
    this.selectedTracker = tracker;
    this.loadEntries(tracker.id!);
  }

  loadEntries(id: number) {
    this.omniService.getEntries(id).subscribe(data => {
      this.entries = data;
    });
  }

  get fieldDefinitions() {
    return this.trackerForm.get('fieldDefinitions') as FormArray;
  }

  addField() {
    this.fieldDefinitions.push(this.fb.group({
      name: ['', Validators.required],
      type: ['NUMBER', Validators.required],
      unit: ['']
    }));
  }

  removeField(index: number) {
    this.fieldDefinitions.removeAt(index);
  }

  openAddTracker() {
    this.trackerForm.reset({ type: 'FINANCE' });
    this.fieldDefinitions.clear();
    this.addField(); // Start with one field by default
    this.isAddingTracker = true;
  }

  createTracker() {
    if (this.trackerForm.invalid) return;
    const trackerData: Tracker = this.trackerForm.value;
    this.omniService.createTracker(trackerData).subscribe(tracker => {
      this.trackers.push(tracker);
      this.isAddingTracker = false;
    });
  }

  openAddEntry() {
    if (!this.selectedTracker) return;
    const formGroup = this.fb.group({});
    this.selectedTracker.fieldDefinitions?.forEach(f => {
      formGroup.addControl(f.name, this.fb.control('', Validators.required));
    });
    this.entryForm.setControl('fieldValues', formGroup);
    this.newEntryNote = '';
    this.editingEntryId = null;
    this.isAddingEntry = true;
  }

  editLog(entry: TrackerEntry) {
    if (!this.selectedTracker) return;
    const formGroup = this.fb.group({});
    this.selectedTracker.fieldDefinitions?.forEach(f => {
      const existingVal = entry.fieldValues ? entry.fieldValues[f.name] : '';
      formGroup.addControl(f.name, this.fb.control(existingVal || '', Validators.required));
    });
    this.entryForm.setControl('fieldValues', formGroup);
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
        const index = this.entries.findIndex(e => e.id === saved.id);
        if (index !== -1) {
          this.entries[index] = saved;
        }
        this.isAddingEntry = false;
        this.editingEntryId = null;
      });
    } else {
      this.omniService.addEntry(entryData).subscribe(saved => {
        this.entries.push(saved);
        this.isAddingEntry = false;
      });
    }
  }

  deleteLog(id: number) {
    this.deleteConfirm = { type: 'ENTRY', id };
  }
  
  deleteTracker(id: number, event: Event) {
    event.stopPropagation();
    this.deleteConfirm = { type: 'TRACKER', id };
  }

  cancelDelete() {
    this.deleteConfirm = null;
  }

  confirmDelete() {
    if (!this.deleteConfirm) return;
    
    if (this.deleteConfirm.type === 'TRACKER') {
      this.omniService.deleteTracker(this.deleteConfirm.id).subscribe(() => {
        this.trackers = this.trackers.filter(t => t.id !== this.deleteConfirm?.id);
        if (this.selectedTracker?.id === this.deleteConfirm?.id) this.selectedTracker = undefined;
        this.deleteConfirm = null;
      });
    } else if (this.deleteConfirm.type === 'ENTRY') {
      this.omniService.deleteEntry(this.deleteConfirm.id).subscribe(() => {
        this.entries = this.entries.filter(e => e.id !== this.deleteConfirm?.id);
        this.deleteConfirm = null;
      });
    }
  }
  
  getMainMetricKey(tracker: Tracker): string {
      if (tracker.fieldDefinitions && tracker.fieldDefinitions.length > 0) {
          return tracker.fieldDefinitions[0].name;
      }
      return '';
  }
  
  getMainMetricUnit(tracker: Tracker): string {
      if (tracker.fieldDefinitions && tracker.fieldDefinitions.length > 0) {
          return tracker.fieldDefinitions[0].unit || '';
      }
      return '';
  }

  getIcon(type: string): string {
    switch(type) {
      case 'FINANCE': return '🏦';
      case 'HEALTH': return '🍎';
      case 'STOCK': return '📈';
      default: return '⚙️';
    }
  }
}
