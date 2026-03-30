export interface AuthRequest {
    email: string;
    password: string;
}

export interface AuthResponse {
    accessToken: string;
    refreshToken: string;
    user: import('./user.model').CurrentUser;
}

export interface TokenRefreshResponse {
    accessToken: string;
    refreshToken: string;
}

export interface InvitationDetails {
    email: string;
    role: string;
    preferredLang: string;
    expiresAt: string;
}

export interface PasswordResetResponse {
    message: string;
    resetUrl: string | null;
    expiresAt: string | null;
}

export interface TokenValidationResponse {
    email: string;
    expiresAt: string;
}
