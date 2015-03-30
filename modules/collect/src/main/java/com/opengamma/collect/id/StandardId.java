/**
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.collect.id;

import java.io.Serializable;
import java.util.regex.Pattern;

import org.joda.convert.FromString;
import org.joda.convert.ToString;

import com.google.common.collect.ComparisonChain;
import com.opengamma.collect.ArgChecker;

/**
 * An immutable standard identifier for an item.
 * <p>
 * A standard identifier is used to uniquely identify domain objects.
 * It is formed from two parts, the scheme and value.
 * <p>
 * The scheme defines a single way of identifying items, while the value is an identifier
 * within that scheme. A value from one scheme may refer to a completely different
 * real-world item than the same value from a different scheme.
 * <p>
 * Real-world examples of {@code StandardId} include instances of:
 * <ul>
 *   <li>Cusip</li>
 *   <li>Isin</li>
 *   <li>Reuters RIC</li>
 *   <li>Bloomberg BUID</li>
 *   <li>Bloomberg Ticker</li>
 *   <li>Trading system OTC trade ID</li>
 * </ul>
 * <p>
 * This class is immutable and thread-safe.
 */
public final class StandardId
    implements StandardIdentifiable, Comparable<StandardId>, Serializable {

  /** Serialization version. */
  private static final long serialVersionUID = 1L;
  /**
   * The valid regex for schemes.
   * One letter, followed by zero-to-many letters, numbers or selected special characters.
   */
  private static final Pattern REGEX_SCHEME = Pattern.compile("[A-Za-z][A-Za-z0-9+.=_-]*");
  /**
   * The valid regex for values.
   * One-to-many ASCII characters excluding square brackets, pipe and tilde.
   */
  private static final Pattern REGEX_VALUE = Pattern.compile("[!-z][ -z]*");

  /**
   * The scheme that categorizes the identifier value.
   */
  private final String scheme;
  /**
   * The identifier value within the scheme.
   */
  private final String value;

  //-------------------------------------------------------------------------
  /**
   * Obtains a {@code StandardId} from a scheme and value.
   * <p>
   * The scheme must be non-empty and match the regular expression '{@code [A-Za-z][A-Za-z0-9+.=_-]*}'.
   * <p>
   * The value must be non-empty and match the regular expression '{@code [!-z][ -z]*}'.
   *
   * @param scheme  the scheme of the identifier, not empty
   * @param value  the value of the identifier, not empty
   * @return the identifier
   */
  public static StandardId of(String scheme, String value) {
    return new StandardId(scheme, value);
  }

  /**
   * Parses an {@code StandardId} from a formatted scheme and value.
   * <p>
   * This parses the identifier from the form produced by {@code toString()}
   * which is '{@code $scheme~$value}'.
   *
   * @param str  the identifier to parse
   * @return the identifier
   * @throws IllegalArgumentException if the identifier cannot be parsed
   */
  @FromString
  public static StandardId parse(String str) {
    int pos = ArgChecker.notNull(str, "str").indexOf("~");
    if (pos < 0) {
      throw new IllegalArgumentException("Invalid identifier format: " + str);
    }
    return new StandardId(str.substring(0, pos), str.substring(pos + 1));
  }

  //-------------------------------------------------------------------------
  /**
   * Creates an identifier.
   *
   * @param scheme  the scheme of the identifier, not empty
   * @param value  the value of the identifier, not empty
   */
  private StandardId(String scheme, String value) {
    this.scheme = ArgChecker.matches(REGEX_SCHEME, scheme, "scheme");
    this.value = ArgChecker.matches(REGEX_VALUE, value, "value");
  }

  //-------------------------------------------------------------------------
  /**
   * Gets the scheme of the identifier.
   * <p>
   * This provides the universe within which the identifier value has meaning.
   * 
   * @return the scheme, not empty
   */
  public String getScheme() {
    return scheme;
  }

  /**
   * Gets the value of the identifier.
   * 
   * @return the value, not empty
   */
  public String getValue() {
    return value;
  }

  //-------------------------------------------------------------------------
  /**
   * Gets the standard identifier, which simply returns {@code this}.
   *
   * @return {@code this}
   */
  @Override
  public StandardId getStandardId() {
    return this;
  }

  /**
   * Checks if the scheme of this identifier equals the specified scheme.
   * 
   * @param scheme  the scheme to check for, null returns false
   * @return true if the schemes match
   */
  public boolean isScheme(String scheme) {
    return this.scheme.equals(scheme);
  }

  /**
   * Checks if the scheme of this identifier does not equal the specified scheme.
   * 
   * @param scheme  the scheme to check for, null returns true
   * @return true if the schemes are different
   */
  public boolean isNotScheme(String scheme) {
    return !isScheme(scheme);
  }

  //-------------------------------------------------------------------------
  /**
   * Compares the external identifiers, sorting alphabetically by scheme followed by value.
   * 
   * @param other  the other external identifier
   * @return negative if this is less, zero if equal, positive if greater
   */
  @Override
  public int compareTo(StandardId other) {
    return ComparisonChain.start()
        .compare(scheme, other.scheme)
        .compare(value, other.value)
        .result();
  }

  /**
   * Checks if this identifier equals another, comparing the scheme and value.
   * 
   * @param obj  the other object
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
    StandardId other = (StandardId) obj;
    return scheme.equals(other.scheme) && value.equals(other.value);
  }

  /**
   * Returns a suitable hash code, based on the scheme and value.
   * 
   * @return the hash code
   */
  @Override
  public int hashCode() {
    return scheme.hashCode() ^ value.hashCode();
  }

  /**
   * Returns the identifier in a stahndard string format.
   * <p>
   * The returned string is in the form '{@code $scheme~$value}'.
   * This is suitable for use with {@link #parse(String)}.
   * 
   * @return a parsable representation of the identifier
   */
  @Override
  @ToString
  public String toString() {
    return scheme + "~" + value;
  }

}
