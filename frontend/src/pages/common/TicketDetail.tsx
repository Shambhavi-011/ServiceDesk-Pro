import React, { useEffect, useState } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { ticketApi } from '../../api/ticketApi';
import { useAuth } from '../../context/AuthContext';
import { AttachmentItem, CommentItem, TicketDetail as ITicketDetail } from '../../types';
import { StatusBadge } from '../../components/common/StatusBadge';
import { PriorityBadge } from '../../components/common/PriorityBadge';
import { 
  ArrowLeft, 
  Send, 
  Paperclip, 
  Download, 
  CheckCircle2, 
  RotateCcw, 
  UserCheck, 
  PlayCircle, 
  AlertTriangle,
  Lock,
  Clock
} from 'lucide-react';

export const TicketDetail: React.FC = () => {
  const { id } = useParams<{ id: string }>();
  const ticketId = Number(id);
  const { user } = useAuth();
  const navigate = useNavigate();

  const [ticket, setTicket] = useState<ITicketDetail | null>(null);
  const [comments, setComments] = useState<CommentItem[]>([]);
  const [attachments, setAttachments] = useState<AttachmentItem[]>([]);
  const [newComment, setNewComment] = useState('');
  const [isInternalNote, setIsInternalNote] = useState(false);
  const [resolutionNotes, setResolutionNotes] = useState('');
  const [reopenReason, setReopenReason] = useState('');
  const [showResolveModal, setShowResolveModal] = useState(false);
  const [showReopenModal, setShowReopenModal] = useState(false);
  const [loading, setLoading] = useState(true);
  const [uploading, setUploading] = useState(false);

  const isAdmin = user?.roles.includes('ROLE_ADMIN');
  const isAgent = user?.roles.includes('ROLE_SUPPORT_AGENT');
  const isCreator = ticket?.createdBy.id === user?.id;

  const loadTicketData = async () => {
    try {
      const [ticketData, commentsData, attachmentsData] = await Promise.all([
        ticketApi.getTicketById(ticketId),
        ticketApi.getComments(ticketId),
        ticketApi.getAttachments(ticketId),
      ]);
      setTicket(ticketData);
      setComments(commentsData);
      setAttachments(attachmentsData);
    } catch (err) {
      console.error('Failed to load ticket', err);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadTicketData();
  }, [ticketId]);

  const handleAddComment = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!newComment.trim()) return;

    await ticketApi.addComment(ticketId, newComment, isInternalNote);
    setNewComment('');
    setIsInternalNote(false);
    const updated = await ticketApi.getComments(ticketId);
    setComments(updated);
  };

  const handleFileUpload = async (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0];
    if (!file) return;

    setUploading(true);
    try {
      await ticketApi.uploadAttachment(ticketId, file);
      const updated = await ticketApi.getAttachments(ticketId);
      setAttachments(updated);
    } catch (err: any) {
      alert(err.response?.data?.message || 'File upload failed');
    } finally {
      setUploading(false);
      e.target.value = '';
    }
  };

  const handleSelfAssign = async () => {
    await ticketApi.assignTicket(ticketId);
    loadTicketData();
  };

  const handleSetInProgress = async () => {
    await ticketApi.updateStatus(ticketId, 'IN_PROGRESS');
    loadTicketData();
  };

  const handleResolve = async () => {
    if (!resolutionNotes.trim()) return;
    await ticketApi.resolveTicket(ticketId, resolutionNotes);
    setShowResolveModal(false);
    loadTicketData();
  };

  const handleReopen = async () => {
    if (!reopenReason.trim()) return;
    await ticketApi.reopenTicket(ticketId, reopenReason);
    setShowReopenModal(false);
    loadTicketData();
  };

  const handleClose = async () => {
    if (confirm('Confirm ticket resolution and close this request?')) {
      await ticketApi.closeTicket(ticketId);
      loadTicketData();
    }
  };

  if (loading || !ticket) {
    return <div className="p-8 text-center text-slate-500">Loading ticket details...</div>;
  }

  return (
    <div className="space-y-6 max-w-6xl mx-auto">
      <button onClick={() => navigate(-1)} className="text-xs font-semibold text-slate-500 hover:text-slate-800 flex items-center gap-1.5">
        <ArrowLeft className="h-4 w-4" /> Back to List
      </button>

      {/* Header Banner */}
      <div className="bg-white rounded-2xl border border-slate-200 p-6 shadow-sm flex flex-col md:flex-row md:items-center justify-between gap-4">
        <div>
          <div className="flex items-center gap-3 mb-2">
            <span className="font-mono text-sm font-bold text-sky-700 bg-sky-50 border border-sky-100 px-2.5 py-0.5 rounded-lg">
              {ticket.ticketNumber}
            </span>
            <StatusBadge status={ticket.status} />
            <PriorityBadge priority={ticket.priority} />
            <span className="text-xs text-slate-500 font-medium">Category: {ticket.categoryName}</span>
          </div>
          <h2 className="text-2xl font-bold text-slate-900">{ticket.title}</h2>
        </div>

        {/* Action Controls */}
        <div className="flex flex-wrap items-center gap-2">
          {(isAgent || isAdmin) && ticket.status === 'OPEN' && !ticket.assignedTo && (
            <button
              onClick={handleSelfAssign}
              className="px-4 py-2 bg-sky-600 hover:bg-sky-700 text-white rounded-xl text-xs font-semibold flex items-center gap-1.5 shadow-sm"
            >
              <UserCheck className="h-4 w-4" /> Self-Assign
            </button>
          )}

          {(isAgent || isAdmin) && (ticket.status === 'ASSIGNED' || ticket.status === 'REOPENED') && (
            <button
              onClick={handleSetInProgress}
              className="px-4 py-2 bg-indigo-600 hover:bg-indigo-700 text-white rounded-xl text-xs font-semibold flex items-center gap-1.5 shadow-sm"
            >
              <PlayCircle className="h-4 w-4" /> Start Working
            </button>
          )}

          {(isAgent || isAdmin) && ticket.status === 'IN_PROGRESS' && (
            <button
              onClick={() => setShowResolveModal(true)}
              className="px-4 py-2 bg-emerald-600 hover:bg-emerald-700 text-white rounded-xl text-xs font-semibold flex items-center gap-1.5 shadow-sm"
            >
              <CheckCircle2 className="h-4 w-4" /> Resolve Ticket
            </button>
          )}

          {(isCreator || isAdmin) && ticket.status === 'RESOLVED' && (
            <>
              <button
                onClick={handleClose}
                className="px-4 py-2 bg-emerald-600 hover:bg-emerald-700 text-white rounded-xl text-xs font-semibold flex items-center gap-1.5 shadow-sm"
              >
                <CheckCircle2 className="h-4 w-4" /> Confirm & Close
              </button>
              <button
                onClick={() => setShowReopenModal(true)}
                className="px-4 py-2 bg-rose-600 hover:bg-rose-700 text-white rounded-xl text-xs font-semibold flex items-center gap-1.5 shadow-sm"
              >
                <RotateCcw className="h-4 w-4" /> Reopen Ticket
              </button>
            </>
          )}
        </div>
      </div>

      {/* Main Grid */}
      <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
        {/* Left Column: Description + Comments Thread */}
        <div className="lg:col-span-2 space-y-6">
          {/* Issue Description */}
          <div className="bg-white rounded-2xl border border-slate-200 p-6 shadow-sm">
            <h3 className="text-sm font-bold text-slate-900 uppercase tracking-wider mb-3">Issue Description</h3>
            <p className="text-sm text-slate-700 whitespace-pre-wrap leading-relaxed">{ticket.description}</p>

            {ticket.resolutionNotes && (
              <div className="mt-6 p-4 rounded-xl bg-emerald-50 border border-emerald-200">
                <h4 className="text-xs font-bold text-emerald-800 uppercase tracking-wider mb-1 flex items-center gap-1.5">
                  <CheckCircle2 className="h-4 w-4 text-emerald-600" /> Resolution Summary
                </h4>
                <p className="text-sm text-emerald-900">{ticket.resolutionNotes}</p>
                <p className="text-[11px] text-emerald-700 mt-2 font-medium">
                  Resolved on: {new Date(ticket.resolvedAt!).toLocaleString()}
                </p>
              </div>
            )}
          </div>

          {/* Comments & Discussion */}
          <div className="bg-white rounded-2xl border border-slate-200 p-6 shadow-sm space-y-6">
            <h3 className="text-sm font-bold text-slate-900 uppercase tracking-wider">Discussion & Updates ({comments.length})</h3>

            <div className="space-y-4 max-h-[500px] overflow-y-auto pr-1">
              {comments.length === 0 ? (
                <p className="text-sm text-slate-400 text-center py-4">No comments posted yet.</p>
              ) : (
                comments.map((c) => (
                  <div
                    key={c.id}
                    className={`p-4 rounded-xl border ${
                      c.internalNote
                        ? 'bg-amber-50/70 border-amber-200'
                        : 'bg-slate-50 border-slate-200/80'
                    }`}
                  >
                    <div className="flex items-center justify-between mb-2">
                      <div className="flex items-center gap-2">
                        <span className="text-xs font-bold text-slate-900">{c.authorFullName}</span>
                        {c.internalNote && (
                          <span className="inline-flex items-center gap-1 text-[10px] font-bold px-2 py-0.5 rounded bg-amber-200 text-amber-900">
                            <Lock className="h-3 w-3" /> Agent Internal Note
                          </span>
                        )}
                      </div>
                      <span className="text-[11px] text-slate-400">{new Date(c.createdAt).toLocaleString()}</span>
                    </div>
                    <p className="text-sm text-slate-700 whitespace-pre-wrap">{c.content}</p>
                  </div>
                ))
              )}
            </div>

            {/* Post Comment Form */}
            {ticket.status !== 'CLOSED' && (
              <form onSubmit={handleAddComment} className="pt-4 border-t border-slate-100 space-y-3">
                <textarea
                  rows={3}
                  required
                  value={newComment}
                  onChange={(e) => setNewComment(e.target.value)}
                  placeholder="Write a reply or status update..."
                  className="w-full px-3.5 py-2.5 rounded-xl border border-slate-200 focus:ring-2 focus:ring-sky-500 text-sm"
                />

                <div className="flex items-center justify-between">
                  {(isAgent || isAdmin) ? (
                    <label className="flex items-center gap-2 text-xs font-semibold text-amber-800 cursor-pointer">
                      <input
                        type="checkbox"
                        checked={isInternalNote}
                        onChange={(e) => setIsInternalNote(e.target.checked)}
                        className="rounded text-amber-600 focus:ring-amber-500"
                      />
                      <span>Post as Private Agent Note (Hidden from employee)</span>
                    </label>
                  ) : <div></div>}

                  <button
                    type="submit"
                    className="px-4 py-2 bg-sky-600 hover:bg-sky-700 text-white font-semibold text-xs rounded-xl flex items-center gap-1.5 shadow-sm"
                  >
                    <Send className="h-3.5 w-3.5" /> Post Update
                  </button>
                </div>
              </form>
            )}
          </div>
        </div>

        {/* Right Column: Metadata & Attachments */}
        <div className="space-y-6">
          {/* Metadata Card */}
          <div className="bg-white rounded-2xl border border-slate-200 p-6 shadow-sm space-y-4">
            <h3 className="text-xs font-bold text-slate-400 uppercase tracking-wider">Ticket Governance</h3>

            <div className="space-y-3 text-xs">
              <div>
                <p className="text-slate-400 font-medium">Requested By</p>
                <p className="font-semibold text-slate-800 text-sm mt-0.5">{ticket.createdBy.firstName} {ticket.createdBy.lastName}</p>
                <p className="text-slate-500">{ticket.createdBy.email} • {ticket.createdBy.department || 'General'}</p>
              </div>

              <div className="pt-3 border-t border-slate-100">
                <p className="text-slate-400 font-medium">Assigned Agent</p>
                {ticket.assignedTo ? (
                  <p className="font-semibold text-slate-800 text-sm mt-0.5">{ticket.assignedTo.firstName} {ticket.assignedTo.lastName}</p>
                ) : (
                  <p className="text-amber-600 font-medium mt-0.5">Unassigned Pool</p>
                )}
              </div>

              <div className="pt-3 border-t border-slate-100">
                <p className="text-slate-400 font-medium">Target SLA Deadline</p>
                <p className="font-semibold text-slate-800 mt-0.5 flex items-center gap-1.5">
                  <Clock className="h-3.5 w-3.5 text-sky-600" />
                  {ticket.slaDueAt ? new Date(ticket.slaDueAt).toLocaleString() : 'N/A'}
                </p>
              </div>

              <div className="pt-3 border-t border-slate-100">
                <p className="text-slate-400 font-medium">Timeline</p>
                <p className="text-slate-600 mt-0.5">Created: {new Date(ticket.createdAt).toLocaleString()}</p>
                <p className="text-slate-600">Updated: {new Date(ticket.updatedAt).toLocaleString()}</p>
              </div>
            </div>
          </div>

          {/* Attachments Card */}
          <div className="bg-white rounded-2xl border border-slate-200 p-6 shadow-sm space-y-4">
            <div className="flex items-center justify-between">
              <h3 className="text-xs font-bold text-slate-400 uppercase tracking-wider flex items-center gap-1.5">
                <Paperclip className="h-3.5 w-3.5" /> Attachments ({attachments.length})
              </h3>
              {ticket.status !== 'CLOSED' && (
                <label className="cursor-pointer text-xs font-semibold text-sky-600 hover:text-sky-700">
                  {uploading ? 'Uploading...' : '+ Upload'}
                  <input type="file" onChange={handleFileUpload} className="hidden" />
                </label>
              )}
            </div>

            <div className="space-y-2">
              {attachments.length === 0 ? (
                <p className="text-xs text-slate-400 py-2">No files attached to this ticket.</p>
              ) : (
                attachments.map((att) => (
                  <div key={att.id} className="flex items-center justify-between p-2.5 bg-slate-50 rounded-xl border border-slate-100 text-xs">
                    <div className="truncate mr-2">
                      <p className="font-semibold text-slate-800 truncate">{att.originalFileName}</p>
                      <p className="text-[10px] text-slate-400">{(att.fileSizeBytes / 1024).toFixed(1)} KB • {att.uploadedByFullName}</p>
                    </div>
                    <a
                      href={ticketApi.downloadAttachmentUrl(att.id)}
                      download
                      className="p-1.5 text-slate-500 hover:text-sky-600 hover:bg-sky-50 rounded-lg transition"
                      title="Download file"
                    >
                      <Download className="h-4 w-4" />
                    </a>
                  </div>
                ))
              )}
            </div>
          </div>
        </div>
      </div>

      {/* Resolve Modal */}
      {showResolveModal && (
        <div className="fixed inset-0 bg-slate-900/60 backdrop-blur-sm flex items-center justify-center p-4 z-50">
          <div className="bg-white rounded-2xl max-w-md w-full p-6 space-y-4 shadow-2xl border border-slate-100">
            <h3 className="text-lg font-bold text-slate-900">Resolve IT Ticket</h3>
            <p className="text-xs text-slate-500">Provide resolution summary explaining how the issue was fixed for the requester.</p>
            <textarea
              rows={4}
              required
              value={resolutionNotes}
              onChange={(e) => setResolutionNotes(e.target.value)}
              placeholder="e.g. Cleared corrupted DNS cache and verified server reachability..."
              className="w-full px-3.5 py-2.5 rounded-xl border border-slate-200 text-sm focus:ring-2 focus:ring-emerald-500"
            />
            <div className="flex justify-end gap-2">
              <button onClick={() => setShowResolveModal(false)} className="px-4 py-2 text-xs font-semibold text-slate-600 hover:bg-slate-100 rounded-xl">
                Cancel
              </button>
              <button onClick={handleResolve} className="px-4 py-2 bg-emerald-600 hover:bg-emerald-700 text-white text-xs font-semibold rounded-xl">
                Confirm Resolution
              </button>
            </div>
          </div>
        </div>
      )}

      {/* Reopen Modal */}
      {showReopenModal && (
        <div className="fixed inset-0 bg-slate-900/60 backdrop-blur-sm flex items-center justify-center p-4 z-50">
          <div className="bg-white rounded-2xl max-w-md w-full p-6 space-y-4 shadow-2xl border border-slate-100">
            <h3 className="text-lg font-bold text-slate-900 flex items-center gap-2">
              <AlertTriangle className="h-5 w-5 text-rose-600" /> Reopen Ticket
            </h3>
            <p className="text-xs text-slate-500">Specify why the resolution was insufficient.</p>
            <textarea
              rows={4}
              required
              value={reopenReason}
              onChange={(e) => setReopenReason(e.target.value)}
              placeholder="e.g. Issue reoccurred upon reboot..."
              className="w-full px-3.5 py-2.5 rounded-xl border border-slate-200 text-sm focus:ring-2 focus:ring-rose-500"
            />
            <div className="flex justify-end gap-2">
              <button onClick={() => setShowReopenModal(false)} className="px-4 py-2 text-xs font-semibold text-slate-600 hover:bg-slate-100 rounded-xl">
                Cancel
              </button>
              <button onClick={handleReopen} className="px-4 py-2 bg-rose-600 hover:bg-rose-700 text-white text-xs font-semibold rounded-xl">
                Reopen Ticket
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
};