package jlox;

import java.util.List;

class LoxFunction implements ILoxCallable {
  final Token name;
  final List<Stmt.FunctionParameter> params;
  final List<Stmt> body;
  final Environment closure;
  final boolean isInitializer;
  final Visibility visibility;

  LoxFunction(Token name, List<Stmt.FunctionParameter> params, List<Stmt> body, Environment closure,
      boolean isInitializer, Visibility visibility) {
    this.isInitializer = isInitializer;
    this.closure = closure;
    this.name = name;
    this.params = params;
    this.body = body;
    this.visibility = visibility;
  }

  LoxFunction bind(LoxInstance instance) {
    return new LoxFunction(name, params, body, instance.getEnvironment(), isInitializer, visibility);
  }

  @Override
  public int arity() {
    return params.size();
  }

  @Override
  public Object call(Interpreter interpreter, List<Object> arguments) {
    Environment environment = new Environment(closure);

    for (int i = 0; i < params.size(); i++) {
      environment = environment.define(params.get(i).name.lexeme, arguments.get(i));
    }

    try {
      interpreter.executeBlock(body, environment);
    } catch (Return returnValue) {
      if (isInitializer) {
        return closure.get(new Token(TokenType.IDENTIFIER, "this", null, -1));
      }

      return returnValue.value;
    }

    if (isInitializer)
      return closure.get(new Token(TokenType.IDENTIFIER, "this", null, -1));

    return null;
  }

  @Override
  public String toString() {
    return "<fn " + name.lexeme + ">";
  }
}
