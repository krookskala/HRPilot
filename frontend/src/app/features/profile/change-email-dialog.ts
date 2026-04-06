import { Component, inject } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatDialogModule, MatDialogRef, MAT_DIALOG_DATA } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatIconModule } from '@angular/material/icon';

@Component({
    selector: 'app-change-email-dialog',
    standalone: true,
    imports: [ReactiveFormsModule, MatDialogModule, MatButtonModule, MatFormFieldModule, MatInputModule, MatIconModule],
    template: `
        <h2 mat-dialog-title>Change Email</h2>
        <mat-dialog-content>
            <form [formGroup]="form" class="email-form">
                <mat-form-field appearance="outline">
                    <mat-label>New Email</mat-label>
                    <input matInput type="email" formControlName="newEmail" autocomplete="email">
                    <mat-error>Enter a valid email</mat-error>
                </mat-form-field>
                <mat-form-field appearance="outline">
                    <mat-label>Current Password</mat-label>
                    <input matInput type="password" formControlName="password" autocomplete="current-password">
                    <mat-error>Password is required</mat-error>
                </mat-form-field>
            </form>
        </mat-dialog-content>
        <mat-dialog-actions align="end">
            <button mat-button mat-dialog-close>Cancel</button>
            <button mat-flat-button color="primary" [disabled]="form.invalid" (click)="onSubmit()">
                <mat-icon>save</mat-icon> Save
            </button>
        </mat-dialog-actions>
    `,
    styles: [`
        .email-form {
            display: flex;
            flex-direction: column;
            gap: 4px;
            min-width: 360px;
        }
    `]
})
export class ChangeEmailDialog {
    private fb = inject(FormBuilder);
    private dialogRef = inject(MatDialogRef<ChangeEmailDialog>);
    data = inject(MAT_DIALOG_DATA);

    form = this.fb.group({
        newEmail: ['', [Validators.required, Validators.email]],
        password: ['', Validators.required]
    });

    onSubmit(): void {
        if (this.form.valid) {
            this.dialogRef.close(this.form.value);
        }
    }
}
