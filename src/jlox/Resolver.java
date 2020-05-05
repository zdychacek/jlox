package jlox;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Stack;

class Resolver implements Expr.Visitor<Void>, Stmt.Visitor<Void> {
  private enum FunctionType {
    NONE, FUNCTION, METHOD, INITIALIZER
  }

  private enum ClassType {
    NONE, CLASS
  }

  private enum VariableState {
    DECLARED, DEFINED
  }

  private enum DeclarationKind {
    FUNCTION, VARIABLE, PARAMETER, CLASS
  }

  private class Declaration {
    VariableState state;
    Token name;
    Boolean isReferenced;
    DeclarationKind kind;

    Declaration(VariableState state, Token name, Boolean isReferenced, DeclarationKind kind) {
      this.state = state;
      this.name = name;
      this.kind = kind;
      this.isReferenced = isReferenced;
    }
  }

  private final Interpreter interpreter;
  private final Stack<Map<String, Declaration>> scopes = new Stack<>();
  private FunctionType currentFunction = FunctionType.NONE;
  private ClassType currentClass = ClassType.NONE;
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
      declare(param, DeclarationKind.PARAMETER);
      define(param, DeclarationKind.PARAMETER);
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
  public Void visitClassStmt(Stmt.Class stmt) {
    ClassType enclosingClass = currentClass;
    currentClass = ClassType.CLASS;

    declare(stmt.name, DeclarationKind.CLASS);
    define(stmt.name, DeclarationKind.CLASS);

    beginScope();
    scopes.peek().put("this", new Declaration(VariableState.DEFINED, null, false, DeclarationKind.VARIABLE));

    for (Stmt.Function method : stmt.methods) {
      FunctionType declaration = FunctionType.METHOD;

      if (method.name.lexeme.equals("init")) {
        declaration = FunctionType.INITIALIZER;
      }

      resolveFunction(method, declaration);
    }

    endScope();

    currentClass = enclosingClass;
    return null;
  }

  @Override
  public Void visitExpressionStmt(Stmt.Expression stmt) {
    resolve(stmt.expression);
    return null;
  }

  @Override
  public Void visitFunctionStmt(Stmt.Function stmt) {
    declare(stmt.name, DeclarationKind.FUNCTION);
    define(stmt.name, DeclarationKind.FUNCTION);

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
      if (currentFunction == FunctionType.INITIALIZER) {
        Lox.error(stmt.keyword, "Cannot return a value from an initializer.");
      }

      resolve(stmt.value);
    }

    return null;
  }

  @Override
  public Void visitVarStmt(Stmt.Var stmt) {
    declare(stmt.name, DeclarationKind.VARIABLE);
    if (stmt.initializer != null) {
      resolve(stmt.initializer);
    }
    define(stmt.name, DeclarationKind.VARIABLE);
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
  public Void visitGetExpr(Expr.Get expr) {
    resolve(expr.object);
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
  public Void visitSetExpr(Expr.Set expr) {
    resolve(expr.value);
    resolve(expr.object);
    return null;
  }

  @Override
  public Void visitThisExpr(Expr.This expr) {
    if (currentClass == ClassType.NONE) {
      Lox.error(expr.keyword, "Cannot use 'this' outside of a class.");
      return null;
    }

    resolveLocal(expr, expr.keyword);
    return null;
  }

  @Override
  public Void visitUnaryExpr(Expr.Unary expr) {
    resolve(expr.right);
    return null;
  }

  @Override
  public Void visitVariableExpr(Expr.Variable expr) {
    if (!scopes.isEmpty() && scopes.peek().get(expr.name.lexeme) != null
        && scopes.peek().get(expr.name.lexeme).state == VariableState.DECLARED) {
      Lox.error(expr.name, "Cannot read local variable in its own initializer.");
    }

    resolveLocal(expr, expr.name);
    return null;
  }

  @Override
  public Void visitFunctionExpr(Expr.Function expr) {
    if (expr.name != null) {
      declare(expr.name, DeclarationKind.FUNCTION);
      define(expr.name, DeclarationKind.FUNCTION);
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
    scopes.push(new HashMap<String, Declaration>());
  }

  private void endScope() {
    Map<String, Declaration> scope = scopes.pop();

    scope.forEach((key, declaration) -> {
      String kind = "";

      switch (declaration.kind) {
        case FUNCTION:
          kind = "function";
          break;
        case PARAMETER:
          kind = "parameter";
          break;
        case VARIABLE:
          kind = "variable";
          break;
      }

      if (declaration.name != null && !declaration.isReferenced) {
        Lox.error(declaration.name, "Unused " + kind + ".");
      }
    });
  }

  private void declare(Token name, DeclarationKind kind) {
    if (scopes.isEmpty())
      return;

    Map<String, Declaration> scope = scopes.peek();

    if (scope.containsKey(name.lexeme)) {
      Lox.error(name, "Variable with this name already declared in this scope.");
    }

    scope.put(name.lexeme, new Declaration(VariableState.DECLARED, name, false, kind));
  }

  private void define(Token name, DeclarationKind kind) {
    if (scopes.isEmpty())
      return;

    scopes.peek().put(name.lexeme, new Declaration(VariableState.DEFINED, name, false, kind));
  }

  private void resolveLocal(Expr expr, Token name) {
    for (int i = scopes.size() - 1; i >= 0; i--) {
      if (scopes.get(i).containsKey(name.lexeme)) {
        scopes.get(i).get(name.lexeme).isReferenced = true;

        interpreter.resolve(expr, scopes.size() - 1 - i);
        return;
      }
    }

    // Not found. Assume it is global.
  }
}
