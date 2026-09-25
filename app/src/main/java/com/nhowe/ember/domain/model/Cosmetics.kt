package com.nhowe.ember.domain.model

/** How the mascot is drawn. Grows with level. */
enum class FlameForm(val minLevel: Int, val title: String) {
    SPARK(1, "Spark"),
    EMBER(3, "Ember"),
    FLAME(5, "Flame"),
    BLAZE(10, "Blaze"),
    INFERNO(15, "Inferno"),
    SUPERNOVA(20, "Supernova");

    companion object {
        fun forLevel(level: Int): FlameForm = entries.last { level >= it.minLevel }
        fun next(level: Int): FlameForm? = entries.firstOrNull { it.minLevel > level }
    }
}

/** Colour set for the flame. ARGB longs so the domain stays free of Compose. */
enum class FlameSkin(val title: String, val unlockLevel: Int, val outer: Long, val inner: Long, val core: Long) {
    CLASSIC("Classic", 1, 0xFFFF6B35, 0xFFFFC145, 0xFFFFF7DB),
    VIOLET_SKY("Violet Sky", 5, 0xFF8B5CF6, 0xFFF472B6, 0xFFFDE7F3),
    EMERALD("Emerald", 8, 0xFF10B981, 0xFFA3E635, 0xFFF0FFE0),
    SOLAR("Solar", 12, 0xFFFFC145, 0xFFFFF1A8, 0xFFFFFFFF),
    MIDNIGHT("Midnight", 16, 0xFF2563EB, 0xFF38BDF8, 0xFFE0F2FE),
    ROSE_GOLD("Rose Gold", 20, 0xFFF43F5E, 0xFFFFB199, 0xFFFFF0E6);

    fun unlockedAt(level: Int): Boolean = level >= unlockLevel
}

/** App accent palette. */
enum class AccentTheme(val title: String, val unlockLevel: Int, val primary: Long, val secondary: Long, val tertiary: Long) {
    EMBER("Ember", 1, 0xFFFF6B35, 0xFFFFC145, 0xFF8B5CF6),
    VIOLET("Violet", 4, 0xFF8B5CF6, 0xFFF472B6, 0xFF38BDF8),
    MINT("Mint", 7, 0xFF34D399, 0xFFA3E635, 0xFFFFC145),
    ROSE("Rose", 10, 0xFFF43F5E, 0xFFFB923C, 0xFF8B5CF6),
    OCEAN("Ocean", 14, 0xFF38BDF8, 0xFF2DD4BF, 0xFFF472B6);

    fun unlockedAt(level: Int): Boolean = level >= unlockLevel
}

data class Unlock(val title: String, val kind: String, val level: Int)

object Unlocks {
    fun all(): List<Unlock> =
        (FlameForm.entries.drop(1).map { Unlock("${it.title} form", "form", it.minLevel) } +
            FlameSkin.entries.drop(1).map { Unlock("${it.title} flame", "skin", it.unlockLevel) } +
            AccentTheme.entries.drop(1).map { Unlock("${it.title} theme", "theme", it.unlockLevel) })
            .sortedBy { it.level }

    fun atLevel(level: Int): List<Unlock> = all().filter { it.level == level }
    fun next(level: Int): Unlock? = all().firstOrNull { it.level > level }
}
