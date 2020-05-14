package jlox;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Stack;

class Resolver implements Expr.IVisitor<Void>, Stmt.IVisitor<Void> {
  private enum FunctionType {
    NONE, FUNCTION, METHOD, INITIALIZER
  }

  private enum ClassType {
    NONE, CLASS
  }

  private enum VariableState {
    DECLARED, DEFINED
  }

  private class Declaration {
    IDeclarator declarator;
    List<Expr> refs;
    VariableState state;

    Declaration(VariableState state, IDeclarator declarator, Boolean isReferenced) {
      this.state = state;
      this.declarator = declarator;
      this.refs = new ArrayList<>();
    }

    Token getDeclaratorName() {
      return declarator.getName();
    }
  }

  private final Stack<Map<String, Declaration>> scopes = new Stack<>();
  private FunctionType currentFunction = FunctionType.NONE;
  private ClassType currentClass = ClassType.NONE;
  private boolean isInsideLoop = false;

  void resolve(List<Stmt> statements) {
    beginScope();
    for (Stmt statement : statements) {
      resolve(statement);
    }
    endScope();
  }

  private void _resolveFunction(List<Stmt.FunctionParameter> params, List<Stmt> body) {
    beginScope();
    for (Stmt.FunctionParameter param : params) {
      resolve(param);
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

    declare(stmt);
    define(stmt);

    beginScope();
    scopes.peek().put("this", new Declaration(VariableState.DEFINED, null, false));

    stmt.fields.forEach(field -> visitVarStmt(field));

    stmt.methods.forEach(method -> {
      declare(method);
      define(method);
    });

    stmt.methods.forEach(method -> {
      FunctionType declaration = FunctionType.METHOD;

      if (method.name.lexeme.equals("init")) {
        declaration = FunctionType.INITIALIZER;
      }

      resolveFunction(method, declaration);
    });

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
    declare(stmt);
    define(stmt);

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
    declare(stmt);
    if (stmt.initializer != null) {
      resolve(stmt.initializer);
    }
    define(stmt);
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
    resolveLocal(expr, expr.name);
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
    resolveFunction(expr);
    return null;
  }

  @Override
  public Void visitFunctionParameter(Stmt.FunctionParameter stmt) {
    declare(stmt);
    define(stmt);
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

      if (declaration.declarator instanceof Stmt.Function) {
        if (((Stmt.Function) declaration.declarator).isClassMember) {
          kind = "instance method";
        } else {
          kind = "function";
        }
      } else if (declaration.declarator instanceof Stmt.FunctionParameter) {
        kind = "parameter";
      } else if (declaration.declarator instanceof Stmt.Var) {
        if (((Stmt.Var) declaration.declarator).isClassMember) {
          kind = "instance field";
        } else {
          kind = "variable";
        }
      } else if (declaration.declarator instanceof Stmt.Class) {
        kind = "class";
      }

      Token declName = null;

      if (declaration.declarator != null) {
        declName = declaration.getDeclaratorName();
      }

      if (declName != null && declaration.refs.size() == 0) {
        // refactor: remove code redundancy
        if (declaration.declarator instanceof Stmt.Var) {
          Stmt.Var varDecl = (Stmt.Var) declaration.declarator;

          if (!(varDecl.isClassMember && varDecl.visibility == Visibility.PRIVATE)) {
            return;
          }
        } else if (declaration.declarator instanceof Stmt.Function) {
          Stmt.Function varDecl = (Stmt.Function) declaration.declarator;

          if (!(varDecl.isClassMember && varDecl.visibility == Visibility.PRIVATE)) {
            return;
          }
        }
        Lox.error(declName, "Unused " + kind + ".");
      }
    });
  }

  private void declare(IDeclarator decl) {
    if (scopes.isEmpty())
      return;

    Map<String, Declaration> scope = scopes.peek();

    if (scope.containsKey(decl.getName().lexeme)) {
      Lox.error(decl.getName(), "Variable with this name already declared in this scope.");
    }

    scope.put(decl.getName().lexeme, new Declaration(VariableState.DECLARED, decl, false));
  }

  private void define(IDeclarator decl) {
    if (scopes.isEmpty())
      return;

    scopes.peek().put(decl.getName().lexeme, new Declaration(VariableState.DEFINED, decl, false));
  }

  private void resolveLocal(Expr expr, Token name) {
    for (int i = scopes.size() - 1; i >= 0; i--) {
      if (scopes.get(i).containsKey(name.lexeme)) {
        scopes.get(i).get(name.lexeme).refs.add(expr);

        return;
      }
    }
  }
}
