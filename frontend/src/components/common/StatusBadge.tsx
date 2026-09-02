import React from 'react';
import { TicketStatus } from '../../types';

interface StatusBadgeProps {
  status: TicketStatus;
}

export const StatusBadge: React.FC<StatusBadgeProps> = ({ status }) => {
  const config: Record<TicketStatus, { bg: string; text: string; label: string }> = {
    OPEN: { bg: 'bg-amber-50 border-amber-200', text: 'text-amber-700', label: 'Open' },
    ASSIGNED: { bg: 'bg-blue-50 border-blue-200', text: 'text-blue-700', label: 'Assigned' },
    IN_PROGRESS: { bg: 'bg-indigo-50 border-indigo-200', text: 'text-indigo-700', label: 'In Progress' },
    RESOLVED: { bg: 'bg-emerald-50 border-emerald-200', text: 'text-emerald-700', label: 'Resolved' },
    CLOSED: { bg: 'bg-slate-100 border-slate-200', text: 'text-slate-600', label: 'Closed' },
    REOPENED: { bg: 'bg-rose-50 border-rose-200', text: 'text-rose-700', label: 'Reopened' },
  };

  const current = config[status] || config.OPEN;

  return (
    <span className={`inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-medium border ${current.bg} ${current.text}`}>
      {current.label}
    </span>
  );
};