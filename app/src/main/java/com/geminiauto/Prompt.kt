package com.geminiauto

data class Prompt(
    val id: Long = System.currentTimeMillis(),
    val text: String,
    val interval: Int = 30
)
