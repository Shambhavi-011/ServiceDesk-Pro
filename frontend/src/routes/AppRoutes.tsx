import React from 'react';
import { Routes, Route, Navigate } from 'react-router-dom';
import { Login } from '../pages/auth/Login';
import { Register } from '../pages/auth/Register';
import { RoleGuard } from '../components/guards/RoleGuard';
import { DashboardLayout } from '../components/layout/DashboardLayout';

import { EmployeeDashboard } from '../pages/employee/EmployeeDashboard';
import { CreateTicket } from '../pages/employee/CreateTicket';
import { MyTickets } from '../pages/employee/MyTickets';
import { TicketDetail } from '../pages/common/TicketDetail';

import { AgentDashboard } from '../pages/agent/AgentDashboard';
import { AgentQueue } from '../pages/agent/AgentQueue';

import { AdminDashboard } from '../pages/admin/AdminDashboard';
import { UserManagement } from '../pages/admin/UserManagement';
import { CategoryManagement } from '../pages/admin/CategoryManagement';
import { AuditLogs } from '../pages/admin/AuditLogs';

export const AppRoutes: React.FC = () => {
  return (
    <Routes>
      {/* Public Routes */}
      <Route path="/login" element={<Login />} />
      <Route path="/register" element={<Register />} />

      {/* Protected App Routes */}
      <Route element={<RoleGuard />}>
        <Route element={<DashboardLayout />}>
          {/* Default Route */}
          <Route path="/" element={<Navigate to="/employee" replace />} />

          {/* Shared Ticket Views */}
          <Route path="/tickets/new" element={<CreateTicket />} />
          <Route path="/tickets/:id" element={<TicketDetail />} />

          {/* Employee Workspace */}
          <Route path="/employee" element={<EmployeeDashboard />} />
          <Route path="/employee/tickets" element={<MyTickets />} />

          {/* Support Agent Workspace */}
          <Route element={<RoleGuard allowedRoles={['ROLE_SUPPORT_AGENT', 'ROLE_ADMIN']} />}>
            <Route path="/agent" element={<AgentDashboard />} />
            <Route path="/agent/queue" element={<AgentQueue />} />
          </Route>

          {/* Admin Workspace */}
          <Route element={<RoleGuard allowedRoles={['ROLE_ADMIN']} />}>
            <Route path="/admin" element={<AdminDashboard />} />
            <Route path="/admin/users" element={<UserManagement />} />
            <Route path="/admin/categories" element={<CategoryManagement />} />
            <Route path="/admin/audit-logs" element={<AuditLogs />} />
          </Route>
        </Route>
      </Route>

      {/* Catch-all */}
      <Route path="*" element={<Navigate to="/login" replace />} />
    </Routes>
  );
};