package karaed.gui.align;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

record Language(String code, String name) {

    @Override
    public String toString() {
        return name;
    }

    private static final List<String> LANG_DATA = List.of(
        "en", "english",
        "zh", "chinese",
        "de", "german",
        "es", "spanish",
        "ru", "russian",
        "ko", "korean",
        "fr", "french",
        "ja", "japanese",
        "pt", "portuguese",
        "tr", "turkish",
        "pl", "polish",
        "ca", "catalan",
        "nl", "dutch",
        "ar", "arabic",
        "sv", "swedish",
        "it", "italian",
        "id", "indonesian",
        "hi", "hindi",
        "fi", "finnish",
        "vi", "vietnamese",
        "he", "hebrew",
        "uk", "ukrainian",
        "el", "greek",
        "ms", "malay",
        "cs", "czech",
        "ro", "romanian",
        "da", "danish",
        "hu", "hungarian",
        "ta", "tamil",
        "no", "norwegian",
        "th", "thai",
        "ur", "urdu",
        "hr", "croatian",
        "bg", "bulgarian",
        "lt", "lithuanian",
        "la", "latin",
        "mi", "maori",
        "ml", "malayalam",
        "cy", "welsh",
        "sk", "slovak",
        "te", "telugu",
        "fa", "persian",
        "lv", "latvian",
        "bn", "bengali",
        "sr", "serbian",
        "az", "azerbaijani",
        "sl", "slovenian",
        "kn", "kannada",
        "et", "estonian",
        "mk", "macedonian",
        "br", "breton",
        "eu", "basque",
        "is", "icelandic",
        "hy", "armenian",
        "ne", "nepali",
        "mn", "mongolian",
        "bs", "bosnian",
        "kk", "kazakh",
        "sq", "albanian",
        "sw", "swahili",
        "gl", "galician",
        "mr", "marathi",
        "pa", "punjabi",
        "si", "sinhala",
        "km", "khmer",
        "sn", "shona",
        "yo", "yoruba",
        "so", "somali",
        "af", "afrikaans",
        "oc", "occitan",
        "ka", "georgian",
        "be", "belarusian",
        "tg", "tajik",
        "sd", "sindhi",
        "gu", "gujarati",
        "am", "amharic",
        "yi", "yiddish",
        "lo", "lao",
        "uz", "uzbek",
        "fo", "faroese",
        "ht", "haitian creole",
        "ps", "pashto",
        "tk", "turkmen",
        "nn", "nynorsk",
        "mt", "maltese",
        "sa", "sanskrit",
        "lb", "luxembourgish",
        "my", "myanmar",
        "bo", "tibetan",
        "tl", "tagalog",
        "mg", "malagasy",
        "as", "assamese",
        "tt", "tatar",
        "haw", "hawaiian",
        "ln", "lingala",
        "ha", "hausa",
        "ba", "bashkir",
        "jw", "javanese",
        "su", "sundanese",
        "yue", "cantonese",
        "lv", "latvian"
    );

    private static final Language AUTO_DETECT = new Language(null, "Auto-detect");
    private static final List<Language> LANGUAGES = new ArrayList<>();

    private static String capitalize(String word) {
        return word.substring(0, 1).toUpperCase() + word.substring(1);
    }

    static {
        for (int i = 0; i < LANG_DATA.size(); i += 2) {
            String code = LANG_DATA.get(i);
            String name = capitalize(LANG_DATA.get(i + 1));
            LANGUAGES.add(new Language(code, name));
        }
        LANGUAGES.sort(Comparator.comparing(Language::name));
        LANGUAGES.addFirst(AUTO_DETECT);
    }

    static Language[] languages() {
        return LANGUAGES.toArray(new Language[0]);
    }

    static Language valueOf(String code) {
        for (Language language : LANGUAGES) {
            if (Objects.equals(language.code(), code))
                return language;
        }
        return AUTO_DETECT;
    }
}
