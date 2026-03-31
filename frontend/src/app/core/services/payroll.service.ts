import { Injectable } from "@angular/core";
import { HttpClient } from "@angular/common/http";
import { environment } from "../../../environments/environment";
import { Observable } from "rxjs";
import { CreatePayrollRequest, CreatePayrollRunRequest, PayrollComponent, PayrollPreviewRequest, PayrollRecord, PayrollRun } from "../../shared/models/payroll.model";
import { Page } from "../../shared/models/page.model";

@Injectable({ providedIn: 'root' })
export class PayrollService {
    private apiUrl = environment.apiUrl;

    constructor(private http: HttpClient) {}

    getAllPayrolls(page = 0, size = 20): Observable<Page<PayrollRecord>> {
        return this.http.get<Page<PayrollRecord>>(`${this.apiUrl}/payrolls?page=${page}&size=${size}`);
    }

    getByEmployee(employeeId: number, page = 0, size = 20): Observable<Page<PayrollRecord>> {
        return this.http.get<Page<PayrollRecord>>(`${this.apiUrl}/payrolls/employee/${employeeId}?page=${page}&size=${size}`);
    }

    create(request: CreatePayrollRequest): Observable<PayrollRecord> {
        return this.http.post<PayrollRecord>(`${this.apiUrl}/payrolls`, request);
    }

    preview(request: PayrollPreviewRequest): Observable<PayrollRecord> {
        return this.http.post<PayrollRecord>(`${this.apiUrl}/payrolls/preview`, request);
    }

    createRun(request: CreatePayrollRunRequest): Observable<PayrollRun> {
        return this.http.post<PayrollRun>(`${this.apiUrl}/payrolls/runs`, request);
    }

    getRuns(page = 0, size = 20): Observable<Page<PayrollRun>> {
        return this.http.get<Page<PayrollRun>>(`${this.apiUrl}/payrolls/runs?page=${page}&size=${size}`);
    }

    markAsPaid(id: number): Observable<PayrollRecord> {
        return this.http.put<PayrollRecord>(`${this.apiUrl}/payrolls/${id}/pay`, {});
    }

    publishRun(id: number): Observable<PayrollRun> {
        return this.http.put<PayrollRun>(`${this.apiUrl}/payrolls/runs/${id}/publish`, {});
    }

    payRun(id: number): Observable<PayrollRun> {
        return this.http.put<PayrollRun>(`${this.apiUrl}/payrolls/runs/${id}/pay`, {});
    }

    getComponents(id: number): Observable<PayrollComponent[]> {
        return this.http.get<PayrollComponent[]>(`${this.apiUrl}/payrolls/${id}/components`);
    }

    getMyPayrolls(page = 0, size = 20): Observable<Page<PayrollRecord>> {
        return this.http.get<Page<PayrollRecord>>(`${this.apiUrl}/me/payrolls?page=${page}&size=${size}`);
    }

    downloadPayslip(id: number): Observable<Blob> {
        return this.http.get(`${this.apiUrl}/payrolls/${id}/payslip`, { responseType: 'blob' });
    }

    downloadMyPayslip(id: number): Observable<Blob> {
        return this.http.get(`${this.apiUrl}/me/payrolls/${id}/payslip`, { responseType: 'blob' });
    }
}
