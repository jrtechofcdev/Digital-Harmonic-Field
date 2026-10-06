package com.example.music

import kotlin.math.ln
import kotlin.math.sqrt

/**
 * Treinador LEVE do detector de tom, para rodar no próprio celular (versão DEV).
 *
 * Ajusta só a última camada do [KeyModel] (34 números) com os hinos que o
 * músico rotulou, puxando sempre de volta para a camada original (para não
 * "decorar" poucos exemplos). O treino completo, com os áudios, é feito fora do
 * app (tools/key-model) — este aqui serve para medir e experimentar.
 *
 * A avaliação é honesta: VALIDAÇÃO CRUZADA por hino — o hino usado para medir
 * nunca entra no ajuste daquela rodada.
 */
object KeyTrainer {

    /** Um momento de um hino: camada oculta (12 × 16), tom certo (0..23) e o hino. */
    class Sample(val hidden: Array<DoubleArray>, val label: Int, val group: String)

    class Report(
        val samples: Int,
        val hymns: Int,
        /** Acerto do modelo original nos mesmos momentos. */
        val baseAccuracy: Double,
        /** Acerto do ajustado, medido só em hinos fora do ajuste (validação cruzada). */
        val adaptedAccuracy: Double,
        val folds: Int,
    ) {
        val improved: Boolean get() = adaptedAccuracy > baseAccuracy
    }

    /** Ajusta a cabeça com todos os [samples]. */
    fun fit(
        samples: List<Sample>,
        base: KeyModel.Head,
        anchor: Double = 0.05,
        epochs: Int = 150,
        lr: Double = 0.03,
    ): KeyModel.Head {
        val w = base.w2.copyOf()
        val b = base.b2.copyOf()
        if (samples.isEmpty()) return KeyModel.Head(w, b, base.temperature)
        val t = base.temperature
        val hidden = samples[0].hidden[0].size
        // Adam simples (poucos parâmetros, lote inteiro).
        val mw = DoubleArray(w.size); val vw = DoubleArray(w.size)
        val mb = DoubleArray(2); val vb = DoubleArray(2)
        for (step in 1..epochs) {
            val gw = DoubleArray(w.size)
            val gb = DoubleArray(2)
            val head = KeyModel.Head(w, b, t)
            for (s in samples) {
                val p = KeyModel.probabilitiesFromHidden(s.hidden, head)
                p[s.label] -= 1.0
                for (k in 0 until 12) for (m in 0 until 2) {
                    val g = p[k + 12 * m] / t / samples.size
                    gb[m] += g
                    for (j in 0 until hidden) gw[j * 2 + m] += g * s.hidden[k][j]
                }
            }
            for (i in w.indices) gw[i] += anchor * (w[i] - base.w2[i])
            for (i in 0 until 2) gb[i] += anchor * (b[i] - base.b2[i])
            adam(w, gw, mw, vw, lr, step)
            adam(b, gb, mb, vb, lr, step)
        }
        return KeyModel.Head(w, b, t)
    }

    /** Validação cruzada por hino: mede o ajuste sem "colar". */
    fun crossValidate(
        samples: List<Sample>,
        base: KeyModel.Head,
        folds: Int = 4,
        anchor: Double = 0.05,
        epochs: Int = 150,
    ): Report {
        val groups = samples.map { it.group }.distinct().sorted()
        val k = minOf(folds, groups.size).coerceAtLeast(1)
        var baseOk = 0
        var adaptedOk = 0
        for (f in 0 until k) {
            val test = groups.filterIndexed { i, _ -> i % k == f }.toSet()
            val train = samples.filter { it.group !in test }
            val head = if (train.isEmpty()) base else fit(train, base, anchor, epochs)
            for (s in samples.filter { it.group in test }) {
                if (argmax(KeyModel.probabilitiesFromHidden(s.hidden, base)) == s.label) baseOk++
                if (argmax(KeyModel.probabilitiesFromHidden(s.hidden, head)) == s.label) adaptedOk++
            }
        }
        val n = samples.size.coerceAtLeast(1)
        return Report(samples.size, groups.size, baseOk.toDouble() / n, adaptedOk.toDouble() / n, k)
    }

    /** Resultado do treino seguro: a cabeça escolhida e o relatório que a justificou. */
    class Outcome(val head: KeyModel.Head, val report: Report, val anchor: Double?) {
        /** false = nenhum ajuste superou o original; [head] é o original. */
        val adapted: Boolean get() = anchor != null
    }

    /**
     * Treino SEGURO: testa três intensidades de ajuste por validação cruzada e só
     * troca o modelo se alguma for melhor que o original nos seus hinos. Assim o
     * treinador nunca entrega algo pior do que já existe.
     */
    fun train(samples: List<Sample>, base: KeyModel.Head): Outcome {
        var best: Pair<Double, Report>? = null
        for (anchor in doubleArrayOf(0.05, 0.5, 5.0)) {
            val r = crossValidate(samples, base, anchor = anchor)
            if (best == null || r.adaptedAccuracy > best.second.adaptedAccuracy) best = anchor to r
        }
        val (anchor, report) = best ?: return Outcome(base, crossValidate(samples, base), null)
        return if (report.improved) Outcome(fit(samples, base, anchor), report, anchor)
        else Outcome(base, report, null)
    }

    /** Perda média (entropia cruzada) — útil para acompanhar o ajuste. */
    fun loss(samples: List<Sample>, head: KeyModel.Head): Double =
        samples.sumOf { -ln(KeyModel.probabilitiesFromHidden(it.hidden, head)[it.label] + 1e-12) } /
            samples.size.coerceAtLeast(1)

    private fun argmax(p: DoubleArray): Int = p.indices.maxBy { p[it] }

    private fun adam(x: DoubleArray, g: DoubleArray, m: DoubleArray, v: DoubleArray, lr: Double, t: Int) {
        for (i in x.indices) {
            m[i] = 0.9 * m[i] + 0.1 * g[i]
            v[i] = 0.999 * v[i] + 0.001 * g[i] * g[i]
            val mh = m[i] / (1 - Math.pow(0.9, t.toDouble()))
            val vh = v[i] / (1 - Math.pow(0.999, t.toDouble()))
            x[i] -= lr * mh / (sqrt(vh) + 1e-8)
        }
    }
}
