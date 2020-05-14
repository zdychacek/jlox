package jlox;

import java.util.HashMap;
import java.util.Map;

class Environment {
  Environment enclosing;
  private Map<String, Object> values = new HashMap<>();
  private boolean isMutable;

  Environment() {
    enclosing = null;
    isMutable = false;
  }

  Environment(Environment enclosing) {
    this.enclosing = enclosing;
    this.isMutable = false;
  }

  Environment(Environment enclosing, boolean isMutable) {
    this.enclosing = enclosing;
    this.isMutable = isMutable;
  }

  public Object get(Token name) {
    if (values.containsKey(name.lexeme)) {
      return values.get(name.lexeme);
    }

    if (enclosing != null)
      return enclosing.get(name);

    throw new RuntimeError(name, "Undefined variable '" + name.lexeme + "'.");
  }

  public void assign(Token name, Object value) {
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

  public boolean has(String key) {
    return values.containsKey(key);
  }

  public Environment define(String name, Object value) {
    if (!isMutable) {
      Environment newEnv = new Environment();

      newEnv.enclosing = this.enclosing;
      newEnv.values = new HashMap<>(this.values);
      newEnv.values.put(name, value);

      return newEnv;
    }

    values.put(name, value);

    return this;
  }

  @Override
  public String toString() {
    StringBuilder strBuilder = new StringBuilder();

    values.forEach((key, value) -> strBuilder.append(key + ":" + value + "\n"));

    return strBuilder.toString();
  }
}
