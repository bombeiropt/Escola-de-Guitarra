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
    private val backgroundColor = Color.rgb(17, 19, 23)
    private val panelColor = Color.rgb(29, 32, 38)
    private val accentColor = Color.rgb(235, 137, 48)
    private val textColor = Color.rgb(245, 242, 236)
    private val mutedColor = Color.rgb(174, 177, 183)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = backgroundColor
        window.navigationBarColor = backgroundColor
        window.decorView.systemUiVisibility = 0
        setContentView(buildHome())
    }

    private fun buildHome(): ScrollView {
        val content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(22), dp(28), dp(22), dp(32))
            setBackgroundColor(backgroundColor)
        }

        content.addView(TextView(this).apply {
            text = "ESCOLA DE GUITARRA"
            textSize = 12f
            letterSpacing = 0.18f
            setTextColor(accentColor)
            typeface = Typeface.DEFAULT_BOLD
        })
        content.addView(TextView(this).apply {
            text = "A tua música\ncomeça aqui."
            textSize = 34f
            setTextColor(textColor)
            typeface = Typeface.create("sans-serif", Typeface.BOLD)
            setPadding(0, dp(12), 0, dp(8))
        })
        content.addView(TextView(this).apply {
            text = "Aprende, pratica e evolui ao teu ritmo."
            textSize = 16f
            setTextColor(mutedColor)
            setPadding(0, 0, 0, dp(22))
        })

        val featured = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(18), dp(18), dp(18), dp(18))
            background = rounded(panelColor, 18)
        }
        featured.addView(TextView(this).apply {
            text = "PERCURSO DE APRENDIZAGEM"
            textSize = 11f
            letterSpacing = 0.1f
            setTextColor(accentColor)
            typeface = Typeface.DEFAULT_BOLD
        })
        featured.addView(TextView(this).apply {
            text = "Do primeiro acorde ao teu primeiro solo."
            textSize = 19f
            setTextColor(textColor)
            typeface = Typeface.DEFAULT_BOLD
            setPadding(0, dp(9), 0, dp(6))
        })
        featured.addView(TextView(this).apply {
            text = "Aulas organizadas por dificuldade, com demonstrações, som e exercícios práticos."
            textSize = 14f
            setTextColor(mutedColor)
        })
        featured.addView(actionButton("Começar a aprender") {
            openSection("Aulas e aprendizagem")
        }, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(50)).apply { topMargin = dp(16) })
        content.addView(featured, marginParams(bottom = 24))

        content.addView(sectionHeading("EXPLORAR A ESCOLA"), marginParams(bottom = 12))
        val sections = listOf(
            "Aulas e aprendizagem" to "Lições passo a passo e teoria musical",
            "Músicas para aprender" to "Riffs, acompanhamento e solos por nível",
            "Acordes & dedilhados" to "Posições, diagramas e padrões rítmicos",
            "Técnica de guitarra" to "Palhetada, bends, vibrato, slides e legato",
            "Solos e improvisação" to "Escalas, fraseado e criação musical",
            "Som e efeitos" to "Amplificadores, distorção, delay e reverb",
            "Treino e evolução" to "Desafios, metrónomo e registo de progresso",
            "Afinador e metrónomo" to "Ferramentas essenciais para praticar"
        )
        sections.forEachIndexed { index, item ->
            val row = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                setPadding(dp(15), dp(14), dp(12), dp(14))
                background = rounded(panelColor, 14)
                isClickable = true
                isFocusable = true
                setOnClickListener { openSection(item.first) }
            }
            val number = TextView(this).apply {
                text = String.format("%02d", index + 1)
                textSize = 13f
                typeface = Typeface.DEFAULT_BOLD
                setTextColor(accentColor)
                gravity = Gravity.CENTER
                background = rounded(Color.rgb(57, 42, 31), 10)
                layoutParams = LinearLayout.LayoutParams(dp(42), dp(42))
            }
            row.addView(number)
            val labels = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(dp(13), 0, dp(4), 0)
            }
            labels.addView(TextView(this).apply {
                text = item.first
                textSize = 16f
                typeface = Typeface.DEFAULT_BOLD
                setTextColor(textColor)
            })
            labels.addView(TextView(this).apply {
                text = item.second
                textSize = 12f
                setTextColor(mutedColor)
                setPadding(0, dp(4), 0, 0)
            })
            row.addView(labels, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
            row.addView(TextView(this).apply {
                text = "›"
                textSize = 28f
                setTextColor(accentColor)
            })
            content.addView(row, marginParams(bottom = 9))
        }

        content.addView(TextView(this).apply {
            text = "PRATICA COM REGULARIDADE · EVOLUI COM CONFIANÇA"
            textSize = 10f
            letterSpacing = 0.08f
            gravity = Gravity.CENTER
            setTextColor(mutedColor)
            setPadding(0, dp(25), 0, dp(4))
        })

        return ScrollView(this).apply {
            isFillViewport = true
            addView(content)
        }
    }

    private fun sectionHeading(value: String) = TextView(this).apply {
        text = value
        textSize = 11f
        letterSpacing = 0.12f
        typeface = Typeface.DEFAULT_BOLD
        setTextColor(mutedColor)
    }

    private fun actionButton(label: String, action: () -> Unit) = Button(this).apply {
        text = label
        textSize = 15f
        isAllCaps = false
        setTextColor(Color.BLACK)
        typeface = Typeface.DEFAULT_BOLD
        background = rounded(accentColor, 12)
        setOnClickListener { action() }
    }

    private fun openSection(section: String) {
        Toast.makeText(this, "$section — conteúdo em preparação", Toast.LENGTH_SHORT).show()
    }

    private fun rounded(color: Int, radius: Int) = GradientDrawable().apply {
        setColor(color)
        cornerRadius = dp(radius).toFloat()
    }

    private fun marginParams(bottom: Int = 0) = LinearLayout.LayoutParams(
        ViewGroup.LayoutParams.MATCH_PARENT,
        ViewGroup.LayoutParams.WRAP_CONTENT
    ).apply { bottomMargin = dp(bottom) }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()
}
