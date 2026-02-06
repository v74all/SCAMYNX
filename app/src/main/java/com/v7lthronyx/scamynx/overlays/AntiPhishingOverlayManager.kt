package com.v7lthronyx.scamynx.overlays

import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.v7lthronyx.scamynx.MainActivity
import com.v7lthronyx.scamynx.R
import com.v7lthronyx.scamynx.data.privacyradar.coordinator.PrivacyRadarCoordinator
import com.v7lthronyx.scamynx.data.privacyradar.model.PrivacyEvent
import com.v7lthronyx.scamynx.data.privacyradar.model.PrivacyEventMetadataKeys
import com.v7lthronyx.scamynx.data.privacyradar.model.PrivacyResourceType
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AntiPhishingOverlayManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val coordinator: PrivacyRadarCoordinator,
) {

    private val windowManager: WindowManager? = ContextCompat.getSystemService(
        context,
        WindowManager::class.java,
    )
    private val overlayParams = WindowManager.LayoutParams(
        WindowManager.LayoutParams.MATCH_PARENT,
        WindowManager.LayoutParams.WRAP_CONTENT,
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        },
        WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
            WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON,
        PixelFormat.TRANSLUCENT,
    ).apply {
        gravity = Gravity.TOP
        y = context.resources.getDimensionPixelSize(R.dimen.phishing_overlay_margin_top)
    }
    private val handler = Handler(Looper.getMainLooper())
    private val hideRunnable = Runnable { dismissOverlay() }
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var started = false
    private var overlayView: View? = null

    fun start() {
        if (started) return
        started = true
        coordinator.start()
        scope.launch {
            coordinator.hotLaneEvents.collectLatest { event ->
                if (event.resourceType == PrivacyResourceType.PHISHING_URL) {
                    showOverlay(event)
                }
            }
        }
    }

    fun stop() {
        scope.cancel()
        dismissOverlay()
        started = false
    }

    private fun showOverlay(event: PrivacyEvent) {
        if (!Settings.canDrawOverlays(context)) return
        val manager = windowManager ?: return
        val view = ensureOverlayView(manager)
        val appLabel = resolveAppLabel(event.packageName)
        val url = event.metadata[PrivacyEventMetadataKeys.NORMALIZED_URL]
            ?: event.metadata[PrivacyEventMetadataKeys.URL]
            ?: context.getString(R.string.phishing_overlay_unknown_url)
        val level = event.metadata[PrivacyEventMetadataKeys.THREAT_LEVEL]?.uppercase()
        view.findViewById<TextView>(R.id.phishingOverlayTitle).text = context.getString(
            R.string.phishing_overlay_title,
            appLabel,
        )
        view.findViewById<TextView>(R.id.phishingOverlayBody).text = context.getString(
            R.string.phishing_overlay_body,
            url,
        )
        val scoreLabel = event.metadata[PrivacyEventMetadataKeys.THREAT_SCORE]?.toDoubleOrNull()
        val detailsView = view.findViewById<TextView>(R.id.phishingOverlayDetails)
        val triggerText = buildString {
            level?.let { append(context.getString(R.string.phishing_overlay_level_label, it)) }
            if (scoreLabel != null) {
                if (isNotEmpty()) append(" • ")
                append(
                    context.getString(
                        R.string.phishing_overlay_score_label,
                        String.format("%.2f", scoreLabel),
                    ),
                )
            }
        }
        if (triggerText.isNotEmpty()) {
            detailsView.text = triggerText
            detailsView.visibility = View.VISIBLE
        } else {
            detailsView.visibility = View.GONE
        }
        handler.removeCallbacks(hideRunnable)
        handler.postDelayed(hideRunnable, OVERLAY_TIMEOUT_MS)
    }

    private fun ensureOverlayView(manager: WindowManager): View {
        val existing = overlayView
        if (existing != null) {
            return existing
        }
        val view = LayoutInflater.from(context).inflate(R.layout.view_phishing_overlay, null, false)
        view.findViewById<View>(R.id.phishingOverlayDismiss).setOnClickListener {
            dismissOverlay()
        }
        view.findViewById<View>(R.id.phishingOverlayOpenApp).setOnClickListener {
            dismissOverlay()
            openApp()
        }
        overlayView = view
        if (view.parent == null) {
            manager.addView(view, overlayParams)
        }
        return view
    }

    private fun dismissOverlay() {
        handler.removeCallbacks(hideRunnable)
        val manager = windowManager ?: return
        val view = overlayView ?: return
        if (view.parent != null) {
            manager.removeView(view)
        }
        overlayView = null
    }

    private fun openApp() {
        val intent = Intent(context, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        }
        context.startActivity(intent)
    }

    private fun resolveAppLabel(packageName: String): String = runCatching {
        val pm = context.packageManager
        val applicationInfo = pm.getApplicationInfo(packageName, 0)
        pm.getApplicationLabel(applicationInfo)?.toString()
    }.getOrNull() ?: packageName

    companion object {
        private const val OVERLAY_TIMEOUT_MS = 6_000L
    }
}
