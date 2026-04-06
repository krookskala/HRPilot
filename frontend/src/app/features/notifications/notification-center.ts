import { Component, inject, OnInit, OnDestroy, ChangeDetectorRef } from "@angular/core";
import { DatePipe, LowerCasePipe } from "@angular/common";
import { RouterLink } from "@angular/router";
import { MatCardModule } from "@angular/material/card";
import { MatButtonModule } from "@angular/material/button";
import { MatIconModule } from "@angular/material/icon";
import { MatProgressSpinnerModule } from "@angular/material/progress-spinner";
import { MatChipsModule } from "@angular/material/chips";
import { MatTooltipModule } from "@angular/material/tooltip";
import { Subject, takeUntil, finalize } from "rxjs";
import { TranslateModule } from "@ngx-translate/core";
import { NotificationService } from "../../core/services/notification.service";
import { NotificationItem } from "../../shared/models/notification.model";

type FilterType = 'ALL' | 'LEAVE' | 'PAYROLL' | 'SECURITY' | 'SYSTEM';

@Component({
    selector: 'app-notification-center',
    standalone: true,
    imports: [DatePipe, LowerCasePipe, RouterLink, MatCardModule, MatButtonModule, MatIconModule, MatProgressSpinnerModule, MatChipsModule, MatTooltipModule, TranslateModule],
    templateUrl: './notification-center.html',
    styleUrl: './notification-center.scss'
})
export class NotificationCenter implements OnInit, OnDestroy {
    private notificationService = inject(NotificationService);
    private cdr = inject(ChangeDetectorRef);
    private destroy$ = new Subject<void>();

    notifications: NotificationItem[] = [];
    loading = true;
    error = '';
    activeFilter: FilterType = 'ALL';

    filters: { type: FilterType; label: string; icon: string }[] = [
        { type: 'ALL', label: 'All', icon: 'notifications' },
        { type: 'LEAVE', label: 'Leave', icon: 'event_available' },
        { type: 'PAYROLL', label: 'Payroll', icon: 'payments' },
        { type: 'SECURITY', label: 'Security', icon: 'shield' },
        { type: 'SYSTEM', label: 'System', icon: 'info' }
    ];

    get hasUnread(): boolean {
        return this.notifications.some(n => !n.read);
    }

    get filteredNotifications(): NotificationItem[] {
        if (this.activeFilter === 'ALL') return this.notifications;
        return this.notifications.filter(n => this.getFilterGroup(n.type) === this.activeFilter);
    }

    ngOnInit(): void {
        this.loadNotifications();
    }

    loadNotifications(): void {
        this.loading = true;
        this.error = '';
        this.notificationService.getNotifications(0, 50).pipe(
            takeUntil(this.destroy$),
            finalize(() => {
                this.loading = false;
                this.cdr.detectChanges();
            })
        ).subscribe({
            next: page => {
                this.notifications = page.content;
                this.cdr.detectChanges();
            },
            error: () => {
                this.error = 'Failed to load notifications';
                this.cdr.detectChanges();
            }
        });
    }

    setFilter(filter: FilterType): void {
        this.activeFilter = filter;
    }

    markAsRead(id: number): void {
        this.notificationService.markAsRead(id).pipe(takeUntil(this.destroy$)).subscribe({
            next: () => this.loadNotifications(),
            error: () => {
                this.error = 'Failed to mark notification as read';
                this.cdr.detectChanges();
            }
        });
    }

    markAllAsRead(): void {
        this.notificationService.markAllAsRead().pipe(takeUntil(this.destroy$)).subscribe({
            next: () => this.loadNotifications(),
            error: () => {
                this.error = 'Failed to mark all notifications as read';
                this.cdr.detectChanges();
            }
        });
    }

    typeIcon(type: string): string {
        switch (type) {
            case 'LEAVE_EVENT': return 'event_available';
            case 'PAYROLL_EVENT': return 'payments';
            case 'SECURITY_EVENT': return 'shield';
            case 'PASSWORD_RESET': return 'lock_reset';
            case 'INVITATION_CREATED': return 'person_add';
            case 'ACCOUNT_ACTIVATED': return 'how_to_reg';
            default: return 'notifications';
        }
    }

    typeAccent(type: string): string {
        switch (type) {
            case 'LEAVE_EVENT': return 'green';
            case 'PAYROLL_EVENT': return 'indigo';
            case 'SECURITY_EVENT':
            case 'PASSWORD_RESET': return 'slate';
            case 'INVITATION_CREATED':
            case 'ACCOUNT_ACTIVATED': return 'cyan';
            default: return 'orange';
        }
    }

    typeLabel(type: string): string {
        switch (type) {
            case 'LEAVE_EVENT': return 'Leave';
            case 'PAYROLL_EVENT': return 'Payroll';
            case 'SECURITY_EVENT': return 'Security';
            case 'PASSWORD_RESET': return 'Password';
            case 'INVITATION_CREATED': return 'Invitation';
            case 'ACCOUNT_ACTIVATED': return 'Account';
            default: return 'System';
        }
    }

    private getFilterGroup(type: string): FilterType {
        switch (type) {
            case 'LEAVE_EVENT': return 'LEAVE';
            case 'PAYROLL_EVENT': return 'PAYROLL';
            case 'SECURITY_EVENT':
            case 'PASSWORD_RESET': return 'SECURITY';
            case 'INVITATION_CREATED':
            case 'ACCOUNT_ACTIVATED': return 'SYSTEM';
            default: return 'SYSTEM';
        }
    }

    ngOnDestroy(): void {
        this.destroy$.next();
        this.destroy$.complete();
    }
}
