/**
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.sesame.marketdata;

import java.util.Collections;
import java.util.Set;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.SetMultimap;
import com.opengamma.util.ArgumentChecker;

/**
 * A cut down multimap that also provides an inverted version of itself. If
 * there are many to many mappings from types K -> V and V -> K, this provides
 * methods to lookup all Vs associated with a K. By using the {@link #inverse()}
 * method, it is then possible to lookup all Ks associated with a V.
 * <p>
 * Modifications made to this instance will be automatically reflected in its
 * inverse. Similarly, modifications made to the inverse will be reflected in
 * this instance.
 * <p>
 * This code is not thread-safe, so any synchronization required must be done
 * externally.
 *
 * @param <K> the type of the keys for the map
 * @param <V> the type of the values for the map, recommended to be
 * a different type from K to avoid confusion when calling methods
 */
public class BidirectionalMultiMap<K, V> {

  /**
   * The multimap holding mappings from K -> V.
   */
  private final SetMultimap<K, V> _primaryMap;

  /**
   * The multimap holding mappings from V -> K.
   */
  private final SetMultimap<V, K> _secondaryMap;

  /**
   * The inverted BidirectionalMultiMap which is a view over the same
   * underlying data.
   */
  private final BidirectionalMultiMap<V, K> _inverse;

  /**
   * Create the multimap.
   */
  public BidirectionalMultiMap() {
    this(HashMultimap.<K, V>create(), HashMultimap.<V, K>create());
  }

  /**
   * Private constructor taking the underlying multimaps. No arg checking as
   * only called from this class. The inverse will be generated from the
   * passed multimaps.
   *
   * @param primaryMap  the multimap holding mappings from K -> V
   * @param secondaryMap  the multimap holding mappings from V -> K
   */
  private BidirectionalMultiMap(SetMultimap<K, V> primaryMap, SetMultimap<V, K> secondaryMap) {
    _primaryMap = primaryMap;
    _secondaryMap = secondaryMap;
    // Ensure that this instance is used as the inverse of the inverse
    _inverse = new BidirectionalMultiMap<>(_secondaryMap, _primaryMap, this);
  }

  /**
   * Private constructor taking the underlying multimaps and the inverse.
   * No arg checking as only called from this class.
   *
   * @param primaryMap  the multimap holding mappings from K -> V
   * @param secondaryMap  the multimap holding mappings from V -> K
   * @param inverse  the inverse of this BidirectionalMultiMap
   */
  private BidirectionalMultiMap(SetMultimap<K, V> primaryMap, SetMultimap<V, K> secondaryMap,
                                BidirectionalMultiMap<V, K> inverse) {
    _primaryMap = primaryMap;
    _secondaryMap = secondaryMap;
    _inverse = inverse;
  }

  /**
   * Return the inverse of this BidirectionalMultiMap.
   *
   * @return the inverse, not null
   */
  public BidirectionalMultiMap<V, K> inverse() {
    return _inverse;
  }

  /**
   * Add a mapping from key to value into this multimap. If the mapping already
   * exists then this method will have no effect.
   *
   * @param key  they key the value is added against, not null
   * @param value  the value to add for the key, not null
   */
  public void put(K key, V value) {
    _primaryMap.put(ArgumentChecker.notNull(key, "key"), ArgumentChecker.notNull(value, "value"));
    _secondaryMap.put(value, key);
  }

  /**
   * Returns the collection of values associated with the specified
   * key in this multimap, if any. An empty collection will be
   * returned if there are no mappings. The returned collection does
   * not permit modifications.
   *
   * @param key  the key find mappings for
   * @return the collection of values associated with the key, not null
   */
  public Set<V> get(K key) {
    return Collections.unmodifiableSet(_primaryMap.get(key));
  }

  /**
   * Returns true if this multimap contains at least one key-value
   * pair with the specified key.
   *
   * @param key  the key to check for
   * @return true if this multimap contains at least one key-value
   * pair with the specified key
   */
  public boolean containsKey(K key) {
    return _primaryMap.containsKey(key);
  }

  /**
   * Returns all distinct keys contained in this multimap. The
   * returned set does not permit modifications.
   *
   * @return the set of distinct keys, not null
   */
  public Set<K> keySet() {
    return Collections.unmodifiableSet(_primaryMap.keySet());
  }

  /**
   * Remove an entry from this multimap.
   *
   * @param key  they key the value is removed from, not null
   * @param value  the value to remove for the key, not null
   */
  public void remove(K key, V value) {
    _primaryMap.remove(ArgumentChecker.notNull(key, "key"), ArgumentChecker.notNull(value, "value"));
    _secondaryMap.remove(value, key);
  }

  /**
   * Return the number of mappings in this multimap.
   *
   * @return the number of mappings
   */
  public int size() {
    return _primaryMap.size();
  }

  /**
   * Remove all the mappings for a key, returning the set
   * of values removed. At the same time, all values
   * referencing the key will have the key removed. The
   * returned set does not permit modifications.
   *
   * @param key  the key to remove
   * @return the set of values removed
   */
  public Set<V> removeAll(K key) {
    Set<V> removed = _primaryMap.removeAll(key);
    for (V value : removed) {
      _inverse.remove(value, key);
    }
    return Collections.unmodifiableSet(removed);
  }
}
