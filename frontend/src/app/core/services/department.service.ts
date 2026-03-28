import { Injectable } from "@angular/core";
import { HttpClient } from "@angular/common/http";
import { environment } from "../../../environments/environment";
import { Observable } from "rxjs";
import { Department, CreateDepartmentRequest } from "../../shared/models/department.model";
import { Page } from "../../shared/models/page.model";

@Injectable({ providedIn: 'root' })
export class DepartmentService {
    private apiUrl = environment.apiUrl;
    constructor(private http: HttpClient) {}

    getAll(page = 0, size = 20): Observable<Page<Department>> {
        return this.http.get<Page<Department>>(`${this.apiUrl}/departments?page=${page}&size=${size}`);
    }

    deleteDepartment(id: number): Observable<void> {
        return this.http.delete<void>(`${this.apiUrl}/departments/${id}`);
    }

    createDepartment(request: CreateDepartmentRequest): Observable<Department> {
        return this.http.post<Department>(`${this.apiUrl}/departments`, request);
    }
}