package com.example.composemvi.presentation.util

import android.content.Context
import javax.inject.Inject

class ResourceProviderImpl @Inject constructor(
    private val context: Context,
) : ResourceProvider {
    override fun getString(resId: Int): String {
        return context.getString(resId)
    }
}
