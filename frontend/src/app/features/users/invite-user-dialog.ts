import { Component, inject } from "@angular/core";
import { FormBuilder, ReactiveFormsModule, Validators } from "@angular/forms";
import { MatDialogModule, MatDialogRef } from "@angular/material/dialog";
import { MatFormFieldModule } from "@angular/material/form-field";
import { MatInputModule } from "@angular/material/input";
import { MatButtonModule } from "@angular/material/button";
import { MatSelectModule } from "@angular/material/select";
import { TranslateModule } from "@ngx-translate/core";
import { Role } from "../../shared/models/user.model";

@Component({
    selector: 'app-invite-user-dialog',
    standalone: true,
    imports: [ReactiveFormsModule, MatDialogModule, MatFormFieldModule, MatInputModule, MatButtonModule, MatSelectModule, TranslateModule],
    templateUrl: './invite-user-dialog.html',
    styleUrl: './invite-user-dialog.scss'
})
export class InviteUserDialog {
    private dialogRef = inject(MatDialogRef<InviteUserDialog>);
    private fb = inject(FormBuilder);

    roles = Object.values(Role);

    form = this.fb.group({
        email: ['', [Validators.required, Validators.email]],
        role: [Role.EMPLOYEE, [Validators.required]],
        preferredLang: ['en']
    });

    save(): void {
        if (this.form.invalid) {
            this.form.markAllAsTouched();
            return;
        }

        this.dialogRef.close(this.form.value);
    }

    cancel(): void {
        this.dialogRef.close();
    }
}
