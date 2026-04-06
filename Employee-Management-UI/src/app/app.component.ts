import { Component, computed, HostListener } from '@angular/core';
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
  title = 'Employee-Management-UI';
  isSidebarOpen = false;
  isProfileMenuOpen = false;

  currentUser = this.authService.currentUser;
  isLoggedIn = computed(() => !!this.currentUser());

  constructor(public authService: AuthService, private router: Router) {}

  @HostListener('document:click', ['$event'])
  onDocumentClick(event: MouseEvent) {
    const target = event.target as HTMLElement;
    if (!target.closest('.user-profile')) {
      this.isProfileMenuOpen = false;
    }
  }

  hideNav() {
    const url = this.router.url;
    return url.includes('/login') || url.includes('/register');
  }

  logout() {
    this.authService.logout();
    this.isProfileMenuOpen = false;
  }

  toggleSidebar() {
    this.isSidebarOpen = !this.isSidebarOpen;
  }

  toggleProfileMenu(event: MouseEvent) {
    event.stopPropagation();
    this.isProfileMenuOpen = !this.isProfileMenuOpen;
  }

  closeSidebar() {
    this.isSidebarOpen = false;
  }
}
