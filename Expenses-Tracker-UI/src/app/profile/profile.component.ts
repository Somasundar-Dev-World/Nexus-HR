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
  profile = { 
    username: '', 
    name: '', 
    password: '', 
    geminiApiKey: '', 
    anthropicApiKey: '', 
    openaiApiKey: '',
    aiProvider: 'GOOGLE',
    aiModel: 'gemini-1.5-flash'
  };

  providers = [
    { id: 'GOOGLE', name: 'Google Gemini', models: ['gemini-2.0-flash', 'gemini-flash-latest', 'gemini-pro-latest'] },
    { id: 'ANTHROPIC', name: 'Anthropic Claude', models: ['claude-3-5-sonnet-20241022', 'claude-3-haiku-20240307', 'claude-3-opus-20240229'] },
    { id: 'OPENAI', name: 'OpenAI (GPT)', models: ['gpt-4o', 'gpt-4o-mini', 'gpt-4-turbo', 'gpt-3.5-turbo'] }
  ];

  selectedProviderModels: string[] = [];

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
        this.profile.openaiApiKey = data.openaiApiKey;
        this.profile.aiProvider = data.aiProvider || 'GOOGLE';
        this.profile.aiModel = data.aiModel || 'gemini-1.5-flash';
        this.updateModelList();
      },
      error: (err) => console.error(err)
    });
  }

  onProviderChange() {
    this.updateModelList();
    // Default to the first model in the list when provider changes
    if (this.selectedProviderModels.length > 0) {
      this.profile.aiModel = this.selectedProviderModels[0];
    }
  }

  updateModelList() {
    const provider = this.providers.find(p => p.id === this.profile.aiProvider);
    this.selectedProviderModels = provider ? provider.models : [];
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
