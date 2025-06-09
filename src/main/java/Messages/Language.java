package Messages;

public enum Language {
    ENGLISH("en"),
    GREEK("el"),
    BOTH("both");

    private final String code;

    Language(String code) {
        this.code = code;
    }

    public String getCode() {
        return code;
    }

    public static Language fromCode(String code) {
        for (Language lang : values()) {
            if (lang.code.equals(code)) {
                return lang;
            }
        }
        return ENGLISH;  // Default fallback
    }
}
