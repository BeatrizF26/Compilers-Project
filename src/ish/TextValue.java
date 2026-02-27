public class TextValue extends Value {
  private String val;

  public TextValue(String val) {
    super(Type.TEXT);
    this.val = val;
  }

  public TextValue() {
    super(Type.TEXT);
    this.val = "";
  }

  public String value() {
    return this.val;
  }

  @Override
  public Value multiply(Value other) {
    throw new RuntimeException("Cannot multiply Text");
  }

  @Override
  public Value divide(Value other) {
    throw new RuntimeException("Cannot divide Text");
  }

  @Override
  public Value add(Value other) {
    if (other.isText()) {
      return new TextValue(this.val + ((TextValue) other).value());
    } else if (other.isProgram()) {
      return new TextValue(this.val + ((Program) other).stdout); // concatenate stdout
    } else if (other.isReal()) {
      return new TextValue(this.val + ((RealValue) other).value().toString());
    } else if (other.isInteger()) {
      return new TextValue(this.val + ((IntegerValue) other).value().toString());
    } else if (other.isBool()) {
      return new TextValue(this.val + ((BooleanValue) other).value().toString());
    }
    throw new RuntimeException("Cannot add Text with " + other.type());
  }

  @Override
  public Value subtract(Value other) {
    throw new RuntimeException("Cannot subtract Text");
  }

  @Override
  public Value unary(String sign) {
    throw new RuntimeException("Cannot apply unary operator on Text");
  }

  @Override
  public Value convertTo(Type type) {
    switch (type) {
      case INTEGER:
        return new IntegerValue(Integer.parseInt(this.val));
      case REAL:
        return new RealValue(Double.parseDouble(this.val));
      case TEXT:
        return this;
      case PROGRAM:
        return new Program(this);
      default:
        throw new RuntimeException("Cannot convert Text to " + type);
    }
  }

  @Override
  public String toString() {
    return this.value();
  }

}
