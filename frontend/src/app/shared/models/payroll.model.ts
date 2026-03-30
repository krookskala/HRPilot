export enum PayrollStatus {
    DRAFT = 'DRAFT',
    PUBLISHED = 'PUBLISHED',
    PAID = 'PAID'
}

export enum PayrollRunStatus {
    DRAFT = 'DRAFT',
    PUBLISHED = 'PUBLISHED',
    PAID = 'PAID'
}

export interface PayrollComponent {
    id: number | null;
    componentType: 'EARNING' | 'EMPLOYEE_DEDUCTION' | 'EMPLOYER_CONTRIBUTION' | 'TAX';
    code: string;
    label: string;
    amount: number;
}

export interface PayrollRecord {
    id: number;
    employeeId: number;
    employeeFullName: string;
    year: number;
    month: number;
    baseSalary: number;
    grossSalary: number;
    bonus: number;
    deductions: number;
    employeeSocialContributions: number;
    employerSocialContributions: number;
    incomeTax: number;
    netSalary: number;
    taxClass: string;
    status: PayrollStatus;
    runId: number | null;
    publishedAt: string | null;
    paidAt: string | null;
    hasPayslip: boolean;
    components: PayrollComponent[];
}

export interface CreatePayrollRequest {
    employeeId: number;
    year: number;
    month: number;
    baseSalary: number;
    bonus: number;
    deductions: number;
    taxClass?: string | null;
}

export interface PayrollPreviewRequest {
    employeeId: number;
    year: number;
    month: number;
    bonus?: number | null;
    additionalDeduction?: number | null;
    taxClass?: string | null;
}

export interface CreatePayrollRunRequest {
    name: string;
    year: number;
    month: number;
    employeeIds: number[];
    includeAllEmployees?: boolean;
    bonus?: number | null;
    additionalDeduction?: number | null;
    taxClass?: string | null;
}

export interface PayrollRun {
    id: number;
    name: string;
    year: number;
    month: number;
    status: PayrollRunStatus;
    payrollCount: number;
    createdAt: string;
    publishedAt: string | null;
    paidAt: string | null;
}
