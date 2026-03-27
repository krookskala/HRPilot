import { Injectable } from "@angular/core";
import { HttpClient } from "@angular/common/http";
import { environment } from "../../../environments/environment";
import { Observable } from "rxjs";
import { Department } from "../../shared/models/department.model";

@Injectable({ providedIn: 'root' })
export class DepartmentService {
    private apiUrl = environment.apiUrl;
    constructor(private http: HttpClient) {}

    getAll(): Observable<Department[]> {
        return this.http.get<Department[]>(`${this.apiUrl}/departments`);
    }

    deleteDepartment(id: number): Observable<void> {
        return this.http.delete<void>(`${this.apiUrl}/departments/${id}`);
    }
}