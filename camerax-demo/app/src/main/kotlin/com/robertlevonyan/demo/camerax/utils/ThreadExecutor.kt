package com.robertlevonyan.demo.camerax.utils

import android.os.Handler
import java.util.concurrent.Executor

open class ThreadExecutor(protected val handler: Handler) : Executor {
    override fun execute(runnable: Runnable) {
        handler.post(runnable)
    }
}
