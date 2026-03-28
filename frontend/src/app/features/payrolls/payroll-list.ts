import { Component, inject, OnInit, ChangeDetectorRef } from "@angular/core";
import { PayrollService } from "../../core/services/payroll.service";
import { PayrollRecord } from "../../shared/models/payroll.model";
import { MatTableModule } from "@angular/material/table";
import { MatButtonModule } from "@angular/material/button";
import { MatDialog, MatDialogModule } from "@angular/material/dialog";
import { PayrollDialog } from "./payroll-dialog";

@Component({
    selector: 'app-payroll-list',
    standalone: true,
    imports: [MatTableModule, MatButtonModule, MatDialogModule],
    templateUrl: './payroll-list.html',
    styleUrl: './payroll-list.scss'
})

export class PayrollList implements OnInit {
    private payrollService = inject(PayrollService);
    private cdr = inject(ChangeDetectorRef);
    private dialog = inject(MatDialog);

    payrolls: PayrollRecord[] = [];
    displayedColumns = ['id', 'employeeFullName', 'year', 'month', 
'baseSalary', 'bonus', 'deductions', 'netSalary', 'actions'];

    ngOnInit(): void {
        this.loadPayrolls();
    }

    loadPayrolls() {
        this.payrollService.getAllPayrolls().subscribe({
            next: (data) => {
                this.payrolls = data;
                this.cdr.detectChanges();
            }
        });    
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