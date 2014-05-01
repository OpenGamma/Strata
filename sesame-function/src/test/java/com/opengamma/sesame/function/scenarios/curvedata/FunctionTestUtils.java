/**
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.sesame.function.scenarios.curvedata;

import java.util.concurrent.FutureTask;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.opengamma.sesame.cache.MethodInvocationKey;
import com.opengamma.sesame.engine.ViewFactory;

/**
 * Helper methods for tests in sesame-function
 */
public class FunctionTestUtils {

  private FunctionTestUtils() {
  }

  /**
   * @return a cache configured for use with the engine
   */
  public static Cache<MethodInvocationKey, FutureTask<Object>> createCache() {
    int concurrencyLevel = Runtime.getRuntime().availableProcessors() + 2;
    return CacheBuilder.newBuilder().maximumSize(ViewFactory.MAX_CACHE_ENTRIES).concurrencyLevel(concurrencyLevel).build();
  }}
