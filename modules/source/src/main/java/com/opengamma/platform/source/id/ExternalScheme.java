/**
 * Copyright (C) 2009 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.platform.source.id;

import java.io.Serializable;
import java.util.regex.Pattern;

import org.joda.convert.FromString;
import org.joda.convert.ToString;

import com.opengamma.strata.collect.ArgChecker;

/**
 * A classification scheme for external identifiers.
 * <p>
 * The scheme defines a universe of identifier values.
 * Each value only has meaning within that scheme, and the same value may have
 * a different meaning in a different scheme.
 * The scheme class is a type-safe wrapper on top of a string name.
 * <p>
 * This class is immutable and thread-safe.
 */
public final class ExternalScheme
    implements Serializable, Comparable<ExternalScheme> {

  /** Serialization version. */
  private static final long serialVersionUID = 1L;
  /**
   * The valid regex for schemes.
   * One letter, followed by zero-to-many letters, numbers or selected special characters.
   */
  private static final Pattern REGEX_SCHEME = Pattern.compile("[A-Za-z][A-Za-z0-9+.=_-]*");

  /**
   * The scheme name.
   */
  private final String name;

  //-------------------------------------------------------------------------
  /**
   * Obtains an {@code ExternalScheme} scheme using the specified name.
   * <p>
   * The name must be non-empty and match the regular expression '{@code [A-Za-z][A-Za-z0-9+.=_-]*}'.
   * 
   * @param name  the scheme name, not empty
   * @return the scheme
   * @throws IllegalArgumentException if the name is invalid
   */
  @FromString
  public static ExternalScheme of(String name) {
    return new ExternalScheme(name);
  }

  /**
   * Constructs a scheme using the specified name.
   * 
   * @param name  the scheme name, not empty
   */
  private ExternalScheme(String name) {
    this.name = ArgChecker.matches(REGEX_SCHEME, name, "name");
  }

  //-------------------------------------------------------------------------
  /**
   * Gets the scheme name.
   * 
   * @return the scheme name
   */
  public String getName() {
    return name;
  }

  //-------------------------------------------------------------------------
  /**
   * Compares this scheme to another sorting alphabetically.
   * 
   * @param other  the other scheme
   * @return negative if this is less, zero if equal, positive if greater
   */
  @Override
  public int compareTo(ExternalScheme other) {
    return name.compareTo(other.name);
  }

  /**
   * Checks if this scheme equals another.
   * 
   * @param obj  the other scheme
   * @return true if equal
   */
  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null || getClass() != obj.getClass()) {
      return false;
    }
    ExternalScheme other = (ExternalScheme) obj;
    return name.equals(other.name);
  }

  /**
   * Returns a suitable hash code.
   * 
   * @return the hash code
   */
  @Override
  public int hashCode() {
    return name.hashCode();
  }

  /**
   * Returns the name of the scheme.
   * 
   * @return the scheme name
   */
  @Override
  @ToString
  public String toString() {
    return name;
  }

}
