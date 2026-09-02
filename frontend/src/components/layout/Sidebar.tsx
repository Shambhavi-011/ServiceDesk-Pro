import React from 'react';
import { NavLink } from 'react-router-dom';
import { useAuth } from '../../context/AuthContext';
import { 
  LayoutDashboard, 
  PlusCircle, 
  Ticket, 
  Inbox, 
  Users, 
  FolderTree, 
  History,
  LifeBuoy
} from 'lucide-react';

export const Sidebar: React.FC = () => {
  const { user } = useAuth();
  const isAdmin = user?.roles.includes('ROLE_ADMIN');
  const isAgent = user?.roles.includes('ROLE_SUPPORT_AGENT');

  const linkClass = ({ isActive }: { isActive: boolean }) =>
    `flex items-center gap-3 px-3.5 py-2.5 rounded-lg text-sm font-medium transition-all ${
      isActive
        ? 'bg-sky-50 text-sky-700 font-semibold'
        : 'text-slate-600 hover:text-slate-900 hover:bg-slate-100/80'
    }`;

  return (
    <aside className="w-64 bg-white border-r border-slate-200 flex flex-col justify-between p-4 min-h-[calc(100vh-4rem)]">
      <div className="space-y-6">
        {/* Employee Section */}
        <div>
          <p className="px-3 text-[11px] font-bold text-slate-400 uppercase tracking-wider mb-2">Self-Service</p>
          <nav className="space-y-1">
            <NavLink to="/employee" end className={linkClass}>
              <LayoutDashboard className="h-4 w-4" /> Dashboard
            </NavLink>
            <NavLink to="/tickets/new" className={linkClass}>
              <PlusCircle className="h-4 w-4" /> Create Ticket
            </NavLink>
            <NavLink to="/employee/tickets" className={linkClass}>
              <Ticket className="h-4 w-4" /> My Tickets
            </NavLink>
          </nav>
        </div>

        {/* Support Agent Section */}
        {(isAgent || isAdmin) && (
          <div>
            <p className="px-3 text-[11px] font-bold text-slate-400 uppercase tracking-wider mb-2">Support Operations</p>
            <nav className="space-y-1">
              <NavLink to="/agent" end className={linkClass}>
                <LayoutDashboard className="h-4 w-4" /> Agent Console
              </NavLink>
              <NavLink to="/agent/queue" className={linkClass}>
                <Inbox className="h-4 w-4" /> Unassigned Queue
              </NavLink>
            </nav>
          </div>
        )}

        {/* Admin Governance Section */}
        {isAdmin && (
          <div>
            <p className="px-3 text-[11px] font-bold text-slate-400 uppercase tracking-wider mb-2">Administration</p>
            <nav className="space-y-1">
              <NavLink to="/admin" end className={linkClass}>
                <LayoutDashboard className="h-4 w-4" /> Admin Console
              </NavLink>
              <NavLink to="/admin/users" className={linkClass}>
                <Users className="h-4 w-4" /> User Governance
              </NavLink>
              <NavLink to="/admin/categories" className={linkClass}>
                <FolderTree className="h-4 w-4" /> Categories & SLA
              </NavLink>
              <NavLink to="/admin/audit-logs" className={linkClass}>
                <History className="h-4 w-4" /> Audit Ledger
              </NavLink>
            </nav>
          </div>
        )}
      </div>

      <div className="p-3 bg-slate-50 border border-slate-200/80 rounded-xl">
        <div className="flex items-center gap-2 text-slate-700 font-semibold text-xs mb-1">
          <LifeBuoy className="h-4 w-4 text-sky-600" /> IT Helpdesk SLA
        </div>
        <p className="text-[11px] text-slate-500 leading-relaxed">
          Critical issues triaged within 2 hours. Standard tickets processed within 24h.
        </p>
      </div>
    </aside>
  );
};