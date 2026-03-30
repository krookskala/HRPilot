import { Component, inject } from "@angular/core";
import { FormBuilder, ReactiveFormsModule, Validators } from "@angular/forms";
import { RouterLink } from "@angular/router";
import { NgIf } from "@angular/common";
import { MatCardModule } from "@angular/material/card";
import { MatFormFieldModule } from "@angular/material/form-field";
import { MatInputModule } from "@angular/material/input";
import { MatButtonModule } from "@angular/material/button";
import { AuthService } from "../../core/services/auth.service";

@Component({
    selector: 'app-request-password-reset',
    standalone: true,
    imports: [ReactiveFormsModule, RouterLink, NgIf, MatCardModule, MatFormFieldModule, MatInputModule, MatButtonModule],
    templateUrl: './request-password-reset.html',
    styleUrl: './request-password-reset.scss'
})
export class RequestPasswordReset {
    private fb = inject(FormBuilder);
    private authService = inject(AuthService);

    message = '';
    error = '';

    form = this.fb.group({
        email: ['', [Validators.required, Validators.email]]
    });

    submit(): void {
        if (this.form.invalid) {
            this.form.markAllAsTouched();
            return;
        }

        this.authService.requestPasswordReset(this.form.value.email!).subscribe({
            next: () => {
                this.message = 'If an account with this email exists, a password reset link has been sent.';
                this.error = '';
            },
            error: () => {
                this.message = 'If an account with this email exists, a password reset link has been sent.';
                this.error = '';
            }
        });
    }
}
