(function (NC) {
  'use strict';

  const { escapeHTML, formatDate, avatarHTML, formData, validateFields, debounce } = NC.utils;
  const state = new NC.crud.ListState('authors', { searchFields: ['title', 'designation', 'location'], sortKey: 'created_at' });
  let root = null;

  function verifiedLabel(record) {
    return record.is_verified ? NC.components.statusBadge('Verified') : NC.components.statusBadge('Unverified');
  }

  function renderList() {
    const content = root.querySelector('[data-authors-content]');
    const { rows, total } = state.paged();
    if (!total) {
      content.innerHTML = NC.components.emptyState({
        icon: 'fa-user-pen', title: state.query ? 'No authors match your search' : 'Add your first author',
        description: state.query ? 'Try a different name, designation, or location.' : 'Author profiles connect byline information to published articles.',
        action: state.query ? '' : '<button type="button" class="btn btn-primary" data-add-author><i class="fa-regular fa-user-plus" aria-hidden="true"></i>Add author</button>'
      });
      bindListEvents(content); return;
    }
    const body = rows.map((record) => `
      <tr>
        <td data-label="Author"><div class="person-cell">${avatarHTML(record.title, record.image, 'person-avatar')}<div><strong>${escapeHTML(record.title)}</strong><span>${escapeHTML(record.location || 'Location not set')}</span></div></div></td>
        <td data-label="Designation"><span class="line-clamp-2">${escapeHTML(record.designation || '—')}</span></td>
        <td data-label="Status">${verifiedLabel(record)}</td>
        <td data-label="Created"><time datetime="${escapeHTML(record.created_at || '')}">${escapeHTML(formatDate(record.created_at))}</time></td>
        <td data-label="Actions" class="text-right">${NC.components.rowActions([
          { action: 'view', id: record.id, label: 'View author', icon: 'fa-eye' },
          { action: 'edit', id: record.id, label: 'Edit author', icon: 'fa-pen' },
          { action: 'delete', id: record.id, label: 'Delete author', icon: 'fa-trash', danger: true }
        ])}</td>
      </tr>`).join('');
    content.innerHTML = `${NC.components.tableShell({
      caption: 'Authors',
      head: `<tr><th><button type="button" data-sort="title">Author ${NC.crud.sortIcon(state, 'title')}</button></th><th><button type="button" data-sort="designation">Designation ${NC.crud.sortIcon(state, 'designation')}</button></th><th>Verification</th><th><button type="button" data-sort="created_at">Created ${NC.crud.sortIcon(state, 'created_at')}</button></th><th class="text-right">Actions</th></tr>`,
      body
    })}${NC.components.pagination({ page: state.page, pageSize: state.pageSize, total })}`;
    bindListEvents(content);
    NC.components.bindImageFallbacks(content);
  }

  function bindListEvents(scope = root) {
    scope.querySelectorAll('[data-add-author]').forEach((button) => button.addEventListener('click', () => openForm()));
    scope.querySelectorAll('[data-action]').forEach((button) => button.addEventListener('click', () => {
      const record = state.records.find((item) => item.id === button.dataset.id);
      if (!record) return;
      if (button.dataset.action === 'view') openView(record);
      if (button.dataset.action === 'edit') openForm(record);
      if (button.dataset.action === 'delete') remove(record);
    }));
    NC.crud.bindPagination(root, state, renderList);
    NC.crud.bindSort(root, state, renderList);
  }

  function openView(record) {
    NC.components.openModal({
      title: record.title,
      eyebrow: 'Author profile', size: 'lg',
      content: `
        <div class="profile-preview">
          ${avatarHTML(record.title, record.image, 'profile-preview-avatar')}
          <div><div class="flex flex-wrap items-center gap-2"><h3>${escapeHTML(record.title)}</h3>${verifiedLabel(record)}</div><p>${escapeHTML(record.designation || 'No designation')}</p><p class="text-sm text-muted-foreground mt-1"><i class="fa-regular fa-location-dot mr-1" aria-hidden="true"></i>${escapeHTML(record.location || 'Location not set')}</p></div>
        </div>
        <div class="prose-content mt-6">${NC.utils.sanitizeHTML(record.description || '<p>No biography has been added.</p>')}</div>
        <dl class="details-grid mt-6"><div><dt>Created</dt><dd>${escapeHTML(formatDate(record.created_at))}</dd></div><div><dt>Last updated</dt><dd>${escapeHTML(formatDate(record.updated_at))}</dd></div></dl>`,
      footer: `<button type="button" class="btn btn-secondary" data-modal-close>Close</button><button type="button" class="btn btn-primary" data-view-edit><i class="fa-regular fa-pen" aria-hidden="true"></i>Edit author</button>`,
      onOpen: (modal) => {
        NC.components.bindImageFallbacks(modal);
        modal.querySelector('[data-view-edit]').addEventListener('click', () => { NC.components.closeModal(); window.setTimeout(() => openForm(record), 180); });
      }
    });
  }

  function previewMarkup(data, media) {
    return `
      <div class="live-profile-card">
        ${avatarHTML(data.title || 'Author', media?.url, 'live-profile-avatar')}
        <div class="min-w-0"><div class="flex flex-wrap items-center gap-2"><h3>${escapeHTML(data.title || 'Author name')}</h3>${data.is_verified ? NC.components.statusBadge('Verified') : ''}</div><p>${escapeHTML(data.designation || 'Designation')}</p><small><i class="fa-regular fa-location-dot" aria-hidden="true"></i>${escapeHTML(data.location || 'Location')}</small></div>
        <div class="live-profile-description prose-content">${NC.utils.sanitizeHTML(data.description || '<p>The author biography preview will appear here.</p>')}</div>
      </div>`;
  }

  function openForm(record = null) {
    const id = record?.id || '';
    let uploader, descriptionEditor;
    const initialMedia = {
      url: record?.image || '', delete_url: record?.imgbb_delete_url || '', image_meta: record?.image_meta || {}
    };
    const modal = NC.components.openModal({
      title: record ? 'Edit author' : 'Add author', eyebrow: record ? 'Update profile' : 'New profile', size: 'xl',
      description: 'Create a clear public byline profile for the magazine.',
      content: `
        <form id="author-form" novalidate>
          ${record ? '' : NC.importer.formControlHTML('authors')}
          <div class="form-preview-layout ${record ? '' : 'mt-5'}">
            <div class="form-stack">
              <div class="field"><label class="field-label" for="author-title">Name / Title <span aria-hidden="true">*</span></label><input class="form-input" id="author-title" name="title" value="${escapeHTML(record?.title || '')}" autocomplete="name" autofocus required><p class="field-error hidden" data-field-error="title"></p></div>
              <div class="form-grid-2">
                <div class="field"><label class="field-label" for="author-designation">Designation</label><input class="form-input" id="author-designation" name="designation" value="${escapeHTML(record?.designation || '')}" placeholder="Writer, researcher, editor…"></div>
                <div class="field"><label class="field-label" for="author-location">Location</label><input class="form-input" id="author-location" name="location" value="${escapeHTML(record?.location || '')}" placeholder="Sylhet, Bangladesh"></div>
              </div>
              ${NC.media.imageUploaderHTML({ id: 'author-image', label: 'Profile image', hint: 'Use a square portrait where possible.' })}
              ${NC.editor.editorHTML({ id: 'author-description', label: 'Description', hint: 'Format the author biography with headings, links, quotations, and lists.' })}
              <label class="check-card"><input type="checkbox" name="is_verified" ${record?.is_verified ? 'checked' : ''}><span class="check-control"><i class="fa-solid fa-check" aria-hidden="true"></i></span><span><strong>Verified author</strong><small>Display a verification badge beside this profile.</small></span></label>
            </div>
            <aside class="form-preview-sticky"><p class="eyebrow mb-3">Live preview</p><div data-author-preview>${previewMarkup(record || {}, initialMedia)}</div><button type="button" class="btn btn-secondary btn-sm mt-4 w-full justify-center" data-refresh-author-preview><i class="fa-regular fa-eye" aria-hidden="true"></i>Preview current form</button></aside>
          </div>
        </form>`,
      footer: `<button type="button" class="btn btn-secondary" data-modal-close>Cancel</button><button type="submit" form="author-form" class="btn btn-primary" data-save-author><i class="fa-regular fa-floppy-disk" aria-hidden="true"></i>${record ? 'Save changes' : 'Add author'}</button>`,
      onOpen: (modalRoot) => {
        const form = modalRoot.querySelector('#author-form');
        const preview = modalRoot.querySelector('[data-author-preview]');
        const updatePreview = () => {
          const data = formData(form);
          data.description = descriptionEditor?.getValue() || record?.description || '';
          preview.innerHTML = previewMarkup(data, uploader?.getValue() || initialMedia);
          NC.components.bindImageFallbacks(preview);
        };
        uploader = NC.media.mountImageUploader(modalRoot.querySelector('#author-image'), {
          initial: initialMedia,
          onChange: updatePreview
        });
        descriptionEditor = NC.editor.mountEditor(modalRoot.querySelector('#author-description'), {
          initial: record?.description || '',
          label: 'Author description',
          placeholder: 'Write the author biography…',
          allowImages: false,
          onChange: debounce(updatePreview, 120)
        });
        form.addEventListener('input', debounce(updatePreview, 120));
        modalRoot.querySelector('[data-refresh-author-preview]').addEventListener('click', updatePreview);
        if (!record) NC.importer.mountFormControl(modalRoot, {
          type: 'authors',
          onRecord: ({ payload }) => {
            NC.importer.fillForm(form, payload);
            uploader.setValue(payload.image || '');
            descriptionEditor.setValue(payload.description || '');
            updatePreview();
          }
        });
        form.addEventListener('submit', async (event) => {
          event.preventDefault();
          const data = formData(form);
          data.description = descriptionEditor.getValue();
          const errors = { title: data.title ? '' : 'Author name is required.' };
          if (!validateFields(form, errors) || !uploader.validate() || uploader.isUploading()) {
            if (uploader.isUploading()) NC.components.toast('Wait for the image upload to finish.', 'warning');
            return;
          }
          const media = uploader.getValue();
          const payload = {
            title: data.title, designation: data.designation, location: data.location,
            description: data.description, is_verified: Boolean(data.is_verified),
            ...NC.crud.imagePayload(media)
          };
          const button = modalRoot.querySelector('[data-save-author]');
          NC.utils.setButtonLoading(button, true, 'Saving…');
          try {
            await NC.crud.save('authors', id, payload);
            await NC.crud.deleteReplacedMedia({ delete_url: record?.imgbb_delete_url }, media);
            NC.components.toast(`Author ${record ? 'updated' : 'added'} successfully.`, 'success');
            NC.components.closeModal();
            await load();
          } catch (error) {
            console.error(error); NC.components.toast(NC.api.userMessage(error, 'Unable to save the author.'), 'error');
          } finally { NC.utils.setButtonLoading(button, false); }
        });
        updatePreview();
      },
      onClose: () => {
        descriptionEditor?.destroy?.();
        descriptionEditor = null;
      }
    });
    return modal;
  }

  async function remove(record) {
    try {
      const deleted = await NC.crud.deleteRecord({ table: 'authors', record, label: 'author', remoteDeleteUrls: [record.imgbb_delete_url] });
      if (deleted) await load();
    } catch (error) {
      console.error(error); NC.components.toast(NC.api.userMessage(error, 'Unable to delete the author. Authors connected to blogs must be reassigned first.'), 'error');
    }
  }

  async function load(context = {}) {
    const content = root.querySelector('[data-authors-content]');
    content.innerHTML = NC.components.skeleton(6, 5);
    try {
      const result = await NC.api.list('authors', { select: '*', order: 'created_at.desc', limit: 1000 });
      if (NC.crud.isStaleNavigation(context)) return;
      state.setRecords(result.data); renderList();
      const action = context.params?.get('action');
      const id = context.params?.get('id');
      if (action === 'new') openForm();
      if (id && ['view', 'edit'].includes(action)) {
        const record = state.records.find((item) => item.id === id);
        if (record) action === 'view' ? openView(record) : openForm(record);
      }
    } catch (error) { NC.crud.handleLoadError(content, error, () => load(context), context); }
  }

  function render(container, context = {}) {
    root = container;
    const initialFilter = context.params?.get('filter') || 'all';
    state.setFilter('is_verified', initialFilter === 'verified' ? true : (initialFilter === 'unverified' ? false : 'all'));
    root.innerHTML = `
      ${NC.components.pageHeader({ eyebrow: 'People', title: 'Authors', description: 'Manage writer profiles, bylines, and verification.', breadcrumb: [{ label: 'Authors' }], actions: `${NC.importer.bulkButton('authors')}<button type="button" class="btn btn-primary" data-add-author><i class="fa-regular fa-user-plus" aria-hidden="true"></i>Add author</button>` })}
      <section class="surface">
        <div class="list-toolbar"><label class="search-field"><i class="fa-regular fa-magnifying-glass" aria-hidden="true"></i><span class="sr-only">Search authors</span><input type="search" placeholder="Search authors…" data-author-search></label><select class="form-select toolbar-select" data-author-filter aria-label="Filter verification"><option value="all">All authors</option><option value="verified">Verified</option><option value="unverified">Unverified</option></select></div>
        <div data-authors-content>${NC.components.skeleton(6, 5)}</div>
      </section>`;
    root.querySelectorAll('[data-add-author]').forEach((button) => button.addEventListener('click', () => openForm()));
    root.querySelector('[data-author-search]').addEventListener('input', debounce((event) => { state.setQuery(event.target.value); renderList(); }, 220));
    const filter = root.querySelector('[data-author-filter]'); filter.value = initialFilter;
    filter.addEventListener('change', (event) => { state.setFilter('is_verified', event.target.value === 'verified' ? true : (event.target.value === 'unverified' ? false : 'all')); renderList(); });
    NC.importer.bindBulk(root, { type: 'authors', onComplete: () => load(context) });
    return load(context);
  }

  NC.views.authors = { render };
})(window.NC);
