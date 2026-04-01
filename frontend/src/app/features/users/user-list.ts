import { Component, inject, OnInit, OnDestroy, ChangeDetectorRef } from "@angular/core";
import { FormControl, ReactiveFormsModule } from "@angular/forms";
import { MatTableModule } from "@angular/material/table";
import { MatButtonModule } from "@angular/material/button";
import { MatDialog, MatDialogModule } from "@angular/material/dialog";
import { MatPaginatorModule, PageEvent } from "@angular/material/paginator";
import { MatProgressSpinnerModule } from "@angular/material/progress-spinner";
import { MatIconModule } from "@angular/material/icon";
import { MatTooltipModule } from "@angular/material/tooltip";
import { MatFormFieldModule } from "@angular/material/form-field";
import { MatInputModule } from "@angular/material/input";
import { MatSelectModule } from "@angular/material/select";
import { debounceTime, distinctUntilChanged, Subject, takeUntil } from "rxjs";
import { UserService } from "../../core/services/user.service";
import { Role, User } from "../../shared/models/user.model";
import { InviteUserDialog } from "./invite-user-dialog";
import { ConfirmDialog } from "../../shared/components/confirm-dialog/confirm-dialog";

@Component({
    selector: 'app-user-list',
    standalone: true,
    imports: [ReactiveFormsModule, MatTableModule, MatButtonModule, MatDialogModule, MatPaginatorModule, MatProgressSpinnerModule, MatIconModule, MatTooltipModule, MatFormFieldModule, MatInputModule, MatSelectModule],
    templateUrl: './user-list.html',
    styleUrl: './user-list.scss'
})
export class UserList implements OnInit, OnDestroy {
    private userService = inject(UserService);
    private dialog = inject(MatDialog);
    private cdr = inject(ChangeDetectorRef);

    users: User[] = [];
    roles = Object.values(Role);
    displayedColumns = ['email', 'role', 'status', 'lastLoginAt', 'actions'];
    loading = false;
    error = '';
    inviteLink = '';
    totalElements = 0;
    pageSize = 10;
    pageIndex = 0;

    emailControl = new FormControl('');
    roleControl = new FormControl<string | null>(null);
    statusControl = new FormControl<string | null>(null);
    private destroy$ = new Subject<void>();

    ngOnInit(): void {
        this.loadUsers();

        this.emailControl.valueChanges.pipe(debounceTime(300), distinctUntilChanged(), takeUntil(this.destroy$)).subscribe(() => {
            this.pageIndex = 0;
            this.loadUsers();
        });
        this.roleControl.valueChanges.pipe(takeUntil(this.destroy$)).subscribe(() => {
            this.pageIndex = 0;
            this.loadUsers();
        });
        this.statusControl.valueChanges.pipe(takeUntil(this.destroy$)).subscribe(() => {
            this.pageIndex = 0;
            this.loadUsers();
        });
    }

    loadUsers(): void {
        this.loading = true;
        this.error = '';

        this.userService.getAll({
            email: this.emailControl.value || undefined,
            role: this.roleControl.value || undefined,
            isActive: this.statusControl.value === null ? null : this.statusControl.value === 'active'
        }, this.pageIndex, this.pageSize).pipe(takeUntil(this.destroy$)).subscribe({
            next: page => {
                this.users = page.content;
                this.totalElements = page.totalElements;
                this.loading = false;
                this.cdr.detectChanges();
            },
            error: () => {
                this.error = 'Failed to load users';
                this.loading = false;
                this.cdr.detectChanges();
            }
        });
    }

    onPageChange(event: PageEvent): void {
        this.pageIndex = event.pageIndex;
        this.pageSize = event.pageSize;
        this.loadUsers();
    }

    openInviteDialog(): void {
        const ref = this.dialog.open(InviteUserDialog, { width: '420px' });
        ref.afterClosed().pipe(takeUntil(this.destroy$)).subscribe(result => {
            if (result) {
                this.userService.inviteUser(result).pipe(takeUntil(this.destroy$)).subscribe({
                    next: response => {
                        this.inviteLink = response.inviteUrl;
                        this.loadUsers();
                        this.cdr.detectChanges();
                    },
                    error: () => {
                        this.error = 'Failed to invite user';
                        this.cdr.detectChanges();
                    }
                });
            }
        });
    }

    resendInvite(user: User): void {
        this.userService.resendInvite(user.id).pipe(takeUntil(this.destroy$)).subscribe({
            next: response => {
                this.inviteLink = response.inviteUrl;
                this.loadUsers();
                this.cdr.detectChanges();
            },
            error: () => {
                this.error = 'Failed to resend invite';
                this.cdr.detectChanges();
            }
        });
    }

    toggleActive(user: User): void {
        this.userService.updateUser(user.id, { isActive: !user.isActive }).pipe(takeUntil(this.destroy$)).subscribe({
            next: () => this.loadUsers(),
            error: () => {
                this.error = 'Failed to update user';
                this.cdr.detectChanges();
            }
        });
    }

    delete(user: User): void {
        const ref = this.dialog.open(ConfirmDialog, {
            width: '350px',
            data: { title: 'Delete User', message: `Delete ${user.email}?` }
        });

        ref.afterClosed().pipe(takeUntil(this.destroy$)).subscribe(confirmed => {
            if (confirmed) {
                this.userService.deleteUser(user.id).pipe(takeUntil(this.destroy$)).subscribe({
                    next: () => this.loadUsers(),
                    error: () => {
                        this.error = 'Failed to delete user';
                        this.cdr.detectChanges();
                    }
                });
            }
        });
    }

    ngOnDestroy(): void {
        this.destroy$.next();
        this.destroy$.complete();
    }
}
