import { Component, OnDestroy, OnInit, inject } from "@angular/core";
import { DatePipe, DecimalPipe, NgFor, NgIf } from "@angular/common";
import { ActivatedRoute } from "@angular/router";
import { FormBuilder, ReactiveFormsModule, Validators } from "@angular/forms";
import { MatButtonModule } from "@angular/material/button";
import { MatCardModule } from "@angular/material/card";
import { MatFormFieldModule } from "@angular/material/form-field";
import { MatIconModule } from "@angular/material/icon";
import { MatInputModule } from "@angular/material/input";
import { MatProgressSpinnerModule } from "@angular/material/progress-spinner";
import { finalize } from "rxjs";
import { AuthService } from "../../core/services/auth.service";
import { EmployeeService } from "../../core/services/employee.service";
import { EmployeeDetail } from "../../shared/models/employee.model";

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
        NgFor,
        NgIf,
        ReactiveFormsModule
    ],
    templateUrl: './employee-detail.html',
    styleUrl: './employee-detail.scss'
})
export class EmployeeDetailPage implements OnInit, OnDestroy {
    private route = inject(ActivatedRoute);
    private employeeService = inject(EmployeeService);
    private authService = inject(AuthService);
    private fb = inject(FormBuilder);

    employee: EmployeeDetail | null = null;
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

    private selectedDocumentFile: File | null = null;

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
        if (this.photoPreviewUrl) {
            URL.revokeObjectURL(this.photoPreviewUrl);
        }
    }

    loadEmployee(id: number): void {
        this.loading = true;
        this.error = '';
        this.employeeService.getEmployeeDetail(id).pipe(
            finalize(() => this.loading = false)
        ).subscribe({
            next: employee => {
                this.employee = employee;
                this.loadPhotoPreview();
            },
            error: () => {
                this.error = 'Failed to load employee details';
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
        this.employeeService.uploadPhoto(this.employee.id, file).subscribe({
            next: () => {
                this.uploadingPhoto = false;
                this.loadEmployee(this.employee!.id);
            },
            error: () => {
                this.error = 'Failed to upload photo';
                this.uploadingPhoto = false;
            }
        });
    }

    onDocumentSelected(event: Event): void {
        const input = event.target as HTMLInputElement;
        this.selectedDocumentFile = input.files?.[0] ?? null;
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
        ).subscribe({
            next: () => {
                this.uploadingDocument = false;
                this.selectedDocumentFile = null;
                this.documentForm.reset();
                this.loadEmployee(this.employee!.id);
            },
            error: () => {
                this.error = 'Failed to upload document';
                this.uploadingDocument = false;
            }
        });
    }

    downloadDocument(documentId: number, filename: string): void {
        if (!this.employee) {
            return;
        }
        this.employeeService.downloadDocument(this.employee.id, documentId).subscribe(blob => {
            this.saveBlob(blob, filename);
        });
    }

    private loadPhotoPreview(): void {
        if (!this.employee?.photoUrl) {
            this.clearPhotoPreview();
            return;
        }

        this.employeeService.downloadPhoto(this.employee.id).subscribe({
            next: blob => {
                this.clearPhotoPreview();
                this.photoPreviewUrl = URL.createObjectURL(blob);
            },
            error: () => {
                this.clearPhotoPreview();
            }
        });
    }

    private clearPhotoPreview(): void {
        if (this.photoPreviewUrl) {
            URL.revokeObjectURL(this.photoPreviewUrl);
            this.photoPreviewUrl = null;
        }
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
