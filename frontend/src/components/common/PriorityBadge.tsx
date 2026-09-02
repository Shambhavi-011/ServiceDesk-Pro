import React from 'react';
import { TicketPriority } from '../../types';

interface PriorityBadgeProps {
  priority: TicketPriority;
}

export const PriorityBadge: React.FC<PriorityBadgeProps> = ({ priority }) => {
  const config: Record<TicketPriority, { bg: string; text: string; dot: string; label: string }> = {
    LOW: { bg: 'bg-slate-50 border-slate-200', text: 'text-slate-700', dot: 'bg-slate-400', label: 'Low' },
    MEDIUM: { bg: 'bg-sky-50 border-sky-200', text: 'text-sky-700', dot: 'bg-sky-500', label: 'Medium' },
    HIGH: { bg: 'bg-orange-50 border-orange-200', text: 'text-orange-700', dot: 'bg-orange-500', label: 'High' },
    CRITICAL: { bg: 'bg-red-50 border-red-200', text: 'text-red-700', dot: 'bg-red-600', label: 'Critical' },
  };

  const current = config[priority] || config.MEDIUM;

  return (
    <span className={`inline-flex items-center gap-1.5 px-2.5 py-0.5 rounded-full text-xs font-semibold border ${current.bg} ${current.text}`}>
      <span className={`h-1.5 w-1.5 rounded-full ${current.dot}`}></span>
      {current.label}
    </span>
  );
};