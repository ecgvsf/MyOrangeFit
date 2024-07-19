package com.example.myorangefit

import android.graphics.Canvas
import android.graphics.Paint
import android.text.style.LineBackgroundSpan

class DotSpan(private val color: Int, private val totalDots: Int, private val index: Int) : LineBackgroundSpan {
    private val radius = 5f // Adjust as needed
    private val additionalSpacing = 5f // Additional space between dots

    override fun drawBackground(
        canvas: Canvas,
        paint: Paint,
        left: Int,
        right: Int,
        top: Int,
        baseline: Int,
        bottom: Int,
        charSequence: CharSequence,
        start: Int,
        end: Int,
        lineNumber: Int
    ) {
        val oldColor = paint.color
        val oldStyle = paint.style
        val oldStrokeWidth = paint.strokeWidth

        paint.color = color
        paint.style = Paint.Style.FILL // Ensure the circle is filled

        // Calculate the space between the dots based on their radius and additional spacing
        val spacing = 2 * radius + additionalSpacing

        // Calculate the total width occupied by the dots
        val totalWidth = totalDots * spacing - additionalSpacing

        // Calculate the starting position to center the dots
        val startX = (left + right - totalWidth) / 2 + radius

        // Define the vertical offset to move the circle downward
        val verticalOffset = 45f // Adjust this value to move the circle further down

        // Draw the dot at the calculated position
        val y = (top + bottom) / 2f + verticalOffset
        val x = startX + index * spacing

        // Draw the circle if it's within the visible bounds
        if (x + radius <= right) {
            canvas.drawCircle(x, y, radius, paint)
        }

        // Restore the paint's color and style
        paint.color = oldColor
        paint.style = oldStyle
        paint.strokeWidth = oldStrokeWidth
    }
}
