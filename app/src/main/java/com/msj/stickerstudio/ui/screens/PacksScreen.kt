package com.msj.stickerstudio.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Collections
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.msj.stickerstudio.StickerPack
import com.msj.stickerstudio.WhatsAppHandler
import com.msj.stickerstudio.ui.theme.*
import java.io.File

@Composable
fun PacksScreen() {
    val context = LocalContext.current
    var packs by remember { mutableStateOf<List<StickerPack>>(emptyList()) }
    var exports by remember { mutableStateOf<List<File>>(emptyList()) }
    var refreshKey by remember { mutableStateOf(0) }

    LaunchedEffect(refreshKey) {
        packs = WhatsAppHandler.getRegisteredPacks(context)
        exports = WhatsAppHandler.exportsDir(context).listFiles()
            ?.filter { it.isFile }
            ?.sortedByDescending { it.lastModified() }
            ?: emptyList()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 22.dp)
            .padding(top = 20.dp, bottom = 24.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("MY PACKS", color = TextPrimary, fontWeight = FontWeight.Black, fontSize = 24.sp, letterSpacing = 1.sp)
                Text("Sticker packs registered in WhatsApp", color = TextDim, fontSize = 12.sp)
            }
            IconButton(onClick = { refreshKey++ }) {
                Icon(Icons.Rounded.Refresh, contentDescription = "Refresh", tint = Cyan)
            }
        }

        Spacer(Modifier.height(18.dp))

        if (packs.isEmpty()) {
            EmptyCard()
        } else {
            packs.forEach { pack ->
                PackCard(pack)
                Spacer(Modifier.height(12.dp))
            }
        }

        Spacer(Modifier.height(14.dp))

        Text("RECENT EXPORTS", color = TextPrimary, fontWeight = FontWeight.Black, fontSize = 18.sp, letterSpacing = 1.sp)
        Spacer(Modifier.height(10.dp))

        if (exports.isEmpty()) {
            Text(
                "Converted files will appear here.",
                color = TextDim,
                fontSize = 13.sp,
                modifier = Modifier.padding(vertical = 8.dp)
            )
        } else {
            exports.forEach { file ->
                ExportRow(file)
                Spacer(Modifier.height(8.dp))
            }
        }
    }
}

@Composable
private fun EmptyCard() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .background(SurfaceCard)
            .padding(28.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            Icons.Rounded.Collections,
            contentDescription = null,
            tint = TextDim,
            modifier = Modifier.size(44.dp)
        )
        Spacer(Modifier.height(10.dp))
        Text("No packs yet", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 15.sp)
        Spacer(Modifier.height(4.dp))
        Text(
            "Create your first sticker from the Create tab \u2014 it lands here instantly.",
            color = TextDim,
            fontSize = 13.sp,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
    }
}

@Composable
private fun PackCard(pack: StickerPack) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(SurfaceCard)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(54.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(Brush.linearGradient(listOf(Violet, Magenta, Cyan))),
            contentAlignment = Alignment.Center
        ) {
            Text(
                pack.name.take(2).uppercase(),
                color = Color.White,
                fontWeight = FontWeight.Black,
                fontSize = 18.sp
            )
        }
        Spacer(Modifier.width(14.dp))
        Column {
            Text(pack.name, color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 15.sp, maxLines = 1)
            Spacer(Modifier.height(2.dp))
            Text("by ${pack.publisher} \u00B7 in WhatsApp", color = TextDim, fontSize = 12.sp)
        }
    }
}

@Composable
private fun ExportRow(file: File) {
    val ext = file.extension.uppercase()
    val accent = when (ext) {
        "WEBP" -> Magenta
        "GIF" -> Cyan
        "WEBM" -> Gold
        "M4A" -> Violet
        else -> TextDim
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(SurfaceCard.copy(alpha = 0.8f))
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(accent.copy(alpha = 0.2f)),
            contentAlignment = Alignment.Center
        ) {
            Text(ext, color = accent, fontWeight = FontWeight.Black, fontSize = 11.sp)
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(file.name, color = TextPrimary, fontSize = 13.sp, maxLines = 1)
            Text(
                "${file.length() / 1024} KB",
                color = TextDim,
                fontSize = 11.sp
            )
        }
    }
}
