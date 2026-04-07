import { Injectable } from "@angular/core";
import { HttpClient } from "@angular/common/http";
import { map, Observable } from "rxjs";
import { environment } from "../../../environments/environment";
import { AuditLogResponse } from "../../shared/models/audit-log.model";
import { normalizePage, Page, RawPageResponse } from "../../shared/models/page.model";

@Injectable({ providedIn: 'root' })
export class AuditLogService {
    private apiUrl = environment.apiUrl;

    constructor(private http: HttpClient) {}

    getAuditLogs(page = 0, size = 20): Observable<Page<AuditLogResponse>> {
        return this.http
            .get<RawPageResponse<AuditLogResponse>>(`${this.apiUrl}/audit-logs?page=${page}&size=${size}`)
            .pipe(map(normalizePage));
    }
}
