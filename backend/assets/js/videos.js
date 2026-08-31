(function (NC) {
  'use strict';

  const { escapeHTML, formatDate, formData, validateFields, debounce, safeImage } = NC.utils;
  const state = new NC.crud.ListState('videos', { searchFields: ['title', 'description', 'platform', 'video_link'], sortKey: 'created_at' });
  let root;

  function renderList() {
    const content = root.querySelector('[data-videos-content]');
    const { rows, total } = state.paged();
    if (!total) {
      content.innerHTML = NC.components.emptyState({
        icon: 'fa-video', title: state.query ? 'No videos match your search' : 'Add the first video',
        description: state.query ? 'Try a different title or provider.' : 'Share YouTube, Facebook, and Instagram videos with readers.',
        action: state.query ? '' : '<button type="button" class="btn btn-primary" data-add-video><i class="fa-regular fa-video-plus" aria-hidden="true"></i>Add video</button>'
      }); bindEvents(content); return;
    }
    const body = rows.map((record) => {
      const info = NC.media.detectVideoProvider(record.video_link);
      const thumbnail = safeImage(record.thumbnail_url || info.thumbnail);
      return `
        <tr><td data-label="Video"><div class="video-cell">${thumbnail ? `<img src="${escapeHTML(thumbnail)}" alt="" loading="lazy" referrerpolicy="no-referrer" data-image-fallback>` : `<span><i class="fa-brands ${info.provider === 'YouTube' ? 'fa-youtube' : info.provider === 'Facebook' ? 'fa-facebook' : info.provider === 'Instagram' ? 'fa-instagram' : 'fa-play'}" aria-hidden="true"></i></span>`}<div><strong>${escapeHTML(record.title)}</strong><small>${escapeHTML(NC.utils.truncate(record.description || record.video_link, 80))}</small></div></div></td><td data-label="Provider">${NC.components.statusBadge(info.provider)}</td><td data-label="Added">${escapeHTML(formatDate(record.created_at))}</td><td data-label="Actions" class="text-right">${NC.components.rowActions([
          { action: 'view', id: record.id, label: 'Preview video', icon: 'fa-eye' },
          { action: 'edit', id: record.id, label: 'Edit video', icon: 'fa-pen' },
          { action: 'delete', id: record.id, label: 'Delete video', icon: 'fa-trash', danger: true }
        ])}</td></tr>`;
    }).join('');
    content.innerHTML = `${NC.components.tableShell({ caption: 'Videos', minWidth: '760px', head: '<tr><th>Video</th><th>Provider</th><th>Added</th><th class="text-right">Actions</th></tr>', body })}${NC.components.pagination({ page: state.page, pageSize: state.pageSize, total })}`;
    bindEvents(content); NC.components.bindImageFallbacks(content);
  }

  function bindEvents(scope = root) {
    scope.querySelectorAll('[data-add-video]').forEach((button) => button.addEventListener('click', () => openForm()));
    scope.querySelectorAll('[data-action]').forEach((button) => button.addEventListener('click', () => {
      const record = state.records.find((item) => item.id === button.dataset.id); if (!record) return;
      if (button.dataset.action === 'view') openView(record);
      if (button.dataset.action === 'edit') openForm(record);
      if (button.dataset.action === 'delete') remove(record);
    }));
    NC.crud.bindPagination(root, state, renderList);
  }

  function openView(record) {
    const info = NC.media.detectVideoProvider(record.video_link);
    NC.components.openModal({
      title: record.title, eyebrow: `${info.provider} video`, size: 'lg',
      content: `${NC.media.videoPreviewHTML(record.video_link, { title: record.title })}${record.description ? `<div class="prose-content mt-5"><p>${escapeHTML(record.description)}</p></div>` : ''}`,
      footer: '<button type="button" class="btn btn-secondary" data-modal-close>Close</button><button type="button" class="btn btn-primary" data-video-edit><i class="fa-regular fa-pen" aria-hidden="true"></i>Edit video</button>',
      onOpen: (modalRoot) => modalRoot.querySelector('[data-video-edit]').addEventListener('click', () => { NC.components.closeModal(); window.setTimeout(() => openForm(record), 180); })
    });
  }

  function openForm(record = null) {
    NC.components.openModal({
      title: record ? 'Edit video' : 'Add video', eyebrow: 'Video library', size: 'xl',
      description: 'The provider is detected from a safe URL; arbitrary embed HTML is never accepted.',
      content: `<form id="video-form" class="form-stack" novalidate>${record ? '' : NC.importer.formControlHTML('videos')}<div class="form-preview-layout"><div class="form-stack"><div class="field"><label class="field-label" for="video-title">Title <span aria-hidden="true">*</span></label><input class="form-input" id="video-title" name="title" value="${escapeHTML(record?.title || '')}" autofocus><p class="field-error hidden" data-field-error="title"></p></div><div class="field"><label class="field-label" for="video-link">Video link <span aria-hidden="true">*</span></label><input class="form-input" type="url" id="video-link" name="video_link" value="${escapeHTML(record?.video_link || '')}" placeholder="https://www.youtube.com/watch?v=…"><p class="field-error hidden" data-field-error="video_link"></p><span class="field-hint">YouTube, Facebook, and Instagram are detected automatically.</span></div><div class="field"><label class="field-label" for="video-thumbnail">Fallback thumbnail URL</label><input class="form-input" type="url" id="video-thumbnail" name="thumbnail_url" value="${escapeHTML(record?.thumbnail_url || '')}" placeholder="Optional"><p class="field-error hidden" data-field-error="thumbnail_url"></p></div><div class="field"><label class="field-label" for="video-description">Description</label><textarea class="form-textarea min-h-32" id="video-description" name="description">${escapeHTML(record?.description || '')}</textarea></div></div><aside class="form-preview-sticky"><p class="eyebrow mb-3">Live preview</p><div data-video-form-preview>${record?.video_link ? NC.media.videoPreviewHTML(record.video_link, { title: record.title }) : NC.components.emptyState({ icon: 'fa-video-slash', title: 'Enter a video link', description: 'A safe preview will appear here.' })}</div></aside></div></form>`,
      footer: `<button type="button" class="btn btn-secondary" data-modal-close>Cancel</button><button type="submit" form="video-form" class="btn btn-primary" data-save-video><i class="fa-regular fa-floppy-disk" aria-hidden="true"></i>${record ? 'Save changes' : 'Add video'}</button>`,
      onOpen: (modalRoot) => {
        const form = modalRoot.querySelector('#video-form'), preview = modalRoot.querySelector('[data-video-form-preview]');
        const updatePreview = debounce(() => {
          const link = form.elements.video_link.value.trim();
          preview.innerHTML = link ? NC.media.videoPreviewHTML(link, { title: form.elements.title.value }) : NC.components.emptyState({ icon: 'fa-video-slash', title: 'Enter a video link', description: 'A safe preview will appear here.' });
        }, 350);
        form.elements.video_link.addEventListener('input', updatePreview); form.elements.title.addEventListener('input', updatePreview);
        if (!record) NC.importer.mountFormControl(modalRoot, {
          type: 'videos',
          onRecord: ({ payload }) => { NC.importer.fillForm(form, payload); updatePreview(); }
        });
        form.addEventListener('submit', async (event) => {
          event.preventDefault(); const data = formData(form); const info = NC.media.detectVideoProvider(data.video_link);
          const errors = {
            title: data.title ? '' : 'Video title is required.',
            video_link: info.url ? '' : 'Enter a complete YouTube, Facebook, Instagram, or public video URL.',
            thumbnail_url: data.thumbnail_url && !NC.utils.isValidUrl(data.thumbnail_url, { allowEmpty: false }) ? 'Enter a complete thumbnail URL.' : ''
          };
          if (!validateFields(form, errors)) return;
          const button = modalRoot.querySelector('[data-save-video]'); NC.utils.setButtonLoading(button, true, 'Saving…');
          try {
            await NC.crud.save('videos', record?.id, { title: data.title, video_link: info.url, platform: info.provider, description: data.description, thumbnail_url: data.thumbnail_url || info.thumbnail || '' });
            NC.components.toast(`Video ${record ? 'updated' : 'added'} successfully.`, 'success'); NC.components.closeModal(); await load();
          } catch (error) { console.error(error); NC.components.toast(NC.api.userMessage(error, 'Unable to save the video.'), 'error'); }
          finally { NC.utils.setButtonLoading(button, false); }
        });
      }
    });
  }

  async function remove(record) {
    try { if (await NC.crud.deleteRecord({ table: 'videos', record, label: 'video' })) await load(); }
    catch (error) { console.error(error); NC.components.toast(NC.api.userMessage(error, 'Unable to delete the video.'), 'error'); }
  }

  async function load(context = {}) {
    const content = root.querySelector('[data-videos-content]'); content.innerHTML = NC.components.skeleton(7, 4);
    try {
      const result = await NC.api.list('videos', { select: '*', order: 'created_at.desc', limit: 2000 });
      if (NC.crud.isStaleNavigation(context)) return;
      state.setRecords(result.data); renderList();
      const action = context.params?.get('action'), id = context.params?.get('id');
      if (action === 'new') openForm();
      if (id && ['view', 'edit'].includes(action)) { const record = state.records.find((item) => item.id === id); if (record) action === 'view' ? openView(record) : openForm(record); }
    } catch (error) { NC.crud.handleLoadError(content, error, () => load(context), context); }
  }

  function render(container, context = {}) {
    root = container;
    root.innerHTML = `${NC.components.pageHeader({ eyebrow: 'Multimedia', title: 'Videos', description: 'Manage safe previews for YouTube, Facebook, and Instagram links.', breadcrumb: [{ label: 'Videos' }], actions: `${NC.importer.bulkButton('videos')}<button type="button" class="btn btn-primary" data-add-video><i class="fa-regular fa-video-plus" aria-hidden="true"></i>Add video</button>` })}<section class="surface"><div class="list-toolbar"><label class="search-field"><i class="fa-regular fa-magnifying-glass" aria-hidden="true"></i><span class="sr-only">Search videos</span><input type="search" placeholder="Search videos…" data-video-search></label><select class="form-select toolbar-select" data-video-provider aria-label="Filter video provider"><option value="all">All providers</option><option>YouTube</option><option>Facebook</option><option>Instagram</option><option>Video Link</option></select></div><div data-videos-content>${NC.components.skeleton(7, 4)}</div></section>`;
    root.querySelector('[data-add-video]').addEventListener('click', () => openForm());
    root.querySelector('[data-video-search]').addEventListener('input', debounce((event) => { state.setQuery(event.target.value); renderList(); }, 220));
    root.querySelector('[data-video-provider]').addEventListener('change', (event) => { const value = event.target.value; state.setFilter('platform', value); renderList(); });
    NC.importer.bindBulk(root, { type: 'videos', onComplete: () => load(context) });
    return load(context);
  }

  NC.views.videos = { render };
})(window.NC);
