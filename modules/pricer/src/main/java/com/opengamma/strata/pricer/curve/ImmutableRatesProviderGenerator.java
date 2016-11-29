/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 * 
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.curve;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSetMultimap;
import com.google.common.collect.SetMultimap;
import com.opengamma.strata.basics.ReferenceData;
import com.opengamma.strata.basics.currency.Currency;
import com.opengamma.strata.basics.index.Index;
import com.opengamma.strata.collect.ArgChecker;
import com.opengamma.strata.collect.array.DoubleArray;
import com.opengamma.strata.market.curve.Curve;
import com.opengamma.strata.market.curve.CurveGroupDefinition;
import com.opengamma.strata.market.curve.CurveGroupEntry;
import com.opengamma.strata.market.curve.CurveInfoType;
import com.opengamma.strata.market.curve.CurveMetadata;
import com.opengamma.strata.market.curve.CurveName;
import com.opengamma.strata.market.curve.JacobianCalibrationMatrix;
import com.opengamma.strata.market.curve.NodalCurveDefinition;
import com.opengamma.strata.pricer.rate.ImmutableRatesProvider;

/**
 * Generates a rates provider based on an existing provider.
 * <p>
 * This takes a base {@link ImmutableRatesProvider} and list of curve definitions
 * to generate a child provider.
 */
public final class ImmutableRatesProviderGenerator
    implements RatesProviderGenerator {

  /**
   * The underlying known data.
   * This includes curves and FX matrix.
   */
  private final ImmutableRatesProvider knownProvider;
  /**
   * The curve definitions for the new curves to be generated.
   */
  private final ImmutableList<NodalCurveDefinition> curveDefinitions;
  /**
   * The list of curve metadata associated with each definition.
   * The size of this list must match the size of the definition list.
   */
  private final ImmutableList<CurveMetadata> curveMetadata;
  /**
   * The map between curve name and currencies for discounting.
   * The map should contains all the curve in the definition list but may have more names
   * than the curve definition list. Only the curves in the definitions list are created.
   */
  private final ImmutableSetMultimap<CurveName, Currency> discountCurveNames;
  /**
   * The map between curve name and indices for forward rates and prices.
   * The map should contains all the curve in the definition list but may have more names
   * than the curve definition list. Only the curves in the definitions list are created
   */
  private final ImmutableSetMultimap<CurveName, Index> forwardCurveNames;

  /**
   * Obtains a generator from an existing provider and definition.
   * 
   * @param knownProvider  the underlying known provider
   * @param groupDefn  the curve group definition
   * @param refData  the reference data to use
   * @return the generator
   */
  public static ImmutableRatesProviderGenerator of(
      ImmutableRatesProvider knownProvider,
      CurveGroupDefinition groupDefn,
      ReferenceData refData) {

    List<NodalCurveDefinition> curveDefns = new ArrayList<>();
    List<CurveMetadata> curveMetadata = new ArrayList<>();
    SetMultimap<CurveName, Currency> discountNames = HashMultimap.create();
    SetMultimap<CurveName, Index> indexNames = HashMultimap.create();

    for (NodalCurveDefinition curveDefn : groupDefn.getCurveDefinitions()) {
      curveDefns.add(curveDefn);
      curveMetadata.add(curveDefn.metadata(knownProvider.getValuationDate(), refData));
      CurveName curveName = curveDefn.getName();
      // A curve group is guaranteed to include an entry for every definition
      CurveGroupEntry entry = groupDefn.findEntry(curveName).get();
      Set<Currency> ccy = entry.getDiscountCurrencies();
      discountNames.putAll(curveName, ccy);
      indexNames.putAll(curveName, entry.getIndices());
    }
    return new ImmutableRatesProviderGenerator(
        knownProvider, curveDefns, curveMetadata, discountNames, indexNames);
  }

  /**
   * Creates an instance.
   * 
   * @param knownProvider  the underlying known provider
   * @param curveDefinitions  the curve definitions
   * @param curveMetadata  the curve metadata
   * @param discountCurveNames  the map of discount curves
   * @param forwardCurveNames  the map of index forward curves
   */
  private ImmutableRatesProviderGenerator(
      ImmutableRatesProvider knownProvider,
      List<NodalCurveDefinition> curveDefinitions,
      List<CurveMetadata> curveMetadata,
      SetMultimap<CurveName, Currency> discountCurveNames,
      SetMultimap<CurveName, Index> forwardCurveNames) {

    this.knownProvider = ArgChecker.notNull(knownProvider, "knownProvider");
    this.curveDefinitions = ImmutableList.copyOf(ArgChecker.notNull(curveDefinitions, "curveDefinitions"));
    this.curveMetadata = ImmutableList.copyOf(ArgChecker.notNull(curveMetadata, "curveMetadata"));
    this.discountCurveNames = ImmutableSetMultimap.copyOf(ArgChecker.notNull(discountCurveNames, "discountCurveNames"));
    this.forwardCurveNames = ImmutableSetMultimap.copyOf(ArgChecker.notNull(forwardCurveNames, "forwardCurveNames"));
  }

  //-------------------------------------------------------------------------
  @Override
  public ImmutableRatesProvider generate(
      DoubleArray parameters,
      Map<CurveName, JacobianCalibrationMatrix> jacobians,
      Map<CurveName, DoubleArray> sensitivitiesMarketQuote) {

    // collect curves for child provider based on existing provider
    Map<Currency, Curve> discountCurves = new HashMap<>();
    Map<Index, Curve> indexCurves = new HashMap<>();
    discountCurves.putAll(knownProvider.getDiscountCurves());
    indexCurves.putAll(knownProvider.getIndexCurves());

    // generate curves from combined parameter array
    int startIndex = 0;
    for (int i = 0; i < curveDefinitions.size(); i++) {
      NodalCurveDefinition curveDefn = curveDefinitions.get(i);
      CurveMetadata metadata = curveMetadata.get(i);
      CurveName name = curveDefn.getName();
      // extract parameters for the child curve
      int paramCount = curveDefn.getParameterCount();
      DoubleArray curveParams = parameters.subArray(startIndex, startIndex + paramCount);
      startIndex += paramCount;
      // create the child curve
      CurveMetadata childMetadata = childMetadata(metadata, curveDefn, jacobians, sensitivitiesMarketQuote);
      Curve curve = curveDefn.curve(knownProvider.getValuationDate(), childMetadata, curveParams);
      // put child curve into maps
      Set<Currency> currencies = discountCurveNames.get(name);
      for (Currency currency : currencies) {
        discountCurves.put(currency, curve);
      }
      Set<Index> indices = forwardCurveNames.get(name);
      for (Index index : indices) {
        indexCurves.put(index, curve);
      }
    }
    return knownProvider.toBuilder()
        .discountCurves(discountCurves)
        .indexCurves(indexCurves)
        .build();
  }

  // build the map of additional info
  private CurveMetadata childMetadata(
      CurveMetadata metadata,
      NodalCurveDefinition curveDefn,
      Map<CurveName, JacobianCalibrationMatrix> jacobians,
      Map<CurveName, DoubleArray> sensitivitiesMarketQuote) {

    JacobianCalibrationMatrix jacobian = jacobians.get(curveDefn.getName());
    CurveMetadata metadataResult = metadata;
    if (jacobian != null) {
      metadataResult = metadata.withInfo(CurveInfoType.JACOBIAN, jacobian);
    }
    DoubleArray sensitivity = sensitivitiesMarketQuote.get(curveDefn.getName());
    if (sensitivity != null) {
      metadataResult = metadataResult.withInfo(CurveInfoType.PV_SENSITIVITY_TO_MARKET_QUOTE, sensitivity);
    }
    return metadataResult;
  }

}
