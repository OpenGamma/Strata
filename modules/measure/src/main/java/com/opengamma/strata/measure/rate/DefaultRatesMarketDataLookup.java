/*
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.measure.rate;

import static com.opengamma.strata.collect.Guavate.toImmutableSet;

import java.io.Serializable;
import java.util.Map;
import java.util.Set;

import org.joda.beans.BeanDefinition;
import org.joda.beans.ImmutableBean;
import org.joda.beans.JodaBeanUtils;
import org.joda.beans.MetaBean;
import org.joda.beans.Property;
import org.joda.beans.PropertyDefinition;
import org.joda.beans.impl.light.LightMetaBean;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import com.opengamma.strata.basics.currency.Currency;
import com.opengamma.strata.basics.currency.FxRateProvider;
import com.opengamma.strata.basics.index.Index;
import com.opengamma.strata.calc.CalculationRules;
import com.opengamma.strata.calc.runner.CalculationParameter;
import com.opengamma.strata.calc.runner.FunctionRequirements;
import com.opengamma.strata.collect.Messages;
import com.opengamma.strata.data.MarketData;
import com.opengamma.strata.data.MarketDataId;
import com.opengamma.strata.data.ObservableId;
import com.opengamma.strata.data.ObservableSource;
import com.opengamma.strata.data.scenario.ScenarioMarketData;
import com.opengamma.strata.market.curve.CurveId;
import com.opengamma.strata.market.observable.IndexQuoteId;
import com.opengamma.strata.pricer.rate.RatesProvider;

/**
 * The rates lookup, used to select curves for pricing.
 * <p>
 * This provides access to discount curves and forward curves.
 * <p>
 * The lookup implements {@link CalculationParameter} and is used by passing it
 * as an argument to {@link CalculationRules}. It provides the link between the
 * data that the function needs and the data that is available in {@link ScenarioMarketData}.
 */
@BeanDefinition(style = "light")
final class DefaultRatesMarketDataLookup
    implements RatesMarketDataLookup, ImmutableBean, Serializable {

  /**
   * The discount curves in the group, keyed by currency.
   */
  @PropertyDefinition(validate = "notNull")
  private final ImmutableMap<Currency, CurveId> discountCurves;
  /**
   * The forward curves in the group, keyed by index.
   */
  @PropertyDefinition(validate = "notNull", builderType = "Map<? extends Index, CurveId>")
  private final ImmutableMap<Index, CurveId> forwardCurves;
  /**
   * The source of market data for quotes and other observable market data.
   */
  @PropertyDefinition(validate = "notNull")
  private final ObservableSource observableSource;
  /**
   * The lookup used to obtain {@code FxRateProvider}.
   */
  @PropertyDefinition(validate = "notNull")
  private final FxRateLookup fxLookup;

  //-------------------------------------------------------------------------
  /**
   * Obtains an instance based on a map of discount and forward curve identifiers.
   * <p>
   * The discount and forward curves refer to the curve identifier.
   * The curves themselves are provided in {@link ScenarioMarketData}
   * using {@link CurveId} as the identifier.
   * 
   * @param discountCurveIds  the discount curve identifiers, keyed by currency
   * @param forwardCurveIds  the forward curves identifiers, keyed by index
   * @param obsSource  the source of market data for FX, quotes and other observable market data
   * @param fxLookup  the lookup used to obtain FX rates
   * @return the rates lookup containing the specified curves
   */
  public static DefaultRatesMarketDataLookup of(
      Map<Currency, CurveId> discountCurveIds,
      Map<? extends Index, CurveId> forwardCurveIds,
      ObservableSource obsSource,
      FxRateLookup fxLookup) {

    return new DefaultRatesMarketDataLookup(discountCurveIds, forwardCurveIds, obsSource, fxLookup);
  }

  //-------------------------------------------------------------------------
  @Override
  public ImmutableSet<Currency> getDiscountCurrencies() {
    return discountCurves.keySet();
  }

  @Override
  public ImmutableSet<MarketDataId<?>> getDiscountMarketDataIds(Currency currency) {
    CurveId id = discountCurves.get(currency);
    if (id == null) {
      throw new IllegalArgumentException(msgCurrencyNotFound(currency));
    }
    return ImmutableSet.of(id);
  }

  @Override
  public ImmutableSet<Index> getForwardIndices() {
    return forwardCurves.keySet();
  }

  @Override
  public ImmutableSet<MarketDataId<?>> getForwardMarketDataIds(Index index) {
    CurveId id = forwardCurves.get(index);
    if (id == null) {
      throw new IllegalArgumentException(msgIndexNotFound(index));
    }
    return ImmutableSet.of(id);
  }

  //-------------------------------------------------------------------------
  @Override
  public FunctionRequirements requirements(Set<Currency> currencies, Set<? extends Index> indices) {
    for (Currency currency : currencies) {
      if (!discountCurves.keySet().contains(currency)) {
        throw new IllegalArgumentException(msgCurrencyNotFound(currency));
      }
    }
    for (Index index : indices) {
      if (!forwardCurves.keySet().contains(index)) {
        throw new IllegalArgumentException(msgIndexNotFound(index));
      }
    }

    // keys for time-series
    Set<ObservableId> indexQuoteIds = indices.stream()
        .map(IndexQuoteId::of)
        .collect(toImmutableSet());

    // keys for forward curves
    Set<MarketDataId<?>> indexCurveIds = indices.stream()
        .map(idx -> forwardCurves.get(idx))
        .collect(toImmutableSet());

    // keys for discount factors
    Set<MarketDataId<?>> discountFactorsIds = currencies.stream()
        .map(ccy -> discountCurves.get(ccy))
        .collect(toImmutableSet());

    return FunctionRequirements.builder()
        .valueRequirements(Sets.union(indexCurveIds, discountFactorsIds))
        .timeSeriesRequirements(indexQuoteIds)
        .outputCurrencies(currencies)
        .observableSource(observableSource)
        .build();
  }

  //-------------------------------------------------------------------------
  @Override
  public RatesProvider ratesProvider(MarketData marketData) {
    return DefaultLookupRatesProvider.of(this, marketData);
  }

  @Override
  public FxRateProvider fxRateProvider(MarketData marketData) {
    return fxLookup.fxRateProvider(marketData);
  }

  //-------------------------------------------------------------------------
  String msgCurrencyNotFound(Currency currency) {
    return Messages.format("Rates lookup has no discount curve defined for currency '{}'", currency);
  }

  String msgIndexNotFound(Index index) {
    return Messages.format("Rates lookup has no forward curve defined for index '{}'", index);
  }

  //------------------------- AUTOGENERATED START -------------------------
  ///CLOVER:OFF
  /**
   * The meta-bean for {@code DefaultRatesMarketDataLookup}.
   */
  private static final MetaBean META_BEAN = LightMetaBean.of(DefaultRatesMarketDataLookup.class);

  /**
   * The meta-bean for {@code DefaultRatesMarketDataLookup}.
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

  private DefaultRatesMarketDataLookup(
      Map<Currency, CurveId> discountCurves,
      Map<? extends Index, CurveId> forwardCurves,
      ObservableSource observableSource,
      FxRateLookup fxLookup) {
    JodaBeanUtils.notNull(discountCurves, "discountCurves");
    JodaBeanUtils.notNull(forwardCurves, "forwardCurves");
    JodaBeanUtils.notNull(observableSource, "observableSource");
    JodaBeanUtils.notNull(fxLookup, "fxLookup");
    this.discountCurves = ImmutableMap.copyOf(discountCurves);
    this.forwardCurves = ImmutableMap.copyOf(forwardCurves);
    this.observableSource = observableSource;
    this.fxLookup = fxLookup;
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
   * Gets the discount curves in the group, keyed by currency.
   * @return the value of the property, not null
   */
  public ImmutableMap<Currency, CurveId> getDiscountCurves() {
    return discountCurves;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the forward curves in the group, keyed by index.
   * @return the value of the property, not null
   */
  public ImmutableMap<Index, CurveId> getForwardCurves() {
    return forwardCurves;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the source of market data for quotes and other observable market data.
   * @return the value of the property, not null
   */
  public ObservableSource getObservableSource() {
    return observableSource;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the lookup used to obtain {@code FxRateProvider}.
   * @return the value of the property, not null
   */
  public FxRateLookup getFxLookup() {
    return fxLookup;
  }

  //-----------------------------------------------------------------------
  @Override
  public boolean equals(Object obj) {
    if (obj == this) {
      return true;
    }
    if (obj != null && obj.getClass() == this.getClass()) {
      DefaultRatesMarketDataLookup other = (DefaultRatesMarketDataLookup) obj;
      return JodaBeanUtils.equal(discountCurves, other.discountCurves) &&
          JodaBeanUtils.equal(forwardCurves, other.forwardCurves) &&
          JodaBeanUtils.equal(observableSource, other.observableSource) &&
          JodaBeanUtils.equal(fxLookup, other.fxLookup);
    }
    return false;
  }

  @Override
  public int hashCode() {
    int hash = getClass().hashCode();
    hash = hash * 31 + JodaBeanUtils.hashCode(discountCurves);
    hash = hash * 31 + JodaBeanUtils.hashCode(forwardCurves);
    hash = hash * 31 + JodaBeanUtils.hashCode(observableSource);
    hash = hash * 31 + JodaBeanUtils.hashCode(fxLookup);
    return hash;
  }

  @Override
  public String toString() {
    StringBuilder buf = new StringBuilder(160);
    buf.append("DefaultRatesMarketDataLookup{");
    buf.append("discountCurves").append('=').append(discountCurves).append(',').append(' ');
    buf.append("forwardCurves").append('=').append(forwardCurves).append(',').append(' ');
    buf.append("observableSource").append('=').append(observableSource).append(',').append(' ');
    buf.append("fxLookup").append('=').append(JodaBeanUtils.toString(fxLookup));
    buf.append('}');
    return buf.toString();
  }

  ///CLOVER:ON
  //-------------------------- AUTOGENERATED END --------------------------
}
