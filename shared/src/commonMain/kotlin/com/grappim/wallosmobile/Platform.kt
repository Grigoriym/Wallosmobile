package com.grappim.wallosmobile

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform