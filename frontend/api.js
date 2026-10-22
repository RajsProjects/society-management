/**
 * Samaaj - API Client
 * Single source of truth for every HTTP call to the Spring Boot backend.
 *
 * Responsibilities:
 *   - Resolve and cache the backend base URL (same-origin in prod, probed in dev)
 *   - Attach the Bearer token to every request
 *   - Normalise every failure into an ApiError with a trace id
 *   - Signal 401 expiry to the app shell via the `samaaj:unauthorized` event
 */

(function (global) {
  'use strict';

  const TOKEN_KEY = 'samaaj_token';
  const BASE_URL_KEY = 'samaaj_api_url';
  const DEV_CANDIDATES = ['http://localhost:8081/api/v1', 'http://localhost:8080/api/v1'];

  class ApiError extends Error {
    constructor(message, status, traceId, body) {
      super(message);
      this.name = 'ApiError';
      this.status = status;
      this.traceId = traceId || null;
      this.body = body || null;
    }

    get isAuthError() {
      return this.status === 401 || this.status === 403;
    }

    get isNotFound() {
      return this.status === 404;
    }
  }

  function isLocalhost(host) {
    return host === 'localhost' || host === '127.0.0.1' || host === '::1' || host === '0.0.0.0';
  }

  /**
   * Resolution order:
   *   1. Explicit override  -> localStorage.samaaj_api_url (Settings UI / debugging)
   *   2. Window global      -> window.SAMAAJ_API_BASE (injected by the deploy template)
   *   3. Same origin        -> "/api/v1" when the app is served by the API host or its proxy
   *   4. Dev fallback       -> http://localhost:8081/api/v1
   */
  function resolveBaseUrl() {
    const override = localStorage.getItem(BASE_URL_KEY);
    if (override) return trimSlash(override);

    if (global.SAMAAJ_API_BASE) return trimSlash(global.SAMAAJ_API_BASE);

    if (global.location && /^https?:$/.test(global.location.protocol) && !isLocalhost(global.location.hostname)) {
      return '/api/v1';
    }

    return DEV_CANDIDATES[0];
  }

  function trimSlash(value) {
    return String(value).replace(/\/+$/, '');
  }

  let cachedBase = resolveBaseUrl();
  let baseLocked = false;

  function currentBase() {
    return cachedBase;
  }

  function buildUrl(path, query) {
    const base = currentBase();
    const suffix = path.startsWith('/') ? path : '/' + path;
    let url = base + suffix;
    if (query && typeof query === 'object') {
      const params = new URLSearchParams();
      Object.keys(query).forEach(function (key) {
        const value = query[key];
        if (value === undefined || value === null || value === '') return;
        params.append(key, value);
      });
      const qs = params.toString();
      if (qs) url += (url.indexOf('?') === -1 ? '?' : '&') + qs;
    }
    return url;
  }

  function token() {
    return localStorage.getItem(TOKEN_KEY);
  }

  function newTraceId() {
    const rand = Math.random().toString(36).slice(2, 10);
    return 'web-' + Date.now().toString(36) + '-' + rand;
  }

  async function request(path, options) {
    const opts = options || {};
    const method = (opts.method || 'GET').toUpperCase();
    const headers = Object.assign({ 'Accept': 'application/json' }, opts.headers || {});
    const traceId = newTraceId();

    headers['X-Trace-Id'] = traceId;
    headers['X-Request-Id'] = traceId;

    const bearer = token();
    if (bearer) headers['Authorization'] = 'Bearer ' + bearer;

    let payload;
    const isFormData = typeof FormData !== 'undefined' && opts.body instanceof FormData;
    if (opts.body !== undefined && opts.body !== null) {
      if (isFormData) {
        // The browser must set the multipart boundary itself.
        payload = opts.body;
      } else {
        headers['Content-Type'] = headers['Content-Type'] || 'application/json';
        payload = typeof opts.body === 'string' ? opts.body : JSON.stringify(opts.body);
      }
    }

    let response;
    try {
      response = await fetch(buildUrl(path, opts.query), {
        method: method,
        headers: headers,
        body: payload,
        signal: opts.signal
      });
    } catch (networkError) {
      if (networkError && networkError.name === 'AbortError') throw networkError;
      throw new ApiError('Cannot reach the server. Check your connection and try again.', 0, traceId, null);
    }

    const text = await response.text();
    let body = null;
    if (text) {
      try {
        body = JSON.parse(text);
      } catch (_) {
        body = text;
      }
    }

    // The backend stamps every response with a trace id, in the header or the body.
    const responseTrace = response.headers.get('X-Trace-Id')
      || (body && typeof body === 'object' ? body.traceId : null)
      || traceId;

    if (!response.ok) {
      if (response.status === 401) {
        signalUnauthorized();
      }
      throw new ApiError(extractMessage(body, response.status), response.status, responseTrace, body);
    }

    if (baseLocked === false && currentBase() !== resolveBaseUrl()) {
      cachedBase = resolveBaseUrl();
    }
    return body;
  }

  function extractMessage(body, status) {
    if (body && typeof body === 'object') {
      // Bean validation failures arrive as { details: { field: message } }.
      if (body.details && typeof body.details === 'object') {
        const details = Object.keys(body.details).map(function (key) {
          return key + ': ' + body.details[key];
        });
        if (details.length) return details.join(' · ');
      }
      if (body.message) return body.message;
      if (body.error) return body.error;
    }
    if (typeof body === 'string' && body.trim()) return body.trim();
    if (status === 401) return 'Your session has expired. Please sign in again.';
    if (status === 402) return 'Your subscription has expired. Renew to make changes.';
    if (status === 403) return 'You do not have permission to perform this action.';
    if (status === 404) return 'The requested record was not found.';
    if (status === 409) return 'That record already exists.';
    if (status === 413) return 'The uploaded file is larger than the 5 MB limit.';
    if (status >= 500) return 'The server encountered an error. Please try again.';
    return 'Request failed with status ' + status;
  }

  function signalUnauthorized() {
    localStorage.removeItem(TOKEN_KEY);
    global.dispatchEvent(new CustomEvent('samaaj:unauthorized'));
  }

  /**
   * Dev convenience: probe the known local ports once and remember the one that answers.
   * No-op in production (same-origin base is used there).
   */
  async function discoverBase() {
    if (!global.location || !isLocalhost(global.location.hostname)) return currentBase();
    for (let i = 0; i < DEV_CANDIDATES.length; i++) {
      const candidate = DEV_CANDIDATES[i];
      try {
        const probe = await fetch(candidate + '/health', { method: 'GET' });
        if (probe.ok) {
          cachedBase = candidate;
          baseLocked = true;
          return candidate;
        }
      } catch (_) {
        // try next candidate
      }
    }
    return currentBase();
  }

  global.Api = {
    ApiError: ApiError,
    request: request,
    get: function (path, query, options) {
      return request(path, Object.assign({}, options, { method: 'GET', query: query }));
    },
    post: function (path, body, options) {
      return request(path, Object.assign({}, options, { method: 'POST', body: body }));
    },
    put: function (path, body, options) {
      return request(path, Object.assign({}, options, { method: 'PUT', body: body }));
    },
    patch: function (path, body, options) {
      return request(path, Object.assign({}, options, { method: 'PATCH', body: body }));
    },
    del: function (path, options) {
      return request(path, Object.assign({}, options, { method: 'DELETE' }));
    },
    baseUrl: currentBase,
    setBaseUrl: function (value) {
      cachedBase = trimSlash(value);
      localStorage.setItem(BASE_URL_KEY, cachedBase);
    },
    discoverBase: discoverBase
  };
})(window);
