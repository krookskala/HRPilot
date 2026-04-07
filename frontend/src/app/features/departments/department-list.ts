import { Component, inject, OnInit, OnDestroy, ChangeDetectorRef } from "@angular/core";
import { DepartmentService } from "../../core/services/department.service";
import { AuthService } from "../../core/services/auth.service";
import { Department } from "../../shared/models/department.model";
import { MatButtonModule } from "@angular/material/button";
import { MatDialog, MatDialogModule } from "@angular/material/dialog";
import { MatPaginatorModule, PageEvent } from "@angular/material/paginator";
import { MatProgressSpinnerModule } from "@angular/material/progress-spinner";
import { MatIconModule } from "@angular/material/icon";
import { MatTooltipModule } from "@angular/material/tooltip";
import { Subject, takeUntil } from "rxjs";
import { TranslateModule, TranslateService } from "@ngx-translate/core";
import { DepartmentDialog } from "./department-dialog";
import { ConfirmDialog } from "../../shared/components/confirm-dialog/confirm-dialog";

@Component({
    selector: 'app-department-list',
    standalone: true,
    imports: [MatButtonModule, MatDialogModule, MatPaginatorModule, MatProgressSpinnerModule, MatIconModule, MatTooltipModule, TranslateModule],
    templateUrl: './department-list.html',
    styleUrl: './department-list.scss'
})
export class DepartmentList implements OnInit, OnDestroy {
    private departmentService = inject(DepartmentService);
    private authService = inject(AuthService);
    private cdr = inject(ChangeDetectorRef);
    private dialog = inject(MatDialog);
    private translateService = inject(TranslateService);

    isAdmin = this.authService.hasRole('ADMIN');
    departments: Department[] = [];
    totalElements = 0;
    pageSize = 10;
    pageIndex = 0;
    loading = false;
    error = '';
    private destroy$ = new Subject<void>();

    private accentColors = ['cyan', 'orange', 'green', 'indigo', 'slate'];

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
                this.error = this.translateService.instant('departments.failedLoad');
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

    getDeptAccent(dept: Department): string {
        return this.accentColors[dept.id % this.accentColors.length];
    }

    getSubDepartments(parentId: number): Department[] {
        return this.departments.filter(d => d.parentDepartmentId === parentId);
    }

    delete(id: number) {
        const ref = this.dialog.open(ConfirmDialog, {
            width: '350px',
            data: { title: this.translateService.instant('departments.deleteDepartment'), message: this.translateService.instant('departments.confirmDelete') }
        });
        ref.afterClosed().pipe(takeUntil(this.destroy$)).subscribe(confirmed => {
            if (confirmed) {
                this.departmentService.deleteDepartment(id).pipe(takeUntil(this.destroy$)).subscribe({
                    next: () => { this.loadDepartments(); },
                    error: () => {
                        this.error = this.translateService.instant('departments.failedDelete');
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
                        this.error = this.translateService.instant('departments.failedUpdate');
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
                        this.error = this.translateService.instant('departments.failedCreate');
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
