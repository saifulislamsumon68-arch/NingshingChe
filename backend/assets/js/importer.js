(function (NC) {
  'use strict';

  const { escapeHTML, qsa, qs, slugify, sanitizeHTML, stripHTML, parseTags, isValidUrl } = NC.utils;
  const MAX_FILE_BYTES = 10 * 1024 * 1024;
  const MAX_ROWS = 5000;
  const MAX_CONTEXT_ROWS = 50000;
  const PAGE_SIZE = 1000;

  function field(key, label, example = '', options = {}) {
    return Object.freeze({ key, label, example, required: false, aliases: [], description: '', ...options });
  }

  function text(value) {
    return value == null ? '' : String(value).trim();
  }

  function normalized(value) {
    return text(value).normalize('NFKC').toLocaleLowerCase().replace(/\s+/g, ' ');
  }

  function headerKey(value) {
    return text(value)
      .replace(/^\uFEFF/, '')
      .normalize('NFKD')
      .replace(/[\u0300-\u036f]/g, '')
      .toLocaleLowerCase()
      .replace(/&/g, ' and ')
      .replace(/[^a-z0-9]+/g, '_')
      .replace(/^_+|_+$/g, '');
  }

  function boolValue(value, fallback = false) {
    if (typeof value === 'boolean') return value;
    const clean = normalized(value);
    if (['1', 'true', 'yes', 'y', 'on', 'verified', 'publish', 'published'].includes(clean)) return true;
    if (['0', 'false', 'no', 'n', 'off', 'unverified', 'draft', 'unpublish', 'unpublished'].includes(clean)) return false;
    return fallback;
  }

  function numberValue(value, fallback = 0, minimum = 0) {
    if (value === '' || value == null) return fallback;
    const parsed = Number(String(value).replace(/,/g, ''));
    return Number.isFinite(parsed) ? Math.max(minimum, parsed) : fallback;
  }

  function dateValue(value, issues, label, strict) {
    if (!value) return null;
    if (value instanceof Date && !Number.isNaN(value.getTime())) return value.toISOString().slice(0, 10);
    const clean = text(value);
    if (/^\d{4}-\d{2}-\d{2}$/.test(clean)) return clean;
    const parsed = new Date(clean);
    if (!Number.isNaN(parsed.getTime())) return parsed.toISOString().slice(0, 10);
    addIssue(issues, `${label} must be a valid date. Use YYYY-MM-DD.`, strict);
    return null;
  }

  function addIssue(issues, message, strict = true) {
    (strict ? issues.errors : issues.warnings).push(message);
  }

  function requireValue(row, key, label, issues, strict) {
    if (strict && !text(row[key])) issues.errors.push(`${label} is required.`);
  }

  function urlValue(value, label, issues, strict) {
    const clean = text(value);
    if (!clean) return '';
    if (!isValidUrl(clean, { allowEmpty: false })) {
      addIssue(issues, `${label} must be a complete http:// or https:// URL.`, strict);
      return '';
    }
    return clean;
  }

  function emailValue(value, issues, strict) {
    const clean = text(value);
    if (clean && !/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(clean)) {
      addIssue(issues, 'Email must be a valid address.', strict);
      return '';
    }
    return clean;
  }

  function richTextValue(value) {
    const clean = text(value);
    if (!clean) return '';
    if (/<\/?[a-z][\s\S]*?>/i.test(clean)) return sanitizeHTML(clean);
    return clean.split(/\n{2,}/).map((paragraph) => `<p>${escapeHTML(paragraph).replace(/\n/g, '<br>')}</p>`).join('');
  }

  function imagePayload(url) {
    return NC.crud.imagePayload(url ? {
      url, display_url: url, delete_url: '', filename: url.split('/').pop().split('?')[0],
      size: 0, mime: '', provider: 'url', uploaded_at: null
    } : null);
  }

  function relation(list, candidates, properties) {
    const values = candidates.map(text).filter(Boolean);
    if (!values.length) return null;
    for (const value of values) {
      const byId = list.find((record) => String(record.id) === value);
      if (byId) return byId;
      const needle = normalized(value);
      const match = list.find((record) => properties.some((property) => normalized(record[property]) === needle));
      if (match) return match;
    }
    return null;
  }

  function prepareResult(payload, issues) {
    return { payload, errors: issues.errors, warnings: issues.warnings };
  }

  const definitions = {
    authors: {
      label: 'Authors', singular: 'author', apiKey: 'authors', primary: 'title', duplicateSelect: 'id,title',
      columns: [
        field('title', 'Name / Title', 'Anupama Singha', { required: true, aliases: ['name', 'author', 'author_name'], description: 'Public author name.' }),
        field('designation', 'Designation', 'Writer'),
        field('location', 'Location', 'Sylhet, Bangladesh'),
        field('image', 'Profile image URL', 'https://example.com/author.jpg', { aliases: ['image_url', 'profile_image', 'profile_image_url'], description: 'Direct public image URL; embedded spreadsheet images are not imported.' }),
        field('description', 'Description', 'Short author biography.', { aliases: ['bio', 'biography'] }),
        field('is_verified', 'Verified', 'yes', { aliases: ['verified'], description: 'yes/no, true/false, or 1/0.' })
      ],
      prepare(row, context, strict) {
        const issues = { errors: [], warnings: [] };
        requireValue(row, 'title', 'Name / Title', issues, strict);
        const image = urlValue(row.image, 'Profile image URL', issues, strict);
        return prepareResult({
          title: text(row.title), designation: text(row.designation), location: text(row.location),
          description: richTextValue(row.description), is_verified: boolValue(row.is_verified), ...imagePayload(image)
        }, issues);
      },
      duplicate(payload) { return normalized(payload.title); },
      existing(record) { return normalized(record.title); }
    },

    categories: {
      label: 'Categories', singular: 'category', apiKey: 'categories', primary: 'title', duplicateSelect: 'id,slug',
      columns: [
        field('title', 'Title', 'Culture', { required: true, aliases: ['category', 'category_title'] }),
        field('sub_title', 'Subtitle', 'Culture and heritage', { aliases: ['subtitle'] }),
        field('slug', 'Slug', 'culture', { description: 'Generated from title when blank.' }),
        field('icon_name', 'Font Awesome icon', 'layer-group', { aliases: ['icon'], description: 'Icon name without the fa- prefix.' })
      ],
      prepare(row, context, strict) {
        const issues = { errors: [], warnings: [] };
        requireValue(row, 'title', 'Title', issues, strict);
        const slug = slugify(row.slug || row.title);
        if (strict && !slug) issues.errors.push('Slug could not be generated.');
        return prepareResult({
          title: text(row.title), sub_title: text(row.sub_title), slug,
          icon_name: text(row.icon_name).replace(/^fa-/, '') || 'layer-group'
        }, issues);
      },
      duplicate(payload) { return normalized(payload.slug); },
      existing(record) { return normalized(record.slug); }
    },

    blogs: {
      label: 'Blogs', singular: 'blog', apiKey: 'blogs', primary: 'title', duplicateSelect: 'id,slug',
      lookups: ['authors', 'categories'],
      columns: [
        field('title', 'Title', 'A new magazine article', { required: true, aliases: ['blog_title'] }),
        field('slug', 'Slug', 'a-new-magazine-article', { description: 'Generated from title when blank.' }),
        field('sub_title', 'Subtitle', 'Optional standfirst', { aliases: ['subtitle'] }),
        field('content', 'Article content', '<p>Article body...</p>', { required: true, aliases: ['body', 'article_content'], description: 'Plain text or sanitized HTML.' }),
        field('category_slug', 'Category slug or title', 'culture', { required: true, aliases: ['category', 'category_name', 'category_title'], description: 'Must match an existing category.' }),
        field('category_id', 'Category ID', '', { description: 'Optional alternative to category slug/title.' }),
        field('author_name', 'Author name', 'Anupama Singha', { required: true, aliases: ['author'], description: 'Must match an existing author.' }),
        field('author_id', 'Author ID', '', { description: 'Optional alternative to author name.' }),
        field('status', 'Status', 'Draft', { description: 'Draft or Publish.' }),
        field('tags', 'Tags', 'culture, heritage', { description: 'Comma-separated.' }),
        field('image', 'Hero image URL', 'https://example.com/hero.jpg', { aliases: ['image_url', 'hero_image', 'hero_image_url'] }),
        field('video_link', 'Video URL', '', { aliases: ['video', 'video_url'] }),
        field('pdf_book_link', 'PDF URL', '', { aliases: ['pdf', 'pdf_url', 'pdf_link'] }),
        field('seo_title', 'SEO title', ''),
        field('seo_description', 'SEO description', ''),
        field('is_slider', 'Hero slider', 'no', { aliases: ['slider'] }),
        field('is_feature', 'Featured', 'yes', { aliases: ['featured', 'is_featured'] }),
        field('is_special_article', 'Special article', 'no', { aliases: ['special', 'is_special'] }),
        field('published_date', 'Published date', '2026-08-30', { aliases: ['publish_date'], description: 'YYYY-MM-DD.' })
      ],
      prepare(row, context, strict) {
        const issues = { errors: [], warnings: [] };
        requireValue(row, 'title', 'Title', issues, strict);
        requireValue(row, 'content', 'Article content', issues, strict);
        const category = relation(context.categories || [], [row.category_id, row.category_slug], ['slug', 'title']);
        const author = relation(context.authors || [], [row.author_id, row.author_name], ['title']);
        if (!category && (strict || row.category_id || row.category_slug)) addIssue(issues, 'Category was not found. Use an existing category ID, slug, or title.', strict);
        if (!author && (strict || row.author_id || row.author_name)) addIssue(issues, 'Author was not found. Use an existing author ID or name.', strict);
        const image = urlValue(row.image, 'Hero image URL', issues, strict);
        const video = urlValue(row.video_link, 'Video URL', issues, strict);
        const pdf = urlValue(row.pdf_book_link, 'PDF URL', issues, strict);
        const status = /^(publish|published)$/i.test(text(row.status)) ? 'Publish' : 'Draft';
        const slug = slugify(row.slug || row.title);
        if (strict && !slug) issues.errors.push('Slug could not be generated.');
        const publishedDate = dateValue(row.published_date, issues, 'Published date', strict);
        const content = richTextValue(row.content);
        const wordCount = stripHTML(content).split(/\s+/).filter(Boolean).length;
        return prepareResult({
          title: text(row.title), sub_title: text(row.sub_title), slug,
          content, reading_time_minutes: Math.max(1, Math.ceil(wordCount / 220)), category_id: category?.id || '', author_id: author?.id || '',
          category_title: category?.title || '', category_slug: category?.slug || '',
          author_name: author?.title || '', author_image: author?.image || '',
          status, tags: parseTags(row.tags), ...imagePayload(image), inline_media: [],
          seo_title: text(row.seo_title), seo_description: text(row.seo_description), video_link: video,
          pdf_book_link: pdf, pdf_file_provider: 'url', pdf_storage_path: '', pdf_file_size_mb: 0,
          is_slider: boolValue(row.is_slider), is_feature: boolValue(row.is_feature),
          is_special_article: boolValue(row.is_special_article), published_date: status === 'Publish' ? (publishedDate || new Date().toISOString().slice(0, 10)) : null
        }, issues);
      },
      duplicate(payload) { return normalized(payload.slug); },
      existing(record) { return normalized(record.slug); }
    },

    comments: {
      label: 'Comments', singular: 'comment', apiKey: 'comments', primary: 'name', duplicateSelect: 'id,blog_id,name,email,content',
      lookups: ['blogs'],
      columns: [
        field('blog_slug', 'Blog slug or title', 'a-new-magazine-article', { required: true, aliases: ['blog', 'blog_title'], description: 'Must match an existing Blog.' }),
        field('blog_id', 'Blog ID', '', { description: 'Optional alternative to Blog slug/title.' }),
        field('name', 'Name', 'Reader Name', { required: true }),
        field('email', 'Email', 'reader@example.com'),
        field('phone', 'Phone', ''),
        field('address', 'Address', ''),
        field('content', 'Comment', 'Thank you for this article.', { required: true, aliases: ['comment', 'message'] }),
        field('status', 'Status', 'Unpublish', { description: 'Publish or Unpublish.' })
      ],
      prepare(row, context, strict) {
        const issues = { errors: [], warnings: [] };
        requireValue(row, 'name', 'Name', issues, strict);
        requireValue(row, 'content', 'Comment', issues, strict);
        const blog = relation(context.blogs || [], [row.blog_id, row.blog_slug], ['slug', 'title']);
        if (!blog && (strict || row.blog_id || row.blog_slug)) addIssue(issues, 'Blog was not found. Use an existing Blog ID, slug, or title.', strict);
        return prepareResult({
          blog_id: blog?.id || '', blog_title: blog?.title || '', name: text(row.name),
          email: emailValue(row.email, issues, strict), phone: text(row.phone), address: text(row.address),
          content: text(row.content), status: /^publish(ed)?$/i.test(text(row.status)) ? 'Publish' : 'Unpublish'
        }, issues);
      },
      duplicate(payload) { return [payload.blog_id, normalized(payload.email || payload.name), normalized(payload.content)].join('|'); },
      existing(record) { return [record.blog_id, normalized(record.email || record.name), normalized(record.content)].join('|'); }
    },

    galleries: {
      label: 'Galleries', singular: 'gallery item', apiKey: 'galleries', primary: 'title', duplicateSelect: 'id,title,image,category',
      columns: [
        field('title', 'Title', 'Festival photograph', { required: true }),
        field('image', 'Image URL', 'https://example.com/gallery.jpg', { required: true, aliases: ['image_url', 'photo', 'photo_url'], description: 'Direct public image URL.' }),
        field('category', 'Category', 'Culture'),
        field('description', 'Description', 'Context for this photograph.')
      ],
      prepare(row, context, strict) {
        const issues = { errors: [], warnings: [] };
        requireValue(row, 'title', 'Title', issues, strict);
        requireValue(row, 'image', 'Image URL', issues, strict);
        const image = urlValue(row.image, 'Image URL', issues, strict);
        return prepareResult({ title: text(row.title), category: text(row.category), description: text(row.description), ...imagePayload(image) }, issues);
      },
      duplicate(payload) { return normalized(payload.image || `${payload.title}|${payload.category}`); },
      existing(record) { return normalized(record.image || `${record.title}|${record.category}`); }
    },

    books: {
      label: 'PDF Books', singular: 'PDF book', apiKey: 'books', primary: 'title', duplicateSelect: 'id,title,edition',
      columns: [
        field('title', 'Title', 'Annual Magazine 2026', { required: true }),
        field('book_published_date', 'Published date', '2026-08-30', { aliases: ['published_date', 'publish_date'], description: 'YYYY-MM-DD.' }),
        field('image', 'Cover image URL', 'https://example.com/cover.jpg', { aliases: ['image_url', 'cover', 'cover_url'] }),
        field('link', 'PDF URL', 'https://example.com/book.pdf', { aliases: ['pdf', 'pdf_url', 'pdf_link'], description: 'Direct public PDF URL.' }),
        field('author_or_editor', 'Author or editor', 'Editorial Team', { aliases: ['author', 'editor'] }),
        field('edition', 'Edition', '2026 edition'),
        field('category', 'Category', 'Annual anthology'),
        field('page_count', 'Page count', '120', { aliases: ['pages'] }),
        field('file_size_mb', 'File size MB', '4.5', { aliases: ['size_mb'] }),
        field('description', 'Description', 'Annual digital edition.')
      ],
      prepare(row, context, strict) {
        const issues = { errors: [], warnings: [] };
        requireValue(row, 'title', 'Title', issues, strict);
        const image = urlValue(row.image, 'Cover image URL', issues, strict);
        const link = urlValue(row.link, 'PDF URL', issues, strict);
        return prepareResult({
          title: text(row.title), book_published_date: dateValue(row.book_published_date, issues, 'Published date', strict),
          ...imagePayload(image), link, file_provider: 'url', file_storage_path: '',
          author_or_editor: text(row.author_or_editor), edition: text(row.edition), category: text(row.category),
          page_count: Math.floor(numberValue(row.page_count)), file_size_mb: numberValue(row.file_size_mb), description: text(row.description)
        }, issues);
      },
      duplicate(payload) { return `${normalized(payload.title)}|${normalized(payload.edition)}`; },
      existing(record) { return `${normalized(record.title)}|${normalized(record.edition)}`; }
    },

    submissions: {
      label: 'Submit Blogs', singular: 'submission', apiKey: 'submissions', primary: 'title', duplicateSelect: 'id,title,writer_email,writer_name',
      columns: [
        field('title', 'Submission title', 'Community article', { required: true }),
        field('designation', 'Submitter designation', ''),
        field('address', 'Address', 'Sylhet, Bangladesh'),
        field('phone', 'Phone', ''),
        field('thumbnail', 'Thumbnail URL', 'https://example.com/thumbnail.jpg', { aliases: ['image', 'image_url', 'thumbnail_url'] }),
        field('writer_name', 'Writer name', 'Contributor Name', { required: true, aliases: ['author_name', 'writer'] }),
        field('writer_designation', 'Writer designation', 'Writer'),
        field('writer_profile_image', 'Writer profile image URL', 'https://example.com/writer.jpg', { aliases: ['writer_image', 'profile_image'] }),
        field('writer_email', 'Writer email', 'writer@example.com', { aliases: ['email'] }),
        field('writer_facebook', 'Writer Facebook URL', '', { aliases: ['facebook'] }),
        field('content_title', 'Content title', 'Optional article subtitle', { aliases: ['subtitle'] }),
        field('content', 'Article content', '<p>Article body...</p>', { required: true, aliases: ['body', 'article_content'] }),
        field('status', 'Status', 'Pending', { description: 'Pending, Reviewed, or Rejected. Approval must use the dashboard conversion workflow.' })
      ],
      prepare(row, context, strict) {
        const issues = { errors: [], warnings: [] };
        requireValue(row, 'title', 'Submission title', issues, strict);
        requireValue(row, 'writer_name', 'Writer name', issues, strict);
        requireValue(row, 'content', 'Article content', issues, strict);
        const thumbnail = urlValue(row.thumbnail, 'Thumbnail URL', issues, strict);
        const writerImage = urlValue(row.writer_profile_image, 'Writer profile image URL', issues, strict);
        const facebook = urlValue(row.writer_facebook, 'Writer Facebook URL', issues, strict);
        const requestedStatus = text(row.status);
        const status = ['Pending', 'Reviewed', 'Rejected'].find((item) => normalized(item) === normalized(requestedStatus)) || 'Pending';
        if (requestedStatus && !['pending', 'reviewed', 'rejected'].includes(normalized(requestedStatus))) {
          addIssue(issues, 'Approved/Published status is reserved for the conversion workflow; status was set to Pending.', false);
        }
        const thumbnailMeta = imagePayload(thumbnail);
        const writerMeta = imagePayload(writerImage);
        return prepareResult({
          title: text(row.title), designation: text(row.designation), address: text(row.address), phone: text(row.phone),
          thumbnail, imgbb_delete_url: '', thumbnail_meta: thumbnailMeta.image_meta,
          writer_name: text(row.writer_name), writer_designation: text(row.writer_designation),
          writer_profile_image: writerImage, writer_profile_delete_url: '', writer_profile_meta: writerMeta.image_meta,
          writer_email: emailValue(row.writer_email, issues, strict), writer_facebook: facebook,
          content_title: text(row.content_title), content: richTextValue(row.content), inline_media: [], status
        }, issues);
      },
      duplicate(payload) { return `${normalized(payload.title)}|${normalized(payload.writer_email || payload.writer_name)}`; },
      existing(record) { return `${normalized(record.title)}|${normalized(record.writer_email || record.writer_name)}`; }
    },

    videos: {
      label: 'Videos', singular: 'video', apiKey: 'videos', primary: 'title', duplicateSelect: 'id,video_link',
      columns: [
        field('title', 'Title', 'Magazine interview', { required: true }),
        field('video_link', 'Video URL', 'https://www.youtube.com/watch?v=example', { required: true, aliases: ['video', 'video_url', 'url'] }),
        field('thumbnail_url', 'Fallback thumbnail URL', '', { aliases: ['thumbnail', 'image_url'] }),
        field('description', 'Description', 'Interview description.')
      ],
      prepare(row, context, strict) {
        const issues = { errors: [], warnings: [] };
        requireValue(row, 'title', 'Title', issues, strict);
        requireValue(row, 'video_link', 'Video URL', issues, strict);
        const video = urlValue(row.video_link, 'Video URL', issues, strict);
        const thumbnail = urlValue(row.thumbnail_url, 'Fallback thumbnail URL', issues, strict);
        const info = NC.media.detectVideoProvider(video);
        return prepareResult({
          title: text(row.title), video_link: info.url, platform: info.provider,
          description: text(row.description), thumbnail_url: thumbnail || info.thumbnail || ''
        }, issues);
      },
      duplicate(payload) { return normalized(payload.video_link); },
      existing(record) { return normalized(record.video_link); }
    }
  };

  function definition(type) {
    const item = definitions[type];
    if (!item) throw new Error(`Spreadsheet import is not configured for “${type}”.`);
    return item;
  }

  function aliasMap(item) {
    const map = new Map();
    item.columns.forEach((column) => {
      [column.key, column.label, ...column.aliases].forEach((alias) => map.set(headerKey(alias), column.key));
    });
    return map;
  }

  function rowsFromMatrix(matrix, item) {
    const first = matrix.findIndex((row) => Array.isArray(row) && row.some((cell) => text(cell)));
    if (first < 0) throw new Error('The selected sheet is empty.');
    const headers = matrix[first].map(headerKey);
    const aliases = aliasMap(item);
    const mapped = headers.map((header) => aliases.get(header) || '');
    const recognized = mapped.filter(Boolean).length;
    if (!recognized) {
      throw new Error(`No recognized ${item.singular} columns were found. Download the template and keep its header row.`);
    }
    const unknownHeaders = headers.filter((header, index) => header && !mapped[index]);
    const rows = [];
    for (let index = first + 1; index < matrix.length; index += 1) {
      const source = matrix[index] || [];
      if (!source.some((cell) => text(cell))) continue;
      const row = { __rowNumber: index + 1 };
      mapped.forEach((key, columnIndex) => {
        if (key && row[key] === undefined) row[key] = source[columnIndex] instanceof Date ? source[columnIndex] : text(source[columnIndex]);
      });
      rows.push(row);
      if (rows.length > MAX_ROWS) throw new Error(`This sheet has more than ${MAX_ROWS.toLocaleString()} data rows. Split it into smaller files.`);
    }
    if (!rows.length) throw new Error('The selected sheet has a header but no data rows.');
    return { rows, unknownHeaders };
  }

  async function readCsvText(file) {
    const bytes = new Uint8Array(await file.arrayBuffer());
    if (bytes[0] === 0xFF && bytes[1] === 0xFE) return new TextDecoder('utf-16le').decode(bytes.subarray(2));
    if (bytes[0] === 0xFE && bytes[1] === 0xFF) return new TextDecoder('utf-16be').decode(bytes.subarray(2));
    return new TextDecoder('utf-8').decode(bytes);
  }

  function parseCsv(textValue) {
    const rows = [];
    let row = [];
    let cell = '';
    let quoted = false;
    const source = String(textValue || '').replace(/^\uFEFF/, '');
    for (let index = 0; index < source.length; index += 1) {
      const char = source[index];
      if (quoted) {
        if (char === '"' && source[index + 1] === '"') { cell += '"'; index += 1; }
        else if (char === '"') quoted = false;
        else cell += char;
      } else if (char === '"') quoted = true;
      else if (char === ',') { row.push(cell); cell = ''; }
      else if (char === '\n') { row.push(cell.replace(/\r$/, '')); rows.push(row); row = []; cell = ''; }
      else cell += char;
    }
    if (quoted) throw new Error('The CSV contains an unterminated quoted field.');
    if (cell || row.length) { row.push(cell.replace(/\r$/, '')); rows.push(row); }
    return rows;
  }

  async function readSpreadsheet(file, type) {
    if (!(file instanceof File)) throw new Error('Choose a CSV or Excel file first.');
    if (file.size > MAX_FILE_BYTES) throw new Error('Spreadsheet files must be 10 MB or smaller.');
    const extension = file.name.split('.').pop().toLocaleLowerCase();
    if (!['csv', 'xlsx', 'xls'].includes(extension)) throw new Error('Choose a .csv, .xlsx, or .xls file.');
    const item = definition(type);
    if (extension === 'csv') {
      const matrix = parseCsv(await readCsvText(file));
      const parsed = rowsFromMatrix(matrix, item);
      return { fileName: file.name, sheets: [{ name: 'CSV', ...parsed }] };
    }
    if (!window.XLSX) throw new Error('The Excel parser did not load. Check the SheetJS CDN connection or use CSV instead.');
    const workbook = window.XLSX.read(await file.arrayBuffer(), { type: 'array', cellDates: true });
    const sheets = workbook.SheetNames.map((name) => {
      const matrix = window.XLSX.utils.sheet_to_json(workbook.Sheets[name], {
        header: 1, defval: '', raw: false, dateNF: 'yyyy-mm-dd', blankrows: false
      });
      try { return { name, ...rowsFromMatrix(matrix, item) }; }
      catch (error) { return { name, rows: [], unknownHeaders: [], error: error.message }; }
    });
    if (!sheets.some((sheet) => sheet.rows.length)) throw new Error('No worksheet contains a recognized header and at least one data row.');
    return { fileName: file.name, sheets };
  }

  async function listAll(apiKey, select = '*', maximum = MAX_CONTEXT_ROWS) {
    const records = [];
    for (let offset = 0; offset < maximum; offset += PAGE_SIZE) {
      const response = await NC.api.list(apiKey, { select, order: 'id.asc', limit: PAGE_SIZE, offset, count: offset === 0 });
      if (offset === 0 && response.count > maximum) {
        throw new Error(`Duplicate validation is limited to ${maximum.toLocaleString()} existing records. Narrow or archive this section before importing.`);
      }
      records.push(...response.data);
      if (response.data.length < PAGE_SIZE) return records;
    }
    const overflow = await NC.api.list(apiKey, { select: 'id', order: 'id.asc', limit: 1, offset: maximum });
    if (overflow.data.length) {
      throw new Error(`Duplicate validation is limited to ${maximum.toLocaleString()} existing records. Narrow or archive this section before importing.`);
    }
    return records;
  }

  async function loadContext(type, options = {}) {
    const item = definition(type);
    const requests = options.includeExisting === false ? [] : [['existing', listAll(item.apiKey, item.duplicateSelect)]];
    if (item.lookups?.includes('authors')) requests.push(['authors', listAll('authors', 'id,title,image')]);
    if (item.lookups?.includes('categories')) requests.push(['categories', listAll('categories', 'id,title,slug')]);
    if (item.lookups?.includes('blogs')) requests.push(['blogs', listAll('blogs', 'id,title,slug,status')]);
    const entries = await Promise.all(requests.map(async ([key, request]) => [key, await request]));
    return Object.fromEntries(entries);
  }

  function analyze(type, rows, context) {
    const item = definition(type);
    const seen = new Set((context.existing || []).map((record) => item.existing(record)).filter(Boolean));
    return rows.map((row) => {
      const prepared = item.prepare(row, context, true);
      const duplicateKey = item.duplicate(prepared.payload);
      let status = prepared.errors.length ? 'invalid' : 'valid';
      if (status === 'valid' && duplicateKey && seen.has(duplicateKey)) status = 'duplicate';
      if (status === 'valid' && duplicateKey) seen.add(duplicateKey);
      return { rowNumber: row.__rowNumber, source: row, ...prepared, duplicateKey, status };
    });
  }

  function summary(analysis) {
    return analysis.reduce((totals, row) => {
      totals[row.status] += 1;
      return totals;
    }, { valid: 0, duplicate: 0, invalid: 0 });
  }

  function templateBaseName(item) {
    return `ningshing-che-${item.apiKey.replace(/_/g, '-')}-template`;
  }

  function csvEscape(value) {
    const clean = String(value ?? '');
    return /[",\r\n]/.test(clean) ? `"${clean.replace(/"/g, '""')}"` : clean;
  }

  function downloadBlob(blob, fileName) {
    const url = URL.createObjectURL(blob);
    const link = document.createElement('a');
    link.href = url; link.download = fileName; link.hidden = true;
    document.body.appendChild(link); link.click(); link.remove();
    window.setTimeout(() => URL.revokeObjectURL(url), 1000);
  }

  function downloadTemplate(type, format = 'xlsx') {
    const item = definition(type);
    const headers = item.columns.map((column) => column.key);
    const example = item.columns.map((column) => column.example);
    if (format === 'csv') {
      const csv = `${headers.map(csvEscape).join(',')}\r\n${example.map(csvEscape).join(',')}\r\n`;
      downloadBlob(new Blob([`\uFEFF${csv}`], { type: 'text/csv;charset=utf-8' }), `${templateBaseName(item)}.csv`);
      return;
    }
    if (!window.XLSX) {
      NC.components.toast('The Excel template generator is unavailable. Download the CSV template instead.', 'error');
      return;
    }
    const workbook = window.XLSX.utils.book_new();
    const dataSheet = window.XLSX.utils.aoa_to_sheet([headers, example]);
    dataSheet['!cols'] = item.columns.map((column) => ({ wch: Math.min(42, Math.max(14, column.label.length + 4, String(column.example).length + 2)) }));
    const instructions = window.XLSX.utils.aoa_to_sheet([
      ['Field', 'Required', 'Description'],
      ...item.columns.map((column) => [column.key, column.required ? 'Yes' : 'No', column.description || column.label])
    ]);
    instructions['!cols'] = [{ wch: 28 }, { wch: 12 }, { wch: 80 }];
    window.XLSX.utils.book_append_sheet(workbook, dataSheet, 'Import Data');
    window.XLSX.utils.book_append_sheet(workbook, instructions, 'Instructions');
    window.XLSX.writeFile(workbook, `${templateBaseName(item)}.xlsx`, { compression: true });
  }

  function bulkButton(type) {
    const item = definition(type);
    return `<button type="button" class="btn btn-secondary" data-bulk-import="${escapeHTML(type)}" aria-label="Import ${escapeHTML(item.label)} from CSV or Excel"><i class="fa-regular fa-file-spreadsheet" aria-hidden="true"></i>Import CSV / Excel</button>`;
  }

  function formControlHTML(type) {
    const item = definition(type);
    return `
      <section class="form-import-card" data-form-import="${escapeHTML(type)}" aria-label="Fill form from spreadsheet">
        <div class="form-import-copy"><span class="form-import-icon"><i class="fa-regular fa-file-spreadsheet" aria-hidden="true"></i></span><div><strong>Fill from CSV or Excel</strong><small>Loads the first data row into this form. Review everything before saving.</small></div></div>
        <div class="form-import-actions">
          <button type="button" class="btn btn-ghost btn-sm" data-template-format="csv"><i class="fa-regular fa-file-csv" aria-hidden="true"></i>CSV template</button>
          <button type="button" class="btn btn-ghost btn-sm" data-template-format="xlsx"><i class="fa-regular fa-file-excel" aria-hidden="true"></i>Excel template</button>
          <label class="btn btn-secondary btn-sm cursor-pointer"><i class="fa-regular fa-file-arrow-up" aria-hidden="true"></i>Choose file<input type="file" class="sr-only" accept=".csv,.xlsx,.xls,text/csv,application/vnd.openxmlformats-officedocument.spreadsheetml.sheet,application/vnd.ms-excel" data-form-import-file></label>
        </div>
        <p class="form-import-status" data-form-import-status aria-live="polite">Use the provided ${escapeHTML(item.label)} template for the expected columns.</p>
      </section>`;
  }

  function fillForm(form, values) {
    if (!form || !values) return;
    Object.entries(values).forEach(([name, value]) => {
      const control = form.elements?.namedItem(name);
      if (!control || value === undefined || value === null || typeof value === 'object' && !Array.isArray(value)) return;
      if (control instanceof RadioNodeList) {
        control.value = String(value);
        return;
      }
      if (control.type === 'checkbox') control.checked = boolValue(value);
      else control.value = Array.isArray(value) ? value.join(', ') : String(value);
      control.dispatchEvent(new Event('input', { bubbles: true }));
      control.dispatchEvent(new Event('change', { bubbles: true }));
    });
  }

  function mountFormControl(root, options) {
    const type = options.type;
    const element = root.querySelector(`[data-form-import="${type}"]`);
    if (!element) return null;
    const input = qs('[data-form-import-file]', element);
    const status = qs('[data-form-import-status]', element);
    qsa('[data-template-format]', element).forEach((button) => button.addEventListener('click', () => downloadTemplate(type, button.dataset.templateFormat)));
    input.addEventListener('change', async () => {
      const file = input.files?.[0];
      if (!file) return;
      status.innerHTML = '<i class="fa-regular fa-spinner-third fa-spin" aria-hidden="true"></i> Reading spreadsheet and resolving relationships…';
      element.classList.add('is-loading');
      try {
        const [workbook, context] = await Promise.all([readSpreadsheet(file, type), loadContext(type, { includeExisting: false })]);
        const sheet = workbook.sheets.find((item) => item.rows.length);
        const firstRow = sheet.rows[0];
        const prepared = definition(type).prepare(firstRow, context, false);
        await options.onRecord?.({ ...prepared, row: firstRow, fileName: workbook.fileName, sheetName: sheet.name, context });
        const issueCount = prepared.errors.length + prepared.warnings.length;
        status.innerHTML = `<i class="fa-regular fa-circle-check" aria-hidden="true"></i> Loaded row ${firstRow.__rowNumber} from ${escapeHTML(workbook.fileName)}${issueCount ? ` with ${issueCount} warning${issueCount === 1 ? '' : 's'}` : ''}. Review before saving.`;
        status.className = `form-import-status ${issueCount ? 'is-warning' : 'is-success'}`;
        if (issueCount) NC.components.toast(prepared.errors.concat(prepared.warnings).join(' '), 'warning', { duration: 8000 });
        else NC.components.toast(`Form filled from ${workbook.fileName}. Review it before saving.`, 'success');
      } catch (error) {
        status.textContent = error.message;
        status.className = 'form-import-status is-error';
        NC.components.toast(error.message, 'error');
      } finally {
        element.classList.remove('is-loading'); input.value = '';
      }
    });
    return { element };
  }

  function importModalHTML(item) {
    return `
      <div class="import-wizard">
        <div class="import-toolbar">
          <div>${NC.components.notice('Spreadsheet media columns accept direct public URLs. Embedded images and PDF binaries are not imported.', 'info')}</div>
          <div class="import-template-actions"><span>Download template:</span><button type="button" class="btn btn-secondary btn-sm" data-template-format="csv"><i class="fa-regular fa-file-csv" aria-hidden="true"></i>CSV</button><button type="button" class="btn btn-secondary btn-sm" data-template-format="xlsx"><i class="fa-regular fa-file-excel" aria-hidden="true"></i>Excel</button></div>
        </div>
        <label class="import-drop-zone" data-import-drop-zone tabindex="0">
          <input type="file" class="sr-only" accept=".csv,.xlsx,.xls,text/csv,application/vnd.openxmlformats-officedocument.spreadsheetml.sheet,application/vnd.ms-excel" data-import-file>
          <span><i class="fa-regular fa-cloud-arrow-up" aria-hidden="true"></i></span>
          <strong>Choose a ${escapeHTML(item.singular)} CSV or Excel file</strong>
          <small>or drag and drop it here · .csv, .xlsx, or .xls · maximum 10 MB / ${MAX_ROWS.toLocaleString()} rows</small>
        </label>
        <div class="import-sheet-row hidden" data-import-sheet-row><label class="field-label" for="import-sheet-select">Worksheet</label><select class="form-select" id="import-sheet-select" data-import-sheet></select></div>
        <div data-import-output><div class="import-idle"><i class="fa-regular fa-table-cells" aria-hidden="true"></i><p>Your validated row preview will appear here before anything is saved.</p></div></div>
      </div>`;
  }

  function previewHTML(item, analysis, fileName, sheet) {
    const totals = summary(analysis);
    const rows = analysis.slice(0, 100).map((row) => {
      const issues = row.errors.concat(row.warnings);
      const label = text(row.payload[item.primary]) || `Row ${row.rowNumber}`;
      const status = row.status === 'valid'
        ? '<span class="import-status is-valid"><i class="fa-solid fa-circle-check" aria-hidden="true"></i>Ready</span>'
        : row.status === 'duplicate'
          ? '<span class="import-status is-duplicate"><i class="fa-solid fa-forward" aria-hidden="true"></i>Duplicate</span>'
          : '<span class="import-status is-invalid"><i class="fa-solid fa-circle-xmark" aria-hidden="true"></i>Invalid</span>';
      return `<tr><td data-label="Row">${row.rowNumber}</td><td data-label="Record"><strong>${escapeHTML(label)}</strong></td><td data-label="Status">${status}</td><td data-label="Details">${escapeHTML(issues.join(' ') || (row.status === 'duplicate' ? 'Matches an existing record or an earlier row.' : 'All required fields are valid.'))}</td></tr>`;
    }).join('');
    return `
      <div class="import-file-summary"><div><i class="fa-regular fa-file-spreadsheet" aria-hidden="true"></i><span><strong>${escapeHTML(fileName)}</strong><small>${escapeHTML(sheet.name)} · ${analysis.length.toLocaleString()} data rows</small></span></div><span>Duplicates will be skipped</span></div>
      <div class="import-summary-grid"><article><span class="is-valid"><i class="fa-regular fa-circle-check" aria-hidden="true"></i></span><div><strong>${totals.valid.toLocaleString()}</strong><small>Ready to import</small></div></article><article><span class="is-duplicate"><i class="fa-regular fa-forward" aria-hidden="true"></i></span><div><strong>${totals.duplicate.toLocaleString()}</strong><small>Duplicates skipped</small></div></article><article><span class="is-invalid"><i class="fa-regular fa-circle-xmark" aria-hidden="true"></i></span><div><strong>${totals.invalid.toLocaleString()}</strong><small>Invalid rows</small></div></article></div>
      ${sheet.unknownHeaders.length ? `<div class="notice notice-warning"><i class="fa-solid fa-triangle-exclamation" aria-hidden="true"></i><p>Ignored unrecognized columns: ${escapeHTML(sheet.unknownHeaders.join(', '))}</p></div>` : ''}
      <div class="import-preview-wrap"><table class="data-table import-preview-table"><caption class="sr-only">Spreadsheet validation preview</caption><thead><tr><th>Row</th><th>Record</th><th>Status</th><th>Details</th></tr></thead><tbody>${rows}</tbody></table></div>
      ${analysis.length > 100 ? `<p class="field-hint mt-3">Showing the first 100 of ${analysis.length.toLocaleString()} rows. All rows were validated.</p>` : ''}`;
  }

  function resultHTML(item, results, skipped) {
    const imported = results.filter((result) => result.status === 'imported');
    const failed = results.filter((result) => result.status === 'failed');
    const rows = failed.slice(0, 100).map((result) => `<tr><td>${result.item.rowNumber}</td><td><strong>${escapeHTML(text(result.item.payload[item.primary]) || `Row ${result.item.rowNumber}`)}</strong></td><td>${escapeHTML(NC.api.userMessage(result.error, 'Import failed.'))}</td></tr>`).join('');
    return `
      <div class="import-complete ${failed.length ? 'has-failures' : ''}" data-import-result tabindex="-1"><span><i class="fa-solid ${failed.length ? 'fa-triangle-exclamation' : 'fa-circle-check'}" aria-hidden="true"></i></span><div><h3>${failed.length ? 'Import completed with some errors' : 'Import completed successfully'}</h3><p>${imported.length.toLocaleString()} ${escapeHTML(item.singular)} record${imported.length === 1 ? '' : 's'} imported · ${skipped.toLocaleString()} duplicate/invalid row${skipped === 1 ? '' : 's'} skipped${failed.length ? ` · ${failed.length.toLocaleString()} failed` : ''}.</p></div></div>
      ${failed.length ? `<div class="import-preview-wrap mt-4"><table class="data-table import-preview-table"><thead><tr><th>Row</th><th>Record</th><th>Error</th></tr></thead><tbody>${rows}</tbody></table></div>` : ''}`;
  }

  function openBulk(options) {
    const type = options.type;
    const item = definition(type);
    let workbook = null;
    let context = null;
    let analysis = [];
    let activeSheet = null;
    let importing = false;
    const modal = NC.components.openModal({
      title: `Import ${item.label}`, eyebrow: 'CSV / Excel bulk import', size: 'xl',
      description: `Validate a spreadsheet, skip duplicates, and create multiple ${item.label.toLocaleLowerCase()} safely.`,
      content: importModalHTML(item),
      footer: '<button type="button" class="btn btn-secondary" data-modal-close>Close</button><button type="button" class="btn btn-primary" data-run-import disabled><i class="fa-regular fa-file-import" aria-hidden="true"></i>Import valid rows</button>',
      onOpen: (modalRoot) => {
        const input = qs('[data-import-file]', modalRoot);
        const dropZone = qs('[data-import-drop-zone]', modalRoot);
        const output = qs('[data-import-output]', modalRoot);
        const sheetRow = qs('[data-import-sheet-row]', modalRoot);
        const sheetSelect = qs('[data-import-sheet]', modalRoot);
        const importButton = qs('[data-run-import]', modalRoot);
        const closeButtons = qsa('[data-modal-close]', modalRoot);

        function blockDismiss(event) {
          if (!importing) return;
          if (event.type === 'keydown' && event.key !== 'Escape') return;
          event.preventDefault(); event.stopPropagation(); event.stopImmediatePropagation();
        }
        modalRoot.addEventListener('click', blockDismiss, true);
        modalRoot.addEventListener('keydown', blockDismiss, true);

        qsa('[data-template-format]', modalRoot).forEach((button) => button.addEventListener('click', () => downloadTemplate(type, button.dataset.templateFormat)));

        async function analyzeSheet(sheet) {
          activeSheet = sheet;
          output.innerHTML = NC.components.skeleton(4, 4);
          if (!context) context = await loadContext(type);
          analysis = analyze(type, sheet.rows, context);
          output.innerHTML = previewHTML(item, analysis, workbook.fileName, sheet);
          const totals = summary(analysis);
          importButton.disabled = totals.valid === 0;
          importButton.innerHTML = `<i class="fa-regular fa-file-import" aria-hidden="true"></i>Import ${totals.valid.toLocaleString()} valid row${totals.valid === 1 ? '' : 's'}`;
        }

        async function processFile(file) {
          if (!file || importing) return;
          dropZone.classList.add('is-loading');
          output.innerHTML = '<div class="import-loading"><i class="fa-regular fa-spinner-third fa-spin" aria-hidden="true"></i><p>Reading spreadsheet and loading current records…</p></div>';
          importButton.disabled = true;
          try {
            [workbook, context] = await Promise.all([readSpreadsheet(file, type), loadContext(type)]);
            const usableSheets = workbook.sheets.filter((sheet) => sheet.rows.length);
            sheetSelect.innerHTML = usableSheets.map((sheet, index) => `<option value="${index}">${escapeHTML(sheet.name)} · ${sheet.rows.length.toLocaleString()} rows</option>`).join('');
            sheetRow.classList.toggle('hidden', usableSheets.length < 2);
            await analyzeSheet(usableSheets[0]);
            sheetSelect.onchange = () => analyzeSheet(usableSheets[Number(sheetSelect.value)]).catch(showError);
          } catch (error) { showError(error); }
          finally { dropZone.classList.remove('is-loading'); input.value = ''; }
        }

        function showError(error) {
          output.innerHTML = `<div class="import-error"><i class="fa-solid fa-file-circle-exclamation" aria-hidden="true"></i><h3>Could not read this spreadsheet</h3><p>${escapeHTML(error.message)}</p></div>`;
          NC.components.toast(error.message, 'error');
        }

        input.addEventListener('change', () => processFile(input.files?.[0]));
        dropZone.addEventListener('keydown', (event) => {
          if (event.key === 'Enter' || event.key === ' ') { event.preventDefault(); input.click(); }
        });
        ['dragenter', 'dragover'].forEach((name) => dropZone.addEventListener(name, (event) => { event.preventDefault(); dropZone.classList.add('is-dragging'); }));
        ['dragleave', 'drop'].forEach((name) => dropZone.addEventListener(name, (event) => { event.preventDefault(); dropZone.classList.remove('is-dragging'); }));
        dropZone.addEventListener('drop', (event) => processFile(event.dataTransfer?.files?.[0]));

        importButton.addEventListener('click', async () => {
          const valid = analysis.filter((row) => row.status === 'valid');
          if (!valid.length || importing) return;
          importing = true;
          importButton.disabled = true;
          closeButtons.forEach((button) => { button.disabled = true; });
          output.innerHTML = `<div class="import-progress-panel"><div><i class="fa-regular fa-spinner-third fa-spin" aria-hidden="true"></i><span><strong>Importing ${valid.length.toLocaleString()} ${escapeHTML(item.label.toLocaleLowerCase())}…</strong><small data-import-progress-label role="status" aria-live="polite">0 of ${valid.length.toLocaleString()} complete</small></span></div><div class="upload-progress"><div class="upload-progress-track"><span data-import-progress-bar></span></div><span data-import-progress-percent>0%</span></div></div>`;
          const results = [];
          let cursor = 0;
          let completed = 0;
          const progressBar = qs('[data-import-progress-bar]', output);
          const progressPercent = qs('[data-import-progress-percent]', output);
          const progressLabel = qs('[data-import-progress-label]', output);
          async function worker() {
            while (cursor < valid.length) {
              const itemToImport = valid[cursor]; cursor += 1;
              try {
                const record = await NC.api.insert(item.apiKey, itemToImport.payload);
                results.push({ status: 'imported', item: itemToImport, record });
              } catch (error) {
                results.push({ status: error.code === '23505' ? 'duplicate' : 'failed', item: itemToImport, error });
              }
              completed += 1;
              const percent = Math.round((completed / valid.length) * 100);
              progressBar.style.width = `${percent}%`; progressPercent.textContent = `${percent}%`;
              progressLabel.textContent = `${completed.toLocaleString()} of ${valid.length.toLocaleString()} complete`;
            }
          }
          await Promise.all(Array.from({ length: Math.min(4, valid.length) }, worker));
          const skipped = analysis.length - valid.length + results.filter((result) => result.status === 'duplicate').length;
          output.innerHTML = resultHTML(item, results, skipped);
          qs('[data-import-result]', output)?.focus();
          importing = false;
          closeButtons.forEach((button) => { button.disabled = false; });
          importButton.classList.add('hidden');
          const importedCount = results.filter((result) => result.status === 'imported').length;
          if (importedCount) {
            NC.components.toast(`${importedCount.toLocaleString()} ${item.singular} record${importedCount === 1 ? '' : 's'} imported.`, 'success');
            try { await options.onComplete?.(); }
            catch (error) {
              console.error(error);
              NC.components.toast('Records were imported, but the list could not refresh. Close this dialog and reload the section.', 'warning');
            }
          }
        });
      }
    });
    return modal;
  }

  function bindBulk(root, options) {
    qsa(`[data-bulk-import="${options.type}"]`, root).forEach((button) => {
      if (button.dataset.importBound) return;
      button.dataset.importBound = 'true';
      button.addEventListener('click', () => openBulk(options));
    });
  }

  NC.importer = Object.freeze({
    definitions, bulkButton, bindBulk, openBulk,
    formControlHTML, mountFormControl, fillForm,
    downloadTemplate, readSpreadsheet, loadContext
  });
})(window.NC);
