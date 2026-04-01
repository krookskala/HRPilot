import { Component, inject } from "@angular/core";
import { MAT_DIALOG_DATA, MatDialogRef, MatDialogModule } from "@angular/material/dialog";
import { ReactiveFormsModule, FormBuilder, Validators } from "@angular/forms";
import { MatFormFieldModule } from "@angular/material/form-field";
import { MatButtonModule } from "@angular/material/button";
import { MatInputModule } from "@angular/material/input";
import { MatIconModule } from "@angular/material/icon";
import { Department } from "../../shared/models/department.model";

@Component({
    selector: 'app-department-dialog',
    standalone: true,
    imports: [MatDialogModule, ReactiveFormsModule, MatFormFieldModule,
    MatInputModule, MatButtonModule, MatIconModule],
    templateUrl: './department-dialog.html',
    styleUrl: './department-dialog.scss'
})

export class DepartmentDialog {
    private dialogRef = inject(MatDialogRef<DepartmentDialog>);
    private fb = inject(FormBuilder);
    data: Department | null = inject(MAT_DIALOG_DATA, { optional: true });

    isEdit = !!this.data;

    form = this.fb.group({
        name: [this.data?.name || '', [Validators.required, Validators.minLength(2)]],
        managerId: [null as number | null],
        parentDepartmentId: [this.data?.parentDepartmentId || null as number | null]
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
