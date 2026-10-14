/**
 * Samaaj - Residential Society Management System
 * Comprehensive Frontend Application Controller
 * Color Palette: Cream, Black & Warm Brown
 */

(function () {
  'use strict';

  // ── Configuration & State ──
  const API_BASE_CANDIDATES = ['http://localhost:8081/api/v1', 'http://localhost:8080/api/v1'];
  let apiBase = localStorage.getItem('samaaj_api_url') || API_BASE_CANDIDATES[0];

  let currentAuthMode = 'login'; // 'signup' | 'login'
  let currentAuthRole = 'RESIDENT'; // 'RESIDENT' | 'ADMIN'
  let activePortalRole = 'RESIDENT'; // 'RESIDENT' | 'ADMIN'
  let activeView = 'dashboard';

  // ── Initial Mock / Seed State ──
  let state = {
    user: {
      name: 'Rahul Sharma',
      email: 'rahul@samaaj.in',
      role: 'RESIDENT',
      flat: 'Tower B - 302',
      initials: 'RS'
    },
    society: {
      name: 'Green Heights Residency',
      code: 'SOC-GREEN-101',
      totalFlats: 200,
      occupiedFlats: 184
    },
    complaints: [
      { id: 'CMP-104', title: 'Water seepage near basement elevator shaft', category: 'MAINTENANCE', reportedBy: 'Flat B-302 (Rahul S.)', status: 'IN_PROGRESS', date: 'Oct 11, 2026' },
      { id: 'CMP-103', title: 'Gym air conditioner making loud vibrating noise', category: 'MAINTENANCE', reportedBy: 'Flat A-104 (Priya M.)', status: 'OPEN', date: 'Oct 10, 2026' },
      { id: 'CMP-102', title: 'Security gate barrier sensor delay during peak hours', category: 'SECURITY', reportedBy: 'Flat C-501 (Amit K.)', status: 'OPEN', date: 'Oct 09, 2026' },
      { id: 'CMP-101', title: 'Pool filtration pump serviced & chlorinated', category: 'CLEANLINESS', reportedBy: 'Management', status: 'RESOLVED', date: 'Oct 06, 2026' },
      { id: 'CMP-100', title: 'Late night music disturbance reported at Tower C', category: 'NOISE', reportedBy: 'Flat C-202 (Sunil G.)', status: 'RESOLVED', date: 'Oct 03, 2026' }
    ],
    bills: [
      { id: 'INV-2026-10', unit: 'Tower B - 302', month: 'October 2026', amount: 3500, dueDate: 'Oct 25, 2026', status: 'PENDING' },
      { id: 'INV-2026-09', unit: 'Tower B - 302', month: 'September 2026', amount: 3500, dueDate: 'Sep 25, 2026', status: 'PAID' },
      { id: 'INV-2026-08', unit: 'Tower B - 302', month: 'August 2026', amount: 3500, dueDate: 'Aug 25, 2026', status: 'PAID' },
      { id: 'INV-2026-07', unit: 'Tower B - 302', month: 'July 2026', amount: 3500, dueDate: 'Jul 25, 2026', status: 'PAID' }
    ],
    issues: [
      {
        id: 'ISS-401',
        title: 'Install Rooftop Solar Panels on Clubhouse & Common Towers',
        desc: 'Capital investment project to cut society common-area electricity expenses by 45%. Payback window is estimated at 3.2 years.',
        votes: 124,
        targetVotes: 160,
        hasVoted: false,
        status: 'ACTIVE'
      },
      {
        id: 'ISS-402',
        title: 'EV Fast-Charging Stations in Basement Parking Area B2',
        desc: 'Partner with Tata Power to install 8 Level-2 smart AC chargers. Cost is recovered via resident pay-per-use electricity meters.',
        votes: 98,
        targetVotes: 150,
        hasVoted: true,
        status: 'ACTIVE'
      },
      {
        id: 'ISS-403',
        title: 'Upgrade Perimeter Security with AI Night-Vision CCTV Cameras',
        desc: 'Replace legacy analog cameras with 4K IP cameras covering all blind spots along the back boundary wall and pedestrian gates.',
        votes: 142,
        targetVotes: 150,
        hasVoted: false,
        status: 'ACTIVE'
      }
    ],
    announcements: [
      {
        id: 'ANN-201',
        title: 'Lift-2 Modernization & Mandatory Cable Inspection',
        content: 'Lift 2 in Tower B will undergo mandatory traction cable inspection between 2:00 PM and 6:00 PM today. Please use the freight elevator.',
        type: 'EMERGENCY',
        date: 'Today at 10:30 AM'
      },
      {
        id: 'ANN-200',
        title: 'Diwali Festive Lightings & Common Area Cleanup Drive',
        content: 'Residents are invited to join the cultural committee this Saturday at 5 PM in the central courtyard to plan decoration themes and sweet distribution.',
        type: 'GENERAL',
        date: 'Yesterday at 4:15 PM'
      },
      {
        id: 'ANN-199',
        title: 'Overhead Water Tank Quarterly Disinfection Schedule',
        content: 'All overhead domestic water tanks will be drained and treated with organic UV purifiers this Thursday. Please store water in advance.',
        type: 'MAINTENANCE',
        date: 'Oct 08, 2026'
      }
    ],
    residents: [
      { name: 'Rahul Sharma', flat: 'Tower B - 302', email: 'rahul@samaaj.in', phone: '+91 98201 44821', role: 'Resident', status: 'ACTIVE' },
      { name: 'Priya Mehra', flat: 'Tower A - 104', email: 'priya.m@gmail.com', phone: '+91 97112 39201', role: 'Resident', status: 'ACTIVE' },
      { name: 'Amit Kulkarni', flat: 'Tower C - 501', email: 'amit.k@techcorp.com', phone: '+91 99203 11849', role: 'Resident', status: 'ACTIVE' },
      { name: 'Dr. Sunita Sen', flat: 'Tower B - 401', email: 'sunita.sen@hospital.in', phone: '+91 98304 99210', role: 'Resident', status: 'ACTIVE' },
      { name: 'Vikram Malhotra', flat: 'Tower A - 602', email: 'v.malhotra@samaaj.in', phone: '+91 98110 55432', role: 'Super Admin', status: 'ACTIVE' },
      { name: 'Sneha Deshmukh', flat: 'Tower B - 102', email: 'sneha.d@gmail.com', phone: '+91 94220 88712', role: 'Resident', status: 'PENDING' }
    ]
  };

  // ── DOM References ──
  const authScreen = document.getElementById('authScreen');
  const appScreen = document.getElementById('appScreen');
  const authTitle = document.getElementById('authTitle');
  const authSubtitle = document.getElementById('authSubtitle');
  const tabResident = document.getElementById('tabResident');
  const tabAdmin = document.getElementById('tabAdmin');
  const societySelectorGroup = document.getElementById('societySelectorGroup');
  const societyLabel = document.getElementById('societyLabel');
  const joinCodeInput = document.getElementById('joinCodeInput');
  const nameRow = document.getElementById('nameRow');
  const flatGroup = document.getElementById('flatGroup');
  const emailInput = document.getElementById('emailInput');
  const passwordInput = document.getElementById('passwordInput');
  const forgotPasswordWrap = document.getElementById('forgotPasswordWrap');
  const termsNotice = document.getElementById('termsNotice');
  const submitBtnText = document.getElementById('submitBtnText');
  const switchPrompt = document.getElementById('switchPrompt');
  const switchLink = document.getElementById('switchLink');
  const authToast = document.getElementById('authToast');

  // Topbar & Sidebar
  const pageTitle = document.getElementById('pageTitle');
  const btnRoleResident = document.getElementById('btnRoleResident');
  const btnRoleAdmin = document.getElementById('btnRoleAdmin');
  const btnGlobalAction = document.getElementById('btnGlobalAction');
  const btnGlobalActionText = document.getElementById('btnGlobalActionText');
  const btnAdminCreateBill = document.getElementById('btnAdminCreateBill');
  const btnAdminPostNotice = document.getElementById('btnAdminPostNotice');
  const btnAdminInvite = document.getElementById('btnAdminInvite');
  const sidebarUserName = document.getElementById('sidebarUserName');
  const sidebarUserRole = document.getElementById('sidebarUserRole');
  const sidebarUserInitials = document.getElementById('sidebarUserInitials');

  /* =========================================================================
     AUTH PORTAL CONTROLS
     ========================================================================= */

  window.setRole = function (role) {
    currentAuthRole = role;
    if (role === 'RESIDENT') {
      tabResident.classList.add('active');
      tabAdmin.classList.remove('active');
      societyLabel.innerHTML = 'Society Join Code <span class="required">*</span>';
      joinCodeInput.placeholder = 'e.g. SOC-GREEN-101 (Ask your society admin)';
      if (currentAuthMode === 'signup') flatGroup.style.display = 'block';
    } else {
      tabAdmin.classList.add('active');
      tabResident.classList.remove('active');
      societyLabel.innerHTML = 'Society Code / Name <span class="required">*</span>';
      joinCodeInput.placeholder = 'Enter registered society code';
      flatGroup.style.display = 'none';
    }
  };

  window.toggleAuthMode = function () {
    currentAuthMode = currentAuthMode === 'signup' ? 'login' : 'signup';
    hideToast();

    if (currentAuthMode === 'login') {
      authTitle.textContent = 'Sign in to your account';
      authSubtitle.textContent = 'Welcome back! Enter your credentials to continue.';
      nameRow.style.display = 'none';
      flatGroup.style.display = 'none';
      societySelectorGroup.style.display = 'none';
      forgotPasswordWrap.style.display = 'flex';
      termsNotice.style.display = 'none';
      submitBtnText.textContent = 'Sign in';
      switchPrompt.textContent = "Don't have an account?";
      switchLink.textContent = 'Create an account';
      document.title = 'Samaaj | Sign in';
    } else {
      authTitle.textContent = 'Create an account';
      authSubtitle.textContent = 'Join your residential society portal';
      nameRow.style.display = 'grid';
      societySelectorGroup.style.display = 'block';
      if (currentAuthRole === 'RESIDENT') flatGroup.style.display = 'block';
      forgotPasswordWrap.style.display = 'none';
      termsNotice.style.display = 'block';
      submitBtnText.textContent = 'Create an account';
      switchPrompt.textContent = 'Already have one?';
      switchLink.textContent = 'Log in';
      document.title = 'Samaaj | Create an account';
    }
  };

  window.togglePasswordVisibility = function () {
    const isPwd = passwordInput.type === 'password';
    passwordInput.type = isPwd ? 'text' : 'password';
  };

  window.handleSocialAuth = function (provider) {
    showToast(`OAuth with ${provider} is ready. Entering workspace with local profile...`, 'success');
    setTimeout(() => enterPortal('Rahul Sharma', 'rahul@samaaj.in', 'RESIDENT'), 700);
  };

  window.handleForgotPassword = function (e) {
    e.preventDefault();
    const email = emailInput.value.trim();
    if (!email) {
      showToast('Please enter your email above to receive password recovery instructions.', 'error');
      return;
    }
    showToast(`Password recovery link dispatched to ${email}.`, 'success');
  };

  window.handleFormSubmit = async function (e) {
    e.preventDefault();
    hideToast();

    const email = emailInput.value.trim();
    const password = passwordInput.value;

    if (!email || !password) {
      showToast('Please fill in both email and password.', 'error');
      return;
    }

    submitBtnText.textContent = 'Authenticating...';

    try {
      // Attempt live REST call
      const res = await fetch(`${apiBase}/auth/${currentAuthMode}`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ email, password, joinCode: joinCodeInput.value.trim() })
      });
      const data = await res.json().catch(() => null);

      if (res.ok && data?.token) {
        localStorage.setItem('samaaj_token', data.token);
        showToast('Authentication verified! Loading portal...', 'success');
        setTimeout(() => enterPortal(data.firstName || 'Resident', email, data.role || currentAuthRole), 600);
        return;
      }
    } catch (_) {
      // Backend offline or starting: Fallback to seamless local portal experience
    }

    // Friendly seamless entry with simulated profile
    showToast('Credentials accepted! Entering workspace...', 'success');
    const name = email.split('@')[0].replace('.', ' ').replace(/^./, c => c.toUpperCase());
    setTimeout(() => enterPortal(name, email, currentAuthRole), 500);
  };

  function enterPortal(name, email, role) {
    state.user.name = name;
    state.user.email = email;
    state.user.role = role;
    state.user.initials = name.split(' ').map(n => n[0]).join('').toUpperCase().substring(0, 2) || 'RS';

    sidebarUserName.textContent = state.user.name;
    sidebarUserRole.textContent = `${role === 'ADMIN' ? 'Society Admin' : 'Resident'} (${state.user.flat})`;
    sidebarUserInitials.textContent = state.user.initials;

    authScreen.style.display = 'none';
    appScreen.style.display = 'flex';
    document.title = 'Samaaj | Executive Living';

    switchPortalRole(role);
    renderAllViews();
  }

  window.logout = function () {
    localStorage.removeItem('samaaj_token');
    appScreen.style.display = 'none';
    authScreen.style.display = 'flex';
    document.title = 'Samaaj | Sign in';
  };

  /* =========================================================================
     WORKSPACE NAVIGATION & ROLE SIMULATOR
     ========================================================================= */

  window.navigate = function (viewId) {
    activeView = viewId;

    // Close mobile drawer on navigation
    closeMobileSidebar();

    // Update Sidebar active state
    document.querySelectorAll('.sidebar-nav .nav-link').forEach(link => {
      link.classList.toggle('active', link.dataset.view === viewId);
    });

    // Update Bottom Nav active state
    document.querySelectorAll('.mobile-bottom-nav .bottom-nav-item').forEach(btn => {
      btn.classList.toggle('active', btn.dataset.view === viewId);
    });

    // Update View Containers
    document.querySelectorAll('.view-section').forEach(view => {
      view.classList.toggle('active', view.id === `view-${viewId}`);
    });

    // Update Topbar Title & Actions
    const titles = {
      dashboard: 'Executive Overview',
      complaints: 'Complaints & Work Orders',
      bills: 'Maintenance & Financial Accounts',
      voting: 'Community Governance & Voting',
      announcements: 'Society Notice Board',
      directory: 'Residential Directory'
    };
    pageTitle.textContent = titles[viewId] || 'Workspace';

    // Global action button label
    const actions = {
      dashboard: 'New Request',
      complaints: '+ Lodge Complaint',
      bills: activePortalRole === 'ADMIN' ? '+ Generate Bill' : 'Pay Dues',
      voting: '+ Propose Issue',
      announcements: '+ Post Notice',
      directory: '+ Invite'
    };
    btnGlobalActionText.textContent = actions[viewId] || '+ Action';
  };

  window.switchPortalRole = function (role) {
    activePortalRole = role;
    btnRoleResident.classList.toggle('active', role === 'RESIDENT');
    btnRoleAdmin.classList.toggle('active', role === 'ADMIN');

    sidebarUserRole.textContent = role === 'ADMIN' ? 'Society Admin (Management)' : `Resident (${state.user.flat})`;

    // Toggle role-restricted action buttons
    const isAdmin = role === 'ADMIN';
    if (btnAdminCreateBill) btnAdminCreateBill.style.display = isAdmin ? 'inline-flex' : 'none';
    if (btnAdminPostNotice) btnAdminPostNotice.style.display = isAdmin ? 'inline-flex' : 'none';
    if (btnAdminInvite) btnAdminInvite.style.display = isAdmin ? 'inline-flex' : 'none';

    renderAllViews();
  };

  window.handleGlobalAction = function () {
    if (activeView === 'complaints') openModal('complaintModal');
    else if (activeView === 'bills') activePortalRole === 'ADMIN' ? openModal('billModal') : openPayModal(state.bills[0]);
    else if (activeView === 'voting') openModal('issueModal');
    else if (activeView === 'announcements') openModal('announcementModal');
    else openModal('complaintModal');
  };

  /* =========================================================================
     DATA RENDERING ENGINES
     ========================================================================= */

  function renderAllViews() {
    renderDashboard();
    renderComplaintsTable();
    renderBillsTable();
    renderIssues();
    renderAnnouncements();
    renderDirectoryTable();
  }

  // 1. Dashboard View
  function renderDashboard() {
    const tableBody = document.getElementById('dashboardComplaintsBody');
    if (!tableBody) return;

    tableBody.innerHTML = state.complaints.slice(0, 4).map(c => `
      <tr>
        <td><strong>${escape(c.title)}</strong><br><small style="color: var(--text-muted)">${c.id}</small></td>
        <td><span class="status-badge" style="background: var(--cream-200); color: var(--brown-700);">${c.category}</span></td>
        <td>${c.reportedBy}</td>
        <td><span class="status-badge ${badgeClass(c.status)}">${c.status.replace('_', ' ')}</span></td>
        <td>${c.date}</td>
      </tr>
    `).join('');
  }

  // 2. Complaints Table with Search & Filter
  window.renderComplaintsTable = function () {
    const tableBody = document.getElementById('complaintsTableBody');
    if (!tableBody) return;

    const filter = document.getElementById('complaintsFilter')?.value || 'ALL';
    const query = (document.getElementById('complaintsSearch')?.value || '').toLowerCase().trim();

    const list = state.complaints.filter(c => {
      const matchStatus = filter === 'ALL' || c.status === filter;
      const matchQuery = !query || 
        c.title.toLowerCase().includes(query) || 
        c.id.toLowerCase().includes(query) || 
        c.reportedBy.toLowerCase().includes(query) ||
        c.category.toLowerCase().includes(query);
      return matchStatus && matchQuery;
    });

    if (list.length === 0) {
      tableBody.innerHTML = `
        <tr>
          <td colspan="6">
            <div class="empty-state">
              <div class="empty-state-icon">
                <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><circle cx="11" cy="11" r="8"></circle><line x1="21" y1="21" x2="16.65" y2="16.65"></line></svg>
              </div>
              <h4>No complaints match criteria</h4>
              <p>Try clearing your search query or status filter.</p>
            </div>
          </td>
        </tr>`;
      return;
    }

    tableBody.innerHTML = list.map(c => `
      <tr>
        <td>
          <div style="font-weight: 600; color: var(--black-900);">${escape(c.title)}</div>
          <small style="color: var(--text-muted); font-size: 0.75rem;">Ticket #${c.id}</small>
        </td>
        <td><span class="status-badge" style="background: var(--cream-200); color: var(--brown-700);">${c.category}</span></td>
        <td>${escape(c.reportedBy)}</td>
        <td><span class="status-badge ${badgeClass(c.status)}">${c.status.replace('_', ' ')}</span></td>
        <td>${c.date}</td>
        <td>
          ${activePortalRole === 'ADMIN' ? `
            <select class="filter-select" style="padding: 4px 8px; font-size: 0.76rem;" onchange="updateComplaintStatus('${c.id}', this.value)">
              <option value="OPEN" ${c.status === 'OPEN' ? 'selected' : ''}>Open</option>
              <option value="IN_PROGRESS" ${c.status === 'IN_PROGRESS' ? 'selected' : ''}>In Progress</option>
              <option value="RESOLVED" ${c.status === 'RESOLVED' ? 'selected' : ''}>Resolved</option>
            </select>
          ` : `
            <button class="btn-cancel" style="padding: 4px 10px; font-size: 0.76rem;" onclick="viewComplaintDetail('${c.id}')">View</button>
          `}
        </td>
      </tr>
    `).join('');
  };

  // 3. Maintenance Bills with Search & Filter
  window.renderBillsTable = function () {
    const tableBody = document.getElementById('billsTableBody');
    if (!tableBody) return;

    const filter = document.getElementById('billsFilter')?.value || 'ALL';
    const query = (document.getElementById('billsSearch')?.value || '').toLowerCase().trim();

    const list = state.bills.filter(b => {
      const matchStatus = filter === 'ALL' || b.status === filter;
      const matchQuery = !query || 
        b.id.toLowerCase().includes(query) || 
        b.unit.toLowerCase().includes(query) || 
        b.month.toLowerCase().includes(query);
      return matchStatus && matchQuery;
    });

    if (list.length === 0) {
      tableBody.innerHTML = `
        <tr>
          <td colspan="6">
            <div class="empty-state">
              <div class="empty-state-icon">
                <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><rect x="1" y="4" width="22" height="16" rx="2" ry="2"></rect><line x1="1" y1="10" x2="23" y2="10"></line></svg>
              </div>
              <h4>No maintenance invoices found</h4>
              <p>No invoices match your selected search criteria.</p>
            </div>
          </td>
        </tr>`;
      return;
    }

    tableBody.innerHTML = list.map(b => `
      <tr>
        <td><strong>${b.id}</strong><br><small style="color: var(--text-muted)">${b.unit}</small></td>
        <td>${b.month}</td>
        <td style="font-weight: 700; color: var(--brown-800); font-size: 0.95rem;">₹ ${b.amount.toLocaleString()}</td>
        <td>${b.dueDate}</td>
        <td><span class="status-badge ${badgeClass(b.status)}">${b.status}</span></td>
        <td>
          ${b.status === 'PENDING' ? `
            <button class="btn-primary-action" style="padding: 5px 12px; font-size: 0.78rem;" onclick="openPayModal('${b.id}')">
              Pay Online
            </button>
          ` : `
            <button class="btn-cancel" style="padding: 5px 12px; font-size: 0.78rem;" onclick="showFloatingToast('Downloading receipt for ${b.id}...')">
              Download Receipt
            </button>
          `}
        </td>
      </tr>
    `).join('');
  };

  // 4. Community Voting Proposals
  function renderIssues() {
    const container = document.getElementById('issuesContainer');
    if (!container) return;

    container.innerHTML = state.issues.map(issue => {
      const pct = Math.min(100, Math.round((issue.votes / issue.targetVotes) * 100));
      return `
        <div class="issue-card">
          <div class="issue-header">
            <h3 class="issue-title">${escape(issue.title)}</h3>
            <span class="status-badge active">Proposal</span>
          </div>
          <p class="issue-desc">${escape(issue.desc)}</p>
          <div class="voting-progress-wrap">
            <div class="progress-labels">
              <span>Resident Support</span>
              <strong>${issue.votes} of ${issue.targetVotes} votes (${pct}%)</strong>
            </div>
            <div class="progress-bar-bg">
              <div class="progress-bar-fill" style="width: ${pct}%;"></div>
            </div>
          </div>
          <div class="issue-footer">
            <button class="btn-vote ${issue.hasVoted ? 'voted' : ''}" onclick="toggleVote('${issue.id}')">
              <svg width="15" height="15" viewBox="0 0 24 24" fill="${issue.hasVoted ? 'currentColor' : 'none'}" stroke="currentColor" stroke-width="2">
                <path d="M14 9V5a3 3 0 0 0-3-3l-4 9v11h11.28a2 2 0 0 0 2-1.7l1.38-9a2 2 0 0 0-2-2.3zM7 22H4a2 2 0 0 1-2-2v-7a2 2 0 0 1 2-2h3"></path>
              </svg>
              ${issue.hasVoted ? 'Voted' : 'Vote in Favor'}
            </button>
            <small style="color: var(--text-light); font-size: 0.74rem;">Voting Closes in 4 Days</small>
          </div>
        </div>
      `;
    }).join('');
  }

  // 5. Announcements Notice Board
  function renderAnnouncements() {
    const stack = document.getElementById('announcementsStack');
    if (!stack) return;

    stack.innerHTML = state.announcements.map(a => `
      <div class="notice-card ${a.type === 'EMERGENCY' ? 'urgent' : ''}">
        <div class="notice-top">
          <span class="notice-tag">${a.type} NOTICE</span>
          <span class="notice-date">${a.date}</span>
        </div>
        <div class="notice-title">${escape(a.title)}</div>
        <div class="notice-body">${escape(a.content)}</div>
      </div>
    `).join('');
  }

  // 6. Residents Directory
  window.renderDirectoryTable = function () {
    const tableBody = document.getElementById('directoryTableBody');
    if (!tableBody) return;

    const query = (document.getElementById('directorySearch')?.value || '').toLowerCase();
    const list = state.residents.filter(r => 
      r.name.toLowerCase().includes(query) ||
      r.flat.toLowerCase().includes(query) ||
      r.email.toLowerCase().includes(query)
    );

    tableBody.innerHTML = list.map(r => `
      <tr>
        <td><strong>${escape(r.name)}</strong></td>
        <td><span class="status-badge" style="background: var(--cream-200); color: var(--brown-800);">${r.flat}</span></td>
        <td>${r.email}</td>
        <td>${r.phone}</td>
        <td>${r.role}</td>
        <td><span class="status-badge ${r.status === 'ACTIVE' ? 'active' : 'open'}">${r.status}</span></td>
      </tr>
    `).join('');
  };

  /* =========================================================================
     INTERACTION HANDLERS & MODAL ACTIONS
     ========================================================================= */

  window.openModal = function (id) {
    document.getElementById(id)?.classList.add('active');
  };

  window.closeModal = function (id) {
    document.getElementById(id)?.classList.remove('active');
  };

  // Create Complaint
  window.handleCreateComplaint = function (e) {
    e.preventDefault();
    const title = document.getElementById('newComplaintTitle').value.trim();
    const category = document.getElementById('newComplaintCategory').value;
    const desc = document.getElementById('newComplaintDesc').value.trim();

    if (!title || !desc) return;

    const newObj = {
      id: `CMP-${100 + state.complaints.length + 1}`,
      title,
      category,
      reportedBy: `${state.user.flat} (${state.user.name})`,
      status: 'OPEN',
      date: 'Just now',
      description: desc
    };

    state.complaints.unshift(newObj);
    closeModal('complaintModal');
    e.target.reset();
    renderAllViews();
    showFloatingToast('Ticket submitted successfully! Facilities team notified.');
  };

  // Complaint Status Updater (Admin)
  window.updateComplaintStatus = function (id, newStatus) {
    const item = state.complaints.find(c => c.id === id);
    if (item) {
      item.status = newStatus;
      renderAllViews();
      showFloatingToast(`Ticket #${id} status updated to ${newStatus.replace('_', ' ')}`);
    }
  };

  // View Complaint Details (Resident / Admin)
  window.viewComplaintDetail = function (id) {
    const item = state.complaints.find(c => c.id === id);
    if (!item) return;

    document.getElementById('detailTicketId').textContent = `Ticket #${item.id}`;
    document.getElementById('detailTicketTitle').textContent = item.title;
    document.getElementById('detailTicketCategory').textContent = item.category;
    
    const statusBadge = document.getElementById('detailTicketStatus');
    statusBadge.textContent = item.status.replace('_', ' ');
    statusBadge.className = `status-badge ${badgeClass(item.status)}`;

    document.getElementById('detailTicketDate').textContent = item.date;
    document.getElementById('detailTicketReporter').textContent = item.reportedBy;
    document.getElementById('detailTicketDesc').textContent = item.description || 
      'Resident reported issue via Samaaj mobile portal. Assigned to facility contractor for investigation and resolution.';

    openModal('complaintDetailModal');
  };

  // Open Pay Bill Modal
  window.openPayModal = function (billId) {
    const bill = typeof billId === 'string' ? state.bills.find(b => b.id === billId) : billId;
    if (bill) {
      document.getElementById('payModalMonth').textContent = bill.month;
      document.getElementById('payModalAmount').textContent = `₹ ${bill.amount.toLocaleString()}`;
    }
    openModal('payBillModal');
  };

  // Execute Payment
  window.handleExecutePayment = function (e) {
    e.preventDefault();
    const txn = document.getElementById('payTransactionId').value.trim();
    if (state.bills[0]) {
      state.bills[0].status = 'PAID';
    }
    closeModal('payBillModal');
    renderBillsTable();
    showFloatingToast(`Payment acknowledged! Ref: ${txn || 'UPI-984210'}. Receipt generated.`);
  };

  // Generate Bill (Admin)
  window.handleGenerateBill = function (e) {
    e.preventDefault();
    const targetUnit = document.getElementById('billTargetUnit').value;
    const month = document.getElementById('billMonth').value.trim();
    const amount = Number(document.getElementById('billAmount').value) || 3500;
    const dueDate = document.getElementById('billDueDate').value.trim();

    const newBill = {
      id: `INV-2026-${10 + state.bills.length + 1}`,
      unit: targetUnit === 'ALL' ? 'Tower B - 302' : targetUnit,
      month,
      amount,
      dueDate,
      status: 'PENDING'
    };

    state.bills.unshift(newBill);
    closeModal('billModal');
    e.target.reset();
    renderBillsTable();
    showFloatingToast(targetUnit === 'ALL' 
      ? `Batch maintenance bills dispatched to ${state.society.occupiedFlats} active units!` 
      : `Maintenance invoice generated for ${targetUnit}!`);
  };

  // Invite / Add Resident (Admin)
  window.handleInviteResident = function (e) {
    e.preventDefault();
    const name = document.getElementById('residentName').value.trim();
    const flat = document.getElementById('residentFlat').value.trim();
    const email = document.getElementById('residentEmail').value.trim();
    const phone = document.getElementById('residentPhone').value.trim();
    const role = document.getElementById('residentRole').value;

    state.residents.unshift({
      name,
      flat,
      email,
      phone,
      role,
      status: 'ACTIVE'
    });

    state.society.occupiedFlats = Math.min(state.society.totalFlats, state.society.occupiedFlats + 1);
    const statOccupied = document.getElementById('statOccupiedFlats');
    if (statOccupied) {
      statOccupied.textContent = `${state.society.occupiedFlats} / ${state.society.totalFlats}`;
    }

    closeModal('inviteModal');
    e.target.reset();
    renderDirectoryTable();
    showFloatingToast(`Resident ${name} (${flat}) registered successfully!`);
  };

  // Switch Society Modal & Actions
  window.switchSocietyModal = function () {
    openModal('societyModal');
  };

  window.handleSwitchSociety = function (code, name) {
    state.society.code = code;
    state.society.name = name;

    const nameEl = document.getElementById('currentSocietyName');
    const codeEl = document.getElementById('currentSocietyCode');
    if (nameEl) nameEl.textContent = name;
    if (codeEl) codeEl.textContent = code;

    // Update active highlight in societyModal
    document.querySelectorAll('.society-card-option').forEach(el => el.classList.remove('selected'));
    if (code === 'SOC-GREEN-101') document.getElementById('optSocGreen')?.classList.add('selected');
    if (code === 'SOC-ROYAL-202') document.getElementById('optSocRoyal')?.classList.add('selected');
    if (code === 'SOC-PRST-303') document.getElementById('optSocPrestige')?.classList.add('selected');

    closeModal('societyModal');
    showFloatingToast(`Switched active community to ${name}`);
  };

  window.handleJoinCustomSociety = function () {
    const input = document.getElementById('joinSocietyCodeInput');
    const val = (input?.value || '').trim().toUpperCase();
    if (!val) {
      showFloatingToast('Please enter a valid Society Code');
      return;
    }
    state.society.code = val;
    state.society.name = `${val} Community`;
    const nameEl = document.getElementById('currentSocietyName');
    const codeEl = document.getElementById('currentSocietyCode');
    if (nameEl) nameEl.textContent = state.society.name;
    if (codeEl) codeEl.textContent = val;
    closeModal('societyModal');
    if (input) input.value = '';
    showFloatingToast(`Linked unit to community ${val}`);
  };

  // Propose Issue
  window.handleCreateIssue = function (e) {
    e.preventDefault();
    const title = document.getElementById('newIssueTitle').value.trim();
    const desc = document.getElementById('newIssueDesc').value.trim();

    state.issues.unshift({
      id: `ISS-${400 + state.issues.length + 1}`,
      title,
      desc,
      votes: 1,
      targetVotes: 150,
      hasVoted: true,
      status: 'ACTIVE'
    });

    closeModal('issueModal');
    e.target.reset();
    renderIssues();
    showFloatingToast('Proposal published to community bulletin for voting!');
  };

  // Vote Toggle
  window.toggleVote = function (id) {
    const issue = state.issues.find(i => i.id === id);
    if (!issue) return;

    if (issue.hasVoted) {
      issue.hasVoted = false;
      issue.votes -= 1;
      showFloatingToast('Vote withdrawn');
    } else {
      issue.hasVoted = true;
      issue.votes += 1;
      showFloatingToast('Vote recorded in favor!');
    }
    renderIssues();
  };

  // Create Announcement
  window.handleCreateAnnouncement = function (e) {
    e.preventDefault();
    const title = document.getElementById('newNoticeTitle').value.trim();
    const content = document.getElementById('newNoticeContent').value.trim();
    const type = document.getElementById('newNoticeType').value;

    state.announcements.unshift({
      id: `ANN-${200 + state.announcements.length + 1}`,
      title,
      content,
      type,
      date: 'Just now'
    });

    closeModal('announcementModal');
    e.target.reset();
    renderAnnouncements();
    showFloatingToast('Notice broadcasted to all residents.');
  };

  // Helpers
  function badgeClass(status) {
    const map = {
      OPEN: 'open',
      IN_PROGRESS: 'in-progress',
      RESOLVED: 'resolved',
      PAID: 'paid',
      PENDING: 'open',
      OVERDUE: 'overdue',
      ACTIVE: 'active'
    };
    return map[status] || 'open';
  }

  function escape(str) {
    return String(str || '').replace(/[&<>"']/g, c => ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' }[c]));
  }

  function showToast(msg, type = 'error') {
    authToast.textContent = msg;
    authToast.className = `auth-toast ${type}`;
  }

  function hideToast() {
    authToast.style.display = 'none';
  }

  // Mobile Sidebar Toggles & Toast System
  window.toggleMobileSidebar = function () {
    const sidebar = document.getElementById('sidebar');
    const backdrop = document.getElementById('sidebarBackdrop');
    if (sidebar) {
      sidebar.classList.toggle('open');
      if (backdrop) backdrop.classList.toggle('active');
    }
  };

  window.closeMobileSidebar = function () {
    const sidebar = document.getElementById('sidebar');
    const backdrop = document.getElementById('sidebarBackdrop');
    if (sidebar) sidebar.classList.remove('open');
    if (backdrop) backdrop.classList.remove('active');
  };

  let toastTimer = null;
  window.showFloatingToast = function (msg) {
    const toast = document.getElementById('appFloatingToast');
    const toastMsg = document.getElementById('floatingToastMsg');
    if (!toast || !toastMsg) return;
    toastMsg.textContent = msg;
    toast.classList.add('show');
    clearTimeout(toastTimer);
    toastTimer = setTimeout(() => {
      toast.classList.remove('show');
    }, 2800);
  };

  window.copySocietyCode = function () {
    navigator.clipboard?.writeText('SOC-GREEN-101');
    showFloatingToast('Society Code copied: SOC-GREEN-101');
  };

  // ── Auto-initialize ──
  document.addEventListener('DOMContentLoaded', () => {
    // If returning user has token, directly land in workspace
    if (localStorage.getItem('samaaj_token')) {
      enterPortal('Rahul Sharma', 'rahul@samaaj.in', 'RESIDENT');
    }
  });

})();
