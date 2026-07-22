package com.example.equalizersoftware

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform