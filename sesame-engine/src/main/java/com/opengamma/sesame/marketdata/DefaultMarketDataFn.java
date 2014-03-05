/**
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.sesame.marketdata;

import com.opengamma.core.value.MarketDataRequirementNames;
import com.opengamma.engine.value.ValueRequirement;
import com.opengamma.financial.analytics.ircurve.strips.CurveNodeWithIdentifier;
import com.opengamma.financial.analytics.ircurve.strips.PointsCurveNodeWithIdentifier;
import com.opengamma.financial.currency.CurrencyMatrix;
import com.opengamma.financial.currency.CurrencyMatrixValue;
import com.opengamma.financial.currency.CurrencyMatrixValueVisitor;
import com.opengamma.financial.currency.CurrencyPair;
import com.opengamma.id.ExternalIdBundle;
import com.opengamma.sesame.Environment;
import com.opengamma.util.ArgumentChecker;
import com.opengamma.util.money.Currency;

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
  public MarketDataItem<Double> getCurveNodeValue(Environment env, CurveNodeWithIdentifier node) {
    MarketDataItem<?> item = env.getMarketDataSource().get(node.getIdentifier().toBundle(), FieldName.of(node.getDataField()));
    return (MarketDataItem<Double>) item;
  }

  @Override
  public MarketDataItem<Double> getPointsCurveNodeUnderlyingValue(Environment env, PointsCurveNodeWithIdentifier node) {
    ExternalIdBundle id = node.getUnderlyingIdentifier().toBundle();
    FieldName fieldName = FieldName.of(node.getUnderlyingDataField());
    MarketDataItem<?> item = env.getMarketDataSource().get(id, fieldName);
    return (MarketDataItem<Double>) item;
  }

  @Override
  public MarketDataItem<Double> getMarketValue(Environment env, ExternalIdBundle id) {
    return (MarketDataItem<Double>) env.getMarketDataSource().get(id, MARKET_VALUE);
  }

  @Override
  public MarketDataItem<?> getValue(Environment env, ExternalIdBundle id, FieldName fieldName) {
    return env.getMarketDataSource().get(id, fieldName);
  }

  @Override
  public MarketDataItem<Double> getFxRate(final Environment env, CurrencyPair currencyPair) {
    return getFxRate(env, currencyPair.getBase(), currencyPair.getCounter());
  }

  private MarketDataItem<Double> getFxRate(final Environment env,
                                           final Currency base,
                                           final Currency counter) {
    CurrencyMatrixValue value = _currencyMatrix.getConversion(base, counter);
    if (value == null) {
      return MarketDataItem.unavailable();
    }
    CurrencyMatrixValueVisitor<MarketDataItem> visitor = new CurrencyMatrixValueVisitor<MarketDataItem>() {
      @Override
      public MarketDataItem visitFixed(CurrencyMatrixValue.CurrencyMatrixFixed fixedValue) {
        return MarketDataItem.available(fixedValue.getFixedValue());
      }

      @SuppressWarnings("unchecked")
      @Override
      public MarketDataItem<Double> visitValueRequirement(CurrencyMatrixValue.CurrencyMatrixValueRequirement req) {
        ValueRequirement valueRequirement = req.getValueRequirement();
        ExternalIdBundle idBundle = valueRequirement.getTargetReference().getRequirement().getIdentifiers();
        String dataField = valueRequirement.getValueName();
        MarketDataItem<?> item = env.getMarketDataSource().get(idBundle, FieldName.of(dataField));
        if (!item.isAvailable()) {
          return MarketDataItem.unavailable();
        }
        Double spotRate = (Double) item.getValue();
        if (req.isReciprocal()) {
          return MarketDataItem.available(1 / spotRate);
        } else {
          return MarketDataItem.available(spotRate);
        }
      }

      @Override
      public MarketDataItem visitCross(CurrencyMatrixValue.CurrencyMatrixCross cross) {
        MarketDataItem baseCrossRate = getFxRate(env, base, cross.getCrossCurrency());
        MarketDataItem crossCounterRate = getFxRate(env, cross.getCrossCurrency(), counter);
        if (!baseCrossRate.isAvailable() || !crossCounterRate.isAvailable()) {
          // TODO should this be pending? YES. depends on the data source. helper method to compose statuses?
          return MarketDataItem.unavailable();
        } else {
          Double rate1 = (Double) baseCrossRate.getValue();
          Double rate2 = (Double) crossCounterRate.getValue();
          return MarketDataItem.available(rate1 * rate2);
        }
      }
    };
    return value.accept(visitor);
  }
}
