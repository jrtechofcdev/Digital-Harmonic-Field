package com.example

/**
 * Dados do autor e do app, num lugar só (abertura, Sobre, textos legais).
 * Para mudar o nome, a profissão ou o contato, edite aqui.
 */
object AppInfo {
    const val APP_NAME = "Digital Harmonic Field"
    const val AUTHOR = "José Renato"
    const val BRAND = "JR TECH OFC"
    const val AUTHOR_ROLE = "Músico, fotógrafo e programador"
    const val YEAR = 2026

    /**
     * Contato para dúvidas de privacidade e direitos (e-mail ou rede social).
     * Vazio = a linha de contato não aparece. Preencha antes de publicar o app.
     */
    const val CONTACT = ""

    val copyright: String get() = "© $YEAR $AUTHOR · $BRAND"
    val version: String get() = BuildConfig.VERSION_NAME
}
