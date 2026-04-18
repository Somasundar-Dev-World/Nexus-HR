import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../environments/environment';

@Injectable({
  providedIn: 'root'
})
export class OmniQueryService {
  private apiUrl = `${environment.apiUrl}/api/omni/query`;

  constructor(private http: HttpClient) { }

  executeQuery(query: string): Observable<any[]> {
    return this.http.post<any[]>(`${this.apiUrl}/execute`, { query });
  }

  saveReport(report: any): Observable<any> {
    return this.http.post<any>(`${this.apiUrl}/save-report`, report);
  }

  runReport(reportId: number): Observable<any> {
    return this.http.get<any>(`${this.apiUrl}/run-report/${reportId}`);
  }
}
