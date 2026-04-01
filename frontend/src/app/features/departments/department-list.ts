import { Component, inject, OnInit, OnDestroy, ChangeDetectorRef } from "@angular/core";
import { DepartmentService } from "../../core/services/department.service";
import { AuthService } from "../../core/services/auth.service";
import { Department } from "../../shared/models/department.model";
import { MatTableModule } from "@angular/material/table";
import { MatButtonModule } from "@angular/material/button";
import { MatDialog, MatDialogModule } from "@angular/material/dialog";
import { MatPaginatorModule, PageEvent } from "@angular/material/paginator";
import { MatProgressSpinnerModule } from "@angular/material/progress-spinner";
import { MatIconModule } from "@angular/material/icon";
import { MatTooltipModule } from "@angular/material/tooltip";
import { Subject, takeUntil } from "rxjs";
import { DepartmentDialog } from "./department-dialog";
import { ConfirmDialog } from "../../shared/components/confirm-dialog/confirm-dialog";

@Component({
    selector: 'app-department-list',
    standalone: true,
    imports: [MatTableModule, MatButtonModule, MatDialogModule, MatPaginatorModule, MatProgressSpinnerModule, MatIconModule, MatTooltipModule],
    templateUrl: './department-list.html',
    styleUrl: './department-list.scss'
})

export class DepartmentList implements OnInit, OnDestroy {
    private departmentService = inject(DepartmentService);
    private authService = inject(AuthService);
    private cdr = inject(ChangeDetectorRef);
    private dialog = inject(MatDialog);

    isAdmin = this.authService.hasRole('ADMIN');
    departments: Department[] = [];
    displayedColumns = ['id', 'name', 'managerEmail', 'parentDepartmentName', 'actions'];
    totalElements = 0;
    pageSize = 10;
    pageIndex = 0;
    loading = false;
    error = '';
    private destroy$ = new Subject<void>();

    ngOnInit(): void {
        this.loadDepartments();
    }

    loadDepartments() {
        this.loading = true;
        this.error = '';
        this.departmentService.getAll(this.pageIndex, this.pageSize).pipe(takeUntil(this.destroy$)).subscribe({
            next: (page) => {
                this.departments = page.content;
                this.totalElements = page.totalElements;
                this.loading = false;
                this.cdr.detectChanges();
            },
            error: () => {
                this.error = 'Failed to load departments';
                this.loading = false;
                this.cdr.detectChanges();
            }
        });
    }

    onPageChange(event: PageEvent) {
        this.pageIndex = event.pageIndex;
        this.pageSize = event.pageSize;
        this.loadDepartments();
    }

    delete(id: number) {
        const ref = this.dialog.open(ConfirmDialog, {
            width: '350px',
            data: { title: 'Delete Department', message: 'Are you sure you want to delete this department?' }
        });
        ref.afterClosed().pipe(takeUntil(this.destroy$)).subscribe(confirmed => {
            if (confirmed) {
                this.departmentService.deleteDepartment(id).pipe(takeUntil(this.destroy$)).subscribe({
                    next: () => { this.loadDepartments(); },
                    error: () => {
                        this.error = 'Failed to delete department';
                        this.cdr.detectChanges();
                    }
                });
            }
        });
    }

    edit(dept: Department) {
        const ref = this.dialog.open(DepartmentDialog, { width: '400px', data: dept });
        ref.afterClosed().pipe(takeUntil(this.destroy$)).subscribe(result => {
            if (result) {
                this.departmentService.updateDepartment(dept.id, result).pipe(takeUntil(this.destroy$)).subscribe({
                    next: () => { this.loadDepartments(); },
                    error: () => {
                        this.error = 'Failed to update department';
                        this.cdr.detectChanges();
                    }
                });
            }
        });
    }

    openDialog() {
        const ref = this.dialog.open(DepartmentDialog, { width: '400px' });
        ref.afterClosed().pipe(takeUntil(this.destroy$)).subscribe(result => {
            if (result) {
                this.departmentService.createDepartment(result).pipe(takeUntil(this.destroy$)).subscribe({
                    next: () => { this.loadDepartments(); },
                    error: () => {
                        this.error = 'Failed to create department';
                        this.cdr.detectChanges();
                    }
                });
            }
        });
    }

    ngOnDestroy(): void {
        this.destroy$.next();
        this.destroy$.complete();
    }
}
