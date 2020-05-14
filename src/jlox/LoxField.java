package jlox;

class LoxField {
  final Token name;
  final Expr initializer;

  LoxField(Token name, Expr initializer) {
    this.name = name;
    this.initializer = initializer;
  }
}
