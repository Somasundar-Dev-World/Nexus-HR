import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router, RouterModule } from '@angular/router';
import { AuthService } from '../auth.service';

@Component({
  selector: 'app-landing',
  standalone: true,
  imports: [CommonModule, RouterModule],
  templateUrl: './landing.component.html',
  styleUrl: './landing.component.css'
})
export class LandingComponent implements OnInit {

  constructor(private authService: AuthService, private router: Router) {}

  ngOnInit() {
    // Auto-forward if already logged in
    if (this.authService.isAuthenticated) {
      this.router.navigate(['/dashboard']);
    }
  }
}
