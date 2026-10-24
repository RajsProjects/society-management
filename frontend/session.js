/**
 * Samaaj - Session Store
 * Owns the JWT, the cached profile, and every role/permission decision the UI makes.
 *
 * The backend roles are:
 *   tenant role   -> SUPER_ADMIN | ADMIN | ACCOUNTANT | SECURITY | RESIDENT
 *   platform role -> PLATFORM_ADMIN | SOCIETY_USER
 */

(function (global) {
  'use strict';

  const TOKEN_KEY = 'samaaj_token';
  const PROFILE_KEY = 'samaaj_profile';

  const ADMIN_ROLES = ['ADMIN', 'SUPER_ADMIN'];
  const TENANT_ADMIN_ROLES = ['ADMIN', 'SUPER_ADMIN', 'ACCOUNTANT'];

  function readProfile() {
    try {
      const raw = localStorage.getItem(PROFILE_KEY);
      return raw ? JSON.parse(raw) : null;
    } catch (_) {
      return null;
    }
  }

  function decodeClaims(token) {
    if (!token || token.split('.').length !== 3) return null;
    try {
      const segment = token.split('.')[1].replace(/-/g, '+').replace(/_/g, '/');
      const padded = segment + '==='.slice((segment.length + 3) % 4);
      return JSON.parse(decodeURIComponent(escape(atob(padded))));
    } catch (_) {
      return null;
    }
  }

  const Session = {
    get token() {
      return localStorage.getItem(TOKEN_KEY);
    },

    get profile() {
      return readProfile();
    },

    get claims() {
      return decodeClaims(this.token);
    },

    get role() {
      const profile = this.profile;
      if (profile && profile.role) return profile.role;
      const claims = this.claims;
      return claims && claims.role ? claims.role : null;
    },

    get platformRole() {
      const profile = this.profile;
      if (profile && profile.platformRole) return profile.platformRole;
      const claims = this.claims;
      return claims && claims.platformRole ? claims.platformRole : null;
    },

    get societyId() {
      const profile = this.profile;
      if (profile && profile.societyId) return profile.societyId;
      const claims = this.claims;
      return claims && claims.societyId ? claims.societyId : null;
    },

    get userId() {
      const claims = this.claims;
      return claims && claims.userId ? claims.userId : null;
    },

    get fullName() {
      const profile = this.profile;
      if (!profile) return '';
      const parts = [profile.firstName, profile.lastName].filter(Boolean);
      return parts.length ? parts.join(' ') : profile.email;
    },

    get initials() {
      const name = this.fullName;
      if (!name) return '?';
      const parts = name.split(/\s+/).filter(Boolean);
      if (!parts.length) return '?';
      return parts.slice(0, 2).map(function (part) { return part[0]; }).join('').toUpperCase();
    },

    isAuthenticated() {
      return Boolean(this.token);
    },

    isPlatformAdmin() {
      return this.platformRole === 'PLATFORM_ADMIN';
    },

    isTenantAdmin() {
      return ADMIN_ROLES.indexOf(this.role) !== -1;
    },

    /** Admins plus the accountant role, which can verify payments. */
    canManageFinance() {
      return TENANT_ADMIN_ROLES.indexOf(this.role) !== -1;
    },

    isResident() {
      return this.role === 'RESIDENT';
    },

    setSession(token, profile) {
      localStorage.setItem(TOKEN_KEY, token);
      if (profile) localStorage.setItem(PROFILE_KEY, JSON.stringify(profile));
    },

    setProfile(profile) {
      localStorage.setItem(PROFILE_KEY, JSON.stringify(profile));
    },

    clear() {
      localStorage.removeItem(TOKEN_KEY);
      localStorage.removeItem(PROFILE_KEY);
    }
  };

  global.Session = Session;
})(window);
