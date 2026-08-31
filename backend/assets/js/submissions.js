(function (NC) {
  'use strict';

  const { escapeHTML, formatDateTime, formData, validateFields, debounce, slugify, safeImage, safeExternalUrl } = NC.utils;
  const state = new NC.crud.ListState('submissions', { searchFields: ['title', 'writer_name', 'designation', 'content_title', 'writer_email'], sortKey: 'created_at' });
  let root;
  let authors = [];
  let categories = [];
  let submissionEditor = null;

  function hasTransferredMedia(record) {
    return Boolean(record?.converted_blog_id) || ['Approved', 'Published'].includes(record?.status);
  }

  function canConvertSubmission() {
    return NC.auth.canAccess('submissions') && NC.auth.canAccess('blogs') && NC.auth.canAccess('authors');
  }

  function approvalAvailable(record) {
    return canConvertSubmission() && !hasTransferredMedia(record) && record?.status !== 'Rejected';
  }

  function renderList() {
    const content = root.querySelector('[data-submissions-content]');
    const { rows, total } = state.paged();
    if (!total) {
      content.innerHTML = NC.components.emptyState({
        icon: 'fa-file-pen', title: state.query ? 'No submissions match your search' : 'No public submissions yet',
        description: state.query ? 'Try a different writer, title, or status.' : 'Articles sent by contributors will arrive here for editorial review.',
        action: state.query ? '' : '<button type="button" class="btn btn-primary" data-add-submission><i class="fa-regular fa-plus" aria-hidden="true"></i>Add submission</button>'
      }); bindEvents(content); return;
    }
    const body = rows.map((record) => `
      <tr>
        <td data-label="Submission"><div class="article-cell">${safeImage(record.thumbnail) ? `<img src="${escapeHTML(safeImage(record.thumbnail))}" alt="" loading="lazy" referrerpolicy="no-referrer" data-image-fallback>` : '<span class="article-thumb-placeholder"><i class="fa-regular fa-file-lines" aria-hidden="true"></i></span>'}<div><strong>${escapeHTML(record.title)}</strong><small>${escapeHTML(record.content_title || 'No content subtitle')}</small></div></div></td>
        <td data-label="Writer"><div class="person-cell compact">${NC.utils.avatarHTML(record.writer_name, record.writer_profile_image, 'person-avatar')}<div><strong>${escapeHTML(record.writer_name)}</strong><span>${escapeHTML(record.writer_designation || 'Contributor')}</span></div></div></td>
        <td data-label="Status">${NC.components.statusBadge(record.status || 'Pending')}</td>
        <td data-label="Submitted"><time datetime="${escapeHTML(record.created_at || '')}">${escapeHTML(formatDateTime(record.created_at))}</time></td>
        <td data-label="Actions" class="text-right">${NC.components.rowActions([
          { action: 'view', id: record.id, label: 'View submission', icon: 'fa-eye' },
          { action: 'preview', id: record.id, label: 'Preview article', icon: 'fa-file-magnifying-glass' },
          { action: 'edit', id: record.id, label: 'Edit submission', icon: 'fa-pen' },
          ...(approvalAvailable(record) ? [{ action: 'approve', id: record.id, label: 'Approve and convert to blog', icon: 'fa-circle-check' }] : []),
          ...(record.status !== 'Rejected' && !hasTransferredMedia(record) ? [{ action: 'reject', id: record.id, label: 'Reject submission', icon: 'fa-circle-xmark', danger: true }] : []),
          { action: 'delete', id: record.id, label: 'Delete submission', icon: 'fa-trash', danger: true }
        ])}</td>
      </tr>`).join('');
    content.innerHTML = `${NC.components.tableShell({ caption: 'Public blog submissions', minWidth: '1000px', head: `<tr><th>Submission</th><th>Writer</th><th>Status</th><th><button type="button" data-sort="created_at">Submitted ${NC.crud.sortIcon(state, 'created_at')}</button></th><th class="text-right">Actions</th></tr>`, body })}${NC.components.pagination({ page: state.page, pageSize: state.pageSize, total })}`;
    bindEvents(content); NC.components.bindImageFallbacks(content);
  }

  function bindEvents(scope = root) {
    scope.querySelectorAll('[data-add-submission]').forEach((button) => button.addEventListener('click', () => openForm()));
    scope.querySelectorAll('[data-action]').forEach((button) => button.addEventListener('click', () => {
      const record = state.records.find((item) => item.id === button.dataset.id); if (!record) return;
      const action = button.dataset.action;
      if (action === 'view') openView(record);
      if (action === 'preview') previewSubmission(record);
      if (action === 'edit') openForm(record);
      if (action === 'approve') openApproval(record);
      if (action === 'reject') reject(record);
      if (action === 'delete') remove(record);
    }));
    NC.crud.bindPagination(root, state, renderList); NC.crud.bindSort(root, state, renderList);
  }

  function submissionPreviewMarkup(record) {
    return `<article class="article-preview submission-preview"><header><div class="article-kicker"><span>Contributor submission</span><time>${escapeHTML(formatDateTime(record.created_at || new Date()))}</time></div><h1>${escapeHTML(record.title || 'Untitled submission')}</h1>${record.content_title ? `<p class="article-subtitle">${escapeHTML(record.content_title)}</p>` : ''}<div class="article-byline">${NC.utils.avatarHTML(record.writer_name, record.writer_profile_image, 'article-author-avatar')}<div><strong>${escapeHTML(record.writer_name || 'Unknown writer')}</strong><span>${escapeHTML(record.writer_designation || record.designation || 'Contributor')}</span></div></div></header>${safeImage(record.thumbnail) ? `<img class="article-hero" src="${escapeHTML(safeImage(record.thumbnail))}" alt="${escapeHTML(record.title || '')}" referrerpolicy="no-referrer">` : ''}<div class="article-body prose-content">${NC.utils.sanitizeHTML(record.content || '<p>No article content.</p>')}</div></article>`;
  }

  function previewSubmission(record) {
    NC.components.openModal({ title: 'Submission preview', eyebrow: record.status || 'Pending', size: 'preview', content: submissionPreviewMarkup(record), footer: `<button type="button" class="btn btn-secondary" data-modal-close>Close</button>${approvalAvailable(record) ? '<button type="button" class="btn btn-primary" data-submission-preview-approve><i class="fa-regular fa-circle-check" aria-hidden="true"></i>Approve</button>' : ''}`, onOpen: (modalRoot) => { NC.components.bindImageFallbacks(modalRoot); modalRoot.querySelector('[data-submission-preview-approve]')?.addEventListener('click', () => { NC.components.closeModal(); window.setTimeout(() => openApproval(record), 180); }); } });
  }

  function openView(record) {
    NC.components.openModal({
      title: record.title, eyebrow: 'Submission details', size: 'xl',
      content: `<div class="submission-detail-grid"><section><h3 class="section-mini-title">Submitter</h3><dl class="details-list"><div><dt>Title</dt><dd>${escapeHTML(record.title)}</dd></div><div><dt>Designation</dt><dd>${escapeHTML(record.designation || '—')}</dd></div><div><dt>Address</dt><dd>${escapeHTML(record.address || '—')}</dd></div><div><dt>Phone</dt><dd>${escapeHTML(record.phone || '—')}</dd></div></dl></section><section><h3 class="section-mini-title">Writer</h3><div class="profile-preview compact">${NC.utils.avatarHTML(record.writer_name, record.writer_profile_image, 'profile-preview-avatar')}<div><h3>${escapeHTML(record.writer_name)}</h3><p>${escapeHTML(record.writer_designation || 'Contributor')}</p></div></div><dl class="details-list mt-4"><div><dt>Email</dt><dd>${escapeHTML(record.writer_email || '—')}</dd></div><div><dt>Facebook</dt><dd>${safeExternalUrl(record.writer_facebook) ? `<a href="${escapeHTML(record.writer_facebook)}" target="_blank" rel="noopener noreferrer">Open profile <i class="fa-regular fa-arrow-up-right-from-square" aria-hidden="true"></i></a>` : '—'}</dd></div></dl></section></div><section class="mt-6"><div class="flex flex-wrap items-center justify-between gap-3"><h3 class="section-mini-title">Article</h3>${NC.components.statusBadge(record.status)}</div><h4 class="text-xl font-semibold mt-4">${escapeHTML(record.content_title || record.title)}</h4><div class="prose-content mt-4">${NC.utils.sanitizeHTML(record.content)}</div></section>`,
      footer: `<button type="button" class="btn btn-secondary" data-modal-close>Close</button><button type="button" class="btn btn-secondary" data-submission-edit><i class="fa-regular fa-pen" aria-hidden="true"></i>Edit</button>${approvalAvailable(record) ? '<button type="button" class="btn btn-primary" data-submission-approve><i class="fa-regular fa-circle-check" aria-hidden="true"></i>Approve & convert</button>' : ''}`,
      onOpen: (modalRoot) => {
        NC.components.bindImageFallbacks(modalRoot);
        modalRoot.querySelector('[data-submission-edit]').addEventListener('click', () => { NC.components.closeModal(); window.setTimeout(() => openForm(record), 180); });
        modalRoot.querySelector('[data-submission-approve]')?.addEventListener('click', () => { NC.components.closeModal(); window.setTimeout(() => openApproval(record), 180); });
      }
    });
  }

  function openForm(record = null) {
    let thumbnailUploader, writerUploader;
    let formSaved = false;
    let formClosed = false;
    let cleanupFormUploads = () => Promise.resolve();
    const thumbnailSessionUploads = [];
    const writerSessionUploads = [];
    const thumbnailInitial = { url: record?.thumbnail || '', delete_url: record?.imgbb_delete_url || '', image_meta: record?.thumbnail_meta || {} };
    const writerInitial = { url: record?.writer_profile_image || '', delete_url: record?.writer_profile_delete_url || '', image_meta: record?.writer_profile_meta || {} };
    NC.components.openModal({
      title: record ? 'Edit submission' : 'Add submission', eyebrow: 'Contributor article', size: 'full',
      description: 'Preserve submitter details while preparing the article for editorial review.',
      content: `<form id="submission-form" class="form-stack" novalidate>${record ? '' : NC.importer.formControlHTML('submissions')}<fieldset class="form-section"><legend><span>01</span>Submitter</legend><div class="form-grid-2"><div class="field"><label class="field-label" for="submission-title">Submission title <span aria-hidden="true">*</span></label><input class="form-input" id="submission-title" name="title" value="${escapeHTML(record?.title || '')}" autofocus><p class="field-error hidden" data-field-error="title"></p></div><div class="field"><label class="field-label" for="submission-designation">Designation</label><input class="form-input" id="submission-designation" name="designation" value="${escapeHTML(record?.designation || '')}"></div></div><div class="form-grid-2"><div class="field"><label class="field-label" for="submission-address">Address</label><input class="form-input" id="submission-address" name="address" value="${escapeHTML(record?.address || '')}"></div><div class="field"><label class="field-label" for="submission-phone">Phone</label><input class="form-input" type="tel" id="submission-phone" name="phone" value="${escapeHTML(record?.phone || '')}"></div></div>${NC.media.imageUploaderHTML({ id: 'submission-thumbnail', label: 'Thumbnail', hint: 'Upload the article thumbnail to ImgBB or paste a URL.' })}</fieldset><fieldset class="form-section"><legend><span>02</span>Writer information</legend><div class="form-grid-2"><div class="field"><label class="field-label" for="writer-name">Writer name <span aria-hidden="true">*</span></label><input class="form-input" id="writer-name" name="writer_name" value="${escapeHTML(record?.writer_name || '')}"><p class="field-error hidden" data-field-error="writer_name"></p></div><div class="field"><label class="field-label" for="writer-designation">Writer designation</label><input class="form-input" id="writer-designation" name="writer_designation" value="${escapeHTML(record?.writer_designation || '')}"></div></div>${NC.media.imageUploaderHTML({ id: 'writer-profile', label: 'Writer profile image', hint: 'This may become the author profile image when approved.' })}<div class="form-grid-2"><div class="field"><label class="field-label" for="writer-email">Email</label><input class="form-input" type="email" id="writer-email" name="writer_email" value="${escapeHTML(record?.writer_email || '')}"><p class="field-error hidden" data-field-error="writer_email"></p></div><div class="field"><label class="field-label" for="writer-facebook">Facebook</label><input class="form-input" type="url" id="writer-facebook" name="writer_facebook" value="${escapeHTML(record?.writer_facebook || '')}" placeholder="https://facebook.com/…"><p class="field-error hidden" data-field-error="writer_facebook"></p></div></div></fieldset><fieldset class="form-section"><legend><span>03</span>Article</legend><div class="form-grid-2"><div class="field"><label class="field-label" for="content-title">Content title</label><input class="form-input" id="content-title" name="content_title" value="${escapeHTML(record?.content_title || '')}"></div><div class="field"><label class="field-label" for="submission-status">Moderation status</label><select class="form-select" id="submission-status" name="status" ${hasTransferredMedia(record) ? 'disabled aria-describedby="submission-status-note"' : ''}>${(hasTransferredMedia(record) ? [record?.status || 'Approved'] : ['Pending', 'Reviewed', 'Rejected']).map((status) => `<option value="${status}" ${status === (record?.status || 'Pending') ? 'selected' : ''}>${status}</option>`).join('')}</select>${hasTransferredMedia(record) ? '<span class="field-hint" id="submission-status-note">Converted status is locked to protect shared media references.</span>' : ''}</div></div>${NC.editor.editorHTML({ id: 'submission-content', label: 'Article content', required: true, hint: 'Review formatting and remove unsafe or unnecessary markup.' })}</fieldset></form>`,
      footer: `<button type="button" class="btn btn-secondary" data-modal-close>Cancel</button><button type="button" class="btn btn-secondary" data-preview-submission-form><i class="fa-regular fa-eye" aria-hidden="true"></i>Preview</button><button type="submit" form="submission-form" class="btn btn-primary" data-save-submission><i class="fa-regular fa-floppy-disk" aria-hidden="true"></i>${record ? 'Save changes' : 'Add submission'}</button>`,
      onOpen: (modalRoot) => {
        const form = modalRoot.querySelector('#submission-form');
        submissionEditor = NC.editor.mountEditor(modalRoot.querySelector('#submission-content'), {
          initial: record?.content || '',
          media: Array.isArray(record?.inline_media) ? record.inline_media : [],
          required: true,
          label: 'Article content'
        });
        thumbnailUploader = NC.media.mountImageUploader(modalRoot.querySelector('#submission-thumbnail'), {
          initial: thumbnailInitial,
          onChange: (next) => {
            if (next?.provider !== 'imgbb' || !next.delete_url) return;
            if (!thumbnailSessionUploads.some((item) => item.url === next.url)) thumbnailSessionUploads.push({ ...next });
            if (formClosed) NC.crud.deleteMediaRecords([next]);
          }
        });
        writerUploader = NC.media.mountImageUploader(modalRoot.querySelector('#writer-profile'), {
          initial: writerInitial,
          onChange: (next) => {
            if (next?.provider !== 'imgbb' || !next.delete_url) return;
            if (!writerSessionUploads.some((item) => item.url === next.url)) writerSessionUploads.push({ ...next });
            if (formClosed) NC.crud.deleteMediaRecords([next]);
          }
        });
        const collect = () => {
          const data = formData(form);
          return {
            ...data,
            status: data.status || record?.status || 'Pending',
            thumbnail: thumbnailUploader.getValue().url,
            writer_profile_image: writerUploader.getValue().url,
            content: submissionEditor.getValue(),
            inline_media: submissionEditor.getMedia(),
            created_at: record?.created_at || new Date().toISOString()
          };
        };
        if (!record) NC.importer.mountFormControl(modalRoot, {
          type: 'submissions',
          onRecord: ({ payload }) => {
            NC.importer.fillForm(form, payload);
            thumbnailUploader.setValue(payload.thumbnail || '');
            writerUploader.setValue(payload.writer_profile_image || '');
            submissionEditor.setValue(payload.content || '');
          }
        });
        cleanupFormUploads = async ({ saved = false, thumbnail = null, writerImage = null } = {}) => {
          formClosed = true;
          const activeInlineUrls = new Set(submissionEditor.getMedia().map((item) => item.url));
          const inline = saved
            ? (hasTransferredMedia(record)
              ? submissionEditor.getSessionUploads().filter((item) => !activeInlineUrls.has(item.url))
              : submissionEditor.getInactiveMedia())
            : submissionEditor.getSessionUploads();
          const thumbnailUnused = thumbnailSessionUploads.filter((item) => !saved || item.url !== thumbnail?.url);
          const writerUnused = writerSessionUploads.filter((item) => !saved || item.url !== writerImage?.url);
          await NC.crud.deleteMediaRecords([...inline, ...thumbnailUnused, ...writerUnused]);
        };
        modalRoot.querySelector('[data-preview-submission-form]').addEventListener('click', () => {
          const data = collect();
          const html = `<!doctype html><html><head><meta charset="utf-8"><meta name="viewport" content="width=device-width,initial-scale=1"><title>${escapeHTML(data.title || 'Submission preview')}</title><style>body{margin:0;background:#f5f2eb;color:#1e293b;font-family:system-ui;padding:24px}article{max-width:840px;margin:auto;background:#fff;padding:clamp(24px,7vw,70px);border-radius:22px}h1{font-family:Georgia,serif;font-size:clamp(2.4rem,6vw,4.5rem);line-height:1.05}.subtitle{font-size:1.25rem;color:#64748b}.hero{width:100%;max-height:520px;object-fit:cover;border-radius:18px;margin:28px 0}.byline{font-weight:700;color:#7c3aed}.body{font-family:Georgia,serif;font-size:1.1rem;line-height:1.85}.body img{max-width:100%}.body table{width:100%;border-collapse:collapse}.body td,.body th{border:1px solid #cbd5e1;padding:9px}</style></head><body><article><p class="byline">Contributor preview · ${escapeHTML(data.writer_name || 'Unknown writer')}</p><h1>${escapeHTML(data.title || 'Untitled submission')}</h1>${data.content_title ? `<p class="subtitle">${escapeHTML(data.content_title)}</p>` : ''}${safeImage(data.thumbnail) ? `<img class="hero" src="${escapeHTML(safeImage(data.thumbnail))}" alt="">` : ''}<div class="body">${NC.utils.sanitizeHTML(data.content)}</div></article></body></html>`;
          const blob = new Blob([html], { type: 'text/html' }); const url = URL.createObjectURL(blob); window.open(url, '_blank', 'noopener,noreferrer'); window.setTimeout(() => URL.revokeObjectURL(url), 60000);
        });
        form.addEventListener('submit', async (event) => {
          event.preventDefault(); const data = collect();
          const errors = {
            title: data.title ? '' : 'Submission title is required.', writer_name: data.writer_name ? '' : 'Writer name is required.',
            writer_email: data.writer_email && !/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(data.writer_email) ? 'Enter a valid email address.' : '',
            writer_facebook: data.writer_facebook && !NC.utils.isValidUrl(data.writer_facebook, { allowEmpty: false }) ? 'Enter a complete Facebook URL.' : ''
          };
          const valid = validateFields(form, errors) && submissionEditor.validate() && thumbnailUploader.validate() && writerUploader.validate();
          if (!valid || thumbnailUploader.isUploading() || writerUploader.isUploading()) {
            if (thumbnailUploader.isUploading() || writerUploader.isUploading()) NC.components.toast('Wait for image uploads to finish.', 'warning');
            return;
          }
          const thumbnail = thumbnailUploader.getValue(), writerImage = writerUploader.getValue(), button = modalRoot.querySelector('[data-save-submission]'); NC.utils.setButtonLoading(button, true, 'Saving…');
          try {
            await NC.crud.save('submissions', record?.id, {
              title: data.title, designation: data.designation, address: data.address, phone: data.phone,
              thumbnail: thumbnail.url, imgbb_delete_url: thumbnail.delete_url || '', thumbnail_meta: NC.crud.imagePayload(thumbnail).image_meta,
              writer_name: data.writer_name, writer_designation: data.writer_designation,
              writer_profile_image: writerImage.url, writer_profile_delete_url: writerImage.delete_url || '', writer_profile_meta: NC.crud.imagePayload(writerImage).image_meta,
              writer_email: data.writer_email, writer_facebook: data.writer_facebook,
              content_title: data.content_title, content: data.content,
              inline_media: data.inline_media, status: data.status
            });
            if (!hasTransferredMedia(record)) {
              await Promise.all([
                NC.crud.deleteReplacedMedia({ delete_url: record?.imgbb_delete_url }, thumbnail),
                NC.crud.deleteReplacedMedia({ delete_url: record?.writer_profile_delete_url }, writerImage)
              ]);
            }
            await cleanupFormUploads({ saved: true, thumbnail, writerImage });
            formSaved = true;
            NC.components.toast(`Submission ${record ? 'updated' : 'added'} successfully.`, 'success'); NC.components.closeModal(); await load();
          } catch (error) { console.error(error); NC.components.toast(NC.api.userMessage(error, 'Unable to save the submission.'), 'error'); }
          finally { NC.utils.setButtonLoading(button, false); }
        });
      },
      onClose: () => {
        if (!formSaved) cleanupFormUploads().catch((error) => console.warn('Unable to clean up unused submission uploads:', error));
        submissionEditor?.destroy?.();
        submissionEditor = null;
      }
    });
  }

  function openApproval(record) {
    if (!canConvertSubmission()) {
      NC.components.toast('Approval conversion requires Submit Blogs, Blogs, and Authors access.', 'warning');
      return;
    }
    if (hasTransferredMedia(record) || record.status === 'Rejected') {
      NC.components.toast('This submission is not eligible for another conversion.', 'warning');
      return;
    }
    const suggestedSlug = slugify(record.title);
    NC.components.openModal({
      title: 'Approve & convert to blog', eyebrow: 'Editorial workflow', size: 'lg',
      description: 'The submission will become a blog. An existing author with the same name will be reused.',
      content: `<form id="approval-form" class="form-stack" novalidate>${NC.components.notice(`Writer mapping: ${record.writer_name} → existing matching author, or a new unverified author.`, 'info')}<div class="field"><label class="field-label" for="approval-category">Blog category <span aria-hidden="true">*</span></label><select class="form-select" id="approval-category" name="category_id"><option value="">Choose a category</option>${categories.map((item) => `<option value="${escapeHTML(item.id)}">${escapeHTML(item.title)}</option>`).join('')}</select><p class="field-error hidden" data-field-error="category_id"></p></div><div class="form-grid-2"><div class="field"><label class="field-label" for="approval-status">Initial blog status</label><select class="form-select" id="approval-status" name="status"><option value="Draft">Save as draft</option><option value="Publish">Publish immediately</option></select></div><div class="field"><label class="field-label" for="approval-slug">Blog slug <span aria-hidden="true">*</span></label><input class="form-input" id="approval-slug" name="slug" value="${escapeHTML(suggestedSlug)}"><p class="field-error hidden" data-field-error="slug"></p></div></div><div class="conversion-map"><div><span>Submission title</span><strong>${escapeHTML(record.title)}</strong><i class="fa-regular fa-arrow-down" aria-hidden="true"></i><span>Blog title</span></div><div><span>Writer</span><strong>${escapeHTML(record.writer_name)}</strong><i class="fa-regular fa-arrow-down" aria-hidden="true"></i><span>Author relationship</span></div><div><span>Thumbnail & content</span><strong>Preserved</strong><i class="fa-regular fa-arrow-down" aria-hidden="true"></i><span>Hero & article body</span></div></div></form>`,
      footer: '<button type="button" class="btn btn-secondary" data-modal-close>Cancel</button><button type="submit" form="approval-form" class="btn btn-primary" data-confirm-approval><i class="fa-regular fa-circle-check" aria-hidden="true"></i>Approve & convert</button>',
      onOpen: (modalRoot) => {
        const form = modalRoot.querySelector('#approval-form');
        form.addEventListener('submit', async (event) => {
          event.preventDefault(); const data = formData(form); data.slug = slugify(data.slug); form.elements.slug.value = data.slug;
          if (!validateFields(form, { category_id: data.category_id ? '' : 'Choose a blog category.', slug: data.slug ? '' : 'Enter a valid unique slug.' })) return;
          const button = modalRoot.querySelector('[data-confirm-approval]'); NC.utils.setButtonLoading(button, true, 'Converting…');
          try {
            if (await NC.api.slugExists(data.slug)) { validateFields(form, { slug: 'This blog slug is already in use.' }); return; }
            let converted;
            try {
              converted = await NC.api.rpc('approve_submission', { p_submission_id: record.id, p_category_id: data.category_id, p_status: data.status, p_slug: data.slug });
            } catch (rpcError) {
              if (![404, 400].includes(rpcError.status) && rpcError.code !== 'PGRST202') throw rpcError;
              converted = await fallbackApproval(record, data);
            }
            const blog = Array.isArray(converted) ? converted[0] : converted;
            NC.components.toast(data.status === 'Publish' ? 'Submission approved and blog published.' : 'Submission approved and converted to a draft.', 'success');
            NC.components.closeModal(); await load();
            if (blog?.id) NC.utils.routeTo('blogs', { action: 'edit', id: blog.id });
          } catch (error) { console.error(error); NC.components.toast(NC.api.userMessage(error, 'Unable to convert this submission. No information was intentionally discarded.'), 'error'); }
          finally { NC.utils.setButtonLoading(button, false); }
        });
      }
    });
  }

  async function fallbackApproval(record, data) {
    let author = authors.find((item) => item.title.trim().toLocaleLowerCase() === record.writer_name.trim().toLocaleLowerCase());
    if (!author) {
      author = await NC.api.insert('authors', {
        title: record.writer_name, designation: record.writer_designation || '', image: record.writer_profile_image || '',
        imgbb_delete_url: record.writer_profile_delete_url || '', image_meta: record.writer_profile_meta || {},
        description: '', is_verified: false, location: record.address || ''
      });
      authors.push(author);
    }
    const category = categories.find((item) => item.id === data.category_id);
    const blog = await NC.api.insert('blogs', {
      title: record.title,
      sub_title: record.content_title || '',
      image: record.thumbnail || '',
      imgbb_delete_url: record.imgbb_delete_url || '',
      image_meta: record.thumbnail_meta || {},
      content: record.content,
      inline_media: Array.isArray(record.inline_media) ? record.inline_media : [],
      category_id: data.category_id, category_title: category?.title || '', category_slug: category?.slug || '',
      author_id: author.id, author_name: author.title, author_image: author.image || '', status: data.status,
      slug: data.slug, tags: [], is_slider: false, is_feature: false, is_special_article: false,
      seo_title: '', seo_description: '', video_link: '', pdf_book_link: '',
      published_date: data.status === 'Publish' ? new Date().toISOString().slice(0, 10) : null
    });
    await NC.api.update('submissions', record.id, { status: 'Approved', reviewed_at: new Date().toISOString(), converted_blog_id: blog.id });
    return blog;
  }

  async function reject(record) {
    if (hasTransferredMedia(record)) {
      NC.components.toast('A converted submission cannot be rejected because its media is shared with published records.', 'warning');
      return;
    }
    const accepted = await NC.components.confirm({ title: 'Reject this submission?', description: `“${record.title}” will remain in the archive with a Rejected status.`, confirmLabel: 'Reject submission', confirmIcon: 'fa-circle-xmark' });
    if (!accepted) return;
    try { await NC.api.update('submissions', record.id, { status: 'Rejected', reviewed_at: new Date().toISOString() }); NC.components.toast('Submission rejected.', 'success'); await load(); }
    catch (error) { console.error(error); NC.components.toast(NC.api.userMessage(error, 'Unable to reject the submission.'), 'error'); }
  }

  async function remove(record) {
    try {
      const mediaWasTransferred = hasTransferredMedia(record);
      const inlineDeleteUrls = (Array.isArray(record.inline_media) ? record.inline_media : []).map((item) => item.delete_url);
      const remoteDeleteUrls = mediaWasTransferred ? [] : [record.imgbb_delete_url, record.writer_profile_delete_url, ...inlineDeleteUrls];
      const deleted = await NC.crud.deleteRecord({ table: 'submissions', record, label: 'submission', remoteDeleteUrls });
      if (deleted) {
        await load();
        if (mediaWasTransferred) NC.components.toast('Transferred images were preserved for the related Blog and author.', 'success');
      }
    } catch (error) { console.error(error); NC.components.toast(NC.api.userMessage(error, 'Unable to delete the submission.'), 'error'); }
  }

  async function load(context = {}) {
    const content = root.querySelector('[data-submissions-content]'); if (content) content.innerHTML = NC.components.skeleton(7, 5);
    try {
      const [submissionsResult, authorsResult, categoriesResult] = await Promise.all([
        NC.api.list('submissions', { select: '*', order: 'created_at.desc', limit: 3000 }),
        NC.api.list('authors', { select: '*', order: 'title.asc', limit: 2000 }),
        NC.api.list('categories', { select: '*', order: 'title.asc', limit: 1000 })
      ]);
      if (NC.crud.isStaleNavigation(context)) return;
      authors = authorsResult.data; categories = categoriesResult.data; state.setRecords(submissionsResult.data); renderList();
      const action = context.params?.get('action'), id = context.params?.get('id');
      if (action === 'new') openForm();
      if (id) { const record = state.records.find((item) => item.id === id); if (record && action === 'view') openView(record); if (record && action === 'edit') openForm(record); }
    } catch (error) { if (content) NC.crud.handleLoadError(content, error, () => load(context), context); else NC.crud.handleLoadError(root, error, () => load(context), context); }
  }

  function render(container, context = {}) {
    root = container;
    const initialStatus = context.params?.get('filter') || 'all'; state.setFilter('status', initialStatus);
    root.innerHTML = `${NC.components.pageHeader({ eyebrow: 'Moderation', title: 'Submit Blogs', description: 'Review public contributions and convert approved work into structured blogs.', breadcrumb: [{ label: 'Submit Blogs' }], actions: `${NC.importer.bulkButton('submissions')}<button type="button" class="btn btn-primary" data-add-submission><i class="fa-regular fa-plus" aria-hidden="true"></i>Add submission</button>` })}<section class="submission-summary"><article><span class="metric-icon metric-amber"><i class="fa-regular fa-clock" aria-hidden="true"></i></span><div><small>Pending review</small><strong data-pending-submissions>—</strong></div></article><article class="submission-help"><i class="fa-regular fa-lightbulb" aria-hidden="true"></i><p>${canConvertSubmission() ? 'Approving a submission reuses a matching author and preserves the original article content.' : 'You can review and reject submissions. Approval conversion also requires the Blogs and Authors menus.'}</p></article></section><section class="surface mt-5"><div class="list-toolbar"><label class="search-field"><i class="fa-regular fa-magnifying-glass" aria-hidden="true"></i><span class="sr-only">Search submissions</span><input type="search" placeholder="Search title or writer…" data-submission-search></label><select class="form-select toolbar-select" data-submission-status aria-label="Filter submissions"><option value="all">All statuses</option>${['Pending', 'Reviewed', 'Approved', 'Rejected'].map((status) => `<option value="${status}">${status}</option>`).join('')}</select></div><div data-submissions-content>${NC.components.skeleton(7, 5)}</div></section>`;
    root.querySelector('[data-add-submission]').addEventListener('click', () => openForm());
    root.querySelector('[data-submission-search]').addEventListener('input', debounce((event) => { state.setQuery(event.target.value); renderList(); }, 220));
    const status = root.querySelector('[data-submission-status]'); status.value = ['Pending', 'Reviewed', 'Approved', 'Rejected'].includes(initialStatus) ? initialStatus : 'all';
    status.addEventListener('change', (event) => { state.setFilter('status', event.target.value); renderList(); });
    NC.importer.bindBulk(root, { type: 'submissions', onComplete: () => load(context) });
    return load(context).then(() => {
      if (NC.crud.isStaleNavigation(context)) return;
      const node = root.querySelector('[data-pending-submissions]');
      if (node) node.textContent = state.records.filter((item) => item.status === 'Pending').length.toLocaleString();
    });
  }

  NC.views.submissions = { render, destroy: () => submissionEditor?.destroy?.() };
})(window.NC);
