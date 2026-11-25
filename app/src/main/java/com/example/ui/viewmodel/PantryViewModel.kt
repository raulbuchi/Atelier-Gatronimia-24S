package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.core.session.CourseSession
import com.example.core.session.UserMessages
import com.example.data.curriculum.SwissmilkAndBaseRecipesCatalog
import com.example.data.local.EquipmentOwnedEntity
import com.example.data.local.LocalKitchenRepository
import com.example.data.local.ShoppingFifoEntity
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** Compras (PB = PL × FC, despensa PVPS/PEPS) e inventário de equipamentos. Tela: Compras & Equipamentos. */
class PantryViewModel(
    private val localRepo: LocalKitchenRepository,
    private val session: CourseSession,
    private val messages: UserMessages
) : ViewModel() {

    val selectedWeekNumber: StateFlow<Int> = session.selectedWeek

    fun selectWeek(week: Int) = session.selectWeek(week)

    val shoppingItems: StateFlow<List<ShoppingFifoEntity>> = localRepo.shoppingItems
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val ownedEquipmentIds: StateFlow<Set<String>> = localRepo.ownedEquipment
        .map { list -> list.filter { it.isOwned }.map { it.equipmentId }.toSet() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptySet())

    /** Requisição consolidada da semana: soma PL e PB de todas as receitas, agrupando por setor/insumo/unidade. */
    fun generateShoppingForWeek(weekNumber: Int, portionsMultiplier: Double = 1.0) {
        val scale = portionsMultiplier.coerceIn(0.25, 20.0)
        val aggregated = mutableMapOf<String, ShoppingFifoEntity>()
        SwissmilkAndBaseRecipesCatalog.recipesForWeek(weekNumber).forEach { recipe ->
            recipe.ingredients.forEach { ing ->
                val key = "${ing.sector}|${ing.name.lowercase()}|${ing.unit}"
                val pl = ing.netAmountPl * scale
                val pb = pl * ing.correctionFactorFc
                val prev = aggregated[key]
                aggregated[key] = prev?.copy(
                    netQuantityPl = prev.netQuantityPl + pl,
                    grossQuantityPb = prev.grossQuantityPb + pb
                ) ?: ShoppingFifoEntity(
                    weekNumber = weekNumber,
                    ingredientName = ing.name,
                    sector = ing.sector,
                    unit = ing.unit,
                    netQuantityPl = pl,
                    correctionFactorFc = ing.correctionFactorFc,
                    grossQuantityPb = pb,
                    batchDate = "Lote Sem. $weekNumber",
                    expiryDate = if (ing.sector == "Açougue & Peixaria") "+2 dias (≤ 4 °C)" else "+7 dias (PVPS/PEPS)"
                )
            }
        }
        viewModelScope.launch {
            localRepo.replaceWeekShopping(weekNumber, aggregated.values.toList())
            messages.show("Requisição de compras da Semana $weekNumber gerada (PB = PL × FC • PVPS/PEPS).")
        }
    }

    fun toggleShoppingPurchased(item: ShoppingFifoEntity) {
        viewModelScope.launch { localRepo.upsertShoppingItem(item.copy(isPurchased = !item.isPurchased)) }
    }

    fun updateShoppingFifoDates(item: ShoppingFifoEntity, batchDate: String, expiryDate: String) {
        viewModelScope.launch { localRepo.upsertShoppingItem(item.copy(batchDate = batchDate, expiryDate = expiryDate)) }
    }

    fun toggleEquipmentOwned(equipmentId: String) {
        val currentlyOwned = equipmentId in ownedEquipmentIds.value
        viewModelScope.launch {
            localRepo.upsertEquipment(EquipmentOwnedEntity(equipmentId = equipmentId, isOwned = !currentlyOwned))
        }
    }
}
