package com.example.data

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class DynamicAppConfig(
    val title: String,
    val description: String,
    val widgets: List<DynamicWidget> = emptyList()
)

@JsonClass(generateAdapter = true)
data class DynamicWidget(
    val id: String,
    val type: String, // "text_field", "button_counter", "checklist", "status_info", "quick_timer", "rating_scale"
    val label: String,
    val placeholder: String? = null,
    val hint: String? = null,
    val options: List<String>? = null,
    val initialValue: String? = null,
    val stateValue: String? = null
)
