import { Injectable } from "@angular/core";
import { HttpClient } from "@angular/common/http";
import { BehaviorSubject, catchError, map, Observable, of, tap } from "rxjs";
import { environment } from "../../../environments/environment";
import {
    AuthRequest,
    AuthResponse,
    InvitationDetails,
    PasswordResetResponse,
    TokenRefreshResponse,
    TokenValidationResponse
} from "../../shared/models/auth.model";
import { CurrentUser, CurrentUserProfile } from "../../shared/models/user.model";

@Injectable({ providedIn: 'root' })
export class AuthService {
    private apiUrl = environment.apiUrl;
    private currentUserSubject = new BehaviorSubject<CurrentUser | null>(null);

    currentUser$ = this.currentUserSubject.asObservable();

    constructor(private http: HttpClient) {}

    login(request: AuthRequest): Observable<CurrentUser> {
        return this.http.post<AuthResponse>(`${this.apiUrl}/auth/login`, request).pipe(
            tap(response => {
                this.storeSession(response);
                if (response.user) {
                    this.currentUserSubject.next(response.user);
                }
            }),
            map(response => response.user)
        );
    }

    getInvitation(token: string): Observable<InvitationDetails> {
        return this.http.get<InvitationDetails>(`${this.apiUrl}/auth/invitations/${token}`);
    }

    acceptInvitation(token: string, password: string): Observable<CurrentUser> {
        return this.http.post<AuthResponse>(`${this.apiUrl}/auth/invitations/accept`, { token, password }).pipe(
            tap(response => {
                this.storeSession(response);
                if (response.user) {
                    this.currentUserSubject.next(response.user);
                }
            }),
            map(response => response.user)
        );
    }

    requestPasswordReset(email: string): Observable<PasswordResetResponse> {
        return this.http.post<PasswordResetResponse>(`${this.apiUrl}/auth/password/request`, { email });
    }

    validatePasswordResetToken(token: string): Observable<TokenValidationResponse> {
        return this.http.get<TokenValidationResponse>(`${this.apiUrl}/auth/password/${token}`);
    }

    resetPassword(token: string, password: string): Observable<void> {
        return this.http.post<void>(`${this.apiUrl}/auth/password/reset`, { token, password });
    }

    refreshToken(): Observable<TokenRefreshResponse> {
        const refreshToken = localStorage.getItem('refreshToken');
        return this.http.post<TokenRefreshResponse>(`${this.apiUrl}/auth/refresh`, { refreshToken }).pipe(
            tap(response => {
                localStorage.setItem('token', response.accessToken);
                localStorage.setItem('refreshToken', response.refreshToken);
            })
        );
    }

    ensureCurrentUser(force = false): Observable<CurrentUser | null> {
        const token = this.getToken();
        if (!token) {
            this.currentUserSubject.next(null);
            return of(null);
        }

        if (!force && this.currentUserSubject.value) {
            return of(this.currentUserSubject.value);
        }

        return this.http.get<CurrentUser>(`${this.apiUrl}/me`).pipe(
            tap(user => this.currentUserSubject.next(user)),
            catchError(() => {
                this.clearSession();
                return of(null);
            })
        );
    }

    getMyProfile(): Observable<CurrentUserProfile> {
        return this.http.get<CurrentUserProfile>(`${this.apiUrl}/me/profile`);
    }

    changeLanguage(preferredLang: string): Observable<void> {
        return this.http.put<void>(`${this.apiUrl}/me/language`, { preferredLang });
    }

    changePassword(currentPassword: string, newPassword: string): Observable<AuthResponse> {
        return this.http.put<AuthResponse>(`${this.apiUrl}/me/password`, { currentPassword, newPassword }).pipe(
            tap(response => {
                this.storeSession(response);
                if (response.user) {
                    this.currentUserSubject.next(response.user);
                }
            })
        );
    }

    changeEmail(newEmail: string, password: string): Observable<AuthResponse> {
        return this.http.put<AuthResponse>(`${this.apiUrl}/me/email`, { newEmail, password }).pipe(
            tap(response => {
                this.storeSession(response);
                if (response.user) {
                    this.currentUserSubject.next(response.user);
                }
            })
        );
    }

    getToken(): string | null {
        return localStorage.getItem('token');
    }

    getRefreshToken(): string | null {
        return localStorage.getItem('refreshToken');
    }

    getCurrentUserSnapshot(): CurrentUser | null {
        return this.currentUserSubject.value;
    }

    hasRole(...roles: string[]): boolean {
        const user = this.currentUserSubject.value;
        return !!user && roles.includes(user.role);
    }

    logout(): Observable<void> {
        const refreshToken = this.getRefreshToken();
        if (!refreshToken) {
            this.clearSession();
            return of(void 0);
        }

        return this.http.post<void>(`${this.apiUrl}/auth/logout`, { refreshToken }).pipe(
            catchError(() => of(void 0)),
            tap(() => this.clearSession())
        );
    }

    clearSession(): void {
        localStorage.removeItem('token');
        localStorage.removeItem('refreshToken');
        this.currentUserSubject.next(null);
    }

    private storeSession(response: AuthResponse): void {
        localStorage.setItem('token', response.accessToken);
        localStorage.setItem('refreshToken', response.refreshToken);
    }
}
