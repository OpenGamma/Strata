/**
 * Copyright (C) 2013 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.sesame;

import static com.opengamma.util.result.ResultGenerator.success;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.threeten.bp.ZonedDateTime;

import com.opengamma.OpenGammaRuntimeException;
import com.opengamma.analytics.financial.forex.method.FXMatrix;
import com.opengamma.core.config.ConfigSource;
import com.opengamma.core.config.impl.ConfigItem;
import com.opengamma.core.convention.ConventionSource;
import com.opengamma.core.security.SecuritySource;
import com.opengamma.financial.analytics.curve.AbstractCurveDefinition;
import com.opengamma.financial.analytics.curve.CurveConstructionConfiguration;
import com.opengamma.financial.analytics.curve.CurveDefinition;
import com.opengamma.financial.analytics.curve.CurveGroupConfiguration;
import com.opengamma.financial.analytics.curve.CurveNodeCurrencyVisitor;
import com.opengamma.financial.analytics.curve.CurveTypeConfiguration;
import com.opengamma.financial.analytics.ircurve.strips.CurveNode;
import com.opengamma.financial.currency.CurrencyPair;
import com.opengamma.id.VersionCorrection;
import com.opengamma.sesame.marketdata.MarketDataFn;
import com.opengamma.sesame.marketdata.MarketDataRequirement;
import com.opengamma.sesame.marketdata.MarketDataRequirementFactory;
import com.opengamma.sesame.marketdata.MarketDataStatus;
import com.opengamma.sesame.marketdata.MarketDataValues;
import com.opengamma.util.ArgumentChecker;
import com.opengamma.util.money.Currency;
import com.opengamma.util.result.Result;
import com.opengamma.util.result.SuccessStatus;

/**
 * Function implementation that provides a FX matrix.
 */
public class DefaultFXMatrixFn implements FXMatrixFn {

  /**
   * The convention source.
   */
  private final ConventionSource _conventionSource;
  /**
   * The security source.
   */
  private final SecuritySource _securitySource;
  /**
   * The currency pairs function.
   */
  private final CurrencyPairsFn _currencyPairsFn;
  /**
   * The market data function.
   */
  private final MarketDataFn _marketDataFn;
  /**
   * The valuation time function.
   */
  private final ValuationTimeFn _valuationTimeFn;
  /**
   * The config source.
   */
  private final ConfigSource _configSource;

  public DefaultFXMatrixFn(ConfigSource configSource,
                           ConventionSource conventionSource,
                           SecuritySource securitySource,
                           CurrencyPairsFn currencyPairsFn,
                           MarketDataFn marketDataFn,
                           ValuationTimeFn valuationTimeFn) {
    _configSource = ArgumentChecker.notNull(configSource, "configSource");
    _conventionSource = ArgumentChecker.notNull(conventionSource, "conventionSource");
    _securitySource = ArgumentChecker.notNull(securitySource, "securitySource");
    _currencyPairsFn = ArgumentChecker.notNull(currencyPairsFn, "currencyPairsFunction");
    _marketDataFn = ArgumentChecker.notNull(marketDataFn, "marketDataProviderFunction");
    _valuationTimeFn = ArgumentChecker.notNull(valuationTimeFn, "valuationTimeFn");
  }

  //-------------------------------------------------------------------------
  private Set<Currency> extractCurrencies(CurveConstructionConfiguration configuration,
                                          CurveNodeCurrencyVisitor curveNodeCurrencyVisitor) {

    final Set<Currency> currencies = new TreeSet<>();

    for (final CurveGroupConfiguration group : configuration.getCurveGroups()) {

      for (final Map.Entry<String, List<? extends CurveTypeConfiguration>> entry : group.getTypesForCurves().entrySet()) {

        final String curveName = entry.getKey();
        final AbstractCurveDefinition curveDefinition = findCurveDefinition(curveName);

        if (curveDefinition == null) {
          throw new OpenGammaRuntimeException("Could not get curve definition called " + curveName);
        }
        if (curveDefinition instanceof CurveDefinition) {
          for (final CurveNode node : ((CurveDefinition) curveDefinition).getNodes()) {
            currencies.addAll(node.accept(curveNodeCurrencyVisitor));
          }
        } else {
          return Collections.emptySet();
        }
      }
    }
    final List<String> exogenousConfigurations = configuration.getExogenousConfigurations();

    if (exogenousConfigurations != null) {
      for (final String name : exogenousConfigurations) {

        final CurveConstructionConfiguration exogenousConfiguration =
            _configSource.getLatestByName(CurveConstructionConfiguration.class, name);
        currencies.addAll(extractCurrencies(exogenousConfiguration, curveNodeCurrencyVisitor));
      }
    }
    return currencies;
  }

  private AbstractCurveDefinition findCurveDefinition(String curveName) {

    final Collection<ConfigItem<Object>> items =
        _configSource.get(Object.class, curveName, VersionCorrection.LATEST);

    for (ConfigItem<Object> item : items) {
      final Object value = item.getValue();
      if (value instanceof AbstractCurveDefinition) {
        return (AbstractCurveDefinition) value;
      }
    }
    return null;
  }

  @Override
  public Result<FXMatrix> getFXMatrix(CurveConstructionConfiguration configuration,
                                      ZonedDateTime valuationTime) {

    // todo - should this actually be another function or set of functions
    final Set<Currency> currencies = extractCurrencies(configuration, new CurveNodeCurrencyVisitor(_conventionSource, _securitySource));
    return buildResult(currencies, valuationTime);
  }

  @Override
  public Result<FXMatrix> getFXMatrix(Set<Currency> currencies) {
    return buildResult(currencies, _valuationTimeFn.getTime());
  }

  private Result<FXMatrix> buildResult(Set<Currency> currencies, ZonedDateTime valuationTime) {
    // todo - if we don't have all the data, do we return a partial/empty fx matrix or an error, doing the latter

    final FXMatrix matrix = new FXMatrix();

    Currency refCurr = null;

    for (Currency currency : currencies) {
      // Use the first currency in the set as the reference currency in the matrix
      if (refCurr == null) {
        refCurr = currency;
      } else {
        Currency base = refCurr;
        Currency counter = currency;
        //note - currency matrix will ensure the spotRate returned is interpreted correctly,
        //depending on the order base and counter are specified in.
        MarketDataRequirement spotReqmt = MarketDataRequirementFactory.of(CurrencyPair.of(base, counter));
        Result<MarketDataValues> marketDataResult = _marketDataFn.requestData(spotReqmt); //, valuationTime);
        MarketDataValues marketDataValues = marketDataResult.getValue();
        if (marketDataValues.getStatus(spotReqmt) == MarketDataStatus.AVAILABLE) {
          double spotRate = (Double) marketDataValues.getValue(spotReqmt);
          matrix.addCurrency(counter, base, spotRate);
        }
      }
    }
    return success(matrix);
  }

}
