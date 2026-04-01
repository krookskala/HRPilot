import { Routes } from '@angular/router';
import { Layout } from './layout/layout';
import { authGuard } from './core/guards/auth.guard';
import { roleGuard } from './core/guards/role.guard';

export const routes: Routes = [
    { path: 'login', loadComponent: () => import('./features/auth/login').then(m => m.Login) },
    { path: 'accept-invite/:token', loadComponent: () => import('./features/auth/accept-invite').then(m => m.AcceptInvite) },
    { path: 'forgot-password', loadComponent: () => import('./features/auth/request-password-reset').then(m => m.RequestPasswordReset) },
    { path: 'reset-password/:token', loadComponent: () => import('./features/auth/reset-password').then(m => m.ResetPassword) },
    {
        path: '',
        component: Layout,
        canActivate: [authGuard],
        children: [
            { path: '', redirectTo: 'dashboard', pathMatch: 'full'},
            { path: 'dashboard', loadComponent: () => import('./features/dashboard/dashboard').then(m => m.Dashboard) },
            { path: 'profile', loadComponent: () => import('./features/profile/my-profile').then(m => m.MyProfile) },
            { path: 'notifications', loadComponent: () => import('./features/notifications/notification-center').then(m => m.NotificationCenter) },
            { path: 'employees', loadComponent: () => import('./features/employees/employee-list').then(m => m.EmployeeList) },
            { path: 'employees/:id', loadComponent: () => import('./features/employees/employee-detail').then(m => m.EmployeeDetailPage) },
            { path: 'departments', loadComponent: () => import('./features/departments/department-list').then(m => m.DepartmentList) },
            { path: 'leaves', loadComponent: () => import('./features/leaves/leave-list').then(m => m.LeaveList) },
            { path: 'payrolls', loadComponent: () => import('./features/payrolls/payroll-list').then(m => m.PayrollList) },
            { path: 'users', loadComponent: () => import('./features/users/user-list').then(m => m.UserList), canActivate: [roleGuard('ADMIN')] },
            { path: 'audit-logs', loadComponent: () => import('./features/audit-logs/audit-log-list').then(m => m.AuditLogList), canActivate: [roleGuard('ADMIN')] },
            { path: 'reports', loadComponent: () => import('./features/reports/reports').then(m => m.Reports), canActivate: [roleGuard('ADMIN')] },
            { path: 'settings', loadComponent: () => import('./features/settings/settings').then(m => m.Settings) }
        ]
    },
    { path: '**', redirectTo: '' }
];
