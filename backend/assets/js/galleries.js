(function (NC) {
  'use strict';

  const { escapeHTML, formatDate, formData, validateFields, debounce, safeImage } = NC.utils;
  const state = new NC.crud.ListState('galleries', { searchFields: ['title', 'description', 'category'], sortKey: 'created_at' });
  let root;
  let viewMode = NC.utils.readPreference('gallery-view', 'grid');

  function actionSet(record) {
    return NC.components.rowActions([
      { action: 'view', id: record.id, label: 'Preview gallery item', icon: 'fa-eye' },
      { action: 'edit', id: record.id, label: 'Edit gallery item', icon: 'fa-pen' },
      { action: 'delete', id: record.id, label: 'Delete gallery item', icon: 'fa-trash', danger: true }
    ]);
  }

  function renderList() {
    const content = root.querySelector('[data-galleries-content]');
    const { rows, total } = state.paged();
    root.querySelectorAll('[data-gallery-view]').forEach((button) => {
      const active = button.dataset.galleryView === viewMode;
      button.classList.toggle('is-active', active); button.setAttribute('aria-pressed', String(active));
    });
    if (!total) {
      content.innerHTML = NC.components.emptyState({
        icon: 'fa-images', title: state.query ? 'No gallery items found' : 'Build the visual archive',
        description: state.query ? 'Try changing your search or category filter.' : 'Upload photographs and visual records for Ningshing Che.',
        action: state.query ? '' : '<button type="button" class="btn btn-primary" data-add-gallery><i class="fa-regular fa-image-polaroid" aria-hidden="true"></i>Add image</button>'
      }); bindListEvents(content); return;
    }
    if (viewMode === 'grid') {
      content.innerHTML = `<div class="gallery-grid">${rows.map((record) => `
        <article class="gallery-card">
          <button type="button" class="gallery-card-image" data-action="view" data-id="${escapeHTML(record.id)}" aria-label="Preview ${escapeHTML(record.title)}">${safeImage(record.image) ? `<img src="${escapeHTML(safeImage(record.image))}" alt="${escapeHTML(record.title)}" loading="lazy" referrerpolicy="no-referrer" data-image-fallback>` : '<span><i class="fa-regular fa-image-slash" aria-hidden="true"></i></span>'}<span class="gallery-card-overlay"><i class="fa-regular fa-expand" aria-hidden="true"></i></span></button>
          <div class="gallery-card-body"><div><span class="content-label">${escapeHTML(record.category || 'Uncategorized')}</span><h3>${escapeHTML(record.title)}</h3><p>${escapeHTML(NC.utils.truncate(record.description || 'No description', 90))}</p></div><div class="gallery-card-footer"><time>${escapeHTML(formatDate(record.created_at))}</time>${actionSet(record)}</div></div>
        </article>`).join('')}</div>${NC.components.pagination({ page: state.page, pageSize: state.pageSize, total })}`;
    } else {
      const body = rows.map((record) => `
        <tr><td data-label="Image"><div class="article-cell">${safeImage(record.image) ? `<img src="${escapeHTML(safeImage(record.image))}" alt="" loading="lazy" referrerpolicy="no-referrer" data-image-fallback>` : '<span class="article-thumb-placeholder"><i class="fa-regular fa-image" aria-hidden="true"></i></span>'}<div><strong>${escapeHTML(record.title)}</strong><small>${escapeHTML(NC.utils.truncate(record.description || 'No description', 70))}</small></div></div></td><td data-label="Category">${escapeHTML(record.category || '—')}</td><td data-label="Added">${escapeHTML(formatDate(record.created_at))}</td><td data-label="Actions" class="text-right">${actionSet(record)}</td></tr>`).join('');
      content.innerHTML = `${NC.components.tableShell({ caption: 'Gallery items', head: '<tr><th>Image</th><th>Category</th><th>Added</th><th class="text-right">Actions</th></tr>', body })}${NC.components.pagination({ page: state.page, pageSize: state.pageSize, total })}`;
    }
    bindListEvents(content); NC.components.bindImageFallbacks(content);
  }

  function bindListEvents(scope = root) {
    scope.querySelectorAll('[data-add-gallery]').forEach((button) => button.addEventListener('click', () => openForm()));
    scope.querySelectorAll('[data-action]').forEach((button) => button.addEventListener('click', () => {
      const record = state.records.find((item) => item.id === button.dataset.id); if (!record) return;
      if (button.dataset.action === 'view') openView(record);
      if (button.dataset.action === 'edit') openForm(record);
      if (button.dataset.action === 'delete') remove(record);
    }));
    NC.crud.bindPagination(root, state, renderList);
  }

  function openView(record) {
    NC.components.openModal({
      title: record.title, eyebrow: record.category || 'Gallery', size: 'lg',
      content: `${safeImage(record.image) ? `<img src="${escapeHTML(safeImage(record.image))}" alt="${escapeHTML(record.title)}" class="preview-image" referrerpolicy="no-referrer">` : NC.components.emptyState({ icon: 'fa-image-slash', title: 'No image' })}<div class="prose-content mt-5"><p>${escapeHTML(record.description || 'No description has been added.')}</p></div><p class="mt-5 text-sm text-muted-foreground"><i class="fa-regular fa-calendar mr-2" aria-hidden="true"></i>Added ${escapeHTML(formatDate(record.created_at))}</p>`,
      footer: `${safeImage(record.image) ? `<a class="btn btn-secondary" href="${escapeHTML(safeImage(record.image))}" target="_blank" rel="noopener noreferrer"><i class="fa-regular fa-arrow-up-right-from-square" aria-hidden="true"></i>Open original</a>` : ''}<button type="button" class="btn btn-primary" data-gallery-edit><i class="fa-regular fa-pen" aria-hidden="true"></i>Edit</button>`,
      onOpen: (modalRoot) => modalRoot.querySelector('[data-gallery-edit]').addEventListener('click', () => { NC.components.closeModal(); window.setTimeout(() => openForm(record), 180); })
    });
  }

  function openForm(record = null) {
    let uploader;
    const initial = { url: record?.image || '', delete_url: record?.imgbb_delete_url || '', image_meta: record?.image_meta || {} };
    NC.components.openModal({
      title: record ? 'Edit gallery item' : 'Add gallery item', eyebrow: 'Visual archive', size: 'xl',
      content: `<form id="gallery-form" class="form-stack" novalidate>${record ? '' : NC.importer.formControlHTML('galleries')}<div class="form-grid-2"><div class="field"><label class="field-label" for="gallery-title">Title <span aria-hidden="true">*</span></label><input class="form-input" id="gallery-title" name="title" value="${escapeHTML(record?.title || '')}" autofocus><p class="field-error hidden" data-field-error="title"></p></div><div class="field"><label class="field-label" for="gallery-category">Category</label><input class="form-input" id="gallery-category" name="category" value="${escapeHTML(record?.category || '')}" placeholder="Culture and festivals"></div></div>${NC.media.imageUploaderHTML({ id: 'gallery-image', label: 'Gallery image', required: true, hint: 'Upload to ImgBB or use a public image URL.' })}<div class="field"><label class="field-label" for="gallery-description">Description</label><textarea class="form-textarea min-h-32" id="gallery-description" name="description" placeholder="Add useful context for this image…">${escapeHTML(record?.description || '')}</textarea></div></form>`,
      footer: `<button type="button" class="btn btn-secondary" data-modal-close>Cancel</button><button type="button" class="btn btn-secondary" data-preview-gallery><i class="fa-regular fa-eye" aria-hidden="true"></i>Preview</button><button type="submit" form="gallery-form" class="btn btn-primary" data-save-gallery><i class="fa-regular fa-floppy-disk" aria-hidden="true"></i>${record ? 'Save changes' : 'Add image'}</button>`,
      onOpen: (modalRoot) => {
        const form = modalRoot.querySelector('#gallery-form');
        uploader = NC.media.mountImageUploader(modalRoot.querySelector('#gallery-image'), { initial, required: true, label: 'Gallery image' });
        if (!record) NC.importer.mountFormControl(modalRoot, {
          type: 'galleries',
          onRecord: ({ payload }) => { NC.importer.fillForm(form, payload); uploader.setValue(payload.image || ''); }
        });
        modalRoot.querySelector('[data-preview-gallery]').addEventListener('click', () => {
          const data = formData(form), media = uploader.getValue();
          if (!media.url) { NC.components.toast('Add an image before previewing.', 'warning'); return; }
          const blob = new Blob([`<!doctype html><meta charset="utf-8"><title>${escapeHTML(data.title || 'Gallery preview')}</title><style>body{margin:0;background:#10121a;color:#fff;font-family:system-ui;display:grid;min-height:100vh;place-items:center;padding:24px}article{width:min(100%,960px)}img{display:block;width:100%;max-height:70vh;object-fit:contain;border-radius:18px;background:#090a0e}h1{font-size:clamp(1.8rem,5vw,3.5rem);margin:24px 0 8px}p{color:#a9b1c3;line-height:1.7}</style><article><img src="${escapeHTML(safeImage(media.url))}" alt=""><h1>${escapeHTML(data.title || 'Untitled image')}</h1><p>${escapeHTML(data.description || '')}</p></article>`], { type: 'text/html' });
          const url = URL.createObjectURL(blob); window.open(url, '_blank', 'noopener,noreferrer'); window.setTimeout(() => URL.revokeObjectURL(url), 60000);
        });
        form.addEventListener('submit', async (event) => {
          event.preventDefault(); const data = formData(form);
          if (!validateFields(form, { title: data.title ? '' : 'Gallery title is required.' }) || !uploader.validate() || uploader.isUploading()) {
            if (uploader.isUploading()) NC.components.toast('Wait for the image upload to finish.', 'warning'); return;
          }
          const media = uploader.getValue(), button = modalRoot.querySelector('[data-save-gallery]'); NC.utils.setButtonLoading(button, true, 'Saving…');
          try {
            await NC.crud.save('galleries', record?.id, { title: data.title, category: data.category, description: data.description, ...NC.crud.imagePayload(media) });
            await NC.crud.deleteReplacedMedia({ delete_url: record?.imgbb_delete_url }, media);
            NC.components.toast(`Gallery item ${record ? 'updated' : 'added'} successfully.`, 'success'); NC.components.closeModal(); await load();
          } catch (error) { console.error(error); NC.components.toast(NC.api.userMessage(error, 'Unable to save the gallery item.'), 'error'); }
          finally { NC.utils.setButtonLoading(button, false); }
        });
      }
    });
  }

  async function remove(record) {
    try { if (await NC.crud.deleteRecord({ table: 'galleries', record, label: 'gallery item', remoteDeleteUrls: [record.imgbb_delete_url] })) await load(); }
    catch (error) { console.error(error); NC.components.toast(NC.api.userMessage(error, 'Unable to delete the gallery item.'), 'error'); }
  }

  async function load(context = {}) {
    const content = root.querySelector('[data-galleries-content]'); content.innerHTML = NC.components.skeleton(6, 4);
    try {
      const result = await NC.api.list('galleries', { select: '*', order: 'created_at.desc', limit: 2000 });
      if (NC.crud.isStaleNavigation(context)) return;
      state.setRecords(result.data); renderList();
      const action = context.params?.get('action'), id = context.params?.get('id');
      if (action === 'new') openForm();
      if (id && ['view', 'edit'].includes(action)) { const record = state.records.find((item) => item.id === id); if (record) action === 'view' ? openView(record) : openForm(record); }
    } catch (error) { NC.crud.handleLoadError(content, error, () => load(context), context); }
  }

  function render(container, context = {}) {
    root = container;
    root.innerHTML = `${NC.components.pageHeader({ eyebrow: 'Visual archive', title: 'Galleries', description: 'Curate magazine photography in flexible grid and list views.', breadcrumb: [{ label: 'Galleries' }], actions: `${NC.importer.bulkButton('galleries')}<button type="button" class="btn btn-primary" data-add-gallery><i class="fa-regular fa-image-polaroid" aria-hidden="true"></i>Add image</button>` })}<section class="surface"><div class="list-toolbar"><label class="search-field"><i class="fa-regular fa-magnifying-glass" aria-hidden="true"></i><span class="sr-only">Search gallery</span><input type="search" placeholder="Search gallery…" data-gallery-search></label><input class="form-input toolbar-select" placeholder="Filter category…" data-gallery-category aria-label="Filter gallery category"><div class="view-toggle" role="group" aria-label="Gallery view"><button type="button" data-gallery-view="grid" aria-label="Grid view"><i class="fa-regular fa-grid-2" aria-hidden="true"></i></button><button type="button" data-gallery-view="list" aria-label="List view"><i class="fa-regular fa-list" aria-hidden="true"></i></button></div></div><div data-galleries-content>${NC.components.skeleton(6, 4)}</div></section>`;
    root.querySelector('[data-add-gallery]').addEventListener('click', () => openForm());
    root.querySelector('[data-gallery-search]').addEventListener('input', debounce((event) => { state.setQuery(event.target.value); renderList(); }, 220));
    root.querySelector('[data-gallery-category]').addEventListener('input', debounce((event) => { const term = event.target.value.trim().toLowerCase(); state.setFilter('category', term ? (value) => String(value || '').toLowerCase().includes(term) : 'all'); renderList(); }, 220));
    root.querySelectorAll('[data-gallery-view]').forEach((button) => button.addEventListener('click', () => { viewMode = button.dataset.galleryView; NC.utils.writePreference('gallery-view', viewMode); renderList(); }));
    NC.importer.bindBulk(root, { type: 'galleries', onComplete: () => load(context) });
    return load(context);
  }

  NC.views.galleries = { render };
})(window.NC);
