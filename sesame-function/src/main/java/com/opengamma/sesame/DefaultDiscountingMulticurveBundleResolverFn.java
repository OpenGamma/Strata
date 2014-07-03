/**
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.sesame;

import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

import com.opengamma.financial.analytics.curve.CurveConstructionConfiguration;
import com.opengamma.util.ArgumentChecker;
import com.opengamma.util.result.Result;

/**
 * Function implementation that for a particular curve config, determines which
 * curves need to go into a multicurve bundle and coordinates the building
 * of them. This ensures that a particular curve only needs to get built once.
 * Once the required curves have been built the function delegates to a
 * DiscountingMulticurveBundleFn to calculate the required result.
 */
public class DefaultDiscountingMulticurveBundleResolverFn implements DiscountingMulticurveBundleResolverFn {

  /**
   * Generates a discounting multicurve bundle.
   */
  private final DiscountingMulticurveBundleFn _multicurveBundleProviderFunction;

  public DefaultDiscountingMulticurveBundleResolverFn(DiscountingMulticurveBundleFn multicurveBundleProviderFunction) {
    _multicurveBundleProviderFunction =
        ArgumentChecker.notNull(multicurveBundleProviderFunction, "multicurveBundleProviderFunction");
  }

  @Override
  public Result<MulticurveBundle> generateBundle(Environment env, CurveConstructionConfiguration curveConfig) {
    return _multicurveBundleProviderFunction.generateBundle(env, curveConfig, buildRequiredCurves(env, curveConfig));
  }

  @Override
  public Result<ImpliedDepositCurveData> extractImpliedDepositCurveData(
      Environment env, CurveConstructionConfiguration curveConfig) {

    Map<CurveConstructionConfiguration, Result<MulticurveBundle>> builtCurves = buildRequiredCurves(env, curveConfig);
    return _multicurveBundleProviderFunction.extractImpliedDepositCurveData(env, curveConfig, builtCurves);
  }

  // Builds all curves required as exogenous config for the supplied curve config.
  // As each curve is built, it is added to a collection and the collection
  // is passed in as each subsequent curve is built so the data can be
  // used in constructing the multicurve.
  // Note that this does not build the supplied curve config itself - callers need
  // to do that if desired.
  private Map<CurveConstructionConfiguration, Result<MulticurveBundle>> buildRequiredCurves(
      Environment env, CurveConstructionConfiguration curveConfig) {

    // Get the order to build any exogenous curves in
    LinkedHashSet<CurveConstructionConfiguration> orderedCurves =
        determineCurveConfigOrdering(curveConfig.resolveCurveConfigurations());

    Map<CurveConstructionConfiguration, Result<MulticurveBundle>> builtCurves = new HashMap<>();

    for (CurveConstructionConfiguration config : orderedCurves) {
      System.out.println("Generating: [" + config + "] for env: [" + env + "]");
      builtCurves.put(config, _multicurveBundleProviderFunction.generateBundle(env, config, builtCurves));
    }

    return builtCurves;
  }

  private LinkedHashSet<CurveConstructionConfiguration> determineCurveConfigOrdering(
      List<CurveConstructionConfiguration> curveConfigs) {
    return determineCurveConfigOrdering(new LinkedHashSet<CurveConstructionConfiguration>(), curveConfigs);
  }

  // Recursively determines the ordered set of curves that need to be built (due to
  // exogenous curves) before the requested set of curve configs can be built. Uses
  // a LinkedHashSet so we get ordering but no duplication of elements
  private LinkedHashSet<CurveConstructionConfiguration> determineCurveConfigOrdering(
      LinkedHashSet<CurveConstructionConfiguration> existing, List<CurveConstructionConfiguration> curveConfigs) {

    LinkedHashSet<CurveConstructionConfiguration> toBuild = new LinkedHashSet<>(existing);
    for (CurveConstructionConfiguration curveConfig : curveConfigs) {

      List<CurveConstructionConfiguration> exogenousConfigs = curveConfig.resolveCurveConfigurations();
      if (!exogenousConfigs.isEmpty()) {
        toBuild.addAll(determineCurveConfigOrdering(toBuild, exogenousConfigs));
      }
      toBuild.add(curveConfig);
    }
    return toBuild;
  }
}
