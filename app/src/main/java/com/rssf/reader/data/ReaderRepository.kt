package com.rssf.reader.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.rssf.reader.BuildConfig
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import okhttp3.MediaType.Companion.toMediaType

private val Context.authStore by preferencesDataStore("auth")
class ReaderRepository(private val context: Context) {
    private val json = Json { ignoreUnknownKeys = true; isLenient = true }
    private var api: ReaderApi
    private val accessKey = stringPreferencesKey("access_token")
    private val refreshKey = stringPreferencesKey("refresh_token")
    private val serverKey = stringPreferencesKey("server_url")
    private val categoriesKey = stringPreferencesKey("cached_categories")
    private val feedsKey = stringPreferencesKey("cached_feeds")
    private val entriesKey = stringPreferencesKey("cached_entries")

    init {
        val auth = Interceptor { chain ->
            val token = runBlocking { context.authStore.data.first()[accessKey] }
            val request = chain.request().newBuilder().apply {
                if (!token.isNullOrBlank()) addHeader("Authorization", "Bearer $token")
            }.build()
            chain.proceed(request)
        }
        val client = OkHttpClient.Builder().addInterceptor(auth).build()
        api = createApi(BuildConfig.API_BASE_URL, client)
    }

    suspend fun isAuthorized(): Boolean {
        val preferences = context.authStore.data.first()
        preferences[serverKey]?.let { api = createApi(it, authenticatedClient()) }
        return !preferences[accessKey].isNullOrBlank()
    }

    suspend fun login(serverUrl: String, username: String, password: String) {
        val normalizedServer = normalizeServerUrl(serverUrl)
        api = createApi(normalizedServer, authenticatedClient())
        val token = api.login(username, password)
        context.authStore.edit {
            it[accessKey] = token.access_token
            it[refreshKey] = token.refresh_token
            it[serverKey] = normalizedServer
        }
    }

    suspend fun logout() {
        context.authStore.edit { it.remove(accessKey); it.remove(refreshKey) }
    }

    private fun authenticatedClient(): OkHttpClient = OkHttpClient.Builder().addInterceptor { chain ->
        val token = runBlocking { context.authStore.data.first()[accessKey] }
        val request = chain.request().newBuilder().apply {
            if (!token.isNullOrBlank()) addHeader("Authorization", "Bearer $token")
        }.build()
        chain.proceed(request)
    }.build()

    private fun createApi(baseUrl: String, client: OkHttpClient): ReaderApi = Retrofit.Builder()
        .baseUrl(normalizeServerUrl(baseUrl))
        .client(client)
        .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
        .build().create(ReaderApi::class.java)

    private fun normalizeServerUrl(value: String): String {
        val trimmed = value.trim().removeSuffix("/")
        require(trimmed.isNotBlank() && !trimmed.contains("/api/")) {
            "Enter only the server host, for example rsse.muxi.de"
        }
        val withScheme = if (trimmed.startsWith("http://") || trimmed.startsWith("https://")) trimmed else "https://$trimmed"
        return "$withScheme/"
    }

    suspend fun categories(): List<Category> = cachedList(categoriesKey) { api.categories() }.mapIndexed { index, item ->
        Category(item.long("id", index.toLong()), item.string("name", "Untitled"), item.int("unread_count"))
    }

    suspend fun feeds(): List<Feed> = cachedList(feedsKey) { api.feeds() }.mapIndexed { index, item ->
        Feed(item.long("id", index.toLong()), item.string("title", item.string("name", "Feed")), item.string("url", ""), item.longOrNull("category_id"), item.int("unread_count"))
    }

    suspend fun entries(): List<Entry> = cachedList(entriesKey) { api.entries() }.map { it.toEntry() }
    suspend fun search(query: String): List<Entry> = api.search(query).asArray().map { it.toEntry() }

    suspend fun createCategory(name: String) {
        api.createCategory(CategoryCreateRequest(name))
        categories()
    }

    suspend fun renameCategory(id: Long, name: String) {
        api.updateCategory(id, CategoryUpdateRequest(name = name))
        categories()
    }

    suspend fun deleteCategory(id: Long) {
        api.deleteCategory(id)
        categories()
    }

    suspend fun createFeed(url: String, categoryId: Long?) {
        api.createFeed(FeedCreateRequest(url, categoryId))
        feeds()
    }

    suspend fun renameFeed(id: Long, title: String) {
        api.updateFeed(id, FeedUpdateRequest(title = title))
        feeds()
    }

    suspend fun deleteFeed(id: Long) {
        api.deleteFeed(id)
        feeds()
    }

    private suspend fun cachedList(key: androidx.datastore.preferences.core.Preferences.Key<String>, fetch: suspend () -> kotlinx.serialization.json.JsonElement): List<kotlinx.serialization.json.JsonElement> {
        return runCatching { fetch().also { value -> context.authStore.edit { it[key] = value.toString() } }.asArray() }
            .getOrElse { context.authStore.data.first()[key]?.let { json.parseToJsonElement(it).asArray() } ?: emptyList() }
    }

    private fun kotlinx.serialization.json.JsonElement.asArray(): List<kotlinx.serialization.json.JsonElement> {
        val value = this as? kotlinx.serialization.json.JsonArray
        if (value != null) return value
        val objectValue = this as? kotlinx.serialization.json.JsonObject
        return (objectValue?.get("items") ?: objectValue?.get("data"))?.let { it as? kotlinx.serialization.json.JsonArray }.orEmpty()
    }
    private fun kotlinx.serialization.json.JsonElement.objectValue() = this as? kotlinx.serialization.json.JsonObject
    private fun kotlinx.serialization.json.JsonElement.string(key: String, fallback: String) = objectValue()?.get(key)?.toString()?.trim('"') ?: fallback
    private fun kotlinx.serialization.json.JsonElement.long(key: String, fallback: Long) = string(key, fallback.toString()).toLongOrNull() ?: fallback
    private fun kotlinx.serialization.json.JsonElement.longOrNull(key: String) = objectValue()?.get(key)?.toString()?.trim('"')?.toLongOrNull()
    private fun kotlinx.serialization.json.JsonElement.int(key: String) = long(key, 0).toInt()
    private fun kotlinx.serialization.json.JsonElement.toEntry() = Entry(
        id = long("id", 0), title = string("title", "Untitled"), feedTitle = string("feed_title", string("source_title", "Feed")),
        author = string("author", ""), published = string("published_at", ""), summary = string("summary", string("content", "")),
        isRead = string("is_read", "false") == "true", isStarred = string("is_starred", "false") == "true"
    )
}
