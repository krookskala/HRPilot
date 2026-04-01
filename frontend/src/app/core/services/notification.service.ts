import { Injectable } from "@angular/core";
import { HttpClient } from "@angular/common/http";
import { Observable } from "rxjs";
import { environment } from "../../../environments/environment";
import { NotificationItem } from "../../shared/models/notification.model";
import { Page } from "../../shared/models/page.model";

@Injectable({ providedIn: 'root' })
export class NotificationService {
    private apiUrl = environment.apiUrl;

    constructor(private http: HttpClient) {}

    getNotifications(page = 0, size = 20): Observable<Page<NotificationItem>> {
        return this.http.get<Page<NotificationItem>>(`${this.apiUrl}/notifications?page=${page}&size=${size}`);
    }

    markAsRead(id: number): Observable<NotificationItem> {
        return this.http.put<NotificationItem>(`${this.apiUrl}/notifications/${id}/read`, {});
    }

    markAllAsRead(): Observable<void> {
        return this.http.put<void>(`${this.apiUrl}/notifications/read-all`, {});
    }
}
