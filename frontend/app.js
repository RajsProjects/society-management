/**
 * Comprehensive Frontend Application Controller
 * Color Palette: Cream, Black & Warm Brown
 */

(function () {
  'use strict';

  // ── Configuration & State ──
  const Api = window.Api;
  const Session = window.Session;
  const Data = window.Data;
  const SUPPORT_EMAIL = 'support@civiclink.in';

  // Single icon family: 24x24 grid, 2px stroke, round caps.
  const ICONS = {
    shield: '<svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M12 22s8-4 8-10V5l-8-3-8 3v7c0 6 8 10 8 10z"/></svg>',
    building: '<svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M3 21h18M5 21V5a2 2 0 0 1 2-2h6a2 2 0 0 1 2 2v16M15 21V9h4a2 2 0 0 1 2 2v10"/><path d="M9 7h2M9 11h2M9 15h2"/></svg>',
    home: '<svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M3 10.5 12 3l9 7.5"/><path d="M5 9.5V21h14V9.5"/></svg>'
  };

  let currentAuthMode = 'login'; // 'signup' | 'login'
  let activePortalRole = 'RESIDENT'; // 'RESIDENT' | 'ADMIN'
  let activeView = 'dashboard';

  // ── Workspace state ──
  // Every domain collection below is populated exclusively by the loaders
  // in this file, from the backend. The chat and notification collections are the
  // only local ones, because the backend has no message or notification API yet.
  let state = {
    user: {
      name: '',
      email: '',
      role: 'RESIDENT',
      flat: '',
      initials: ''
    },
    society: {
      name: '',
      code: '',
      totalFlats: 0,
      occupiedFlats: 0
    },

    complaints: [],
    bills: [],
    issues: [],
    announcements: [],
    residents: [],
    adminUsers: [],
    chatGroups: [],
    activeChatId: null,
    chatFilterCategory: 'ALL',
    chatSearchQuery: '',
    chatMessages: {},
    groupMembers: [],
    selectedNotificationForModal: null,
    notifications: [],
    notificationFilter: 'ALL'
  };


  // Paged metadata for each collection, populated by the loaders.
  state.complaintsPage = null;
  state.billsPage = null;
  state.issuesPage = null;
  state.announcementsPage = null;
  state.residentsPage = null;
  state.adminUsersPage = null;
  state.stats = null;
  state.myVotes = [];


  // ── DOM References ──
  const authScreen = document.getElementById('authScreen');
  const appScreen = document.getElementById('appScreen');
  const authTitle = document.getElementById('authTitle');
  const authSubtitle = document.getElementById('authSubtitle');
  const societySelectorGroup = document.getElementById('societySelectorGroup');
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
  const submitBtn = document.getElementById('submitBtn');
  const firstNameInput = document.getElementById('firstNameInput');
  const lastNameInput = document.getElementById('lastNameInput');
  const flatInput = document.getElementById('flatInput');
  const authPendingPanel = document.getElementById('authPendingPanel');
  const authForm = document.getElementById('authForm');
  const societySwitcherCard = document.getElementById('sidebarSocietyName');
  const sidebarSocietyCode = document.getElementById('sidebarSocietyCode');

  // Topbar & Sidebar
  const pageTitle = document.getElementById('pageTitle');
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
      flatGroup.style.display = 'block';
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

  window.handleForgotPassword = function (e) {
    e.preventDefault();
    const email = emailInput.value.trim();
    const subject = email
      ? `Password reset request for ${email}`
      : 'Password reset request';
    window.location.href =
      'mailto:' + SUPPORT_EMAIL + '?subject=' + encodeURIComponent(subject);
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

    if (submitBtn) submitBtn.disabled = true;
    submitBtnText.textContent = currentAuthMode === 'signup' ? 'Creating account...' : 'Authenticating...';

    try {
      if (currentAuthMode === 'signup') {
        const joinCode = joinCodeInput ? joinCodeInput.value.trim() : '';
        if (!joinCode) {
          showToast('Enter the society join code issued by your society admin.', 'error');
          return;
        }

        // The backend resolves the tenant from the join code and ignores any client
        // supplied societyId. flatId carries the apartment number - the backend maps it
        // straight through to the resident's apartmentNumber.
        const flat = flatInput ? flatInput.value.trim() : '';
        if (!flat) {
          showToast('Enter your flat or unit number.', 'error');
          return;
        }

        const payload = {
          email: email,
          password: password,
          firstName: firstNameInput ? firstNameInput.value.trim() : '',
          lastName: lastNameInput ? lastNameInput.value.trim() : '',
          flatId: flat,
          joinCode: joinCode
        };

        await Api.post('/auth/signup', payload);
        showSignupPending(email);
        return;
      }

      const data = await Api.post('/auth/login', { email: email, password: password });
      if (!data || !data.token) {
        throw new Api.ApiError('The server did not return a session token.', 500);
      }

      Session.setSession(data.token, {
        email: email,
        firstName: data.firstName,
        lastName: data.lastName,
        role: data.role,
        platformRole: data.platformRole,
        societyId: data.societyId
      });

      showToast('Signed in. Loading your workspace...', 'success');
      await bootstrapPortal();
    } catch (err) {
      showToast(describeAuthError(err), 'error');
    } finally {
      if (submitBtn) submitBtn.disabled = false;
      submitBtnText.textContent = currentAuthMode === 'signup' ? 'Create an account' : 'Sign in';
    }
  };

  function describeAuthError(err) {
    if (!err) return 'Something went wrong. Please try again.';
    if (err.status === 0) {
      return 'Cannot reach the server. Make sure the backend is running and try again.';
    }
    if (err.status === 403) {
      return err.message || 'Your account is not active yet.';
    }
    if (err.status === 409) {
      return err.message || 'That email is already registered. Try signing in instead.';
    }
    return err.message || 'Sign-in failed. Please check your details and try again.';
  }

  function showSignupPending(email) {
    if (authForm) authForm.style.display = 'none';
    if (authPendingPanel) {
      const target = authPendingPanel.querySelector('[data-pending-email]');
      if (target) target.textContent = email;
      authPendingPanel.style.display = 'flex';
    }
  }

  window.backToSignIn = function () {
    if (authPendingPanel) authPendingPanel.style.display = 'none';
    if (authForm) authForm.style.display = 'block';
    if (currentAuthMode !== 'login') toggleAuthMode();
    const emailField = document.getElementById('emailInput');
    if (emailField) emailField.focus();
  };

  /**
   * Loads the authoritative profile and society for the current token, then opens the portal.
   * The token is never trusted on its own for identity - /users/me is the source of truth.
   */
  async function bootstrapPortal() {
    let profile = Session.profile;

    try {
      const me = await Api.get('/users/me');
      if (me) {
        profile = Object.assign({}, profile || {}, me);
        Session.setProfile(profile);
      }
    } catch (err) {
      if (err && (err.status === 401 || err.status === 403)) {
        Session.clear();
        showAuthScreen('Your session is no longer valid. Please sign in again.');
        return;
      }
      // A transient network failure must not strand the user; fall through to token claims.
    }

    let society = null;
    try {
      society = await Api.get('/societies/me');
    } catch (_) {
      society = null;
    }

    enterPortal(profile, society);
  }

  function enterPortal(profile, society) {
    const p = profile || {};
    const name = [p.firstName, p.lastName].filter(Boolean).join(' ') || p.email || 'Resident';

    state.user.name = name;
    state.user.email = p.email || '';
    state.user.role = p.role || 'RESIDENT';
    state.user.platformRole = p.platformRole || 'SOCIETY_USER';
    state.user.flat = p.apartmentNumber || '';
    state.user.initials = Session.initials || 'RS';

    if (society) {
      state.society.name = society.name || '';
      state.society.code = society.joinCode || '';
      state.society.totalFlats = society.totalFlats || 0;
      state.society.occupiedFlats = society.occupiedFlats || 0;
      state.society.plan = society.plan || null;
      state.society.subscriptionStatus = society.subscriptionStatus || null;
      state.society.trialEndsAt = society.trialEndsAt || null;
      state.society.subscriptionEndsAt = society.subscriptionEndsAt || null;
    }

    const emailDisp = document.getElementById('platformAdminEmailDisplay');
    const shieldEmailDisp = document.getElementById('shieldUserEmail');
    if (emailDisp) emailDisp.textContent = state.user.email;
    if (shieldEmailDisp) shieldEmailDisp.textContent = state.user.email;

    if (societySwitcherCard) societySwitcherCard.textContent = state.society.name || 'Your society';
    if (sidebarSocietyCode) sidebarSocietyCode.textContent = state.society.code || '';

    authScreen.style.display = 'none';
    appScreen.style.display = 'flex';
    document.title = 'Samaaj | Executive Living';

    applyAuthenticatedRole(state.user.role);
  }

  function showAuthScreen(message) {
    appScreen.style.display = 'none';
    authScreen.style.display = 'flex';
    document.title = 'Samaaj | Sign in';
    if (message) showToast(message, 'error');
  }

  window.logout = function () {
    Session.clear();
    showAuthScreen();
  };

  /* =========================================================================
     WORKSPACE NAVIGATION & ROLE SIMULATOR
     ========================================================================= */

  window.navigate = function (viewId) {
    // ── Architectural Scope Guard ──
    // Platform Admin cannot access domestic society spaces (chat, complaints, bills, voting, directory)
    if (activePortalRole === 'PLATFORM_ADMIN') {
      const domesticViews = ['chat', 'complaints', 'bills', 'voting', 'directory', 'dashboard', 'announcements'];
      if (domesticViews.includes(viewId)) {
        viewId = 'tenant-shield';
        showFloatingToast('Platform scope restricted: Tenant confidentiality shield active.');
      }
    } else {
      // Resident and Society Admin cannot access platform governance views
      const platformViews = ['platform-overview', 'platform-societies', 'platform-infra', 'platform-broadcast', 'tenant-shield'];
      if (platformViews.includes(viewId)) {
        viewId = 'dashboard';
        showFloatingToast('Platform administration restricted to PLATFORM_ADMIN scope.');
      }
      // Billing and user administration are guarded by hasRole('ADMIN') on the server.
      const adminViews = ['billing', 'admin-users'];
      if (adminViews.includes(viewId) && !isTenantAdmin()) {
        viewId = 'dashboard';
        showFloatingToast('That area is restricted to society administrators.');
      }
    }

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
      chat: 'Society Community Group',
      complaints: 'Complaints & Work Orders',
      bills: 'Maintenance & Financial Accounts',
      voting: 'Community Governance & Voting',
      announcements: 'Society Notice Board',
      directory: 'Residential Directory',
      profile: 'Profile & Security',
      billing: 'Plan & Billing',
      'admin-users': 'User Management',
      'platform-overview': 'Platform Command Center',
      'platform-societies': 'Societies Registry & Verification',
      'platform-infra': 'Actuator & Observability Telemetry',
      'platform-broadcast': 'Global System Broadcaster',
      'tenant-shield': 'Zero-Knowledge Tenant Isolation Shield'
    };
    pageTitle.textContent = titles[viewId] || 'Workspace';

    // Global action button label
    const actions = {
      dashboard: 'New Request',
      chat: '+ New Message',
      complaints: '+ Lodge Complaint',
      bills: isTenantAdmin() ? '+ Generate Bill' : 'Pay Dues',
      voting: '+ Propose Issue',
      announcements: '+ Post Notice',
      directory: '+ Invite',
      profile: '',
      billing: '',
      'admin-users': '+ Invite',
      'platform-overview': '+ Onboard Society',
      'platform-societies': '+ Onboard Society',
      'platform-infra': 'Refresh Metrics',
      'platform-broadcast': 'New Broadcast',
      'tenant-shield': 'Platform Overview'
    };
    btnGlobalActionText.textContent = actions[viewId] || '+ Action';

    if (viewId === 'chat') {
      setTimeout(() => {
        const wrap = document.getElementById('chatMessagesWrap');
        if (wrap) wrap.scrollTop = wrap.scrollHeight;
      }, 40);
    }

    if (viewId === 'profile') renderProfile();
    if (viewId === 'billing') {
      renderBillingSummary();
      renderPlanGrid();
    }
    if (viewId === 'admin-users') {
      window.reloadAdminUsers(0);
    }

    syncUrlToView(viewId);
  };

  /**
   * Keeps the address bar in step with the active view so links are shareable and the
   * browser back button behaves like a real application.
   */
  function syncUrlToView(viewId) {
    if (!window.history || !window.history.pushState) return;
    const target = '#/' + viewId;
    if (window.location.hash === target) return;
    try {
      window.history.pushState({ view: viewId }, '', target);
    } catch (_) {
      window.location.hash = target;
    }
  }

  window.addEventListener('popstate', function (event) {
    const fromState = event.state && event.state.view;
    const fromHash = (window.location.hash || '').replace(/^#\/?/, '');
    const target = fromState || fromHash;
    if (target && document.getElementById('view-' + target)) {
      window.navigate(target);
    }
  });

  function applyAuthenticatedRole(role) {
    const isPlatform = Session.isPlatformAdmin();
    activePortalRole = isPlatform ? 'PLATFORM_ADMIN' : (role || 'RESIDENT');

    const topbarRoleIcon = document.getElementById('topbarRoleIcon');
    const topbarRoleTitle = document.getElementById('topbarRoleTitle');
    const topbarRoleSubtitle = document.getElementById('topbarRoleSubtitle');
    const navDomesticGroup = document.getElementById('navDomesticGroup');
    const navPlatformGroup = document.getElementById('navPlatformGroup');

    if (isPlatform) {
      if (topbarRoleIcon) topbarRoleIcon.innerHTML = ICONS.shield;
      if (topbarRoleTitle) topbarRoleTitle.textContent = 'Platform Administrator';
      if (topbarRoleSubtitle) topbarRoleSubtitle.textContent = 'Global Operator (Zero Domestic Access)';

      if (navDomesticGroup) navDomesticGroup.style.display = 'none';
      if (navPlatformGroup) navPlatformGroup.style.display = 'block';

      sidebarUserName.textContent = state.user.name;
      sidebarUserRole.textContent = 'Platform Admin (Global Operator)';
      sidebarUserInitials.textContent = state.user.initials;

      if (btnAdminCreateBill) btnAdminCreateBill.style.display = 'none';
      if (btnAdminPostNotice) btnAdminPostNotice.style.display = 'none';
      if (btnAdminInvite) btnAdminInvite.style.display = 'none';

      const adminBroadcastBar = document.getElementById('adminNotifBroadcastBar');
      if (adminBroadcastBar) adminBroadcastBar.style.display = 'none';

      navigate('platform-overview');
      renderPlatformSocieties();
      loadPlatformSocieties();
      fetchLivePlatformMetrics();
      return;
    }

    // Domestic scope: resident, society admin, accountant or security.
    if (navDomesticGroup) navDomesticGroup.style.display = 'block';
    if (navPlatformGroup) navPlatformGroup.style.display = 'none';

    const isAdmin = role === 'ADMIN' || role === 'SUPER_ADMIN';
    const isAccountant = role === 'ACCOUNTANT';
    const societyLabel = state.society.name || 'Society Management';

    if (topbarRoleIcon) topbarRoleIcon.innerHTML = isAdmin ? ICONS.building : ICONS.home;
    if (topbarRoleTitle) {
      topbarRoleTitle.textContent = isAdmin ? 'Society Management' : (isAccountant ? 'Accountant' : 'Resident');
    }
    if (topbarRoleSubtitle) {
      topbarRoleSubtitle.textContent = isAdmin
        ? societyLabel
        : (state.user.flat ? 'Unit ' + state.user.flat : societyLabel);
    }

    sidebarUserName.textContent = state.user.name;
    sidebarUserRole.textContent = isAdmin
      ? 'Society Admin (Management Desk)'
      : (state.user.flat ? 'Resident (' + state.user.flat + ')' : 'Resident');
    sidebarUserInitials.textContent = state.user.initials;

    if (btnAdminCreateBill) btnAdminCreateBill.style.display = isAdmin ? 'inline-flex' : 'none';
    if (btnAdminPostNotice) btnAdminPostNotice.style.display = isAdmin ? 'inline-flex' : 'none';
    if (btnAdminInvite) btnAdminInvite.style.display = isAdmin ? 'inline-flex' : 'none';

    const navAdminUsers = document.getElementById('navAdminUsers');
    const navBilling = document.getElementById('navBilling');
    if (navAdminUsers) navAdminUsers.style.display = isAdmin ? 'flex' : 'none';
    if (navBilling) navBilling.style.display = isAdmin ? 'flex' : 'none';

    const adminBroadcastBar = document.getElementById('adminNotifBroadcastBar');
    if (adminBroadcastBar) {
      adminBroadcastBar.style.display = isAdmin ? 'block' : 'none';
    }

    navigate('dashboard');
    loadWorkspace();
  }

  window.switchPortalRole = applyAuthenticatedRole;

  /** Tenant admin = the roles the backend guards with hasRole('ADMIN'). */
  function isTenantAdmin() {
    return activePortalRole === 'ADMIN' || activePortalRole === 'SUPER_ADMIN';
  }

  /** Roles the backend allows to verify or reject incoming payments. */
  function canVerifyPayments() {
    return isTenantAdmin() || activePortalRole === 'ACCOUNTANT';
  }

  window.handleGlobalAction = function () {
    if (activeView === 'chat') {
      const input = document.getElementById('chatMessageInput');
      if (input) input.focus();
    }
    else if (activeView === 'complaints') openModal('complaintModal');
    else if (activeView === 'bills') isTenantAdmin() ? openModal('billModal') : openPayModal(state.bills[0]);
    else if (activeView === 'voting') openModal('issueModal');
    else if (activeView === 'announcements') openModal('announcementModal');
    else if (activeView === 'platform-overview' || activeView === 'platform-societies') openModal('onboardSocietyModal');
    else if (activeView === 'platform-infra') fetchLivePlatformMetrics();
    else if (activeView === 'platform-broadcast') {
      const input = document.getElementById('platformBroadcastHeadline');
      if (input) input.focus();
    }
    else if (activeView === 'tenant-shield') navigate('platform-overview');
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
    renderPlatformSocieties();
  }

  /* -------------------------------------------------------------------------
     LIVE DATA LOADERS
     Each list endpoint returns PagedResponse<T>. Loaders are independent so one
     failing endpoint never blanks the whole workspace.
     ------------------------------------------------------------------------- */

  async function loadComplaints(page) {
    const filter = document.getElementById('complaintsFilter');
    const status = filter && filter.value && filter.value !== 'ALL' ? filter.value : undefined;
    const result = await Data.loadComplaints({ page: page || 0, status: status });
    state.complaints = result.items;
    state.complaintsPage = result;
    return result;
  }

  async function loadBills(page) {
    const filter = document.getElementById('billsFilter');
    const status = filter && filter.value && filter.value !== 'ALL' ? filter.value : undefined;
    const result = await Data.loadBills({ page: page || 0, status: status });
    state.bills = result.items;
    state.billsPage = result;
    return result;
  }

  async function loadIssues(page) {
    const result = await Data.loadIssues({ page: page || 0 });
    state.issues = result.items;
    state.issuesPage = result;
    return result;
  }

  async function loadAnnouncements(page) {
    const result = await Data.loadAnnouncements({ page: page || 0 });
    state.announcements = result.items;
    state.announcementsPage = result;
    return result;
  }

  async function loadResidents(page) {
    const result = await Data.loadResidents({ page: page || 0 });
    state.residents = result.items;
    state.residentsPage = result;
    return result;
  }

  async function loadDashboardStats() {
    const stats = await Data.loadDashboardStats();
    state.stats = stats;
    return stats;
  }

  /**
   * The backend exposes exactly one official group per society, with its member roster.
   * There is no message API yet, so the conversation pane stays empty on purpose.
   */
  async function loadMyGroup() {
    try {
      const group = await Api.get('/groups/my-group');
      if (!group) return null;

      state.chatGroups = [{
        id: group.id,
        name: group.name || 'Society Community',
        avatar: (group.name || 'S').charAt(0).toUpperCase(),
        avatarClass: '',
        category: 'COMMUNITY',
        membersCount: group.memberCount || 0,
        unread: 0,
        lastSender: '',
        lastMsg: group.description || 'Official community channel',
        lastTime: '',
        pinned: '',
        verified: true
      }];
      state.activeChatId = group.id;
      state.groupMembers = group.members || [];
      return state.chatGroups[0];
    } catch (err) {
      state.chatGroups = [];
      state.groupMembers = [];
      return null;
    }
  }

  /**
   * Loads every live collection the workspace renders.
   */
  async function loadWorkspace() {
    showSkeleton('dashboardMetrics', true);

    const tasks = [
      loadComplaints(0).catch(() => null),
      loadBills(0).catch(() => null),
      loadIssues(0).catch(() => null),
      loadAnnouncements(0).catch(() => null),
      loadMyGroup().catch(() => null)
    ];

    if (isTenantAdmin()) {
      tasks.push(loadResidents(0).catch(() => null));
      tasks.push(loadDashboardStats().catch(() => null));
    }

    const settled = await Promise.allSettled(tasks);
    const failed = settled.filter(function (r) { return r.status === 'rejected'; }).length;

    showSkeleton('dashboardMetrics', false);
    renderAllViews();

    if (failed > 0) {
      showFloatingToast('Some data could not be loaded. Check your connection and retry.');
    }
  }

  /** Paints a shimmering placeholder over a container while data is in flight. */
  function showSkeleton(containerId, visible) {
    const el = document.getElementById(containerId);
    if (!el) return;
    if (visible) {
      el.classList.add('is-loading');
      if (!el.querySelector('.skeleton-overlay')) {
        const overlay = document.createElement('div');
        overlay.className = 'skeleton-overlay';
        overlay.innerHTML = '<span class="skeleton-line"></span><span class="skeleton-line"></span><span class="skeleton-line"></span>';
        el.appendChild(overlay);
      }
    } else {
      el.classList.remove('is-loading');
      const overlay = el.querySelector('.skeleton-overlay');
      if (overlay) overlay.remove();
    }
  }

  /** Renders a pagination footer for a PagedResponse-backed list. */
  function renderPagination(containerId, page, totalPages, total, label, onPage) {
    const host = document.getElementById(containerId);
    if (!host) return;
    if (!total) {
      host.innerHTML = '';
      return;
    }
    const from = page * 1 + 1;
    const size = totalPages > 0 ? Math.ceil(total / totalPages) : total;
    const shown = Math.min(total, (page + 1) * size);
    host.innerHTML = `
      <div class="pagination-bar">
        <span class="pagination-summary">Showing ${from}-${shown} of ${total} ${label}</span>
        <div class="pagination-controls">
          <button type="button" class="btn-cancel" ${page <= 0 ? 'disabled' : ''} data-page="${page - 1}">Previous</button>
          <span class="pagination-page">Page ${page + 1} of ${Math.max(totalPages, 1)}</span>
          <button type="button" class="btn-cancel" ${page + 1 >= totalPages ? 'disabled' : ''} data-page="${page + 1}">Next</button>
        </div>
      </div>`;
    host.querySelectorAll('[data-page]').forEach(function (btn) {
      btn.addEventListener('click', function () {
        const next = parseInt(btn.getAttribute('data-page'), 10);
        if (!isNaN(next)) onPage(next);
      });
    });
  }

  // 1. Dashboard View
  function renderDashboard() {
    const tableBody = document.getElementById('dashboardComplaintsBody');
    const stats = state.stats;

    const setMetric = function (valueId, labelId, value, label) {
      const v = document.getElementById(valueId);
      const l = document.getElementById(labelId);
      if (v) v.textContent = value;
      if (l) l.textContent = label;
    };

    if (stats) {
      setMetric('statFlats', 'statFlatsLabel',
        stats.occupiedFlats + ' / ' + stats.totalFlats, 'Occupied Flats');
      setMetric('statCollection', 'statCollectionLabel',
        stats.paidBills + ' / ' + stats.totalBills, 'Bills Settled');
      setMetric('statComplaints', 'statComplaintsLabel',
        (stats.openComplaints + stats.inProgressComplaints) + ' Active', 'Complaints & Work Orders');
      setMetric('statProposals', 'statProposalsLabel',
        stats.openIssues + ' Live', 'Community Voting Items');
    } else {
      // Resident scope: derive only from records the resident is allowed to see.
      const mine = state.complaints;
      const myOpen = mine.filter(function (c) {
        return c.status === 'OPEN' || c.status === 'IN_PROGRESS';
      }).length;
      const myDue = state.bills.filter(function (b) {
        return b.status === 'PENDING' || b.status === 'OVERDUE' || b.status === 'PENDING_VERIFICATION';
      }).length;
      const awaiting = state.bills.filter(function (b) {
        return b.status === 'PENDING_VERIFICATION';
      }).length;

      setMetric('statFlats', 'statFlatsLabel', String(myOpen), 'My Open Complaints');
      setMetric('statCollection', 'statCollectionLabel', String(myDue),
        awaiting ? 'My Dues (awaiting verification)' : 'My Pending Dues');
      setMetric('statComplaints', 'statComplaintsLabel', String(mine.length), 'My Complaints');
      setMetric('statProposals', 'statProposalsLabel', String(state.issues.length) + ' Live',
        'Community Voting Items');
    }

    if (!tableBody) return;

    if (!state.complaints.length) {
      tableBody.innerHTML = '<tr><td colspan="5"><div class="empty-state"><h4>No complaints yet</h4><p>Tickets raised by residents will appear here.</p></div></td></tr>';
      return;
    }

    tableBody.innerHTML = state.complaints.slice(0, 4).map(c => `
      <tr>
        <td><strong>${escape(c.title)}</strong><br><small style="color: var(--text-muted)">#${c.id}</small></td>
        <td><span class="status-badge" style="background: var(--cream-200); color: var(--brown-700);">${escape(Data.titleCase(c.category))}</span></td>
        <td>${escape(c.reportedBy)}</td>
        <td><span class="status-badge ${badgeClass(c.status)}">${Data.titleCase(c.status)}</span></td>
        <td>${c.date}</td>
      </tr>
    `).join('');
  }

  // 2. Complaints Table with Search & Filter
  window.renderComplaintsTable = function () {
    const tableBody = document.getElementById('complaintsTableBody');
    if (!tableBody) return;

    const query = (document.getElementById('complaintsSearch')?.value || '').toLowerCase().trim();

    // The status filter is applied server side; the text search narrows the loaded page.
    const list = state.complaints.filter(c => {
      if (!query) return true;
      return c.title.toLowerCase().includes(query) ||
        String(c.id).toLowerCase().includes(query) ||
        String(c.reportedBy).toLowerCase().includes(query) ||
        String(c.category).toLowerCase().includes(query);
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
      renderPagination('complaintsPagination', 0, 0, 0, 'tickets', function () {});
      return;
    }

    tableBody.innerHTML = list.map(c => `
      <tr>
        <td>
          <div style="font-weight: 600; color: var(--black-900);">${escape(c.title)}</div>
          <small style="color: var(--text-muted); font-size: 0.75rem;">Ticket #${escape(c.id)}</small>
        </td>
        <td><span class="status-badge" style="background: var(--cream-200); color: var(--brown-700);">${escape(Data.titleCase(c.category))}</span></td>
        <td>${escape(c.reportedBy)}</td>
        <td><span class="status-badge ${badgeClass(c.status)}">${escape(Data.titleCase(c.status))}</span></td>
        <td>${c.date}</td>
        <td>
          ${isTenantAdmin() ? `
            <select class="filter-select" style="padding: 4px 8px; font-size: 0.76rem;" onchange="updateComplaintStatus('${c.id}', this.value)">
              <option value="OPEN" ${c.status === 'OPEN' ? 'selected' : ''}>Open</option>
              <option value="IN_PROGRESS" ${c.status === 'IN_PROGRESS' ? 'selected' : ''}>In Progress</option>
              <option value="RESOLVED" ${c.status === 'RESOLVED' ? 'selected' : ''}>Resolved</option>
              <option value="REJECTED" ${c.status === 'REJECTED' ? 'selected' : ''}>Rejected</option>
            </select>
          ` : `
            <button class="btn-cancel" style="padding: 4px 10px; font-size: 0.76rem;" onclick="viewComplaintDetail('${c.id}')">View</button>
          `}
        </td>
      </tr>
    `).join('');

    const page = state.complaintsPage || { page: 0, totalPages: 0, total: 0 };
    renderPagination('complaintsPagination', page.page, page.totalPages, page.total, 'tickets', reloadComplaints);
  };

  window.reloadComplaints = function (page) {
    loadComplaints(page || 0)
      .then(renderComplaintsTable)
      .catch(function (err) {
        showErrorToast(err, 'Could not load complaints: ');
      });
  };

  // 3. Maintenance Bills with Search & Filter
  window.renderBillsTable = function () {
    const tableBody = document.getElementById('billsTableBody');
    if (!tableBody) return;

    const query = (document.getElementById('billsSearch')?.value || '').toLowerCase().trim();

    const list = state.bills.filter(b => {
      if (!query) return true;
      return String(b.id).toLowerCase().includes(query) ||
        String(b.unit).toLowerCase().includes(query) ||
        String(b.month).toLowerCase().includes(query);
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
      renderPagination('billsPagination', 0, 0, 0, 'invoices', function () {});
      return;
    }

    tableBody.innerHTML = list.map(b => `
      <tr>
        <td><strong>${escape(b.id)}</strong><br><small style="color: var(--text-muted)">${escape(b.unit)}</small></td>
        <td>${escape(b.month)}</td>
        <td style="font-weight: 700; color: var(--brown-800); font-size: 0.95rem;">₹ ${b.amount.toLocaleString('en-IN')}</td>
        <td>${b.dueDate}</td>
        <td><span class="status-badge ${badgeClass(b.status)}">${escape(Data.titleCase(b.status))}</span></td>
        <td>
          ${b.status === 'PENDING' && !isTenantAdmin() && !canVerifyPayments() ? `
            <button class="btn-primary-action" style="padding: 5px 12px; font-size: 0.78rem;" onclick="openPayModal('${b.id}')">
              Pay Online
            </button>
          ` : b.status === 'PENDING_VERIFICATION' && canVerifyPayments() ? `
            <button class="btn-primary-action" style="padding: 5px 12px; font-size: 0.78rem;" onclick="verifyBillPayment('${b.id}', true)">
              Verify
            </button>
            <button class="btn-cancel" style="padding: 5px 12px; font-size: 0.78rem; margin-left: 6px;" onclick="verifyBillPayment('${b.id}', false)">
              Reject
            </button>
          ` : b.status === 'PAID' ? `
            <button class="btn-cancel" style="padding: 5px 12px; font-size: 0.78rem;" onclick="viewBillDetail('${b.id}')">
              View receipt
            </button>
          ` : `
            <span style="color: var(--text-light); font-size: 0.76rem;">—</span>
          `}
        </td>
      </tr>
    `).join('');

    const page = state.billsPage || { page: 0, totalPages: 0, total: 0 };
    renderPagination('billsPagination', page.page, page.totalPages, page.total, 'invoices', reloadBills);
  };

  window.reloadBills = function (page) {
    loadBills(page || 0)
      .then(renderBillsTable)
      .catch(function (err) {
        showErrorToast(err, 'Could not load invoices: ');
      });
  };

  // 4. Community Voting Proposals
  function renderIssues() {
    const container = document.getElementById('issuesContainer');
    if (!container) return;

    if (!state.issues.length) {
      container.innerHTML = '<div class="empty-state" style="grid-column: 1 / -1;"><h4>No open proposals</h4><p>Proposals raised by residents will appear here for voting.</p></div>';
      return;
    }

    const target = Math.max(state.society.totalFlats || 0, state.issues.length, 1);

    container.innerHTML = state.issues.map(issue => {
      const pct = Math.min(100, Math.round((issue.votes / target) * 100));
      const hasVoted = state.myVotes.indexOf(issue.id) !== -1;
      return `
        <div class="issue-card">
          <div class="issue-header">
            <h3 class="issue-title">${escape(issue.title)}</h3>
            <span class="status-badge ${badgeClass(issue.status)}">${escape(Data.titleCase(issue.status))}</span>
          </div>
          <p class="issue-desc">${escape(issue.description)}</p>
          <div class="voting-progress-wrap">
            <div class="progress-labels">
              <span>Resident Support</span>
              <strong>${issue.votes} vote${issue.votes === 1 ? '' : 's'} (${pct}% of homes)</strong>
            </div>
            <div class="progress-bar-bg">
              <div class="progress-bar-fill" style="width: ${pct}%;"></div>
            </div>
          </div>
          <div class="issue-footer">
            <button class="btn-vote ${hasVoted ? 'voted' : ''}" onclick="toggleVote('${issue.id}')">
              <svg width="15" height="15" viewBox="0 0 24 24" fill="${hasVoted ? 'currentColor' : 'none'}" stroke="currentColor" stroke-width="2">
                <path d="M14 9V5a3 3 0 0 0-3-3l-4 9v11h11.28a2 2 0 0 0 2-1.7l1.38-9a2 2 0 0 0-2-2.3zM7 22H4a2 2 0 0 1-2-2v-7a2 2 0 0 1 2-2h3"></path>
              </svg>
              ${hasVoted ? 'Voted' : 'Vote in Favor'}
            </button>
            <small style="color: var(--text-light); font-size: 0.74rem;">Raised by ${escape(issue.author)} · ${issue.date}</small>
          </div>
        </div>
      `;
    }).join('');
  }

  // 5. Announcements Notice Board
  function renderAnnouncements() {
    const stack = document.getElementById('announcementsStack');
    if (!stack) return;

    if (!state.announcements.length) {
      stack.innerHTML = '<div class="empty-state"><h4>No notices published</h4><p>Society announcements will appear here.</p></div>';
      return;
    }

    stack.innerHTML = state.announcements.map(a => `
      <div class="notice-card ${a.type === 'EMERGENCY' ? 'urgent' : ''}">
        <div class="notice-top">
          <span class="notice-tag">${escape(Data.titleCase(a.type))} NOTICE</span>
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
      String(r.flat).toLowerCase().includes(query) ||
      String(r.email).toLowerCase().includes(query)
    );

    if (!list.length) {
      tableBody.innerHTML = '<tr><td colspan="5"><div class="empty-state"><h4>No residents found</h4><p>Residents appear here once they are registered and approved.</p></div></td></tr>';
      renderPagination('directoryPagination', 0, 0, 0, 'residents', function () {});
      return;
    }

    tableBody.innerHTML = list.map(r => `
      <tr>
        <td><strong>${escape(r.name)}</strong></td>
        <td><span class="status-badge" style="background: var(--cream-200); color: var(--brown-800);">${escape(r.flat)}</span></td>
        <td>${escape(r.email)}</td>
        <td>${escape(Data.titleCase(r.role))}</td>
        <td><span class="status-badge ${r.status === 'ACTIVE' ? 'active' : (r.status === 'BLOCKED' ? 'overdue' : 'open')}">${escape(Data.titleCase(r.status))}</span></td>
      </tr>
    `).join('');

    const page = state.residentsPage || { page: 0, totalPages: 0, total: 0 };
    renderPagination('directoryPagination', page.page, page.totalPages, page.total, 'residents', reloadResidents);
  };

  window.reloadResidents = function (page) {
    loadResidents(page || 0)
      .then(renderDirectoryTable)
      .catch(function (err) {
        showErrorToast(err, 'Could not load residents: ');
      });
  };

  /* =========================================================================
     7. WHATSAPP-STYLE SOCIETY GROUPS CHAT ENGINE
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
          ${query
            ? 'No society groups match "' + escape(query) + '"'
            : 'Your society group will appear here once it has been created.'}
        </div>`;
      return;
    }

    listEl.innerHTML = filtered.map(g => {
      const isActive = g.id === state.activeChatId;
      return `
        <div class="chat-group-item ${isActive ? 'active' : ''}" onclick="selectChatGroup('${g.id}')">
          <div class="chat-avatar ${g.avatarClass}">
            ${g.avatar}
            <span class="online-dot" title="Verified society member channel"></span>
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
      badgeEl.textContent = totalUnread > 0 ? `${totalUnread} new` : (state.chatGroups.length ? 'Open' : '—');
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

    if (titleEl) titleEl.textContent = group.name;
    if (avatarEl) {
      avatarEl.textContent = group.avatar;
      avatarEl.className = `chat-avatar ${group.avatarClass}`;
    }
    if (membersEl) membersEl.textContent = `${group.membersCount} member${group.membersCount === 1 ? '' : 's'}`;
    if (pinnedTextEl) pinnedTextEl.textContent = group.pinned || 'Official community channel for this society.';

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
          <strong>No messages to show</strong>
          <p style="margin-top: 6px; font-size: 0.78rem; max-width: 38ch; margin-left: auto; margin-right: auto;">
            Society messaging is not connected to the backend yet. Announcements and
            maintenance notices are delivered through the notice board.
          </p>
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

  // The backend exposes no message endpoints yet, so the composer stays closed.
  const CHAT_UNAVAILABLE = 'Society messaging is not connected yet. Use the notice board for announcements.';

  window.handleSendChatMessage = function () {
    showFloatingToast(CHAT_UNAVAILABLE);
  };

  window.handleChatInputKey = function (e) {
    if (e.key === 'Enter' && !e.shiftKey) {
      e.preventDefault();
    }
  };

  // Emojis & Attachments
  window.toggleEmojiTray = function () {
    showFloatingToast(CHAT_UNAVAILABLE);
  };

  function closeEmojiTray() {
    document.getElementById('chatEmojiTray')?.classList.remove('active');
  }

  window.insertEmoji = function () {
    closeEmojiTray();
  };

  window.toggleAttachmentMenu = function () {
    showFloatingToast(CHAT_UNAVAILABLE);
  };

  function closeAttachmentMenu() {
    document.getElementById('chatAttachmentMenu')?.classList.remove('active');
  }

  window.sendAttachment = function () {
    closeAttachmentMenu();
    showFloatingToast(CHAT_UNAVAILABLE);
  };

  window.handleReactToMessage = function () {
    return;
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
      showFloatingToast(isPlaying ? 'Audio paused' : 'Playing voice note');
    }
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
    if (!item) return;

    item.unread = false;
    renderNotificationItems();

    // Close notification panel dropdown
    window.closeNotificationPanel();

    // Route or display modal based on user role and targetView
    if (activePortalRole === 'PLATFORM_ADMIN') {
      if (item.targetView && (item.targetView.startsWith('platform-') || item.type === 'SYSTEM')) {
        navigate(item.targetView.startsWith('platform-') ? item.targetView : 'platform-overview');
        showFloatingToast(`Opened Platform Telemetry: ${item.title}`);
      } else {
        openNotificationDetailModal(item);
      }
      return;
    }

    // Resident or Society Admin
    if (item.targetView && !item.targetView.startsWith('platform-')) {
      navigate(item.targetView);
      showFloatingToast(`Navigated to ${item.targetView.toUpperCase()}: ${item.title}`);
    } else {
      openNotificationDetailModal(item);
    }
  };

  window.openNotificationDetailModal = function (item) {
    state.selectedNotificationForModal = item;
    const titleEl = document.getElementById('notifDetailModalTitle');
    const tagEl = document.getElementById('notifDetailModalTag');
    const timeEl = document.getElementById('notifDetailModalTime');
    const textEl = document.getElementById('notifDetailModalText');
    const actionBtn = document.getElementById('btnNotifDetailAction');

    if (titleEl) titleEl.textContent = item.title;
    if (tagEl) tagEl.textContent = item.tag || item.type;
    if (timeEl) timeEl.textContent = item.time;
    if (textEl) textEl.textContent = item.desc;

    if (actionBtn) {
      if (item.targetView && (activePortalRole !== 'PLATFORM_ADMIN' || item.targetView.startsWith('platform-'))) {
        actionBtn.style.display = 'inline-flex';
        actionBtn.textContent = `Open ${item.targetView.toUpperCase()} ↗`;
      } else {
        actionBtn.style.display = 'none';
      }
    }

    openModal('notificationDetailModal');
  };

  window.handleNotificationDetailAction = function () {
    const item = state.selectedNotificationForModal;
    closeModal('notificationDetailModal');
    if (item && item.targetView) {
      navigate(item.targetView);
      showFloatingToast(`Opened: ${item.title}`);
    }
  };

  // ── Platform Admin: Societies Registry & Telemetry ──

  /**
   * Loads the platform-wide society registry from GET /societies (PLATFORM_ADMIN only).
   * The response is a PagedResponse<SocietyResponse>.
   */
  async function loadPlatformSocieties() {
    try {
      const page = await Api.get('/societies', { page: 0, size: 50 });
      const rows = (page && page.content) || [];
      state.platformSocieties = rows.map(function (s) {
        return {
          id: s.id,
          name: s.name,
          code: s.registrationNumber || '',
          joinCode: s.joinCode || '',
          city: s.city || '',
          state: s.state || '',
          flats: s.totalFlats || 0,
          occupiedFlats: s.occupiedFlats || 0,
          activeResidents: s.activeResidents || 0,
          status: s.status || 'PENDING_VERIFICATION',
          plan: s.plan || null,
          subscriptionStatus: s.subscriptionStatus || null
        };
      });
      renderPlatformSocieties();
    } catch (err) {
      if (err && err.status === 0) {
        showFloatingToast('Cannot reach the server. Showing the last known registry.');
      }
    }
  }

  window.renderPlatformSocieties = function () {
    const tbody = document.getElementById('platformSocietiesTableBody');
    if (!tbody) return;

    if (!state.platformSocieties.length) {
      tbody.innerHTML = '<tr><td colspan="6"><div class="empty-state"><h4>No societies registered yet</h4><p>Onboard the first society to get started.</p></div></td></tr>';
    } else {
      tbody.innerHTML = state.platformSocieties.map(s => {
        const isPending = s.status === 'PENDING_VERIFICATION';
        return `
          <tr>
            <td>
              <strong>${escape(s.name)}</strong>
              <div style="font-size: 0.74rem; color: var(--text-light); font-family: monospace;">${escape(s.code)}</div>
            </td>
            <td>${escape(s.city)}${s.state ? ', ' + escape(s.state) : ''}</td>
            <td><strong>${s.occupiedFlats}</strong> / ${s.flats} units</td>
            <td><code>${escape(s.joinCode || '—')}</code></td>
            <td>
              <span class="${isPending ? 'society-badge-pending' : 'society-badge-active'}">
                ${escape(String(s.status).replace(/_/g, ' '))}
              </span>
            </td>
            <td style="text-align: right;">
              ${isPending ? `
                <button type="button" class="btn-verify-approve" onclick="handleVerifySociety('${s.id}', true)">Approve</button>
                <button type="button" class="btn-verify-reject" onclick="handleVerifySociety('${s.id}', false)">Reject</button>
              ` : `
                <button type="button" class="btn-text-subtle" onclick="showFloatingToast('Society ${escape(s.code)} is ${escape(String(s.status).toLowerCase())}.')">Inspect</button>
              `}
            </td>
          </tr>
        `;
      }).join('');
    }

    const totalEl = document.getElementById('platformTotalSocieties');
    const activeEl = document.getElementById('platformActiveSocieties');
    const pendingEl = document.getElementById('platformPendingSocieties');

    if (totalEl) totalEl.textContent = state.platformSocieties.length;
    if (activeEl) activeEl.textContent = state.platformSocieties.filter(s => s.status === 'ACTIVE').length;
    if (pendingEl) pendingEl.textContent = state.platformSocieties.filter(s => s.status === 'PENDING_VERIFICATION').length;
  };

  window.handleVerifySociety = async function (socId, isApproved) {
    const soc = state.platformSocieties.find(s => s.id === socId);
    if (!soc) return;

    confirmAction(
      isApproved
        ? 'Approve ' + soc.name + ' (' + soc.code + ')? Residents will be able to register with its join code.'
        : 'Reject ' + soc.name + ' (' + soc.code + ')? The society will not be able to accept registrations.',
      async function () {
        try {
          await Api.patch('/societies/' + socId + '/verify', {
            status: isApproved ? 'ACTIVE' : 'REJECTED',
            rejectionReason: isApproved ? null : 'Incomplete documentation'
          });
          soc.status = isApproved ? 'ACTIVE' : 'REJECTED';
          renderPlatformSocieties();
          showFloatingToast(isApproved
            ? 'Approved society: ' + soc.name
            : 'Rejected verification for: ' + soc.name);
        } catch (err) {
          showErrorToast(err, 'Verification update failed: ');
        }
      },
      { confirmLabel: isApproved ? 'Approve society' : 'Reject society' }
    );
  };

  window.handleOnboardSocietySubmit = async function (e) {
    e.preventDefault();

    const value = function (id) {
      const el = document.getElementById(id);
      return el ? el.value.trim() : '';
    };

    const payload = {
      name: value('newSocietyName'),
      registrationNumber: value('newSocietyRegNo'),
      address: value('newSocietyAddress'),
      city: value('newSocietyCity'),
      state: value('newSocietyState'),
      pincode: value('newSocietyPincode'),
      totalFlats: parseInt(value('newSocietyFlats'), 10),
      firstName: value('newSocietyAdminFirstName'),
      lastName: value('newSocietyAdminLastName'),
      email: value('newSocietyAdminEmail'),
      phone: value('newSocietyAdminPhone'),
      password: value('newSocietyAdminPassword')
    };

    const form = new FormData();
    form.append('data', new Blob([JSON.stringify(payload)], { type: 'application/json' }));

    const fileInput = document.getElementById('newSocietyDocument');
    if (fileInput && fileInput.files && fileInput.files.length) {
      form.append('document', fileInput.files[0]);
    }

    const submitBtn = e.target.querySelector('button[type="submit"]');
    if (submitBtn) submitBtn.disabled = true;

    try {
      await Api.post('/societies/register', form);
      closeModal('onboardSocietyModal');
      showFloatingToast('Society submitted for verification: ' + payload.name);
      await loadPlatformSocieties();
    } catch (err) {
      showErrorToast(err, 'Registration failed: ');
    } finally {
      if (submitBtn) submitBtn.disabled = false;
    }
  };

  window.handleSendPlatformBroadcast = async function (e) {
    e.preventDefault();
    const cat = document.getElementById('platformBroadcastCategory')?.value;
    const headline = document.getElementById('platformBroadcastHeadline')?.value.trim();
    const version = document.getElementById('platformBroadcastVersion')?.value.trim();
    const notes = document.getElementById('platformBroadcastNotes')?.value.trim();

    const newNotif = {
      id: `NOTIF-PLAT-${Date.now()}`,
      type: 'SYSTEM',
      title: `${headline} ${version ? `(${version})` : ''}`,
      desc: notes,
      time: 'Just now',
      unread: true,
      tag: cat.replace('_', ' '),
      targetView: 'platform-overview'
    };

    state.notifications.unshift(newNotif);
    renderNotificationItems();

    try {
      await Api.post('/platform/broadcast', {
        title: headline,
        message: notes,
        version: version || 'v2.5'
      });
      showFloatingToast('Global Platform Broadcast dispatched to all society admins');
    } catch (err) {
      showErrorToast(err, 'Broadcast failed: ');
    }

    const hInput = document.getElementById('platformBroadcastHeadline');
    const nInput = document.getElementById('platformBroadcastNotes');
    if (hInput) hInput.value = '';
    if (nInput) nInput.value = '';
  };

  async function fetchLivePlatformMetrics() {
    if (!Session.isAuthenticated()) return;

    try {
      const data = await Api.get('/platform/overview');
      if (!data) return;
      const usedEl = document.getElementById('infraJvmUsed');
      const uptimeEl = document.getElementById('infraUptime');
      if (usedEl) usedEl.textContent = `${data.usedMemoryMb} MB / ${data.maxMemoryMb} MB`;
      if (uptimeEl) {
        const mins = Math.floor(data.uptimeSeconds / 60);
        const hrs = Math.floor(mins / 60);
        uptimeEl.textContent = `${hrs} hrs, ${mins % 60} mins`;
      }
    } catch (_) {
      // Telemetry is non-critical; leave the last known values on screen.
    }
  }

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
    if (id === 'billModal') {
      const select = document.getElementById('billTargetUnit');
      if (select) {
        const options = state.residents.map(function (r) {
          return '<option value="' + escape(r.flat) + '">' + escape(r.flat) + ' — ' + escape(r.name) + '</option>';
        }).join('');
        select.innerHTML = '<option value="">Select a unit</option>' + options;
        if (!state.residents.length) {
          select.insertAdjacentHTML('beforeend', '<option value="" disabled>No residents in the registry yet</option>');
        }
      }
    }
    if (id === 'chatMembersModal') {
      const activeGroup = state.chatGroups.find(g => g.id === state.activeChatId);
      const titleEl = document.getElementById('groupMembersModalTitle');
      const countEl = document.getElementById('groupMembersCount');
      const listEl = document.getElementById('groupMembersList');
      if (titleEl && activeGroup) titleEl.textContent = `${activeGroup.name} Members`;
      if (countEl && activeGroup) {
        countEl.textContent = `${activeGroup.membersCount} member${activeGroup.membersCount === 1 ? '' : 's'} in this society group`;
      }
      if (listEl) {
        const members = state.groupMembers || [];
        if (!members.length) {
          listEl.innerHTML = '<div style="padding: 24px; text-align: center; color: var(--text-muted); font-size: 0.82rem;">No members to show.</div>';
        } else {
          listEl.innerHTML = members.map(m => `
            <div class="notification-item" style="cursor: default;">
              <div class="notif-icon-wrap" style="background: var(--cream-200); color: var(--brown-800); font-weight: 700; font-size: 0.75rem;">
                ${escape(String(m.name || '?').split(' ').map(n => n[0]).join('').substring(0, 2))}
              </div>
              <div class="notif-content">
                <div class="notif-title">
                  <span>${escape(m.name || 'Resident')} ${m.flatNumber ? `<small style="color: var(--brown-700); font-weight: 600;">(${escape(m.flatNumber)})</small>` : ''}</span>
                  <span class="status-badge" style="background: var(--cream-200); color: var(--brown-800); font-size: 0.68rem;">${escape(Data.titleCase(m.role))}</span>
                </div>
                <div class="notif-desc">${escape(m.email || '')}${m.isOwner ? ' • Owner' : ''}</div>
              </div>
            </div>
          `).join('');
        }
      }
    }
    document.getElementById(id)?.classList.add('active');
  };

  window.closeModal = function (id) {
    document.getElementById(id)?.classList.remove('active');
  };

  // Create Complaint
  window.handleCreateComplaint = async function (e) {
    e.preventDefault();
    const title = document.getElementById('newComplaintTitle').value.trim();
    const category = document.getElementById('newComplaintCategory').value;
    const desc = document.getElementById('newComplaintDesc').value.trim();

    if (!title || !desc) {
      showFloatingToast('A title and description are required.');
      return;
    }

    const submitBtn = e.target.querySelector('button[type="submit"]');
    if (submitBtn) submitBtn.disabled = true;

    try {
      await Api.post('/complaints', {
        title: title,
        description: desc,
        category: category
      });
      closeModal('complaintModal');
      e.target.reset();
      await reloadComplaints(0);
      showFloatingToast('Ticket submitted. The facilities team has been notified.');
    } catch (err) {
      showErrorToast(err, 'Could not submit the complaint: ');
    } finally {
      if (submitBtn) submitBtn.disabled = false;
    }
  };

  // Complaint Status Updater (Admin)
  window.updateComplaintStatus = async function (id, newStatus) {
    if (!isTenantAdmin()) return;
    try {
      await Api.patch('/complaints/' + id + '/status', { status: newStatus });
      await reloadComplaints(state.complaintsPage ? state.complaintsPage.page : 0);
      showFloatingToast('Ticket status updated to ' + Data.titleCase(newStatus));
    } catch (err) {
      showErrorToast(err, 'Could not update the ticket: ');
      renderComplaintsTable();
    }
  };

  // View Complaint Details (Resident / Admin)
  window.viewComplaintDetail = async function (id) {
    let item = state.complaints.find(c => c.id === id);
    try {
      const fresh = await Api.get('/complaints/' + id);
      if (fresh) {
        item = {
          id: fresh.id,
          title: fresh.title,
          description: fresh.description,
          category: fresh.category,
          status: fresh.status,
          reportedBy: fresh.apartmentNumber
            ? fresh.apartmentNumber + (fresh.residentName ? ' (' + fresh.residentName + ')' : '')
            : (fresh.residentName || 'Management'),
          date: Data.formatDate(fresh.createdAt),
          adminNote: fresh.adminNote || ''
        };
      }
    } catch (err) {
      if (!item) {
        showErrorToast(err, 'Could not load the ticket: ');
        return;
      }
    }
    if (!item) return;

    document.getElementById('detailTicketId').textContent = `Ticket #${item.id}`;
    document.getElementById('detailTicketTitle').textContent = item.title;
    document.getElementById('detailTicketCategory').textContent = Data.titleCase(item.category);

    const statusBadge = document.getElementById('detailTicketStatus');
    statusBadge.textContent = Data.titleCase(item.status);
    statusBadge.className = `status-badge ${badgeClass(item.status)}`;

    document.getElementById('detailTicketDate').textContent = item.date;
    document.getElementById('detailTicketReporter').textContent = item.reportedBy;
    document.getElementById('detailTicketDesc').textContent = item.description || 'No description provided.';

    openModal('complaintDetailModal');
  };

  // Open Pay Bill Modal
  let activePaymentBill = null;

  window.openPayModal = function (billId) {
    const bill = typeof billId === 'string' ? state.bills.find(b => b.id === billId) : billId;
    if (!bill) return;
    activePaymentBill = bill;
    document.getElementById('payModalMonth').textContent = bill.month;
    document.getElementById('payModalAmount').textContent = `₹ ${bill.amount.toLocaleString('en-IN')}`;
    const txnField = document.getElementById('payTransactionId');
    if (txnField) txnField.value = '';
    openModal('payBillModal');
  };

  // Execute Payment
  window.handleExecutePayment = async function (e) {
    e.preventDefault();
    const txnField = document.getElementById('payTransactionId');
    const txn = txnField ? txnField.value.trim() : '';

    if (!activePaymentBill) {
      showFloatingToast('No invoice selected for payment.');
      return;
    }
    if (!/^[a-zA-Z0-9_-]{10,50}$/.test(txn)) {
      showFloatingToast('Enter the UPI transaction reference (10-50 letters, digits, - or _).');
      return;
    }

    const submitBtn = e.target.querySelector('button[type="submit"]');
    if (submitBtn) submitBtn.disabled = true;

    try {
      await Api.post('/finance/bills/' + activePaymentBill.id + '/pay', {
        upiTransactionId: txn,
        amount: activePaymentBill.amount
      });
      closeModal('payBillModal');
      await reloadBills(state.billsPage ? state.billsPage.page : 0);
      showFloatingToast('Payment submitted. It is awaiting verification by the committee.');
    } catch (err) {
      showErrorToast(err, 'Payment could not be submitted: ');
    } finally {
      if (submitBtn) submitBtn.disabled = false;
    }
  };

  /** Admin / accountant action on a bill whose payment reference is pending review. */
  window.verifyBillPayment = function (billId, approve) {
    confirmAction(
      approve
        ? 'Verify this payment? The invoice will be marked as PAID.'
        : 'Reject this payment? The invoice will return to PENDING and the resident will need to resubmit.',
      async function () {
        try {
          if (approve) {
            await Api.post('/finance/bills/' + billId + '/verify-payment', {});
          } else {
            await Api.post('/finance/bills/' + billId + '/reject-payment?reason=' +
              encodeURIComponent('Payment reference could not be confirmed with the bank'), {});
          }
          await reloadBills(state.billsPage ? state.billsPage.page : 0);
          showFloatingToast(approve ? 'Payment verified and invoice settled.' : 'Payment rejected.');
        } catch (err) {
          showErrorToast(err, 'Could not update the invoice');
        }
      },
      { confirmLabel: approve ? 'Verify payment' : 'Reject payment' }
    );
  };

  /** Shows the stored invoice record for a settled bill. */
  window.viewBillDetail = async function (billId) {
    try {
      const bill = await Api.get('/finance/bills/' + billId);
      if (!bill) return;
      openModal('billDetailModal');
      document.getElementById('billDetailId').textContent = bill.id;
      document.getElementById('billDetailUnit').textContent = bill.apartmentNumber || '—';
      document.getElementById('billDetailMonth').textContent = bill.billingMonth || '—';
      document.getElementById('billDetailAmount').textContent = '₹ ' + Number(bill.amount || 0).toLocaleString('en-IN');
      document.getElementById('billDetailDueDate').textContent = bill.dueDate ? Data.formatDate(bill.dueDate) : '—';
      const statusEl = document.getElementById('billDetailStatus');
      statusEl.textContent = Data.titleCase(bill.status);
      statusEl.className = 'status-badge ' + badgeClass(bill.status);
    } catch (err) {
      showErrorToast(err, 'Could not load the invoice: ');
    }
  };

  // Generate Bill (Admin)
  window.handleGenerateBill = async function (e) {
    e.preventDefault();
    const unit = document.getElementById('billTargetUnit').value.trim();
    const month = document.getElementById('billMonth').value.trim();
    const amount = Number(document.getElementById('billAmount').value);
    const dueDate = document.getElementById('billDueDate').value.trim();

    if (!/^\d{4}-\d{2}$/.test(month)) {
      showFloatingToast('Billing month must be in YYYY-MM format.');
      return;
    }
    if (!amount || amount <= 0) {
      showFloatingToast('Enter an amount greater than zero.');
      return;
    }
    if (!dueDate) {
      showFloatingToast('A due date is required.');
      return;
    }

    // The backend bills one apartment at a time and resolves the resident by user id.
    const resident = state.residents.find(function (r) {
      return unit && String(r.flat).toLowerCase() === unit.toLowerCase();
    });
    if (!resident) {
      showFloatingToast('No resident is mapped to unit ' + unit + '. Select a unit from the directory.');
      return;
    }

    const submitBtn = e.target.querySelector('button[type="submit"]');
    if (submitBtn) submitBtn.disabled = true;

    try {
      await Api.post('/finance/bills', {
        userId: resident.id,
        apartmentNumber: resident.flat,
        amount: amount,
        billingMonth: month,
        dueDate: dueDate
      });
      closeModal('billModal');
      e.target.reset();
      await reloadBills(0);
      showFloatingToast('Maintenance invoice generated for ' + resident.flat);
    } catch (err) {
      showErrorToast(err, 'Could not generate the invoice: ');
    } finally {
      if (submitBtn) submitBtn.disabled = false;
    }
  };

  // Invite / Add Resident (Admin)
  window.handleInviteResident = async function (e) {
    e.preventDefault();
    const name = document.getElementById('residentName').value.trim();
    const flat = document.getElementById('residentFlat').value.trim();
    const email = document.getElementById('residentEmail').value.trim();
    const phone = document.getElementById('residentPhone').value.trim();
    const role = document.getElementById('residentRole').value;

    if (!email) {
      showFloatingToast('An email address is required to send the invitation.');
      return;
    }

    const submitBtn = e.target.querySelector('button[type="submit"]');
    if (submitBtn) submitBtn.disabled = true;

    try {
      await Api.post('/auth/invite', {
        email: email,
        role: role,
        flatId: flat || null
      });
      closeModal('inviteModal');
      e.target.reset();
      await reloadResidents(0);
      showFloatingToast('Invitation sent to ' + email);
    } catch (err) {
      showErrorToast(err, 'Invitation failed: ');
    } finally {
      if (submitBtn) submitBtn.disabled = false;
    }
  };

  // Switch Society Modal & Actions
  window.switchSocietyModal = function () {
    openModal('societyModal');
  };

  /**
   * The backend scopes every user to exactly one society, so a "switch" is really a
   * lookup of a join code. The response tells us which society the code belongs to.
   */
  window.handleSwitchSociety = async function (code, name) {
    await lookupJoinCode(code, name);
  };

  window.handleJoinCustomSociety = async function () {
    const input = document.getElementById('joinSocietyCodeInput');
    const val = (input?.value || '').trim().toUpperCase();
    if (!val) {
      showFloatingToast('Enter the society join code issued by your admin.');
      return;
    }
    const ok = await lookupJoinCode(val, null);
    if (ok && input) input.value = '';
  };

  async function lookupJoinCode(code, fallbackName) {
    try {
      const society = await Api.get('/societies/join/' + encodeURIComponent(code));
      if (!society) return false;

      const label = society.name || fallbackName || code;
      state.society.code = society.joinCode || code;
      state.society.name = label;

      if (societySwitcherCard) societySwitcherCard.textContent = label;
      if (sidebarSocietyCode) sidebarSocietyCode.textContent = state.society.code;

      closeModal('societyModal');
      showFloatingToast('Society confirmed: ' + label);
      return true;
    } catch (err) {
      const message = err && err.status === 404
        ? 'No society is registered with that join code.'
        : 'Could not verify the join code: ' + (err && err.message ? err.message : 'request failed');
      showFloatingToast(message);
      return false;
    }
  }

  // Propose Issue
  window.handleCreateIssue = async function (e) {
    e.preventDefault();
    const title = document.getElementById('newIssueTitle').value.trim();
    const desc = document.getElementById('newIssueDesc').value.trim();

    if (!title || !desc) {
      showFloatingToast('A title and description are required.');
      return;
    }

    const submitBtn = e.target.querySelector('button[type="submit"]');
    if (submitBtn) submitBtn.disabled = true;

    try {
      await Api.post('/issues', { title: title, description: desc, photoUrl: null });
      closeModal('issueModal');
      e.target.reset();
      await reloadIssues(0);
      showFloatingToast('Proposal published for community voting.');
    } catch (err) {
      showErrorToast(err, 'Could not publish the proposal: ');
    } finally {
      if (submitBtn) submitBtn.disabled = false;
    }
  };

  // Vote Toggle
  window.toggleVote = async function (id) {
    const alreadyVoted = state.myVotes.indexOf(id) !== -1;
    try {
      if (alreadyVoted) {
        await Api.del('/issues/' + id + '/votes');
        state.myVotes = state.myVotes.filter(function (v) { return v !== id; });
        showFloatingToast('Vote withdrawn');
      } else {
        await Api.post('/issues/' + id + '/votes', {});
        state.myVotes.push(id);
        showFloatingToast('Vote recorded in favor.');
      }
      await reloadIssues(state.issuesPage ? state.issuesPage.page : 0);
    } catch (err) {
      showErrorToast(err, 'Could not record your vote: ');
    }
  };

  // Create Announcement
  window.handleCreateAnnouncement = async function (e) {
    e.preventDefault();
    const title = document.getElementById('newNoticeTitle').value.trim();
    const content = document.getElementById('newNoticeContent').value.trim();
    const type = document.getElementById('newNoticeType').value;

    if (!title || !content) {
      showFloatingToast('A title and message are required.');
      return;
    }

    const submitBtn = e.target.querySelector('button[type="submit"]');
    if (submitBtn) submitBtn.disabled = true;

    try {
      await Api.post('/announcements', { title: title, content: content, type: type });
      closeModal('announcementModal');
      e.target.reset();
      await reloadAnnouncements(0);
      showFloatingToast('Notice published to all residents.');
    } catch (err) {
      showErrorToast(err, 'Could not publish the notice: ');
    } finally {
      if (submitBtn) submitBtn.disabled = false;
    }
  };

  window.reloadAnnouncements = function (page) {
    loadAnnouncements(page || 0)
      .then(renderAnnouncements)
      .catch(function (err) {
        showErrorToast(err, 'Could not load notices: ');
      });
  };

  window.reloadIssues = function (page) {
    loadIssues(page || 0)
      .then(renderIssues)
      .catch(function (err) {
        showErrorToast(err, 'Could not load proposals: ');
      });
  };

  /**
   * Shared confirmation dialog for destructive or irreversible actions.
   */
  window.confirmAction = function (message, onConfirm, options) {
    const opts = options || {};
    const modal = document.getElementById('confirmModal');
    const messageEl = document.getElementById('confirmMessage');
    const actionBtn = document.getElementById('confirmActionBtn');
    if (!modal || !messageEl || !actionBtn) {
      if (onConfirm) onConfirm();
      return;
    }
    messageEl.textContent = message;
    actionBtn.textContent = opts.confirmLabel || 'Confirm';
    actionBtn.className = opts.danger === false ? 'btn-primary-action' : 'btn-danger';
    actionBtn.onclick = function () {
      closeModal('confirmModal');
      if (onConfirm) onConfirm();
    };
    openModal('confirmModal');
  };

  /* =========================================================================
     9. SAAS SURFACES: PROFILE & SECURITY, PLAN & BILLING, USER MANAGEMENT
     ========================================================================= */

  const PLAN_CATALOG = [
    { key: 'TRIAL', name: 'Trial', price: 'Free', period: 'during the trial window', capacity: 'Full access while you evaluate Samaaj', selfServe: false },
    { key: 'BASIC', name: 'Basic', price: '₹999', period: 'per month', capacity: 'Up to 50 flats', selfServe: true },
    { key: 'GROWTH', name: 'Growth', price: '₹2,499', period: 'per month', capacity: 'Up to 200 flats', selfServe: true },
    { key: 'SCALE', name: 'Scale', price: '₹4,999', period: 'per month', capacity: 'Up to 500 flats', selfServe: true },
    { key: 'ENTERPRISE', name: 'Enterprise', price: 'Custom', period: '', capacity: 'Unlimited flats with a dedicated account manager', selfServe: false }
  ];

  /* -------------------------------------------------------------------------
     PROFILE & SECURITY
     ------------------------------------------------------------------------- */

  function renderProfile() {
    const profile = Session.profile || {};
    const name = state.user.name || Session.fullName || 'Your account';

    setText('profileInitials', Session.initials || '—');
    setText('profileDisplayName', name);
    setText('profileDisplayMeta', [state.society.name, state.user.flat ? 'Unit ' + state.user.flat : ''].filter(Boolean).join(' • ') || '—');
    setText('profileEmail', state.user.email || profile.email || '—');
    setText('profileRole', Data.titleCase(state.user.role) || '—');
    setText('profilePlatformRole', Data.titleCase(state.user.platformRole) || '—');
    setText('profileFlat', state.user.flat || '—');
    setText('profileSociety', state.society.name || '—');
    setText('profileJoined', profile.createdAt ? Data.formatDate(profile.createdAt) : '—');
  }

  function setText(id, value) {
    const el = document.getElementById(id);
    if (el) el.textContent = value === null || value === undefined || value === '' ? '—' : value;
  }

  window.updatePasswordStrength = function () {
    const input = document.getElementById('newPasswordInput');
    const bar = document.getElementById('passwordStrengthBar');
    const label = document.getElementById('passwordStrengthLabel');
    const value = input ? input.value : '';
    if (!bar || !label) return;

    let score = 0;
    if (value.length >= 8) score++;
    if (/[A-Z]/.test(value)) score++;
    if (/[a-z]/.test(value)) score++;
    if (/\d/.test(value)) score++;

    bar.className = 'password-strength-bar';
    if (!value) {
      label.textContent = 'Enter a new password to check its strength.';
      return;
    }
    if (score <= 2) {
      bar.classList.add('weak');
      label.textContent = 'Weak — add uppercase, lowercase and a number, 8 characters minimum.';
    } else if (score === 3) {
      bar.classList.add('fair');
      label.textContent = 'Fair — one more rule will make this strong.';
    } else {
      bar.classList.add('strong');
      label.textContent = 'Strong — meets the password policy.';
    }
  };

  window.resetPasswordForm = function () {
    ['currentPasswordInput', 'newPasswordInput', 'confirmPasswordInput'].forEach(function (id) {
      const el = document.getElementById(id);
      if (el) el.value = '';
    });
    const matchLabel = document.getElementById('passwordMatchLabel');
    if (matchLabel) matchLabel.textContent = '';
    window.updatePasswordStrength();
  };

  window.handleChangePassword = async function (e) {
    e.preventDefault();

    const current = document.getElementById('currentPasswordInput').value;
    const next = document.getElementById('newPasswordInput').value;
    const confirmValue = document.getElementById('confirmPasswordInput').value;
    const matchLabel = document.getElementById('passwordMatchLabel');
    const btn = document.getElementById('changePasswordBtn');

    if (next !== confirmValue) {
      if (matchLabel) matchLabel.textContent = 'The two new passwords do not match.';
      return;
    }
    if (matchLabel) matchLabel.textContent = '';

    if (btn) btn.disabled = true;
    try {
      await Api.post('/users/change-password', {
        currentPassword: current,
        newPassword: next
      });
      window.resetPasswordForm();
      showFloatingToast('Password updated successfully.');
    } catch (err) {
      showErrorToast(err, 'Could not change the password: ');
    } finally {
      if (btn) btn.disabled = false;
    }
  };

  /* -------------------------------------------------------------------------
     PLAN & BILLING
     ------------------------------------------------------------------------- */

  function renderBillingSummary() {
    setText('billingSocietyName', state.society.name || 'your society');
    setText('billingCurrentPlan', Data.titleCase(state.society.plan) || '—');
    setText('billingStatus', Data.titleCase(state.society.subscriptionStatus) || '—');

    const isTrial = state.society.subscriptionStatus === 'TRIAL';
    setText('billingExpiryLabel', isTrial ? 'Trial ends on' : 'Renews on');

    const expiry = isTrial ? state.society.trialEndsAt : state.society.subscriptionEndsAt;
    setText('billingExpiry', expiry ? Data.formatDate(expiry) : '—');
  }

  function renderPlanGrid() {
    const grid = document.getElementById('planGrid');
    if (!grid) return;
    const current = state.society.plan;

    grid.innerHTML = PLAN_CATALOG.map(function (plan) {
      const isCurrent = plan.key === current;
      const label = isCurrent
        ? 'Current plan'
        : (plan.selfServe ? 'Switch to ' + plan.name : 'Contact sales');
      return `
        <div class="plan-card ${isCurrent ? 'current' : ''}">
          <div class="plan-card-head">
            <h4>${escape(plan.name)}</h4>
            <span class="plan-price">${escape(plan.price)} ${plan.period ? `<small>${escape(plan.period)}</small>` : ''}</span>
          </div>
          <p class="plan-capacity">${escape(plan.capacity)}</p>
          <button type="button" class="${isCurrent ? 'btn-cancel' : 'btn-primary-action'}" ${isCurrent || !plan.selfServe ? 'disabled' : ''} onclick="handleChangePlan('${plan.key}')">
            ${escape(label)}
          </button>
        </div>
      `;
    }).join('');
  }

  window.refreshBilling = async function () {
    renderBillingSummary();
    renderPlanGrid();
    try {
      const society = await Api.get('/societies/me');
      if (society) {
        state.society.plan = society.plan;
        state.society.subscriptionStatus = society.subscriptionStatus;
        state.society.trialEndsAt = society.trialEndsAt;
        state.society.subscriptionEndsAt = society.subscriptionEndsAt;
        renderBillingSummary();
        renderPlanGrid();
      }
    } catch (err) {
      showErrorToast(err, 'Could not refresh the subscription: ');
    }
  };

  window.handleChangePlan = function (plan) {
    const entry = PLAN_CATALOG.find(function (p) { return p.key === plan; });
    if (!entry) return;
    if (!entry.selfServe) {
      showFloatingToast(entry.key === 'ENTERPRISE'
        ? 'Enterprise plans are sold by our team. Contact support to upgrade.'
        : 'The trial plan is assigned automatically when your society is approved.');
      return;
    }

    confirmAction(
      'Move your society to the ' + entry.name + ' plan (' + entry.price + ' ' + entry.period + ')?',
      function () { startPlanCheckout(plan); },
      { confirmLabel: 'Continue to payment' }
    );
  };

  let razorpayScriptPromise = null;

  function loadRazorpayCheckout() {
    if (window.Razorpay) return Promise.resolve(true);
    if (razorpayScriptPromise) return razorpayScriptPromise;

    razorpayScriptPromise = new Promise(function (resolve) {
      const script = document.createElement('script');
      script.src = 'https://checkout.razorpay.com/v1/checkout.js';
      script.async = true;
      script.onload = function () { resolve(true); };
      script.onerror = function () { resolve(false); };
      document.head.appendChild(script);
    });
    return razorpayScriptPromise;
  }

  async function startPlanCheckout(plan) {
    let order;
    try {
      order = await Api.post('/subscriptions/create-order', { plan: plan });
    } catch (err) {
      showErrorToast(err, 'Could not create the payment order: ');
      return;
    }

    if (!order || !order.razorpayOrderId || !order.keyId) {
      showFloatingToast('The server did not return a usable payment order.');
      return;
    }

    const loaded = await loadRazorpayCheckout();
    if (!loaded) {
      showFloatingToast('The Razorpay checkout could not be loaded. Check your network and try again.');
      return;
    }

    const checkout = new window.Razorpay({
      key: order.keyId,
      amount: order.amount,
      currency: order.currency || 'INR',
      order_id: order.razorpayOrderId,
      name: 'Samaaj',
      description: 'Samaaj subscription - ' + plan,
      theme: { color: '#966340' },
      handler: function (response) {
        verifyPlanPayment(response, plan);
      },
      modal: {
        ondismiss: function () {
          showFloatingToast('Payment was cancelled.');
        }
      }
    });
    checkout.open();
  }

  async function verifyPlanPayment(response, plan) {
    try {
      await Api.post('/subscriptions/verify-payment', {
        razorpayOrderId: response.razorpay_order_id,
        razorpayPaymentId: response.razorpay_payment_id,
        razorpaySignature: response.razorpay_signature
      });
      showFloatingToast('Payment verified. The ' + Data.titleCase(plan) + ' plan is active.');
      await window.refreshBilling();
    } catch (err) {
      showErrorToast(err, 'Payment verification failed: ');
    }
  }

  /* -------------------------------------------------------------------------
     USER MANAGEMENT (society admin)
     ------------------------------------------------------------------------- */

  async function loadAdminUsers(page) {
    const roleFilter = document.getElementById('adminUsersRoleFilter');
    const statusFilter = document.getElementById('adminUsersStatusFilter');
    const result = await Data.loadAdminUsers({
      page: page || 0,
      role: roleFilter && roleFilter.value ? roleFilter.value : undefined,
      status: statusFilter && statusFilter.value ? statusFilter.value : undefined
    });
    state.adminUsers = result.items;
    state.adminUsersPage = result;
    return result;
  }

  window.reloadAdminUsers = async function (page) {
    try {
      await loadAdminUsers(page || 0);
      renderAdminUsersTable();
    } catch (err) {
      showErrorToast(err, 'Could not load users: ');
    }
  };

  function renderAdminUsersTable() {
    const body = document.getElementById('adminUsersTableBody');
    if (!body) return;

    const query = (document.getElementById('adminUsersSearch')?.value || '').toLowerCase().trim();
    const list = (state.adminUsers || []).filter(function (u) {
      if (!query) return true;
      return u.name.toLowerCase().includes(query) || u.email.toLowerCase().includes(query) ||
        String(u.flat).toLowerCase().includes(query);
    });

    if (!list.length) {
      body.innerHTML = '<tr><td colspan="6"><div class="empty-state"><h4>No users match these filters</h4><p>Adjust the role or status filter, or invite a new resident.</p></div></td></tr>';
      renderPagination('adminUsersPagination', 0, 0, 0, 'users', function () {});
      return;
    }

    body.innerHTML = list.map(function (u) {
      const isAdminRow = u.role === 'ADMIN' || u.role === 'SUPER_ADMIN';
      const nextStatus = u.status === 'ACTIVE' ? 'BLOCKED' : 'ACTIVE';
      const actionLabel = u.status === 'ACTIVE' ? 'Block' : 'Activate';
      return `
        <tr>
          <td><strong>${escape(u.name)}</strong></td>
          <td>${escape(u.email)}</td>
          <td><span class="status-badge" style="background: var(--cream-200); color: var(--brown-800);">${escape(u.flat)}</span></td>
          <td>${escape(Data.titleCase(u.role))}</td>
          <td><span class="status-badge ${badgeClass(u.status)}">${escape(Data.titleCase(u.status))}</span></td>
          <td>
            <div class="user-row-actions">
              ${isAdminRow
                ? '<span class="inline-note">Protected</span>'
                : `<button type="button" class="btn-text-subtle" onclick="handleUpdateUserStatus('${u.id}', '${nextStatus}', '${escape(u.name)}')">${actionLabel}</button>`}
            </div>
          </td>
        </tr>
      `;
    }).join('');

    const page = state.adminUsersPage || { page: 0, totalPages: 0, total: 0 };
    renderPagination('adminUsersPagination', page.page, page.totalPages, page.total, 'users', function (p) {
      window.reloadAdminUsers(p);
    });
  }

  window.handleUpdateUserStatus = function (userId, status, userName) {
    const verb = status === 'ACTIVE' ? 'activate' : 'block';
    confirmAction(
      'Are you sure you want to ' + verb + ' ' + userName + '? ' +
      (status === 'ACTIVE'
        ? 'They will be able to sign in and use the society workspace.'
        : 'They will lose access until they are activated again.'),
      async function () {
        try {
          await Api.patch('/users/' + encodeURIComponent(userId) + '/status', { status: status });
          showFloatingToast(userName + ' is now ' + status.toLowerCase() + '.');
          await window.reloadAdminUsers((state.adminUsersPage || {}).page || 0);
        } catch (err) {
          showErrorToast(err, 'Could not update the account: ');
        }
      },
      { confirmLabel: status === 'ACTIVE' ? 'Activate' : 'Block' }
    );
  };

  // Helpers
  function badgeClass(status) {
    const map = {
      OPEN: 'open',
      IN_PROGRESS: 'in-progress',
      RESOLVED: 'resolved',
      REJECTED: 'rejected',
      PAID: 'paid',
      PENDING: 'open',
      PENDING_VERIFICATION: 'pending-verification',
      OVERDUE: 'overdue',
      ACTIVE: 'active',
      INACTIVE: 'overdue',
      BLOCKED: 'rejected',
      TRIAL: 'pending-verification'
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
  window.showFloatingToast = function (msg, type) {
    const toast = document.getElementById('appFloatingToast');
    const toastMsg = document.getElementById('floatingToastMsg');
    if (!toast || !toastMsg) return;
    toastMsg.textContent = msg;
    toast.classList.toggle('is-error', type === 'error');

    const successIcon = document.getElementById('toastIconSuccess');
    const errorIcon = document.getElementById('toastIconError');
    if (successIcon) successIcon.style.display = type === 'error' ? 'none' : '';
    if (errorIcon) errorIcon.style.display = type === 'error' ? '' : 'none';

    toast.classList.add('show');
    clearTimeout(toastTimer);
    toastTimer = setTimeout(() => {
      toast.classList.remove('show');
    }, type === 'error' ? 6500 : 2800);
  };

  /**
   * Surfaces an API failure with the backend trace id so support can trace the request.
   */
  function showErrorToast(err, prefix) {
    const message = err && err.message ? err.message : 'The request failed.';
    const reference = err && err.traceId ? ' (ref ' + err.traceId + ')' : '';
    showFloatingToast((prefix ? prefix + ': ' : '') + message + reference, 'error');
  }

  window.copySocietyCode = function () {
    const code = state.society.code;
    if (!code) {
      showFloatingToast('Your society code is not available yet.');
      return;
    }
    navigator.clipboard?.writeText(code);
    showFloatingToast('Society Code copied: ' + code);
  };

  // A 401 anywhere in the app means the session is gone - drop straight back to sign in.
  window.addEventListener('samaaj:unauthorized', function () {
    Session.clear();
    showAuthScreen('Your session has expired. Please sign in again.');
  });

  window.updateApiStatusIndicator = async function () {
    const dot = document.getElementById('apiStatusDot');
    const text = document.getElementById('apiStatusText');
    if (!text || !dot) return;

    const current = Api.baseUrl();
    text.textContent = 'Probing ' + current + '...';
    dot.className = 'api-status-dot';

    const ok = await Api.testConnection();
    if (ok) {
      dot.className = 'api-status-dot online';
      text.textContent = 'Server: ' + current;
    } else {
      dot.className = 'api-status-dot offline';
      text.textContent = 'Offline (' + current + ')';
    }
  };

  window.openApiConfigModal = function () {
    const input = document.getElementById('apiBaseInput');
    if (input) input.value = Api.baseUrl();
    openModal('apiConfigModal');
  };

  window.setApiPreset = function (url) {
    const input = document.getElementById('apiBaseInput');
    if (input) input.value = url;
  };

  window.saveApiBaseConfig = async function () {
    const input = document.getElementById('apiBaseInput');
    const val = (input && input.value ? input.value : '').trim();
    if (!val) {
      showToast('Please enter a valid API URL', 'error');
      return;
    }
    Api.setBaseUrl(val);
    closeModal('apiConfigModal');
    showFloatingToast('Server URL set. Checking connection...');
    await window.updateApiStatusIndicator();
  };

  // ── Auto-initialize ──
  document.addEventListener('DOMContentLoaded', async () => {
    // In local/LAN development probe available candidate endpoints
    await Api.discoverBase();
    window.updateApiStatusIndicator();

    // A stored token is not proof of a session: confirm it with the backend first.
    if (Session.isAuthenticated()) {
      await bootstrapPortal();
    }
  });

})();
