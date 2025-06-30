package com.myorangefit.app.compose

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.myorangefit.app.math.Spline
import com.patrykandpatrick.vico.compose.cartesian.CartesianChartHost
import com.patrykandpatrick.vico.compose.cartesian.axis.rememberBottomAxis
import com.patrykandpatrick.vico.compose.cartesian.axis.rememberStartAxis
import com.patrykandpatrick.vico.compose.cartesian.layer.rememberLine
import com.patrykandpatrick.vico.compose.cartesian.layer.rememberLineCartesianLayer
import com.patrykandpatrick.vico.compose.cartesian.rememberCartesianChart
import com.patrykandpatrick.vico.compose.cartesian.rememberVicoScrollState
import com.patrykandpatrick.vico.compose.common.fill
import com.patrykandpatrick.vico.core.cartesian.data.CartesianChartModelProducer
import com.patrykandpatrick.vico.core.cartesian.data.lineSeries
import com.patrykandpatrick.vico.core.cartesian.layer.LineCartesianLayer

@Composable
fun ComposeLineChart(
    list: List<Pair<String, Int>>,
    color: Color = Color(0xFFFF9800),
    showHorizontalGrid: Boolean = true,
    showVerticalGrid: Boolean = true
) {
    val scrollState = rememberVicoScrollState(scrollEnabled = false)

    val x = (1..list.size).map { it.toDouble() }.toDoubleArray()
    val y = list.map { it.second.toDouble() }.toDoubleArray()
    val z = list.map { it.first }

    // Costruisci la spline
    val spline = remember(x, y) { Spline(x, y) }
    val xMin = x.first()
    val xMax = x.last()

    // Genera punti interpolati
    val smoothY: List<Float> = remember(x, y) {
        (0..100).map { i ->
            val xVal = xMin + i * (xMax - xMin) / 100
            spline.interpolate(xVal).toFloat()
        }
    }

    val modelProducer = remember { CartesianChartModelProducer() }
    LaunchedEffect(smoothY) {
        modelProducer.runTransaction {
            lineSeries { series(smoothY) }
        }
    }

    // Definizione linea smooth/cubic
    val line = rememberLine(
        fill = LineCartesianLayer.LineFill.single(fill(color.copy(alpha = 0.15f))),
        thickness = 4.dp,
        pointConnector = remember { LineCartesianLayer.PointConnector.cubic() },
    )

    // Layer del grafico
    val lineLayer = rememberLineCartesianLayer(
        lineProvider =  LineCartesianLayer.LineProvider.series(line)
    )

    // Griglie (axes)
    val startAxis = rememberStartAxis(
        tickLength = 0.dp // Imposta a 0dp se vuoi solo la griglia senza tick
    )
    val bottomAxis = rememberBottomAxis(
        tickLength = 0.dp
    )

    CartesianChartHost(
        chart = rememberCartesianChart(
            lineLayer,
            startAxis = startAxis,
        ),
        modelProducer = modelProducer,
        scrollState = scrollState
    )
}



@Preview
@Composable
private fun ComposeChartPreview() {
    ComposeLineChart(
        listOf(
            "a" to 2,
            "b" to 7,
            "c" to 3,
            "d" to 12,
            "e" to 5,
            "f" to 14
        ), Color(0xFFFF9800)
    )
}




































