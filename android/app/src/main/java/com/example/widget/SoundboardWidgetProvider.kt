package com.example.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.example.R
import com.example.MainActivity
import com.example.data.db.AppDatabase
import com.example.data.preferences.UserPreferencesRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class SoundboardWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        for (appWidgetId in appWidgetIds) {
            updateWidget(context, appWidgetManager, appWidgetId)
        }
    }

    companion object {
        fun updateAllWidgets(context: Context) {
            val appWidgetManager = AppWidgetManager.getInstance(context)
            val ids = appWidgetManager.getAppWidgetIds(
                android.content.ComponentName(context, SoundboardWidgetProvider::class.java)
            )
            for (id in ids) {
                updateWidget(context, appWidgetManager, id)
            }
        }

        private fun updateWidget(
            context: Context,
            appWidgetManager: AppWidgetManager,
            appWidgetId: Int
        ) {
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val preferencesRepo = UserPreferencesRepository(context)
                    val prefs = preferencesRepo.userPreferencesFlow.first()
                    val db = AppDatabase.getInstance(context)

                    val boards = db.soundboardDao().getBoardsSync()
                    if (boards.isEmpty()) return@launch

                    val targetBoardId = prefs.widgetBoardId
                        ?: prefs.activeBoardId
                        ?: boards.first().id

                    val board = boards.find { it.id == targetBoardId } ?: boards.first()
                    val pads = db.soundboardDao().getPadsForBoardSync(board.id)

                    val views = RemoteViews(context.packageName, R.layout.widget_soundboard)
                    views.setTextViewText(R.id.widget_title, "🎵 " + board.name)

                    // Open app when header clicked
                    val openAppIntent = Intent(context, MainActivity::class.java)
                    val openAppPendingIntent = PendingIntent.getActivity(
                        context,
                        0,
                        openAppIntent,
                        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                    )
                    views.setOnClickPendingIntent(R.id.widget_title, openAppPendingIntent)

                    appWidgetManager.updateAppWidget(appWidgetId, views)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }
}
