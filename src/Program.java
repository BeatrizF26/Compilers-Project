public class Program extends Value {
  public String stdout;
  public String stderr;
  public Integer exitValue = null;
  public Value value;

  public Program() {
    super(Type.PROGRAM);
    this.stdout = null;
    this.stderr = null;
    this.exitValue = null;
    this.value = null;
  }

  public Program(Value val) {
    super(Type.PROGRAM);
    this.value = val;
  }

  public Program(String out, String err, int code) {
    super(Type.PROGRAM);
    this.stdout = out;
    this.stderr = err;
    this.exitValue = code;
    this.value = null;
  }

  public Program(String out, String err, int code, Value val) {
    super(Type.PROGRAM);
    this.stdout = out;
    this.stderr = err;
    this.exitValue = code;
    this.value = val;
  }

  public Value value() {
    return this.value;
  }

  public String stdout() {
    return this.stdout;
  }

  public String stderr() {
    return this.stderr;
  }

  public int exitValue() {
    return this.exitValue;
  }

  @Override
  public Value multiply(Value other) {
    throw new RuntimeException("Cannot multiply Program");
  }

  @Override
  public Value divide(Value other) {
    throw new RuntimeException("Cannot divide Program");
  }

  @Override
  public Value add(Value other) {
    throw new RuntimeException("Cannot add Program");
  }

  @Override
  public Value subtract(Value other) {
    throw new RuntimeException("Cannot subtract Program");
  }

  @Override
  public Value unary(String sign) {
    throw new RuntimeException("Cannot apply unary operator on Program");
  }

  @Override
  public Value convertTo(Type type) {
    throw new RuntimeException("Cannot convert Program to " + type);
  }

  @Override
  public String toString() {
      return "stdout:\n" + stdout + 
            "\nstderr:\n" + stderr + 
            "\nexit code: " + exitValue;
            //+ "\nvalue: " + (value != null ? value.toString() : "null");
  }

}
