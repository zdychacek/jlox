package jlox;

import java.util.ArrayList;
import java.util.Map;

class LoxInstance {
  private LoxClass klass;
  private Environment env;

  LoxInstance(LoxClass klass, Interpreter interpreter) {
    this.klass = klass;

    // create mutable environment
    env = new Environment(interpreter.getEnvironment(), true);
    env.define("this", this);

    for (Map.Entry<String, LoxFunction> entry : klass.getMethods().entrySet()) {
      env.define(entry.getKey(), entry.getValue());
    }

    Environment prevEnv = interpreter.setEnvironment(env);

    interpreter.interpret(new ArrayList<>(klass.getFields().values()));

    interpreter.setEnvironment(prevEnv);
  }

  Object get(Token name) {
    if (env.has(name.lexeme)) {
      Object property = env.get(name);

      if (property instanceof LoxFunction) {
        return ((LoxFunction) property).bind(this);
      } else {
        return property;
      }
    }

    throw new RuntimeError(name, "Undefined property '" + name.lexeme + "'.");
  }

  void set(Token name, Object value) {
    env.assign(name, value);
  }

  Environment getEnvironment() {
    return this.env;
  }

  LoxClass getKlass() {
    return klass;
  }

  @Override
  public String toString() {
    return klass.name + " instance";
  }
}
