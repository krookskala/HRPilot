import { Injectable } from "@angular/core";
import { HttpClient } from "@angular/common/http";
import { environment } from "../../../environments/environment";
import { Observable } from "rxjs";

export interface DashboardMetric {
    label: string;
    value: string;
    icon: string;
    accent: 'cyan' | 'orange' | 'green' | 'indigo' | 'slate';
}

export interface DashboardData {
    role: 'ADMIN' | 'HR_MANAGER' | 'DEPARTMENT_MANAGER' | 'EMPLOYEE';
    headline: string;
    subheadline: string;
    keyMetrics: DashboardMetric[];
    recentActivities: {
        type: string;
        description: string;
        timestamp: string;
    }[];
    leaveOverview: {
        pending: number;
        approved: number;
        rejected: number;
        cancelled: number;
    };
    payrollOverview: {
        draft: number;
        published: number;
        paid: number;
        totalNetSalary: number;
    };
    notificationOverview: {
        unreadNotifications: number;
    };
    teamOverview: {
        managedDepartments: number;
        teamMembers: number;
        pendingLeaveRequests: number;
        approvedLeaveRequests: number;
        paidPayrolls: number;
    } | null;
    personalOverview: {
        pendingLeaveRequests: number;
        approvedLeaveRequests: number;
        availableLeaveDays: number;
        payrollRecords: number;
        unreadNotifications: number;
        profileCompletion: number;
    } | null;
    auditOverview: {
        totalEvents: number;
        recentEvents: number;
    } | null;
    departmentDistribution: {
        department: string;
        count: number;
    }[] | null;
}

@Injectable({ providedIn: 'root' })
export class DashboardService {
    private apiUrl = environment.apiUrl;

    constructor(private http: HttpClient) {}

    getDashboardData(): Observable<DashboardData> {
        return this.http.get<DashboardData>(`${this.apiUrl}/dashboard`);
    }
}
