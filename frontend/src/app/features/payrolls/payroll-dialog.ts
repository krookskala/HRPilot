import { Component, inject } from "@angular/core";
import { MatDialogRef, MatDialogModule } from "@angular/material/dialog";
import { ReactiveFormsModule, FormBuilder, Validators } from "@angular/forms";
import { MatFormFieldModule } from "@angular/material/form-field";
import { MatButtonModule } from "@angular/material/button";
import { MatInputModule } from "@angular/material/input";
import { MatIconModule } from "@angular/material/icon";
import { MatSlideToggleModule } from "@angular/material/slide-toggle";
import { MatSelectModule } from "@angular/material/select";

@Component({
    selector: 'app-payroll-dialog',
    standalone: true,
    imports: [MatDialogModule, ReactiveFormsModule, MatFormFieldModule,
    MatInputModule, MatButtonModule, MatIconModule, MatSlideToggleModule, MatSelectModule],
    templateUrl: './payroll-dialog.html',
    styleUrl: './payroll-dialog.scss'
})

export class PayrollDialog {
    private dialogRef = inject(MatDialogRef<PayrollDialog>);
    private fb = inject(FormBuilder);

    form = this.fb.group({
        name: ['Monthly Payroll Run', [Validators.required]],
        employeeId: [0, [Validators.required, Validators.min(1)]],
        year: [new Date().getFullYear(), [Validators.required, Validators.min(2020)]],
        month: [new Date().getMonth() + 1, [Validators.required, Validators.min(1), Validators.max(12)]],
        bonus: [0, [Validators.required, Validators.min(0)]],
        additionalDeduction: [0, [Validators.required, Validators.min(0)]],
        taxClass: ['I', [Validators.required]]
    });

    save() {
        if (this.form.valid) {
            this.dialogRef.close(this.form.value);
        } else {
            this.form.markAllAsTouched();
        }
    }

    cancel() {
        this.dialogRef.close();
    }
}
