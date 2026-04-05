package io.github.takkunlego0916.autoclicker

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.accessibilityservice.GestureDescription
import android.graphics.PixelFormat
import android.graphics.Path
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.LayoutInflater
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
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
    private var interval = 100L

    private var startKey = KeyEvent.KEYCODE_F6
    private var stopKey = KeyEvent.KEYCODE_F7

    private enum class Mode { NONE, SET_START, SET_STOP }
    private var mode = Mode.NONE

    private lateinit var btnStart: Button
    private lateinit var btnStop: Button
    private lateinit var tvHotkey: TextView
    private lateinit var tvState: TextView
    private lateinit var editInterval: EditText

    private val loop = object : Runnable {
        override fun run() {
            if (!isClicking) return

            val x = targetCenterX()
            val y = targetCenterY()

            val ok = tap(x, y)
            if (!ok) {
                stopClicking()
                tvState.text = "クリック送信に失敗"
                return
            }

            handler.postDelayed(this, interval)
        }
    }

    override fun onServiceConnected() {
        super.onServiceConnected()

        val info = serviceInfo
        info.flags = info.flags or AccessibilityServiceInfo.FLAG_REQUEST_FILTER_KEY_EVENTS
        serviceInfo = info

        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        showUI()
    }

    private fun showUI() {
        controlView = LayoutInflater.from(this).inflate(R.layout.layout_floating_control, null)
        controlParams = createParams(100, 100, touchable = true)
        windowManager.addView(controlView, controlParams)

        targetView = LayoutInflater.from(this).inflate(R.layout.layout_target, null)
        targetParams = createParams(500, 500, touchable = true)
        windowManager.addView(targetView, targetParams)

        val drag = controlView.findViewById<TextView>(R.id.dragHandle)

        btnStart = controlView.findViewById(R.id.btnStart)
        btnStop = controlView.findViewById(R.id.btnStop)
        tvHotkey = controlView.findViewById(R.id.tvHotkey)
        tvState = controlView.findViewById(R.id.tvCaptureState)
        editInterval = controlView.findViewById(R.id.editInterval)

        val btnSetStart = controlView.findViewById<Button>(R.id.btnSetStartKey)
        val btnSetStop = controlView.findViewById<Button>(R.id.btnSetStopKey)

        btnStart.setOnClickListener {
            startClicking()
        }

        btnStop.setOnClickListener {
            stopClicking()
            tvState.text = "停止中"
        }

        btnSetStart.setOnClickListener {
            mode = Mode.SET_START
            tvState.text = "開始キー入力待ち"
        }

        btnSetStop.setOnClickListener {
            mode = Mode.SET_STOP
            tvState.text = "停止キー入力待ち"
        }

        attachDragBehavior(controlView, controlParams, drag)
        setTargetInteractive(true)
        updateLabel()
        tvState.text = "待機中"
    }

    private fun startClicking() {
        if (isClicking) return

        interval = editInterval.text.toString().toLongOrNull()?.coerceAtLeast(30) ?: 100L
        setTargetInteractive(false)

        isClicking = true
        btnStart.isEnabled = false
        btnStop.isEnabled = true

        handler.removeCallbacks(loop)
        handler.post(loop)
        tvState.text = "クリック中"
    }

    private fun stopClicking() {
        isClicking = false
        handler.removeCallbacks(loop)

        btnStart.isEnabled = true
        btnStop.isEnabled = false

        setTargetInteractive(true)
    }

    override fun onKeyEvent(event: KeyEvent): Boolean {
        if (event.action != KeyEvent.ACTION_DOWN || event.repeatCount != 0) return false

        when (mode) {
            Mode.SET_START -> {
                startKey = event.keyCode
                mode = Mode.NONE
                updateLabel()
                tvState.text = "開始キーを ${prettyKeyName(startKey)} に設定"
                return true
            }

            Mode.SET_STOP -> {
                stopKey = event.keyCode
                mode = Mode.NONE
                updateLabel()
                tvState.text = "停止キーを ${prettyKeyName(stopKey)} に設定"
                return true
            }

            Mode.NONE -> {
                when (event.keyCode) {
                    startKey -> {
                        startClicking()
                        return true
                    }

                    stopKey -> {
                        stopClicking()
                        tvState.text = "停止中"
                        return true
                    }
                }
            }
        }

        return false
    }

    private fun prettyKeyName(keyCode: Int): String {
        return when (keyCode) {
            KeyEvent.KEYCODE_F1 -> "F1"
            KeyEvent.KEYCODE_F2 -> "F2"
            KeyEvent.KEYCODE_F3 -> "F3"
            KeyEvent.KEYCODE_F4 -> "F4"
            KeyEvent.KEYCODE_F5 -> "F5"
            KeyEvent.KEYCODE_F6 -> "F6"
            KeyEvent.KEYCODE_F7 -> "F7"
            KeyEvent.KEYCODE_F8 -> "F8"
            KeyEvent.KEYCODE_F9 -> "F9"
            KeyEvent.KEYCODE_F10 -> "F10"
            KeyEvent.KEYCODE_F11 -> "F11"
            KeyEvent.KEYCODE_F12 -> "F12"
            KeyEvent.KEYCODE_VOLUME_UP -> "Volume Up"
            KeyEvent.KEYCODE_VOLUME_DOWN -> "Volume Down"
            KeyEvent.KEYCODE_BACK -> "Back"
            KeyEvent.KEYCODE_HOME -> "Home"
            KeyEvent.KEYCODE_POWER -> "Power"
            else -> KeyEvent.keyCodeToString(keyCode)
                .removePrefix("KEYCODE_")
                .replace('_', ' ')
                .lowercase()
                .replaceFirstChar { it.uppercase() }
        }
    }

    private fun updateLabel() {
        tvHotkey.text = "開始: ${prettyKeyName(startKey)} / 停止: ${prettyKeyName(stopKey)}"
    }

    private fun tap(x: Float, y: Float): Boolean {
        val path = Path().apply { moveTo(x, y) }
        val gesture = GestureDescription.Builder()
            .addStroke(GestureDescription.StrokeDescription(path, 0, 10))
            .build()

        return dispatchGesture(
            gesture,
            object : AccessibilityService.GestureResultCallback() {
                override fun onCompleted(gestureDescription: GestureDescription?) {
                    // 成功時は次のループへ
                }

                override fun onCancelled(gestureDescription: GestureDescription?) {
                    isClicking = false
                    handler.post {
                        btnStart.isEnabled = true
                        btnStop.isEnabled = false
                        setTargetInteractive(true)
                        tvState.text = "停止中"
                    }
                }
            },
            handler
        )
    }

    private fun createParams(x: Int, y: Int, touchable: Boolean): WindowManager.LayoutParams {
        val flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                if (touchable) 0 else WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE

        return WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
            flags,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            this.x = x
            this.y = y
        }
    }

    private fun setTargetInteractive(interactive: Boolean) {
        if (!::targetView.isInitialized) return

        if (interactive) {
            targetParams.flags = targetParams.flags and WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE.inv()
            windowManager.updateViewLayout(targetView, targetParams)
            attachTargetDrag()
        } else {
            targetView.setOnTouchListener(null)
            targetParams.flags = targetParams.flags or WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
            windowManager.updateViewLayout(targetView, targetParams)
        }
    }

    private fun attachTargetDrag() {
        targetView.setOnTouchListener(object : View.OnTouchListener {
            private var ix = 0
            private var iy = 0
            private var tx = 0f
            private var ty = 0f

            override fun onTouch(v: View, e: MotionEvent): Boolean {
                when (e.action) {
                    MotionEvent.ACTION_DOWN -> {
                        ix = targetParams.x
                        iy = targetParams.y
                        tx = e.rawX
                        ty = e.rawY
                        return true
                    }

                    MotionEvent.ACTION_MOVE -> {
                        targetParams.x = ix + (e.rawX - tx).toInt()
                        targetParams.y = iy + (e.rawY - ty).toInt()
                        windowManager.updateViewLayout(targetView, targetParams)
                        return true
                    }

                    MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                        return true
                    }
                }
                return false
            }
        })
    }

    private fun attachDragBehavior(view: View, params: WindowManager.LayoutParams, handle: View) {
        handle.setOnTouchListener(object : View.OnTouchListener {
            private var ix = 0
            private var iy = 0
            private var tx = 0f
            private var ty = 0f

            override fun onTouch(v: View, e: MotionEvent): Boolean {
                when (e.action) {
                    MotionEvent.ACTION_DOWN -> {
                        ix = params.x
                        iy = params.y
                        tx = e.rawX
                        ty = e.rawY
                        return true
                    }

                    MotionEvent.ACTION_MOVE -> {
                        params.x = ix + (e.rawX - tx).toInt()
                        params.y = iy + (e.rawY - ty).toInt()
                        windowManager.updateViewLayout(view, params)
                        return true
                    }

                    MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                        return true
                    }
                }
                return false
            }
        })
    }

    private fun targetCenterX(): Float {
        val w = when {
            targetView.width > 0 -> targetView.width
            targetView.measuredWidth > 0 -> targetView.measuredWidth
            else -> 50
        }
        return targetParams.x + (w / 2f)
    }

    private fun targetCenterY(): Float {
        val h = when {
            targetView.height > 0 -> targetView.height
            targetView.measuredHeight > 0 -> targetView.measuredHeight
            else -> 50
        }
        return targetParams.y + (h / 2f)
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {}
    override fun onInterrupt() {}

    override fun onDestroy() {
        super.onDestroy()
        isClicking = false
        handler.removeCallbacks(loop)

        if (::controlView.isInitialized) windowManager.removeView(controlView)
        if (::targetView.isInitialized) windowManager.removeView(targetView)
    }
}
