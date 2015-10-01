/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 * 
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.calibration;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSetMultimap;
import com.google.common.collect.SetMultimap;
import com.opengamma.strata.basics.currency.Currency;
import com.opengamma.strata.basics.index.Index;
import com.opengamma.strata.collect.ArgChecker;
import com.opengamma.strata.market.curve.Curve;
import com.opengamma.strata.market.curve.CurveName;
import com.opengamma.strata.market.curve.definition.CurveGroupDefinition;
import com.opengamma.strata.market.curve.definition.CurveGroupEntry;
import com.opengamma.strata.market.curve.definition.NodalCurveDefinition;
import com.opengamma.strata.pricer.rate.ImmutableRatesProvider;

/**
 * Generates a rates provider based on an existing provider.
 * <p>
 * This takes a base {@link ImmutableRatesProvider} and list of curve definitions
 * to generate a child provider.
 */
public class ImmutableRatesProviderGenerator
    implements RatesProviderGenerator {

  /**
   * The underlying known data.
   * This includes curves and FX matrix.
   */
  private final ImmutableRatesProvider knownProvider;
  /**
   * The curve templates for the new curves to be generated.
   */
  private final ImmutableList<NodalCurveDefinition> curveDefinitions;
  /**
   * The map between curve name and currencies for discounting.
   * The map should contains all the curve in the template list but may have more names
   * than the curve template list. Only the curves in the templates list are created.
   */
  private final ImmutableSetMultimap<CurveName, Currency> discountCurveNames;
  /**
   * The map between curve name and indices for forward.
   * The map should contains all the curve in the template list but may have more names
   * than the curve template list. Only the curves in the templates list are created
   */
  private final ImmutableSetMultimap<CurveName, Index> forwardCurveNames;

  /**
   * Obtains a generator from an existing provider and definition.
   * 
   * @param knownProvider  the underlying known provider
   * @param groupDefn  the curve group definition
   * @return the generator
   */
  public static ImmutableRatesProviderGenerator of(
      ImmutableRatesProvider knownProvider,
      CurveGroupDefinition groupDefn) {

    List<NodalCurveDefinition> curveDefns = new ArrayList<>();
    SetMultimap<CurveName, Currency> discountNames = HashMultimap.create();
    SetMultimap<CurveName, Index> indexNames = HashMultimap.create();
    for (CurveGroupEntry entry : groupDefn.getEntries()) {
      NodalCurveDefinition curveDefn = entry.getCurveDefinition();
      curveDefns.add(curveDefn);
      Set<Currency> ccy = entry.getDiscountCurrencies();
      discountNames.putAll(curveDefn.getName(), ccy);
      indexNames.putAll(curveDefn.getName(), entry.getIborIndices());
      indexNames.putAll(curveDefn.getName(), entry.getOvernightIndices());
    }
    return new ImmutableRatesProviderGenerator(knownProvider, curveDefns, discountNames, indexNames);
  }

  /**
   * Creates an instance.
   * 
   * @param knownProvider  the underlying known provider
   * @param curveDefinitions  the curve definitions
   * @param discountCurveNames  the map of discount curves
   * @param forwardCurveNames  the map of index forward curves
   */
  private ImmutableRatesProviderGenerator(
      ImmutableRatesProvider knownProvider,
      List<NodalCurveDefinition> curveDefinitions,
      SetMultimap<CurveName, Currency> discountCurveNames,
      SetMultimap<CurveName, Index> forwardCurveNames) {

    this.knownProvider = ArgChecker.notNull(knownProvider, "knownProvider");
    this.curveDefinitions = ImmutableList.copyOf(ArgChecker.notNull(curveDefinitions, "curveDefinitions"));
    this.discountCurveNames = ImmutableSetMultimap.copyOf(ArgChecker.notNull(discountCurveNames, "discountCurveNames"));
    this.forwardCurveNames = ImmutableSetMultimap.copyOf(ArgChecker.notNull(forwardCurveNames, "forwardCurveNames"));
  }

  //-------------------------------------------------------------------------
  @Override
  public ImmutableRatesProvider generate(double[] parameters) {
    // collect curves for child provider based on existing provider
    Map<Currency, Curve> discountCurves = new HashMap<>();
    Map<Index, Curve> indexCurves = new HashMap<>();
    discountCurves.putAll(knownProvider.getDiscountCurves());
    indexCurves.putAll(knownProvider.getIndexCurves());

    // generate curves from combined parameter array
    int nbCurves = curveDefinitions.size();
    int nbPreviousParams = 0;
    for (int i = 0; i < nbCurves; i++) {
      NodalCurveDefinition curveDefn = curveDefinitions.get(i);
      // extract parameters for the child curve
      int nbParams = curveDefn.getParameterCount();
      double[] curveParams = Arrays.copyOfRange(parameters, nbPreviousParams, nbPreviousParams + nbParams);
      nbPreviousParams += nbParams;
      // create the child curve
      Curve curve = curveDefn.curve(knownProvider.getValuationDate(), curveParams);
      // put child curve into maps
      Set<Currency> currencies = discountCurveNames.get(curveDefn.getName());
      for (Currency currency : currencies) {
        discountCurves.put(currency, curve);
      }
      Set<Index> indices = forwardCurveNames.get(curveDefn.getName());
      for (Index index : indices) {
        indexCurves.put(index, curve);
      }
    }

    // create child provider
    return knownProvider.toBuilder()
        .discountCurves(discountCurves)
        .indexCurves(indexCurves)
        .build();
  }

}
