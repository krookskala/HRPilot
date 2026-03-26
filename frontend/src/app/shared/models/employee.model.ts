export interface Employee {
    id: number;
    email: string;
    firstName: string;
    lastName: string;
    position: string;
    salary: number;
    hireDate: string;
    photoUrl: string;
}

export interface CreateEmployeeRequest {
    userId: number;
    firstName: string;
    lastName: string;
    position: string;
    salary: number;
    hireDate: string;
    departmentId: number;
    photoUrl: string;
}