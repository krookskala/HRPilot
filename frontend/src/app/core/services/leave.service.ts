import { Injectable } from "@angular/core";
import { HttpClient, HttpParams } from "@angular/common/http";
import { environment } from "../../../environments/environment";
import { Observable } from "rxjs";
import { CreateLeaveRequest, LeaveBalance, LeaveRequest, LeaveRequestHistory, LeaveStatus, LeaveType } from "../../shared/models/leave.model";
import { Page } from "../../shared/models/page.model";

@Injectable({ providedIn: 'root' })
export class LeaveService {
    private apiUrl = environment.apiUrl;
    constructor(private http: HttpClient) {}

    getAll(page = 0, size = 20, filters?: {
        status?: LeaveStatus | null;
        type?: LeaveType | null;
        employeeId?: number | null;
        departmentId?: number | null;
    }): Observable<Page<LeaveRequest>> {
        let params = new HttpParams()
            .set('page', String(page))
            .set('size', String(size));

        if (filters?.status) {
            params = params.set('status', filters.status);
        }
        if (filters?.type) {
            params = params.set('type', filters.type);
        }
        if (filters?.employeeId) {
            params = params.set('employeeId', String(filters.employeeId));
        }
        if (filters?.departmentId) {
            params = params.set('departmentId', String(filters.departmentId));
        }

        return this.http.get<Page<LeaveRequest>>(`${this.apiUrl}/leave-requests`, { params });
    }

    getByEmployee(employeeId: number): Observable<LeaveRequest[]> {
        return this.http.get<LeaveRequest[]>(`${this.apiUrl}/leave-requests/employee/${employeeId}`);
    }

    getMine(): Observable<LeaveRequest[]> {
        return this.http.get<LeaveRequest[]>(`${this.apiUrl}/me/leave-requests`);
    }

    create(request: CreateLeaveRequest): Observable<LeaveRequest> {
        return this.http.post<LeaveRequest>(`${this.apiUrl}/leave-requests`, request);
    }

    approve(id: number): Observable<LeaveRequest> {
        return this.http.put<LeaveRequest>(`${this.apiUrl}/leave-requests/${id}/approve`, {});
    }

    reject(id: number, reason: string): Observable<LeaveRequest> {
        return this.http.put<LeaveRequest>(`${this.apiUrl}/leave-requests/${id}/reject`, { reason });
    }

    cancel(id: number, reason: string): Observable<LeaveRequest> {
        return this.http.put<LeaveRequest>(`${this.apiUrl}/leave-requests/${id}/cancel`, { reason });
    }

    getHistory(id: number): Observable<LeaveRequestHistory[]> {
        return this.http.get<LeaveRequestHistory[]>(`${this.apiUrl}/leave-requests/${id}/history`);
    }

    getBalances(employeeId: number, year?: number): Observable<LeaveBalance[]> {
        const yearParam = year ? `?year=${year}` : '';
        return this.http.get<LeaveBalance[]>(`${this.apiUrl}/leave-requests/balances/${employeeId}${yearParam}`);
    }

    getMyBalances(year?: number): Observable<LeaveBalance[]> {
        const yearParam = year ? `?year=${year}` : '';
        return this.http.get<LeaveBalance[]>(`${this.apiUrl}/me/leave-balances${yearParam}`);
    }
}
