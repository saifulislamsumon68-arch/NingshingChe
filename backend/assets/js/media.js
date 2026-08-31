(function (NC) {
  'use strict';

  const { escapeHTML, qs, debounce, isValidUrl, safeExternalUrl, bytes, uuid } = NC.utils;

  function uploadImage(file, onProgress) {
    return new Promise((resolve, reject) => {
      if (!(file instanceof File)) {
        reject(new Error('Choose an image first.'));
        return;
      }
      if (!NC_CONFIG.imgbb.acceptedTypes.includes(file.type)) {
        reject(new Error('Choose a JPG, PNG, WebP, GIF, BMP, AVIF, HEIC, or HEIF image.'));
        return;
      }
      if (file.size > NC_CONFIG.imgbb.maxBytes) {
        reject(new Error('The image is larger than ImgBB’s 32 MB limit.'));
        return;
      }

      const form = new FormData();
      form.append('image', file, file.name);
      form.append('name', file.name.replace(/\.[^.]+$/, '').slice(0, 100));
      const request = new XMLHttpRequest();
      request.open('POST', `${NC_CONFIG.imgbb.endpoint}?key=${encodeURIComponent(NC_CONFIG.imgbb.apiKey)}`);
      request.timeout = 60000;
      request.responseType = 'json';
      request.upload.addEventListener('progress', (event) => {
        if (event.lengthComputable) onProgress?.(Math.round((event.loaded / event.total) * 100));
      });
      request.addEventListener('load', () => {
        const body = request.response || {};
        if (request.status >= 200 && request.status < 300 && body.success && body.data) {
          const data = body.data;
          resolve({
            url: data.url || data.display_url || '',
            display_url: data.display_url || data.url || '',
            delete_url: data.delete_url || '',
            filename: data.image?.filename || file.name,
            size: Number(data.size || file.size),
            mime: data.image?.mime || file.type,
            provider: 'imgbb',
            uploaded_at: new Date().toISOString()
          });
        } else {
          reject(new Error(body.error?.message || 'ImgBB could not upload this image.'));
        }
      });
      request.addEventListener('error', () => reject(new Error('The image upload failed. Check your connection and try again.')));
      request.addEventListener('timeout', () => reject(new Error('The image upload timed out. Please try again.')));
      request.send(form);
    });
  }

  function imageUploaderHTML({ id = uuid(), label = 'Image', hint = 'Upload to ImgBB or paste a direct image URL.', required = false } = {}) {
    return `
      <div class="image-uploader" data-image-uploader id="${escapeHTML(id)}">
        <div class="field-heading">
          <label class="field-label" for="${escapeHTML(id)}-url">${escapeHTML(label)}${required ? '<span aria-hidden="true"> *</span>' : ''}</label>
          <span class="field-hint">${escapeHTML(hint)}</span>
        </div>
        <div class="uploader-layout">
          <div class="uploader-preview" data-upload-preview>
            <div class="uploader-empty" data-upload-empty>
              <i class="fa-regular fa-image" aria-hidden="true"></i>
              <span>No image</span>
            </div>
            <img class="hidden" data-upload-image alt="Image preview" referrerpolicy="no-referrer">
            <div class="uploader-overlay hidden" data-upload-overlay>
              <i class="fa-regular fa-spinner-third fa-spin" aria-hidden="true"></i>
              <span data-upload-state>Uploading…</span>
            </div>
          </div>
          <div class="uploader-controls">
            <label class="drop-zone" tabindex="0">
              <input type="file" class="sr-only" accept="image/*,.heic,.heif" data-upload-file>
              <i class="fa-regular fa-cloud-arrow-up" aria-hidden="true"></i>
              <span><strong>Choose an image</strong> or drag it here</span>
              <small>Image files up to 32 MB</small>
            </label>
            <div class="divider-label"><span>or use a URL</span></div>
            <div class="input-with-action">
              <input type="url" class="form-input" id="${escapeHTML(id)}-url" data-upload-url placeholder="https://example.com/image.jpg" autocomplete="url">
              <button type="button" class="btn btn-secondary btn-icon" data-apply-url aria-label="Preview image URL"><i class="fa-regular fa-arrow-right" aria-hidden="true"></i></button>
            </div>
            <div class="upload-progress hidden" data-upload-progress-wrap aria-live="polite">
              <div class="upload-progress-track"><span data-upload-progress></span></div>
              <span data-upload-percent>0%</span>
            </div>
            <div class="uploader-meta hidden" data-upload-meta></div>
            <div class="flex flex-wrap gap-2 hidden" data-upload-actions>
              <button type="button" class="btn btn-secondary btn-sm" data-open-image><i class="fa-regular fa-arrow-up-right-from-square" aria-hidden="true"></i>Open original</button>
              <button type="button" class="btn btn-ghost-danger btn-sm" data-remove-image><i class="fa-regular fa-trash" aria-hidden="true"></i>Remove</button>
            </div>
            <p class="field-error hidden" data-upload-error role="alert"></p>
          </div>
        </div>
      </div>`;
  }

  function mountImageUploader(root, options = {}) {
    const element = root?.matches?.('[data-image-uploader]') ? root : qs('[data-image-uploader]', root);
    if (!element) throw new Error('Image uploader element was not found.');
    const image = qs('[data-upload-image]', element);
    const empty = qs('[data-upload-empty]', element);
    const overlay = qs('[data-upload-overlay]', element);
    const stateLabel = qs('[data-upload-state]', element);
    const fileInput = qs('[data-upload-file]', element);
    const urlInput = qs('[data-upload-url]', element);
    const progressWrap = qs('[data-upload-progress-wrap]', element);
    const progress = qs('[data-upload-progress]', element);
    const percent = qs('[data-upload-percent]', element);
    const meta = qs('[data-upload-meta]', element);
    const actions = qs('[data-upload-actions]', element);
    const errorNode = qs('[data-upload-error]', element);
    const dropZone = qs('.drop-zone', element);
    let value = normalizeInitial(options.initial);
    let uploading = false;

    function normalizeInitial(initial) {
      if (!initial) return { url: '', display_url: '', delete_url: '', filename: '', size: 0, mime: '', provider: '' };
      if (typeof initial === 'string') return { url: initial, display_url: initial, delete_url: '', filename: '', size: 0, mime: '', provider: 'url' };
      return {
        url: initial.url || initial.image || '',
        display_url: initial.display_url || initial.url || initial.image || '',
        delete_url: initial.delete_url || initial.imgbb_delete_url || '',
        filename: initial.filename || initial.image_meta?.filename || '',
        size: Number(initial.size || initial.image_meta?.size || 0),
        mime: initial.mime || initial.image_meta?.mime || '',
        provider: initial.provider || initial.image_meta?.provider || (initial.delete_url || initial.imgbb_delete_url ? 'imgbb' : 'url')
      };
    }

    function setError(message = '') {
      errorNode.textContent = message;
      errorNode.classList.toggle('hidden', !message);
      element.classList.toggle('has-error', Boolean(message));
    }

    function render() {
      const url = safeExternalUrl(value.display_url || value.url);
      urlInput.value = value.url || '';
      image.classList.toggle('hidden', !url);
      empty.classList.toggle('hidden', Boolean(url));
      actions.classList.toggle('hidden', !url);
      if (url) image.src = url;
      meta.classList.toggle('hidden', !(value.filename || value.size || value.provider));
      if (!meta.classList.contains('hidden')) {
        const details = [value.filename, value.size ? bytes(value.size) : '', value.provider === 'imgbb' ? 'ImgBB' : 'Direct URL'].filter(Boolean);
        meta.innerHTML = `<i class="fa-regular fa-circle-info" aria-hidden="true"></i><span>${details.map(escapeHTML).join(' · ')}</span>`;
      }
    }

    function emit(previous) {
      options.onChange?.({ ...value }, previous ? { ...previous } : null);
    }

    function applyUrl() {
      const url = urlInput.value.trim();
      if (!url) {
        const previous = value;
        value = normalizeInitial(null);
        setError(''); render(); emit(previous); return;
      }
      if (!isValidUrl(url, { allowEmpty: false })) {
        setError('Enter a complete http:// or https:// image URL.');
        return;
      }
      const previous = value;
      value = { url, display_url: url, delete_url: '', filename: url.split('/').pop().split('?')[0], size: 0, mime: '', provider: 'url' };
      setError(''); render(); emit(previous);
    }

    async function handleFile(file) {
      if (!file || uploading) return;
      uploading = true;
      setError('');
      overlay.classList.remove('hidden');
      stateLabel.textContent = 'Uploading…';
      progressWrap.classList.remove('hidden');
      progress.style.width = '0%'; percent.textContent = '0%';
      dropZone.classList.add('is-uploading');
      const previous = value;
      try {
        const uploaded = await uploadImage(file, (amount) => {
          progress.style.width = `${amount}%`;
          percent.textContent = `${amount}%`;
        });
        value = uploaded;
        render(); emit(previous);
        NC.components.toast('Image uploaded successfully.', 'success');
      } catch (error) {
        setError(error.message);
        stateLabel.textContent = 'Upload failed';
      } finally {
        uploading = false;
        overlay.classList.add('hidden');
        progressWrap.classList.add('hidden');
        dropZone.classList.remove('is-uploading');
        fileInput.value = '';
      }
    }

    qs('[data-apply-url]', element).addEventListener('click', applyUrl);
    urlInput.addEventListener('change', applyUrl);
    urlInput.addEventListener('input', debounce(() => {
      if (urlInput.value.trim() && isValidUrl(urlInput.value.trim(), { allowEmpty: false })) applyUrl();
    }, 600));
    fileInput.addEventListener('change', () => handleFile(fileInput.files[0]));
    dropZone.addEventListener('keydown', (event) => {
      if (event.key === 'Enter' || event.key === ' ') { event.preventDefault(); fileInput.click(); }
    });
    ['dragenter', 'dragover'].forEach((name) => dropZone.addEventListener(name, (event) => {
      event.preventDefault(); dropZone.classList.add('is-dragging');
    }));
    ['dragleave', 'drop'].forEach((name) => dropZone.addEventListener(name, (event) => {
      event.preventDefault(); dropZone.classList.remove('is-dragging');
    }));
    dropZone.addEventListener('drop', (event) => handleFile(event.dataTransfer?.files?.[0]));
    qs('[data-remove-image]', element).addEventListener('click', () => {
      const previous = value;
      value = normalizeInitial(null);
      setError(''); render(); emit(previous);
    });
    qs('[data-open-image]', element).addEventListener('click', () => {
      const url = safeExternalUrl(value.display_url || value.url);
      if (url) window.open(url, '_blank', 'noopener,noreferrer');
    });
    image.addEventListener('error', () => {
      setError('The image URL could not be loaded. You can replace it or check the URL.');
      empty.classList.remove('hidden'); image.classList.add('hidden');
    });
    image.addEventListener('load', () => setError(''));
    render();

    return Object.freeze({
      getValue: () => ({ ...value }),
      setValue: (next) => { const previous = value; value = normalizeInitial(next); setError(''); render(); emit(previous); },
      isUploading: () => uploading,
      validate: () => {
        if (options.required && !safeExternalUrl(value.url)) { setError(`${options.label || 'Image'} is required.`); return false; }
        if (value.url && !safeExternalUrl(value.url)) { setError('Enter a valid image URL.'); return false; }
        return true;
      }
    });
  }

  function pdfUploaderHTML({ id = uuid(), label = 'PDF file' } = {}) {
    return `
      <div class="pdf-uploader" data-pdf-uploader id="${escapeHTML(id)}">
        <div class="field-heading">
          <label class="field-label" for="${escapeHTML(id)}-url">${escapeHTML(label)}</label>
          <span class="field-hint">Choose a local PDF file or paste a direct PDF URL. Files use Supabase Storage; maximum 32 MB.</span>
        </div>
        <div class="grid gap-3 sm:grid-cols-[1fr_auto]">
          <input type="url" class="form-input" id="${escapeHTML(id)}-url" data-pdf-url placeholder="https://example.com/book.pdf">
          <label class="btn btn-secondary cursor-pointer justify-center">
            <i class="fa-regular fa-file-arrow-up" aria-hidden="true"></i><span>Upload PDF</span>
            <input type="file" class="sr-only" accept="application/pdf,.pdf" data-pdf-file>
          </label>
        </div>
        <div class="upload-progress hidden mt-3" data-pdf-progress-wrap aria-live="polite">
          <div class="upload-progress-track"><span data-pdf-progress></span></div>
          <span data-pdf-percent>0%</span>
        </div>
        <div class="uploader-meta hidden mt-3" data-pdf-meta></div>
        <div class="mt-3 flex flex-wrap gap-2 hidden" data-pdf-actions>
          <button type="button" class="btn btn-secondary btn-sm" data-open-pdf><i class="fa-regular fa-arrow-up-right-from-square" aria-hidden="true"></i>Open PDF</button>
          <button type="button" class="btn btn-ghost-danger btn-sm" data-remove-pdf><i class="fa-regular fa-trash" aria-hidden="true"></i>Remove</button>
        </div>
        <p class="field-error hidden" data-pdf-error role="alert"></p>
      </div>`;
  }

  function mountPdfUploader(root, options = {}) {
    const element = root?.matches?.('[data-pdf-uploader]') ? root : qs('[data-pdf-uploader]', root);
    if (!element) throw new Error('PDF uploader element was not found.');
    const urlInput = qs('[data-pdf-url]', element);
    const fileInput = qs('[data-pdf-file]', element);
    const progressWrap = qs('[data-pdf-progress-wrap]', element);
    const progress = qs('[data-pdf-progress]', element);
    const percent = qs('[data-pdf-percent]', element);
    const meta = qs('[data-pdf-meta]', element);
    const actions = qs('[data-pdf-actions]', element);
    const errorNode = qs('[data-pdf-error]', element);
    let value = {
      url: options.initial?.url || options.initial?.link || '',
      path: options.initial?.path || options.initial?.file_storage_path || options.initial?.pdf_storage_path || '',
      provider: options.initial?.provider || options.initial?.file_provider || options.initial?.pdf_file_provider || 'url',
      size: Number(options.initial?.size || (options.initial?.file_size_mb || options.initial?.pdf_file_size_mb || 0) * 1024 * 1024 || 0),
      filename: options.initial?.filename || ''
    };
    let uploading = false;

    function setError(message = '') {
      errorNode.textContent = message;
      errorNode.classList.toggle('hidden', !message);
    }
    function render() {
      urlInput.value = value.url || '';
      const hasUrl = Boolean(safeExternalUrl(value.url));
      actions.classList.toggle('hidden', !hasUrl);
      meta.classList.toggle('hidden', !hasUrl);
      if (hasUrl) {
        meta.innerHTML = `<i class="fa-regular fa-file-pdf" aria-hidden="true"></i><span>${[value.filename || 'PDF link', value.size ? bytes(value.size) : '', value.provider === 'supabase-storage' ? 'Supabase Storage' : 'Direct URL'].filter(Boolean).map(escapeHTML).join(' · ')}</span>`;
      }
    }
    function emit(previous) { options.onChange?.({ ...value }, previous ? { ...previous } : null); }
    function setValue(nextValue = '') {
      const source = typeof nextValue === 'string' ? { url: nextValue } : (nextValue || {});
      const previous = value;
      const url = source.url || source.link || '';
      value = {
        url,
        path: source.path || source.file_storage_path || source.pdf_storage_path || '',
        provider: source.provider || source.file_provider || source.pdf_file_provider || 'url',
        size: Number(source.size || (source.file_size_mb || source.pdf_file_size_mb || 0) * 1024 * 1024 || 0),
        filename: source.filename || (url ? url.split('/').pop().split('?')[0] : '')
      };
      setError(''); render(); emit(previous);
    }
    function applyUrl() {
      const url = urlInput.value.trim();
      if (url && !isValidUrl(url, { allowEmpty: false })) { setError('Enter a complete http:// or https:// PDF URL.'); return; }
      const previous = value;
      value = { url, path: '', provider: 'url', size: 0, filename: url ? url.split('/').pop().split('?')[0] : '' };
      setError(''); render(); emit(previous);
    }
    async function handleFile(file) {
      if (!file || uploading) return;
      uploading = true; setError(''); progressWrap.classList.remove('hidden');
      const previous = value;
      try {
        const result = await NC.api.uploadPdf(file, (amount) => {
          progress.style.width = `${amount}%`; percent.textContent = `${amount}%`;
        });
        value = result; render(); emit(previous);
        NC.components.toast('PDF uploaded successfully.', 'success');
      } catch (error) {
        setError(NC.api.userMessage(error, 'Unable to upload the PDF.'));
      } finally {
        uploading = false; progressWrap.classList.add('hidden'); fileInput.value = '';
      }
    }
    urlInput.addEventListener('change', applyUrl);
    fileInput.addEventListener('change', () => handleFile(fileInput.files[0]));
    qs('[data-open-pdf]', element).addEventListener('click', () => {
      const url = safeExternalUrl(value.url); if (url) window.open(url, '_blank', 'noopener,noreferrer');
    });
    qs('[data-remove-pdf]', element).addEventListener('click', () => {
      const previous = value; value = { url: '', path: '', provider: 'url', size: 0, filename: '' }; render(); emit(previous);
    });
    render();
    return Object.freeze({
      getValue: () => ({ ...value }), setValue, isUploading: () => uploading,
      validate: () => {
        if (value.url && !safeExternalUrl(value.url)) { setError('Enter a valid PDF URL.'); return false; }
        return true;
      }
    });
  }

  function detectVideoProvider(value) {
    const url = safeExternalUrl(value);
    if (!url) return { provider: 'Unknown', url: '', embedUrl: '', id: '', thumbnail: '' };
    const parsed = new URL(url);
    const host = parsed.hostname.toLowerCase().replace(/^www\./, '');
    let id = '';
    if (host === 'youtu.be') id = parsed.pathname.split('/').filter(Boolean)[0] || '';
    if (host.endsWith('youtube.com')) {
      id = parsed.searchParams.get('v') || parsed.pathname.match(/\/(?:embed|shorts|live)\/([\w-]{6,})/)?.[1] || '';
    }
    if (/^[\w-]{6,20}$/.test(id)) {
      return {
        provider: 'YouTube', url, id,
        embedUrl: `https://www.youtube-nocookie.com/embed/${encodeURIComponent(id)}`,
        thumbnail: `https://i.ytimg.com/vi/${encodeURIComponent(id)}/hqdefault.jpg`
      };
    }
    if (host.endsWith('facebook.com') || host === 'fb.watch') {
      return {
        provider: 'Facebook', url, id: '',
        embedUrl: `https://www.facebook.com/plugins/video.php?href=${encodeURIComponent(url)}&show_text=false&width=720`,
        thumbnail: ''
      };
    }
    if (host.endsWith('instagram.com')) {
      const match = parsed.pathname.match(/^\/(?:p|reel|tv)\/([\w-]+)/);
      return {
        provider: 'Instagram', url, id: match?.[1] || '',
        embedUrl: match ? `https://www.instagram.com${parsed.pathname.replace(/\/$/, '')}/embed/` : '',
        thumbnail: ''
      };
    }
    return { provider: 'Video Link', url, id: '', embedUrl: '', thumbnail: '' };
  }

  function videoPreviewHTML(value, options = {}) {
    const info = detectVideoProvider(value);
    if (!info.url) {
      return NC.components.emptyState({ icon: 'fa-video-slash', title: 'No video to preview', description: 'Enter a supported YouTube, Facebook, or Instagram URL.' });
    }
    const link = `<a class="btn btn-secondary btn-sm" href="${escapeHTML(info.url)}" target="_blank" rel="noopener noreferrer"><i class="fa-regular fa-arrow-up-right-from-square" aria-hidden="true"></i>Open video</a>`;
    if (info.embedUrl) {
      return `
        <div class="video-preview-card">
          <div class="video-frame">
            <iframe src="${escapeHTML(info.embedUrl)}" title="${escapeHTML(options.title || `${info.provider} video preview`)}" loading="lazy" allow="accelerometer; autoplay; clipboard-write; encrypted-media; gyroscope; picture-in-picture; web-share" allowfullscreen referrerpolicy="strict-origin-when-cross-origin"></iframe>
          </div>
          <div class="video-preview-meta"><span>${NC.components.statusBadge(info.provider)}</span>${link}</div>
          <p class="field-hint">If ${escapeHTML(info.provider)} blocks embedding, use “Open video” to view it on the provider.</p>
        </div>`;
    }
    return `
      <div class="video-fallback">
        ${info.thumbnail ? `<img src="${escapeHTML(info.thumbnail)}" alt="" loading="lazy" referrerpolicy="no-referrer">` : '<span class="empty-icon"><i class="fa-solid fa-video" aria-hidden="true"></i></span>'}
        <div><p class="font-semibold">${escapeHTML(info.provider)}</p><p class="break-all text-sm text-muted-foreground">${escapeHTML(info.url)}</p><div class="mt-3">${link}</div></div>
      </div>`;
  }

  NC.media = Object.freeze({
    uploadImage, imageUploaderHTML, mountImageUploader,
    pdfUploaderHTML, mountPdfUploader, detectVideoProvider, videoPreviewHTML
  });
})(window.NC);
