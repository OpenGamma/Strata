/**
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.collect.type;

import java.io.Serializable;

import org.joda.convert.ToString;

import com.opengamma.collect.ArgChecker;

/**
 * An abstract class designed to enable typed strings.
 * <p>
 * The purpose of {@code TypedString} is to provide a Java type to a concept
 * that might otherwise be represented as a string.
 * It could be thought of as a way to provide a type alias for a string.
 * <p>
 * Subclasses must be written as follows:
 * <pre>
 *  public final class FooType
 *      extends TypedString&lt;FooType&gt; {
 *    private static final long serialVersionUID = 1L;
 *    @FromString
 *    public static FooType of(String name) {
 *      return new FooType(name);
 *    }
 *    private FooType(String name) {
 *      super(name);
 *    }
 *  }
 * </pre>
 * 
 * @param <T>  the implementation subclass of this class
 */
public abstract class TypedString<T extends TypedString<T>>
    implements Comparable<T>, Serializable {

  /** Serialization version. */
  private static final long serialVersionUID = 1L;

  /**
   * The name.
   */
  private final String name;

  /**
   * Creates an instance.
   * 
   * @param name  the name, not null
   */
  protected TypedString(String name) {
    this.name = ArgChecker.notNull(name, "name");
  }

  //-------------------------------------------------------------------------
  /**
   * Compares this type to another.
   * <p>
   * Instances are compared in alphabetical order based on the name.
   * 
   * @param other  the object to compare to, not null
   * @return the comparison
   */
  @Override
  public final int compareTo(T other) {
    return name.compareTo(other.toString());
  }

  /**
   * Checks if this type equals another.
   * <p>
   * Instances are compared based on the name.
   * 
   * @param obj  the object to compare to, null returns false
   * @return true if equal
   */
  @Override
  public final boolean equals(Object obj) {
    if (obj == this) {
      return true;
    }
    if (obj != null && obj.getClass() == getClass()) {
      TypedString<?> other = (TypedString<?>) obj;
      return name.equals(other.name);
    }
    return false;
  }

  /**
   * Returns a suitable hash code.
   * 
   * @return a suitable hash code
   */
  @Override
  public final int hashCode() {
    return name.hashCode() ^ getClass().hashCode();
  }

  /**
   * Returns the name.
   * 
   * @return the string form, not null
   */
  @Override
  @ToString
  public final String toString() {
    return name;
  }

}
