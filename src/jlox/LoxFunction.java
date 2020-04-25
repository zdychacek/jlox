package jlox;

import java.util.List;

class LoxFunction implements LoxCallable {
  private final Token name;
  private final List<Token> params;
  private final List<Stmt> body;
  private final Environment closure;

  LoxFunction(Stmt.Function stmt, Environment closure) {
    this.closure = closure;
    this.name = stmt.name;
    this.params = stmt.params;
    this.body = stmt.body;
  }

  LoxFunction(Expr.Function expr, Environment closure) {
    this.closure = closure;
    this.name = expr.name;
    this.params = expr.params;
    this.body = expr.body;
  }

  @Override
  public int arity() {
    return params.size();
  }

  @Override
  public Object call(Interpreter interpreter, List<Object> arguments) {
    Environment environment = new Environment(closure);

    for (int i = 0; i < params.size(); i++) {
      environment.define(params.get(i).lexeme, arguments.get(i));
    }

    try {
      interpreter.executeBlock(body, environment);
    } catch (Return returnValue) {
      return returnValue.value;
    }

    return null;
  }

  @Override
  public String toString() {
    return "<fn " + name.lexeme + ">";
  }
}
