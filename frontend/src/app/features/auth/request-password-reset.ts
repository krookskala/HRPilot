import { Component, inject } from "@angular/core";
import { FormBuilder, ReactiveFormsModule, Validators } from "@angular/forms";
import { RouterLink } from "@angular/router";
import { MatCardModule } from "@angular/material/card";
import { MatFormFieldModule } from "@angular/material/form-field";
import { MatInputModule } from "@angular/material/input";
import { MatButtonModule } from "@angular/material/button";
import { TranslateModule, TranslateService } from "@ngx-translate/core";
import { AuthService } from "../../core/services/auth.service";

@Component({
    selector: 'app-request-password-reset',
    standalone: true,
    imports: [ReactiveFormsModule, RouterLink, MatCardModule, MatFormFieldModule, MatInputModule, MatButtonModule, TranslateModule],
    templateUrl: './request-password-reset.html',
    styleUrl: './request-password-reset.scss'
})
export class RequestPasswordReset {
    private fb = inject(FormBuilder);
    private authService = inject(AuthService);
    private translate = inject(TranslateService);

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
                this.message = this.translate.instant('auth.resetLinkSent');
                this.error = '';
            },
            error: () => {
                this.message = this.translate.instant('auth.resetLinkSent');
                this.error = '';
            }
        });
    }
}
