export interface AuthRequest {
    email: string;
    password: string;
}

export interface AuthResponse {
    token: string;
    refreshToken: string;
}

export interface TokenRefreshResponse {
    accessToken: string;
    refreshToken: string;
}
