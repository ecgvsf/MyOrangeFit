package com.myorangefit.app.compose

import android.annotation.SuppressLint
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.patrykandpatrick.vico.compose.cartesian.CartesianChartHost
import com.patrykandpatrick.vico.compose.cartesian.axis.rememberAxisGuidelineComponent
import com.patrykandpatrick.vico.compose.cartesian.axis.rememberAxisLabelComponent
import com.patrykandpatrick.vico.compose.cartesian.axis.rememberBottomAxis
import com.patrykandpatrick.vico.compose.cartesian.axis.rememberStartAxis
import com.patrykandpatrick.vico.compose.cartesian.layer.rememberColumnCartesianLayer
import com.patrykandpatrick.vico.compose.cartesian.rememberCartesianChart
import com.patrykandpatrick.vico.compose.cartesian.rememberVicoScrollState
import com.patrykandpatrick.vico.compose.common.component.rememberLineComponent
import com.patrykandpatrick.vico.compose.common.vicoTheme
import com.patrykandpatrick.vico.core.cartesian.data.CartesianChartModelProducer
import com.patrykandpatrick.vico.core.cartesian.data.CartesianValueFormatter
import com.patrykandpatrick.vico.core.cartesian.data.columnSeries
import com.patrykandpatrick.vico.core.cartesian.layer.ColumnCartesianLayer
import com.patrykandpatrick.vico.core.common.shape.Shape

@SuppressLint("RestrictedApi")
@Composable
fun ComposeBarChart(
    data: List<Pair<String, Int>>,
    customColor: Color = Color(0xFFFF9800),
    labelX : Boolean = false,
    labelY : Boolean = true,
    guidelineX: Boolean = true,
    guidelineY: Boolean = true,
) {
    val modelProducer = remember { CartesianChartModelProducer() }

    LaunchedEffect(data) {
        modelProducer.runTransaction {
            columnSeries { series(data.map { it.second }) }
        }
    }

    // Valori custom (puoi modificarli a piacere)
    val barWidth = 64.dp
    val barRoundness = 100 // percentuale

    val columnLayer = rememberColumnCartesianLayer(
        columnProvider = ColumnCartesianLayer.ColumnProvider.series(
            vicoTheme.columnCartesianLayerColors.map {
                rememberLineComponent(
                    color = customColor,
                    thickness = barWidth,
                    shape = Shape.rounded(barRoundness)
                )
            }
        )
    )

    val scrollSpec = rememberVicoScrollState(
        scrollEnabled  = false                  // disabilita ogni scrolling
    )

    val startAxis = rememberStartAxis(
        label = if (labelY) rememberAxisLabelComponent() else rememberAxisLabelComponent(),
        guideline = if (guidelineX) rememberAxisGuidelineComponent() else null
    )

    // 1) Prepara la lista delle etichette
    val labels = data.map { it.first }

    // 2) Crea il formatter: valore → label (o stringa vuota se fuori range)
    val customFormatter = CartesianValueFormatter { value, _, _ ->
        labels.getOrNull(value.toInt()) ?: ""
    }


    var bottomAxis = rememberBottomAxis(
        label = if (labelX) rememberAxisLabelComponent() else null,
        valueFormatter = if (labelX) customFormatter else CartesianValueFormatter.decimal(),
        guideline = if (guidelineY) rememberAxisGuidelineComponent() else null
    )

    CartesianChartHost(
        chart = rememberCartesianChart(
            columnLayer,
            startAxis = startAxis,
            bottomAxis = bottomAxis
        ),
        modelProducer = modelProducer,
        scrollState = scrollSpec,
    )
}

@Preview
@Composable
private fun VerticalBarChartPreview() {
    ComposeBarChart(
        data = listOf(
            "Lun" to 4,
            "Mar" to 2,
            "Mer" to 6,
            "Gio" to 3,
            "Ven" to 5,
            "Sab" to 1,
            "Dom" to 4
        )
    )
}
