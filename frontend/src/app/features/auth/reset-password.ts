import { Component, inject, OnInit } from "@angular/core";
import { FormBuilder, ReactiveFormsModule, Validators } from "@angular/forms";
import { ActivatedRoute, Router } from "@angular/router";
import { NgIf } from "@angular/common";
import { MatCardModule } from "@angular/material/card";
import { MatFormFieldModule } from "@angular/material/form-field";
import { MatInputModule } from "@angular/material/input";
import { MatButtonModule } from "@angular/material/button";
import { AuthService } from "../../core/services/auth.service";

@Component({
    selector: 'app-reset-password',
    standalone: true,
    imports: [ReactiveFormsModule, NgIf, MatCardModule, MatFormFieldModule, MatInputModule, MatButtonModule],
    templateUrl: './reset-password.html',
    styleUrl: './reset-password.scss'
})
export class ResetPassword implements OnInit {
    private route = inject(ActivatedRoute);
    private router = inject(Router);
    private authService = inject(AuthService);
    private fb = inject(FormBuilder);

    token = '';
    email = '';
    loading = true;
    error = '';

    form = this.fb.group({
        password: ['', [Validators.required, Validators.minLength(8)]],
        confirmPassword: ['', [Validators.required, Validators.minLength(8)]]
    });

    ngOnInit(): void {
        this.token = this.route.snapshot.paramMap.get('token') ?? '';
        this.authService.validatePasswordResetToken(this.token).subscribe({
            next: response => {
                this.email = response.email;
                this.loading = false;
            },
            error: err => {
                this.error = err.error?.message || 'Reset token is invalid';
                this.loading = false;
            }
        });
    }

    submit(): void {
        if (this.form.invalid) {
            this.form.markAllAsTouched();
            return;
        }

        if (this.form.value.password !== this.form.value.confirmPassword) {
            this.error = 'Passwords do not match';
            return;
        }

        this.authService.resetPassword(this.token, this.form.value.password!).subscribe({
            next: () => this.router.navigate(['/login']),
            error: err => {
                this.error = err.error?.message || 'Password could not be reset';
            }
        });
    }
}
