import React, { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { ticketApi } from '../../api/ticketApi';
import { Category, TicketPriority } from '../../types';
import { AlertCircle, CheckCircle2, ArrowLeft } from 'lucide-react';

export const CreateTicket: React.FC = () => {
  const [categories, setCategories] = useState<Category[]>([]);
  const [title, setTitle] = useState('');
  const [description, setDescription] = useState('');
  const [categoryId, setCategoryId] = useState<number | ''>('');
  const [priority, setPriority] = useState<TicketPriority>('MEDIUM');
  const [error, setError] = useState('');
  const [isSubmitting, setIsSubmitting] = useState(false);
  const navigate = useNavigate();

  useEffect(() => {
    ticketApi.getCategories().then((cats) => {
      setCategories(cats);
      if (cats.length > 0) setCategoryId(cats[0].id);
    });
  }, []);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!categoryId) return;
    setError('');
    setIsSubmitting(true);

    try {
      const ticket = await ticketApi.createTicket({
        title,
        description,
        categoryId: Number(categoryId),
        priority,
      });
      navigate(`/tickets/${ticket.id}`);
    } catch (err: any) {
      setError(err.response?.data?.message || 'Failed to submit support ticket.');
    } finally {
      setIsSubmitting(false);
    }
  };

  const selectedCat = categories.find((c) => c.id === categoryId);

  return (
    <div className="max-w-3xl mx-auto space-y-6">
      <button onClick={() => navigate(-1)} className="text-xs font-semibold text-slate-500 hover:text-slate-800 flex items-center gap-1.5">
        <ArrowLeft className="h-4 w-4" /> Back
      </button>

      <div className="bg-white rounded-2xl border border-slate-200 p-8 shadow-sm">
        <h2 className="text-xl font-bold text-slate-900 mb-1">Create Support Ticket</h2>
        <p className="text-sm text-slate-500 mb-6">Describe your issue in detail. An IT Support Agent will be assigned according to SLA.</p>

        {error && (
          <div className="mb-6 p-4 rounded-xl bg-red-50 border border-red-200 flex items-start gap-3 text-red-700 text-sm">
            <AlertCircle className="h-5 w-5 shrink-0" />
            <span>{error}</span>
          </div>
        )}

        <form onSubmit={handleSubmit} className="space-y-5">
          <div>
            <label className="block text-xs font-semibold text-slate-700 uppercase tracking-wider mb-2">Subject / Title</label>
            <input
              type="text"
              required
              minLength={5}
              maxLength={150}
              value={title}
              onChange={(e) => setTitle(e.target.value)}
              placeholder="e.g. VPN client connection error after Windows update"
              className="w-full px-4 py-2.5 rounded-xl border border-slate-200 focus:ring-2 focus:ring-sky-500 text-sm"
            />
          </div>

          <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
            <div>
              <label className="block text-xs font-semibold text-slate-700 uppercase tracking-wider mb-2">Category</label>
              <select
                required
                value={categoryId}
                onChange={(e) => setCategoryId(Number(e.target.value))}
                className="w-full px-4 py-2.5 rounded-xl border border-slate-200 focus:ring-2 focus:ring-sky-500 text-sm bg-white"
              >
                {categories.map((c) => (
                  <option key={c.id} value={c.id}>
                    {c.name} (SLA: {c.defaultSlaHours}h)
                  </option>
                ))}
              </select>
            </div>

            <div>
              <label className="block text-xs font-semibold text-slate-700 uppercase tracking-wider mb-2">Priority</label>
              <select
                value={priority}
                onChange={(e) => setPriority(e.target.value as TicketPriority)}
                className="w-full px-4 py-2.5 rounded-xl border border-slate-200 focus:ring-2 focus:ring-sky-500 text-sm bg-white"
              >
                <option value="LOW">Low (Minor inconvenience)</option>
                <option value="MEDIUM">Medium (Standard business task impacted)</option>
                <option value="HIGH">High (Important deadline impacted)</option>
                <option value="CRITICAL">Critical (Total work stoppage / Security issue)</option>
              </select>
            </div>
          </div>

          {selectedCat && (
            <div className="p-3.5 bg-sky-50/60 rounded-xl border border-sky-100 text-xs text-sky-800 flex items-center gap-2">
              <CheckCircle2 className="h-4 w-4 text-sky-600 shrink-0" />
              <span>
                <strong>Target SLA:</strong> Resolution window for <strong>{selectedCat.name}</strong> is <strong>{selectedCat.defaultSlaHours} hours</strong>.
              </span>
            </div>
          )}

          <div>
            <label className="block text-xs font-semibold text-slate-700 uppercase tracking-wider mb-2">Detailed Description</label>
            <textarea
              required
              rows={6}
              minLength={10}
              value={description}
              onChange={(e) => setDescription(e.target.value)}
              placeholder="Provide exact error messages, steps to reproduce, device model, or error codes..."
              className="w-full px-4 py-3 rounded-xl border border-slate-200 focus:ring-2 focus:ring-sky-500 text-sm"
            />
          </div>

          <button
            type="submit"
            disabled={isSubmitting}
            className="w-full py-3 bg-sky-600 hover:bg-sky-700 text-white font-semibold text-sm rounded-xl shadow-md transition disabled:opacity-50"
          >
            {isSubmitting ? 'Submitting Ticket...' : 'Submit Support Request'}
          </button>
        </form>
      </div>
    </div>
  );
};