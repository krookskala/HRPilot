import { Injectable } from "@angular/core";
import { HttpClient } from "@angular/common/http";
import { BehaviorSubject, map, Observable, tap } from "rxjs";
import { environment } from "../../../environments/environment";
import { NotificationItem } from "../../shared/models/notification.model";
import { normalizePage, Page, RawPageResponse } from "../../shared/models/page.model";

@Injectable({ providedIn: 'root' })
export class NotificationService {
    private apiUrl = environment.apiUrl;
    private _unreadCount$ = new BehaviorSubject<number>(0);
    unreadCount$ = this._unreadCount$.asObservable();

    constructor(private http: HttpClient) {}

    getNotifications(page = 0, size = 20): Observable<Page<NotificationItem>> {
        return this.http
            .get<RawPageResponse<NotificationItem>>(`${this.apiUrl}/notifications?page=${page}&size=${size}`)
            .pipe(map(normalizePage));
    }

    getUnreadCount(): Observable<number> {
        return this.http.get<number>(`${this.apiUrl}/notifications/unread-count`);
    }

    refreshUnreadCount(): void {
        this.getUnreadCount().subscribe({
            next: count => this._unreadCount$.next(count),
            error: () => {}
        });
    }

    markAsRead(id: number): Observable<NotificationItem> {
        return this.http.put<NotificationItem>(`${this.apiUrl}/notifications/${id}/read`, {}).pipe(
            tap(() => this.refreshUnreadCount())
        );
    }

    markAllAsRead(): Observable<void> {
        return this.http.put<void>(`${this.apiUrl}/notifications/read-all`, {}).pipe(
            tap(() => this._unreadCount$.next(0))
        );
    }
}
