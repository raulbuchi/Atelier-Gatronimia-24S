package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.domain.model.SmartKitchenAlertType
import com.example.ui.theme.AtelierCharcoal
import com.example.ui.theme.InfoBlue
import com.example.ui.theme.InfoBlueBg
import com.example.ui.theme.JetBrainsMonoFamily
import com.example.ui.theme.PccAmber
import com.example.ui.theme.PccAmberBg
import com.example.ui.theme.PccDanger
import com.example.ui.theme.PccDangerBg
import com.example.ui.theme.PccGreen
import com.example.ui.theme.PccGreenBg
import com.example.ui.theme.PureSurface

@Composable
fun KeepScreenOnEffect(enabled: Boolean) {
    val currentView = LocalView.current
    DisposableEffect(enabled) {
        val previous = currentView.keepScreenOn
        if (enabled) {
            currentView.keepScreenOn = true
        }
        onDispose {
            currentView.keepScreenOn = previous
        }
    }
}

/**
 * Renders the official Atelier Gastronomia 24S circular compass + chef knife icon (#272E36 & #AE2127).
 */
@Composable
fun SwissCrossBadge(modifier: Modifier = Modifier, sizeDp: Int = 40) {
    val cd = stringResource(R.string.cd_swiss_emblem)
    Box(
        modifier = modifier
            .size(sizeDp.dp)
            .clip(RoundedCornerShape((sizeDp * 0.22f).dp))
            .background(AtelierCharcoal),
        contentAlignment = Alignment.Center
    ) {
        Image(
            painter = painterResource(R.drawable.brand_icon_emblem),
            contentDescription = cd,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )
    }
}

/**
 * Renders the official full horizontal logo ("ATELIER GASTRONOMIA 24S" + circular knife-compass emblem).
 */
@Composable
fun AtelierFullLogoBanner(
    modifier: Modifier = Modifier,
    maxHeight: Dp = 112.dp
) {
    val cd = stringResource(R.string.cd_swiss_emblem)
    Surface(
        color = PureSurface,
        shape = RoundedCornerShape(12.dp),
        modifier = modifier.fillMaxWidth()
    ) {
        Image(
            painter = painterResource(R.drawable.brand_logo_wide),
            contentDescription = cd,
            contentScale = ContentScale.Fit,
            alignment = Alignment.CenterStart,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 68.dp, max = maxHeight)
                .padding(vertical = 4.dp)
        )
    }
}

@Composable
fun TagPill(
    text: String,
    containerColor: Color = MaterialTheme.colorScheme.surfaceVariant,
    contentColor: Color = MaterialTheme.colorScheme.onSurfaceVariant,
    modifier: Modifier = Modifier
) {
    Surface(
        color = containerColor,
        contentColor = contentColor,
        shape = RoundedCornerShape(6.dp),
        modifier = modifier
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            fontFamily = JetBrainsMonoFamily,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}

@Composable
fun SmartKitchenAlertBanner(
    alertType: SmartKitchenAlertType,
    message: String,
    modifier: Modifier = Modifier
) {
    if (alertType == SmartKitchenAlertType.NONE || message.isBlank()) return

    val (bg, fg, title) = when (alertType) {
        SmartKitchenAlertType.DEEP_FRY_ACRYLAMIDE ->
            Triple(PccDangerBg, PccDanger, "ALERTA ACRILAMIDA • TETO 175 °C")
        SmartKitchenAlertType.NON_STICK_VITAMIN_C ->
            Triple(PccAmberBg, PccAmber, "PROTEÇÃO DE VITAMINA C • ANTIADERENTE")
        SmartKitchenAlertType.POELIEREN_MATIGNON ->
            Triple(InfoBlueBg, InfoBlue, "PROTOCOLO POELIEREN • 140–160 °C SEM LÍQUIDO")
        SmartKitchenAlertType.SOUS_VIDE_PROTOCOL ->
            Triple(InfoBlueBg, InfoBlue, "PROTOCOLO SOUS-VIDE & SALMOURA 5–10%")
        SmartKitchenAlertType.GALANTINE_PCC_65C ->
            Triple(PccGreenBg, PccGreen, "PCC GALANTINA • CENTRO TÉRMICO 65 °C")
        SmartKitchenAlertType.PUFF_PASTRY_OXIDATION ->
            Triple(PccAmberBg, PccAmber, "ALERTA OXIDAÇÃO MASSA FOLHADA (5 °C)")
        SmartKitchenAlertType.PARALLEL_POACHING_82C ->
            Triple(PccGreenBg, PccGreen, "CONTROLE PARALELO DE SUB-EBULIÇÃO (~82 °C)")
        SmartKitchenAlertType.NONE ->
            Triple(PccAmberBg, PccAmber, "NOTA TÉCNICA")
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = bg),
        border = BorderStroke(1.5.dp, fg),
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Warning,
                contentDescription = title,
                tint = fg,
                modifier = Modifier.size(22.dp)
            )
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelMedium,
                    color = fg,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}

fun formatSecondsMmSs(totalSeconds: Int): String {
    val safe = totalSeconds.coerceAtLeast(0)
    val mins = safe / 60
    val secs = safe % 60
    return "%02d:%02d".format(mins, secs)
}
