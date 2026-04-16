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
  isDropdownOpen = false;

  constructor(public authService: AuthService, private router: Router) {}

  toggleDropdown() {
    this.isDropdownOpen = !this.isDropdownOpen;
  }

  isPublicPage(): boolean {
    const publicRoutes = ['/', '/home', '/author'];
    return publicRoutes.includes(this.router.url);
  }

  closeAll(event: Event) {
    this.isDropdownOpen = false;
  }

  logout() {
    this.authService.logout();
    this.isDropdownOpen = false;
    this.router.navigate(['/']);
  }

  navigateToDocs() {
    let topic = 'default';
    if (this.router.url.includes('/track')) topic = 'track';
    if (this.router.url.includes('deep-research')) topic = 'deep-research';
    if (this.router.url.includes('/profile')) topic = 'profile';

    this.router.navigate(['/docs'], { queryParams: { topic } });
  }
}
