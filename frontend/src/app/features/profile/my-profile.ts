import { Component, OnDestroy, inject, OnInit, ChangeDetectorRef } from "@angular/core";
import { DatePipe } from "@angular/common";
import { FormBuilder, ReactiveFormsModule, Validators, AbstractControl, ValidationErrors } from "@angular/forms";
import { RouterLink } from "@angular/router";
import { MatCardModule } from "@angular/material/card";
import { MatIconModule } from "@angular/material/icon";
import { MatButtonModule } from "@angular/material/button";
import { MatProgressSpinnerModule } from "@angular/material/progress-spinner";
import { MatMenuModule } from "@angular/material/menu";
import { MatFormFieldModule } from "@angular/material/form-field";
import { MatInputModule } from "@angular/material/input";
import { MatTooltipModule } from "@angular/material/tooltip";
import { MatSnackBar, MatSnackBarModule } from "@angular/material/snack-bar";
import { MatDialog, MatDialogModule } from "@angular/material/dialog";
import { TranslateModule, TranslateService } from "@ngx-translate/core";
import { finalize, Subject, takeUntil } from "rxjs";
import { AuthService } from "../../core/services/auth.service";
import { EmployeeService } from "../../core/services/employee.service";
import { CurrentUserProfile } from "../../shared/models/user.model";
import { ChangeEmailDialog } from "./change-email-dialog";

@Component({
    selector: 'app-my-profile',
    standalone: true,
    imports: [
        DatePipe, RouterLink, ReactiveFormsModule,
        MatCardModule, MatIconModule, MatButtonModule, MatProgressSpinnerModule,
        MatMenuModule, MatFormFieldModule, MatInputModule, MatTooltipModule,
        MatSnackBarModule, MatDialogModule, TranslateModule
    ],
    templateUrl: './my-profile.html',
    styleUrl: './my-profile.scss'
})
export class MyProfile implements OnInit, OnDestroy {
    private authService = inject(AuthService);
    private employeeService = inject(EmployeeService);
    private cdr = inject(ChangeDetectorRef);
    private fb = inject(FormBuilder);
    private snackBar = inject(MatSnackBar);
    private dialog = inject(MatDialog);
    private translate = inject(TranslateService);

    profile: CurrentUserProfile | null = null;
    photoPreviewUrl: string | null = null;
    loading = true;
    uploadingPhoto = false;
    error = '';
    changingPassword = false;
    savingPersonalInfo = false;
    personalInfoExpanded = false;
    securityExpanded = false;
    private destroy$ = new Subject<void>();

    personalInfoForm = this.fb.group({
        firstName: ['', [Validators.required, Validators.minLength(2)]],
        lastName: ['', [Validators.required, Validators.minLength(2)]],
        phone: [''],
        address: [''],
        emergencyContactName: [''],
        emergencyContactPhone: ['']
    });

    passwordForm = this.fb.group({
        currentPassword: ['', Validators.required],
        newPassword: ['', [
            Validators.required,
            Validators.minLength(12),
            Validators.pattern(/^(?=.*[a-z])(?=.*[A-Z])(?=.*\d)(?=.*[@$!%*?&#]).*$/)
        ]],
        confirmPassword: ['', Validators.required]
    }, { validators: this.passwordMatchValidator });

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
                this.error = this.translate.instant('profile.failedLoadProfile');
                this.cdr.detectChanges();
            }
        });
    }

    ngOnDestroy(): void {
        this.destroy$.next();
        this.destroy$.complete();
        if (this.photoPreviewUrl?.startsWith('blob:')) {
            URL.revokeObjectURL(this.photoPreviewUrl);
        }
    }

    onPhotoSelected(event: Event): void {
        const input = event.target as HTMLInputElement;
        const file = input.files?.[0];
        const employeeId = this.profile?.employee?.employeeId;
        if (!file || !employeeId) return;

        this.uploadingPhoto = true;
        this.employeeService.uploadPhoto(employeeId, file).pipe(takeUntil(this.destroy$)).subscribe({
            next: () => {
                this.uploadingPhoto = false;
                this.ngOnInit();
            },
            error: () => {
                this.error = this.translate.instant('employees.failedUploadPhoto');
                this.uploadingPhoto = false;
                this.cdr.detectChanges();
            }
        });
    }

    downloadDocument(documentId: number, filename: string): void {
        const employeeId = this.profile?.employee?.employeeId;
        if (!employeeId) return;

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
                this.error = this.translate.instant('employees.failedDownloadDoc');
                this.cdr.detectChanges();
            }
        });
    }

    // --- Personal info ---
    togglePersonalInfo(): void {
        this.personalInfoExpanded = !this.personalInfoExpanded;
        if (this.personalInfoExpanded && this.profile?.employee) {
            const emp = this.profile.employee;
            this.personalInfoForm.patchValue({
                firstName: emp.firstName,
                lastName: emp.lastName,
                phone: emp.phone ?? '',
                address: emp.address ?? '',
                emergencyContactName: emp.emergencyContactName ?? '',
                emergencyContactPhone: emp.emergencyContactPhone ?? ''
            });
        }
    }

    onSavePersonalInfo(): void {
        if (this.personalInfoForm.invalid) return;
        this.savingPersonalInfo = true;
        this.authService.updatePersonalInfo(this.personalInfoForm.value as import("../../shared/models/employee.model").UpdatePersonalInfoRequest)
            .pipe(
                takeUntil(this.destroy$),
                finalize(() => { this.savingPersonalInfo = false; this.cdr.detectChanges(); })
            )
            .subscribe({
                next: () => {
                    this.snackBar.open(this.translate.instant('profile.personalInfoSaved'), 'OK', { duration: 3000 });
                    this.personalInfoExpanded = false;
                    this.ngOnInit();
                },
                error: () => {
                    this.snackBar.open(this.translate.instant('profile.failedUpdatePersonalInfo'), 'OK', { duration: 3000 });
                }
            });
    }

    // --- Language change ---
    onLanguageChange(lang: string): void {
        this.authService.changeLanguage(lang).pipe(takeUntil(this.destroy$)).subscribe({
            next: () => {
                this.profile!.preferredLang = lang;
                this.translate.use(lang);
                localStorage.setItem('lang', lang);
                this.snackBar.open(this.translate.instant('profile.langChanged'), 'OK', { duration: 3000 });
                this.cdr.detectChanges();
            },
            error: () => {
                this.snackBar.open(this.translate.instant('profile.failedUpdateLang'), 'OK', { duration: 3000 });
                this.cdr.detectChanges();
            }
        });
    }

    // --- Email change dialog ---
    openEmailChangeDialog(): void {
        const dialogRef = this.dialog.open(ChangeEmailDialog, {
            width: '440px',
            data: { currentEmail: this.profile?.email }
        });
        dialogRef.afterClosed().pipe(takeUntil(this.destroy$)).subscribe(result => {
            if (result) {
                this.authService.changeEmail(result.newEmail, result.password).pipe(takeUntil(this.destroy$)).subscribe({
                    next: () => {
                        this.snackBar.open(this.translate.instant('profile.emailChanged'), 'OK', { duration: 3000 });
                        this.ngOnInit();
                    },
                    error: () => {
                        this.snackBar.open(this.translate.instant('profile.failedUpdateEmail'), 'OK', { duration: 3000 });
                    }
                });
            }
        });
    }

    // --- Password change ---
    onChangePassword(): void {
        if (this.passwordForm.invalid) return;
        this.changingPassword = true;
        const { currentPassword, newPassword } = this.passwordForm.value;
        this.authService.changePassword(currentPassword!, newPassword!).pipe(
            takeUntil(this.destroy$),
            finalize(() => { this.changingPassword = false; this.cdr.detectChanges(); })
        ).subscribe({
            next: () => {
                this.snackBar.open(this.translate.instant('profile.passwordChanged'), 'OK', { duration: 3000 });
                this.passwordForm.reset();
            },
            error: () => {
                this.snackBar.open(this.translate.instant('profile.failedChangePassword'), 'OK', { duration: 4000 });
            }
        });
    }

    // --- Helpers ---
    getRoleBadgeClass(): string {
        switch (this.profile?.role) {
            case 'ADMIN': return 'role-badge admin';
            case 'HR_MANAGER': return 'role-badge hr';
            case 'DEPARTMENT_MANAGER': return 'role-badge manager';
            default: return 'role-badge employee';
        }
    }

    getLanguageLabel(): string {
        switch (this.profile?.preferredLang) {
            case 'de': return 'Deutsch';
            case 'tr': return 'Türkçe';
            default: return 'English';
        }
    }

    getDocIcon(filename: string): string {
        if (!filename) return 'description';
        const ext = filename.split('.').pop()?.toLowerCase();
        if (ext === 'pdf') return 'picture_as_pdf';
        if (['jpg', 'jpeg', 'png', 'gif', 'webp'].includes(ext || '')) return 'image';
        return 'description';
    }

    private passwordMatchValidator(control: AbstractControl): ValidationErrors | null {
        const newPassword = control.get('newPassword')?.value;
        const confirm = control.get('confirmPassword')?.value;
        if (!newPassword || !confirm) return null;
        return newPassword === confirm ? null : { passwordMismatch: true };
    }

    private loadPhotoPreview(): void {
        const employee = this.profile?.employee;
        if (!employee?.photoUrl) {
            if (this.photoPreviewUrl?.startsWith('blob:')) {
                URL.revokeObjectURL(this.photoPreviewUrl);
            }
            this.photoPreviewUrl = null;
            this.cdr.detectChanges();
            return;
        }

        if (this.employeeService.isFrontendAssetPhoto(employee.photoUrl)) {
            if (this.photoPreviewUrl?.startsWith('blob:')) {
                URL.revokeObjectURL(this.photoPreviewUrl);
            }
            this.photoPreviewUrl = this.employeeService.resolvePhotoUrl(employee.photoUrl);
            this.cdr.detectChanges();
            return;
        }

        this.employeeService.downloadPhoto(employee.employeeId).pipe(takeUntil(this.destroy$)).subscribe({
            next: blob => {
                if (this.photoPreviewUrl?.startsWith('blob:')) {
                    URL.revokeObjectURL(this.photoPreviewUrl);
                }
                this.photoPreviewUrl = URL.createObjectURL(blob);
                this.cdr.detectChanges();
            },
            error: () => {}
        });
    }
}
