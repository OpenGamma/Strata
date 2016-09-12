/**
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.product.deposit.type;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import com.opengamma.strata.basics.index.IborIndex;
import com.opengamma.strata.collect.named.NamedLookup;

/**
 * Allows conventions to be created dynamically from an index name.
 */
final class IborFixingDepositConventionLookup
    implements NamedLookup<IborFixingDepositConvention> {

  /**
   * The singleton instance of the lookup.
   */
  public static final IborFixingDepositConventionLookup INSTANCE = new IborFixingDepositConventionLookup();

  /**
   * The cache by name.
   */
  private static final ConcurrentMap<String, IborFixingDepositConvention> BY_NAME = new ConcurrentHashMap<>();

  /**
   * Restricted constructor.
   */
  private IborFixingDepositConventionLookup() {
  }

  //-------------------------------------------------------------------------
  @Override
  public IborFixingDepositConvention lookup(String name) {
    return BY_NAME.computeIfAbsent(name, IborFixingDepositConventionLookup::createByName);
  }

  @Override
  public Map<String, IborFixingDepositConvention> lookupAll() {
    return BY_NAME;
  }

  private static IborFixingDepositConvention createByName(String name) {
    return IborIndex.extendedEnum().find(name)
        .map(index -> ImmutableIborFixingDepositConvention.of(index))
        .orElse(null);
  }

}
