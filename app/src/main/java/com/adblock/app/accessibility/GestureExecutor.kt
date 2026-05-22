package com.adblock.app.accessibility

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.graphics.Path
import android.graphics.Rect
import android.os.Build
import android.os.Handler
import android.os.HandlerThread
import android.util.DisplayMetrics
import android.view.accessibility.AccessibilityNodeInfo
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean

class GestureExecutor(private val service: AccessibilityService) {

    private val gestureHandler: Handler by lazy {
        val thread = HandlerThread("gesture-executor").also { it.start() }
        Handler(thread.looper)
    }

    fun clickNode(node: AccessibilityNodeInfo): Boolean {
        if (node.isClickable) {
            return node.performAction(AccessibilityNodeInfo.ACTION_CLICK)
        }
        val clickable = findClickableChild(node)
        val result = clickable?.performAction(AccessibilityNodeInfo.ACTION_CLICK) ?: false
        clickable?.recycle()
        return result
    }

    private fun findClickableChild(node: AccessibilityNodeInfo): AccessibilityNodeInfo? {
        for (i in 0 until node.childCount) {
            val child = node.getChild(i) ?: continue
            if (child.isClickable) {
                return child
            }
            val found = findClickableChild(child)
            child.recycle()
            if (found != null) return found
        }
        return null
    }

    fun tapAtFraction(xFraction: Double, yFraction: Double): Boolean {
        val display = service.windowManager?.defaultDisplay ?: return false
        val metrics = DisplayMetrics().also { display.getRealMetrics(it) }
        val x = (metrics.widthPixels * xFraction).toInt()
        val y = (metrics.heightPixels * yFraction).toInt()
        return tapAt(x, y)
    }

    fun tapAt(x: Int, y: Int): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) return false

        val path = Path().also { it.moveTo(x.toFloat(), y.toFloat()) }
        val gesture = GestureDescription.Builder()
            .addStroke(GestureDescription.StrokeDescription(path, 0, 100))
            .build()

        return try {
            val result = AtomicBoolean(false)
            val latch = CountDownLatch(1)

            service.dispatchGesture(gesture, object : AccessibilityService.GestureResultCallback() {
                override fun onCompleted(gestureDescription: GestureDescription?) {
                    result.set(true)
                    latch.countDown()
                }
                override fun onCancelled(gestureDescription: GestureDescription?) {
                    result.set(false)
                    latch.countDown()
                }
            }, gestureHandler)

            latch.await(1, TimeUnit.SECONDS)
            result.get()
        } catch (e: Exception) { false }
    }

    fun getNodeCenter(node: AccessibilityNodeInfo): Pair<Int, Int>? {
        val rect = Rect()
        node.getBoundsInScreen(rect)
        if (rect.isEmpty) return null
        return Pair(rect.centerX(), rect.centerY())
    }
}
