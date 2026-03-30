import { Component, inject } from "@angular/core";
import { MatDialogRef, MatDialogModule } from "@angular/material/dialog";
import { ReactiveFormsModule, FormBuilder, Validators } from "@angular/forms";
import { MatFormFieldModule } from "@angular/material/form-field";
import { MatButtonModule } from "@angular/material/button";
import { MatInputModule } from "@angular/material/input";
import { MatSelectModule } from "@angular/material/select";
import { MatIconModule } from "@angular/material/icon";
import { LeaveType } from "../../shared/models/leave.model";

@Component({
    selector: 'app-leave-dialog',
    standalone: true,
    imports: [MatDialogModule, ReactiveFormsModule, MatFormFieldModule,
    MatInputModule, MatButtonModule, MatSelectModule, MatIconModule],
    templateUrl: './leave-dialog.html',
    styleUrl: './leave-dialog.scss'
})

export class LeaveDialog {
    private dialogRef = inject(MatDialogRef<LeaveDialog>);
    private fb = inject(FormBuilder);

    leaveTypes = Object.values(LeaveType);

    form = this.fb.group({
        type: [LeaveType.ANNUAL, [Validators.required]],
        startDate: ['', [Validators.required]],
        endDate: ['', [Validators.required]],
        reason: ['']
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
