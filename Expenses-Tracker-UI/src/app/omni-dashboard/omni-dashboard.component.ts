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
    this.isAddingEntry = true;
  }

  addEntry() {
    if (!this.selectedTracker || this.entryForm.invalid) return;
    const entry: TrackerEntry = {
      trackerId: this.selectedTracker.id!,
      fieldValues: this.entryForm.get('fieldValues')?.value,
      note: this.newEntryNote,
      date: new Date().toISOString()
    };
    this.omniService.addEntry(entry).subscribe(saved => {
      this.entries.push(saved);
      this.isAddingEntry = false;
    });
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

  deleteTracker(id: number, event: Event) {
    event.stopPropagation();
    if (confirm('Delete this tracker and all its logs?')) {
      this.omniService.deleteTracker(id).subscribe(() => {
        this.trackers = this.trackers.filter(t => t.id !== id);
        if (this.selectedTracker?.id === id) this.selectedTracker = undefined;
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
}
