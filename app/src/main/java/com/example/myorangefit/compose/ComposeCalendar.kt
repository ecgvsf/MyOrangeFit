package com.example.myorangefit.compose

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import com.example.myorangefit.R
import com.patrykandpatrick.vico.compose.cartesian.CartesianChartHost
import com.patrykandpatrick.vico.compose.cartesian.axis.rememberBottomAxis
import com.patrykandpatrick.vico.compose.cartesian.axis.rememberStartAxis
import com.patrykandpatrick.vico.compose.cartesian.layer.rememberLine
import com.patrykandpatrick.vico.compose.cartesian.layer.rememberLineCartesianLayer
import com.patrykandpatrick.vico.compose.cartesian.rememberCartesianChart
import com.patrykandpatrick.vico.compose.common.fill
import com.patrykandpatrick.vico.core.cartesian.data.CartesianChartModelProducer
import com.patrykandpatrick.vico.core.cartesian.data.lineSeries
import com.patrykandpatrick.vico.core.cartesian.layer.LineCartesianLayer
import com.patrykandpatrick.vico.core.cartesian.marker.CartesianMarker

@Composable
fun ComposeCalendar(list: MutableList<Pair<String,Int>>) {
    val modelProducer = remember { CartesianChartModelProducer() }
    LaunchedEffect(Unit) { modelProducer.runTransaction { lineSeries { series(list.map { it.second }.toMutableList()) } } }
    CartesianChartHost(
        rememberCartesianChart(
            rememberLineCartesianLayer(
                rememberLineCartesianLayer(
                    lineProvider =  LineCartesianLayer.LineProvider.series(rememberLine(
                        fill = LineCartesianLayer.LineFill.single(fill( Color(0xFFFF9800)))))
                ).lineProvider
            ),
            startAxis = rememberStartAxis(),
            bottomAxis = rememberBottomAxis(),
        ),
        modelProducer,
    )
}


@Preview
@Composable
private fun ComposeCalendarPreview() {
    ComposeCalendar(mutableListOf(Pair("a",1)))
}