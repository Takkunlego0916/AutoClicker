package io.github.takkunlego0916.autoclicker

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.graphics.Path
import android.graphics.PixelFormat
import android.os.Handler
import android.os.Looper
import android.view.*
import android.view.accessibility.AccessibilityEvent
import android.widget.Button
import android.widget.EditText
import android.widget.TextView

class AutoClickService : AccessibilityService() {

    private lateinit var windowManager: WindowManager
    private lateinit var controlView: View
    private lateinit var targetView: View

    private lateinit var controlParams: WindowManager.LayoutParams
    private lateinit var targetParams: WindowManager.LayoutParams

    private val handler = Handler(Looper.getMainLooper())
    private var isClicking = false
    private var currentInterval = 100L

    private val clickRunnable = object : Runnable {
        override fun run() {
            if (!isClicking) return
            val density = resources.displayMetrics.density
            val offset = (25 * density).toInt()

            val clickX = targetParams.x + offset.toFloat()
            val clickY = targetParams.y + offset.toFloat()

            tap(clickX, clickY)

            handler.postDelayed(this, currentInterval)
        }
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        showControlPanel()
        showTargetIcon()
    }

    private fun showControlPanel() {
        controlView = LayoutInflater.from(this).inflate(R.layout.layout_floating_control, null)
        controlParams = createLayoutParams(100, 100)
        windowManager.addView(controlView, controlParams)

        val btnStart = controlView.findViewById<Button>(R.id.btnStart)
        val btnStop = controlView.findViewById<Button>(R.id.btnStop)
        val dragHandle = controlView.findViewById<TextView>(R.id.dragHandle)
        val editInterval = controlView.findViewById<EditText>(R.id.editInterval)

        btnStart.setOnClickListener {
            val input = editInterval.text.toString().toLongOrNull() ?: 100L
            currentInterval = if (input < 10) 10 else input

            isClicking = true
            btnStart.isEnabled = false
            btnStop.isEnabled = true

            handler.post(clickRunnable)
        }

        btnStop.setOnClickListener {
            isClicking = false
            btnStart.isEnabled = true
            btnStop.isEnabled = false
            handler.removeCallbacks(clickRunnable)
        }

        setDragListener(dragHandle, controlView, controlParams)
    }

    private fun showTargetIcon() {
        targetView = LayoutInflater.from(this).inflate(R.layout.layout_target, null)
        targetParams = createLayoutParams(500, 500)
        windowManager.addView(targetView, targetParams)
        setDragListener(targetView, targetView, targetParams)
    }

    private fun createLayoutParams(xPos: Int, yPos: Int): WindowManager.LayoutParams {
        return WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = xPos
            y = yPos
        }
    }

    private fun setDragListener(touchView: View, moveView: View, params: WindowManager.LayoutParams) {
        touchView.setOnTouchListener(object : View.OnTouchListener {
            private var initialX = 0
            private var initialY = 0
            private var initialTouchX = 0f
            private var initialTouchY = 0f

            override fun onTouch(v: View, event: MotionEvent): Boolean {
                when (event.action) {
                    MotionEvent.ACTION_DOWN -> {
                        initialX = params.x
                        initialY = params.y
                        initialTouchX = event.rawX
                        initialTouchY = event.rawY
                        return true
                    }
                    MotionEvent.ACTION_MOVE -> {
                        params.x = initialX + (event.rawX - initialTouchX).toInt()
                        params.y = initialY + (event.rawY - initialTouchY).toInt()
                        windowManager.updateViewLayout(moveView, params)
                        return true
                    }
                }
                return false
            }
        })
    }

    private fun tap(x: Float, y: Float) {
        val path = Path().apply { moveTo(x, y) }
        val builder = GestureDescription.Builder()
        val stroke = GestureDescription.StrokeDescription(path, 0, 10)
        builder.addStroke(stroke)
        dispatchGesture(builder.build(), null, null)
    }

    override fun onDestroy() {
        super.onDestroy()
        isClicking = false
        handler.removeCallbacks(clickRunnable)
        if (::controlView.isInitialized) windowManager.removeView(controlView)
        if (::targetView.isInitialized) windowManager.removeView(targetView)
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {}
    override fun onInterrupt() {}
}
