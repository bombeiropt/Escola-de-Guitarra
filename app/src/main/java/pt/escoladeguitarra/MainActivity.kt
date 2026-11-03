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
        "Técnica de guitarra" to Lesson("Precisão antes da velocidade", "Movimentos pequenos e relaxados melhoram a consistência. Trabalha cada técnica isoladamente antes de a juntar a uma música.", listOf("Palhetada alternada: mantém o movimento contínuo para baixo e para cima, mesmo quando mudas de corda.", "Slides: toca uma nota e desliza o mesmo dedo até ao traste seguinte sem levantar a pressão.", "Bends: empurra a corda lateralmente para atingir a altura da nota pretendida; compara com a nota de referência.", "Vibrato: oscila ligeiramente a afinação com movimentos regulares e controlados do dedo.", "Mantém o pulso relaxado e interrompe o exercício se surgir dor."), "A 60 BPM, toca quatro notas por corda com palhetada alternada. Depois faz um slide de dois trastes e sustém uma nota com vibrato lento. Repete sem tensão e aumenta o andamento apenas quando estiver limpo."),
        "Solos e improvisação" to Lesson("Cria frases musicais", "A pentatónica menor é uma excelente base para construir solos de rock e blues. Aprende primeiro o desenho e depois transforma as notas em frases.", listOf("Em Lá menor, começa pela posição aberta/caixa 1: corda Mi grave, trastes 5 e 8.", "Segue o padrão: Mi grave 5–8; Lá 5–7; Ré 5–7; Sol 5–7; Si 5–8; Mi aguda 5–8.", "Desce o mesmo desenho até à corda grave, mantendo a alternância da palheta.", "Cria frases curtas de três a cinco notas e deixa pequenos silêncios.", "Experimenta terminar uma frase na tónica Lá e ouve a sensação de repouso."), "Com metrónomo a 60 BPM, toca a pentatónica de Lá menor a subir e a descer. Depois improvisa durante dois minutos usando apenas as notas dos trastes 5 e 8 da primeira e da sexta corda, variando o ritmo e incluindo pausas."),
        "Som e efeitos" to Lesson("Constrói o teu som", "Ajusta primeiro o volume e o ganho; adiciona efeitos gradualmente.", listOf("Começa com som limpo e equalização neutra.", "Aumenta o ganho aos poucos para controlar o ruído.", "Experimenta delay ou reverb com moderação."), "Toca a mesma frase com som limpo e depois com overdrive. Compara a clareza das notas."),
        "Treino e evolução" to Lesson("Treino com objetivo", "Uma sessão equilibrada alterna técnica, repertório e criatividade. A regularidade vale mais do que a velocidade.", listOf("Aquece durante cinco minutos com movimentos leves e palhetada lenta.", "Dedica dez minutos à técnica que estás a desenvolver.", "Estuda uma passagem de uma música em pequenos segmentos.", "Reserva cinco minutos para improvisar ou criar uma frase.", "Regista o andamento, as dificuldades e uma pequena vitória da sessão."), "Faz uma sessão de 20 minutos: 5 de aquecimento, 5 de técnica, 5 de repertório e 5 de improvisação. Regista o que correu melhor e escolhe um objetivo para a próxima sessão."),
        "Afinador e metrónomo" to Lesson("Afinação e pulso", "Afinar e praticar com um pulso estável são hábitos essenciais.", listOf("Afinação standard: E–A–D–G–B–E, da corda mais grave à mais aguda.", "Toca uma corda de cada vez e deixa a nota estabilizar.", "No metrónomo, começa devagar e mantém o tempo constante."), "Afina cada corda com o afinador por microfone. Depois, regula o metrónomo para 60 BPM e toca uma nota em cada clique durante um minuto."),
        "Teoria musical" to Lesson("Compreende o que estás a tocar", "A teoria musical ajuda-te a encontrar notas, construir acordes e criar progressões com intenção.", listOf("Na guitarra, cada traste corresponde a um semitom; dois semitons formam um tom.", "A escala maior segue a sequência: tom – tom – semitom – tom – tom – tom – semitom.", "Uma tríade maior é formada por tónica, terça maior e quinta justa; uma menor usa terça menor.", "Os intervalos dão identidade às melodias e aos acordes. Aprende a ouvi-los e a encontrá-los no braço.", "Relaciona a teoria com músicas e progressões que já conheces."), "Na corda Mi grave, encontra Sol no traste 3, Lá no 5 e Si no 7. Toca as notas e identifica os intervalos. Depois constrói Dó maior com Dó–Mi–Sol e Lá menor com Lá–Dó–Mi.")
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
        if (section == "Solos e improvisação") {
            content.addView(label("MAPA VISUAL · PENTATÓNICA DE LÁ MENOR", bright, 12f, true), margins(top = 16, bottom = 8))
            content.addView(pentatonicDiagram(), margins(bottom = 8))
            content.addView(label("Os círculos azuis assinalam as notas da escala. Os círculos dourados representam a nota Lá, ponto de repouso para as tuas frases.", muted, 12f), margins(bottom = 8))
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
        if (section == "Teoria musical") addMusicTheory(content)
        if (section == "Acordes & dedilhados") addChordExamples(content)
        if (section == "Músicas para aprender") addSongPath(content)
        content.addView(label("O progresso fica guardado neste dispositivo.", muted, 12f), margins(top = 16))
        attach(content)
    }

    private fun addMusicTheory(content: LinearLayout) {
        val theory = card(blue)
        theory.addView(label("FUNDAMENTOS PARA O BRAÇO DA GUITARRA", bright, 13f, true))
        theory.addView(label("1 · Tons e semitons", white, 17f, true), margins(top = 12, bottom = 4))
        theory.addView(label("Um traste para o seguinte representa um semitom. Dois trastes representam um tom. Entre Mi–Fá e Si–Dó existe naturalmente apenas um semitom; entre as restantes notas naturais existe um tom.", muted, 13f))
        theory.addView(label("2 · Fórmula da escala maior", white, 17f, true), margins(top = 12, bottom = 4))
        theory.addView(label("Tom · Tom · Semitom · Tom · Tom · Tom · Semitom", bright, 15f, true))
        theory.addView(label("Exemplo: Dó maior = Dó – Ré – Mi – Fá – Sol – Lá – Si – Dó. Não contém sustenidos nem bemóis.", muted, 13f))
        theory.addView(label("3 · Como se constrói uma tríade", white, 17f, true), margins(top = 12, bottom = 4))
        theory.addView(label("Maior: 1 – 3 – 5 (terça maior + terça menor).\nMenor: 1 – b3 – 5 (terça menor + terça maior).\nExemplos: Dó maior = Dó–Mi–Sol; Lá menor = Lá–Dó–Mi.", muted, 13f))
        theory.addView(label("4 · Intervalos essenciais", white, 17f, true), margins(top = 12, bottom = 4))
        theory.addView(label("Uníssono: 0 semitons · 2.ª menor: 1 · 2.ª maior: 2 · 3.ª menor: 3 · 3.ª maior: 4 · 4.ª justa: 5 · trítono: 6 · 5.ª justa: 7 · oitava: 12.", muted, 13f))
        theory.addView(label("5 · Aplicação prática", white, 17f, true), margins(top = 12, bottom = 4))
        theory.addView(label("Na corda Mi grave: traste 3 = Sol, traste 5 = Lá, traste 7 = Si. Usa estas referências para localizar notas e transportar riffs para outras tonalidades.", muted, 13f))
        theory.addView(label("Desafio: toca Dó–Mi–Sol e depois Lá–Dó–Mi. Ouve a diferença entre o carácter maior e menor. Canta cada nota antes de a tocar.", bright, 13f, true), margins(top = 12))
        content.addView(theory, margins(top = 18))
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

    private fun addSongPath(content: LinearLayout) {
        val panelView = card(blue)
        panelView.addView(label("REPERTÓRIO SUGERIDO", bright, 13f, true))
        panelView.addView(label("Uma seleção para desenvolver ritmo, riffs, mudanças de acordes e fraseado. A dificuldade é indicativa; adapta o andamento ao teu nível.", muted, 13f), margins(top = 6, bottom = 10))
        val songs = listOf(
            Triple("Seven Nation Army", "Inicial", "Riff principal, pulsação e precisão."),
            Triple("Smoke on the Water", "Inicial", "Power chords e controlo da palheta."),
            Triple("Knockin’ on Heaven’s Door", "Inicial", "Acompanhamento e mudanças de acordes."),
            Triple("Sweet Home Alabama", "Intermédio", "Ritmo, pequenos preenchimentos e articulação."),
            Triple("Another Brick in the Wall", "Intermédio", "Riff, dinâmica e precisão rítmica."),
            Triple("Californication", "Intermédio", "Melodia, limpeza das notas e controlo do tempo."),
            Triple("Hotel California", "Intermédio/Avançado", "Arpejos, construção de solo e fraseado."),
            Triple("November Rain", "Avançado", "Bends, vibrato e expressão."),
            Triple("Sweet Child O’ Mine", "Avançado", "Riff, resistência e palhetada consistente.")
        )
        songs.forEachIndexed { index, song ->
            val row = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(dp(11), dp(9), dp(11), dp(9))
                background = roundDrawable(Color.rgb(7, 35, 62), 10)
            }
            row.addView(label(song.first, white, 15f, true))
            row.addView(label(song.second.uppercase() + "  ·  " + song.third, muted, 12f))
            panelView.addView(row, margins(bottom = 7))
        }
        panelView.addView(label("Nota: a aplicação não reproduz nem distribui gravações ou tablaturas protegidas. Usa edições, aulas e fontes autorizadas para estudar cada música.", muted, 11f), margins(top = 5))
        content.addView(panelView, margins(top = 18))
    }

    private fun addChordExamples(content: LinearLayout) {
        val panelView = card(blue)
        panelView.addView(label("ACORDES EM DIAGRAMA", bright, 13f, true))
        panelView.addView(label("Cada diagrama mostra as seis cordas. Os círculos indicam onde colocar os dedos; 0 significa corda solta.", muted, 13f), margins(top = 6, bottom = 10))
        panelView.addView(label("E  ·  MI MAIOR", white, 15f, true), margins(top = 8))
        panelView.addView(chordDiagram("E", intArrayOf(0, 2, 2, 1, 0, 0), intArrayOf(0, 2, 3, 1, 0, 0)), margins(bottom = 12))
        panelView.addView(label("Em  ·  MI MENOR", white, 15f, true), margins(top = 4))
        panelView.addView(chordDiagram("Em", intArrayOf(0, 2, 2, 0, 0, 0), intArrayOf(0, 2, 3, 0, 0, 0)), margins(bottom = 12))
        panelView.addView(label("A  ·  LÁ MAIOR", white, 15f, true), margins(top = 4))
        panelView.addView(chordDiagram("A", intArrayOf(0, 0, 2, 2, 2, 0), intArrayOf(0, 0, 1, 2, 3, 0)), margins(bottom = 12))
        panelView.addView(label("Am  ·  LÁ MENOR", white, 15f, true), margins(top = 4))
        panelView.addView(chordDiagram("Am", intArrayOf(0, 0, 2, 2, 1, 0), intArrayOf(0, 0, 2, 3, 1, 0)), margins(bottom = 12))
        panelView.addView(label("D  ·  RÉ MAIOR", white, 15f, true), margins(top = 4))
        panelView.addView(chordDiagram("D", intArrayOf(-1, -1, 0, 2, 3, 2), intArrayOf(0, 0, 0, 1, 3, 2)), margins(bottom = 12))
        panelView.addView(label("Dm  ·  RÉ MENOR", white, 15f, true), margins(top = 4))
        panelView.addView(chordDiagram("Dm", intArrayOf(-1, -1, 0, 2, 3, 1), intArrayOf(0, 0, 0, 2, 3, 1)), margins(bottom = 12))
        panelView.addView(label("G  ·  SOL MAIOR", white, 15f, true), margins(top = 4))
        panelView.addView(chordDiagram("G", intArrayOf(3, 2, 0, 0, 0, 3), intArrayOf(2, 1, 0, 0, 0, 3)), margins(bottom = 12))
        panelView.addView(label("C  ·  DÓ MAIOR", white, 15f, true), margins(top = 4))
        panelView.addView(chordDiagram("C", intArrayOf(-1, 3, 2, 0, 1, 0), intArrayOf(0, 3, 2, 0, 1, 0)), margins(bottom = 12))
        panelView.addView(label("E7  ·  MI DE SÉTIMA", white, 15f, true), margins(top = 4))
        panelView.addView(chordDiagram("E7", intArrayOf(0, 2, 0, 1, 0, 0), intArrayOf(0, 2, 0, 1, 0, 0)), margins(bottom = 12))
        panelView.addView(label("A7  ·  LÁ DE SÉTIMA", white, 15f, true), margins(top = 4))
        panelView.addView(chordDiagram("A7", intArrayOf(-1, 0, 2, 0, 2, 0), intArrayOf(0, 0, 2, 0, 3, 0)), margins(bottom = 12))
        panelView.addView(label("D7  ·  RÉ DE SÉTIMA", white, 15f, true), margins(top = 4))
        panelView.addView(chordDiagram("D7", intArrayOf(-1, -1, 0, 2, 1, 2), intArrayOf(0, 0, 0, 2, 1, 3)), margins(bottom = 12))
        panelView.addView(label("G7  ·  SOL DE SÉTIMA", white, 15f, true), margins(top = 4))
        panelView.addView(chordDiagram("G7", intArrayOf(3, 2, 0, 0, 0, 1), intArrayOf(3, 2, 0, 0, 0, 1)), margins(bottom = 12))
        panelView.addView(label("E5  ·  POWER CHORD", white, 15f, true), margins(top = 4))
        panelView.addView(chordDiagram("E5", intArrayOf(0, 2, 2, -1, -1, -1), intArrayOf(0, 1, 2, 0, 0, 0)), margins(bottom = 8))
        panelView.addView(label("X significa corda abafada; 0 significa corda solta. Coloca os dedos perto do traste, sem tocar nas cordas vizinhas. Toca cada corda separadamente para confirmar que o acorde soa limpo.", muted, 12f), margins(top = 4))
        panelView.addView(label("PROGRESSÕES PARA PRATICAR", bright, 13f, true), margins(top = 18, bottom = 8))
        val progressions = listOf(
            Triple("Rock · nível inicial", "Em  –  C  –  G  –  D", "Quatro batidas por acorde. Mantém o movimento da mão direita regular."),
            Triple("Pop · nível intermédio", "G  –  D  –  Em  –  C", "Experimenta duas batidas para baixo e duas para cima em cada compasso."),
            Triple("Blues · 12 compassos", "A7 × 4  ·  D7 × 2  ·  A7 × 2  ·  E7  ·  D7  ·  A7  ·  E7", "Conta quatro tempos por compasso: 4 em A7, 2 em D7, 2 em A7 e termina com um compasso em cada acorde E7, D7, A7 e E7. Experimenta um ritmo shuffle lento.")
        )
        progressions.forEach { item ->
            val block = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(dp(11), dp(10), dp(11), dp(10))
                background = roundDrawable(Color.rgb(7, 35, 62), 10)
            }
            block.addView(label(item.first.uppercase(), bright, 11f, true))
            block.addView(label(item.second, white, 18f, true), margins(top = 3, bottom = 3))
            block.addView(label(item.third, muted, 12f))
            panelView.addView(block, margins(bottom = 8))
        }
        panelView.addView(label("Desafio: começa a 60 BPM. Quando conseguires mudar de acorde sem parar e sem cordas abafadas, aumenta 5 BPM.", muted, 12f), margins(top = 3))
        content.addView(panelView, margins(top = 18))
    }

    private fun pentatonicDiagram(): View {
        return object : View(this) {
            private val gridPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = bright; strokeWidth = dp(2).toFloat() }
            private val rootPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.rgb(255, 190, 55); style = Paint.Style.FILL }
            private val notePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = bright; style = Paint.Style.FILL }
            private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = white; textSize = dp(11).toFloat(); typeface = Typeface.DEFAULT_BOLD }
            override fun onDraw(canvas: Canvas) {
                super.onDraw(canvas)
                val left = dp(38).toFloat()
                val right = width - dp(16).toFloat()
                val top = dp(42).toFloat()
                val bottom = height - dp(22).toFloat()
                val sx = (right - left) / 4f
                val sy = (bottom - top) / 5f
                val strings = arrayOf("E", "A", "D", "G", "B", "e")
                for (i in 0..5) {
                    val y = top + i * sy
                    canvas.drawLine(left, y, right, y, gridPaint)
                    canvas.drawText(strings[i], dp(12).toFloat(), y + dp(4).toFloat(), textPaint)
                }
                for (f in 0..4) {
                    val x = left + f * sx
                    canvas.drawLine(x, top, x, bottom, gridPaint)
                    canvas.drawText((f + 5).toString(), x + sx / 2f - dp(4), top - dp(12).toFloat(), textPaint)
                }
                val positions = arrayOf(
                    intArrayOf(0, 5), intArrayOf(0, 8),
                    intArrayOf(1, 5), intArrayOf(1, 7),
                    intArrayOf(2, 5), intArrayOf(2, 7),
                    intArrayOf(3, 5), intArrayOf(3, 7),
                    intArrayOf(4, 5), intArrayOf(4, 8),
                    intArrayOf(5, 5), intArrayOf(5, 8)
                )
                for (p in positions) {
                    val x = left + (p[1] - 5 + 0.5f) * sx
                    val y = top + p[0] * sy
                    val isRoot = (p[0] == 0 && p[1] == 5) || (p[0] == 2 && p[1] == 7) || (p[0] == 5 && p[1] == 5)
                    canvas.drawCircle(x, y, dp(8).toFloat(), if (isRoot) rootPaint else notePaint)
                }
                canvas.drawText("Trastes 5–8 · corda grave em cima", left, height - dp(5).toFloat(), textPaint)
            }
        }.apply {
            layoutParams = LinearLayout.LayoutParams(-1, dp(190))
            background = roundDrawable(Color.rgb(7, 35, 62), 12)
            contentDescription = "Mapa da pentatónica menor de Lá entre os trastes cinco e oito"
        }
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
                    } else if (frets[i] < 0) {
                        val crossY = top - dp(25).toFloat()
                        canvas.drawLine(x - dp(5), crossY - dp(5), x + dp(5), crossY + dp(5), textPaint)
                        canvas.drawLine(x - dp(5), crossY + dp(5), x + dp(5), crossY - dp(5), textPaint)
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
