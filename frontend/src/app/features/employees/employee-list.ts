import { Component, inject, OnInit, ChangeDetectorRef } from "@angular/core";
import { EmployeeService } from "../../core/services/employee.service";
import { AuthService } from "../../core/services/auth.service";
import { Employee } from "../../shared/models/employee.model";
import { MatTableModule } from "@angular/material/table";
import { MatButtonModule } from "@angular/material/button";
import { MatDialog, MatDialogModule } from "@angular/material/dialog";
import { MatPaginatorModule, PageEvent } from "@angular/material/paginator";
import { MatProgressSpinnerModule } from "@angular/material/progress-spinner";
import { NgIf } from "@angular/common";
import { EmployeeDialog } from "./employee-dialog";
import { ConfirmDialog } from "../../shared/components/confirm-dialog/confirm-dialog";

@Component({
    selector: 'app-employee-list',
    standalone: true,
    imports: [MatTableModule, MatButtonModule, MatDialogModule, MatPaginatorModule, MatProgressSpinnerModule, NgIf],
    templateUrl: './employee-list.html',
    styleUrl: './employee-list.scss'
})

export class EmployeeList implements OnInit {
    private employeeService = inject(EmployeeService);
    private authService = inject(AuthService);
    private cdr = inject(ChangeDetectorRef);
    private dialog = inject(MatDialog);

    employees: Employee[] = [];
    displayedColumns = ['id', 'firstName', 'lastName', 'position', 'salary', 'actions'];
    canManage = this.authService.hasRole('ADMIN', 'HR_MANAGER');
    totalElements = 0;
    pageSize = 10;
    pageIndex = 0;
    loading = false;
    error = '';

    ngOnInit(): void {
        this.loadEmployees();
    }

    loadEmployees() {
        this.loading = true;
        this.error = '';
        this.employeeService.getAll(this.pageIndex, this.pageSize).subscribe({
            next: (page) => {
                this.employees = page.content;
                this.totalElements = page.totalElements;
                this.loading = false;
                this.cdr.detectChanges();
            },
            error: () => {
                this.error = 'Failed to load employees';
                this.loading = false;
                this.cdr.detectChanges();
            }
        });
    }

    onPageChange(event: PageEvent) {
        this.pageIndex = event.pageIndex;
        this.pageSize = event.pageSize;
        this.loadEmployees();
    }

    delete(id: number) {
        const ref = this.dialog.open(ConfirmDialog, {
            width: '350px',
            data: { title: 'Delete Employee', message: 'Are you sure you want to delete this employee?' }
        });
        ref.afterClosed().subscribe(confirmed => {
            if (confirmed) {
                this.employeeService.deleteEmployee(id).subscribe({
                    next: () => { this.loadEmployees(); }
                });
            }
        });
    }

    openDialog() {
        const ref = this.dialog.open(EmployeeDialog, { width: '400px' });
        ref.afterClosed().subscribe(result => {
            if (result) {
                this.employeeService.createEmployee(result).subscribe({
                    next: () => { this.loadEmployees(); }
                });
            }
        });
    }
}
