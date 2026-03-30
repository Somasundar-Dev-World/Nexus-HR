import { Component, OnInit } from '@angular/core';
import { RouterLink } from '@angular/router';
import { EmployeeService } from '../employee.service';

@Component({
  selector: 'app-dashboard',
  standalone: true,
  imports: [RouterLink],
  templateUrl: './dashboard.component.html',
  styleUrl: './dashboard.component.css'
})
export class DashboardComponent implements OnInit {
  stats: any = { totalEmployees: 0, departments: 0, newHires: 0 };
  userName: string = 'Admin';
  isLoading = true;

  constructor(private employeeService: EmployeeService) {}

  ngOnInit() {
    this.employeeService.getStats().subscribe({
      next: (data) => { this.stats = data; this.isLoading = false; },
      error: (err) => { console.error(err); this.isLoading = false; }
    });
  }
}
