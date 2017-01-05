package com.opengamma.strata.measure.credit;

import java.util.Map;

import com.opengamma.strata.basics.StandardId;
import com.opengamma.strata.basics.currency.Currency;
import com.opengamma.strata.calc.runner.CalculationParameter;
import com.opengamma.strata.calc.runner.CalculationParameters;
import com.opengamma.strata.calc.runner.FunctionRequirements;
import com.opengamma.strata.collect.tuple.Pair;
import com.opengamma.strata.data.MarketData;
import com.opengamma.strata.data.ObservableSource;
import com.opengamma.strata.data.scenario.ScenarioMarketData;
import com.opengamma.strata.market.curve.CurveId;
import com.opengamma.strata.measure.swaption.SwaptionMarketData;
import com.opengamma.strata.pricer.credit.CreditRatesProvider;
import com.opengamma.strata.pricer.swaption.SwaptionVolatilities;

public interface CreditRatesMarketDataLookup extends CalculationParameter {

  /**
   * Obtains an instance based on a single mapping from index to volatility identifier.
   * <p>
   * The lookup provides volatilities for the specified index.
   *
   * @param index  the Ibor index
   * @param volatilityId  the volatility identifier
   * @return the swaption lookup containing the specified mapping
   */
  public static CreditRatesMarketDataLookup of(
      Map<Pair<StandardId, Currency>, CurveId> creditCurveIds,
      Map<Currency, CurveId> discountCurveIds,
      Map<StandardId, CurveId> recoveryRateCurveIds) {
//    return DefaultSwaptionMarketDataLookup.of(ImmutableMap.of(index, volatilityId));
    return null;
  }

  /**
   * Obtains an instance based on a map of volatility identifiers.
   * <p>
   * The map is used to specify the appropriate volatilities to use for each index.
   *
   * @param volatilityIds  the volatility identifiers, keyed by index
   * @return the swaption lookup containing the specified volatilities
   */
  public static CreditRatesMarketDataLookup of(
      Map<Pair<StandardId, Currency>, CurveId> creditCurveIds,
      Map<Currency, CurveId> discountCurveIds,
      Map<StandardId, CurveId> recoveryRateCurveIds,
      ObservableSource observableSource) {
//  return DefaultSwaptionMarketDataLookup.of(ImmutableMap.of(index, volatilityId));
    return null;
  }

  //-------------------------------------------------------------------------
  /**
   * Gets the type that the lookup will be queried by.
   * <p>
   * This returns {@code SwaptionMarketLookup.class}.
   * When querying parameters using {@link CalculationParameters#findParameter(Class)},
   * {@code SwaptionMarketLookup.class} must be passed in to find the instance.
   * 
   * @return the type of the parameter implementation
   */
  @Override
  default Class<? extends CalculationParameter> queryType() {
    return CreditRatesMarketDataLookup.class;
  }

  //-------------------------------------------------------------------------
  /**
   * Creates market data requirements for the specified indices.
   * 
   * @param indices  the indices, for which volatilities are required
   * @return the requirements
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
//    return DefaultSwaptionScenarioMarketData.of(this, marketData);
    return null;
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
  public default CreditRatesScenarioMarketData marketDataView(MarketData marketData) {
//    return DefaultSwaptionMarketData.of(this, marketData);
    return null;
  }

  //-------------------------------------------------------------------------
  /**
   * Obtains swaption volatilities based on the specified market data.
   * <p>
   * This provides {@link SwaptionVolatilities} suitable for pricing a swaption.
   * Although this method can be used directly, it is typically invoked indirectly
   * via {@link SwaptionMarketData}:
   * <pre>
   *  // bind the baseData to this lookup
   *  SwaptionMarketData view = lookup.marketDataView(baseData);
   *  
   *  // pass around SwaptionMarketData within the function to use in pricing
   *  SwaptionVolatilities vols = view.volatilities(index);
   * </pre>
   * 
   * @param index  the Ibor index
   * @param marketData  the complete set of market data for one scenario
   * @return the volatilities
   */
  public abstract CreditRatesProvider creditRatesProvider(MarketData marketData);

}
