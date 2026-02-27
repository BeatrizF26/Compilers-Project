public enum CompShType {
    INTEGER("integer"),
    REAL("real"),
    TEXT("text"),
    PROGRAM("program"),
    BOOLEAN("boolean"),
    LIST("list"),
    VOID("void"),
    ERROR("error");

    private final String name;

    CompShType(String name) {
        this.name = name;
    }

    public String toString() {
        return name;
    }

    public boolean isNumeric() {
        return this == INTEGER || this == REAL;
    }

    public static CompShType fromString(String text) {
        for (CompShType t : CompShType.values()) {
            if (t.name.equalsIgnoreCase(text)) {
                return t;
            }
        }
        return ERROR;
    }
}