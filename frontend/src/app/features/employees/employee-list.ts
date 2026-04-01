import { Component, inject, OnInit, OnDestroy, ChangeDetectorRef } from "@angular/core";
import { Router } from "@angular/router";
import { FormControl, ReactiveFormsModule } from "@angular/forms";
import { EmployeeService } from "../../core/services/employee.service";
import { DepartmentService } from "../../core/services/department.service";
import { AuthService } from "../../core/services/auth.service";
import { Employee } from "../../shared/models/employee.model";
import { Department } from "../../shared/models/department.model";
import { MatTableModule } from "@angular/material/table";
import { MatButtonModule } from "@angular/material/button";
import { MatDialog, MatDialogModule } from "@angular/material/dialog";
import { MatPaginatorModule, PageEvent } from "@angular/material/paginator";
import { MatProgressSpinnerModule } from "@angular/material/progress-spinner";
import { MatIconModule } from "@angular/material/icon";
import { MatTooltipModule } from "@angular/material/tooltip";
import { MatFormFieldModule } from "@angular/material/form-field";
import { MatInputModule } from "@angular/material/input";
import { MatSelectModule } from "@angular/material/select";
import { MatCardModule } from "@angular/material/card";
import { DecimalPipe } from "@angular/common";
import { Subject, debounceTime, distinctUntilChanged, takeUntil } from "rxjs";
import { EmployeeDialog } from "./employee-dialog";
import { ConfirmDialog } from "../../shared/components/confirm-dialog/confirm-dialog";

@Component({
    selector: 'app-employee-list',
    standalone: true,
    imports: [
        MatTableModule, MatButtonModule, MatDialogModule, MatPaginatorModule,
        MatProgressSpinnerModule, MatIconModule, MatTooltipModule,
        MatFormFieldModule, MatInputModule, MatSelectModule, MatCardModule,
        DecimalPipe, ReactiveFormsModule
    ],
    templateUrl: './employee-list.html',
    styleUrl: './employee-list.scss'
})
export class EmployeeList implements OnInit, OnDestroy {
    private employeeService = inject(EmployeeService);
    private departmentService = inject(DepartmentService);
    private authService = inject(AuthService);
    private cdr = inject(ChangeDetectorRef);
    private dialog = inject(MatDialog);
    private router = inject(Router);
    private destroy$ = new Subject<void>();

    employees: Employee[] = [];
    departments: Department[] = [];
    displayedColumns = ['id', 'firstName', 'lastName', 'department', 'position', 'salary', 'actions'];
    canManage = this.authService.hasRole('ADMIN', 'HR_MANAGER');
    totalElements = 0;
    pageSize = 10;
    pageIndex = 0;
    loading = false;
    error = '';
    viewMode: 'table' | 'card' = 'table';

    searchControl = new FormControl('');
    departmentControl = new FormControl<number | null>(null);
    positionControl = new FormControl('');

    ngOnInit(): void {
        this.loadDepartments();
        this.loadEmployees();

        // Debounced search — waits 400ms after user stops typing
        this.searchControl.valueChanges.pipe(
            debounceTime(400),
            distinctUntilChanged(),
            takeUntil(this.destroy$)
        ).subscribe(() => {
            this.pageIndex = 0;
            this.loadEmployees();
        });

        this.positionControl.valueChanges.pipe(
            debounceTime(400),
            distinctUntilChanged(),
            takeUntil(this.destroy$)
        ).subscribe(() => {
            this.pageIndex = 0;
            this.loadEmployees();
        });

        this.departmentControl.valueChanges.pipe(
            takeUntil(this.destroy$)
        ).subscribe(() => {
            this.pageIndex = 0;
            this.loadEmployees();
        });
    }

    ngOnDestroy(): void {
        this.destroy$.next();
        this.destroy$.complete();
    }

    loadDepartments(): void {
        this.departmentService.getAll(0, 100).pipe(takeUntil(this.destroy$)).subscribe({
            next: (page) => { this.departments = page.content; }
        });
    }

    loadEmployees(): void {
        this.loading = true;
        this.error = '';

        const filters = {
            search: this.searchControl.value || undefined,
            departmentId: this.departmentControl.value || undefined,
            position: this.positionControl.value || undefined
        };

        this.employeeService.search(filters, this.pageIndex, this.pageSize).pipe(takeUntil(this.destroy$)).subscribe({
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

    hasActiveFilters(): boolean {
        return !!(this.searchControl.value || this.departmentControl.value || this.positionControl.value);
    }

    clearFilters(): void {
        this.searchControl.setValue('');
        this.departmentControl.setValue(null);
        this.positionControl.setValue('');
    }

    onPageChange(event: PageEvent): void {
        this.pageIndex = event.pageIndex;
        this.pageSize = event.pageSize;
        this.loadEmployees();
    }

    exportCsv(): void {
        this.employeeService.exportCsv().pipe(takeUntil(this.destroy$)).subscribe({
            next: (blob) => {
                const url = window.URL.createObjectURL(blob);
                const a = document.createElement('a');
                a.href = url;
                a.download = 'employees.csv';
                a.click();
                window.URL.revokeObjectURL(url);
            }
        });
    }

    delete(id: number): void {
        const ref = this.dialog.open(ConfirmDialog, {
            width: '350px',
            data: { title: 'Delete Employee', message: 'Are you sure you want to delete this employee?' }
        });
        ref.afterClosed().pipe(takeUntil(this.destroy$)).subscribe(confirmed => {
            if (confirmed) {
                this.employeeService.deleteEmployee(id).pipe(takeUntil(this.destroy$)).subscribe({
                    next: () => { this.loadEmployees(); }
                });
            }
        });
    }

    openDialog(): void {
        const ref = this.dialog.open(EmployeeDialog, { width: '400px' });
        ref.afterClosed().pipe(takeUntil(this.destroy$)).subscribe(result => {
            if (result) {
                this.employeeService.createEmployee(result).pipe(takeUntil(this.destroy$)).subscribe({
                    next: () => { this.loadEmployees(); }
                });
            }
        });
    }

    openDetail(id: number): void {
        this.router.navigate(['/employees', id]);
    }
}
