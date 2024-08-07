package com.example.myorangefit.compose

import android.app.Activity.RESULT_OK
import android.content.Intent
import android.graphics.Paint
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Typography
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.lang.Math.toRadians
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.window.Dialog
import androidx.core.content.res.ResourcesCompat
import com.example.myorangefit.activity.ActivityManager
import com.example.myorangefit.R


class Scale : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        ActivityManager.add(this)
        ActivityManager.add(this)
        super.onCreate(savedInstanceState)
        setContent {
            ScaleTheme {
                WeightPickerApp(this)
            }
        }
    }
}

@Preview
@Composable
fun WeightPickerApp(scale: Scale? = null) {
    var weight by remember { mutableFloatStateOf(20f) }
    var showDialog by remember { mutableStateOf(false) }
    var count by remember { mutableIntStateOf(20) }
    val customFont = ResourcesCompat.getFont(LocalContext.current, R.font.comfortaa)
    val dialogBackgroundColor = Color(0xFF3A3A3A)

    Surface(modifier = Modifier.fillMaxSize(), color = Color.Black) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            WeightPicker(
                onWeightChange = { newWeight ->
                    weight = newWeight.coerceIn(0f ,150f)
                }
            )
            Spacer(modifier = Modifier.height(32.dp))
            MyButton(
                modifier = Modifier
                    .size(60.dp)
                    .offset(x = 0.dp, y = (30).dp),
                text = "✓",
                onClick = {
                    showDialog = true
                }
            )
            if (showDialog) {
                Dialog(onDismissRequest = { showDialog = false }) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(dialogBackgroundColor, RoundedCornerShape(16.dp))
                            .padding(16.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(dialogBackgroundColor)
                                .padding(16.dp),
                            verticalArrangement = Arrangement.Center,
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "Choose how many reps:",
                                color = Color.White,
                                fontFamily = customFont?.let { FontFamily(it) }
                            )

                            Spacer(modifier = Modifier.height(16.dp))

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceAround,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                MyButton(
                                    modifier = Modifier.size(60.dp),
                                    text = "-",
                                    onClick = { if (count > 1) count-- }
                                )

                                Canvas(modifier = Modifier.size(100.dp)) {
                                    drawContext.canvas.nativeCanvas.apply {
                                        drawText(
                                            count.toString(),
                                            size.width / 2,
                                            size.height / 2,
                                            Paint().apply {
                                                textAlign = android.graphics.Paint.Align.CENTER
                                                textSize = 60f
                                                typeface = customFont
                                                color = android.graphics.Color.WHITE
                                            }
                                        )
                                    }
                                }

                                MyButton(
                                    modifier = Modifier.size(60.dp),
                                    text = "+",
                                    onClick = { count++ }
                                )
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceEvenly
                            ) {
                                Button(
                                    colors = ButtonDefaults.buttonColors(Color(0xFFFF9800)),
                                    onClick = { showDialog = false }
                                ) {
                                    Text("Cancel", fontFamily = customFont?.let { FontFamily(it) })
                                }
                                Button(
                                    colors = ButtonDefaults.buttonColors(Color(0xFFFF9800)),
                                    onClick = {
                                        Log.d("peso", weight.toString())
                                        Log.d("peso", weight.toString())
                                        val resultIntent = Intent()
                                        resultIntent.putExtra("weight", weight)
                                        resultIntent.putExtra("reps", count)
                                        scale?.setResult(RESULT_OK, resultIntent)

                                        scale?.finish()
                                    }
                                ) {
                                    Text("OK", fontFamily = customFont?.let { FontFamily(it) })
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ScaleTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        typography = Typography(
            displaySmall = TextStyle(
                fontFamily = FontFamily(
                    Font(R.font.comfortaa, FontWeight.Normal)
                ),
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold
            ),
        ),
        content = content
    )
}

@Composable
fun WeightPicker(onWeightChange: (Float) -> Unit) {

    val outerRadius = 450f
    val middleRadius = 390f
    val innerRadius = 360f
    val needleBigRadius = 26f
    val needleSmallRadius = 12f
    val markerLength = 20f
    //val textRadius = innerRadius / 2

    var rotationAngle by remember { mutableFloatStateOf(-19f) }
    val customFont = ResourcesCompat.getFont(LocalContext.current, R.font.comfortaa)



    Box(
        modifier = Modifier
            .size(400.dp)
            .pointerInput(Unit) {
                detectHorizontalDragGestures { change, dragAmount ->
                    // Coefficient to slow down the drag
                    val dragCoefficient = 0.2f

                    // Update rotationAngle based on drag amount and coefficient
                    rotationAngle += dragAmount * dragCoefficient / 2.4f

                    // Clamp rotationAngle to its limits
                    rotationAngle = rotationAngle.coerceIn(-150f, 0f)

                    // Calculate new weight (maximum 160)
                    val newWeight = (rotationAngle * (160 / 150f)).coerceIn(0f, 160f)
                    onWeightChange(newWeight)

                    change.consume()
                }
            }
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val center = Offset(size.width / 2, size.height / 2)

            // Draw the outer circle
            drawCircle(
                color = Color.DarkGray,
                radius = outerRadius,
                center = center
            )

            // Draw the middle circle
            drawCircle(
                color = Color(0xFFFF9800), // Light orange
                radius = middleRadius,
                center = center
            )

            // Draw the inner circle
            drawCircle(
                color = Color(0xFFDF6310), // Darker orange
                radius = innerRadius,
                center = center
            )

            // Draw the arc
            drawArc(
                color = Color.Black,
                startAngle = 225f,
                sweepAngle = 90f,
                useCenter = false,
                topLeft = Offset(center.x - (innerRadius - 180), center.y - (innerRadius / 2) - 30),
                size = Size(innerRadius, innerRadius),
                style = Stroke(width = 180f)
            )

            // Draw markers along the edge of the circle
            val numMarkers = 5
            val angleStep = 110 / numMarkers.toFloat()

            // Draw markers along the arc
            for (i in 0 until numMarkers) {
                if (i != 2) {
                    val angle = 225f + angleStep * i
                    val startX =
                        center.x + (innerRadius - 285) * cos(toRadians(angle.toDouble())).toFloat()
                    val startY =
                        center.y + (innerRadius - 290) * sin(toRadians(angle.toDouble())).toFloat() - 50
                    val endX = startX + markerLength * cos(toRadians(angle.toDouble())).toFloat()
                    val endY = startY + markerLength * sin(toRadians(angle.toDouble())).toFloat()

                    drawLine(
                        color = Color.Yellow,
                        start = Offset(startX, startY),
                        end = Offset(endX, endY),
                        strokeWidth = 3f
                    )
                }
            }

            // Draw the arc (black)
            val arcPath = Path().apply {
                addArc(
                    Rect(
                        center.x - innerRadius + 130,
                        center.y - (innerRadius / 2) - 80,
                        center.x + innerRadius - 130,
                        center.y + (innerRadius / 2) + 60
                    ),
                    225f,
                    90f
                )
            }

            // Clip to the arc path to ensure only text within the arc is visible
            clipPath(arcPath) {
                // Draw text
                val textPaint = Paint().apply {
                    isAntiAlias = true
                    this.textSize = 52f
                    color = android.graphics.Color.WHITE
                    textAlign = Paint.Align.CENTER
                    typeface = customFont
                }

                val text = "0 10 20 30 40 50 60 70 80 90 100 110 120 130 140 150"
                val words = text.split(" ")
                val totalWords = words.size
                val angleStepText = 360f / totalWords

                // Calculate the initial rotation angle so that when it's at the top (angle 0),
                // the text is vertical
                val initialRotationAngle = -90f + rotationAngle * (360f / 150f)

                words.forEachIndexed { index, word ->
                    val angle = initialRotationAngle + angleStepText * index
                    val angleRad = toRadians(angle.toDouble())
                    val x = (center.x + (innerRadius - 130) * cos(angleRad)).toFloat()
                    val y = (center.y + (innerRadius - 150) * sin(angleRad)).toFloat()

                    // Rotate the canvas
                    drawContext.canvas.nativeCanvas.save()
                    drawContext.canvas.nativeCanvas.rotate(angle + 90f, x, y)

                    // Draw text
                    drawContext.canvas.nativeCanvas.drawText(word, x, y, textPaint)

                    // Restore canvas rotation
                    drawContext.canvas.nativeCanvas.restore()
                }
            }

            // Draw the fixed needle
            //val needleAngle = 270f
            val needleLength = (innerRadius / 2) - 10
            //val needleEnd = Offset(
            //    x = center.x + needleLength * cos(toRadians(needleAngle.toDouble())).toFloat(),
            //    y = center.y + needleLength * sin(toRadians(needleAngle.toDouble())).toFloat()
            //)
            drawCircle(
                color = Color.Black,
                radius = needleBigRadius,
                center = center
            )
            drawCircle(
                color = Color.Yellow,
                radius = needleSmallRadius,
                center = center
            )

            // Draw the needle triangle
            val needlePath = Path().apply {
                moveTo(center.x, center.y)
                lineTo(center.x + needleSmallRadius - 5, center.y)
                lineTo(center.x, center.y - needleLength)
                lineTo(center.x - needleSmallRadius + 5, center.y)
                close()
            }

            drawPath(
                path = needlePath,
                color = Color.Yellow
            )

            // Draw text background rounded rectangle
            val text = (-rotationAngle * (160 / 150f)).roundToInt().toString()
            // Notifica il cambiamento di weight a WeightPickerApp
            onWeightChange(text.toFloat())
            val textPaint = Paint().apply {
                isAntiAlias = true
                this.textSize = 64f
                color = android.graphics.Color.WHITE
                textAlign = Paint.Align.CENTER
                typeface = customFont
            }

            val textWidth = textPaint.measureText(text)
            val textHeight = textPaint.textSize

            // Calculate position for the background rectangle
            val rectLeft = center.x - (textWidth / 2) - 50
            val rectTop = center.y - (textHeight / 2) + 125
            val rectRight = center.x + (textWidth / 2) + 50
            val rectBottom = center.y + (textHeight / 2) + 225

            drawRoundRect(
                color = Color(0xFFFF9800),
                topLeft = Offset(rectLeft, rectTop),
                size = Size(rectRight - rectLeft, rectBottom - rectTop),
                cornerRadius = CornerRadius(100f)
            )

            // Draw text
            drawContext.canvas.nativeCanvas.drawText(
                text,
                center.x,
                (rectTop + rectBottom) / 2 + textHeight / 2 - 10,
                textPaint
            )
        }

        // Draw buttons inside the circle
        val buttonRadius = 60f

        MyButton(
            modifier = Modifier
                .size(buttonRadius.dp)
                .offset(x = 80.dp, y = 180.dp),
            text = "-5",
            onClick = {
                if (rotationAngle < 0f) {
                    rotationAngle =
                        ((((rotationAngle * (160f / 150f)).coerceIn(-160f, 0f) + 5) / 5).roundToInt() * 5f) * (150f / 160f)
                }
            }
        )

        MyButton(
            modifier = Modifier
                .size(buttonRadius.dp)
                .offset(x = (220).dp, y = 180.dp),
            text = "+5",
            onClick = {
                if (rotationAngle > -150f) {
                    rotationAngle =
                        ((((rotationAngle * (160f / 150f)).coerceIn(-160f, 0f) - 5) / 5).roundToInt() * 5f) * (150f / 160f)
                }
            }
        )
    }
}

@Composable
fun MyButton(modifier: Modifier = Modifier, text: String, fontSize: Float = 20f, onClick: () -> Unit) {
    Box(
        modifier = modifier
            .pointerInput(Unit) {
                detectTapGestures(
                    onTap = {
                        onClick()
                    }
                )
            },
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawCircle(
                color = Color(0xFFFF9800),
                radius = size.minDimension / 2
            )
        }

        Text(
            fontFamily = FontFamily(
                Font(R.font.comfortaa, FontWeight.Normal)
            ),
            text = text,
            color = Color.White,
            fontSize = fontSize.sp
        )
    }
}