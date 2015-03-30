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

import com.google.common.collect.ComparisonChain;
import com.opengamma.collect.ArgChecker;

/**
 * An immutable external identifier for an item.
 * <p>
 * This identifier is used as a handle within the system to refer to an externally defined identifier.
 * By contrast, the {@code ObjectId} and {@code UniqueId} represent identifiers within an OpenGamma system.
 * <p>
 * The external identifier is formed from two parts, the scheme and value.
 * The scheme defines a single way of identifying items, while the value is an identifier
 * within that scheme. A value from one scheme may refer to a completely different
 * real-world item than the same value from a different scheme.
 * <p>
 * Real-world examples of {@code ExternalId} include instances of:
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
public final class ExternalId
    implements ExternalIdentifiable, Comparable<ExternalId>, Serializable {

  /** Serialization version. */
  private static final long serialVersionUID = 1L;
  /**
   * The valid regex for values.
   * One-to-many ASCII characters excluding square brackets, pipe and tilde.
   */
  static final Pattern REGEX_VALUE = Pattern.compile("[!-z][ -z]*");

  /**
   * The scheme that categorizes the identifier value.
   */
  private final ExternalScheme scheme;
  /**
   * The identifier value within the scheme.
   */
  private final String value;

  //-------------------------------------------------------------------------
  /**
   * Obtains an {@code ExternalId} from a scheme and value.
   * <p>
   * The value must be non-empty and match the regular expression '{@code [!-z][ -z]*}'.
   * 
   * @param scheme  the scheme of the external identifier
   * @param value  the value of the external identifier, not empty
   * @return the external identifier
   */
  public static ExternalId of(ExternalScheme scheme, String value) {
    return new ExternalId(scheme, value);
  }

  /**
   * Obtains an {@code ExternalId} from a scheme and value.
   * <p>
   * The scheme must be non-empty and match the regular expression '{@code [A-Za-z][A-Za-z0-9+.=_-]*}'.
   * <p>
   * The value must be non-empty and match the regular expression '{@code [!-z][ -z]*}'.
   * 
   * @param scheme  the scheme of the external identifier, not empty
   * @param value  the value of the external identifier, not empty
   * @return the external identifier
   */
  public static ExternalId of(String scheme, String value) {
    return new ExternalId(ExternalScheme.of(scheme), value);
  }

  /**
   * Parses an {@code ExternalId} from a formatted scheme and value.
   * <p>
   * This parses the identifier from the form produced by {@code toString()}
   * which is '{@code $scheme~$value}'.
   * 
   * @param str  the external identifier to parse
   * @return the external identifier
   * @throws IllegalArgumentException if the identifier cannot be parsed
   */
  @FromString
  public static ExternalId parse(String str) {
    ArgChecker.notNull(str, "str");
    int pos = str.indexOf("~");
    if (pos < 0) {
      throw new IllegalArgumentException("Invalid identifier format: " + str);
    }
    return new ExternalId(ExternalScheme.of(str.substring(0, pos)), str.substring(pos + 1));
  }

  /**
   * Creates an external identifier.
   * 
   * @param scheme  the scheme
   * @param value  the value of the identifier, not empty
   */
  private ExternalId(ExternalScheme scheme, String value) {
    this.scheme = ArgChecker.notNull(scheme, "scheme");
    this.value = ArgChecker.matches(REGEX_VALUE, value, "value");
  }

  //-------------------------------------------------------------------------
  /**
   * Gets the scheme of the identifier.
   * <p>
   * This provides the universe within which the identifier value has meaning.
   * 
   * @return the scheme,
   */
  public ExternalScheme getScheme() {
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
   * Checks if the scheme of this identifier equals the specified scheme.
   * 
   * @param scheme  the scheme to check for, null returns false
   * @return true if the schemes match
   */
  public boolean isScheme(ExternalScheme scheme) {
    return this.scheme.equals(scheme);
  }

  /**
   * Checks if the scheme of this identifier does not equal the specified scheme.
   * 
   * @param scheme  the scheme to check for, null returns true
   * @return true if the schemes are different
   */
  public boolean isNotScheme(ExternalScheme scheme) {
    return this.scheme.equals(scheme) == false;
  }

  //-------------------------------------------------------------------------
  /**
   * Gets the external identifier.
   * <p>
   * This method trivially returns {@code this}.
   * 
   * @return {@code this}
   */
  @Override
  public ExternalId getExternalId() {
    return this;
  }

  /**
   * Converts this identifier to a bundle.
   * 
   * @return a bundle wrapping this identifier
   */
  public ExternalIdBundle toBundle() {
    return ExternalIdBundle.of(this);
  }

  //-------------------------------------------------------------------------
  /**
   * Compares the external identifiers, sorting alphabetically by scheme followed by value.
   * 
   * @param other  the other external identifier
   * @return negative if this is less, zero if equal, positive if greater
   */
  @Override
  public int compareTo(ExternalId other) {
    return ComparisonChain.start()
        .compare(scheme, other.scheme)
        .compare(value, other.value)
        .result();
  }

  /**
   * Checks if this identifier equals another.
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
    ExternalId other = (ExternalId) obj;
    return scheme.equals(other.scheme) && value.equals(other.value);
  }

  /**
   * Returns a suitable hash code.
   * 
   * @return the hash code
   */
  @Override
  public int hashCode() {
    return scheme.hashCode() ^ value.hashCode();
  }

  /**
   * Returns the identifier in the form '{@code $scheme~$value}'.
   * 
   * @return a parsable representation of the identifier
   */
  @Override
  @ToString
  public String toString() {
    int len = scheme.getName().length() + value.length() + 1;
    return new StringBuilder(len).append(scheme).append('~').append(value).toString();
  }

}
