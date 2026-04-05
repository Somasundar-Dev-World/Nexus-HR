import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../environments/environment';
import { OmniApp } from './omni-app.model';

export interface Tracker {
  id?: number;
  name: string;
  fieldDefinitions: any[];
  type: 'FINANCE' | 'HEALTH' | 'STOCK' | 'CUSTOM';
  metadata?: string;
  icon?: string;
  userId?: number;
  appId?: number;
}

export interface TrackerEntry {
  id?: number;
  fieldValues: any;
  date: string;
  note?: string;
  trackerId: number;
  userId?: number;
}

@Injectable({ providedIn: 'root' })
export class OmniTrackerService {
  private apiUrl = `${environment.apiUrl}/omni`;

  constructor(private http: HttpClient) { }

  // --- Apps ---
  getApps(): Observable<OmniApp[]> {
    return this.http.get<OmniApp[]>(`${this.apiUrl}/apps`);
  }

  createApp(app: OmniApp): Observable<OmniApp> {
    return this.http.post<OmniApp>(`${this.apiUrl}/apps`, app);
  }

  deleteApp(id: number): Observable<any> {
    return this.http.delete(`${this.apiUrl}/apps/${id}`);
  }

  // --- Trackers ---
  getTrackers(appId?: number): Observable<Tracker[]> { 
    let params = new HttpParams();
    if (appId) params = params.set('appId', appId.toString());
    return this.http.get<Tracker[]>(`${this.apiUrl}/trackers`, { params }); 
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

  updateEntry(id: number, entry: TrackerEntry): Observable<TrackerEntry> {
    return this.http.put<TrackerEntry>(`${this.apiUrl}/entries/${id}`, entry);
  }

  deleteEntry(id: number): Observable<any> { 
    return this.http.delete(`${this.apiUrl}/entries/${id}`); 
  }
}
