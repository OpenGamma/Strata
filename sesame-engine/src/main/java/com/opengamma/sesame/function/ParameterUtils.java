/**
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.sesame.function;

import java.lang.reflect.GenericArrayType;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import com.google.common.reflect.TypeToken;

import groovy.json.StringEscapeUtils;

/**
 * Helper methods for use when creating {@link ParameterType} instances and converting between arguments and strings.
 */
public final class ParameterUtils {

  private static final Pattern WHITESPACE = Pattern.compile("\\s");

  private ParameterUtils() {
  }

  /**
   * @param type a type
   * @return true if the type is a parameterized {@link Collection}, {@link List} or {@link Set}
   */
  public static boolean isCollection(Type type) {
    // we're only interested in parameterized types because we need to know the element type
    if (!(type instanceof ParameterizedType)) {
      return false;
    }
    Class<?> rawType = TypeToken.of(type).getRawType();
    return rawType == Collection.class || rawType == List.class || rawType == Set.class;
  }

  /**
   * @param type a type
   * @return true if the type is an array
   */
  public static boolean isArray(Type type) {
    return (type instanceof GenericArrayType) || ((type instanceof Class<?>) && ((Class<?>) type).isArray());
  }

  /**
   * @param type a type
   * @return true if they type is a parameterized map
   */
  public static boolean isMap(Type type) {
    // we're only interested in parameterized types because we need to know the key and value types
    if (!(type instanceof ParameterizedType)) {
      return false;
    }
    Class<?> rawType = TypeToken.of(type).getRawType();
    return rawType == Map.class;
  }

  /**
   * Returns the element type of an array or parameterized collection ({@link Collection}, {@link List} or {@link Set}).
   *
   * @param type the array or collection type
   * @return the type of the element in the collection or array
   * @throws IllegalArgumentException if the type isn't a collection or array or if the element can't be found (e.g.
   *   if the type doesn't have a type parameter)
   */
  public static Class<?> getElementType(Type type) {
    if (!isCollection(type) && !isArray(type)) {
      throw new IllegalArgumentException("Type must be a collection or array");
    }
    if (type instanceof GenericArrayType) {
      Type genericComponentType = ((GenericArrayType) type).getGenericComponentType();
      return TypeToken.of(genericComponentType).getRawType();
    }
    if ((type instanceof Class<?>) && ((Class<?>) type).isArray()) {
      return ((Class<?>) type).getComponentType();
    }
    if (type instanceof ParameterizedType) {
      ParameterizedType parameterizedType = (ParameterizedType) type;
      Type[] typeArguments = parameterizedType.getActualTypeArguments();

      // this shouldn't ever happen
      if (typeArguments.length != 1) {
        throw new IllegalArgumentException("Container must have one type argument " + type);
      }
      return TypeToken.of(typeArguments[0]).getRawType();
    }
    throw new IllegalArgumentException("Can't get element type for " + type);
  }

  /**
   * Returns the type of the keys in a generic map.
   *
   * @param type a map type
   * @return the type of the map's key
   * @throws IllegalArgumentException if the type isn't a map or the key type can't be found
   */
  public static Class<?> getKeyType(Type type) {
    if (!isMap(type)) {
      throw new IllegalArgumentException("Type isn't a map, can't get the key type. ");
    }
    ParameterizedType parameterizedType = (ParameterizedType) type;
    Type[] typeArguments = parameterizedType.getActualTypeArguments();

    if (typeArguments.length != 2) {
      throw new IllegalArgumentException("Expected 2 type arguments. ");
    }
    Type keyType = typeArguments[0];
    return TypeToken.of(keyType).getRawType();
  }

  /**
   * Returns the type of the values in a generic map.
   *
   * @param type a map type
   * @return the type of the map's key
   * @throws IllegalArgumentException if the type isn't a map or the value type can't be found
   */
  public static Class<?> getValueType(Type type) {
    if (!isMap(type)) {
      throw new IllegalArgumentException("Type isn't a map, can't get the value type. ");
    }
    ParameterizedType parameterizedType = (ParameterizedType) type;
    Type[] typeArguments = parameterizedType.getActualTypeArguments();

    if (typeArguments.length != 2) {
      throw new IllegalArgumentException("Expected 2 type arguments. ");
    }
    Type valueType = typeArguments[1];
    return TypeToken.of(valueType).getRawType();
  }

  /**
   * Escapes the string and wraps it in quotes if it contains any whitespace.
   *
   * @param str a string
   * @return the escaped string, surrounded with quotes if it contains any whitespace
   */
  public static String escapeString(String str) {
    if (WHITESPACE.matcher(str).find()) {
      return "\"" + StringEscapeUtils.escapeJava(str) + "\"";
    } else {
      return StringEscapeUtils.escapeJava(str);
    }
  }
}
