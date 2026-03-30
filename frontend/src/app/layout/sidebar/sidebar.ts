import { Component, inject } from "@angular/core";
import { RouterLink, RouterLinkActive, Router } from "@angular/router";
import { MatListModule } from "@angular/material/list";
import { MatIconModule } from "@angular/material/icon";
import { MatDividerModule } from "@angular/material/divider";
import { TranslateModule } from "@ngx-translate/core";
import { AuthService } from "../../core/services/auth.service";
import { NgIf } from "@angular/common";

@Component({
    selector: 'app-sidebar',
    standalone: true,
    imports: [RouterLink, RouterLinkActive, MatListModule, MatIconModule, MatDividerModule, NgIf, TranslateModule],
    templateUrl: './sidebar.html',
    styleUrl: './sidebar.scss'
})

export class Sidebar {
    protected authService = inject(AuthService);
    private router = inject(Router);

    get isAdmin(): boolean {
        return this.authService.hasRole('ADMIN');
    }

    get isAdminOrHR(): boolean {
        return this.authService.hasRole('ADMIN', 'HR_MANAGER');
    }

    logout(): void {
        this.authService.logout().subscribe(() => {
            this.router.navigate(['/login']);
        });
    }
}
