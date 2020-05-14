package jlox;

import java.util.List;

abstract class Stmt {
  interface IVisitor<R> {
    R visitBlockStmt(Block stmt);

    R visitClassStmt(Class stmt);

    R visitBreakStmt(Break stmt);

    R visitContinueStmt(Continue stmt);

    R visitExpressionStmt(Expression stmt);

    R visitFunctionStmt(Function stmt);

    R visitFunctionParameter(FunctionParameter stmt);

    R visitReturnStmt(Return stmt);

    R visitIfStmt(If stmt);

    R visitVarStmt(Var stmt);

    R visitWhileStmt(While stmt);
  }

  static class Block extends Stmt {
    Block(List<Stmt> statements) {
      this.statements = statements;
    }

    @Override
    <R> R accept(IVisitor<R> visitor) {
      return visitor.visitBlockStmt(this);
    }

    final List<Stmt> statements;
  }

  static class Class extends Stmt implements IDeclarator {
    Class(Token name, List<Stmt.Var> fields, List<Stmt.Function> methods) {
      this.name = name;
      this.fields = fields;
      this.methods = methods;
    }

    @Override
    <R> R accept(IVisitor<R> visitor) {
      return visitor.visitClassStmt(this);
    }

    final Token name;
    final List<Stmt.Var> fields;
    final List<Stmt.Function> methods;

    @Override
    public Token getName() {
      return name;
    }

    @Override
    public Visibility getVisibility() {
      return Visibility.UNSPECIFIED;
    }
  }

  static class Break extends Stmt {
    Break(Token keyword) {
      this.keyword = keyword;
    }

    @Override
    <R> R accept(IVisitor<R> visitor) {
      return visitor.visitBreakStmt(this);
    }

    final Token keyword;
  }

  static class Continue extends Stmt {
    Continue(Token keyword) {
      this.keyword = keyword;
    }

    @Override
    <R> R accept(IVisitor<R> visitor) {
      return visitor.visitContinueStmt(this);
    }

    final Token keyword;
  }

  static class Expression extends Stmt {
    Expression(Expr expression) {
      this.expression = expression;
    }

    @Override
    <R> R accept(IVisitor<R> visitor) {
      return visitor.visitExpressionStmt(this);
    }

    final Expr expression;
  }

  static class Function extends Stmt implements IDeclarator {
    Function(Token name, List<FunctionParameter> params, List<Stmt> body, Visibility visibility,
        boolean isClassMember) {
      this.name = name;
      this.params = params;
      this.body = body;
      this.visibility = visibility;
      this.isClassMember = isClassMember;
    }

    @Override
    <R> R accept(IVisitor<R> visitor) {
      return visitor.visitFunctionStmt(this);
    }

    final Token name;
    final List<FunctionParameter> params;
    final List<Stmt> body;
    final Visibility visibility;
    final boolean isClassMember;

    @Override
    public Token getName() {
      return name;
    }

    @Override
    public Visibility getVisibility() {
      return Visibility.UNSPECIFIED;
    }
  }

  static class Return extends Stmt {
    Return(Token keyword, Expr value) {
      this.keyword = keyword;
      this.value = value;
    }

    @Override
    <R> R accept(IVisitor<R> visitor) {
      return visitor.visitReturnStmt(this);
    }

    final Token keyword;
    final Expr value;
  }

  static class If extends Stmt {
    If(Expr condition, Stmt thenBranch, Stmt elseBranch) {
      this.condition = condition;
      this.thenBranch = thenBranch;
      this.elseBranch = elseBranch;
    }

    @Override
    <R> R accept(IVisitor<R> visitor) {
      return visitor.visitIfStmt(this);
    }

    final Expr condition;
    final Stmt thenBranch;
    final Stmt elseBranch;
  }

  static class Var extends Stmt implements IDeclarator {
    Var(Token name, Expr initializer, Visibility visibility, boolean isClassMember) {
      this.name = name;
      this.initializer = initializer;
      this.visibility = visibility;
      this.isClassMember = isClassMember;
    }

    @Override
    <R> R accept(IVisitor<R> visitor) {
      return visitor.visitVarStmt(this);
    }

    final Token name;
    final Expr initializer;
    final Visibility visibility;
    final boolean isClassMember;

    @Override
    public Token getName() {
      return name;
    }

    @Override
    public Visibility getVisibility() {
      return visibility;
    }
  }

  static class While extends Stmt {
    While(Expr condition, Stmt body) {
      this.condition = condition;
      this.body = body;
    }

    @Override
    <R> R accept(IVisitor<R> visitor) {
      return visitor.visitWhileStmt(this);
    }

    final Expr condition;
    final Stmt body;
  }

  static class FunctionParameter extends Stmt implements IDeclarator {
    FunctionParameter(Token name, Expr initializer) {
      this.name = name;
      this.initializer = initializer;
    }

    @Override
    <R> R accept(IVisitor<R> visitor) {
      return visitor.visitFunctionParameter(this);
    }

    final Token name;
    final Expr initializer;

    @Override
    public Token getName() {
      return name;
    }

    @Override
    public Visibility getVisibility() {
      return Visibility.UNSPECIFIED;
    }
  }

  abstract <R> R accept(IVisitor<R> visitor);
}
