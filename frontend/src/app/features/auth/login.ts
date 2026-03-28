import { Component, inject } from "@angular/core";
import { ReactiveFormsModule, FormBuilder, Validators } from "@angular/forms";
import { AuthService } from "../../core/services/auth.service";
import { MatCardModule } from "@angular/material/card";
import { MatFormFieldModule } from "@angular/material/form-field";
import { MatInputModule } from "@angular/material/input";
import { MatButtonModule } from "@angular/material/button";
import { MatProgressSpinnerModule } from "@angular/material/progress-spinner";
import { NgIf } from "@angular/common";
import { Router } from "@angular/router";

@Component({
    selector: 'app-login',
    standalone: true,
    imports: [ReactiveFormsModule, MatCardModule, MatFormFieldModule,
    MatInputModule, MatButtonModule, MatProgressSpinnerModule, NgIf],
    templateUrl: './login.html',
    styleUrl: './login.scss'
})

export class Login {
    private authService = inject(AuthService);
    private router = inject(Router);
    private fb = inject(FormBuilder);

    error = '';
    loading = false;

    form = this.fb.group({
        email: ['', [Validators.required, Validators.email]],
        password: ['', [Validators.required, Validators.minLength(6)]]
    });

    login() {
        if (!this.form.valid) {
            this.form.markAllAsTouched();
            return;
        }
        this.loading = true;
        this.error = '';
        this.authService.login({
            email: this.form.value.email!,
            password: this.form.value.password!
        }).subscribe({
            next: (response) => {
                localStorage.setItem('token', response.token);
                this.router.navigate(['/dashboard']);
            },
            error: (err) => {
                this.error = err.error?.message || 'Email or Password Wrong';
                this.loading = false;
            }
        });
    }
}
