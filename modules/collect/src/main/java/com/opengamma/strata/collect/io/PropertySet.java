/*
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.collect.io;

import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;

import com.google.common.base.Joiner;
import com.google.common.base.MoreObjects;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableListMultimap;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Multimap;
import com.opengamma.strata.collect.ArgChecker;
import com.opengamma.strata.collect.MapStream;

/**
 * A map of key-value properties.
 * <p>
 * This class represents a map of key to value.
 * Multiple values may be associated with each key.
 * <p>
 * This class is generally created by reading an INI or properties file.
 * See {@link IniFile} and {@link PropertiesFile}.
 */
public final class PropertySet {
  // this class is common between IniFile and PropertiesFile

  /**
   * The empty instance.
   */
  private static final PropertySet EMPTY = new PropertySet(ImmutableListMultimap.of());

  /**
   * The key-value pairs.
   */
  private final ImmutableListMultimap<String, String> keyValueMap;

  //-------------------------------------------------------------------------
  /**
   * Obtains an empty property set.
   * <p>
   * The result contains no properties.
   *
   * @return an empty property set
   */
  public static PropertySet empty() {
    return EMPTY;
  }

  /**
   * Obtains an instance from a map.
   * <p>
   * The returned instance will have one value for each key.
   * 
   * @param keyValues  the key-values to create the instance with
   * @return the property set
   */
  public static PropertySet of(Map<String, String> keyValues) {
    ArgChecker.notNull(keyValues, "keyValues");
    ImmutableListMultimap.Builder<String, String> builder = ImmutableListMultimap.builder();
    for (Entry<String, String> entry : keyValues.entrySet()) {
      builder.put(entry);
    }
    return new PropertySet(builder.build());
  }

  /**
   * Obtains an instance from a map allowing for multiple values for each key.
   * <p>
   * The returned instance may have more than one value for each key.
   * 
   * @param keyValues  the key-values to create the instance with
   * @return the property set
   */
  public static PropertySet of(Multimap<String, String> keyValues) {
    ArgChecker.notNull(keyValues, "keyValues");
    return new PropertySet(ImmutableListMultimap.copyOf(keyValues));
  }

  //-------------------------------------------------------------------------
  /**
   * Restricted constructor.
   * 
   * @param keyValues  the key-value pairs
   */
  private PropertySet(ImmutableListMultimap<String, String> keyValues) {
    this.keyValueMap = keyValues;
  }

  //-------------------------------------------------------------------------
  /**
   * Returns the keys and values as a {@code MapStream}.
   * <p>
   * There may be multiple values for the same key.
   * The iteration order of the stream matches that of the input data.
   * 
   * @return the map stream
   */
  public MapStream<String, String> stream() {
    return MapStream.of(keyValueMap);
  }

  /**
   * Returns the set of keys of this property set.
   * <p>
   * The iteration order of the map matches that of the input data.
   * 
   * @return the set of keys
   */
  public ImmutableSet<String> keys() {
    return ImmutableSet.copyOf(keyValueMap.keySet());
  }

  /**
   * Returns the property set as a multimap.
   * <p>
   * The iteration order of the map matches that of the input data.
   * 
   * @return the key-value map
   */
  public ImmutableListMultimap<String, String> asMultimap() {
    return keyValueMap;
  }

  /**
   * Returns the property set as a map.
   * <p>
   * The iteration order of the map matches that of the input data.
   * <p>
   * If a key has multiple values, the values will be returned as a comma separated list.
   * 
   * @return the key-value map
   */
  public ImmutableMap<String, String> asMap() {
    ImmutableMap.Builder<String, String> builder = ImmutableMap.builder();
    for (String key : keys()) {
      builder.put(key, value(key));
    }
    return builder.build();
  }

  //-------------------------------------------------------------------------
  /**
   * Checks if this property set is empty.
   * 
   * @return true if the set is empty
   */
  public boolean isEmpty() {
    return keyValueMap.isEmpty();
  }

  /**
   * Checks if this property set contains the specified key.
   * 
   * @param key  the key name
   * @return true if the key exists
   */
  public boolean contains(String key) {
    ArgChecker.notNull(key, "key");
    return keyValueMap.containsKey(key);
  }

  /**
   * Gets a single value from this property set.
   * <p>
   * This returns the value associated with the specified key.
   * <p>
   * If a key has multiple values, the values will be returned as a comma separated list.
   * 
   * @param key  the key name
   * @return the value
   * @throws IllegalArgumentException if the key does not exist
   */
  public String value(String key) {
    ArgChecker.notNull(key, "key");
    ImmutableList<String> values = keyValueMap.get(key);
    if (values.size() == 0) {
      throw new IllegalArgumentException("Unknown key: " + key);
    }
    if (values.size() > 1) {
      return Joiner.on(',').join(values);
    }
    return values.get(0);
  }

  /**
   * Finds a single value in this property set.
   * <p>
   * This returns the value associated with the specified key, empty if not present.
   * <p>
   * If a key has multiple values, the values will be returned as a comma separated list.
   * 
   * @param key  the key name
   * @return the value, empty if not found
   * @throws IllegalArgumentException if more than one value is associated
   */
  public Optional<String> findValue(String key) {
    ArgChecker.notNull(key, "key");
    ImmutableList<String> values = keyValueMap.get(key);
    if (values.size() == 0) {
      return Optional.empty();
    }
    if (values.size() > 1) {
      return Optional.of(Joiner.on(',').join(values));
    }
    return Optional.of(values.get(0));
  }

  /**
   * Gets the list of values associated with the specified key.
   * <p>
   * A key-values instance may contain multiple values for each key.
   * This method returns that list of values.
   * The iteration order of the map matches that of the input data.
   * The returned list may be empty.
   * 
   * @param key  the key name
   * @return the list of values associated with the key
   */
  public ImmutableList<String> valueList(String key) {
    ArgChecker.notNull(key, "key");
    return MoreObjects.firstNonNull(keyValueMap.get(key), ImmutableList.<String>of());
  }

  //-------------------------------------------------------------------------
  /**
   * Combines this property set with another.
   * <p>
   * This property set takes precedence. Where a key exists in both sets the values in the other
   * property set will be discarded.
   * Any order of any additional keys will be retained, with those keys located after the base set of keys.
   *
   * @param other  the other property set
   * @return the combined property set
   */
  public PropertySet combinedWith(PropertySet other) {
    ArgChecker.notNull(other, "other");
    if (other.isEmpty()) {
      return this;
    }
    if (isEmpty()) {
      return other;
    }
    // cannot use ArrayListMultiMap as it does not retain the order of the keys
    // whereas ImmutableListMultimap does retain the order of the keys
    ImmutableListMultimap.Builder<String, String> map = ImmutableListMultimap.builder();
    map.putAll(this.keyValueMap);
    for (String key : other.keyValueMap.keySet()) {
      if (!this.contains(key)) {
        map.putAll(key, other.valueList(key));
      }
    }
    return new PropertySet(map.build());
  }

  //-------------------------------------------------------------------------
  /**
   * Overrides this property set with another.
   * <p>
   * The specified property set takes precedence.
   * The order of any existing keys will be retained, with the value replaced.
   * Any order of any additional keys will be retained, with those keys located after the base set of keys.
   * 
   * @param other  the other property set
   * @return the combined property set
   */
  public PropertySet overrideWith(PropertySet other) {
    ArgChecker.notNull(other, "other");
    if (other.isEmpty()) {
      return this;
    }
    if (isEmpty()) {
      return other;
    }
    // cannot use ArrayListMultiMap as it does not retain the order of the keys
    // whereas ImmutableListMultimap does retain the order of the keys
    ImmutableListMultimap.Builder<String, String> map = ImmutableListMultimap.builder();
    for (String key : this.keyValueMap.keySet()) {
      if (other.contains(key)) {
        map.putAll(key, other.valueList(key));
      } else {
        map.putAll(key, this.valueList(key));
      }
    }
    for (String key : other.keyValueMap.keySet()) {
      if (!this.contains(key)) {
        map.putAll(key, other.valueList(key));
      }
    }
    return new PropertySet(map.build());
  }

  //-------------------------------------------------------------------------
  /**
   * Checks if this property set equals another.
   * <p>
   * The comparison checks the content.
   * 
   * @param obj  the other section, null returns false
   * @return true if equal
   */
  @Override
  public boolean equals(Object obj) {
    if (obj == this) {
      return true;
    }
    if (obj instanceof PropertySet) {
      return keyValueMap.equals(((PropertySet) obj).keyValueMap);
    }
    return false;
  }

  /**
   * Returns a suitable hash code for the property set.
   * 
   * @return the hash code
   */
  @Override
  public int hashCode() {
    return keyValueMap.hashCode();
  }

  /**
   * Returns a string describing the property set.
   * 
   * @return the descriptive string
   */
  @Override
  public String toString() {
    return keyValueMap.toString();
  }

}
