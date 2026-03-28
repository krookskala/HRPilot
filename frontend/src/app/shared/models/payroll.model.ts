export enum PayrollStatus {
    DRAFT = 'DRAFT',
    PAID = 'PAID'
}

export interface PayrollRecord {
    id: number;
    employeeId: number;
    employeeFullName: string;
    year: number;
    month: number;
    baseSalary: number;
    bonus: number;
    deductions: number;
    netSalary: number;
    status: PayrollStatus;
}

export interface CreatePayrollRequest {
    employeeId: number;
    year: number;
    month: number;
    baseSalary: number;
    bonus: number;
    deductions: number;
}