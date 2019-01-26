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
        registerNewHandler()
    }

    fun executeTransaction(context: Context, transaction: DatabaseTransaction) {
        while (!initialized) {
        }

        val message = {
            val db = AppDatabase.getDatabase(context)
            transaction.invoke(db)
            db.close()
        }

        val success = handler.post(message)
        if (!success) {
            Log.w(TAG, "Could not schedule database transaction")
            registerNewHandler(message)
        }
    }

    private fun registerNewHandler(message: () -> Unit = {}) {
        thread {
            Looper.prepare()
            looper = Looper.myLooper()!!
            handler = Handler(looper)
            initialized = true
            handler.post(message)
            Looper.loop()
        }
    }

    fun destroy() {
        looper.quitSafely()
    }
}