package com.imuxuan.floatingview

import android.app.Activity
import android.view.ViewGroup

class FloatingView private constructor() {
    var view: FloatingMagnetView? = null
        private set

    fun customView(view: FloatingMagnetView): FloatingView {
        detachFromParent(view)
        this.view = view
        return this
    }

    fun attach(activity: Activity) {
        val floatingView = view ?: return
        detachFromParent(floatingView)
        activity.addContentView(floatingView, floatingView.layoutParams)
    }

    fun detach(activity: Activity) {
        val floatingView = view ?: return
        val decorView = activity.window?.decorView as? ViewGroup ?: return
        if (floatingView.parent === decorView) {
            decorView.removeView(floatingView)
        }
    }

    private fun detachFromParent(floatingView: FloatingMagnetView) {
        (floatingView.parent as? ViewGroup)?.removeView(floatingView)
    }

    companion object {
        private val INSTANCE = FloatingView()

        @JvmStatic
        fun get(): FloatingView = INSTANCE
    }
}
