public enum Type {
    INTEGER("integer"),
    REAL("real"),
    TEXT("text"),
    PROGRAM("program"),
    BOOLEAN("boolean"),
    LIST("list"),
    VOID("void"),
    ERROR("error");

    private final String name;

    Type(String name) {
        this.name = name;
    }

    public String toString() {
        return name;
    }

    public boolean isNumeric() {
        return this == INTEGER || this == REAL;
    }

    public static Type fromString(String text) {
        for (Type t : Type.values()) {
            if (t.name.equalsIgnoreCase(text)) {
                return t;
            }
        }
        return ERROR;
    }
}