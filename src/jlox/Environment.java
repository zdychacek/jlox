package jlox;

import java.util.HashMap;
import java.util.Map;

class Environment {
  Environment enclosing;
  private Map<String, Object> values = new HashMap<>();

  Environment() {
    enclosing = null;
  }

  Environment(Environment enclosing) {
    this.enclosing = enclosing;
  }

  Object get(Token name) {
    if (values.containsKey(name.lexeme)) {
      return values.get(name.lexeme);
    }

    if (enclosing != null)
      return enclosing.get(name);

    throw new RuntimeError(name, "Undefined variable '" + name.lexeme + "'.");
  }

  void assign(Token name, Object value) {
    if (values.containsKey(name.lexeme)) {
      values.put(name.lexeme, value);
      return;
    }

    if (enclosing != null) {
      enclosing.assign(name, value);
      return;
    }

    throw new RuntimeError(name, "Undefined variable '" + name.lexeme + "'.");
  }

  Environment define(String name, Object value) {
    Environment newEnv = new Environment();

    newEnv.enclosing = this.enclosing;
    newEnv.values = new HashMap<>(this.values);
    newEnv.values.put(name, value);

    return newEnv;
  }
}
