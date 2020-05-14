package jlox;

import java.util.List;

interface ILoxCallable {
  int arity();

  Object call(Interpreter interpreter, List<Object> arguments);
}
