import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { OmniTrackerService, Tracker, TrackerEntry } from '../omni-tracker.service';
import { forkJoin } from 'rxjs';

@Component({
  selector: 'app-omni-dashboard',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './omni-dashboard.component.html',
  styleUrl: './omni-dashboard.component.css'
})
export class OmniDashboardComponent implements OnInit {
  trackers: Tracker[] = [];
  selectedTracker?: Tracker;
  entries: TrackerEntry[] = [];
  
  newTracker: Tracker = { name: '', unit: '', type: 'FINANCE' };
  newEntryValue: number = 0;
  newEntryNote: string = '';

  isLoading = true;
  isAddingTracker = false;
  isAddingEntry = false;

  constructor(private omniService: OmniTrackerService) {}

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

  createTracker() {
    if (!this.newTracker.name) return;
    this.omniService.createTracker(this.newTracker).subscribe(tracker => {
      this.trackers.push(tracker);
      this.isAddingTracker = false;
      this.newTracker = { name: '', unit: '', type: 'FINANCE' };
    });
  }

  addEntry() {
    if (!this.selectedTracker || this.newEntryValue === null) return;
    const entry: TrackerEntry = {
      trackerId: this.selectedTracker.id!,
      value: this.newEntryValue,
      note: this.newEntryNote,
      date: new Date().toISOString()
    };
    this.omniService.addEntry(entry).subscribe(saved => {
      this.entries.push(saved);
      this.isAddingEntry = false;
      this.newEntryValue = 0;
      this.newEntryNote = '';
    });
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
