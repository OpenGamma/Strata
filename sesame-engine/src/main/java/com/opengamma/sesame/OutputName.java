/**
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.sesame;

import java.io.Serializable;

import org.joda.convert.FromString;
import org.joda.convert.ToString;

import com.opengamma.util.ArgumentChecker;

/**
 * The name of an output.
 */
public final class OutputName implements Comparable<OutputName>, Serializable {

  /** Serialization version. */
  private static final long serialVersionUID = 1L;

  /**
   * The name.
   */
  private final String _name;

  /**
   * Obtains a {@code OutputName}.
   * 
   * @param name  the convention name, not null
   * @return the convention type, not null
   */
  @FromString
  public static OutputName of(String name) {
    return new OutputName(name);
  }

  //-------------------------------------------------------------------------
  /**
   * Creates an instance.
   * 
   * @param name  the convention name, not null
   */
  private OutputName(String name) {
    _name = name;
  }

  //-------------------------------------------------------------------------
  /**
   * Gets the name of the convention.
   * 
   * @return the convention name, not null
   */
  public String getName() {
    ArgumentChecker.notNull(_name, "name");
    return _name;
  }

  //-------------------------------------------------------------------------
  /**
   * Compares this type to another.
   * 
   * @param other  the object to compare to, not null
   * @return the comparison
   */
  @Override
  public int compareTo(OutputName other) {
    return getName().compareTo(other.getName());
  }

  /**
   * Checks if this type equals another.
   * <p>
   * Types are compared based on the name.
   * 
   * @param obj  the object to compare to, null returns false
   * @return true if equal
   */
  @Override
  public boolean equals(Object obj) {
    if (obj == this) {
      return true;
    }
    if (obj instanceof OutputName) {
      OutputName other = (OutputName) obj;
      return _name.equals(other.getName());
    }
    return false;
  }

  /**
   * Returns a suitable hash code.
   * 
   * @return a suitable hash code
   */
  @Override
  public int hashCode() {
    return _name.hashCode() + 31;
  }

  /**
   * Returns the name.
   * 
   * @return the string form, not null
   */
  @Override
  @ToString
  public String toString() {
    return _name;
  }

}
