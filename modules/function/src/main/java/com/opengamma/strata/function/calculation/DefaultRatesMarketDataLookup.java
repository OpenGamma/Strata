/**
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.function.calculation;

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
import com.opengamma.strata.basics.index.Index;
import com.opengamma.strata.basics.market.MarketData;
import com.opengamma.strata.basics.market.MarketDataKey;
import com.opengamma.strata.basics.market.ObservableKey;
import com.opengamma.strata.calc.CalculationRules;
import com.opengamma.strata.calc.marketdata.CalculationEnvironment;
import com.opengamma.strata.calc.marketdata.CalculationMarketData;
import com.opengamma.strata.calc.marketdata.FunctionRequirements;
import com.opengamma.strata.calc.runner.CalculationParameter;
import com.opengamma.strata.collect.Messages;
import com.opengamma.strata.market.id.SimpleCurveId;
import com.opengamma.strata.market.key.IndexRateKey;
import com.opengamma.strata.pricer.rate.RatesProvider;

/**
 * The rates lookup, used to select curves for pricing.
 * <p>
 * This provides access to discount curves and forward curves.
 * <p>
 * The lookup implements {@link CalculationParameter} and is used by passing it
 * as an argument to {@link CalculationRules}. It provides the link between the
 * data that the function needs and the data that is available in {@link CalculationMarketData}.
 */
@BeanDefinition(style = "light")
final class DefaultRatesMarketDataLookup
    implements RatesMarketDataLookup, ImmutableBean, Serializable {

  /**
   * The discount curves in the group, keyed by currency.
   */
  @PropertyDefinition(validate = "notNull")
  private final ImmutableMap<Currency, SimpleCurveId> discountCurves;
  /**
   * The forward curves in the group, keyed by index.
   */
  @PropertyDefinition(validate = "notNull", builderType = "Map<? extends Index, SimpleCurveId>")
  private final ImmutableMap<Index, SimpleCurveId> forwardCurves;

  //-------------------------------------------------------------------------
  /**
   * Obtains an instance based on a map of discount and forward curve identifiers.
   * <p>
   * The discount and forward curves refer to the curve identifier.
   * The curves themselves are provided in {@link CalculationEnvironment}
   * using {@link SimpleCurveId} as the identifier.
   * 
   * @param discountCurveIds  the discount curve identifiers, keyed by currency
   * @param forwardCurveIds  the forward curves identifiers, keyed by index
   * @return the rates lookup containing the specified curves
   */
  public static DefaultRatesMarketDataLookup of(
      Map<Currency, SimpleCurveId> discountCurveIds,
      Map<? extends Index, SimpleCurveId> forwardCurveIds) {

    return new DefaultRatesMarketDataLookup(discountCurveIds, forwardCurveIds);
  }

  //-------------------------------------------------------------------------
  @Override
  public ImmutableSet<Currency> getDiscountCurrencies() {
    return discountCurves.keySet();
  }

  @Override
  public ImmutableSet<MarketDataKey<?>> getDiscountMarketDataIds(Currency currency) {
    SimpleCurveId id = discountCurves.get(currency);
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
  public ImmutableSet<MarketDataKey<?>> getForwardMarketDataIds(Index index) {
    SimpleCurveId id = forwardCurves.get(index);
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
    Set<ObservableKey> indexRateKeys = indices.stream()
        .map(IndexRateKey::of)
        .collect(toImmutableSet());

    // keys for forward curves
    Set<MarketDataKey<?>> indexCurveKeys = indices.stream()
        .map(idx -> forwardCurves.get(idx))
        .collect(toImmutableSet());

    // keys for discount factors
    Set<MarketDataKey<?>> discountFactorsKeys = currencies.stream()
        .map(ccy -> discountCurves.get(ccy))
        .collect(toImmutableSet());

    return FunctionRequirements.builder()
        .singleValueRequirements(Sets.union(indexCurveKeys, discountFactorsKeys))
        .timeSeriesRequirements(indexRateKeys)
        .outputCurrencies(currencies)
        .build();
  }

  //-------------------------------------------------------------------------
  @Override
  public RatesProvider ratesProvider(MarketData marketData) {
    return DefaultLookupRatesProvider.of(this, marketData);
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
  private static MetaBean META_BEAN = LightMetaBean.of(DefaultRatesMarketDataLookup.class);

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
      Map<Currency, SimpleCurveId> discountCurves,
      Map<? extends Index, SimpleCurveId> forwardCurves) {
    JodaBeanUtils.notNull(discountCurves, "discountCurves");
    JodaBeanUtils.notNull(forwardCurves, "forwardCurves");
    this.discountCurves = ImmutableMap.copyOf(discountCurves);
    this.forwardCurves = ImmutableMap.copyOf(forwardCurves);
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
  public ImmutableMap<Currency, SimpleCurveId> getDiscountCurves() {
    return discountCurves;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the forward curves in the group, keyed by index.
   * @return the value of the property, not null
   */
  public ImmutableMap<Index, SimpleCurveId> getForwardCurves() {
    return forwardCurves;
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
          JodaBeanUtils.equal(forwardCurves, other.forwardCurves);
    }
    return false;
  }

  @Override
  public int hashCode() {
    int hash = getClass().hashCode();
    hash = hash * 31 + JodaBeanUtils.hashCode(discountCurves);
    hash = hash * 31 + JodaBeanUtils.hashCode(forwardCurves);
    return hash;
  }

  @Override
  public String toString() {
    StringBuilder buf = new StringBuilder(96);
    buf.append("DefaultRatesMarketDataLookup{");
    buf.append("discountCurves").append('=').append(discountCurves).append(',').append(' ');
    buf.append("forwardCurves").append('=').append(JodaBeanUtils.toString(forwardCurves));
    buf.append('}');
    return buf.toString();
  }

  ///CLOVER:ON
  //-------------------------- AUTOGENERATED END --------------------------
}
