import java.util.List;

public class ListValue extends Value {
  private final List<Value> value;

  public ListValue(List<Value> elems) {
    super(Type.LIST);
    this.value = elems;
  }

  @Override
  public Object value() {
    return this.value;
  }

  @Override
  public Value multiply(Value other) {
    throw new RuntimeException("Cannot multiply List");
  }

  @Override
  public Value divide(Value other) {
    throw new RuntimeException("Cannot divide List");
  }

  @Override
  public Value add(Value other) {
    throw new RuntimeException("Cannot add List");
  }

  @Override
  public Value subtract(Value other) {
    throw new RuntimeException("Cannot subtract List");
  }

  @Override
  public Value unary(String sign) {
    throw new RuntimeException("Cannot apply unary operator '" + sign + "' on List");
  }

  @Override
  public Value convertTo(Type type) {
    if (type == Type.PROGRAM) {
      return new Program(this);
    }
    throw new RuntimeException("Cannot convert List");
  }

  @Override
  public String toString() {
    return this.value().toString();
  }

}
