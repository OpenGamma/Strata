/**
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.sesame.function;

import java.lang.reflect.Type;

import com.opengamma.util.ArgumentChecker;

/**
 * Type information for a {@link Parameter}.
 * This is a very simplified view of Java's generic types which handles arrays and generic collections and maps.
 * Just enough type information is preserved to know the element type of containers. Any type that isn't a
 * container is reduced to its raw type.
 */
public abstract class ParameterType {

  /**
   * The non-generic type of the parameter.
   * If the parameter is a regular type with no type parameters this returns the class. If the parameter has type
   * parameters this returns the raw type.
   *
   * @return the non-generic type of the parameter
   */
  public abstract Class<?> getType();

  /**
   * The name of the parameter type.
   * This closely matches the Java simple name, e.g. {@code List&lt;String&gt;}
   *
   * @return the name of the parameter type
   */
  public abstract String getName();

  // TODO humane name "list of strings" etc

  /**
   * Creates the parameter type for a generic type.
   *
   * @param type a generic type
   * @return the parameter type
   */
  public static ParameterType ofType(Type type) {
    ArgumentChecker.notNull(type, "type");

    if (ParameterUtils.isCollection(type)) {
      return new CollectionType(type);
    }
    if (ParameterUtils.isArray(type)) {
      return new ArrayType(type);
    }
    if (ParameterUtils.isMap(type)) {
      return new MapType(type);
    }
    return new SimpleType(type);
  }
}
