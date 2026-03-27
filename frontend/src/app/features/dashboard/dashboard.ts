import { Component, inject, OnInit, ChangeDetectorRef } from "@angular/core";
import { EmployeeService } from "../../core/services/employee.service";
import { DepartmentService } from "../../core/services/department.service";
import { MatCardModule } from "@angular/material/card";

@Component({
    selector: 'app-dashboard',
    standalone: true,
    imports: [MatCardModule],
    templateUrl: './dashboard.html',
    styleUrl: './dashboard.scss'
})

export class Dashboard implements OnInit {
    private employeeService = inject(EmployeeService);
    private departmentService = inject(DepartmentService);
    private cdr = inject(ChangeDetectorRef);

    employeeCount = 0;
    departmentCount = 0;

    ngOnInit(): void {
        this.employeeService.getAll().subscribe({
            next: (data) => { 
                this.employeeCount = data.length;
                this.cdr.detectChanges();
            }
        });
        this.departmentService.getAll().subscribe({
            next: (data) => {
                this.departmentCount = data.length;
                this.cdr.detectChanges();    
            }
        });
    }
}