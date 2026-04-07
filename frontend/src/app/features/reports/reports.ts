import { Component, inject, OnInit, OnDestroy, ChangeDetectorRef } from "@angular/core";
import { DecimalPipe } from "@angular/common";
import { MatCardModule } from "@angular/material/card";
import { MatIconModule } from "@angular/material/icon";
import { MatProgressSpinnerModule } from "@angular/material/progress-spinner";
import { MatButtonModule } from "@angular/material/button";
import { Subject, takeUntil } from "rxjs";
import { TranslateModule, TranslateService } from "@ngx-translate/core";
import { DashboardService, DashboardData } from "../../core/services/dashboard.service";
import { EmployeeService } from "../../core/services/employee.service";

@Component({
    selector: 'app-reports',
    standalone: true,
    imports: [DecimalPipe, MatCardModule, MatIconModule, MatProgressSpinnerModule, MatButtonModule, TranslateModule],
    templateUrl: './reports.html',
    styleUrl: './reports.scss'
})
export class Reports implements OnInit, OnDestroy {
    private dashboardService = inject(DashboardService);
    private employeeService = inject(EmployeeService);
    private cdr = inject(ChangeDetectorRef);
    private translate = inject(TranslateService);
    private destroy$ = new Subject<void>();

    data: DashboardData | null = null;
    loading = true;
    error = '';

    ngOnInit(): void {
        this.loadData();
    }

    loadData(): void {
        this.loading = true;
        this.error = '';
        this.dashboardService.getDashboardData().pipe(takeUntil(this.destroy$)).subscribe({
            next: (data) => {
                this.data = data;
                this.loading = false;
                this.cdr.detectChanges();
            },
            error: () => {
                this.error = this.translate.instant('reports.failedLoad');
                this.loading = false;
                this.cdr.detectChanges();
            }
        });
    }

    exportEmployeesCsv(): void {
        this.employeeService.exportCsv().pipe(takeUntil(this.destroy$)).subscribe({
            next: (blob) => {
                const url = URL.createObjectURL(blob);
                const a = document.createElement('a');
                a.href = url;
                a.download = 'employees.csv';
                a.click();
                URL.revokeObjectURL(url);
            },
            error: () => {
                this.error = this.translate.instant('reports.failedExport');
                this.cdr.detectChanges();
            }
        });
    }

    ngOnDestroy(): void {
        this.destroy$.next();
        this.destroy$.complete();
    }
}
