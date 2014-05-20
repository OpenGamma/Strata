/**
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.sesame.engine;

import com.opengamma.sesame.cache.CacheInvalidator;
import com.opengamma.sesame.cache.NoOpCacheInvalidator;
import com.opengamma.sesame.graph.NodeDecorator;
import com.opengamma.util.ArgumentChecker;

/**
 * Caching manager that does not attempt to handle
 * cache invalidation. This is primarily useful for
 * testing purposes.
 */
public class NoOpCachingManager implements CachingManager {

  private final ComponentMap _components;

  /**
   * Constructor for the caching manager.
   *
   * @param components
   */
  public NoOpCachingManager(ComponentMap components) {
    _components = ArgumentChecker.notNull(components, "components");
  }

  @Override
  public ComponentMap getComponentMap() {
    return _components;
  }

  @Override
  public CacheInvalidator getCacheInvalidator() {
    return new NoOpCacheInvalidator();
  }

  @Override
  public NodeDecorator getCachingDecorator() {
    return NodeDecorator.IDENTITY;
  }
}
