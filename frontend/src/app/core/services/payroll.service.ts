import { Injectable } from "@angular/core";
import { HttpClient } from "@angular/common/http";
import { environment } from "../../../environments/environment";
import { Observable } from "rxjs";
import { CreatePayrollRequest, PayrollRecord } from "../../shared/models/payroll.model";

@Injectable({ providedIn: 'root' })
export class PayrollService {
    private apiUrl = environment.apiUrl;
    constructor(private http: HttpClient) {}

    getAllPayrolls(): Observable<PayrollRecord[]> {
        return this.http.get<PayrollRecord[]>(`${this.apiUrl}/payrolls`);
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