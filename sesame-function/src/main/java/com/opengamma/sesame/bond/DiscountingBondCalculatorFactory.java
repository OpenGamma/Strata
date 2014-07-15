/**
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.sesame.bond;

import java.util.Map;

import com.opengamma.analytics.financial.forex.method.FXMatrix;
import com.opengamma.analytics.financial.provider.curve.CurveBuildingBlockBundle;
import com.opengamma.analytics.financial.provider.description.interestrate.ParameterIssuerProviderInterface;
import com.opengamma.financial.analytics.conversion.BondAndBondFutureTradeConverter;
import com.opengamma.financial.analytics.curve.CurveDefinition;
import com.opengamma.sesame.CurveDefinitionFn;
import com.opengamma.sesame.Environment;
import com.opengamma.sesame.IssuerProviderFn;
import com.opengamma.sesame.trade.BondTrade;
import com.opengamma.util.ArgumentChecker;
import com.opengamma.util.result.Result;
import com.opengamma.util.tuple.Pair;

/**
 * Implementation of the BondCalculatorFactory that uses the discounting calculator to return values.
 */
public class DiscountingBondCalculatorFactory implements BondCalculatorFactory {


  /**
   * Converter to the appropriate classes in the analytics
   */
  private final BondAndBondFutureTradeConverter _converter;

  /**
   * The multicurve bundle for curves by issuer.
   */
  private final IssuerProviderFn _issuerProviderFn;

  /**
   * Curve definition function
   */
  private final CurveDefinitionFn _curveDefinitionFn;

  /**
   * Creates the factory.
   *
   * @param converter converter for transforming a bond
   * @param issuerProviderFn multicurve bundle for curves by issuer.
   * @param curveDefinitionFn the curve definition function, not null.

   */
  public DiscountingBondCalculatorFactory(BondAndBondFutureTradeConverter converter,
                                          IssuerProviderFn issuerProviderFn,
                                          CurveDefinitionFn curveDefinitionFn) {
    _converter = ArgumentChecker.notNull(converter, "converter");
    _issuerProviderFn = ArgumentChecker.notNull(issuerProviderFn, "issuerProviderFn");
    _curveDefinitionFn = ArgumentChecker.notNull(curveDefinitionFn, "curveDefinitionFn");
  }

  @Override
  public Result<DiscountingBondCalculator> createCalculator(Environment env, BondTrade trade) {

    Result<Pair<ParameterIssuerProviderInterface, CurveBuildingBlockBundle>> bundleResult =
        _issuerProviderFn.createBundle(env, trade, new FXMatrix());

    if (bundleResult.isSuccess()) {

      Result<Map<String, CurveDefinition>> curveDefinitions =
          _curveDefinitionFn.getCurveDefinitions(bundleResult.getValue().getSecond());

      if (!curveDefinitions.isSuccess()) {
        return Result.failure(curveDefinitions);
      }

      ParameterIssuerProviderInterface curves = bundleResult.getValue().getFirst();
      CurveBuildingBlockBundle blocks = bundleResult.getValue().getSecond();
      DiscountingBondCalculator calculator =
          new DiscountingBondCalculator(trade, curves, blocks,  _converter, env.getValuationTime(),
                                        curveDefinitions.getValue());
      return Result.success(calculator);
    } else {
      return Result.failure(bundleResult);
    }


  }
}
