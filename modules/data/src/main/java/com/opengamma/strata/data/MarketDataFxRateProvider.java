/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.data;

import java.io.Serializable;
import java.util.Optional;
import java.util.Set;

import org.joda.beans.BeanDefinition;
import org.joda.beans.ImmutableBean;
import org.joda.beans.JodaBeanUtils;
import org.joda.beans.MetaBean;
import org.joda.beans.Property;
import org.joda.beans.PropertyDefinition;
import org.joda.beans.impl.light.LightMetaBean;

import com.opengamma.strata.basics.currency.Currency;
import com.opengamma.strata.basics.currency.FxRate;
import com.opengamma.strata.basics.currency.FxRateProvider;
import com.opengamma.strata.collect.ArgChecker;
import com.opengamma.strata.collect.Messages;

/**
 * Provides FX rates from market data.
 * <p>
 * This decorates an instance of {@link MarketData} to provide FX rates.
 * <p>
 * The FX rates provided are obtained via a triangulation process.
 * To find the FX rate for the currency pair AAA/BBB:
 * <ol>
 * <li>If the rate AAA/BBB is available, return it
 * <li>Find the triangulation currency of AAA (TTA), try to return rate from AAA/TTA and TTA/BBB
 * <li>Find the triangulation currency of BBB (TTB), try to return rate from AAA/TTB and TTB/BBB
 * <li>Find the triangulation currency of AAA (TTA) and BBB (TTB), try to return rate from AAA/TTA, TTA/TTB and TTB/BBB
 * </ol>
 * The triangulation currency can also be specified, which is useful if all
 * FX rates are supplied relative to a currency other than USD.
 */
@BeanDefinition(style = "light")
public final class MarketDataFxRateProvider
    implements FxRateProvider, ImmutableBean, Serializable {

  /**
   * The market data that provides the FX rates.
   */
  @PropertyDefinition(validate = "notNull")
  private final MarketData marketData;
  /**
   * The source of market data for FX rates.
   */
  @PropertyDefinition(validate = "notNull")
  private final ObservableSource fxRatesSource;
  /**
   * The triangulation currency to use.
   * <p>
   * If specified, this currency is used to triangulate FX rates in preference to the standard approach.
   * This would be useful if all FX rates are supplied relative to a currency other than USD.
   */
  @PropertyDefinition(get = "optional")
  private final Currency triangulationCurrency;

  //-------------------------------------------------------------------------
  /**
   * Obtains an instance which takes FX rates from the market data.
   *
   * @param marketData  market data used for looking up FX rates
   * @return the provider
   */
  public static MarketDataFxRateProvider of(MarketData marketData) {
    return of(marketData, ObservableSource.NONE);
  }

  /**
   * Obtains an instance which takes FX rates from the market data,
   * specifying the source of FX rates.
   * <p>
   * The source of FX rates is rarely needed, as most applications only need one set of FX rates.
   *
   * @param marketData  market data used for looking up FX rates
   * @param fxRatesSource  the source of market data for FX rates
   * @return the provider
   */
  public static MarketDataFxRateProvider of(MarketData marketData, ObservableSource fxRatesSource) {
    return new MarketDataFxRateProvider(marketData, fxRatesSource, null);
  }

  /**
   * Obtains an instance which takes FX rates from the market data,
   * specifying the source of FX rates.
   * <p>
   * The source of FX rates is rarely needed, as most applications only need one set of FX rates.
   *
   * @param marketData  market data used for looking up FX rates
   * @param fxRatesSource  the source of market data for FX rates
   * @param triangulationCurrency  the triangulation currency to use
   * @return the provider
   */
  public static MarketDataFxRateProvider of(
      MarketData marketData,
      ObservableSource fxRatesSource,
      Currency triangulationCurrency) {

    ArgChecker.notNull(triangulationCurrency, "triangulationCurrency");
    return new MarketDataFxRateProvider(marketData, fxRatesSource, triangulationCurrency);
  }

  //-------------------------------------------------------------------------
  @Override
  public double fxRate(Currency baseCurrency, Currency counterCurrency) {
    if (baseCurrency.equals(counterCurrency)) {
      return 1;
    }
    // Try direct pair
    Optional<FxRate> rate = marketData.findValue(FxRateId.of(baseCurrency, counterCurrency, fxRatesSource));
    if (rate.isPresent()) {
      return rate.get().fxRate(baseCurrency, counterCurrency);
    }
    // try specified triangulation currency
    if (triangulationCurrency != null) {
      Optional<FxRate> rateBase1 = marketData.findValue(FxRateId.of(baseCurrency, triangulationCurrency, fxRatesSource));
      Optional<FxRate> rateBase2 = marketData.findValue(FxRateId.of(triangulationCurrency, counterCurrency, fxRatesSource));
      if (rateBase1.isPresent() && rateBase2.isPresent()) {
        return rateBase1.get().crossRate(rateBase2.get()).fxRate(baseCurrency, counterCurrency);
      }
    }
    // Try triangulation on base currency
    Currency triangularBaseCcy = baseCurrency.getTriangulationCurrency();
    Optional<FxRate> rateBase1 = marketData.findValue(FxRateId.of(baseCurrency, triangularBaseCcy, fxRatesSource));
    Optional<FxRate> rateBase2 = marketData.findValue(FxRateId.of(triangularBaseCcy, counterCurrency, fxRatesSource));
    if (rateBase1.isPresent() && rateBase2.isPresent()) {
      return rateBase1.get().crossRate(rateBase2.get()).fxRate(baseCurrency, counterCurrency);
    }
    // Try triangulation on counter currency
    Currency triangularCounterCcy = counterCurrency.getTriangulationCurrency();
    Optional<FxRate> rateCounter1 = marketData.findValue(FxRateId.of(baseCurrency, triangularCounterCcy, fxRatesSource));
    Optional<FxRate> rateCounter2 = marketData.findValue(FxRateId.of(triangularCounterCcy, counterCurrency, fxRatesSource));
    if (rateCounter1.isPresent() && rateCounter2.isPresent()) {
      return rateCounter1.get().crossRate(rateCounter2.get()).fxRate(baseCurrency, counterCurrency);
    }
    // Double triangulation
    if (rateBase1.isPresent() && rateCounter2.isPresent()) {
      Optional<FxRate> rateTriangular2 =
          marketData.findValue(FxRateId.of(triangularBaseCcy, triangularCounterCcy, fxRatesSource));
      if (rateTriangular2.isPresent()) {
        return rateBase1.get().crossRate(rateTriangular2.get()).crossRate(rateCounter2.get())
            .fxRate(baseCurrency, counterCurrency);
      }
    }
    if (fxRatesSource.equals(ObservableSource.NONE)) {
      throw new MarketDataNotFoundException(Messages.format(
          "No FX rate market data for {}/{}", baseCurrency, counterCurrency));
    }
    throw new MarketDataNotFoundException(Messages.format(
        "No FX rate market data for {}/{} using source '{}'", baseCurrency, counterCurrency, fxRatesSource));
  }

  //------------------------- AUTOGENERATED START -------------------------
  ///CLOVER:OFF
  /**
   * The meta-bean for {@code MarketDataFxRateProvider}.
   */
  private static final MetaBean META_BEAN = LightMetaBean.of(MarketDataFxRateProvider.class);

  /**
   * The meta-bean for {@code MarketDataFxRateProvider}.
   * @return the meta-bean, not null
   */
  public static MetaBean meta() {
    return META_BEAN;
  }

  static {
    JodaBeanUtils.registerMetaBean(META_BEAN);
  }

  /**
   * The serialization version id.
   */
  private static final long serialVersionUID = 1L;

  private MarketDataFxRateProvider(
      MarketData marketData,
      ObservableSource fxRatesSource,
      Currency triangulationCurrency) {
    JodaBeanUtils.notNull(marketData, "marketData");
    JodaBeanUtils.notNull(fxRatesSource, "fxRatesSource");
    this.marketData = marketData;
    this.fxRatesSource = fxRatesSource;
    this.triangulationCurrency = triangulationCurrency;
  }

  @Override
  public MetaBean metaBean() {
    return META_BEAN;
  }

  @Override
  public <R> Property<R> property(String propertyName) {
    return metaBean().<R>metaProperty(propertyName).createProperty(this);
  }

  @Override
  public Set<String> propertyNames() {
    return metaBean().metaPropertyMap().keySet();
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the market data that provides the FX rates.
   * @return the value of the property, not null
   */
  public MarketData getMarketData() {
    return marketData;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the source of market data for FX rates.
   * @return the value of the property, not null
   */
  public ObservableSource getFxRatesSource() {
    return fxRatesSource;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the triangulation currency to use.
   * <p>
   * If specified, this currency is used to triangulate FX rates in preference to the standard approach.
   * This would be useful if all FX rates are supplied relative to a currency other than USD.
   * @return the optional value of the property, not null
   */
  public Optional<Currency> getTriangulationCurrency() {
    return Optional.ofNullable(triangulationCurrency);
  }

  //-----------------------------------------------------------------------
  @Override
  public boolean equals(Object obj) {
    if (obj == this) {
      return true;
    }
    if (obj != null && obj.getClass() == this.getClass()) {
      MarketDataFxRateProvider other = (MarketDataFxRateProvider) obj;
      return JodaBeanUtils.equal(marketData, other.marketData) &&
          JodaBeanUtils.equal(fxRatesSource, other.fxRatesSource) &&
          JodaBeanUtils.equal(triangulationCurrency, other.triangulationCurrency);
    }
    return false;
  }

  @Override
  public int hashCode() {
    int hash = getClass().hashCode();
    hash = hash * 31 + JodaBeanUtils.hashCode(marketData);
    hash = hash * 31 + JodaBeanUtils.hashCode(fxRatesSource);
    hash = hash * 31 + JodaBeanUtils.hashCode(triangulationCurrency);
    return hash;
  }

  @Override
  public String toString() {
    StringBuilder buf = new StringBuilder(128);
    buf.append("MarketDataFxRateProvider{");
    buf.append("marketData").append('=').append(marketData).append(',').append(' ');
    buf.append("fxRatesSource").append('=').append(fxRatesSource).append(',').append(' ');
    buf.append("triangulationCurrency").append('=').append(JodaBeanUtils.toString(triangulationCurrency));
    buf.append('}');
    return buf.toString();
  }

  ///CLOVER:ON
  //-------------------------- AUTOGENERATED END --------------------------
}
