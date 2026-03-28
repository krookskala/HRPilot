import { Injectable } from "@angular/core";
import { HttpClient } from "@angular/common/http";
import { environment } from "../../../environments/environment";
import { Observable } from "rxjs";
import { CreatePayrollRequest, PayrollRecord } from "../../shared/models/payroll.model";
import { Page } from "../../shared/models/page.model";

@Injectable({ providedIn: 'root' })
export class PayrollService {
    private apiUrl = environment.apiUrl;
    constructor(private http: HttpClient) {}

    getAllPayrolls(page = 0, size = 20): Observable<Page<PayrollRecord>> {
        return this.http.get<Page<PayrollRecord>>(`${this.apiUrl}/payrolls?page=${page}&size=${size}`);
    }

    getByEmployee(employeeId: number): Observable<PayrollRecord[]> {
        return this.http.get<PayrollRecord[]>(`${this.apiUrl}/payrolls/employee/${employeeId}`);
    }

    create(request: CreatePayrollRequest): Observable<PayrollRecord> {
        return this.http.post<PayrollRecord>(`${this.apiUrl}/payrolls`, request);
    }

    markAsPaid(id: number): Observable<PayrollRecord> {
        return this.http.put<PayrollRecord>(`${this.apiUrl}/payrolls/${id}/pay`, {});
    }
}