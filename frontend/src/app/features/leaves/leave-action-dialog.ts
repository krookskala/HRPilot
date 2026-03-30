import { Component, Inject, inject } from "@angular/core";
import { MAT_DIALOG_DATA, MatDialogModule, MatDialogRef } from "@angular/material/dialog";
import { FormBuilder, ReactiveFormsModule, Validators } from "@angular/forms";
import { MatButtonModule } from "@angular/material/button";
import { MatFormFieldModule } from "@angular/material/form-field";
import { MatIconModule } from "@angular/material/icon";
import { MatInputModule } from "@angular/material/input";

export interface LeaveActionDialogData {
    title: string;
    actionLabel: string;
    reasonLabel: string;
    presetReason?: string | null;
}

@Component({
    selector: 'app-leave-action-dialog',
    standalone: true,
    imports: [
        MatDialogModule,
        ReactiveFormsModule,
        MatButtonModule,
        MatFormFieldModule,
        MatIconModule,
        MatInputModule
    ],
    templateUrl: './leave-action-dialog.html',
    styleUrl: './leave-action-dialog.scss'
})
export class LeaveActionDialog {
    private dialogRef = inject(MatDialogRef<LeaveActionDialog>);
    private fb = inject(FormBuilder);
    form;

    constructor(@Inject(MAT_DIALOG_DATA) public data: LeaveActionDialogData) {
        this.form = this.fb.group({
            reason: [data.presetReason ?? '', [Validators.required, Validators.maxLength(500)]]
        });
    }

    save(): void {
        if (this.form.valid) {
            this.dialogRef.close(this.form.value.reason?.trim() ?? '');
            return;
        }
        this.form.markAllAsTouched();
    }

    cancel(): void {
        this.dialogRef.close();
    }
}
