package com.example.myorangefit.compose

import android.app.Activity.RESULT_OK
import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.myorangefit.activity.ActivityManager
import com.example.myorangefit.R
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin

class Clock : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        ActivityManager.add(this)
        super.onCreate(savedInstanceState)
        setContent {
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = MaterialTheme.colorScheme.background
            ) {
                ClockApp(this)
            }
        }
    }
}

@Composable
fun ClockApp(clock: Clock) {

    var secondHandAngle by remember { mutableFloatStateOf(90f) }
    var minuteHandAngle by remember { mutableFloatStateOf(0f) }

    var minutes by remember { mutableIntStateOf(0) }
    var seconds by remember { mutableIntStateOf(15) }

    fun addSeconds(additionalSeconds: Int) {
        val totalSeconds = seconds + additionalSeconds
        seconds = totalSeconds % 60
        minutes = (minutes + totalSeconds / 60) % 60

        // Update angles based on new time
        secondHandAngle = (seconds * 6f) % 360
        minuteHandAngle = ((minutes * 6) + (secondHandAngle / 60f)) % 360
    }

    fun resetTime() {
        minutes = 0
        seconds = 0
        secondHandAngle = 0f
        minuteHandAngle = 0f
    }

    fun closeActivity(clock: Clock) {
        val resultIntent = Intent()
        resultIntent.putExtra("time", seconds + (minutes * 60))
        clock.setResult(RESULT_OK, resultIntent)

        clock.finish()

    }

    Surface(
        color = Color.Black,
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectDragGestures { change, _ ->
                    val center = Offset(size.width / 2f, size.height / 2f)
                    val x = change.position.x - center.x
                    val y = change.position.y - center.y
                    val newAngle = (atan2(y, x) * 180 / PI).toFloat() + 90
                    val normalizedNewAngle = ((newAngle % 360) + 360) % 360

                    // Calcola la differenza di angolo rispetto all'angolo attuale della lancetta dei secondi
                    val diffAngle = normalizedNewAngle - secondHandAngle

                    // NON FUNZIONA MANNAGGIA A CHI SO IO

                    // Gestisci il conteggio dei minuti quando la lancetta dei secondi completa un giro completo
                    if (diffAngle > 180) {
                        if (minutes == 0 && seconds == 0) {
                            return@detectDragGestures // Impedisce di muovere indietro se siamo a 0 secondi e 0 minuti
                        }
                        minutes = (minutes - 1 + 60) % 60
                    } else if (diffAngle < -180) {
                        if (minutes == 59 && seconds == 59) {
                            return@detectDragGestures // Impedisce di muovere avanti se siamo a 59 secondi e 59 minuti
                        }
                        minutes = (minutes + 1) % 60
                    }

                    // Calcola i secondi
                    val newSeconds = ((normalizedNewAngle / 6f).toInt() + 60) % 60

                    // Impedisci di andare oltre i limiti
                    if (minutes == 0 && newSeconds == 0 && diffAngle > 0) {
                        return@detectDragGestures
                    }
                    if (minutes == 59 && newSeconds == 59 && diffAngle < 0) {
                        return@detectDragGestures
                    }

                    // Aggiorna l'angolo della lancetta dei secondi
                    secondHandAngle = normalizedNewAngle
                    seconds = newSeconds

                    // Calcola e aggiorna l'angolo della lancetta dei minuti
                    minuteHandAngle = ((minutes * 6) + (secondHandAngle / 60f)) % 360

                    // Debug per controllare i valori
                    Log.d("secondi", "$secondHandAngle $seconds $minutes $minuteHandAngle")
                }

            }
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                fontFamily = FontFamily(
                    Font(R.font.comfortaa, FontWeight.Normal)
                ),
                text = "$minutes m $seconds s",
                color = Color.White,
                fontSize = 20.sp
            )

            Spacer(modifier = Modifier.height(40.dp))

            Canvas(
                modifier = Modifier
                    .size(400.dp)
                    .pointerInput(Unit) {
                        detectTapGestures { tapOffset ->
                            val centerX = size.width / 2
                            val centerY = size.height / 2
                            val radius = (size.height / 5)

                            // Calcola la distanza dal centro
                            val distanceFromCenter = kotlin.math.sqrt(
                                (tapOffset.x - centerX) * (tapOffset.x - centerX) +
                                        (tapOffset.y - centerY) * (tapOffset.y - centerY)
                            ).toInt()

                            // Se il tocco è all'interno di un piccolo raggio dal centro, resetta il tempo
                            if (distanceFromCenter < radius) {
                                resetTime()
                            }
                        }
                    }
            ) {
                val centerX = size.width / 2
                val centerY = size.height / 2
                val radius = size.minDimension / 3

                val markerLength = 30f
                val markers = listOf(0, 15, 30, 45)

                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color(0x63FF9800),
                            Color(0x63FF9800),
                            Color(0x63FF9800),
                            Color(0x63FF9800),
                            Color(0x63FF9800),
                            Color(0x63FF9800),
                            Color(0x63FF9800),
                            Color(0x63FF9800),
                            Color(0x63FF9800),
                            Color(0x63FF9800),
                            Color(0x63FF9800),
                            Color(0x2FFF9800),
                            Color(0x17FF9800),
                            Color(0x05FF9800),
                            Color(0x02FF9800),
                            Color(0x00FFFFFF)
                        ),
                        center = center
                    ),
                    radius = radius + 100f,
                    center = center
                )

                drawCircle(
                    color = Color.Black,
                    radius = radius + 50f,
                    center = center
                )

                // Draw second marks
                for (i in 0 until 60) {
                    val angle = i * 6f
                    val startX = centerX + radius * cos((angle - 90) * PI / 180).toFloat()
                    val startY = centerY + radius * sin((angle - 90) * PI / 180).toFloat()
                    val endX = centerX + (radius - 10) * cos((angle - 90) * PI / 180).toFloat()
                    val endY = centerY + (radius - 10) * sin((angle - 90) * PI / 180).toFloat()
                    drawLine(
                        color = Color.Gray,
                        start = Offset(startX, startY),
                        end = Offset(endX, endY),
                        strokeWidth = 2f
                    )
                }

                // Draw markers
                for (marker in markers) {
                    val markerAngle = marker * 6 - 90
                    val start = Offset(
                        center.x + (radius - markerLength) * cos(markerAngle * PI / 180).toFloat(),
                        center.y + (radius - markerLength) * sin(markerAngle * PI / 180).toFloat()
                    )
                    val end = Offset(
                        center.x + (radius)  * cos(markerAngle * PI / 180).toFloat(),
                        center.y + (radius) * sin(markerAngle * PI / 180).toFloat()
                    )
                    drawLine(
                        color = Color.Gray,
                        start = start,
                        end = end,
                        strokeWidth = 5f,
                        cap = StrokeCap.Round
                    )
                }

                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color(0x69FF9800),
                            Color(0x69FF9800),
                            Color(0x69FF9800),
                            Color(0x69FF9800),
                            Color(0x63FF9800),
                            Color(0x2FFF9800),
                            Color(0x17FF9800),
                            Color(0x10FF9800),
                            Color(0x05FF9800),
                            Color(0x05FF9800),
                            Color(0x02FF9800),
                            Color(0x02FF9800),
                            Color(0x02FF9800),
                            Color(0x00FFFFFF)
                        ),
                        center = center
                    ),
                    radius = radius - 30f,
                    center = center
                )

                drawCircle(
                    color = Color.Black,
                    radius = radius - 60f,
                    center = center
                )

                // Draw seconds arc
                drawArc(
                    color = Color(0xFFFF9800),
                    startAngle = -90f,
                    sweepAngle = secondHandAngle,
                    useCenter = false,
                    topLeft = Offset(center.x - radius , center.y - radius),
                    size = androidx.compose.ui.geometry.Size(radius * 2, radius * 2),
                    style = Stroke(width = 40f)
                )

                drawCircle(
                    color = Color.White,
                    radius = 15f,
                    center = center
                )

                // Draw second hand
                val secondHandEndX =
                    centerX + radius * cos((secondHandAngle - 90) * PI / 180).toFloat()
                val secondHandEndY =
                    centerY + radius * sin((secondHandAngle - 90) * PI / 180).toFloat()
                drawLine(
                    color = Color(0xFFDF6310),
                    start = Offset(centerX, centerY),
                    end = Offset(secondHandEndX, secondHandEndY),
                    strokeWidth = 4f,
                    cap = StrokeCap.Round
                )

                // Draw second hand tail
                val secondHandTailEndX =
                    centerX + ((radius / 2) - 130) * cos((secondHandAngle - 90 + 180) * PI / 180).toFloat()
                val secondHandTailEndY =
                    centerY + ((radius / 2) - 130) * sin((secondHandAngle - 90 + 180) * PI / 180).toFloat()
                drawLine(
                    color = Color(0xFFDF6310),
                    start = Offset(centerX, centerY),
                    end = Offset(secondHandTailEndX, secondHandTailEndY),
                    strokeWidth = 4f,
                    cap = StrokeCap.Round
                )

                // Draw minute hand
                val minuteHandEndX =
                    centerX + (radius - 50) * cos((minuteHandAngle - 90) * PI / 180).toFloat()
                val minuteHandEndY =
                    centerY + (radius - 50) * sin((minuteHandAngle - 90) * PI / 180).toFloat()
                drawLine(
                    color = Color.White,
                    start = Offset(centerX, centerY),
                    end = Offset(minuteHandEndX, minuteHandEndY),
                    strokeWidth = 8f,
                    cap = StrokeCap.Round
                )

                // Draw minute hand tail
                val minuteHandTailEndX =
                    centerX + ((radius / 3) - 75) * cos((minuteHandAngle - 90 + 180) * PI / 180).toFloat()
                val minuteHandTailEndY =
                    centerY + ((radius / 3) - 75) * sin((minuteHandAngle - 90 + 180) * PI / 180).toFloat()
                drawLine(
                    color = Color.White,
                    start = Offset(centerX, centerY),
                    end = Offset(minuteHandTailEndX, minuteHandTailEndY),
                    strokeWidth = 8f,
                    cap = StrokeCap.Round
                )

                drawCircle(
                    color = Color(0xFFDF6310),
                    radius = 8f,
                    center = center
                )
            }

            Spacer(modifier = Modifier.height(40.dp))

            Row {
                MyButton(
                    modifier = Modifier
                        .size(60.dp),
                    text = "+5",
                    onClick = {
                        addSeconds(5)
                    }
                )

                Spacer(modifier = Modifier.width(40.dp))

                MyButton(
                    modifier = Modifier
                        .size(60.dp),
                    text = "✓",
                    onClick = {
                        closeActivity(clock)
                    }
                )
            }

            Spacer(modifier = Modifier.height(40.dp))
        }
    }
}

@Preview(showBackground = true)
@Composable
fun DefaultPreview() {
    ClockApp(Clock())
}
