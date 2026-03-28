import { Component, inject, OnInit, ChangeDetectorRef } from "@angular/core";
import { PayrollService } from "../../core/services/payroll.service";
import { AuthService } from "../../core/services/auth.service";
import { PayrollRecord } from "../../shared/models/payroll.model";
import { MatTableModule } from "@angular/material/table";
import { MatButtonModule } from "@angular/material/button";
import { MatDialog, MatDialogModule } from "@angular/material/dialog";
import { MatPaginatorModule, PageEvent } from "@angular/material/paginator";
import { MatProgressSpinnerModule } from "@angular/material/progress-spinner";
import { NgIf } from "@angular/common";
import { PayrollDialog } from "./payroll-dialog";

@Component({
    selector: 'app-payroll-list',
    standalone: true,
    imports: [MatTableModule, MatButtonModule, MatDialogModule, MatPaginatorModule, MatProgressSpinnerModule, NgIf],
    templateUrl: './payroll-list.html',
    styleUrl: './payroll-list.scss'
})

export class PayrollList implements OnInit {
    private payrollService = inject(PayrollService);
    private authService = inject(AuthService);
    private cdr = inject(ChangeDetectorRef);
    private dialog = inject(MatDialog);

    canManage = this.authService.hasRole('ADMIN', 'HR_MANAGER');
    payrolls: PayrollRecord[] = [];
    displayedColumns = ['id', 'employeeFullName', 'year', 'month',
'baseSalary', 'bonus', 'deductions', 'netSalary', 'actions'];
    totalElements = 0;
    pageSize = 10;
    pageIndex = 0;
    loading = false;
    error = '';

    ngOnInit(): void {
        this.loadPayrolls();
    }

    loadPayrolls() {
        this.loading = true;
        this.error = '';
        this.payrollService.getAllPayrolls(this.pageIndex, this.pageSize).subscribe({
            next: (page) => {
                this.payrolls = page.content;
                this.totalElements = page.totalElements;
                this.loading = false;
                this.cdr.detectChanges();
            },
            error: () => {
                this.error = 'Failed to load payrolls';
                this.loading = false;
                this.cdr.detectChanges();
            }
        });
    }

    onPageChange(event: PageEvent) {
        this.pageIndex = event.pageIndex;
        this.pageSize = event.pageSize;
        this.loadPayrolls();
    }

    markAsPaid(id: number) {
        this.payrollService.markAsPaid(id).subscribe({
            next: () => { this.loadPayrolls(); }
        });
    }

    openDialog() {
        const ref = this.dialog.open(PayrollDialog, { width: '400px' });
        ref.afterClosed().subscribe(result => {
            if (result) {
                this.payrollService.create(result).subscribe({
                    next: () => { this.loadPayrolls(); }
                });
            }
        });
    }
}
