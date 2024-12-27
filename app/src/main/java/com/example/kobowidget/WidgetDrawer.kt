package com.example.kobowidget

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.RemoteViews

open class WidgetDrawer {

    fun getById(
        context: Context,
        layoutId: Int
    ) = RemoteViews(context.packageName, layoutId)

    fun getLayoutById(
        inflater: LayoutInflater,
        layoutId: Int,
        parent: ViewGroup
    ): View = inflater.inflate(layoutId, parent, false)

    open fun drawDayCellsRemote(
        widgetViews: RemoteViews
    ) {

    }

    open fun drawDayCellsMain(
        widgetView: LinearLayout
    ) {

    }
}