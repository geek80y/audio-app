package com.knigiaudio.app

import android.app.Application
import com.knigiaudio.app.data.Library

class App : Application() {
    override fun onCreate() {
        super.onCreate()
        Library.init(this)
    }
}
