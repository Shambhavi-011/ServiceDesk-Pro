import React, { useEffect, useState } from 'react';
import { adminApi } from '../../api/adminApi';
import { ticketApi } from '../../api/ticketApi';
import { Category } from '../../types';
import { FolderTree, Plus, Clock } from 'lucide-react';

export const CategoryManagement: React.FC = () => {
  const [categories, setCategories] = useState<Category[]>([]);
  const [showModal, setShowModal] = useState(false);
  const [newCat, setNewCat] = useState({ name: '', description: '', defaultSlaHours: 24 });
  const [loading, setLoading] = useState(true);

  const fetchCategories = async () => {
    setLoading(true);
    try {
      const data = await ticketApi.getCategories();
      setCategories(data);
    } catch (err) {
      console.error(err);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchCategories();
  }, []);

  const handleCreateCategory = async (e: React.FormEvent) => {
    e.preventDefault();
    await adminApi.createCategory(newCat);
    setShowModal(false);
    setNewCat({ name: '', description: '', defaultSlaHours: 24 });
    fetchCategories();
  };

  return (
    <div className="space-y-6">
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
        <div>
          <h2 className="text-2xl font-bold text-slate-900 flex items-center gap-2">
            <FolderTree className="h-6 w-6 text-sky-600" /> Categories & SLA Configuration
          </h2>
          <p className="text-sm text-slate-500">Define IT ticket classifications and resolution time windows</p>
        </div>
        <button
          onClick={() => setShowModal(true)}
          className="inline-flex items-center gap-2 px-4 py-2.5 bg-sky-600 hover:bg-sky-700 text-white rounded-xl text-xs font-semibold shadow-sm"
        >
          <Plus className="h-4 w-4" /> Add Category
        </button>
      </div>

      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
        {loading ? (
          <p className="text-slate-400">Loading categories...</p>
        ) : (
          categories.map((c) => (
            <div key={c.id} className="bg-white rounded-2xl border border-slate-200 p-6 shadow-sm flex flex-col justify-between">
              <div>
                <div className="flex items-center justify-between mb-3">
                  <h3 className="font-bold text-slate-900 text-base">{c.name}</h3>
                  <span className="inline-flex items-center gap-1 text-xs font-bold px-2.5 py-1 rounded-lg bg-sky-50 text-sky-700 border border-sky-100">
                    <Clock className="h-3.5 w-3.5" /> SLA: {c.defaultSlaHours}h
                  </span>
                </div>
                <p className="text-xs text-slate-600 leading-relaxed mb-4">{c.description || 'No description provided.'}</p>
              </div>
              <div className="pt-4 border-t border-slate-100 flex items-center justify-between text-[11px] text-slate-400">
                <span>Status: <strong className="text-emerald-600 font-semibold">Active</strong></span>
                <span>Created: {new Date(c.createdAt).toLocaleDateString()}</span>
              </div>
            </div>
          ))
        )}
      </div>

      {showModal && (
        <div className="fixed inset-0 bg-slate-900/60 backdrop-blur-sm flex items-center justify-center p-4 z-50">
          <div className="bg-white rounded-2xl max-w-md w-full p-6 space-y-4 shadow-2xl border border-slate-100">
            <h3 className="text-lg font-bold text-slate-900">Add Service Category</h3>
            <form onSubmit={handleCreateCategory} className="space-y-4 text-xs">
              <div>
                <label className="font-semibold text-slate-700">Category Name</label>
                <input
                  type="text"
                  required
                  value={newCat.name}
                  onChange={(e) => setNewCat({ ...newCat, name: e.target.value })}
                  placeholder="e.g. Cloud Infrastructure"
                  className="w-full px-3 py-2 rounded-xl border border-slate-200 mt-1"
                />
              </div>
              <div>
                <label className="font-semibold text-slate-700">Description</label>
                <textarea
                  rows={3}
                  value={newCat.description}
                  onChange={(e) => setNewCat({ ...newCat, description: e.target.value })}
                  placeholder="Scope of issues covered under this category..."
                  className="w-full px-3 py-2 rounded-xl border border-slate-200 mt-1"
                />
              </div>
              <div>
                <label className="font-semibold text-slate-700">Target SLA Resolution (Hours)</label>
                <input
                  type="number"
                  min={1}
                  max={720}
                  required
                  value={newCat.defaultSlaHours}
                  onChange={(e) => setNewCat({ ...newCat, defaultSlaHours: Number(e.target.value) })}
                  className="w-full px-3 py-2 rounded-xl border border-slate-200 mt-1"
                />
              </div>
              <div className="flex justify-end gap-2 pt-2">
                <button type="button" onClick={() => setShowModal(false)} className="px-4 py-2 text-slate-600 hover:bg-slate-100 rounded-xl font-semibold">
                  Cancel
                </button>
                <button type="submit" className="px-4 py-2 bg-sky-600 hover:bg-sky-700 text-white rounded-xl font-semibold">
                  Save Category
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
};