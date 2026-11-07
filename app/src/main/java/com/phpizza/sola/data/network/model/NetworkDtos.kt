package com.phpizza.sola.data.network.model

import com.google.gson.annotations.SerializedName

data class CreateSessionRequestDto(
    @SerializedName("id") val id: String? = null,
    @SerializedName("title") val title: String = "",
    @SerializedName("source") val source: String = "api",
    @SerializedName("bound_channel") val boundChannel: String? = null,
    @SerializedName("default_profile") val defaultProfile: String = "interactive",
    @SerializedName("memory_scope") val memoryScope: String = "shared",
    @SerializedName("parent_session") val parentSession: String? = null
)

data class UpdateSessionRequestDto(
    @SerializedName("title") val title: String
)

data class RunRequestDto(
    @SerializedName("message") val message: String,
    @SerializedName("profile") val profile: String? = null
)

data class ApiSessionDto(
    @SerializedName("id") val id: String,
    @SerializedName("title") val title: String? = null,
    @SerializedName("source") val source: String? = null,
    @SerializedName("created_at") val createdAt: String? = null,
    @SerializedName("archived_at") val archivedAt: String? = null,
    @SerializedName("default_profile") val defaultProfile: String? = null
)
