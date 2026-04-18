import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../environments/environment';

@Injectable({
  providedIn: 'root'
})
export class OmniQueryService {
  private apiUrl = `${environment.apiUrl}/api/omni/query`;

  constructor(private http: HttpClient) { }

  executeQuery(query: string, userId: number): Observable<any[]> {
    const params = new HttpParams().set('userId', userId.toString());
    return this.http.post<any[]>(`${this.apiUrl}/execute`, { query }, { params });
  }

  saveReport(report: any, userId: number): Observable<any> {
    const params = new HttpParams().set('userId', userId.toString());
    return this.http.post<any>(`${this.apiUrl}/save-report`, report, { params });
  }

  runReport(reportId: number, userId: number): Observable<any> {
    const params = new HttpParams().set('userId', userId.toString());
    return this.http.get<any>(`${this.apiUrl}/run-report/${reportId}`, { params });
  }
}
