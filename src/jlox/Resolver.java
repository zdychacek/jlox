package jlox;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Stack;

class Resolver implements Expr.Visitor<Void>, Stmt.Visitor<Void> {
  private enum FunctionType {
    NONE, FUNCTION
  }

  private final Interpreter interpreter;
  private final Stack<Map<String, Boolean>> scopes = new Stack<>();
  private FunctionType currentFunction = FunctionType.NONE;
  private boolean isInsideLoop = false;

  Resolver(Interpreter interpreter) {
    this.interpreter = interpreter;
  }

  void resolve(List<Stmt> statements) {
    for (Stmt statement : statements) {
      resolve(statement);
    }
  }

  private void _resolveFunction(List<Token> params, List<Stmt> body) {
    beginScope();
    for (Token param : params) {
      declare(param);
      define(param);
    }
    resolve(body);
    endScope();
  }

  private void resolveFunction(Stmt.Function function, FunctionType type) {
    FunctionType enclosingFunction = currentFunction;
    currentFunction = type;

    _resolveFunction(function.params, function.body);

    currentFunction = enclosingFunction;
  }

  private void resolveFunction(Expr.Function function) {
    FunctionType enclosingFunction = currentFunction;
    currentFunction = FunctionType.FUNCTION;

    _resolveFunction(function.params, function.body);

    currentFunction = enclosingFunction;
  }

  @Override
  public Void visitBlockStmt(Stmt.Block stmt) {
    beginScope();
    resolve(stmt.statements);
    endScope();
    return null;
  }

  @Override
  public Void visitExpressionStmt(Stmt.Expression stmt) {
    resolve(stmt.expression);
    return null;
  }

  @Override
  public Void visitFunctionStmt(Stmt.Function stmt) {
    declare(stmt.name);
    define(stmt.name);

    resolveFunction(stmt, FunctionType.FUNCTION);
    return null;
  }

  @Override
  public Void visitIfStmt(Stmt.If stmt) {
    resolve(stmt.condition);
    resolve(stmt.thenBranch);
    if (stmt.elseBranch != null)
      resolve(stmt.elseBranch);
    return null;
  }

  @Override
  public Void visitWhileStmt(Stmt.While stmt) {
    boolean originalIsInLoop = isInsideLoop;
    isInsideLoop = true;

    resolve(stmt.condition);
    resolve(stmt.body);

    isInsideLoop = originalIsInLoop;

    return null;
  }

  @Override
  public Void visitReturnStmt(Stmt.Return stmt) {
    if (currentFunction == FunctionType.NONE) {
      Lox.error(stmt.keyword, "Cannot return from top-level code.");
    }

    if (stmt.value != null) {
      resolve(stmt.value);
    }

    return null;
  }

  @Override
  public Void visitVarStmt(Stmt.Var stmt) {
    declare(stmt.name);
    if (stmt.initializer != null) {
      resolve(stmt.initializer);
    }
    define(stmt.name);
    return null;
  }

  @Override
  public Void visitBreakStmt(Stmt.Break stmt) {
    if (!isInsideLoop) {
      Lox.error(stmt.keyword, "Break statement can be used only inside loops.");
    }

    return null;
  }

  @Override
  public Void visitContinueStmt(Stmt.Continue stmt) {
    if (!isInsideLoop) {
      Lox.error(stmt.keyword, "Continue statement can be used only inside loops.");
    }

    return null;
  }

  @Override
  public Void visitAssignExpr(Expr.Assign expr) {
    resolve(expr.value);
    resolveLocal(expr, expr.name);
    return null;
  }

  @Override
  public Void visitBinaryExpr(Expr.Binary expr) {
    resolve(expr.left);
    resolve(expr.right);
    return null;
  }

  @Override
  public Void visitCallExpr(Expr.Call expr) {
    resolve(expr.callee);

    for (Expr argument : expr.arguments) {
      resolve(argument);
    }

    return null;
  }

  @Override
  public Void visitGroupingExpr(Expr.Grouping expr) {
    resolve(expr.expression);
    return null;
  }

  @Override
  public Void visitLiteralExpr(Expr.Literal expr) {
    return null;
  }

  @Override
  public Void visitLogicalExpr(Expr.Logical expr) {
    resolve(expr.left);
    resolve(expr.right);
    return null;
  }

  @Override
  public Void visitUnaryExpr(Expr.Unary expr) {
    resolve(expr.right);
    return null;
  }

  @Override
  public Void visitVariableExpr(Expr.Variable expr) {
    if (!scopes.isEmpty() && scopes.peek().get(expr.name.lexeme) == false) {
      Lox.error(expr.name, "Cannot read local variable in its own initializer.");
    }

    resolveLocal(expr, expr.name);
    return null;
  }

  @Override
  public Void visitFunctionExpr(Expr.Function expr) {
    if (expr.name != null) {
      declare(expr.name);
      define(expr.name);
    }

    resolveFunction(expr);
    return null;
  }

  private void resolve(Stmt stmt) {
    stmt.accept(this);
  }

  private void resolve(Expr expr) {
    expr.accept(this);
  }

  private void beginScope() {
    scopes.push(new HashMap<String, Boolean>());
  }

  private void endScope() {
    scopes.pop();
  }

  private void declare(Token name) {
    if (scopes.isEmpty())
      return;

    Map<String, Boolean> scope = scopes.peek();

    if (scope.containsKey(name.lexeme)) {
      Lox.error(name, "Variable with this name already declared in this scope.");
    }

    scope.put(name.lexeme, false);
  }

  private void define(Token name) {
    if (scopes.isEmpty())
      return;
    scopes.peek().put(name.lexeme, true);
  }

  private void resolveLocal(Expr expr, Token name) {
    for (int i = scopes.size() - 1; i >= 0; i--) {
      if (scopes.get(i).containsKey(name.lexeme)) {
        interpreter.resolve(expr, scopes.size() - 1 - i);
        return;
      }
    }

    // Not found. Assume it is global.
  }
}
