package com.mibi.xkas.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.mibi.xkas.utils.AvatarUtils

@Composable
fun AvatarDisplay(
    avatarType: String,
    avatarValue: String,
    avatarColor: String,
    userName: String,
    size: Dp = 120.dp,
    onClick: (() -> Unit)? = null,
    showBorder: Boolean = true
) {
    val modifier = Modifier
        .size(size)
        .clip(CircleShape)
        .let {
            if (onClick != null) it.clickable { onClick() } else it
        }
        .let {
            if (showBorder) {
                it.border(
                    width = 3.dp,
                    color = MaterialTheme.colorScheme.primary,
                    shape = CircleShape
                )
            } else it
        }

    when (avatarType) {
        "initial" -> {
            InitialAvatar(
                initial = avatarValue.ifEmpty { AvatarUtils.generateInitial(userName) },
                color = if (avatarColor.isNotEmpty()) {
                    AvatarUtils.hexToColor(avatarColor)
                } else {
                    AvatarUtils.generateColorFromName(userName)
                },
                size = size,
                modifier = modifier
            )
        }
        "preset" -> {
            PresetAvatar(
                presetId = avatarValue,
                size = size,
                modifier = modifier
            )
        }
        else -> {
            // Fallback ke initial avatar
            InitialAvatar(
                initial = AvatarUtils.generateInitial(userName),
                color = AvatarUtils.generateColorFromName(userName),
                size = size,
                modifier = modifier
            )
        }
    }
}

@Composable
private fun InitialAvatar(
    initial: String,
    color: Color,
    size: Dp,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.background(color),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = initial,
            style = when {
                size >= 120.dp -> MaterialTheme.typography.headlineLarge
                size >= 80.dp -> MaterialTheme.typography.headlineMedium
                size >= 60.dp -> MaterialTheme.typography.titleLarge
                else -> MaterialTheme.typography.titleMedium
            },
            fontWeight = FontWeight.Bold,
            color = Color.White
        )
    }
}

@Composable
private fun PresetAvatar(
    presetId: String,
    size: Dp,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.background(MaterialTheme.colorScheme.surfaceVariant),
        contentAlignment = Alignment.Center
    ) {
        // Gunakan Image daripada Icon agar warna asli tetap terjaga
        Image(
            painter = painterResource(id = AvatarUtils.getPresetAvatarResource(presetId)),
            contentDescription = "Avatar",
            modifier = Modifier.size(size * 0.8f), // Diperbesar sedikit karena tidak ada tint
            contentScale = ContentScale.Fit
            // HAPUS tint parameter agar warna asli gambar tetap terjaga
        )
    }
}