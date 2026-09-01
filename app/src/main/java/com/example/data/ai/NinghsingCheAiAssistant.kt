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
 * The Ningshing Che AI Assistant.
 *
 * Highly capable, grounded AI scholar powered by gemini-3.5-flash with deep
 * specialization in Bishnupriya Manipuri language, literature, culture, history,
 * arts, personalities, and the Supabase digital archive.
 */
class NinghsingCheAiAssistant(
    private val repository: ArticleRepository,
    private val portalRepository: PortalRepository? = null
) {

    private val httpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(60, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .build()
    }

    /**
     * Complete local knowledge snapshot loaded from Supabase database.
     */
    private data class Knowledge(
        val articles: List<Article>,
        val authors: List<Author>,
        val categories: List<Category>,
        val pdfs: List<PdfDocument>,
        val galleries: List<GalleryItem>,
        val videos: List<VideoItem>
    )

    suspend fun answerQuestion(
        userQuestion: String,
        history: List<AiChatMessage> = emptyList()
    ): AiChatMessage = withContext(Dispatchers.IO) {
        val query = userQuestion.trim()
        val knowledge = loadKnowledge()

        val tokens = tokenize(query)
        val ranked = rank(query, tokens, knowledge)

        val citations = ranked.map { (article, _) ->
            ArticleCitation(
                articleId = article.id,
                title = article.title,
                author = article.authorName,
                category = article.category,
                snippet = article.excerpt.ifBlank { article.content.take(160) }
            )
        }.distinctBy { it.articleId }

        // Call Gemini 3.5 Flash with full conversation history and grounded knowledge
        val geminiAnswer = tryCallGemini(query, ranked, knowledge, history)
        val finalAnswer = if (!geminiAnswer.isNullOrBlank()) {
            geminiAnswer
        } else if (ranked.isEmpty()) {
            buildNoInformation(query, knowledge)
        } else {
            buildAnswer(query, ranked, knowledge)
        }

        AiChatMessage(
            id = UUID.randomUUID().toString(),
            text = finalAnswer,
            isUser = false,
            timestamp = System.currentTimeMillis(),
            citations = citations,
            offerOnline = false
        )
    }

    /**
     * Web-grounded query for external or live updates.
     */
    suspend fun answerOnline(userQuestion: String): AiChatMessage = withContext(Dispatchers.IO) {
        val query = userQuestion.trim()
        val onlineAnswer = tryCallGeminiWebSearch(query)

        val text = if (!onlineAnswer.isNullOrBlank()) {
            onlineAnswer
        } else {
            "দুঃখিত, অনলাইন থেকে তথ্য আনা যায়নি (AI কী কনফিগার করা নেই বা সংযোগ ব্যর্থ হয়েছে)। অনুগ্রহ করে পুনরায় চেষ্টা করুন।"
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
        val portal = portalRepository
        if (portal != null) {
            runCatching { portal.galleries(limit = 30).getOrNull()?.items }
                .onSuccess { galleries = it.orEmpty() }
            runCatching { portal.videos(limit = 30).getOrNull() }
                .onSuccess { videos = it.orEmpty() }
        }

        return Knowledge(pool, authors, categories, pdfs, galleries, videos)
    }

    // ------------------------------------------------------------------ ranking

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

        val q = raw.lowercase()
        val t = title.lowercase()
        if (t == q) points += 140
        else if (t.contains(q) || q.contains(t)) points += 80

        if (excerpt.contains(raw, ignoreCase = true)) points += 15
        if (tags.contains(raw, ignoreCase = true)) points += 12

        tokens.forEach { token ->
            if (title.contains(token, ignoreCase = true)) points += 10
            if (tags.contains(token, ignoreCase = true)) points += 6
            if (excerpt.contains(token, ignoreCase = true)) points += 4
            if (meta.contains(token, ignoreCase = true)) points += 3
            if (content.contains(token, ignoreCase = true)) points += 2
        }
        return points
    }

    // ------------------------------------------------------------------ gemini

    private fun geminiKey(): String {
        return runCatching { BuildConfig.GEMINI_API_KEY }.getOrNull().orEmpty()
    }

    private fun isKeyConfigured(key: String): Boolean =
        key.isNotBlank() && !key.startsWith("AIzaSyDummy")

    /**
     * Primary grounded reasoning call to Gemini 3.5 Flash.
     */
    private fun tryCallGemini(
        query: String,
        ranked: List<Pair<Article, Int>>,
        knowledge: Knowledge,
        history: List<AiChatMessage>
    ): String? {
        val apiKey = geminiKey()
        if (!isKeyConfigured(apiKey)) return null

        return try {
            val endpoint = "https://generativelanguage.googleapis.com/v1beta/models/gemini-3.5-flash:generateContent?key=$apiKey"

            val archiveContext = buildString {
                if (ranked.isNotEmpty()) {
                    append("### নিংশিং চে ডাটাবেজের প্রাসঙ্গিক নিবন্ধসমূহ:\n")
                    append(ranked.take(5).joinToString("\n---\n") { (article, _) ->
                        val bodySnippet = if (article.content.isNotBlank()) {
                            article.content.take(800)
                        } else {
                            article.excerpt
                        }
                        "**শিরোনাম:** ${article.title}\n" +
                            "**লেখক:** ${article.authorName}\n" +
                            "**বিভাগ:** ${article.category}\n" +
                            "**ট্যাগ:** ${article.tags.joinToString(", ")}\n" +
                            "**মূল পাঠ্যাংশ/সারসংক্ষেপ:** $bodySnippet"
                    })
                }

                val relevantAuthors = knowledge.authors
                    .filter { author -> query.contains(author.name, ignoreCase = true) || ranked.any { it.first.authorName == author.name } }
                    .take(3)
                if (relevantAuthors.isNotEmpty()) {
                    append("\n\n### সম্পর্কিত লেখক পরিচিতি:\n")
                    append(relevantAuthors.joinToString("\n") {
                        "- **${it.name}**: ${it.designation} (${it.bio.take(150)})"
                    })
                }

                val relevantPdfs = knowledge.pdfs
                    .filter { pdf -> tokensMatched(pdf.title, query) }
                    .take(3)
                if (relevantPdfs.isNotEmpty()) {
                    append("\n\n### সংশ্লিষ্ট বই/ই-বুক তালিকা:\n")
                    append(relevantPdfs.joinToString("\n") {
                        "- **${it.title}** (লেখক/সম্পাদক: ${it.authorOrEditor}, প্রকাশকাল: ${it.year})"
                    })
                }
            }

            val userPromptWithContext = buildString {
                if (archiveContext.isNotBlank()) {
                    append(archiveContext)
                    append("\n\n---\n")
                }
                append("ব্যবহারকারীর প্রশ্ন: $query")
            }

            // Build multi-turn conversational contents
            val contentsArray = JSONArray()

            val recentHistory = history
                .filter { it.id != "welcome" }
                .takeLast(6)

            recentHistory.forEach { msg ->
                val role = if (msg.isUser) "user" else "model"
                contentsArray.put(JSONObject().apply {
                    put("role", role)
                    put("parts", JSONArray().apply {
                        put(JSONObject().apply {
                            put("text", msg.text)
                        })
                    })
                })
            }

            // Append current prompt
            contentsArray.put(JSONObject().apply {
                put("role", "user")
                put("parts", JSONArray().apply {
                    put(JSONObject().apply {
                        put("text", userPromptWithContext)
                    })
                })
            })

            val systemInstructionText = """
                You are 'Ninghsing Che AI' (নিংশিং চে এআই), a highly capable, knowledgeable, and trained AI expert specializing in Bishnupriya Manipuri language, literature, culture, history, heritage, arts, personalities, festivals, and general knowledge.

                Guidelines for your responses:
                1. Provide comprehensive, deeply informative, and intellectually rich answers in elegant Bengali.
                2. Structure your response clearly using clean Markdown formatting with headers (###), bold key terms, organized bullet points, and numbered lists where suitable.
                3. When relevant archive articles or authors are provided in the context, synthesize their facts accurately and cite them seamlessly (e.g. সূত্র: "প্রবন্ধের শিরোনাম" — লেখক: লেখকের নাম).
                4. If the question asks about Bishnupriya Manipuri culture (e.g., Inchaughar, Minkou, Language Movement, Sudeshna Sinha, Bishu festival, Rasleela, poetry, folklore, grammar), deliver a thorough, historically accurate, and culturally authentic explanation.
                5. If the question is a general knowledge, translation, literary, or analytical inquiry, answer it with full depth, accuracy, and clarity.
                6. Do NOT start your reply with repetitive self-introductions (e.g. "আমি নিংশিং চে AI..."). Dive straight into the authoritative, well-reasoned answer.
            """.trimIndent()

            val jsonBody = JSONObject().apply {
                put("contents", contentsArray)
                put("systemInstruction", JSONObject().apply {
                    put("parts", JSONArray().apply {
                        put(JSONObject().apply {
                            put("text", systemInstructionText)
                        })
                    })
                })
                put("generationConfig", JSONObject().apply {
                    put("temperature", 0.6)
                    put("topP", 0.95)
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

    private fun tokensMatched(text: String, query: String): Boolean {
        val qTokens = tokenize(query)
        return qTokens.any { token -> text.contains(token, ignoreCase = true) }
    }

    /** General-knowledge answer with Google web-search grounding. */
    private fun tryCallGeminiWebSearch(query: String): String? {
        val apiKey = geminiKey()
        if (!isKeyConfigured(apiKey)) return null

        return try {
            val endpoint = "https://generativelanguage.googleapis.com/v1beta/models/gemini-3.5-flash:generateContent?key=$apiKey"

            val jsonBody = JSONObject().apply {
                put("contents", JSONArray().apply {
                    put(JSONObject().apply {
                        put("role", "user")
                        put("parts", JSONArray().apply {
                            put(JSONObject().apply {
                                put("text", query)
                            })
                        })
                    })
                })
                put("tools", JSONArray().apply {
                    put(JSONObject().apply {
                        put("googleSearch", JSONObject())
                    })
                })
                put("systemInstruction", JSONObject().apply {
                    put("parts", JSONArray().apply {
                        put(JSONObject().apply {
                            put("text", "You are Ninghsing Che AI. Answer in articulate, natural Bengali with structured Markdown. Do not introduce yourself.")
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

    // ------------------------------------------------------------------ fallback

    private fun buildAnswer(
        query: String,
        matches: List<Pair<Article, Int>>,
        knowledge: Knowledge
    ): String {
        val top = matches.first().first
        val snippet = top.content.ifBlank { top.excerpt }
            .replace(Regex("""\s+"""), " ")
            .take(450)
            .trim()
        val more = matches.drop(1).take(3).joinToString("\n") { (article, _) ->
            "• **${article.title}** — ${article.authorName} (${article.category})"
        }
        return buildString {
            append("### ${top.title}\n\n")
            if (snippet.isNotBlank()) {
                append(snippet)
                if (snippet.length >= 400) append("…")
                append("\n\n")
            }
            append("**লেখক:** ${top.authorName} | **বিভাগ:** ${top.category} | **প্রকাশকাল:** ${top.publishedDate}\n")
            if (more.isNotBlank()) {
                append("\n#### এই বিষয়ে আরও প্রাসঙ্গিক প্রবন্ধ:\n")
                append(more)
            }
            append("\n\n_নিচে তথ্যসূত্র কার্ড থেকে মূল প্রবন্ধ পড়তে পারেন।_")
        }
    }

    private fun buildNoInformation(
        query: String,
        knowledge: Knowledge
    ): String {
        val cats = knowledge.categories
            .take(6)
            .joinToString(" • ") { "${it.name} (${it.articleCount})" }
        return """
            এই প্রশ্নের ("$query") সাথে সরাসরি সম্পর্কিত কোনো প্রবন্ধ ডাটাবেজে চিহ্নিত করা যায়নি।

            বর্তমানে নিংশিং চে ডাটাবেজে **${knowledge.articles.size}টি প্রবন্ধ**, **${knowledge.authors.size}জন লেখক**, **${knowledge.pdfs.size}টি PDF বই**, এবং বহু ফটো-ভিডিও আর্কাইভ সংরক্ষিত আছে।

            **প্রধান বিভাগসমূহ:** ${cats.ifBlank { "সাধারণ" }}

            আপনি চাইলে কোনো নির্দিষ্ট লেখক, উৎসব (যেমন বিষু, রাস), ভাষা আন্দোলন, ঐতিহ্য বা প্রবন্ধের শিরোনাম দিয়ে প্রশ্ন করতে পারেন।
        """.trimIndent()
    }
}
