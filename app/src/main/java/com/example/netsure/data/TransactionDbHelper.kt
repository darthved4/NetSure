package com.example.netsure.data

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

/**
 * Lightweight SQLite helper that stores detected UPI transactions locally.
 */
class TransactionDbHelper(context: Context) :
    SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        private const val DATABASE_NAME = "netsure_transactions.db"
        private const val DATABASE_VERSION = 1

        private const val TABLE = "transactions"
        private const val COL_ID = "id"
        private const val COL_AMOUNT = "amount"
        private const val COL_TARGET = "target"
        private const val COL_REF = "reference_number"
        private const val COL_TIMESTAMP = "timestamp_ms"
    }

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE $TABLE (
                $COL_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COL_AMOUNT TEXT NOT NULL,
                $COL_TARGET TEXT NOT NULL,
                $COL_REF TEXT NOT NULL UNIQUE,
                $COL_TIMESTAMP INTEGER NOT NULL
            )
            """.trimIndent()
        )
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS $TABLE")
        onCreate(db)
    }

    /**
     * Insert a transaction. Returns the row id, or -1 if the reference number
     * already exists (duplicate prevention).
     */
    fun insertTransaction(txn: Transaction): Long {
        val db = writableDatabase
        val values = ContentValues().apply {
            put(COL_AMOUNT, txn.amount)
            put(COL_TARGET, txn.target)
            put(COL_REF, txn.referenceNumber)
            put(COL_TIMESTAMP, txn.timestampMs)
        }
        return try {
            db.insertOrThrow(TABLE, null, values)
        } catch (e: Exception) {
            // UNIQUE constraint on reference_number prevents duplicates
            -1L
        }
    }

    /**
     * Check whether a transaction with the given reference number already exists.
     */
    fun existsByRef(refNumber: String): Boolean {
        val db = readableDatabase
        val cursor = db.rawQuery(
            "SELECT 1 FROM $TABLE WHERE $COL_REF = ? LIMIT 1",
            arrayOf(refNumber)
        )
        val exists = cursor.moveToFirst()
        cursor.close()
        return exists
    }
}
