import { Component, inject, OnInit, ChangeDetectorRef } from "@angular/core";
import { EmployeeService } from "../../core/services/employee.service";
import { DepartmentService } from "../../core/services/department.service";
import { LeaveService } from "../../core/services/leave.service";
import { PayrollService } from "../../core/services/payroll.service";
import { MatCardModule } from "@angular/material/card";
import { MatProgressSpinnerModule } from "@angular/material/progress-spinner";
import { MatIconModule } from "@angular/material/icon";
import { MatButtonModule } from "@angular/material/button";
import { NgIf } from "@angular/common";
import { RouterLink } from "@angular/router";

@Component({
    selector: 'app-dashboard',
    standalone: true,
    imports: [MatCardModule, MatProgressSpinnerModule, MatIconModule, MatButtonModule, NgIf, RouterLink],
    templateUrl: './dashboard.html',
    styleUrl: './dashboard.scss'
})

export class Dashboard implements OnInit {
    private employeeService = inject(EmployeeService);
    private departmentService = inject(DepartmentService);
    private leaveService = inject(LeaveService);
    private payrollService = inject(PayrollService);
    private cdr = inject(ChangeDetectorRef);

    employeeCount = 0;
    departmentCount = 0;
    leaveCount = 0;
    payrollCount = 0;
    loading = true;
    private loadedCount = 0;

    ngOnInit(): void {
        this.employeeService.getAll().subscribe({
            next: (page) => { this.employeeCount = page.totalElements; this.tick(); },
            error: () => { this.tick(); }
        });
        this.departmentService.getAll().subscribe({
            next: (page) => { this.departmentCount = page.totalElements; this.tick(); },
            error: () => { this.tick(); }
        });
        this.leaveService.getAll().subscribe({
            next: (page) => { this.leaveCount = page.totalElements; this.tick(); },
            error: () => { this.tick(); }
        });
        this.payrollService.getAllPayrolls().subscribe({
            next: (page) => { this.payrollCount = page.totalElements; this.tick(); },
            error: () => { this.tick(); }
        });
    }

    private tick() {
        this.loadedCount++;
        if (this.loadedCount >= 4) {
            this.loading = false;
            this.cdr.detectChanges();
        }
    }
}
