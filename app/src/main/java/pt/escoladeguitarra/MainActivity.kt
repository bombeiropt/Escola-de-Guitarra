package pt.escoladeguitarra

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.Canvas
import android.graphics.Paint
import android.view.View
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.media.AudioManager
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.media.ToneGenerator
import android.os.Handler
import android.os.Looper
import android.widget.SeekBar
import android.os.Bundle
import android.view.Gravity
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import kotlin.math.roundToInt

class MainActivity : Activity() {
    private val bg = Color.rgb(4, 6, 12)
    private val panel = Color.rgb(12, 17, 29)
    private val blue = Color.rgb(0, 153, 255)
    private val bright = Color.rgb(52, 190, 255)
    private val white = Color.WHITE
    private val muted = Color.rgb(190, 204, 222)
    private lateinit var root: LinearLayout
    private val metronomeHandler = Handler(Looper.getMainLooper())
    private var toneGenerator: ToneGenerator? = null
    private var metronomeRunning = false
    private var metronomeBpm = 80
    private var metronomeBeat = 0
    @Volatile private var tunerRunning = false
    private var tunerRecord: AudioRecord? = null
    private var tunerStatus: TextView? = null
    private val metronomeTick = object : Runnable {
        override fun run() {
            if (!metronomeRunning) return
            try {
                toneGenerator?.startTone(if (metronomeBeat == 0) ToneGenerator.TONE_PROP_ACK else ToneGenerator.TONE_PROP_BEEP, 70)
            } catch (_: Exception) { }
            metronomeBeat = (metronomeBeat + 1) % 4
            metronomeHandler.postDelayed(this, (60000L / metronomeBpm))
        }
    }

    private data class Lesson(val title: String, val intro: String, val steps: List<String>, val exercise: String)
    private val lessons = linkedMapOf(
        "Aulas e aprendizagem" to Lesson("Constrói bases sólidas", "Uma rotina curta e regular ajuda mais do que uma sessão longa de vez em quando.", listOf("Afina a guitarra antes de começar.", "Mantém os ombros relaxados e o pulso sem tensão.", "Estuda devagar e aumenta a velocidade quando as notas soarem limpas."), "Toca quatro notas seguidas em cada corda, usando um dedo diferente por traste. Repete lentamente e sem ruído."),
        "Músicas para aprender" to Lesson("Aprende por partes", "Divide cada música em pequenos segmentos e junta-os quando estiverem seguros.", listOf("Escolhe um riff adequado ao teu nível.", "Ouve a gravação para reconhecer o ritmo.", "Pratica dois compassos de cada vez, primeiro devagar."), "Escolhe um riff simples que conheças. Repete-o cinco vezes e regista onde sentiste dificuldade. Usa fontes autorizadas para tablaturas e gravações."),
        "Acordes & dedilhados" to Lesson("Acordes limpos e mudanças suaves", "Pressiona perto do traste, com apenas a força necessária.", listOf("Coloca um dedo de cada vez e verifica cada corda.", "Dedilha as cordas individualmente para encontrar notas abafadas.", "Muda entre dois acordes sem acelerar."), "Alterna entre Em e G: quatro batidas em cada acorde. Repete durante dois minutos, mantendo um pulso constante."),
        "Técnica de guitarra" to Lesson("Precisão antes da velocidade", "Movimentos pequenos e relaxados melhoram a consistência.", listOf("Na palhetada alternada, alterna para baixo e para cima.", "Mantém a palheta perto das cordas.", "Para e descansa se sentires dor ou tensão."), "Toca uma nota por batida com palhetada alternada durante um minuto. Procura volume e duração semelhantes."),
        "Solos e improvisação" to Lesson("Cria frases musicais", "Uma boa improvisação combina ritmo, silêncio e intenção.", listOf("Começa com poucas notas da pentatónica menor.", "Repete uma frase curta e altera o final.", "Usa pausas para dar espaço à música."), "Improvisa usando apenas três notas durante dois minutos. Varia o ritmo e termina cada frase com uma pausa."),
        "Som e efeitos" to Lesson("Constrói o teu som", "Ajusta primeiro o volume e o ganho; adiciona efeitos gradualmente.", listOf("Começa com som limpo e equalização neutra.", "Aumenta o ganho aos poucos para controlar o ruído.", "Experimenta delay ou reverb com moderação."), "Toca a mesma frase com som limpo e depois com overdrive. Compara a clareza das notas."),
        "Treino e evolução" to Lesson("Treino com objetivo", "Define uma meta pequena para cada sessão e regista o que melhorou.", listOf("Escolhe um único foco para a sessão.", "Usa um andamento em que consigas tocar sem erros.", "Termina repetindo o exercício com atenção ao som."), "Pratica cinco minutos e avalia a dificuldade de 1 a 5. Na próxima sessão, tenta melhorar um ponto."),
        "Afinador e metrónomo" to Lesson("Afinação e pulso", "Afinar e praticar com um pulso estável são hábitos essenciais.", listOf("Afinação standard: E–A–D–G–B–E, da corda mais grave à mais aguda.", "Começa num andamento confortável.", "Toca uma nota por clique antes de experimentar duas."), "Usa um metrónomo a 60 BPM e toca uma nota em cada clique durante um minuto. O afinador por microfone será acrescentado numa fase seguinte.")
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = bg
        window.navigationBarColor = bg
        root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setBackgroundColor(bg) }
        setContentView(root)
        showHome()
    }

    private fun showHome() {
        root.removeAllViews()
        val content = page()
        content.addView(label("ESCOLA DE GUITARRA", bright, 12f, true))
        content.addView(label("A tua música\ncomeça aqui.", white, 34f, true))
        content.addView(label("Aprende, pratica e evolui ao teu ritmo.", muted, 16f), margins(bottom = 20))
        val done = getSharedPreferences("guitar_progress", MODE_PRIVATE).getInt("completed", 0)
        val feature = card(blue)
        feature.addView(label("PERCURSO DE APRENDIZAGEM", bright, 11f, true))
        feature.addView(label("Do primeiro acorde ao teu primeiro solo.", white, 19f, true))
        feature.addView(label("Aulas práticas, exercícios e progresso guardado neste dispositivo.", muted, 14f))
        feature.addView(label("EXERCÍCIOS CONCLUÍDOS: " + done, bright, 12f, true))
        feature.addView(button("Começar a aprender") { showLesson("Aulas e aprendizagem") }, matchHeight(50).apply { topMargin = dp(12) })
        content.addView(feature, margins(bottom = 24))
        content.addView(label("EXPLORAR A ESCOLA", bright, 12f, true), margins(bottom = 10))
        lessons.keys.forEachIndexed { i, name ->
            val row = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL
                setPadding(dp(14), dp(12), dp(12), dp(12)); background = boxDrawable(blue, 14)
                isClickable = true; isFocusable = true; setOnClickListener { showLesson(name) }
            }
            row.addView(TextView(this).apply {
                text = String.format("%02d", i + 1); textSize = 13f; typeface = Typeface.DEFAULT_BOLD
                setTextColor(bright); gravity = Gravity.CENTER; background = roundDrawable(Color.rgb(7, 35, 62), 10)
                layoutParams = LinearLayout.LayoutParams(dp(42), dp(42))
            })
            val labels = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(12), 0, dp(4), 0) }
            labels.addView(label(name, white, 15f, true))
            labels.addView(label(lessons.getValue(name).intro, muted, 12f))
            row.addView(labels, LinearLayout.LayoutParams(0, -2, 1f))
            row.addView(label("›", bright, 26f))
            content.addView(row, margins(bottom = 9))
        }
        content.addView(label("PRATICA COM REGULARIDADE · EVOLUI COM CONFIANÇA", muted, 10f), margins(top = 20))
        attach(content)
    }

    private fun showLesson(section: String) {
        val lesson = lessons[section] ?: return
        root.removeAllViews()
        val content = page()
        content.addView(button("‹  Voltar à escola") { showHome() }, matchHeight(46))
        content.addView(label("ESCOLA DE GUITARRA  /  APRENDIZAGEM", bright, 11f, true), margins(top = 22, bottom = 8))
        content.addView(label(section.uppercase(), bright, 12f, true), margins(bottom = 8))
        content.addView(label(lesson.title, white, 28f, true), margins(bottom = 8))
        content.addView(label(lesson.intro, muted, 16f), margins(bottom = 18))
        content.addView(label("PASSOS A SEGUIR", bright, 12f, true), margins(bottom = 8))
        lesson.steps.forEachIndexed { i, step ->
            val row = card(blue).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL }
            row.addView(TextView(this).apply {
                text = (i + 1).toString(); textSize = 16f; typeface = Typeface.DEFAULT_BOLD
                setTextColor(bright); gravity = Gravity.CENTER; background = roundDrawable(Color.rgb(7, 35, 62), 10)
                layoutParams = LinearLayout.LayoutParams(dp(38), dp(38))
            })
            row.addView(label(step, white, 15f), LinearLayout.LayoutParams(0, -2, 1f).apply { leftMargin = dp(12) })
            content.addView(row, margins(bottom = 9))
        }
        val exercise = card(blue)
        exercise.addView(label("EXERCÍCIO PRÁTICO", bright, 12f, true))
        exercise.addView(label(lesson.exercise, white, 15f), margins(top = 8, bottom = 12))
        val prefs = getSharedPreferences("guitar_progress", MODE_PRIVATE)
        val key = "done_" + section
        exercise.addView(button(if (prefs.getBoolean(key, false)) "✓  Exercício concluído" else "Marcar exercício concluído") {
            if (!prefs.getBoolean(key, false)) {
                prefs.edit().putBoolean(key, true).putInt("completed", prefs.getInt("completed", 0) + 1).apply()
                Toast.makeText(this, "Progresso guardado neste dispositivo.", Toast.LENGTH_SHORT).show()
            }
            showLesson(section)
        }, matchHeight(50))
        content.addView(exercise, margins(top = 18))
        if (section == "Afinador e metrónomo") { addTuner(content); addMetronome(content) }
        if (section == "Acordes & dedilhados") addChordExamples(content)
        content.addView(label("O progresso fica guardado neste dispositivo.", muted, 12f), margins(top = 16))
        attach(content)
    }

    private fun addTuner(content: LinearLayout) {
        val tuner = card(blue)
        tuner.addView(label("AFINADOR CROMÁTICO", bright, 13f, true))
        tuner.addView(label("Afina uma corda de cada vez, num ambiente silencioso.", muted, 13f), margins(top = 5, bottom = 10))
        tunerStatus = label("Pronto para ouvir a guitarra", white, 21f, true).apply { gravity = Gravity.CENTER }
        tuner.addView(tunerStatus!!, margins(bottom = 8))
        tuner.addView(button("▶  Iniciar / parar afinador") {
            if (tunerRunning) stopTuner() else if (checkSelfPermission(Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) startTuner() else requestPermissions(arrayOf(Manifest.permission.RECORD_AUDIO), 741)
        }, matchHeight(48))
        tuner.addView(label("Afinação standard: E – A – D – G – B – E. O indicador mostra a nota mais próxima e a diferença em cents.", muted, 12f), margins(top = 8))
        content.addView(tuner, margins(top = 18))
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 741) { if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) startTuner() else tunerStatus?.text = "É necessária autorização do microfone" }
    }

    private fun startTuner() {
        if (tunerRunning) return
        try {
            val rate = 44100
            val size = maxOf(AudioRecord.getMinBufferSize(rate, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT), 8192)
            val recorder = AudioRecord(MediaRecorder.AudioSource.MIC, rate, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT, size)
            if (recorder.state != AudioRecord.STATE_INITIALIZED) { recorder.release(); tunerStatus?.text = "Microfone indisponível"; return }
            tunerRecord = recorder; tunerRunning = true; recorder.startRecording(); tunerStatus?.text = "A ouvir… toca uma corda"
            Thread {
                val samples = ShortArray(4096)
                while (tunerRunning) {
                    val count = try { recorder.read(samples, 0, samples.size) } catch (_: Exception) { -1 }
                    if (count > 1024 && tunerRunning) {
                        val hz = detectPitch(samples, count, rate)
                        runOnUiThread {
                            if (tunerRunning && hz > 0.0) {
                                val names = arrayOf("C", "C♯", "D", "D♯", "E", "F", "F♯", "G", "G♯", "A", "A♯", "B")
                                val midi = (69.0 + 12.0 * kotlin.math.ln(hz / 440.0) / kotlin.math.ln(2.0)).roundToInt()
                                val target = 440.0 * Math.pow(2.0, (midi - 69) / 12.0)
                                val cents = (1200.0 * kotlin.math.ln(hz / target) / kotlin.math.ln(2.0)).roundToInt()
                                val dir = when { cents < -3 -> "grave"; cents > 3 -> "aguda"; else -> "afinada" }
                                tunerStatus?.text = "${names[(midi % 12 + 12) % 12]}${midi / 12 - 1}  ${cents} cents · $dir"
                            } else if (tunerRunning) tunerStatus?.text = "Toca uma corda isolada…"
                        }
                    }
                }
            }.apply { priority = Thread.MAX_PRIORITY; start() }
        } catch (_: Exception) { tunerRunning = false; tunerStatus?.text = "Não foi possível iniciar o afinador" }
    }

    private fun detectPitch(data: ShortArray, count: Int, rate: Int): Double {
        var mean = 0.0; for (i in 0 until count) mean += data[i]; mean /= count
        var energy = 0.0; for (i in 0 until count) { val v = data[i] - mean; energy += v * v }
        if (energy / count < 18000.0) return -1.0
        var bestLag = -1; var best = 0.0
        for (lag in rate / 1000..minOf(rate / 65, count / 2)) {
            var corr = 0.0; var a2 = 0.0; var b2 = 0.0; var i = 0
            while (i < count - lag) { val a = data[i].toDouble() - mean; val b = data[i + lag].toDouble() - mean; corr += a * b; a2 += a * a; b2 += b * b; i += 2 }
            val score = if (a2 > 0 && b2 > 0) corr / kotlin.math.sqrt(a2 * b2) else 0.0
            if (score > best) { best = score; bestLag = lag }
        }
        return if (bestLag > 0 && best > 0.72) rate.toDouble() / bestLag else -1.0
    }

    private fun stopTuner() {
        tunerRunning = false
        try { tunerRecord?.stop() } catch (_: Exception) {}
        try { tunerRecord?.release() } catch (_: Exception) {}
        tunerRecord = null; tunerStatus?.text = "Afinador parado"
    }

    private fun addMetronome(content: LinearLayout) {
        val panelView = card(blue)
        panelView.addView(label("METRÓNOMO INTERATIVO", bright, 13f, true))
        val bpmLabel = label("$metronomeBpm BPM", white, 26f, true).apply { gravity = Gravity.CENTER }
        panelView.addView(bpmLabel, margins(top = 8, bottom = 4))
        panelView.addView(label("Ajusta o andamento entre 40 e 200 BPM.", muted, 13f))
        val slider = SeekBar(this).apply {
            max = 160
            progress = metronomeBpm - 40
            setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                    metronomeBpm = progress + 40
                    bpmLabel.text = "$metronomeBpm BPM"
                }
                override fun onStartTrackingTouch(seekBar: SeekBar?) {}
                override fun onStopTrackingTouch(seekBar: SeekBar?) {}
            })
        }
        panelView.addView(slider, margins(top = 6, bottom = 12))
        panelView.addView(button(if (metronomeRunning) "Parar metrónomo" else "▶  Iniciar metrónomo") {
            if (metronomeRunning) {
                stopMetronome()
            } else {
                try {
                    if (toneGenerator == null) toneGenerator = ToneGenerator(AudioManager.STREAM_MUSIC, 85)
                    metronomeRunning = true
                    metronomeBeat = 0
                    metronomeHandler.removeCallbacks(metronomeTick)
                    metronomeTick.run()
                } catch (_: Exception) {
                    Toast.makeText(this, "Não foi possível iniciar o som. Verifica o volume do dispositivo.", Toast.LENGTH_LONG).show()
                    stopMetronome()
                }
            }
            showLesson("Afinador e metrónomo")
        }, matchHeight(50))
        panelView.addView(label("O primeiro tempo de cada compasso tem um clique diferente.", muted, 12f), margins(top = 8))
        content.addView(panelView, margins(top = 18))
    }

    private fun addChordExamples(content: LinearLayout) {
        val panelView = card(blue)
        panelView.addView(label("ACORDES EM DIAGRAMA", bright, 13f, true))
        panelView.addView(label("Cada diagrama mostra as seis cordas. Os círculos indicam onde colocar os dedos; 0 significa corda solta.", muted, 13f), margins(top = 6, bottom = 10))
        panelView.addView(chordDiagram("E  ·  MI MAIOR", intArrayOf(0, 2, 2, 1, 0, 0), intArrayOf(0, 2, 3, 1, 0, 0)), margins(bottom = 12))
        panelView.addView(chordDiagram("Em  ·  MI MENOR", intArrayOf(0, 2, 2, 0, 0, 0), intArrayOf(0, 2, 3, 0, 0, 0)), margins(bottom = 8))
        panelView.addView(label("Dica: coloca os dedos perto do traste, sem tocar nas cordas vizinhas. Toca cada corda separadamente para confirmar que o acorde soa limpo.", muted, 12f), margins(top = 4))
        content.addView(panelView, margins(top = 18))
    }

    private fun chordDiagram(title: String, frets: IntArray, fingers: IntArray): View {
        return object : View(this) {
            private val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = bright; strokeWidth = dp(2).toFloat(); style = Paint.Style.STROKE }
            private val nutPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = white; strokeWidth = dp(4).toFloat() }
            private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = white; textSize = dp(13).toFloat(); typeface = Typeface.DEFAULT_BOLD }
            private val notePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = bright; style = Paint.Style.FILL }
            private val noteText = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.BLACK; textSize = dp(11).toFloat(); textAlign = Paint.Align.CENTER; typeface = Typeface.DEFAULT_BOLD }
            override fun onDraw(canvas: Canvas) {
                super.onDraw(canvas)
                val w = width.toFloat()
                val left = w * 0.19f
                val right = w * 0.91f
                val top = dp(57).toFloat()
                val bottom = dp(205).toFloat()
                val stepX = (right - left) / 5f
                val stepY = (bottom - top) / 4f
                canvas.drawText(title, left, dp(23).toFloat(), textPaint)
                val strings = arrayOf("E", "A", "D", "G", "B", "e")
                for (i in 0..5) {
                    val x = left + i * stepX
                    canvas.drawLine(x, top, x, bottom, linePaint)
                    canvas.drawText(strings[i], x - dp(4), top - dp(12).toFloat(), textPaint)
                }
                for (i in 0..4) {
                    val y = top + i * stepY
                    canvas.drawLine(left, y, right, y, if (i == 0) nutPaint else linePaint)
                }
                for (i in 0..5) {
                    val x = left + i * stepX
                    if (frets[i] == 0) {
                        canvas.drawCircle(x, top - dp(25).toFloat(), dp(6).toFloat(), linePaint)
                        canvas.drawText("0", x, top - dp(36).toFloat(), textPaint)
                    } else {
                        val y = top + (frets[i] - 0.5f) * stepY
                        canvas.drawCircle(x, y, dp(10).toFloat(), notePaint)
                        if (fingers[i] > 0) canvas.drawText(fingers[i].toString(), x, y + dp(4).toFloat(), noteText)
                    }
                }
                canvas.drawText("Trastes", dp(8).toFloat(), bottom + dp(19).toFloat(), textPaint)
            }
        }.apply {
            layoutParams = LinearLayout.LayoutParams(-1, dp(235))
            setBackgroundColor(Color.rgb(7, 35, 62))
            contentDescription = "Diagrama do acorde $title"
        }
    }

    private fun stopMetronome() {
        metronomeRunning = false
        metronomeHandler.removeCallbacks(metronomeTick)
        try { toneGenerator?.stopTone() } catch (_: Exception) {}
    }

    override fun onPause() {
        stopMetronome()
        stopTuner()
        super.onPause()
    }

    override fun onDestroy() {
        stopMetronome()
        stopTuner()
        try { toneGenerator?.release() } catch (_: Exception) {}
        toneGenerator = null
        super.onDestroy()
    }

    private fun page() = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        setPadding(dp(22), dp(24), dp(22), dp(30))
        background = GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM, intArrayOf(Color.rgb(5, 10, 22), bg, Color.BLACK))
    }

    private fun attach(content: LinearLayout) {
        val scroll = ScrollView(this).apply { isFillViewport = true; addView(content) }
        root.addView(scroll, LinearLayout.LayoutParams(-1, 0, 1f))
    }

    private fun label(value: String, color: Int, size: Float, bold: Boolean = false) = TextView(this).apply {
        text = value; textSize = size; setTextColor(color); setPadding(0, dp(4), 0, dp(4))
        if (bold) typeface = Typeface.DEFAULT_BOLD
    }

    private fun card(border: Int) = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL; setPadding(dp(15), dp(15), dp(15), dp(15)); background = boxDrawable(border, 16)
    }

    private fun button(title: String, action: () -> Unit) = Button(this).apply {
        text = title; textSize = 15f; isAllCaps = false; setTextColor(white); typeface = Typeface.DEFAULT_BOLD
        background = GradientDrawable(GradientDrawable.Orientation.LEFT_RIGHT, intArrayOf(Color.rgb(0, 105, 220), bright)).apply { cornerRadius = dp(12).toFloat() }
        setOnClickListener { action() }
    }

    private fun boxDrawable(border: Int, radius: Int) = GradientDrawable().apply {
        setColor(panel); cornerRadius = dp(radius).toFloat(); setStroke(dp(1), border)
    }
    private fun roundDrawable(color: Int, radius: Int) = GradientDrawable().apply {
        setColor(color); cornerRadius = dp(radius).toFloat()
    }
    private fun margins(top: Int = 0, bottom: Int = 0) = LinearLayout.LayoutParams(-1, -2).apply {
        topMargin = dp(top); bottomMargin = dp(bottom)
    }
    private fun matchHeight(height: Int) = LinearLayout.LayoutParams(-1, dp(height))
    private fun dp(value: Int) = (value * resources.displayMetrics.density).toInt()

    @Deprecated("Deprecated in Android API")
    override fun onBackPressed() { showHome() }
}
