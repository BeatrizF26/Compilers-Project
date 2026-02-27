public class IntegerValue extends Value {
  private final int val;

  public IntegerValue(int val) {
    super(Type.INTEGER);
    this.val = val;
  }

  public IntegerValue() {
    super(Type.INTEGER);
    this.val = 0;
  }

  public Integer value() {
    return this.val;
  }

  @Override
  public Value multiply(Value other) {
    if (other.isInteger()) {
      return new IntegerValue(this.val * ((IntegerValue) other).value());
    } else if (other.isReal()) {
      double result = this.val * ((RealValue) other).value();
      return new RealValue(result);
    }
    throw new RuntimeException("Cannot multiply Integer by " + other.type());
  }

  @Override
  public Value divide(Value other) {
    if (other.isInteger()) {
      int divisor = ((IntegerValue) other).value();
      if (divisor == 0) {
        throw new ArithmeticException("Division by zero");
      }
      return new IntegerValue(this.val / divisor);
    } else if (other.isReal()) {
      double divisor = ((RealValue) other).value();
      if (divisor == 0) {
        throw new ArithmeticException("Division by zero");
      }
      return new RealValue(this.val / divisor);
    }
    throw new RuntimeException("Cannot divide Integer by " + other.type());
  }

  @Override
  public Value add(Value other) {
    if (other.isInteger()) {
      return new IntegerValue(this.val + ((IntegerValue) other).value());
    } else if (other.isReal()) {
      return new RealValue(this.val + ((RealValue) other).value());
    }
    throw new RuntimeException("Cannot add Integer by " + other.type());
  }

  @Override
  public Value subtract(Value other) {
    if (other.isInteger()) {
      return new IntegerValue(this.val - ((IntegerValue) other).value());
    } else if (other.isReal()) {
      return new RealValue(this.val - ((RealValue) other).value());
    }
    throw new RuntimeException("Cannot subtract Integer by " + other.type());
  }

  @Override
  public Value unary(String sign) {
    return sign.equals("-") ? new IntegerValue(-this.val) : new IntegerValue(this.val);
  }

  @Override
  public Value convertTo(Type type) {
    switch (type) {
      case INTEGER:
        return this;
      case REAL:
        return new RealValue((double) this.val);
      case TEXT:
        return new TextValue(String.valueOf(this.val));
      default:
        throw new RuntimeException("Cannot convert Integer to " + type);
    }
  }

}