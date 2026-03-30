import { Injectable } from "@angular/core";
import { HttpClient, HttpParams } from "@angular/common/http";
import { environment } from "../../../environments/environment";
import { Observable } from "rxjs";
import { CreateEmployeeRequest, Employee, EmployeeDetail, EmployeeDocument } from "../../shared/models/employee.model";
import { Page } from "../../shared/models/page.model";

@Injectable({ providedIn: 'root' })
export class EmployeeService {
    private apiUrl = environment.apiUrl;
    constructor(private http: HttpClient) {}

    getAll(page = 0, size = 20): Observable<Page<Employee>> {
        return this.http.get<Page<Employee>>(`${this.apiUrl}/employees?page=${page}&size=${size}`);
    }

    search(filters: { search?: string; departmentId?: number; position?: string },
           page = 0, size = 20): Observable<Page<Employee>> {
        let params = new HttpParams()
            .set('page', page.toString())
            .set('size', size.toString());

        if (filters.search) params = params.set('search', filters.search);
        if (filters.departmentId) params = params.set('departmentId', filters.departmentId.toString());
        if (filters.position) params = params.set('position', filters.position);

        return this.http.get<Page<Employee>>(`${this.apiUrl}/employees`, { params });
    }

    deleteEmployee(id: number): Observable<void> {
        return this.http.delete<void>(`${this.apiUrl}/employees/${id}`);
    }

    createEmployee(request: CreateEmployeeRequest): Observable<Employee> {
        return this.http.post<Employee>(`${this.apiUrl}/employees`, request);
    }

    getEmployeeDetail(id: number): Observable<EmployeeDetail> {
        return this.http.get<EmployeeDetail>(`${this.apiUrl}/employees/${id}/detail`);
    }

    exportCsv(): Observable<Blob> {
        return this.http.get(`${this.apiUrl}/employees/export/csv`, {
            responseType: 'blob'
        });
    }

    uploadPhoto(employeeId: number, file: File): Observable<Employee> {
        const formData = new FormData();
        formData.append('file', file);
        return this.http.post<Employee>(`${this.apiUrl}/employees/${employeeId}/photo`, formData);
    }

    downloadPhoto(employeeId: number): Observable<Blob> {
        return this.http.get(`${this.apiUrl}/employees/${employeeId}/photo/download`, {
            responseType: 'blob'
        });
    }

    getDocuments(employeeId: number): Observable<EmployeeDocument[]> {
        return this.http.get<EmployeeDocument[]>(`${this.apiUrl}/employees/${employeeId}/documents`);
    }

    uploadDocument(employeeId: number, file: File, title: string, description?: string | null): Observable<EmployeeDocument> {
        const formData = new FormData();
        formData.append('file', file);
        formData.append('title', title);
        if (description) {
            formData.append('description', description);
        }
        return this.http.post<EmployeeDocument>(`${this.apiUrl}/employees/${employeeId}/documents`, formData);
    }

    downloadDocument(employeeId: number, documentId: number): Observable<Blob> {
        return this.http.get(`${this.apiUrl}/employees/${employeeId}/documents/${documentId}/download`, {
            responseType: 'blob'
        });
    }
}
