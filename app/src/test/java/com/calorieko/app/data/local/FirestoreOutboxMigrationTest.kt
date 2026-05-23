package com.calorieko.app.data.local

import android.app.Application
import android.content.Context
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteOpenHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class, sdk = [35])
class FirestoreOutboxMigrationTest {
    private lateinit var context: Context
    private lateinit var helper: SupportSQLiteOpenHelper

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        context.deleteDatabase(DB_NAME)
        helper = FrameworkSQLiteOpenHelperFactory().create(
            SupportSQLiteOpenHelper.Configuration.builder(context)
                .name(DB_NAME)
                .callback(object : SupportSQLiteOpenHelper.Callback(28) {
                    override fun onCreate(db: SupportSQLiteDatabase) {
                        createVersion28Tables(db)
                    }

                    override fun onUpgrade(db: SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) = Unit
                })
                .build()
        )
    }

    @After
    fun tearDown() {
        helper.close()
        context.deleteDatabase(DB_NAME)
    }

    @Test
    fun migrationCreatesOutboxRemoteIdsIndexesAndBackfillsUnsyncedRows() = runBlocking {
        val db = helper.writableDatabase
        seedVersion28Rows(db)

        AppDatabase.MIGRATION_28_29.migrate(db)

        assertTrue(tableNames(db).contains("firestore_outbox"))
        assertTrue(columnNames(db, "activity_log_table").contains("remote_id"))
        assertTrue(columnNames(db, "meal_log_table").contains("remote_id"))
        assertTrue(columnNames(db, "meal_log_item_table").contains("remote_id"))
        assertTrue(indexNames(db, "firestore_outbox").contains("index_firestore_outbox_uid_state_created_at"))
        assertTrue(indexNames(db, "activity_log_table").contains("index_activity_log_table_uid_remote_id"))
        assertTrue(indexNames(db, "meal_log_table").contains("index_meal_log_table_uid_remote_id"))
        assertTrue(indexNames(db, "meal_log_item_table").contains("index_meal_log_item_table_meal_log_id_remote_id"))

        assertEquals("10", stringScalar(db, "SELECT remote_id FROM activity_log_table WHERE id = 10"))
        assertEquals("20", stringScalar(db, "SELECT remote_id FROM meal_log_table WHERE meal_log_id = 20"))
        assertEquals("30", stringScalar(db, "SELECT remote_id FROM meal_log_item_table WHERE meal_log_item_id = 30"))
        assertEquals(3, longScalar(db, "SELECT COUNT(*) FROM firestore_outbox"))
        assertEquals(1, longScalar(db, "SELECT COUNT(*) FROM firestore_outbox WHERE entity_type = 'ACTIVITY_LOG' AND entity_key = '10'"))
        assertEquals(1, longScalar(db, "SELECT COUNT(*) FROM firestore_outbox WHERE entity_type = 'MEAL_LOG' AND entity_key = '20'"))
        assertEquals(1, longScalar(db, "SELECT COUNT(*) FROM firestore_outbox WHERE entity_type = 'WEIGHT_LOG' AND entity_key = '1000'"))
        assertEquals(0, longScalar(db, "SELECT COUNT(*) FROM firestore_outbox WHERE entity_key IN ('11', '21', '2000')"))
    }

    private fun createVersion28Tables(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE activity_log_table (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                uid TEXT NOT NULL,
                sync_status INTEGER NOT NULL DEFAULT 0,
                updated_at INTEGER NOT NULL DEFAULT 0
            )
            """.trimIndent()
        )
        db.execSQL(
            """
            CREATE TABLE meal_log_table (
                meal_log_id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                uid TEXT NOT NULL,
                sync_status INTEGER NOT NULL DEFAULT 0,
                updated_at INTEGER NOT NULL DEFAULT 0
            )
            """.trimIndent()
        )
        db.execSQL(
            """
            CREATE TABLE meal_log_item_table (
                meal_log_item_id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                meal_log_id INTEGER NOT NULL
            )
            """.trimIndent()
        )
        db.execSQL(
            """
            CREATE TABLE weight_log_table (
                uid TEXT NOT NULL,
                timestamp INTEGER NOT NULL,
                sync_status INTEGER NOT NULL DEFAULT 0,
                updated_at INTEGER NOT NULL DEFAULT 0,
                PRIMARY KEY(uid, timestamp)
            )
            """.trimIndent()
        )
    }

    private fun seedVersion28Rows(db: SupportSQLiteDatabase) {
        db.execSQL("INSERT INTO activity_log_table(id, uid, sync_status, updated_at) VALUES (10, 'uid', 0, 100)")
        db.execSQL("INSERT INTO activity_log_table(id, uid, sync_status, updated_at) VALUES (11, 'uid', 1, 110)")
        db.execSQL("INSERT INTO meal_log_table(meal_log_id, uid, sync_status, updated_at) VALUES (20, 'uid', 0, 200)")
        db.execSQL("INSERT INTO meal_log_table(meal_log_id, uid, sync_status, updated_at) VALUES (21, 'uid', 1, 210)")
        db.execSQL("INSERT INTO meal_log_item_table(meal_log_item_id, meal_log_id) VALUES (30, 20)")
        db.execSQL("INSERT INTO weight_log_table(uid, timestamp, sync_status, updated_at) VALUES ('uid', 1000, 0, 300)")
        db.execSQL("INSERT INTO weight_log_table(uid, timestamp, sync_status, updated_at) VALUES ('uid', 2000, 1, 310)")
    }

    private fun tableNames(db: SupportSQLiteDatabase): Set<String> =
        stringSet(db, "SELECT name FROM sqlite_master WHERE type = 'table'")

    private fun columnNames(db: SupportSQLiteDatabase, table: String): Set<String> =
        stringSet(db, "PRAGMA table_info($table)", column = "name")

    private fun indexNames(db: SupportSQLiteDatabase, table: String): Set<String> =
        stringSet(db, "PRAGMA index_list($table)", column = "name")

    private fun stringSet(db: SupportSQLiteDatabase, sql: String, column: String? = null): Set<String> =
        db.query(sql).use { cursor ->
            val index = column?.let(cursor::getColumnIndex) ?: 0
            buildSet {
                while (cursor.moveToNext()) {
                    add(cursor.getString(index))
                }
            }
        }

    private fun longScalar(db: SupportSQLiteDatabase, sql: String): Long =
        db.query(sql).use { cursor ->
            cursor.moveToFirst()
            cursor.getLong(0)
        }

    private fun stringScalar(db: SupportSQLiteDatabase, sql: String): String =
        db.query(sql).use { cursor ->
            cursor.moveToFirst()
            cursor.getString(0)
        }

    private companion object {
        const val DB_NAME = "firestore-migration-test.db"
    }
}
