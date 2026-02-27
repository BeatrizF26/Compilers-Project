
public abstract class Value {

  protected final Type type;

  protected Value(Type type) {
    this.type = type;
  }

  public Type type() {
    return this.type;
  }

  public boolean isText() {
    return this.type == Type.TEXT;
  }

  public boolean isInteger() {
    return this.type == Type.INTEGER;
  }

  public boolean isReal() {
    return this.type == Type.REAL;
  }

  public boolean isProgram() {
    return this.type == Type.PROGRAM;
  }

  public boolean isBool() {
    return this.type == Type.BOOLEAN;
  }

  public abstract Object value();

  public abstract Value multiply(Value other);

  public abstract Value divide(Value other);

  public abstract Value add(Value other);

  public abstract Value subtract(Value other);

  public abstract Value unary(String sign);

  public abstract Value convertTo(Type type);

}