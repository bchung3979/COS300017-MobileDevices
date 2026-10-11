package com.example.gymnasticsapp

import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class MainActivity : AppCompatActivity() {

    companion object {
        private const val TAG = "GymnasticsApp"
        private const val MAX_SCORE = 20
        private const val TOTAL_ELEMENTS = 10

        private const val KEY_SCORE = "score"
        private const val KEY_COMPLETED = "completed"
        private const val KEY_DEDUCTION = "deduction"
    }

    private var score = 0
    private var completedElements = 0
    private var deductionTaken = false

    private lateinit var scoreText: TextView
    private lateinit var elementText: TextView
    private lateinit var zoneText: TextView
    private lateinit var statusText: TextView

    private lateinit var performButton: Button
    private lateinit var deductionButton: Button
    private lateinit var resetButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_main)
        applySystemBarInsets()

        scoreText = findViewById(R.id.scoreText)
        elementText = findViewById(R.id.elementText)
        zoneText = findViewById(R.id.zoneText)
        statusText = findViewById(R.id.statusText)

        performButton = findViewById(R.id.performButton)
        deductionButton = findViewById(R.id.deductionButton)
        resetButton = findViewById(R.id.resetButton)

        if (savedInstanceState != null) {
            score = savedInstanceState.getInt(
                KEY_SCORE, 0
            ).coerceIn(0, MAX_SCORE)

            completedElements = savedInstanceState.getInt(
                KEY_COMPLETED, 0
            ).coerceIn(0, TOTAL_ELEMENTS)

            deductionTaken = savedInstanceState.getBoolean(
                KEY_DEDUCTION, false
            )

            Log.d(
                TAG,
                "State restored: score=$score, " +
                        "completed=$completedElements, " +
                        "deduction=$deductionTaken"
            )
        }

        performButton.setOnClickListener {
            performElement()
        }

        deductionButton.setOnClickListener {
            applyDeduction()
        }

        resetButton.setOnClickListener {
            resetRoutine()
        }

        renderState()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putInt(KEY_SCORE, score)
        outState.putInt(KEY_COMPLETED, completedElements)
        outState.putBoolean(KEY_DEDUCTION, deductionTaken)

        Log.d(
            TAG,
            "State saved: score=$score, " +
                    "completed=$completedElements, " +
                    "deduction=$deductionTaken"
        )

        super.onSaveInstanceState(outState)
    }

    private fun calculatePoints(element: Int): Int {
        return when (element) {
            in 1..3 -> 1
            in 4..7 -> 2
            in 8..10 -> 3
            else -> 0
        }
    }

    private fun performElement() {
        Log.d(TAG, "Perform button clicked")

        if (deductionTaken || completedElements >= TOTAL_ELEMENTS) {
            Log.w(TAG, "Perform blocked")
            return
        }

        val nextElement = completedElements + 1
        val points = calculatePoints(nextElement)

        score = (score + points).coerceIn(0, MAX_SCORE)
        completedElements = nextElement

        Log.d(
            TAG,
            "Performed element=$nextElement, " +
                    "added=$points, score=$score"
        )

        renderState()
    }

    private fun applyDeduction() {
        Log.d(TAG, "Deduction button clicked")

        if (completedElements == 0 ||
            completedElements >= TOTAL_ELEMENTS ||
            deductionTaken ||
            score == 0
        ) {
            Log.w(TAG, "Deduction blocked")
            return
        }

        score = (score - 2).coerceAtLeast(0)
        deductionTaken = true

        Log.d(TAG, "Deduction applied, score=$score")

        renderState()
    }

    private fun resetRoutine() {
        Log.d(TAG, "Reset button clicked")

        score = 0
        completedElements = 0
        deductionTaken = false

        Log.d(TAG, "Routine reset")

        renderState()
    }

    private fun renderState() {
        val currentElement =
            (completedElements + 1).coerceAtMost(TOTAL_ELEMENTS)

        scoreText.text = getString(R.string.score_format, score)
        elementText.text = getString(
            R.string.element_format,
            currentElement
        )

        val zoneName: Int
        val zoneColor: Int

        when (currentElement) {
            in 1..3 -> {
                zoneName = R.string.zone_basic
                zoneColor = R.color.score_blue
            }
            in 4..7 -> {
                zoneName = R.string.zone_intermediate
                zoneColor = R.color.score_green
            }
            else -> {
                zoneName = R.string.zone_advanced
                zoneColor = R.color.score_orange
            }
        }

        zoneText.text = getString(
            R.string.zone_format,
            getString(zoneName)
        )

        scoreText.setTextColor(getColor(zoneColor))
        zoneText.setTextColor(getColor(zoneColor))

        performButton.isEnabled =
            !deductionTaken && completedElements < TOTAL_ELEMENTS

        deductionButton.isEnabled =
            !deductionTaken &&
            completedElements in 1 until TOTAL_ELEMENTS && score > 0

        statusText.text = when {
            completedElements == TOTAL_ELEMENTS ->
                getString(R.string.status_complete, score)
            deductionTaken ->
                getString(R.string.status_deduction)
            completedElements == 0 ->
                getString(R.string.status_ready)
            else ->
                getString(R.string.status_progress)
        }
    }

    private fun applySystemBarInsets() {
        val root = findViewById<View>(R.id.rootLayout)

        val left = root.paddingLeft
        val top = root.paddingTop
        val right = root.paddingRight
        val bottom = root.paddingBottom

        ViewCompat.setOnApplyWindowInsetsListener(root) { view, insets ->
            val bars = insets.getInsets(
                WindowInsetsCompat.Type.systemBars()
            )

            view.setPadding(
                left + bars.left,
                top + bars.top,
                right + bars.right,
                bottom + bars.bottom
            )

            insets
        }
    }
}