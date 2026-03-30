import { Component, OnInit } from '@angular/core';
import { RouterLink, Router, ActivatedRoute } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { Employee, EmployeeService } from '../employee.service';
import { Department, DepartmentService } from '../department.service';

@Component({
  selector: 'app-employee-form',
  standalone: true,
  imports: [RouterLink, FormsModule],
  templateUrl: './employee-form.component.html',
  styleUrl: './employee-form.component.css'
})
export class EmployeeFormComponent implements OnInit {
  isEditMode = false;
  employeeId?: number;
  departments: Department[] = [];

  employee: Employee = {
    firstName: '',
    lastName: '',
    email: '',
    phone: '',
    department: { id: 1 }, 
    jobTitle: '',
    status: 'Active',
    role: 'Member'
  };

  constructor(
    private employeeService: EmployeeService, 
    private departmentService: DepartmentService,
    private router: Router,
    private route: ActivatedRoute
  ) {}

  ngOnInit() {
    this.departmentService.getDepartments().subscribe(deps => {
       this.departments = deps;
       if(deps.length > 0 && !this.isEditMode) {
          this.employee.department = { id: deps[0].id! };
       }
    });

    const idParam = this.route.snapshot.paramMap.get('id');
    if (idParam) {
      this.isEditMode = true;
      this.employeeId = +idParam;
      this.employeeService.getEmployee(this.employeeId).subscribe({
        next: (data) => {
          this.employee = data;
          if (!this.employee.department) {
             this.employee.department = { id: this.departments.length > 0 ? this.departments[0].id : 1 };
          }
        },
        error: (err) => console.error(err)
      });
    }
  }

  saveEmployee() {
    if (this.isEditMode && this.employeeId) {
      this.employeeService.updateEmployee(this.employeeId, this.employee).subscribe({
        next: () => this.router.navigate(['/employees']),
        error: (err) => console.error(err)
      });
    } else {
      this.employeeService.createEmployee(this.employee).subscribe({
        next: () => this.router.navigate(['/employees']),
        error: (err) => console.error(err)
      });
    }
  }
}
