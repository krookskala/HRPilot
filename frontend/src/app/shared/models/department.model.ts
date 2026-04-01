export interface Department {
    id: number;
    name: string;
    managerEmail: string | null;
    parentDepartmentId: number | null;
    parentDepartmentName: string | null;
}

export interface CreateDepartmentRequest {
    name: string;
    managerId: number | null;
    parentDepartmentId: number | null;
}

export interface UpdateDepartmentRequest {
    name: string;
    managerId: number | null;
    parentDepartmentId: number | null;
}