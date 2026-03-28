import { Component, inject } from "@angular/core";
import { RouterLink, RouterLinkActive, Router } from "@angular/router";
import { MatListModule } from "@angular/material/list";
import { AuthService } from "../../core/services/auth.service";
import { NgIf } from "@angular/common";

@Component({
    selector: 'app-sidebar',
    standalone: true,
    imports: [RouterLink, RouterLinkActive, MatListModule, NgIf],
    templateUrl: './sidebar.html',
    styleUrl: './sidebar.scss'
})

export class Sidebar {
    private authService = inject(AuthService);
    private router = inject(Router);

    get isAdmin(): boolean {
        return this.authService.hasRole('ADMIN');
    }

    get isAdminOrHR(): boolean {
        return this.authService.hasRole('ADMIN', 'HR_MANAGER');
    }

    logout(): void {
        this.authService.logout();
        this.router.navigate(['/login']);
    }
}