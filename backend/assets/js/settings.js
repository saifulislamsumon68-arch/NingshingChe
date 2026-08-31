(function (NC) {
  'use strict';

  const { escapeHTML, formData, validateFields } = NC.utils;
  let root;
  let current = null;

  const defaults = Object.freeze({
    id: 'site_settings', site_title: 'Ningshing Che', site_description: 'Bishnupriya Manipuri Magazine',
    logo_url: '', favicon_url: '', default_seo_title: '', default_seo_description: '',
    contact_email: '', contact_phone: '', facebook_url: '', youtube_url: '', instagram_url: '',
    hero_slider_enabled: true, featured_articles_enabled: true, special_articles_enabled: true,
    allow_comments: true, allow_user_submissions: true
  });

  function field(name, label, type = 'text', placeholder = '') {
    return `<div class="field"><label class="field-label" for="settings-${name}">${escapeHTML(label)}</label><input class="form-input" type="${escapeHTML(type)}" id="settings-${name}" name="${escapeHTML(name)}" value="${escapeHTML(current?.[name] || '')}" placeholder="${escapeHTML(placeholder)}"><p class="field-error hidden" data-field-error="${escapeHTML(name)}"></p></div>`;
  }

  function securityContent() {
    const secure = !NC.auth.isLegacy();
    return `${NC.components.notice(
      secure
        ? 'Role-based login is active. Supabase validates hashed sessions and enforces menu permissions on protected data changes.'
        : 'Compatibility login is active. Run backend/supabase/migrations/004_dashboard_access_control.sql to install secure users, roles, and sessions.',
      secure ? 'success' : 'warning', secure ? 'fa-shield-check' : 'fa-shield-exclamation'
    )}
      <div class="security-facts mt-5">
        <div><i class="fa-solid fa-check" aria-hidden="true"></i><span><strong>Publishable key only</strong><small>No service-role key is included in browser code.</small></span></div>
        <div><i class="fa-solid fa-check" aria-hidden="true"></i><span><strong>${secure ? 'Hashed server sessions' : 'Migration-aware fallback'}</strong><small>${secure ? 'Raw tokens and passwords are never stored in database-readable form.' : 'Legacy login is automatically retired after migration 004.'}</small></span></div>
        <div><i class="fa-solid fa-check" aria-hidden="true"></i><span><strong>Sanitized rich text</strong><small>DOMPurify cleans article HTML before rendering and save.</small></span></div>
      </div>
      <div class="button-row mt-5"><button type="button" class="btn btn-secondary btn-sm" data-settings-account><i class="fa-regular fa-key" aria-hidden="true"></i>Update my login</button>${NC.auth.isSuperAdmin() ? '<a href="#/access-control" class="btn btn-secondary btn-sm"><i class="fa-regular fa-user-shield" aria-hidden="true"></i>Manage users & roles</a>' : ''}</div>`;
  }

  function renderSettings() {
    const theme = document.documentElement.classList.contains('dark') ? 'dark' : 'light';
    const landing = NC.utils.readPreference('landing-page', NC_CONFIG.app.defaultRoute);
    const pageSize = Number(NC.utils.readPreference('items-per-page', NC_CONFIG.app.defaultPageSize));
    const density = NC.utils.readPreference('table-density', 'comfortable');
    root.innerHTML = `
      ${NC.components.pageHeader({ eyebrow: 'System', title: 'Settings', description: 'Control appearance, editorial preferences, and public website configuration.', breadcrumb: [{ label: 'Settings' }], actions: '<button type="submit" form="settings-form" class="btn btn-primary" data-save-settings><i class="fa-regular fa-floppy-disk" aria-hidden="true"></i>Save settings</button>' })}
      <form id="settings-form" class="settings-layout" novalidate>
        <nav class="settings-nav" aria-label="Settings sections"><a href="#settings-appearance"><i class="fa-regular fa-palette" aria-hidden="true"></i>Appearance</a><a href="#settings-dashboard"><i class="fa-regular fa-gauge-high" aria-hidden="true"></i>Dashboard</a><a href="#settings-site"><i class="fa-regular fa-globe" aria-hidden="true"></i>Frontend</a><a href="#settings-features"><i class="fa-regular fa-toggle-on" aria-hidden="true"></i>Features</a><a href="#settings-security"><i class="fa-regular fa-shield-check" aria-hidden="true"></i>Security</a></nav>
        <div class="settings-content">
          <section class="surface settings-section" id="settings-appearance"><div class="surface-header"><div><p class="eyebrow">Personal preference</p><h2>Appearance</h2><p>Choose the dashboard color scheme. Your selection is saved in this browser.</p></div></div><div class="theme-options" role="radiogroup" aria-label="Theme"><label class="theme-option ${theme === 'dark' ? 'is-selected' : ''}"><input type="radio" name="theme" value="dark" ${theme === 'dark' ? 'checked' : ''}><span class="theme-preview theme-preview-dark"><i></i><i></i><i></i></span><span><strong>Dark</strong><small>Default editorial workspace</small></span><i class="fa-solid fa-circle-check theme-check" aria-hidden="true"></i></label><label class="theme-option ${theme === 'light' ? 'is-selected' : ''}"><input type="radio" name="theme" value="light" ${theme === 'light' ? 'checked' : ''}><span class="theme-preview theme-preview-light"><i></i><i></i><i></i></span><span><strong>Light</strong><small>Bright, high-clarity workspace</small></span><i class="fa-solid fa-circle-check theme-check" aria-hidden="true"></i></label></div></section>
          <section class="surface settings-section" id="settings-dashboard"><div class="surface-header"><div><p class="eyebrow">Workspace behavior</p><h2>Dashboard preferences</h2></div></div><div class="form-grid-2"><div class="field"><label class="field-label" for="settings-landing">Default landing page</label><select class="form-select" id="settings-landing" name="landing_page">${NC_CONFIG.routes.filter((item) => item.id !== 'settings' && NC.auth.canAccess(item.id)).map((item) => `<option value="${item.id}" ${item.id === landing ? 'selected' : ''}>${escapeHTML(item.label)}</option>`).join('')}</select></div><div class="field"><label class="field-label" for="settings-page-size">Items per page</label><select class="form-select" id="settings-page-size" name="page_size">${[10, 20, 30, 50].map((size) => `<option value="${size}" ${size === pageSize ? 'selected' : ''}>${size} items</option>`).join('')}</select></div></div><fieldset class="field mt-5"><legend class="field-label">Table density</legend><div class="segmented-control"><label><input type="radio" name="density" value="comfortable" ${density !== 'compact' ? 'checked' : ''}><span>Comfortable</span></label><label><input type="radio" name="density" value="compact" ${density === 'compact' ? 'checked' : ''}><span>Compact</span></label></div></fieldset></section>
          <section class="surface settings-section" id="settings-site"><div class="surface-header"><div><p class="eyebrow">Public site</p><h2>Frontend configuration</h2><p>Values saved here can later be consumed by ningshingche.com.</p></div></div><div class="form-stack"><div class="form-grid-2">${field('site_title', 'Site title')}${field('contact_email', 'Contact email', 'email')}</div><div class="field"><label class="field-label" for="settings-site_description">Site description</label><textarea class="form-textarea" id="settings-site_description" name="site_description" rows="3">${escapeHTML(current?.site_description || '')}</textarea></div><div class="form-grid-2">${field('logo_url', 'Logo URL', 'url', 'https://…')}${field('favicon_url', 'Favicon URL', 'url', 'https://…')}</div><div class="form-grid-2">${field('default_seo_title', 'Default SEO title')}<div class="field"><label class="field-label" for="settings-default_seo_description">Default SEO description</label><textarea class="form-textarea" id="settings-default_seo_description" name="default_seo_description" rows="3">${escapeHTML(current?.default_seo_description || '')}</textarea></div></div><div class="form-grid-2">${field('contact_phone', 'Contact phone', 'tel')}${field('facebook_url', 'Facebook URL', 'url', 'https://facebook.com/…')}</div><div class="form-grid-2">${field('youtube_url', 'YouTube URL', 'url', 'https://youtube.com/…')}${field('instagram_url', 'Instagram URL', 'url', 'https://instagram.com/…')}</div></div></section>
          <section class="surface settings-section" id="settings-features"><div class="surface-header"><div><p class="eyebrow">Public features</p><h2>Content controls</h2></div></div><div class="settings-toggles">${[
            ['hero_slider_enabled', 'Hero slider', 'Allow slider articles on the public home page.'],
            ['featured_articles_enabled', 'Featured articles', 'Show editor-selected featured articles.'],
            ['special_articles_enabled', 'Special articles', 'Enable the special editorial section.'],
            ['allow_comments', 'Reader comments', 'Allow the public site to submit comments for moderation.'],
            ['allow_user_submissions', 'Public submissions', 'Allow readers to send articles for review.']
          ].map(([name, label, description]) => `<label class="switch-row"><span><strong>${escapeHTML(label)}</strong><small>${escapeHTML(description)}</small></span><span class="switch"><input type="checkbox" name="${name}" ${current?.[name] !== false ? 'checked' : ''}><span></span></span></label>`).join('')}</div></section>
          <section class="surface settings-section" id="settings-security"><div class="surface-header"><div><p class="eyebrow">Security posture</p><h2>Authentication & database</h2></div><button type="button" class="btn btn-secondary btn-sm" data-check-schema><i class="fa-regular fa-stethoscope" aria-hidden="true"></i>Run check</button></div>${securityContent()}<div class="diagnostics mt-5" data-schema-diagnostics><p class="text-sm text-muted-foreground">Run the database check to verify required tables and columns.</p></div></section>
          <div class="settings-save-bar"><p><i class="fa-regular fa-cloud" aria-hidden="true"></i>Site configuration is saved to Supabase; preferences stay in this browser.</p><button type="submit" class="btn btn-primary" data-save-settings><i class="fa-regular fa-floppy-disk" aria-hidden="true"></i>Save settings</button></div>
        </div>
      </form>`;
    bindEvents();
  }

  function bindEvents() {
    const form = root.querySelector('#settings-form');
    form.querySelectorAll('input[name="theme"]').forEach((input) => input.addEventListener('change', () => {
      NC.app?.setTheme?.(input.value);
      form.querySelectorAll('.theme-option').forEach((option) => option.classList.toggle('is-selected', option.contains(input)));
    }));
    form.querySelectorAll('input[name="density"]').forEach((input) => input.addEventListener('change', () => NC.app?.setDensity?.(input.value)));
    root.querySelector('[data-check-schema]').addEventListener('click', checkSchema);
    root.querySelector('[data-settings-account]')?.addEventListener('click', () => NC.accessControl?.openOwnCredentials?.());
    form.addEventListener('submit', save);
  }

  async function checkSchema() {
    const button = root.querySelector('[data-check-schema]'), output = root.querySelector('[data-schema-diagnostics]');
    NC.utils.setButtonLoading(button, true, 'Checking…'); output.innerHTML = NC.components.skeleton(3, 3);
    try {
      const probe = await NC.api.schemaProbe();
      output.innerHTML = `<div class="diagnostic-grid">${probe.results.map((item) => `<div class="diagnostic-item"><span class="diagnostic-dot ${item.ok ? 'is-ok' : 'is-error'}"></span><code>${escapeHTML(item.table)}</code><span>${item.ok ? 'Ready' : item.error?.isAccessControlMissing ? 'Migration 004 needed' : item.error?.isSchemaMissing ? 'Missing' : item.error?.isSchemaMismatch ? 'Migration needed' : `Error ${item.error?.status || ''}`}</span></div>`).join('')}</div>${probe.ok ? NC.components.notice('All required Supabase tables, media columns, and access-control functions are ready.', 'success') : NC.components.notice(probe.missing.length ? 'One or more tables are unavailable. Run backend/supabase/schema.sql, then migrations 003 and 004.' : probe.mismatched.length ? 'Blog media columns are unavailable. Run backend/supabase/migrations/003_blog_media_uploads.sql.' : 'Secure login management is unavailable. Run backend/supabase/migrations/004_dashboard_access_control.sql.', 'warning')}`;
    } catch (error) { output.innerHTML = NC.components.errorState(error, { retry: false }); }
    finally { NC.utils.setButtonLoading(button, false); }
  }

  async function save(event) {
    event.preventDefault(); const form = event.currentTarget, data = formData(form);
    const urlFields = ['logo_url', 'favicon_url', 'facebook_url', 'youtube_url', 'instagram_url'];
    const errors = {
      contact_email: data.contact_email && !/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(data.contact_email) ? 'Enter a valid contact email.' : ''
    };
    urlFields.forEach((name) => { errors[name] = data[name] && !NC.utils.isValidUrl(data[name], { allowEmpty: false }) ? 'Enter a complete http:// or https:// URL.' : ''; });
    if (!validateFields(form, errors)) return;
    root.querySelectorAll('[data-save-settings]').forEach((button) => NC.utils.setButtonLoading(button, true, 'Saving…'));
    try {
      NC.utils.writePreference('landing-page', data.landing_page);
      NC.utils.writePreference('items-per-page', Number(data.page_size));
      NC.utils.writePreference('table-density', data.density);
      NC.app?.setDensity?.(data.density);
      NC.app?.setTheme?.(data.theme);
      const payload = {
        id: 'site_settings', site_title: data.site_title, site_description: data.site_description,
        logo_url: data.logo_url, favicon_url: data.favicon_url, default_seo_title: data.default_seo_title,
        default_seo_description: data.default_seo_description, contact_email: data.contact_email,
        contact_phone: data.contact_phone, facebook_url: data.facebook_url, youtube_url: data.youtube_url,
        instagram_url: data.instagram_url, hero_slider_enabled: Boolean(data.hero_slider_enabled),
        featured_articles_enabled: Boolean(data.featured_articles_enabled), special_articles_enabled: Boolean(data.special_articles_enabled),
        allow_comments: Boolean(data.allow_comments), allow_user_submissions: Boolean(data.allow_user_submissions)
      };
      current = await NC.api.upsert('settings', payload, 'id');
      NC.components.toast('Settings saved successfully.', 'success');
    } catch (error) { console.error(error); NC.components.toast(NC.api.userMessage(error, 'Unable to save settings.'), 'error'); }
    finally { root.querySelectorAll('[data-save-settings]').forEach((button) => NC.utils.setButtonLoading(button, false)); }
  }

  async function render(container, context = {}) {
    root = container;
    root.innerHTML = `${NC.components.pageHeader({ eyebrow: 'System', title: 'Settings', description: 'Loading configuration…', breadcrumb: [{ label: 'Settings' }] })}${NC.components.skeleton(8, 2)}`;
    try {
      const result = await NC.api.list('settings', { select: '*', filters: { id: 'site_settings' }, limit: 1 });
      if (NC.crud.isStaleNavigation(context)) return;
      current = { ...defaults, ...(result.data[0] || {}) };
    } catch (error) {
      if (NC.crud.isStaleNavigation(context)) return;
      console.error(error); current = { ...defaults };
      NC.components.toast(NC.api.userMessage(error, 'Using local defaults because site settings could not be loaded.'), 'warning');
    }
    renderSettings();
  }

  NC.views.settings = { render };
})(window.NC);
