package com.nammaspendai.util

import android.view.View

@androidx.databinding.BindingAdapter("visibleIf")
fun setVisibleIf(view: View, visible: Boolean) {
    view.visibility = if (visible) View.VISIBLE else View.GONE
}

@androidx.databinding.BindingAdapter("goneIf")
fun setGoneIf(view: View, gone: Boolean) {
    view.visibility = if (gone) View.GONE else View.VISIBLE
}

@androidx.databinding.BindingAdapter("invisibleIf")
fun setInvisibleIf(view: View, invisible: Boolean) {
    view.visibility = if (invisible) View.INVISIBLE else View.VISIBLE
}
