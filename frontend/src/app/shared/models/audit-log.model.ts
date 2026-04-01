export interface AuditLogResponse {
    id: number;
    actorUserId: number;
    actorEmail: string;
    actionType: string;
    targetType: string;
    targetId: string;
    summary: string;
    details: string;
    ipAddress: string;
    userAgent: string;
    createdAt: string;
}
