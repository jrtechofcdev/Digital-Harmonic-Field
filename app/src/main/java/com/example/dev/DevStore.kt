package com.example.dev

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.compose.runtime.mutableIntStateOf
import androidx.core.content.FileProvider
import com.example.BuildConfig
import com.example.HarmonicDatabase
import com.example.music.KeyModel
import com.example.music.KeyTrainer
import com.example.music.pitchClassCiphers
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.RandomAccessFile
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.Executors
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

/**
 * Pasta de TREINO REAL da versão DEV. Tudo fica num único diretório, pronto para
 * compactar e enviar:
 *
 * ```
 * treino-real/
 *   LEIA-ME.txt            o que é cada arquivo
 *   sessoes.json           1 entrada por hino (atualiza a cada hino e a cada rótulo)
 *   log.jsonl              diário de eventos (1 linha JSON por evento)
 *   audio/<sessão>.wav     o áudio exato que o detector ouviu (44,1 kHz, mono)
 *   features/<sessão>.json entradas do modelo a cada 2,5 s (para o treinador)
 *   modelo_ajustado.json   última camada ajustada no aparelho (se usada)
 *   treino_relatorios.json resultados do treinador do app
 * ```
 * Só existe na versão DEV ([BuildConfig.DEV_TOOLS]); o app normal nunca grava.
 */
object DevStore {

    /** Sessão pronta para gravar (montada pelo [DevRecorder]). */
    class Pending(
        val id: String,
        val meta: JSONObject,
        val features: JSONObject,
        val pcm: ShortArray,
        val sampleRate: Int,
    )

    /** Linha da lista de sessões na tela. */
    class Summary(
        val id: String,
        val startedAt: String,
        val seconds: Double,
        val detected: String?,
        val detectedStatus: String?,
        val label: String?,
        val labelKind: String?,   // "tom", "nao_sei", "nao_era_hino"
        val correct: Boolean?,
        val stars: Int?,
        val audioFile: File,
    )

    class Stats(
        val total: Int,
        val labeled: Int,
        val pending: Int,
        val roundAccuracy: List<Pair<Int, Double>>,   // (rodada, acerto) — só rotulados
        val roundCounts: List<Int>,
        val finalAccuracy: Double?,
        val top3Accuracy: Double?,
        val avgStars: Double?,
        val errors: Map<String, Int>,
        val megabytes: Double,
    )

    private val io = Executors.newSingleThreadExecutor()
    private val sessions = ArrayList<JSONObject>()
    private lateinit var appContext: Context

    lateinit var dir: File
        private set

    /** Muda sempre que algo é gravado — a tela observa para se atualizar. */
    val version = mutableIntStateOf(0)

    val recorder by lazy { DevRecorder(this) }

    val enabled: Boolean get() = BuildConfig.DEV_TOOLS

    private const val PREFS = "dev_treino"
    private const val PREF_ADAPTED = "usar_modelo_ajustado"

    /** Prepara a pasta. [force] só para testes (no app, só a versão DEV liga). */
    fun init(context: Context, force: Boolean = false) {
        if (!(enabled || force) || ::dir.isInitialized) return
        appContext = context.applicationContext
        dir = File(appContext.getExternalFilesDir(null) ?: appContext.filesDir, "treino-real")
        File(dir, "audio").mkdirs()
        File(dir, "features").mkdirs()
        writeReadme()
        val f = File(dir, "sessoes.json")
        if (f.exists()) runCatching {
            val arr = JSONObject(f.readText()).getJSONArray("sessoes")
            for (i in 0 until arr.length()) sessions.add(arr.getJSONObject(i))
        }
        counter = sessions.mapNotNull { it.optString("id").drop(1).take(4).toIntOrNull() }.maxOrNull() ?: 0
        if (useAdapted) loadAdaptedHead()
        log(
            "app_aberto",
            "versao" to BuildConfig.VERSION_NAME,
            "aparelho" to "${Build.MANUFACTURER} ${Build.MODEL} (Android ${Build.VERSION.RELEASE})",
            "modelo" to KeyModel.activeName,
        )
    }

    // ------------------------------------------------------------------ sessões

    private var counter = 0

    /** Próximo número de sessão (s0001, s0002…) + data/hora. */
    @Synchronized
    fun newSessionId(): String {
        counter++
        return "s%04d_%s".format(counter, stamp("yyyyMMdd-HHmmss"))
    }

    /** Grava (ou atualiza) a sessão como pendente, sem perder um rótulo já dado. */
    fun saveSession(p: Pending) = io.execute {
        writeWav(File(dir, "audio/${p.id}.wav"), p.pcm, p.sampleRate)
        File(dir, "features/${p.id}.json").writeText(p.features.toString())
        synchronized(this) {
            val old = sessions.indexOfFirst { it.optString("id") == p.id }
            if (old >= 0) {
                val prev = sessions[old]
                prev.optJSONObject("rotulo")?.let { p.meta.put("rotulo", it) }
                p.meta.put("status", prev.optString("status", "pendente"))
                sessions[old] = p.meta
                p.meta.optJSONObject("rotulo")?.let { p.meta.put("avaliacao", evaluate(p.meta, it)) }
            } else {
                sessions.add(p.meta)
            }
            writeSessions()
        }
        log(
            "sessao_salva", "sessao" to p.id, "duracao_s" to round1(p.pcm.size / p.sampleRate.toDouble()),
            "detectado" to p.meta.optJSONObject("resultado_final")?.topKey(),
        )
        bump()
    }

    /** Rótulo dado pelo músico (tom certo, estrelas, etiquetas…). */
    fun saveLabel(id: String, label: JSONObject) = io.execute {
        synchronized(this) {
            val s = sessions.firstOrNull { it.optString("id") == id } ?: return@execute
            label.put("rotulado_em", stamp("yyyy-MM-dd'T'HH:mm:ssXXX"))
            s.put("rotulo", label)
            s.put("status", "rotulado")
            s.put("avaliacao", evaluate(s, label))
            writeSessions()
        }
        log("rotulo_salvo", "sessao" to id, "rotulo" to label)
        bump()
    }

    fun discard(id: String) = io.execute {
        synchronized(this) {
            sessions.removeAll { it.optString("id") == id }
            writeSessions()
        }
        File(dir, "audio/$id.wav").delete()
        File(dir, "features/$id.json").delete()
        log("sessao_descartada", "sessao" to id)
        bump()
    }

    @Synchronized
    fun session(id: String): JSONObject? = sessions.firstOrNull { it.optString("id") == id }?.let { JSONObject(it.toString()) }

    @Synchronized
    fun summaries(): List<Summary> = sessions.reversed().map { s ->
        val fin = s.optJSONObject("resultado_final")
        val lab = s.optJSONObject("rotulo")
        val kind = lab?.let { if (it.optBoolean("nao_era_hino")) "nao_era_hino" else if (it.optString("tom").isNotEmpty()) "tom" else "nao_sei" }
        Summary(
            id = s.optString("id"),
            startedAt = s.optString("inicio_legivel"),
            seconds = s.optDouble("duracao_s", 0.0),
            detected = fin?.topKey(),
            detectedStatus = fin?.optString("status"),
            label = lab?.optString("tom")?.ifEmpty { null },
            labelKind = kind,
            correct = s.optJSONObject("avaliacao")?.takeIf { it.has("acertou_final") }?.optBoolean("acertou_final"),
            stars = lab?.optInt("estrelas", 0)?.takeIf { it > 0 },
            audioFile = File(dir, "audio/${s.optString("id")}.wav"),
        )
    }

    @Synchronized
    fun stats(): Stats {
        val labeled = sessions.filter { it.optJSONObject("rotulo")?.optString("tom")?.isNotEmpty() == true }
        val rounds = (1..4).map { r ->
            val vals = labeled.mapNotNull { s ->
                s.optJSONObject("avaliacao")?.optJSONArray("acertou_rodada")?.let { a ->
                    if (a.length() >= r) a.optBoolean(r - 1) else null
                }
            }
            r to vals
        }.filter { it.second.isNotEmpty() }
        val finals = labeled.mapNotNull { it.optJSONObject("avaliacao")?.optBoolean("acertou_final") }
        val top3 = labeled.mapNotNull { it.optJSONObject("avaliacao")?.optBoolean("certo_entre_3") }
        val stars = sessions.mapNotNull { it.optJSONObject("rotulo")?.optInt("estrelas", 0)?.takeIf { s -> s > 0 } }
        val errors = labeled.mapNotNull { it.optJSONObject("avaliacao")?.optString("tipo_erro")?.ifEmpty { null } }
            .groupingBy { it }.eachCount()
        val bytes = dir.walkTopDown().filter { it.isFile }.sumOf { it.length() }
        return Stats(
            total = sessions.size,
            labeled = sessions.count { it.has("rotulo") },
            pending = sessions.count { !it.has("rotulo") },
            roundAccuracy = rounds.map { (r, v) -> r to v.count { it }.toDouble() / v.size },
            roundCounts = rounds.map { it.second.size },
            finalAccuracy = finals.takeIf { it.isNotEmpty() }?.let { f -> f.count { it }.toDouble() / f.size },
            top3Accuracy = top3.takeIf { it.isNotEmpty() }?.let { f -> f.count { it }.toDouble() / f.size },
            avgStars = stars.takeIf { it.isNotEmpty() }?.average(),
            errors = errors,
            megabytes = bytes / 1_048_576.0,
        )
    }

    // ------------------------------------------------------------------ treinador

    val useAdapted: Boolean
        get() = appContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getBoolean(PREF_ADAPTED, false)

    val hasAdaptedModel: Boolean get() = File(dir, "modelo_ajustado.json").exists()

    fun setUseAdapted(on: Boolean) {
        appContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putBoolean(PREF_ADAPTED, on).apply()
        if (on) loadAdaptedHead() else KeyModel.adaptedHead = null
        log("modelo_ajustado_" + if (on) "ligado" else "desligado")
        bump()
    }

    /** Monta os exemplos de treino a partir dos hinos rotulados com o tom certo. */
    fun trainingSamples(): List<KeyTrainer.Sample> {
        val labeled = synchronized(this) {
            sessions.filter { it.optJSONObject("rotulo")?.optString("tom")?.isNotEmpty() == true }
                .map { it.optString("id") to it.getJSONObject("rotulo").getString("tom") }
        }
        val out = ArrayList<KeyTrainer.Sample>()
        for ((id, tom) in labeled) {
            val label = keyIndex(tom) ?: continue
            val f = File(dir, "features/$id.json")
            if (!f.exists()) continue
            val pts = runCatching { JSONObject(f.readText()).getJSONArray("pontos") }.getOrNull() ?: continue
            for (i in 0 until pts.length()) {
                val arr = pts.getJSONObject(i).getJSONArray("x")
                if (arr.length() != KeyModel.DIM) continue
                val x = DoubleArray(arr.length()) { arr.getDouble(it) }
                out.add(KeyTrainer.Sample(KeyModel.hidden(x), label, id))
            }
        }
        return out
    }

    /**
     * Treino seguro com os hinos rotulados. Só grava modelo_ajustado.json se o
     * ajuste foi MELHOR que o original na validação cruzada; senão mantém o
     * original (e registra o relatório do mesmo jeito).
     */
    fun runTrainer(): KeyTrainer.Outcome {
        val samples = trainingSamples()
        val outcome = KeyTrainer.train(samples, KeyModel.baseHead)
        val report = outcome.report
        val f = File(dir, "modelo_ajustado.json")
        if (outcome.adapted) {
            val head = outcome.head
            f.writeText(
                JSONObject()
                    .put("criado_em", stamp("yyyy-MM-dd'T'HH:mm:ssXXX"))
                    .put("w2", JSONArray(head.w2.toList()))
                    .put("b2", JSONArray(head.b2.toList()))
                    .put("temperatura", head.temperature)
                    .put("ancora", outcome.anchor)
                    .put("amostras", report.samples)
                    .put("hinos", report.hymns)
                    .toString(1),
            )
        } else {
            f.delete()
            if (useAdapted) setUseAdapted(false)
        }
        val rep = JSONObject()
            .put("quando", stamp("yyyy-MM-dd'T'HH:mm:ssXXX"))
            .put("amostras", report.samples).put("hinos", report.hymns).put("dobras", report.folds)
            .put("acerto_base", round3(report.baseAccuracy))
            .put("acerto_ajustado_validacao_cruzada", round3(report.adaptedAccuracy))
            .put("ajuste_aceito", outcome.adapted)
        val rf = File(dir, "treino_relatorios.json")
        val all = runCatching { JSONArray(rf.readText()) }.getOrDefault(JSONArray())
        all.put(rep)
        rf.writeText(all.toString(1))
        log("treinador", "relatorio" to rep)
        if (useAdapted) loadAdaptedHead()
        bump()
        return outcome
    }

    private fun loadAdaptedHead() {
        val f = File(dir, "modelo_ajustado.json")
        if (!f.exists()) { KeyModel.adaptedHead = null; return }
        runCatching {
            val j = JSONObject(f.readText())
            val w = j.getJSONArray("w2"); val b = j.getJSONArray("b2")
            KeyModel.adaptedHead = KeyModel.Head(
                DoubleArray(w.length()) { w.getDouble(it) },
                DoubleArray(b.length()) { b.getDouble(it) },
                j.getDouble("temperatura"),
            )
        }
    }

    // ------------------------------------------------------------------ exportar

    /** Compacta a pasta inteira num .zip (no cache) e devolve o arquivo. */
    fun exportZip(): File {
        awaitIdle()
        val outDir = File(appContext.cacheDir, "exportacao").apply { mkdirs() }
        outDir.listFiles()?.forEach { it.delete() }
        val zip = File(outDir, "treino-real_${stamp("yyyyMMdd-HHmm")}.zip")
        ZipOutputStream(zip.outputStream().buffered()).use { z ->
            dir.walkTopDown().filter { it.isFile }.forEach { f ->
                z.putNextEntry(ZipEntry("treino-real/" + f.relativeTo(dir).path))
                f.inputStream().use { it.copyTo(z) }
                z.closeEntry()
            }
        }
        log("exportado", "arquivo" to zip.name, "mb" to round1(zip.length() / 1_048_576.0))
        return zip
    }

    /** Intenção de compartilhar o .zip (WhatsApp, Drive, e-mail…). */
    fun shareIntent(zip: File): Intent {
        val uri = FileProvider.getUriForFile(appContext, appContext.packageName + ".arquivos", zip)
        return Intent(Intent.ACTION_SEND).apply {
            type = "application/zip"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, "Treino real — detector de tom (${zip.name})")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }.let { Intent.createChooser(it, "Enviar material de treino") }
    }

    /** Copia o .zip para Downloads/DHF-treino (Android 10+). */
    fun saveToDownloads(zip: File): Uri? {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return null
        val values = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, zip.name)
            put(MediaStore.MediaColumns.MIME_TYPE, "application/zip")
            put(MediaStore.MediaColumns.RELATIVE_PATH, "Download/DHF-treino")
        }
        val resolver = appContext.contentResolver
        val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values) ?: return null
        resolver.openOutputStream(uri)?.use { out -> zip.inputStream().use { it.copyTo(out) } }
        log("salvo_em_downloads", "arquivo" to zip.name)
        return uri
    }

    // ------------------------------------------------------------------ log

    fun log(event: String, vararg fields: Pair<String, Any?>) {
        if (!::dir.isInitialized) return
        val line = JSONObject().put("quando", stamp("yyyy-MM-dd'T'HH:mm:ss.SSSXXX")).put("evento", event)
        for ((k, v) in fields) line.put(k, v ?: JSONObject.NULL)
        io.execute { File(dir, "log.jsonl").appendText(line.toString() + "\n") }
    }

    /** Espera as gravações pendentes terminarem (testes e exportação). */
    fun awaitIdle() {
        io.submit {}.get()
    }

    // ------------------------------------------------------------------ internos

    private fun bump() {
        android.os.Handler(android.os.Looper.getMainLooper()).post { version.intValue++ }
    }

    private fun writeSessions() {
        val root = JSONObject()
            .put("formato", 1)
            .put("app", BuildConfig.VERSION_NAME)
            .put("aparelho", "${Build.MANUFACTURER} ${Build.MODEL} (Android ${Build.VERSION.RELEASE})")
            .put("atualizado_em", stamp("yyyy-MM-dd'T'HH:mm:ssXXX"))
            .put("sessoes", JSONArray(sessions))
        val tmp = File(dir, "sessoes.json.tmp")
        tmp.writeText(root.toString(1))
        tmp.renameTo(File(dir, "sessoes.json"))
    }

    private fun writeReadme() {
        File(dir, "LEIA-ME.txt").writeText(
            """
            TREINO REAL — Detector de tom (versão DEV)
            ==========================================
            Esta pasta guarda os trechos captados no culto para treinar o detector.
            Para enviar: no app, Ferramentas → Treino real → "Compartilhar .zip".

            sessoes.json          1 entrada por hino: o que o app detectou em cada
                                  rodada (5, 10, 15 s…), o resultado final e o seu
                                  rótulo (tom certo, certeza, estrelas, etiquetas,
                                  nº da Harpa, comentário). Atualiza a cada hino.
            log.jsonl             diário de tudo o que aconteceu (1 linha por evento).
            audio/<sessão>.wav    o áudio EXATO que o detector ouviu (44,1 kHz mono).
            features/<sessão>.json entradas do modelo a cada 2,5 s (treinador do app).
            modelo_ajustado.json  ajuste feito no aparelho (se você usou o treinador).
            treino_relatorios.json resultados do treinador (validação cruzada).

            Tons: C, C#, D … B (maior) e Cm, C#m … Bm (menor).
            """.trimIndent() + "\n",
        )
    }

    private fun writeWav(file: File, pcm: ShortArray, rate: Int) {
        val dataLen = pcm.size * 2
        RandomAccessFile(file, "rw").use { f ->
            f.setLength(0)
            val h = java.nio.ByteBuffer.allocate(44).order(java.nio.ByteOrder.LITTLE_ENDIAN)
            h.put("RIFF".toByteArray()).putInt(36 + dataLen).put("WAVE".toByteArray())
            h.put("fmt ".toByteArray()).putInt(16).putShort(1).putShort(1).putInt(rate).putInt(rate * 2)
                .putShort(2).putShort(16)
            h.put("data".toByteArray()).putInt(dataLen)
            f.write(h.array())
            val body = java.nio.ByteBuffer.allocate(dataLen).order(java.nio.ByteOrder.LITTLE_ENDIAN)
            for (v in pcm) body.putShort(v)
            f.write(body.array())
        }
    }

    private fun stamp(pattern: String) = SimpleDateFormat(pattern, Locale.US).format(Date())

    private fun JSONObject.topKey(): String? =
        optJSONArray("top")?.optJSONObject(0)?.optString("tom")?.ifEmpty { null }

    /** "G" → 7, "Em" → 4 + 12. */
    fun keyIndex(cipher: String): Int? {
        val minor = cipher.endsWith("m")
        val root = pitchClassCiphers.indexOf(cipher.removeSuffix("m"))
        return if (root < 0) null else root + if (minor) 12 else 0
    }

    /** Compara o que o app detectou com o rótulo do músico. */
    private fun evaluate(s: JSONObject, label: JSONObject): JSONObject {
        val out = JSONObject()
        val truth = label.optString("tom").ifEmpty { return out.put("observacao", "sem tom rotulado") }
        val rounds = s.optJSONArray("rodadas") ?: JSONArray()
        val perRound = JSONArray()
        for (i in 0 until rounds.length()) perRound.put(rounds.getJSONObject(i).topKey() == truth)
        out.put("acertou_rodada", perRound)
        val fin = s.optJSONObject("resultado_final")
        val top = fin?.topKey()
        out.put("acertou_final", top == truth)
        val top3 = fin?.optJSONArray("top")
        out.put("certo_entre_3", (0 until (top3?.length() ?: 0)).any { top3!!.getJSONObject(it).optString("tom") == truth })
        DevEval.errorKind(top, truth)?.let { out.put("tipo_erro", it) }
        return out
    }

    private fun round1(x: Double) = Math.round(x * 10) / 10.0
    private fun round3(x: Double) = Math.round(x * 1000) / 1000.0

    /** Nome em português de uma cifra (para o JSON ficar legível). */
    fun ptName(cipher: String): String = HarmonicDatabase.ptNameByCipher[cipher] ?: cipher
}
