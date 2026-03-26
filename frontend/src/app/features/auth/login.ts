import { Component } from "@angular/core";
import { FormsModule } from "@angular/forms";
import { AuthService } from "../../core/services/auth.service";


@Component({
    selector: 'app-login',
    standalone: true,
    imports: [FormsModule],
    templateUrl: './login.html',
    styleUrl: './login.scss'
})

export class Login {
    email = '';
    password = '';
    error = '';

    constructor(private authService: AuthService) {}

    login() {
        this.authService.login({ email: this.email, password: this.password
         }).subscribe({
            next: (response) => {
                localStorage.setItem('token', response.token);
            },
            error: (err) => {
                this.error = 'Email or Password Wrong';
            }
         });
    }
}