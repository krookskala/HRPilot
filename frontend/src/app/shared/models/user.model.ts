export enum Role {
    ADMIN = 'ADMIN',
    HR_MANAGER = 'HR_MANAGER',
    DEPARTMENT_MANAGER = 'DEPARTMENT_MANAGER',
    EMPLOYEE = 'EMPLOYEE'
}

export interface User {
    id: number;
    email: string;
    role: Role;
    isActive: boolean;
    preferredLang: string;
    employeeId: number | null;
    pendingInvitation: boolean;
    activatedAt: string | null;
    lastLoginAt: string | null;
}

export interface InviteUserRequest {
    email: string;
    role: Role;
    preferredLang?: string | null;
}

export interface UserInvitationResponse {
    user: User;
    inviteToken: string;
    inviteUrl: string;
    expiresAt: string;
}

export interface UpdateUserRequest {
    role?: Role | null;
    isActive?: boolean | null;
    preferredLang?: string | null;
}

export interface CurrentUser {
    id: number;
    email: string;
    role: Role;
    isActive: boolean;
    preferredLang: string;
    employeeId: number | null;
    firstName: string | null;
    lastName: string | null;
    departmentId: number | null;
    departmentName: string | null;
    unreadNotifications: number;
    activatedAt: string | null;
    lastLoginAt: string | null;
}

export interface CurrentUserProfile {
    id: number;
    email: string;
    role: Role;
    isActive: boolean;
    preferredLang: string;
    activatedAt: string | null;
    lastLoginAt: string | null;
    unreadNotifications: number;
    employee: {
        employeeId: number;
        firstName: string;
        lastName: string;
        position: string;
        hireDate: string;
        photoUrl: string | null;
        departmentId: number | null;
        departmentName: string | null;
        employmentHistory: import("./employee.model").EmploymentHistoryItem[];
        documents: import("./employee.model").EmployeeDocument[];
    } | null;
}
