package io.wenk8.app

import android.app.Application
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

class Wenk8App : Application() {
    val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    lateinit var library: LibraryRepo

    override fun onCreate() {
        super.onCreate()
        library = LibraryRepo(this, appScope)
    }
}
