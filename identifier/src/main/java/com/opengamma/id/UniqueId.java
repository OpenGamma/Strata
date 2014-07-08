/**
 * Copyright (C) 2009 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.id;

import java.io.Serializable;
import java.util.regex.Pattern;

import org.joda.convert.FromString;
import org.joda.convert.ToString;

import com.google.common.collect.ComparisonChain;

/**
 * An immutable unique identifier for an item within the OpenGamma installation.
 * <p>
 * This identifier is used as a handle within the system to refer to an item uniquely.
 * All versions of the same object share an {@link ObjectId} with the
 * {@code UniqueId} referring to a single version.
 * <p>
 * Many external identifiers, represented by {@link ExternalId}, are not truly unique.
 * This {@code ObjectId} and {@code UniqueId} are unique within the OpenGamma instance.
 * <p>
 * The unique identifier is formed from three parts, the scheme, value and version.
 * The scheme defines a single way of identifying items, while the value is an identifier
 * within that scheme. A value from one scheme may refer to a completely different
 * real-world item than the same value from a different scheme.
 * The version allows the object being identifier to change over time.
 * The version may be empty.
 * <p>
 * Real-world examples of {@code UniqueId} include instances of:
 * <ul>
 * <li>Database key - DbSec~123456~1</li>
 * <li>In memory key - MemSec~123456~234</li>
 * </ul>
 * <p>
 * This class is immutable and thread-safe.
 */
public final class UniqueId
    implements Comparable<UniqueId>, UniqueIdentifiable, ObjectIdentifiable, Serializable {

  /** Serialization version. */
  private static final long serialVersionUID = 1L;
  /**
   * The valid regex for schemes.
   * One letter, followed by zero-to-many letters, numbers or selected special characters.
   */
  static final Pattern REGEX_SCHEME = Pattern.compile("[A-Za-z][A-Za-z0-9+.=_-]*");
  /**
   * The valid regex for values.
   * One-to-many letters, numbers or selected special characters.
   */
  static final Pattern REGEX_VALUE = Pattern.compile("[A-Za-z0-9+.=_-]+");
  /**
   * The valid regex for versions.
   * One-to-many letters, numbers or selected special characters.
   */
  static final Pattern REGEX_VERSION = Pattern.compile("[A-Za-z0-9+.=_-]*");

  /**
   * The scheme that categorizes the identifier value.
   */
  private final String scheme;
  /**
   * The identifier value within the scheme.
   */
  private final String value;
  /**
   * The version of the identifier.
   */
  private final String version;

  //-------------------------------------------------------------------------
  /**
   * Obtains a {@code UniqueId} from a scheme, value and version.
   * <p>
   * The scheme must be non-empty and match the regular expression '{@code [A-Za-z][A-Za-z0-9+.=_-]*}'.
   * <p>
   * The value must be non-empty and match the regular expression '{@code [A-Za-z0-9+.=_-]+}'.
   * <p>
   * The version must match the regular expression '{@code [A-Za-z0-9+.=_-]*}'.
   * 
   * @param scheme  the scheme of the identifier, not empty
   * @param value  the value of the identifier, not empty
   * @param version  the version of the identifier, may be empty
   * @return the unique identifier
   */
  public static UniqueId of(String scheme, String value, String version) {
    return new UniqueId(scheme, value, version);
  }

  /**
   * Obtains a {@code UniqueId} from an {@code ObjectId} and a version.
   * 
   * @param objectId  the object identifier
   * @param version  the version of the identifier, may be empty
   * @return the unique identifier
   */
  public static UniqueId of(ObjectId objectId, String version) {
    ArgChecker.notNull(objectId, "objectId");
    return new UniqueId(objectId.getScheme(), objectId.getValue(), version);
  }

  /**
   * Parses a {@code UniqueId} from a formatted scheme and value.
   * <p>
   * This parses the identifier from the form produced by {@code toString()}
   * which is '{@code $scheme~$value~$version}'.
   * 
   * @param str  the unique identifier to parse
   * @return the unique identifier
   * @throws IllegalArgumentException if the identifier cannot be parsed
   */
  @FromString
  public static UniqueId parse(String str) {
    ArgChecker.notNull(str, "str");
    int pos1 = str.indexOf("~");
    if (pos1 < 0) {
      throw new IllegalArgumentException("Invalid identifier format: " + str);
    }
    int pos2 = str.indexOf("~", pos1 + 1);
    if (pos2 < 0) {
      throw new IllegalArgumentException("Invalid identifier format: " + str);
    }
    int pos3 = str.indexOf("~", pos2 + 1);
    if (pos3 >= 0) {
      throw new IllegalArgumentException("Invalid identifier format: " + str);
    }
    String scheme = str.substring(0, pos1);
    String value = str.substring(pos1 + 1, pos2);
    String version = str.substring(pos2 + 1);
    return UniqueId.of(scheme, value, version);
  }

  /**
   * Creates a unique instance.
   * 
   * @param scheme  the scheme of the identifier, not empty
   * @param value  the value of the identifier, not empty
   * @param version  the version of the identifier, may be empty
   */
  private UniqueId(String scheme, String value, String version) {
    this.scheme = ArgChecker.matches(REGEX_SCHEME, scheme, "scheme");
    this.value = ArgChecker.matches(REGEX_VALUE, value, "value");
    this.version = ArgChecker.matches(REGEX_VERSION, version, "version");
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

  /**
   * Gets the version of the identifier.
   * <p>
   * This is the individual version of the entity.
   * 
   * @return the version, may be empty
   */
  public String getVersion() {
    return version;
  }

  //-------------------------------------------------------------------------
  /**
   * Returns a copy of this identifier with the specified scheme.
   * 
   * @param scheme  the new scheme of the identifier, not empty
   * @return an {@link ObjectId} based on this identifier with the specified scheme
   */
  public UniqueId withScheme(String scheme) {
    if (this.scheme.equals(scheme)) {
      return this;
    }
    return UniqueId.of(scheme, value, version);
  }

  /**
   * Returns a copy of this identifier with the specified value.
   * 
   * @param value  the new value of the identifier, not empty
   * @return an {@link ObjectId} based on this identifier with the specified value
   */
  public UniqueId withValue(String value) {
    if (this.value.equals(value)) {
      return this;
    }
    return UniqueId.of(scheme, value, version);
  }

  /**
   * Returns a copy of this identifier with the specified version.
   * 
   * @param version  the new version of the identifier, may be empty
   * @return the created identifier with the specified version
   */
  public UniqueId withVersion(String version) {
    if (this.version.equals(version)) {
      return this;
    }
    return new UniqueId(scheme, value, version);
  }

  //-------------------------------------------------------------------------
  /**
   * Gets the object identifier.
   * <p>
   * All versions of the same object share the same object identifier.
   * 
   * @return the scheme, not empty
   */
  @Override
  public ObjectId getObjectId() {
    return ObjectId.of(scheme, value);
  }

  /**
   * Gets the unique identifier.
   * <p>
   * This method trivially returns {@code this}.
   * 
   * @return {@code this}
   */
  @Override
  public UniqueId getUniqueId() {
    return this;
  }

  //-------------------------------------------------------------------------
  /**
   * Compares this identifier to another based on the object identifier, ignoring the version.
   * <p>
   * This checks to see if two unique identifiers represent the same underlying object.
   * 
   * @param other  the other identifier, null returns false
   * @return true if the object identifier are equal, ignoring the version
   */
  public boolean equalObjectId(ObjectIdentifiable other) {
    if (other == null) {
      return false;
    }
    ObjectId objectId = other.getObjectId();
    return scheme.equals(objectId.getScheme()) &&
        value.equals(objectId.getValue());
  }

  //-------------------------------------------------------------------------
  /**
   * Compares the unique identifiers.
   * <p>
   * This sorts alphabetically by scheme, then value, then version.
   * 
   * @param other  the other unique identifier
   * @return negative if this is less, zero if equal, positive if greater
   */
  @Override
  public int compareTo(UniqueId other) {
    return ComparisonChain.start()
        .compare(scheme, other.scheme)
        .compare(value, other.value)
        .compare(version, other.version)
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
    if (obj instanceof UniqueId) {
      UniqueId other = (UniqueId) obj;
      return scheme.equals(other.scheme) &&
          value.equals(other.value) &&
          version.equals(other.version);
    }
    return false;
  }

  /**
   * Returns a suitable hash code.
   * 
   * @return the hash code
   */
  @Override
  public int hashCode() {
    return scheme.hashCode() ^ value.hashCode() ^ version.hashCode();
  }

  /**
   * Returns the identifier in the form '{@code $scheme~$value~$version}'.
   * 
   * @return a parsable representation of the identifier
   */
  @Override
  @ToString
  public String toString() {
    int len = scheme.length() + value.length() + version.length() + 2;
    return new StringBuilder(len).append(scheme).append('~').append(value).append('~').append(version).toString();
  }

}
