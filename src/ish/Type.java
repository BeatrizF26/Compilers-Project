public enum Type {
  TEXT,
  REAL,
  PROGRAM,
  INTEGER,
  BOOLEAN,
  LIST;

  public static Type fromString(String typeStr) {
    switch (typeStr.toLowerCase()) {
      case "text":
        return TEXT;
      case "integer":
        return INTEGER;
      case "real":
        return REAL;
      case "program":
        return PROGRAM;
      case "boolean":
        return BOOLEAN;
      default:
        throw new IllegalArgumentException("Unknown type: " + typeStr);
    }
  }
}