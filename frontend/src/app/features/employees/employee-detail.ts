import { Component, OnDestroy, OnInit, ChangeDetectorRef, inject } from "@angular/core";
import { DatePipe, DecimalPipe } from "@angular/common";
import { ActivatedRoute } from "@angular/router";
import { FormBuilder, ReactiveFormsModule, Validators } from "@angular/forms";
import { MatButtonModule } from "@angular/material/button";
import { MatCardModule } from "@angular/material/card";
import { MatFormFieldModule } from "@angular/material/form-field";
import { MatIconModule } from "@angular/material/icon";
import { MatInputModule } from "@angular/material/input";
import { MatProgressSpinnerModule } from "@angular/material/progress-spinner";
import { MatTooltipModule } from "@angular/material/tooltip";
import { Subject, finalize, takeUntil } from "rxjs";
import { TranslateModule, TranslateService } from "@ngx-translate/core";
import { MatDialog, MatDialogModule } from "@angular/material/dialog";
import { AuthService } from "../../core/services/auth.service";
import { EmployeeService } from "../../core/services/employee.service";
import { EmployeeDetail } from "../../shared/models/employee.model";
import { FilePreviewDialog, FilePreviewData } from "../../shared/components/file-preview-dialog/file-preview-dialog";

@Component({
    selector: 'app-employee-detail',
    standalone: true,
    imports: [
        DatePipe,
        DecimalPipe,
        MatButtonModule,
        MatCardModule,
        MatFormFieldModule,
        MatIconModule,
        MatInputModule,
        MatProgressSpinnerModule,
        ReactiveFormsModule,
        TranslateModule,
        MatDialogModule,
        MatTooltipModule,
    ],
    templateUrl: './employee-detail.html',
    styleUrl: './employee-detail.scss'
})
export class EmployeeDetailPage implements OnInit, OnDestroy {
    private route = inject(ActivatedRoute);
    private employeeService = inject(EmployeeService);
    private authService = inject(AuthService);
    private fb = inject(FormBuilder);
    private cdr = inject(ChangeDetectorRef);
    private translateService = inject(TranslateService);
    private dialog = inject(MatDialog);
    private destroy$ = new Subject<void>();

    private static readonly MAX_FILE_SIZE = 10 * 1024 * 1024; // 10MB
    private static readonly ALLOWED_TYPES = [
        'application/pdf', 'image/jpeg', 'image/png', 'image/gif', 'image/webp',
        'application/msword',
        'application/vnd.openxmlformats-officedocument.wordprocessingml.document',
        'application/vnd.ms-excel',
        'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet'
    ];

    employee: EmployeeDetail | null = null;
    dragging = false;
    fileError = '';
    photoPreviewUrl: string | null = null;
    loading = true;
    uploadingPhoto = false;
    uploadingDocument = false;
    error = '';
    readonly canManage = this.authService.hasRole('ADMIN', 'HR_MANAGER');

    readonly documentForm = this.fb.group({
        title: ['', [Validators.required, Validators.maxLength(255)]],
        description: ['']
    });

    selectedDocumentFile: File | null = null;

    ngOnInit(): void {
        const id = Number(this.route.snapshot.paramMap.get('id'));
        if (!id) {
            this.error = 'Employee not found';
            this.loading = false;
            return;
        }
        this.loadEmployee(id);
    }

    ngOnDestroy(): void {
        this.destroy$.next();
        this.destroy$.complete();
        if (this.photoPreviewUrl) {
            URL.revokeObjectURL(this.photoPreviewUrl);
        }
    }

    loadEmployee(id: number): void {
        this.loading = true;
        this.error = '';
        this.employeeService.getEmployeeDetail(id).pipe(
            takeUntil(this.destroy$),
            finalize(() => {
                this.loading = false;
                this.cdr.detectChanges();
            })
        ).subscribe({
            next: employee => {
                this.employee = employee;
                this.loadPhotoPreview();
                this.cdr.detectChanges();
            },
            error: () => {
                this.error = this.translateService.instant('employees.failedLoadDetail');
                this.cdr.detectChanges();
            }
        });
    }

    onPhotoSelected(event: Event): void {
        const input = event.target as HTMLInputElement;
        const file = input.files?.[0];
        if (!file || !this.employee) {
            return;
        }

        this.uploadingPhoto = true;
        this.employeeService.uploadPhoto(this.employee.id, file).pipe(takeUntil(this.destroy$)).subscribe({
            next: () => {
                this.uploadingPhoto = false;
                this.loadEmployee(this.employee!.id);
            },
            error: () => {
                this.error = this.translateService.instant('employees.failedUploadPhoto');
                this.uploadingPhoto = false;
                this.cdr.detectChanges();
            }
        });
    }

    onDocumentSelected(event: Event): void {
        const input = event.target as HTMLInputElement;
        const file = input.files?.[0] ?? null;
        if (file && this.validateFile(file)) {
            this.selectedDocumentFile = file;
        }
    }

    onDragOver(event: DragEvent): void {
        event.preventDefault();
        event.stopPropagation();
        this.dragging = true;
    }

    onDragLeave(event: DragEvent): void {
        event.preventDefault();
        event.stopPropagation();
        this.dragging = false;
    }

    onDrop(event: DragEvent): void {
        event.preventDefault();
        event.stopPropagation();
        this.dragging = false;
        const file = event.dataTransfer?.files?.[0] ?? null;
        if (file && this.validateFile(file)) {
            this.selectedDocumentFile = file;
        }
    }

    previewDocument(documentId: number, filename: string, contentType: string): void {
        if (!this.employee) return;
        this.employeeService.downloadDocument(this.employee.id, documentId).pipe(takeUntil(this.destroy$)).subscribe({
            next: blob => {
                this.dialog.open(FilePreviewDialog, {
                    width: contentType.startsWith('image/') ? '600px' : '900px',
                    maxHeight: '90vh',
                    data: { blob, filename, contentType } as FilePreviewData
                });
            },
            error: () => {
                this.error = this.translateService.instant('employees.failedDownloadDoc');
            }
        });
    }

    private validateFile(file: File): boolean {
        this.fileError = '';
        if (file.size > EmployeeDetailPage.MAX_FILE_SIZE) {
            this.fileError = this.translateService.instant('fileUpload.tooLarge');
            return false;
        }
        if (!EmployeeDetailPage.ALLOWED_TYPES.includes(file.type)) {
            this.fileError = this.translateService.instant('fileUpload.invalidType');
            return false;
        }
        return true;
    }

    uploadDocument(): void {
        if (!this.employee || !this.selectedDocumentFile || this.documentForm.invalid) {
            this.documentForm.markAllAsTouched();
            return;
        }

        this.uploadingDocument = true;
        this.employeeService.uploadDocument(
            this.employee.id,
            this.selectedDocumentFile,
            this.documentForm.value.title ?? '',
            this.documentForm.value.description ?? null
        ).pipe(takeUntil(this.destroy$)).subscribe({
            next: () => {
                this.uploadingDocument = false;
                this.selectedDocumentFile = null;
                this.documentForm.reset();
                this.loadEmployee(this.employee!.id);
            },
            error: () => {
                this.error = this.translateService.instant('employees.failedUploadDoc');
                this.uploadingDocument = false;
                this.cdr.detectChanges();
            }
        });
    }

    downloadDocument(documentId: number, filename: string): void {
        if (!this.employee) {
            return;
        }
        this.employeeService.downloadDocument(this.employee.id, documentId).pipe(takeUntil(this.destroy$)).subscribe({
            next: blob => { this.saveBlob(blob, filename); },
            error: () => { this.error = this.translateService.instant('employees.failedDownloadDoc'); }
        });
    }

    private loadPhotoPreview(): void {
        if (!this.employee?.photoUrl) {
            this.clearPhotoPreview();
            return;
        }

        if (this.employeeService.isFrontendAssetPhoto(this.employee.photoUrl)) {
            this.clearPhotoPreview();
            this.photoPreviewUrl = this.employeeService.resolvePhotoUrl(this.employee.photoUrl);
            this.cdr.detectChanges();
            return;
        }

        this.employeeService.downloadPhoto(this.employee.id).pipe(takeUntil(this.destroy$)).subscribe({
            next: blob => {
                this.clearPhotoPreview();
                this.photoPreviewUrl = URL.createObjectURL(blob);
                this.cdr.detectChanges();
            },
            error: () => {
                this.clearPhotoPreview();
            }
        });
    }

    private clearPhotoPreview(): void {
        if (this.photoPreviewUrl?.startsWith('blob:')) {
            URL.revokeObjectURL(this.photoPreviewUrl);
        }
        this.photoPreviewUrl = null;
    }

    private saveBlob(blob: Blob, filename: string): void {
        const url = URL.createObjectURL(blob);
        const link = document.createElement('a');
        link.href = url;
        link.download = filename;
        link.click();
        URL.revokeObjectURL(url);
    }
}
