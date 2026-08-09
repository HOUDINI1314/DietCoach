package com.dietcoach.app

import android.content.Context
import com.dietcoach.app.ai.DashScopeClient
import com.dietcoach.app.data.db.AppDatabase
import com.dietcoach.app.data.repo.DietRepository
import com.dietcoach.app.data.secrets.ApiKeyStore

class AppContainer(context: Context) {
    private val appContext = context.applicationContext
    val apiKeyStore = ApiKeyStore(appContext)
    private val db = AppDatabase.get(appContext)
    private val dashScope = DashScopeClient(apiKeyStore)
    val repository = DietRepository(db, dashScope)
}
