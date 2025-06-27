package com.example.myorangefit.compose

import android.app.Activity.RESULT_OK
import android.app.Dialog
import android.content.Intent
import android.content.pm.ActivityInfo
import android.graphics.Paint
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.core.content.res.ResourcesCompat
import com.example.myorangefit.R
import com.example.myorangefit.activity.ActivityManager
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin
import java.lang.Math.toRadians
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue

class Scale : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        ActivityManager.add(this)
        super.onCreate(savedInstanceState)
        // Forza l'orientamento in portrait
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        val crtReps = intent.getIntExtra("crtReps", 20)
        val crtWeight = intent.getFloatExtra("crtWeight", 20f)
        setContent {
            ScaleTheme {
                WeightPickerApp(this, crtWeight, crtReps)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WeightPickerApp(
    scale: Scale? = null,
    crtWeight: Float,
    crtReps: Int
) {
    var weight by remember { mutableFloatStateOf(crtWeight) }
    var showDialog by remember { mutableStateOf(false) }
    var count by remember { mutableIntStateOf(crtReps) }
    val customFont = ResourcesCompat.getFont(LocalContext.current, R.font.comfortaa)
    val dialogBackgroundColor = Color(0xFF3A3A3A)

    // --- Stato per la modifica delle ripetizioni ---
    var isEditingReps by remember { mutableStateOf(false) }
    var textReps by remember(count) { mutableStateOf(TextFieldValue(count.toString(), TextRange(count.toString().length))) }
    val focusRequester = remember { FocusRequester() }

    var isPlusPressed by remember { mutableStateOf(false) }
    var isMinusPressed by remember { mutableStateOf(false) }


    Surface(modifier = Modifier.fillMaxSize(), color = Color.Transparent) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            WeightPicker(
                initialWeight = weight,
                onWeightChange = { newWeight ->
                    weight = newWeight.coerceIn(0f, 150f)
                }
            )
            Spacer(modifier = Modifier.height(32.dp))
            MyButton(
                modifier = Modifier
                    .size(60.dp)
                    .offset(y = 30.dp),
                text = "✓",
                onClick = {
                    showDialog = true
                },
                onLongPress = {  }
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
                            // --- Row centrale con ripetizioni editabili ---
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceAround,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                MyButton(
                                    modifier = Modifier.size(60.dp),
                                    text = "-",
                                    onClick = {
                                        if (count > 1) {
                                            count--
                                            val t = count.toString()
                                            textReps = TextFieldValue(t, TextRange(t.length))
                                        }
                                    },
                                    onLongPress = { isMinusPressed = true },
                                    onLongPressRelease = { isMinusPressed = false }
                                )
                                // Numero centrale: click per scrivere, TextField o Text
                                Box(
                                    modifier = Modifier
                                        .size(100.dp)
                                        .pointerInput(Unit) {
                                            detectTapGestures(onTap = { isEditingReps = true })
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (isEditingReps) {
                                        LaunchedEffect(Unit) {
                                            focusRequester.requestFocus()
                                            textReps = textReps.copy(selection = TextRange(textReps.text.length))
                                        }
                                        androidx.compose.material3.TextField(
                                            value = textReps,
                                            onValueChange = {newValue ->
                                                // Solo numeri (ed eventualmente stringa vuota per consentire delete)
                                                if (newValue.text.isEmpty() || newValue.text.all { it.isDigit() }) {
                                                    Log.e("sss", TextRange(textReps.text.length).toString())
                                                    textReps = newValue
                                                    //textReps = newValue.copy(selection = TextRange(newValue.text.length))

                                                    // Aggiorna count live (solo se il testo è valido e non vuoto)
                                                    val num = newValue.text.toIntOrNull()?.coerceAtLeast(1) ?: 1
                                                    count = num

                                                    Log.e("sss", TextRange(newValue.text.length).toString())
                                                    Log.e("sss", count.toString())
                                                }
                                            },
                                            singleLine = true,
                                            textStyle = TextStyle(
                                                color = Color.White,
                                                fontSize = 24.sp,
                                                fontFamily = customFont?.let { FontFamily(it) } ?: FontFamily.Default,
                                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                            ),
                                            colors = androidx.compose.material3.TextFieldDefaults.textFieldColors(
                                                containerColor = Color.Transparent,
                                                focusedIndicatorColor = Color(0xFFFF9800),
                                                unfocusedIndicatorColor = Color(0xFFFF9800),
                                                cursorColor = Color.White
                                            ),
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .focusRequester(focusRequester),
                                            keyboardOptions = KeyboardOptions.Default.copy(
                                                keyboardType = KeyboardType.Number
                                            ),
                                            keyboardActions = KeyboardActions(
                                                onDone = {
                                                    val reps = textReps.text.toIntOrNull()?.coerceAtLeast(1) ?: 1
                                                    count = reps
                                                    val t = count.toString()
                                                    textReps = TextFieldValue(t, TextRange(t.length))
                                                    isEditingReps = false

                                                }
                                            )
                                        )
                                    } else {
                                        Text(
                                            text = textReps.text,
                                            fontFamily = customFont?.let { FontFamily(it) } ?: FontFamily.Default,
                                            fontSize = 24.sp,
                                            color = Color.White,
                                            modifier = Modifier
                                                .pointerInput(Unit) { detectTapGestures(onTap = {
                                                    isEditingReps = true
                                                }) }
                                        )
                                    }
                                }
                                MyButton(
                                    modifier = Modifier.size(60.dp),
                                    text = "+",
                                    onClick = {
                                        count++
                                        val t = count.toString()
                                        textReps = TextFieldValue(t, TextRange(t.length))
                                    },
                                    onLongPress = { isPlusPressed  = true },
                                    onLongPressRelease = { isPlusPressed  = false }
                                )
                                LongPressCounter(
                                    isPressed = isMinusPressed,
                                    onTick = {
                                        if (count > 1) {
                                            count--
                                            val t = count.toString()
                                            textReps = TextFieldValue(t, TextRange(t.length))
                                        }
                                    }
                                )
                                LongPressCounter(
                                    isPressed = isPlusPressed,
                                    onTick = {
                                        count++
                                        val t = count.toString()
                                        textReps = TextFieldValue(t, TextRange(t.length))
                                    }
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
                                        val resultIntent = Intent()
                                        resultIntent.putExtra("weight", weight)
                                        resultIntent.putExtra("reps", count)
                                        scale?.setResult(android.app.Activity.RESULT_OK, resultIntent)
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
fun LongPressCounter(
    isPressed: Boolean,
    onTick: () -> Unit,
    initialDelay: Long = 350,
    minDelay: Long = 35
) {
    LaunchedEffect(isPressed) {
        if (isPressed) {
            var delayMillis = initialDelay
            while (isPressed) {
                onTick()
                kotlinx.coroutines.delay(delayMillis)
                // Accelerazione esponenziale (dimezza il delay ad ogni tick fino al minimo)
                delayMillis = (delayMillis * 0.7).toLong().coerceAtLeast(minDelay)
            }
        }
    }
}


val MyDarkColorScheme = darkColorScheme(
    primary = Color(0xFFFF9800),
    onPrimary = Color.White,
    secondary = Color(0xFFDF6310),
    onSecondary = Color.White,
    // ... altri colori opzionali ...
)


@Composable
fun ScaleTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = MyDarkColorScheme,
        typography = Typography(
            displaySmall = TextStyle(
                fontFamily = FontFamily(
                    Font(R.font.comfortaa, FontWeight.Normal)
                ),
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold
            )
        ),
        content = content
    )
}

@Composable
fun WeightPicker(onWeightChange: (Float) -> Unit, initialWeight: Float) {
    val outerRadius = 450f
    val middleRadius = 390f
    val innerRadius = 360f
    val needleBigRadius = 26f
    val needleSmallRadius = 12f
    val markerLength = 20f

    var rotationAngle by remember { mutableFloatStateOf(-initialWeight * 150 / 160) }
    val customFont = ResourcesCompat.getFont(LocalContext.current, R.font.comfortaa)

    Box(
        modifier = Modifier
            .size(400.dp)
            .pointerInput(Unit) {
                detectHorizontalDragGestures { change, dragAmount ->
                    // Coefficiente per rallentare lo spostamento
                    val dragCoefficient = 0.2f
                    rotationAngle += dragAmount * dragCoefficient / 2.4f
                    rotationAngle = rotationAngle.coerceIn(-150f, 0f)
                    val newWeight = (rotationAngle * (160 / 150f)).coerceIn(0f, 160f)
                    onWeightChange(newWeight)
                    change.consume()
                }
            }
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val center = Offset(size.width / 2, size.height / 2)
            // Cerchi concentrici
            drawCircle(color = Color.DarkGray, radius = outerRadius, center = center)
            drawCircle(color = Color(0xFFFF9800), radius = middleRadius, center = center)
            drawCircle(color = Color(0xFFDF6310), radius = innerRadius, center = center)
            // Arco nero
            drawArc(
                color = Color.Black,
                startAngle = 225f,
                sweepAngle = 90f,
                useCenter = false,
                topLeft = Offset(center.x - (innerRadius - 180), center.y - (innerRadius / 2) - 30),
                size = Size(innerRadius, innerRadius),
                style = Stroke(width = 180f)
            )
            // Marker lungo l'arco
            val numMarkers = 5
            val angleStep = 110 / numMarkers.toFloat()
            for (i in 0 until numMarkers) {
                if (i != 2) {
                    val angle = 225f + angleStep * i
                    val startX = center.x + (innerRadius - 285) * cos(toRadians(angle.toDouble())).toFloat()
                    val startY = center.y + (innerRadius - 290) * sin(toRadians(angle.toDouble())).toFloat() - 50
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
            // Disegna il percorso dell'arco per il testo
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
            clipPath(arcPath) {
                val textPaint = Paint().apply {
                    isAntiAlias = true
                    textSize = 52f
                    color = android.graphics.Color.WHITE
                    textAlign = Paint.Align.CENTER
                    typeface = customFont
                }
                val text = "0 10 20 30 40 50 60 70 80 90 100 110 120 130 140 150"
                val words = text.split(" ")
                val angleStepText = 360f / words.size
                val initialRotationAngle = -90f + rotationAngle * (360f / 150f)
                words.forEachIndexed { index, word ->
                    val angle = initialRotationAngle + angleStepText * index
                    val angleRad = toRadians(angle.toDouble())
                    val x = (center.x + (innerRadius - 130) * cos(angleRad)).toFloat()
                    val y = (center.y + (innerRadius - 150) * sin(angleRad)).toFloat()
                    drawContext.canvas.nativeCanvas.save()
                    drawContext.canvas.nativeCanvas.rotate(angle + 90f, x, y)
                    drawContext.canvas.nativeCanvas.drawText(word, x, y, textPaint)
                    drawContext.canvas.nativeCanvas.restore()
                }
            }
            // Ago fisso
            val needleLength = (innerRadius / 2) - 10
            drawCircle(color = Color.Black, radius = needleBigRadius, center = center)
            drawCircle(color = Color.Yellow, radius = needleSmallRadius, center = center)
            val needlePath = Path().apply {
                moveTo(center.x, center.y)
                lineTo(center.x + needleSmallRadius - 5, center.y)
                lineTo(center.x, center.y - needleLength)
                lineTo(center.x - needleSmallRadius + 5, center.y)
                close()
            }
            drawPath(path = needlePath, color = Color.Yellow)
            // Rettangolo di sfondo per il testo e disegno del valore
            val textValue = (-rotationAngle * (160 / 150f)).roundToInt().toString()
            onWeightChange(textValue.toFloat())
            val textPaint = Paint().apply {
                isAntiAlias = true
                textSize = 64f
                color = android.graphics.Color.WHITE
                textAlign = Paint.Align.CENTER
                typeface = customFont
            }
            val textWidth = textPaint.measureText(textValue)
            val textHeight = textPaint.textSize
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
            drawContext.canvas.nativeCanvas.drawText(
                textValue,
                center.x,
                (rectTop + rectBottom) / 2 + textHeight / 2 - 10,
                textPaint
            )
        }
        // Pulsanti circolari equidistanti dal centro
        CircularButtons(buttonCount = 2, radiusFraction = 0.4f) { index ->
            when (index) {
                0 -> MyButton(
                    modifier = Modifier.size(60.dp),
                    text = "-5",
                    onClick = {
                        if (rotationAngle < 0f) {
                            rotationAngle =
                                ((((rotationAngle * (160f / 150f)).coerceIn(-160f, 0f) + 5) / 5)
                                    .roundToInt() * 5f) * (150f / 160f)
                        }
                    },
                    onLongPress = {  }
                )
                1 -> MyButton(
                    modifier = Modifier.size(60.dp),
                    text = "+5",
                    onClick = {
                        if (rotationAngle > -150f) {
                            rotationAngle =
                                ((((rotationAngle * (160f / 150f)).coerceIn(-160f, 0f) - 5) / 5)
                                    .roundToInt() * 5f) * (150f / 160f)
                        }
                    },
                    onLongPress = {  }
                )
            }
        }
    }
}

@Composable
fun CircularButtons(
    buttonCount: Int = 2,
    radiusFraction: Float = 0.3f,
    buttonContent: @Composable (Int) -> Unit
) {
    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val minSize = if (maxWidth < maxHeight) maxWidth else maxHeight
        val radius = minSize * radiusFraction
        for (i in 0 until buttonCount) {
            // Calcola l'angolo in gradi (partendo dall'alto a -90°)
            val angleDeg = i * (360f / buttonCount)  - 180f
            val angleRad = toRadians(angleDeg.toDouble())
            val offsetX = (radius / 2) * cos(angleRad).toFloat()
            val offsetY = radius * sin(angleRad).toFloat()
            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .offset(x = offsetX, y = offsetY)
            ) {
                buttonContent(i)
            }
        }
    }
}

@Composable
fun MyButton(
    modifier: Modifier = Modifier,
    text: String,
    fontSize: Float = 20f,
    onClick: () -> Unit,
    onLongPress: (() -> Unit)? = null,
    onLongPressRelease: (() -> Unit)? = null
) {
    Box(
        modifier = modifier.pointerInput(Unit) {
            detectTapGestures(
                onTap = { onClick() },
                onLongPress = { onLongPress?.invoke() },
                onPress = {
                    val released = tryAwaitRelease()
                    onLongPressRelease?.invoke()
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


@Preview
@Composable
fun Preview() {
    WeightPickerApp(Scale(), 20f, 20)
}
