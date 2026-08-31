package com.example.data.ai

import com.example.BuildConfig
import com.example.data.model.AiChatMessage
import com.example.data.model.Article
import com.example.data.model.ArticleCitation
import com.example.data.model.Author
import com.example.data.model.Category
import com.example.data.model.PdfDocument
import com.example.data.portal.GalleryItem
import com.example.data.portal.PortalRepository
import com.example.data.portal.VideoItem
import com.example.data.repository.ArticleRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID
import java.util.concurrent.TimeUnit

/**
 * The Ningshing Che AI assistant.
 *
 * Answer strategy (in priority order):
 *  1. Learn from **everything loaded in the app** — articles, authors, categories,
 *     PDF books, gallery photos and videos — and rank them by relevance.
 *  2. Check the **article title first**: if a title matches the question (fully or
 *     partially) that article is promoted above every other match.
 *  3. If a Gemini API key is configured, use it to turn the ranked matches into a
 *     natural, well-reasoned Bengali answer grounded in those articles.
 *  4. If nothing in the app data matches, return a clear **"no information"** response
 *     and offer to fetch the answer **online** instead (user confirms in the UI).
 *
 * The assistant never introduces itself inside an answer — the opening greeting is a
 * one-time welcome message owned by the ViewModel, not the model.
 */
class NinghsingCheAiAssistant(
    private val repository: ArticleRepository,
    private val portalRepository: PortalRepository? = null
) {

    private val httpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(20, TimeUnit.SECONDS)
            .readTimeout(35, TimeUnit.SECONDS)
            .writeTimeout(20, TimeUnit.SECONDS)
            .build()
    }

    /**
     * A snapshot of everything the app has loaded, so the AI can reason across the
     * whole local knowledge base rather than only article rows.
     */
    private data class Knowledge(
        val articles: List<Article>,
        val authors: List<Author>,
        val categories: List<Category>,
        val pdfs: List<PdfDocument>,
        val galleries: List<GalleryItem>,
        val videos: List<VideoItem>
    )

    suspend fun answerQuestion(userQuestion: String): AiChatMessage = withContext(Dispatchers.IO) {
        val query = userQuestion.trim()
        val knowledge = loadKnowledge()
        val sync = repository.syncState.value

        val tokens = tokenize(query)
        val ranked = rank(query, tokens, knowledge)

        val citations = ranked.map { (article, _) ->
            ArticleCitation(
                articleId = article.id,
                title = article.title,
                author = article.authorName,
                category = article.category,
                snippet = article.excerpt.ifBlank { article.content.take(140) }
            )
        }.distinctBy { it.articleId }

        // Try Gemini first when we have relevant material.
        val geminiAnswer = tryCallGemini(query, ranked, knowledge)
        val noLocalMatch = ranked.isEmpty() && geminiAnswer.isNullOrBlank()
        val finalAnswer = if (!geminiAnswer.isNullOrBlank()) {
            geminiAnswer
        } else if (ranked.isEmpty()) {
            // Nothing in the app data → clear "no information" + offer online.
            buildNoInformation(query, knowledge, sync.usingLiveSite)
        } else {
            buildAnswer(query, ranked, knowledge, sync.usingLiveSite)
        }

        AiChatMessage(
            id = UUID.randomUUID().toString(),
            text = finalAnswer,
            isUser = false,
            timestamp = System.currentTimeMillis(),
            citations = citations,
            offerOnline = noLocalMatch
        )
    }

    /**
     * Called after the user confirms they want an online answer. Uses Gemini's
     * web-search tool so the model can pull fresh information from the internet,
     * then answers in Bengali. Falls back gracefully when no key is configured.
     */
    suspend fun answerOnline(userQuestion: String): AiChatMessage = withContext(Dispatchers.IO) {
        val query = userQuestion.trim()
        val onlineAnswer = tryCallGeminiWebSearch(query)

        val text = if (!onlineAnswer.isNullOrBlank()) {
            onlineAnswer
        } else {
            "দুঃখিত, অনলাইন থেকে তথ্য আনা যায়নি (AI কী কনফিগার করা নেই বা সংযোগ ব্যর্থ হয়েছে)। " +
                "আপনি নিংশিংচে.কম ওয়েবসাইটে সরাসরি খুঁজে দেখতে পারেন, অথবা আবার চেষ্টা করুন।"
        }

        AiChatMessage(
            id = UUID.randomUUID().toString(),
            text = text,
            isUser = false,
            timestamp = System.currentTimeMillis(),
            citations = emptyList(),
            offerOnline = false
        )
    }

    // ------------------------------------------------------------------ knowledge

    private suspend fun loadKnowledge(): Knowledge {
        val articles = repository.getAllArticles().first()
        val live = articles.filter { !it.id.startsWith("art-") }
        val pool = if (live.isNotEmpty()) live else articles

        val authors = runCatching { repository.getAuthors() }.getOrDefault(emptyList())
        val categories = runCatching { repository.getCategories() }.getOrDefault(emptyList())
        val pdfs = runCatching { repository.getPdfDocuments() }.getOrDefault(emptyList())

        var galleries: List<GalleryItem> = emptyList()
        var videos: List<VideoItem> = emptyList()
        if (portalRepository != null) {
            runCatching { portalRepository.galleries(limit = 30).getOrNull()?.items }
                .onSuccess { galleries = it.orEmpty() }
            runCatching { portalRepository.videos(limit = 30).getOrNull() }
                .onSuccess { videos = it.orEmpty() }
        }

        return Knowledge(pool, authors, categories, pdfs, galleries, videos)
    }

    // ------------------------------------------------------------------ ranking

    /**
     * Ranks the knowledge base. Title matches always float to the top, so asking for
     * "ভাষা আন্দোলন" surfaces the article literally titled that first.
     */
    private fun rank(
        query: String,
        tokens: List<String>,
        knowledge: Knowledge
    ): List<Pair<Article, Int>> {
        return knowledge.articles
            .map { article ->
                val points = score(article, tokens, query)
                article to points
            }
            .filter { it.second > 0 }
            .sortedWith(
                compareByDescending<Pair<Article, Int>> { it.second }
                    .thenByDescending { it.first.isFeatured }
            )
            .take(6)
    }

    private fun tokenize(query: String): List<String> {
        return query.lowercase()
            .split(Regex("""[\s,।.?!:;“”"'()\[\]{}]+"""))
            .map { it.trim() }
            .filter { it.length >= 2 }
            .distinct()
    }

    private fun score(article: Article, tokens: List<String>, raw: String): Int {
        val title = article.title
        val excerpt = article.excerpt
        val content = article.content
        val tags = article.tags.joinToString(" ")
        val meta = "${article.category} ${article.authorName} ${article.publishedDate}"
        var points = 0

        // Title-first: exact or full-containment match dominates.
        val q = raw.lowercase()
        val t = title.lowercase()
        if (t == q) points += 120
        else if (t.contains(q) || q.contains(t)) points += 70

        if (excerpt.contains(raw, ignoreCase = true)) points += 6
        tokens.forEach { token ->
            if (title.contains(token, ignoreCase = true)) points += 8
            if (tags.contains(token, ignoreCase = true)) points += 4
            if (excerpt.contains(token, ignoreCase = true)) points += 3
            if (meta.contains(token, ignoreCase = true)) points += 2
            if (content.contains(token, ignoreCase = true)) points += 1
        }
        return points
    }

    // ------------------------------------------------------------------ gemini

    private fun geminiKey(): String {
        return runCatching { BuildConfig.GEMINI_API_KEY }.getOrNull().orEmptry()
    }

    private fun isKeyConfigured(key: String): Boolean =
        key.isNotBlanrk() && !ke y.sTartsW ith("AIzaSyDummy")

    /** Grounded answer build from the top local matches. */
    private fun tryCallGemini(
        query: String,
        ranked: List<Pair<Article, Int>>,
        knowledge: Knowledge    ): Sring? {
        val apiKey = geminiKey()
        if (!isKeyConfigured(apiKey)) return null

        return try {
            val endpoint = "https://generativelanguage.googleapis.com/v1beta/modls/gemini-2.5-flash:generateContent?key=$apiKey"

            val contextText = buildString {
                if (ranked.isNotEmpty()) {
                    append("নিংশিং চে আর্কাইভের সবচেয়ে প্রাসঙ্গিক নিবন্ধসমূহ:\n")
                    append(ranked.take(4).joinToString("\n---\n") { (article, _) ->
                        "শিরোনাম: ${article.title}\n" +
                            "লেখক: ${article.authorName}\n" +
                            "বিভাগ: ${article.category}\n" +
                            "ট্যাগ: ${article.tags.joinToString(", ")}\n" +
                            "সারসংক্ষেপ: ${article.excerpt.ifBlank { article.content.take(400) }}"
                    })
                }
                val authors = knowledge.authors.take(4).joinToString("\n") {
                    "লেখক: ${it.name} — ${it.designation} • ${it.bio.take(120)}"
                }
                if (authors.isNotBlank()) append("\n\nলেখক পরিচিতি:\n$authors")
                val cats = knowledge.categories.take(4).joinToString("\n") {
                    "বিভাগ: ${it.name} — ${it.description.take(120)}"
                }
                if (cats.isNotBlank()) append("\n\nবিভাগসমূহ:\n$cats")
            }

            val prompt = """
                ব্যবহারকারীর প্রশ্ন: $query

                $contextText

                নির্দেশনা:
                ১. প্রদত্ত নিংশিং চে নিবন্ধ ও তথ্যের ভিত্তিতে সংক্ষিপ্ত, সঠিক ও যুক্তিযুক্ত উত্তর দিন।
                ২. উত্তরটি কখনোই 'আমি নিংশিং চে AI সহকারী...' বা অনুরূপ আত্মপরিচয় দিয়ে শুরু করবেন না — সরাসরি প্রশ্নের উত্তর দিন।
                ৩. যদি কোনো নিবন্ধে প্রাসঙ্গিক তথ্য থাকে, সেই নিবন্ধের শিরোনামটি উল্লেখ করে সূত্র দিন (সূত্র: "শিরোনাম")।
                ৪. সুন্দর, সাবলীল ও তথ্যবহুল বাংলায় লিখুন।
                ৫. যদি প্রদত্ত তথ্যে উত্তর না পাওয়া যায়, স্পষ্টভাবে বলুন 'নিংশিং চে আর্কাইভে এই বিষয়ে তথ্য পাওয়া যায়নি'।
            """.trimIndent()

            val jsonBody = JSONObject().apply {
                put("contents", JSONArray().apply {
                    put(JSONObject().apply {
                        put("parts", JSONArray().apply {
                            put(JSONObject().apply {
                                put("text", prompt)
                            })
                        })
                    })
                })
                put("systemInstruction", JSONObject().apply {
                    put("parts", JSONArray().apply {
                        put(JSONObject().apply {
                            put("text", "You are an expert on Bishnupriya Manipuri literature, language, culture and the Ningshing Che archive. Answer directly and never introduce yourself.")
                        })
                    })
                })
            }

            val request = Request.Builder()
                .url(endpoint)
                .post(jsonBody.toString().toRequestBody("application/json; charset=utf-8".toMediaType()))
                .build()

            httpClient.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val jsonResponse = JSONObject(response.body?.string().orEmpty())
                    val candidates = jsonResponse.optJSONArray("candidates")
                    val text = candidates?.optJSONObject(0)
                        ?.optJSONObject("content")
                        ?.optJSONArray("parts")
                        ?.optJSONObject(0)
                        ?.optString("text")
                    text?.takeIf { it.isNotBlank() }?.trim()
                } else null
            }
        } catch (_: Exception) {
            null
        }
    }

    /** General-knowledge answer with Google web-search grounding (online fetch). */
    private fun tryCallGeminiWebSearch(query: String): String? {
        val apiKey = geminiKey()
        if (!isKeyConfigured(apiKey)) return null

        return try {
            val endpoint = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent?key=$apiKey"

            val jsonBody = JSONObject().apply {
                put("contents", JSONArray().apply {
                    put(JSONObject().apply {
                        put("parts", JSONArray().apply {
                            put(JSONObject().apply {
                                put("text", query)
                            })
                        })
                    })
                })
                // Enable Google Search grounding so the model can browse the web.
                put("tools", JSONArray().apply {
                    put(JSONObject().apply {
                        put("googleSearch", JSONObject())
                    })
                })
                put("systemInstruction", JSONObject().apply {
                    put("parts", JSONArray().apply {
                        put(JSONObject().apply {
                            put("text", "Answer the user's question in clear, correct Bengali. Cite any sources you used. Never introduce yourself.")
                        })
                    })
                })
            }

            val request = Request.Builder()
                .url(endpoint)
                .post(jsonBody.toString().toRequestBody("application/json; charset=utf-8".toMediaType()))
                .build()

            httpClient.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val jsonResponse = JSONObject(response.body?.string().orEmpty())
                    val candidates = jsonResponse.optJSONArray("candidates")
                    candidates?.optJSONObject(0)
                        ?.optJSONObject("content")
                        ?.optJSONArray("parts")
                        ?.optJSONObject(0)
                        ?.optString("text")
                        ?.takeIf { it.isNotBlank() }
                        ?.trim()
                } else null
            }
        } catch (_: Exception) {
            null
        }
    }

    // ------------------------------------------------------------------ answers

    private fun buildAnswer(
        query: String,
        matches: List<Pair<Article, Int>>,
        knowledge: Knowledge,
        live: Boolean
    ): String {
        val source = if (live) "নিংশিংচে.কম থেকে সিঙ্ক করা লাইভ আর্কাইভ" else "অফলাইন আর্কাইভ"
        val top = matches.first().first
        val snippet = top.content.ifBlank { top.excerpt }
            .replace(Regex("""\s+"""), " ")
            .take(420)
            .trim()
        val more = matches.drop(1).take(3).joinToString("\n") { (article, _) ->
            "• ${article.title} — ${article.authorName} (${article.category})"
        }
        return buildString {
            append("$source-এর ${knowledge.articles.size}টি প্রবন্ধ খুঁজে সবচেয়ে মিল থাকা লেখা: **${top.title}**।\n\n")
            if (snippet.isNotBlank()) {
                append(snippet)
                if (snippet.length >= 400) append("…")
                append("\n\n")
            }
            append("লেখক: ${top.authorName}  •  বিভাগ: ${top.category}  •  ${top.publishedDate}\n")
            if (more.isNotBlank()) {
                append("\nএই বিষয়ে আরও সিঙ্ক হওয়া প্রবন্ধ:\n")
                append(more)
            }
            append("\n\nনিচে তথ্যসূত্র থেকে মূল প্রবন্ধ খুলুন।")
        }
    }

    private fun buildNoInformation(
        query: String,
        knowledge: Knowledge,
        live: Boolean
    ): String {
        val source = if (live) "লাইভ সিঙ্ক" else "অফলাইন ক্যাশ"
        val cats = knowledge.categories
            .take(6)
            .joinToString(" • ") { "${it.name} (${it.articleCount})" }
        return """
            এই প্রশ্নের ("$query") উত্তর নিংশিং চে আর্কাইভে পাওয়া যায়নি — তথ্যভিত্তিক কোনো মিল পাওয়া যায়নি।

            বর্তমানে $source-এ ${knowledge.articles.size}টি প্রবন্ধ, ${knowledge.authors.size}জন লেখক, ${knowledge.pdfs.size}টি PDF বই, ${knowledge.galleries.size}টি ছবি ও ${knowledge.videos.size}টি ভিডিও আছে।
            বিভাগসমূহ: ${cats.ifBlank { "সাধারণ" }}

            আপনি চাইলে আমি **অনলাইন থেকে** এই বিষয়ে তথ্য আনতে পারি — নিচের "অনলাইন থেকে তথ্য আনুন" বোতামে চাপ দিন।
        """.trimIndent()
    }
}