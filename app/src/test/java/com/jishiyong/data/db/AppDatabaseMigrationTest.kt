package com.jishiyong.data.db

import androidx.room.Room
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.core.app.ApplicationProvider
import com.jishiyong.data.db.entity.AgentMemoryEntity
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class AppDatabaseMigrationTest {

    private val context = ApplicationProvider.getApplicationContext<android.content.Context>()
    private val databaseName = "migration-test.db"

    @After
    fun tearDown() {
        context.deleteDatabase(databaseName)
    }

    @Test
    fun migrationFrom1To2PreservesItemsAndEnablesAgentMemoryFts() = runTest {
        createVersion1Database()

        val database = Room.databaseBuilder(context, AppDatabase::class.java, databaseName)
            .addMigrations(AppDatabase.MIGRATION_1_2)
            .build()

        try {
            val item = database.itemDao().getItemById(1)
            assertEquals("蒙牛纯牛奶", item?.name)
            assertEquals(3, item?.quantity)

            database.agentMemoryDao().replaceAllMemories(
                listOf(
                    AgentMemoryEntity(
                        type = "ALIAS",
                        key = "常买奶",
                        valueJson = """{"alias":"常买奶","canonical_name":"蒙牛纯牛奶"}""",
                        searchableText = "常买奶 蒙牛纯牛奶 蒙牛 牛奶",
                        confidence = 0.8f,
                        hits = 1,
                        updatedAt = 1_000L
                    )
                )
            )

            val memories = database.agentMemoryDao().searchMemories("\"蒙牛\"", limit = 5)
            assertEquals(1, memories.size)
            assertTrue(memories.single().searchableText.contains("蒙牛纯牛奶"))
        } finally {
            database.close()
        }
    }

    private fun createVersion1Database() {
        val helper = FrameworkSQLiteOpenHelperFactory().create(
            androidx.sqlite.db.SupportSQLiteOpenHelper.Configuration.builder(context)
                .name(databaseName)
                .callback(
                    object : androidx.sqlite.db.SupportSQLiteOpenHelper.Callback(1) {
                        override fun onCreate(db: SupportSQLiteDatabase) {
                            db.execSQL(
                                """
                                CREATE TABLE IF NOT EXISTS `items` (
                                    `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                                    `name` TEXT NOT NULL,
                                    `category` TEXT NOT NULL,
                                    `purchaseDate` TEXT NOT NULL,
                                    `expirationDate` TEXT NOT NULL,
                                    `note` TEXT NOT NULL,
                                    `imagePaths` TEXT NOT NULL,
                                    `quantity` INTEGER NOT NULL,
                                    `usedQuantity` INTEGER NOT NULL,
                                    `reminderDays` TEXT NOT NULL,
                                    `isConsumed` INTEGER NOT NULL,
                                    `consumeType` TEXT,
                                    `createdAt` INTEGER NOT NULL,
                                    `updatedAt` INTEGER NOT NULL
                                )
                                """.trimIndent()
                            )
                            db.execSQL(
                                """
                                INSERT INTO `items` (
                                    `id`,
                                    `name`,
                                    `category`,
                                    `purchaseDate`,
                                    `expirationDate`,
                                    `note`,
                                    `imagePaths`,
                                    `quantity`,
                                    `usedQuantity`,
                                    `reminderDays`,
                                    `isConsumed`,
                                    `consumeType`,
                                    `createdAt`,
                                    `updatedAt`
                                ) VALUES (
                                    1,
                                    '蒙牛纯牛奶',
                                    'DRINK',
                                    '2026-06-01',
                                    '2026-06-12',
                                    '冰箱',
                                    '[]',
                                    3,
                                    0,
                                    '[7,3,1]',
                                    0,
                                    NULL,
                                    100,
                                    100
                                )
                                """.trimIndent()
                            )
                        }

                        override fun onUpgrade(
                            db: SupportSQLiteDatabase,
                            oldVersion: Int,
                            newVersion: Int
                        ) = Unit
                    }
                )
                .build()
        )

        helper.writableDatabase.close()
        helper.close()
    }
}
