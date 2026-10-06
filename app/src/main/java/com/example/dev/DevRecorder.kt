package com.example.dev

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.example.audio.SessionTap
import com.example.music.KeyDetector
import com.example.music.KeyModel
import com.example.music.KeyResult

/** Resumo de uma leitura do detector, como vai para o JSON. */
class Reading(
    val t: Double,
    val status: String,
    val top: List<Pair<String, Double>>,
    val voicedSeconds: Double,
    val bassSeconds: Double,
    val tuningCents: Double,
    val betweenKeys: Boolean,
) {
    companion object {
        fun of(r: KeyResult, t: Double) = Reading(
            t, r.status.name, r.candidates.map { it.keyCipher to it.probability },
            r.voicedSeconds, r.bassSeconds, r.tuningOffsetCents, r.betweenKeys,
        )
    }
}

/** Tudo o que aconteceu num hino (sessão) até agora. */
class SessionDraft(val id: String, val startedAt: Long, val sampleRate: Int) {
    /** Leitura no fim de cada rodada de 5 s (5, 10, 15… s). */
    val rounds = ArrayList<Reading>()
    /** Leituras ao vivo (~a cada 0,3 s): tom mais provável e chance. */
    val timeline = ArrayList<Reading>()
    /** Entradas do modelo a cada 2,5 s (para o treinador do app). */
    val features = ArrayList<Pair<Double, DoubleArray>>()
    val stops = ArrayList<Pair<Double, String>>()
    var listenMore = 0
    var final: Reading? = null
    /** Fader de sensibilidade no início (null = automático). */
    var manualGainDb: Double? = null
    /** Ganho realmente aplicado no fim da rodada (dB). */
    var appliedGainDb: Double = 0.0
    var model: String = KeyModel.activeName
}

/**
 * Grava a sessão de detecção da versão DEV: o áudio EXATO que o detector ouviu
 * (pré-buffer + ao vivo + "ouvir mais") e o que ele pensou em cada momento.
 * Ao fim de cada rodada a sessão é salva sozinha como "pendente" na pasta de
 * treino; o rótulo (tom certo, estrelas…) vem depois, pela tela.
 *
 * Os métodos de [SessionTap] rodam na thread de áudio; o resto, na tela.
 */
class DevRecorder(private val store: DevStore) : SessionTap {

    companion object {
        private const val MAX_SECONDS = 90
        private const val FEATURE_STEP = 2.5
    }

    private val lock = Any()
    private var audio = ShortArray(0)
    private var size = 0
    private var draft: SessionDraft? = null
    private var nextRound = 5.0
    private var nextFeature = FEATURE_STEP

    /** Sessão em andamento/na tela (para o cartão de rótulo). */
    var currentId by mutableStateOf<String?>(null)
        private set

    override fun onStart(sampleRate: Int, manualGainDb: Double?) {
        val d = SessionDraft(store.newSessionId(), System.currentTimeMillis(), sampleRate)
        d.manualGainDb = manualGainDb
        synchronized(lock) {
            audio = ShortArray(sampleRate * 20)
            size = 0
            draft = d
            nextRound = 5.0
            nextFeature = FEATURE_STEP
        }
        currentId = d.id
        store.log("deteccao_inicio", "sessao" to d.id, "modelo" to d.model)
    }

    override fun onAudio(samples: ShortArray, count: Int) {
        synchronized(lock) {
            val d = draft ?: return
            val max = d.sampleRate * MAX_SECONDS
            if (size + count > max) return
            if (size + count > audio.size) audio = audio.copyOf(minOf(max, maxOf(audio.size * 2, size + count)))
            System.arraycopy(samples, 0, audio, size, count)
            size += count
        }
    }

    override fun onEvaluation(detector: KeyDetector, result: KeyResult) {
        val t = detector.secondsFed
        synchronized(lock) {
            val d = draft ?: return
            val reading = Reading.of(result, t)
            d.timeline.add(reading)
            while (t >= nextRound - 0.05) {
                d.rounds.add(reading)
                nextRound += 5.0
            }
            if (t >= nextFeature - 0.05) {
                d.features.add(t to KeyModel.features(detector.snapshot()))
                nextFeature = (Math.floor(t / FEATURE_STEP) + 1) * FEATURE_STEP
            }
        }
    }

    override fun onDone(detector: KeyDetector, result: KeyResult, reason: String) {
        val t = detector.secondsFed
        // Monta o JSON aqui dentro: a thread de áudio continua mexendo no rascunho
        // depois (se o músico pedir "ouvir mais").
        val saved = synchronized(lock) {
            val d = draft ?: return
            d.final = Reading.of(result, t)
            d.appliedGainDb = detector.gainDb
            d.stops.add(t to reason)
            if (d.features.none { kotlin.math.abs(it.first - t) < 0.3 }) {
                d.features.add(t to KeyModel.features(detector.snapshot()))
            }
            DevStore.Pending(d.id, DevJson.session(d, size), DevJson.features(d), audio.copyOf(size), d.sampleRate)
        }
        store.saveSession(saved)
    }

    override fun onListenMore() {
        synchronized(lock) { draft?.listenMore = (draft?.listenMore ?: 0) + 1 }
        store.log("ouvir_mais", "sessao" to (currentId ?: ""))
    }

    override fun onCancel() {
        val id = synchronized(lock) {
            val id = draft?.id
            draft = null
            size = 0
            id
        }
        // Cancelado antes do primeiro resultado: nada a salvar. Se já havia um
        // resultado salvo, ele continua na pasta (pendente).
        store.log("deteccao_cancelada", "sessao" to (id ?: ""))
    }
}
