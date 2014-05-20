/**
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.sesame.engine;

import com.opengamma.sesame.cache.CacheInvalidator;
import com.opengamma.sesame.graph.NodeDecorator;

/**
 * Manages the set of cache-aware components, providing them
 * to Views as required.
 */
public interface CachingManager {

  /**
   * Return the component map. Note that the components in the map
   * may well be wrapped to allow them to interact with the cache
   * correctly.
   *
   * @return the component map to be used by Views
   */
  ComponentMap getComponentMap();

  /**
   * Return the cache invalidator which is used to mark cache
   * entries as invalid.
   *
   * @return the cache invalidator
   */
  CacheInvalidator getCacheInvalidator();

  /**
   * Return the decorator for engine nodes which ensures that
   * calls get memoized.
   *
   * @return the caching decorator
   */
  NodeDecorator getCachingDecorator();
}
