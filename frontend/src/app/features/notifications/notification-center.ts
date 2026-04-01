import { Component, inject, OnInit, OnDestroy, ChangeDetectorRef } from "@angular/core";
import { DatePipe } from "@angular/common";
import { MatCardModule } from "@angular/material/card";
import { MatButtonModule } from "@angular/material/button";
import { MatIconModule } from "@angular/material/icon";
import { MatProgressSpinnerModule } from "@angular/material/progress-spinner";
import { Subject, takeUntil, finalize } from "rxjs";
import { NotificationService } from "../../core/services/notification.service";
import { NotificationItem } from "../../shared/models/notification.model";

@Component({
    selector: 'app-notification-center',
    standalone: true,
    imports: [DatePipe, MatCardModule, MatButtonModule, MatIconModule, MatProgressSpinnerModule],
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

    get hasUnread(): boolean {
        return this.notifications.some(n => !n.read);
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

    markAsRead(id: number): void {
        this.notificationService.markAsRead(id).pipe(takeUntil(this.destroy$)).subscribe({
            next: () => {
                this.loadNotifications();
            },
            error: () => {
                this.error = 'Failed to mark notification as read';
                this.cdr.detectChanges();
            }
        });
    }

    markAllAsRead(): void {
        this.notificationService.markAllAsRead().pipe(takeUntil(this.destroy$)).subscribe({
            next: () => {
                this.loadNotifications();
            },
            error: () => {
                this.error = 'Failed to mark all notifications as read';
                this.cdr.detectChanges();
            }
        });
    }

    ngOnDestroy(): void {
        this.destroy$.next();
        this.destroy$.complete();
    }
}
