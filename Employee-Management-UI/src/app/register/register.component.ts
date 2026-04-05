import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router, RouterModule } from '@angular/router';
import { AuthService } from '../auth.service';

@Component({
  selector: 'app-register',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterModule],
  templateUrl: './register.component.html',
  styleUrl: './register.component.css'
})
export class RegisterComponent {
  userData = { username: '', password: '', name: '' };
  error = '';
  loading = false;

  constructor(private authService: AuthService, private router: Router) {}

  onSubmit() {
    this.loading = true;
    this.error = '';
    this.authService.register(this.userData).subscribe({
      next: () => this.router.navigate(['/dashboard']),
      error: (err) => {
        console.error('Registration error:', err);
        if (err.status === 0) {
          this.error = 'Could not connect to the authentication server. Please ensure the backend is running on port 8080.';
        } else {
          this.error = err.error?.message || err.error || 'Registration failed. Username might already exist.';
        }
        this.loading = false;
      }
    });
  }
}
