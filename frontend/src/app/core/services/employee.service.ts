import { Injectable } from "@angular/core";
import { HttpClient } from "@angular/common/http";
import { environment } from "../../../environments/environment";
import { Observable } from "rxjs";
import { CreateEmployeeRequest, Employee } from "../../shared/models/employee.model";

@Injectable({ providedIn: 'root' })
export class EmployeeService {
    private apiUrl = environment.apiUrl;
    constructor(private http: HttpClient) {}

    getAll(): Observable<Employee[]> {
        return this.http.get<Employee[]>(`${this.apiUrl}/employees`);
    }

    deleteEmployee(id: number): Observable<void> {
        return this.http.delete<void>(`${this.apiUrl}/employees/${id}`);
    }

    createEmployee(request: CreateEmployeeRequest): Observable<Employee> {
        return this.http.post<Employee>(`${this.apiUrl}/employees`, request);
    }
}