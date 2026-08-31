(function (NC) {
  'use strict';

  const htmlEntities = Object.freeze({
    '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#039;'
  });

  function escapeHTML(value) {
    return String(value == null ? '' : value).replace(/[&<>"']/g, (char) => htmlEntities[char]);
  }

  function sanitizeHTML(value) {
    const html = String(value || '');
    if (window.DOMPurify) {
      return window.DOMPurify.sanitize(html, {
        USE_PROFILES: { html: true },
        ALLOWED_URI_REGEXP: /^(?:(?:https?|mailto|tel):|[^a-z]|[a-z+.-]+(?:[^a-z+.-:]|$))/i,
        ADD_ATTR: ['target', 'rel']
      });
    }
    // Conservative fallback when the CDN is unavailable: show plain text.
    return escapeHTML(html).replace(/\n/g, '<br>');
  }

  function stripHTML(value) {
    const node = document.createElement('div');
    node.innerHTML = sanitizeHTML(value);
    return (node.textContent || '').replace(/\s+/g, ' ').trim();
  }

  function truncate(value, maxLength = 90) {
    const text = String(value || '').trim();
    return text.length > maxLength ? `${text.slice(0, maxLength - 1).trim()}…` : text;
  }

  function qs(selector, root = document) {
    return root.querySelector(selector);
  }

  function qsa(selector, root = document) {
    return Array.from(root.querySelectorAll(selector));
  }

  function toDate(value) {
    if (!value) return null;
    const date = value instanceof Date ? value : new Date(value);
    return Number.isNaN(date.getTime()) ? null : date;
  }

  function formatDate(value, options = {}) {
    const date = toDate(value);
    if (!date) return '—';
    const defaults = { day: '2-digit', month: 'short', year: 'numeric' };
    return new Intl.DateTimeFormat(NC_CONFIG.app.locale, { ...defaults, ...options }).format(date);
  }

  function formatDateTime(value) {
    return formatDate(value, {
      day: '2-digit', month: 'short', year: 'numeric',
      hour: '2-digit', minute: '2-digit'
    });
  }

  function relativeTime(value) {
    const date = toDate(value);
    if (!date) return 'Unknown time';
    const delta = date.getTime() - Date.now();
    const absolute = Math.abs(delta);
    const units = [
      ['year', 31536000000], ['month', 2592000000], ['week', 604800000],
      ['day', 86400000], ['hour', 3600000], ['minute', 60000]
    ];
    const formatter = new Intl.RelativeTimeFormat(NC_CONFIG.app.locale, { numeric: 'auto' });
    for (const [unit, milliseconds] of units) {
      if (absolute >= milliseconds || unit === 'minute') {
        return formatter.format(Math.round(delta / milliseconds), unit);
      }
    }
    return 'just now';
  }

  function debounce(callback, wait = 250) {
    let timeout;
    return function debounced(...args) {
      window.clearTimeout(timeout);
      timeout = window.setTimeout(() => callback.apply(this, args), wait);
    };
  }

  function throttle(callback, wait = 200) {
    let waiting = false;
    let latestArgs;
    return function throttled(...args) {
      latestArgs = args;
      if (waiting) return;
      waiting = true;
      callback.apply(this, latestArgs);
      window.setTimeout(() => {
        waiting = false;
        if (latestArgs !== args) callback.apply(this, latestArgs);
      }, wait);
    };
  }

  function slugify(value) {
    return String(value || '')
      .normalize('NFKC')
      .trim()
      .toLocaleLowerCase()
      .replace(/[’'`]/g, '')
      .replace(/[^\p{Letter}\p{Mark}\p{Number}]+/gu, '-')
      .replace(/^-+|-+$/g, '')
      .replace(/-{2,}/g, '-');
  }

  function parseTags(value) {
    const text = Array.isArray(value) ? value.join(',') : String(value || '');
    const parts = text.includes(',')
      ? text.split(',')
      : text.split(/\s{2,}|[;|]/);
    return Array.from(new Set(parts.map((tag) => tag.trim().replace(/^#/, '')).filter(Boolean))).slice(0, 30);
  }

  function isValidUrl(value, { allowEmpty = true } = {}) {
    const text = String(value || '').trim();
    if (!text) return allowEmpty;
    try {
      const url = new URL(text);
      return ['http:', 'https:'].includes(url.protocol);
    } catch (_) {
      return false;
    }
  }

  function safeExternalUrl(value) {
    return isValidUrl(value, { allowEmpty: false }) ? String(value).trim() : '';
  }

  function safeImage(value) {
    const url = safeExternalUrl(value);
    return url || '';
  }

  function uuid() {
    if (window.crypto && typeof window.crypto.randomUUID === 'function') return window.crypto.randomUUID();
    return 'xxxxxxxx-xxxx-4xxx-yxxx-xxxxxxxxxxxx'.replace(/[xy]/g, (char) => {
      const random = Math.random() * 16 | 0;
      return (char === 'x' ? random : (random & 0x3 | 0x8)).toString(16);
    });
  }

  async function sha256(value) {
    if (!window.crypto?.subtle) throw new Error('Secure hashing is unavailable in this browser.');
    const data = new TextEncoder().encode(String(value));
    const digest = await window.crypto.subtle.digest('SHA-256', data);
    return Array.from(new Uint8Array(digest)).map((byte) => byte.toString(16).padStart(2, '0')).join('');
  }

  function initials(value, max = 2) {
    const text = String(value || '').trim();
    if (!text) return 'NC';
    return text.split(/\s+/).slice(0, max).map((word) => Array.from(word)[0]).join('').toUpperCase();
  }

  function avatarHTML(name, image, className = '') {
    const safe = safeImage(image);
    if (safe) {
      return `<img src="${escapeHTML(safe)}" alt="" class="${escapeHTML(className)} object-cover" loading="lazy" referrerpolicy="no-referrer" data-image-fallback>`;
    }
    return `<span class="${escapeHTML(className)} inline-grid place-items-center bg-brand-500/15 text-brand-500 font-semibold" aria-hidden="true">${escapeHTML(initials(name))}</span>`;
  }

  function bytes(value) {
    const size = Number(value) || 0;
    if (size < 1024) return `${size} B`;
    if (size < 1024 ** 2) return `${(size / 1024).toFixed(1)} KB`;
    return `${(size / 1024 ** 2).toFixed(1)} MB`;
  }

  function number(value) {
    return new Intl.NumberFormat(NC_CONFIG.app.locale).format(Number(value) || 0);
  }

  function readPreference(key, fallback) {
    try {
      const value = localStorage.getItem(`nc:${key}`);
      return value == null ? fallback : JSON.parse(value);
    } catch (_) {
      return fallback;
    }
  }

  function writePreference(key, value) {
    try {
      localStorage.setItem(`nc:${key}`, JSON.stringify(value));
    } catch (_) {
      // Storage can be disabled in privacy modes; the app remains usable.
    }
  }

  function setButtonLoading(button, loading, label) {
    if (!button) return;
    if (loading) {
      button.dataset.originalHtml = button.innerHTML;
      button.disabled = true;
      button.setAttribute('aria-busy', 'true');
      button.innerHTML = `<i class="fa-regular fa-spinner-third fa-spin" aria-hidden="true"></i><span>${escapeHTML(label || 'Working…')}</span>`;
    } else {
      button.disabled = false;
      button.removeAttribute('aria-busy');
      if (button.dataset.originalHtml) button.innerHTML = button.dataset.originalHtml;
      delete button.dataset.originalHtml;
    }
  }

  function formData(form) {
    const result = {};
    new FormData(form).forEach((value, key) => {
      result[key] = typeof value === 'string' ? value.trim() : value;
    });
    qsa('input[type="checkbox"][name]', form).forEach((input) => {
      result[input.name] = input.checked;
    });
    return result;
  }

  function validateFields(form, errors = {}) {
    qsa('[data-field-error]', form).forEach((node) => {
      node.textContent = '';
      node.classList.add('hidden');
    });
    qsa('[aria-invalid="true"]', form).forEach((field) => field.removeAttribute('aria-invalid'));
    let firstInvalid = null;
    Object.entries(errors).forEach(([name, message]) => {
      if (!message) return;
      const field = form.elements[name];
      const error = qs(`[data-field-error="${CSS.escape(name)}"]`, form);
      if (field) {
        field.setAttribute('aria-invalid', 'true');
        firstInvalid ||= field;
      }
      if (error) {
        error.textContent = message;
        error.classList.remove('hidden');
      }
    });
    firstInvalid?.focus();
    return !firstInvalid;
  }

  function groupMonthly(records, months = 6) {
    const now = new Date();
    const buckets = [];
    for (let offset = months - 1; offset >= 0; offset -= 1) {
      const date = new Date(now.getFullYear(), now.getMonth() - offset, 1);
      buckets.push({
        key: `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, '0')}`,
        label: new Intl.DateTimeFormat(NC_CONFIG.app.locale, { month: 'short' }).format(date),
        count: 0
      });
    }
    const map = new Map(buckets.map((bucket) => [bucket.key, bucket]));
    (records || []).forEach((record) => {
      const date = toDate(record.created_at);
      if (!date) return;
      const key = `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, '0')}`;
      if (map.has(key)) map.get(key).count += 1;
    });
    return buckets;
  }

  function sortRecords(records, key, direction = 'desc') {
    const multiplier = direction === 'asc' ? 1 : -1;
    return [...records].sort((a, b) => {
      const left = a?.[key] ?? '';
      const right = b?.[key] ?? '';
      if (key.includes('date') || key.endsWith('_at')) {
        return ((toDate(left)?.getTime() || 0) - (toDate(right)?.getTime() || 0)) * multiplier;
      }
      return String(left).localeCompare(String(right), NC_CONFIG.app.locale, { numeric: true }) * multiplier;
    });
  }

  function getHashRoute() {
    const raw = window.location.hash.replace(/^#\/?/, '') || NC_CONFIG.app.defaultRoute;
    const [route, query = ''] = raw.split('?');
    return { route, params: new URLSearchParams(query) };
  }

  function routeTo(route, params = {}) {
    const query = new URLSearchParams();
    Object.entries(params).forEach(([key, value]) => {
      if (value !== undefined && value !== null && value !== '') query.set(key, value);
    });
    window.location.hash = `#/${route}${query.toString() ? `?${query}` : ''}`;
  }

  function copyText(value) {
    if (navigator.clipboard?.writeText) return navigator.clipboard.writeText(String(value));
    const area = document.createElement('textarea');
    area.value = String(value);
    area.style.position = 'fixed';
    area.style.opacity = '0';
    document.body.appendChild(area);
    area.select();
    document.execCommand('copy');
    area.remove();
    return Promise.resolve();
  }

  NC.utils = Object.freeze({
    escapeHTML, sanitizeHTML, stripHTML, truncate, qs, qsa, toDate,
    formatDate, formatDateTime, relativeTime, debounce, throttle, slugify,
    parseTags, isValidUrl, safeExternalUrl, safeImage, uuid, sha256,
    initials, avatarHTML, bytes, number, readPreference, writePreference,
    setButtonLoading, formData, validateFields, groupMonthly, sortRecords,
    getHashRoute, routeTo, copyText
  });
})(window.NC);
