import { Injectable } from "@angular/core";
import { HttpClient } from "@angular/common/http";
import { AuthRequest, AuthResponse, TokenRefreshResponse } from "../../shared/models/auth.model";
import { environment } from "../../../environments/environment";
import { Observable, tap } from "rxjs";

@Injectable({ providedIn: 'root' })
export class AuthService {
    private apiUrl = environment.apiUrl;
    constructor(private http: HttpClient) {}

    login(request: AuthRequest): Observable<AuthResponse> {
        return this.http.post<AuthResponse>(`${this.apiUrl}/auth/login`, request).pipe(
            tap(response => {
                localStorage.setItem('token', response.token);
                localStorage.setItem('refreshToken', response.refreshToken);
            })
        );
    }

    register(request: AuthRequest): Observable<AuthResponse> {
        return this.http.post<AuthResponse>(`${this.apiUrl}/auth/register`, request);
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

    getToken(): string | null {
        return localStorage.getItem('token');
    }

    getRole(): string | null {
        const token = this.getToken();
        if (!token) return null;
        try {
            const payload = JSON.parse(atob(token.split('.')[1]));
            return payload.role || null;
        } catch {
            return null;
        }
    }

    hasRole(...roles: string[]): boolean {
        const userRole = this.getRole();
        return userRole !== null && roles.includes(userRole);
    }

    isLoggedIn(): boolean {
        return this.getToken() !== null;
    }

    logout(): void {
        localStorage.removeItem('token');
        localStorage.removeItem('refreshToken');
    }
}
