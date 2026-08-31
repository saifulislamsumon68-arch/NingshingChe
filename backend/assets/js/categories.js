(function (NC) {
  'use strict';

  const { escapeHTML, formatDate, formData, validateFields, debounce, slugify } = NC.utils;
  const state = new NC.crud.ListState('categories', { searchFields: ['title', 'sub_title', 'slug'], sortKey: 'title', sortDirection: 'asc' });
  let root;
  let blogs = [];

  function canViewBlogs() {
    return NC.auth.canAccess('blogs');
  }

  function usageCount(categoryId) {
    return canViewBlogs() ? blogs.filter((blog) => blog.category_id === categoryId).length : 0;
  }

  function renderList() {
    const content = root.querySelector('[data-categories-content]');
    const { rows, total } = state.paged();
    if (!total) {
      content.innerHTML = NC.components.emptyState({
        icon: 'fa-layer-group',
        title: state.query ? 'No categories found' : 'Create your first category',
        description: state.query ? 'Try a different category name or slug.' : 'Categories keep the magazine organized and easy to explore.',
        action: state.query ? '' : '<button type="button" class="btn btn-primary" data-add-category><i class="fa-regular fa-layer-plus" aria-hidden="true"></i>Add category</button>'
      });
      bindListEvents(content); return;
    }
    const body = rows.map((record) => {
      const count = usageCount(record.id);
      return `
        <tr>
          <td data-label="Category"><div class="category-cell"><span><i class="fa-regular fa-${escapeHTML(record.icon_name || 'layer-group')}" aria-hidden="true"></i></span><div><strong>${escapeHTML(record.title)}</strong><small>/${escapeHTML(record.slug)}</small></div></div></td>
          <td data-label="Subtitle"><span class="line-clamp-2">${escapeHTML(record.sub_title || '—')}</span></td>
          <td data-label="Blogs">${canViewBlogs() ? `<button type="button" class="usage-count" data-filter-blogs="${escapeHTML(record.id)}">${count.toLocaleString()} blog${count === 1 ? '' : 's'}</button>` : '<span class="text-xs text-muted-foreground"><i class="fa-regular fa-lock" aria-hidden="true"></i> Blogs restricted</span>'}</td>
          <td data-label="Created">${escapeHTML(formatDate(record.created_at))}</td>
          <td data-label="Actions" class="text-right">${NC.components.rowActions([
            { action: 'edit', id: record.id, label: 'Edit category', icon: 'fa-pen' },
            { action: 'delete', id: record.id, label: 'Delete category', icon: 'fa-trash', danger: true }
          ])}</td>
        </tr>`;
    }).join('');
    content.innerHTML = `${NC.components.tableShell({
      caption: 'Categories and blog usage',
      head: `<tr><th><button type="button" data-sort="title">Category ${NC.crud.sortIcon(state, 'title')}</button></th><th>Subtitle</th><th>Usage</th><th><button type="button" data-sort="created_at">Created ${NC.crud.sortIcon(state, 'created_at')}</button></th><th class="text-right">Actions</th></tr>`,
      body
    })}${NC.components.pagination({ page: state.page, pageSize: state.pageSize, total })}`;
    bindListEvents(content);
  }

  function bindListEvents(scope = root) {
    scope.querySelectorAll('[data-add-category]').forEach((button) => button.addEventListener('click', () => openForm()));
    scope.querySelectorAll('[data-action]').forEach((button) => button.addEventListener('click', () => {
      const record = state.records.find((item) => item.id === button.dataset.id);
      if (!record) return;
      if (button.dataset.action === 'edit') openForm(record);
      if (button.dataset.action === 'delete') remove(record);
    }));
    scope.querySelectorAll('[data-filter-blogs]').forEach((button) => button.addEventListener('click', () => NC.utils.routeTo('blogs', { category: button.dataset.filterBlogs })));
    NC.crud.bindPagination(root, state, renderList);
    NC.crud.bindSort(root, state, renderList);
  }

  function openForm(record = null) {
    const modal = NC.components.openModal({
      title: record ? 'Edit category' : 'Add category', eyebrow: 'Content taxonomy', size: 'md',
      description: 'Use concise labels that make sense to readers.',
      content: `
        <form id="category-form" class="form-stack" novalidate>
          ${record ? '' : NC.importer.formControlHTML('categories')}
          <div class="field"><label class="field-label" for="category-title">Title <span aria-hidden="true">*</span></label><input class="form-input" id="category-title" name="title" value="${escapeHTML(record?.title || '')}" autofocus required><p class="field-error hidden" data-field-error="title"></p></div>
          <div class="field"><label class="field-label" for="category-subtitle">Subtitle</label><input class="form-input" id="category-subtitle" name="sub_title" value="${escapeHTML(record?.sub_title || '')}" placeholder="Optional short description"></div>
          <div class="field"><div class="field-heading"><label class="field-label" for="category-slug">Slug <span aria-hidden="true">*</span></label><button type="button" class="field-action" data-generate-slug><i class="fa-regular fa-wand-magic-sparkles" aria-hidden="true"></i>Generate</button></div><div class="input-prefix"><span>/category/</span><input class="form-input" id="category-slug" name="slug" value="${escapeHTML(record?.slug || '')}" required></div><p class="field-error hidden" data-field-error="slug"></p></div>
          <div class="field"><label class="field-label" for="category-icon">Font Awesome icon</label><div class="input-prefix"><span><i class="fa-regular fa-icons" aria-hidden="true"></i></span><input class="form-input" id="category-icon" name="icon_name" value="${escapeHTML(record?.icon_name || 'layer-group')}" placeholder="layer-group"></div><span class="field-hint">Enter the icon name without the “fa-” prefix.</span></div>
          <div class="category-live-preview"><span data-category-preview-icon><i class="fa-regular fa-${escapeHTML(record?.icon_name || 'layer-group')}" aria-hidden="true"></i></span><div><small>Category preview</small><strong data-category-preview-title>${escapeHTML(record?.title || 'Category title')}</strong><p data-category-preview-subtitle>${escapeHTML(record?.sub_title || 'Optional subtitle')}</p></div></div>
        </form>`,
      footer: `<button type="button" class="btn btn-secondary" data-modal-close>Cancel</button><button type="submit" form="category-form" class="btn btn-primary" data-save-category><i class="fa-regular fa-floppy-disk" aria-hidden="true"></i>${record ? 'Save changes' : 'Add category'}</button>`,
      onOpen: (modalRoot) => {
        const form = modalRoot.querySelector('#category-form');
        const title = form.elements.title;
        const slug = form.elements.slug;
        const updatePreview = () => {
          modalRoot.querySelector('[data-category-preview-title]').textContent = title.value || 'Category title';
          modalRoot.querySelector('[data-category-preview-subtitle]').textContent = form.elements.sub_title.value || 'Optional subtitle';
          const icon = form.elements.icon_name.value.trim().replace(/^fa-/, '') || 'layer-group';
          modalRoot.querySelector('[data-category-preview-icon]').innerHTML = `<i class="fa-regular fa-${escapeHTML(icon)}" aria-hidden="true"></i>`;
        };
        form.addEventListener('input', debounce(updatePreview, 80));
        modalRoot.querySelector('[data-generate-slug]').addEventListener('click', () => { slug.value = slugify(title.value); slug.focus(); });
        if (!record) NC.importer.mountFormControl(modalRoot, {
          type: 'categories',
          onRecord: ({ payload }) => { NC.importer.fillForm(form, payload); updatePreview(); }
        });
        form.addEventListener('submit', async (event) => {
          event.preventDefault();
          const data = formData(form);
          const cleanSlug = slugify(data.slug);
          slug.value = cleanSlug;
          const errors = { title: data.title ? '' : 'Category title is required.', slug: cleanSlug ? '' : 'Create a valid category slug.' };
          if (!validateFields(form, errors)) return;
          const button = modalRoot.querySelector('[data-save-category]');
          NC.utils.setButtonLoading(button, true, 'Saving…');
          try {
            const duplicateFilters = { slug: cleanSlug };
            if (record) duplicateFilters.id = { op: 'neq', value: record.id };
            if (await NC.api.count('categories', duplicateFilters)) {
              validateFields(form, { slug: 'This category slug is already in use.' });
              return;
            }
            await NC.crud.save('categories', record?.id, {
              title: data.title, sub_title: data.sub_title, slug: cleanSlug,
              icon_name: data.icon_name.replace(/^fa-/, '') || 'layer-group'
            });
            NC.components.toast(`Category ${record ? 'updated' : 'created'} successfully.`, 'success');
            NC.components.closeModal(); await load();
          } catch (error) {
            console.error(error); NC.components.toast(NC.api.userMessage(error, 'Unable to save the category.'), 'error');
          } finally { NC.utils.setButtonLoading(button, false); }
        });
      }
    });
    return modal;
  }

  async function remove(record) {
    const count = usageCount(record.id);
    if (count > 0) {
      NC.components.openModal({
        title: 'Category is in use', eyebrow: 'Deletion blocked', size: 'sm',
        content: `${NC.components.notice(`“${record.title}” is assigned to ${count} blog${count === 1 ? '' : 's'}. Reassign those blogs before deleting this category.`, 'warning')}<button type="button" class="btn btn-secondary w-full justify-center mt-5" data-open-related>View related blogs</button>`,
        footer: '<button type="button" class="btn btn-primary" data-modal-close>Understood</button>',
        onOpen: (modalRoot) => modalRoot.querySelector('[data-open-related]').addEventListener('click', () => { NC.components.closeModal(); NC.utils.routeTo('blogs', { category: record.id }); })
      });
      return;
    }
    try {
      if (await NC.crud.deleteRecord({ table: 'categories', record, label: 'category' })) await load();
    } catch (error) {
      console.error(error); NC.components.toast(NC.api.userMessage(error, 'Unable to delete the category.'), 'error');
    }
  }

  async function load(context = {}) {
    const content = root.querySelector('[data-categories-content]');
    content.innerHTML = NC.components.skeleton(6, 5);
    try {
      const [categoriesResult, blogsResult] = await Promise.all([
        NC.api.list('categories', { select: '*', order: 'title.asc', limit: 1000 }),
        canViewBlogs() ? NC.api.list('blogs', { select: 'id,category_id', limit: 5000 }) : Promise.resolve({ data: [] })
      ]);
      if (NC.crud.isStaleNavigation(context)) return;
      blogs = blogsResult.data; state.setRecords(categoriesResult.data); renderList();
      const action = context.params?.get('action');
      const id = context.params?.get('id');
      if (action === 'new') openForm();
      if (action === 'edit' && id) {
        const record = state.records.find((item) => item.id === id); if (record) openForm(record);
      }
    } catch (error) { NC.crud.handleLoadError(content, error, () => load(context), context); }
  }

  function render(container, context = {}) {
    root = container;
    root.innerHTML = `
      ${NC.components.pageHeader({ eyebrow: 'Taxonomy', title: 'Categories', description: 'Organize articles into clear, reader-friendly sections.', breadcrumb: [{ label: 'Categories' }], actions: `${NC.importer.bulkButton('categories')}<button type="button" class="btn btn-primary" data-add-category><i class="fa-regular fa-layer-plus" aria-hidden="true"></i>Add category</button>` })}
      <section class="surface"><div class="list-toolbar"><label class="search-field"><i class="fa-regular fa-magnifying-glass" aria-hidden="true"></i><span class="sr-only">Search categories</span><input type="search" placeholder="Search categories…" data-category-search></label><span class="toolbar-note"><i class="fa-regular ${canViewBlogs() ? 'fa-circle-info' : 'fa-lock'}" aria-hidden="true"></i>${canViewBlogs() ? 'Usage is calculated from live blog relationships' : 'Blog usage requires Blogs menu access'}</span></div><div data-categories-content>${NC.components.skeleton(6, 5)}</div></section>`;
    root.querySelector('[data-add-category]').addEventListener('click', () => openForm());
    root.querySelector('[data-category-search]').addEventListener('input', debounce((event) => { state.setQuery(event.target.value); renderList(); }, 220));
    NC.importer.bindBulk(root, { type: 'categories', onComplete: () => load(context) });
    return load(context);
  }

  NC.views.categories = { render };
})(window.NC);
