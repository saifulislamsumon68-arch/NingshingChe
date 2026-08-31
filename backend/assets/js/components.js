(function (NC) {
  'use strict';

  const { escapeHTML, qs, qsa } = NC.utils;
  let activeModal = null;
  let lastFocused = null;

  const toastIcons = Object.freeze({
    success: 'fa-circle-check', error: 'fa-circle-xmark',
    warning: 'fa-triangle-exclamation', info: 'fa-circle-info'
  });

  function toast(message, type = 'info', options = {}) {
    const container = qs('#toast-container');
    if (!container) return;
    const id = `toast-${NC.utils.uuid()}`;
    const item = document.createElement('div');
    item.id = id;
    item.className = `toast toast-${type}`;
    item.setAttribute('role', type === 'error' ? 'alert' : 'status');
    item.setAttribute('aria-live', type === 'error' ? 'assertive' : 'polite');
    item.innerHTML = `
      <span class="toast-icon"><i class="fa-solid ${toastIcons[type] || toastIcons.info}" aria-hidden="true"></i></span>
      <div class="min-w-0 flex-1">
        ${options.title ? `<p class="font-semibold text-sm">${escapeHTML(options.title)}</p>` : ''}
        <p class="text-sm leading-5">${escapeHTML(message)}</p>
        ${options.action?.url ? `<a class="toast-action" href="${escapeHTML(options.action.url)}" target="_blank" rel="noopener noreferrer">${escapeHTML(options.action.label || 'Open')}</a>` : ''}
      </div>
      <button type="button" class="toast-close" aria-label="Dismiss notification"><i class="fa-regular fa-xmark" aria-hidden="true"></i></button>`;
    container.appendChild(item);
    requestAnimationFrame(() => item.classList.add('is-visible'));
    const close = () => {
      item.classList.remove('is-visible');
      window.setTimeout(() => item.remove(), 180);
    };
    qs('.toast-close', item).addEventListener('click', close);
    window.setTimeout(close, options.duration || (type === 'error' ? 7000 : 4500));
    return { close, element: item };
  }

  function focusableElements(root) {
    return qsa('a[href], button:not([disabled]), input:not([disabled]), select:not([disabled]), textarea:not([disabled]), [tabindex]:not([tabindex="-1"])', root)
      .filter((node) => !node.hidden && node.offsetParent !== null);
  }

  function closeModal(reason = 'close') {
    if (!activeModal) return;
    const { root, onClose } = activeModal;
    activeModal = null;
    document.body.classList.remove('modal-open');
    root.classList.remove('is-open');
    window.setTimeout(() => root.remove(), 160);
    try { onClose?.(reason); } catch (error) { console.error(error); }
    lastFocused?.focus?.();
    lastFocused = null;
  }

  function openModal(options = {}) {
    if (activeModal) closeModal('replace');
    lastFocused = document.activeElement;
    const root = document.createElement('div');
    root.id = 'modal-root';
    root.className = 'modal-root';
    root.dataset.modalId = options.id || 'current';
    root.innerHTML = `
      <div class="modal-backdrop" ${options.dismissible === false ? '' : 'data-modal-close'} aria-hidden="true"></div>
      <section class="modal-panel modal-${escapeHTML(options.size || 'md')}" role="dialog" aria-modal="true" aria-labelledby="modal-title-${escapeHTML(options.id || 'current')}">
        <div class="modal-header">
          <div class="min-w-0">
            ${options.eyebrow ? `<p class="eyebrow mb-1">${escapeHTML(options.eyebrow)}</p>` : ''}
            <h2 id="modal-title-${escapeHTML(options.id || 'current')}" class="modal-title">${escapeHTML(options.title || '')}</h2>
            ${options.description ? `<p class="modal-description">${escapeHTML(options.description)}</p>` : ''}
          </div>
          ${options.dismissible === false ? '' : '<button type="button" class="icon-button shrink-0" data-modal-close aria-label="Close dialog"><i class="fa-regular fa-xmark" aria-hidden="true"></i></button>'}
        </div>
        <div class="modal-body">${options.content || ''}</div>
        ${options.footer === false ? '' : `<div class="modal-footer">${options.footer || '<button type="button" class="btn btn-secondary" data-modal-close>Close</button>'}</div>`}
      </section>`;
    document.body.appendChild(root);
    document.body.classList.add('modal-open');
    activeModal = { root, onClose: options.onClose };

    root.addEventListener('click', (event) => {
      if (event.target.closest('[data-modal-close]')) closeModal('dismiss');
    });
    root.addEventListener('keydown', (event) => {
      if (event.key === 'Escape' && options.dismissible !== false) {
        event.preventDefault();
        closeModal('escape');
      }
      if (event.key === 'Tab') {
        const nodes = focusableElements(root);
        if (!nodes.length) return;
        const first = nodes[0];
        const last = nodes[nodes.length - 1];
        if (event.shiftKey && document.activeElement === first) {
          event.preventDefault(); last.focus();
        } else if (!event.shiftKey && document.activeElement === last) {
          event.preventDefault(); first.focus();
        }
      }
    });
    requestAnimationFrame(() => {
      root.classList.add('is-open');
      (qs('[autofocus]', root) || focusableElements(root)[0] || qs('.modal-panel', root))?.focus?.();
      options.onOpen?.(root);
    });
    return { element: root, close: closeModal, body: qs('.modal-body', root), footer: qs('.modal-footer', root) };
  }

  function confirm(options = {}) {
    return new Promise((resolve) => {
      let settled = false;
      const finish = (value) => {
        if (settled) return;
        settled = true;
        resolve(value);
        closeModal(value ? 'confirm' : 'cancel');
      };
      const modal = openModal({
        id: 'confirm',
        size: 'sm',
        eyebrow: options.eyebrow || 'Please confirm',
        title: options.title || 'Are you sure?',
        description: options.description || '',
        content: `
          <div class="confirm-visual ${options.danger === false ? 'confirm-neutral' : 'confirm-danger'}">
            <i class="fa-solid ${options.icon || (options.danger === false ? 'fa-circle-question' : 'fa-triangle-exclamation')}" aria-hidden="true"></i>
          </div>
          ${options.details ? `<div class="notice notice-muted mt-5">${escapeHTML(options.details)}</div>` : ''}`,
        footer: `
          <button type="button" class="btn btn-secondary" data-confirm-cancel>${escapeHTML(options.cancelLabel || 'Cancel')}</button>
          <button type="button" class="btn ${options.danger === false ? 'btn-primary' : 'btn-danger'}" data-confirm-accept>
            <i class="fa-solid ${options.confirmIcon || (options.danger === false ? 'fa-check' : 'fa-trash')}" aria-hidden="true"></i>
            ${escapeHTML(options.confirmLabel || 'Confirm')}
          </button>`,
        onClose: () => { if (!settled) { settled = true; resolve(false); } }
      });
      qs('[data-confirm-cancel]', modal.element).addEventListener('click', () => finish(false));
      qs('[data-confirm-accept]', modal.element).addEventListener('click', () => finish(true));
    });
  }

  function pageHeader({ eyebrow = 'Workspace', title, description = '', actions = '', breadcrumb = [] }) {
    const crumbs = [{ label: 'Dashboard', route: 'dashboard' }, ...breadcrumb];
    return `
      <div class="page-heading">
        <div class="min-w-0">
          <nav class="breadcrumbs" aria-label="Breadcrumb">
            ${crumbs.map((item, index) => `
              ${index ? '<i class="fa-regular fa-chevron-right" aria-hidden="true"></i>' : ''}
              ${item.route ? `<a href="#/${escapeHTML(item.route)}">${escapeHTML(item.label)}</a>` : `<span aria-current="page">${escapeHTML(item.label)}</span>`}
            `).join('')}
          </nav>
          <p class="eyebrow">${escapeHTML(eyebrow)}</p>
          <h1 class="page-title">${escapeHTML(title)}</h1>
          ${description ? `<p class="page-description">${escapeHTML(description)}</p>` : ''}
        </div>
        ${actions ? `<div class="page-actions">${actions}</div>` : ''}
      </div>`;
  }

  function statusBadge(status) {
    const value = String(status || 'Unknown');
    const normalized = value.toLowerCase();
    let tone = 'neutral';
    let icon = 'fa-circle';
    if (['publish', 'published', 'approved', 'active', 'verified'].includes(normalized)) { tone = 'success'; icon = 'fa-circle-check'; }
    else if (['pending', 'draft', 'reviewed'].includes(normalized)) { tone = 'warning'; icon = 'fa-clock'; }
    else if (['rejected', 'unpublish', 'unpublished', 'failed'].includes(normalized)) { tone = 'danger'; icon = 'fa-circle-xmark'; }
    else if (['slider', 'feature', 'special'].includes(normalized)) { tone = 'info'; icon = 'fa-sparkles'; }
    return `<span class="status-badge status-${tone}"><i class="fa-solid ${icon}" aria-hidden="true"></i>${escapeHTML(value)}</span>`;
  }

  function skeleton(rows = 5, columns = 4) {
    return `
      <div class="surface overflow-hidden" role="status" aria-label="Loading content">
        <div class="skeleton-table">
          ${Array.from({ length: rows }, (_, row) => `
            <div class="skeleton-row" style="--cols:${columns}">
              ${Array.from({ length: columns }, (_, column) => `<span class="skeleton-line ${column === 0 ? 'w-3/4' : ''}"></span>`).join('')}
            </div>`).join('')}
        </div>
        <span class="sr-only">Loading…</span>
      </div>`;
  }

  function emptyState({ icon = 'fa-inbox', title = 'Nothing here yet', description = '', action = '' } = {}) {
    return `
      <div class="empty-state">
        <span class="empty-icon"><i class="fa-duotone fa-solid ${escapeHTML(icon)}" aria-hidden="true"></i></span>
        <h3>${escapeHTML(title)}</h3>
        ${description ? `<p>${escapeHTML(description)}</p>` : ''}
        ${action ? `<div class="mt-5">${action}</div>` : ''}
      </div>`;
  }

  function errorState(error, options = {}) {
    const message = NC.api?.userMessage?.(error, options.message) || options.message || 'Unable to load this section.';
    return `
      <div class="error-state" role="alert">
        <span class="error-icon"><i class="fa-solid fa-cloud-exclamation" aria-hidden="true"></i></span>
        <div class="min-w-0">
          <h3>${escapeHTML(options.title || 'Something went wrong')}</h3>
          <p>${escapeHTML(message)}</p>
          <div class="mt-4 flex flex-wrap gap-2">
            ${options.retry !== false ? '<button type="button" class="btn btn-secondary btn-sm" data-retry><i class="fa-regular fa-rotate-right" aria-hidden="true"></i>Try again</button>' : ''}
            ${error?.isSchemaMissing || error?.isSchemaMismatch ? '<a class="btn btn-secondary btn-sm" href="README.md#database-setup" target="_blank"><i class="fa-regular fa-book-open" aria-hidden="true"></i>Setup guide</a>' : ''}
          </div>
        </div>
      </div>`;
  }

  function pagination({ page = 1, pageSize = 10, total = 0 } = {}) {
    const pages = Math.max(1, Math.ceil(total / pageSize));
    const safePage = Math.min(Math.max(1, page), pages);
    const start = total ? (safePage - 1) * pageSize + 1 : 0;
    const end = Math.min(safePage * pageSize, total);
    const candidates = Array.from(new Set([1, safePage - 1, safePage, safePage + 1, pages]))
      .filter((value) => value >= 1 && value <= pages).sort((a, b) => a - b);
    let previous = 0;
    const buttons = candidates.map((value) => {
      const gap = value - previous > 1 ? '<span class="pagination-gap">…</span>' : '';
      previous = value;
      return `${gap}<button type="button" class="pagination-page ${value === safePage ? 'is-active' : ''}" data-page="${value}" ${value === safePage ? 'aria-current="page"' : ''}>${value}</button>`;
    }).join('');
    return `
      <div class="pagination" data-pagination>
        <p class="pagination-summary">Showing <strong>${start}</strong>–<strong>${end}</strong> of <strong>${total}</strong></p>
        <div class="pagination-controls" aria-label="Pagination">
          <button type="button" class="pagination-page" data-page="${safePage - 1}" ${safePage <= 1 ? 'disabled' : ''} aria-label="Previous page"><i class="fa-regular fa-chevron-left" aria-hidden="true"></i></button>
          ${buttons}
          <button type="button" class="pagination-page" data-page="${safePage + 1}" ${safePage >= pages ? 'disabled' : ''} aria-label="Next page"><i class="fa-regular fa-chevron-right" aria-hidden="true"></i></button>
        </div>
      </div>`;
  }

  function tableShell({ head = '', body = '', caption = '', minWidth = '760px' } = {}) {
    return `
      <div class="table-shell">
        <div class="table-scroll">
          <table class="data-table" style="min-width:${escapeHTML(minWidth)}">
            ${caption ? `<caption class="sr-only">${escapeHTML(caption)}</caption>` : ''}
            <thead>${head}</thead>
            <tbody>${body}</tbody>
          </table>
        </div>
      </div>`;
  }

  function rowActions(actions = []) {
    return `<div class="row-actions">${actions.map((action) => `
      <button type="button" class="row-action ${action.danger ? 'row-action-danger' : ''}" data-action="${escapeHTML(action.action)}" data-id="${escapeHTML(action.id)}" aria-label="${escapeHTML(action.label)}" title="${escapeHTML(action.label)}">
        <i class="fa-regular ${escapeHTML(action.icon)}" aria-hidden="true"></i>
      </button>`).join('')}</div>`;
  }

  function notice(message, tone = 'info', icon = '') {
    const icons = { info: 'fa-circle-info', warning: 'fa-triangle-exclamation', success: 'fa-circle-check', danger: 'fa-shield-exclamation' };
    return `<div class="notice notice-${escapeHTML(tone)}"><i class="fa-solid ${escapeHTML(icon || icons[tone] || icons.info)}" aria-hidden="true"></i><p>${escapeHTML(message)}</p></div>`;
  }

  function remoteDeleteWarning(result, message = 'The database record was removed, but ImgBB did not confirm remote image deletion. Use the saved deletion page to finish manually.') {
    if (!result || result.ok || result.skipped) return;
    toast(message, 'warning', {
      duration: 10000,
      action: result.deleteUrl ? { label: 'Open ImgBB deletion page', url: result.deleteUrl } : null
    });
  }

  function imagePreviewModal({ title = 'Preview', image = '', description = '' }) {
    const url = NC.utils.safeImage(image);
    return openModal({
      title,
      size: 'lg',
      content: `
        ${url ? `<img src="${escapeHTML(url)}" alt="${escapeHTML(title)}" class="preview-image" referrerpolicy="no-referrer">` : emptyState({ icon: 'fa-image-slash', title: 'No image', description: 'This record does not have an image.' })}
        ${description ? `<div class="prose-content mt-5">${NC.utils.sanitizeHTML(description)}</div>` : ''}`,
      footer: `${url ? `<a class="btn btn-secondary" href="${escapeHTML(url)}" target="_blank" rel="noopener noreferrer"><i class="fa-regular fa-arrow-up-right-from-square" aria-hidden="true"></i>Open original</a>` : ''}<button type="button" class="btn btn-primary" data-modal-close>Done</button>`
    });
  }

  function bindImageFallbacks(root = document) {
    qsa('[data-image-fallback]', root).forEach((image) => {
      image.addEventListener('error', () => {
        const replacement = document.createElement('span');
        replacement.className = `${image.className} inline-grid place-items-center bg-muted text-muted-foreground`;
        replacement.innerHTML = '<i class="fa-regular fa-image-slash" aria-hidden="true"></i>';
        replacement.setAttribute('aria-label', 'Image unavailable');
        image.replaceWith(replacement);
      }, { once: true });
    });
  }

  NC.components = Object.freeze({
    toast, openModal, closeModal, confirm, pageHeader, statusBadge,
    skeleton, emptyState, errorState, pagination, tableShell, rowActions,
    notice, remoteDeleteWarning, imagePreviewModal, bindImageFallbacks
  });
})(window.NC);
