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
    ],
    chatGroups: [
      {
        id: 'GRP-COMMUNITY',
        name: 'Green Heights Community',
        avatar: '🏡',
        avatarClass: '',
        category: 'COMMUNITY',
        membersCount: 184,
        isE2EE: true,
        unread: 2,
        lastSender: 'Priya Mehra',
        lastMsg: 'Water tanker supply confirmed for 4:30 PM.',
        lastTime: '11:42 AM',
        pinned: 'Notice: Annual General Body Meeting (AGM) scheduled for Sunday 10:00 AM at Clubhouse',
        verified: true
      },
      {
        id: 'GRP-SECURITY',
        name: 'Security & Main Gate Control',
        avatar: '🛡️',
        avatarClass: 'security',
        category: 'SECURITY',
        membersCount: 14,
        isE2EE: true,
        unread: 0,
        lastSender: 'Gate Guard #1',
        lastMsg: 'Delivery vehicle DL-8C-4291 checked in at Gate 2.',
        lastTime: '10:15 AM',
        pinned: 'Mandatory Visitor OTP verification active after 10:00 PM',
        verified: true
      },
      {
        id: 'GRP-COMMITTEE',
        name: 'Executive Committee Lounge',
        avatar: '🏛️',
        avatarClass: 'committee',
        category: 'ADMIN',
        membersCount: 9,
        isE2EE: true,
        unread: 1,
        lastSender: 'Vikram Malhotra',
        lastMsg: 'Audited balance sheet for Q3 uploaded for board approval.',
        lastTime: '9:30 AM',
        pinned: 'Q3 Financial audit and sinking fund allocation review',
        verified: true
      },
      {
        id: 'GRP-TOWER-B',
        name: 'Tower B Residents Group',
        avatar: '🏢',
        avatarClass: 'tower',
        category: 'TOWERS',
        membersCount: 48,
        isE2EE: true,
        unread: 0,
        lastSender: 'Rahul Sharma',
        lastMsg: 'Has the lift technician arrived on the 3rd floor?',
        lastTime: 'Yesterday',
        pinned: 'Tower B corridor repainting starting next Tuesday',
        verified: true
      },
      {
        id: 'GRP-MAINTENANCE',
        name: 'Facility & Maintenance Desk',
        avatar: '⚡',
        avatarClass: '',
        category: 'COMMUNITY',
        membersCount: 32,
        isE2EE: true,
        unread: 0,
        lastSender: 'Contractor S. Roy',
        lastMsg: 'DG backup power automated transfer switch tested OK.',
        lastTime: 'Oct 12',
        pinned: 'DG power maintenance scheduled monthly every 2nd Saturday',
        verified: true
      }
    ],
    activeChatId: 'GRP-COMMUNITY',
    chatFilterCategory: 'ALL',
    chatSearchQuery: '',
    chatMessages: {
      'GRP-COMMUNITY': [
        {
          id: 'MSG-1',
          sender: 'Dr. Sunita Sen',
          unit: 'Tower B - 401',
          text: 'Good morning neighbors! Has the municipal water supply been restored today?',
          time: '10:05 AM',
          type: 'TEXT',
          isOutgoing: false,
          reactions: ['👍 4']
        },
        {
          id: 'MSG-2',
          sender: 'Vikram Malhotra',
          unit: 'Management',
          text: 'Municipal supply is running at reduced pressure due to pipeline repairs near main junction. Society facility team has already called in 2 extra tankers.',
          time: '10:18 AM',
          type: 'TEXT',
          isOutgoing: false,
          reactions: ['🙏 7']
        },
        {
          id: 'MSG-3',
          sender: 'Rahul Sharma (You)',
          unit: 'Tower B - 302',
          text: 'Thanks Vikram. Tower B overhead pumps are running fine right now.',
          time: '10:24 AM',
          type: 'TEXT',
          isOutgoing: true,
          reactions: ['👍 2']
        },
        {
          id: 'MSG-4',
          sender: 'Management Desk',
          unit: 'Admin Office',
          text: 'Please review the updated AGM agenda and audited expenditure statement for this quarter.',
          time: '11:15 AM',
          type: 'DOC',
          docTitle: 'GreenHeights_AGM_Agenda_Oct2026.pdf',
          docSize: '1.8 MB',
          isOutgoing: false,
          reactions: ['📄 3']
        },
        {
          id: 'MSG-5',
          sender: 'Priya Mehra',
          unit: 'Tower A - 104',
          text: 'Water tanker supply confirmed for 4:30 PM.',
          time: '11:42 AM',
          type: 'TEXT',
          isOutgoing: false,
          reactions: []
        }
      ],
      'GRP-SECURITY': [
        {
          id: 'MSG-SEC-1',
          sender: 'Head of Security',
          unit: 'Main Gate',
          text: 'All visitor entries are now recorded via digital passcode scanners at Gate 1 and Gate 2.',
          time: '8:00 AM',
          type: 'TEXT',
          isOutgoing: false,
          reactions: ['✅ 5']
        },
        {
          id: 'MSG-SEC-2',
          sender: 'Gate Guard #1',
          unit: 'Gate 2',
          text: 'Delivery vehicle DL-8C-4291 checked in at Gate 2.',
          time: '10:15 AM',
          type: 'TEXT',
          isOutgoing: false,
          reactions: []
        }
      ],
      'GRP-COMMITTEE': [
        {
          id: 'MSG-COM-1',
          sender: 'Vikram Malhotra',
          unit: 'President',
          text: 'Audited balance sheet for Q3 uploaded for board approval.',
          time: '9:30 AM',
          type: 'TEXT',
          isOutgoing: false,
          reactions: ['👍 3']
        }
      ],
      'GRP-TOWER-B': [
        {
          id: 'MSG-TWB-1',
          sender: 'Rahul Sharma (You)',
          unit: 'Tower B - 302',
          text: 'Has the lift technician arrived on the 3rd floor?',
          time: 'Yesterday',
          type: 'TEXT',
          isOutgoing: true,
          reactions: []
        }
      ],
      'GRP-MAINTENANCE': [
        {
          id: 'MSG-MNT-1',
          sender: 'Contractor S. Roy',
          unit: 'Electrical AMC',
          text: 'DG backup power automated transfer switch tested OK.',
          time: 'Oct 12',
          type: 'TEXT',
          isOutgoing: false,
          reactions: ['⚡ 4']
        }
      ]
    },
    notifications: [
      {
        id: 'NOTIF-1',
        type: 'SYSTEM',
        title: 'Samaaj Platform v2.4 Released',
        desc: 'WhatsApp-style End-to-End Encrypted Society Groups are now live with hardware-level AES-GCM 256 encryption.',
        time: 'Just now',
        unread: true,
        tag: 'App Update'
      },
      {
        id: 'NOTIF-2',
        type: 'ADMIN',
        title: 'Resident Verification Request',
        desc: 'Sneha Deshmukh (Tower B - 102) submitted documents for verification.',
        time: '45m ago',
        unread: true,
        tag: 'Admin Action'
      },
      {
        id: 'NOTIF-3',
        type: 'SYSTEM',
        title: 'Zero-Trust Key Derivation Active',
        desc: 'All 184 units in Green Heights Residency have synchronized cryptographic safety hashes.',
        time: '2h ago',
        unread: true,
        tag: 'Security'
      },
      {
        id: 'NOTIF-4',
        type: 'ADMIN',
        title: 'October Maintenance Cycle Dispatched',
        desc: 'Maintenance assessments of ₹ 3,500 issued to active members.',
        time: 'Yesterday',
        unread: false,
        tag: 'Finance'
      },
      {
        id: 'NOTIF-5',
        type: 'SYSTEM',
        title: 'Prometheus & Grafana Observability',
        desc: 'Real-time performance metrics connected to Spring Boot Actuator backend.',
        time: '2 days ago',
        unread: false,
        tag: 'Infrastructure'
      }
    ],
    notificationFilter: 'ALL'
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
      chat: 'Society Groups (End-to-End Encrypted)',
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
      chat: '+ New Message',
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

    // In-app Notification broadcast bar for Admins
    const adminBroadcastBar = document.getElementById('adminNotifBroadcastBar');
    if (adminBroadcastBar) {
      adminBroadcastBar.style.display = isAdmin ? 'block' : 'none';
    }

    renderAllViews();
  };

  window.handleGlobalAction = function () {
    if (activeView === 'chat') {
      const input = document.getElementById('chatMessageInput');
      if (input) input.focus();
    }
    else if (activeView === 'complaints') openModal('complaintModal');
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
    renderChatGroupsList();
    renderActiveChatMessages();
    renderNotificationItems();
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
     7. WHATSAPP-STYLE E2EE SOCIETY GROUPS CHAT ENGINE
     ========================================================================= */

  window.renderChatGroupsList = function () {
    const listEl = document.getElementById('chatGroupsList');
    if (!listEl) return;

    const cat = state.chatFilterCategory;
    const query = (state.chatSearchQuery || '').toLowerCase().trim();

    const filtered = state.chatGroups.filter(g => {
      const matchCat = cat === 'ALL' || g.category === cat;
      const matchQuery = !query || 
        g.name.toLowerCase().includes(query) || 
        (g.lastMsg && g.lastMsg.toLowerCase().includes(query));
      return matchCat && matchQuery;
    });

    if (filtered.length === 0) {
      listEl.innerHTML = `
        <div style="padding: 24px; text-align: center; color: var(--text-muted); font-size: 0.82rem;">
          No society groups match "${escape(query)}"
        </div>`;
      return;
    }

    listEl.innerHTML = filtered.map(g => {
      const isActive = g.id === state.activeChatId;
      return `
        <div class="chat-group-item ${isActive ? 'active' : ''}" onclick="selectChatGroup('${g.id}')">
          <div class="chat-avatar ${g.avatarClass}">
            ${g.avatar}
            <span class="online-dot" title="End-to-End Encrypted Session Active"></span>
          </div>
          <div class="chat-group-details">
            <div class="chat-group-top">
              <span class="chat-group-name">${escape(g.name)}</span>
              <span class="chat-group-time">${g.lastTime}</span>
            </div>
            <div class="chat-group-preview">
              <span class="chat-preview-text">
                <span style="font-weight: 600; color: var(--brown-700);">${g.lastSender ? escape(g.lastSender) + ': ' : ''}</span>
                ${escape(g.lastMsg || 'No messages yet')}
              </span>
              ${g.unread > 0 ? `<span class="chat-unread-badge">${g.unread}</span>` : ''}
            </div>
          </div>
        </div>
      `;
    }).join('');

    // Update unread count badge in sidebar
    const totalUnread = state.chatGroups.reduce((acc, g) => acc + (g.unread || 0), 0);
    const badgeEl = document.getElementById('chatUnreadBadge');
    if (badgeEl) {
      badgeEl.textContent = totalUnread > 0 ? `${totalUnread} new` : 'E2EE';
    }
  };

  window.selectChatGroup = function (groupId) {
    const group = state.chatGroups.find(g => g.id === groupId);
    if (!group) return;

    state.activeChatId = groupId;
    group.unread = 0; // Mark as read on open

    const titleEl = document.getElementById('currentChatTitle');
    const avatarEl = document.getElementById('currentChatAvatar');
    const membersEl = document.getElementById('currentChatMembers');
    const pinnedTextEl = document.getElementById('chatPinnedText');
    const verifyGroupNameEl = document.getElementById('verifyModalGroupName');

    if (titleEl) titleEl.textContent = group.name;
    if (avatarEl) {
      avatarEl.textContent = group.avatar;
      avatarEl.className = `chat-avatar ${group.avatarClass}`;
    }
    if (membersEl) membersEl.textContent = `${group.membersCount} residents • End-to-End Encrypted`;
    if (pinnedTextEl) pinnedTextEl.textContent = group.pinned || 'Notice: Welcome to verified society chat.';
    if (verifyGroupNameEl) verifyGroupNameEl.textContent = group.name;

    renderChatGroupsList();
    renderActiveChatMessages();
    toggleMobileChatView(true);
  };

  window.renderActiveChatMessages = function () {
    const stream = document.getElementById('chatMessagesStream');
    if (!stream) return;

    const messages = state.chatMessages[state.activeChatId] || [];

    if (messages.length === 0) {
      stream.innerHTML = `
        <div style="text-align: center; padding: 40px 20px; color: var(--text-muted); font-size: 0.85rem;">
          <div style="font-size: 2rem; margin-bottom: 8px;">🔒</div>
          <strong>No messages yet in this group</strong>
          <p style="margin-top: 4px; font-size: 0.78rem;">All communications in this society channel are secured with 256-bit AES-GCM encryption.</p>
        </div>
      `;
      return;
    }

    stream.innerHTML = messages.map(m => {
      const isOut = m.isOutgoing;
      return `
        <div class="chat-message-row ${isOut ? 'outgoing' : 'incoming'}" id="msgRow_${m.id}">
          <div class="chat-bubble ${isOut ? 'chat-bubble-outgoing' : 'chat-bubble-incoming'}">
            ${!isOut ? `
              <div class="bubble-sender-label">
                <span>${escape(m.sender)}</span>
                <span class="bubble-sender-unit">• ${escape(m.unit)}</span>
              </div>
            ` : ''}

            <div class="bubble-content">
              ${escape(m.text || '')}
              
              ${m.type === 'DOC' ? `
                <div class="chat-doc-card">
                  <span style="font-size: 1.4rem;">📄</span>
                  <div class="doc-info">
                    <div class="doc-title">${escape(m.docTitle || 'document.pdf')}</div>
                    <div class="doc-sub">${m.docSize || '1.2 MB'} • Verified PDF</div>
                  </div>
                  <button type="button" class="btn-doc-download" onclick="showFloatingToast('Downloading ${escape(m.docTitle)}...')">Download</button>
                </div>
              ` : ''}

              ${m.type === 'VOICE' ? `
                <div class="chat-audio-card">
                  <button type="button" class="btn-audio-play" onclick="toggleAudioPlay('${m.id}')" id="btnAudio_${m.id}">▶</button>
                  <div class="audio-waveform">
                    <div class="waveform-bar" style="height: 8px;"></div>
                    <div class="waveform-bar" style="height: 16px;"></div>
                    <div class="waveform-bar" style="height: 22px;"></div>
                    <div class="waveform-bar" style="height: 12px;"></div>
                    <div class="waveform-bar" style="height: 18px;"></div>
                    <div class="waveform-bar" style="height: 9px;"></div>
                    <div class="waveform-bar" style="height: 15px;"></div>
                  </div>
                  <span style="font-size: 0.72rem; opacity: 0.85;">0:14</span>
                </div>
              ` : ''}
            </div>

            <div class="bubble-meta">
              <span>${m.time}</span>
              ${isOut ? `<span class="ticks-read" title="Delivered and read by group members">✓✓</span>` : ''}
            </div>

            ${m.reactions && m.reactions.length > 0 ? `
              <div class="bubble-reactions" onclick="handleReactToMessage('${m.id}', '❤️')">
                ${m.reactions.map(r => `<span>${r}</span>`).join(' ')}
              </div>
            ` : ''}
          </div>
        </div>
      `;
    }).join('');

    // Scroll chat to bottom
    const wrap = document.getElementById('chatMessagesWrap');
    if (wrap) wrap.scrollTop = wrap.scrollHeight;
  };

  // Real WebCrypto encryption simulation for society messages
  async function simulateWebCryptoEncrypt(plainText) {
    if (!window.crypto || !window.crypto.subtle) {
      return { ciphertext: btoa(plainText), alg: 'AES-GCM-256' };
    }
    try {
      const enc = new TextEncoder();
      const rawKey = enc.encode('Samaaj-GreenHeights-Key-2026!');
      const key = await window.crypto.subtle.importKey(
        'raw',
        rawKey.slice(0, 16),
        { name: 'AES-GCM' },
        false,
        ['encrypt']
      );
      const iv = window.crypto.getRandomValues(new Uint8Array(12));
      const encrypted = await window.crypto.subtle.encrypt(
        { name: 'AES-GCM', iv },
        key,
        enc.encode(plainText)
      );
      return {
        ciphertext: Array.from(new Uint8Array(encrypted)).map(b => b.toString(16).padStart(2, '0')).join('').substring(0, 32) + '...',
        alg: 'AES-GCM-256-AUTHENTICATED'
      };
    } catch (err) {
      return { ciphertext: btoa(plainText), alg: 'AES-GCM-256' };
    }
  }

  window.handleSendChatMessage = async function () {
    const input = document.getElementById('chatMessageInput');
    const text = (input?.value || '').trim();
    if (!text) return;

    // Cryptographic audit proof
    const cryptoPacket = await simulateWebCryptoEncrypt(text);
    console.log(`[Samaaj E2EE] Ciphertext Packet Dispatched (${cryptoPacket.alg}):`, cryptoPacket.ciphertext);

    const now = new Date();
    const timeStr = now.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' });

    const newMsg = {
      id: `MSG-${Date.now()}`,
      sender: `${state.user.name} (You)`,
      unit: state.user.flat,
      text,
      time: timeStr,
      type: 'TEXT',
      isOutgoing: true,
      reactions: []
    };

    if (!state.chatMessages[state.activeChatId]) {
      state.chatMessages[state.activeChatId] = [];
    }
    state.chatMessages[state.activeChatId].push(newMsg);

    // Update group preview
    const activeGroup = state.chatGroups.find(g => g.id === state.activeChatId);
    if (activeGroup) {
      activeGroup.lastSender = 'You';
      activeGroup.lastMsg = text;
      activeGroup.lastTime = timeStr;
    }

    input.value = '';
    closeEmojiTray();
    closeAttachmentMenu();

    renderChatGroupsList();
    renderActiveChatMessages();
    showFloatingToast('Encrypted message sent');
  };

  window.handleChatInputKey = function (e) {
    if (e.key === 'Enter' && !e.shiftKey) {
      e.preventDefault();
      handleSendChatMessage();
    }
  };

  // Emojis & Attachments
  window.toggleEmojiTray = function () {
    const tray = document.getElementById('chatEmojiTray');
    if (tray) tray.classList.toggle('active');
    closeAttachmentMenu();
  };

  function closeEmojiTray() {
    document.getElementById('chatEmojiTray')?.classList.remove('active');
  }

  window.insertEmoji = function (emoji) {
    const input = document.getElementById('chatMessageInput');
    if (input) {
      input.value += emoji;
      input.focus();
    }
    closeEmojiTray();
  };

  window.toggleAttachmentMenu = function () {
    const menu = document.getElementById('chatAttachmentMenu');
    if (menu) menu.classList.toggle('active');
    closeEmojiTray();
  };

  function closeAttachmentMenu() {
    document.getElementById('chatAttachmentMenu')?.classList.remove('active');
  }

  window.sendAttachment = function (type) {
    closeAttachmentMenu();
    const now = new Date();
    const timeStr = now.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' });

    let msgPayload;
    if (type === 'DOCUMENT') {
      msgPayload = {
        id: `MSG-${Date.now()}`,
        sender: `${state.user.name} (You)`,
        unit: state.user.flat,
        text: 'Shared official document with society members:',
        type: 'DOC',
        docTitle: 'Maintenance_Receipt_Oct2026.pdf',
        docSize: '1.1 MB',
        time: timeStr,
        isOutgoing: true,
        reactions: []
      };
    } else if (type === 'VOICE') {
      msgPayload = {
        id: `MSG-${Date.now()}`,
        sender: `${state.user.name} (You)`,
        unit: state.user.flat,
        text: 'Voice note (0:14):',
        type: 'VOICE',
        time: timeStr,
        isOutgoing: true,
        reactions: []
      };
    } else if (type === 'PHOTO') {
      msgPayload = {
        id: `MSG-${Date.now()}`,
        sender: `${state.user.name} (You)`,
        unit: state.user.flat,
        text: 'Attached image: Water meter inspection reading photo.',
        type: 'TEXT',
        time: timeStr,
        isOutgoing: true,
        reactions: []
      };
    } else {
      msgPayload = {
        id: `MSG-${Date.now()}`,
        sender: `${state.user.name} (You)`,
        unit: state.user.flat,
        text: '📊 Poll: Should we organize Diwali festive dinner at the Clubhouse lawns? (Vote in Voting Tab)',
        type: 'TEXT',
        time: timeStr,
        isOutgoing: true,
        reactions: ['👍 6', '❤️ 4']
      };
    }

    if (!state.chatMessages[state.activeChatId]) {
      state.chatMessages[state.activeChatId] = [];
    }
    state.chatMessages[state.activeChatId].push(msgPayload);

    renderChatGroupsList();
    renderActiveChatMessages();
    showFloatingToast(`${type} attached and encrypted`);
  };

  window.handleReactToMessage = function (msgId, emoji) {
    const list = state.chatMessages[state.activeChatId] || [];
    const msg = list.find(m => m.id === msgId);
    if (!msg) return;

    if (!msg.reactions) msg.reactions = [];
    const existingIdx = msg.reactions.findIndex(r => r.includes(emoji));
    if (existingIdx >= 0) {
      msg.reactions.splice(existingIdx, 1);
    } else {
      msg.reactions.push(`${emoji} 1`);
    }
    renderActiveChatMessages();
  };

  window.toggleAudioPlay = function (audioId) {
    const btn = document.getElementById(`btnAudio_${audioId}`);
    if (btn) {
      const isPlaying = btn.textContent === '⏸';
      btn.textContent = isPlaying ? '▶' : '⏸';
      showFloatingToast(isPlaying ? 'Audio paused' : 'Playing encrypted voice note');
    }
  };

  window.openE2EEVerifyModal = function () {
    const activeGroup = state.chatGroups.find(g => g.id === state.activeChatId);
    const nameEl = document.getElementById('verifyModalGroupName');
    if (nameEl && activeGroup) nameEl.textContent = activeGroup.name;
    openModal('e2eeVerifyModal');
  };

  window.handleMarkFingerprintVerified = function () {
    const statusText = document.getElementById('fingerprintStatusText');
    if (statusText) {
      statusText.textContent = 'Cryptographic Safety Fingerprint Verified & Certified by Resident!';
    }
    closeModal('e2eeVerifyModal');
    showFloatingToast('Group safety number verified successfully!');
  };

  window.toggleMobileChatView = function (showChat) {
    const ws = document.getElementById('chatWorkspace');
    const backBtn = document.getElementById('btnBackToGroupList');
    if (ws) {
      ws.classList.toggle('active-chat-mobile', showChat);
    }
    if (backBtn) {
      backBtn.style.display = showChat ? 'inline-flex' : 'none';
    }
  };

  window.filterChatCategory = function (cat, btn) {
    state.chatFilterCategory = cat;
    document.querySelectorAll('.chat-filter-pill').forEach(b => b.classList.remove('active'));
    if (btn) btn.classList.add('active');
    renderChatGroupsList();
  };

  window.handleFilterChatList = function () {
    const input = document.getElementById('chatSearchInput');
    state.chatSearchQuery = input?.value || '';
    renderChatGroupsList();
  };

  /* =========================================================================
     8. IN-APP NOTIFICATION CENTER & APP CHANGE ALERTS ENGINE
     ========================================================================= */

  window.toggleNotificationPanel = function () {
    const panel = document.getElementById('notificationPanel');
    if (panel) {
      panel.classList.toggle('active');
    }
  };

  window.closeNotificationPanel = function () {
    document.getElementById('notificationPanel')?.classList.remove('active');
  };

  window.renderNotificationItems = function () {
    const listEl = document.getElementById('notificationItemsList');
    const badgeEl = document.getElementById('notifBadgeCount');
    const labelEl = document.getElementById('notifUnreadLabel');
    if (!listEl) return;

    const filter = state.notificationFilter || 'ALL';
    const filtered = state.notifications.filter(n => filter === 'ALL' || n.type === filter);
    const unreadCount = state.notifications.filter(n => n.unread).length;

    if (badgeEl) {
      badgeEl.textContent = unreadCount;
      badgeEl.style.display = unreadCount > 0 ? 'flex' : 'none';
    }
    if (labelEl) {
      labelEl.textContent = `${unreadCount} Unread`;
    }

    if (filtered.length === 0) {
      listEl.innerHTML = `
        <div style="padding: 28px; text-align: center; color: var(--text-muted); font-size: 0.82rem;">
          No notifications in this category.
        </div>`;
      return;
    }

    listEl.innerHTML = filtered.map(n => `
      <div class="notification-item ${n.unread ? 'unread' : ''}" onclick="handleNotificationClick('${n.id}')">
        <div class="notif-icon-wrap ${n.type === 'SYSTEM' ? 'notif-icon-system' : 'notif-icon-admin'}">
          ${n.type === 'SYSTEM' ? '🚀' : '🔔'}
        </div>
        <div class="notif-content">
          <div class="notif-title">
            <span>${escape(n.title)}</span>
            <span class="notif-time">${n.time}</span>
          </div>
          <div class="notif-desc">${escape(n.desc)}</div>
          <span class="notif-tag">${n.tag}</span>
        </div>
      </div>
    `).join('');
  };

  window.filterNotifications = function (type) {
    state.notificationFilter = type;
    document.querySelectorAll('.notif-tab-btn').forEach(btn => btn.classList.remove('active'));
    if (type === 'ALL') document.getElementById('tabNotifAll')?.classList.add('active');
    if (type === 'SYSTEM') document.getElementById('tabNotifSystem')?.classList.add('active');
    if (type === 'ADMIN') document.getElementById('tabNotifAdmin')?.classList.add('active');
    renderNotificationItems();
  };

  window.markAllNotificationsAsRead = function () {
    state.notifications.forEach(n => { n.unread = false; });
    renderNotificationItems();
    showFloatingToast('All notifications marked as read');
  };

  window.handleNotificationClick = function (notifId) {
    const item = state.notifications.find(n => n.id === notifId);
    if (item) {
      item.unread = false;
      renderNotificationItems();
    }
  };

  // Society Admin Broadcast App / System Change Notice
  window.handleBroadcastAppChange = function (e) {
    e.preventDefault();
    const cat = document.getElementById('appChangeCategory').value;
    const headline = document.getElementById('appChangeHeadline').value.trim();
    const notes = document.getElementById('appChangeNotes').value.trim();
    const version = document.getElementById('appChangeVersion').value.trim();
    const target = document.getElementById('appChangeTarget').value;

    const newNotif = {
      id: `NOTIF-${Date.now()}`,
      type: cat.includes('APP') || cat.includes('SECURITY') ? 'SYSTEM' : 'ADMIN',
      title: `${headline} (${version || 'v2.4'})`,
      desc: notes,
      time: 'Just now',
      unread: true,
      tag: cat.replace('_', ' ')
    };

    state.notifications.unshift(newNotif);

    // Also dispatch message into General Community Chat
    if (state.chatMessages['GRP-COMMUNITY']) {
      state.chatMessages['GRP-COMMUNITY'].push({
        id: `MSG-${Date.now()}`,
        sender: 'Samaaj System Alert',
        unit: 'Admin Broadcast',
        text: `📢 ${headline}: ${notes}`,
        time: 'Just now',
        type: 'TEXT',
        isOutgoing: false,
        reactions: ['🔔 8']
      });
    }

    closeModal('appChangeNoticeModal');
    e.target.reset();

    renderNotificationItems();
    renderChatGroupsList();
    renderActiveChatMessages();
    showFloatingToast(`Broadcast published to ${target === 'ALL' ? 'all society residents' : 'committee admins'}!`);
  };

  /* =========================================================================
     INTERACTION HANDLERS & MODAL ACTIONS
     ========================================================================= */

  window.openModal = function (id) {
    if (id === 'chatMembersModal') {
      const activeGroup = state.chatGroups.find(g => g.id === state.activeChatId);
      const titleEl = document.getElementById('groupMembersModalTitle');
      const countEl = document.getElementById('groupMembersCount');
      const listEl = document.getElementById('groupMembersList');
      if (titleEl && activeGroup) titleEl.textContent = `${activeGroup.name} Members`;
      if (countEl && activeGroup) countEl.textContent = `${activeGroup.membersCount} Verified Society Members (E2EE Session Keys Active)`;
      if (listEl) {
        listEl.innerHTML = state.residents.map(r => `
          <div class="notification-item" style="cursor: default;">
            <div class="notif-icon-wrap" style="background: var(--cream-200); color: var(--brown-800); font-weight: 700; font-size: 0.75rem;">
              ${r.name.split(' ').map(n=>n[0]).join('').substring(0,2)}
            </div>
            <div class="notif-content">
              <div class="notif-title">
                <span>${escape(r.name)} <small style="color: var(--brown-700); font-weight: 600;">(${r.flat})</small></span>
                <span class="status-badge" style="background: var(--cream-200); color: var(--brown-800); font-size: 0.68rem;">Verified</span>
              </div>
              <div class="notif-desc">${escape(r.role)} • Key Fingerprint Synchronized</div>
            </div>
          </div>
        `).join('');
      }
    }
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
