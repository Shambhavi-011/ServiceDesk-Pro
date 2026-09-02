import React from 'react';
import { useAuth } from '../../context/AuthContext';
import { NotificationDropdown } from '../common/NotificationDropdown';
import { LogOut, User as UserIcon, Shield, Headphones, UserCheck } from 'lucide-react';

export const Navbar: React.FC = () => {
  const { user, logout } = useAuth();

  const getRoleBadge = () => {
    if (user?.roles.includes('ROLE_ADMIN')) {
      return (
        <span className="flex items-center gap-1 text-xs font-semibold px-2 py-0.5 rounded bg-purple-100 text-purple-800 border border-purple-200">
          <Shield className="h-3 w-3" /> Admin
        </span>
      );
    }
    if (user?.roles.includes('ROLE_SUPPORT_AGENT')) {
      return (
        <span className="flex items-center gap-1 text-xs font-semibold px-2 py-0.5 rounded bg-blue-100 text-blue-800 border border-blue-200">
          <Headphones className="h-3 w-3" /> Agent
        </span>
      );
    }
    return (
      <span className="flex items-center gap-1 text-xs font-semibold px-2 py-0.5 rounded bg-slate-100 text-slate-700 border border-slate-200">
        <UserCheck className="h-3 w-3" /> Employee
      </span>
    );
  };

  return (
    <header className="h-16 bg-white border-b border-slate-200 px-6 flex items-center justify-between sticky top-0 z-30">
      <div className="flex items-center gap-3">
        <div className="h-9 w-9 rounded-lg bg-sky-600 flex items-center justify-center text-white font-black text-lg shadow-sm">
          S
        </div>
        <div>
          <h1 className="font-bold text-slate-900 text-base leading-tight">ServiceDesk Pro</h1>
          <p className="text-[11px] text-slate-500 font-medium">Enterprise ITSM System</p>
        </div>
      </div>

      <div className="flex items-center gap-4">
        <NotificationDropdown />

        <div className="h-6 w-px bg-slate-200"></div>

        <div className="flex items-center gap-3">
          <div className="h-9 w-9 rounded-full bg-slate-100 border border-slate-200 flex items-center justify-center text-slate-600">
            <UserIcon className="h-5 w-5" />
          </div>
          <div className="hidden sm:block text-left">
            <div className="flex items-center gap-2">
              <p className="text-xs font-semibold text-slate-800">{user?.firstName} {user?.lastName}</p>
              {getRoleBadge()}
            </div>
            <p className="text-[11px] text-slate-500">{user?.email}</p>
          </div>
        </div>

        <button
          onClick={logout}
          className="p-2 text-slate-400 hover:text-red-600 hover:bg-red-50 rounded-lg transition-colors"
          title="Sign out"
        >
          <LogOut className="h-5 w-5" />
        </button>
      </div>
    </header>
  );
};