/**
 * Samaaj - Domain Data Layer
 *
 * Every list endpoint in the backend returns PagedResponse<T>:
 *   { content: T[], page, size, totalElements, totalPages }
 *
 * This module owns pagination for each domain, translates the backend DTOs into
 * the shape the render functions consume, and exposes one loader per view.
 */

(function (global) {
  'use strict';

  const Api = window.Api;

  function formatDate(value) {
    if (!value) return '—';
    const parsed = new Date(value);
    if (isNaN(parsed.getTime())) return '—';
    return parsed.toLocaleDateString('en-IN', {
      day: '2-digit', month: 'short', year: 'numeric'
    });
  }

  function formatDateTime(value) {
    if (!value) return '—';
    const parsed = new Date(value);
    if (isNaN(parsed.getTime())) return '—';
    return parsed.toLocaleString('en-IN', {
      day: '2-digit', month: 'short', year: 'numeric', hour: '2-digit', minute: '2-digit'
    });
  }

  function titleCase(value) {
    if (!value) return '';
    return String(value).toLowerCase().replace(/_/g, ' ').replace(/\b\w/g, function (c) {
      return c.toUpperCase();
    });
  }

  function emptyPage(size) {
    return { items: [], page: 0, size: size || 10, total: 0, totalPages: 0 };
  }

  function mapComplaint(c) {
    const who = c.apartmentNumber
      ? c.apartmentNumber + (c.residentName ? ' (' + c.residentName + ')' : '')
      : (c.residentName || 'Management');
    return {
      id: c.id,
      title: c.title,
      description: c.description,
      category: c.category,
      status: c.status,
      reportedBy: who,
      adminNote: c.adminNote || '',
      date: formatDate(c.createdAt)
    };
  }

  function mapBill(b) {
    return {
      id: b.id,
      unit: b.apartmentNumber || '—',
      month: b.billingMonth || '—',
      amount: Number(b.amount || 0),
      dueDate: b.dueDate ? formatDate(b.dueDate) : '—',
      status: b.status,
      raw: b
    };
  }

  function mapIssue(i) {
    const creator = i.creator || {};
    const creatorName = [creator.firstName, creator.lastName].filter(Boolean).join(' ') || 'Resident';
    return {
      id: i.id,
      title: i.title,
      description: i.description,
      status: i.status,
      priority: i.priority,
      votes: i.voteCount || 0,
      author: creatorName,
      date: formatDate(i.createdAt),
      photoUrl: i.photoUrl || ''
    };
  }

  function mapAnnouncement(a) {
    return {
      id: a.id,
      title: a.title,
      content: a.content,
      type: a.type,
      author: a.authorId || 'Committee',
      date: formatDateTime(a.createdAt)
    };
  }

  function mapResident(u) {
    const name = [u.firstName, u.lastName].filter(Boolean).join(' ') || u.email;
    return {
      id: u.id,
      name: name,
      email: u.email,
      flat: u.apartmentNumber || 'Unassigned',
      role: u.role,
      status: u.status,
      date: formatDate(u.createdAt)
    };
  }

  const Data = {
    formatDate: formatDate,
    formatDateTime: formatDateTime,
    titleCase: titleCase,

    complaints: emptyPage(),
    bills: emptyPage(),
    issues: emptyPage(),
    announcements: emptyPage(),
    residents: emptyPage(),
    adminUsers: emptyPage(),

    async loadComplaints(options) {
      const opts = options || {};
      const res = await Api.get('/complaints', {
        status: opts.status || undefined,
        page: opts.page || 0,
        size: opts.size || 10
      });
      this.complaints = {
        items: ((res && res.content) || []).map(mapComplaint),
        page: res.page, size: res.size, total: res.totalElements, totalPages: res.totalPages
      };
      return this.complaints;
    },

    async loadBills(options) {
      const opts = options || {};
      const res = await Api.get('/finance/bills', {
        status: opts.status || undefined,
        page: opts.page || 0,
        size: opts.size || 10
      });
      this.bills = {
        items: ((res && res.content) || []).map(mapBill),
        page: res.page, size: res.size, total: res.totalElements, totalPages: res.totalPages
      };
      return this.bills;
    },

    async loadIssues(options) {
      const opts = options || {};
      const res = await Api.get('/issues', {
        page: opts.page || 0,
        size: opts.size || 12
      });
      this.issues = {
        items: ((res && res.content) || []).map(mapIssue),
        page: res.page, size: res.size, total: res.totalElements, totalPages: res.totalPages
      };
      return this.issues;
    },

    async loadAnnouncements(options) {
      const opts = options || {};
      const res = await Api.get('/announcements', {
        page: opts.page || 0,
        size: opts.size || 20
      });
      this.announcements = {
        items: ((res && res.content) || []).map(mapAnnouncement),
        page: res.page, size: res.size, total: res.totalElements, totalPages: res.totalPages
      };
      return this.announcements;
    },

    async loadResidents(options) {
      const opts = options || {};
      const res = await Api.get('/users', {
        page: opts.page || 0,
        size: opts.size || 20,
        status: opts.status || undefined,
        role: opts.role || undefined
      });
      this.residents = {
        items: ((res && res.content) || []).map(mapResident),
        page: res.page, size: res.size, total: res.totalElements, totalPages: res.totalPages
      };
      return this.residents;
    },

    async loadAdminUsers(options) {
      const opts = options || {};
      const res = await Api.get('/users', {
        page: opts.page || 0,
        size: opts.size || 20,
        status: opts.status || undefined,
        role: opts.role || undefined
      });
      this.adminUsers = {
        items: ((res && res.content) || []).map(mapResident),
        page: res.page, size: res.size, total: res.totalElements, totalPages: res.totalPages
      };
      return this.adminUsers;
    },

    async loadDashboardStats() {
      const stats = await Api.get('/dashboard/stats');
      if (!stats) return null;
      return {
        totalResidents: stats.totalResidents || 0,
        pendingApprovals: stats.pendingApprovals || 0,
        totalFlats: stats.totalFlats || 0,
        occupiedFlats: stats.occupiedFlats || 0,
        openComplaints: stats.openComplaints || 0,
        inProgressComplaints: stats.inProgressComplaints || 0,
        resolvedComplaints: stats.resolvedComplaints || 0,
        totalBills: stats.totalBills || 0,
        paidBills: stats.paidBills || 0,
        overdueBills: stats.overdueBills || 0,
        openIssues: stats.openIssues || 0,
        resolvedIssues: stats.resolvedIssues || 0
      };
    }
  };

  global.Data = Data;
})(window);
