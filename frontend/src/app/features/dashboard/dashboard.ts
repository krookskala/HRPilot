import { Component, inject, OnInit, ChangeDetectorRef } from "@angular/core";
import { EmployeeService } from "../../core/services/employee.service";
import { DepartmentService } from "../../core/services/department.service";
import { MatCardModule } from "@angular/material/card";
import { MatProgressSpinnerModule } from "@angular/material/progress-spinner";
import { NgIf } from "@angular/common";

@Component({
    selector: 'app-dashboard',
    standalone: true,
    imports: [MatCardModule, MatProgressSpinnerModule, NgIf],
    templateUrl: './dashboard.html',
    styleUrl: './dashboard.scss'
})

export class Dashboard implements OnInit {
    private employeeService = inject(EmployeeService);
    private departmentService = inject(DepartmentService);
    private cdr = inject(ChangeDetectorRef);

    employeeCount = 0;
    departmentCount = 0;
    loading = true;

    ngOnInit(): void {
        this.employeeService.getAll().subscribe({
            next: (page) => {
                this.employeeCount = page.totalElements;
                this.checkLoading();
            },
            error: () => { this.checkLoading(); }
        });
        this.departmentService.getAll().subscribe({
            next: (page) => {
                this.departmentCount = page.totalElements;
                this.checkLoading();
            },
            error: () => { this.checkLoading(); }
        });
    }

    private checkLoading() {
        this.loading = false;
        this.cdr.detectChanges();
    }
}
