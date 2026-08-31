(function () {
  'use strict';

  /**
   * Browser-safe application configuration.
   *
   * IMPORTANT:
   * - The Supabase value below is a publishable browser key, never a service key.
   * - ImgBB requires its upload key in browser requests; restrict/rotate it in
   *   the provider dashboard if the project is ever transferred.
   * - Migration 004 validates dashboard credentials and permissions in Supabase.
   */
  window.NC_CONFIG = Object.freeze({
    app: Object.freeze({
      name: 'Ningshing Che',
      subtitle: 'Editorial Command Center',
      version: '1.3.0',
      websiteUrl: 'https://ningshingche.com',
      locale: 'en-BD',
      timeZone: 'Asia/Dhaka',
      sessionHours: 8,
      rememberedSessionDays: 7,
      defaultTheme: 'dark',
      defaultRoute: 'dashboard',
      defaultPageSize: 10,
      requestTimeoutMs: 20000
    }),

    supabase: Object.freeze({
      url: 'https://slcpvmpsynkqdozvlsii.supabase.co',
      publishableKey: 'sb_publishable_jqJACnQHmCMcGjt0kG6Sug_ddknIbAA',
      restPath: '/rest/v1',
      storagePath: '/storage/v1',
      pdfBucket: 'pdf-books',
      pdfMaxBytes: 32 * 1024 * 1024
    }),

    imgbb: Object.freeze({
      endpoint: 'https://api.imgbb.com/1/upload',
      apiKey: '576f654932a2b7398e765cf27d8c73d4',
      maxBytes: 32 * 1024 * 1024,
      acceptedTypes: Object.freeze([
        'image/jpeg', 'image/png', 'image/webp', 'image/gif',
        'image/bmp', 'image/avif', 'image/heic', 'image/heif'
      ])
    }),

    // Migration-only fallback. Once migration 004 installs dashboard_login,
    // credentials and roles come exclusively from Supabase.
    auth: Object.freeze({
      username: 'admin',
      passwordHash: '240be518fabd2724ddb6f04eeb1da5967448d7e831c08c8fa822809f74c720a9',
      displayName: 'Chief Editor',
      role: 'Super Admin'
    }),

    tables: Object.freeze({
      authors: 'authors',
      categories: 'categories',
      blogs: 'blogs',
      comments: 'comments',
      galleries: 'galleries',
      books: 'pdf_books',
      submissions: 'submitted_blogs',
      videos: 'videos',
      settings: 'settings'
    }),

    routes: Object.freeze([
      { id: 'dashboard', label: 'Dashboard', icon: 'fa-gauge-high', group: 'overview' },
      { id: 'authors', label: 'Authors', icon: 'fa-user-pen', group: 'content' },
      { id: 'blogs', label: 'Blogs', icon: 'fa-newspaper', group: 'content' },
      { id: 'categories', label: 'Categories', icon: 'fa-layer-group', group: 'content' },
      { id: 'comments', label: 'Comments', icon: 'fa-comments', group: 'content' },
      { id: 'galleries', label: 'Galleries', icon: 'fa-images', group: 'content' },
      { id: 'books', label: 'PDF Books', icon: 'fa-books', group: 'content' },
      { id: 'submissions', label: 'Submit Blogs', icon: 'fa-file-pen', group: 'content' },
      { id: 'videos', label: 'Videos', icon: 'fa-video', group: 'content' },
      { id: 'analytics', label: 'Analytics', icon: 'fa-chart-mixed', group: 'system' },
      { id: 'settings', label: 'Settings', icon: 'fa-gear', group: 'system' },
      { id: 'access-control', label: 'Users & Roles', icon: 'fa-user-shield', group: 'system' }
    ])
  });

  window.NC = window.NC || { views: {}, state: {} };
})();
