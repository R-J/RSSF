package com.rssf.reader.data

import kotlinx.serialization.json.JsonElement
import retrofit2.http.Body
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.DELETE
import retrofit2.http.Path
import retrofit2.http.Query

interface ReaderApi {
    @FormUrlEncoded
    @POST("api/v1/auth/token")
    suspend fun login(
        @Field("username") username: String,
        @Field("password") password: String,
        @Field("grant_type") grantType: String = "password"
    ): TokenResponse

    @POST("api/v1/auth/refresh")
    suspend fun refresh(@Body body: Map<String, String>): TokenResponse

    @GET("api/v1/auth/me")
    suspend fun me(): JsonElement

    @GET("api/v1/categories")
    suspend fun categories(): JsonElement

    @POST("api/v1/categories")
    suspend fun createCategory(@Body body: CategoryCreateRequest): JsonElement

    @PATCH("api/v1/categories/{id}")
    suspend fun updateCategory(@Path("id") id: Long, @Body body: CategoryUpdateRequest): JsonElement

    @DELETE("api/v1/categories/{id}")
    suspend fun deleteCategory(@Path("id") id: Long)

    @GET("api/v1/feeds")
    suspend fun feeds(): JsonElement

    @POST("api/v1/feeds")
    suspend fun createFeed(@Body body: FeedCreateRequest): JsonElement

    @PATCH("api/v1/feeds/{id}")
    suspend fun updateFeed(@Path("id") id: Long, @Body body: FeedUpdateRequest): JsonElement

    @DELETE("api/v1/feeds/{id}")
    suspend fun deleteFeed(@Path("id") id: Long)

    @GET("api/v1/entries")
    suspend fun entries(@Query("category_id") categoryId: Long? = null, @Query("feed_id") feedId: Long? = null): JsonElement

    @GET("api/v1/entries/starred")
    suspend fun starred(): JsonElement

    @GET("api/v1/search")
    suspend fun search(@Query("q") query: String): JsonElement

    @PATCH("api/v1/entries/{id}")
    suspend fun updateEntry(@Path("id") id: Long, @Body body: Map<String, Boolean?>): JsonElement

    @POST("api/v1/entries/{id}/star")
    suspend fun star(@Path("id") id: Long)

    @DELETE("api/v1/entries/{id}/star")
    suspend fun unstar(@Path("id") id: Long)

    @POST("api/v1/entries/mark-all-read")
    suspend fun markAllRead(@Body body: Map<String, Long?> = emptyMap())

    @GET("api/v1/entries/{id}/similar")
    suspend fun similar(@Path("id") id: Long): JsonElement
}
