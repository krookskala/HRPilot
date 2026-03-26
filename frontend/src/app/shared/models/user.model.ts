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
}

export interface CreateUserRequest {
    email: string;
    password: string;
    role: Role;
}

export interface UpdateUserRequest {
    role: Role;
    isActive: boolean;
    preferredLang: string;
}