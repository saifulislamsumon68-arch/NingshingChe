(function (NC) {
  'use strict';

  const { escapeHTML, qs, qsa, sanitizeHTML, stripHTML, uuid } = NC.utils;

  function editorHTML({ id = uuid(), label = 'Content', hint = 'Use the toolbar to structure the article.', required = false } = {}) {
    return `
      <div class="rich-editor-field" data-rich-editor id="${escapeHTML(id)}">
        <div class="field-heading">
          <label class="field-label" id="${escapeHTML(id)}-label">${escapeHTML(label)}${required ? '<span aria-hidden="true"> *</span>' : ''}</label>
          <span class="field-hint">${escapeHTML(hint)}</span>
        </div>
        <div class="editor-toolbar-extra">
          <button type="button" class="editor-extra-button" data-insert-table title="Insert a 2 × 2 table"><i class="fa-regular fa-table" aria-hidden="true"></i><span>Table</span></button>
          <button type="button" class="editor-extra-button" data-source-toggle title="Edit sanitized HTML source"><i class="fa-regular fa-code" aria-hidden="true"></i><span>HTML</span></button>
        </div>
        <div class="editor-host-wrap"><div class="editor-host" data-editor-host aria-labelledby="${escapeHTML(id)}-label"></div></div>
        <textarea class="editor-source hidden" data-editor-source spellcheck="false" aria-label="HTML source"></textarea>
        <div class="editor-footer"><span data-editor-count>0 words</span><span>HTML is sanitized before preview and save</span></div>
        <p class="field-error hidden" data-editor-error role="alert"></p>
      </div>`;
  }

  function editorDialog({ title, description = '', content, confirmLabel = 'Insert', onMount, canCancel }) {
    return new Promise((resolve) => {
      const id = `editor-dialog-${uuid()}`;
      const previousFocus = document.activeElement;
      const overlay = document.createElement('div');
      overlay.className = 'editor-dialog-layer';
      overlay.innerHTML = `
        <button type="button" class="editor-dialog-backdrop" data-editor-dialog-cancel tabindex="-1" aria-label="Close dialog"></button>
        <section class="editor-dialog-panel" role="dialog" aria-modal="true" aria-labelledby="${escapeHTML(id)}-title" ${description ? `aria-describedby="${escapeHTML(id)}-description"` : ''}>
          <div class="editor-dialog-header">
            <div><h2 id="${escapeHTML(id)}-title">${escapeHTML(title)}</h2>${description ? `<p id="${escapeHTML(id)}-description">${escapeHTML(description)}</p>` : ''}</div>
            <button type="button" class="btn btn-icon btn-ghost" data-editor-dialog-cancel aria-label="Close"><i class="fa-regular fa-xmark" aria-hidden="true"></i></button>
          </div>
          <div class="editor-dialog-body" data-editor-dialog-body>${content}</div>
          <div class="editor-dialog-footer">
            <button type="button" class="btn btn-secondary" data-editor-dialog-cancel>Cancel</button>
            <button type="button" class="btn btn-primary" data-editor-dialog-apply>${escapeHTML(confirmLabel)}</button>
          </div>
        </section>`;
      document.body.appendChild(overlay);
      document.body.classList.add('editor-dialog-open');
      const applyButton = qs('[data-editor-dialog-apply]', overlay);
      let settled = false;
      const getResult = onMount?.(overlay) || (() => null);

      function finish(value) {
        if (settled) return;
        settled = true;
        document.removeEventListener('keydown', onKeydown, true);
        overlay.remove();
        if (!document.querySelector('.editor-dialog-layer')) document.body.classList.remove('editor-dialog-open');
        if (previousFocus?.isConnected) previousFocus.focus();
        resolve(value);
      }

      function cancel() {
        if (canCancel && !canCancel()) return;
        finish(null);
      }

      function onKeydown(event) {
        if (event.key === 'Escape') { event.preventDefault(); cancel(); return; }
        if (event.key !== 'Tab') return;
        const focusable = qsa('button:not([disabled]), input:not([disabled]), textarea:not([disabled]), select:not([disabled]), [tabindex]:not([tabindex="-1"])', overlay)
          .filter((node) => node.offsetParent !== null && node.tabIndex >= 0);
        if (!focusable.length) return;
        const first = focusable[0];
        const last = focusable[focusable.length - 1];
        if (event.shiftKey && document.activeElement === first) { event.preventDefault(); last.focus(); }
        else if (!event.shiftKey && document.activeElement === last) { event.preventDefault(); first.focus(); }
      }

      qsa('[data-editor-dialog-cancel]', overlay).forEach((button) => button.addEventListener('click', cancel));
      applyButton.addEventListener('click', async () => {
        if (applyButton.disabled) return;
        applyButton.disabled = true;
        try {
          const value = await getResult();
          if (value !== undefined) finish(value);
        } finally {
          if (applyButton.isConnected) applyButton.disabled = false;
        }
      });
      document.addEventListener('keydown', onKeydown, true);
      window.requestAnimationFrame(() => {
        const autofocus = qs('[autofocus]', overlay);
        (autofocus || qs('.editor-dialog-panel input, .editor-dialog-panel button', overlay))?.focus();
      });
    });
  }

  function requestUrl({ title, label, placeholder }) {
    const inputId = `editor-url-${uuid()}`;
    return editorDialog({
      title,
      content: `<label class="field-label" for="${escapeHTML(inputId)}">${escapeHTML(label)}</label><input id="${escapeHTML(inputId)}" class="form-input mt-2" type="url" placeholder="${escapeHTML(placeholder)}" autofocus><p class="field-error hidden mt-2" data-editor-url-error role="alert"></p>`,
      onMount: (dialog) => {
        const input = qs(`#${inputId}`, dialog);
        const error = qs('[data-editor-url-error]', dialog);
        input.addEventListener('keydown', (event) => {
          if (event.key === 'Enter') { event.preventDefault(); qs('[data-editor-dialog-apply]', dialog).click(); }
        });
        return () => {
          const value = input.value.trim();
          if (!NC.utils.isValidUrl(value, { allowEmpty: false })) {
            error.textContent = 'Enter a complete http:// or https:// URL.';
            error.classList.remove('hidden');
            input.focus();
            return undefined;
          }
          return value;
        };
      }
    });
  }

  function normalizeMedia(media) {
    if (!media) return null;
    const url = NC.utils.safeExternalUrl(media.url || media.display_url || '');
    if (!url) return null;
    return {
      url,
      display_url: NC.utils.safeExternalUrl(media.display_url || url) || url,
      delete_url: NC.utils.safeExternalUrl(media.delete_url || media.imgbb_delete_url || ''),
      filename: String(media.filename || media.image_meta?.filename || ''),
      size: Number(media.size || media.image_meta?.size || 0),
      mime: String(media.mime || media.image_meta?.mime || ''),
      provider: String(media.provider || media.image_meta?.provider || (media.delete_url ? 'imgbb' : 'url')),
      uploaded_at: media.uploaded_at || media.image_meta?.uploaded_at || null
    };
  }

  function requestImage() {
    const uploaderId = `editor-image-${uuid()}`;
    const sessionUploads = [];
    let uploader = null;
    return editorDialog({
      title: 'Insert image',
      description: 'Upload a local image to ImgBB or use a direct public image link.',
      confirmLabel: 'Insert image',
      canCancel: () => {
        if (!uploader?.isUploading()) return true;
        NC.components.toast('Wait for the image upload to finish before closing this dialog.', 'warning');
        return false;
      },
      content: NC.media.imageUploaderHTML({
        id: uploaderId,
        label: 'Article image',
        hint: 'Choose a local image for ImgBB upload, or paste its direct URL.',
        required: true
      }),
      onMount: (dialog) => {
        uploader = NC.media.mountImageUploader(qs(`#${uploaderId}`, dialog), {
          label: 'Article image',
          required: true,
          onChange: (next) => {
            const media = normalizeMedia(next);
            if (media?.provider === 'imgbb' && media.delete_url && !sessionUploads.some((item) => item.url === media.url)) {
              sessionUploads.push(media);
            }
          }
        });
        return () => {
          if (uploader.isUploading()) {
            NC.components.toast('Wait for the image upload to finish.', 'warning');
            return undefined;
          }
          if (!uploader.validate()) return undefined;
          return { media: normalizeMedia(uploader.getValue()), uploads: sessionUploads.map((item) => ({ ...item })) };
        };
      }
    }).then((result) => result || { media: null, uploads: sessionUploads.map((item) => ({ ...item })) });
  }

  function mountEditor(root, options = {}) {
    const element = root?.matches?.('[data-rich-editor]') ? root : qs('[data-rich-editor]', root);
    if (!element) throw new Error('Rich text editor element was not found.');
    const host = qs('[data-editor-host]', element);
    const source = qs('[data-editor-source]', element);
    const count = qs('[data-editor-count]', element);
    const errorNode = qs('[data-editor-error]', element);
    const sourceButton = qs('[data-source-toggle]', element);
    let sourceMode = false;
    let quill = null;
    const mediaRecords = [];
    const initialMediaUrls = new Set();

    (Array.isArray(options.media) ? options.media : []).forEach((item) => {
      const media = normalizeMedia(item);
      if (!media || mediaRecords.some((record) => record.url === media.url)) return;
      mediaRecords.push({ ...media, session_upload: false });
      initialMediaUrls.add(media.url);
    });

    function registerMedia(item, sessionUpload = false) {
      const media = normalizeMedia(item);
      if (!media) return null;
      const existing = mediaRecords.find((record) => record.url === media.url);
      if (existing) {
        Object.assign(existing, media, { session_upload: existing.session_upload || sessionUpload });
        return existing;
      }
      const record = { ...media, session_upload: sessionUpload };
      mediaRecords.push(record);
      return record;
    }

    function imageUrlsInContent() {
      const clean = getValue();
      const documentFragment = new DOMParser().parseFromString(`<body>${clean}</body>`, 'text/html');
      return new Set(qsa('img[src]', documentFragment).map((image) => NC.utils.safeExternalUrl(image.getAttribute('src') || '')).filter(Boolean));
    }

    function serializeMedia(record) {
      const { session_upload, ...media } = record;
      return { ...media };
    }

    function activeMedia() {
      const urls = imageUrlsInContent();
      return mediaRecords.filter((record) => urls.has(record.url)).map(serializeMedia);
    }

    function inactiveMedia() {
      const urls = imageUrlsInContent();
      return mediaRecords.filter((record) => !urls.has(record.url)).map(serializeMedia);
    }

    function setError(message = '') {
      errorNode.textContent = message;
      errorNode.classList.toggle('hidden', !message);
      element.classList.toggle('has-error', Boolean(message));
    }

    function updateCount(html) {
      const text = stripHTML(html || '');
      const words = text ? text.split(/\s+/).length : 0;
      count.textContent = `${words.toLocaleString()} word${words === 1 ? '' : 's'}`;
    }

    function rawValue() {
      if (sourceMode) return source.value;
      if (quill) return quill.root.innerHTML === '<p><br></p>' ? '' : quill.root.innerHTML;
      return host.innerHTML;
    }

    function getValue() {
      return sanitizeHTML(rawValue());
    }

    function setValue(value) {
      const clean = sanitizeHTML(value || '');
      if (quill) quill.clipboard.dangerouslyPasteHTML(clean);
      else host.innerHTML = clean;
      source.value = clean;
      updateCount(clean);
    }

    function toggleSource() {
      if (!sourceMode) {
        source.value = getValue();
        source.classList.remove('hidden');
        host.parentElement?.classList.add('hidden');
        sourceMode = true;
        sourceButton.classList.add('is-active');
        sourceButton.setAttribute('aria-pressed', 'true');
        source.focus();
      } else {
        const clean = sanitizeHTML(source.value);
        if (quill) quill.clipboard.dangerouslyPasteHTML(clean);
        else host.innerHTML = clean;
        source.classList.add('hidden');
        host.parentElement?.classList.remove('hidden');
        sourceMode = false;
        sourceButton.classList.remove('is-active');
        sourceButton.setAttribute('aria-pressed', 'false');
        updateCount(clean);
        quill?.focus();
      }
    }

    function insertTable() {
      const markup = '<table><tbody><tr><th>Heading</th><th>Heading</th></tr><tr><td>Cell</td><td>Cell</td></tr></tbody></table><p><br></p>';
      if (sourceMode) {
        const start = source.selectionStart;
        source.setRangeText(markup, start, source.selectionEnd, 'end');
        source.dispatchEvent(new Event('input'));
      } else if (quill) {
        const range = quill.getSelection(true) || { index: quill.getLength() - 1 };
        quill.clipboard.dangerouslyPasteHTML(range.index, markup, 'user');
      } else {
        host.focus(); document.execCommand('insertHTML', false, markup);
      }
    }

    if (window.Quill) {
      quill = new window.Quill(host, {
        theme: 'snow',
        placeholder: options.placeholder || 'Start writing…',
        modules: {
          toolbar: {
            container: [
              [{ header: [2, 3, 4, false] }],
              ['bold', 'italic', 'underline'],
              [{ list: 'ordered' }, { list: 'bullet' }],
              ['blockquote', 'link'],
              [{ align: [] }],
              options.allowImages === false ? ['clean'] : ['image', 'clean']
            ],
            handlers: {
              link: async function linkHandler(value) {
                if (!value) { this.quill.format('link', false); return; }
                const url = await requestUrl({ title: 'Insert link', label: 'Destination URL', placeholder: 'https://example.com' });
                if (url) this.quill.format('link', url);
              },
              image: async function imageHandler() {
                const activeQuill = this.quill;
                const result = await requestImage();
                result.uploads.forEach((media) => registerMedia(media, true));
                if (!result.media?.url) {
                  options.onMediaChange?.(activeMedia(), inactiveMedia());
                  return;
                }
                registerMedia(result.media, result.media.provider === 'imgbb' && result.uploads.some((item) => item.url === result.media.url));
                const range = activeQuill.getSelection(true) || { index: activeQuill.getLength() - 1 };
                activeQuill.insertEmbed(range.index, 'image', result.media.url, 'user');
                activeQuill.setSelection(range.index + 1, 0, 'silent');
                options.onMediaChange?.(activeMedia(), inactiveMedia());
              }
            }
          }
        }
      });
      qsa('.ql-picker', element).forEach((picker) => {
        const label = qs('.ql-picker-label', picker);
        if (!label) return;
        const accessibleName = picker.classList.contains('ql-header') ? 'Heading level'
          : picker.classList.contains('ql-align') ? 'Text alignment'
            : 'Formatting options';
        label.setAttribute('aria-label', accessibleName);
      });
      quill.on('text-change', () => {
        setError('');
        updateCount(rawValue());
        options.onChange?.(getValue());
        options.onMediaChange?.(activeMedia(), inactiveMedia());
      });
    } else {
      host.contentEditable = 'true';
      host.classList.add('editor-fallback');
      host.setAttribute('role', 'textbox');
      host.setAttribute('aria-multiline', 'true');
      host.dataset.placeholder = options.placeholder || 'Start writing…';
      const toolbar = document.createElement('div');
      toolbar.className = 'fallback-toolbar';
      toolbar.innerHTML = [
        ['bold', 'fa-bold', 'Bold'], ['italic', 'fa-italic', 'Italic'],
        ['underline', 'fa-underline', 'Underline'], ['insertUnorderedList', 'fa-list-ul', 'Bullet list'],
        ['formatBlock', 'fa-quote-left', 'Blockquote']
      ].map(([command, icon, label]) => `<button type="button" data-command="${command}" aria-label="${label}" title="${label}"><i class="fa-regular ${icon}" aria-hidden="true"></i></button>`).join('');
      host.before(toolbar);
      qsa('[data-command]', toolbar).forEach((button) => button.addEventListener('click', () => {
        host.focus();
        document.execCommand(button.dataset.command, false, button.dataset.command === 'formatBlock' ? 'blockquote' : null);
      }));
      host.addEventListener('input', () => { setError(''); updateCount(rawValue()); options.onChange?.(getValue()); });
    }

    sourceButton.setAttribute('aria-pressed', 'false');
    sourceButton.addEventListener('click', toggleSource);
    qs('[data-insert-table]', element).addEventListener('click', insertTable);
    source.addEventListener('input', () => {
      setError('');
      updateCount(source.value);
      options.onChange?.(sanitizeHTML(source.value));
      options.onMediaChange?.(activeMedia(), inactiveMedia());
    });
    setValue(options.initial || '');

    return Object.freeze({
      getValue,
      setValue,
      getMedia: activeMedia,
      getInactiveMedia: inactiveMedia,
      getSessionUploads: () => mediaRecords.filter((record) => record.session_upload).map(serializeMedia),
      getRemovedMedia: () => inactiveMedia().filter((record) => initialMediaUrls.has(record.url)),
      focus: () => sourceMode ? source.focus() : (quill ? quill.focus() : host.focus()),
      validate: () => {
        const empty = !stripHTML(getValue());
        if (options.required && empty) { setError(`${options.label || 'Content'} is required.`); return false; }
        setError(''); return true;
      },
      destroy: () => { quill = null; }
    });
  }

  NC.editor = Object.freeze({ editorHTML, mountEditor });
})(window.NC);
