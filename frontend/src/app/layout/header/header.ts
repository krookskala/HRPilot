import { Component, inject } from "@angular/core";
import { Router, RouterLink } from "@angular/router";
import { MatToolbarModule } from "@angular/material/toolbar";
import { MatButtonModule } from "@angular/material/button";
import { MatIconModule } from "@angular/material/icon";
import { MatTooltipModule } from "@angular/material/tooltip";
import { MatMenuModule } from "@angular/material/menu";
import { AsyncPipe } from "@angular/common";
import { TranslateModule, TranslateService } from "@ngx-translate/core";
import { AuthService } from "../../core/services/auth.service";

@Component({
    selector: 'app-header',
    standalone: true,
    imports: [MatToolbarModule, MatButtonModule, MatIconModule, MatTooltipModule, MatMenuModule, AsyncPipe, RouterLink, TranslateModule],
    templateUrl: './header.html',
    styleUrl: './header.scss'
})

export class Header {
    private router = inject(Router);
    protected authService = inject(AuthService);
    private translate = inject(TranslateService);
    currentUser$ = this.authService.currentUser$;

    get currentLang(): string {
        return this.translate.currentLang || this.translate.defaultLang || 'en';
    }

    switchLang(lang: string): void {
        this.translate.use(lang);
        localStorage.setItem('lang', lang);
    }

    logout() {
        this.authService.logout().subscribe(() => {
            this.router.navigate(['/login']);
        });
    }
}
