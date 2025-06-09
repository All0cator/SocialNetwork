package Messages;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

public class PayloadUpload implements Serializable {

    public int clientID;
    public String imageName;
    public byte[] imageData;
    public String textName;
    public String acompanyingText;
    public Map<Language, String> multilingualText;

    public PayloadUpload() {
        this.multilingualText = new HashMap<>();
    }

    // Get formatted text combining all languages
    public String getFormattedText(Language language) {
        if (this.multilingualText == null) return "";
        
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

    public void addText(Language language, String text) {
        if (this.multilingualText == null) {
            this.multilingualText = new HashMap<>();
        }
        this.multilingualText.put(language, text);
    }

    public String getText(Language language) {
        if (this.multilingualText == null) return null;
        return this.multilingualText.get(language);
    }

    public boolean hasText(Language language) {
        return this.multilingualText.containsKey(language) && 
               this.multilingualText.get(language) != null && 
               !this.multilingualText.get(language).trim().isEmpty();
    }
}
