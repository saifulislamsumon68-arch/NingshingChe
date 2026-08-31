(function (NC) {
  'use strict';

  const { escapeHTML, formatDateTime, qsa, validateFields } = NC.utils;
  let root = null;
  let renderContext = null;
  let snapshot = { users: [], roles: [], active_sessions: 0 };

  function rpcObject(value) {
    return Array.isArray(value) ? (value[0] || {}) : (value || {});
  }

  function message(error, fallback) {
    return error?.message || NC.api.userMessage(error, fallback) || fallback;
  }

  function menuRoutes() {
    return NC_CONFIG.routes.filter((item) => item.id !== 'access-control');
  }

  function menuLabel(id) {
    return NC_CONFIG.routes.find((route) => route.id === id)?.label || id;
  }

  function roleById(id) {
    return snapshot.roles.find((role) => role.id === id);
  }

  function migrationPanel() {
    return `
      ${NC.components.pageHeader({
        eyebrow: 'System security',
        title: 'Users & Roles',
        description: 'Create dashboard logins and control which menus each role can access.',
        breadcrumb: [{ label: 'Users & Roles' }]
      })}
      <section class="surface access-migration-card">
        <span class="access-migration-icon"><i class="fa-solid fa-database" aria-hidden="true"></i></span>
        <div>
          <p class="eyebrow">One-time database upgrade</p>
          <h2>Install role-based access control</h2>
          <p>The dashboard is using compatibility login because the access-control RPCs are not installed yet. Run the complete migration in the Supabase SQL Editor, then sign out and sign in with the initial Super Admin account.</p>
          <code>backend/supabase/migrations/004_dashboard_access_control.sql</code>
          <ol>
            <li>Back up the current Supabase project.</li>
            <li>Run migration 003 first if the Blog media update is still pending.</li>
            <li>Run migration 004 and refresh this page.</li>
            <li>Change the initial Super Admin password immediately.</li>
          </ol>
        </div>
      </section>`;
  }

  function permissionChips(role) {
    const permissions = Array.isArray(role.permissions) ? role.permissions : [];
    if (!permissions.length) return '<span class="text-sm text-muted-foreground">No menu access</span>';
    return `<div class="permission-chip-list">${permissions.map((permission) => `
      <span class="permission-chip"><i class="fa-regular fa-check" aria-hidden="true"></i>${escapeHTML(menuLabel(permission))}</span>
    `).join('')}</div>`;
  }

  function userRows() {
    const currentId = NC.state.session?.user?.id;
    return snapshot.users.map((user) => {
      const isCurrent = user.id === currentId;
      return `<tr>
        <td data-label="User">
          <div class="access-user-cell">
            <span class="user-avatar">${escapeHTML(NC.utils.initials(user.name))}</span>
            <span><strong>${escapeHTML(user.name)}</strong><small>@${escapeHTML(user.username)}${isCurrent ? ' · You' : ''}</small></span>
          </div>
        </td>
        <td data-label="Role"><span class="role-name-badge"><i class="fa-regular ${user.role_slug === 'super-admin' ? 'fa-crown' : 'fa-user-tag'}" aria-hidden="true"></i>${escapeHTML(user.role)}</span></td>
        <td data-label="Status"><span class="status-badge ${user.is_active ? 'status-success' : 'status-danger'}"><i class="fa-solid ${user.is_active ? 'fa-circle-check' : 'fa-circle-xmark'}" aria-hidden="true"></i>${user.is_active ? 'Active' : 'Disabled'}</span>${user.must_change_password ? '<span class="credential-warning"><i class="fa-regular fa-key" aria-hidden="true"></i>Password change required</span>' : ''}</td>
        <td data-label="Last login">${escapeHTML(user.last_login_at ? formatDateTime(user.last_login_at) : 'Never')}</td>
        <td data-label="Actions">
          <div class="row-actions">
            ${isCurrent
              ? '<button type="button" class="row-action" data-own-credentials aria-label="Update my login" title="Update my login"><i class="fa-regular fa-key" aria-hidden="true"></i></button>'
              : `<button type="button" class="row-action" data-edit-user="${escapeHTML(user.id)}" aria-label="Edit ${escapeHTML(user.name)}" title="Edit user"><i class="fa-regular fa-pen" aria-hidden="true"></i></button><button type="button" class="row-action row-action-danger" data-delete-user="${escapeHTML(user.id)}" aria-label="Delete ${escapeHTML(user.name)}" title="Delete user"><i class="fa-regular fa-trash" aria-hidden="true"></i></button>`}
          </div>
        </td>
      </tr>`;
    }).join('');
  }

  function roleCards() {
    return snapshot.roles.map((role) => {
      const protectedRole = role.slug === 'super-admin';
      return `<article class="role-card ${protectedRole ? 'role-card-protected' : ''}">
        <div class="role-card-heading">
          <span class="role-card-icon"><i class="fa-regular ${protectedRole ? 'fa-crown' : 'fa-id-badge'}" aria-hidden="true"></i></span>
          <div><h3>${escapeHTML(role.name)}</h3><p>${escapeHTML(role.description || 'Custom dashboard role')}</p></div>
          ${role.is_system ? '<span class="system-role-label">System</span>' : '<span class="system-role-label custom-role-label">Custom</span>'}
        </div>
        <div class="role-card-meta"><span><strong>${Number(role.user_count || 0).toLocaleString()}</strong> user${Number(role.user_count || 0) === 1 ? '' : 's'}</span><span><strong>${(role.permissions || []).length}</strong> menus</span></div>
        ${permissionChips(role)}
        <div class="role-card-actions">
          ${protectedRole
            ? '<span><i class="fa-regular fa-lock" aria-hidden="true"></i>Full access is protected</span>'
            : `<button type="button" class="btn btn-secondary btn-sm" data-edit-role="${escapeHTML(role.id)}"><i class="fa-regular fa-pen" aria-hidden="true"></i>Edit access</button>${role.is_system ? '' : `<button type="button" class="btn btn-ghost-danger btn-sm" data-delete-role="${escapeHTML(role.id)}"><i class="fa-regular fa-trash" aria-hidden="true"></i>Delete</button>`}`}
        </div>
      </article>`;
    }).join('');
  }

  function renderPage() {
    const current = NC.state.session?.user || {};
    const activeUsers = snapshot.users.filter((user) => user.is_active).length;
    root.innerHTML = `
      ${NC.components.pageHeader({
        eyebrow: 'System security',
        title: 'Users & Roles',
        description: 'Create login accounts, assign roles, and define the dashboard menus each role can see.',
        breadcrumb: [{ label: 'Users & Roles' }],
        actions: '<button type="button" class="btn btn-secondary" data-add-role><i class="fa-regular fa-shield-plus" aria-hidden="true"></i>New role</button><button type="button" class="btn btn-primary" data-add-user><i class="fa-regular fa-user-plus" aria-hidden="true"></i>New login</button>'
      })}
      ${current.mustChangePassword ? `<div class="mb-6">${NC.components.notice('Your initial password must be changed before this account is considered secure.', 'warning', 'fa-key')}</div>` : ''}
      <section class="access-summary-grid" aria-label="Access-control summary">
        <article><span><i class="fa-regular fa-users" aria-hidden="true"></i></span><div><strong>${snapshot.users.length.toLocaleString()}</strong><small>Total logins</small></div></article>
        <article><span><i class="fa-regular fa-user-check" aria-hidden="true"></i></span><div><strong>${activeUsers.toLocaleString()}</strong><small>Active users</small></div></article>
        <article><span><i class="fa-regular fa-id-badge" aria-hidden="true"></i></span><div><strong>${snapshot.roles.length.toLocaleString()}</strong><small>Available roles</small></div></article>
        <article><span><i class="fa-regular fa-shield-check" aria-hidden="true"></i></span><div><strong>${Number(snapshot.active_sessions || 0).toLocaleString()}</strong><small>Active sessions</small></div></article>
      </section>
      <section class="surface access-own-card mt-6">
        <div><span class="user-avatar user-avatar-large">${escapeHTML(NC.utils.initials(current.name))}</span><div><p class="eyebrow">Signed-in Super Admin</p><h2>${escapeHTML(current.name || current.username)}</h2><p>@${escapeHTML(current.username)} · ${escapeHTML(current.role || 'Super Admin')}</p></div></div>
        <button type="button" class="btn btn-secondary" data-own-credentials><i class="fa-regular fa-key" aria-hidden="true"></i>Update my username or password</button>
      </section>
      <section class="surface mt-6">
        <div class="surface-header"><div><p class="eyebrow">Login accounts</p><h2>Dashboard users</h2><p>New and reset passwords are stored only as server-side bcrypt hashes.</p></div><button type="button" class="btn btn-primary btn-sm" data-add-user><i class="fa-regular fa-user-plus" aria-hidden="true"></i>Add login</button></div>
        ${snapshot.users.length ? NC.components.tableShell({
          caption: 'Dashboard login accounts', minWidth: '900px',
          head: '<tr><th>User</th><th>Role</th><th>Status</th><th>Last login</th><th class="text-right">Actions</th></tr>',
          body: userRows()
        }) : NC.components.emptyState({ icon: 'fa-users-slash', title: 'No login accounts', description: 'Create a Super Admin account in the migration before using access control.' })}
      </section>
      <section class="mt-6">
        <div class="section-heading-row"><div><p class="eyebrow">Menu permissions</p><h2>Roles</h2><p>Only Super Admins can open this page. Other roles receive exactly the selected sidebar menus.</p></div><button type="button" class="btn btn-secondary" data-add-role><i class="fa-regular fa-shield-plus" aria-hidden="true"></i>Create role</button></div>
        <div class="role-grid">${roleCards()}</div>
      </section>`;
    bindPageEvents();
  }

  async function refresh() {
    if (!root) return;
    root.innerHTML = `${NC.components.pageHeader({ eyebrow: 'System security', title: 'Users & Roles', description: 'Loading access rules…', breadcrumb: [{ label: 'Users & Roles' }] })}${NC.components.skeleton(6, 5)}`;
    try {
      snapshot = rpcObject(await NC.api.rpc('dashboard_access_snapshot'));
      if (NC.crud.isStaleNavigation(renderContext)) return;
      snapshot.users ||= [];
      snapshot.roles ||= [];
      renderPage();
    } catch (error) {
      if (NC.crud.isStaleNavigation(renderContext)) return;
      if (error?.isRpcMissing) root.innerHTML = migrationPanel();
      else NC.crud.handleLoadError(root, error, refresh, renderContext);
    }
  }

  function fieldError(name) {
    return `<p class="field-error hidden" data-field-error="${escapeHTML(name)}"></p>`;
  }

  function openUser(user = null) {
    const editing = Boolean(user);
    const roles = snapshot.roles;
    const content = `
      <form id="access-user-form" class="form-stack" novalidate>
        <div class="form-grid-2">
          <div class="field"><label class="field-label" for="access-display-name">Display name <span aria-hidden="true">*</span></label><input class="form-input" id="access-display-name" name="display_name" value="${escapeHTML(user?.name || '')}" maxlength="80" autocomplete="off" required>${fieldError('display_name')}</div>
          <div class="field"><label class="field-label" for="access-username">Username <span aria-hidden="true">*</span></label><input class="form-input" id="access-username" name="username" value="${escapeHTML(user?.username || '')}" minlength="3" maxlength="40" autocomplete="off" required><p class="field-hint mt-1">3–40 characters; no spaces, slash, or colon.</p>${fieldError('username')}</div>
        </div>
        <div class="form-grid-2">
          <div class="field"><label class="field-label" for="access-role">Role <span aria-hidden="true">*</span></label><select class="form-select" id="access-role" name="role_id" required><option value="">Choose role</option>${roles.map((role) => `<option value="${escapeHTML(role.id)}" ${role.id === user?.role_id ? 'selected' : ''}>${escapeHTML(role.name)}</option>`).join('')}</select>${fieldError('role_id')}</div>
          <div class="field"><span class="field-label">Account status</span><label class="switch-row compact-switch-row"><span><strong>Active login</strong><small>Disabled users cannot start or continue sessions.</small></span><span class="switch"><input type="checkbox" name="is_active" ${user?.is_active !== false ? 'checked' : ''}><span></span></span></label></div>
        </div>
        <div class="form-grid-2">
          <div class="field"><label class="field-label" for="access-password">${editing ? 'Reset password' : 'Temporary password'} ${editing ? '' : '<span aria-hidden="true">*</span>'}</label><input class="form-input" type="password" id="access-password" name="password" minlength="8" autocomplete="new-password" placeholder="${editing ? 'Leave blank to keep current password' : 'At least 8 characters'}" ${editing ? '' : 'required'}>${fieldError('password')}</div>
          <div class="field"><label class="field-label" for="access-password-confirm">Confirm password</label><input class="form-input" type="password" id="access-password-confirm" name="password_confirm" autocomplete="new-password" placeholder="Repeat the new password">${fieldError('password_confirm')}</div>
        </div>
        <div>${NC.components.notice(editing ? 'Entering a reset password revokes this user’s active sessions and requires them to change it after login.' : 'The new user must change this temporary password after their first login.', 'info', 'fa-key')}</div>
      </form>`;
    const modal = NC.components.openModal({
      id: 'access-user', size: 'lg', eyebrow: editing ? 'Edit login' : 'New login',
      title: editing ? `Edit ${user.name}` : 'Create dashboard login',
      description: 'Assign one role to control visible menus and protected data changes.',
      content,
      footer: '<button type="button" class="btn btn-secondary" data-modal-close>Cancel</button><button type="submit" form="access-user-form" class="btn btn-primary" data-save-access-user><i class="fa-regular fa-floppy-disk" aria-hidden="true"></i>Save login</button>'
    });
    const form = modal.element.querySelector('#access-user-form');
    form.addEventListener('submit', async (event) => {
      event.preventDefault();
      const data = NC.utils.formData(form);
      const errors = {
        display_name: data.display_name.length < 2 ? 'Enter a display name of at least 2 characters.' : '',
        username: !/^[^\s/:]{3,40}$/u.test(data.username) ? 'Use 3–40 characters without spaces, slash, or colon.' : '',
        role_id: roleById(data.role_id) ? '' : 'Choose a valid role.',
        password: !editing && data.password.length < 8 ? 'Enter a temporary password of at least 8 characters.' : (data.password && data.password.length < 8 ? 'Password must contain at least 8 characters.' : ''),
        password_confirm: data.password !== data.password_confirm ? 'The passwords do not match.' : ''
      };
      if (!validateFields(form, errors)) return;
      const button = modal.element.querySelector('[data-save-access-user]');
      NC.utils.setButtonLoading(button, true, 'Saving…');
      try {
        await NC.api.rpc('dashboard_save_user', {
          p_user_id: user?.id || null,
          p_username: data.username,
          p_display_name: data.display_name,
          p_role_id: data.role_id,
          p_password: data.password || '',
          p_is_active: Boolean(data.is_active)
        });
        NC.components.closeModal('saved');
        NC.components.toast(editing ? 'Login account updated.' : 'New login account created.', 'success');
        await refresh();
      } catch (error) {
        console.error(error);
        NC.components.toast(message(error, 'Unable to save this login.'), 'error');
      } finally { NC.utils.setButtonLoading(button, false); }
    });
  }

  function permissionOptions(role) {
    const selected = new Set(role?.permissions || ['dashboard']);
    selected.add('dashboard');
    const groups = [
      ['overview', 'Overview'], ['content', 'Content'], ['system', 'System']
    ];
    return groups.map(([group, label]) => {
      const routes = menuRoutes().filter((route) => route.group === group);
      if (!routes.length) return '';
      return `<fieldset class="permission-group"><legend>${escapeHTML(label)}</legend><div class="permission-option-grid">${routes.map((route) => {
        const fixed = route.id === 'dashboard';
        return `<label class="permission-option ${selected.has(route.id) ? 'is-selected' : ''}"><input type="checkbox" data-menu-permission value="${escapeHTML(route.id)}" ${selected.has(route.id) ? 'checked' : ''} ${fixed ? 'disabled' : ''}><span><i class="fa-regular ${escapeHTML(route.icon)}" aria-hidden="true"></i><strong>${escapeHTML(route.label)}</strong><small>${fixed ? 'Required home menu' : 'Show this sidebar menu'}</small></span><i class="fa-solid fa-circle-check" aria-hidden="true"></i></label>`;
      }).join('')}</div></fieldset>`;
    }).join('');
  }

  function openRole(role = null) {
    const editing = Boolean(role);
    const content = `
      <form id="access-role-form" class="form-stack" novalidate>
        <div class="field"><label class="field-label" for="access-role-name">Role name <span aria-hidden="true">*</span></label><input class="form-input" id="access-role-name" name="name" value="${escapeHTML(role?.name || '')}" maxlength="80" required>${fieldError('name')}</div>
        <div class="field"><label class="field-label" for="access-role-description">Description</label><textarea class="form-textarea" id="access-role-description" name="description" rows="2" maxlength="300">${escapeHTML(role?.description || '')}</textarea></div>
        <div class="field"><span class="field-label">Visible dashboard menus</span><p class="field-hint mb-3">Dashboard is required. Users & Roles remains exclusive to the protected Super Admin role.</p>${permissionOptions(role)}</div>
      </form>`;
    const modal = NC.components.openModal({
      id: 'access-role', size: 'xl', eyebrow: editing ? 'Edit role' : 'New role',
      title: editing ? `Edit ${role.name}` : 'Create dashboard role',
      description: 'Selected menus are enforced in both navigation and Supabase write policies.',
      content,
      footer: '<button type="button" class="btn btn-secondary" data-modal-close>Cancel</button><button type="submit" form="access-role-form" class="btn btn-primary" data-save-access-role><i class="fa-regular fa-floppy-disk" aria-hidden="true"></i>Save role</button>'
    });
    const form = modal.element.querySelector('#access-role-form');
    qsa('[data-menu-permission]', form).forEach((input) => input.addEventListener('change', () => input.closest('.permission-option').classList.toggle('is-selected', input.checked)));
    form.addEventListener('submit', async (event) => {
      event.preventDefault();
      const data = NC.utils.formData(form);
      const errors = { name: data.name.length < 2 ? 'Enter a role name of at least 2 characters.' : '' };
      if (!validateFields(form, errors)) return;
      const permissions = qsa('[data-menu-permission]', form).filter((input) => input.checked).map((input) => input.value);
      if (!permissions.includes('dashboard')) permissions.unshift('dashboard');
      const button = modal.element.querySelector('[data-save-access-role]');
      NC.utils.setButtonLoading(button, true, 'Saving…');
      try {
        await NC.api.rpc('dashboard_save_role', {
          p_role_id: role?.id || null,
          p_name: data.name,
          p_description: data.description,
          p_menu_permissions: permissions
        });
        NC.components.closeModal('saved');
        NC.components.toast(editing ? 'Role permissions updated.' : 'New role created.', 'success');
        await refresh();
      } catch (error) {
        console.error(error);
        NC.components.toast(message(error, 'Unable to save this role.'), 'error');
      } finally { NC.utils.setButtonLoading(button, false); }
    });
  }

  async function deleteUser(id) {
    const user = snapshot.users.find((item) => item.id === id);
    if (!user) return;
    const accepted = await NC.components.confirm({
      title: `Delete ${user.name}?`,
      description: `The @${user.username} login and all of its active sessions will be removed.`,
      details: 'This does not delete any editorial content created by the user.',
      confirmLabel: 'Delete login', confirmIcon: 'fa-user-xmark'
    });
    if (!accepted) return;
    try {
      await NC.api.rpc('dashboard_delete_user', { p_user_id: id });
      NC.components.toast('Login account deleted.', 'success');
      await refresh();
    } catch (error) {
      console.error(error);
      NC.components.toast(message(error, 'Unable to delete this login.'), 'error');
    }
  }

  async function deleteRole(id) {
    const role = roleById(id);
    if (!role) return;
    const accepted = await NC.components.confirm({
      title: `Delete ${role.name}?`,
      description: 'This custom role will be permanently removed.',
      details: 'A role assigned to any user cannot be deleted until those users are moved.',
      confirmLabel: 'Delete role', confirmIcon: 'fa-shield-xmark'
    });
    if (!accepted) return;
    try {
      await NC.api.rpc('dashboard_delete_role', { p_role_id: id });
      NC.components.toast('Custom role deleted.', 'success');
      await refresh();
    } catch (error) {
      console.error(error);
      NC.components.toast(message(error, 'Unable to delete this role.'), 'error');
    }
  }

  function openOwnCredentials() {
    const current = NC.state.session?.user;
    if (!current) return;
    if (NC.auth.isLegacy()) {
      NC.components.openModal({
        id: 'legacy-account', size: 'md', eyebrow: 'Database upgrade required',
        title: 'Install secure login management',
        description: 'Username and password updates become available after migration 004.',
        content: `${NC.components.notice('Run backend/supabase/migrations/004_dashboard_access_control.sql in the Supabase SQL Editor, then sign in again.', 'warning', 'fa-database')}<code class="migration-path-block">backend/supabase/migrations/004_dashboard_access_control.sql</code>`
      });
      return;
    }
    const mustChange = Boolean(current.mustChangePassword);
    const content = `
      <form id="own-credentials-form" class="form-stack" novalidate>
        ${mustChange ? NC.components.notice('Choose a private password to replace the initial or reset password.', 'warning', 'fa-key') : ''}
        <div class="form-grid-2">
          <div class="field"><label class="field-label" for="own-display-name">Display name</label><input class="form-input" id="own-display-name" name="display_name" value="${escapeHTML(current.name)}" maxlength="80" required>${fieldError('display_name')}</div>
          <div class="field"><label class="field-label" for="own-username">Username</label><input class="form-input" id="own-username" name="username" value="${escapeHTML(current.username)}" minlength="3" maxlength="40" autocomplete="username" required>${fieldError('username')}</div>
        </div>
        <div class="field"><label class="field-label" for="own-current-password">Current password <span aria-hidden="true">*</span></label><input class="form-input" type="password" id="own-current-password" name="current_password" autocomplete="current-password" required>${fieldError('current_password')}</div>
        <div class="form-grid-2">
          <div class="field"><label class="field-label" for="own-new-password">New password ${mustChange ? '<span aria-hidden="true">*</span>' : ''}</label><input class="form-input" type="password" id="own-new-password" name="new_password" minlength="8" autocomplete="new-password" placeholder="${mustChange ? 'At least 8 characters' : 'Leave blank to keep current password'}" ${mustChange ? 'required' : ''}>${fieldError('new_password')}</div>
          <div class="field"><label class="field-label" for="own-password-confirm">Confirm new password</label><input class="form-input" type="password" id="own-password-confirm" name="password_confirm" autocomplete="new-password">${fieldError('password_confirm')}</div>
        </div>
        <p class="field-hint"><i class="fa-regular fa-shield-check" aria-hidden="true"></i> Password hashing happens inside Supabase. Other sessions are revoked when your username or password changes.</p>
      </form>`;
    const modal = NC.components.openModal({
      id: 'own-credentials', size: 'lg', eyebrow: 'My account',
      title: 'Update my login', description: 'Change your display name, username, or password securely.',
      content,
      dismissible: !mustChange,
      footer: `${mustChange ? '' : '<button type="button" class="btn btn-secondary" data-modal-close>Cancel</button>'}<button type="submit" form="own-credentials-form" class="btn btn-primary" data-save-own-credentials><i class="fa-regular fa-key" aria-hidden="true"></i>Update login</button>`
    });
    const form = modal.element.querySelector('#own-credentials-form');
    form.addEventListener('submit', async (event) => {
      event.preventDefault();
      const data = NC.utils.formData(form);
      const errors = {
        display_name: data.display_name.length < 2 ? 'Enter a display name of at least 2 characters.' : '',
        username: !/^[^\s/:]{3,40}$/u.test(data.username) ? 'Use 3–40 characters without spaces, slash, or colon.' : '',
        current_password: !data.current_password ? 'Enter your current password.' : '',
        new_password: (mustChange || data.new_password) && data.new_password.length < 8 ? 'Use at least 8 characters.' : '',
        password_confirm: data.new_password !== data.password_confirm ? 'The new passwords do not match.' : ''
      };
      if (!validateFields(form, errors)) return;
      const button = modal.element.querySelector('[data-save-own-credentials]');
      NC.utils.setButtonLoading(button, true, 'Updating…');
      try {
        const result = rpcObject(await NC.api.rpc('dashboard_update_own_credentials', {
          p_current_password: data.current_password,
          p_username: data.username,
          p_display_name: data.display_name,
          p_new_password: data.new_password || ''
        }));
        if (!result.ok || !result.user) throw new Error(result.error || 'The account could not be updated.');
        NC.auth.updateUser(result.user);
        NC.components.closeModal('saved');
        NC.components.toast('Your login details were updated.', 'success');
        if (root && NC.utils.getHashRoute().route === 'access-control') await refresh();
      } catch (error) {
        console.error(error);
        NC.components.toast(message(error, 'Unable to update your login.'), 'error');
      } finally { NC.utils.setButtonLoading(button, false); }
    });
  }

  function bindPageEvents() {
    qsa('[data-add-user]', root).forEach((button) => button.addEventListener('click', () => openUser()));
    qsa('[data-add-role]', root).forEach((button) => button.addEventListener('click', () => openRole()));
    qsa('[data-own-credentials]', root).forEach((button) => button.addEventListener('click', openOwnCredentials));
    qsa('[data-edit-user]', root).forEach((button) => button.addEventListener('click', () => {
      const user = snapshot.users.find((item) => item.id === button.dataset.editUser);
      if (user) openUser(user);
    }));
    qsa('[data-delete-user]', root).forEach((button) => button.addEventListener('click', () => deleteUser(button.dataset.deleteUser)));
    qsa('[data-edit-role]', root).forEach((button) => button.addEventListener('click', () => {
      const role = roleById(button.dataset.editRole);
      if (role) openRole(role);
    }));
    qsa('[data-delete-role]', root).forEach((button) => button.addEventListener('click', () => deleteRole(button.dataset.deleteRole)));
  }

  async function render(container, context = {}) {
    root = container;
    renderContext = context;
    if (!NC.auth.isSuperAdmin()) {
      root.innerHTML = NC.components.errorState(new Error('Super Admin authorization is required.'), {
        title: 'Access denied', message: 'Only a Super Admin can manage dashboard users and roles.', retry: false
      });
      return;
    }
    if (NC.auth.isLegacy()) {
      root.innerHTML = migrationPanel();
      return;
    }
    await refresh();
  }

  NC.accessControl = Object.freeze({ openOwnCredentials });
  NC.views['access-control'] = { render };
})(window.NC);
