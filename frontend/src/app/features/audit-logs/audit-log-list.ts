import { Component, inject, OnInit, OnDestroy, ChangeDetectorRef } from "@angular/core";
import { DatePipe } from "@angular/common";
import { MatTableModule } from "@angular/material/table";
import { MatPaginatorModule, PageEvent } from "@angular/material/paginator";
import { MatProgressSpinnerModule } from "@angular/material/progress-spinner";
import { MatIconModule } from "@angular/material/icon";
import { MatTooltipModule } from "@angular/material/tooltip";
import { Subject, takeUntil } from "rxjs";
import { TranslateModule, TranslateService } from "@ngx-translate/core";
import { AuditLogService } from "../../core/services/audit-log.service";
import { AuditLogResponse } from "../../shared/models/audit-log.model";

@Component({
    selector: 'app-audit-log-list',
    standalone: true,
    imports: [DatePipe, MatTableModule, MatPaginatorModule, MatProgressSpinnerModule, MatIconModule, MatTooltipModule, TranslateModule],
    templateUrl: './audit-log-list.html',
    styleUrl: './audit-log-list.scss'
})
export class AuditLogList implements OnInit, OnDestroy {
    private auditLogService = inject(AuditLogService);
    private cdr = inject(ChangeDetectorRef);
    private translateService = inject(TranslateService);
    private destroy$ = new Subject<void>();

    logs: AuditLogResponse[] = [];
    displayedColumns = ['createdAt', 'actorEmail', 'actionType', 'targetType', 'summary', 'ipAddress'];
    totalElements = 0;
    pageSize = 20;
    pageIndex = 0;
    loading = false;
    error = '';

    ngOnInit(): void {
        this.loadLogs();
    }

    loadLogs(): void {
        this.loading = true;
        this.error = '';
        this.auditLogService.getAuditLogs(this.pageIndex, this.pageSize).pipe(takeUntil(this.destroy$)).subscribe({
            next: (page) => {
                this.logs = page.content;
                this.totalElements = page.totalElements;
                this.loading = false;
                this.cdr.detectChanges();
            },
            error: () => {
                this.error = this.translateService.instant('auditLogs.failedLoad');
                this.loading = false;
                this.cdr.detectChanges();
            }
        });
    }

    onPageChange(event: PageEvent): void {
        this.pageIndex = event.pageIndex;
        this.pageSize = event.pageSize;
        this.loadLogs();
    }

    ngOnDestroy(): void {
        this.destroy$.next();
        this.destroy$.complete();
    }
}
