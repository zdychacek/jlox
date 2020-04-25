package jlox;

class Jump extends RuntimeException {
  public enum Type {
    BREAK, CONTINUE
  }

  final Type type;

  Jump(Type type) {
    super(null, null, false, false);
    this.type = type;
  }
}
