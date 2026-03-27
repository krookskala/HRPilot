import { Component, inject, OnInit, ChangeDetectorRef } from "@angular/core";
import { EmployeeService } from "../../core/services/employee.service";
import { Employee } from "../../shared/models/employee.model";
import { MatTableModule } from "@angular/material/table";
import { MatButtonModule } from "@angular/material/button";

@Component({
    selector: 'app-employee-list',
    standalone: true,
    imports: [MatTableModule, MatButtonModule],
    templateUrl: './employee-list.html',
    styleUrl: './employee-list.scss'
})

export class EmployeeList implements OnInit {
    private employeeService = inject(EmployeeService);
    private cdr = inject(ChangeDetectorRef);
    employees: Employee[] = [];
    displayedColumns = ['id', 'firstName', 'lastName', 'position', 'salary', 'actions'];

    ngOnInit(): void {
        this.loadEmployees();
    }

    loadEmployees() {
        this.employeeService.getAll().subscribe({
            next: (data) => { 
                this.employees = data;
                this.cdr.detectChanges();
            }
        }); 
    }

    delete(id: number) {
        this.employeeService.deleteEmployee(id).subscribe({
            next: () => { this.loadEmployees(); }
        });
    }
}