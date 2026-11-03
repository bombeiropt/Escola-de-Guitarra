package pt.escoladeguitarra

import android.app.Activity
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.Gravity
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast

class MainActivity : Activity() {
    private val bg = Color.rgb(4, 6, 12)
    private val panel = Color.rgb(12, 17, 29)
    private val blue = Color.rgb(0, 153, 255)
    private val bright = Color.rgb(52, 190, 255)
    private val white = Color.WHITE
    private val muted = Color.rgb(190, 204, 222)
    private lateinit var root: LinearLayout

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
        content.addView(label("O progresso fica guardado neste dispositivo.", muted, 12f), margins(top = 16))
        attach(content)
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
