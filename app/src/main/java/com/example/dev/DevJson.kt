package com.example.dev

import com.example.HarmonicDatabase
import com.example.music.pitchClassCiphers
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Monta o JSON das sessões (formato descrito no LEIA-ME e em docs/treino-real.md). */
object DevJson {

    fun reading(r: Reading): JSONObject = JSONObject()
        .put("t", round2(r.t))
        .put("status", r.status)
        .put("top", JSONArray(r.top.map { (k, p) ->
            JSONObject().put("tom", k).put("nome", HarmonicDatabase.ptNameByCipher[k] ?: k).put("chance", round3(p))
        }))
        .put("voz_s", round2(r.voicedSeconds))
        .put("baixo_s", round2(r.bassSeconds))
        .put("afinacao_cents", Math.round(r.tuningCents))
        .put("entre_tons", r.betweenKeys)

    fun session(d: SessionDraft, samples: Int): JSONObject {
        val start = Date(d.startedAt)
        return JSONObject()
            .put("id", d.id)
            .put("arquivo_audio", "audio/${d.id}.wav")
            .put("inicio", SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX", Locale.US).format(start))
            .put("inicio_legivel", SimpleDateFormat("dd/MM HH:mm", Locale("pt", "BR")).format(start))
            .put("duracao_s", round2(samples / d.sampleRate.toDouble()))
            .put("taxa_amostragem", d.sampleRate)
            .put("modelo", d.model)
            .put("ouvir_mais", d.listenMore)
            .put("ganho", JSONObject()
                .put("modo", if (d.manualGainDb == null) "automatico" else "manual")
                .put("fader_db", d.manualGainDb ?: JSONObject.NULL)
                .put("aplicado_db", Math.round(d.appliedGainDb * 10) / 10.0))
            .put("paradas", JSONArray(d.stops.map { (t, why) -> JSONObject().put("t", round2(t)).put("motivo", why) }))
            .put("rodadas", JSONArray(d.rounds.map { reading(it) }))
            .put("resultado_final", d.final?.let { reading(it) } ?: JSONObject.NULL)
            .put("linha_do_tempo", JSONArray(d.timeline.map { r ->
                JSONArray().put(round2(r.t)).put(r.top.firstOrNull()?.first ?: "-")
                    .put(round3(r.top.firstOrNull()?.second ?: 0.0)).put(r.status)
            }))
            .put("status", "pendente")
    }

    fun features(d: SessionDraft): JSONObject = JSONObject()
        .put("id", d.id)
        .put("modelo", d.model)
        .put("pontos", JSONArray(d.features.map { (t, x) ->
            JSONObject().put("t", round2(t)).put("x", JSONArray(x.map { round4(it) }))
        }))

    private fun round2(x: Double) = Math.round(x * 100) / 100.0
    private fun round3(x: Double) = Math.round(x * 1000) / 1000.0
    private fun round4(x: Double) = Math.round(x * 10000) / 10000.0
}

/** Classifica o erro do app em relação ao tom certo (para as estatísticas). */
object DevEval {
    fun errorKind(predicted: String?, truth: String): String? {
        if (predicted == truth) return null
        if (predicted == null) return "sem_resposta"
        val pm = predicted.endsWith("m"); val tm = truth.endsWith("m")
        val pr = pitchClassCiphers.indexOf(predicted.removeSuffix("m"))
        val tr = pitchClassCiphers.indexOf(truth.removeSuffix("m"))
        if (pr < 0 || tr < 0) return "outro"
        val d = Math.floorMod(pr - tr, 12)
        return when {
            pm != tm && pr == tr -> "homonima"                      // G ↔ Gm
            pm != tm && (if (tm) d == 3 else d == 9) -> "relativa"  // G ↔ Em
            pm == tm && d == 7 -> "quinta_acima"                     // disse D, era G
            pm == tm && d == 5 -> "quarta_acima"                     // disse C, era G
            pm == tm && (d == 1 || d == 11) -> "semitom"             // grupo entre dois tons
            else -> "outro"
        }
    }
}
