/*
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.measure.rate;

import com.opengamma.strata.basics.currency.Currency;
import com.opengamma.strata.basics.currency.FxMatrix;
import com.opengamma.strata.basics.currency.FxRateProvider;
import com.opengamma.strata.data.FxMatrixId;
import com.opengamma.strata.data.FxRateId;
import com.opengamma.strata.data.MarketData;
import com.opengamma.strata.data.ObservableSource;

/**
 * The lookup that provides access to FX rates in market data.
 * <p>
 * The FX rates lookup provides access to FX rates.
 * <p>
 * Implementations of this interface must be immutable.
 */
public interface FxRateLookup {

  /**
   * Obtains the standard instance.
   * <p>
   * This expects the market data to contain instances of {@link FxRateId}
   * based on the default {@link ObservableSource}.
   * Triangulation will use the default of the currency, typically USD.
   * 
   * @return the FX rate lookup
   */
  public static FxRateLookup ofRates() {
    return DefaultFxRateLookup.DEFAULT;
  }

  /**
   * Obtains the standard instance.
   * <p>
   * This expects the market data to contain instances of {@link FxRateId}
   * based on the specified {@link ObservableSource}.
   * Triangulation will use the default of the currency, typically USD.
   * 
   * @param observableSource  the source of observable market data
   * @return the FX rate lookup
   */
  public static FxRateLookup ofRates(ObservableSource observableSource) {
    return new DefaultFxRateLookup(observableSource);
  }

  /**
   * Obtains an instance that uses triangulation on the specified currency.
   * <p>
   * This expects the market data to contain instances of {@link FxRateId}
   * based on the default {@link ObservableSource}.
   * Triangulation will use the specified currency.
   * 
   * @param triangulationCurrency  the triangulation currency
   * @return the FX rate lookup
   */
  public static FxRateLookup ofRates(Currency triangulationCurrency) {
    return new DefaultFxRateLookup(triangulationCurrency, ObservableSource.NONE);
  }

  /**
   * Obtains an instance that uses triangulation on the specified currency.
   * <p>
   * This expects the market data to contain instances of {@link FxRateId}
   * based on the specified {@link ObservableSource}.
   * Triangulation will use the specified currency.
   * 
   * @param triangulationCurrency  the triangulation currency
   * @param observableSource  the source of observable market data
   * @return the FX rate lookup
   */
  public static FxRateLookup ofRates(Currency triangulationCurrency, ObservableSource observableSource) {
    return new DefaultFxRateLookup(triangulationCurrency, observableSource);
  }

  /**
   * Obtains an instance that uses an FX matrix.
   * <p>
   * This expects the market data to contain an instance of {@link FxMatrix}
   * accessed by the standard {@link FxMatrixId}.
   * 
   * @return the FX rate lookup
   */
  public static FxRateLookup ofMatrix() {
    return MatrixFxRateLookup.DEFAULT;
  }

  /**
   * Obtains an instance that uses an FX matrix.
   * <p>
   * This expects the market data to contain an instance of {@link FxMatrix}
   * accessed by the specified {@link FxMatrixId}.
   * 
   * @param matrixId  the FX matrix identifier
   * @return the FX rate lookup
   */
  public static FxRateLookup ofMatrix(FxMatrixId matrixId) {
    return new MatrixFxRateLookup(matrixId);
  }

  //-------------------------------------------------------------------------
  /**
   * Obtains an FX rate provider based on the specified market data.
   * <p>
   * This provides an {@link FxRateProvider} suitable for obtaining FX rates.
   * 
   * @param marketData  the complete set of market data for one scenario
   * @return the FX rate provider
   */
  public abstract FxRateProvider fxRateProvider(MarketData marketData);

}
