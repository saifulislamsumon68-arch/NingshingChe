package com.example.data.repository

import com.example.data.model.Article
import com.example.data.model.Author
import com.example.data.model.Category
import com.example.data.model.PdfCategory
import com.example.data.model.PdfDocument
import com.example.data.model.YearArchive

object NinghsingCheContentData {

    const val APP_LOGO_URL = "https://surajit-singha-sisir.github.io/NingshingCheNew/NingshingChe-Logo.png"
    const val WEBSITE_URL = "https://ningshingche.com"
    const val GITHUB_REPO_URL = "https://github.com/surajit-singha-sisir/NingshingCheNew"

    val pdfCategories: List<PdfCategory> = listOf(
        PdfCategory(
            id = "pdf-cat-all",
            name = "সকল PDF",
            description = "নিংশিং চে-র সকল প্রকাশিত ডিজিটাল ও মুদ্রিত পিডিএফ প্রকাশনা।",
            count = 9
        ),
        PdfCategory(
            id = "pdf-cat-annual",
            name = "বার্ষিক ও উৎসব সংখ্যা",
            description = "নিয়মিত বার্ষিক সংখ্যা, শারদীয় ও বসন্ত উৎসব সংখ্যা।",
            count = 3
        ),
        PdfCategory(
            id = "pdf-cat-jubilee",
            name = "সুবর্ণ জয়ন্তী ও ইতিহাস",
            description = "বিষ্ণুপ্রিয়া মণিপুরি ইতিহাস ও সুবর্ণ জয়ন্তীর বিশেষ প্রকাশনা।",
            count = 2
        ),
        PdfCategory(
            id = "pdf-cat-research",
            name = "গবেষণা ও ভাষা আন্দোলন",
            description = "ভাষা আন্দোলন, প্রামাণ্য দলিল ও নৃতাত্ত্বিক গবেষণা প্রবন্ধমালা।",
            count = 2
        ),
        PdfCategory(
            id = "pdf-cat-literature",
            name = "সাহিত্য ও সংকলন",
            description = "কবিতা, লোকগাথা, নাটক ও প্রাচীন পাণ্ডুলিপি সংকলন।",
            count = 2
        )
    )

    val pdfDocuments: List<PdfDocument> = listOf(
        PdfDocument(
            id = "pdf-doc-1",
            title = "নিংশিং চে — সুবর্ণ জয়ন্তী বিশেষ সংকলন",
            edition = "সুবর্ণ জয়ন্তী সংস্করণ (২০২৪)",
            category = "সুবর্ণ জয়ন্তী ও ইতিহাস",
            categorySlug = "pdf-cat-jubilee",
            year = 2024,
            authorOrEditor = "অধ্যাপক বারীন্দ্র কুমার সিংহ ও সম্পাদনা পরিষদ",
            pageCount = 148,
            fileSizeMb = 14.2f,
            pdfUrl = "https://ningshingche.com/archive/pdf/ningshingche-golden-jubilee-2024.pdf",
            coverImageUrl = "https://images.unsplash.com/photo-1544716278-ca5e3f4abd8c?w=600&q=80",
            description = "বিষ্ণুপ্রিয়া মণিপুরি ভাষার পঞ্চাশ বছরের সাহিত্য, সমাজবিকাশ ও সাংস্কৃতিক আন্দোলনের প্রামাণ্য সুবর্ণ জয়ন্তী সংকলন।",
            tags = listOf("সুবর্ণ জয়ন্তী", "ইতিহাস", "প্রামাণ্য দলিল", "সংকলন")
        ),
        PdfDocument(
            id = "pdf-doc-2",
            title = "নিংশিং চে — দ্বাদশ বার্ষিক সংখ্যা (২০২৫)",
            edition = "দ্বাদশ বর্ষ ১ম সংখ্যা (২০২৫)",
            category = "বার্ষিক ও উৎসব সংখ্যা",
            categorySlug = "pdf-cat-annual",
            year = 2025,
            authorOrEditor = "নিংশিং চে প্রকাশনা পর্ষদ",
            pageCount = 96,
            fileSizeMb = 8.5f,
            pdfUrl = "https://ningshingche.com/archive/pdf/ningshingche-vol12-2025.pdf",
            coverImageUrl = "https://images.unsplash.com/photo-1457369804613-52c61a468e7d?w=600&q=80",
            description = "আধুনিক বিষ্ণুপ্রিয়া মণিপুরি প্রবন্ধ, কবিতা, ডিজিটাল সাহিত্য ও সমকালীন শিল্পভাবনার সংকলন।",
            tags = listOf("২০২৫", "বার্ষিক সংখ্যা", "সাহিত্য", "প্রবন্ধ")
        ),
        PdfDocument(
            id = "pdf-doc-3",
            title = "বিষ্ণুপ্রিয়া মণিপুরি ভাষা আন্দোলন প্রামাণ্য দলিল",
            edition = "ঐতিহাসিক গবেষণা প্রকাশনা (২০২৩)",
            category = "গবেষণা ও ভাষা আন্দোলন",
            categorySlug = "pdf-cat-research",
            year = 2023,
            authorOrEditor = "ড. প্রদীপ সিংহ ও সুজিত সিংহ",
            pageCount = 120,
            fileSizeMb = 11.8f,
            pdfUrl = "https://ningshingche.com/archive/pdf/language-movement-documents-2023.pdf",
            coverImageUrl = "https://images.unsplash.com/photo-1461360370896-922624d12aa1?w=600&q=80",
            description = "১৯৫৫ সালের ১৬ মার্চ থেকে শুরু করে মাতৃভাষা স্বীকৃতির দীর্ঘ ঐতিহাসিক সংগ্রামের পূর্ণাঙ্গ প্রামাণ্য নথি ও চিত্রাবলি।",
            tags = listOf("ভাষা আন্দোলন", "সুদেষ্ণা সিংহ", "গবেষণা", "নথি")
        ),
        PdfDocument(
            id = "pdf-doc-4",
            title = "নিংশিং চে — ৩য় সংখ্যা মুদ্রণ সংস্করণ",
            edition = "ঐতিহাসিক ৩য় মুদ্রণ (২০১৬)",
            category = "বার্ষিক ও উৎসব সংখ্যা",
            categorySlug = "pdf-cat-annual",
            year = 2016,
            authorOrEditor = "নিংশিং চে সাহিত্য পর্ষদ",
            pageCount = 64,
            fileSizeMb = 5.6f,
            pdfUrl = "https://ningshingche.com/archive/pdf/ningshingche-issue3-2016.pdf",
            coverImageUrl = "https://images.unsplash.com/photo-1512820790803-83ca734da794?w=600&q=80",
            description = "নিংশিং চে-র ঐতিহাসিক ৩য় মুদ্রণ সংখ্যা, যাতে স্থান পেয়েছিল বহু বিরল পাণ্ডুলিপি ও প্রাচীন কবিতা।",
            tags = listOf("২০১৬", "মুদ্রিত সংখ্যা", "বিরল পাণ্ডুলিপি", "স্মৃতি")
        ),
        PdfDocument(
            id = "pdf-doc-5",
            title = "মণিপুরি লোকসংস্কৃতি, নৃত্যকলা ও গীতিসংকলন",
            edition = "লোকসাহিত্য গবেষণা সংস্করণ (২০২২)",
            category = "সাহিত্য ও সংকলন",
            categorySlug = "pdf-cat-literature",
            year = 2022,
            authorOrEditor = "সুজিত সিংহ ও স্মৃতিকণা সিংহ",
            pageCount = 112,
            fileSizeMb = 9.4f,
            pdfUrl = "https://ningshingche.com/archive/pdf/manipuri-folk-culture-dance-2022.pdf",
            coverImageUrl = "https://images.unsplash.com/photo-1533854775446-95c4609da544?w=600&q=80",
            description = "মহারাস, বসন্তরাস, তাল-লয়, ঢাকের বোল এবং বিষ্ণুপ্রিয়া মণিপুরি ধ্রুপদী নৃত্যকলার পূর্ণাঙ্গ সচিত্র নির্দেশিকা।",
            tags = listOf("লোকসংস্কৃতি", "নৃত্যকলা", "রাসলীলা", "লোকসংগীত")
        ),
        PdfDocument(
            id = "pdf-doc-6",
            title = "নিংশিং চে — একাদশ বর্ষ শারদীয় সংখ্যা (২০২৪)",
            edition = "শারদীয় বিশেষ সংখ্যা (২০২৪)",
            category = "বার্ষিক ও উৎসব সংখ্যা",
            categorySlug = "pdf-cat-annual",
            year = 2024,
            authorOrEditor = "সম্পাদনা পরিষদ",
            pageCount = 88,
            fileSizeMb = 7.9f,
            pdfUrl = "https://ningshingche.com/archive/pdf/ningshingche-autumn-2024.pdf",
            coverImageUrl = "https://images.unsplash.com/photo-1506880018603-83d5b814b5a6?w=600&q=80",
            description = "শারদীয় উৎসবের রঙে সাজানো বিষ্ণুপ্রিয়া মণিপুরি সাহিত্য, গল্প ও স্মৃতিকথা সংকলন।",
            tags = listOf("২০২৪", "শারদীয় সংখ্যা", "গল্প", "উৎসব")
        ),
        PdfDocument(
            id = "pdf-doc-7",
            title = "শহীদ সুদেষ্ণা সিংহ স্মারক পত্রিকা",
            edition = "স্মারক সংখ্যা (২০১৮)",
            category = "গবেষণা ও ভাষা আন্দোলন",
            categorySlug = "pdf-cat-research",
            year = 2018,
            authorOrEditor = "অধ্যাপক বারীন্দ্র কুমার সিংহ",
            pageCount = 76,
            fileSizeMb = 6.8f,
            pdfUrl = "https://ningshingche.com/archive/pdf/martyr-sudeshna-singha-memorial-2018.pdf",
            coverImageUrl = "https://images.unsplash.com/photo-1524995997946-a1c2e315a42f?w=600&q=80",
            description = "মাতৃভাষার প্রথম নারী শহীদ সুদেষ্ণা সিংহের জীবন, আদর্শ ও ১৬ মার্চের আত্মদানের ইতিহাস।",
            tags = listOf("সুদেষ্ণা সিংহ", "শহীদ স্মারক", "ভাষা আন্দোলন", "ইতিহাস")
        ),
        PdfDocument(
            id = "pdf-doc-8",
            title = "ইঞ্চৌঘর ও মণিপুরি লোকস্থাপত্য সমীক্ষা",
            edition = "স্থাপত্য গবেষণা গ্রন্থ (২০২১)",
            category = "সুবর্ণ জয়ন্তী ও ইতিহাস",
            categorySlug = "pdf-cat-jubilee",
            year = 2021,
            authorOrEditor = "ড. প্রদীপ সিংহ",
            pageCount = 84,
            fileSizeMb = 8.1f,
            pdfUrl = "https://ningshingche.com/archive/pdf/inchoughor-architecture-survey-2021.pdf",
            coverImageUrl = "https://images.unsplash.com/photo-1486406146926-c627a92ad1ab?w=600&q=80",
            description = "প্রকৃতির সাথে তাল মিলিয়ে নির্মিত পরিবেশবান্ধব বাঁশ, মাটি ও ছনের ঐতিহ্যবাহী ইঞ্চৌঘরের স্থাপত্য বিশ্লেষণ।",
            tags = listOf("ইঞ্চৌঘর", "স্থাপত্য", "ঐতিহ্য", "পরিবেশ")
        ),
        PdfDocument(
            id = "pdf-doc-9",
            title = "নিংশিং চে — প্রারম্ভিক মুদ্রণ সংখ্যা (২০১৪)",
            edition = "প্রতিষ্ঠা ১ম সংখ্যা (২০১৪)",
            category = "সাহিত্য ও সংকলন",
            categorySlug = "pdf-cat-literature",
            year = 2014,
            authorOrEditor = "প্রতিষ্ঠাতা সম্পাদক মণ্ডলী",
            pageCount = 52,
            fileSizeMb = 4.7f,
            pdfUrl = "https://ningshingche.com/archive/pdf/ningshingche-inaugural-2014.pdf",
            coverImageUrl = "https://images.unsplash.com/photo-1495640388908-05fa85288e61?w=600&q=80",
            description = "নিংশিং চে তথ্যকোষের জন্মলগ্নের ঐতিহাসিক প্রথম প্রকাশনা ও প্রতিষ্ঠাতা রূপরেখা।",
            tags = listOf("২০১৪", "প্রথম সংখ্যা", "প্রতিষ্ঠা", "আর্কাইভ")
        )
    )

    val categories: List<Category> = listOf(
        Category(
            id = "cat-history",
            name = "ইতিহাস",
            slug = "history",
            description = "বিষ্ণুপ্রিয়া মণিপুরি জাতির প্রাচীন উৎপত্তি, মণিপুরের লোকতাক হ্রদের ইতিহাস ও অভিবাসনের ধারাবাহিক প্রেক্ষাপট।",
            articleCount = 28,
            iconName = "history_edu"
        ),
        Category(
            id = "cat-literature",
            name = "সাহিত্য",
            slug = "literature",
            description = "বিষ্ণুপ্রিয়া মণিপুরি কাব্যধারা, গল্প, নাটক, সমালোচনা এবং আধুনিক সাহিত্য আন্দোলনের রূপরেখা।",
            articleCount = 34,
            iconName = "menu_book"
        ),
        Category(
            id = "cat-society-culture",
            name = "সমাজ ও সংস্কৃতি",
            slug = "society-culture",
            description = "ঐতিহ্যবাহী মণিপুরি সমাজব্যবস্থা, লোকাচার, নৃত্যকলা, রাসলীলা, পোশাক-পরিচ্ছদ ও লোকসংগীত।",
            articleCount = 42,
            iconName = "groups"
        ),
        Category(
            id = "cat-biography",
            name = "জীবনী",
            slug = "biography",
            description = "সমাজসংস্কারক, ভাষা সংগ্রামী, সাহিত্যিক ও বিশিষ্ট গুণীজনদের অনুপ্রেরণাদায়ী জীবনকথা।",
            articleCount = 22,
            iconName = "person"
        ),
        Category(
            id = "cat-reminiscence",
            name = "স্মৃতিচারণ",
            slug = "reminiscence",
            description = "হারিয়ে যাওয়া স্মৃতি, প্রাচীন পল্লীগ্রামের আখ্যান এবং সমাজজীবনের অমূল্য স্মৃতিকথা।",
            articleCount = 18,
            iconName = "auto_stories"
        ),
        Category(
            id = "cat-mythology",
            name = "পৌরাণিক কাহিনী",
            slug = "mythology",
            description = "বিষ্ণুপ্রিয়া মণিপুরি লোকবিশ্বাস, পৌরাণিক দেব-দেবী, লাই-হারাওবা এবং কিংবদন্তির আখ্যান।",
            articleCount = 15,
            iconName = "temple_hindu"
        ),
        Category(
            id = "cat-editorial",
            name = "সম্পাদকীয়",
            slug = "editorial",
            description = "নিংশিং চে-র মূল ভাবনা, সাংস্কৃতিক পথপরিক্রমা এবং সমকালীন সমাজ ভাবনার সম্পাদকীয় প্রতিফলন।",
            articleCount = 12,
            iconName = "edit_note"
        ),
        Category(
            id = "cat-reviews",
            name = "পর্যালোচনা",
            slug = "reviews",
            description = "বিষ্ণুপ্রিয়া মণিপুরি ভাষায় প্রকাশিত বই, সাময়িকী ও গবেষণাকর্মের নিরপেক্ষ বিশ্লেষণ ও গ্রন্থালোচনা।",
            articleCount = 16,
            iconName = "rate_review"
        ),
        Category(
            id = "cat-social-activities",
            name = "সামাজিক কার্যকলাপ",
            slug = "social-activities",
            description = "ভাষা, সংস্কৃতি ও সমাজ বিকাশে বিভিন্ন সামাজিক ও সাংস্কৃতিক সংগঠনের নানামুখী উদ্যোগ।",
            articleCount = 19,
            iconName = "volunteer_activism"
        ),
        Category(
            id = "cat-scitech",
            name = "বিজ্ঞান ও প্রযুক্তি",
            slug = "science-technology",
            description = "মাতৃভাষায় বিজ্ঞান চেতনা প্রসার, ডিজিটাল মাধ্যমে ভাষা সংরক্ষণ এবং আধুনিক প্রযুক্তির সংযোগ।",
            articleCount = 11,
            iconName = "biotech"
        ),
        Category(
            id = "cat-preface",
            name = "ভূমিকা",
            slug = "preface",
            description = "তথ্যকোষের উদ্দেশ্য, পরিকল্পনা ও বিষ্ণুপ্রিয়া মণিপুরি সাহিত্যের পটভূমি সংক্রান্ত সামগ্রিক ভূমিকা।",
            articleCount = 8,
            iconName = "info"
        ),
        Category(
            id = "cat-authors",
            name = "লেখক",
            slug = "authors-directory",
            description = "বিষ্ণুপ্রিয়া মণিপুরি ভাষার প্রথিতযশা গবেষক, কবি ও লেখকদের পরিচিতি ও লেখনী।",
            articleCount = 30,
            iconName = "draw"
        )
    )

    val authors: List<Author> = listOf(
        Author(
            id = "auth-1",
            name = "অধ্যাপক বারীন্দ্র কুমার সিংহ",
            designation = "ভাষা ও সংস্কৃতি গবেষক",
            bio = "বিষ্ণুপ্রিয়া মণিপুরি ভাষা আন্দোলন এবং প্রাচীন লোকসংস্কৃতি নিয়ে দীর্ঘদিন ধরে গবেষণা ও লেখালেখি করছেন। নিংশিং চে তথ্যকোষের অন্যতম প্রধান উপদেষ্টা।",
            avatarUrl = "https://images.unsplash.com/photo-1544005313-94ddf0286df2?w=400&q=80",
            articleCount = 14,
            location = "সিলেট, বাংলাদেশ",
            topics = listOf("ইতিহাস", "ভাষা আন্দোলন", "সমাজতত্ত্ব")
        ),
        Author(
            id = "auth-2",
            name = "ড. প্রদীপ সিংহ",
            designation = "নৃতাত্ত্বিক ও লোকসাহিত্য বিশারদ",
            bio = "উত্তর-পূর্ব ভারতের মণিপুরি জনজাতির বাস্তুসংস্থান, ইঞ্চৌঘরের স্থাপত্য ও পৌরাণিক লোকগাথা সংক্রান্ত গবেষণার জন্য সমাদৃত।",
            avatarUrl = "https://images.unsplash.com/photo-1507003211169-0a1dd7228f2d?w=400&q=80",
            articleCount = 11,
            location = "আসাম, ভারত",
            topics = listOf("স্থাপত্য", "লোকগাথা", "সংস্কৃতি")
        ),
        Author(
            id = "auth-3",
            name = "সুজিত সিংহ",
            designation = "সাংস্কৃতিক কর্মী ও প্রাবন্ধিক",
            bio = "বিষ্ণুপ্রিয়া মণিপুরি ঐতিহ্যবাহী লোকসংগীত, রাস উৎসব এবং যুব সমাজের সাংস্কৃতিক বিকাশ নিয়ে নিয়মিত প্রবন্ধ রচনা করেন।",
            avatarUrl = "https://images.unsplash.com/photo-1500648767791-00dcc994a43e?w=400&q=80",
            articleCount = 18,
            location = "মৌলভীবাজার, বাংলাদেশ",
            topics = listOf("লোকসংগীত", "রাসলীলা", "সাহিত্য")
        ),
        Author(
            id = "auth-4",
            name = "স্মৃতিকণা সিংহ",
            designation = "কবি ও জীবনীকার",
            bio = "বিষ্ণুপ্রিয়া মণিপুরি ভাষার নারী লেখক ও শহীদ সুদেষ্ণা সিংহের আত্মত্যাগের ঐতিহাসিক প্রেক্ষাপট নিয়ে তাৎপর্যপূর্ণ গ্রন্থ রচনা করেছেন।",
            avatarUrl = "https://images.unsplash.com/photo-1534528741775-53994a69daeb?w=400&q=80",
            articleCount = 9,
            location = "ত্রিপুরা, ভারত",
            topics = listOf("জীবনী", "কবিতা", "স্মৃতিচারণ")
        ),
        Author(
            id = "auth-5",
            name = "কমল সিংহ বিষ্ণুপ্রিয়া",
            designation = "ডিজিটাল আর্কাইভ ও প্রযুক্তি সংগ্রাহক",
            bio = "ডিজিটাল মাধ্যমে বিষ্ণুপ্রিয়া মণিপুরি ইউনিকোড ফন্ট, তথ্যকোষ এবং অনলাইন লাইব্রেরি তৈরিতে গুরুত্বপূর্ণ অবদান রাখছেন।",
            avatarUrl = "https://images.unsplash.com/photo-1519085360753-af0119f7cbe7?w=400&q=80",
            articleCount = 8,
            location = "ঢাকা, বাংলাদেশ",
            topics = listOf("বিজ্ঞান ও প্রযুক্তি", "ডিজিটাল আর্কাইভ", "শিক্ষা")
        )
    )

    val yearArchives: List<YearArchive> = listOf(
        YearArchive(
            year = 2025,
            bengaliYearText = "২০২৫",
            title = "নিংশিং চে — দ্বাদশ বর্ষ (২০২৫)",
            description = "আধুনিক বিষ্ণুপ্রিয়া মণিপুরি সাহিত্য ও ডিজিটাল তথ্য সংরক্ষণের বিশেষ সংখ্যা।",
            issueCount = 4,
            articleCount = 26
        ),
        YearArchive(
            year = 2024,
            bengaliYearText = "২০২৪",
            title = "নিংশিং চে — একাদশ বর্ষ (২০২৪)",
            description = "বিষ্ণুপ্রিয়া মণিপুরি লোকসংগীত, পৌরাণিক ঐতিহ্য ও সমাজসংস্কৃতি সংখ্যা।",
            issueCount = 4,
            articleCount = 32
        ),
        YearArchive(
            year = 2023,
            bengaliYearText = "২০২৩",
            title = "নিংশিং চে — দশম বর্ষ (২০২৩)",
            description = "এক দশকের সাহিত্যিক অভিযাত্রা এবং ঐতিহাসিক গবেষণা সংকলন।",
            issueCount = 4,
            articleCount = 30
        ),
        YearArchive(
            year = 2022,
            bengaliYearText = "২০২২",
            title = "নিংশিং চে — নবম বর্ষ (২০২২)",
            description = "ভাষা আন্দোলনের প্রেক্ষাপট ও প্রাচীন লোকনৃত্যের সমাজতাত্ত্বিক মূল্যায়ন।",
            issueCount = 3,
            articleCount = 24
        ),
        YearArchive(
            year = 2021,
            bengaliYearText = "২০২১",
            title = "নিংশিং চে — অষ্টম বর্ষ (২০২১)",
            description = "করোনাকালের স্মৃতিচারণ, লোকঔষধ ও মণিপুরি লোকগাথার প্রামাণ্য রূপ।",
            issueCount = 3,
            articleCount = 21
        ),
        YearArchive(
            year = 2020,
            bengaliYearText = "২০২০",
            title = "নিংশিং চে — সপ্তম বর্ষ (২০২০)",
            description = "বিষ্ণুপ্রিয়া মণিপুরি কবি ও প্রাবন্ধিকদের নির্বাচিত শ্রেষ্ঠ রচনার সংকলন।",
            issueCount = 4,
            articleCount = 28
        ),
        YearArchive(
            year = 2019,
            bengaliYearText = "২০১৯",
            title = "নিংশিং চে — ষষ্ঠ বর্ষ (২০১৯)",
            description = "মণিপুরের লোকতাক হ্রদের প্রাচীন ইতিহাস এবং ঐতিহ্যবাহী গৃহসংস্থান।",
            issueCount = 3,
            articleCount = 20
        ),
        YearArchive(
            year = 2018,
            bengaliYearText = "২০১৮",
            title = "নিংশিং চে — পঞ্চম বর্ষ (২০১৮)",
            description = "ভাষা শহীদ সুদেষ্ণা সিংহ স্মারক সংখ্যা ও সমাজ জাগরণের চালচিত্র।",
            issueCount = 4,
            articleCount = 27
        ),
        YearArchive(
            year = 2016,
            bengaliYearText = "২০১৬",
            title = "নিংশিং চে — ৩য় সংখ্যা (২০১৬)",
            description = "ঐতিহাসিক ৩য় মুদ্রণ সংখ্যা, প্রামাণ্য দলিল ও সমাজ সংস্কারের ইতিবৃত্ত।",
            issueCount = 2,
            articleCount = 18
        ),
        YearArchive(
            year = 2014,
            bengaliYearText = "২০১৪",
            title = "নিংশিং চে — প্রারম্ভিক সংখ্যা (২০১৪)",
            description = "নিংশিং চে-র প্রথম পথচলা, তথ্যকোষের মূল রূপরেখা ও প্রতিষ্ঠাতা প্রবন্ধমালা।",
            issueCount = 2,
            articleCount = 15
        )
    )

    val articles: List<Article> = listOf(
        Article(
            id = "art-1",
            title = "বিষ্ণুপ্রিয়া মণিপুরি ভাষা আন্দোলনের সংক্ষিপ্ত ইতিহাস ও তাৎপর্য",
            slug = "bishnupriya-manipuri-language-movement-history",
            excerpt = "১৯৫৫ সালের ভাষা আন্দোলনের ঐতিহাসিক পটভূমি, মাতৃভাষার অধিকার রক্ষা এবং শহীদ সুদেষ্ণা সিংহের অসামান্য আত্মত্যাগের প্রামাণ্য ইতিহাস।",
            content = """
                বিষ্ণুপ্রিয়া মণিপুরি ভাষা আন্দোলন ছিল নিজস্ব মাতৃভাষা ও অস্তিত্ব রক্ষার এক অবিস্মরণীয় ঐতিহাসিক সংগ্রাম। ভারতীয় উপমহাদেশের ভাষা আন্দোলনসমূহের মধ্যে এই আন্দোলনের এক অনন্য ও গৌরবোজ্জ্বল স্থান রয়েছে।
                
                ১. আন্দোলনের পটভূমি ও ১৯৫৫ সালের সূচনা:
                বিষ্ণুপ্রিয়া মণিপুরি জনগোষ্ঠী মূলত ভারতের আসাম, ত্রিপুরা, মণিপুর এবং বাংলাদেশের সিলেট অঞ্চলে বসবাসকারী এক প্রাচীন নৃতাত্ত্বিক জনগোষ্ঠী। বিংশ শতাব্দীর মাঝামাঝি সময়ে যখন সরকারি স্তরে মাতৃভাষার স্বীকৃতি ও শিক্ষাদানের প্রশ্নটি সামনে আসে, তখন মণিপুরিদের মধ্যে স্বাধিকার চেতনার এক নবজাগরণ সৃষ্টি হয়। ১৯৫৫ সালের দিকে এই আন্দোলন সংগঠিত রূপ লাভ করে।
                
                ২. শহীদ সুদেষ্ণা সিংহের আত্মত্যাগ:
                ১৯৯৬ সালের ১৬ই মার্চ আসামের বরাক উপত্যকার পাথারকান্দির কলকলিঘাটে ভাষা আন্দোলনের মিছিলে পুলিশ গুলি চালালে শহীদ হন বীরাঙ্গনা সুদেষ্ণা সিংহ। তিনিই ছিলেন বিষ্ণুপ্রিয়া মণিপুরি ভাষা আন্দোলনের প্রথম নারী শহীদ। তাঁর এই অমর আত্মত্যাগ ভাষা আন্দোলনকে এক নতুন গতিশক্তি ও মর্যাদায় অভিষিক্ত করে। প্রতি বছর ১৬ই মার্চ দিনটি অত্যন্ত ভাবগম্ভীর পরিবেশে 'বিষ্ণুপ্রিয়া মণিপুরি ভাষা শহীদ দিবস' হিসেবে পালন করা হয়।
                
                ৩. শিক্ষা ও সাহিত্যের বিকাশ:
                আন্দোলনের ফসল হিসেবে প্রাথমিক বিদ্যালয়ে বিষ্ণুপ্রিয়া মণিপুরি ভাষায় পাঠদান শুরু হয় এবং গুয়াহাটি ও আসাম বিশ্ববিদ্যালয়ে ভাষা ও সাহিত্যের পাঠ্যক্রম চালু হয়। এই আন্দোলন শুধু ভাষার স্বীকৃতির জন্য ছিল না, এটি ছিল বিষ্ণুপ্রিয়া মণিপুরি সমাজের আত্মমর্যাদা, সংস্কৃতি এবং ভবিষ্যতের অস্তিত্ব রক্ষার লড়াই।
                
                ৪. নিংশিং চে তথ্যকোষের দায়িত্ব:
                আজকের প্রজন্মের কাছে এই গৌরবোজ্জ্বল ইতিহাস যথাযথভাবে পৌঁছে দিতে নিংশিং চে ডিজিটাল মাধ্যমে এই ইতিহাসকে নথিবদ্ধ করছে। প্রতিটি নতুন প্রজন্মের কাছে এই সত্য তুলে ধরা আমাদের পরম দায়িত্ব।
            """.trimIndent(),
            featuredImageUrl = "https://images.unsplash.com/photo-1455390582262-044cdead277a?w=800&q=80",
            authorId = "auth-1",
            authorName = "অধ্যাপক বারীন্দ্র কুমার সিংহ",
            category = "ইতিহাস",
            categorySlug = "history",
            tags = listOf("ভাষা আন্দোলন", "শহীদ সুদেষ্ণা সিংহ", "ইতিহাস", "১৬ই মার্চ"),
            publishedDate = "১৫ জানুয়ারি ২০২৫",
            year = 2025,
            readingTimeMinutes = 6,
            isFeatured = true,
            isEditorialPick = true,
            viewCount = 1420,
            sourceUrl = "https://ningshingche.com/article/language-movement",
            relatedArticleIds = listOf("art-2", "art-4", "art-7")
        ),
        Article(
            id = "art-2",
            title = "বিষ্ণুপ্রিয়া মণিপুরি সমাজের ঐতিহ্যবাহী লোকসংগীত ও রাগধারা",
            slug = "traditional-folk-music-and-ragas-of-bishnupriya-manipuri",
            excerpt = "অনন্য রাগ, পাখোয়াজ ও করতাল সংবলিত মণিপুরি লোকসংগীতের গভীরতা, বৈষ্ণব পদাবলী এবং বাউলধারার সমন্বিত রূপ।",
            content = """
                বিষ্ণুপ্রিয়া মণিপুরি সংস্কৃতির প্রাণশক্তি নিহিত রয়েছে এর লোকসংগীত ও ঐতিহ্যবাহী রাগধারার মধ্যে। ধর্মীয় আনুষ্ঠানিকতা থেকে শুরু করে দৈনন্দিন জীবনের সুখ-দুঃখ প্রতিটি অনুভূতির সাথে সুর ও ছন্দ ওতপ্রোতভাবে জড়িয়ে আছে।
                
                ১. সংগীতের মূল বৈশিষ্ট্য ও বাদ্যযন্ত্র:
                মণিপুরি লোকসংগীতে মৃদঙ্গ, পাখোয়াজ, করতাল, শঙ্খ এবং বাঁশির ব্যবহার অত্যন্ত নিপুণ। এর সংগীতধারা প্রধানত বৈষ্ণব ভাবধারায় জারিত। এতে জয়দেব, চণ্ডীদাস ও বিদ্যাপতির পদাবলীর পাশাপাশি স্থানীয় ভাবসাধকদের অপূর্ব সৃষ্টিকর্ম স্থান পেয়েছে।
                
                ২. গীতস্বামী ও পদকর্তাদের অবদান:
                গোকুলানন্দ গীতিস্বামীর মতো মহামানবদের আবির্ভাব বিষ্ণুপ্রিয়া মণিপুরি সংগীতে এক নতুন দিগন্ত উন্মোচন করেছিল। তাঁর রচিত সুর ও পদ আজও গ্রামবাংলার প্রতি ঘরে ঘরে অত্যন্ত শ্রদ্ধার সাথে গাওয়া হয়।
                
                ৩. হোলি ও বিষু উৎসবের সংগীত:
                হোলি বা দোল পূর্ণিমার সময় পরিবেশিত 'থাবল চোংবা' ও হোলি গানগুলোতে এক অনুপম সমবেত সুর অনুরণিত হয়। নারী-পুরুষ নির্বিশেষে গোল হয়ে নাচের সাথে সাথে যে আনন্দময় সুর গাওয়া হয়, তা সামাজিক সম্প্রীতির এক অনন্য উদাহরণ।
                
                ৪. সংরক্ষণের প্রয়োজনীয়তা:
                আধুনিক প্রযুক্তির প্রভাবে অনেক প্রাচীন সুর হারিয়ে যেতে বসেছে। নিংশিং চে ডিজিটাল অডিও আর্কাইভের মাধ্যমে এইসব বিরল সংগীতের প্রামাণ্য নথিকরণ চালিয়ে যাচ্ছে।
            """.trimIndent(),
            featuredImageUrl = "https://images.unsplash.com/photo-1511671782779-c97d3d27a1d4?w=800&q=80",
            authorId = "auth-3",
            authorName = "সুজিত সিংহ",
            category = "সমাজ ও সংস্কৃতি",
            categorySlug = "society-culture",
            tags = listOf("লোকসংগীত", "গোকুলানন্দ গীতিস্বামী", "বৈষ্ণব পদাবলী", "থাবল চোংবা"),
            publishedDate = "১০ ফেব্রুয়ারি ২০২৫",
            year = 2025,
            readingTimeMinutes = 5,
            isFeatured = true,
            isEditorialPick = false,
            viewCount = 980,
            sourceUrl = "https://ningshingche.com/article/folk-music",
            relatedArticleIds = listOf("art-1", "art-3", "art-5")
        ),
        Article(
            id = "art-3",
            title = "বিষ্ণুপ্রিয়া মণিপুরিদের ঐতিহ্যবাহী বসতবাড়ির স্থাপত্য: 'ইঞ্চৌঘর'",
            slug = "traditional-housing-architecture-inchoughor",
            excerpt = "প্রকৃতির সাথে নিবিড় সম্পর্কের পরিচায়ক 'ইঞ্চৌঘর' ও মাটির তৈরি ভিটের বাস্তুসংস্থান ও নির্মাণ শৈলীর নান্দনিক পর্যালোচনা।",
            content = """
                বাস্তু ও স্থাপত্যকলার দিক থেকে বিষ্ণুপ্রিয়া মণিপুরি সমাজের লোক-স্থাপত্য অত্যন্ত সমৃদ্ধ ও পরিবেশবান্ধব। এর অন্যতম প্রধান নিদর্শন হলো ঐতিহ্যবাহী 'ইঞ্চৌঘর'।
                
                ১. ইঞ্চৌঘরের গঠন ও কাঠামো:
                'ইঞ্চৌঘর' সাধারণত চারচালা বা দোচালা খড়ের ও বাঁশের কাঠামোর ওপর বিশেষ উপায়ে নির্মিত হয়। এর মেঝে তৈরি করা হয় অত্যন্ত নিপুণভাবে লেপা মাটির আস্তরণ দিয়ে, যা গ্রীষ্মকালে শীতল এবং শীতকালে আরামদায়ক উষ্ণতা প্রদান করে।
                
                ২. পারিবারিক ও সামাজিক বিন্যাস:
                ইঞ্চৌঘরের সম্মুখভাগে থাকে প্রশস্ত বারান্দা বা 'মণ্ডপ', যা শুধু অতিথিদের অভ্যর্থনা নয়, বরং সামাজিক বিচার-সালিশ, ধর্মীয় কীর্তন এবং শিশুদের শিক্ষাদানের কেন্দ্র হিসেবে ব্যবহৃত হতো। ঘরের ভেতরে ঠাকুরঘর বা পূজার স্থানটি নির্দিষ্ট ও পবিত্র স্থানে স্থাপন করা হয়।
                
                ৩. বাতাস ও আলোর প্রাকৃতিক চলাচল:
                বাস্তুশাস্ত্রের প্রাচীন রীতিনীতি মেনে তৈরি এই ঘরগুলোতে পর্যাপ্ত বাতাস ও সূর্যালোক প্রবেশের সুব্যবস্থা থাকে। বাঁশ, বেত ও খড়ের এই কারুকাজ পরিবেশের কোনো ক্ষতি না করেই দীর্ঘস্থায়ী স্থায়িত্ব দেয়।
                
                ৪. আধুনিকতার ছোঁয়ায় রূপান্তর:
                আজকাল কংক্রিটের দালানের যুগে ইঞ্চৌঘরের সংখ্যা ক্রমশ কমে যাচ্ছে। তবে এর স্থাপত্যিক দর্শন আজও তরুণ স্থপতিদের কাছে পরিবেশবান্ধব নির্মাণের এক অন্যতম মডেল হিসেবে বিবেচিত হয়।
            """.trimIndent(),
            featuredImageUrl = "https://images.unsplash.com/photo-1513694203232-719a280e022f?w=800&q=80",
            authorId = "auth-2",
            authorName = "ড. প্রদীপ সিংহ",
            category = "সমাজ ও সংস্কৃতি",
            categorySlug = "society-culture",
            tags = listOf("ইঞ্চৌঘর", "স্থাপত্য", "বাস্তুসংস্থান", "ঐতিহ্য"),
            publishedDate = "০৫ জানুয়ারি ২০২৫",
            year = 2025,
            readingTimeMinutes = 7,
            isFeatured = false,
            isEditorialPick = true,
            viewCount = 1120,
            sourceUrl = "https://ningshingche.com/article/inchoughor-architecture",
            relatedArticleIds = listOf("art-2", "art-6", "art-8")
        ),
        Article(
            id = "art-4",
            title = "মণিপুরি সমাজে 'মিংকৌ' বা নামপ্রথা: সাংস্কৃতিক তাৎপর্য ও ঐতিহ্য",
            slug = "mingkou-naming-tradition-in-manipuri-society",
            excerpt = "বিষ্ণুপ্রিয়া মণিপুরি গোত্র, বংশনাম এবং আত্মপরিচয়ের প্রতীক 'মিংকৌ' ব্যবস্থার উৎপত্তি ও আধুনিক সমাজের প্রাসঙ্গিকতা।",
            content = """
                প্রতিটি প্রাচীন জাতির নিজস্ব নামপ্রথা বা পরিচিতি কাঠামো থাকে। বিষ্ণুপ্রিয়া মণিপুরি সমাজে 'মিংকৌ' তেমনই এক সুপ্রাচীন ও অত্যন্ত তাৎপর্যপূর্ণ নামপদ্ধতি।
                
                ১. মিংকৌ শব্দের অর্থ ও গোত্রীয় সম্পর্ক:
                'মিংকৌ' কথাটি মূলত বংশপদবী, গোত্র (সালই) এবং পারিবার পরিচয়ের এক সুনির্দিষ্ট কাঠামো। প্রাচীন মণিপুরে বিভিন্ন বৃত্তিমূলক দায়িত্ব, ভৌগোলিক অবস্থান এবং সামাজিক অবদানের ওপর ভিত্তি করে এই মিংকৌ নির্ধারিত হতো।
                
                ২. সামাজিক শ্রেণিবিন্যাস ও বৈবাহিক নিয়ম:
                মণিপুরি সমাজ ব্যবস্থায় স্বগোত্রে বিবাহ কঠোরভাবে নিয়ন্ত্রিত। ফলে মিংকৌ ব্যবস্থা মানুষের সঠিক বংশলতিকা এবং রক্তীয় সম্পর্ক শনাক্ত করতে অপরিসীম ভূমিকা পালন করে। একই সালাই বা গোত্রের অন্তর্ভুক্ত পরিবারের মধ্যে পারস্পরিক শ্রদ্ধাবোধ বজায় থাকে।
                
                ৩. বর্তমান সময়ে সংরক্ষণ:
                নগরায়ণ ও বিশ্বায়নের যুগে অনেকেই কেবল সাধারণ পদবী ব্যবহারে অভ্যস্ত হয়ে পড়ছেন, যার ফলে বহু ঐতিহাসিক মিংকৌ বিস্মৃতির অতলে তলিয়ে যাচ্ছে। এই প্রবন্ধের মাধ্যমে বিভিন্ন অঞ্চলের ঐতিহাসিক মিংকৌসমূহ সংকলিত করার একটি প্রয়াস নেওয়া হয়েছে।
            """.trimIndent(),
            featuredImageUrl = "https://images.unsplash.com/photo-1457369804613-52c61a468e7d?w=800&q=80",
            authorId = "auth-1",
            authorName = "অধ্যাপক বারীন্দ্র কুমার সিংহ",
            category = "সমাজ ও সংস্কৃতি",
            categorySlug = "society-culture",
            tags = listOf("মিংকৌ", "গোত্র", "বংশনাম", "সামাজিক নিয়ম"),
            publishedDate = "২৮ ডিসেম্বর ২০২৪",
            year = 2024,
            readingTimeMinutes = 5,
            isFeatured = false,
            isEditorialPick = false,
            viewCount = 840,
            sourceUrl = "https://ningshingche.com/article/mingkou-tradition",
            relatedArticleIds = listOf("art-1", "art-3", "art-9")
        ),
        Article(
            id = "art-5",
            title = "বিষ্ণুপ্রিয়া মণিপুরি সাহিত্যের আদিযুগ থেকে আধুনিক পর্ব",
            slug = "history-of-bishnupriya-manipuri-literature",
            excerpt = "প্রাচীন লোকগাথা, বৈষ্ণব কাব্যানুবাদ, সেনাপতি রাজকুমার ও আধুনিক কবিদের লেখনীতে সমৃদ্ধ সাহিত্যধারার পরিক্রমা।",
            content = """
                বিষ্ণুপ্রিয়া মণিপুরি সাহিত্য নিজস্ব মাধুর্য, দার্শনিক গভীরতা ও সমৃদ্ধ রূপকল্পে বাংলা ও অসমীয়া সাহিত্যের পাশাপাশি এক স্বকীয় দীপ্তিতে ভাস্বর।
                
                ১. আদি ও মধ্যযুগ:
                সাহিত্যচর্চার আদি রূপটি পাওয়া যায় লোকসংগীত, ডাকের বচন, খণ্ডকাব্য এবং পৌরাণিক গাথায়। মধ্যযুগে বৈষ্ণব ধর্মের প্রভাবে ভাগবত, মহাভারত ও রামায়ণের বহু পদাবলী মণিপুরি ভাষায় রচিত ও অনুবাদিত হয়।
                
                ২. রেনেসাঁ ও আধুনিক যুগের সূচনা:
                বিংশ শতাব্দীর শুরুর দিকে কবি সেনাপতি রাজকুমার, ব্রজেন্দ্র কুমার সিংহ এবং গোকুলানন্দ গীতিস্বামীর লেখনীর মাধ্যমে আধুনিক সাহিত্যের ভিত্তি স্থাপিত হয়। পত্র-পত্রিকা প্রকাশ, ছোটগল্প, নাটক ও মুক্তছন্দের কবিতার বিস্তার ঘটতে থাকে।
                
                ৩. নিংশিং চে-র অবদান:
                আজকের দিনে তরুণ প্রজন্মের সাহিত্যিকরা ইন্টারনেট ও ডিজিটাল মাধ্যমে মণিপুরি কবিতা ও গল্পকে বিশ্বমঞ্চে ছড়িয়ে দিচ্ছেন। নিংশিং চে সাহিত্যের এই ধারাকে বেগবান রাখতে এক সার্বক্ষণিক ডিজিটাল প্ল্যাটফর্মের ভূমিকা পালন করছে।
            """.trimIndent(),
            featuredImageUrl = "https://images.unsplash.com/photo-1474932430478-367dbb6832c1?w=800&q=80",
            authorId = "auth-4",
            authorName = "স্মৃতিকণা সিংহ",
            category = "সাহিত্য",
            categorySlug = "literature",
            tags = listOf("সাহিত্য", "কবিতা", "সেনাপতি রাজকুমার", "আধুনিক সাহিত্য"),
            publishedDate = "২০ ডিসেম্বর ২০২৪",
            year = 2024,
            readingTimeMinutes = 8,
            isFeatured = true,
            isEditorialPick = true,
            viewCount = 1350,
            sourceUrl = "https://ningshingche.com/article/literature-history",
            relatedArticleIds = listOf("art-2", "art-7", "art-10")
        ),
        Article(
            id = "art-6",
            title = "বিশু উৎসব: মণিপুরি নববর্ষের প্রকৃতি ও আত্মশুদ্ধির আনন্দধারা",
            slug = "bishu-festival-manipuri-new-year-nature-celebration",
            excerpt = "চৈত্র সংক্রান্তি ও বৈশাখের মিলনে উদযাপিত বিষ্ণুপ্রিয়া মণিপুরিদের ঐতিহ্যবাহী 'বিশু' উৎসবের পুণ্য আচার ও সামাজিক মিলনমেলা।",
            content = """
                প্রকৃতি ও মানবজীবনের মেলবন্ধনে যে কয়টি লোক-উৎসব বিষ্ণুপ্রিয়া মণিপুরি সমাজে অত্যন্ত গভীর আবেগ নিয়ে পালিত হয়, তার মধ্যে অন্যতম প্রধান হলো 'বিশু'।
                
                ১. বিশুর তিনটি পর্ব:
                বিশু সাধারণত তিন দিনব্যাপী পালিত হয়— 'কাঞ্জি বিশু' (শাকসবজি ও প্রকৃতিবরণ), 'বাজে বিশু' (মূল আনন্দোৎসব ও প্রবীণদের প্রণাম) এবং 'রং বিশু' (পরবর্তী দিনের সামাজিক শুভেচ্ছা বিনিময়)।
                
                ২. ঐতিহ্যবাহী খাদ্যাভ্যাস ও 'হেইরূই':
                বিশুর দিনে ১০৮ পদের শাক বা বিভিন্ন প্রকার ঔষধি লতাপাতা সহযোগে প্রস্তুত বিশেষ ব্যঞ্জন গ্রহণ করা হয়। এটি কেবল খাদ্য নয়, বরং ঋতু পরিবর্তনের সময় শারীরিক রোগ প্রতিরোধ ক্ষমতা বৃদ্ধির এক প্রাচীন আয়ুর্বেদিক সংস্কৃতি।
                
                ৩. প্রবীণদের আশীর্বাদ গ্রহণ ও শুভেচ্ছা বিনিময়:
                পরিবারের অনুজরা গুরুজনদের চরণে প্রণাম করে নতুন বস্ত্র উপহার দেন এবং আগামী বছরের মঙ্গল কামনা করে আশীর্বাদ গ্রহণ করেন।
            """.trimIndent(),
            featuredImageUrl = "https://images.unsplash.com/photo-1509198397868-475647b2a1e5?w=800&q=80",
            authorId = "auth-3",
            authorName = "সুজিত সিংহ",
            category = "সমাজ ও সংস্কৃতি",
            categorySlug = "society-culture",
            tags = listOf("বিশু", "নববর্ষ", "লোকউৎসব", "সংস্কৃতি"),
            publishedDate = "১২ এপ্রিল ২০২৪",
            year = 2024,
            readingTimeMinutes = 5,
            isFeatured = false,
            isEditorialPick = false,
            viewCount = 760,
            sourceUrl = "https://ningshingche.com/article/bishu-festival",
            relatedArticleIds = listOf("art-2", "art-3", "art-4")
        ),
        Article(
            id = "art-7",
            title = "ভাষা শহীদ সুদেষ্ণা সিংহ: অবিনাশী আত্মত্যাগ ও প্রেরণার শিখা",
            slug = "language-martyr-sudeshna-sinha-biography",
            excerpt = "বিষ্ণুপ্রিয়া মণিপুরি মাতৃভাষা আন্দোলনে প্রথম নারী শহীদ সুদেষ্ণা সিংহের সংগ্রামী জীবন, আত্মোৎসর্গ ও অমর স্মৃতিগাঁথা।",
            content = """
                ১৯৯৬ সালের ১৬ই মার্চ দিনটি বিষ্ণুপ্রিয়া মণিপুরি জাতির ইতিহাসে এক রক্তিম অক্ষরে লিখিত অধ্যায়। সেই রক্তঝরা দিনে বরাক উপত্যকার মাটিতে আত্মোৎসর্গ করেছিলেন মহীয়সী বীরাঙ্গনা সুদেষ্ণা সিংহ।
                
                ১. শৈশব ও দেশপ্রেমের দীক্ষা:
                সুদেষ্ণা সিংহ অত্যন্ত সাধারণ অথচ সংস্কৃতিবান পরিবারে বেড়ে ওঠেন। ছোটবেলা থেকেই তিনি ছিলেন স্পষ্টভাষী, পরোপকারী এবং মাতৃভাষার প্রতি নিবেদিতপ্রাণ।
                
                ২. ১৬ই মার্চের সেই উত্তাল প্রহর:
                পাথারকান্দির রেল অবরোধ ও শান্তিপূর্ণ সমাবেশে অংশ নিতে তিনি সহযোদ্ধাদের সাথে যোগ দেন। যখন পুলিশের নির্মম লাঠিচার্জ ও গুলিবর্ষণ শুরু হয়, তিনি নির্ভীকচিত্তে অন্যায়ের প্রতিবাদ জানিয়ে মৃত্যুকে বরণ করে নেন।
                
                ৩. উত্তরসূরিদের প্রেরণা:
                সুদেষ্ণা সিংহের আত্মত্যাগ কেবল একটি সম্প্রদায়ের ভাষার অধিকার নিশ্চিত করেনি, বরং সমগ্র বিশ্বের সংখ্যালঘু মাতৃভাষা রক্ষার আন্দোলনে নারীদের নির্ভীক অংশগ্রহণের এক মহান প্রতীক হয়ে উঠেছে।
            """.trimIndent(),
            featuredImageUrl = "https://images.unsplash.com/photo-1499209974431-9dddcece7f88?w=800&q=80",
            authorId = "auth-4",
            authorName = "স্মৃতিকণা সিংহ",
            category = "জীবনী",
            categorySlug = "biography",
            tags = listOf("শহীদ সুদেষ্ণা সিংহ", "জীবনী", "ভাষা শহীদ", "১৬ই মার্চ"),
            publishedDate = "১৬ মার্চ ২০২৪",
            year = 2024,
            readingTimeMinutes = 6,
            isFeatured = true,
            isEditorialPick = true,
            viewCount = 1890,
            sourceUrl = "https://ningshingche.com/article/sudeshna-sinha",
            relatedArticleIds = listOf("art-1", "art-5", "art-8")
        ),
        Article(
            id = "art-8",
            title = "পৌরাণিক লোকগাথা: মণিপুরের লোকতাক হ্রদ ও সৃষ্টির আখ্যান",
            slug = "mythology-loktak-lake-and-creation-myths",
            excerpt = "বিশ্বের একমাত্র ভাসমান হ্রদ লোকতাককে ঘিরে গড়ে ওঠা বিষ্ণুপ্রিয়া মণিপুরি লোকবিশ্বাস, নাগরাজ এবং প্রকৃতি বন্দনা।",
            content = """
                লোকতাক হ্রদ কেবল একটি প্রাকৃতিক জলাশয় নয়, এটি বিষ্ণুপ্রিয়া মণিপুরি এবং মেতেই লোকসংস্কৃতির এক পৌরাণিক স্মারক। লোকতাকের প্রতিটি জলতরঙ্গে জড়িয়ে আছে প্রাচীন পূর্বপুরুষদের স্মৃতিকথা।
                
                ১. ভাসমান ফুংডি ও লোকবিশ্বাস:
                লোকতাক হ্রদের বুকে প্রাকৃতিকভাবে ভেসে থাকা গাছপালার দ্বীপগুলোকে স্থানীয় ভাষায় 'ফুংডি' বলা হয়। পৌরাণিক বিশ্বাস অনুসারে, সৃষ্টির আদিমুহূর্তে দেবতারা এই হ্রদের তীরেই প্রথম চরণ রেখেছিলেন।
                
                ২. পুঙৈবা ও ময়রাং কাংলেইপাকের গাথা:
                প্রাচীন মণিপুরের সাতটি বংশ এবং তাদের মধ্যকার প্রেম-বিরহ ও বীরত্বগাথার কাহিনী লোকতাক হ্রদের পটভূমিতে রচিত হয়েছে। বিশেষ করে খাম্বা-থোইবীর মহাকাব্যিক প্রেম কাহিনী এই অঞ্চলের ঘরে ঘরে প্রচলিত।
                
                ৩. সাংস্কৃতিক মিলনক্ষেত্র:
                হ্রদের রূপ যেমন ঋতুভেদে বদলায়, তেমনি মণিপুরিদের সংগীতে ও কাব্যে লোকতাক হ্রদ এসেছে অনন্ত শান্তির প্রতীক হিসেবে।
            """.trimIndent(),
            featuredImageUrl = "https://images.unsplash.com/photo-1507525428034-b723cf961d3e?w=800&q=80",
            authorId = "auth-2",
            authorName = "ড. প্রদীপ সিংহ",
            category = "পৌরাণিক কাহিনী",
            categorySlug = "mythology",
            tags = listOf("লোকতাক হ্রদ", "পৌরাণিক কাহিনী", "লোকগাথা", "মণিপুর"),
            publishedDate = "০৮ আগস্ট ২০২৪",
            year = 2024,
            readingTimeMinutes = 6,
            isFeatured = false,
            isEditorialPick = false,
            viewCount = 670,
            sourceUrl = "https://ningshingche.com/article/loktak-mythology",
            relatedArticleIds = listOf("art-3", "art-5", "art-9")
        ),
        Article(
            id = "art-9",
            title = "ডিজিটাল যুগে ভাষা সংরক্ষণ: ইউনিকোড ও ওপেন নলেজ আর্কাইভ",
            slug = "digital-language-preservation-unicode-and-open-knowledge",
            excerpt = "মাতৃভাষাকে ইন্টারনেটের আঙিনায় সমৃদ্ধ করতে ডিজিটাল লিপিকরণ, তথ্যকোষ নির্মাণ ও ভবিষ্যৎ প্রযুক্তির মেলবন্ধন।",
            content = """
                একবিংশ শতাব্দীর প্রযুক্তি বিপ্লবে কোনো ভাষা যদি ইন্টারনেটে ও ডিজিটাল মাধ্যমে সংরক্ষিত না হয়, তবে সেই ভাষা কালের গর্ভে হারিয়ে যাওয়ার ঝুঁকিতে পড়ে।
                
                ১. ইউনিকোড ফন্ট ও টাইপিং সুবিধার রূপরেখা:
                বিষ্ণুপ্রিয়া মণিপুরি ভাষা বাংলা-অসমীয়া লিপিতে লেখা হলেও এর ধ্বনিতাত্ত্বিক কিছু বিশেষ বৈশিষ্ট্য রয়েছে। ইউনিকোড সমন্বয় এবং মোবাইল কিবোর্ড লেআউট তৈরি করার ফলে আজ তরুণ প্রজন্ম অনায়াসে মাতৃভাষায় বার্তা আদান-প্রদান করতে পারছে।
                
                ২. নিংশিং চে তথ্যকোষের প্রযুক্তিগত অভিযাত্রা:
                'নিংশিং চে' ওয়েবসাইট ও অ্যাপের মাধ্যমে ক্লাউড ও অ্যান্ড্রয়েড প্ল্যাটফর্মে প্রাচীন পাণ্ডুলিপি, প্রবন্ধ এবং অভিধানকে উন্মুক্ত জ্ঞানভাণ্ডারে পরিণত করা হচ্ছে।
                
                ৩. কৃত্রিম বুদ্ধিমত্তা ও ভবিষ্যৎ:
                এআই অ্যাসিস্ট্যান্ট এবং লার্জ ল্যাঙ্গুয়েজ মডেলের সহায়তায় বিষ্ণুপ্রিয়া মণিপুরি ভাষার ইতিহাসকে আরও সহজবোধ্য ও অনুসন্ধানযোগ্য করে তোলা সম্ভব হয়েছে।
            """.trimIndent(),
            featuredImageUrl = "https://images.unsplash.com/photo-1526374965328-7f61d4dc18c5?w=800&q=80",
            authorId = "auth-5",
            authorName = "কমল সিংহ বিষ্ণুপ্রিয়া",
            category = "বিজ্ঞান ও প্রযুক্তি",
            categorySlug = "science-technology",
            tags = listOf("বিজ্ঞান ও প্রযুক্তি", "ডিজিটাল আর্কাইভ", "ইউনিকোড", "কৃত্রিম বুদ্ধিমত্তা"),
            publishedDate = "০২ জানুয়ারি ২০২৫",
            year = 2025,
            readingTimeMinutes = 5,
            isFeatured = false,
            isEditorialPick = true,
            viewCount = 1040,
            sourceUrl = "https://ningshingche.com/article/digital-preservation",
            relatedArticleIds = listOf("art-1", "art-5", "art-10")
        ),
        Article(
            id = "art-10",
            title = "সম্পাদকীয়: নিংশিং চে-র দশম বর্ষপূর্তি ও আমাদের আগামী",
            slug = "editorial-tenth-anniversary-of-ningshing-che",
            excerpt = "একটি সাংস্কৃতিক আন্দোলনের এক দশকের পথপরিক্রমা, প্রাপ্তি ও আগামী দিনের ডিজিটাল রূপকল্প।",
            content = """
                দশটি বছর পূর্বে যখন 'নিংশিং চে' তার প্রথম সংখ্যা প্রকাশ করেছিল, তখন উদ্দেশ্য ছিল একটিই— আমাদের ইতিহাস ও সাহিত্য যেন কোনোভাবেই বিস্মৃতির অন্তরালে হারিয়ে না যায়।
                
                ১. স্মৃতি ও দায়বদ্ধতার মেলবন্ধন:
                'নিংশিং চে' শব্দের অর্থই হলো 'স্মৃতির পাতা'। আমরা প্রতিটি পৃষ্ঠায় ধরে রাখতে চেয়েছি আমাদের মাটির সুর, পল্লীর শান্ত ছায়া এবং সংগ্রামী মানুষের মুখের ভাষা।
                
                ২. লেখক ও পাঠকদের অবদান:
                গত এক দশকে ভারত, বাংলাদেশ এবং বিশ্বজুড়ে ছড়িয়ে থাকা মণিপুরি গবেষক ও সুধীসমাজ যেভাবে আমাদের সাথে যুক্ত হয়েছেন, তা আমাদের কৃতজ্ঞতার বাঁধনে বেঁধেছে।
                
                ৩. আগামীর প্রতিশ্রুতি:
                ওয়েবসাইটের পাশাপাশি আজকের এই অফিশিয়াল অ্যান্ড্রয়েড অ্যাপের প্রকাশ আমাদের প্রতিশ্রুতিরই আরেকটি উজ্জ্বল মাইলফলক। আসুন, আমরা সকলে মিলে আমাদের ঐতিহ্যকে বিশ্বদরবারে গৌরবের সাথে তুলে ধরি।
            """.trimIndent(),
            featuredImageUrl = "https://images.unsplash.com/photo-1497633762265-9d179a990aa6?w=800&q=80",
            authorId = "auth-1",
            authorName = "অধ্যাপক বারীন্দ্র কুমার সিংহ",
            category = "সম্পাদকীয়",
            categorySlug = "editorial",
            tags = listOf("সম্পাদকীয়", "নিংশিং চে", "বর্ষপূর্তি", "সাংস্কৃতিক অভিযাত্রা"),
            publishedDate = "০১ জানুয়ারি ২০২৫",
            year = 2025,
            readingTimeMinutes = 4,
            isFeatured = false,
            isEditorialPick = false,
            viewCount = 920,
            sourceUrl = "https://ningshingche.com/article/tenth-anniversary-editorial",
            relatedArticleIds = listOf("art-1", "art-5", "art-9")
        )
    )
}
