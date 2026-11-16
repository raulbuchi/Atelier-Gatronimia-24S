package com.example.data.local

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import kotlinx.coroutines.flow.Flow

@Dao
interface AtelierDao {

    // 1. Study Timer 80 min
    @Query("SELECT * FROM study_timer_state")
    fun observeAllTimers(): Flow<List<StudyTimerEntity>>

    @Query("SELECT * FROM study_timer_state")
    suspend fun getAllTimersOnce(): List<StudyTimerEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertTimer(entity: StudyTimerEntity)

    // 2. SM-2 Spaced Repetition
    @Query("SELECT * FROM sm2_progress")
    fun observeSm2Progress(): Flow<List<Sm2ProgressEntity>>

    @Query("SELECT * FROM sm2_progress")
    suspend fun getAllSm2Once(): List<Sm2ProgressEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertSm2(entity: Sm2ProgressEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAllSm2(entities: List<Sm2ProgressEntity>)

    // 3. Ficha MEP Digital (8 Blocos)
    @Query("SELECT * FROM mep_sheets")
    fun observeAllMepSheets(): Flow<List<MepSheetEntity>>

    @Query("SELECT * FROM mep_sheets")
    suspend fun getAllMepSheetsOnce(): List<MepSheetEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertMepSheet(sheet: MepSheetEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAllMepSheets(sheets: List<MepSheetEntity>)

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
}

@Database(
    entities = [
        StudyTimerEntity::class,
        Sm2ProgressEntity::class,
        MepSheetEntity::class,
        ShoppingFifoEntity::class,
        EquipmentOwnedEntity::class,
        SelfEvalEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AtelierDatabase : RoomDatabase() {
    abstract fun atelierDao(): AtelierDao

    companion object {
        @Volatile
        private var INSTANCE: AtelierDatabase? = null

        fun getInstance(context: Context): AtelierDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AtelierDatabase::class.java,
                    "atelier_gastronomia_24s_v4.db"
                ).fallbackToDestructiveMigration().build().also { INSTANCE = it }
            }
        }
    }
}
