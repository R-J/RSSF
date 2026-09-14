package com.rssf.reader.data

import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put

interface ReaderDataSource {
    suspend fun login(username: String, password: String, grantType: String = "password"): TokenResponse
    suspend fun refresh(body: Map<String, String>): TokenResponse
    suspend fun me(): JsonElement
    suspend fun categories(): JsonElement
    suspend fun createCategory(body: CategoryCreateRequest): JsonElement
    suspend fun updateCategory(id: Long, body: CategoryUpdateRequest): JsonElement
    suspend fun deleteCategory(id: Long)
    suspend fun feeds(): JsonElement
    suspend fun createFeed(body: FeedCreateRequest): JsonElement
    suspend fun updateFeed(id: Long, body: FeedUpdateRequest): JsonElement
    suspend fun deleteFeed(id: Long)
    suspend fun entries(categoryId: Long? = null, feedId: Long? = null): JsonElement
    suspend fun starred(): JsonElement
    suspend fun search(query: String): JsonElement
    suspend fun updateEntry(id: Long, body: Map<String, Boolean?>): JsonElement
    suspend fun star(id: Long)
    suspend fun unstar(id: Long)
    suspend fun markAllRead(body: Map<String, Long?> = emptyMap())
    suspend fun similar(id: Long): JsonElement
}

class RetrofitReaderDataSource(private val api: ReaderApi) : ReaderDataSource {
    override suspend fun login(username: String, password: String, grantType: String) = api.login(username, password, grantType)
    override suspend fun refresh(body: Map<String, String>) = api.refresh(body)
    override suspend fun me() = api.me()
    override suspend fun categories() = api.categories()
    override suspend fun createCategory(body: CategoryCreateRequest) = api.createCategory(body)
    override suspend fun updateCategory(id: Long, body: CategoryUpdateRequest) = api.updateCategory(id, body)
    override suspend fun deleteCategory(id: Long) = api.deleteCategory(id)
    override suspend fun feeds() = api.feeds()
    override suspend fun createFeed(body: FeedCreateRequest) = api.createFeed(body)
    override suspend fun updateFeed(id: Long, body: FeedUpdateRequest) = api.updateFeed(id, body)
    override suspend fun deleteFeed(id: Long) = api.deleteFeed(id)
    override suspend fun entries(categoryId: Long?, feedId: Long?) = api.entries(categoryId, feedId)
    override suspend fun starred() = api.starred()
    override suspend fun search(query: String) = api.search(query)
    override suspend fun updateEntry(id: Long, body: Map<String, Boolean?>) = api.updateEntry(id, body)
    override suspend fun star(id: Long) = api.star(id)
    override suspend fun unstar(id: Long) = api.unstar(id)
    override suspend fun markAllRead(body: Map<String, Long?>) = api.markAllRead(body)
    override suspend fun similar(id: Long) = api.similar(id)
}

class MockReaderDataSource : ReaderDataSource {
    private val categories = mutableListOf(
        category(1, "News", 2),
        category(2, "Gadgets", 1),
        category(3, "Develop", 0)
    )
    private val feeds = mutableListOf(
        feed(1, "The Verge", "https://www.theverge.com/rss/index.xml", 2, 1),
        feed(2, "Kotlin Blog", "https://blog.jetbrains.com/kotlin/feed/", 3, 0),
        feed(3, "TechCrunch", "https://techcrunch.com/feed/", 1, 1)
    )
    private val entries = mutableListOf(
        entry(1, "Android 16 makes everyday multitasking feel effortless", "The Verge", "2026-09-14", "The latest Android release focuses on useful details that make a busy reading day calmer.", false, true, 1),
        entry(2, "Kotlin 2.3 brings a faster compiler pipeline", "Kotlin Blog", "2026-09-13", "A practical look at the new compiler improvements and what they mean for Android teams.", false, false, 2),
        entry(3, "Small teams are building surprisingly ambitious products", "TechCrunch", "2026-09-12", "The tools are better, the feedback loops are shorter, and the best ideas are shipping sooner.", true, false, 3),
        entry(4, "Designing feeds people can actually finish", "The Verge", "2026-09-11", "A thoughtful interface helps readers keep context without turning every article into a task.", false, false, 1)
    )
    private var nextCategoryId = 4L
    private var nextFeedId = 4L

    override suspend fun login(username: String, password: String, grantType: String) = TokenResponse("mock-access-token", "mock-refresh-token")
    override suspend fun refresh(body: Map<String, String>) = TokenResponse("mock-access-token", "mock-refresh-token")
    override suspend fun me() = buildJsonObject { put("id", 1); put("username", "demo"); put("email", "demo@example.com") }
    override suspend fun categories() = JsonArray(categories.toList())

    override suspend fun createCategory(body: CategoryCreateRequest): JsonElement {
        val created = category(nextCategoryId++, body.name)
        categories += created
        return created
    }

    override suspend fun updateCategory(id: Long, body: CategoryUpdateRequest): JsonElement {
        val index = categories.indexOfFirst { it.long("id") == id }
        if (index >= 0) {
            val updated = categories[index].let { current ->
                buildJsonObject {
                    current.forEach { (key, value) -> put(key, value) }
                    body.name?.let { put("name", it) }
                }
            }
            if (body.sort_order == null) categories[index] = updated
            else {
                categories.removeAt(index)
                categories.add(body.sort_order.coerceIn(0, categories.size), updated)
                categories.replaceAll { current ->
                    buildJsonObject {
                        current.forEach { (key, value) -> put(key, value) }
                        put("sort_order", categories.indexOf(current))
                    }
                }
            }
        }
        return categories.getOrElse(index) { buildJsonObject { put("id", id) } }
    }

    override suspend fun deleteCategory(id: Long) { categories.removeAll { it.long("id") == id } }
    override suspend fun feeds() = JsonArray(feeds.toList())

    override suspend fun createFeed(body: FeedCreateRequest): JsonElement {
        val created = feed(nextFeedId++, body.url.substringAfterLast('/').ifBlank { "New feed" }, body.url, body.category_id)
        feeds += created
        return created
    }

    override suspend fun updateFeed(id: Long, body: FeedUpdateRequest): JsonElement {
        val index = feeds.indexOfFirst { it.long("id") == id }
        if (index >= 0) feeds[index] = feeds[index].let { current ->
            buildJsonObject {
                current.forEach { (key, value) -> put(key, value) }
                body.title?.let { put("title", it) }
                body.category_id?.let { put("category_id", it) }
            }
        }
        return feeds.getOrElse(index) { buildJsonObject { put("id", id) } }
    }

    override suspend fun deleteFeed(id: Long) { feeds.removeAll { it.long("id") == id } }
    override suspend fun entries(categoryId: Long?, feedId: Long?) = JsonArray(entries.filter {
        (categoryId == null || it.long("category_id") == categoryId) && (feedId == null || it.long("feed_id") == feedId)
    })
    override suspend fun starred() = JsonArray(entries.filter { it.boolean("is_starred") })
    override suspend fun search(query: String) = JsonArray(entries.filter { entry ->
        entry.string("title").contains(query, ignoreCase = true) || entry.string("summary").contains(query, ignoreCase = true)
    })

    override suspend fun updateEntry(id: Long, body: Map<String, Boolean?>): JsonElement {
        val index = entries.indexOfFirst { it.long("id") == id }
        if (index >= 0) entries[index] = entries[index].let { current ->
            buildJsonObject {
                current.forEach { (key, value) -> put(key, value) }
                body["is_read"]?.let { put("is_read", it) }
                body["is_starred"]?.let { put("is_starred", it) }
            }
        }
        return entries.getOrElse(index) { buildJsonObject { put("id", id) } }
    }

    override suspend fun star(id: Long) { updateEntry(id, mapOf("is_starred" to true)) }
    override suspend fun unstar(id: Long) { updateEntry(id, mapOf("is_starred" to false)) }
    override suspend fun markAllRead(body: Map<String, Long?>) {
        val categoryId = body["category_id"]
        val feedId = body["feed_id"]
        entries.replaceAll { current ->
            val matches = (categoryId == null || current.long("category_id") == categoryId) && (feedId == null || current.long("feed_id") == feedId)
            if (matches) buildJsonObject { current.forEach { (key, value) -> put(key, value) }; put("is_read", true) } else current
        }
        categoryId?.let { id ->
            val index = categories.indexOfFirst { it.long("id") == id }
            if (index >= 0) categories[index] = buildJsonObject { categories[index].forEach { (key, value) -> put(key, value) }; put("unread_count", 0) }
        }
        feedId?.let { id ->
            val index = feeds.indexOfFirst { it.long("id") == id }
            if (index >= 0) feeds[index] = buildJsonObject { feeds[index].forEach { (key, value) -> put(key, value) }; put("unread_count", 0) }
        }
    }
    override suspend fun similar(id: Long) = JsonArray(entries.filter { it.long("id") != id }.take(2))

    private fun JsonObject.long(key: String) = this[key]?.jsonPrimitive?.content?.toLongOrNull() ?: 0L
    private fun JsonObject.string(key: String) = this[key]?.jsonPrimitive?.content.orEmpty()
    private fun JsonObject.boolean(key: String) = this[key]?.jsonPrimitive?.content?.toBooleanStrictOrNull() ?: false

    private companion object {
        fun category(id: Long, name: String, unread: Int = 0) = buildJsonObject { put("id", id); put("name", name); put("unread_count", unread) }
        fun feed(id: Long, title: String, url: String, categoryId: Long? = null, unread: Int = 0) = buildJsonObject {
            put("id", id); put("title", title); put("url", url); categoryId?.let { put("category_id", it) }; put("unread_count", unread)
        }
        fun entry(id: Long, title: String, feedTitle: String, published: String, summary: String, isRead: Boolean, isStarred: Boolean, feedId: Long) = buildJsonObject {
            put("id", id); put("title", title); put("feed_title", feedTitle); put("author", "RSSF Editorial"); put("published_at", published)
            put("summary", summary); put("is_read", isRead); put("is_starred", isStarred); put("feed_id", feedId)
        }
    }
}