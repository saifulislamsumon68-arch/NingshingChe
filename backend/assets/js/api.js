(function (NC) {
  'use strict';

  const { supabase, tables } = NC_CONFIG;
  const restBase = `${supabase.url.replace(/\/$/, '')}${supabase.restPath}`;
  const storageBase = `${supabase.url.replace(/\/$/, '')}${supabase.storagePath}`;

  class ApiError extends Error {
    constructor(message, options = {}) {
      super(message);
      this.name = 'ApiError';
      this.status = options.status || 0;
      this.code = options.code || '';
      this.details = options.details || '';
      this.hint = options.hint || '';
      this.body = options.body;
      this.isSchemaMissing = this.status === 404 && (
        this.code === 'PGRST205' || /schema cache|could not find the table/i.test(`${message} ${this.details}`)
      );
      this.isSchemaMismatch = ['PGRST204', '42703'].includes(this.code) || /could not find.+column|column.+(?:schema cache|does not exist)/i.test(`${message} ${this.details}`);
      this.isRpcMissing = this.code === 'PGRST202' || (
        this.status === 404 && /function.+schema cache|could not find the function/i.test(`${message} ${this.details}`)
      );
    }
  }

  function authHeaders() {
    const accessToken = NC.auth?.getAccessToken?.();
    const dashboardToken = NC.auth?.getDashboardToken?.();
    const dashboardSession = NC.auth?.getSessionToken?.();
    return {
      apikey: supabase.publishableKey,
      Authorization: `Bearer ${accessToken || supabase.publishableKey}`,
      ...(dashboardSession ? { 'x-dashboard-session': dashboardSession } : {}),
      ...(dashboardToken ? { 'x-dashboard-token': dashboardToken } : {})
    };
  }

  function parseResponseBody(response, text) {
    if (!text) return null;
    const contentType = response.headers.get('content-type') || '';
    if (contentType.includes('json')) {
      try { return JSON.parse(text); } catch (_) { return text; }
    }
    return text;
  }

  async function request(url, options = {}) {
    const controller = new AbortController();
    const timeout = window.setTimeout(() => controller.abort(), options.timeout || NC_CONFIG.app.requestTimeoutMs);
    const headers = {
      Accept: 'application/json',
      ...authHeaders(),
      ...(options.body && !(options.body instanceof FormData) && !(options.body instanceof Blob)
        ? { 'Content-Type': 'application/json' }
        : {}),
      ...(options.headers || {})
    };

    try {
      const response = await fetch(url, {
        method: options.method || 'GET',
        headers,
        body: options.body,
        signal: controller.signal,
        cache: options.cache || 'no-store'
      });
      const text = options.method === 'HEAD' ? '' : await response.text();
      const body = parseResponseBody(response, text);
      if (!response.ok) {
        const source = body && typeof body === 'object' ? body : {};
        throw new ApiError(
          source.message || source.error_description || `Request failed with status ${response.status}.`,
          {
            status: response.status,
            code: source.code,
            details: source.details || (typeof body === 'string' ? body : ''),
            hint: source.hint,
            body
          }
        );
      }
      return { data: body, response };
    } catch (error) {
      if (error.name === 'AbortError') {
        throw new ApiError('The request timed out. Please check your connection and try again.', { code: 'TIMEOUT' });
      }
      if (error instanceof ApiError) throw error;
      throw new ApiError('Unable to reach Supabase. Check your internet connection and configuration.', {
        code: 'NETWORK_ERROR', details: error.message
      });
    } finally {
      window.clearTimeout(timeout);
    }
  }

  function tableName(keyOrName) {
    return tables[keyOrName] || keyOrName;
  }

  function filterExpression(value) {
    if (value && typeof value === 'object' && !Array.isArray(value)) {
      const op = value.op || 'eq';
      const raw = value.value;
      if (op === 'in') return `in.(${(raw || []).map((item) => String(item).replace(/[(),]/g, '')).join(',')})`;
      if (op === 'is') return `is.${raw}`;
      if (op === 'cs') return `cs.{${(raw || []).join(',')}}`;
      return `${op}.${raw}`;
    }
    return `eq.${value}`;
  }

  function buildListUrl(keyOrName, options = {}) {
    const params = new URLSearchParams();
    params.set('select', options.select || '*');
    if (options.order) params.set('order', options.order);
    if (Number.isFinite(options.limit)) params.set('limit', String(options.limit));
    if (Number.isFinite(options.offset) && options.offset > 0) params.set('offset', String(options.offset));
    Object.entries(options.filters || {}).forEach(([key, value]) => {
      if (value !== undefined && value !== null && value !== '') params.set(key, filterExpression(value));
    });
    if (options.or) params.set('or', `(${options.or})`);
    return `${restBase}/${tableName(keyOrName)}?${params}`;
  }

  async function list(keyOrName, options = {}) {
    const { data, response } = await request(buildListUrl(keyOrName, options), {
      headers: options.count ? { Prefer: 'count=exact' } : undefined
    });
    const contentRange = response.headers.get('content-range') || '';
    const match = contentRange.match(/\/(\d+|\*)$/);
    return {
      data: Array.isArray(data) ? data : [],
      count: match && match[1] !== '*' ? Number(match[1]) : (Array.isArray(data) ? data.length : 0)
    };
  }

  async function getById(keyOrName, id, select = '*') {
    const result = await list(keyOrName, {
      select,
      filters: { id },
      limit: 1
    });
    return result.data[0] || null;
  }

  async function count(keyOrName, filters = {}) {
    const params = new URLSearchParams({ select: 'id', limit: '1' });
    Object.entries(filters).forEach(([key, value]) => params.set(key, filterExpression(value)));
    const { response } = await request(`${restBase}/${tableName(keyOrName)}?${params}`, {
      headers: { Prefer: 'count=exact', Range: '0-0' }
    });
    const range = response.headers.get('content-range') || '*/0';
    const match = range.match(/\/(\d+)$/);
    return match ? Number(match[1]) : 0;
  }

  function cleanPayload(payload) {
    return Object.fromEntries(Object.entries(payload || {}).filter(([, value]) => value !== undefined));
  }

  async function insert(keyOrName, payload) {
    const { data } = await request(`${restBase}/${tableName(keyOrName)}`, {
      method: 'POST',
      headers: { Prefer: 'return=representation' },
      body: JSON.stringify(cleanPayload(payload))
    });
    return Array.isArray(data) ? data[0] : data;
  }

  async function update(keyOrName, id, payload) {
    const params = new URLSearchParams({ id: `eq.${id}` });
    const { data } = await request(`${restBase}/${tableName(keyOrName)}?${params}`, {
      method: 'PATCH',
      headers: { Prefer: 'return=representation' },
      body: JSON.stringify(cleanPayload(payload))
    });
    return Array.isArray(data) ? data[0] : data;
  }

  async function upsert(keyOrName, payload, conflict = 'id') {
    const params = new URLSearchParams({ on_conflict: conflict });
    const { data } = await request(`${restBase}/${tableName(keyOrName)}?${params}`, {
      method: 'POST',
      headers: { Prefer: 'resolution=merge-duplicates,return=representation' },
      body: JSON.stringify(cleanPayload(payload))
    });
    return Array.isArray(data) ? data[0] : data;
  }

  async function remove(keyOrName, id) {
    const params = new URLSearchParams({ id: `eq.${id}` });
    await request(`${restBase}/${tableName(keyOrName)}?${params}`, {
      method: 'DELETE', headers: { Prefer: 'return=minimal' }
    });
    return true;
  }

  async function rpc(functionName, payload = {}) {
    const { data } = await request(`${restBase}/rpc/${functionName}`, {
      method: 'POST',
      headers: { Prefer: 'return=representation' },
      body: JSON.stringify(cleanPayload(payload))
    });
    return data;
  }

  async function slugExists(slug, excludeId = '') {
    const filters = { slug: { op: 'eq', value: slug } };
    if (excludeId) filters.id = { op: 'neq', value: excludeId };
    return (await count('blogs', filters)) > 0;
  }

  function escapeSearchTerm(value) {
    return String(value || '').trim().replace(/[,*()]/g, ' ').replace(/\s+/g, ' ').slice(0, 80);
  }

  async function searchAll(query) {
    const term = escapeSearchTerm(query);
    if (term.length < 2) return [];
    const definitions = [
      ['blogs', 'Blogs', 'newspaper', ['title', 'sub_title', 'slug']],
      ['authors', 'Authors', 'user-pen', ['title', 'designation']],
      ['categories', 'Categories', 'layer-group', ['title', 'sub_title']],
      ['comments', 'Comments', 'comments', ['name', 'content', 'blog_title']],
      ['galleries', 'Galleries', 'images', ['title', 'description']],
      ['books', 'PDF Books', 'books', ['title', 'author_or_editor']],
      ['submissions', 'Submit Blogs', 'file-pen', ['title', 'writer_name', 'content_title']],
      ['videos', 'Videos', 'video', ['title', 'description']]
    ];
    const accessible = definitions.filter(([table]) => NC.auth?.canAccess?.(table));
    const settled = await Promise.allSettled(accessible.map(async ([table, label, icon, fields]) => {
      const or = fields.map((field) => `${field}.ilike.*${term}*`).join(',');
      const result = await list(table, { select: '*', or, order: 'created_at.desc', limit: 5 });
      return result.data.map((item) => ({ table, label, icon, item }));
    }));
    return settled.flatMap((result) => result.status === 'fulfilled' ? result.value : []);
  }

  async function schemaProbe() {
    const requiredColumns = {
      blogs: 'id,imgbb_delete_url,image_meta,inline_media,pdf_file_provider,pdf_storage_path,pdf_file_size_mb',
      submissions: 'id,inline_media'
    };
    const keys = Object.keys(tables);
    const results = await Promise.all(keys.map(async (key) => {
      try {
        await list(key, { select: requiredColumns[key] || 'id', limit: 1 });
        return { key, table: tables[key], ok: true };
      } catch (error) {
        return { key, table: tables[key], ok: false, error };
      }
    }));
    const accessControlMissing = Boolean(NC.auth?.isLegacy?.());
    results.push(accessControlMissing
      ? { key: 'access-control', table: 'dashboard_access_control', ok: false, error: { isAccessControlMissing: true, message: 'Migration 004 is not installed.' } }
      : { key: 'access-control', table: 'dashboard_access_control', ok: true });
    const missing = results.filter((item) => item.error?.isSchemaMissing);
    const mismatched = results.filter((item) => item.error?.isSchemaMismatch);
    return { ok: results.every((item) => item.ok), results, missing, mismatched, accessControlMissing };
  }

  function storageObjectUrl(bucket, path) {
    const encodedPath = String(path).split('/').map(encodeURIComponent).join('/');
    return `${storageBase}/object/${encodeURIComponent(bucket)}/${encodedPath}`;
  }

  function storagePublicUrl(bucket, path) {
    const encodedPath = String(path).split('/').map(encodeURIComponent).join('/');
    return `${storageBase}/object/public/${encodeURIComponent(bucket)}/${encodedPath}`;
  }

  async function uploadPdf(file, onProgress) {
    if (!(file instanceof File)) throw new ApiError('Choose a PDF file first.', { code: 'NO_FILE' });
    if (file.type !== 'application/pdf' && !file.name.toLowerCase().endsWith('.pdf')) {
      throw new ApiError('Only PDF files can be uploaded to the book library.', { code: 'INVALID_FILE' });
    }
    if (file.size > supabase.pdfMaxBytes) {
      throw new ApiError('The PDF is larger than the 32 MB upload limit.', { code: 'FILE_TOO_LARGE' });
    }
    const safeName = file.name.normalize('NFKD').replace(/[^a-zA-Z0-9._-]+/g, '-').replace(/-+/g, '-');
    const path = `${new Date().getUTCFullYear()}/${NC.utils.uuid()}-${safeName}`;
    const responseBody = await new Promise((resolve, reject) => {
      const upload = new XMLHttpRequest();
      upload.open('POST', storageObjectUrl(supabase.pdfBucket, path));
      upload.timeout = 60000;
      upload.responseType = 'json';
      Object.entries({ ...authHeaders(), 'Content-Type': 'application/pdf', 'x-upsert': 'false' })
        .forEach(([name, value]) => upload.setRequestHeader(name, value));
      upload.upload.addEventListener('progress', (event) => {
        if (event.lengthComputable) onProgress?.(Math.round((event.loaded / event.total) * 100));
      });
      upload.addEventListener('load', () => {
        const body = upload.response || {};
        if (upload.status >= 200 && upload.status < 300) { onProgress?.(100); resolve(body); return; }
        reject(new ApiError(body.message || body.error || 'Supabase Storage could not upload this PDF.', {
          status: upload.status,
          code: body.error || body.statusCode || 'STORAGE_UPLOAD_ERROR',
          body
        }));
      });
      upload.addEventListener('error', () => reject(new ApiError('The PDF upload failed. Check your connection and try again.', { code: 'NETWORK_ERROR' })));
      upload.addEventListener('timeout', () => reject(new ApiError('The PDF upload timed out. Please try again.', { code: 'TIMEOUT' })));
      upload.send(file);
    });
    return {
      url: storagePublicUrl(supabase.pdfBucket, path),
      path,
      provider: 'supabase-storage',
      size: file.size,
      filename: file.name,
      mime: file.type || 'application/pdf',
      response: responseBody
    };
  }

  async function deleteStorageObject(bucket, path) {
    if (!path) return { ok: true, skipped: true };
    try {
      await request(`${storageBase}/object/${encodeURIComponent(bucket)}`, {
        method: 'DELETE',
        body: JSON.stringify({ prefixes: [path] })
      });
      return { ok: true };
    } catch (error) {
      return { ok: false, error };
    }
  }

  async function attemptImgBBDelete(deleteUrl) {
    const url = NC.utils.safeExternalUrl(deleteUrl);
    if (!url) return { ok: true, skipped: true };
    const controller = new AbortController();
    const timeout = window.setTimeout(() => controller.abort(), 8000);
    try {
      // ImgBB normally exposes a human confirmation URL rather than a public
      // deletion API. We still attempt a DELETE and report failures honestly.
      const response = await fetch(url, {
        method: 'DELETE', mode: 'cors', credentials: 'omit', signal: controller.signal
      });
      if (!response.ok) {
        return { ok: false, requiresManual: true, status: response.status, deleteUrl: url };
      }
      return { ok: true, deleteUrl: url };
    } catch (error) {
      return { ok: false, requiresManual: true, error, deleteUrl: url };
    } finally {
      window.clearTimeout(timeout);
    }
  }

  function userMessage(error, fallback = 'The operation could not be completed.') {
    if (!error) return fallback;
    if (error.isSchemaMissing) return 'The Supabase tables are not installed yet. Run backend/supabase/schema.sql first.';
    if (error.isSchemaMismatch) return 'The media columns are not installed yet. Run backend/supabase/migrations/003_blog_media_uploads.sql in Supabase.';
    if (error.code === '23505') return 'A record with this unique value already exists.';
    if (error.code === '23503') return 'This record is still used by related content and cannot be deleted.';
    if (error.code === '42501' || error.status === 401 || error.status === 403) return 'You do not have permission to perform this action. Sign in again or review the RLS policies.';
    if (error.code === 'NETWORK_ERROR' || error.code === 'TIMEOUT') return error.message;
    return error.message || fallback;
  }

  NC.api = Object.freeze({
    ApiError, request, list, getById, count, insert, update, upsert, remove,
    rpc, slugExists, searchAll, schemaProbe, uploadPdf, deleteStorageObject,
    storagePublicUrl, attemptImgBBDelete, userMessage, tableName
  });
})(window.NC);
