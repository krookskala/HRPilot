import { Component, inject, OnInit, OnDestroy } from "@angular/core";
import { Router, RouterLink } from "@angular/router";
import { MatToolbarModule } from "@angular/material/toolbar";
import { MatButtonModule } from "@angular/material/button";
import { MatIconModule } from "@angular/material/icon";
import { MatTooltipModule } from "@angular/material/tooltip";
import { MatMenuModule } from "@angular/material/menu";
import { MatSlideToggleModule } from "@angular/material/slide-toggle";
import { MatBadgeModule } from "@angular/material/badge";
import { AsyncPipe } from "@angular/common";
import { TranslateModule, TranslateService } from "@ngx-translate/core";
import { Subject, takeUntil, interval, switchMap } from "rxjs";
import { AuthService } from "../../core/services/auth.service";
import { EmployeeService } from "../../core/services/employee.service";
import { NotificationService } from "../../core/services/notification.service";

@Component({
    selector: 'app-header',
    standalone: true,
    imports: [MatToolbarModule, MatButtonModule, MatIconModule, MatTooltipModule, MatMenuModule, MatSlideToggleModule, AsyncPipe, RouterLink, TranslateModule, MatBadgeModule],
    templateUrl: './header.html',
    styleUrl: './header.scss'
})

export class Header implements OnInit, OnDestroy {
    private router = inject(Router);
    protected authService = inject(AuthService);
    private employeeService = inject(EmployeeService);
    private notificationService = inject(NotificationService);
    private translate = inject(TranslateService);
    currentUser$ = this.authService.currentUser$;
    avatarUrl: string | null = null;
    darkMode = false;
    unreadCount = 0;
    private destroy$ = new Subject<void>();

    ngOnInit(): void {
        this.darkMode = localStorage.getItem('darkMode') === 'true';
        this.applyTheme();

        this.currentUser$.pipe(takeUntil(this.destroy$)).subscribe(user => {
            if (user?.employeeId) {
                this.loadAvatar(user.employeeId);
            } else {
                this.revokeAvatar();
            }
        });

        this.notificationService.unreadCount$.pipe(takeUntil(this.destroy$)).subscribe(count => {
            this.unreadCount = count;
        });

        this.notificationService.refreshUnreadCount();
        interval(60000).pipe(
            takeUntil(this.destroy$),
            switchMap(() => this.notificationService.getUnreadCount())
        ).subscribe({
            next: count => this.notificationService['_unreadCount$'].next(count),
            error: () => {}
        });
    }

    ngOnDestroy(): void {
        this.destroy$.next();
        this.destroy$.complete();
        this.revokeAvatar();
    }

    get currentLang(): string {
        return this.translate.currentLang || this.translate.defaultLang || 'en';
    }

    switchLang(lang: string): void {
        this.translate.use(lang);
        localStorage.setItem('lang', lang);
    }

    toggleDarkMode(): void {
        this.darkMode = !this.darkMode;
        localStorage.setItem('darkMode', String(this.darkMode));
        this.applyTheme();
    }

    logout() {
        this.authService.logout().subscribe(() => {
            this.router.navigate(['/login']);
        });
    }

    private loadAvatar(employeeId: number): void {
        this.employeeService.downloadPhoto(employeeId).pipe(takeUntil(this.destroy$)).subscribe({
            next: blob => {
                this.revokeAvatar();
                this.avatarUrl = URL.createObjectURL(blob);
            },
            error: () => {}
        });
    }

    private revokeAvatar(): void {
        if (this.avatarUrl) {
            URL.revokeObjectURL(this.avatarUrl);
            this.avatarUrl = null;
        }
    }

    private applyTheme(): void {
        document.documentElement.classList.toggle('dark-theme', this.darkMode);
    }
}
