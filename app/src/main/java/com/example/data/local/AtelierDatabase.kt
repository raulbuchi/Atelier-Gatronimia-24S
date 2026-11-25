package com.example.data.local

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.Transaction
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.flow.Flow

/**
 * Versão atual do schema. Ao incrementar: crie a Migration correspondente, registre-a em
 * [AtelierDatabase.ALL_MIGRATIONS], faça o build (gera `app/schemas/.../<versão>.json`) e
 * cubra a migração em `AtelierDatabaseMigrationTest`.
 */
const val ATELIER_DB_VERSION = 5

@Dao
interface AtelierDao {

    // 1. Study Timer 80 min
    @Query("SELECT * FROM study_timer_state")
    fun observeAllTimers(): Flow<List<StudyTimerEntity>>

    @Query("SELECT * FROM study_timer_state")
    suspend fun getAllTimersOnce(): List<StudyTimerEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertTimer(entity: StudyTimerEntity)

    @Query("SELECT * FROM study_timer_state WHERE weekNumber = :weekNumber")
    suspend fun getTimerOnce(weekNumber: Int): StudyTimerEntity?

    // 1b. Cronômetros paralelos do Modo Cozinha
    @Query("SELECT * FROM kitchen_timers ORDER BY createdAtEpochMs ASC")
    fun observeKitchenTimers(): Flow<List<KitchenTimerEntity>>

    @Query("SELECT * FROM kitchen_timers")
    suspend fun getAllKitchenTimersOnce(): List<KitchenTimerEntity>

    @Query("SELECT * FROM kitchen_timers WHERE id = :id")
    suspend fun getKitchenTimerOnce(id: String): KitchenTimerEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertKitchenTimer(entity: KitchenTimerEntity)

    @Query("DELETE FROM kitchen_timers WHERE id = :id")
    suspend fun deleteKitchenTimer(id: String)

    // 2. SM-2 Spaced Repetition
    @Query("SELECT * FROM sm2_progress")
    fun observeSm2Progress(): Flow<List<Sm2ProgressEntity>>

    @Query("SELECT * FROM sm2_progress")
    suspend fun getAllSm2Once(): List<Sm2ProgressEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertSm2(entity: Sm2ProgressEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAllSm2(entities: List<Sm2ProgressEntity>)

    // 3. Ficha MEP Digital & Calculadora (8 Blocos + Linhas de Insumos PB/PL/FC)
    @Query("SELECT * FROM mep_sheets ORDER BY updatedAtEpochMs DESC")
    fun observeAllMepSheets(): Flow<List<MepSheetEntity>>

    @Query("SELECT * FROM mep_sheets ORDER BY updatedAtEpochMs DESC")
    suspend fun getAllMepSheetsOnce(): List<MepSheetEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertMepSheet(sheet: MepSheetEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAllMepSheets(sheets: List<MepSheetEntity>)

    @Query("DELETE FROM mep_sheets WHERE recipeId = :recipeId")
    suspend fun deleteMepSheetById(recipeId: String)

    @Query("SELECT * FROM mep_ingredient_lines ORDER BY recipeId ASC, sortOrder ASC, id ASC")
    fun observeAllMepLines(): Flow<List<MepIngredientLineEntity>>

    @Query("SELECT * FROM mep_ingredient_lines ORDER BY recipeId ASC, sortOrder ASC, id ASC")
    suspend fun getAllMepLinesOnce(): List<MepIngredientLineEntity>

    @Query("SELECT * FROM mep_ingredient_lines WHERE recipeId = :recipeId ORDER BY sortOrder ASC, id ASC")
    suspend fun getMepLinesForSheetOnce(recipeId: String): List<MepIngredientLineEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMepLines(lines: List<MepIngredientLineEntity>)

    @Query("DELETE FROM mep_ingredient_lines WHERE recipeId = :recipeId")
    suspend fun deleteMepLinesForSheet(recipeId: String)

    @Transaction
    suspend fun saveMepSheetWithLines(
        sheet: MepSheetEntity,
        lines: List<MepIngredientLineEntity>
    ) {
        upsertMepSheet(sheet)
        deleteMepLinesForSheet(sheet.recipeId)
        if (lines.isNotEmpty()) {
            val normalized = lines.mapIndexed { idx, line ->
                line.copy(id = 0L, recipeId = sheet.recipeId, sortOrder = idx)
            }
            insertMepLines(normalized)
        }
    }

    @Transaction
    suspend fun deleteMepSheetComplete(recipeId: String) {
        deleteMepLinesForSheet(recipeId)
        deleteMepSheetById(recipeId)
    }

    // 4. Shopping & FIFO Pantry
    @Query("SELECT * FROM shopping_fifo_items ORDER BY sector ASC, expiryDate ASC, ingredientName ASC")
    fun observeAllShoppingItems(): Flow<List<ShoppingFifoEntity>>

    @Query("SELECT * FROM shopping_fifo_items")
    suspend fun getAllShoppingItemsOnce(): List<ShoppingFifoEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertShoppingItem(item: ShoppingFifoEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertShoppingItems(items: List<ShoppingFifoEntity>)

    @Query("DELETE FROM shopping_fifo_items WHERE weekNumber = :weekNumber")
    suspend fun clearShoppingForWeek(weekNumber: Int)

    @Query("DELETE FROM shopping_fifo_items WHERE id = :id")
    suspend fun deleteShoppingItem(id: Long)

    // 5. Equipment Inventory
    @Query("SELECT * FROM equipment_owned")
    fun observeOwnedEquipment(): Flow<List<EquipmentOwnedEntity>>

    @Query("SELECT * FROM equipment_owned")
    suspend fun getAllEquipmentOnce(): List<EquipmentOwnedEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertEquipmentOwned(entity: EquipmentOwnedEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAllEquipmentOwned(entities: List<EquipmentOwnedEntity>)

    // 6. Selbstkontrolle Self-Evaluation
    @Query("SELECT * FROM self_evaluations ORDER BY weekNumber ASC")
    fun observeSelfEvaluations(): Flow<List<SelfEvalEntity>>

    @Query("SELECT * FROM self_evaluations")
    suspend fun getAllSelfEvaluationsOnce(): List<SelfEvalEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertSelfEvaluation(entity: SelfEvalEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAllSelfEvaluations(entities: List<SelfEvalEntity>)

    // 7. Progressão do Curso (semana em foco, data de início, semanas concluídas, entregáveis)
    @Query("SELECT * FROM course_progress WHERE id = 1")
    fun observeCourseProgress(): Flow<CourseProgressEntity?>

    @Query("SELECT * FROM course_progress WHERE id = 1")
    suspend fun getCourseProgressOnce(): CourseProgressEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertCourseProgress(entity: CourseProgressEntity)

    @Query("SELECT * FROM week_completions ORDER BY weekNumber ASC")
    fun observeWeekCompletions(): Flow<List<WeekCompletionEntity>>

    @Query("SELECT * FROM week_completions")
    suspend fun getAllWeekCompletionsOnce(): List<WeekCompletionEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertWeekCompletion(entity: WeekCompletionEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAllWeekCompletions(entities: List<WeekCompletionEntity>)

    @Query("DELETE FROM week_completions WHERE weekNumber = :weekNumber")
    suspend fun deleteWeekCompletion(weekNumber: Int)

    @Query("SELECT * FROM cycle_deliverable_checks ORDER BY cycleNumber ASC, deliverableIndex ASC")
    fun observeCycleDeliverableChecks(): Flow<List<CycleDeliverableCheckEntity>>

    @Query("SELECT * FROM cycle_deliverable_checks")
    suspend fun getAllCycleDeliverableChecksOnce(): List<CycleDeliverableCheckEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertCycleDeliverableCheck(entity: CycleDeliverableCheckEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAllCycleDeliverableChecks(entities: List<CycleDeliverableCheckEntity>)
}

@Database(
    entities = [
        StudyTimerEntity::class,
        Sm2ProgressEntity::class,
        MepSheetEntity::class,
        MepIngredientLineEntity::class,
        ShoppingFifoEntity::class,
        EquipmentOwnedEntity::class,
        SelfEvalEntity::class,
        CourseProgressEntity::class,
        WeekCompletionEntity::class,
        CycleDeliverableCheckEntity::class,
        KitchenTimerEntity::class
    ],
    version = ATELIER_DB_VERSION,
    exportSchema = true
)
abstract class AtelierDatabase : RoomDatabase() {
    abstract fun atelierDao(): AtelierDao

    companion object {
        @Volatile
        private var INSTANCE: AtelierDatabase? = null

        const val DATABASE_NAME = "atelier_gastronomia_24s_v4.db"

        /**
         * Versões anteriores ao schema exportado (sem histórico de migração possível).
         * Somente essas podem ser recriadas do zero; a partir da v3 toda mudança de schema
         * EXIGE uma [Migration] coberta por `AtelierDatabaseMigrationTest`.
         */
        private val LEGACY_UNMIGRATABLE_VERSIONS = intArrayOf(1, 2)

        /** Todas as migrações suportadas, em ordem. Adicione aqui cada nova migração. */
        val ALL_MIGRATIONS: Array<Migration> by lazy { arrayOf(MIGRATION_3_4, MIGRATION_4_5) }

        /**
         * v4 → v5: timers baseados em prazo (sobrevivem ao processo) + cronômetros da cozinha persistidos.
         * Blocos de estudo que estavam "rodando" na v4 não tinham prazo gravado: são pausados mantendo
         * o tempo restante.
         */
        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "ALTER TABLE `study_timer_state` ADD COLUMN `phaseEndsAtEpochMs` INTEGER NOT NULL DEFAULT 0"
                )
                db.execSQL("UPDATE `study_timer_state` SET `isRunning` = 0")
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `kitchen_timers` (" +
                        "`id` TEXT NOT NULL, `label` TEXT NOT NULL, `totalSeconds` INTEGER NOT NULL, " +
                        "`remainingSeconds` INTEGER NOT NULL, `endsAtEpochMs` INTEGER NOT NULL, " +
                        "`isRunning` INTEGER NOT NULL, `createdAtEpochMs` INTEGER NOT NULL, " +
                        "PRIMARY KEY(`id`))"
                )
            }
        }

        /** v3 → v4: tabelas de progressão do curso (preserva todo o progresso já existente). */
        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `course_progress` (" +
                        "`id` INTEGER NOT NULL, `startEpochDay` INTEGER NOT NULL, " +
                        "`focusedWeekNumber` INTEGER NOT NULL, `updatedAtEpochMs` INTEGER NOT NULL, " +
                        "PRIMARY KEY(`id`))"
                )
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `week_completions` (" +
                        "`weekNumber` INTEGER NOT NULL, `completedAtEpochMs` INTEGER NOT NULL, " +
                        "PRIMARY KEY(`weekNumber`))"
                )
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `cycle_deliverable_checks` (" +
                        "`cycleNumber` INTEGER NOT NULL, `deliverableIndex` INTEGER NOT NULL, " +
                        "`isDone` INTEGER NOT NULL, `updatedAtEpochMs` INTEGER NOT NULL, " +
                        "PRIMARY KEY(`cycleNumber`, `deliverableIndex`))"
                )
            }
        }

        fun getInstance(context: Context): AtelierDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AtelierDatabase::class.java,
                    DATABASE_NAME
                )
                    .addMigrations(*ALL_MIGRATIONS)
                    .fallbackToDestructiveMigrationFrom(
                        dropAllTables = true,
                        *LEGACY_UNMIGRATABLE_VERSIONS
                    )
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }
}
