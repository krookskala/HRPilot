import { Component, inject } from "@angular/core";
import { MatDialogRef, MatDialogModule } from "@angular/material/dialog";
import { ReactiveFormsModule, FormBuilder, Validators } from "@angular/forms";
import { MatFormFieldModule } from "@angular/material/form-field";
import { MatButtonModule } from "@angular/material/button";
import { MatInputModule } from "@angular/material/input";

@Component({
    selector: 'app-department-dialog',
    standalone: true,
    imports: [MatDialogModule, ReactiveFormsModule, MatFormFieldModule,
    MatInputModule, MatButtonModule],
    templateUrl: './department-dialog.html',
    styleUrl: './department-dialog.scss'
})

export class DepartmentDialog {
    private dialogRef = inject(MatDialogRef<DepartmentDialog>);
    private fb = inject(FormBuilder);

    form = this.fb.group({
        name: ['', [Validators.required, Validators.minLength(2)]],
        managerId: [null as number | null],
        parentDepartmentId: [null as number | null]
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
