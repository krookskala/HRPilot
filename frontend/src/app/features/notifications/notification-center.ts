import { Component, inject, OnInit } from "@angular/core";
import { NgFor, NgIf, DatePipe } from "@angular/common";
import { MatCardModule } from "@angular/material/card";
import { MatButtonModule } from "@angular/material/button";
import { MatProgressSpinnerModule } from "@angular/material/progress-spinner";
import { finalize } from "rxjs";
import { NotificationService } from "../../core/services/notification.service";
import { NotificationItem } from "../../shared/models/notification.model";

@Component({
    selector: 'app-notification-center',
    standalone: true,
    imports: [NgIf, NgFor, DatePipe, MatCardModule, MatButtonModule, MatProgressSpinnerModule],
    templateUrl: './notification-center.html',
    styleUrl: './notification-center.scss'
})
export class NotificationCenter implements OnInit {
    private notificationService = inject(NotificationService);

    notifications: NotificationItem[] = [];
    loading = true;
    error = '';

    ngOnInit(): void {
        this.loadNotifications();
    }

    loadNotifications(): void {
        this.loading = true;
        this.error = '';
        this.notificationService.getNotifications(0, 50).pipe(
            finalize(() => this.loading = false)
        ).subscribe({
            next: page => {
                this.notifications = page.content;
            },
            error: () => {
                this.error = 'Failed to load notifications';
            }
        });
    }

    markAsRead(id: number): void {
        this.notificationService.markAsRead(id).subscribe({
            next: () => this.loadNotifications()
        });
    }
}
