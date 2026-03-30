import { Component, inject, OnInit } from "@angular/core";
import { DecimalPipe, NgFor, NgIf } from "@angular/common";
import { finalize, forkJoin } from "rxjs";
import { MatButtonModule } from "@angular/material/button";
import { MatCardModule } from "@angular/material/card";
import { MatDialog, MatDialogModule } from "@angular/material/dialog";
import { MatIconModule } from "@angular/material/icon";
import { MatPaginatorModule, PageEvent } from "@angular/material/paginator";
import { MatProgressSpinnerModule } from "@angular/material/progress-spinner";
import { MatTableModule } from "@angular/material/table";
import { MatTooltipModule } from "@angular/material/tooltip";
import { AuthService } from "../../core/services/auth.service";
import { PayrollService } from "../../core/services/payroll.service";
import { PayrollRecord, PayrollRun } from "../../shared/models/payroll.model";
import { PayrollDialog } from "./payroll-dialog";

@Component({
    selector: 'app-payroll-list',
    standalone: true,
    imports: [
        DecimalPipe,
        MatButtonModule,
        MatCardModule,
        MatDialogModule,
        MatIconModule,
        MatPaginatorModule,
        MatProgressSpinnerModule,
        MatTableModule,
        MatTooltipModule,
        NgFor,
        NgIf
    ],
    templateUrl: './payroll-list.html',
    styleUrl: './payroll-list.scss'
})
export class PayrollList implements OnInit {
    private payrollService = inject(PayrollService);
    private authService = inject(AuthService);
    private dialog = inject(MatDialog);

    readonly canManage = this.authService.hasRole('ADMIN', 'HR_MANAGER');
    readonly currentUser = this.authService.getCurrentUserSnapshot();

    payrolls: PayrollRecord[] = [];
    runs: PayrollRun[] = [];
    selectedPayroll: PayrollRecord | null = null;
    displayedColumns = ['employeeFullName', 'year', 'month', 'grossSalary', 'netSalary', 'taxClass', 'status', 'actions'];
    runColumns = ['name', 'period', 'status', 'count', 'actions'];
    totalElements = 0;
    pageSize = 10;
    pageIndex = 0;
    loading = false;
    refreshing = false;
    error = '';

    ngOnInit(): void {
        this.loadData();
    }

    loadData(): void {
        this.loading = this.payrolls.length === 0;
        this.refreshing = !this.loading;
        this.error = '';

        if (this.canManage) {
            forkJoin({
                payrollPage: this.payrollService.getAllPayrolls(this.pageIndex, this.pageSize),
                runsPage: this.payrollService.getRuns(0, 20)
            }).pipe(
                finalize(() => {
                    this.loading = false;
                    this.refreshing = false;
                })
            ).subscribe({
                next: ({ payrollPage, runsPage }) => {
                    this.payrolls = payrollPage.content;
                    this.totalElements = payrollPage.totalElements;
                    this.runs = runsPage.content;
                },
                error: () => {
                    this.error = 'Failed to load payrolls';
                }
            });
            return;
        }

        this.payrollService.getMyPayrolls().pipe(
            finalize(() => {
                this.loading = false;
                this.refreshing = false;
            })
        ).subscribe({
            next: payrolls => {
                this.payrolls = payrolls;
                this.totalElements = payrolls.length;
            },
            error: () => {
                this.error = 'Failed to load payrolls';
            }
        });
    }

    onPageChange(event: PageEvent): void {
        this.pageIndex = event.pageIndex;
        this.pageSize = event.pageSize;
        this.loadData();
    }

    openDialog(): void {
        const ref = this.dialog.open(PayrollDialog, { width: '440px' });
        ref.afterClosed().subscribe(result => {
            if (!result) {
                return;
            }

            this.payrollService.createRun({
                name: result.name,
                year: result.year,
                month: result.month,
                employeeIds: [Number(result.employeeId)],
                includeAllEmployees: false,
                bonus: Number(result.bonus),
                additionalDeduction: Number(result.additionalDeduction),
                taxClass: result.taxClass
            }).subscribe({
                next: () => this.loadData(),
                error: err => {
                    this.error = err?.error?.message ?? 'Failed to create payroll run';
                }
            });
        });
    }

    publishRun(runId: number): void {
        this.payrollService.publishRun(runId).subscribe({
            next: () => this.loadData()
        });
    }

    payRun(runId: number): void {
        this.payrollService.payRun(runId).subscribe({
            next: () => this.loadData()
        });
    }

    markAsPaid(id: number): void {
        this.payrollService.markAsPaid(id).subscribe({
            next: () => this.loadData()
        });
    }

    toggleComponents(payroll: PayrollRecord): void {
        if (this.selectedPayroll?.id === payroll.id) {
            this.selectedPayroll = null;
            return;
        }

        if (payroll.components?.length) {
            this.selectedPayroll = payroll;
            return;
        }

        this.selectedPayroll = payroll;
        this.payrollService.getComponents(payroll.id).subscribe({
            next: components => {
                const enrichedPayroll = { ...payroll, components };
                this.selectedPayroll = enrichedPayroll;
                this.payrolls = this.payrolls.map(item => item.id === payroll.id ? enrichedPayroll : item);
            }
        });
    }

    downloadPayslip(payroll: PayrollRecord): void {
        const request$ = this.canManage
            ? this.payrollService.downloadPayslip(payroll.id)
            : this.payrollService.downloadMyPayslip(payroll.id);

        request$.subscribe(blob => {
            const url = URL.createObjectURL(blob);
            const link = document.createElement('a');
            link.href = url;
            link.download = `payslip-${payroll.year}-${payroll.month}.pdf`;
            link.click();
            URL.revokeObjectURL(url);
        });
    }
}
