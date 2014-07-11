/**
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.sesame.bond;

import com.opengamma.analytics.financial.forex.method.FXMatrix;
import com.opengamma.analytics.financial.provider.curve.CurveBuildingBlockBundle;
import com.opengamma.analytics.financial.provider.description.interestrate.ParameterIssuerProviderInterface;
import com.opengamma.financial.analytics.conversion.BondAndBondFutureTradeConverter;
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

  private final BondAndBondFutureTradeConverter _converter;
  private final IssuerProviderFn _issuerProviderFn;

  public DiscountingBondCalculatorFactory(BondAndBondFutureTradeConverter converter,
                                          IssuerProviderFn issuerProviderFn) {
    _converter = ArgumentChecker.notNull(converter, "converter");
    _issuerProviderFn = ArgumentChecker.notNull(issuerProviderFn, "issuerProviderFn");
  }

  @Override
  public Result<DiscountingBondCalculator> createCalculator(Environment env, BondTrade trade) {

    Result<Pair<ParameterIssuerProviderInterface, CurveBuildingBlockBundle>> bundleResult =
        _issuerProviderFn.createBundle(env, trade, new FXMatrix());

    if (bundleResult.isSuccess()) {
      ParameterIssuerProviderInterface curves = bundleResult.getValue().getFirst();
      DiscountingBondCalculator calculator = new DiscountingBondCalculator(trade,
                                                                           curves,
                                                                           _converter,
                                                                           env.getValuationTime());
      return Result.success(calculator);
    } else {
      return Result.failure(bundleResult);
    }


  }
}
