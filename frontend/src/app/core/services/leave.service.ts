import { Injectable } from "@angular/core";
import { HttpClient } from "@angular/common/http";
import { environment } from "../../../environments/environment";
import { Observable } from "rxjs";
import { CreateLeaveRequest, LeaveRequest } from "../../shared/models/leave.model";

@Injectable({ providedIn: 'root' })
export class LeaveService {
    private apiUrl = environment.apiUrl;
    constructor(private http: HttpClient) {}

    getAll(): Observable<LeaveRequest[]> {
        return this.http.get<LeaveRequest[]>(`${this.apiUrl}/leave-requests`);
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
}