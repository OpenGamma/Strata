/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.market.curve;

import static com.opengamma.strata.collect.Guavate.toImmutableSet;

import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;
import java.util.Set;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.opengamma.strata.basics.currency.Currency;
import com.opengamma.strata.basics.index.IborIndex;
import com.opengamma.strata.basics.index.OvernightIndex;
import com.opengamma.strata.basics.index.RateIndex;
import com.opengamma.strata.collect.ArgChecker;

/**
 * A mutable builder for creating instances of {@code CurveGroupDefinition}.
 */
@SuppressWarnings("unchecked")
public final class CurveGroupDefinitionBuilder {

  /**
   * The name of the curve group.
   */
  private CurveGroupName name;
  /**
   * The entries in the curve group.
   */
  private final List<CurveGroupEntry> entries = new ArrayList<>();

  //-------------------------------------------------------------------------
  /**
   * Sets the name of the curve group definition.
   *
   * @param name  the name of the curve group, not empty
   * @return this builder
   */
  public CurveGroupDefinitionBuilder name(CurveGroupName name) {
    this.name = ArgChecker.notNull(name, "name");
    return this;
  }

  //-------------------------------------------------------------------------
  /**
   * Adds the definition of a discount curve to the curve group definition.
   *
   * @param curveDefinition  the discount curve configuration
   * @param otherCurrencies  additional currencies for which the curve can provide discount factors
   * @param currency  the currency for which the curve provides discount rates
   * @return this builder
   */
  public CurveGroupDefinitionBuilder addDiscountCurve(
      NodalCurveDefinition curveDefinition,
      Currency currency,
      Currency... otherCurrencies) {

    ArgChecker.notNull(curveDefinition, "curveDefinition");
    ArgChecker.notNull(currency, "currency");
    CurveGroupEntry entry = CurveGroupEntry.builder()
        .curveDefinition(curveDefinition)
        .discountCurrencies(ImmutableSet.copyOf(Lists.asList(currency, otherCurrencies)))
        .build();
    return mergeEntry(entry);
  }

  /**
   * Adds the definition of a forward curve to the curve group definition.
   *
   * @param curveDefinition  the definition of the forward curve
   * @param index  the index for which the curve provides forward rates
   * @param otherIndices  the additional indices for which the curve provides forward rates
   * @return this builder
   */
  public CurveGroupDefinitionBuilder addForwardCurve(
      NodalCurveDefinition curveDefinition,
      RateIndex index,
      RateIndex... otherIndices) {

    ArgChecker.notNull(curveDefinition, "curveDefinition");
    ArgChecker.notNull(index, "index");
    CurveGroupEntry entry = CurveGroupEntry.builder()
        .curveDefinition(curveDefinition)
        .iborIndices(iborIndices(index, otherIndices))
        .overnightIndices(overnightIndices(index, otherIndices))
        .build();
    return mergeEntry(entry);
  }

  /**
   * Adds the definition of a curve to the curve group definition used to provide discount rates and forward rates.
   *
   * @param curveDefinition  the definition of the forward curve
   * @param currency  the currency for which the curve provides discount rates
   * @param index  the index for which the curve provides forward rates
   * @param otherIndices  the additional indices for which the curve provides forward rates
   * @return this builder
   */
  public CurveGroupDefinitionBuilder addCurve(
      NodalCurveDefinition curveDefinition,
      Currency currency,
      RateIndex index,
      RateIndex... otherIndices) {

    CurveGroupEntry entry = CurveGroupEntry.builder()
        .curveDefinition(curveDefinition)
        .discountCurrencies(ImmutableSet.of(currency))
        .iborIndices(iborIndices(index, otherIndices))
        .overnightIndices(overnightIndices(index, otherIndices))
        .build();
    return mergeEntry(entry);
  }

  // merges the specified entry with those already stored
  private CurveGroupDefinitionBuilder mergeEntry(CurveGroupEntry newEntry) {
    for (ListIterator<CurveGroupEntry> it = entries.listIterator(); it.hasNext();) {
      CurveGroupEntry entry = (CurveGroupEntry) it.next();
      if (entry.getCurveDefinition().equals(newEntry.getCurveDefinition())) {
        it.set(entry.merge(newEntry));
        return this;
      }
    }
    entries.add(newEntry);
    return this;
  }

  /**
   * Returns a set containing any Ibor indices in the arguments.
   */
  private static Set<IborIndex> iborIndices(RateIndex index, RateIndex... otherIndices) {
    return ImmutableList.<RateIndex>builder().add(index).add(otherIndices).build().stream()
        .filter(IborIndex.class::isInstance)
        .map(IborIndex.class::cast)
        .collect(toImmutableSet());
  }

  /**
   * Returns a set containing any overnight indices in the arguments.
   */
  private static Set<OvernightIndex> overnightIndices(RateIndex index, RateIndex... otherIndices) {
    return ImmutableList.<RateIndex>builder().add(index).add(otherIndices).build().stream()
        .filter(OvernightIndex.class::isInstance)
        .map(OvernightIndex.class::cast)
        .collect(toImmutableSet());
  }

  //-------------------------------------------------------------------------
  /**
   * Builds the definition of the curve group from the data in this object.
   *
   * @return the definition of the curve group built from the data in this object
   */
  public CurveGroupDefinition build() {
    return new CurveGroupDefinition(name, entries);
  }

}
