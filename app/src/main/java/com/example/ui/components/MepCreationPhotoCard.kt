package com.example.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.graphics.Bitmap
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.core.media.MediaCompressor
import com.example.ui.theme.InfoBlue
import com.example.ui.theme.InfoBlueBg
import com.example.ui.theme.JetBrainsMonoFamily
import com.example.ui.theme.OnSwissRedContainer
import com.example.ui.theme.PccGreen
import com.example.ui.theme.PccGreenBg
import com.example.ui.theme.SwissRed
import com.example.ui.theme.SwissRedContainer

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun MepCreationPhotoCard(
    sheetTitle: String,
    cloudImageLink: String,
    cloudMediaId: String,
    photoThumbnailBase64: String,
    isSyncingPhoto: Boolean,
    fullResolutionBitmap: Bitmap?,
    onOpenCameraX: () -> Unit,
    onPickFromGallery: () -> Unit,
    onRequestFullResolution: (String) -> Unit,
    onAuditWithChefAi: () -> Unit
) {
    val context = LocalContext.current
    var showZoomDialog by remember { mutableStateOf(false) }

    val previewBitmap = remember(photoThumbnailBase64, fullResolutionBitmap) {
        fullResolutionBitmap ?: MediaCompressor.decodeBase64ToBitmap(photoThumbnailBase64)
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.5.dp, SwissRed),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("mep_creation_photo_card")
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                TagPill("CAMERAX • REGISTRO DA CRIAÇÃO", SwissRedContainer, OnSwissRedContainer)
                if (cloudImageLink.isNotBlank()) {
                    TagPill("LINK SALVO NO FIRESTORE", PccGreenBg, PccGreen)
                } else {
                    TagPill("AGUARDANDO FOTO DA BANCADA", InfoBlueBg, InfoBlue)
                }
            }

            Text(
                text = "Associação Direta à Ficha Técnica & Firebase Firestore",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Fotografe seu empratamento ou mise en place diretamente na bancada. A imagem é otimizada sem metadados EXIF, associada localmente à Ficha Técnica (Room) e sincronizada com link canônico no Firebase Firestore.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Button(
                    onClick = onOpenCameraX,
                    enabled = !isSyncingPhoto,
                    colors = ButtonDefaults.buttonColors(containerColor = SwissRed),
                    modifier = Modifier
                        .weight(1.4f)
                        .height(56.dp)
                        .testTag("mep_open_camerax_button")
                ) {
                    if (isSyncingPhoto) {
                        CircularProgressIndicator(
                            color = MaterialTheme.colorScheme.onPrimary,
                            strokeWidth = 2.5.dp,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text("Sincronizando...", fontWeight = FontWeight.Bold)
                    } else {
                        Icon(Icons.Default.CameraAlt, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = if (cloudImageLink.isBlank()) {
                                stringResource(R.string.capture_camerax_mep)
                            } else {
                                "Nova Foto (CameraX)"
                            },
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                OutlinedButton(
                    onClick = onPickFromGallery,
                    enabled = !isSyncingPhoto,
                    modifier = Modifier
                        .weight(1f)
                        .height(56.dp)
                        .testTag("mep_pick_gallery_photo_button")
                ) {
                    Icon(Icons.Default.AddPhotoAlternate, contentDescription = null)
                    Spacer(Modifier.width(6.dp))
                    Text("Galeria", fontWeight = FontWeight.SemiBold)
                }
            }

            if (previewBitmap != null) {
                Image(
                    bitmap = previewBitmap.asImageBitmap(),
                    contentDescription = "Criação associada à ficha $sheetTitle",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(210.dp)
                        .clickable {
                            if (cloudMediaId.isNotBlank()) {
                                onRequestFullResolution(cloudMediaId)
                            }
                            showZoomDialog = true
                        }
                        .testTag("mep_linked_photo_preview")
                )
            }

            if (cloudImageLink.isNotBlank()) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = PccGreenBg),
                    border = BorderStroke(1.dp, PccGreen),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = Icons.Default.CloudDone,
                                contentDescription = null,
                                tint = PccGreen
                            )
                            Column {
                                Text(
                                    text = "Link da Imagem no Firebase Firestore",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = PccGreen,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = cloudImageLink,
                                    fontFamily = JetBrainsMonoFamily,
                                    style = MaterialTheme.typography.bodySmall,
                                    modifier = Modifier.testTag("mep_firestore_image_link_text")
                                )
                            }
                        }
                        IconButton(
                            onClick = {
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                                clipboard?.setPrimaryClip(
                                    ClipData.newPlainText("Link Firestore Ficha Técnica", cloudImageLink)
                                )
                            },
                            modifier = Modifier.size(48.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.ContentCopy,
                                contentDescription = "Copiar link da imagem no Firestore",
                                tint = PccGreen
                            )
                        }
                    }
                }

                Button(
                    onClick = onAuditWithChefAi,
                    colors = ButtonDefaults.buttonColors(containerColor = InfoBlue),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .testTag("mep_send_photo_to_chef_ai_button")
                ) {
                    Icon(Icons.Default.AutoAwesome, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Auditar Foto da Ficha no Chef IA & Mural", fontWeight = FontWeight.Bold)
                }
            }
        }
    }

    if (showZoomDialog && previewBitmap != null) {
        val highRes = fullResolutionBitmap ?: previewBitmap
        AlertDialog(
            onDismissRequest = { showZoomDialog = false },
            title = { Text(sheetTitle, style = MaterialTheme.typography.titleMedium) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Image(
                        bitmap = highRes.asImageBitmap(),
                        contentDescription = "Foto em alta resolução de $sheetTitle",
                        contentScale = ContentScale.FillWidth,
                        modifier = Modifier.fillMaxWidth()
                    )
                    if (cloudImageLink.isNotBlank()) {
                        Text(
                            text = cloudImageLink,
                            fontFamily = JetBrainsMonoFamily,
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showZoomDialog = false }) { Text("Fechar") }
            }
        )
    }
}
