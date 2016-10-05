/**
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer;

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
import com.opengamma.strata.collect.ArgChecker;
import com.opengamma.strata.collect.array.DoubleArray;
import com.opengamma.strata.data.MarketDataName;
import com.opengamma.strata.market.param.CurrencyParameterSensitivities;
import com.opengamma.strata.market.param.ParameterMetadata;
import com.opengamma.strata.market.param.ParameterPerturbation;

/**
 * Caches the result of finding discount factors.
 * <p>
 * This wraps another {@link DiscountFactors} and provides caching based on the date.
 */
@BeanDefinition(builderScope = "private")
public final class CachedDiscountFactors
    implements DiscountFactors, ImmutableBean, Serializable {

  /**
   * The underlying discount factors.
   */
  @PropertyDefinition(validate = "notNull")
  private final DiscountFactors underlying;
  /**
   * The base date of the cache.
   */
  private final int baseDate;
  /**
   * The cache.
   */
  private final double[] cache = new double[1850];  // just larger than 5 years

  //-------------------------------------------------------------------------
  /**
   * Obtains an instance wrapping underlying discount factors.
   * 
   * @param underlying  the underlying discount factors
   * @return the curve
   */
  public static CachedDiscountFactors of(DiscountFactors underlying) {
    return new CachedDiscountFactors(underlying);
  }

  @ImmutableConstructor
  private CachedDiscountFactors(DiscountFactors underlying) {
    this.underlying = ArgChecker.notNull(underlying, "underlying");
    LocalDate valDate = underlying.getValuationDate();
    this.baseDate = intDate(valDate);
//    for (int i = 0; i < cache.length; i++) {
//      double yearFraction = relativeYearFraction(valDate.plusDays(i));
//      this.cache[i] = discountFactor(yearFraction);
//    }
  }

  private static int intDate(LocalDate date) {
    return (date.getYear() * 12 + date.getMonthValue() - 1) * 31 + date.getDayOfMonth() - 1;
  }

  //-------------------------------------------------------------------------
  @Override
  public LocalDate getValuationDate() {
    return underlying.getValuationDate();
  }

  @Override
  public Currency getCurrency() {
    return underlying.getCurrency();
  }

  @Override
  public <T> Optional<T> findData(MarketDataName<T> name) {
    return underlying.findData(name);
  }

  @Override
  public int getParameterCount() {
    return underlying.getParameterCount();
  }

  @Override
  public double getParameter(int parameterIndex) {
    return underlying.getParameter(parameterIndex);
  }

  @Override
  public ParameterMetadata getParameterMetadata(int parameterIndex) {
    return underlying.getParameterMetadata(parameterIndex);
  }

  @Override
  public CachedDiscountFactors withParameter(int parameterIndex, double newValue) {
    return new CachedDiscountFactors(underlying.withParameter(parameterIndex, newValue));
  }

  @Override
  public CachedDiscountFactors withPerturbation(ParameterPerturbation perturbation) {
    return new CachedDiscountFactors(underlying.withPerturbation(perturbation));
  }

  //-------------------------------------------------------------------------
  @Override
  public double relativeYearFraction(LocalDate date) {
    return underlying.relativeYearFraction(date);
  }

  @Override
  public double discountFactor(LocalDate date) {
    int index = intDate(date) - baseDate;
    if (index < 0) {
      return 1d;
    }
    if (index < cache.length) {
      double df = cache[index];
      if (df == 0d) {
        double yearFraction = relativeYearFraction(date);
        cache[index] = df = discountFactor(yearFraction);
      }
      return df;
    }
    double yearFraction = relativeYearFraction(date);
    return discountFactor(yearFraction);
  }

  @Override
  public double discountFactor(double yearFraction) {
    return underlying.discountFactor(yearFraction);
  }

  @Override
  public double discountFactorWithSpread(
      double yearFraction,
      double zSpread,
      CompoundedRateType compoundedRateType,
      int periodPerYear) {

    return underlying.discountFactorWithSpread(yearFraction, zSpread, compoundedRateType, periodPerYear);
  }

  @Override
  public double zeroRate(double yearFraction) {
    return underlying.zeroRate(yearFraction);
  }

  //-------------------------------------------------------------------------
  @Override
  public ZeroRateSensitivity zeroRatePointSensitivity(double yearFraction, Currency sensitivityCurrency) {
    return underlying.zeroRatePointSensitivity(yearFraction, sensitivityCurrency);
  }

  @Override
  public ZeroRateSensitivity zeroRatePointSensitivityWithSpread(
      double yearFraction,
      Currency sensitivityCurrency,
      double zSpread,
      CompoundedRateType compoundedRateType,
      int periodPerYear) {

    return underlying.zeroRatePointSensitivityWithSpread(
        yearFraction, sensitivityCurrency, zSpread, compoundedRateType, periodPerYear);
  }

  @Override
  public CurrencyParameterSensitivities parameterSensitivity(ZeroRateSensitivity pointSens) {
    return underlying.parameterSensitivity(pointSens);
  }

  @Override
  public CurrencyParameterSensitivities createParameterSensitivity(Currency currency, DoubleArray sensitivities) {
    return underlying.createParameterSensitivity(currency, sensitivities);
  }

  //------------------------- AUTOGENERATED START -------------------------
  ///CLOVER:OFF
  /**
   * The meta-bean for {@code CachedDiscountFactors}.
   * @return the meta-bean, not null
   */
  public static CachedDiscountFactors.Meta meta() {
    return CachedDiscountFactors.Meta.INSTANCE;
  }

  static {
    JodaBeanUtils.registerMetaBean(CachedDiscountFactors.Meta.INSTANCE);
  }

  /**
   * The serialization version id.
   */
  private static final long serialVersionUID = 1L;

  @Override
  public CachedDiscountFactors.Meta metaBean() {
    return CachedDiscountFactors.Meta.INSTANCE;
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
   * Gets the underlying discount factors.
   * @return the value of the property, not null
   */
  public DiscountFactors getUnderlying() {
    return underlying;
  }

  //-----------------------------------------------------------------------
  @Override
  public boolean equals(Object obj) {
    if (obj == this) {
      return true;
    }
    if (obj != null && obj.getClass() == this.getClass()) {
      CachedDiscountFactors other = (CachedDiscountFactors) obj;
      return JodaBeanUtils.equal(underlying, other.underlying);
    }
    return false;
  }

  @Override
  public int hashCode() {
    int hash = getClass().hashCode();
    hash = hash * 31 + JodaBeanUtils.hashCode(underlying);
    return hash;
  }

  @Override
  public String toString() {
    StringBuilder buf = new StringBuilder(64);
    buf.append("CachedDiscountFactors{");
    buf.append("underlying").append('=').append(JodaBeanUtils.toString(underlying));
    buf.append('}');
    return buf.toString();
  }

  //-----------------------------------------------------------------------
  /**
   * The meta-bean for {@code CachedDiscountFactors}.
   */
  public static final class Meta extends DirectMetaBean {
    /**
     * The singleton instance of the meta-bean.
     */
    static final Meta INSTANCE = new Meta();

    /**
     * The meta-property for the {@code underlying} property.
     */
    private final MetaProperty<DiscountFactors> underlying = DirectMetaProperty.ofImmutable(
        this, "underlying", CachedDiscountFactors.class, DiscountFactors.class);
    /**
     * The meta-properties.
     */
    private final Map<String, MetaProperty<?>> metaPropertyMap$ = new DirectMetaPropertyMap(
        this, null,
        "underlying");

    /**
     * Restricted constructor.
     */
    private Meta() {
    }

    @Override
    protected MetaProperty<?> metaPropertyGet(String propertyName) {
      switch (propertyName.hashCode()) {
        case -1770633379:  // underlying
          return underlying;
      }
      return super.metaPropertyGet(propertyName);
    }

    @Override
    public BeanBuilder<? extends CachedDiscountFactors> builder() {
      return new CachedDiscountFactors.Builder();
    }

    @Override
    public Class<? extends CachedDiscountFactors> beanType() {
      return CachedDiscountFactors.class;
    }

    @Override
    public Map<String, MetaProperty<?>> metaPropertyMap() {
      return metaPropertyMap$;
    }

    //-----------------------------------------------------------------------
    /**
     * The meta-property for the {@code underlying} property.
     * @return the meta-property, not null
     */
    public MetaProperty<DiscountFactors> underlying() {
      return underlying;
    }

    //-----------------------------------------------------------------------
    @Override
    protected Object propertyGet(Bean bean, String propertyName, boolean quiet) {
      switch (propertyName.hashCode()) {
        case -1770633379:  // underlying
          return ((CachedDiscountFactors) bean).getUnderlying();
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
   * The bean-builder for {@code CachedDiscountFactors}.
   */
  private static final class Builder extends DirectFieldsBeanBuilder<CachedDiscountFactors> {

    private DiscountFactors underlying;

    /**
     * Restricted constructor.
     */
    private Builder() {
    }

    //-----------------------------------------------------------------------
    @Override
    public Object get(String propertyName) {
      switch (propertyName.hashCode()) {
        case -1770633379:  // underlying
          return underlying;
        default:
          throw new NoSuchElementException("Unknown property: " + propertyName);
      }
    }

    @Override
    public Builder set(String propertyName, Object newValue) {
      switch (propertyName.hashCode()) {
        case -1770633379:  // underlying
          this.underlying = (DiscountFactors) newValue;
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
    public CachedDiscountFactors build() {
      return new CachedDiscountFactors(
          underlying);
    }

    //-----------------------------------------------------------------------
    @Override
    public String toString() {
      StringBuilder buf = new StringBuilder(64);
      buf.append("CachedDiscountFactors.Builder{");
      buf.append("underlying").append('=').append(JodaBeanUtils.toString(underlying));
      buf.append('}');
      return buf.toString();
    }

  }

  ///CLOVER:ON
  //-------------------------- AUTOGENERATED END --------------------------
}
