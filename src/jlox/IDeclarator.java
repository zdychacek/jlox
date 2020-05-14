package jlox;

interface IDeclarator {
  Token getName();

  Visibility getVisibility();
}
