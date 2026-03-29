import { Injectable } from "@angular/core";
import { HttpClient } from "@angular/common/http";
import { environment } from "../../../environments/environment";
import { Observable } from "rxjs";

export interface DashboardData {
    counts: {
        employees: number;
        departments: number;
        leaveRequests: number;
        payrollRecords: number;
    };
    recentActivities: {
        type: string;
        description: string;
        timestamp: string;
    }[];
    leaveOverview: {
        pending: number;
        approved: number;
        rejected: number;
    };
    payrollOverview: {
        draft: number;
        paid: number;
        totalNetSalary: number;
    };
}

@Injectable({ providedIn: 'root' })
export class DashboardService {
    private apiUrl = environment.apiUrl;
    constructor(private http: HttpClient) {}

    getDashboardData(): Observable<DashboardData> {
        return this.http.get<DashboardData>(`${this.apiUrl}/dashboard`);
    }
}
