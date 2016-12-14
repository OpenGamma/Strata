/**
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.product.deposit.type;

import java.util.Locale;
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
    IborFixingDepositConvention value = BY_NAME.get(name);
    if (value == null) {
      IborFixingDepositConvention created = createByName(name);
      if (created != null) {
        String correctName = created.getName();
        value = BY_NAME.computeIfAbsent(correctName, k -> created);
        BY_NAME.putIfAbsent(correctName.toUpperCase(Locale.ENGLISH), value);
      }
    }
    return value;
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
