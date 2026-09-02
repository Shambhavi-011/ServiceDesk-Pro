import React, { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { adminApi } from '../../api/adminApi';
import { ticketApi } from '../../api/ticketApi';
import { DashboardStats, TicketSummary } from '../../types';
import { StatCard } from '../../components/common/StatCard';
import { StatusBadge } from '../../components/common/StatusBadge';
import { PriorityBadge } from '../../components/common/PriorityBadge';
import { Inbox, UserCheck, PlayCircle, AlertTriangle, ArrowRight } from 'lucide-react';

export const AgentDashboard: React.FC = () => {
  const [stats, setStats] = useState<DashboardStats | null>(null);
  const [myTickets, setMyTickets] = useState<TicketSummary[]>([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    const loadDashboard = async () => {
      try {
        const [statsData, ticketsData] = await Promise.all([
          adminApi.getDashboardStats(),
          ticketApi.getTickets({ page: 0, size: 5, myTickets: true, sortBy: 'updatedAt', sortDir: 'desc' }),
        ]);
        setStats(statsData);
        setMyTickets(ticketsData.content);
      } catch (err) {
        console.error(err);
      } finally {
        setLoading(false);
      }
    };

    loadDashboard();
  }, []);

  if (loading) return <div className="p-8 text-center text-slate-400">Loading Agent Console...</div>;

  const metrics = stats?.metrics || {};

  return (
    <div className="space-y-8">
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
        <div>
          <h2 className="text-2xl font-bold text-slate-900">Support Agent Console</h2>
          <p className="text-sm text-slate-500">Triage incoming tickets, fulfill SLA commitments, and resolve incidents</p>
        </div>
        <Link
          to="/agent/queue"
          className="inline-flex items-center gap-2 px-4 py-2.5 bg-sky-600 hover:bg-sky-700 text-white text-sm font-semibold rounded-xl shadow-sm transition"
        >
          <Inbox className="h-4 w-4" /> Open Unassigned Queue ({metrics.unassignedQueue || 0})
        </Link>
      </div>

      <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4">
        <StatCard title="Unassigned Pool" value={metrics.unassignedQueue || 0} icon={Inbox} color="amber" subtitle="Awaiting assignment" />
        <StatCard title="Assigned To Me" value={metrics.assignedToMe || 0} icon={UserCheck} color="sky" subtitle="Active workload" />
        <StatCard title="In Progress" value={metrics.inProgressByMe || 0} icon={PlayCircle} color="indigo" subtitle="Under investigation" />
        <StatCard title="Critical Incidents" value={metrics.criticalTickets || 0} icon={AlertTriangle} color="rose" subtitle="SLA urgent" />
      </div>

      <div className="bg-white rounded-2xl border border-slate-200/80 overflow-hidden shadow-sm">
        <div className="px-6 py-4 border-b border-slate-100 flex items-center justify-between">
          <h3 className="font-bold text-slate-900 text-base">My Assigned Workload</h3>
          <Link to="/employee/tickets" className="text-xs font-semibold text-sky-600 hover:text-sky-700 flex items-center gap-1">
            Search all tickets <ArrowRight className="h-3.5 w-3.5" />
          </Link>
        </div>

        <div className="overflow-x-auto">
          <table className="w-full text-left text-sm text-slate-600">
            <thead className="bg-slate-50 text-xs font-semibold text-slate-500 uppercase tracking-wider border-b border-slate-100">
              <tr>
                <th className="px-6 py-3.5">Ticket #</th>
                <th className="px-6 py-3.5">Title</th>
                <th className="px-6 py-3.5">Requester</th>
                <th className="px-6 py-3.5">Priority</th>
                <th className="px-6 py-3.5">Status</th>
                <th className="px-6 py-3.5">SLA Target</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-slate-100">
              {myTickets.length === 0 ? (
                <tr>
                  <td colSpan={6} className="text-center py-8 text-slate-400">
                    No tickets assigned to you right now. Grab one from the Unassigned Queue!
                  </td>
                </tr>
              ) : (
                myTickets.map((t) => (
                  <tr key={t.id} className="hover:bg-slate-50/70 transition-colors">
                    <td className="px-6 py-4 font-mono font-medium text-sky-600">
                      <Link to={`/tickets/${t.id}`}>{t.ticketNumber}</Link>
                    </td>
                    <td className="px-6 py-4 font-medium text-slate-900">
                      <Link to={`/tickets/${t.id}`} className="hover:underline">{t.title}</Link>
                    </td>
                    <td className="px-6 py-4">{t.createdByFullName}</td>
                    <td className="px-6 py-4"><PriorityBadge priority={t.priority} /></td>
                    <td className="px-6 py-4"><StatusBadge status={t.status} /></td>
                    <td className="px-6 py-4 text-xs font-medium text-slate-600">
                      {t.slaDueAt ? new Date(t.slaDueAt).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' }) : 'N/A'}
                    </td>
                  </tr>
                ))
              )}
            </tbody>
          </table>
        </div>
      </div>
    </div>
  );
};