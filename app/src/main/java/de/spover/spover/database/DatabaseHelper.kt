package de.spover.spover.database

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import kotlin.concurrent.thread

typealias DatabaseTransaction = (AppDatabase) -> Unit

class DatabaseHelper private constructor() {

    companion object {
        val INSTANCE = DatabaseHelper()
        val TAG = DatabaseHelper::class.simpleName
    }

    private lateinit var handler: Handler
    private lateinit var looper: Looper

    private var initialized = false

    init {
        thread {
            Looper.prepare()
            looper = Looper.myLooper()!!
            handler = Handler(looper)
            initialized = true
            Looper.loop()
        }
    }

    fun executeTransaction(context: Context, transaction: DatabaseTransaction) {
        while (!initialized) {
        }

        val success = handler.post {
            val db = AppDatabase.getDatabase(context)
            transaction.invoke(db)
            db.close()
        }
        if (!success) {
            Log.w(TAG, "Could not schedule database transaction")
        }
    }

    fun destroy() {
        looper.quitSafely()
    }
}