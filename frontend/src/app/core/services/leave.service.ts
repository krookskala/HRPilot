import { Injectable } from "@angular/core";
import { HttpClient } from "@angular/common/http";
import { environment } from "../../../environments/environment";
import { Observable } from "rxjs";
import { CreateLeaveRequest, LeaveBalance, LeaveRequest } from "../../shared/models/leave.model";
import { Page } from "../../shared/models/page.model";

@Injectable({ providedIn: 'root' })
export class LeaveService {
    private apiUrl = environment.apiUrl;
    constructor(private http: HttpClient) {}

    getAll(page = 0, size = 20): Observable<Page<LeaveRequest>> {
        return this.http.get<Page<LeaveRequest>>(`${this.apiUrl}/leave-requests?page=${page}&size=${size}`);
    }

    getByEmployee(employeeId: number): Observable<LeaveRequest[]> {
        return this.http.get<LeaveRequest[]>(`${this.apiUrl}/leave-requests/employee/${employeeId}`);
    } 

    create(request: CreateLeaveRequest): Observable<LeaveRequest> {
        return this.http.post<LeaveRequest>(`${this.apiUrl}/leave-requests`, request);
    }

    approve(id: number): Observable<LeaveRequest> {
        return this.http.put<LeaveRequest>(`${this.apiUrl}/leave-requests/${id}/approve`, {});
    }

    reject(id: number): Observable<LeaveRequest> {
        return this.http.put<LeaveRequest>(`${this.apiUrl}/leave-requests/${id}/reject`, {});
    }

    getBalances(employeeId: number, year?: number): Observable<LeaveBalance[]> {
        const yearParam = year ? `?year=${year}` : '';
        return this.http.get<LeaveBalance[]>(`${this.apiUrl}/leave-requests/balances/${employeeId}${yearParam}`);
    }
}