import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { HttpClient } from '@angular/common/http';
import { AuthService } from '../auth.service';
import { environment } from '../../environments/environment';

@Component({
  selector: 'app-profile',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './profile.component.html',
  styleUrl: '../login/login.component.css'
})
export class ProfileComponent implements OnInit {
  profile = { username: '', name: '', password: '', geminiApiKey: '', anthropicApiKey: '' };
  isLoading = false;
  successMsg = '';
  errorMsg = '';

  constructor(private http: HttpClient, private authService: AuthService) {}

  ngOnInit() {
    this.http.get<any>(`${environment.apiUrl}/user/profile`).subscribe({
      next: (data) => {
        this.profile.username = data.username;
        this.profile.name = data.name;
        this.profile.geminiApiKey = data.geminiApiKey;
        this.profile.anthropicApiKey = data.anthropicApiKey;
      },
      error: (err) => console.error(err)
    });
  }

  onSubmit() {
    this.isLoading = true;
    this.successMsg = '';
    this.errorMsg = '';
    
    this.http.put<any>(`${environment.apiUrl}/user/profile`, this.profile).subscribe({
      next: (data) => {
        this.isLoading = false;
        this.successMsg = 'Profile updated successfully!';
        
        this.authService.updateProfileContext(data.name);
      },
      error: (err) => {
        this.isLoading = false;
        this.errorMsg = 'Failed to update profile.';
      }
    });
  }
}
