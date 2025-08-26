package com.mibi.xkas.utils

import androidx.compose.ui.graphics.Color
import com.mibi.xkas.R
import com.mibi.xkas.ui.profile.AvatarSelection


object AvatarUtils {

    // Preset avatar icons dari drawable resources
    val presetAvatars = listOf(
        "avatar_1" to R.drawable.ic_avm1,
        "avatar_2" to R.drawable.ic_avm2, // Ganti dengan avatar yang sesuai
        "avatar_3" to R.drawable.ic_avm3, // Ganti dengan avatar yang sesuai
        "avatar_4" to R.drawable.ic_avfm1, // Ganti dengan avatar yang sesuai
        "avatar_5" to R.drawable.ic_avfm2, // Ganti dengan avatar yang sesuai
        "avatar_6" to R.drawable.ic_avfm3, // Ganti dengan avatar yang sesuai
    )

    // Warna untuk avatar inisial
    private val avatarColors = listOf(
        Color(0xFF1E88E5), // Blue
        Color(0xFF43A047), // Green
        Color(0xFFE53935), // Red
        Color(0xFFFB8C00), // Orange
        Color(0xFF8E24AA), // Purple
        Color(0xFF00ACC1), // Cyan
        Color(0xFF3949AB), // Indigo
        Color(0xFF8BC34A), // Light Green
        Color(0xFFFF7043), // Deep Orange
        Color(0xFFAB47BC), // Medium Purple
    )

    /**
     * Generate avatar inisial lengkap dengan warna
     */
    fun generateInitialAvatar(name: String): AvatarSelection {
        val initial = generateInitial(name)
        val color = generateColorFromName(name)
        return AvatarSelection(
            type = "initial",
            value = initial,
            color = colorToHex(color)
        )
    }

    /**
     * Generate warna berdasarkan nama
     */
    fun generateColorFromName(name: String): Color {
        val hash = name.hashCode()
        val index = kotlin.math.abs(hash) % avatarColors.size
        return avatarColors[index]
    }

    /**
     * Generate inisial dari nama
     */
    fun generateInitial(name: String): String {
        return if (name.isNotBlank()) {
            val words = name.trim().split(" ")
            when (words.size) {
                1 -> words[0].take(1).uppercase()
                else -> "${words[0].take(1)}${words[1].take(1)}".uppercase()
            }
        } else {
            "U"
        }
    }

    /**
     * Convert Color ke hex string
     */
    fun colorToHex(color: Color): String {
        val red = (color.red * 255).toInt()
        val green = (color.green * 255).toInt()
        val blue = (color.blue * 255).toInt()
        return String.format("#%02X%02X%02X", red, green, blue)
    }

    /**
     * Convert hex string ke Color
     */
    fun hexToColor(hex: String): Color {
        return try {
            val colorLong = hex.removePrefix("#").toLong(16)
            Color(colorLong or 0x00000000FF000000)
        } catch (e: Exception) {
            avatarColors[0] // Default color
        }
    }

    /**
     * Get preset avatar resource by ID
     */
    fun getPresetAvatarResource(avatarId: String): Int {
        return presetAvatars.find { it.first == avatarId }?.second
            ?: R.drawable.ic_rocket_launch
    }

    /**
     * Get all preset avatar IDs
     */
    fun getAllPresetAvatarIds(): List<String> {
        return presetAvatars.map { it.first }
    }
}