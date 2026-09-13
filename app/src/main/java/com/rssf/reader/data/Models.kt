package com.rssf.reader.data

import kotlinx.serialization.Serializable

@Serializable
data class TokenResponse(
    val access_token: String,
    val refresh_token: String,
    val token_type: String = "bearer"
)

@Serializable
data class CategoryCreateRequest(val name: String, val sort_order: Int = 0)

@Serializable
data class CategoryUpdateRequest(val name: String? = null, val sort_order: Int? = null)

@Serializable
data class FeedCreateRequest(val url: String, val category_id: Long? = null)

@Serializable
data class FeedUpdateRequest(val title: String? = null, val category_id: Long? = null, val sort_order: Int? = null)

@Serializable
data class LoginRequest(
    val username: String,
    val password: String,
    val grant_type: String = "password",
    val scope: String = ""
)

data class Category(val id: Long, val name: String, val unread: Int = 0)
data class Feed(val id: Long, val title: String, val url: String, val categoryId: Long? = null, val unread: Int = 0)
data class Entry(
    val id: Long,
    val title: String,
    val feedTitle: String,
    val author: String = "",
    val published: String = "",
    val summary: String = "",
    val isRead: Boolean = false,
    val isStarred: Boolean = false
)
