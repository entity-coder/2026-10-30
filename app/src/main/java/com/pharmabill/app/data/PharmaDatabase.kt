package com.pharmabill.app.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [Medicine::class, Bill::class, BillLine::class,
        StockMovement::class],
    version = 1,
    exportSchema = false
)
abstract class PharmaDatabase : RoomDatabase() {
    abstract fun dao(): PharmaDao

    companion object {
        @Volatile
        private var instance: PharmaDatabase? = null

        fun get(context: Context): PharmaDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    PharmaDatabase::class.java,
                    "pharmabill.db"
                ).fallbackToDestructiveMigration().build().also { instance = it }
            }
    }
}
