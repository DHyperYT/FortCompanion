package com.dhyper.fncompanion.data.api

import com.dhyper.fncompanion.data.api.AuthInterceptor
import com.dhyper.fncompanion.data.repository.AuthRepository
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.util.concurrent.TimeUnit

object ApiClient {
    private val moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()

    private var authRepository: AuthRepository? = null
    private var cache: okhttp3.Cache? = null

    fun init(repository: AuthRepository, context: android.content.Context) {
        authRepository = repository
        val cacheSize = (10 * 1024 * 1024).toLong() // 10 MB
        cache = okhttp3.Cache(context.cacheDir, cacheSize)
    }

    private val okHttpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .cache(cache)
            .addInterceptor { chain ->
                val request = chain.request()
                val repo = authRepository
                if (repo != null) {
                    AuthInterceptor(repo).intercept(chain)
                } else {
                    chain.proceed(request)
                }
            }
            .addInterceptor(HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BASIC
            })
            .build()
    }

    val publicApi: FortnitePublicApi by lazy {
        Retrofit.Builder()
            .baseUrl("https://fortnite-api.com/")
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(FortnitePublicApi::class.java)
    }

    val epicApi: EpicAccountApi by lazy {
        Retrofit.Builder()
            .baseUrl("https://account-public-service-prod03.ol.epicgames.com/")
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(EpicAccountApi::class.java)
    }

    val centralApi: CentralApi by lazy {
        Retrofit.Builder()
            .baseUrl("https://fortnite-central.app/")
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(CentralApi::class.java)
    }

    val pennyApi: PennyApi by lazy {
        Retrofit.Builder()
            .baseUrl("https://pennydb.net/")
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(PennyApi::class.java)
    }
}
