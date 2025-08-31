package com.tpl.hemen_lazim.model

data class HealthCheckResponse(
    val status: String,
    val components: Components?
)

data class Components(
    val db: ComponentStatus?,
    val diskSpace: ComponentStatus?,
    val ping: ComponentStatus?,
    val ssl: ComponentStatus?
)

data class ComponentStatus(
    val status: String,
    val details: Map<String, Any>?
)
