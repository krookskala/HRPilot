import { Component } from "@angular/core";
import { FormsModule } from "@angular/forms";
import { AuthService } from "../../core/services/auth.service";
import { MatCardModule } from "@angular/material/card";
import { MatFormFieldModule } from "@angular/material/form-field";
import { MatInputModule } from "@angular/material/input";
import { MatButtonModule } from "@angular/material/button";
import { Router } from "@angular/router";

@Component({
    selector: 'app-login',
    standalone: true,
    imports: [FormsModule, MatCardModule, MatFormFieldModule, MatInputModule, MatButtonModule],
    templateUrl: './login.html',
    styleUrl: './login.scss'
})

export class Login {
    email = '';
    password = '';
    error = '';

    constructor(private authService: AuthService, private router: Router) {}

    login() {
        this.authService.login({ email: this.email, password: this.password
         }).subscribe({
            next: (response) => {
                localStorage.setItem('token', response.token);
                this.router.navigate(['/dashboard']);
            },
            error: (err) => {
                this.error = 'Email or Password Wrong';
            }
         });
    }
}