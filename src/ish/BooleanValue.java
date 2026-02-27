public class BooleanValue extends Value {
  private final Boolean val;

  public BooleanValue(Boolean val) {
    super(Type.BOOLEAN);
    this.val = val;
  }

  public BooleanValue() {
    super(Type.BOOLEAN);
    this.val = false; // by default
  }

  public Boolean value() {
    return this.val;
  }

  public Value and(Value other) {
    if (isBool()) {
      return new BooleanValue(this.val && (boolean) other.value());
    }
    throw new RuntimeException("Cannot perform 'and' operation between Boolean and " + other.type());
  }

  public Value or(Value other) {
    if (isBool()) {
      return new BooleanValue(this.val || (boolean) other.value());
    }
    throw new RuntimeException("Cannot perform 'or' operation between Boolean and " + other.type());
  }

  @Override
  public Value multiply(Value other) {
    throw new RuntimeException("Cannot multiply Boolean");
  }

  @Override
  public Value divide(Value other) {
    throw new RuntimeException("Cannot divide Boolean");
  }

  @Override
  public Value add(Value other) {
    throw new RuntimeException("Cannot add Boolean");
  }

  @Override
  public Value subtract(Value other) {
    throw new RuntimeException("Cannot subtract Boolean");
  }

  @Override
  public Value unary(String sign) {
    if (sign.equals("not")) {
      return new BooleanValue(!this.val);
    }
    throw new RuntimeException("Cannot apply unary operator '" + sign + "' on Boolean");
  }

  @Override
  public Value convertTo(Type type) {
    if (type == Type.PROGRAM) {
      return new Program(this);
    }
    throw new RuntimeException("Cannot convert Boolean");
  }

  @Override
  public String toString() {
    return this.value().toString();
  }
}