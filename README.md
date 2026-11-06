# Escola de Guitarra

Aplicação Android nativa para aprendizagem de guitarra elétrica, com interface em português europeu e tema escuro azul elétrico.

## Estrutura do projeto

```text
Escola-de-Guitarra/
├── .github/
│   └── workflows/
│       └── android.yml
├── app/
│   ├── build.gradle.kts
│   └── src/
│       └── main/
│           ├── AndroidManifest.xml
│           └── java/
│               └── pt/
│                   └── escoladeguitarra/
│                       └── MainActivity.kt
├── build.gradle.kts
├── gradle.properties
├── settings.gradle.kts
└── README.md
```

## Funcionalidades incluídas

- Percurso de aprendizagem com aulas, passos e exercícios.
- Repertório sugerido por nível, sem distribuir tablaturas ou gravações protegidas.
- Diagramas de acordes e progressões de rock, pop e blues, com posições dos dedos.
- Técnica de guitarra, pentatónica menor de Lá e improvisação, com mapas visuais do braço e nomes das notas.
- Biblioteca visual de escalas: pentatónica menor de Lá e escala maior de Dó, com orientação para memorizar notas, estudar posições e transportar padrões.
- Teoria musical aplicada ao braço da guitarra.
- Afinador cromático por microfone, com indicação de nota e desvio em cents.
- Metrónomo ajustável entre 40 e 200 BPM, com compassos selecionáveis 3/4, 4/4 e 6/8 e acentuação do primeiro tempo.
- Plano semanal de estudo e registo local de exercícios concluídos.
- Preferência de andamento do metrónomo guardada no dispositivo.
- Gestão do ciclo de vida dos recursos de áudio para evitar reprodução indevida em segundo plano.

## Tecnologia e compatibilidade

- Android nativo, Kotlin e Gradle Kotlin DSL.
- Android mínimo: API 23.
- Compilação: SDK 35 e Java 17.
- Progresso e preferências guardados localmente com SharedPreferences.
- O afinador requer autorização de acesso ao microfone.
- O workflow GitHub Actions produz um APK de teste (debug).

## Versão

- Nome: 1.5.2
- Código: 8
- Alteração recente: nova secção dedicada aos mapas visuais de escalas, com pentatónica menor de Lá e escala maior de Dó.

## Limites de validação

Uma compilação bem-sucedida confirma que o projeto compila no ambiente de CI; não substitui testes em dispositivos Android reais. O APK debug destina-se a testes e não é uma versão release assinada para distribuição comercial.
