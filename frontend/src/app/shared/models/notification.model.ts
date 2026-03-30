export interface NotificationItem {
    id: number;
    type: string;
    title: string;
    message: string;
    actionUrl: string | null;
    read: boolean;
    createdAt: string;
    readAt: string | null;
}
