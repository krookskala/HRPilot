import { Component, inject } from "@angular/core";
import { MatDialogRef, MatDialogModule } from "@angular/material/dialog";
import { ReactiveFormsModule, FormBuilder, Validators } from "@angular/forms";
import { MatFormFieldModule } from "@angular/material/form-field";
import { MatButtonModule } from "@angular/material/button";
import { MatInputModule } from "@angular/material/input";
import { MatIconModule } from "@angular/material/icon";
import { TranslateModule } from "@ngx-translate/core";

@Component({
    selector: 'app-employee-dialog',
    standalone: true,
    imports: [MatDialogModule, ReactiveFormsModule, MatFormFieldModule,
    MatInputModule, MatButtonModule, MatIconModule, TranslateModule],
    templateUrl: './employee-dialog.html',
    styleUrl: './employee-dialog.scss'
})

export class EmployeeDialog {
    private dialogRef = inject(MatDialogRef<EmployeeDialog>);
    private fb = inject(FormBuilder);

    form = this.fb.group({
        userId: [0, [Validators.required, Validators.min(1)]],
        firstName: ['', [Validators.required, Validators.minLength(2)]],
        lastName: ['', [Validators.required, Validators.minLength(2)]],
        position: ['', [Validators.required]],
        salary: [0, [Validators.required, Validators.min(1)]],
        hireDate: ['', [Validators.required]]
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
