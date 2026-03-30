package com.example.employee_service;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.HashMap;

@CrossOrigin(origins = "http://localhost:4200")
@RestController
@RequestMapping("/api/employees")
public class EmployeeController {

    private final EmployeeRepository repository;
    private final DepartmentRepository departmentRepository;

    public EmployeeController(EmployeeRepository repository, DepartmentRepository departmentRepository) {
        this.repository = repository;
        this.departmentRepository = departmentRepository;
    }

    @GetMapping
    public List<Employee> getAllEmployees() {
        return repository.findAll();
    }

    @GetMapping("/{id}")
    public ResponseEntity<Employee> getEmployeeById(@PathVariable Long id) {
        return repository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<Employee> createEmployee(@Valid @RequestBody Employee employee) {
        if(employee.getStatus() == null) employee.setStatus("Active");
        if(employee.getRole() == null) employee.setRole("Member");
        if(employee.getDepartment() != null && employee.getDepartment().getId() != null) {
            departmentRepository.findById(employee.getDepartment().getId()).ifPresent(employee::setDepartment);
        }
        Employee saved = repository.save(employee);
        return new ResponseEntity<>(saved, HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Employee> updateEmployee(@PathVariable Long id, @Valid @RequestBody Employee employeeDetails) {
        return repository.findById(id)
                .map(employee -> {
                    employee.setFirstName(employeeDetails.getFirstName());
                    employee.setLastName(employeeDetails.getLastName());
                    employee.setEmail(employeeDetails.getEmail());
                    employee.setPhone(employeeDetails.getPhone());
                    if(employeeDetails.getDepartment() != null && employeeDetails.getDepartment().getId() != null) {
                        departmentRepository.findById(employeeDetails.getDepartment().getId()).ifPresent(employee::setDepartment);
                    } else {
                        employee.setDepartment(null);
                    }
                    employee.setJobTitle(employeeDetails.getJobTitle());
                    employee.setStatus(employeeDetails.getStatus());
                    employee.setRole(employeeDetails.getRole());
                    return ResponseEntity.ok(repository.save(employee));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteEmployee(@PathVariable Long id) {
        return repository.findById(id)
                .map(employee -> {
                    repository.delete(employee);
                    return new ResponseEntity<Void>(HttpStatus.NO_CONTENT);
                })
                .orElse(ResponseEntity.notFound().build());
    }
    
    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getStats() {
        long count = repository.count();
        long deptCount = departmentRepository.count();
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalEmployees", count);
        stats.put("departments", deptCount); 
        stats.put("newHires", 3); 
        return ResponseEntity.ok(stats);
    }
}
