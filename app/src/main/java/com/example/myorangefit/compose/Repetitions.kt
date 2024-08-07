package com.example.myorangefit.compose

import android.content.Context
import android.content.Intent
import android.graphics.Paint
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.content.res.ResourcesCompat
import com.example.myorangefit.activity.ActivityManager
import com.example.myorangefit.R
import com.example.myorangefit.activity.SeriesActivity
import kotlin.math.cos
import kotlin.math.sin


class CircularNumberPickerActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        ActivityManager.add(this)
        super.onCreate(savedInstanceState)

        val data = intent.getStringExtra("date").orEmpty()
        val id = intent.getIntExtra("workout_id", -1)
        val weight = intent.getIntExtra("weight", -1)
        setContent {
            MaterialTheme {
                CircularNumberPickerApp(data, id, weight)
            }
        }
    }
}

@Composable
fun CircularNumberPickerApp(date: String, id: Int, weight: Int) {
    var selectedNumber by remember { mutableStateOf(0) }
    val context = LocalContext.current

    fun start(context: Context) {
        val intent = Intent(context, SeriesActivity::class.java)
        intent.putExtra("date", date)
        intent.putExtra("id_workout", id)
        intent.putExtra("weight", weight)
        context.startActivity(intent)
    }

    Surface(modifier = Modifier.fillMaxSize(), color = Color.Black) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            CircularNumberPicker(
                onNumberChange = { newNumber ->
                    selectedNumber = newNumber
                },
                selectedNumber = selectedNumber
            )
            Spacer(modifier = Modifier.height(32.dp))
            MyButton(
                modifier = Modifier
                    .size(60.dp)
                    .offset(x = 0.dp, y = (30).dp),
                text = "✓",
                onClick = {
                    start(context)
                }
            )
        }
    }
}

@Composable
fun CircularNumberPicker(
    onNumberChange: (Int) -> Unit,
    selectedNumber: Int,
    totalNumbers: Int = 36
) {
    val radius = 600f
    var rotationAngle by remember { mutableFloatStateOf(0f) }
    val customFont = ResourcesCompat.getFont(LocalContext.current, R.font.comfortaa)

    Box(
        modifier = Modifier
            .size(600.dp)
            .pointerInput(Unit) {
                detectHorizontalDragGestures { _, dragAmount ->
                    val dragCoefficient = 0.3f
                    rotationAngle += dragAmount * dragCoefficient

                    val newNumber = ((rotationAngle / (360f / totalNumbers)) % totalNumbers).toInt()
                    onNumberChange(newNumber.coerceIn(0, totalNumbers - 1))
                }
            }
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            var center = Offset(size.width / 2, size.height / 2)

            drawCircle(
                color = Color.White,
                radius = radius + 250,
                center = Offset(0F, center.y),
            )

            drawCircle(
                color = Color.Black,
                radius = radius + 150,
                center = Offset(0F, center.y),
            )

            var textPaint = Paint().apply {
                isAntiAlias = true
                textSize = 40f
                color = android.graphics.Color.WHITE
                textAlign = Paint.Align.CENTER
                typeface = customFont
            }

            val angleStep = 360f / totalNumbers

            for (i in 0 until totalNumbers) {
                val angle = (i * angleStep + rotationAngle) % 360
                val angleRad = Math.toRadians(angle.toDouble())
                val x = (radius * cos(angleRad)).toFloat()
                val y = (center.y + radius * sin(angleRad)).toFloat()

                drawContext.canvas.nativeCanvas.save()
                drawContext.canvas.nativeCanvas.drawText(
                    i.toString(),
                    x,
                    y,
                    textPaint
                )
                drawContext.canvas.nativeCanvas.restore()
            }

            fun ui(){
                // Disegnare rettangoli
                var rectLeft = center.x
                var rectTop = center.y - 550
                var rectRight = center.x + 450
                var rectBottom = center.y - 300
                var rotationAngleRect = -45f // Angolo di rotazione in gradi

                drawContext.canvas.nativeCanvas.save()
                drawContext.canvas.nativeCanvas.rotate(rotationAngleRect, center.x, center.y)
                drawRect(
                    color = Color.White,
                    topLeft = Offset(rectLeft, rectTop),
                    size = Size(rectRight - rectLeft, rectBottom - rectTop)
                )
                drawContext.canvas.nativeCanvas.restore()

                rectTop = center.y + 550
                rectBottom = center.y + 300
                rotationAngleRect = 45f
                drawContext.canvas.nativeCanvas.save()
                drawContext.canvas.nativeCanvas.rotate(rotationAngleRect, center.x, center.y)
                drawRect(
                    color = Color.White,
                    topLeft = Offset(rectLeft, rectTop),
                    size = Size(rectRight - rectLeft, rectBottom - rectTop)
                )
                drawContext.canvas.nativeCanvas.restore()


                rectLeft = -center.x
                rectTop = center.y - 750
                rectRight = center.x - 100
                rectBottom = center.y - 350
                drawRect(
                    color = Color.White,
                    topLeft = Offset(rectLeft, rectTop),
                    size = Size(rectRight - rectLeft, rectBottom - rectTop)
                )

                drawRect(
                    color = Color.White,
                    topLeft = Offset(rectLeft, rectTop),
                    size = Size(rectRight - rectLeft, rectBottom - rectTop)
                )

                rectTop = center.y + 750
                rectBottom = center.y + 350
                rectTop = center.y + 750
                rectBottom = center.y + 350
                drawRect(
                    color = Color.White,
                    topLeft = Offset(rectLeft, rectTop),
                    size = Size(rectRight - rectLeft, rectBottom - rectTop)
                )
            }
            ui()

            drawCircle(
                color = Color.White,
                radius = radius - 150,
                center = Offset(0F, center.y),
            )

            drawCircle(
                color = Color.White,
                radius = radius + 150,
                center = Offset(0F, center.y),
                style = androidx.compose.ui.graphics.drawscope.Stroke(width = 4f)
            )


            // Disegnare un triangolo
            var path = Path().apply {
                moveTo(center.x + 150, center.y - 10) // Punto superiore del triangolo
                lineTo(center.x + 400, center.y - 110) // Punto inferiore sinistro del triangolo
                lineTo(center.x + 400, center.y + 90) // Punto inferiore destro del triangolo
                close()
            }

            drawPath(
                path = path,
                color = Color(0xFFFF9800),
                style = androidx.compose.ui.graphics.drawscope.Fill
            )

            // X Rep
            center = Offset(size.width / 10, (size.height / 2))
            val text = "X $selectedNumber"

            textPaint = Paint().apply {
                isAntiAlias = true
                this.textSize = 100f
                color = android.graphics.Color.BLACK
                textAlign = Paint.Align.CENTER
                typeface = customFont
            }

            drawContext.canvas.nativeCanvas.drawText(
                text,
                center.x,
                center.y,
                textPaint
            )
        }
    }
}

@Preview
@Composable
fun CircularNumberPickerAppPreview() {
    CircularNumberPickerApp("", -1, -1)
}
