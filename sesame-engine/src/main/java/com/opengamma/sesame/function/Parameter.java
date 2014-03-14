/**
 * Copyright (C) 2013 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.sesame.function;

import java.lang.annotation.Annotation;
import java.util.Map;
import java.util.Objects;

import javax.annotation.Nullable;

import com.google.common.collect.ImmutableMap;
import com.opengamma.util.ArgumentChecker;

/**
 * Metadata for a method or constructor parameter.
 * TODO [SSM-108] use generic types for parameters, will allow for nicer error messages
 * TODO joda bean
 */
public final class Parameter {

  private final Class<?> _declaringClass;
  private final String _name;
  private final Class<?> _type;
  private final int _ordinal;
  private final ImmutableMap<Class<?>, Annotation> _annotations;

  public Parameter(Class<?> declaringClass, String name, Class<?> type, int ordinal, Map<Class<?>, Annotation> annotations) {
    _declaringClass = ArgumentChecker.notNull(declaringClass, "declaringClass");
    _name = ArgumentChecker.notEmpty(name, "name");
    _ordinal = ordinal;
    _type = ArgumentChecker.notNull(type, "type");
    _annotations = ImmutableMap.copyOf(ArgumentChecker.notNull(annotations, "annotations"));
  }

  public String getName() {
    return _name;
  }

  public int getOrdinal() {
    return _ordinal;
  }

  public Class<?> getType() {
    return _type;
  }

  public ImmutableMap<Class<?>, Annotation> getAnnotations() {
    return _annotations;
  }

  public boolean isNullable() {
    return _annotations.get(Nullable.class) != null;
  }

  public String getFullName() {
    return _declaringClass.getSimpleName() + "(" + _type.getSimpleName() + " " + _name + ")";
  }

  @Override
  public String toString() {
    return "Parameter [" +
        "_name='" + _name + "'" +
        ", _type=" + _type +
        ", _ordinal=" + _ordinal +
        ", _annotations=" + _annotations +
        "]";
  }

  @Override
  public int hashCode() {
    return Objects.hash(_declaringClass, _name, _type, _ordinal, _annotations);
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null || getClass() != obj.getClass()) {
      return false;
    }
    final Parameter other = (Parameter) obj;
    return
        Objects.equals(this._declaringClass, other._declaringClass) &&
        Objects.equals(this._name, other._name) &&
        Objects.equals(this._type, other._type) &&
        Objects.equals(this._ordinal, other._ordinal) &&
        Objects.equals(this._annotations, other._annotations);
  }
}
