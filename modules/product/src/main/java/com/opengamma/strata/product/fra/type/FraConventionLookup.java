/**
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.product.fra.type;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import com.opengamma.strata.basics.index.IborIndex;
import com.opengamma.strata.collect.named.NamedLookup;

/**
 * Allows conventions to be created dynamically from an index name.
 */
final class FraConventionLookup
    implements NamedLookup<FraConvention> {

  /**
   * The singleton instance of the lookup.
   */
  public static final FraConventionLookup INSTANCE = new FraConventionLookup();

  /**
   * The cache by name.
   */
  private static final ConcurrentMap<String, FraConvention> BY_NAME = new ConcurrentHashMap<>();

  /**
   * Restricted constructor.
   */
  private FraConventionLookup() {
  }

  //-------------------------------------------------------------------------
  @Override
  public FraConvention lookup(String name) {
    return BY_NAME.computeIfAbsent(name, FraConventionLookup::createByName);
  }

  @Override
  public Map<String, FraConvention> lookupAll() {
    return BY_NAME;
  }

  private static FraConvention createByName(String name) {
    return IborIndex.extendedEnum().find(name)
        .map(index -> ImmutableFraConvention.of(index))
        .orElse(null);
  }

}
