package com.roger.catloadinglibrary

import android.app.Dialog
import android.os.Bundle
import androidx.fragment.app.DialogFragment

class CatLoadingView : DialogFragment() {
    private var cancelOnClick = true

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return Dialog(requireContext()).apply {
            setCancelable(cancelOnClick)
            setCanceledOnTouchOutside(cancelOnClick)
        }
    }

    fun setBackgroundColor(color: Int) {
        // Compatibility no-op for the previous third-party loading dialog API.
    }

    fun setClickCancelAble(cancelable: Boolean) {
        cancelOnClick = cancelable
        dialog?.setCancelable(cancelable)
        dialog?.setCanceledOnTouchOutside(cancelable)
    }
}
