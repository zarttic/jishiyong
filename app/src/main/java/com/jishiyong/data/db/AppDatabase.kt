package com.jishiyong.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.sqlite.db.SupportSQLiteDatabase
import com.jishiyong.data.db.converter.DateConverter
import com.jishiyong.data.db.converter.ListConverter
import com.jishiyong.data.db.dao.ItemDao
import com.jishiyong.data.db.entity.Item
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.LocalDate

@Database(
    entities = [Item::class],
    version = 1,
    exportSchema = true
)
@TypeConverters(DateConverter::class, ListConverter::class)
abstract class AppDatabase : RoomDatabase() {

    abstract fun itemDao(): ItemDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "jishiyong_db"
                )
                    .addCallback(DatabaseCallback())
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }

    /**
     * 数据库回调 - 首次创建时可插入示例数据
     */
    private class DatabaseCallback : Callback() {
        override fun onCreate(db: SupportSQLiteDatabase) {
            super.onCreate(db)
            INSTANCE?.let { database ->
                CoroutineScope(Dispatchers.IO).launch {
                    populateDatabase(database.itemDao())
                }
            }
        }

        suspend fun populateDatabase(itemDao: ItemDao) {
            // 插入一些示例数据方便用户了解 App 用法
            val today = LocalDate.now()
            val sampleItems = listOf(
                Item(
                    name = "牛奶",
                    category = com.jishiyong.data.db.entity.ItemCategory.DRINK,
                    purchaseDate = today.minusDays(3),
                    expirationDate = today.plusDays(5),
                    note = "冰箱里的纯牛奶",
                    quantity = 2,
                    reminderDays = listOf(7, 3, 1)
                ),
                Item(
                    name = "洗面奶",
                    category = com.jishiyong.data.db.entity.ItemCategory.COSMETICS,
                    purchaseDate = today.minusDays(30),
                    expirationDate = today.plusDays(60),
                    note = "开盖后12个月内用完",
                    quantity = 1,
                    reminderDays = listOf(14, 7, 3)
                ),
                Item(
                    name = "感冒药",
                    category = com.jishiyong.data.db.entity.ItemCategory.MEDICINE,
                    purchaseDate = today.minusDays(10),
                    expirationDate = today.plusDays(25),
                    note = "放在药箱里",
                    quantity = 1,
                    reminderDays = listOf(30, 14, 7, 3)
                )
            )
            itemDao.insertAll(sampleItems)
        }
    }
}
