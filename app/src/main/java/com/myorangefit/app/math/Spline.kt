package com.myorangefit.app.math

// Cubic Spline naturale: interpolazione di punti (x, y)
// Puoi usare questa classe per generare punti interpolati e poi mostrarli con una libreria grafica come Vico.

class Spline(private val x: DoubleArray, private val y: DoubleArray) {
    private val n = x.size
    private val a = y.copyOf()
    private val b = DoubleArray(n - 1)
    private val d = DoubleArray(n - 1)
    private val h = DoubleArray(n - 1)
    private val c = DoubleArray(n)

    init {
        require(x.size == y.size && x.size >= 2) { "Array x e y devono avere almeno due valori e stessa dimensione." }

        // Calcolo intervalli
        for (i in 0 until n - 1) {
            h[i] = x[i + 1] - x[i]
            require(h[i] > 0) { "x deve essere strettamente crescente" }
        }

        // Sistema tridiagonale
        val alpha = DoubleArray(n - 1)
        for (i in 1 until n - 1) {
            alpha[i] = (3 / h[i]) * (a[i + 1] - a[i]) - (3 / h[i - 1]) * (a[i] - a[i - 1])
        }

        val l = DoubleArray(n)
        val mu = DoubleArray(n)
        val z = DoubleArray(n)
        l[0] = 1.0
        mu[0] = 0.0
        z[0] = 0.0

        for (i in 1 until n - 1) {
            l[i] = 2 * (x[i + 1] - x[i - 1]) - h[i - 1] * mu[i - 1]
            mu[i] = h[i] / l[i]
            z[i] = (alpha[i] - h[i - 1] * z[i - 1]) / l[i]
        }

        l[n - 1] = 1.0
        z[n - 1] = 0.0
        c[n - 1] = 0.0

        for (j in n - 2 downTo 0) {
            c[j] = z[j] - mu[j] * c[j + 1]
            b[j] = (a[j + 1] - a[j]) / h[j] - h[j] * (c[j + 1] + 2 * c[j]) / 3
            d[j] = (c[j + 1] - c[j]) / (3 * h[j])
        }
    }

    /**
     * Interpolazione: dato un valore xValue, calcola il valore y interpolato.
     * x deve essere ordinato in modo strettamente crescente!
     */
    fun interpolate(xValue: Double): Double {
        // Trova il segmento giusto (puoi ottimizzare con binary search se hai molti punti)
        var i = x.asList().indexOfFirst { it > xValue }
        if (i == -1) i = x.size - 1
        i -= 1
        if (i < 0) i = 0
        if (i > x.size - 2) i = x.size - 2

        val dx = xValue - x[i]
        return a[i] + b[i] * dx + c[i] * dx * dx + d[i] * dx * dx * dx
    }
}
