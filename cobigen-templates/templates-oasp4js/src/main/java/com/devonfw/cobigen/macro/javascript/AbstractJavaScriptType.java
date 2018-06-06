package com.devonfw.cobigen.macro.javascript;

/**
 * Implementation of {@link JavaScriptType} for a complex type (TypeScript, ES2016+, etc.).
 */
public abstract class AbstractJavaScriptType implements JavaScriptType {

  @Override
  public boolean isPrimitive() {

    return false;
  }

  @Override
  public JavaScriptType getComponentType() {

    return null;
  }

  @Override
  public String toString() {

    return getSimpleName();
  }

}