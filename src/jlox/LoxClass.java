package jlox;

import java.util.List;
import java.util.Map;

class LoxClass implements ILoxCallable {
  final String name;
  private final Map<String, LoxFunction> methods;
  private final Map<String, LoxField> fields;

  LoxClass(String name, Map<String, LoxField> fields, Map<String, LoxFunction> methods) {
    this.name = name;
    this.fields = fields;
    this.methods = methods;
  }

  LoxFunction findMethod(String name) {
    if (methods.containsKey(name)) {
      return methods.get(name);
    }

    return null;
  }

  LoxField findField(String name) {
    if (fields.containsKey(name)) {
      return fields.get(name);
    }

    return null;
  }

  Map<String, LoxField> getFields() {
    return fields;
  }

  Map<String, LoxFunction> getMethods() {
    return methods;
  }

  @Override
  public Object call(Interpreter interpreter, List<Object> arguments) {
    LoxInstance instance = new LoxInstance(this, interpreter);
    LoxFunction initializer = findMethod("init");
    if (initializer != null) {
      initializer.bind(instance).call(interpreter, arguments);
    }
    return instance;
  }

  @Override
  public int arity() {
    LoxFunction initializer = findMethod("init");
    if (initializer == null)
      return 0;
    return initializer.arity();
  }
}
