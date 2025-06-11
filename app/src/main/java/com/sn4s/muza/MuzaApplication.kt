package com.sn4s.muza

import android.app.Application
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.disk.DiskCache
import coil.memory.MemoryCache
import dagger.hilt.android.HiltAndroidApp
import okhttp3.OkHttpClient
import java.io.File

@HiltAndroidApp
class MuzaApplication : Application(), ImageLoaderFactory {

    override fun newImageLoader(): ImageLoader {
        return ImageLoader.Builder(this)
            .memoryCache {
                MemoryCache.Builder(this)
                    .maxSizePercent(0.25) // Use 25% of app's available memory
                    .build()
            }
            .diskCache {
                DiskCache.Builder()
                    .directory(File(cacheDir, "image_cache"))
                    .maxSizeBytes(100 * 1024 * 1024) // 100MB disk cache
                    .build()
            }
            .okHttpClient {
                OkHttpClient.Builder()
                    .cache(
                        okhttp3.Cache(
                            directory = File(cacheDir, "http_cache"),
                            maxSize = 50L * 1024L * 1024L // 50MB HTTP cache
                        )
                    )
                    .build()
            }
            .respectCacheHeaders(false) // Ignore server cache headers for longer caching
            .crossfade(true) // Enable crossfade animation by default
            .build()
    }
}