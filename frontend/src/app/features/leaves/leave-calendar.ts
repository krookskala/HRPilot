import { Component, Input, OnChanges } from "@angular/core";
import { DatePipe } from "@angular/common";
import { MatButtonModule } from "@angular/material/button";
import { MatIconModule } from "@angular/material/icon";
import { MatTooltipModule } from "@angular/material/tooltip";
import { TranslateModule } from "@ngx-translate/core";
import { LeaveRequest, LeaveStatus } from "../../shared/models/leave.model";

interface CalendarDay {
    date: Date;
    day: number;
    isCurrentMonth: boolean;
    isToday: boolean;
    leaves: LeaveRequest[];
}

@Component({
    selector: 'app-leave-calendar',
    standalone: true,
    imports: [DatePipe, MatButtonModule, MatIconModule, MatTooltipModule, TranslateModule],
    templateUrl: './leave-calendar.html',
    styleUrl: './leave-calendar.scss'
})
export class LeaveCalendar implements OnChanges {
    @Input() leaves: LeaveRequest[] = [];

    currentDate = new Date();
    currentYear = this.currentDate.getFullYear();
    currentMonth = this.currentDate.getMonth();
    weeks: CalendarDay[][] = [];
    weekDays = ['Mon', 'Tue', 'Wed', 'Thu', 'Fri', 'Sat', 'Sun'];

    ngOnChanges(): void {
        this.buildCalendar();
    }

    prevMonth(): void {
        this.currentMonth--;
        if (this.currentMonth < 0) {
            this.currentMonth = 11;
            this.currentYear--;
        }
        this.buildCalendar();
    }

    nextMonth(): void {
        this.currentMonth++;
        if (this.currentMonth > 11) {
            this.currentMonth = 0;
            this.currentYear++;
        }
        this.buildCalendar();
    }

    goToday(): void {
        const today = new Date();
        this.currentYear = today.getFullYear();
        this.currentMonth = today.getMonth();
        this.buildCalendar();
    }

    getMonthLabel(): string {
        return new Date(this.currentYear, this.currentMonth).toLocaleDateString('en-US', {
            month: 'long',
            year: 'numeric'
        });
    }

    getStatusClass(status: LeaveStatus): string {
        switch (status) {
            case LeaveStatus.APPROVED: return 'leave-approved';
            case LeaveStatus.PENDING: return 'leave-pending';
            case LeaveStatus.REJECTED: return 'leave-rejected';
            case LeaveStatus.CANCELLED: return 'leave-cancelled';
            default: return '';
        }
    }

    private buildCalendar(): void {
        const firstDay = new Date(this.currentYear, this.currentMonth, 1);
        const lastDay = new Date(this.currentYear, this.currentMonth + 1, 0);

        // Monday = 0, Sunday = 6
        let startDow = firstDay.getDay() - 1;
        if (startDow < 0) startDow = 6;

        const today = new Date();
        today.setHours(0, 0, 0, 0);

        const days: CalendarDay[] = [];

        // Previous month padding
        for (let i = startDow - 1; i >= 0; i--) {
            const d = new Date(this.currentYear, this.currentMonth, -i);
            days.push({ date: d, day: d.getDate(), isCurrentMonth: false, isToday: false, leaves: [] });
        }

        // Current month
        for (let d = 1; d <= lastDay.getDate(); d++) {
            const date = new Date(this.currentYear, this.currentMonth, d);
            date.setHours(0, 0, 0, 0);
            const matchingLeaves = this.getLeaveForDate(date);
            days.push({
                date,
                day: d,
                isCurrentMonth: true,
                isToday: date.getTime() === today.getTime(),
                leaves: matchingLeaves
            });
        }

        // Next month padding
        while (days.length % 7 !== 0) {
            const d = new Date(this.currentYear, this.currentMonth + 1, days.length - lastDay.getDate() - startDow + 1);
            days.push({ date: d, day: d.getDate(), isCurrentMonth: false, isToday: false, leaves: [] });
        }

        // Group into weeks
        this.weeks = [];
        for (let i = 0; i < days.length; i += 7) {
            this.weeks.push(days.slice(i, i + 7));
        }
    }

    private getLeaveForDate(date: Date): LeaveRequest[] {
        return this.leaves.filter(leave => {
            if (leave.status === LeaveStatus.REJECTED || leave.status === LeaveStatus.CANCELLED) return false;
            const start = new Date(leave.startDate);
            const end = new Date(leave.endDate);
            start.setHours(0, 0, 0, 0);
            end.setHours(0, 0, 0, 0);
            return date >= start && date <= end;
        });
    }
}
