/*
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.market.curve;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.opengamma.strata.basics.currency.Currency;
import com.opengamma.strata.basics.index.Index;
import com.opengamma.strata.basics.index.RateIndex;
import com.opengamma.strata.collect.ArgChecker;

/**
 * A mutable builder for creating instances of {@code CurveGroupDefinition}.
 */
@SuppressWarnings("unchecked")
public final class RatesCurveGroupDefinitionBuilder {

  /**
   * The name of the curve group.
   */
  private CurveGroupName name;
  /**
   * The entries in the curve group.
   */
  private final Map<CurveName, RatesCurveGroupEntry> entries;
  /**
   * The definitions specifying how the curves are calibrated.
   */
  private final Map<CurveName, CurveDefinition> curveDefinitions;
  /**
   * The definitions specifying which seasonality should be used some some price index curves.
   */
  private final Map<CurveName, SeasonalityDefinition> seasonalityDefinitions;
  /**
   * Flag indicating if the Jacobian matrices should be computed and stored in metadata or not.
   * The default value is 'true'.
   */
  private boolean computeJacobian = true;
  /**
   * Flag indicating if present value sensitivity to market quotes should be computed and stored in metadata or not.
   * The default value is 'false'.
   */
  private boolean computePvSensitivityToMarketQuote;

  RatesCurveGroupDefinitionBuilder() {
    this.entries = new LinkedHashMap<>();
    this.curveDefinitions = new LinkedHashMap<>();
    this.seasonalityDefinitions = new LinkedHashMap<>();
  }

  RatesCurveGroupDefinitionBuilder(
      CurveGroupName name,
      Map<CurveName, RatesCurveGroupEntry> entries,
      Map<CurveName, CurveDefinition> curveDefinitions,
      Map<CurveName, SeasonalityDefinition> seasonalityDefinitions,
      boolean computeJacobian,
      boolean computePvSensitivityToMarketQuote) {
    this.name = name;
    this.entries = new LinkedHashMap<>(entries);
    this.curveDefinitions = new LinkedHashMap<>(curveDefinitions);
    this.seasonalityDefinitions = new LinkedHashMap<>(seasonalityDefinitions);
    this.computeJacobian = computeJacobian;
    this.computePvSensitivityToMarketQuote = computePvSensitivityToMarketQuote;
  }

  //-------------------------------------------------------------------------
  /**
   * Sets the name of the curve group definition.
   *
   * @param name  the name of the curve group, not empty
   * @return this builder
   */
  public RatesCurveGroupDefinitionBuilder name(CurveGroupName name) {
    this.name = ArgChecker.notNull(name, "name");
    return this;
  }

  /**
   * Sets the 'compute Jacobian' flag of the curve group definition.
   *
   * @param computeJacobian  the flag indicating if the Jacobian matrices should be
   *   computed and stored in metadata or not
   * @return this builder
   */
  public RatesCurveGroupDefinitionBuilder computeJacobian(boolean computeJacobian) {
    this.computeJacobian = computeJacobian;
    return this;
  }

  /**
   * Sets the 'compute PV sensitivity to market quote' flag of the curve group definition.
   * <p>
   * If set, the Jacobian matrices will also be calculated, even if not requested.
   *
   * @param computePvSensitivityToMarketQuote  the flag indicating if present value sensitivity
   *   to market quotes should be computed and stored in metadata or not
   * @return this builder
   */
  public RatesCurveGroupDefinitionBuilder computePvSensitivityToMarketQuote(boolean computePvSensitivityToMarketQuote) {
    this.computePvSensitivityToMarketQuote = computePvSensitivityToMarketQuote;
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
  public RatesCurveGroupDefinitionBuilder addDiscountCurve(
      CurveDefinition curveDefinition,
      Currency currency,
      Currency... otherCurrencies) {

    ArgChecker.notNull(curveDefinition, "curveDefinition");
    ArgChecker.notNull(currency, "currency");
    RatesCurveGroupEntry entry = RatesCurveGroupEntry.builder()
        .curveName(curveDefinition.getName())
        .discountCurrencies(ImmutableSet.copyOf(Lists.asList(currency, otherCurrencies)))
        .build();
    return merge(entry, curveDefinition);
  }

  /**
   * Adds the definition of a discount curve to the curve group definition.
   * <p>
   * A curve added with this method cannot be calibrated by the market data system as it does not include
   * a curve definition. It is intended to be used with curves which are supplied by the user.
   *
   * @param curveName  the name of the curve
   * @param otherCurrencies  additional currencies for which the curve can provide discount factors
   * @param currency  the currency for which the curve provides discount rates
   * @return this builder
   */
  public RatesCurveGroupDefinitionBuilder addDiscountCurve(
      CurveName curveName,
      Currency currency,
      Currency... otherCurrencies) {

    ArgChecker.notNull(curveName, "curveName");
    ArgChecker.notNull(currency, "currency");
    RatesCurveGroupEntry entry = RatesCurveGroupEntry.builder()
        .curveName(curveName)
        .discountCurrencies(ImmutableSet.copyOf(Lists.asList(currency, otherCurrencies)))
        .build();
    return mergeEntry(entry);
  }

  //-------------------------------------------------------------------------
  /**
   * Adds the definition of a forward curve to the curve group definition.
   *
   * @param curveDefinition  the definition of the forward curve
   * @param index  the index for which the curve provides forward rates
   * @param otherIndices  the additional indices for which the curve provides forward rates
   * @return this builder
   */
  public RatesCurveGroupDefinitionBuilder addForwardCurve(
      CurveDefinition curveDefinition,
      Index index,
      Index... otherIndices) {

    ArgChecker.notNull(curveDefinition, "curveDefinition");
    ArgChecker.notNull(index, "index");
    RatesCurveGroupEntry entry = RatesCurveGroupEntry.builder()
        .curveName(curveDefinition.getName())
        .indices(indices(index, otherIndices))
        .build();
    return merge(entry, curveDefinition);
  }

  /**
   * Adds the definition of a forward curve to the curve group definition.
   * <p>
   * A curve added with this method cannot be calibrated by the market data system as it does not include
   * a curve definition. It is intended to be used with curves which are supplied by the user.
   *
   * @param curveName  the name of the curve
   * @param index  the index for which the curve provides forward rates
   * @param otherIndices  the additional indices for which the curve provides forward rates
   * @return this builder
   */
  public RatesCurveGroupDefinitionBuilder addForwardCurve(
      CurveName curveName,
      Index index,
      Index... otherIndices) {

    ArgChecker.notNull(curveName, "curveName");
    ArgChecker.notNull(index, "index");

    RatesCurveGroupEntry entry = RatesCurveGroupEntry.builder()
        .curveName(curveName)
        .indices(indices(index, otherIndices))
        .build();
    return mergeEntry(entry);
  }

  //-------------------------------------------------------------------------
  /**
   * Adds the definition of a curve to the curve group definition which is used to provide
   * discount rates and forward rates.
   *
   * @param curveDefinition  the definition of the forward curve
   * @param currency  the currency for which the curve provides discount rates
   * @param index  the index for which the curve provides forward rates
   * @param otherIndices  the additional indices for which the curve provides forward rates
   * @return this builder
   */
  public RatesCurveGroupDefinitionBuilder addCurve(
      CurveDefinition curveDefinition,
      Currency currency,
      RateIndex index,
      RateIndex... otherIndices) {

    ArgChecker.notNull(curveDefinition, "curveDefinition");
    ArgChecker.notNull(currency, "currency");
    ArgChecker.notNull(index, "index");

    RatesCurveGroupEntry entry = RatesCurveGroupEntry.builder()
        .curveName(curveDefinition.getName())
        .discountCurrencies(ImmutableSet.of(currency))
        .indices(indices(index, otherIndices))
        .build();
    return merge(entry, curveDefinition);
  }

  /**
   * Adds a curve to the curve group definition which is used to provide discount rates and forward rates.
   * <p>
   * A curve added with this method cannot be calibrated by the market data system as it does not include
   * a curve definition. It is intended to be used with curves which are supplied by the user.
   *
   * @param curveName  the name of the curve
   * @param currency  the currency for which the curve provides discount rates
   * @param index  the index for which the curve provides forward rates
   * @param otherIndices  the additional indices for which the curve provides forward rates
   * @return this builder
   */
  public RatesCurveGroupDefinitionBuilder addCurve(
      CurveName curveName,
      Currency currency,
      RateIndex index,
      RateIndex... otherIndices) {

    RatesCurveGroupEntry entry = RatesCurveGroupEntry.builder()
        .curveName(curveName)
        .discountCurrencies(ImmutableSet.of(currency))
        .indices(indices(index, otherIndices))
        .build();
    return mergeEntry(entry);
  }

  /**
   * Adds a seasonality to the curve group definition.
   * 
   * @param curveName  the name of the curve
   * @param seasonalityDefinition  the seasonality associated to the curve
   * @return this builder
   */
  public RatesCurveGroupDefinitionBuilder addSeasonality(
      CurveName curveName,
      SeasonalityDefinition seasonalityDefinition) {

    seasonalityDefinitions.put(curveName, seasonalityDefinition);
    return this;
  }

  //-------------------------------------------------------------------------
  // merges the definition and entry
  private RatesCurveGroupDefinitionBuilder merge(RatesCurveGroupEntry newEntry, CurveDefinition curveDefinition) {
    curveDefinitions.put(curveDefinition.getName(), curveDefinition);
    return mergeEntry(newEntry);
  }

  // merges the specified entry with those already stored
  private RatesCurveGroupDefinitionBuilder mergeEntry(RatesCurveGroupEntry newEntry) {
    CurveName curveName = newEntry.getCurveName();
    RatesCurveGroupEntry existingEntry = entries.get(curveName);
    RatesCurveGroupEntry entry = existingEntry == null ? newEntry : existingEntry.merge(newEntry);
    entries.put(curveName, entry);
    return this;
  }

  /**
   * Returns a set containing any Ibor indices in the arguments.
   */
  private static Set<Index> indices(Index index, Index... otherIndices) {
    // The type parameter is needed for the benefit of the Eclipse compiler
    return ImmutableSet.<Index>builder().add(index).add(otherIndices).build();
  }

  //-------------------------------------------------------------------------
  /**
   * Builds the definition of the curve group from the data in this object.
   *
   * @return the definition of the curve group built from the data in this object
   */
  public RatesCurveGroupDefinition build() {
    // note that this defaults the jacobian flag based on the market quote flag
    return new RatesCurveGroupDefinition(
        name,
        entries.values(),
        curveDefinitions.values(),
        seasonalityDefinitions,
        computeJacobian || computePvSensitivityToMarketQuote,
        computePvSensitivityToMarketQuote);
  }

}
