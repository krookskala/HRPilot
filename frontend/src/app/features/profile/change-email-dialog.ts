import { Component, inject } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatDialogModule, MatDialogRef, MAT_DIALOG_DATA } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatIconModule } from '@angular/material/icon';
import { TranslateModule } from '@ngx-translate/core';

@Component({
    selector: 'app-change-email-dialog',
    standalone: true,
    imports: [ReactiveFormsModule, MatDialogModule, MatButtonModule, MatFormFieldModule, MatInputModule, MatIconModule, TranslateModule],
    template: `
        <h2 mat-dialog-title>{{ 'profile.changeEmailTitle' | translate }}</h2>
        <mat-dialog-content>
            <form [formGroup]="form" class="email-form">
                <mat-form-field appearance="outline">
                    <mat-label>{{ 'profile.newEmail' | translate }}</mat-label>
                    <input matInput type="email" formControlName="newEmail" autocomplete="email">
                    <mat-error>{{ 'profile.enterValidEmail' | translate }}</mat-error>
                </mat-form-field>
                <mat-form-field appearance="outline">
                    <mat-label>{{ 'profile.currentPassword' | translate }}</mat-label>
                    <input matInput type="password" formControlName="password" autocomplete="current-password">
                    <mat-error>{{ 'profile.passwordRequired' | translate }}</mat-error>
                </mat-form-field>
            </form>
        </mat-dialog-content>
        <mat-dialog-actions align="end">
            <button mat-button mat-dialog-close>{{ 'common.cancel' | translate }}</button>
            <button mat-flat-button color="primary" [disabled]="form.invalid" (click)="onSubmit()">
                <mat-icon>save</mat-icon> {{ 'common.save' | translate }}
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
