package pt.escoladeguitarra

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.view.View
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.media.AudioFormat
import android.media.AudioManager
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
    private var tunerThread: Thread? = null
    private var tunerStatus: AnalogTunerView? = null
    @Volatile private var tunerTargetMidi: Int = 0 // 0 = cromático automático; 40–64 = cordas standard
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
        "Solos e improvisação" to Lesson("Cria frases musicais", "A pentatónica menor é uma excelente base para construir solos de rock e blues. Aprende primeiro o desenho e depois transforma as notas em frases.", listOf("Em Lá menor, começa na posição da pentatónica menor apresentada no diagrama.", "Percorre as posições assinaladas nas seis cordas, começando pela corda Mi grave.", "Sobe e desce o desenho sem acelerar; mantém a alternância da palheta.", "Cria frases curtas de três a cinco notas e deixa pequenos silêncios.", "Experimenta terminar uma frase na nota Lá, destacada a dourado no diagrama."), "Com metrónomo a 60 BPM, toca a pentatónica de Lá menor a subir e a descer. Depois improvisa durante dois minutos usando apenas as notas dos trastes 5 e 8 da primeira e da sexta corda, variando o ritmo e incluindo pausas."),
        "Som e efeitos" to Lesson("Constrói o teu som", "Ajusta primeiro o volume e o ganho; adiciona efeitos gradualmente.", listOf("Começa com som limpo e equalização neutra.", "Aumenta o ganho aos poucos para controlar o ruído.", "Experimenta delay ou reverb com moderação."), "Toca a mesma frase com som limpo e depois com overdrive. Compara a clareza das notas."),
        "Treino e evolução" to Lesson("Treino com objetivo", "Uma sessão equilibrada alterna técnica, repertório e criatividade. A regularidade vale mais do que a velocidade.", listOf("Aquece durante cinco minutos com movimentos leves e palhetada lenta.", "Dedica dez minutos à técnica que estás a desenvolver.", "Estuda uma passagem de uma música em pequenos segmentos.", "Reserva cinco minutos para improvisar ou criar uma frase.", "Regista o andamento, as dificuldades e uma pequena vitória da sessão."), "Faz uma sessão de 20 minutos: 5 de aquecimento, 5 de técnica, 5 de repertório e 5 de improvisação. Regista o que correu melhor e escolhe um objetivo para a próxima sessão."),
        "Afinador e metrónomo" to Lesson("Afinação e pulso", "Afinar e praticar com um pulso estável são hábitos essenciais.", listOf("Afinação standard: E–A–D–G–B–E, da corda mais grave à mais aguda.", "Toca uma corda de cada vez e deixa a nota estabilizar.", "No metrónomo, começa devagar e mantém o tempo constante."), "Afina cada corda com o afinador por microfone. Depois, regula o metrónomo para 60 BPM e toca uma nota em cada clique durante um minuto."),
        "Teoria musical" to Lesson("Compreende o que estás a tocar", "A teoria musical ajuda-te a encontrar notas, construir acordes e criar progressões com intenção.", listOf("Na guitarra, cada traste corresponde a um semitom; dois semitons formam um tom.", "A escala maior segue a sequência: tom – tom – semitom – tom – tom – tom – semitom.", "Uma tríade maior é formada por tónica, terça maior e quinta justa; uma menor usa terça menor.", "Os intervalos dão identidade às melodias e aos acordes. Aprende a ouvi-los e a encontrá-los no braço.", "Relaciona a teoria com músicas e progressões que já conheces."), "Na corda Mi grave, encontra Sol no traste 3, Lá no 5 e Si no 7. Toca as notas e identifica os intervalos. Depois constrói Dó maior com Dó–Mi–Sol e Lá menor com Lá–Dó–Mi."),
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = bg
        window.navigationBarColor = bg
        root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setBackgroundColor(bg) }
        setContentView(root)
        metronomeBpm = getSharedPreferences("guitar_progress", MODE_PRIVATE).getInt("metronome_bpm", 80).coerceIn(40, 200)
        showHome()
    }

    private fun showHome() {
        stopMetronome()
        stopTuner()
        root.removeAllViews()
        val content = page().apply { setPadding(dp(14), dp(16), dp(14), dp(28)) }

        // Cabeçalho com acabamento de madeira escura e moldura dourada.
        val header = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(dp(18), dp(22), dp(18), dp(20))
            background = GradientDrawable(
                GradientDrawable.Orientation.TOP_BOTTOM,
                intArrayOf(Color.rgb(105, 61, 31), Color.rgb(48, 29, 20), Color.rgb(24, 20, 18))
            ).apply { cornerRadius = dp(18).toFloat(); setStroke(dp(2), Color.rgb(205, 157, 76)) }
        }
        header.addView(label("♫  ESCOLA DE GUITARRA  ♫", Color.rgb(255, 220, 150), 23f, true))
        header.addView(label("APRENDER  ·  PRATICAR  ·  EVOLUIR", white, 11f, true))
        header.addView(label("Do primeiro acorde ao teu primeiro solo.", Color.rgb(238, 220, 194), 13f), margins(top = 8))
        content.addView(header, margins(bottom = 16))

        val done = getSharedPreferences("guitar_progress", MODE_PRIVATE).getInt("completed", 0)
        val progress = card(Color.rgb(205, 157, 76)).apply {
            background = GradientDrawable(GradientDrawable.Orientation.LEFT_RIGHT,
                intArrayOf(Color.rgb(18, 42, 62), Color.rgb(8, 18, 31))).apply {
                cornerRadius = dp(14).toFloat(); setStroke(dp(1), blue)
            }
        }
        progress.addView(label("O TEU PERCURSO", Color.rgb(255, 211, 126), 11f, true))
        progress.addView(label("$done exercícios concluídos", white, 17f, true))
        progress.addView(label("O progresso fica guardado neste dispositivo.", muted, 12f))
        content.addView(progress, margins(bottom = 18))

        content.addView(label("EXPLORAR A ESCOLA", Color.rgb(255, 211, 126), 12f, true), margins(bottom = 10))
        val descriptions = mapOf(
            "Aulas e aprendizagem" to Triple("♫", "Aulas passo a passo", "Do nível inicial ao avançado"),
            "Teoria musical" to Triple("𝄞", "Notas, escalas e harmonia", "Aprende a compreender a música"),
            "Afinador e metrónomo" to Triple("♩", "Afinação e ritmo", "Afinador analógico e metrónomo"),
            "Acordes & dedilhados" to Triple("♬", "Acordes e dedilhados", "Diagramas e progressões"),
            "Solos e improvisação" to Triple("🎸", "Solos e improvisação", "Escalas e frases de guitarra"),
            "Músicas para aprender" to Triple("▶", "Repertório", "Aprende músicas por etapas"),
            "Técnica de guitarra" to Triple("✦", "Técnica", "Palhetada, slides, bends e vibrato"),
            "Som e efeitos" to Triple("♫", "Som e efeitos", "Constrói o teu próprio som"),
            "Treino e evolução" to Triple("✓", "Plano de evolução", "Organiza a tua prática semanal")
        )
        val entries = lessons.keys.toList()
        var index = 0
        while (index < entries.size) {
            val row = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
            for (column in 0..1) {
                if (index < entries.size) {
                    val section = entries[index]
                    val info = descriptions[section]!!
                    val tile = LinearLayout(this).apply {
                        orientation = LinearLayout.VERTICAL
                        gravity = Gravity.CENTER_HORIZONTAL
                        setPadding(dp(10), dp(13), dp(10), dp(13))
                        background = GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM,
                            intArrayOf(Color.rgb(24, 37, 53), Color.rgb(8, 14, 24))).apply {
                            cornerRadius = dp(15).toFloat()
                            setStroke(dp(1), Color.rgb(0, 145, 235))
                        }
                        isClickable = true
                        isFocusable = true
                        setOnClickListener { showLesson(section) }
                    }
                    val symbol = TextView(this).apply {
                        text = info.first
                        textSize = 29f
                        gravity = Gravity.CENTER
                        setTextColor(Color.rgb(255, 197, 91))
                        background = GradientDrawable().apply {
                            shape = GradientDrawable.OVAL
                            setColor(Color.rgb(9, 30, 48))
                            setStroke(dp(1), Color.rgb(0, 145, 235))
                        }
                        layoutParams = LinearLayout.LayoutParams(dp(54), dp(54)).apply { bottomMargin = dp(9) }
                    }
                    tile.addView(symbol)
                    tile.addView(label(info.second, white, 14f, true).apply { gravity = Gravity.CENTER })
                    tile.addView(label(info.third, muted, 11f).apply { gravity = Gravity.CENTER; textAlignment = TextView.TEXT_ALIGNMENT_CENTER })
                    tile.addView(label("ABRIR  ›", Color.rgb(255, 197, 91), 10f, true), margins(top = 8))
                    row.addView(tile, LinearLayout.LayoutParams(0, dp(158), 1f).apply {
                        if (column == 0) rightMargin = dp(6) else leftMargin = dp(6)
                        bottomMargin = dp(10)
                    })
                    index++
                } else {
                    row.addView(View(this), LinearLayout.LayoutParams(0, 1, 1f))
                }
            }
            content.addView(row)
        }
        content.addView(label("ESCOLA DE GUITARRA  ·  FEITA PARA APRENDER AO TEU RITMO", muted, 10f).apply {
            gravity = Gravity.CENTER
        }, margins(top = 12))
        attach(content)
    }

    private fun showLesson(section: String) {
        val lesson = lessons[section] ?: return
        if (section != "Afinador e metrónomo") {
            stopMetronome()
            stopTuner()
        }
        root.removeAllViews()
        val content = page()
        content.addView(button("‹  Voltar à escola") { showHome() }, matchHeight(46))
        content.addView(label("ESCOLA DE GUITARRA  /  APRENDIZAGEM", bright, 11f, true), margins(top = 22, bottom = 8))
        content.addView(label(section.uppercase(), bright, 12f, true), margins(bottom = 8))
        content.addView(label(lesson.title, white, 28f, true), margins(bottom = 8))
        content.addView(label(lesson.intro, muted, 16f), margins(bottom = 18))
        if (section == "Solos e improvisação") {
            content.addView(label("MAPA VISUAL · PENTATÓNICA DE LÁ MENOR", bright, 12f, true), margins(top = 12, bottom = 8))
            content.addView(pentatonicDiagram(), margins(bottom = 8))
            content.addView(label("As notas azuis pertencem à escala. As notas douradas são Lá, a tónica. A corda Mi grave está em cima; a corda Mi aguda está em baixo.", muted, 12f), margins(bottom = 12))
        }
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
        if (section == "Teoria musical") addMusicTheory(content)
        if (section == "Treino e evolução") addWeeklyPlan(content)
        if (section == "Acordes & dedilhados") addChordExamples(content)
        if (section == "Músicas para aprender") addSongPath(content)
        content.addView(label("O progresso fica guardado neste dispositivo.", muted, 12f), margins(top = 16))
        attach(content)
    }

    private fun addWeeklyPlan(content: LinearLayout) {
        val plan = card(blue)
        plan.addView(label("PLANO SEMANAL DE PRÁTICA", bright, 13f, true))
        plan.addView(label("Sessões curtas de 20 a 30 minutos. Se um dia não for possível praticar, retoma no seguinte sem tentar compensar tudo de uma vez.", muted, 13f), margins(top = 6, bottom = 10))
        val days = listOf(
            Triple("SEGUNDA · TÉCNICA", "Aquecimento 5 min · palhetada alternada 10 min · mudanças de corda 5 min.", "Objetivo: tocar lentamente, com volume uniforme e sem tensão."),
            Triple("TERÇA · ACORDES", "Rever Em, G, C e D. Fazer mudanças durante 10 min e progressão Em–C–G–D durante 10 min.", "Objetivo: cada corda deve soar limpa antes de aumentar o andamento."),
            Triple("QUARTA · ESCALAS", "Pentatónica menor de Lá: subir e descer o desenho durante 10 min; criar frases curtas durante 10 min.", "Objetivo: memorizar as notas e terminar algumas frases na tónica Lá."),
            Triple("QUINTA · RITMO", "Metrónomo a 60 BPM. Tocar notas em semínimas, colcheias e pausas durante 15 a 20 min.", "Objetivo: manter o pulso estável sem acelerar nas passagens difíceis."),
            Triple("SEXTA · REPERTÓRIO", "Escolher uma música adequada ao nível. Trabalhar uma passagem curta, em andamento lento, durante 20 min.", "Objetivo: tocar a passagem três vezes seguidas sem parar."),
            Triple("SÁBADO · CRIATIVIDADE", "Improvisar sobre uma progressão simples durante 10 min; gravar uma ideia própria, se possível, e ouvi-la.", "Objetivo: criar frases curtas com pausas e variação rítmica."),
            Triple("DOMINGO · REVISÃO", "Rever o que foi estudado, repetir os exercícios mais difíceis e registar o andamento confortável.", "Objetivo: identificar uma melhoria e escolher uma prioridade para a semana seguinte.")
        )
        days.forEach { day ->
            val block = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(dp(11), dp(10), dp(11), dp(10))
                background = roundDrawable(Color.rgb(7, 35, 62), 10)
            }
            block.addView(label(day.first, bright, 12f, true))
            block.addView(label(day.second, white, 13f), margins(top = 4, bottom = 4))
            block.addView(label(day.third, muted, 12f))
            plan.addView(block, margins(bottom = 8))
            if (day.first.startsWith("QUARTA")) {
                plan.addView(label("MAPA DA ESCALA · LÁ MENOR", bright, 11f, true), margins(top = 2, bottom = 5))
                plan.addView(pentatonicDiagram(), margins(bottom = 10))
            }
        }
        plan.addView(label("REGRA DE PROGRESSÃO", bright, 12f, true), margins(top = 6, bottom = 4))
        plan.addView(label("Quando conseguires executar um exercício corretamente em três repetições consecutivas, aumenta apenas 3 a 5 BPM. Se perderes a limpeza ou sentires tensão, reduz o andamento. A qualidade vem antes da velocidade.", white, 13f))
        content.addView(plan, margins(top = 18))
    }

    private fun addMusicTheory(content: LinearLayout) {
        val theory = card(blue)
        theory.addView(label("FUNDAMENTOS PARA O BRAÇO DA GUITARRA", bright, 13f, true))
        theory.addView(label("1 · Tons e semitons", white, 17f, true), margins(top = 12, bottom = 4))
        theory.addView(label("Um traste para o seguinte representa um semitom. Dois trastes representam um tom. Entre Mi–Fá e Si–Dó existe naturalmente apenas um semitom; entre as restantes notas naturais existe um tom.", muted, 13f))
        theory.addView(label("2 · Fórmula da escala maior", white, 17f, true), margins(top = 12, bottom = 4))
        theory.addView(label("Tom · Tom · Semitom · Tom · Tom · Tom · Semitom", bright, 15f, true))
        theory.addView(label("EXEMPLO VISUAL · ESCALA DE DÓ MAIOR", bright, 12f, true), margins(top = 10, bottom = 6))
        theory.addView(majorScaleDiagram(), margins(bottom = 8))
        theory.addView(label("Cada círculo mostra uma nota da escala no braço. Os círculos dourados assinalam a nota Dó. As cordas estão representadas da mais grave (em cima) à mais aguda (em baixo).", muted, 12f))
        theory.addView(label("Exemplo: Dó maior = Dó – Ré – Mi – Fá – Sol – Lá – Si – Dó. Não contém sustenidos nem bemóis.", muted, 13f), margins(top = 6))
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
        tunerStatus = AnalogTunerView()
        tuner.addView(tunerStatus!!, LinearLayout.LayoutParams(-1, dp(230)).apply { bottomMargin = dp(8) })
        tuner.addView(label("ESCOLHER CORDA", bright, 11f, true), margins(top = 4, bottom = 6))
        val stringRow = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        listOf(Triple("E", 40, "Mi grave"), Triple("A", 45, "Lá"), Triple("D", 50, "Ré"),
            Triple("G", 55, "Sol"), Triple("B", 59, "Si"), Triple("e", 64, "Mi agudo")).forEach { item ->
            stringRow.addView(button(item.first) {
                tunerTargetMidi = item.second
                tunerStatus?.setTarget(item.third)
            }, LinearLayout.LayoutParams(0, dp(42), 1f).apply { marginEnd = dp(3) })
        }
        tuner.addView(stringRow, margins(bottom = 6))
        tuner.addView(button("AUTO  ·  cromático") {
            tunerTargetMidi = 0
            tunerStatus?.setTarget("Detecção automática")
        }, matchHeight(42).apply { bottomMargin = dp(8) })
        tuner.addView(button("▶  Iniciar / parar afinador") {
            if (tunerRunning) stopTuner() else if (checkSelfPermission(Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) startTuner() else requestPermissions(arrayOf(Manifest.permission.RECORD_AUDIO), 741)
        }, matchHeight(48))
        tuner.addView(label("Afinação standard: E – A – D – G – B – E. O indicador mostra a nota mais próxima e a diferença em cents.", muted, 12f), margins(top = 8))
        content.addView(tuner, margins(top = 18))
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 741) { if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) startTuner() else tunerStatus?.setStatus("Autoriza o microfone para afinar") }
    }

    private fun startTuner() {
        if (tunerRunning) return
        try {
            val rate = 44100
            val size = maxOf(AudioRecord.getMinBufferSize(rate, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT), 8192)
            val recorder = AudioRecord(MediaRecorder.AudioSource.MIC, rate, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT, size)
            if (recorder.state != AudioRecord.STATE_INITIALIZED) { recorder.release(); tunerStatus?.setStatus("Microfone indisponível"); return }
            tunerRecord = recorder; tunerRunning = true; recorder.startRecording(); tunerStatus?.setStatus("A ouvir… toca uma corda")
            val worker = Thread {
                val samples = ShortArray(4096)
                while (tunerRunning && tunerRecord === recorder) {
                    val count = try { recorder.read(samples, 0, samples.size) } catch (_: Exception) { -1 }
                    if (count > 1024 && tunerRunning) {
                        val hz = detectPitch(samples, count, rate)
                        runOnUiThread {
                            if (tunerRunning && tunerRecord === recorder && hz > 0.0) {
                                val names = arrayOf("C", "C♯", "D", "D♯", "E", "F", "F♯", "G", "G♯", "A", "A♯", "B")
                                val midi = (69.0 + 12.0 * kotlin.math.ln(hz / 440.0) / kotlin.math.ln(2.0)).roundToInt()
                                val selectedMidi = tunerTargetMidi
                                val referenceMidi = if (selectedMidi in 40..64) selectedMidi else midi
                                val referenceHz = 440.0 * Math.pow(2.0, (referenceMidi - 69) / 12.0)
                                val cents = (1200.0 * kotlin.math.ln(hz / referenceHz) / kotlin.math.ln(2.0)).roundToInt()
                                val shownNote = if (selectedMidi in 40..64) {
                                    val stringNames = mapOf(40 to "E2 · Mi grave", 45 to "A2 · Lá", 50 to "D3 · Ré", 55 to "G3 · Sol", 59 to "B3 · Si", 64 to "E4 · Mi agudo")
                                    stringNames[selectedMidi] ?: names[(midi % 12 + 12) % 12] + (midi / 12 - 1)
                                } else names[(midi % 12 + 12) % 12] + (midi / 12 - 1)
                                tunerStatus?.setReading(shownNote, cents)
                            } else if (tunerRunning && tunerRecord === recorder) tunerStatus?.setStatus("Toca uma corda isolada…")
                        }
                    }
                }
            }.apply { priority = Thread.MAX_PRIORITY }
            tunerThread = worker
            worker.start()
        } catch (_: Exception) { tunerRunning = false; tunerStatus?.setStatus("Não foi possível iniciar o afinador") }
    }

    /** Mostrador analógico com escala de cents, zona verde e ponteiro em tempo real. */
    private inner class AnalogTunerView : View(this@MainActivity) {
        private val arcPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.rgb(72, 91, 112); style = Paint.Style.STROKE; strokeWidth = dp(13).toFloat(); strokeCap = Paint.Cap.ROUND }
        private val greenPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.rgb(52, 190, 125); style = Paint.Style.STROKE; strokeWidth = dp(13).toFloat(); strokeCap = Paint.Cap.ROUND }
        private val tickPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = white; strokeWidth = dp(2).toFloat() }
        private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = muted; textSize = dp(11).toFloat(); textAlign = Paint.Align.CENTER }
        private val notePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = white; textSize = dp(31).toFloat(); textAlign = Paint.Align.CENTER; typeface = Typeface.DEFAULT_BOLD }
        private val centsPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = bright; textSize = dp(14).toFloat(); textAlign = Paint.Align.CENTER; typeface = Typeface.DEFAULT_BOLD }
        private val needlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.rgb(255, 190, 55); strokeWidth = dp(4).toFloat(); strokeCap = Paint.Cap.ROUND }
        private val pivotPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = white; style = Paint.Style.FILL }
        private var note = "—"
        private var cents = 0
        private var status = "Pronto para ouvir a guitarra"
        private var targetDescription = "Detecção automática"
        private var hasReading = false

        fun setTarget(description: String) {
            targetDescription = description
            status = "Modo: $description"
            hasReading = false
            invalidate()
        }
        fun setStatus(message: String) { status = message; hasReading = false; invalidate() }
        fun setReading(noteName: String, offset: Int) {
            note = noteName
            val bounded = offset.coerceIn(-50, 50)
            // Suavização exponencial: reduz tremores do ponteiro sem atrasar demasiado a resposta.
            cents = if (hasReading) (cents * 0.68 + bounded * 0.32).roundToInt() else bounded
            status = when { cents < -5 -> "GRAVE · aperta a corda"; cents > 5 -> "AGUDA · alivia a corda"; else -> "AFINADA · no centro" }
            hasReading = true
            invalidate()
        }

        override fun onDraw(canvas: Canvas) {
            super.onDraw(canvas)
            val cx = width / 2f
            val cy = height * 0.78f
            val radius = minOf(width * 0.43f, height * 0.69f)
            val rect = RectF(cx - radius, cy - radius, cx + radius, cy + radius)
            canvas.drawArc(rect, 180f, 180f, false, arcPaint)
            canvas.drawArc(rect, 261f, 18f, false, greenPaint)

            for (value in -50..50 step 10) {
                val angle = Math.toRadians(180.0 + (value + 50) * 1.8)
                val inner = radius - dp(if (value % 20 == 0) 18 else 13)
                val outer = radius - dp(3)
                val x1 = cx + kotlin.math.cos(angle).toFloat() * inner
                val y1 = cy + kotlin.math.sin(angle).toFloat() * inner
                val x2 = cx + kotlin.math.cos(angle).toFloat() * outer
                val y2 = cy + kotlin.math.sin(angle).toFloat() * outer
                canvas.drawLine(x1, y1, x2, y2, tickPaint)
                if (value % 20 == 0) {
                    val labelRadius = radius - dp(30)
                    val lx = cx + kotlin.math.cos(angle).toFloat() * labelRadius
                    val ly = cy + kotlin.math.sin(angle).toFloat() * labelRadius + dp(4)
                    canvas.drawText(if (value > 0) "+$value" else value.toString(), lx, ly, textPaint)
                }
            }

            val needleValue = if (hasReading) cents.coerceIn(-50, 50) else 0
            val needleAngle = Math.toRadians(180.0 + (needleValue + 50) * 1.8)
            val needleLength = radius - dp(39)
            val nx = cx + kotlin.math.cos(needleAngle).toFloat() * needleLength
            val ny = cy + kotlin.math.sin(needleAngle).toFloat() * needleLength
            canvas.drawLine(cx, cy, nx, ny, needlePaint)
            canvas.drawCircle(cx, cy, dp(8).toFloat(), pivotPaint)

            canvas.drawText(if (hasReading) note else "—", cx, cy - radius * 0.24f, notePaint)
            canvas.drawText(if (hasReading) (if (cents > 0) "+$cents" else cents.toString()) + " cents" else "−50       0       +50", cx, cy - radius * 0.02f, centsPaint)
            canvas.drawText(status, cx, height - dp(5).toFloat(), textPaint)
        }
        init { contentDescription = "Afinador analógico com ponteiro e escala de cents" }
    }

    private fun detectPitch(data: ShortArray, count: Int, rate: Int): Double {
        var mean = 0.0; for (i in 0 until count) mean += data[i]; mean /= count
        var energy = 0.0; for (i in 0 until count) { val v = data[i] - mean; energy += v * v }
        if (energy / count < 18000.0) return -1.0
        // Autocorrelação normalizada: compara períodos sem depender apenas do volume.
        // A pesquisa desce até 40 Hz para captar a corda Mi grave e exige confiança mínima.
        var bestLag = -1
        var best = 0.0
        val scores = DoubleArray(minOf(rate / 40, count / 2) + 1)
        val minLag = rate / 1000
        val maxLag = minOf(rate / 40, count / 2)
        for (lag in minLag..maxLag) {
            var corr = 0.0; var a2 = 0.0; var b2 = 0.0; var i = 0
            while (i < count - lag) {
                val a = data[i].toDouble() - mean
                val b = data[i + lag].toDouble() - mean
                corr += a * b; a2 += a * a; b2 += b * b
                i += 2
            }
            val score = if (a2 > 0 && b2 > 0) corr / kotlin.math.sqrt(a2 * b2) else 0.0
            scores[lag] = score
            if (score > best) { best = score; bestLag = lag }
        }
        if (bestLag <= 0 || best < 0.78) return -1.0
        // Interpolação parabólica entre amostras vizinhas para afinar a estimativa.
        val before = if (bestLag > minLag) scores[bestLag - 1] else scores[bestLag]
        val after = if (bestLag < maxLag) scores[bestLag + 1] else scores[bestLag]
        val denominator = before - 2.0 * best + after
        val correction = if (kotlin.math.abs(denominator) > 1e-9) 0.5 * (before - after) / denominator else 0.0
        val refinedLag = bestLag + correction.coerceIn(-0.5, 0.5)
        return rate.toDouble() / refinedLag
    }

    private fun stopTuner() {
        // Primeiro sinaliza a saída e desassocia o gravador; assim o ciclo de leitura
        // deixa de publicar resultados enquanto o recurso está a ser encerrado.
        tunerRunning = false
        val recorder = tunerRecord
        val worker = tunerThread
        tunerRecord = null
        try {
            if (recorder?.recordingState == AudioRecord.RECORDSTATE_RECORDING) recorder.stop()
        } catch (_: Exception) { }
        // AudioRecord.stop() normalmente desbloqueia read(). Aguarda brevemente pela
        // thread antes de libertar o recurso, evitando libertação concorrente.
        if (worker != null && worker !== Thread.currentThread()) {
            try { worker.join(300) } catch (_: InterruptedException) { Thread.currentThread().interrupt() }
        }
        try { recorder?.release() } catch (_: Exception) { }
        if (tunerThread === worker) tunerThread = null
        tunerStatus?.setStatus("Afinador parado")
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
                    if (fromUser) getSharedPreferences("guitar_progress", MODE_PRIVATE).edit().putInt("metronome_bpm", metronomeBpm).apply()
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

    private fun majorScaleDiagram(): View {
        return object : View(this) {
            private val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = bright; strokeWidth = dp(2).toFloat() }
            private val rootPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.rgb(255, 190, 55); style = Paint.Style.FILL }
            private val notePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = bright; style = Paint.Style.FILL }
            private val noteText = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.BLACK; textSize = dp(9).toFloat(); textAlign = Paint.Align.CENTER; typeface = Typeface.DEFAULT_BOLD }
            private val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = white; textSize = dp(10).toFloat(); typeface = Typeface.DEFAULT_BOLD }
            private val notes = arrayOf(
                intArrayOf(0,3), intArrayOf(0,5), intArrayOf(0,7), intArrayOf(0,8),
                intArrayOf(1,3), intArrayOf(1,5), intArrayOf(1,7),
                intArrayOf(2,3), intArrayOf(2,5), intArrayOf(2,7),
                intArrayOf(3,4), intArrayOf(3,5), intArrayOf(3,7),
                intArrayOf(4,3), intArrayOf(4,5), intArrayOf(4,6), intArrayOf(4,8),
                intArrayOf(5,3), intArrayOf(5,5), intArrayOf(5,7), intArrayOf(5,8)
            )
            private val names = mapOf(
                "0:3" to "G", "0:5" to "A", "0:7" to "B", "0:8" to "C",
                "1:3" to "C", "1:5" to "D", "1:7" to "E",
                "2:3" to "F", "2:5" to "G", "2:7" to "A",
                "3:4" to "B", "3:5" to "C", "3:7" to "D",
                "4:3" to "D", "4:5" to "E", "4:6" to "F", "4:8" to "G",
                "5:3" to "G", "5:5" to "A", "5:7" to "B", "5:8" to "C"
            )
            override fun onDraw(canvas: Canvas) {
                super.onDraw(canvas)
                val left = dp(34).toFloat()
                val right = width - dp(14).toFloat()
                val top = dp(34).toFloat()
                val bottom = height - dp(22).toFloat()
                val sx = (right - left) / 5f
                val sy = (bottom - top) / 5f
                val strings = arrayOf("E", "A", "D", "G", "B", "e")
                for (i in 0..5) {
                    val y = top + i * sy
                    canvas.drawLine(left, y, right, y, linePaint)
                    canvas.drawText(strings[i], dp(9).toFloat(), y + dp(4).toFloat(), labelPaint)
                }
                for (fret in 3..8) {
                    val x = left + (fret - 3) * sx
                    canvas.drawLine(x, top, x, bottom, linePaint)
                    canvas.drawText(fret.toString(), x + sx / 2f - dp(3), top - dp(9).toFloat(), labelPaint)
                }
                notes.forEach { p ->
                    val x = left + (p[1] - 3) * sx + sx / 2f
                    val y = top + p[0] * sy
                    val name = names["${p[0]}:${p[1]}"] ?: ""
                    val isRoot = name == "C"
                    canvas.drawCircle(x, y, dp(9).toFloat(), if (isRoot) rootPaint else notePaint)
                    canvas.drawText(name, x, y + dp(3).toFloat(), noteText)
                }
                canvas.drawText("Trastes 3–8", left, height - dp(5).toFloat(), labelPaint)
            }
        }.apply {
            layoutParams = LinearLayout.LayoutParams(-1, dp(190))
            background = roundDrawable(Color.rgb(7, 35, 62), 12)
            contentDescription = "Diagrama da escala de Dó maior no braço da guitarra, trastes 3 a 8"
        }
    }

    private fun pentatonicDiagram(): View {
        return object : View(this) {
            private val gridPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = bright; strokeWidth = dp(2).toFloat() }
            private val rootPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.rgb(255, 190, 55); style = Paint.Style.FILL }
            private val notePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = bright; style = Paint.Style.FILL }
            private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = white; textSize = dp(11).toFloat(); typeface = Typeface.DEFAULT_BOLD }
            private val noteTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.BLACK; textSize = dp(9).toFloat(); textAlign = Paint.Align.CENTER; typeface = Typeface.DEFAULT_BOLD }
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
                val noteNames = mapOf(
                    "0:5" to "A", "0:8" to "C", "1:5" to "D", "1:7" to "E",
                    "2:5" to "G", "2:7" to "A", "3:5" to "C", "3:7" to "D",
                    "4:5" to "E", "4:8" to "G", "5:5" to "A", "5:8" to "C"
                )
                for (p in positions) {
                    val x = left + (p[1] - 5 + 0.5f) * sx
                    val y = top + p[0] * sy
                    val isRoot = (p[0] == 0 && p[1] == 5) || (p[0] == 2 && p[1] == 7) || (p[0] == 5 && p[1] == 5)
                    canvas.drawCircle(x, y, dp(8).toFloat(), if (isRoot) rootPaint else notePaint)
                    canvas.drawText(noteNames["${p[0]}:${p[1]}"] ?: "", x, y + dp(3).toFloat(), noteTextPaint)
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
