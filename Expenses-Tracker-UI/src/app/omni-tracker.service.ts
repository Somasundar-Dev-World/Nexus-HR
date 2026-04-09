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

export interface SmartInsight {
  type: 'METRIC' | 'ADVICE' | 'ALERT' | 'CHART';
  title: string;
  value: string;
  subtitle: string;
  icon: string;
  color: 'success' | 'warning' | 'danger' | 'primary' | 'magic';
  priority: number;
  reasoning?: string;
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

  updateApp(id: number, app: OmniApp): Observable<OmniApp> {
    return this.http.put<OmniApp>(`${this.apiUrl}/apps/${id}`, app);
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

  updateTracker(id: number, tracker: Tracker): Observable<Tracker> { 
    return this.http.put<Tracker>(`${this.apiUrl}/trackers/${id}`, tracker); 
  }

  deleteTracker(id: number): Observable<any> { 
    return this.http.delete(`${this.apiUrl}/trackers/${id}`); 
  }

  importTracker(file: File, appId: number, trackerName?: string): Observable<any> {
    const formData = new FormData();
    formData.append('file', file);
    formData.append('appId', appId.toString());
    if (trackerName) {
      formData.append('trackerName', trackerName);
    }
    return this.http.post<any>(`${this.apiUrl}/trackers/import`, formData);
  }

  importEntries(file: File, trackerId: number): Observable<any> {
    const formData = new FormData();
    formData.append('file', file);
    formData.append('trackerId', trackerId.toString());
    return this.http.post<any>(`${this.apiUrl}/entries/import`, formData);
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

  deleteAllEntries(trackerId: number): Observable<any> {
    return this.http.delete(`${this.apiUrl}/entries/tracker/${trackerId}`);
  }

  getAiInsights(appId: number, refresh: boolean = false): Observable<SmartInsight[]> {
    let params = new HttpParams();
    if (refresh) params = params.set('refresh', 'true');
    return this.http.get<SmartInsight[]>(`${this.apiUrl}/apps/${appId}/insights`, { params });
  }

  // --- Plaid Integrations (Plugin Model) ---
  createPlaidLinkToken(): Observable<any> {
    return this.http.post<any>(`${this.apiUrl}/integrations/plaid/link-token`, {});
  }

  exchangePlaidToken(trackerId: number, publicToken: string, institutionName: string): Observable<any> {
    return this.http.post<any>(`${this.apiUrl}/integrations/plaid/exchange/${trackerId}`, { public_token: publicToken, institution_name: institutionName });
  }

  getSuggestedPlaidMapping(trackerId: number): Observable<{ [key: string]: string }> {
    return this.http.get<{ [key: string]: string }>(`${this.apiUrl}/integrations/plaid/suggest-mapping/${trackerId}`);
  }

  setPlaidMapping(trackerId: number, mapping: any): Observable<any> {
    return this.http.post<any>(`${this.apiUrl}/integrations/plaid/mapping/${trackerId}`, mapping);
  }

  syncPlaidTransactions(trackerId: number): Observable<any> {
    return this.http.post<any>(`${this.apiUrl}/integrations/plaid/sync/${trackerId}`, {});
  }
}
