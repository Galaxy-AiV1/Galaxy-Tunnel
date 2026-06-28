package com.example

data class VpnNode(
    val id: Int,
    val name: String,
    val description: String,
    val location: String,
    val vlessConfig: String,
    val pingUrl: String,
    val latencyMs: Int? = null,
    val isChecking: Boolean = false,
    val isOffline: Boolean = false
)
