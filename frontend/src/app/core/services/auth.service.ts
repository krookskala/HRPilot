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
}