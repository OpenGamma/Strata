/**
 * Copyright (C) 2009 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.id;

import java.io.Serializable;

import org.joda.convert.FromString;
import org.joda.convert.ToString;

import com.google.common.collect.ComparisonChain;

/**
 * An immutable object identifier for an item within the OpenGamma instance.
 * <p>
 * This identifier is used as a handle within the system to refer to an item uniquely over time.
 * All versions of the same object share an object identifier.
 * A {@link UniqueId} refers to a single version of an object identifier.
 * <p>
 * Many external identifiers, represented by {@link ExternalId}, are not truly unique.
 * This {@code ObjectId} and {@code UniqueId} are unique within the OpenGamma instance.
 * <p>
 * The object identifier is formed from two parts, the scheme and value.
 * The scheme defines a single way of identifying items, while the value is an identifier
 * within that scheme. A value from one scheme may refer to a completely different
 * real-world item than the same value from a different scheme.
 * <p>
 * Real-world examples of {@code ObjectId} include instances of:
 * <ul>
 * <li>Database key - DbSec~123456</li>
 * <li>In memory key - MemSec~123456</li>
 * </ul>
 * <p>
 * This class is immutable and thread-safe.
 */
public final class ObjectId
    implements Comparable<ObjectId>, ObjectIdentifiable, Serializable {

  /** Serialization version. */
  private static final long serialVersionUID = 1L;

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
   * Obtains an {@code ObjectId} from a scheme and value.
   * <p>
   * The scheme must be non-empty and match the regular expression '{@code [A-Za-z][A-Za-z0-9+.=_-]*}'.
   * <p>
   * The value must be non-empty and match the regular expression '{@code [A-Za-z0-9+.=_-]+}'.
   * 
   * @param scheme  the scheme of the object identifier, not empty
   * @param value  the value of the object identifier, not empty
   * @return the object identifier
   */
  public static ObjectId of(String scheme, String value) {
    return new ObjectId(scheme, value);
  }

  /**
   * Parses an {@code ObjectId} from a formatted scheme and value.
   * <p>
   * This parses the identifier from the form produced by {@code toString()}
   * which is '{@code $scheme~$value}'.
   * 
   * @param str  the object identifier to parse
   * @return the object identifier
   * @throws IllegalArgumentException if the identifier cannot be parsed
   */
  @FromString
  public static ObjectId parse(String str) {
    ArgChecker.notNull(str, "str");
    int pos1 = str.indexOf("~");
    if (pos1 < 0) {
      throw new IllegalArgumentException("Invalid identifier format: " + str);
    }
    int pos2 = str.indexOf("~", pos1 + 1);
    if (pos2 >= 0) {
      throw new IllegalArgumentException("Invalid identifier format: " + str);
    }
    String scheme = str.substring(0, pos1);
    String value = str.substring(pos1 + 1);
    return ObjectId.of(scheme, value);
  }

  /**
   * Creates an object identifier.
   * 
   * @param scheme  the scheme of the identifier, not empty
   * @param value  the value of the identifier, not empty
   */
  private ObjectId(String scheme, String value) {
    this.scheme = ArgChecker.matches(UniqueId.REGEX_SCHEME, scheme, "scheme");
    this.value = ArgChecker.matches(UniqueId.REGEX_VALUE, value, "value");
  }

  //-------------------------------------------------------------------------
  /**
   * Gets the scheme of the identifier.
   * <p>
   * This defines the system that creates the identifier.
   * 
   * @return the scheme, not empty
   */
  public String getScheme() {
    return scheme;
  }

  /**
   * Gets the value of the identifier.
   * <p>
   * This is the identifier of the entity.
   * 
   * @return the value, not empty
   */
  public String getValue() {
    return value;
  }

  //-------------------------------------------------------------------------
  /**
   * Returns a copy of this identifier with the specified scheme.
   * 
   * @param scheme  the new scheme of the identifier, not empty
   * @return an {@link ObjectId} based on this identifier with the specified scheme
   */
  public ObjectId withScheme(String scheme) {
    if (this.scheme.equals(scheme)) {
      return this;
    }
    return ObjectId.of(scheme, value);
  }

  /**
   * Returns a copy of this identifier with the specified value.
   * 
   * @param value  the new value of the identifier, not empty
   * @return an {@link ObjectId} based on this identifier with the specified value
   */
  public ObjectId withValue(String value) {
    if (this.value.equals(value)) {
      return this;
    }
    return ObjectId.of(scheme, value);
  }

  //-------------------------------------------------------------------------
  /**
   * Creates a unique identifier with the specified version.
   * <p>
   * This creates a new unique identifier based on this object identifier using
   * the specified version.
   * 
   * @param version  the new version of the identifier, may be empty
   * @return a {@link UniqueId} based on this identifier at the specified version
   */
  public UniqueId atVersion(String version) {
    return UniqueId.of(scheme, value, version);
  }

  //-------------------------------------------------------------------------
  /**
   * Gets the object identifier.
   * <p>
   * This method trivially returns {@code this}.
   * 
   * @return {@code this}
   */
  @Override
  public ObjectId getObjectId() {
    return this;
  }

  //-------------------------------------------------------------------------
  /**
   * Compares the object identifiers.
   * <p>
   * This sorts alphabetically by scheme then value.
   * 
   * @param other  the other object identifier
   * @return negative if this is less, zero if equal, positive if greater
   */
  @Override
  public int compareTo(ObjectId other) {
    return ComparisonChain.start()
        .compare(scheme, other.scheme)
        .compare(value, other.value)
        .result();
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj instanceof ObjectId) {
      ObjectId other = (ObjectId) obj;
      return scheme.equals(other.scheme) &&
          value.equals(other.value);
    }
    return false;
  }

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
    int len = scheme.length() + value.length() + 1;
    return new StringBuilder(len).append(scheme).append('~').append(value).toString();
  }

}
