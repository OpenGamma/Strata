/**
 * Copyright (C) 2013 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.sesame;

import static com.opengamma.util.result.FunctionResultGenerator.success;

import java.util.Set;

import com.opengamma.analytics.financial.forex.method.FXMatrix;
import com.opengamma.core.config.ConfigSource;
import com.opengamma.core.convention.ConventionSource;
import com.opengamma.financial.analytics.curve.ConfigDBCurveConstructionConfigurationSource;
import com.opengamma.financial.analytics.curve.CurveConstructionConfiguration;
import com.opengamma.financial.analytics.curve.CurveConstructionConfigurationSource;
import com.opengamma.financial.analytics.curve.CurveNodeCurrencyVisitor;
import com.opengamma.financial.analytics.curve.CurveUtils;
import com.opengamma.financial.currency.CurrencyPair;
import com.opengamma.id.VersionCorrection;
import com.opengamma.sesame.marketdata.MarketDataFn;
import com.opengamma.sesame.marketdata.MarketDataRequirement;
import com.opengamma.sesame.marketdata.MarketDataRequirementFactory;
import com.opengamma.sesame.marketdata.MarketDataStatus;
import com.opengamma.sesame.marketdata.MarketDataValues;
import com.opengamma.util.ArgumentChecker;
import com.opengamma.util.money.Currency;
import com.opengamma.util.result.FunctionResult;
import com.opengamma.util.result.SuccessStatus;

/**
 */
public class DefaultFXMatrixFn implements FXMatrixFn {

  private final CurveConstructionConfigurationSource _curveConfigurationSource;

  private final ConventionSource _conventionSource;

  private final ConfigSource _configSource;

  private final CurrencyPairsFn _currencyPairsFn;

  private final MarketDataFn _marketDataFn;

  public DefaultFXMatrixFn(ConfigSource configSource,
                           ConventionSource conventionSource,
                           CurrencyPairsFn currencyPairsFn,
                           MarketDataFn marketDataFn) {
    _configSource = ArgumentChecker.notNull(configSource, "configSource");
    _curveConfigurationSource = new ConfigDBCurveConstructionConfigurationSource(_configSource);
    _conventionSource = ArgumentChecker.notNull(conventionSource, "conventionSource");
    _currencyPairsFn = ArgumentChecker.notNull(currencyPairsFn, "currencyPairsFunction");
    _marketDataFn = ArgumentChecker.notNull(marketDataFn, "marketDataProviderFunction");
  }

  @Override
  public FunctionResult<FXMatrix> getFXMatrix(CurveConstructionConfiguration configuration) {

    // todo - should this actually be another function or set of functions
    final Set<Currency> currencies = CurveUtils.getCurrencies(configuration,
                                                              _configSource,
                                                              VersionCorrection.LATEST,
                                                              new CurveNodeCurrencyVisitor(_conventionSource));

    return buildResult(currencies);
  }

  @Override
  public FunctionResult<FXMatrix> getFXMatrix(Set<Currency> currencies) {
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
        MarketDataRequirement spotReqmt = MarketDataRequirementFactory.of(CurrencyPair.of(refCurr, currency));
        FunctionResult<MarketDataValues> marketDataFunctionResult = _marketDataFn.requestData(spotReqmt);
        MarketDataValues marketDataValues = marketDataFunctionResult.getResult();
        if (marketDataValues.getStatus(spotReqmt) == MarketDataStatus.AVAILABLE) {
          double spotRate = (Double) marketDataValues.getValue(spotReqmt);

          FunctionResult<CurrencyPair> result = _currencyPairsFn.getCurrencyPair(refCurr, currency);
          if (result.getStatus() == SuccessStatus.SUCCESS) {
            boolean inversionRequired = result.getResult().getCounter().equals(refCurr);
            matrix.addCurrency(currency, refCurr, inversionRequired ? 1 / spotRate : spotRate);
          }
        }
      }
    }
    return success(matrix);
  }
}
