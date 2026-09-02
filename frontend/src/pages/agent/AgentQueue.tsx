import React, { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { ticketApi } from '../../api/ticketApi';
import { TicketSummary } from '../../types';
import { StatusBadge } from '../../components/common/StatusBadge';
import { PriorityBadge } from '../../components/common/PriorityBadge';
import { Inbox, UserCheck } from 'lucide-react';

export const AgentQueue: React.FC = () => {
  const [tickets, setTickets] = useState<TicketSummary[]>([]);
  const [loading, setLoading] = useState(true);

  const fetchQueue = async () => {
    setLoading(true);
    try {
      const res = await ticketApi.getTickets({
        unassigned: true,
        page: 0,
        size: 50,
        sortBy: 'priority',
        sortDir: 'desc',
      });
      setTickets(res.content);
    } catch (err) {
      console.error(err);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchQueue();
  }, []);

  const handleSelfAssign = async (ticketId: number) => {
    await ticketApi.assignTicket(ticketId);
    fetchQueue();
  };

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <div>
          <h2 className="text-2xl font-bold text-slate-900 flex items-center gap-2">
            <Inbox className="h-6 w-6 text-amber-600" /> Unassigned Triage Pool
          </h2>
          <p className="text-sm text-slate-500">Tickets awaiting support agent pickup ({tickets.length} available)</p>
        </div>
      </div>

      <div className="bg-white rounded-2xl border border-slate-200/80 overflow-hidden shadow-sm">
        <div className="overflow-x-auto">
          <table className="w-full text-left text-sm text-slate-600">
            <thead className="bg-slate-50 text-xs font-semibold text-slate-500 uppercase tracking-wider border-b border-slate-100">
              <tr>
                <th className="px-6 py-3.5">Ticket #</th>
                <th className="px-6 py-3.5">Title</th>
                <th className="px-6 py-3.5">Category</th>
                <th className="px-6 py-3.5">Requester</th>
                <th className="px-6 py-3.5">Priority</th>
                <th className="px-6 py-3.5">Status</th>
                <th className="px-6 py-3.5 text-right">Action</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-slate-100">
              {loading ? (
                <tr>
                  <td colSpan={7} className="text-center py-8 text-slate-400">Loading unassigned queue...</td>
                </tr>
              ) : tickets.length === 0 ? (
                <tr>
                  <td colSpan={7} className="text-center py-8 text-slate-400">All caught up! No unassigned tickets in queue.</td>
                </tr>
              ) : (
                tickets.map((t) => (
                  <tr key={t.id} className="hover:bg-slate-50/70 transition-colors">
                    <td className="px-6 py-4 font-mono font-medium text-sky-600">
                      <Link to={`/tickets/${t.id}`}>{t.ticketNumber}</Link>
                    </td>
                    <td className="px-6 py-4 font-medium text-slate-900">
                      <Link to={`/tickets/${t.id}`} className="hover:underline">{t.title}</Link>
                    </td>
                    <td className="px-6 py-4">{t.categoryName}</td>
                    <td className="px-6 py-4">{t.createdByFullName}</td>
                    <td className="px-6 py-4"><PriorityBadge priority={t.priority} /></td>
                    <td className="px-6 py-4"><StatusBadge status={t.status} /></td>
                    <td className="px-6 py-4 text-right">
                      <button
                        onClick={() => handleSelfAssign(t.id)}
                        className="px-3 py-1.5 bg-sky-600 hover:bg-sky-700 text-white rounded-lg text-xs font-semibold flex items-center gap-1 ml-auto shadow-sm"
                      >
                        <UserCheck className="h-3.5 w-3.5" /> Pick Up
                      </button>
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