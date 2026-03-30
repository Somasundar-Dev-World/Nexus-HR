import { Injectable } from '@angular/core';
import { Router, UrlTree } from '@angular/router';
import { AuthService } from './auth.service';

@Injectable({ providedIn: 'root' })
export class AuthGuard {
  constructor(private router: Router, private authService: AuthService) {}

  canActivate(): boolean | UrlTree {
    if (this.authService.isAuthenticated) {
      return true;
    }
    return this.router.parseUrl('/login');
  }
}
