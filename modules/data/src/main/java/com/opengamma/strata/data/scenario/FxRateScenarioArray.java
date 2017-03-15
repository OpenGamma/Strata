/*
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.data.scenario;

import java.io.Serializable;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.stream.Stream;

import org.joda.beans.Bean;
import org.joda.beans.BeanBuilder;
import org.joda.beans.BeanDefinition;
import org.joda.beans.ImmutableBean;
import org.joda.beans.ImmutableValidator;
import org.joda.beans.JodaBeanUtils;
import org.joda.beans.MetaProperty;
import org.joda.beans.Property;
import org.joda.beans.PropertyDefinition;
import org.joda.beans.impl.direct.DirectMetaBean;
import org.joda.beans.impl.direct.DirectMetaProperty;
import org.joda.beans.impl.direct.DirectMetaPropertyMap;
import org.joda.beans.impl.direct.DirectPrivateBeanBuilder;

import com.opengamma.strata.basics.currency.Currency;
import com.opengamma.strata.basics.currency.CurrencyPair;
import com.opengamma.strata.basics.currency.FxRate;
import com.opengamma.strata.collect.Messages;
import com.opengamma.strata.collect.array.DoubleArray;

/**
 * A set of FX rates between two currencies containing rates for multiple scenarios.
 * <p>
 * This represents rates of foreign exchange. The rate 'EUR/USD 1.25' consists of three
 * elements - the base currency 'EUR', the counter currency 'USD' and the rate '1.25'.
 * When performing a conversion a rate of '1.25' means that '1 EUR = 1.25 USD'.
 * <p>
 * The {@link FxRate} class represents a single rate for a currency pair. This class is
 * intended as an efficient way of storing multiple rates for the same currency pair
 * for use in multiple scenarios.
 *
 * @see FxRate
 */
@BeanDefinition(builderScope = "private")
public final class FxRateScenarioArray
    implements ScenarioArray<FxRate>, ImmutableBean, Serializable {

  /**
   * The currency pair.
   * The pair is formed of two parts, the base and the counter.
   * In the pair 'AAA/BBB' the base is 'AAA' and the counter is 'BBB'.
   */
  @PropertyDefinition(validate = "notNull")
  private final CurrencyPair pair;

  /**
   * The rates applicable to the currency pair.
   * One unit of the base currency is exchanged for this amount of the counter currency.
   */
  @PropertyDefinition(validate = "notNull", get = "private")
  private final DoubleArray rates;

  //-------------------------------------------------------------------------
  /**
   * Returns an array of FX rates for a currency pair.
   * <p>
   * The rates are the rates from the base currency to the counter currency
   * as defined by this formula: {@code (1 * baseCurrency = fxRate * counterCurrency)}.
   *
   * @param currencyPair  the currency pair
   * @param rates  the FX rates for the currency pair
   * @return an array of FX rates for a currency pair
   */
  public static FxRateScenarioArray of(CurrencyPair currencyPair, DoubleArray rates) {
    return new FxRateScenarioArray(currencyPair, rates);
  }

  /**
   * Returns an array of FX rates for a currency pair.
   * <p>
   * The rates are the rates from the base currency to the counter currency
   * as defined by this formula: {@code (1 * baseCurrency = fxRate * counterCurrency)}.
   *
   * @param base  the base currency of the pair
   * @param counter  the counter currency of the pair
   * @param rates  the FX rates for the currency pair
   * @return an array of FX rates for a currency pair
   */
  public static FxRateScenarioArray of(Currency base, Currency counter, DoubleArray rates) {
    return new FxRateScenarioArray(CurrencyPair.of(base, counter), rates);
  }

  //-------------------------------------------------------------------------
  @Override
  public int getScenarioCount() {
    return rates.size();
  }

  /**
   * Returns the FX rate for a scenario.
   *
   * @param scenarioIndex  the index of the scenario
   * @return the FX rate for the specified scenario
   * @throws IndexOutOfBoundsException if the index is invalid
   */
  @Override
  public FxRate get(int scenarioIndex) {
    return FxRate.of(pair, rates.get(scenarioIndex));
  }

  @Override
  public Stream<FxRate> stream() {
    return rates.stream().mapToObj(rate -> FxRate.of(pair, rate));
  }

  //-------------------------------------------------------------------------
  /**
   * Returns the FX rate for the specified currency pair and scenario index.
   * <p>
   * The rate returned is the rate from the base currency to the counter currency
   * as defined by this formula: {@code (1 * baseCurrency = fxRate * counterCurrency)}.
   * <p>
   * This will return the rate or inverse rate, or 1 if the two input currencies are the same.
   * <p>
   * This method is more efficient than {@link #get(int)} as it doesn't create an instance
   * of {@link FxRate} for every invocation.
   *
   * @param baseCurrency  the base currency, to convert from
   * @param counterCurrency  the counter currency, to convert to
   * @param scenarioIndex  the index of the scenario for which rates are required
   * @return the FX rate for the currency pair
   * @throws IllegalArgumentException if no FX rate could be found
   */
  public double fxRate(Currency baseCurrency, Currency counterCurrency, int scenarioIndex) {
    if (baseCurrency.equals(pair.getBase()) && counterCurrency.equals(pair.getCounter())) {
      return rates.get(scenarioIndex);
    }
    if (counterCurrency.equals(pair.getBase()) && baseCurrency.equals(pair.getCounter())) {
      return 1d / rates.get(scenarioIndex);
    }
    throw new IllegalArgumentException("Unknown rate: " + baseCurrency + "/" + counterCurrency);
  }

  /**
   * Converts an amount in a currency to an amount in a different currency using this rate.
   * <p>
   * The from and to currencies must be the same as this rate.
   *
   * @param amounts  the amounts in {@code fromCurrency} to convert
   * @param fromCurrency  the currency of the amounts
   * @param toCurrency  the currency into which the amount should be converted
   * @return the amount converted into {@code toCurrency}
   * @throws IllegalArgumentException if one or both input currencies are not in part of this rate
   */
  public DoubleArray convert(DoubleArray amounts, Currency fromCurrency, Currency toCurrency) {
    if (fromCurrency.equals(pair.getBase()) && toCurrency.equals(pair.getCounter())) {
      return amounts.multipliedBy(rates);
    }
    if (toCurrency.equals(pair.getBase()) && fromCurrency.equals(pair.getCounter())) {
      return rates.mapWithIndex((i, v) -> amounts.get(i) / v);
    }
    throw new IllegalArgumentException("Unknown rate: " + fromCurrency + "/" + toCurrency);
  }

  /**
   * Derives a set of FX rates from these rates and another set of rates.
   * <p>
   * For example, given rates for EUR/GBP and EUR/CHF it is possible to derive rates for GBP/CHF.
   * <p>
   * There must be exactly one currency in common between the two currency pairs and
   * each pair must contain two different currencies. The other rates must have the same scenario count
   * as these rates.
   * <p>
   * The returned object contains rates for converting between the two currencies which only appear in
   * one set of rates.
   *
   * @param other  the other rates
   * @return a set of FX rates derived from these rates and the other rates
   */
  public FxRateScenarioArray crossRates(FxRateScenarioArray other) {
    return pair.cross(other.pair).map(cross -> computeCross(other, cross))
        .orElseThrow(() -> new IllegalArgumentException(Messages.format(
            "Unable to cross when no unique common currency: {} and {}", pair, other.pair)));
  }

  private FxRateScenarioArray computeCross(FxRateScenarioArray other, CurrencyPair crossPairAC) {
    // aim is to convert AAA/BBB and BBB/CCC to AAA/CCC
    Currency currA = crossPairAC.getBase();
    Currency currC = crossPairAC.getCounter();
    // given the conventional cross rate pair, order the two rates to match
    boolean crossBaseCurrencyInFx1 = pair.contains(currA);
    FxRateScenarioArray fxABorBA = crossBaseCurrencyInFx1 ? this : other;
    FxRateScenarioArray fxBCorCB = crossBaseCurrencyInFx1 ? other : this;
    // extract the rates, taking the inverse if the pair is in the inverse order
    DoubleArray ratesAB = fxABorBA.getPair().getBase().equals(currA) ? fxABorBA.rates : fxABorBA.rates.map(v -> 1 / v);
    DoubleArray ratesBC = fxBCorCB.getPair().getCounter().equals(currC) ? fxBCorCB.rates : fxBCorCB.rates.map(v -> 1 / v);
    return FxRateScenarioArray.of(crossPairAC, ratesAB.multipliedBy(ratesBC));
  }

  //-------------------------------------------------------------------------
  @ImmutableValidator
  private void validate() {
    if (pair.getBase().equals(pair.getCounter()) && !rates.stream().allMatch(v -> v == 1d)) {
      throw new IllegalArgumentException("Conversion rate between identical currencies must be one");
    }
  }

  //------------------------- AUTOGENERATED START -------------------------
  ///CLOVER:OFF
  /**
   * The meta-bean for {@code FxRateScenarioArray}.
   * @return the meta-bean, not null
   */
  public static FxRateScenarioArray.Meta meta() {
    return FxRateScenarioArray.Meta.INSTANCE;
  }

  static {
    JodaBeanUtils.registerMetaBean(FxRateScenarioArray.Meta.INSTANCE);
  }

  /**
   * The serialization version id.
   */
  private static final long serialVersionUID = 1L;

  private FxRateScenarioArray(
      CurrencyPair pair,
      DoubleArray rates) {
    JodaBeanUtils.notNull(pair, "pair");
    JodaBeanUtils.notNull(rates, "rates");
    this.pair = pair;
    this.rates = rates;
    validate();
  }

  @Override
  public FxRateScenarioArray.Meta metaBean() {
    return FxRateScenarioArray.Meta.INSTANCE;
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
   * Gets the currency pair.
   * The pair is formed of two parts, the base and the counter.
   * In the pair 'AAA/BBB' the base is 'AAA' and the counter is 'BBB'.
   * @return the value of the property, not null
   */
  public CurrencyPair getPair() {
    return pair;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the rates applicable to the currency pair.
   * One unit of the base currency is exchanged for this amount of the counter currency.
   * @return the value of the property, not null
   */
  private DoubleArray getRates() {
    return rates;
  }

  //-----------------------------------------------------------------------
  @Override
  public boolean equals(Object obj) {
    if (obj == this) {
      return true;
    }
    if (obj != null && obj.getClass() == this.getClass()) {
      FxRateScenarioArray other = (FxRateScenarioArray) obj;
      return JodaBeanUtils.equal(pair, other.pair) &&
          JodaBeanUtils.equal(rates, other.rates);
    }
    return false;
  }

  @Override
  public int hashCode() {
    int hash = getClass().hashCode();
    hash = hash * 31 + JodaBeanUtils.hashCode(pair);
    hash = hash * 31 + JodaBeanUtils.hashCode(rates);
    return hash;
  }

  @Override
  public String toString() {
    StringBuilder buf = new StringBuilder(96);
    buf.append("FxRateScenarioArray{");
    buf.append("pair").append('=').append(pair).append(',').append(' ');
    buf.append("rates").append('=').append(JodaBeanUtils.toString(rates));
    buf.append('}');
    return buf.toString();
  }

  //-----------------------------------------------------------------------
  /**
   * The meta-bean for {@code FxRateScenarioArray}.
   */
  public static final class Meta extends DirectMetaBean {
    /**
     * The singleton instance of the meta-bean.
     */
    static final Meta INSTANCE = new Meta();

    /**
     * The meta-property for the {@code pair} property.
     */
    private final MetaProperty<CurrencyPair> pair = DirectMetaProperty.ofImmutable(
        this, "pair", FxRateScenarioArray.class, CurrencyPair.class);
    /**
     * The meta-property for the {@code rates} property.
     */
    private final MetaProperty<DoubleArray> rates = DirectMetaProperty.ofImmutable(
        this, "rates", FxRateScenarioArray.class, DoubleArray.class);
    /**
     * The meta-properties.
     */
    private final Map<String, MetaProperty<?>> metaPropertyMap$ = new DirectMetaPropertyMap(
        this, null,
        "pair",
        "rates");

    /**
     * Restricted constructor.
     */
    private Meta() {
    }

    @Override
    protected MetaProperty<?> metaPropertyGet(String propertyName) {
      switch (propertyName.hashCode()) {
        case 3433178:  // pair
          return pair;
        case 108285843:  // rates
          return rates;
      }
      return super.metaPropertyGet(propertyName);
    }

    @Override
    public BeanBuilder<? extends FxRateScenarioArray> builder() {
      return new FxRateScenarioArray.Builder();
    }

    @Override
    public Class<? extends FxRateScenarioArray> beanType() {
      return FxRateScenarioArray.class;
    }

    @Override
    public Map<String, MetaProperty<?>> metaPropertyMap() {
      return metaPropertyMap$;
    }

    //-----------------------------------------------------------------------
    /**
     * The meta-property for the {@code pair} property.
     * @return the meta-property, not null
     */
    public MetaProperty<CurrencyPair> pair() {
      return pair;
    }

    /**
     * The meta-property for the {@code rates} property.
     * @return the meta-property, not null
     */
    public MetaProperty<DoubleArray> rates() {
      return rates;
    }

    //-----------------------------------------------------------------------
    @Override
    protected Object propertyGet(Bean bean, String propertyName, boolean quiet) {
      switch (propertyName.hashCode()) {
        case 3433178:  // pair
          return ((FxRateScenarioArray) bean).getPair();
        case 108285843:  // rates
          return ((FxRateScenarioArray) bean).getRates();
      }
      return super.propertyGet(bean, propertyName, quiet);
    }

    @Override
    protected void propertySet(Bean bean, String propertyName, Object newValue, boolean quiet) {
      metaProperty(propertyName);
      if (quiet) {
        return;
      }
      throw new UnsupportedOperationException("Property cannot be written: " + propertyName);
    }

  }

  //-----------------------------------------------------------------------
  /**
   * The bean-builder for {@code FxRateScenarioArray}.
   */
  private static final class Builder extends DirectPrivateBeanBuilder<FxRateScenarioArray> {

    private CurrencyPair pair;
    private DoubleArray rates;

    /**
     * Restricted constructor.
     */
    private Builder() {
      super(meta());
    }

    //-----------------------------------------------------------------------
    @Override
    public Object get(String propertyName) {
      switch (propertyName.hashCode()) {
        case 3433178:  // pair
          return pair;
        case 108285843:  // rates
          return rates;
        default:
          throw new NoSuchElementException("Unknown property: " + propertyName);
      }
    }

    @Override
    public Builder set(String propertyName, Object newValue) {
      switch (propertyName.hashCode()) {
        case 3433178:  // pair
          this.pair = (CurrencyPair) newValue;
          break;
        case 108285843:  // rates
          this.rates = (DoubleArray) newValue;
          break;
        default:
          throw new NoSuchElementException("Unknown property: " + propertyName);
      }
      return this;
    }

    @Override
    public FxRateScenarioArray build() {
      return new FxRateScenarioArray(
          pair,
          rates);
    }

    //-----------------------------------------------------------------------
    @Override
    public String toString() {
      StringBuilder buf = new StringBuilder(96);
      buf.append("FxRateScenarioArray.Builder{");
      buf.append("pair").append('=').append(JodaBeanUtils.toString(pair)).append(',').append(' ');
      buf.append("rates").append('=').append(JodaBeanUtils.toString(rates));
      buf.append('}');
      return buf.toString();
    }

  }

  ///CLOVER:ON
  //-------------------------- AUTOGENERATED END --------------------------
}
