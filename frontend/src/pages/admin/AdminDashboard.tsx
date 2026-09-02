import React, { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { adminApi } from '../../api/adminApi';
import { DashboardStats } from '../../types';
import { StatCard } from '../../components/common/StatCard';
import { Shield, Users, FolderTree, History, Clock, PlayCircle, AlertTriangle, ArrowRight } from 'lucide-react';

export const AdminDashboard: React.FC = () => {
  const [stats, setStats] = useState<DashboardStats | null>(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    adminApi.getDashboardStats().then((data) => {
      setStats(data);
      setLoading(false);
    });
  }, []);

  if (loading) return <div className="p-8 text-center text-slate-400">Loading Admin Console...</div>;

  const metrics = stats?.metrics || {};

  return (
    <div className="space-y-8">
      <div>
        <h2 className="text-2xl font-bold text-slate-900 flex items-center gap-2">
          <Shield className="h-6 w-6 text-purple-600" /> Admin Governance Console
        </h2>
        <p className="text-sm text-slate-500">System-wide operational telemetry, user administration, and audit monitoring</p>
      </div>

      <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4">
        <StatCard title="Total Tickets Logged" value={metrics.totalTickets || 0} icon={History} color="sky" subtitle="All time history" />
        <StatCard title="Open / Unassigned" value={metrics.openTickets || 0} icon={Clock} color="amber" subtitle="Pending assignment" />
        <StatCard title="In Progress Workload" value={metrics.inProgressTickets || 0} icon={PlayCircle} color="indigo" subtitle="Active across agents" />
        <StatCard title="Critical Incidents" value={metrics.criticalTickets || 0} icon={AlertTriangle} color="rose" subtitle="High priority SLA" />
      </div>

      <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
        <div className="bg-white p-6 rounded-2xl border border-slate-200 shadow-sm flex flex-col justify-between">
          <div>
            <div className="p-3 bg-purple-50 text-purple-600 rounded-xl w-fit mb-4">
              <Users className="h-6 w-6" />
            </div>
            <h3 className="font-bold text-slate-900 text-lg">User & Agent Governance</h3>
            <p className="text-xs text-slate-500 mt-1">Manage all enterprise accounts, provision support agents, or deactivate access.</p>
          </div>
          <Link
            to="/admin/users"
            className="mt-6 inline-flex items-center gap-1.5 text-xs font-bold text-purple-600 hover:text-purple-700"
          >
            Manage Users ({metrics.totalUsers || 0}) <ArrowRight className="h-3.5 w-3.5" />
          </Link>
        </div>

        <div className="bg-white p-6 rounded-2xl border border-slate-200 shadow-sm flex flex-col justify-between">
          <div>
            <div className="p-3 bg-sky-50 text-sky-600 rounded-xl w-fit mb-4">
              <FolderTree className="h-6 w-6" />
            </div>
            <h3 className="font-bold text-slate-900 text-lg">Categories & SLA Targets</h3>
            <p className="text-xs text-slate-500 mt-1">Configure service categories, routing rules, and default SLA target thresholds.</p>
          </div>
          <Link
            to="/admin/categories"
            className="mt-6 inline-flex items-center gap-1.5 text-xs font-bold text-sky-600 hover:text-sky-700"
          >
            Configure SLAs <ArrowRight className="h-3.5 w-3.5" />
          </Link>
        </div>

        <div className="bg-white p-6 rounded-2xl border border-slate-200 shadow-sm flex flex-col justify-between">
          <div>
            <div className="p-3 bg-emerald-50 text-emerald-600 rounded-xl w-fit mb-4">
              <History className="h-6 w-6" />
            </div>
            <h3 className="font-bold text-slate-900 text-lg">Immutable Audit Ledger</h3>
            <p className="text-xs text-slate-500 mt-1">Inspect tamper-evident system logs tracking all state mutations and assignments.</p>
          </div>
          <Link
            to="/admin/audit-logs"
            className="mt-6 inline-flex items-center gap-1.5 text-xs font-bold text-emerald-600 hover:text-emerald-700"
          >
            Inspect Audit Trail <ArrowRight className="h-3.5 w-3.5" />
          </Link>
        </div>
      </div>
    </div>
  );
};