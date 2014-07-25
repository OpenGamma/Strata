/**
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.sesame.marketdata;

import com.opengamma.core.value.MarketDataRequirementNames;
import com.opengamma.financial.analytics.ircurve.strips.CurveNodeWithIdentifier;
import com.opengamma.financial.analytics.ircurve.strips.PointsCurveNodeWithIdentifier;
import com.opengamma.financial.currency.CurrencyMatrix;
import com.opengamma.financial.currency.CurrencyMatrixValue;
import com.opengamma.financial.currency.CurrencyMatrixValue.CurrencyMatrixCross;
import com.opengamma.financial.currency.CurrencyMatrixValue.CurrencyMatrixExternalId;
import com.opengamma.financial.currency.CurrencyMatrixValue.CurrencyMatrixFixed;
import com.opengamma.financial.currency.CurrencyPair;
import com.opengamma.id.ExternalIdBundle;
import com.opengamma.sesame.Environment;
import com.opengamma.util.ArgumentChecker;
import com.opengamma.util.money.Currency;
import com.opengamma.util.result.FailureStatus;
import com.opengamma.util.result.Result;

/**
 *
 */
@SuppressWarnings("unchecked")
public class DefaultMarketDataFn implements MarketDataFn {

  private final CurrencyMatrix _currencyMatrix;

  private static final FieldName MARKET_VALUE = FieldName.of(MarketDataRequirementNames.MARKET_VALUE);

  public DefaultMarketDataFn(CurrencyMatrix currencyMatrix) {
    _currencyMatrix = ArgumentChecker.notNull(currencyMatrix, "currencyMatrix");
  }

  @Override
  public Result<Double> getCurveNodeValue(Environment env, CurveNodeWithIdentifier node) {
    Result<?> result = env.getMarketDataSource().get(node.getIdentifier().toBundle(), FieldName.of(node.getDataField()));
    return (Result<Double>) result;
  }

  @Override
  public Result<Double> getCurveNodeUnderlyingValue(Environment env, PointsCurveNodeWithIdentifier node) {
    ExternalIdBundle id = node.getUnderlyingIdentifier().toBundle();
    FieldName fieldName = FieldName.of(node.getUnderlyingDataField());
    Result<?> result = env.getMarketDataSource().get(id, fieldName);
    return (Result<Double>) result;
  }

  @Override
  public Result<Double> getMarketValue(Environment env, ExternalIdBundle id) {
    return (Result<Double>) env.getMarketDataSource().get(id, MARKET_VALUE);
  }

  @Override
  public Result<?> getValue(Environment env, ExternalIdBundle id, FieldName fieldName) {
    return env.getMarketDataSource().get(id, fieldName);
  }

  @Override
  public Result<Double> getFxRate(final Environment env, CurrencyPair currencyPair) {
    return getFxRate(env, currencyPair.getBase(), currencyPair.getCounter());
  }

  private Result<Double> getFxRate(final Environment env, final Currency base, final Currency counter) {
    CurrencyMatrixValue value = _currencyMatrix.getConversion(base, counter);
    if (value == null) {
      return Result.failure(FailureStatus.MISSING_DATA, "No conversion available for {}", CurrencyPair.of(base, counter));
    }
    if (value instanceof CurrencyMatrixFixed) {
      double rate = ((CurrencyMatrixFixed) value).getFixedValue();
      return Result.success(rate);
    }
    if (value instanceof CurrencyMatrixCross) {
      Currency crossCurrency = ((CurrencyMatrixCross) value).getCrossCurrency();
      Result<Double> baseCrossRate = getFxRate(env, base, crossCurrency);
      Result<Double> crossCounterRate = getFxRate(env, crossCurrency, counter);
      return baseCrossRate.combineWith(crossCounterRate, (rate1, rate2) -> Result.success(rate1 * rate2));
    }
    if (value instanceof CurrencyMatrixExternalId) {
      CurrencyMatrixExternalId idValue = (CurrencyMatrixExternalId) value;
      ExternalIdBundle externalId = idValue.getExternalIdBundle();
      String dataField = idValue.getFieldName();
      Result<?> result = env.getMarketDataSource().get(externalId, FieldName.of(dataField));
      if (result.isSuccess()) {
        Double spotRate = (Double) result.getValue();
        return Result.success(idValue.isReciprocal() ? 1 / spotRate : spotRate);
      } else {
        return Result.failure(result);
      }
    }
    throw new IllegalStateException("Unknown CurrencyMatrix class");
  }
}
