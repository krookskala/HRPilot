import { Component, OnDestroy, inject, OnInit } from "@angular/core";
import { NgFor, NgIf, DatePipe } from "@angular/common";
import { MatCardModule } from "@angular/material/card";
import { MatIconModule } from "@angular/material/icon";
import { MatButtonModule } from "@angular/material/button";
import { MatProgressSpinnerModule } from "@angular/material/progress-spinner";
import { finalize } from "rxjs";
import { AuthService } from "../../core/services/auth.service";
import { EmployeeService } from "../../core/services/employee.service";
import { CurrentUserProfile } from "../../shared/models/user.model";

@Component({
    selector: 'app-my-profile',
    standalone: true,
    imports: [NgIf, NgFor, DatePipe, MatCardModule, MatIconModule, MatButtonModule, MatProgressSpinnerModule],
    templateUrl: './my-profile.html',
    styleUrl: './my-profile.scss'
})
export class MyProfile implements OnInit, OnDestroy {
    private authService = inject(AuthService);
    private employeeService = inject(EmployeeService);

    profile: CurrentUserProfile | null = null;
    photoPreviewUrl: string | null = null;
    loading = true;
    uploadingPhoto = false;
    error = '';

    ngOnInit(): void {
        this.loading = true;
        this.error = '';
        this.authService.getMyProfile().pipe(
            finalize(() => this.loading = false)
        ).subscribe({
            next: profile => {
                this.profile = profile;
                this.loadPhotoPreview();
            },
            error: () => {
                this.error = 'Failed to load profile';
            }
        });
    }

    ngOnDestroy(): void {
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
        this.employeeService.uploadPhoto(employeeId, file).subscribe({
            next: () => {
                this.uploadingPhoto = false;
                this.ngOnInit();
            },
            error: () => {
                this.error = 'Failed to upload photo';
                this.uploadingPhoto = false;
            }
        });
    }

    downloadDocument(documentId: number, filename: string): void {
        const employeeId = this.profile?.employee?.employeeId;
        if (!employeeId) {
            return;
        }

        this.employeeService.downloadDocument(employeeId, documentId).subscribe(blob => {
            const url = URL.createObjectURL(blob);
            const link = document.createElement('a');
            link.href = url;
            link.download = filename;
            link.click();
            URL.revokeObjectURL(url);
        });
    }

    private loadPhotoPreview(): void {
        const employee = this.profile?.employee;
        if (!employee?.photoUrl) {
            return;
        }

        this.employeeService.downloadPhoto(employee.employeeId).subscribe({
            next: blob => {
                if (this.photoPreviewUrl) {
                    URL.revokeObjectURL(this.photoPreviewUrl);
                }
                this.photoPreviewUrl = URL.createObjectURL(blob);
            }
        });
    }
}
