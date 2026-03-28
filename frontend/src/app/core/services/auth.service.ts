import { Injectable } from "@angular/core";
import { HttpClient } from "@angular/common/http";
import { AuthRequest, AuthResponse } from "../../shared/models/auth.model";
import { environment } from "../../../environments/environment";
import { Observable } from "rxjs";

@Injectable({ providedIn: 'root' })
export class AuthService {
    private apiUrl = environment.apiUrl;
    constructor(private http: HttpClient) {}

    login(request: AuthRequest): Observable<AuthResponse> {
        return this.http.post<AuthResponse>(`${this.apiUrl}/auth/login`, request);
    }

    register(request: AuthRequest): Observable<AuthResponse> {
        return this.http.post<AuthResponse>(`${this.apiUrl}/auth/register`, request);
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
    }
}