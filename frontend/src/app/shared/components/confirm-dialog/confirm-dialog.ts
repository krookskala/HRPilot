import { Component, inject } from "@angular/core";
import { MAT_DIALOG_DATA, MatDialogModule, MatDialogRef } from "@angular/material/dialog";
import { MatButtonModule } from "@angular/material/button";
import { MatIconModule } from "@angular/material/icon";

export interface ConfirmDialogData {
    title: string;
    message: string;
}

@Component({
    selector: 'app-confirm-dialog',
    standalone: true,
    imports: [MatDialogModule, MatButtonModule, MatIconModule],
    template: `
        <div class="dialog-header">
            <mat-icon class="warning-icon">warning_amber</mat-icon>
            <h2>{{ data.title }}</h2>
        </div>
        <mat-dialog-content>
            <p class="confirm-message">{{ data.message }}</p>
        </mat-dialog-content>
        <mat-dialog-actions align="end">
            <button mat-button (click)="dialogRef.close(false)">Cancel</button>
            <button mat-flat-button color="warn" (click)="dialogRef.close(true)">Confirm</button>
        </mat-dialog-actions>
    `,
    styles: [`
        .dialog-header {
            display: flex;
            align-items: center;
            gap: 12px;
            padding: 20px 24px 16px;
            border-bottom: 1px solid rgba(0, 0, 0, 0.08);
        }
        .dialog-header h2 {
            margin: 0;
            font-size: 1.25rem;
            font-weight: 600;
            color: #1e293b;
        }
        .warning-icon {
            color: #f59e0b;
            font-size: 24px;
            width: 24px;
            height: 24px;
        }
        .confirm-message {
            font-size: 0.9375rem;
            color: #475569;
            padding: 8px 0;
        }
        mat-dialog-content {
            min-width: 350px;
            padding: 16px 24px !important;
        }
        mat-dialog-actions {
            border-top: 1px solid rgba(0, 0, 0, 0.08);
            padding: 12px 24px !important;
        }
    `]
})
export class ConfirmDialog {
    data = inject<ConfirmDialogData>(MAT_DIALOG_DATA);
    dialogRef = inject(MatDialogRef<ConfirmDialog>);
}
