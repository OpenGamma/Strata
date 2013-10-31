/**
 * Copyright (C) 2013 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.sesame;

import java.util.Set;

import com.opengamma.analytics.financial.forex.method.FXMatrix;
import com.opengamma.core.config.ConfigSource;
import com.opengamma.financial.analytics.curve.ConfigDBCurveConstructionConfigurationSource;
import com.opengamma.financial.analytics.curve.CurveConstructionConfiguration;
import com.opengamma.financial.analytics.curve.CurveConstructionConfigurationSource;
import com.opengamma.financial.analytics.curve.CurveNodeCurrencyVisitor;
import com.opengamma.financial.analytics.curve.CurveUtils;
import com.opengamma.financial.convention.ConventionSource;
import com.opengamma.financial.currency.CurrencyPair;
import com.opengamma.id.VersionCorrection;
import com.opengamma.util.ArgumentChecker;
import com.opengamma.util.money.Currency;

/**
 */
public class FXMatrixProvider implements FXMatrixProviderFunction {

  private final CurveConstructionConfigurationSource _curveConfigurationSource;

  private final ConventionSource _conventionSource;

  private final ResultGenerator _resultGenerator;
  private final ConfigSource _configSource;

  private final CurrencyPairsFunction _currencyPairsFunction;

  private final MarketDataProviderFunction _marketDataProviderFunction;

  public FXMatrixProvider(ConfigSource configSource,
                          ConventionSource conventionSource,
                          ResultGenerator resultGenerator,
                          CurrencyPairsFunction currencyPairsFunction,
                          MarketDataProviderFunction marketDataProviderFunction) {
    ArgumentChecker.notNull(configSource, "configSource");
    ArgumentChecker.notNull(conventionSource, "conventionSource");
    ArgumentChecker.notNull(resultGenerator, "resultGenerator");
    ArgumentChecker.notNull(currencyPairsFunction, "currencyPairsFunction");
    ArgumentChecker.notNull(marketDataProviderFunction, "marketDataProviderFunction");
    _configSource = configSource;
    _curveConfigurationSource = new ConfigDBCurveConstructionConfigurationSource(_configSource);
    _conventionSource = conventionSource;
    _resultGenerator = resultGenerator;
    _currencyPairsFunction = currencyPairsFunction;
    _marketDataProviderFunction = marketDataProviderFunction;
  }

  @Override
  public FunctionResult<FXMatrix> getFXMatrix(String curveConfigurationName) {

    final CurveConstructionConfiguration curveConstructionConfiguration =
        _curveConfigurationSource.getCurveConstructionConfiguration(curveConfigurationName);

    if (curveConstructionConfiguration == null) {
      return _resultGenerator.generateFailureResult(
          ResultStatus.MISSING_DATA, "Could not get curve construction configuration called: {}", curveConfigurationName);
    }

    // todo - should this actually be another function or set of functions
    final Set<Currency> currencies = CurveUtils.getCurrencies(curveConstructionConfiguration,
                                                              _configSource,
                                                              VersionCorrection.LATEST,
                                                              _conventionSource,
                                                              new CurveNodeCurrencyVisitor(_conventionSource));

    return buildResult(currencies);
  }

  private FunctionResult<FXMatrix> buildResult(Set<Currency> currencies) {
    // todo - if we don't have all the data, do we return a partial/empty fx matrix or an error, doing the latter

    final FXMatrix matrix = new FXMatrix();

    Currency refCurr = null;

    for (Currency currency : currencies) {
      // Use the first currency in the set as the reference currency in the matrix
      if (refCurr == null) {
        refCurr = currency;
      } else {
        MarketDataRequirement spotReqmt = StandardMarketDataRequirement.of(
            CurrencyPair.of(currency, refCurr));
        MarketDataFunctionResult marketDataFunctionResult = _marketDataProviderFunction.requestData(spotReqmt);

        if (marketDataFunctionResult.getMarketDataState(spotReqmt) == MarketDataStatus.AVAILABLE) {
          double spotRate = (Double) marketDataFunctionResult.getMarketDataValue(spotReqmt).getValue();

          FunctionResult<CurrencyPair> result = _currencyPairsFunction.getCurrencyPair(refCurr, currency);
          if (result.getStatus() == ResultStatus.SUCCESS) {
            boolean inversionRequired = result.getResult().getCounter().equals(refCurr);
            matrix.addCurrency(currency, refCurr, inversionRequired ? 1 / spotRate : spotRate);
          }
        }
      }
    }
    return _resultGenerator.generateSuccessResult(matrix);
  }
}
