(function (NC) {
  'use strict';

  const { escapeHTML, formatDate, formData, validateFields, debounce, safeImage, safeExternalUrl } = NC.utils;
  const state = new NC.crud.ListState('books', { searchFields: ['title', 'author_or_editor', 'edition', 'category', 'description'], sortKey: 'book_published_date' });
  let root;

  function renderList() {
    const content = root.querySelector('[data-books-content]');
    const { rows, total } = state.paged();
    if (!total) {
      content.innerHTML = NC.components.emptyState({
        icon: 'fa-books', title: state.query ? 'No books match your search' : 'Start the digital library',
        description: state.query ? 'Try a different title, editor, or category.' : 'Add magazines, books, and archival PDF editions.',
        action: state.query ? '' : '<button type="button" class="btn btn-primary" data-add-book><i class="fa-regular fa-book-circle-plus" aria-hidden="true"></i>Add book</button>'
      }); bindEvents(content); return;
    }
    const body = rows.map((record) => `
      <tr>
        <td data-label="Book"><div class="book-cell">${safeImage(record.image) ? `<img src="${escapeHTML(safeImage(record.image))}" alt="" loading="lazy" referrerpolicy="no-referrer" data-image-fallback>` : '<span><i class="fa-regular fa-book-open" aria-hidden="true"></i></span>'}<div><strong>${escapeHTML(record.title)}</strong><small>${escapeHTML(record.edition || record.category || 'No edition')}</small></div></div></td>
        <td data-label="Author / Editor">${escapeHTML(record.author_or_editor || '—')}</td>
        <td data-label="Published">${escapeHTML(formatDate(record.book_published_date))}</td>
        <td data-label="File">${safeExternalUrl(record.link) ? `<a class="file-link" href="${escapeHTML(record.link)}" target="_blank" rel="noopener noreferrer"><i class="fa-regular fa-file-pdf" aria-hidden="true"></i>${record.file_size_mb ? `${Number(record.file_size_mb).toFixed(1)} MB` : 'Open PDF'}</a>` : '<span class="text-faint">No file</span>'}</td>
        <td data-label="Actions" class="text-right">${NC.components.rowActions([
          { action: 'view', id: record.id, label: 'Preview book', icon: 'fa-eye' },
          { action: 'edit', id: record.id, label: 'Edit book', icon: 'fa-pen' },
          { action: 'delete', id: record.id, label: 'Delete book', icon: 'fa-trash', danger: true }
        ])}</td>
      </tr>`).join('');
    content.innerHTML = `${NC.components.tableShell({ caption: 'PDF books', minWidth: '850px', head: `<tr><th><button type="button" data-sort="title">Book ${NC.crud.sortIcon(state, 'title')}</button></th><th>Author / Editor</th><th><button type="button" data-sort="book_published_date">Published ${NC.crud.sortIcon(state, 'book_published_date')}</button></th><th>File</th><th class="text-right">Actions</th></tr>`, body })}${NC.components.pagination({ page: state.page, pageSize: state.pageSize, total })}`;
    bindEvents(content); NC.components.bindImageFallbacks(content);
  }

  function bindEvents(scope = root) {
    scope.querySelectorAll('[data-add-book]').forEach((button) => button.addEventListener('click', () => openForm()));
    scope.querySelectorAll('[data-action]').forEach((button) => button.addEventListener('click', () => {
      const record = state.records.find((item) => item.id === button.dataset.id); if (!record) return;
      if (button.dataset.action === 'view') openView(record);
      if (button.dataset.action === 'edit') openForm(record);
      if (button.dataset.action === 'delete') remove(record);
    }));
    NC.crud.bindPagination(root, state, renderList); NC.crud.bindSort(root, state, renderList);
  }

  function bookPreviewMarkup(data) {
    return `<article class="book-preview"><div class="book-preview-cover">${safeImage(data.image) ? `<img src="${escapeHTML(safeImage(data.image))}" alt="Cover of ${escapeHTML(data.title || '')}" referrerpolicy="no-referrer">` : '<span><i class="fa-regular fa-book-open" aria-hidden="true"></i></span>'}</div><div class="book-preview-copy"><p class="eyebrow">${escapeHTML(data.category || 'Digital library')}</p><h2>${escapeHTML(data.title || 'Untitled book')}</h2><p class="book-credit">${escapeHTML(data.author_or_editor || 'Author or editor not set')}</p><div class="badge-wrap mt-3">${data.edition ? `<span class="status-badge status-neutral"><i class="fa-regular fa-bookmark" aria-hidden="true"></i>${escapeHTML(data.edition)}</span>` : ''}${data.book_published_date ? `<span class="status-badge status-neutral"><i class="fa-regular fa-calendar" aria-hidden="true"></i>${escapeHTML(formatDate(data.book_published_date))}</span>` : ''}${data.page_count ? `<span class="status-badge status-neutral"><i class="fa-regular fa-file-lines" aria-hidden="true"></i>${Number(data.page_count).toLocaleString()} pages</span>` : ''}</div><p class="mt-5 text-muted-foreground leading-7">${escapeHTML(data.description || 'No description has been added.')}</p>${safeExternalUrl(data.link) ? `<a class="btn btn-primary mt-6" href="${escapeHTML(data.link)}" target="_blank" rel="noopener noreferrer"><i class="fa-regular fa-file-pdf" aria-hidden="true"></i>Open PDF</a>` : '<p class="notice notice-warning mt-6"><i class="fa-regular fa-triangle-exclamation" aria-hidden="true"></i>No PDF link is attached.</p>'}</div></article>`;
  }

  function openView(record) {
    NC.components.openModal({ title: record.title, eyebrow: 'PDF book preview', size: 'xl', content: bookPreviewMarkup(record), footer: '<button type="button" class="btn btn-secondary" data-modal-close>Close</button><button type="button" class="btn btn-primary" data-book-edit><i class="fa-regular fa-pen" aria-hidden="true"></i>Edit book</button>', onOpen: (modalRoot) => modalRoot.querySelector('[data-book-edit]').addEventListener('click', () => { NC.components.closeModal(); window.setTimeout(() => openForm(record), 180); }) });
  }

  function openForm(record = null) {
    let imageUploader, pdfUploader;
    const initialImage = { url: record?.image || '', delete_url: record?.imgbb_delete_url || '', image_meta: record?.image_meta || {} };
    const initialPdf = { url: record?.link || '', path: record?.file_storage_path || '', provider: record?.file_provider || 'url', size: Number(record?.file_size_mb || 0) * 1024 * 1024 };
    NC.components.openModal({
      title: record ? 'Edit PDF book' : 'Add PDF book', eyebrow: 'Digital library', size: 'xl',
      description: 'Use a wide cover image (approximately 2:1) and a stable public PDF link.',
      content: `<form id="book-form" class="form-stack" novalidate>${record ? '' : NC.importer.formControlHTML('books')}<div class="form-grid-2"><div class="field"><label class="field-label" for="book-title">Title <span aria-hidden="true">*</span></label><input class="form-input" id="book-title" name="title" value="${escapeHTML(record?.title || '')}" autofocus><p class="field-error hidden" data-field-error="title"></p></div><div class="field"><label class="field-label" for="book-date">Book published date</label><input class="form-input" type="date" id="book-date" name="book_published_date" value="${escapeHTML((record?.book_published_date || '').slice(0, 10))}"></div></div>${NC.media.imageUploaderHTML({ id: 'book-cover', label: 'Cover image', hint: 'Recommended ratio: approximately 2:1. Images are uploaded to ImgBB.' })}${NC.media.pdfUploaderHTML({ id: 'book-pdf', label: 'PDF book link' })}<div class="form-grid-2"><div class="field"><label class="field-label" for="book-author">Author or editor</label><input class="form-input" id="book-author" name="author_or_editor" value="${escapeHTML(record?.author_or_editor || '')}"></div><div class="field"><label class="field-label" for="book-edition">Edition</label><input class="form-input" id="book-edition" name="edition" value="${escapeHTML(record?.edition || '')}" placeholder="2026 edition"></div></div><div class="form-grid-2"><div class="field"><label class="field-label" for="book-category">Category</label><input class="form-input" id="book-category" name="category" value="${escapeHTML(record?.category || '')}" placeholder="Annual anthology"></div><div class="field"><label class="field-label" for="book-pages">Page count</label><input class="form-input" type="number" min="0" id="book-pages" name="page_count" value="${escapeHTML(record?.page_count || '')}"></div></div><div class="field"><label class="field-label" for="book-description">Description</label><textarea class="form-textarea min-h-28" id="book-description" name="description">${escapeHTML(record?.description || '')}</textarea></div></form>`,
      footer: `<button type="button" class="btn btn-secondary" data-modal-close>Cancel</button><button type="button" class="btn btn-secondary" data-preview-book><i class="fa-regular fa-eye" aria-hidden="true"></i>Preview</button><button type="submit" form="book-form" class="btn btn-primary" data-save-book><i class="fa-regular fa-floppy-disk" aria-hidden="true"></i>${record ? 'Save changes' : 'Add book'}</button>`,
      onOpen: (modalRoot) => {
        const form = modalRoot.querySelector('#book-form');
        imageUploader = NC.media.mountImageUploader(modalRoot.querySelector('#book-cover'), { initial: initialImage });
        pdfUploader = NC.media.mountPdfUploader(modalRoot.querySelector('#book-pdf'), { initial: initialPdf });
        const collect = () => {
          const data = formData(form), image = imageUploader.getValue(), pdf = pdfUploader.getValue();
          return { ...data, image: image.url, link: pdf.url, file_provider: pdf.provider, file_storage_path: pdf.path, file_size_mb: pdf.size ? Number((pdf.size / 1024 / 1024).toFixed(2)) : Number(record?.file_size_mb || 0) };
        };
        if (!record) NC.importer.mountFormControl(modalRoot, {
          type: 'books',
          onRecord: ({ payload }) => {
            NC.importer.fillForm(form, payload);
            imageUploader.setValue(payload.image || '');
            pdfUploader.setValue({ url: payload.link || '', provider: 'url', file_size_mb: payload.file_size_mb || 0 });
          }
        });
        modalRoot.querySelector('[data-preview-book]').addEventListener('click', () => {
          const data = collect();
          const cover = safeImage(data.image);
          const documentHtml = `<!doctype html><html><head><meta charset="utf-8"><meta name="viewport" content="width=device-width,initial-scale=1"><title>${escapeHTML(data.title || 'Book preview')}</title><style>*{box-sizing:border-box}body{margin:0;background:#f5f1e9;color:#1e293b;font-family:system-ui;padding:clamp(20px,6vw,72px)}article{display:grid;grid-template-columns:minmax(220px,380px) 1fr;gap:clamp(28px,6vw,80px);max-width:1100px;margin:auto;align-items:center}img,.cover{width:100%;aspect-ratio:2/1;object-fit:cover;border-radius:20px;background:#e2e8f0;display:grid;place-items:center}.eyebrow{text-transform:uppercase;letter-spacing:.12em;color:#7c3aed;font-size:.75rem;font-weight:700}h1{font-family:Georgia,serif;font-size:clamp(2.5rem,6vw,5rem);line-height:1;margin:.4em 0}.credit{font-size:1.1rem;color:#64748b}.description{line-height:1.8;color:#475569;margin-top:28px}a{display:inline-block;margin-top:24px;background:#7c3aed;color:white;text-decoration:none;border-radius:12px;padding:12px 18px;font-weight:700}@media(max-width:700px){article{grid-template-columns:1fr}}</style></head><body><article><div>${cover ? `<img src="${escapeHTML(cover)}" alt="">` : '<div class="cover">No cover</div>'}</div><div><p class="eyebrow">${escapeHTML(data.category || 'Digital library')}</p><h1>${escapeHTML(data.title || 'Untitled book')}</h1><p class="credit">${escapeHTML(data.author_or_editor || 'Author or editor not set')} ${data.edition ? `· ${escapeHTML(data.edition)}` : ''}</p><p class="description">${escapeHTML(data.description || 'No description has been added.')}</p>${safeExternalUrl(data.link) ? `<a href="${escapeHTML(data.link)}" target="_blank" rel="noopener">Open PDF</a>` : ''}</div></article></body></html>`;
          const blob = new Blob([documentHtml], { type: 'text/html' });
          const url = URL.createObjectURL(blob); window.open(url, '_blank', 'noopener,noreferrer'); window.setTimeout(() => URL.revokeObjectURL(url), 60000);
        });
        form.addEventListener('submit', async (event) => {
          event.preventDefault(); const data = collect();
          const errors = { title: data.title ? '' : 'Book title is required.' };
          if (!validateFields(form, errors) || !imageUploader.validate() || !pdfUploader.validate() || imageUploader.isUploading() || pdfUploader.isUploading()) {
            if (imageUploader.isUploading() || pdfUploader.isUploading()) NC.components.toast('Wait for all uploads to finish.', 'warning'); return;
          }
          const image = imageUploader.getValue(), pdf = pdfUploader.getValue(), button = modalRoot.querySelector('[data-save-book]'); NC.utils.setButtonLoading(button, true, 'Saving…');
          try {
            await NC.crud.save('books', record?.id, {
              title: data.title, book_published_date: data.book_published_date || null,
              author_or_editor: data.author_or_editor, edition: data.edition, category: data.category,
              page_count: Math.max(0, Number(data.page_count || 0)), description: data.description,
              ...NC.crud.imagePayload(image), link: pdf.url, file_provider: pdf.provider || 'url',
              file_storage_path: pdf.path || '', file_size_mb: data.file_size_mb
            });
            await NC.crud.deleteReplacedMedia({ delete_url: record?.imgbb_delete_url }, image);
            if (record?.file_storage_path && record.file_storage_path !== pdf.path) {
              const result = await NC.api.deleteStorageObject(NC_CONFIG.supabase.pdfBucket, record.file_storage_path);
              if (!result.ok) NC.components.toast('The book was saved, but the old PDF could not be removed from storage.', 'warning');
            }
            NC.components.toast(`PDF book ${record ? 'updated' : 'added'} successfully.`, 'success'); NC.components.closeModal(); await load();
          } catch (error) { console.error(error); NC.components.toast(NC.api.userMessage(error, 'Unable to save the PDF book.'), 'error'); }
          finally { NC.utils.setButtonLoading(button, false); }
        });
      }
    });
  }

  async function remove(record) {
    try {
      const deleted = await NC.crud.deleteRecord({ table: 'books', record, label: 'PDF book', remoteDeleteUrls: [record.imgbb_delete_url], storageObjects: record.file_storage_path ? [{ bucket: NC_CONFIG.supabase.pdfBucket, path: record.file_storage_path }] : [] });
      if (deleted) await load();
    } catch (error) { console.error(error); NC.components.toast(NC.api.userMessage(error, 'Unable to delete the PDF book.'), 'error'); }
  }

  async function load(context = {}) {
    const content = root.querySelector('[data-books-content]'); content.innerHTML = NC.components.skeleton(7, 5);
    try {
      const result = await NC.api.list('books', { select: '*', order: 'book_published_date.desc.nullslast,created_at.desc', limit: 2000 });
      if (NC.crud.isStaleNavigation(context)) return;
      state.setRecords(result.data); renderList();
      const action = context.params?.get('action'), id = context.params?.get('id');
      if (action === 'new') openForm();
      if (id && ['view', 'edit'].includes(action)) { const record = state.records.find((item) => item.id === id); if (record) action === 'view' ? openView(record) : openForm(record); }
    } catch (error) { NC.crud.handleLoadError(content, error, () => load(context), context); }
  }

  function render(container, context = {}) {
    root = container;
    root.innerHTML = `${NC.components.pageHeader({ eyebrow: 'Library', title: 'PDF Books', description: 'Manage magazine editions, books, and downloadable archives.', breadcrumb: [{ label: 'PDF Books' }], actions: `${NC.importer.bulkButton('books')}<button type="button" class="btn btn-primary" data-add-book><i class="fa-regular fa-book-circle-plus" aria-hidden="true"></i>Add book</button>` })}<section class="surface"><div class="list-toolbar"><label class="search-field"><i class="fa-regular fa-magnifying-glass" aria-hidden="true"></i><span class="sr-only">Search books</span><input type="search" placeholder="Search title, editor, edition…" data-book-search></label><input class="form-input toolbar-select" type="date" data-book-date aria-label="Filter by published date"></div><div data-books-content>${NC.components.skeleton(7, 5)}</div></section>`;
    root.querySelector('[data-add-book]').addEventListener('click', () => openForm());
    root.querySelector('[data-book-search]').addEventListener('input', debounce((event) => { state.setQuery(event.target.value); renderList(); }, 220));
    root.querySelector('[data-book-date]').addEventListener('change', (event) => { const value = event.target.value; state.setFilter('book_published_date', value ? (date) => String(date || '').slice(0, 10) === value : 'all'); renderList(); });
    NC.importer.bindBulk(root, { type: 'books', onComplete: () => load(context) });
    return load(context);
  }

  NC.views.books = { render };
})(window.NC);
