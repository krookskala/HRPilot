import { Component, inject } from "@angular/core";
import { MatDialogRef, MatDialogModule } from "@angular/material/dialog";
import { FormsModule } from "@angular/forms";
import { MatFormFieldModule } from "@angular/material/form-field";
import { MatButtonModule } from "@angular/material/button";
import { MatInputModule } from "@angular/material/input";
import { CreatePayrollRequest } from "../../shared/models/payroll.model";

@Component({
    selector: 'app-payroll-dialog',
    standalone: true,
    imports: [MatDialogModule, FormsModule, MatFormFieldModule,
    MatInputModule, MatButtonModule],
    templateUrl: './payroll-dialog.html',
    styleUrl: './payroll-dialog.scss'
})

export class PayrollDialog {
    private dialogRef = inject(MatDialogRef<PayrollDialog>);

    request: CreatePayrollRequest = {
        employeeId: 0,
        year: 0,
        month: 0,
        baseSalary: 0,
        bonus: 0,
        deductions: 0,
    };

    save() {
        this.dialogRef.close(this.request);
    }

    cancel() {
        this.dialogRef.close();
    }
}