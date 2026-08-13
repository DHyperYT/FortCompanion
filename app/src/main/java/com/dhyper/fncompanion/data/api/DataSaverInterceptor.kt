package com.dhyper.fncompanion.data.api

import android.content.Context
import coil.intercept.Interceptor
import coil.request.CachePolicy
import coil.request.ImageResult
import com.dhyper.fncompanion.data.db.SettingsDao
import com.dhyper.fncompanion.util.NetworkManager

class DataSaverInterceptor(
    private val context: Context,
    private val settingsDao: SettingsDao
) : Interceptor {
    override suspend fun intercept(chain: Interceptor.Chain): ImageResult {
        val settings = settingsDao.getSettingsDirect()
        val dataSaverEnabled = settings?.dataSaverMode ?: false
        
        var request = chain.request
        
        if (dataSaverEnabled && !NetworkManager.isWifi(context)) {
            // If data saver is on and not on wifi, only use cache
            request = request.newBuilder()
                .networkCachePolicy(CachePolicy.DISABLED)
                .build()
        }
        
        return chain.proceed(request)
    }
}
