import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { BehaviorSubject, Observable, tap } from 'rxjs';
import { environment } from '../environments/environment';

export interface User { username: string; name: string; }
export interface AuthResponse { token: string; username: string; name: string; }

@Injectable({ providedIn: 'root' })
export class AuthService {
  private apiUrl = `${environment.apiUrl}/auth`;
  private currentUserSubject = new BehaviorSubject<User | null>(null);

  constructor(private http: HttpClient) {
    const token = localStorage.getItem('token');
    const username = localStorage.getItem('username');
    const name = localStorage.getItem('name');
    if (token && username && name) {
      this.currentUserSubject.next({ username, name });
    }
  }

  public get currentUserValue(): User | null { return this.currentUserSubject.value; }
  public get isAuthenticated(): boolean { return localStorage.getItem('token') !== null; }

  login(credentials: {username: string, password: string}): Observable<AuthResponse> {
    return this.http.post<AuthResponse>(`${this.apiUrl}/login`, credentials)
      .pipe(tap(res => this.setSession(res)));
  }

  register(profile: {username: string, password: string, name: string}): Observable<AuthResponse> {
    return this.http.post<AuthResponse>(`${this.apiUrl}/register`, profile)
      .pipe(tap(res => this.setSession(res)));
  }

  private setSession(authResult: AuthResponse) {
    localStorage.setItem('token', authResult.token);
    localStorage.setItem('username', authResult.username);
    localStorage.setItem('name', authResult.name);
    this.currentUserSubject.next({ username: authResult.username, name: authResult.name });
  }

  logout() {
    this.http.post(`${this.apiUrl}/logout`, {})
      .subscribe({ error: (err) => console.error('Logout error', err) });
      
    localStorage.removeItem('token');
    localStorage.removeItem('username');
    localStorage.removeItem('name');
    this.currentUserSubject.next(null);
  }

  updateProfileContext(name: string) {
    const current = this.currentUserSubject.value;
    if (current) {
      localStorage.setItem('name', name);
      this.currentUserSubject.next({ ...current, name });
    }
  }
}
