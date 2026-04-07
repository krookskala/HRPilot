import { Component, inject, OnInit } from "@angular/core";
import { FormBuilder, ReactiveFormsModule, Validators } from "@angular/forms";
import { ActivatedRoute, Router } from "@angular/router";
import { MatCardModule } from "@angular/material/card";
import { MatFormFieldModule } from "@angular/material/form-field";
import { MatInputModule } from "@angular/material/input";
import { MatButtonModule } from "@angular/material/button";
import { MatIconModule } from "@angular/material/icon";
import { TranslateModule, TranslateService } from "@ngx-translate/core";
import { AuthService } from "../../core/services/auth.service";

@Component({
    selector: 'app-accept-invite',
    standalone: true,
    imports: [ReactiveFormsModule, MatCardModule, MatFormFieldModule, MatInputModule, MatButtonModule, MatIconModule, TranslateModule],
    templateUrl: './accept-invite.html',
    styleUrl: './accept-invite.scss'
})
export class AcceptInvite implements OnInit {
    private route = inject(ActivatedRoute);
    private router = inject(Router);
    private authService = inject(AuthService);
    private fb = inject(FormBuilder);
    private translate = inject(TranslateService);

    token = '';
    email = '';
    loading = true;
    submitting = false;
    error = '';

    form = this.fb.group({
        password: ['', [Validators.required, Validators.minLength(8)]],
        confirmPassword: ['', [Validators.required, Validators.minLength(8)]]
    });

    ngOnInit(): void {
        this.token = this.route.snapshot.paramMap.get('token') ?? '';
        this.authService.getInvitation(this.token).subscribe({
            next: details => {
                this.email = details.email;
                this.loading = false;
            },
            error: err => {
                this.error = err.error?.message || this.translate.instant('auth.failedLoadInvitation');
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
            this.error = this.translate.instant('auth.passwordsDoNotMatch');
            return;
        }

        this.submitting = true;
        this.error = '';

        this.authService.acceptInvitation(this.token, this.form.value.password!).subscribe({
            next: () => this.router.navigate(['/dashboard']),
            error: err => {
                this.error = err.error?.message || this.translate.instant('auth.failedAcceptInvitation');
                this.submitting = false;
            }
        });
    }
}
