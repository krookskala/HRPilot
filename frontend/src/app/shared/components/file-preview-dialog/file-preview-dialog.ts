import { Component, inject, OnInit, OnDestroy } from '@angular/core';
import { DomSanitizer, SafeResourceUrl } from '@angular/platform-browser';
import { MatButtonModule } from '@angular/material/button';
import { MatDialogModule, MAT_DIALOG_DATA } from '@angular/material/dialog';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { TranslateModule } from '@ngx-translate/core';

export interface FilePreviewData {
    blob: Blob;
    filename: string;
    contentType: string;
}

@Component({
    selector: 'app-file-preview-dialog',
    standalone: true,
    imports: [MatDialogModule, MatButtonModule, MatIconModule, MatProgressSpinnerModule, TranslateModule],
    template: `
        <h2 mat-dialog-title>{{ data.filename }}</h2>
        <mat-dialog-content class="preview-content">
            @if (isPdf) {
                <iframe [src]="safeUrl" class="pdf-frame"></iframe>
            } @else if (isImage) {
                <img [src]="safeUrl" [alt]="data.filename" class="preview-image">
            } @else {
                <div class="no-preview">
                    <mat-icon>description</mat-icon>
                    <p>{{ 'filePreview.noPreview' | translate }}</p>
                </div>
            }
        </mat-dialog-content>
        <mat-dialog-actions align="end">
            <button mat-button mat-dialog-close>{{ 'common.close' | translate }}</button>
            <button mat-flat-button color="primary" (click)="download()">
                <mat-icon>download</mat-icon>
                {{ 'employees.download' | translate }}
            </button>
        </mat-dialog-actions>
    `,
    styles: [`
        .preview-content {
            min-width: 500px;
            min-height: 400px;
            display: flex;
            align-items: center;
            justify-content: center;
        }
        .pdf-frame {
            width: 100%;
            height: 70vh;
            border: none;
            border-radius: 8px;
        }
        .preview-image {
            max-width: 100%;
            max-height: 70vh;
            border-radius: 8px;
            object-fit: contain;
        }
        .no-preview {
            display: flex;
            flex-direction: column;
            align-items: center;
            gap: 12px;
            color: var(--hr-text-secondary);
            mat-icon { font-size: 48px; width: 48px; height: 48px; }
        }
    `]
})
export class FilePreviewDialog implements OnInit, OnDestroy {
    data = inject<FilePreviewData>(MAT_DIALOG_DATA);
    private sanitizer = inject(DomSanitizer);

    safeUrl: SafeResourceUrl | null = null;
    private objectUrl: string | null = null;

    get isPdf(): boolean {
        return this.data.contentType === 'application/pdf';
    }

    get isImage(): boolean {
        return this.data.contentType.startsWith('image/');
    }

    ngOnInit(): void {
        this.objectUrl = URL.createObjectURL(this.data.blob);
        this.safeUrl = this.sanitizer.bypassSecurityTrustResourceUrl(this.objectUrl);
    }

    ngOnDestroy(): void {
        if (this.objectUrl) {
            URL.revokeObjectURL(this.objectUrl);
        }
    }

    download(): void {
        if (!this.objectUrl) return;
        const a = document.createElement('a');
        a.href = this.objectUrl;
        a.download = this.data.filename;
        a.click();
    }
}
