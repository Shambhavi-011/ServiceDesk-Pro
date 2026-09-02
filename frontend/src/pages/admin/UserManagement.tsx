import React, { useEffect, useState } from 'react';
import { adminApi } from '../../api/adminApi';
import { UserProfile } from '../../types';
import { Users, UserPlus, Shield, Headphones, UserCheck, AlertCircle, ChevronLeft, ChevronRight } from 'lucide-react';

export const UserManagement: React.FC = () => {
  const [users, setUsers] = useState<UserProfile[]>([]);
  const [page, setPage] = useState(0);
  const [totalPages, setTotalPages] = useState(1);
  const [showModal, setShowModal] = useState(false);
  const [newAgent, setNewAgent] = useState({
    username: '',
    email: '',
    password: '',
    firstName: '',
    lastName: '',
    department: 'IT Support & Operations',
  });
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(true);

  const fetchUsers = async () => {
    setLoading(true);
    try {
      const res = await adminApi.getUsers(page, 10);
      setUsers(res.content);
      setTotalPages(res.totalPages);
    } catch (err) {
      console.error(err);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchUsers();
  }, [page]);

  const handleCreateAgent = async (e: React.FormEvent) => {
    e.preventDefault();
    setError('');
    try {
      await adminApi.createAgent(newAgent);
      setShowModal(false);
      setNewAgent({ username: '', email: '', password: '', firstName: '', lastName: '', department: 'IT Support & Operations' });
      fetchUsers();
    } catch (err: any) {
      setError(err.response?.data?.message || 'Failed to provision agent');
    }
  };

  const handleToggleStatus = async (user: UserProfile) => {
    if (confirm(`Are you sure you want to ${user.active ? 'deactivate' : 'activate'} ${user.username}?`)) {
      await adminApi.toggleUserStatus(user.id, !user.active);
      fetchUsers();
    }
  };

  return (
    <div className="space-y-6">
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
        <div>
          <h2 className="text-2xl font-bold text-slate-900 flex items-center gap-2">
            <Users className="h-6 w-6 text-purple-600" /> User Governance
          </h2>
          <p className="text-sm text-slate-500">Manage corporate employees and provision IT Support Agents</p>
        </div>
        <button
          onClick={() => setShowModal(true)}
          className="inline-flex items-center gap-2 px-4 py-2.5 bg-purple-600 hover:bg-purple-700 text-white rounded-xl text-xs font-semibold shadow-sm"
        >
          <UserPlus className="h-4 w-4" /> Provision Support Agent
        </button>
      </div>

      <div className="bg-white rounded-2xl border border-slate-200/80 overflow-hidden shadow-sm">
        <div className="overflow-x-auto">
          <table className="w-full text-left text-sm text-slate-600">
            <thead className="bg-slate-50 text-xs font-semibold text-slate-500 uppercase tracking-wider border-b border-slate-100">
              <tr>
                <th className="px-6 py-3.5">User</th>
                <th className="px-6 py-3.5">Email</th>
                <th className="px-6 py-3.5">Department</th>
                <th className="px-6 py-3.5">Role</th>
                <th className="px-6 py-3.5">Status</th>
                <th className="px-6 py-3.5 text-right">Action</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-slate-100">
              {loading ? (
                <tr>
                  <td colSpan={6} className="text-center py-8 text-slate-400">Loading users...</td>
                </tr>
              ) : (
                users.map((u) => (
                  <tr key={u.id} className="hover:bg-slate-50/70">
                    <td className="px-6 py-4">
                      <p className="font-semibold text-slate-900">{u.firstName} {u.lastName}</p>
                      <p className="text-xs text-slate-400 font-mono">@{u.username}</p>
                    </td>
                    <td className="px-6 py-4">{u.email}</td>
                    <td className="px-6 py-4">{u.department || 'N/A'}</td>
                    <td className="px-6 py-4">
                      {u.roles.includes('ROLE_ADMIN') && (
                        <span className="inline-flex items-center gap-1 text-[10px] font-bold px-2 py-0.5 rounded bg-purple-100 text-purple-800">
                          <Shield className="h-3 w-3" /> ADMIN
                        </span>
                      )}
                      {u.roles.includes('ROLE_SUPPORT_AGENT') && (
                        <span className="inline-flex items-center gap-1 text-[10px] font-bold px-2 py-0.5 rounded bg-blue-100 text-blue-800">
                          <Headphones className="h-3 w-3" /> AGENT
                        </span>
                      )}
                      {u.roles.includes('ROLE_EMPLOYEE') && !u.roles.includes('ROLE_SUPPORT_AGENT') && !u.roles.includes('ROLE_ADMIN') && (
                        <span className="inline-flex items-center gap-1 text-[10px] font-bold px-2 py-0.5 rounded bg-slate-100 text-slate-700">
                          <UserCheck className="h-3 w-3" /> EMPLOYEE
                        </span>
                      )}
                    </td>
                    <td className="px-6 py-4">
                      <span className={`inline-flex items-center px-2 py-0.5 rounded text-xs font-semibold ${
                        u.active ? 'bg-emerald-100 text-emerald-800' : 'bg-rose-100 text-rose-800'
                      }`}>
                        {u.active ? 'Active' : 'Deactivated'}
                      </span>
                    </td>
                    <td className="px-6 py-4 text-right">
                      {!u.roles.includes('ROLE_ADMIN') && (
                        <button
                          onClick={() => handleToggleStatus(u)}
                          className={`text-xs font-semibold hover:underline ${
                            u.active ? 'text-rose-600' : 'text-emerald-600'
                          }`}
                        >
                          {u.active ? 'Deactivate' : 'Activate'}
                        </button>
                      )}
                    </td>
                  </tr>
                ))
              )}
            </tbody>
          </table>
        </div>

        <div className="px-6 py-4 border-t border-slate-100 flex items-center justify-between">
          <p className="text-xs text-slate-500">Page {page + 1} of {Math.max(1, totalPages)}</p>
          <div className="flex gap-2">
            <button
              disabled={page === 0}
              onClick={() => setPage(page - 1)}
              className="p-1.5 rounded-lg border border-slate-200 text-slate-600 hover:bg-slate-50 disabled:opacity-30"
            >
              <ChevronLeft className="h-4 w-4" />
            </button>
            <button
              disabled={page + 1 >= totalPages}
              onClick={() => setPage(page + 1)}
              className="p-1.5 rounded-lg border border-slate-200 text-slate-600 hover:bg-slate-50 disabled:opacity-30"
            >
              <ChevronRight className="h-4 w-4" />
            </button>
          </div>
        </div>
      </div>

      {showModal && (
        <div className="fixed inset-0 bg-slate-900/60 backdrop-blur-sm flex items-center justify-center p-4 z-50">
          <div className="bg-white rounded-2xl max-w-md w-full p-6 space-y-4 shadow-2xl border border-slate-100">
            <h3 className="text-lg font-bold text-slate-900 flex items-center gap-2">
              <Headphones className="h-5 w-5 text-purple-600" /> Provision Support Agent
            </h3>
            <p className="text-xs text-slate-500">Create an internal IT Support Agent account with triage privileges.</p>

            {error && (
              <div className="p-3 bg-red-50 text-red-700 text-xs rounded-xl flex items-center gap-2">
                <AlertCircle className="h-4 w-4 shrink-0" /> {error}
              </div>
            )}

            <form onSubmit={handleCreateAgent} className="space-y-3 text-xs">
              <div className="grid grid-cols-2 gap-3">
                <div>
                  <label className="font-semibold text-slate-700">First Name</label>
                  <input
                    type="text"
                    required
                    value={newAgent.firstName}
                    onChange={(e) => setNewAgent({ ...newAgent, firstName: e.target.value })}
                    className="w-full px-3 py-2 rounded-xl border border-slate-200 mt-1"
                  />
                </div>
                <div>
                  <label className="font-semibold text-slate-700">Last Name</label>
                  <input
                    type="text"
                    required
                    value={newAgent.lastName}
                    onChange={(e) => setNewAgent({ ...newAgent, lastName: e.target.value })}
                    className="w-full px-3 py-2 rounded-xl border border-slate-200 mt-1"
                  />
                </div>
              </div>

              <div>
                <label className="font-semibold text-slate-700">Username</label>
                <input
                  type="text"
                  required
                  value={newAgent.username}
                  onChange={(e) => setNewAgent({ ...newAgent, username: e.target.value })}
                  placeholder="e.g. sarah.agent"
                  className="w-full px-3 py-2 rounded-xl border border-slate-200 mt-1"
                />
              </div>

              <div>
                <label className="font-semibold text-slate-700">Corporate Email</label>
                <input
                  type="email"
                  required
                  value={newAgent.email}
                  onChange={(e) => setNewAgent({ ...newAgent, email: e.target.value })}
                  placeholder="sarah.agent@company.com"
                  className="w-full px-3 py-2 rounded-xl border border-slate-200 mt-1"
                />
              </div>

              <div>
                <label className="font-semibold text-slate-700">Temporary Password</label>
                <input
                  type="password"
                  required
                  minLength={6}
                  value={newAgent.password}
                  onChange={(e) => setNewAgent({ ...newAgent, password: e.target.value })}
                  placeholder="••••••••"
                  className="w-full px-3 py-2 rounded-xl border border-slate-200 mt-1"
                />
              </div>

              <div className="flex justify-end gap-2 pt-3">
                <button type="button" onClick={() => setShowModal(false)} className="px-4 py-2 text-slate-600 hover:bg-slate-100 rounded-xl font-semibold">
                  Cancel
                </button>
                <button type="submit" className="px-4 py-2 bg-purple-600 hover:bg-purple-700 text-white rounded-xl font-semibold">
                  Create Agent
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
};