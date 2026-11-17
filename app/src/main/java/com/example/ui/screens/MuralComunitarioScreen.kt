package com.example.ui.screens

import android.graphics.Bitmap
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.background
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Comment
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AddAPhoto
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.core.media.CompressedMediaPayload
import com.example.core.media.MediaCompressor
import com.example.data.curriculum.SwissmilkAndBaseRecipesCatalog
import com.example.data.remote.PortfolioPost
import com.example.data.remote.PostComment
import com.example.domain.model.RecipeSpec
import com.example.ui.components.TagPill
import com.example.ui.theme.InfoBlue
import com.example.ui.theme.InfoBlueBg
import com.example.ui.theme.JetBrainsMonoFamily
import com.example.ui.theme.OnSwissRedContainer
import com.example.ui.theme.PccAmber
import com.example.ui.theme.PccAmberBg
import com.example.ui.theme.PccDanger
import com.example.ui.theme.PccGreen
import com.example.ui.theme.PccGreenBg
import com.example.ui.theme.SwissRed
import com.example.ui.theme.SwissRedContainer
import com.example.ui.viewmodel.ChefAiUiState
import com.example.ui.viewmodel.StreamUiState
import com.google.firebase.Timestamp
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val QUICK_PEER_FEEDBACK_CHIPS = listOf(
    "Ponto & brilho impecáveis!",
    "Cortes super regulares!",
    "Belo nappé no molho!",
    "Excelente controle de PCC!"
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun MuralComunitarioSection(
    state: StreamUiState<List<PortfolioPost>>,
    currentUserId: String,
    onOpenQuickPublish: () -> Unit,
    onReact: (PortfolioPost) -> Unit,
    onOpenComments: (PortfolioPost) -> Unit,
    onZoomPhoto: (PortfolioPost) -> Unit,
    onShareCard: (PortfolioPost) -> Unit,
    onToggleVisibility: (PortfolioPost) -> Unit,
    onDeleteOwnPost: (PortfolioPost) -> Unit
) {
    var sortMode by rememberSaveable { mutableStateOf("RECENTES") } // RECENTES | CURTIDAS | COMENTADAS | MINHAS
    var filterCycle by rememberSaveable { mutableIntStateOf(0) }
    var filterAxis by rememberSaveable { mutableIntStateOf(0) }
    var filterWeek by rememberSaveable { mutableIntStateOf(0) }
    var filterMaterial by rememberSaveable { mutableStateOf("TODOS") }
    var filterCanton by rememberSaveable { mutableStateOf("TODOS") }
    var onlySwissmilk by rememberSaveable { mutableStateOf(false) }
    var onlyChefkoch by rememberSaveable { mutableStateOf(false) }

    val posts = (state as? StreamUiState.Success)?.data.orEmpty()
    val totalLikes = remember(posts) { posts.sumOf { it.reactionCount } }
    val totalComments = remember(posts) { posts.sumOf { it.commentCount } }

    // Hero Card do Mural Comunitário com KPIs em tempo real e publicação direta
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.5.dp, SwissRed),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("mural_hero_card")
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                TagPill("MURAL COMUNITÁRIO • FIRESTORE TEMPO REAL", SwissRedContainer, OnSwissRedContainer)
                TagPill("${posts.size} pratos", PccGreenBg, PccGreen)
            }
            Text(
                text = stringResource(R.string.mural_comunitario_title),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = stringResource(R.string.mural_comunitario_subtitle),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                TagPill("Publicações: ${posts.size}", InfoBlueBg, InfoBlue)
                TagPill("Curtidas: $totalLikes", SwissRedContainer, OnSwissRedContainer)
                TagPill("Comentários: $totalComments", PccAmberBg, PccAmber)
            }
            Button(
                onClick = onOpenQuickPublish,
                colors = ButtonDefaults.buttonColors(containerColor = SwissRed),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .testTag("mural_open_publish_dialog_button")
            ) {
                Icon(Icons.Default.AddAPhoto, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.mural_publish_cta), fontWeight = FontWeight.Bold)
            }
        }
    }

    // Ordenação rápida e escopo de bancada (baixo toque >= 48dp)
    Row(
        modifier = Modifier.horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        listOf(
            "RECENTES" to "Mais Recentes",
            "CURTIDAS" to "Mais Curtidas",
            "COMENTADAS" to "Mais Comentadas",
            "MINHAS" to "Minhas Publicações"
        ).forEach { (modeId, label) ->
            FilterChip(
                selected = sortMode == modeId,
                onClick = { sortMode = modeId },
                label = { Text(label, fontWeight = FontWeight.SemiBold) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = SwissRedContainer,
                    selectedLabelColor = OnSwissRedContainer
                ),
                modifier = Modifier
                    .height(48.dp)
                    .testTag("mural_sort_chip_$modeId")
            )
        }
    }

    // Filtros curriculares combinados (Ciclos, Semanas, Material, Origem)
    Row(modifier = Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        (0..4).forEach { c ->
            FilterChip(
                selected = filterCycle == c,
                onClick = { filterCycle = c },
                label = { Text(if (c == 0) "Todos Ciclos" else "Ciclo $c") },
                modifier = Modifier.height(48.dp)
            )
        }
        FilterChip(
            selected = onlySwissmilk,
            onClick = { onlySwissmilk = !onlySwissmilk },
            label = { Text("Clássicos Swissmilk") },
            modifier = Modifier.height(48.dp)
        )
        FilterChip(
            selected = onlyChefkoch,
            onClick = { onlyChefkoch = !onlyChefkoch },
            label = { Text("Chefkoch Passo a Passo") },
            modifier = Modifier.height(48.dp)
        )
    }
    Row(modifier = Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        (0..6).forEach { ax ->
            FilterChip(
                selected = filterAxis == ax,
                onClick = { filterAxis = ax },
                label = { Text(if (ax == 0) "Todos Eixos" else "Eixo $ax") },
                modifier = Modifier.height(48.dp)
            )
        }
        (listOf(0) + (1..24).toList()).forEach { wk ->
            FilterChip(
                selected = filterWeek == wk,
                onClick = { filterWeek = wk },
                label = { Text(if (wk == 0) "Todas Sem." else "S${"%02d".format(wk)}", fontFamily = JetBrainsMonoFamily) },
                modifier = Modifier.height(48.dp)
            )
        }
    }
    Row(modifier = Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        listOf("TODOS", "prato_final", "cortes_mep", "ficha_preenchida", "resumo_esquema").forEach { m ->
            FilterChip(
                selected = filterMaterial == m,
                onClick = { filterMaterial = m },
                label = { Text(formatMuralMaterialTypePtBr(m)) },
                modifier = Modifier.height(48.dp)
            )
        }
        (listOf("TODOS") + SwissmilkAndBaseRecipesCatalog.availableCantons).forEach { ct ->
            FilterChip(
                selected = filterCanton == ct,
                onClick = { filterCanton = ct },
                label = { Text(if (ct == "TODOS") "Cantão: Todos" else ct, fontFamily = JetBrainsMonoFamily) },
                modifier = Modifier.height(48.dp)
            )
        }
    }

    when (state) {
        is StreamUiState.Loading -> {
            Row(
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier.fillMaxWidth().padding(24.dp)
            ) { CircularProgressIndicator(color = SwissRed) }
        }
        is StreamUiState.Error -> {
            Card(colors = CardDefaults.cardColors(containerColor = SwissRedContainer), modifier = Modifier.fillMaxWidth()) {
                Text(state.message, color = PccDanger, modifier = Modifier.padding(14.dp))
            }
        }
        is StreamUiState.Success -> {
            val filtered = remember(
                state.data, sortMode, filterCycle, filterAxis, filterWeek,
                filterMaterial, filterCanton, onlySwissmilk, onlyChefkoch, currentUserId
            ) {
                val base = state.data.filter { post ->
                    (sortMode != "MINHAS" || post.userId == currentUserId) &&
                        (filterCycle == 0 || post.cycleNumber == filterCycle) &&
                        (filterAxis == 0 || post.axisId == filterAxis) &&
                        (filterWeek == 0 || post.weekNumber == filterWeek) &&
                        (filterMaterial == "TODOS" || post.materialType == filterMaterial) &&
                        (filterCanton == "TODOS" || post.cantonCode == filterCanton) &&
                        (!onlySwissmilk || post.isSwissmilkClassic) &&
                        (!onlyChefkoch || post.isChefkochV4)
                }
                when (sortMode) {
                    "CURTIDAS" -> base.sortedWith(
                        compareByDescending<PortfolioPost> { it.reactionCount }
                            .thenByDescending { it.createdAt?.seconds ?: 0L }
                    )
                    "COMENTADAS" -> base.sortedWith(
                        compareByDescending<PortfolioPost> { it.commentCount }
                            .thenByDescending { it.createdAt?.seconds ?: 0L }
                    )
                    else -> base.sortedByDescending { it.createdAt?.seconds ?: 0L }
                }
            }

            if (filtered.isEmpty()) {
                Card(colors = CardDefaults.cardColors(containerColor = InfoBlueBg), modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = "Nenhuma preparação encontrada neste filtro do Mural Comunitário.",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = InfoBlue
                        )
                        Text(
                            text = "Toque em 'Publicar Preparação no Mural' acima para fotografar seu prato ou mise en place e compartilhar com os colegas!",
                            style = MaterialTheme.typography.bodySmall,
                            color = InfoBlue
                        )
                    }
                }
            }

            filtered.forEach { post ->
                val isOwner = post.userId == currentUserId
                MuralPostCard(
                    post = post,
                    isLiked = currentUserId in post.reactedUserIds,
                    showOwnerControls = isOwner,
                    onReact = { onReact(post) },
                    onComment = { onOpenComments(post) },
                    onShareCard = { onShareCard(post) },
                    onZoom = { onZoomPhoto(post) },
                    onToggleVisibility = { onToggleVisibility(post) },
                    onDelete = { onDeleteOwnPost(post) }
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun MuralPostCard(
    post: PortfolioPost,
    isLiked: Boolean,
    showOwnerControls: Boolean,
    onReact: () -> Unit,
    onComment: () -> Unit,
    onShareCard: () -> Unit,
    onZoom: () -> Unit,
    onToggleVisibility: () -> Unit,
    onDelete: () -> Unit
) {
    val thumbBmp = remember(post.thumbnailBase64) {
        MediaCompressor.decodeBase64ToBitmap(post.thumbnailBase64)
    }
    val authorAvatarBmp = remember(post.authorAvatarBase64) {
        post.authorAvatarBase64?.let { MediaCompressor.decodeBase64ToBitmap(it) }
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("mural_post_card_${post.id}")
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                TagPill("S${"%02d".format(post.weekNumber)} • ${formatMuralMaterialTypePtBr(post.materialType)}", SwissRedContainer, OnSwissRedContainer)
                if (post.cantonCode.isNotBlank()) TagPill("CANTÃO ${post.cantonCode}", InfoBlueBg, InfoBlue)
                if (post.isSwissmilkClassic) TagPill("SWISSMILK", InfoBlueBg, InfoBlue)
                if (post.isChefkochV4) TagPill("CHEFKOCH", PccAmberBg, PccAmber)
                if (post.selbstkontrolleScore in 1..5) {
                    TagPill("AUTOAVALIAÇÃO ${post.selbstkontrolleScore}/5", PccAmberBg, PccAmber)
                }
                TagPill(if (post.visibility == "public") "MURAL PÚBLICO" else "PORTFÓLIO PRIVADO", PccGreenBg, PccGreen)
            }

            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    if (authorAvatarBmp != null) {
                        Image(
                            bitmap = authorAvatarBmp.asImageBitmap(),
                            contentDescription = "Avatar de ${post.authorName}",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                        )
                    } else {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(36.dp)
                                .background(SwissRedContainer, CircleShape)
                        ) {
                            Text(
                                text = post.authorName.trim().take(1).uppercase().ifEmpty { "C" },
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                color = OnSwissRedContainer
                            )
                        }
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(post.recipeTitle, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text(
                            text = "Brigada: ${post.authorName} • ${formatFirestoreTimestampPtBr(post.createdAt)}",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                if (thumbBmp != null) {
                    IconButton(onClick = onZoom, modifier = Modifier.size(48.dp)) {
                        Icon(Icons.Default.ZoomIn, contentDescription = "Ampliar foto de ${post.recipeTitle}", tint = SwissRed)
                    }
                }
            }

            if (thumbBmp != null) {
                Image(
                    bitmap = thumbBmp.asImageBitmap(),
                    contentDescription = "Foto da preparação ${post.recipeTitle} por ${post.authorName}",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(220.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .clickable { onZoom() }
                )
            }

            if (post.caption.isNotBlank()) {
                Text(post.caption, style = MaterialTheme.typography.bodyMedium)
            }
            if (post.mepSummary.isNotBlank()) {
                Text(post.mepSummary, style = MaterialTheme.typography.bodySmall, fontFamily = JetBrainsMonoFamily)
            }
            if (post.erkenntnisse.isNotBlank() || post.offenePunkte.isNotBlank()) {
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f))) {
                    Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        if (post.erkenntnisse.isNotBlank()) {
                            Text("✓ Aprendizados (Erkenntnisse): ${post.erkenntnisse}", style = MaterialTheme.typography.bodySmall)
                        }
                        if (post.offenePunkte.isNotBlank()) {
                            Text("➔ Pontos Abertos (Offene Punkte): ${post.offenePunkte}", style = MaterialTheme.typography.bodySmall, color = PccAmber)
                        }
                    }
                }
            }
            if (post.aiFeedback.isNotBlank()) {
                Card(colors = CardDefaults.cardColors(containerColor = PccGreenBg)) {
                    Text(post.aiFeedback, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(10.dp))
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

            // Barra social comunitária: Curtidas e Comentários sempre acessíveis
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedButton(
                    onClick = onReact,
                    border = BorderStroke(1.dp, if (isLiked) SwissRed else MaterialTheme.colorScheme.outline),
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = if (isLiked) SwissRedContainer else MaterialTheme.colorScheme.surface,
                        contentColor = if (isLiked) OnSwissRedContainer else MaterialTheme.colorScheme.onSurface
                    ),
                    modifier = Modifier
                        .height(48.dp)
                        .testTag("mural_like_button_${post.id}")
                ) {
                    Icon(
                        imageVector = if (isLiked) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        contentDescription = stringResource(R.string.cd_mural_like),
                        tint = if (isLiked) SwissRed else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = "${if (isLiked) stringResource(R.string.mural_liked_action) else stringResource(R.string.mural_like_action)} (${post.reactionCount})",
                        fontWeight = FontWeight.SemiBold
                    )
                }

                OutlinedButton(
                    onClick = onComment,
                    modifier = Modifier
                        .height(48.dp)
                        .testTag("mural_comment_button_${post.id}")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Comment,
                        contentDescription = stringResource(R.string.cd_mural_comments),
                        tint = InfoBlue
                    )
                    Spacer(Modifier.width(6.dp))
                    Text("${stringResource(R.string.mural_comments_action)} (${post.commentCount})", fontWeight = FontWeight.SemiBold)
                }

                OutlinedButton(onClick = onShareCard, modifier = Modifier.height(48.dp)) {
                    Icon(Icons.Default.Share, contentDescription = stringResource(R.string.export_shareable_card))
                    Spacer(Modifier.width(6.dp))
                    Text("Cartão")
                }
            }

            if (showOwnerControls) {
                Row(
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedButton(
                        onClick = onToggleVisibility,
                        modifier = Modifier
                            .height(48.dp)
                            .testTag("mural_visibility_button_${post.id}")
                    ) {
                        Icon(
                            imageVector = if (post.visibility == "public") Icons.Default.Lock else Icons.Default.Public,
                            contentDescription = "Alternar visibilidade da preparação"
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(if (post.visibility == "public") "Tornar Privado" else "Publicar no Mural")
                    }
                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("mural_delete_post_button_${post.id}")
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = "Excluir publicação", tint = PccDanger)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun MuralQuickPublishDialog(
    authorName: String,
    initialWeekNumber: Int,
    selectedMedia: CompressedMediaPayload?,
    chefAiState: ChefAiUiState,
    isPublishing: Boolean,
    onOpenCameraX: (String, Int) -> Unit,
    onPickGalleryPhoto: () -> Unit,
    onRunChefAudit: (String, String, Int, String) -> Unit,
    onSubmitPublicPost: (
        weekNumber: Int,
        materialType: String,
        recipe: RecipeSpec?,
        caption: String,
        mepSummary: String,
        aiFeedback: String
    ) -> Unit,
    onDismiss: () -> Unit
) {
    var weekFilter by rememberSaveable { mutableIntStateOf(0) } // 0 = todas as 45 fichas
    var selectedRecipeId by rememberSaveable {
        mutableStateOf(
            SwissmilkAndBaseRecipesCatalog.recipesForWeek(initialWeekNumber).firstOrNull()?.id
                ?: SwissmilkAndBaseRecipesCatalog.allRecipes.first().id
        )
    }
    var materialType by rememberSaveable { mutableStateOf("prato_final") }
    var caption by rememberSaveable { mutableStateOf("") }
    var mepSummary by rememberSaveable { mutableStateOf("") }

    val visibleRecipes = remember(weekFilter) {
        if (weekFilter == 0) SwissmilkAndBaseRecipesCatalog.allRecipes
        else SwissmilkAndBaseRecipesCatalog.recipesForWeek(weekFilter)
    }
    val activeRecipe = SwissmilkAndBaseRecipesCatalog.findById(selectedRecipeId)
    val effectiveWeek = activeRecipe?.weekNumber ?: if (weekFilter in 1..24) weekFilter else initialWeekNumber
    val aiFeedbackText = (chefAiState as? ChefAiUiState.AuditReady)?.formattedText.orEmpty()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Publicar Foto no Mural Comunitário", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "Autor na Brigada: $authorName • Visibilidade: Público para a turma",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    Button(
                        onClick = { onOpenCameraX(selectedRecipeId, effectiveWeek) },
                        colors = ButtonDefaults.buttonColors(containerColor = SwissRed),
                        modifier = Modifier
                            .weight(1.2f)
                            .height(52.dp)
                            .testTag("mural_quick_camerax_button")
                    ) {
                        Icon(Icons.Default.CameraAlt, contentDescription = null)
                        Spacer(Modifier.width(6.dp))
                        Text("Câmera", fontWeight = FontWeight.Bold)
                    }
                    OutlinedButton(
                        onClick = onPickGalleryPhoto,
                        modifier = Modifier
                            .weight(1f)
                            .height(52.dp)
                            .testTag("mural_quick_gallery_button")
                    ) {
                        Icon(Icons.Default.AddPhotoAlternate, contentDescription = null)
                        Spacer(Modifier.width(6.dp))
                        Text(if (selectedMedia == null) "Galeria" else "Trocar")
                    }
                }

                if (selectedMedia != null) {
                    Image(
                        bitmap = selectedMedia.fullBitmap.asImageBitmap(),
                        contentDescription = "Prévia da preparação selecionada",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(160.dp)
                            .clip(RoundedCornerShape(10.dp))
                    )
                    if (selectedMedia.compressedSizeBytes > 0) {
                        val fmt = MediaCompressor.formatBytes(selectedMedia.compressedSizeBytes.toLong())
                        val sav = if (selectedMedia.savingsPercent > 0) " (−${selectedMedia.savingsPercent}%)" else ""
                        TagPill("${selectedMedia.width}×${selectedMedia.height} px • $fmt$sav", InfoBlueBg, InfoBlue)
                    }
                    OutlinedButton(
                        onClick = {
                            onRunChefAudit(
                                materialType,
                                activeRecipe?.title ?: "Preparação S${"%02d".format(effectiveWeek)}",
                                effectiveWeek,
                                caption
                            )
                        },
                        modifier = Modifier.fillMaxWidth().height(48.dp)
                    ) {
                        Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = InfoBlue)
                        Spacer(Modifier.width(6.dp))
                        Text("Opcional: Anexar Avaliação do Chef IA")
                    }
                } else {
                    Card(colors = CardDefaults.cardColors(containerColor = PccAmberBg)) {
                        Text(
                            text = "Fotografe com CameraX ou escolha da Galeria para habilitar a publicação no Mural.",
                            style = MaterialTheme.typography.bodySmall,
                            color = PccAmber,
                            modifier = Modifier.padding(10.dp)
                        )
                    }
                }

                if (chefAiState is ChefAiUiState.Loading) {
                    CircularProgressIndicator(modifier = Modifier.size(28.dp), color = InfoBlue)
                } else if (chefAiState is ChefAiUiState.AuditReady) {
                    Card(colors = CardDefaults.cardColors(containerColor = PccGreenBg)) {
                        Text(chefAiState.formattedText, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(8.dp))
                    }
                }

                Text("Filtrar Receita por Semana (45 preparações):", style = MaterialTheme.typography.labelMedium)
                Row(modifier = Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    (listOf(0) + (1..24).toList()).forEach { wk ->
                        FilterChip(
                            selected = weekFilter == wk,
                            onClick = { weekFilter = wk },
                            label = { Text(if (wk == 0) "Todas (45)" else "S${"%02d".format(wk)}", fontFamily = JetBrainsMonoFamily) },
                            modifier = Modifier.height(44.dp)
                        )
                    }
                }
                Row(modifier = Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    visibleRecipes.forEach { rec ->
                        FilterChip(
                            selected = selectedRecipeId == rec.id,
                            onClick = { selectedRecipeId = rec.id },
                            label = { Text("S${"%02d".format(rec.weekNumber)} • ${rec.title.take(22)}") },
                            modifier = Modifier.height(48.dp)
                        )
                    }
                }

                Row(modifier = Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf("prato_final", "cortes_mep", "ficha_preenchida", "resumo_esquema").forEach { m ->
                        FilterChip(
                            selected = materialType == m,
                            onClick = { materialType = m },
                            label = { Text(formatMuralMaterialTypePtBr(m)) },
                            modifier = Modifier.height(48.dp)
                        )
                    }
                }

                OutlinedTextField(
                    value = caption,
                    onValueChange = { caption = it },
                    label = { Text("Notas da preparação (ponto, textura, empratamento)") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("mural_quick_caption_input")
                )
                OutlinedTextField(
                    value = mepSummary,
                    onValueChange = { mepSummary = it },
                    label = { Text("PCC / Resumo técnico opcional (ex.: 68 °C • FC 1,15)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSubmitPublicPost(
                        effectiveWeek,
                        materialType,
                        activeRecipe,
                        caption.ifBlank { "Preparação compartilhada com a brigada no Mural Comunitário." },
                        mepSummary,
                        aiFeedbackText
                    )
                },
                enabled = selectedMedia != null && !isPublishing,
                colors = ButtonDefaults.buttonColors(containerColor = SwissRed),
                modifier = Modifier
                    .height(48.dp)
                    .testTag("mural_quick_submit_button")
            ) {
                Text(
                    if (isPublishing) "Publicando…" else stringResource(R.string.mural_quick_publish_submit),
                    fontWeight = FontWeight.Bold
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, modifier = Modifier.height(48.dp)) { Text("Cancelar") }
        }
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun MuralCommentsDialog(
    post: PortfolioPost,
    comments: List<PostComment>,
    currentUserId: String,
    currentAuthorName: String,
    onSubmitComment: (String) -> Unit,
    onDeleteComment: (PostComment) -> Unit,
    onDismiss: () -> Unit
) {
    var commentDraft by rememberSaveable(post.id) { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text("Comentários • ${post.recipeTitle}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(
                    text = "Preparação de ${post.authorName} (${comments.size} avaliações da turma)",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 240.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (comments.isEmpty()) {
                        Card(colors = CardDefaults.cardColors(containerColor = InfoBlueBg), modifier = Modifier.fillMaxWidth()) {
                            Text(
                                text = "Nenhum comentário ainda. Envie um elogio técnico ou sugestão de bancada para ${post.authorName}!",
                                style = MaterialTheme.typography.bodySmall,
                                color = InfoBlue,
                                modifier = Modifier.padding(10.dp)
                            )
                        }
                    } else {
                        comments.forEach { c ->
                            val canDelete = c.userId == currentUserId || post.userId == currentUserId
                            Card(
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 8.dp)
                                ) {
                                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                        Text(
                                            text = "${c.authorName} • ${formatFirestoreTimestampPtBr(c.createdAt)}",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = SwissRed
                                        )
                                        Text(c.text, style = MaterialTheme.typography.bodySmall)
                                    }
                                    if (canDelete) {
                                        IconButton(
                                            onClick = { onDeleteComment(c) },
                                            modifier = Modifier
                                                .size(48.dp)
                                                .testTag("mural_delete_comment_${c.id}")
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Delete,
                                                contentDescription = stringResource(R.string.cd_mural_delete_comment),
                                                tint = PccDanger
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                Text("Feedback rápido de bancada (1 toque):", style = MaterialTheme.typography.labelSmall)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    QUICK_PEER_FEEDBACK_CHIPS.forEachIndexed { idx, chipText ->
                        FilterChip(
                            selected = commentDraft == chipText,
                            onClick = { commentDraft = chipText },
                            label = { Text(chipText, style = MaterialTheme.typography.labelSmall) },
                            modifier = Modifier
                                .height(40.dp)
                                .testTag("mural_quick_comment_chip_$idx")
                        )
                    }
                }

                OutlinedTextField(
                    value = commentDraft,
                    onValueChange = { if (it.length <= 500) commentDraft = it },
                    label = { Text(stringResource(R.string.mural_comments_placeholder)) },
                    supportingText = { Text("Assinando como $currentAuthorName • ${commentDraft.length}/500") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("mural_comment_input")
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (commentDraft.isNotBlank()) {
                        onSubmitComment(commentDraft.trim())
                        commentDraft = ""
                    }
                },
                enabled = commentDraft.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = SwissRed),
                modifier = Modifier
                    .height(48.dp)
                    .testTag("mural_send_comment_button")
            ) {
                Icon(Icons.AutoMirrored.Filled.Send, contentDescription = null)
                Spacer(Modifier.width(6.dp))
                Text(stringResource(R.string.mural_send_comment), fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, modifier = Modifier.height(48.dp)) { Text("Fechar") }
        }
    )
}

@Composable
fun MuralPhotoZoomDialog(
    post: PortfolioPost,
    fullBitmap: Bitmap?,
    onDismiss: () -> Unit
) {
    val bmp = fullBitmap ?: remember(post.thumbnailBase64) {
        MediaCompressor.decodeBase64ToBitmap(post.thumbnailBase64)
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(post.recipeTitle, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text("Brigada: ${post.authorName} • Semana S${"%02d".format(post.weekNumber)}", style = MaterialTheme.typography.labelMedium)
                if (bmp != null) {
                    Image(
                        bitmap = bmp.asImageBitmap(),
                        contentDescription = "Foto em alta resolução de ${post.recipeTitle}",
                        contentScale = ContentScale.FillWidth,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                    )
                }
                if (post.caption.isNotBlank()) {
                    Text(post.caption, style = MaterialTheme.typography.bodyMedium)
                }
                if (post.aiFeedback.isNotBlank()) {
                    Card(colors = CardDefaults.cardColors(containerColor = PccGreenBg)) {
                        Text(post.aiFeedback, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(10.dp))
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss, modifier = Modifier.height(48.dp)) { Text("Fechar") }
        }
    )
}

fun formatMuralMaterialTypePtBr(type: String): String = when (type) {
    "TODOS" -> "Todos"
    "prato_final" -> "Prato Finalizado"
    "cortes_mep" -> "Cortes & Mise en Place"
    "ficha_preenchida" -> "Ficha Técnica"
    "resumo_esquema" -> "Mapa Técnico Visual"
    else -> type
}

private fun formatFirestoreTimestampPtBr(timestamp: Timestamp?): String {
    val sec = timestamp?.seconds ?: return "agora"
    return runCatching {
        val sdf = SimpleDateFormat("dd/MM HH:mm", Locale.forLanguageTag("pt-BR"))
        sdf.format(Date(sec * 1000L))
    }.getOrDefault("agora")
}
