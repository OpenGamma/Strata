/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.fx;

import java.io.Serializable;
import java.time.LocalDate;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.Set;

import org.joda.beans.Bean;
import org.joda.beans.BeanBuilder;
import org.joda.beans.BeanDefinition;
import org.joda.beans.ImmutableBean;
import org.joda.beans.ImmutableConstructor;
import org.joda.beans.JodaBeanUtils;
import org.joda.beans.MetaProperty;
import org.joda.beans.Property;
import org.joda.beans.PropertyDefinition;
import org.joda.beans.impl.direct.DirectFieldsBeanBuilder;
import org.joda.beans.impl.direct.DirectMetaBean;
import org.joda.beans.impl.direct.DirectMetaProperty;
import org.joda.beans.impl.direct.DirectMetaPropertyMap;

import com.opengamma.strata.basics.currency.Currency;
import com.opengamma.strata.basics.currency.CurrencyAmount;
import com.opengamma.strata.basics.currency.CurrencyPair;
import com.opengamma.strata.basics.currency.FxRateProvider;
import com.opengamma.strata.basics.currency.MultiCurrencyAmount;
import com.opengamma.strata.collect.ArgChecker;
import com.opengamma.strata.collect.Messages;
import com.opengamma.strata.data.MarketDataName;
import com.opengamma.strata.market.param.CurrencyParameterSensitivities;
import com.opengamma.strata.market.param.ParameterMetadata;
import com.opengamma.strata.market.param.ParameterPerturbation;
import com.opengamma.strata.market.param.ParameterizedDataCombiner;
import com.opengamma.strata.market.sensitivity.PointSensitivityBuilder;
import com.opengamma.strata.pricer.DiscountFactors;
import com.opengamma.strata.pricer.ZeroRateSensitivity;

/**
 * Provides access to discount factors for currencies.
 * <p>
 * This provides discount factors for a single currency pair.
 * <p>
 * This implementation is based on two underlying {@link DiscountFactors} objects,
 * one for each currency, and an {@link FxRateProvider}.
 */
@BeanDefinition(builderScope = "private")
public final class DiscountFxForwardRates
    implements FxForwardRates, ImmutableBean, Serializable {

  /**
   * The currency pair that the rates are for.
   */
  @PropertyDefinition(validate = "notNull", overrideGet = true)
  private final CurrencyPair currencyPair;
  /**
   * The provider of FX rates.
   */
  @PropertyDefinition(validate = "notNull")
  private final FxRateProvider fxRateProvider;
  /**
   * The discount factors for the base currency of the currency pair.
   */
  @PropertyDefinition(validate = "notNull")
  private final DiscountFactors baseCurrencyDiscountFactors;
  /**
   * The discount factors for the counter currency of the currency pair.
   */
  @PropertyDefinition(validate = "notNull")
  private final DiscountFactors counterCurrencyDiscountFactors;
  /**
   * The valuation date.
   */
  private final transient LocalDate valuationDate;  // not a property, derived and cached from input data
  /**
   * The parameter combiner.
   */
  private final transient ParameterizedDataCombiner paramCombiner;  // not a property

  //-------------------------------------------------------------------------
  /**
   * Obtains an instance based on two discount factors, one for each currency.
   * <p>
   * The instance is based on the discount factors for each currency.
   * 
   * @param currencyPair  the currency pair
   * @param fxRateProvider  the provider of FX rates
   * @param baseCurrencyFactors  the discount factors in the base currency of the index
   * @param counterCurrencyFactors  the discount factors in the counter currency of the index
   * @return the rates instance
   */
  public static DiscountFxForwardRates of(
      CurrencyPair currencyPair,
      FxRateProvider fxRateProvider,
      DiscountFactors baseCurrencyFactors,
      DiscountFactors counterCurrencyFactors) {

    return new DiscountFxForwardRates(currencyPair, fxRateProvider, baseCurrencyFactors, counterCurrencyFactors);
  }

  //-------------------------------------------------------------------------
  @ImmutableConstructor
  private DiscountFxForwardRates(
      CurrencyPair currencyPair,
      FxRateProvider fxRateProvider,
      DiscountFactors baseCurrencyDiscountFactors,
      DiscountFactors counterCurrencyDiscountFactors) {
    JodaBeanUtils.notNull(currencyPair, "currencyPair");
    JodaBeanUtils.notNull(fxRateProvider, "fxRateProvider");
    JodaBeanUtils.notNull(baseCurrencyDiscountFactors, "baseCurrencyDiscountFactors");
    JodaBeanUtils.notNull(counterCurrencyDiscountFactors, "counterCurrencyDiscountFactors");
    if (!baseCurrencyDiscountFactors.getCurrency().equals(currencyPair.getBase())) {
      throw new IllegalArgumentException(Messages.format(
          "Index base currency {} did not match discount factor base currency {}",
          currencyPair.getBase(),
          baseCurrencyDiscountFactors.getCurrency()));
    }
    if (!counterCurrencyDiscountFactors.getCurrency().equals(currencyPair.getCounter())) {
      throw new IllegalArgumentException(Messages.format(
          "Index counter currency {} did not match discount factor counter currency {}",
          currencyPair.getCounter(),
          counterCurrencyDiscountFactors.getCurrency()));
    }
    if (!baseCurrencyDiscountFactors.getValuationDate().equals(counterCurrencyDiscountFactors.getValuationDate())) {
      throw new IllegalArgumentException("Curves must have the same valuation date");
    }
    this.currencyPair = currencyPair;
    this.fxRateProvider = fxRateProvider;
    this.baseCurrencyDiscountFactors = baseCurrencyDiscountFactors;
    this.counterCurrencyDiscountFactors = counterCurrencyDiscountFactors;
    this.valuationDate = baseCurrencyDiscountFactors.getValuationDate();
    this.paramCombiner = ParameterizedDataCombiner.of(baseCurrencyDiscountFactors, counterCurrencyDiscountFactors);
  }

  // ensure standard constructor is invoked
  private Object readResolve() {
    return new DiscountFxForwardRates(currencyPair, fxRateProvider, baseCurrencyDiscountFactors, counterCurrencyDiscountFactors);
  }

  //-------------------------------------------------------------------------
  @Override
  public LocalDate getValuationDate() {
    return valuationDate;
  }

  @Override
  public <T> Optional<T> findData(MarketDataName<T> name) {
    return baseCurrencyDiscountFactors.findData(name)
        .map(Optional::of)
        .orElse(counterCurrencyDiscountFactors.findData(name));
  }

  @Override
  public int getParameterCount() {
    return paramCombiner.getParameterCount();
  }

  @Override
  public double getParameter(int parameterIndex) {
    return paramCombiner.getParameter(parameterIndex);
  }

  @Override
  public ParameterMetadata getParameterMetadata(int parameterIndex) {
    return paramCombiner.getParameterMetadata(parameterIndex);
  }

  @Override
  public DiscountFxForwardRates withParameter(int parameterIndex, double newValue) {
    return new DiscountFxForwardRates(
        currencyPair,
        fxRateProvider,
        paramCombiner.underlyingWithParameter(0, DiscountFactors.class, parameterIndex, newValue),
        paramCombiner.underlyingWithParameter(1, DiscountFactors.class, parameterIndex, newValue));
  }

  @Override
  public DiscountFxForwardRates withPerturbation(ParameterPerturbation perturbation) {
    return new DiscountFxForwardRates(
        currencyPair,
        fxRateProvider,
        paramCombiner.underlyingWithPerturbation(0, DiscountFactors.class, perturbation),
        paramCombiner.underlyingWithPerturbation(1, DiscountFactors.class, perturbation));
  }

  //-------------------------------------------------------------------------
  @Override
  public double rate(Currency baseCurrency, LocalDate referenceDate) {
    ArgChecker.isTrue(
        currencyPair.contains(baseCurrency), "Currency {} invalid for CurrencyPair {}", baseCurrency, currencyPair);
    boolean inverse = baseCurrency.equals(currencyPair.getCounter());
    double dfCcyBaseAtMaturity = baseCurrencyDiscountFactors.discountFactor(referenceDate);
    double dfCcyCounterAtMaturity = counterCurrencyDiscountFactors.discountFactor(referenceDate);
    double forwardRate = fxRateProvider.fxRate(currencyPair) * (dfCcyBaseAtMaturity / dfCcyCounterAtMaturity);
    return inverse ? 1d / forwardRate : forwardRate;
  }

  //-------------------------------------------------------------------------
  @Override
  public PointSensitivityBuilder ratePointSensitivity(Currency baseCurrency, LocalDate referenceDate) {
    ArgChecker.isTrue(
        currencyPair.contains(baseCurrency), "Currency {} invalid for CurrencyPair {}", baseCurrency, currencyPair);
    return FxForwardSensitivity.of(currencyPair, baseCurrency, referenceDate, 1d);
  }

  //-------------------------------------------------------------------------
  @Override
  public double rateFxSpotSensitivity(Currency baseCurrency, LocalDate referenceDate) {
    ArgChecker.isTrue(
        currencyPair.contains(baseCurrency), "Currency {} invalid for CurrencyPair {}", baseCurrency, currencyPair);
    boolean inverse = baseCurrency.equals(currencyPair.getCounter());
    double dfCcyBaseAtMaturity = baseCurrencyDiscountFactors.discountFactor(referenceDate);
    double dfCcyCounterAtMaturity = counterCurrencyDiscountFactors.discountFactor(referenceDate);
    double forwardRateDelta = dfCcyBaseAtMaturity / dfCcyCounterAtMaturity;
    return inverse ? 1d / forwardRateDelta : forwardRateDelta;
  }

  //-------------------------------------------------------------------------
  @Override
  public CurrencyParameterSensitivities parameterSensitivity(FxForwardSensitivity pointSensitivity) {
    // use the specified base currency to determine the desired currency pair
    // then derive sensitivity from discount factors based off desired currency pair, not that of the index
    CurrencyPair currencyPair = pointSensitivity.getCurrencyPair();
    Currency refBaseCurrency = pointSensitivity.getReferenceCurrency();
    Currency refCounterCurrency = pointSensitivity.getReferenceCounterCurrency();
    Currency sensitivityCurrency = pointSensitivity.getCurrency();
    LocalDate referenceDate = pointSensitivity.getReferenceDate();

    boolean inverse = refBaseCurrency.equals(currencyPair.getCounter());
    DiscountFactors discountFactorsRefBase = (inverse ? counterCurrencyDiscountFactors : baseCurrencyDiscountFactors);
    DiscountFactors discountFactorsRefCounter = (inverse ? baseCurrencyDiscountFactors : counterCurrencyDiscountFactors);
    double dfCcyBaseAtMaturity = discountFactorsRefBase.discountFactor(referenceDate);
    double dfCcyCounterAtMaturityInv = 1d / discountFactorsRefCounter.discountFactor(referenceDate);

    double fxRate = fxRateProvider.fxRate(refBaseCurrency, refCounterCurrency);
    ZeroRateSensitivity dfCcyBaseAtMaturitySensitivity =
        discountFactorsRefBase.zeroRatePointSensitivity(referenceDate, sensitivityCurrency)
            .multipliedBy(fxRate * dfCcyCounterAtMaturityInv * pointSensitivity.getSensitivity());

    ZeroRateSensitivity dfCcyCounterAtMaturitySensitivity =
        discountFactorsRefCounter.zeroRatePointSensitivity(referenceDate, sensitivityCurrency)
            .multipliedBy(-fxRate * dfCcyBaseAtMaturity * dfCcyCounterAtMaturityInv *
                dfCcyCounterAtMaturityInv * pointSensitivity.getSensitivity());

    return discountFactorsRefBase.parameterSensitivity(dfCcyBaseAtMaturitySensitivity)
        .combinedWith(discountFactorsRefCounter.parameterSensitivity(dfCcyCounterAtMaturitySensitivity));
  }

  @Override
  public MultiCurrencyAmount currencyExposure(FxForwardSensitivity pointSensitivity) {
    ArgChecker.isTrue(pointSensitivity.getCurrency().equals(pointSensitivity.getReferenceCurrency()),
        "Currency exposure defined only when sensitivity currency equal reference currency");
    Currency ccyRef = pointSensitivity.getReferenceCurrency();
    CurrencyPair pair = pointSensitivity.getCurrencyPair();
    double s = pointSensitivity.getSensitivity();
    LocalDate d = pointSensitivity.getReferenceDate();
    double f = fxRateProvider.fxRate(pair.getBase(), pair.getCounter());
    double pA = baseCurrencyDiscountFactors.discountFactor(d);
    double pB = counterCurrencyDiscountFactors.discountFactor(d);
    if (ccyRef.equals(pair.getBase())) {
      CurrencyAmount amountCounter = CurrencyAmount.of(pair.getBase(), s * f * pA / pB);
      CurrencyAmount amountBase = CurrencyAmount.of(pair.getCounter(), -s * f * f * pA / pB);
      return MultiCurrencyAmount.of(amountBase, amountCounter);
    } else {
      CurrencyAmount amountBase = CurrencyAmount.of(pair.getBase(), -s * pB / (pA * f * f));
      CurrencyAmount amountCounter = CurrencyAmount.of(pair.getCounter(), s * pB / (pA * f));
      return MultiCurrencyAmount.of(amountBase, amountCounter);
    }
  }

  //-------------------------------------------------------------------------
  /**
   * Returns a new instance with different discount factors.
   * 
   * @param baseCurrencyFactors  the new base currency discount factors
   * @param counterCurrencyFactors  the new counter currency discount factors
   * @return the new instance
   */
  public DiscountFxForwardRates withDiscountFactors(
      DiscountFactors baseCurrencyFactors, DiscountFactors counterCurrencyFactors) {
    return new DiscountFxForwardRates(currencyPair, fxRateProvider, baseCurrencyFactors, counterCurrencyFactors);
  }

  //------------------------- AUTOGENERATED START -------------------------
  ///CLOVER:OFF
  /**
   * The meta-bean for {@code DiscountFxForwardRates}.
   * @return the meta-bean, not null
   */
  public static DiscountFxForwardRates.Meta meta() {
    return DiscountFxForwardRates.Meta.INSTANCE;
  }

  static {
    JodaBeanUtils.registerMetaBean(DiscountFxForwardRates.Meta.INSTANCE);
  }

  /**
   * The serialization version id.
   */
  private static final long serialVersionUID = 1L;

  @Override
  public DiscountFxForwardRates.Meta metaBean() {
    return DiscountFxForwardRates.Meta.INSTANCE;
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
   * Gets the currency pair that the rates are for.
   * @return the value of the property, not null
   */
  @Override
  public CurrencyPair getCurrencyPair() {
    return currencyPair;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the provider of FX rates.
   * @return the value of the property, not null
   */
  public FxRateProvider getFxRateProvider() {
    return fxRateProvider;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the discount factors for the base currency of the currency pair.
   * @return the value of the property, not null
   */
  public DiscountFactors getBaseCurrencyDiscountFactors() {
    return baseCurrencyDiscountFactors;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the discount factors for the counter currency of the currency pair.
   * @return the value of the property, not null
   */
  public DiscountFactors getCounterCurrencyDiscountFactors() {
    return counterCurrencyDiscountFactors;
  }

  //-----------------------------------------------------------------------
  @Override
  public boolean equals(Object obj) {
    if (obj == this) {
      return true;
    }
    if (obj != null && obj.getClass() == this.getClass()) {
      DiscountFxForwardRates other = (DiscountFxForwardRates) obj;
      return JodaBeanUtils.equal(currencyPair, other.currencyPair) &&
          JodaBeanUtils.equal(fxRateProvider, other.fxRateProvider) &&
          JodaBeanUtils.equal(baseCurrencyDiscountFactors, other.baseCurrencyDiscountFactors) &&
          JodaBeanUtils.equal(counterCurrencyDiscountFactors, other.counterCurrencyDiscountFactors);
    }
    return false;
  }

  @Override
  public int hashCode() {
    int hash = getClass().hashCode();
    hash = hash * 31 + JodaBeanUtils.hashCode(currencyPair);
    hash = hash * 31 + JodaBeanUtils.hashCode(fxRateProvider);
    hash = hash * 31 + JodaBeanUtils.hashCode(baseCurrencyDiscountFactors);
    hash = hash * 31 + JodaBeanUtils.hashCode(counterCurrencyDiscountFactors);
    return hash;
  }

  @Override
  public String toString() {
    StringBuilder buf = new StringBuilder(160);
    buf.append("DiscountFxForwardRates{");
    buf.append("currencyPair").append('=').append(currencyPair).append(',').append(' ');
    buf.append("fxRateProvider").append('=').append(fxRateProvider).append(',').append(' ');
    buf.append("baseCurrencyDiscountFactors").append('=').append(baseCurrencyDiscountFactors).append(',').append(' ');
    buf.append("counterCurrencyDiscountFactors").append('=').append(JodaBeanUtils.toString(counterCurrencyDiscountFactors));
    buf.append('}');
    return buf.toString();
  }

  //-----------------------------------------------------------------------
  /**
   * The meta-bean for {@code DiscountFxForwardRates}.
   */
  public static final class Meta extends DirectMetaBean {
    /**
     * The singleton instance of the meta-bean.
     */
    static final Meta INSTANCE = new Meta();

    /**
     * The meta-property for the {@code currencyPair} property.
     */
    private final MetaProperty<CurrencyPair> currencyPair = DirectMetaProperty.ofImmutable(
        this, "currencyPair", DiscountFxForwardRates.class, CurrencyPair.class);
    /**
     * The meta-property for the {@code fxRateProvider} property.
     */
    private final MetaProperty<FxRateProvider> fxRateProvider = DirectMetaProperty.ofImmutable(
        this, "fxRateProvider", DiscountFxForwardRates.class, FxRateProvider.class);
    /**
     * The meta-property for the {@code baseCurrencyDiscountFactors} property.
     */
    private final MetaProperty<DiscountFactors> baseCurrencyDiscountFactors = DirectMetaProperty.ofImmutable(
        this, "baseCurrencyDiscountFactors", DiscountFxForwardRates.class, DiscountFactors.class);
    /**
     * The meta-property for the {@code counterCurrencyDiscountFactors} property.
     */
    private final MetaProperty<DiscountFactors> counterCurrencyDiscountFactors = DirectMetaProperty.ofImmutable(
        this, "counterCurrencyDiscountFactors", DiscountFxForwardRates.class, DiscountFactors.class);
    /**
     * The meta-properties.
     */
    private final Map<String, MetaProperty<?>> metaPropertyMap$ = new DirectMetaPropertyMap(
        this, null,
        "currencyPair",
        "fxRateProvider",
        "baseCurrencyDiscountFactors",
        "counterCurrencyDiscountFactors");

    /**
     * Restricted constructor.
     */
    private Meta() {
    }

    @Override
    protected MetaProperty<?> metaPropertyGet(String propertyName) {
      switch (propertyName.hashCode()) {
        case 1005147787:  // currencyPair
          return currencyPair;
        case -1499624221:  // fxRateProvider
          return fxRateProvider;
        case 1151357473:  // baseCurrencyDiscountFactors
          return baseCurrencyDiscountFactors;
        case -453959018:  // counterCurrencyDiscountFactors
          return counterCurrencyDiscountFactors;
      }
      return super.metaPropertyGet(propertyName);
    }

    @Override
    public BeanBuilder<? extends DiscountFxForwardRates> builder() {
      return new DiscountFxForwardRates.Builder();
    }

    @Override
    public Class<? extends DiscountFxForwardRates> beanType() {
      return DiscountFxForwardRates.class;
    }

    @Override
    public Map<String, MetaProperty<?>> metaPropertyMap() {
      return metaPropertyMap$;
    }

    //-----------------------------------------------------------------------
    /**
     * The meta-property for the {@code currencyPair} property.
     * @return the meta-property, not null
     */
    public MetaProperty<CurrencyPair> currencyPair() {
      return currencyPair;
    }

    /**
     * The meta-property for the {@code fxRateProvider} property.
     * @return the meta-property, not null
     */
    public MetaProperty<FxRateProvider> fxRateProvider() {
      return fxRateProvider;
    }

    /**
     * The meta-property for the {@code baseCurrencyDiscountFactors} property.
     * @return the meta-property, not null
     */
    public MetaProperty<DiscountFactors> baseCurrencyDiscountFactors() {
      return baseCurrencyDiscountFactors;
    }

    /**
     * The meta-property for the {@code counterCurrencyDiscountFactors} property.
     * @return the meta-property, not null
     */
    public MetaProperty<DiscountFactors> counterCurrencyDiscountFactors() {
      return counterCurrencyDiscountFactors;
    }

    //-----------------------------------------------------------------------
    @Override
    protected Object propertyGet(Bean bean, String propertyName, boolean quiet) {
      switch (propertyName.hashCode()) {
        case 1005147787:  // currencyPair
          return ((DiscountFxForwardRates) bean).getCurrencyPair();
        case -1499624221:  // fxRateProvider
          return ((DiscountFxForwardRates) bean).getFxRateProvider();
        case 1151357473:  // baseCurrencyDiscountFactors
          return ((DiscountFxForwardRates) bean).getBaseCurrencyDiscountFactors();
        case -453959018:  // counterCurrencyDiscountFactors
          return ((DiscountFxForwardRates) bean).getCounterCurrencyDiscountFactors();
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
   * The bean-builder for {@code DiscountFxForwardRates}.
   */
  private static final class Builder extends DirectFieldsBeanBuilder<DiscountFxForwardRates> {

    private CurrencyPair currencyPair;
    private FxRateProvider fxRateProvider;
    private DiscountFactors baseCurrencyDiscountFactors;
    private DiscountFactors counterCurrencyDiscountFactors;

    /**
     * Restricted constructor.
     */
    private Builder() {
    }

    //-----------------------------------------------------------------------
    @Override
    public Object get(String propertyName) {
      switch (propertyName.hashCode()) {
        case 1005147787:  // currencyPair
          return currencyPair;
        case -1499624221:  // fxRateProvider
          return fxRateProvider;
        case 1151357473:  // baseCurrencyDiscountFactors
          return baseCurrencyDiscountFactors;
        case -453959018:  // counterCurrencyDiscountFactors
          return counterCurrencyDiscountFactors;
        default:
          throw new NoSuchElementException("Unknown property: " + propertyName);
      }
    }

    @Override
    public Builder set(String propertyName, Object newValue) {
      switch (propertyName.hashCode()) {
        case 1005147787:  // currencyPair
          this.currencyPair = (CurrencyPair) newValue;
          break;
        case -1499624221:  // fxRateProvider
          this.fxRateProvider = (FxRateProvider) newValue;
          break;
        case 1151357473:  // baseCurrencyDiscountFactors
          this.baseCurrencyDiscountFactors = (DiscountFactors) newValue;
          break;
        case -453959018:  // counterCurrencyDiscountFactors
          this.counterCurrencyDiscountFactors = (DiscountFactors) newValue;
          break;
        default:
          throw new NoSuchElementException("Unknown property: " + propertyName);
      }
      return this;
    }

    @Override
    public Builder set(MetaProperty<?> property, Object value) {
      super.set(property, value);
      return this;
    }

    @Override
    public Builder setString(String propertyName, String value) {
      setString(meta().metaProperty(propertyName), value);
      return this;
    }

    @Override
    public Builder setString(MetaProperty<?> property, String value) {
      super.setString(property, value);
      return this;
    }

    @Override
    public Builder setAll(Map<String, ? extends Object> propertyValueMap) {
      super.setAll(propertyValueMap);
      return this;
    }

    @Override
    public DiscountFxForwardRates build() {
      return new DiscountFxForwardRates(
          currencyPair,
          fxRateProvider,
          baseCurrencyDiscountFactors,
          counterCurrencyDiscountFactors);
    }

    //-----------------------------------------------------------------------
    @Override
    public String toString() {
      StringBuilder buf = new StringBuilder(160);
      buf.append("DiscountFxForwardRates.Builder{");
      buf.append("currencyPair").append('=').append(JodaBeanUtils.toString(currencyPair)).append(',').append(' ');
      buf.append("fxRateProvider").append('=').append(JodaBeanUtils.toString(fxRateProvider)).append(',').append(' ');
      buf.append("baseCurrencyDiscountFactors").append('=').append(JodaBeanUtils.toString(baseCurrencyDiscountFactors)).append(',').append(' ');
      buf.append("counterCurrencyDiscountFactors").append('=').append(JodaBeanUtils.toString(counterCurrencyDiscountFactors));
      buf.append('}');
      return buf.toString();
    }

  }

  ///CLOVER:ON
  //-------------------------- AUTOGENERATED END --------------------------
}
