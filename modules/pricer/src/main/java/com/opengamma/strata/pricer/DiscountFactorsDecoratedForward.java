/**
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer;

import java.io.Serializable;
import java.time.LocalDate;
import java.util.Optional;

import org.joda.beans.BeanDefinition;
import org.joda.beans.ImmutableBean;
import org.joda.beans.ImmutableConstructor;
import org.joda.beans.PropertyDefinition;

import com.opengamma.strata.basics.currency.Currency;
import com.opengamma.strata.collect.ArgChecker;
import com.opengamma.strata.collect.array.DoubleArray;
import com.opengamma.strata.data.MarketDataName;
import com.opengamma.strata.market.param.CurrencyParameterSensitivities;
import com.opengamma.strata.market.param.ParameterMetadata;
import com.opengamma.strata.market.param.ParameterPerturbation;
import com.opengamma.strata.pricer.DiscountFactors;
import com.opengamma.strata.pricer.ZeroRateSensitivity;

import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import org.joda.beans.Bean;
import org.joda.beans.BeanBuilder;
import org.joda.beans.JodaBeanUtils;
import org.joda.beans.MetaProperty;
import org.joda.beans.Property;
import org.joda.beans.impl.direct.DirectFieldsBeanBuilder;
import org.joda.beans.impl.direct.DirectMetaBean;
import org.joda.beans.impl.direct.DirectMetaProperty;
import org.joda.beans.impl.direct.DirectMetaPropertyMap;

/**
 * Discount factors based on an underlying discount factors and a forward date. 
 * The new discount factors acts as the implied forward discount factor.
 * <p>
 * Only the methods used for direct valuation are implemented. The methods with spread and the methods related
 * to sensitivities are not implemented.
 */
@BeanDefinition(builderScope = "private")
public class DiscountFactorsDecoratedForward 
    implements DiscountFactors, ImmutableBean, Serializable {

  /**
   * Year fraction used as an effective zero.
   */
  private static final double EFFECTIVE_ZERO = 1e-10;
  
  /** Underlying provider. */
  @PropertyDefinition(validate = "notNull")
  private final DiscountFactors underlying;
  /** The forward rate. */
  @PropertyDefinition(validate = "notNull", overrideGet = true)
  private final LocalDate valuationDate;
  /** The discount factor at the forward date. */
  private final double discountFactorForwardDate;  // cached, not a property
  /** The relative year fraction to the forward date. */
  private final double yearFractionForwardDate;  // cached, not a property

  //-------------------------------------------------------------------------
  /**
   * Creates a new {@link DiscountFactors} from an existing one and a forward date. 
   * <p>
   * The provider created as a valuation date at the forward date. The discount factors at a given date are the 
   * forward discount factors, i.e the ratio of the original discount factor at the date and the discount 
   * factor at the forward date.
   * 
   * @param underlying  the underlying discount factors
   * @param valuationDate  the valuation date for which the curve is valid
   * @return the discount factors
   */
  public static DiscountFactorsDecoratedForward of(DiscountFactors underlying, LocalDate valuationDate) {
    return new DiscountFactorsDecoratedForward(underlying, valuationDate);
  }

  @ImmutableConstructor
  private DiscountFactorsDecoratedForward(DiscountFactors underlying, LocalDate valuationDate) {
    this.underlying = ArgChecker.notNull(underlying, "underlying");
    this.valuationDate = ArgChecker.notNull(valuationDate, "valuation date");
    this.discountFactorForwardDate = underlying.discountFactor(valuationDate);
    this.yearFractionForwardDate = underlying.relativeYearFraction(valuationDate);
  }

  @Override
  public Currency getCurrency() {
    return underlying.getCurrency();
  }

  @Override
  public int getParameterCount() {
    return underlying.getParameterCount();
  }

  @Override
  public double discountFactor(LocalDate date) {
    return underlying.discountFactor(date) / discountFactorForwardDate;
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
  public double relativeYearFraction(LocalDate date) {
    return underlying.relativeYearFraction(date) - yearFractionForwardDate; 
    // Rely on additive relative year fraction
  }

  @Override
  public double discountFactor(double yearFraction) {
    return underlying.discountFactor(yearFraction + yearFractionForwardDate) 
        / discountFactorForwardDate;
  }

  @Override
  public double zeroRate(double yearFraction) {
    double yearFractionMod = Math.max(EFFECTIVE_ZERO, yearFraction);
    double discountFactor = discountFactor(yearFractionMod);
    return -Math.log(discountFactor) / yearFractionMod;
  }

  @Override
  public DiscountFactors withParameter(int parameterIndex, double newValue) {
    return DiscountFactorsDecoratedForward.of(underlying.withParameter(parameterIndex, newValue), valuationDate);
  }

  @Override
  public DiscountFactors withPerturbation(ParameterPerturbation perturbation) {
    return DiscountFactorsDecoratedForward.of(underlying.withPerturbation(perturbation), valuationDate);
  }

  @Override
  public ZeroRateSensitivity zeroRatePointSensitivity(double yearFraction, Currency sensitivityCurrency) {
    double discountFactor = discountFactor(yearFraction);
    return ZeroRateSensitivity
        .of(underlying.getCurrency(), yearFraction, sensitivityCurrency, -discountFactor * yearFraction);
  }

  @Override
  public CurrencyParameterSensitivities parameterSensitivity(ZeroRateSensitivity pointSensitivity) {
    double yearFraction = pointSensitivity.getYearFraction();
    if (Math.abs(yearFraction) < EFFECTIVE_ZERO) {
      return CurrencyParameterSensitivities.empty(); // Discount factor in 0 is always 1, no sensitivity.
    }
    double dfForward = discountFactor(yearFraction);
    double n = pointSensitivity.getSensitivity() / (-yearFraction * dfForward);
    ZeroRateSensitivity ptsTimeShifted =
        ZeroRateSensitivity.of(pointSensitivity.getCurveCurrency(), pointSensitivity.getYearFraction() + yearFractionForwardDate,
            pointSensitivity.getCurrency(), -(yearFraction + yearFractionForwardDate) * dfForward);
    ZeroRateSensitivity ptsForward =
        ZeroRateSensitivity.of(pointSensitivity.getCurveCurrency(), yearFractionForwardDate,
            pointSensitivity.getCurrency(), yearFractionForwardDate * dfForward);
    return  (underlying.parameterSensitivity(ptsTimeShifted)
        .combinedWith(underlying.parameterSensitivity(ptsForward))).multipliedBy(n);
  }

  @Override
  public CurrencyParameterSensitivities createParameterSensitivity(Currency currency, DoubleArray sensitivities) {
    return underlying.createParameterSensitivity(currency, sensitivities);
  }

  @Override
  public <T> Optional<T> findData(MarketDataName<T> name) {
    return underlying.findData(name);
  }
    
  //------------------------- AUTOGENERATED START -------------------------
  ///CLOVER:OFF
  /**
   * The meta-bean for {@code DiscountFactorsDecoratedForward}.
   * @return the meta-bean, not null
   */
  public static DiscountFactorsDecoratedForward.Meta meta() {
    return DiscountFactorsDecoratedForward.Meta.INSTANCE;
  }

  static {
    JodaBeanUtils.registerMetaBean(DiscountFactorsDecoratedForward.Meta.INSTANCE);
  }

  /**
   * The serialization version id.
   */
  private static final long serialVersionUID = 1L;

  @Override
  public DiscountFactorsDecoratedForward.Meta metaBean() {
    return DiscountFactorsDecoratedForward.Meta.INSTANCE;
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
   * Gets underlying provider.
   * @return the value of the property, not null
   */
  public DiscountFactors getUnderlying() {
    return underlying;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the forward rate.
   * @return the value of the property, not null
   */
  @Override
  public LocalDate getValuationDate() {
    return valuationDate;
  }

  //-----------------------------------------------------------------------
  @Override
  public boolean equals(Object obj) {
    if (obj == this) {
      return true;
    }
    if (obj != null && obj.getClass() == this.getClass()) {
      DiscountFactorsDecoratedForward other = (DiscountFactorsDecoratedForward) obj;
      return JodaBeanUtils.equal(underlying, other.underlying) &&
          JodaBeanUtils.equal(valuationDate, other.valuationDate);
    }
    return false;
  }

  @Override
  public int hashCode() {
    int hash = getClass().hashCode();
    hash = hash * 31 + JodaBeanUtils.hashCode(underlying);
    hash = hash * 31 + JodaBeanUtils.hashCode(valuationDate);
    return hash;
  }

  @Override
  public String toString() {
    StringBuilder buf = new StringBuilder(96);
    buf.append("DiscountFactorsDecoratedForward{");
    int len = buf.length();
    toString(buf);
    if (buf.length() > len) {
      buf.setLength(buf.length() - 2);
    }
    buf.append('}');
    return buf.toString();
  }

  protected void toString(StringBuilder buf) {
    buf.append("underlying").append('=').append(JodaBeanUtils.toString(underlying)).append(',').append(' ');
    buf.append("valuationDate").append('=').append(JodaBeanUtils.toString(valuationDate)).append(',').append(' ');
  }

  //-----------------------------------------------------------------------
  /**
   * The meta-bean for {@code DiscountFactorsDecoratedForward}.
   */
  public static class Meta extends DirectMetaBean {
    /**
     * The singleton instance of the meta-bean.
     */
    static final Meta INSTANCE = new Meta();

    /**
     * The meta-property for the {@code underlying} property.
     */
    private final MetaProperty<DiscountFactors> underlying = DirectMetaProperty.ofImmutable(
        this, "underlying", DiscountFactorsDecoratedForward.class, DiscountFactors.class);
    /**
     * The meta-property for the {@code valuationDate} property.
     */
    private final MetaProperty<LocalDate> valuationDate = DirectMetaProperty.ofImmutable(
        this, "valuationDate", DiscountFactorsDecoratedForward.class, LocalDate.class);
    /**
     * The meta-properties.
     */
    private final Map<String, MetaProperty<?>> metaPropertyMap$ = new DirectMetaPropertyMap(
        this, null,
        "underlying",
        "valuationDate");

    /**
     * Restricted constructor.
     */
    protected Meta() {
    }

    @Override
    protected MetaProperty<?> metaPropertyGet(String propertyName) {
      switch (propertyName.hashCode()) {
        case -1770633379:  // underlying
          return underlying;
        case 113107279:  // valuationDate
          return valuationDate;
      }
      return super.metaPropertyGet(propertyName);
    }

    @Override
    public BeanBuilder<? extends DiscountFactorsDecoratedForward> builder() {
      return new DiscountFactorsDecoratedForward.Builder();
    }

    @Override
    public Class<? extends DiscountFactorsDecoratedForward> beanType() {
      return DiscountFactorsDecoratedForward.class;
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
    public final MetaProperty<DiscountFactors> underlying() {
      return underlying;
    }

    /**
     * The meta-property for the {@code valuationDate} property.
     * @return the meta-property, not null
     */
    public final MetaProperty<LocalDate> valuationDate() {
      return valuationDate;
    }

    //-----------------------------------------------------------------------
    @Override
    protected Object propertyGet(Bean bean, String propertyName, boolean quiet) {
      switch (propertyName.hashCode()) {
        case -1770633379:  // underlying
          return ((DiscountFactorsDecoratedForward) bean).getUnderlying();
        case 113107279:  // valuationDate
          return ((DiscountFactorsDecoratedForward) bean).getValuationDate();
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
   * The bean-builder for {@code DiscountFactorsDecoratedForward}.
   */
  private static class Builder extends DirectFieldsBeanBuilder<DiscountFactorsDecoratedForward> {

    private DiscountFactors underlying;
    private LocalDate valuationDate;

    /**
     * Restricted constructor.
     */
    protected Builder() {
    }

    //-----------------------------------------------------------------------
    @Override
    public Object get(String propertyName) {
      switch (propertyName.hashCode()) {
        case -1770633379:  // underlying
          return underlying;
        case 113107279:  // valuationDate
          return valuationDate;
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
        case 113107279:  // valuationDate
          this.valuationDate = (LocalDate) newValue;
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
    public DiscountFactorsDecoratedForward build() {
      return new DiscountFactorsDecoratedForward(
          underlying,
          valuationDate);
    }

    //-----------------------------------------------------------------------
    @Override
    public String toString() {
      StringBuilder buf = new StringBuilder(96);
      buf.append("DiscountFactorsDecoratedForward.Builder{");
      int len = buf.length();
      toString(buf);
      if (buf.length() > len) {
        buf.setLength(buf.length() - 2);
      }
      buf.append('}');
      return buf.toString();
    }

    protected void toString(StringBuilder buf) {
      buf.append("underlying").append('=').append(JodaBeanUtils.toString(underlying)).append(',').append(' ');
      buf.append("valuationDate").append('=').append(JodaBeanUtils.toString(valuationDate)).append(',').append(' ');
    }

  }

  ///CLOVER:ON
  //-------------------------- AUTOGENERATED END --------------------------
}
