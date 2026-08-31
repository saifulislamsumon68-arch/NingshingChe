(function (NC) {
  'use strict';

  const SESSION_KEY = 'nc:admin-session';
  const SERVER_MODE = 'supabase-rbac';
  const LEGACY_MODE = 'legacy-demo';
  let expiryTimer = null;

  function getStoredSession() {
    for (const storage of [sessionStorage, localStorage]) {
      try {
        const raw = storage.getItem(SESSION_KEY);
        if (raw) return { session: JSON.parse(raw), storage };
      } catch (_) {
        // Continue with the other storage mechanism.
      }
    }
    return null;
  }

  function clearStoredSession() {
    for (const storage of [sessionStorage, localStorage]) {
      try { storage.removeItem(SESSION_KEY); } catch (_) { /* ignored */ }
    }
  }

  function writeSession(session, remember = session?.remember) {
    clearStoredSession();
    const storage = remember ? localStorage : sessionStorage;
    storage.setItem(SESSION_KEY, JSON.stringify({ ...session, remember: Boolean(remember) }));
  }

  function scheduleExpiry(session) {
    window.clearTimeout(expiryTimer);
    const delay = Math.max(0, Math.min(session.expiresAt - Date.now(), 2147483647));
    expiryTimer = window.setTimeout(() => {
      if (Date.now() >= session.expiresAt) logout('expired');
      else scheduleExpiry(session);
    }, delay);
  }

  function setSession(session, remember = false) {
    const complete = { ...session, remember: Boolean(remember) };
    writeSession(complete, remember);
    NC.state.session = complete;
    NC.state.authReason = '';
    scheduleExpiry(complete);
    window.dispatchEvent(new CustomEvent('nc:auth-change', { detail: { authenticated: true } }));
  }

  function normalizeUser(value = {}) {
    return {
      id: value.id || '',
      username: String(value.username || '').trim(),
      name: String(value.name || value.display_name || value.username || 'Dashboard user').trim(),
      roleId: value.role_id || value.roleId || '',
      role: String(value.role || 'Dashboard user').trim(),
      roleSlug: String(value.role_slug || value.roleSlug || '').trim().toLocaleLowerCase(),
      permissions: Array.isArray(value.permissions) ? Array.from(new Set(value.permissions.map(String))) : [],
      mustChangePassword: Boolean(value.must_change_password ?? value.mustChangePassword),
      isActive: value.is_active !== false
    };
  }

  function unwrapRpc(value) {
    return Array.isArray(value) ? (value[0] || {}) : (value || {});
  }

  function isRpcMissing(error) {
    return Boolean(error?.isRpcMissing || error?.code === 'PGRST202'
      || (error?.status === 404 && /function|schema cache/i.test(`${error?.message || ''} ${error?.details || ''}`)));
  }

  function restore() {
    const stored = getStoredSession();
    const session = stored?.session;
    if (!session || !Number.isFinite(session.expiresAt) || session.expiresAt <= Date.now()) {
      clearStoredSession();
      NC.state.session = null;
      return null;
    }

    const validServerSession = session.version === 2
      && session.mode === SERVER_MODE
      && session.token
      && session.user?.username;
    const validLegacySession = session.version === 1
      && session.mode === LEGACY_MODE
      && session.dashboardToken
      && session.user?.username;
    if (!validServerSession && !validLegacySession) {
      clearStoredSession();
      NC.state.session = null;
      return null;
    }

    session.user = normalizeUser(session.user);
    session.remember = stored.storage === localStorage;
    NC.state.session = session;
    scheduleExpiry(session);
    return session;
  }

  async function legacyLogin(username, password, remember) {
    const normalizedUsername = String(username || '').trim().toLocaleLowerCase();
    const passwordDigest = await NC.utils.sha256(String(password || ''));
    const valid = normalizedUsername === NC_CONFIG.auth.username.toLocaleLowerCase()
      && passwordDigest === NC_CONFIG.auth.passwordHash;
    if (!valid) {
      await new Promise((resolve) => window.setTimeout(resolve, 350));
      throw new Error('The username or password is incorrect.');
    }

    const now = Date.now();
    const duration = remember
      ? NC_CONFIG.app.rememberedSessionDays * 24 * 60 * 60 * 1000
      : NC_CONFIG.app.sessionHours * 60 * 60 * 1000;
    const permissions = NC_CONFIG.routes.map((route) => route.id);
    const session = {
      version: 1,
      id: NC.utils.uuid(),
      mode: LEGACY_MODE,
      user: normalizeUser({
        username: normalizedUsername,
        name: NC_CONFIG.auth.displayName,
        role: 'Super Admin',
        role_slug: 'super-admin',
        permissions
      }),
      // Compatibility only until migration 004 is installed. The production
      // session flow never stores or sends this discoverable demo digest.
      dashboardToken: await NC.utils.sha256(`${normalizedUsername}:${password}`),
      issuedAt: now,
      expiresAt: now + duration
    };
    setSession(session, remember);
    return session;
  }

  async function login(username, password, remember = false) {
    try {
      const result = unwrapRpc(await NC.api.rpc('dashboard_login', {
        p_username: String(username || '').trim(),
        p_password: String(password || ''),
        p_remember: Boolean(remember),
        p_user_agent: navigator.userAgent || ''
      }));
      if (!result.ok || !result.token || !result.user) {
        throw new Error(result.error || 'The username or password is incorrect.');
      }
      const expiresAt = new Date(result.expires_at).getTime();
      if (!Number.isFinite(expiresAt)) throw new Error('The login server returned an invalid session.');
      const session = {
        version: 2,
        id: NC.utils.uuid(),
        mode: SERVER_MODE,
        token: result.token,
        user: normalizeUser(result.user),
        issuedAt: Date.now(),
        expiresAt
      };
      setSession(session, remember);
      return session;
    } catch (error) {
      // Prevent lockout while the additive access-control migration is being
      // installed. Once its RPC exists, legacy login is never used.
      if (isRpcMissing(error)) return legacyLogin(username, password, remember);
      throw error;
    }
  }

  function endSession(reason = 'manual') {
    window.clearTimeout(expiryTimer);
    clearStoredSession();
    NC.state.session = null;
    NC.state.authReason = reason;
    window.dispatchEvent(new CustomEvent('nc:auth-change', {
      detail: { authenticated: false, reason }
    }));
  }

  function logout(reason = 'manual') {
    if (NC.state.session?.mode === SERVER_MODE && NC.state.session?.token && NC.api?.rpc) {
      // request() captures the current token synchronously before local state is
      // cleared. Logout remains immediate even if the network is unavailable.
      NC.api.rpc('dashboard_logout').catch(() => {});
    }
    endSession(reason);
  }

  function isAuthenticated() {
    const session = NC.state.session || restore();
    if (!session || session.expiresAt <= Date.now()) {
      if (session) logout('expired');
      return false;
    }
    return true;
  }

  async function validateSession() {
    const session = NC.state.session || restore();
    if (!session) return false;
    try {
      const result = unwrapRpc(await NC.api.rpc('dashboard_session'));
      if (session.mode === LEGACY_MODE) {
        // A present RPC means the database has upgraded and old demo sessions
        // must be replaced by a server-issued session.
        endSession('security-upgrade');
        return false;
      }
      if (!result.ok || !result.user) {
        endSession('expired');
        return false;
      }
      const expiresAt = new Date(result.expires_at).getTime();
      session.user = normalizeUser(result.user);
      if (Number.isFinite(expiresAt)) session.expiresAt = expiresAt;
      writeSession(session, session.remember);
      scheduleExpiry(session);
      window.dispatchEvent(new CustomEvent('nc:session-change', { detail: { user: session.user } }));
      return true;
    } catch (error) {
      if (session.mode === LEGACY_MODE && isRpcMissing(error)) return true;
      // A temporary network outage should not destroy an otherwise unexpired
      // session. Every protected database request is still checked server-side.
      if (error?.code === 'NETWORK_ERROR' || error?.code === 'TIMEOUT') return true;
      endSession('expired');
      return false;
    }
  }

  function updateUser(user) {
    if (!NC.state.session) return;
    NC.state.session.user = normalizeUser(user);
    writeSession(NC.state.session, NC.state.session.remember);
    window.dispatchEvent(new CustomEvent('nc:session-change', { detail: { user: NC.state.session.user } }));
  }

  function getDashboardToken() {
    return isAuthenticated() && NC.state.session.mode === LEGACY_MODE
      ? NC.state.session.dashboardToken
      : '';
  }

  function getSessionToken() {
    return isAuthenticated() && NC.state.session.mode === SERVER_MODE
      ? NC.state.session.token
      : '';
  }

  function getAccessToken() {
    return isAuthenticated() ? (NC.state.session.accessToken || '') : '';
  }

  function canAccess(route) {
    if (!isAuthenticated()) return false;
    const user = NC.state.session.user || {};
    if (NC.state.session.mode === LEGACY_MODE || user.roleSlug === 'super-admin') return true;
    return Array.isArray(user.permissions) && user.permissions.includes(String(route || ''));
  }

  function isSuperAdmin() {
    return isAuthenticated() && (
      NC.state.session.mode === LEGACY_MODE
      || NC.state.session.user?.roleSlug === 'super-admin'
    );
  }

  function firstAccessibleRoute(preferred = '') {
    if (preferred && canAccess(preferred)) return preferred;
    const route = NC_CONFIG.routes.find((item) => canAccess(item.id));
    return route?.id || 'dashboard';
  }

  function remainingMilliseconds() {
    return isAuthenticated() ? Math.max(0, NC.state.session.expiresAt - Date.now()) : 0;
  }

  function isLegacy() {
    return isAuthenticated() && NC.state.session.mode === LEGACY_MODE;
  }

  NC.auth = Object.freeze({
    restore, login, logout, isAuthenticated, validateSession, updateUser,
    getDashboardToken, getSessionToken, getAccessToken, canAccess,
    isSuperAdmin, firstAccessibleRoute, remainingMilliseconds, isLegacy
  });
})(window.NC);
