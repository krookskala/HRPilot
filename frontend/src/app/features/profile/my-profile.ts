import { Component, OnDestroy, inject, OnInit, ChangeDetectorRef } from "@angular/core";
import { DatePipe } from "@angular/common";
import { MatCardModule } from "@angular/material/card";
import { MatIconModule } from "@angular/material/icon";
import { MatButtonModule } from "@angular/material/button";
import { MatProgressSpinnerModule } from "@angular/material/progress-spinner";
import { finalize, Subject, takeUntil } from "rxjs";
import { AuthService } from "../../core/services/auth.service";
import { EmployeeService } from "../../core/services/employee.service";
import { CurrentUserProfile } from "../../shared/models/user.model";

@Component({
    selector: 'app-my-profile',
    standalone: true,
    imports: [DatePipe, MatCardModule, MatIconModule, MatButtonModule, MatProgressSpinnerModule],
    templateUrl: './my-profile.html',
    styleUrl: './my-profile.scss'
})
export class MyProfile implements OnInit, OnDestroy {
    private authService = inject(AuthService);
    private employeeService = inject(EmployeeService);
    private cdr = inject(ChangeDetectorRef);

    profile: CurrentUserProfile | null = null;
    photoPreviewUrl: string | null = null;
    loading = true;
    uploadingPhoto = false;
    error = '';
    private destroy$ = new Subject<void>();

    ngOnInit(): void {
        this.loading = true;
        this.error = '';
        this.authService.getMyProfile().pipe(
            takeUntil(this.destroy$),
            finalize(() => {
                this.loading = false;
                this.cdr.detectChanges();
            })
        ).subscribe({
            next: profile => {
                this.profile = profile;
                this.loadPhotoPreview();
                this.cdr.detectChanges();
            },
            error: () => {
                this.error = 'Failed to load profile';
                this.cdr.detectChanges();
            }
        });
    }

    ngOnDestroy(): void {
        this.destroy$.next();
        this.destroy$.complete();
        if (this.photoPreviewUrl) {
            URL.revokeObjectURL(this.photoPreviewUrl);
        }
    }

    onPhotoSelected(event: Event): void {
        const input = event.target as HTMLInputElement;
        const file = input.files?.[0];
        const employeeId = this.profile?.employee?.employeeId;
        if (!file || !employeeId) {
            return;
        }

        this.uploadingPhoto = true;
        this.employeeService.uploadPhoto(employeeId, file).pipe(takeUntil(this.destroy$)).subscribe({
            next: () => {
                this.uploadingPhoto = false;
                this.ngOnInit();
            },
            error: () => {
                this.error = 'Failed to upload photo';
                this.uploadingPhoto = false;
                this.cdr.detectChanges();
            }
        });
    }

    downloadDocument(documentId: number, filename: string): void {
        const employeeId = this.profile?.employee?.employeeId;
        if (!employeeId) {
            return;
        }

        this.employeeService.downloadDocument(employeeId, documentId).pipe(takeUntil(this.destroy$)).subscribe({
            next: blob => {
                const url = URL.createObjectURL(blob);
                const link = document.createElement('a');
                link.href = url;
                link.download = filename;
                link.click();
                URL.revokeObjectURL(url);
            },
            error: () => {
                this.error = 'Failed to download document';
                this.cdr.detectChanges();
            }
        });
    }

    private loadPhotoPreview(): void {
        const employee = this.profile?.employee;
        if (!employee?.photoUrl) {
            return;
        }

        this.employeeService.downloadPhoto(employee.employeeId).pipe(takeUntil(this.destroy$)).subscribe({
            next: blob => {
                if (this.photoPreviewUrl) {
                    URL.revokeObjectURL(this.photoPreviewUrl);
                }
                this.photoPreviewUrl = URL.createObjectURL(blob);
                this.cdr.detectChanges();
            },
            error: () => {}
        });
    }
}
