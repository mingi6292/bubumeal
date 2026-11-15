package com.mg.bubumeal

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform