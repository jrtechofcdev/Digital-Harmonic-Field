package com.example.legal

import com.example.AppInfo
import com.example.R

/**
 * Textos legais do app (mostrados em Ajustes > Sobre e legal). A mesma versão
 * fica em docs/legal/ para publicar num site, se o app for para a loja.
 * Ao mudar algo aqui, atualize a data e o arquivo em docs/legal/.
 */
const val LEGAL_UPDATED = "13 de outubro de 2026"

data class LegalSection(val title: String, val paragraphs: List<String>)

/** Um componente de terceiros usado no app e a licença dele. */
data class ThirdParty(val name: String, val owner: String, val license: String, val rawText: Int)

enum class LegalDoc(val title: String, val subtitle: String) {
    PRIVACIDADE("Política de privacidade", "O que o app faz (e não faz) com seus dados"),
    TERMOS("Termos de uso", "Regras de uso e direitos autorais"),
    PERMISSOES("Permissões", "Microfone e vibração: para que servem"),
    LICENCAS("Licenças de código aberto", "Bibliotecas, fonte e ícones usados no app"),
}

private val contactLine: String
    get() = if (AppInfo.CONTACT.isNotBlank()) "Contato: ${AppInfo.CONTACT}." else
        "Dúvidas: fale com o desenvolvedor pelos canais da ${AppInfo.BRAND}."

val privacySections: List<LegalSection> get() = listOf(
    LegalSection("Resumo", listOf(
        "O ${AppInfo.APP_NAME} não coleta, não vende e não compartilha dados pessoais. Não tem conta, login, anúncios, rastreamento nem análise de uso, e funciona sem internet.",
    )),
    LegalSection("Quem é o responsável", listOf(
        "${AppInfo.AUTHOR} (${AppInfo.BRAND}), desenvolvedor do app. $contactLine",
    )),
    LegalSection("Microfone", listOf(
        "Usado só no Detectar tom, quando você toca no botão e autoriza.",
        "O som é analisado na memória do aparelho, na hora. Com a tela aberta, ficam guardados no máximo os últimos 5 segundos, e tudo é apagado ao sair dela. Nada é gravado em arquivo nem enviado para lugar nenhum.",
        "Você pode negar ou retirar a permissão nas configurações do Android. O resto do app continua funcionando.",
    )),
    LegalSection("O que fica guardado no seu aparelho", listOf(
        "Tons favoritos, o repertório do culto (números, nomes e observações que você digita) e as preferências do app (tema, ícone, notação, abertura etc.).",
        "Esses dados ficam só no seu celular. Você pode apagá-los em Ajustes > Dados, limpando os dados do app ou desinstalando.",
        "Se o backup automático do Android estiver ligado no seu celular, essas preferências podem ir no backup da sua conta Google, sob o seu controle e o do Google.",
    )),
    LegalSection("Doações", listOf(
        "O botão Apoiar abre a página do Mercado Pago no navegador. O pagamento acontece fora do app, sob a política de privacidade do Mercado Pago. O app não recebe nem guarda dados de pagamento.",
    )),
    LegalSection("Compartilhar o repertório", listOf(
        "Quando você toca em Compartilhar a lista, o texto vai para o app que você escolher (WhatsApp, por exemplo). Só é enviado o que aparece no texto, e só porque você mandou.",
    )),
    LegalSection("Crianças", listOf(
        "O app pode ser usado por qualquer idade. Como não coleta dados de ninguém, também não coleta dados de crianças e adolescentes.",
    )),
    LegalSection("Seus direitos (LGPD)", listOf(
        "A Lei Geral de Proteção de Dados (Lei 13.709/2018) garante o acesso, a correção e a exclusão dos seus dados. Como o desenvolvedor não recebe nenhum dado seu, tudo o que existe está no seu aparelho e sob o seu controle.",
    )),
    LegalSection("Versão de treino (DEV)", listOf(
        "Existe uma versão interna, usada só pelo autor no próprio aparelho, que grava trechos de áudio para treinar o detector de tom. Ela não é distribuída ao público. A versão que você usa nunca grava áudio.",
    )),
    LegalSection("Mudanças nesta política", listOf(
        "Qualquer mudança aparece aqui, com a data atualizada. Última atualização: $LEGAL_UPDATED.",
    )),
)

val termsSections: List<LegalSection> get() = listOf(
    LegalSection("Aceite", listOf(
        "Ao usar o ${AppInfo.APP_NAME}, você concorda com estes termos. Se não concordar, não use o app.",
    )),
    LegalSection("Licença de uso", listOf(
        "O app é gratuito para uso pessoal, inclusive em igrejas, ensaios e aulas. A licença é pessoal, não exclusiva e intransferível.",
        "Não é permitido copiar, modificar, descompilar, vender, alugar, redistribuir ou publicar o app, ou partes dele (código, design, logos, vídeo de abertura, textos e imagens), sem autorização por escrito do autor.",
    )),
    LegalSection("Direitos autorais e marcas", listOf(
        "${AppInfo.copyright}. Todos os direitos reservados.",
        "O nome ${AppInfo.APP_NAME}, a marca DHF, a marca ${AppInfo.BRAND}, os logos e o vídeo de abertura pertencem ao autor e são protegidos pela Lei de Direitos Autorais (Lei 9.610/1998) e pela Lei do Software (Lei 9.609/1998).",
        "Bibliotecas, fonte e ícones de terceiros seguem as próprias licenças (veja Licenças de código aberto).",
    )),
    LegalSection("Conteúdo musical", listOf(
        "Campos harmônicos, graus, progressões e acordes são conhecimento musical de uso livre. O app explica e calcula essas relações. Ele não contém letras, partituras nem gravações de hinos ou músicas.",
        "Os números e nomes de hinos do repertório são digitados por você e ficam no seu aparelho. A Harpa Cristã é citada só como referência de uso; o app não tem vínculo com a CPAD, com igrejas nem com editoras.",
    )),
    LegalSection("Detector de tom", listOf(
        "O tom sugerido é uma estimativa automática e pode errar, principalmente com muito barulho. Sempre confira com o ouvido antes de tocar.",
    )),
    LegalSection("Doações", listOf(
        "As doações são voluntárias, não liberam recursos extras e não dão direito a suporte especial. O pagamento é feito e processado pelo Mercado Pago, fora do app.",
    )),
    LegalSection("Garantia e responsabilidade", listOf(
        "O app é oferecido como está, sem garantia de funcionamento sem falhas em todos os aparelhos. O autor não se responsabiliza por usos que violem a lei ou estes termos.",
        "Nada aqui limita direitos que o Código de Defesa do Consumidor garante a você.",
    )),
    LegalSection("Lei aplicável", listOf(
        "Estes termos seguem as leis do Brasil. Última atualização: $LEGAL_UPDATED. $contactLine",
    )),
)

val permissionSections: List<LegalSection> get() = listOf(
    LegalSection("Microfone (gravar áudio)", listOf(
        "Para quê: ouvir quem está cantando e descobrir o tom do hino (Detectar tom).",
        "Quando é pedida: só na primeira vez que você abre o Detectar tom, depois de uma explicação na própria tela.",
        "O que acontece com o som: é analisado na memória, na hora, e apagado ao sair da tela. Nada é gravado nem enviado.",
        "Se você negar: só o Detectar tom deixa de funcionar. Para mudar depois: Configurações do Android > Apps > ${AppInfo.APP_NAME} > Permissões.",
    )),
    LegalSection("Vibração", listOf(
        "Para quê: uma vibração curta quando o Detectar tom termina, para você saber o resultado sem olhar a tela.",
        "Pode desligar em Ajustes > No culto > Vibrar ao terminar a detecção.",
    )),
    LegalSection("O que o app NÃO pede", listOf(
        "Internet, localização, contatos, câmera, fotos, arquivos, notificações ou conta. O botão Apoiar só abre o navegador, e o Compartilhar a lista só abre o app que você escolher.",
    )),
)

val thirdParties: List<ThirdParty> = listOf(
    ThirdParty("Jetpack Compose, AndroidX e Material 3", "The Android Open Source Project / Google LLC", "Apache License 2.0", R.raw.licenca_apache),
    ThirdParty("Kotlin e kotlinx.coroutines", "JetBrains s.r.o. e colaboradores", "Apache License 2.0", R.raw.licenca_apache),
    ThirdParty("Material Icons", "Google LLC", "Apache License 2.0", R.raw.licenca_apache),
    ThirdParty("Fonte Space Grotesk", "The Space Grotesk Project Authors (Florian Karsten)", "SIL Open Font License 1.1", R.raw.licenca_ofl),
    ThirdParty("Ícones Phosphor", "Phosphor Icons (Tobias Fried e Helena Zhang)", "MIT", R.raw.licenca_mit),
)

fun sectionsFor(doc: LegalDoc): List<LegalSection> = when (doc) {
    LegalDoc.PRIVACIDADE -> privacySections
    LegalDoc.TERMOS -> termsSections
    LegalDoc.PERMISSOES -> permissionSections
    LegalDoc.LICENCAS -> emptyList()
}
