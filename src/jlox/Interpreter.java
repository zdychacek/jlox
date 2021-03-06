package jlox;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

class Interpreter implements Expr.IVisitor<Object>, Stmt.IVisitor<Void> {
  private Environment globals = new Environment();
  private Environment environment = null;

  Interpreter() {
    globals = globals.define("clock", new ILoxCallable() {
      @Override
      public int arity() {
        return 0;
      }

      @Override
      public Object call(Interpreter interpreter, List<Object> arguments) {
        return (double) System.currentTimeMillis() / 1000.0;
      }

      @Override
      public String toString() {
        return "<native fn>";
      }
    });
    globals = globals.define("toString", new ILoxCallable() {
      @Override
      public int arity() {
        return 1;
      }

      @Override
      public Object call(Interpreter interpreter, List<Object> arguments) {
        return stringify(arguments.get(0));
      }

      @Override
      public String toString() {
        return "<native fn>";
      }
    });
    globals = globals.define("print", new ILoxCallable() {
      @Override
      public int arity() {
        return 1;
      }

      @Override
      public Object call(Interpreter interpreter, List<Object> arguments) {
        Object arg = arguments.get(0);

        System.out.println(stringify(arg));

        return null;
      }

      @Override
      public String toString() {
        return "<native fn>";
      }
    });

    environment = globals;
  }

  void interpret(List<Stmt> statements) {
    try {
      for (Stmt statement : statements) {
        execute(statement);
      }
    } catch (RuntimeError error) {
      Lox.runtimeError(error);
    }
  }

  @Override
  public Object visitLiteralExpr(Expr.Literal expr) {
    return expr.value;
  }

  @Override
  public Object visitLogicalExpr(Expr.Logical expr) {
    Object left = evaluate(expr.left);

    if (expr.operator.type == TokenType.OR) {
      if (isTruthy(left))
        return left;
    } else {
      if (!isTruthy(left))
        return left;
    }

    return evaluate(expr.right);
  }

  @Override
  public Object visitSetExpr(Expr.Set expr) {
    Object object = evaluate(expr.object);

    if (!(object instanceof LoxInstance)) {
      throw new RuntimeError(expr.name, "Only instances have fields.");
    }

    Object value = evaluate(expr.value);

    LoxInstance instance = (LoxInstance) object;

    try {
      instance.set(expr.name, value);
    } catch (RuntimeError error) {
      throw new RuntimeError(error.token,
          "Property '" + error.token.lexeme + "' does not exist on type '" + instance.getKlass().name + "'.");
    }

    return value;
  }

  @Override
  public Object visitThisExpr(Expr.This expr) {
    return environment.get(expr.keyword);
  }

  @Override
  public Object visitGroupingExpr(Expr.Grouping expr) {
    return evaluate(expr.expression);
  }

  @Override
  public Void visitExpressionStmt(Stmt.Expression stmt) {
    evaluate(stmt.expression);
    return null;
  }

  @Override
  public Void visitIfStmt(Stmt.If stmt) {
    if (isTruthy(evaluate(stmt.condition))) {
      execute(stmt.thenBranch);
    } else if (stmt.elseBranch != null) {
      execute(stmt.elseBranch);
    }
    return null;
  }

  @Override
  public Void visitReturnStmt(Stmt.Return stmt) {
    Object value = null;
    if (stmt.value != null)
      value = evaluate(stmt.value);

    throw new Return(value);
  }

  @Override
  public Void visitVarStmt(Stmt.Var stmt) {
    Object value = null;
    if (stmt.initializer != null) {
      value = evaluate(stmt.initializer);
    }

    environment = environment.define(stmt.name.lexeme, value);
    return null;
  }

  @Override
  public Void visitWhileStmt(Stmt.While stmt) {
    while (isTruthy(evaluate(stmt.condition))) {
      try {
        execute(stmt.body);

      } catch (Jump jump) {
        if (jump.type == Jump.Type.BREAK) {
          break;
        } else if (jump.type == Jump.Type.CONTINUE) {
          continue;
        }
      }
    }

    return null;
  }

  @Override
  public Void visitBreakStmt(Stmt.Break stmt) {
    throw new Jump(Jump.Type.BREAK);
  }

  @Override
  public Void visitContinueStmt(Stmt.Continue stmt) {
    throw new Jump(Jump.Type.CONTINUE);
  }

  @Override
  public Object visitAssignExpr(Expr.Assign expr) {
    Object value = evaluate(expr.value);

    environment.assign(expr.name, value);
    return value;
  }

  @Override
  public Object visitBinaryExpr(Expr.Binary expr) {
    Object left = evaluate(expr.left);
    Object right = evaluate(expr.right);

    switch (expr.operator.type) {
      case GREATER:
        checkNumberOperands(expr.operator, left, right);
        return (double) left > (double) right;
      case GREATER_EQUAL:
        checkNumberOperands(expr.operator, left, right);
        return (double) left >= (double) right;
      case LESS:
        checkNumberOperands(expr.operator, left, right);
        return (double) left < (double) right;
      case LESS_EQUAL:
        checkNumberOperands(expr.operator, left, right);
        return (double) left <= (double) right;
      case MINUS:
        return (double) left - (double) right;
      case PLUS:
        if (left instanceof Double && right instanceof Double) {
          return (double) left + (double) right;
        }

        if (left instanceof String && right instanceof String) {
          return (String) left + (String) right;
        }

        throw new RuntimeError(expr.operator, "Operands must be two numbers or two strings.");
      case BANG_EQUAL:
        return !isEqual(left, right);
      case EQUAL_EQUAL:
        return isEqual(left, right);
      case SLASH:
        checkNumberOperands(expr.operator, left, right);
        return (double) left / (double) right;
      case STAR:
        checkNumberOperands(expr.operator, left, right);
        return (double) left * (double) right;
    }

    // Unreachable.
    return null;
  }

  @Override
  public Object visitCallExpr(Expr.Call expr) {
    Object callee = evaluate(expr.callee);

    if (!(callee instanceof ILoxCallable)) {
      throw new RuntimeError(expr.paren, "Can only call functions, methods and classes.");
    }

    ILoxCallable function = (ILoxCallable) callee;

    List<Object> arguments = new ArrayList<>();
    for (Expr argument : expr.arguments) {
      arguments.add(evaluate(argument));
    }

    if (arguments.size() != function.arity()) {
      throw new RuntimeError(expr.paren,
          "Expected " + function.arity() + " arguments but got " + arguments.size() + ".");
    }

    return function.call(this, arguments);
  }

  @Override
  public Object visitGetExpr(Expr.Get expr) {
    Object object = evaluate(expr.object);
    if (object instanceof LoxInstance) {
      LoxInstance instance = (LoxInstance) object;

      LoxFunction method = instance.getKlass().findMethod(expr.name.lexeme);
      LoxField field = instance.getKlass().findField(expr.name.lexeme);

      if (method != null && method.visibility == Visibility.PRIVATE
          || field != null && field.visibility == Visibility.PRIVATE) {
        throw new RuntimeError(expr.name, "Property '" + expr.name.lexeme
            + "' is private and only accessible within class '" + instance.getKlass().name + "'.");
      }

      try {
        return instance.get(expr.name);
      } catch (RuntimeError error) {
        throw new RuntimeError(error.token,
            "Property '" + error.token.lexeme + "' does not exist on type '" + instance.getKlass().name + "'.");
      }
    }

    throw new RuntimeError(expr.name, "Only instances have properties.");
  }

  @Override
  public Void visitFunctionStmt(Stmt.Function stmt) {
    LoxFunction function = new LoxFunction(stmt.name, stmt.params, stmt.body, environment, false, stmt.visibility);
    environment = environment.define(stmt.name.lexeme, function);
    return null;
  }

  @Override
  public Void visitFunctionParameter(Stmt.FunctionParameter stmt) {
    return null;
  }

  @Override
  public Object visitFunctionExpr(Expr.Function expr) {
    return new LoxFunction(expr.name, expr.params, expr.body, environment, false, Visibility.UNSPECIFIED);
  }

  @Override
  public Object visitUnaryExpr(Expr.Unary expr) {
    Object right = evaluate(expr.right);

    switch (expr.operator.type) {
      case BANG:
        return !isTruthy(right);
      case MINUS:
        checkNumberOperand(expr.operator, right);
        return -(double) right;
    }

    // Unreachable.
    return null;
  }

  @Override
  public Object visitVariableExpr(Expr.Variable expr) {
    return environment.get(expr.name);
  }

  @Override
  public Void visitBlockStmt(Stmt.Block stmt) {
    executeBlock(stmt.statements, new Environment(environment));
    return null;
  }

  @Override
  public Void visitClassStmt(Stmt.Class stmt) {
    environment = environment.define(stmt.name.lexeme, null);

    environment = new Environment(environment);

    Map<String, LoxField> fields = new LinkedHashMap<>();

    stmt.fields.forEach(field -> {
      fields.put(field.name.lexeme, new LoxField(field));
    });

    Map<String, LoxFunction> methods = new HashMap<>();

    stmt.methods.forEach(method -> {
      LoxFunction function = new LoxFunction(method.name, method.params, method.body, environment,
          method.name.lexeme.equals("init"), method.visibility);

      methods.put(method.name.lexeme, function);
    });

    LoxClass klass = new LoxClass(stmt.name.lexeme, fields, methods);

    environment = environment.enclosing;

    environment.assign(stmt.name, klass);
    return null;
  }

  private Object evaluate(Expr expr) {
    return expr.accept(this);
  }

  private void execute(Stmt stmt) {
    stmt.accept(this);
  }

  void executeBlock(List<Stmt> statements, Environment environment) {
    Environment previous = this.environment;
    try {
      this.environment = environment;

      for (Stmt statement : statements) {
        execute(statement);
      }
    } finally {
      this.environment = previous;
    }
  }

  Environment getEnvironment() {
    return environment;
  }

  Environment setEnvironment(Environment newEnv) {
    Environment old = environment;

    environment = newEnv;

    return old;
  }

  private void checkNumberOperand(Token operator, Object operand) {
    if (operand instanceof Double)
      return;
    throw new RuntimeError(operator, "Operand must be a number.");
  }

  private void checkNumberOperands(Token operator, Object left, Object right) {
    if (left instanceof Double && right instanceof Double)
      return;

    throw new RuntimeError(operator, "Operands must be numbers.");
  }

  private boolean isTruthy(Object object) {
    if (object == null)
      return false;
    if (object instanceof Boolean)
      return (boolean) object;
    return true;
  }

  private boolean isEqual(Object a, Object b) {
    // nil is only equal to nil.
    if (a == null && b == null)
      return true;
    if (a == null)
      return false;

    return a.equals(b);
  }

  private String stringify(Object object) {
    if (object == null)
      return "nil";

    // Hack. Work around Java adding ".0" to integer-valued doubles.
    if (object instanceof Double) {
      String text = object.toString();
      if (text.endsWith(".0")) {
        text = text.substring(0, text.length() - 2);
      }
      return text;
    }

    return object.toString();
  }
}
