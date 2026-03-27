export enum LeaveType {
    ANNUAL = 'ANNUAL',
    SICK = 'SICK',
    UNPAID = 'UNPAID'
}

export enum LeaveStatus {
    PENDING = 'PENDING',
    APPROVED = 'APPROVED',
    REJECTED = 'REJECTED'
}

export interface LeaveRequest {
    id: number;
    employeeId: number;
    employeeFullName: string;
    type: LeaveType;
    startDate: string;
    endDate: string;
    status: LeaveStatus;
    reason: string;
}

export interface CreateLeaveRequest {
    employeeId: number;
    type: LeaveType;
    startDate: string;
    endDate: string;
    reason: string;
}