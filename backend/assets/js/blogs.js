(function (NC) {
  'use strict';

  const { escapeHTML, formatDate, formData, validateFields, debounce, slugify, parseTags, safeImage, safeExternalUrl } = NC.utils;
  const state = new NC.crud.ListState('blogs', { searchFields: ['title', 'sub_title', 'slug', 'author_name', 'category_title', 'tags'], sortKey: 'created_at' });
  let root;
  let authors = [];
  let categories = [];
  let editorController = null;
  let heroUploader = null;
  let pdfUploader = null;
  let editorCleanup = null;

  function authorName(record) {
    return authors.find((item) => item.id === record.author_id)?.title || record.author_name || 'Unassigned';
  }
  function categoryName(record) {
    return categories.find((item) => item.id === record.category_id)?.title || record.category_title || 'Uncategorized';
  }

  function featureBadges(record) {
    const badges = [];
    if (record.is_slider) badges.push(NC.components.statusBadge('Slider'));
    if (record.is_feature) badges.push(NC.components.statusBadge('Feature'));
    if (record.is_special_article) badges.push(NC.components.statusBadge('Special'));
    return badges.join('') || '<span class="text-faint">—</span>';
  }

  function renderList() {
    const content = root.querySelector('[data-blogs-content]');
    if (!content) return;
    const { rows, total } = state.paged();
    if (!total) {
      content.innerHTML = NC.components.emptyState({
        icon: 'fa-newspaper', title: state.query ? 'No blogs match your search' : 'Write your first article',
        description: state.query ? 'Try changing the search or filters.' : 'Draft, preview, and publish magazine articles from one focused editor.',
        action: state.query ? '' : '<button type="button" class="btn btn-primary" data-add-blog><i class="fa-regular fa-plus" aria-hidden="true"></i>Add blog</button>'
      });
      bindListEvents(content); return;
    }
    const body = rows.map((record) => `
      <tr>
        <td data-label="Article"><div class="article-cell">${record.image ? `<img src="${escapeHTML(safeImage(record.image))}" alt="" loading="lazy" referrerpolicy="no-referrer" data-image-fallback>` : '<span class="article-thumb-placeholder"><i class="fa-regular fa-image" aria-hidden="true"></i></span>'}<div><strong>${escapeHTML(record.title)}</strong><small>/${escapeHTML(record.slug || 'no-slug')}</small></div></div></td>
        <td data-label="Author & Category"><div class="stacked-cell"><strong>${escapeHTML(authorName(record))}</strong><span>${escapeHTML(categoryName(record))}</span></div></td>
        <td data-label="Status"><div class="flex flex-wrap gap-1.5">${NC.components.statusBadge(record.status || 'Draft')}</div></td>
        <td data-label="Placement"><div class="badge-wrap">${featureBadges(record)}</div></td>
        <td data-label="Date"><div class="stacked-cell"><strong>${escapeHTML(formatDate(record.published_date || record.created_at))}</strong><span>${Number(record.views_count || 0).toLocaleString()} views</span></div></td>
        <td data-label="Actions" class="text-right">${NC.components.rowActions([
          { action: 'preview', id: record.id, label: 'Preview blog', icon: 'fa-eye' },
          { action: 'edit', id: record.id, label: 'Edit blog', icon: 'fa-pen' },
          { action: 'delete', id: record.id, label: 'Delete blog', icon: 'fa-trash', danger: true }
        ])}</td>
      </tr>`).join('');
    content.innerHTML = `${NC.components.tableShell({
      caption: 'Blogs', minWidth: '980px',
      head: `<tr><th><button type="button" data-sort="title">Article ${NC.crud.sortIcon(state, 'title')}</button></th><th>Author & Category</th><th>Status</th><th>Placement</th><th><button type="button" data-sort="created_at">Date ${NC.crud.sortIcon(state, 'created_at')}</button></th><th class="text-right">Actions</th></tr>`,
      body
    })}${NC.components.pagination({ page: state.page, pageSize: state.pageSize, total })}`;
    bindListEvents(content);
    NC.components.bindImageFallbacks(content);
  }

  function bindListEvents(scope = root) {
    scope.querySelectorAll('[data-add-blog]').forEach((button) => button.addEventListener('click', () => renderEditor()));
    scope.querySelectorAll('[data-action]').forEach((button) => button.addEventListener('click', () => {
      const record = state.records.find((item) => item.id === button.dataset.id);
      if (!record) return;
      if (button.dataset.action === 'preview') previewBlog(record);
      if (button.dataset.action === 'edit') renderEditor(record);
      if (button.dataset.action === 'delete') remove(record);
    }));
    NC.crud.bindPagination(root, state, renderList);
    NC.crud.bindSort(root, state, renderList);
  }

  function articlePreviewMarkup(data) {
    const image = safeImage(data.image);
    const video = data.video_link ? NC.media.videoPreviewHTML(data.video_link, { title: data.title }) : '';
    const tags = parseTags(data.tags || []).map((tag) => `<span class="article-tag">#${escapeHTML(tag)}</span>`).join('');
    return `
      <article class="article-preview">
        <header>
          <div class="article-kicker"><span>${escapeHTML(data.category_title || categoryName(data))}</span><time>${escapeHTML(formatDate(data.published_date || data.created_at || new Date()))}</time></div>
          <h1>${escapeHTML(data.title || 'Untitled article')}</h1>
          ${data.sub_title ? `<p class="article-subtitle">${escapeHTML(data.sub_title)}</p>` : ''}
          <div class="article-byline">${NC.utils.avatarHTML(data.author_name || authorName(data), data.author_image || authors.find((item) => item.id === data.author_id)?.image, 'article-author-avatar')}<div><strong>${escapeHTML(data.author_name || authorName(data))}</strong><span>${escapeHTML(data.status || 'Draft')} preview · ${Number(data.reading_time_minutes || estimateReadingTime(data.content)).toLocaleString()} min read</span></div></div>
        </header>
        ${image ? `<img class="article-hero" src="${escapeHTML(image)}" alt="${escapeHTML(data.title || '')}" referrerpolicy="no-referrer">` : ''}
        <div class="article-body prose-content">${NC.utils.sanitizeHTML(data.content || '<p>Article content preview will appear here.</p>')}</div>
        ${video ? `<section class="article-attachment"><h2>Watch</h2>${video}</section>` : ''}
        ${safeExternalUrl(data.pdf_book_link) ? `<a class="pdf-callout" href="${escapeHTML(data.pdf_book_link)}" target="_blank" rel="noopener noreferrer"><i class="fa-regular fa-file-pdf" aria-hidden="true"></i><span><strong>Open attached PDF book</strong><small>${escapeHTML(data.pdf_book_link)}</small></span><i class="fa-regular fa-arrow-up-right-from-square" aria-hidden="true"></i></a>` : ''}
        ${tags ? `<footer class="article-tags">${tags}</footer>` : ''}
      </article>`;
  }

  function estimateReadingTime(content) {
    const words = NC.utils.stripHTML(content || '').split(/\s+/).filter(Boolean).length;
    return Math.max(1, Math.ceil(words / 220));
  }

  async function hasRelatedSubmission(blogId) {
    if (!blogId) return false;
    try {
      const related = await NC.api.list('submissions', { select: 'id', filters: { converted_blog_id: blogId }, limit: 1 });
      return related.data.length > 0;
    } catch (error) {
      console.warn('Could not verify shared submission media; remote images will be preserved.', error);
      return true;
    }
  }

  function previewDocument(data) {
    const content = articlePreviewMarkup(data);
    return `<!doctype html><html lang="en"><head><meta charset="utf-8"><meta name="viewport" content="width=device-width,initial-scale=1"><title>${escapeHTML(data.title || 'Article preview')}</title><style>
      :root{color-scheme:light;font-family:Inter,ui-sans-serif,system-ui,sans-serif;color:#172033;background:#f6f4ef}*{box-sizing:border-box}body{margin:0;padding:48px 20px}.article-preview{max-width:820px;margin:auto;background:#fff;border:1px solid #e8e3da;border-radius:24px;padding:clamp(24px,6vw,64px);box-shadow:0 20px 70px rgba(15,23,42,.08)}h1{font-family:Georgia,serif;font-size:clamp(2.2rem,6vw,4rem);line-height:1.03;margin:18px 0}.article-subtitle{font-size:1.2rem;color:#64748b}.article-kicker{display:flex;gap:12px;text-transform:uppercase;letter-spacing:.1em;font-size:.75rem;color:#7c3aed}.article-byline{display:flex;align-items:center;gap:12px;margin:24px 0}.article-author-avatar{width:44px;height:44px;border-radius:50%}.article-byline span{display:block;color:#64748b;font-size:.85rem}.article-hero{width:100%;max-height:520px;object-fit:cover;border-radius:18px;margin:20px 0 36px}.prose-content{font-family:Georgia,serif;font-size:1.12rem;line-height:1.85}.prose-content img{max-width:100%;border-radius:12px}.prose-content table{border-collapse:collapse;width:100%}.prose-content th,.prose-content td{border:1px solid #dbe2ea;padding:10px}.article-tags{display:flex;flex-wrap:wrap;gap:8px;margin-top:32px}.article-tag{padding:6px 11px;border-radius:999px;background:#f3e8ff;color:#7e22ce;font-size:.85rem}.pdf-callout{display:flex;align-items:center;gap:12px;padding:16px;margin-top:28px;border:1px solid #ddd6fe;border-radius:14px;text-decoration:none;color:#5b21b6}.pdf-callout span{flex:1}.pdf-callout small{display:block;color:#64748b;white-space:nowrap;overflow:hidden;text-overflow:ellipsis}.video-frame{aspect-ratio:16/9}.video-frame iframe{width:100%;height:100%;border:0;border-radius:14px}.video-preview-meta,.field-hint{display:none}@media(max-width:640px){body{padding:0}.article-preview{border:0;border-radius:0;padding:24px;min-height:100vh}}</style></head><body>${content}</body></html>`;
  }

  function previewBlog(data) {
    const modal = NC.components.openModal({
      title: 'Article preview', eyebrow: data.status === 'Publish' ? 'Published article' : 'Unpublished draft', size: 'preview',
      content: articlePreviewMarkup(data),
      footer: '<button type="button" class="btn btn-secondary" data-modal-close>Close</button><button type="button" class="btn btn-primary" data-preview-tab><i class="fa-regular fa-arrow-up-right-from-square" aria-hidden="true"></i>Open in new tab</button>',
      onOpen: (modalRoot) => {
        NC.components.bindImageFallbacks(modalRoot);
        modalRoot.querySelector('[data-preview-tab]').addEventListener('click', () => {
          const blob = new Blob([previewDocument(data)], { type: 'text/html' });
          const url = URL.createObjectURL(blob);
          window.open(url, '_blank', 'noopener,noreferrer');
          window.setTimeout(() => URL.revokeObjectURL(url), 60000);
        });
      }
    });
    return modal;
  }

  function selectOptions(items, selected, labelKey = 'title') {
    return items.map((item) => `<option value="${escapeHTML(item.id)}" ${item.id === selected ? 'selected' : ''}>${escapeHTML(item[labelKey] || 'Untitled')}</option>`).join('');
  }

  function renderEditor(record = null) {
    const isEdit = Boolean(record?.id);
    editorCleanup?.();
    editorCleanup = null;
    editorController?.destroy?.();
    root.innerHTML = `
      ${NC.components.pageHeader({ eyebrow: isEdit ? 'Edit article' : 'New article', title: isEdit ? record.title : 'Create a blog', description: 'Write, preview, and publish from a focused editorial workspace.', breadcrumb: [{ label: 'Blogs', route: 'blogs' }, { label: isEdit ? 'Edit' : 'New' }], actions: '<button type="button" class="btn btn-secondary" data-editor-cancel><i class="fa-regular fa-arrow-left" aria-hidden="true"></i>Back to blogs</button>' })}
      <form id="blog-editor-form" class="blog-editor" novalidate>
        <div class="blog-editor-main">
          <section class="surface form-stack">
            ${isEdit ? '' : NC.importer.formControlHTML('blogs')}
            <div class="field"><label class="field-label" for="blog-title">Title <span aria-hidden="true">*</span></label><input class="title-input" id="blog-title" name="title" value="${escapeHTML(record?.title || '')}" placeholder="Enter an article title" autofocus required><p class="field-error hidden" data-field-error="title"></p></div>
            <div class="field"><label class="field-label" for="blog-subtitle">Subtitle</label><textarea class="subtitle-input" id="blog-subtitle" name="sub_title" rows="2" placeholder="Add context or a compelling standfirst">${escapeHTML(record?.sub_title || '')}</textarea></div>
            ${NC.media.imageUploaderHTML({ id: 'blog-hero-image', label: 'Hero image', hint: 'Choose a local image for ImgBB upload, or paste a direct image URL.' })}
            ${NC.editor.editorHTML({ id: 'blog-content', label: 'Article content', hint: 'Use headings and short paragraphs for a readable magazine article.', required: true })}
          </section>
          <details class="surface advanced-panel mt-5" ${isEdit ? '' : 'open'}><summary><span><i class="fa-regular fa-sliders" aria-hidden="true"></i><strong>SEO & Advanced Options</strong></span><i class="fa-regular fa-chevron-down" aria-hidden="true"></i></summary><div class="advanced-content form-stack">
            <div class="form-grid-2"><div class="field"><label class="field-label" for="blog-seo-title">SEO title</label><input class="form-input" id="blog-seo-title" name="seo_title" value="${escapeHTML(record?.seo_title || '')}" maxlength="70"><span class="field-hint">Recommended: 50–60 characters</span></div><div class="field"><label class="field-label" for="blog-seo-description">SEO description</label><textarea class="form-textarea" id="blog-seo-description" name="seo_description" rows="3" maxlength="170">${escapeHTML(record?.seo_description || '')}</textarea></div></div>
            <div class="field"><label class="field-label" for="blog-video">Video link</label><input class="form-input" type="url" id="blog-video" name="video_link" value="${escapeHTML(record?.video_link || '')}" placeholder="YouTube, Facebook, or Instagram URL"><p class="field-error hidden" data-field-error="video_link"></p></div>
            ${NC.media.pdfUploaderHTML({ id: 'blog-pdf-attachment', label: 'PDF attachment' })}
          </div></details>
        </div>
        <aside class="blog-editor-sidebar">
          <section class="surface form-stack"><div class="surface-header compact"><div><p class="eyebrow">Publishing</p><h2>Article details</h2></div></div>
            <div class="field"><label class="field-label" for="blog-status">Status</label><select class="form-select" id="blog-status" name="status"><option value="Draft" ${record?.status !== 'Publish' ? 'selected' : ''}>Draft</option><option value="Publish" ${record?.status === 'Publish' ? 'selected' : ''}>Publish</option></select></div>
            <div class="field"><label class="field-label" for="blog-category">Category <span aria-hidden="true">*</span></label><select class="form-select" id="blog-category" name="category_id" required><option value="">Choose a category</option>${selectOptions(categories, record?.category_id)}</select><p class="field-error hidden" data-field-error="category_id"></p></div>
            <div class="field"><label class="field-label" for="blog-author">Author <span aria-hidden="true">*</span></label><select class="form-select" id="blog-author" name="author_id" required><option value="">Choose an author</option>${selectOptions(authors, record?.author_id)}</select><p class="field-error hidden" data-field-error="author_id"></p></div>
            <div class="field"><label class="field-label" for="blog-tags">Tags</label><input class="form-input" id="blog-tags" name="tags" value="${escapeHTML(Array.isArray(record?.tags) ? record.tags.join(', ') : record?.tags || '')}" placeholder="culture, language, literature"><span class="field-hint">Separate with commas or double spaces.</span></div>
            <div class="field"><div class="field-heading"><label class="field-label" for="blog-slug">Slug <span aria-hidden="true">*</span></label><button type="button" class="field-action" data-generate-blog-slug><i class="fa-regular fa-wand-magic-sparkles" aria-hidden="true"></i>Generate</button></div><input class="form-input" id="blog-slug" name="slug" value="${escapeHTML(record?.slug || '')}" placeholder="article-url-slug"><div class="slug-feedback" data-slug-feedback></div><p class="field-error hidden" data-field-error="slug"></p></div>
            <fieldset class="field"><legend class="field-label">Homepage placement</legend><div class="check-list"><label class="check-row"><input type="checkbox" name="is_slider" ${record?.is_slider ? 'checked' : ''}><span class="check-control"><i class="fa-solid fa-check" aria-hidden="true"></i></span><span>Hero slider</span></label><label class="check-row"><input type="checkbox" name="is_feature" ${record?.is_feature ? 'checked' : ''}><span class="check-control"><i class="fa-solid fa-check" aria-hidden="true"></i></span><span>Featured article</span></label><label class="check-row"><input type="checkbox" name="is_special_article" ${record?.is_special_article ? 'checked' : ''}><span class="check-control"><i class="fa-solid fa-check" aria-hidden="true"></i></span><span>Special article</span></label></div></fieldset>
          </section>
          <section class="surface mt-5"><p class="eyebrow mb-3">At a glance</p><div class="editor-summary"><div><span>Words</span><strong data-blog-word-count>0</strong></div><div><span>Reading time</span><strong data-blog-read-time>1 min</strong></div><div><span>Last saved</span><strong>${escapeHTML(record?.updated_at ? formatDate(record.updated_at) : 'Not saved')}</strong></div></div></section>
        </aside>
        <div class="editor-action-bar"><div><span class="save-indicator"><i class="fa-regular fa-shield-check" aria-hidden="true"></i>Content is sanitized before save</span></div><div class="editor-actions"><button type="button" class="btn btn-secondary" data-editor-cancel>Cancel</button><button type="button" class="btn btn-secondary" data-blog-preview><i class="fa-regular fa-eye" aria-hidden="true"></i>Preview</button><button type="button" class="btn btn-secondary" data-blog-save="Draft"><i class="fa-regular fa-floppy-disk" aria-hidden="true"></i>Save draft</button><button type="button" class="btn btn-primary" data-blog-save="Publish"><i class="fa-regular fa-paper-plane" aria-hidden="true"></i>Publish</button></div></div>
      </form>`;

    const form = root.querySelector('#blog-editor-form');
    const initialHero = {
      url: record?.image || '',
      delete_url: record?.imgbb_delete_url || '',
      image_meta: record?.image_meta || {}
    };
    const initialPdf = {
      url: record?.pdf_book_link || '',
      path: record?.pdf_storage_path || '',
      provider: record?.pdf_file_provider || 'url',
      size: Number(record?.pdf_file_size_mb || 0) * 1024 * 1024
    };
    const heroSessionUploads = [];
    const pdfSessionUploads = [];
    let editorClosed = false;
    heroUploader = NC.media.mountImageUploader(root.querySelector('#blog-hero-image'), {
      initial: initialHero,
      label: 'Hero image',
      onChange: (next) => {
        if (next?.provider !== 'imgbb' || !next.delete_url) return;
        if (!heroSessionUploads.some((item) => item.url === next.url)) heroSessionUploads.push({ ...next });
        if (editorClosed) NC.crud.deleteMediaRecords([next]);
      }
    });
    pdfUploader = NC.media.mountPdfUploader(root.querySelector('#blog-pdf-attachment'), {
      initial: initialPdf,
      onChange: (next) => {
        if (next?.provider !== 'supabase-storage' || !next.path) return;
        if (!pdfSessionUploads.some((item) => item.path === next.path)) pdfSessionUploads.push({ ...next });
        if (editorClosed) {
          NC.api.deleteStorageObject(NC_CONFIG.supabase.pdfBucket, next.path).then((result) => {
            if (!result.ok) NC.components.toast('A PDF completed after the editor closed and could not be cleaned up automatically.', 'warning');
          });
        }
      }
    });
    editorController = NC.editor.mountEditor(root.querySelector('#blog-content'), {
      initial: record?.content || '',
      media: Array.isArray(record?.inline_media) ? record.inline_media : [],
      required: true,
      label: 'Article content',
      onChange: updateWordCount
    });
    const title = form.elements.title;
    const slug = form.elements.slug;
    let slugTouched = Boolean(record?.slug);
    function updateWordCount(changedHtml) {
      const html = typeof changedHtml === 'string'
        ? changedHtml
        : (editorController?.getValue?.() || record?.content || '');
      const words = NC.utils.stripHTML(html).split(/\s+/).filter(Boolean).length;
      root.querySelector('[data-blog-word-count]').textContent = words.toLocaleString();
      root.querySelector('[data-blog-read-time]').textContent = `${Math.max(1, Math.ceil(words / 220))} min`;
    }
    title.addEventListener('input', debounce(() => { if (!slugTouched) slug.value = slugify(title.value); }, 100));
    slug.addEventListener('input', () => { slugTouched = true; root.querySelector('[data-slug-feedback]').textContent = ''; });
    root.querySelector('[data-generate-blog-slug]').addEventListener('click', () => { slug.value = slugify(title.value); slugTouched = true; checkSlug(); });
    async function checkSlug() {
      const clean = slugify(slug.value); slug.value = clean;
      const feedback = root.querySelector('[data-slug-feedback]');
      if (!clean) { feedback.innerHTML = ''; return false; }
      feedback.innerHTML = '<i class="fa-regular fa-spinner-third fa-spin" aria-hidden="true"></i>Checking availability…';
      try {
        const exists = await NC.api.slugExists(clean, record?.id || '');
        feedback.className = `slug-feedback ${exists ? 'is-error' : 'is-success'}`;
        feedback.innerHTML = exists ? '<i class="fa-regular fa-circle-xmark" aria-hidden="true"></i>This slug is already in use.' : '<i class="fa-regular fa-circle-check" aria-hidden="true"></i>Slug is available.';
        return !exists;
      } catch (error) {
        feedback.className = 'slug-feedback is-error'; feedback.textContent = NC.api.userMessage(error, 'Could not validate the slug.'); return false;
      }
    }
    slug.addEventListener('blur', checkSlug);
    if (!isEdit) NC.importer.mountFormControl(root, {
      type: 'blogs',
      onRecord: ({ payload }) => {
        NC.importer.fillForm(form, payload);
        heroUploader.setValue(payload.image || '');
        pdfUploader.setValue({ url: payload.pdf_book_link || '', provider: 'url', file_size_mb: payload.pdf_file_size_mb || 0 });
        editorController.setValue(payload.content || '');
        slugTouched = Boolean(payload.slug);
        updateWordCount(payload.content || '');
        checkSlug();
      }
    });

    function collectData(statusOverride) {
      const data = formData(form);
      const category = categories.find((item) => item.id === data.category_id);
      const author = authors.find((item) => item.id === data.author_id);
      const hero = heroUploader.getValue();
      const pdf = pdfUploader.getValue();
      const content = editorController.getValue();
      return {
        ...record,
        title: data.title,
        sub_title: data.sub_title,
        ...NC.crud.imagePayload(hero),
        content,
        inline_media: editorController.getMedia(),
        category_id: data.category_id,
        category_title: category?.title || record?.category_title || '',
        category_slug: category?.slug || record?.category_slug || '',
        author_id: data.author_id,
        author_name: author?.title || record?.author_name || '',
        author_image: author?.image || record?.author_image || '',
        status: statusOverride || data.status || 'Draft',
        tags: parseTags(data.tags),
        seo_title: data.seo_title,
        seo_description: data.seo_description,
        video_link: data.video_link,
        pdf_book_link: pdf.url || '',
        pdf_file_provider: pdf.provider || 'url',
        pdf_storage_path: pdf.path || '',
        pdf_file_size_mb: pdf.size ? Number((pdf.size / 1024 / 1024).toFixed(2)) : 0,
        slug: slugify(data.slug),
        is_slider: Boolean(data.is_slider),
        is_feature: Boolean(data.is_feature),
        is_special_article: Boolean(data.is_special_article),
        reading_time_minutes: estimateReadingTime(content),
        published_date: (statusOverride || data.status) === 'Publish' ? (record?.published_date || new Date().toISOString().slice(0, 10)) : record?.published_date || null,
        created_at: record?.created_at || new Date().toISOString()
      };
    }

    async function cleanupEditorUploads({ saved = false, payload = null, preserveExisting = false } = {}) {
      editorClosed = true;
      const keepInlineUrls = new Set((payload?.inline_media || []).map((item) => item.url));
      const cleanupCandidates = saved && !preserveExisting ? editorController.getInactiveMedia() : editorController.getSessionUploads();
      const inlineToDelete = cleanupCandidates.filter((item) => !keepInlineUrls.has(item.url));
      const heroToDelete = heroSessionUploads.filter((item) => !saved || item.url !== payload?.image);
      await NC.crud.deleteMediaRecords([...heroToDelete, ...inlineToDelete]);

      const paths = [...new Set(pdfSessionUploads.map((item) => item.path).filter((path) => path && (!saved || path !== payload?.pdf_storage_path)))];
      const results = await Promise.all(paths.map((path) => NC.api.deleteStorageObject(NC_CONFIG.supabase.pdfBucket, path)));
      if (results.some((result) => !result.ok)) {
        NC.components.toast('An unused PDF upload could not be removed from Supabase Storage. Review the pdf-books bucket.', 'warning');
      }
    }
    editorCleanup = cleanupEditorUploads;

    root.querySelector('[data-blog-preview]').addEventListener('click', () => previewBlog(collectData()));
    root.querySelectorAll('[data-editor-cancel]').forEach((button) => button.addEventListener('click', async () => {
      if (heroUploader.isUploading() || pdfUploader.isUploading()) {
        NC.components.toast('Wait for active media uploads to finish before leaving the editor.', 'warning');
        return;
      }
      const leave = await NC.components.confirm({ title: 'Leave the editor?', description: 'Unsaved changes will be lost. Newly uploaded files will be cleaned up where the provider permits it.', danger: false, confirmLabel: 'Leave editor', confirmIcon: 'fa-arrow-left' });
      if (leave) {
        await cleanupEditorUploads();
        editorCleanup = null;
        render(root);
      }
    }));
    root.querySelectorAll('[data-blog-save]').forEach((button) => button.addEventListener('click', () => saveBlog(button.dataset.blogSave, button)));

    async function saveBlog(status, button) {
      const payload = collectData(status);
      form.elements.status.value = status;
      const errors = {
        title: payload.title ? '' : 'Blog title is required.',
        category_id: payload.category_id ? '' : 'Choose a category.',
        author_id: payload.author_id ? '' : 'Choose an author.',
        slug: payload.slug ? '' : 'Generate or enter a valid slug.',
        video_link: payload.video_link && !NC.utils.isValidUrl(payload.video_link, { allowEmpty: false }) ? 'Enter a valid video URL.' : ''
      };
      const valid = validateFields(form, errors) && editorController.validate() && heroUploader.validate() && pdfUploader.validate();
      if (!valid) return;
      if (heroUploader.isUploading() || pdfUploader.isUploading()) {
        NC.components.toast('Wait for all media uploads to finish.', 'warning');
        return;
      }
      NC.utils.setButtonLoading(button, true, status === 'Publish' ? 'Publishing…' : 'Saving…');
      try {
        if (!await checkSlug()) { validateFields(form, { slug: 'Choose a unique slug.' }); return; }
        const preserveSharedMedia = await hasRelatedSubmission(record?.id);
        const preservedSharedImages = preserveSharedMedia && (
          Boolean(record?.imgbb_delete_url && record.imgbb_delete_url !== payload.imgbb_delete_url)
          || editorController.getRemovedMedia().some((item) => item.delete_url)
        );
        const cleanPayload = {
          title: payload.title,
          sub_title: payload.sub_title,
          image: payload.image,
          imgbb_delete_url: payload.imgbb_delete_url,
          image_meta: payload.image_meta,
          content: payload.content,
          inline_media: payload.inline_media,
          category_id: payload.category_id || null,
          author_id: payload.author_id || null,
          category_title: payload.category_title,
          category_slug: payload.category_slug,
          author_name: payload.author_name,
          author_image: payload.author_image,
          status: payload.status,
          tags: payload.tags,
          seo_title: payload.seo_title,
          seo_description: payload.seo_description,
          video_link: payload.video_link,
          pdf_book_link: payload.pdf_book_link,
          pdf_file_provider: payload.pdf_file_provider,
          pdf_storage_path: payload.pdf_storage_path,
          pdf_file_size_mb: payload.pdf_file_size_mb,
          slug: payload.slug,
          is_slider: payload.is_slider,
          is_feature: payload.is_feature,
          is_special_article: payload.is_special_article,
          published_date: payload.published_date
        };
        const saved = await NC.crud.save('blogs', record?.id, cleanPayload);
        if (!preserveSharedMedia) {
          await NC.crud.deleteReplacedMedia({ delete_url: record?.imgbb_delete_url || '' }, { delete_url: payload.imgbb_delete_url || '' });
        }
        if (record?.pdf_storage_path && record.pdf_storage_path !== payload.pdf_storage_path) {
          const result = await NC.api.deleteStorageObject(NC_CONFIG.supabase.pdfBucket, record.pdf_storage_path);
          if (!result.ok) NC.components.toast('The Blog was saved, but its replaced PDF could not be removed from Supabase Storage.', 'warning');
        }
        await cleanupEditorUploads({ saved: true, payload, preserveExisting: preserveSharedMedia });
        editorCleanup = null;
        if (preservedSharedImages) NC.components.toast('Source submission images were preserved.', 'success');
        NC.components.toast(status === 'Publish' ? 'Blog published successfully.' : 'Draft saved successfully.', 'success');
        await load({}, saved?.id || record?.id);
      } catch (error) {
        console.error(error); NC.components.toast(NC.api.userMessage(error, 'Unable to save the blog. Please try again.'), 'error');
      } finally { NC.utils.setButtonLoading(button, false); }
    }
    updateWordCount();
  }

  async function remove(record) {
    try {
      const mediaIsShared = await hasRelatedSubmission(record.id);
      const inlineDeleteUrls = (Array.isArray(record.inline_media) ? record.inline_media : []).map((item) => item.delete_url);
      const deleted = await NC.crud.deleteRecord({
        table: 'blogs',
        record,
        label: 'blog',
        remoteDeleteUrls: mediaIsShared ? [] : [record.imgbb_delete_url, ...inlineDeleteUrls],
        storageObjects: record.pdf_storage_path ? [{ bucket: NC_CONFIG.supabase.pdfBucket, path: record.pdf_storage_path }] : []
      });
      if (deleted) {
        await load();
        if (mediaIsShared) NC.components.toast('Shared submission images were preserved.', 'success');
      }
    } catch (error) {
      console.error(error); NC.components.toast(NC.api.userMessage(error, 'Unable to delete the blog.'), 'error');
    }
  }

  function renderListShell() {
    root.innerHTML = `
      ${NC.components.pageHeader({ eyebrow: 'Editorial content', title: 'Blogs', description: 'Draft, review, preview, and publish magazine articles.', breadcrumb: [{ label: 'Blogs' }], actions: `${NC.importer.bulkButton('blogs')}<button type="button" class="btn btn-primary" data-add-blog><i class="fa-regular fa-plus" aria-hidden="true"></i>Add blog</button>` })}
      <section class="surface"><div class="list-toolbar"><label class="search-field"><i class="fa-regular fa-magnifying-glass" aria-hidden="true"></i><span class="sr-only">Search blogs</span><input type="search" placeholder="Search title, author, category, tag…" data-blog-search></label><select class="form-select toolbar-select" data-blog-status aria-label="Filter by status"><option value="all">All statuses</option><option value="Publish">Published</option><option value="Draft">Draft</option></select><select class="form-select toolbar-select" data-blog-category aria-label="Filter by category"><option value="all">All categories</option>${categories.map((item) => `<option value="${escapeHTML(item.id)}">${escapeHTML(item.title)}</option>`).join('')}</select></div><div data-blogs-content>${NC.components.skeleton(7, 6)}</div></section>`;
    root.querySelector('[data-add-blog]').addEventListener('click', () => renderEditor());
    root.querySelector('[data-blog-search]').addEventListener('input', debounce((event) => { state.setQuery(event.target.value); renderList(); }, 220));
    root.querySelector('[data-blog-status]').addEventListener('change', (event) => { state.setFilter('status', event.target.value); renderList(); });
    root.querySelector('[data-blog-category]').addEventListener('change', (event) => { state.setFilter('category_id', event.target.value); renderList(); });
    NC.importer.bindBulk(root, { type: 'blogs', onComplete: () => load() });
  }

  async function load(context = {}, editId = '') {
    editorController?.destroy?.();
    editorController = null;
    heroUploader = null;
    pdfUploader = null;
    root.innerHTML = `${NC.components.pageHeader({ eyebrow: 'Editorial content', title: 'Blogs', description: 'Loading articles and reference data…', breadcrumb: [{ label: 'Blogs' }] })}${NC.components.skeleton(7, 6)}`;
    try {
      const [blogsResult, authorsResult, categoriesResult] = await Promise.all([
        NC.api.list('blogs', { select: '*', order: 'created_at.desc', limit: 2000 }),
        NC.api.list('authors', { select: '*', order: 'title.asc', limit: 1000 }),
        NC.api.list('categories', { select: '*', order: 'title.asc', limit: 1000 })
      ]);
      if (NC.crud.isStaleNavigation(context)) return;
      authors = authorsResult.data; categories = categoriesResult.data; state.setRecords(blogsResult.data);
      const statusFilter = context.params?.get('filter') || 'all';
      const categoryFilter = context.params?.get('category') || 'all';
      state.setFilter('status', ['Publish', 'Draft'].includes(statusFilter) ? statusFilter : 'all');
      state.setFilter('category_id', categoryFilter);
      renderListShell();
      root.querySelector('[data-blog-status]').value = ['Publish', 'Draft'].includes(statusFilter) ? statusFilter : 'all';
      if (categories.some((item) => item.id === categoryFilter)) root.querySelector('[data-blog-category]').value = categoryFilter;
      renderList();
      const action = context.params?.get('action');
      const id = editId || context.params?.get('id');
      if (action === 'new') renderEditor();
      else if (id && action === 'view') { const record = state.records.find((item) => item.id === id); if (record) previewBlog(record); }
      else if (id && (action === 'edit' || editId)) { const record = state.records.find((item) => item.id === id); if (record) renderEditor(record); }
    } catch (error) { NC.crud.handleLoadError(root, error, () => load(context), context); }
  }

  function render(container, context = {}) { root = container; return load(context); }

  NC.views.blogs = {
    render,
    destroy: () => {
      editorCleanup?.();
      editorCleanup = null;
      editorController?.destroy?.();
      editorController = null;
      heroUploader = null;
      pdfUploader = null;
    }
  };
})(window.NC);
