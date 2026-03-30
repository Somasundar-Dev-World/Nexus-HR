import { Component, OnInit } from '@angular/core';
import { RouterLink } from '@angular/router';
import { Employee, EmployeeService } from '../employee.service';

@Component({
  selector: 'app-employee-list',
  standalone: true,
  imports: [RouterLink],
  templateUrl: './employee-list.component.html',
  styleUrl: './employee-list.component.css'
})
export class EmployeeListComponent implements OnInit {
  employees: Employee[] = [];
  isLoading = true;

  constructor(private employeeService: EmployeeService) {}

  ngOnInit() {
    this.employeeService.getEmployees().subscribe({
      next: (data) => { this.employees = data; this.isLoading = false; },
      error: (err) => { console.error(err); this.isLoading = false; }
    });
  }
}
