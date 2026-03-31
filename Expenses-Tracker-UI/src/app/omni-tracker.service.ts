import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../environments/environment';

export interface Tracker {
  id?: number;
  name: string;
  unit: string;
  type: 'FINANCE' | 'HEALTH' | 'STOCK' | 'CUSTOM';
  metadata?: string;
  userId?: number;
}

export interface TrackerEntry {
  id?: number;
  value: number;
  date: string;
  note?: string;
  trackerId: number;
  userId?: number;
}

@Injectable({ providedIn: 'root' })
export class OmniTrackerService {
  private apiUrl = `${environment.apiUrl}/omni`;

  constructor(private http: HttpClient) { }

  // --- Trackers ---
  getTrackers(): Observable<Tracker[]> { 
    return this.http.get<Tracker[]>(`${this.apiUrl}/trackers`); 
  }

  getTrackersByType(type: string): Observable<Tracker[]> { 
    return this.http.get<Tracker[]>(`${this.apiUrl}/trackers/type/${type}`); 
  }

  createTracker(tracker: Tracker): Observable<Tracker> { 
    return this.http.post<Tracker>(`${this.apiUrl}/trackers`, tracker); 
  }

  deleteTracker(id: number): Observable<any> { 
    return this.http.delete(`${this.apiUrl}/trackers/${id}`); 
  }

  // --- Entries ---
  getEntries(trackerId: number): Observable<TrackerEntry[]> { 
    return this.http.get<TrackerEntry[]>(`${this.apiUrl}/entries/tracker/${trackerId}`); 
  }

  addEntry(entry: TrackerEntry): Observable<TrackerEntry> { 
    return this.http.post<TrackerEntry>(`${this.apiUrl}/entries`, entry); 
  }

  deleteEntry(id: number): Observable<any> { 
    return this.http.delete(`${this.apiUrl}/entries/${id}`); 
  }
}
