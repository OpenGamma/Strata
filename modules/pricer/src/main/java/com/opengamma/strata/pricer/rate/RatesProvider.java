/**
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.rate;

import java.util.Optional;
import java.util.Set;

import com.opengamma.strata.basics.currency.CurrencyPair;
import com.opengamma.strata.basics.currency.MultiCurrencyAmount;
import com.opengamma.strata.basics.index.FxIndex;
import com.opengamma.strata.basics.index.IborIndex;
import com.opengamma.strata.basics.index.Index;
import com.opengamma.strata.basics.index.OvernightIndex;
import com.opengamma.strata.basics.index.PriceIndex;
import com.opengamma.strata.collect.timeseries.LocalDateDoubleTimeSeries;
import com.opengamma.strata.data.MarketDataName;
import com.opengamma.strata.market.curve.Curve;
import com.opengamma.strata.market.curve.CurveName;
import com.opengamma.strata.market.param.CurrencyParameterSensitivities;
import com.opengamma.strata.market.sensitivity.PointSensitivities;
import com.opengamma.strata.market.sensitivity.PointSensitivity;
import com.opengamma.strata.pricer.BaseProvider;
import com.opengamma.strata.pricer.DiscountFactors;
import com.opengamma.strata.pricer.ZeroRateSensitivity;
import com.opengamma.strata.pricer.fx.FxForwardRates;
import com.opengamma.strata.pricer.fx.FxForwardSensitivity;
import com.opengamma.strata.pricer.fx.FxIndexRates;
import com.opengamma.strata.pricer.fx.FxIndexSensitivity;

/**
 * A provider of rates, such as Ibor and Overnight, used for pricing financial instruments.
 * <p>
 * This provides the environmental information against which pricing occurs.
 * The valuation date, FX rates, discount factors, time-series and forward curves are included.
 * <p>
 * All implementations of this interface must be immutable and thread-safe.
 */
public interface RatesProvider
    extends BaseProvider {

  /**
   * Gets the set of Ibor indices that are available.
   *
   * @return the set of Ibor indices
   */
  public abstract Set<IborIndex> getIborIndices();

  /**
   * Gets the set of Overnight indices that are available.
   *
   * @return the set of Overnight indices
   */
  public abstract Set<OvernightIndex> getOvernightIndices();

  /**
   * Gets the set of Price indices that are available.
   *
   * @return the set of Price indices
   */
  public abstract Set<PriceIndex> getPriceIndices();

  //-------------------------------------------------------------------------
  /**
   * Gets the rates for an FX index.
   * <p>
   * This returns an object that can provide historic and forward rates for the specified index.
   * <p>
   * An FX rate is the conversion rate between two currencies. An FX index is the rate
   * as published by a specific organization, typically at a well-known time-of-day.
   * 
   * @param index  the index to find rates for
   * @return the rates for the specified index
   * @throws IllegalArgumentException if the rates are not available
   */
  public abstract FxIndexRates fxIndexRates(FxIndex index);

  //-------------------------------------------------------------------------
  /**
   * Gets the forward FX rates for a currency pair.
   * <p>
   * This returns an object that can provide forward rates for the specified currency pair.
   * See {@link #fxIndexRates(FxIndex)} for forward rates with daily fixings.
   * 
   * @param currencyPair  the currency pair to find forward rates for
   * @return the forward rates for the specified currency pair
   * @throws IllegalArgumentException if the rates are not available
   */
  public abstract FxForwardRates fxForwardRates(CurrencyPair currencyPair);

  //-------------------------------------------------------------------------
  /**
   * Gets the rates for an Ibor index.
   * <p>
   * The rate of the Ibor index, such as 'GBP-LIBOR-3M', varies over time.
   * This returns an object that can provide historic and forward rates for the specified index.
   * 
   * @param index  the index to find rates for
   * @return the rates for the specified index
   * @throws IllegalArgumentException if the rates are not available
   */
  public abstract IborIndexRates iborIndexRates(IborIndex index);

  //-------------------------------------------------------------------------
  /**
   * Gets the rates for an Overnight index.
   * <p>
   * The rate of the Overnight index, such as 'EUR-EONIA', varies over time.
   * This returns an object that can provide historic and forward rates for the specified index.
   * 
   * @param index  the index to find rates for
   * @return the rates for the specified index
   * @throws IllegalArgumentException if the rates are not available
   */
  public abstract OvernightIndexRates overnightIndexRates(OvernightIndex index);

  //-------------------------------------------------------------------------
  /**
   * Gets the values for an Price index.
   * <p>
   * The value of the Price index, such as 'US-CPI-U', varies over time.
   * This returns an object that can provide historic and forward values for the specified index.
   * 
   * @param index  the index to find values for
   * @return the values for the specified index
   * @throws IllegalArgumentException if the values are not available
   */
  public abstract PriceIndexValues priceIndexValues(PriceIndex index);

  //-------------------------------------------------------------------------
  /**
   * Computes the parameter sensitivity.
   * <p>
   * This computes the {@link CurrencyParameterSensitivities} associated with the {@link PointSensitivities}.
   * This corresponds to the projection of the point sensitivity to the internal parameters representation.
   * <p>
   * For example, the point sensitivities could represent the sensitivity to a date on the first
   * of each month in a year relative to a specific forward curve. This method converts to the point
   * sensitivities to be relative to each parameter on the underlying curve, such as the 1 day, 1 week,
   * 1 month, 3 month, 12 month and 5 year nodal points.
   * 
   * @param pointSensitivities  the point sensitivities
   * @return the sensitivity to the curve parameters
   */
  public default CurrencyParameterSensitivities parameterSensitivity(PointSensitivities pointSensitivities) {
    CurrencyParameterSensitivities sens = CurrencyParameterSensitivities.empty();
    for (PointSensitivity point : pointSensitivities.getSensitivities()) {
      if (point instanceof ZeroRateSensitivity) {
        ZeroRateSensitivity pt = (ZeroRateSensitivity) point;
        DiscountFactors factors = discountFactors(pt.getCurveCurrency());
        sens = sens.combinedWith(factors.parameterSensitivity(pt));

      } else if (point instanceof IborRateSensitivity) {
        IborRateSensitivity pt = (IborRateSensitivity) point;
        IborIndexRates rates = iborIndexRates(pt.getIndex());
        sens = sens.combinedWith(rates.parameterSensitivity(pt));

      } else if (point instanceof OvernightRateSensitivity) {
        OvernightRateSensitivity pt = (OvernightRateSensitivity) point;
        OvernightIndexRates rates = overnightIndexRates(pt.getIndex());
        sens = sens.combinedWith(rates.parameterSensitivity(pt));

      } else if (point instanceof FxIndexSensitivity) {
        FxIndexSensitivity pt = (FxIndexSensitivity) point;
        FxIndexRates rates = fxIndexRates(pt.getIndex());
        sens = sens.combinedWith(rates.parameterSensitivity(pt));

      } else if (point instanceof InflationRateSensitivity) {
        InflationRateSensitivity pt = (InflationRateSensitivity) point;
        PriceIndexValues rates = priceIndexValues(pt.getIndex());
        sens = sens.combinedWith(rates.parameterSensitivity(pt));

      } else if (point instanceof FxForwardSensitivity) {
        FxForwardSensitivity pt = (FxForwardSensitivity) point;
        FxForwardRates rates = fxForwardRates(pt.getCurrencyPair());
        sens = sens.combinedWith(rates.parameterSensitivity(pt));
      }
    }
    return sens;
  }

  /**
   * Computes the currency exposure.
   * <p>
   * This computes the currency exposure in the form of a {@link MultiCurrencyAmount} associated with the 
   * {@link PointSensitivities}. This corresponds to the projection of the point sensitivity to the
   * currency exposure associated to an {@link FxIndexSensitivity}.
   * <p>
   * For example, the point sensitivities could represent the sensitivity to a FX Index.
   * This method produces the implicit currency exposure embedded in the FX index sensitivity.
   * <p>
   * Reference: Currency Exposure and FX index, OpenGamma Documentation 32, July 2015.
   * 
   * @param pointSensitivities  the point sensitivities
   * @return the currency exposure
   */
  public default MultiCurrencyAmount currencyExposure(PointSensitivities pointSensitivities) {
    MultiCurrencyAmount ce = MultiCurrencyAmount.empty();
    for (PointSensitivity point : pointSensitivities.getSensitivities()) {
      if (point instanceof FxIndexSensitivity) {
        FxIndexSensitivity pt = (FxIndexSensitivity) point;
        FxIndexRates rates = fxIndexRates(pt.getIndex());
        ce = ce.plus(rates.currencyExposure(pt));
      }
      if (point instanceof FxForwardSensitivity) {
        FxForwardSensitivity pt = (FxForwardSensitivity) point;
        pt = (FxForwardSensitivity) pt.convertedTo(pt.getReferenceCurrency(), this);
        FxForwardRates rates = fxForwardRates(pt.getCurrencyPair());
        ce = ce.plus(rates.currencyExposure(pt));
      }
    }
    return ce;
  }

  //-------------------------------------------------------------------------
  /**
   * Finds the market data with the specified name.
   * <p>
   * This is most commonly used to find a {@link Curve} using a {@link CurveName}.
   * If the market data cannot be found, empty is returned.
   * 
   * @param <T>  the type of the market data value
   * @param name  the name to find
   * @return the market data value, empty if not found
   */
  public abstract <T> Optional<T> findData(MarketDataName<T> name);

  /**
   * Gets the time series.
   * <p>
   * This returns time series for the index.
   * 
   * @param index  the index
   * @return the time series
   */
  public abstract LocalDateDoubleTimeSeries timeSeries(Index index);

  //-------------------------------------------------------------------------
  /**
   * Converts this provider to an equivalent {@code ImmutableRatesProvider}.
   * 
   * @return the equivalent immutable rates provider
   */
  public abstract ImmutableRatesProvider toImmutableRatesProvider();

}
