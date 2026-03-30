import { Component, OnInit } from '@angular/core';
import { RouterLink, Router, ActivatedRoute } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { Department, DepartmentService } from '../department.service';

@Component({
  selector: 'app-department-form',
  standalone: true,
  imports: [RouterLink, FormsModule],
  templateUrl: './department-form.component.html'
})
export class DepartmentFormComponent implements OnInit {
  isEditMode = false;
  departmentId?: number;
  errorMessage: string = '';

  department: Department = {
    name: '',
    description: ''
  };

  constructor(
    private service: DepartmentService, 
    private router: Router,
    private route: ActivatedRoute
  ) {}

  ngOnInit() {
    const idParam = this.route.snapshot.paramMap.get('id');
    if (idParam) {
      this.isEditMode = true;
      this.departmentId = +idParam;
      this.service.getDepartment(this.departmentId).subscribe(data => this.department = data);
    }
  }

  saveDepartment() {
    this.errorMessage = '';
    if (this.isEditMode && this.departmentId) {
      this.service.updateDepartment(this.departmentId, this.department).subscribe({
        next: () => this.router.navigate(['/departments']),
        error: (err) => {
          console.error(err);
          this.errorMessage = err.error?.message || 'Failed to update department. Please check your inputs.';
        }
      });
    } else {
      this.service.createDepartment(this.department).subscribe({
        next: () => this.router.navigate(['/departments']),
        error: (err) => {
          console.error(err);
          this.errorMessage = err.error?.message || 'Failed to create department. Please check your inputs.';
        }
      });
    }
  }
}
