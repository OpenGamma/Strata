/*
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.measure.rate;

import static com.opengamma.strata.collect.Guavate.filtering;
import static com.opengamma.strata.collect.Guavate.toImmutableSet;

import java.io.Serializable;
import java.lang.invoke.MethodHandles;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

import org.joda.beans.ImmutableBean;
import org.joda.beans.JodaBeanUtils;
import org.joda.beans.MetaBean;
import org.joda.beans.TypedMetaBean;
import org.joda.beans.gen.BeanDefinition;
import org.joda.beans.gen.ImmutableConstructor;
import org.joda.beans.gen.PropertyDefinition;
import org.joda.beans.impl.light.LightMetaBean;

import com.google.common.collect.ImmutableSet;
import com.opengamma.strata.basics.currency.Currency;
import com.opengamma.strata.basics.currency.CurrencyPair;
import com.opengamma.strata.basics.currency.FxRate;
import com.opengamma.strata.basics.currency.FxRateProvider;
import com.opengamma.strata.basics.index.FxIndex;
import com.opengamma.strata.basics.index.IborIndex;
import com.opengamma.strata.basics.index.Index;
import com.opengamma.strata.basics.index.OvernightIndex;
import com.opengamma.strata.basics.index.PriceIndex;
import com.opengamma.strata.collect.ArgChecker;
import com.opengamma.strata.collect.timeseries.LocalDateDoubleTimeSeries;
import com.opengamma.strata.data.MarketData;
import com.opengamma.strata.data.MarketDataId;
import com.opengamma.strata.data.MarketDataName;
import com.opengamma.strata.data.MarketDataNotFoundException;
import com.opengamma.strata.data.ObservableId;
import com.opengamma.strata.market.curve.Curve;
import com.opengamma.strata.market.curve.CurveId;
import com.opengamma.strata.market.observable.IndexQuoteId;
import com.opengamma.strata.pricer.DiscountFactors;
import com.opengamma.strata.pricer.fx.DiscountFxForwardRates;
import com.opengamma.strata.pricer.fx.ForwardFxIndexRates;
import com.opengamma.strata.pricer.fx.FxForwardRates;
import com.opengamma.strata.pricer.fx.FxIndexRates;
import com.opengamma.strata.pricer.rate.HistoricIborIndexRates;
import com.opengamma.strata.pricer.rate.HistoricOvernightIndexRates;
import com.opengamma.strata.pricer.rate.HistoricPriceIndexValues;
import com.opengamma.strata.pricer.rate.IborIndexRates;
import com.opengamma.strata.pricer.rate.ImmutableRatesProvider;
import com.opengamma.strata.pricer.rate.OvernightIndexRates;
import com.opengamma.strata.pricer.rate.PriceIndexValues;
import com.opengamma.strata.pricer.rate.RatesProvider;

/**
 * A rates provider based on a rates lookup.
 * <p>
 * This uses a {@link DefaultRatesMarketDataLookup} to provide a view on {@link MarketData}.
 */
@BeanDefinition(style = "light")
final class DefaultLookupRatesProvider
    implements RatesProvider, ImmutableBean, Serializable {

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
  private final transient FxRateProvider fxRateProvider;  // derived

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
    this.fxRateProvider = lookup.fxRateProvider(marketData);
  }

  // ensure standard constructor is invoked
  private Object readResolve() {
    return new DefaultLookupRatesProvider(lookup, marketData);
  }

  //-------------------------------------------------------------------------
  @Override
  public LocalDate getValuationDate() {
    return marketData.getValuationDate();
  }

  @Override
  public ImmutableSet<Currency> getDiscountCurrencies() {
    return lookup.getDiscountCurrencies();
  }

  @Override
  public Stream<Index> indices() {
    return lookup.getForwardIndices().stream();
  }

  @Override
  public ImmutableSet<IborIndex> getIborIndices() {
    return lookup.getForwardIndices().stream()
        .flatMap(filtering(IborIndex.class))
        .collect(toImmutableSet());
  }

  @Override
  public ImmutableSet<OvernightIndex> getOvernightIndices() {
    return lookup.getForwardIndices().stream()
        .flatMap(filtering(OvernightIndex.class))
        .collect(toImmutableSet());
  }

  @Override
  public ImmutableSet<PriceIndex> getPriceIndices() {
    return lookup.getForwardIndices().stream()
        .flatMap(filtering(PriceIndex.class))
        .collect(toImmutableSet());
  }

  @Override
  public ImmutableSet<Index> getTimeSeriesIndices() {
    return marketData.getTimeSeriesIds().stream()
        .flatMap(filtering(IndexQuoteId.class))
        .map(id -> id.getIndex())
        .collect(toImmutableSet());
  }

  //-------------------------------------------------------------------------
  @Override
  public <T> T data(MarketDataId<T> key) {
    return marketData.getValue(key);
  }

  //-------------------------------------------------------------------------
  @Override
  public <T> Optional<T> findData(MarketDataName<T> name) {
    return Stream.concat(lookup.getDiscountCurves().values().stream(), lookup.getForwardCurves().values().stream())
        .filter(id -> id.getMarketDataName().equals(name))
        .findFirst()
        .flatMap(id -> marketData.findValue(id))
        .map(v -> name.getMarketDataType().cast(v));
  }

  @Override
  public LocalDateDoubleTimeSeries timeSeries(Index index) {
    return marketData.getTimeSeries(IndexQuoteId.of(index));
  }

  //-------------------------------------------------------------------------
  @Override
  public double fxRate(Currency baseCurrency, Currency counterCurrency) {
    return fxRateProvider.fxRate(baseCurrency, counterCurrency);
  }

  //-------------------------------------------------------------------------
  @Override
  public DiscountFactors discountFactors(Currency currency) {
    CurveId curveId = lookup.getDiscountCurves().get(currency);
    if (curveId == null) {
      throw new MarketDataNotFoundException(lookup.msgCurrencyNotFound(currency));
    }
    Curve curve = marketData.getValue(curveId);
    return DiscountFactors.of(currency, getValuationDate(), curve);
  }

  //-------------------------------------------------------------------------
  @Override
  public FxIndexRates fxIndexRates(FxIndex index) {
    LocalDateDoubleTimeSeries fixings = timeSeries(index);
    FxForwardRates fxForwardRates = fxForwardRates(index.getCurrencyPair());
    return ForwardFxIndexRates.of(index, fxForwardRates, fixings);
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
    CurveId curveId = lookup.getForwardCurves().get(index);
    if (curveId == null) {
      return historicCurve(index);
    }
    return IborIndexRates.of(index, getValuationDate(), marketData.getValue(curveId), timeSeries(index));
  }

  // creates a historic rates instance if index is inactive and time-series is available
  private IborIndexRates historicCurve(IborIndex index) {
    LocalDateDoubleTimeSeries fixings = timeSeries(index);
    if (index.isActive() || fixings.isEmpty()) {
      throw new MarketDataNotFoundException(lookup.msgIndexNotFound(index));
    }
    return HistoricIborIndexRates.of(index, getValuationDate(), fixings);
  }

  //-------------------------------------------------------------------------
  @Override
  public OvernightIndexRates overnightIndexRates(OvernightIndex index) {
    CurveId curveId = lookup.getForwardCurves().get(index);
    if (curveId == null) {
      return historicCurve(index);
    }
    return OvernightIndexRates.of(index, getValuationDate(), marketData.getValue(curveId), timeSeries(index));
  }

  // creates a historic rates instance if index is inactive and time-series is available
  private OvernightIndexRates historicCurve(OvernightIndex index) {
    LocalDateDoubleTimeSeries fixings = timeSeries(index);
    if (index.isActive() || fixings.isEmpty()) {
      throw new MarketDataNotFoundException(lookup.msgIndexNotFound(index));
    }
    return HistoricOvernightIndexRates.of(index, getValuationDate(), fixings);
  }

  //-------------------------------------------------------------------------
  @Override
  public PriceIndexValues priceIndexValues(PriceIndex index) {
    CurveId curveId = lookup.getForwardCurves().get(index);
    if (curveId == null) {
      return historicCurve(index);
    }
    return PriceIndexValues.of(index, getValuationDate(), marketData.getValue(curveId), timeSeries(index));
  }

  // creates a historic rates instance if index is inactive and time-series is available
  private PriceIndexValues historicCurve(PriceIndex index) {
    LocalDateDoubleTimeSeries fixings = timeSeries(index);
    if (index.isActive() || fixings.isEmpty()) {
      throw new MarketDataNotFoundException(lookup.msgIndexNotFound(index));
    }
    return HistoricPriceIndexValues.of(index, getValuationDate(), fixings);
  }

  //-------------------------------------------------------------------------
  @Override
  public ImmutableRatesProvider toImmutableRatesProvider() {
    // discount curves
    Map<Currency, Curve> dscMap = new HashMap<>();
    for (Currency currency : lookup.getDiscountCurrencies()) {
      CurveId curveId = lookup.getDiscountCurves().get(currency);
      if (curveId != null && marketData.containsValue(curveId)) {
        dscMap.put(currency, marketData.getValue(curveId));
      }
    }
    // forward curves
    Map<Index, Curve> fwdMap = new HashMap<>();
    for (Index index : lookup.getForwardIndices()) {
      CurveId curveId = lookup.getForwardCurves().get(index);
      if (curveId != null && marketData.containsValue(curveId)) {
        fwdMap.put(index, marketData.getValue(curveId));
      }
    }
    // time-series
    Map<Index, LocalDateDoubleTimeSeries> tsMap = new HashMap<>();
    for (ObservableId id : marketData.getTimeSeriesIds()) {
      if (id instanceof IndexQuoteId) {
        IndexQuoteId indexId = (IndexQuoteId) id;
        tsMap.put(indexId.getIndex(), marketData.getTimeSeries(id));
      }
    }
    // build result
    return ImmutableRatesProvider.builder(getValuationDate())
        .discountCurves(dscMap)
        .indexCurves(fwdMap)
        .timeSeries(tsMap)
        .fxRateProvider(fxRateProvider)
        .build();
  }

  //------------------------- AUTOGENERATED START -------------------------
  /**
   * The meta-bean for {@code DefaultLookupRatesProvider}.
   */
  private static final TypedMetaBean<DefaultLookupRatesProvider> META_BEAN =
      LightMetaBean.of(
          DefaultLookupRatesProvider.class,
          MethodHandles.lookup(),
          new String[] {
              "lookup",
              "marketData"},
          new Object[0]);

  /**
   * The meta-bean for {@code DefaultLookupRatesProvider}.
   * @return the meta-bean, not null
   */
  public static TypedMetaBean<DefaultLookupRatesProvider> meta() {
    return META_BEAN;
  }

  static {
    MetaBean.register(META_BEAN);
  }

  /**
   * The serialization version id.
   */
  private static final long serialVersionUID = 1L;

  @Override
  public TypedMetaBean<DefaultLookupRatesProvider> metaBean() {
    return META_BEAN;
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
    buf.append("lookup").append('=').append(JodaBeanUtils.toString(lookup)).append(',').append(' ');
    buf.append("marketData").append('=').append(JodaBeanUtils.toString(marketData));
    buf.append('}');
    return buf.toString();
  }

  //-------------------------- AUTOGENERATED END --------------------------
}
