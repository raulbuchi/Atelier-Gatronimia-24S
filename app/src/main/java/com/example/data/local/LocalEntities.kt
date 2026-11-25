package com.example.data.local

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Bloco de estudo de 80 min de uma semana.
 * - Pausado: [remainingSeconds] é o tempo restante da fase atual e [phaseEndsAtEpochMs] = 0.
 * - Rodando: [phaseEndsAtEpochMs] é o instante (epoch ms) em que a fase atual termina; o restante
 *   é sempre derivado do relógio (ver `TimerMath`), portanto sobrevive à morte do processo.
 */
@Entity(tableName = "study_timer_state")
data class StudyTimerEntity(
    @PrimaryKey val weekNumber: Int,
    val currentPhaseIndex: Int = 0, // 0: Conceito(10m), 1: Visualização(20m), 2: Aplicação(30m), 3: Treino(15m), 4: Síntese(5m)
    val remainingSeconds: Int = 600,
    val isRunning: Boolean = false,
    val completedBlocksCount: Int = 0,
    val updatedAtEpochMs: Long = System.currentTimeMillis(),
    @ColumnInfo(defaultValue = "0") val phaseEndsAtEpochMs: Long = 0L
)

/**
 * Cronômetro paralelo do Modo Cozinha (ex.: Berner Platte 120/90/60/20 min), persistido para
 * continuar contando e alertar mesmo com o app fechado. Mesma semântica de prazo do bloco de estudo.
 */
@Entity(tableName = "kitchen_timers")
data class KitchenTimerEntity(
    @PrimaryKey val id: String,
    val label: String,
    val totalSeconds: Int,
    val remainingSeconds: Int,
    val endsAtEpochMs: Long = 0L,
    val isRunning: Boolean = false,
    val createdAtEpochMs: Long = System.currentTimeMillis()
)

@Entity(tableName = "sm2_progress")
data class Sm2ProgressEntity(
    @PrimaryKey val cardId: String,
    val repetitions: Int = 0,
    val easinessFactor: Double = 2.5,
    val intervalDays: Int = 0,
    val nextReviewEpochDay: Long = 0L,
    val lastGrade: Int = -1,
    val updatedAtEpochMs: Long = System.currentTimeMillis()
)

@Entity(tableName = "mep_sheets")
data class MepSheetEntity(
    @PrimaryKey val recipeId: String,
    val title: String = "",
    val weekNumber: Int,
    val stationBrigade: String = "Saucier",
    val baseServings: Int = 4,
    val targetServings: Int = 4,
    val portionWeightGrams: Double = 220.0,
    val cookingShrinkagePercent: Double = 12.0,
    val targetCmvPercent: Double = 28.0,
    val directLaborCost: Double = 8.0,
    val pccTargetTempC: Int = 70,
    val plateTempC: Int = 55,
    val allergensCsv: String = "",
    val actualBenchYieldGrams: Double = 0.0,
    val executionScore1To5: Int = 4,
    val isCustomSheet: Boolean = false,
    val block1Identification: String = "",
    val block2IngredientsPbPlFc: String = "",
    val block3CookwareBlades: String = "",
    val block4PrePrepCuts: String = "",
    val block5ThermalFlowPcc: String = "",
    val block6PlatingFinishing: String = "",
    val block7HygieneAllergens: String = "",
    val block8Selbstkontrolle: String = "",
    val localPhotoUri: String = "",
    val cloudImageLink: String = "",
    val cloudMediaId: String = "",
    val photoThumbnailBase64: String = "",
    val updatedAtEpochMs: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "mep_ingredient_lines",
    indices = [Index(value = ["recipeId"])]
)
data class MepIngredientLineEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val recipeId: String,
    val sortOrder: Int = 0,
    val name: String,
    val sector: String = "Mercearia Seca",
    val unit: String = "g", // g, ml, kg, L, un
    val baseNetPl: Double,
    val trimLossPercent: Double = 0.0,
    val correctionFactorFc: Double = 1.0,
    val unitCostPerKgOrL: Double = 28.0
)

@Entity(tableName = "shopping_fifo_items")
data class ShoppingFifoEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val weekNumber: Int,
    val ingredientName: String,
    val sector: String, // Hortifrúti, Açougue & Peixaria, Laticínios & Ovos, Mercearia Seca, Especiarias & Fundos
    val unit: String,
    val netQuantityPl: Double,
    val correctionFactorFc: Double,
    val grossQuantityPb: Double,
    val batchDate: String = "",
    val expiryDate: String = "",
    val isPurchased: Boolean = false,
    val fifoPriorityNote: String = "PVPS / PEPS: primeiro que vence, primeiro que sai"
)

@Entity(tableName = "equipment_owned")
data class EquipmentOwnedEntity(
    @PrimaryKey val equipmentId: String,
    val isOwned: Boolean = false
)

@Entity(tableName = "self_evaluations")
data class SelfEvalEntity(
    @PrimaryKey val weekNumber: Int,
    val scoreMiseEnPlace: Int = 4, // 1..5
    val scoreTechnique: Int = 4,   // 1..5
    val scoreHygienePcc: Int = 5,  // 1..5
    val scoreSensory: Int = 4,     // 1..5
    val erkenntnisse: String = "",
    val offenePunkte: String = "",
    val updatedAtEpochMs: Long = System.currentTimeMillis()
)

/**
 * Estado global do aluno no curso (linha única, id = 1).
 * - [startEpochDay]: dia de início do curso (LocalDate.toEpochDay), base da "semana do calendário".
 * - [focusedWeekNumber]: última semana em foco no app (restaurada ao reabrir).
 */
@Entity(tableName = "course_progress")
data class CourseProgressEntity(
    @PrimaryKey val id: Int = SINGLETON_ID,
    val startEpochDay: Long,
    val focusedWeekNumber: Int = 1,
    val updatedAtEpochMs: Long = System.currentTimeMillis()
) {
    companion object {
        const val SINGLETON_ID = 1
    }
}

/** Semana formalmente concluída pelo aluno (após cumprir os critérios da semana). */
@Entity(tableName = "week_completions")
data class WeekCompletionEntity(
    @PrimaryKey val weekNumber: Int,
    val completedAtEpochMs: Long = System.currentTimeMillis()
)

/** Checklist dos entregáveis de portfólio de cada ciclo (`cycles[].portfolio_deliverables`). */
@Entity(
    tableName = "cycle_deliverable_checks",
    primaryKeys = ["cycleNumber", "deliverableIndex"]
)
data class CycleDeliverableCheckEntity(
    val cycleNumber: Int,
    val deliverableIndex: Int,
    val isDone: Boolean = false,
    val updatedAtEpochMs: Long = System.currentTimeMillis()
)
