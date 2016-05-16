/**
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.function.calculation;

import java.time.LocalDate;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

import org.joda.beans.BeanDefinition;
import org.joda.beans.ImmutableBean;
import org.joda.beans.ImmutableConstructor;
import org.joda.beans.JodaBeanUtils;
import org.joda.beans.MetaBean;
import org.joda.beans.Property;
import org.joda.beans.PropertyDefinition;
import org.joda.beans.impl.light.LightMetaBean;

import com.opengamma.strata.basics.currency.Currency;
import com.opengamma.strata.basics.currency.CurrencyPair;
import com.opengamma.strata.basics.currency.FxRate;
import com.opengamma.strata.basics.currency.FxRateProvider;
import com.opengamma.strata.basics.index.FxIndex;
import com.opengamma.strata.basics.index.IborIndex;
import com.opengamma.strata.basics.index.Index;
import com.opengamma.strata.basics.index.OvernightIndex;
import com.opengamma.strata.basics.index.PriceIndex;
import com.opengamma.strata.basics.market.MarketData;
import com.opengamma.strata.basics.market.MarketDataFxRateProvider;
import com.opengamma.strata.basics.market.MarketDataKey;
import com.opengamma.strata.collect.ArgChecker;
import com.opengamma.strata.collect.timeseries.LocalDateDoubleTimeSeries;
import com.opengamma.strata.market.curve.Curve;
import com.opengamma.strata.market.curve.CurveName;
import com.opengamma.strata.market.curve.InterpolatedNodalCurve;
import com.opengamma.strata.market.id.SimpleCurveId;
import com.opengamma.strata.market.key.IndexRateKey;
import com.opengamma.strata.market.view.DiscountFactors;
import com.opengamma.strata.market.view.DiscountFxForwardRates;
import com.opengamma.strata.market.view.DiscountFxIndexRates;
import com.opengamma.strata.market.view.FxForwardRates;
import com.opengamma.strata.market.view.FxIndexRates;
import com.opengamma.strata.market.view.IborIndexRates;
import com.opengamma.strata.market.view.OvernightIndexRates;
import com.opengamma.strata.market.view.PriceIndexValues;
import com.opengamma.strata.pricer.rate.AbstractRatesProvider;
import com.opengamma.strata.pricer.rate.RatesProvider;

/**
 * A rates provider based on a rates lookup.
 * <p>
 * This uses a {@link DefaultRatesMarketDataLookup} to provide a view on {@link MarketData}.
 */
@BeanDefinition(style = "light")
final class DefaultLookupRatesProvider
    extends AbstractRatesProvider
    implements RatesProvider, ImmutableBean {

  /**
   * The lookup.
   */
  @PropertyDefinition(validate = "notNull")
  private final DefaultRatesMarketDataLookup lookup;
  /**
   * The market data.
   */
  @PropertyDefinition(validate = "notNull")
  private final MarketData marketData;
  /**
   * The FX rate provider.
   */
  private final FxRateProvider fxRateProvider;  // derived

  //-------------------------------------------------------------------------
  /**
   * Obtains an instance based on a lookup and market data.
   * <p>
   * The lookup provides the mapping from currency to discount curve, and from
   * index to forward curve. The curves are in the market data.
   *
   * @param lookup  the lookup
   * @param marketData  the market data
   * @return the rates provider
   */
  public static DefaultLookupRatesProvider of(DefaultRatesMarketDataLookup lookup, MarketData marketData) {
    return new DefaultLookupRatesProvider(lookup, marketData);
  }

  @ImmutableConstructor
  private DefaultLookupRatesProvider(DefaultRatesMarketDataLookup lookup, MarketData marketData) {
    this.lookup = ArgChecker.notNull(lookup, "lookup");
    this.marketData = ArgChecker.notNull(marketData, "marketData");
    this.fxRateProvider = MarketDataFxRateProvider.of(marketData);
  }

  //-------------------------------------------------------------------------
  @Override
  public LocalDate getValuationDate() {
    return marketData.getValuationDate();
  }

  //-------------------------------------------------------------------------
  @Override
  public <T> T data(MarketDataKey<T> key) {
    return marketData.getValue(key);
  }

  //-------------------------------------------------------------------------
  @Override
  public Optional<Curve> findCurve(CurveName name) {
    return Stream.concat(lookup.getDiscountCurves().values().stream(), lookup.getForwardCurves().values().stream())
        .filter(id -> id.getCurveName().equals(name))
        .findFirst()
        .flatMap(id -> marketData.findValue(id));
  }

  @Override
  public LocalDateDoubleTimeSeries timeSeries(Index index) {
    return marketData.getTimeSeries(IndexRateKey.of(index));
  }

  //-------------------------------------------------------------------------
  @Override
  public double fxRate(Currency baseCurrency, Currency counterCurrency) {
    return fxRateProvider.fxRate(baseCurrency, counterCurrency);
  }

  //-------------------------------------------------------------------------
  @Override
  public DiscountFactors discountFactors(Currency currency) {
    SimpleCurveId curveId = lookup.getDiscountCurves().get(currency);
    if (curveId == null) {
      throw new IllegalArgumentException(lookup.msgCurrencyNotFound(currency));
    }
    Curve curve = marketData.getValue(curveId);
    return DiscountFactors.of(currency, getValuationDate(), curve);
  }

  //-------------------------------------------------------------------------
  @Override
  public FxIndexRates fxIndexRates(FxIndex index) {
    FxForwardRates fxForwardRates = fxForwardRates(index.getCurrencyPair());
    return DiscountFxIndexRates.of(index, fxForwardRates, timeSeries(index));
  }

  //-------------------------------------------------------------------------
  @Override
  public FxForwardRates fxForwardRates(CurrencyPair currencyPair) {
    DiscountFactors base = discountFactors(currencyPair.getBase());
    DiscountFactors counter = discountFactors(currencyPair.getCounter());
    FxRate fxRate = FxRate.of(currencyPair, fxRate(currencyPair));
    return DiscountFxForwardRates.of(currencyPair, fxRate, base, counter);
  };

  //-------------------------------------------------------------------------
  @Override
  public IborIndexRates iborIndexRates(IborIndex index) {
    SimpleCurveId curveId = lookup.getForwardCurves().get(index);
    if (curveId == null) {
      throw new IllegalArgumentException(lookup.msgIndexNotFound(index));
    }
    Curve curve = marketData.getValue(curveId);
    return IborIndexRates.of(index, getValuationDate(), curve, timeSeries(index));
  }

  //-------------------------------------------------------------------------
  @Override
  public OvernightIndexRates overnightIndexRates(OvernightIndex index) {
    SimpleCurveId curveId = lookup.getForwardCurves().get(index);
    if (curveId == null) {
      throw new IllegalArgumentException(lookup.msgIndexNotFound(index));
    }
    Curve curve = marketData.getValue(curveId);
    return OvernightIndexRates.of(index, getValuationDate(), curve, timeSeries(index));
  }

  //-------------------------------------------------------------------------
  @Override
  public PriceIndexValues priceIndexValues(PriceIndex index) {
    SimpleCurveId curveId = lookup.getForwardCurves().get(index);
    if (curveId == null) {
      throw new IllegalArgumentException(lookup.msgIndexNotFound(index));
    }
    Curve curve = marketData.getValue(curveId);
    if (!(curve instanceof InterpolatedNodalCurve)) {
      throw new IllegalArgumentException("Curve must be an InterpolatedNodalCurve: " + index);
    }
    return PriceIndexValues.of(index, getValuationDate(), (InterpolatedNodalCurve) curve, timeSeries(index));
  }

  //------------------------- AUTOGENERATED START -------------------------
  ///CLOVER:OFF
  /**
   * The meta-bean for {@code DefaultLookupRatesProvider}.
   */
  private static MetaBean META_BEAN = LightMetaBean.of(DefaultLookupRatesProvider.class);

  /**
   * The meta-bean for {@code DefaultLookupRatesProvider}.
   * @return the meta-bean, not null
   */
  public static MetaBean meta() {
    return META_BEAN;
  }

  static {
    JodaBeanUtils.registerMetaBean(META_BEAN);
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
   * Gets the lookup.
   * @return the value of the property, not null
   */
  public DefaultRatesMarketDataLookup getLookup() {
    return lookup;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the market data.
   * @return the value of the property, not null
   */
  public MarketData getMarketData() {
    return marketData;
  }

  //-----------------------------------------------------------------------
  @Override
  public boolean equals(Object obj) {
    if (obj == this) {
      return true;
    }
    if (obj != null && obj.getClass() == this.getClass()) {
      DefaultLookupRatesProvider other = (DefaultLookupRatesProvider) obj;
      return JodaBeanUtils.equal(lookup, other.lookup) &&
          JodaBeanUtils.equal(marketData, other.marketData);
    }
    return false;
  }

  @Override
  public int hashCode() {
    int hash = getClass().hashCode();
    hash = hash * 31 + JodaBeanUtils.hashCode(lookup);
    hash = hash * 31 + JodaBeanUtils.hashCode(marketData);
    return hash;
  }

  @Override
  public String toString() {
    StringBuilder buf = new StringBuilder(96);
    buf.append("DefaultLookupRatesProvider{");
    buf.append("lookup").append('=').append(lookup).append(',').append(' ');
    buf.append("marketData").append('=').append(JodaBeanUtils.toString(marketData));
    buf.append('}');
    return buf.toString();
  }

  ///CLOVER:ON
  //-------------------------- AUTOGENERATED END --------------------------
}
