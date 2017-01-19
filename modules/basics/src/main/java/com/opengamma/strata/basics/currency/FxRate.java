/**
 * Copyright (C) 2009 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.basics.currency;

import java.io.Serializable;
import java.util.Locale;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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

import com.google.common.math.DoubleMath;
import com.opengamma.strata.collect.ArgChecker;
import com.opengamma.strata.collect.Messages;

/**
 * A single foreign exchange rate between two currencies, such as 'EUR/USD 1.25'.
 * <p>
 * This represents a rate of foreign exchange. The rate 'EUR/USD 1.25' consists of three
 * elements - the base currency 'EUR', the counter currency 'USD' and the rate '1.25'.
 * When performing a conversion a rate of '1.25' means that '1 EUR = 1.25 USD'.
 * <p>
 * See {@link CurrencyPair} for the representation that does not contain a rate.
 * <p>
 * This class is immutable and thread-safe.
 */
@BeanDefinition(builderScope = "private")
public final class FxRate
    implements FxRateProvider, ImmutableBean, Serializable {

  /**
   * Regular expression to parse the textual format.
   */
  private static final Pattern REGEX_FORMAT = Pattern.compile("([A-Z]{3})[/]([A-Z]{3})[ ]([0-9+.-]+)");

  /**
   * The currency pair.
   * The pair is formed of two parts, the base and the counter.
   * In the pair 'AAA/BBB' the base is 'AAA' and the counter is 'BBB'.
   */
  @PropertyDefinition(validate = "notNull")
  private final CurrencyPair pair;
  /**
   * The rate applicable to the currency pair.
   * One unit of the base currency is exchanged for this amount of the counter currency.
   */
  @PropertyDefinition(validate = "ArgChecker.notNegativeOrZero", get = "private")
  private final double rate;

  //-------------------------------------------------------------------------
  /**
   * Obtains an instance from two currencies.
   * <p>
   * The first currency is the base and the second is the counter.
   * The two currencies may be the same, but if they are then the rate must be one.
   * 
   * @param base  the base currency
   * @param counter  the counter currency
   * @param rate  the conversion rate, greater than zero
   * @return the FX rate
   * @throws IllegalArgumentException if the rate is invalid
   */
  public static FxRate of(Currency base, Currency counter, double rate) {
    return new FxRate(CurrencyPair.of(base, counter), rate);
  }

  /**
   * Obtains an instance from a currency pair.
   * <p>
   * The two currencies may be the same, but if they are then the rate must be one.
   * 
   * @param pair  the currency pair
   * @param rate  the conversion rate, greater than zero
   * @return the FX rate
   * @throws IllegalArgumentException if the rate is invalid
   */
  public static FxRate of(CurrencyPair pair, double rate) {
    return new FxRate(pair, rate);
  }

  //-------------------------------------------------------------------------
  /**
   * Parses a rate from a string with format AAA/BBB RATE.
   * <p>
   * The parsed format is '${baseCurrency}/${counterCurrency} ${rate}'.
   * Currency parsing is case insensitive.
   * 
   * @param rateStr  the rate as a string AAA/BBB RATE
   * @return the FX rate
   * @throws IllegalArgumentException if the FX rate cannot be parsed
   */
  public static FxRate parse(String rateStr) {
    ArgChecker.notNull(rateStr, "rateStr");
    Matcher matcher = REGEX_FORMAT.matcher(rateStr.toUpperCase(Locale.ENGLISH));
    if (!matcher.matches()) {
      throw new IllegalArgumentException("Invalid rate: " + rateStr);
    }
    try {
      Currency base = Currency.parse(matcher.group(1));
      Currency counter = Currency.parse(matcher.group(2));
      double rate = Double.parseDouble(matcher.group(3));
      return new FxRate(CurrencyPair.of(base, counter), rate);
    } catch (RuntimeException ex) {
      throw new IllegalArgumentException("Unable to parse rate: " + rateStr, ex);
    }
  }

  //-------------------------------------------------------------------------
  @ImmutableValidator
  private void validate() {
    if (pair.getBase().equals(pair.getCounter()) && rate != 1d) {
      throw new IllegalArgumentException("Conversion rate between identical currencies must be one");
    }
  }

  //-------------------------------------------------------------------------
  /**
   * Gets the inverse rate.
   * <p>
   * The inverse rate has the same currencies but in reverse order.
   * The rate is the reciprocal of the original.
   * 
   * @return the inverse pair
   */
  public FxRate inverse() {
    return new FxRate(pair.inverse(), 1d / rate);
  }

  /**
   * Gets the FX rate for the specified currency pair.
   * <p>
   * The rate returned is the rate from the base currency to the counter currency
   * as defined by this formula: {@code (1 * baseCurrency = fxRate * counterCurrency)}.
   * <p>
   * This will return the rate or inverse rate, or 1 if the two input currencies are the same.
   * 
   * @param baseCurrency  the base currency, to convert from
   * @param counterCurrency  the counter currency, to convert to
   * @return the FX rate for the currency pair
   * @throws IllegalArgumentException if no FX rate could be found
   */
  @Override
  public double fxRate(Currency baseCurrency, Currency counterCurrency) {
    if (baseCurrency.equals(counterCurrency)) {
      return 1d;
    }
    if (baseCurrency.equals(pair.getBase()) && counterCurrency.equals(pair.getCounter())) {
      return rate;
    }
    if (counterCurrency.equals(pair.getBase()) && baseCurrency.equals(pair.getCounter())) {
      return 1d / rate;
    }
    throw new IllegalArgumentException(Messages.format(
        "No FX rate found for {}/{}", baseCurrency, counterCurrency));
  }

  /**
   * Derives an FX rate from two related FX rates.
   * <p>
   * Given two FX rates it is possible to derive another rate if they have a currency in common.
   * For example, given rates for EUR/GBP and EUR/CHF it is possible to derive rates for GBP/CHF.
   * The result will always have a currency pair in the conventional order.
   * <p>
   * The cross is only returned if the two pairs contains three currencies in total.
   * If the inputs are invalid, an exception is thrown.
   * <ul>
   * <li>AAA/BBB and BBB/CCC - valid, producing AAA/CCC
   * <li>AAA/BBB and CCC/BBB - valid, producing AAA/CCC
   * <li>AAA/BBB and BBB/AAA - invalid, exception thrown
   * <li>AAA/BBB and BBB/BBB - invalid, exception thrown
   * <li>AAA/BBB and CCC/DDD - invalid, exception thrown
   * </ul>
   *
   * @param other  the other rates
   * @return a set of FX rates derived from these rates and the other rates
   * @throws IllegalArgumentException if the cross rate cannot be calculated
   */
  public FxRate crossRate(FxRate other) {
    return pair.cross(other.pair).map(cross -> computeCross(this, other, cross))
        .orElseThrow(() -> new IllegalArgumentException(Messages.format(
            "Unable to cross when no unique common currency: {} and {}", pair, other.pair)));
  }

  // computes the cross rate
  private static FxRate computeCross(FxRate fx1, FxRate fx2, CurrencyPair crossPairAC) {
    // aim is to convert AAA/BBB and BBB/CCC to AAA/CCC
    Currency currA = crossPairAC.getBase();
    Currency currC = crossPairAC.getCounter();
    // given the conventional cross rate pair, order the two rates to match
    boolean crossBaseCurrencyInFx1 = fx1.pair.contains(currA);
    FxRate fxABorBA = crossBaseCurrencyInFx1 ? fx1 : fx2;
    FxRate fxBCorCB = crossBaseCurrencyInFx1 ? fx2 : fx1;
    // extract the rates, taking the inverse if the pair is in the inverse order
    double rateAB = fxABorBA.getPair().getBase().equals(currA) ? fxABorBA.rate : 1d / fxABorBA.rate;
    double rateBC = fxBCorCB.getPair().getCounter().equals(currC) ? fxBCorCB.rate : 1d / fxBCorCB.rate;
    return FxRate.of(crossPairAC, rateAB * rateBC);
  }

  /**
   * Returns an FX rate object representing the market convention rate between the two currencies.
   * <p>
   * If the currency pair is the market convention pair, this method returns {@code this}, otherwise
   * it returns an {@code FxRate} with the inverse currency pair and reciprocal rate.
   *
   * @return an FX rate object representing the market convention rate between the two currencies
   */
  public FxRate toConventional() {
    return pair.isConventional() ? this : FxRate.of(pair.toConventional(), 1 / rate);
  }

  //-------------------------------------------------------------------------
  /**
   * Returns the formatted string version of the currency pair.
   * <p>
   * The format is '${baseCurrency}/${counterCurrency} ${rate}'.
   * 
   * @return the formatted string
   */
  @Override
  public String toString() {
    return pair + " " + (DoubleMath.isMathematicalInteger(rate) ? Long.toString((long) rate) : Double.toString(rate));
  }

  //------------------------- AUTOGENERATED START -------------------------
  ///CLOVER:OFF
  /**
   * The meta-bean for {@code FxRate}.
   * @return the meta-bean, not null
   */
  public static FxRate.Meta meta() {
    return FxRate.Meta.INSTANCE;
  }

  static {
    JodaBeanUtils.registerMetaBean(FxRate.Meta.INSTANCE);
  }

  /**
   * The serialization version id.
   */
  private static final long serialVersionUID = 1L;

  private FxRate(
      CurrencyPair pair,
      double rate) {
    JodaBeanUtils.notNull(pair, "pair");
    ArgChecker.notNegativeOrZero(rate, "rate");
    this.pair = pair;
    this.rate = rate;
    validate();
  }

  @Override
  public FxRate.Meta metaBean() {
    return FxRate.Meta.INSTANCE;
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
   * Gets the rate applicable to the currency pair.
   * One unit of the base currency is exchanged for this amount of the counter currency.
   * @return the value of the property
   */
  private double getRate() {
    return rate;
  }

  //-----------------------------------------------------------------------
  @Override
  public boolean equals(Object obj) {
    if (obj == this) {
      return true;
    }
    if (obj != null && obj.getClass() == this.getClass()) {
      FxRate other = (FxRate) obj;
      return JodaBeanUtils.equal(pair, other.pair) &&
          JodaBeanUtils.equal(rate, other.rate);
    }
    return false;
  }

  @Override
  public int hashCode() {
    int hash = getClass().hashCode();
    hash = hash * 31 + JodaBeanUtils.hashCode(pair);
    hash = hash * 31 + JodaBeanUtils.hashCode(rate);
    return hash;
  }

  //-----------------------------------------------------------------------
  /**
   * The meta-bean for {@code FxRate}.
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
        this, "pair", FxRate.class, CurrencyPair.class);
    /**
     * The meta-property for the {@code rate} property.
     */
    private final MetaProperty<Double> rate = DirectMetaProperty.ofImmutable(
        this, "rate", FxRate.class, Double.TYPE);
    /**
     * The meta-properties.
     */
    private final Map<String, MetaProperty<?>> metaPropertyMap$ = new DirectMetaPropertyMap(
        this, null,
        "pair",
        "rate");

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
        case 3493088:  // rate
          return rate;
      }
      return super.metaPropertyGet(propertyName);
    }

    @Override
    public BeanBuilder<? extends FxRate> builder() {
      return new FxRate.Builder();
    }

    @Override
    public Class<? extends FxRate> beanType() {
      return FxRate.class;
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
     * The meta-property for the {@code rate} property.
     * @return the meta-property, not null
     */
    public MetaProperty<Double> rate() {
      return rate;
    }

    //-----------------------------------------------------------------------
    @Override
    protected Object propertyGet(Bean bean, String propertyName, boolean quiet) {
      switch (propertyName.hashCode()) {
        case 3433178:  // pair
          return ((FxRate) bean).getPair();
        case 3493088:  // rate
          return ((FxRate) bean).getRate();
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
   * The bean-builder for {@code FxRate}.
   */
  private static final class Builder extends DirectPrivateBeanBuilder<FxRate> {

    private CurrencyPair pair;
    private double rate;

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
        case 3493088:  // rate
          return rate;
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
        case 3493088:  // rate
          this.rate = (Double) newValue;
          break;
        default:
          throw new NoSuchElementException("Unknown property: " + propertyName);
      }
      return this;
    }

    @Override
    public FxRate build() {
      return new FxRate(
          pair,
          rate);
    }

    //-----------------------------------------------------------------------
    @Override
    public String toString() {
      StringBuilder buf = new StringBuilder(96);
      buf.append("FxRate.Builder{");
      buf.append("pair").append('=').append(JodaBeanUtils.toString(pair)).append(',').append(' ');
      buf.append("rate").append('=').append(JodaBeanUtils.toString(rate));
      buf.append('}');
      return buf.toString();
    }

  }

  ///CLOVER:ON
  //-------------------------- AUTOGENERATED END --------------------------
}
