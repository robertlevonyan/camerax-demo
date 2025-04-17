package com.robertlevonyan.demo.camerax.utils

import android.view.GestureDetector
import android.view.MotionEvent
import kotlin.math.abs

class SwipeGestureDetector : GestureDetector.SimpleOnGestureListener() {
    companion object {
        private const val MIN_SWIPE_DISTANCE_X = 100
    }

    var swipeCallback: SwipeCallback? = null

    override fun onFling(e1: MotionEvent?, e2: MotionEvent, velocityX: Float, velocityY: Float): Boolean {
        e1 ?: return super.onFling(e1, e2, velocityX, velocityY)

        val deltaX = e1.x - e2.x
        val deltaXAbs = abs(deltaX)

        if (deltaXAbs >= MIN_SWIPE_DISTANCE_X) {
            if (deltaX > 0) {
                swipeCallback?.onLeftSwipe()
            } else {
                swipeCallback?.onRightSwipe()
            }
        }

        return true
    }

    interface SwipeCallback {
        fun onLeftSwipe()

        fun onRightSwipe()
    }

    fun setSwipeCallback(left: ()-> Unit = {}, right: ()-> Unit = {}) {
        swipeCallback = object : SwipeCallback {
            override fun onLeftSwipe() {
                left()
            }

            override fun onRightSwipe() {
                right()
            }
        }
    }
}
