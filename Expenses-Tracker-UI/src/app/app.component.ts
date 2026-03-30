import { Component } from '@angular/core';
import { RouterOutlet, RouterLink, RouterLinkActive, Router } from '@angular/router';
import { CommonModule } from '@angular/common';
import { AuthService } from './auth.service';

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [RouterOutlet, RouterLink, RouterLinkActive, CommonModule],
  templateUrl: './app.component.html',
  styleUrl: './app.component.css'
})
export class AppComponent {
  title = 'Expenses-Tracker-UI';
  isMobileMenuOpen = false;
  isDropdownOpen = false;
  
  constructor(public authService: AuthService, private router: Router) {}

  toggleMenu() {
    this.isMobileMenuOpen = !this.isMobileMenuOpen;
    if (this.isMobileMenuOpen) this.isDropdownOpen = false;
  }

  toggleDropdown() {
    this.isDropdownOpen = !this.isDropdownOpen;
    if (this.isDropdownOpen) this.isMobileMenuOpen = false;
  }

  closeAll(event: Event) {
    // Close both menus when clicking outside
    this.isDropdownOpen = false;
    // Don't close sidebar — it has its own overlay
  }

  logout() {
    this.authService.logout();
    this.isMobileMenuOpen = false;
    this.isDropdownOpen = false;
    this.router.navigate(['/login']);
  }
}
