import { Injectable } from "@angular/core";
import { HttpClient, HttpParams } from "@angular/common/http";
import { map, Observable } from "rxjs";
import { environment } from "../../../environments/environment";
import { InviteUserRequest, UpdateUserRequest, User, UserInvitationResponse } from "../../shared/models/user.model";
import { normalizePage, Page, RawPageResponse } from "../../shared/models/page.model";

@Injectable({ providedIn: 'root' })
export class UserService {
    private apiUrl = environment.apiUrl;

    constructor(private http: HttpClient) {}

    getAll(filters: { email?: string; role?: string; isActive?: boolean | null }, page = 0, size = 20): Observable<Page<User>> {
        let params = new HttpParams()
            .set('page', page.toString())
            .set('size', size.toString());

        if (filters.email) params = params.set('email', filters.email);
        if (filters.role) params = params.set('role', filters.role);
        if (filters.isActive !== null && filters.isActive !== undefined) {
            params = params.set('isActive', String(filters.isActive));
        }

        return this.http
            .get<RawPageResponse<User>>(`${this.apiUrl}/users`, { params })
            .pipe(map(normalizePage));
    }

    inviteUser(request: InviteUserRequest): Observable<UserInvitationResponse> {
        return this.http.post<UserInvitationResponse>(`${this.apiUrl}/users/invite`, request);
    }

    updateUser(id: number, request: UpdateUserRequest): Observable<User> {
        return this.http.put<User>(`${this.apiUrl}/users/${id}`, request);
    }

    resendInvite(id: number): Observable<UserInvitationResponse> {
        return this.http.post<UserInvitationResponse>(`${this.apiUrl}/users/${id}/resend-invite`, {});
    }

    deleteUser(id: number): Observable<void> {
        return this.http.delete<void>(`${this.apiUrl}/users/${id}`);
    }
}
