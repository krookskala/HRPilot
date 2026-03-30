export interface Employee {
    id: number;
    email: string;
    firstName: string;
    lastName: string;
    position: string;
    salary: number;
    hireDate: string;
    photoUrl: string;
    departmentId: number;
    departmentName: string;
}

export interface EmployeeDocument {
    id: number;
    documentType: 'HR_DOCUMENT';
    title: string;
    description: string | null;
    originalFilename: string;
    contentType: string;
    fileSize: number;
    uploadedByUserId: number | null;
    uploadedByEmail: string | null;
    uploadedAt: string;
}

export interface EmploymentHistoryItem {
    id: number;
    changeType: string;
    oldValue: string | null;
    newValue: string | null;
    changedAt: string;
}

export interface EmployeeDetail extends Employee {
    employmentHistory: EmploymentHistoryItem[];
    documents: EmployeeDocument[];
}

export interface CreateEmployeeRequest {
    userId: number;
    firstName: string;
    lastName: string;
    position: string;
    salary: number;
    hireDate: string;
    departmentId: number | null;
    photoUrl?: string | null;
}
