package jlox;

import java.util.List;

class LoxFunction implements LoxCallable {
  private final Token name;
  private final List<Token> params;
  private final List<Stmt> body;
  private final Environment closure;
  private final boolean isInitializer;

  LoxFunction(Token name, List<Token> params, List<Stmt> body, Environment closure, boolean isInitializer) {
    this.isInitializer = isInitializer;
    this.closure = closure;
    this.name = name;
    this.params = params;
    this.body = body;
  }

  LoxFunction bind(LoxInstance instance) {
    Environment environment = new Environment(closure);
    environment.define("this", instance);
    return new LoxFunction(name, params, body, environment, isInitializer);
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
      if (isInitializer)
        return closure.getAt(0, "this");

      return returnValue.value;
    }

    if (isInitializer)
      return closure.getAt(0, "this");

    return null;
  }

  @Override
  public String toString() {
    return "<fn " + name.lexeme + ">";
  }
}
