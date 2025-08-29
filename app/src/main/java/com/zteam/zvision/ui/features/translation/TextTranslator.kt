package com.zteam.zvision.ui.features.translation

class TextTranslatorService {
    fun mapToMlKitLang(tag: String): String? {
        return when (tag.lowercase()) {
            "af" -> TranslateLanguage.AFRIKAANS
            "ar" -> TranslateLanguage.ARABIC
            "bn" -> TranslateLanguage.BENGALI
            "bg" -> TranslateLanguage.BULGARIAN
            "ca" -> TranslateLanguage.CATALAN
            "zh", "zh-cn", "zh-hans" -> TranslateLanguage.CHINESE
            "hr" -> TranslateLanguage.CROATIAN
            "cs" -> TranslateLanguage.CZECH
            "da" -> TranslateLanguage.DANISH
            "nl" -> TranslateLanguage.DUTCH
            "en", "en-us", "en-gb" -> TranslateLanguage.ENGLISH
            "eo" -> TranslateLanguage.ESPERANTO
            "et" -> TranslateLanguage.ESTONIAN
            "fi" -> TranslateLanguage.FINNISH
            "fr", "fr-fr", "fr-ca" -> TranslateLanguage.FRENCH
            "de", "de-de" -> TranslateLanguage.GERMAN
            "el" -> TranslateLanguage.GREEK
            "gu" -> TranslateLanguage.GUJARATI
            "ht" -> TranslateLanguage.HAITIAN_CREOLE
            "he", "iw" -> TranslateLanguage.HEBREW
            "hi" -> TranslateLanguage.HINDI
            "hu" -> TranslateLanguage.HUNGARIAN
            "is" -> TranslateLanguage.ICELANDIC
            "id" -> TranslateLanguage.INDONESIAN
            "ga" -> TranslateLanguage.IRISH
            "it" -> TranslateLanguage.ITALIAN
            "ja" -> TranslateLanguage.JAPANESE
            "kn" -> TranslateLanguage.KANNADA
            "kk" -> TranslateLanguage.KAZAKH
            "ko" -> TranslateLanguage.KOREAN
            "lv" -> TranslateLanguage.LATVIAN
            "lt" -> TranslateLanguage.LITHUANIAN
            "ms" -> TranslateLanguage.MALAY
            "mt" -> TranslateLanguage.MALTESE
            "mr" -> TranslateLanguage.MARATHI
            "no", "nb", "nn" -> TranslateLanguage.NORWEGIAN
            "fa", "fa-ir", "prs", "pes" -> TranslateLanguage.PERSIAN
            "pl" -> TranslateLanguage.POLISH
            "pt", "pt-pt", "pt-br" -> TranslateLanguage.PORTUGUESE
            "ro" -> TranslateLanguage.ROMANIAN
            "ru" -> TranslateLanguage.RUSSIAN
            "sk" -> TranslateLanguage.SLOVAK
            "sl" -> TranslateLanguage.SLOVENIAN
            "es", "es-es", "es-419" -> TranslateLanguage.SPANISH
            "sv" -> TranslateLanguage.SWEDISH
            "sw" -> TranslateLanguage.SWAHILI
            "tl", "fil" -> TranslateLanguage.TAGALOG
            "ta" -> TranslateLanguage.TAMIL
            "te" -> TranslateLanguage.TELUGU
            "th" -> TranslateLanguage.THAI
            "tr" -> TranslateLanguage.TURKISH
            "uk" -> TranslateLanguage.UKRAINIAN
            "ur" -> TranslateLanguage.URDU
            "vi" -> TranslateLanguage.VIETNAMESE
            else -> null
        }
    }
}