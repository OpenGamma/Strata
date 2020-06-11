/*
 * Copyright (C) 2017 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.measure.credit;

import java.util.Map;

import com.google.common.collect.ImmutableSet;
import com.opengamma.strata.basics.StandardId;
import com.opengamma.strata.basics.currency.Currency;
import com.opengamma.strata.calc.CalculationRules;
import com.opengamma.strata.calc.runner.CalculationParameter;
import com.opengamma.strata.calc.runner.CalculationParameters;
import com.opengamma.strata.calc.runner.FunctionRequirements;
import com.opengamma.strata.collect.tuple.Pair;
import com.opengamma.strata.data.MarketData;
import com.opengamma.strata.data.MarketDataId;
import com.opengamma.strata.data.ObservableSource;
import com.opengamma.strata.data.scenario.ScenarioMarketData;
import com.opengamma.strata.market.curve.CurveId;
import com.opengamma.strata.pricer.credit.CreditRatesProvider;

/**
 * The lookup that provides access to credit rates in market data.
 * <p>
 * The credit rates market lookup provides access to credit, discount and recovery rate curves.
 * <p>
 * The lookup implements {@link CalculationParameter} and is used by passing it
 * as an argument to {@link CalculationRules}. It provides the link between the
 * data that the function needs and the data that is available in {@link ScenarioMarketData}.
 * <p>
 * Implementations of this interface must be immutable.
 */
public interface CreditRatesMarketDataLookup extends CalculationParameter {

  /**
   * Obtains an instance based on a maps for credit, discount and recovery rate curves.
   *
   * @param creditCurveIds  the credit curve identifiers, keyed by legal entity ID and currency
   * @param discountCurveIds  the discount curve identifiers, keyed by currency
   * @param recoveryRateCurveIds  the recovery rate curve identifiers, keyed by legal entity ID
   * @return the rates lookup containing the specified curves
   */
  public static CreditRatesMarketDataLookup of(
      Map<Pair<StandardId, Currency>, CurveId> creditCurveIds,
      Map<Currency, CurveId> discountCurveIds,
      Map<StandardId, CurveId> recoveryRateCurveIds) {

    return DefaultCreditRatesMarketDataLookup.of(creditCurveIds, discountCurveIds, recoveryRateCurveIds, ObservableSource.NONE);
  }

  /**
   * Obtains an instance based on a maps for credit, discount and recovery rate curves.
   *
   * @param creditCurveIds  the credit curve identifiers, keyed by legal entity ID and currency
   * @param discountCurveIds  the discount curve identifiers, keyed by currency
   * @param recoveryRateCurveIds  the recovery rate curve identifiers, keyed by legal entity ID
   * @param observableSource  the source of market data for quotes and other observable market data
   * @return the rates lookup containing the specified curves
   */
  public static CreditRatesMarketDataLookup of(
      Map<Pair<StandardId, Currency>, CurveId> creditCurveIds,
      Map<Currency, CurveId> discountCurveIds,
      Map<StandardId, CurveId> recoveryRateCurveIds,
      ObservableSource observableSource) {

    return DefaultCreditRatesMarketDataLookup.of(creditCurveIds, discountCurveIds, recoveryRateCurveIds, observableSource);
  }

  //-------------------------------------------------------------------------
  /**
   * Gets the type that the lookup will be queried by.
   * <p>
   * This returns {@code CreditRatesMarketDataLookup.class}.
   * When querying parameters using {@link CalculationParameters#findParameter(Class)},
   * {@code CreditRatesMarketDataLookup.class} must be passed in to find the instance.
   * 
   * @return the type of the parameter implementation
   */
  @Override
  public default Class<? extends CalculationParameter> queryType() {
    return CreditRatesMarketDataLookup.class;
  }

  //-------------------------------------------------------------------------
  /**
   * Gets the set of currencies that discount factors are provided for.
   *
   * @return the set of discount curve currencies
   */
  public abstract ImmutableSet<Currency> getDiscountCurrencies();

  /**
   * Gets the identifiers used to obtain the discount factors for the specified currency.
   * <p>
   * In most cases, the identifier will refer to a curve.
   * If the currency is not found, an exception is thrown.
   *
   * @param currency  the currency for which identifiers are required
   * @return the set of market data identifiers 
   * @throws IllegalArgumentException if the currency is not found
   */
  public abstract ImmutableSet<MarketDataId<?>> getDiscountMarketDataIds(Currency currency);

  /**
   * Gets the set of pairs of legal entity ID and currency that credit curves are provided for.
   *
   * @return the set of pairs of legal entity ID and currency
   */
  public abstract ImmutableSet<Pair<StandardId, Currency>> getCreditLegalEntities();

  /**
   * Gets the identifiers used to obtain the credit curve for the pair of legal entity ID and currency. 
   * <p>
   * In most cases, the identifier will refer to a curve.
   * If the pair is not found, an exception is thrown.
   * 
   * @param standardId  the legal entity ID
   * @param currency  the currency 
   * @return the set of market data identifiers 
   * @throws IllegalArgumentException if the pair is not found
   */
  public abstract ImmutableSet<MarketDataId<?>> getCreditMarketDataIds(StandardId standardId, Currency currency);

  /**
   * Gets the set of legal entity IDs that recovery rate curves are provided for.
   *
   * @return the set of legal entity IDs
   */
  public abstract ImmutableSet<StandardId> getRecoveryRateLegalEntities();

  /**
   * Gets the identifiers used to obtain the recovery rate curve for the legal entity ID. 
   * <p>
   * In most cases, the identifier will refer to a curve.
   * If the ID is not found, an exception is thrown.
   * 
   * @param standardId  the legal entity ID
   * @return the set of market data identifiers 
   * @throws IllegalArgumentException if the ID is not found
   */
  public abstract ImmutableSet<MarketDataId<?>> getRecoveryRateMarketDataIds(StandardId standardId);

  //-------------------------------------------------------------------------
  /**
   * Creates market data requirements for the specified standard ID and currency.
   * 
   * @param legalEntityId  legal entity ID
   * @param currency  the currency 
   * @return the requirements
   * @throws IllegalArgumentException if unable to create requirements
   */
  public abstract FunctionRequirements requirements(StandardId legalEntityId, Currency currency);

  //-------------------------------------------------------------------------
  /**
   * Obtains a filtered view of the complete set of market data.
   * <p>
   * This method returns an instance that binds the lookup to the market data.
   * The input is {@link ScenarioMarketData}, which contains market data for all scenarios.
   * 
   * @param marketData  the complete set of market data for all scenarios
   * @return the filtered market data
   */
  public default CreditRatesScenarioMarketData marketDataView(ScenarioMarketData marketData) {
    return DefaultCreditRatesScenarioMarketData.of(this, marketData);
  }

  /**
   * Obtains a filtered view of the complete set of market data.
   * <p>
   * This method returns an instance that binds the lookup to the market data.
   * The input is {@link MarketData}, which contains market data for one scenario.
   * 
   * @param marketData  the complete set of market data for one scenario
   * @return the filtered market data
   */
  public default CreditRatesMarketData marketDataView(MarketData marketData) {
    return DefaultCreditRatesMarketData.of(this, marketData);
  }

  //-------------------------------------------------------------------------
  /**
   * Obtains credit rates provider based on the specified market data.
   * <p>
   * This provides {@link CreditRatesProvider} suitable for pricing credit products.
   * Although this method can be used directly, it is typically invoked indirectly
   * via {@link CreditRatesMarketData}:
   * <pre>
   *  // bind the baseData to this lookup
   *  CreditRatesMarketData view = lookup.marketView(baseData);
   *  
   *  // pass around CreditRatesMarketData within the function to use in pricing
   *  CreditRatesProvider provider = view.creditRatesProvider();
   * </pre>
   * 
   * @param marketData  the complete set of market data for one scenario
   * @return the rates provider
   */
  public abstract CreditRatesProvider creditRatesProvider(MarketData marketData);

}
