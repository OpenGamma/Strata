/**
 * Copyright (C) 2013 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.sesame;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import org.threeten.bp.LocalDate;
import org.threeten.bp.OffsetTime;

import com.opengamma.analytics.financial.forex.method.FXMatrix;
import com.opengamma.analytics.financial.provider.curve.CurveBuildingBlockBundle;
import com.opengamma.analytics.financial.provider.description.interestrate.MulticurveProviderDiscount;
import com.opengamma.analytics.financial.provider.description.interestrate.ProviderUtils;
import com.opengamma.core.position.Counterparty;
import com.opengamma.core.position.Trade;
import com.opengamma.core.position.impl.SimpleCounterparty;
import com.opengamma.core.position.impl.SimpleTrade;
import com.opengamma.financial.analytics.curve.CurveConstructionConfiguration;
import com.opengamma.financial.security.FinancialSecurity;
import com.opengamma.id.ExternalId;
import com.opengamma.util.ArgumentChecker;
import com.opengamma.util.result.FailureStatus;
import com.opengamma.util.result.Result;
import com.opengamma.util.tuple.Pair;
import com.opengamma.util.tuple.Pairs;

/**
 * Creates a collection of multicurves based on a security and market exposure
 * selector, combining them into a single multicurve.
 */
public class ExposureFunctionsDiscountingMulticurveCombinerFn implements DiscountingMulticurveCombinerFn {

  /**
   * Generates the market exposure selector. In turn this can be used to get
   * an ExposureFunction.
   */
  private final MarketExposureSelectorFn _marketExposureSelectorFn;

  /**
   * Generates a discounting multicurve bundle.
   */
  private final DiscountingMulticurveBundleFn _multicurveBundleProviderFunction;

  /**
   * Constructor for a multicurve function that selects the multicurves by either trade or security.
   *
   * @param marketExposureSelectorFn the exposure function selector.
   * @param multicurveBundleProviderFunction the function used to generate the multicurves.
   */
  public ExposureFunctionsDiscountingMulticurveCombinerFn(MarketExposureSelectorFn marketExposureSelectorFn,
                                                          DiscountingMulticurveBundleFn multicurveBundleProviderFunction) {
    _marketExposureSelectorFn =
        ArgumentChecker.notNull(marketExposureSelectorFn, "marketExposureSelectorFn");
    _multicurveBundleProviderFunction =
        ArgumentChecker.notNull(multicurveBundleProviderFunction, "multicurveBundleProviderFunction");
  }

  @Override
  public Result<Pair<MulticurveProviderDiscount, CurveBuildingBlockBundle>> createMergedMulticurveBundle(
      Environment env, FinancialSecurity security, FXMatrix fxMatrix) {

    Trade trade = new SimpleTrade(security,
                                  BigDecimal.ONE,
                                  new SimpleCounterparty(ExternalId.of(Counterparty.DEFAULT_SCHEME, "CPARTY")),
                                  LocalDate.now(),
                                  OffsetTime.now());
    return createMergedMulticurveBundle(env, trade, fxMatrix);
  }
  
  @Override
  public Result<Pair<MulticurveProviderDiscount, CurveBuildingBlockBundle>> createMergedMulticurveBundle(
      Environment env, Trade trade, FXMatrix fxMatrix) {
    Result<MarketExposureSelector> mesResult = _marketExposureSelectorFn.getMarketExposureSelector();

    if (mesResult.isSuccess()) {
      Set<Result<?>> incompleteBundles = new HashSet<>();
      Set<MulticurveProviderDiscount> bundles = new HashSet<>();
      CurveBuildingBlockBundle mergedJacobianBundle = new CurveBuildingBlockBundle();

      MarketExposureSelector selector = mesResult.getValue();
      Set<CurveConstructionConfiguration> curveConfigs = selector.determineCurveConfigurations(trade);
      for (CurveConstructionConfiguration curveConfig : curveConfigs) {
        Result<Pair<MulticurveProviderDiscount, CurveBuildingBlockBundle>> bundle =
            _multicurveBundleProviderFunction.generateBundle(env, curveConfig);
        if (bundle.isSuccess()) {
          Pair<MulticurveProviderDiscount, CurveBuildingBlockBundle> result = bundle.getValue();
          bundles.add(result.getFirst());
          mergedJacobianBundle.addAll(result.getSecond());
        } else {
          incompleteBundles.add(bundle);
        }
      }

      // TODO this can be cleaned up
      if (!curveConfigs.isEmpty() && incompleteBundles.isEmpty()) {
        return Result.success(Pairs.of(mergeBundlesAndMatrix(bundles, fxMatrix), mergedJacobianBundle));
      } else if (curveConfigs.isEmpty()) {
        return Result.failure(FailureStatus.MISSING_DATA, "No matching curves found for trade: {}", trade);
      } else {
        return Result.failure(incompleteBundles);
      }
    } else {
      return Result.failure(mesResult);
    }
  }

  private MulticurveProviderDiscount mergeBundlesAndMatrix(Collection<MulticurveProviderDiscount> providers,
                                                           FXMatrix fxMatrix) {
    return providers.size() > 1 ?
        ProviderUtils.mergeDiscountingProviders(mergeBundles(providers), fxMatrix) :
        providers.iterator().next();
  }

  private MulticurveProviderDiscount mergeBundles(Collection<MulticurveProviderDiscount> providers) {
    return ProviderUtils.mergeDiscountingProviders(providers);
  }
}
