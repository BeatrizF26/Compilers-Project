public class RealValue extends Value {
  private double val;

  public RealValue(double val) {
    super(Type.REAL);
    this.val = val;
  }

  public RealValue() {
    super(Type.REAL);
    this.val = 0.0;
  }

  public Double value() {
    return this.val;
  }

  @Override
  public Value multiply(Value other) {
    if (other.isReal()) {
      return new RealValue(this.val * ((RealValue) other).value());
    } else if (other.isInteger()) {
      return new RealValue(this.val * ((IntegerValue) other).value());
    }
    throw new RuntimeException("Cannot multiply Real by " + other.type());
  }

  @Override
  public Value divide(Value other) {
    if (other.isReal()) {
      double divisor = ((RealValue) other).value();
      if (divisor == 0) {
        throw new ArithmeticException("Division by zero");
      }
      return new RealValue(this.val / divisor);
    } else if (other.isInteger()) {
      int divisor = ((IntegerValue) other).value();
      if (divisor == 0) {
        throw new ArithmeticException("Division by zero");
      }
      return new RealValue(this.val / divisor);
    }
    throw new RuntimeException("Cannot divide Real by " + other.type());
  }

  @Override
  public Value add(Value other) {
    if (other.isReal()) {
      return new RealValue(this.val + ((RealValue) other).value());
    } else if (other.isInteger()) {
      return new RealValue(this.val + ((IntegerValue) other).value());
    }
    throw new RuntimeException("Cannot add Real by " + other.type());
  }

  @Override
  public Value subtract(Value other) {
    if (other.isReal()) {
      return new RealValue(this.val - ((RealValue) other).value());
    } else if (other.isInteger()) {
      return new RealValue(this.val - ((IntegerValue) other).value());
    }
    throw new RuntimeException("Cannot subtract Real by " + other.type());
  }

  @Override
  public Value unary(String sign) {
    return sign.equals("-") ? new RealValue(-this.val) : new RealValue(this.val);
  }

  @Override
  public Value convertTo(Type type) {
    switch (type) {
      case REAL:
        return this;
      case INTEGER:
        return new IntegerValue((int) this.val);
      case TEXT:
        return new TextValue(String.valueOf(this.val));
      default:
        throw new RuntimeException("Cannot convert Real to " + type);
    }
  }
}
