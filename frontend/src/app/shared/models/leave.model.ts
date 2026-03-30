export enum LeaveType {
    ANNUAL = 'ANNUAL',
    SICK = 'SICK',
    UNPAID = 'UNPAID'
}

export enum LeaveStatus {
    PENDING = 'PENDING',
    APPROVED = 'APPROVED',
    REJECTED = 'REJECTED',
    CANCELLED = 'CANCELLED'
}

export interface LeaveRequest {
    id: number;
    employeeId: number;
    employeeFullName: string;
    type: LeaveType;
    startDate: string;
    endDate: string;
    workingDays: number;
    status: LeaveStatus;
    reason: string | null;
    approvedByUserId: number | null;
    approvedByUserEmail: string | null;
    rejectedByUserId: number | null;
    rejectedByUserEmail: string | null;
    cancelledByUserId: number | null;
    cancelledByUserEmail: string | null;
    actionedAt: string | null;
    cancelledAt: string | null;
    rejectionReason: string | null;
    cancellationReason: string | null;
    createdAt: string;
}

export interface LeaveRequestHistory {
    id: number;
    actionType: 'CREATED' | 'APPROVED' | 'REJECTED' | 'CANCELLED';
    fromStatus: LeaveStatus | null;
    toStatus: LeaveStatus;
    actorUserId: number | null;
    actorUserEmail: string | null;
    note: string | null;
    occurredAt: string;
}

export interface LeaveBalance {
    id: number;
    employeeId: number;
    leaveType: LeaveType;
    year: number;
    totalDays: number;
    usedDays: number;
    remainingDays: number;
}

export interface CreateLeaveRequest {
    employeeId: number;
    type: LeaveType;
    startDate: string;
    endDate: string;
    reason: string | null;
}
