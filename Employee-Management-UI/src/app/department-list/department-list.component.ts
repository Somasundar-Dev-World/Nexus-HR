import { Component, OnInit } from '@angular/core';
import { RouterLink } from '@angular/router';
import { Department, DepartmentService } from '../department.service';

@Component({
  selector: 'app-department-list',
  standalone: true,
  imports: [RouterLink],
  templateUrl: './department-list.component.html',
  styleUrl: './department-list.component.css'
})
export class DepartmentListComponent implements OnInit {
  departments: Department[] = [];

  constructor(private departmentService: DepartmentService) {}

  ngOnInit() {
    this.departmentService.getDepartments().subscribe({
      next: (data) => this.departments = data,
      error: (err) => console.error(err)
    });
  }
}
