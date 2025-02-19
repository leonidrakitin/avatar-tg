package dev.avatar.middle.model;

public enum Language {
    RU("🇷🇺 Русский"),
    EN("🇬🇧 English"),
    DE("🇩🇪 Deutsch"),
    AR("🇸🇦 العربية"),
    ZH("🇨🇳 中文"),
    IT("🇮🇹 Italiano"),
    FR("🇫🇷 Français"),
    ES("🇪🇸 Español"),
    HI("🇮🇳 हिन्दी"),
    TR("🇹🇷 Türkçe"),
    JA("🇯🇵 日本語"),
    PT("🇵🇹 Português");

    private String language;

    Language(String language) {
        this.language = language;
    }

    public String getLanguage() {
        return language;
    }
}
