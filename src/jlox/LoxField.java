package jlox;

class LoxField {
  final Stmt.Var stmt;
  final Visibility visibility;

  LoxField(Stmt.Var stmt) {
    this.stmt = stmt;
    this.visibility = stmt.visibility;
  }
}
