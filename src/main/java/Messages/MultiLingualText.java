package Messages;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

public class MultiLingualText implements Serializable {
    public Map<Language, String> text;

    public MultiLingualText() {
        this.text = new HashMap<Language, String>();
    }

    // Get formatted text combining all languages
    public String getFormattedText(Language language) {
        if (this.text == null) return "";
        

        if (language == Language.BOTH) {
            StringBuilder result = new StringBuilder();
            if (hasText(Language.ENGLISH)) {
                result.append("[EN]\n").append(getText(Language.ENGLISH)).append("\n");
            }
            if (hasText(Language.GREEK)) {
                result.append("[EL]\n").append(getText(Language.GREEK)).append("\n");
            }
            return result.toString().trim();
        } else {
            String text = getText(language);
            return text != null ? text : "";
        }
    }

    public String getOneLineText(Language language) {
        if (this.text == null) return "";
        
        if (language == Language.BOTH) {
            StringBuilder result = new StringBuilder();
            if (hasText(Language.ENGLISH)) {
                result.append("[EN] ").append(getText(Language.ENGLISH)).append(" ");
            }
            if (hasText(Language.GREEK)) {
                result.append("[EL] ").append(getText(Language.GREEK)).append(" ");
            }
            return result.toString().trim();
        } else {
            String text = getText(language);
            return text != null ? text : "";
        }
    }

    public void addText(Language language, String text) {
        if (this.text == null) {
            this.text = new HashMap<Language, String>();
        }
        this.text.put(language, text);
    }

    public String getText(Language language) {
        if (this.text == null) return null;
        return this.text.get(language);
    }

    public boolean hasText(Language language) {
        return this.text.containsKey(language) && 
               this.text.get(language) != null && 
               !this.text.get(language).trim().isEmpty();
    }

    @Override
    public String toString() {
        return getFormattedText(Language.BOTH);
    }
}
