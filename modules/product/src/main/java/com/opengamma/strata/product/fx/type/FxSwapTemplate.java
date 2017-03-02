/*
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.product.fx.type;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.Period;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;

import org.joda.beans.Bean;
import org.joda.beans.BeanDefinition;
import org.joda.beans.ImmutableBean;
import org.joda.beans.ImmutableValidator;
import org.joda.beans.JodaBeanUtils;
import org.joda.beans.MetaProperty;
import org.joda.beans.Property;
import org.joda.beans.PropertyDefinition;
import org.joda.beans.impl.direct.DirectFieldsBeanBuilder;
import org.joda.beans.impl.direct.DirectMetaBean;
import org.joda.beans.impl.direct.DirectMetaProperty;
import org.joda.beans.impl.direct.DirectMetaPropertyMap;

import com.opengamma.strata.basics.ReferenceData;
import com.opengamma.strata.basics.ReferenceDataNotFoundException;
import com.opengamma.strata.basics.currency.CurrencyPair;
import com.opengamma.strata.collect.ArgChecker;
import com.opengamma.strata.product.TradeTemplate;
import com.opengamma.strata.product.common.BuySell;
import com.opengamma.strata.product.fx.FxSwapTrade;

/**
 * A template for creating an FX swap trade.
 * <p>
 * This defines almost all the data necessary to create a {@link FxSwapTrade}.
 * The trade date, notional, FX rate and forward points are required to complete the template and create the trade.
 * As such, it is often possible to get a market price for a trade based on the template.
 * <p>
 * The convention is defined by four dates.
 * <ul>
 * <li>Trade date, the date that the trade is agreed
 * <li>Spot date, the base for date calculations, typically 2 business days in the
 *  joint calendar of both currencies after the trade date
 * <li>Near date, the date on which the near leg of the swap is exchanged,
 *  typically equal to the spot date
 * <li>Far date, the date on which the far leg of the swap is exchanged,
 *  typically a number of months or years after the spot date
 * </ul>
 * Some of these dates are specified by the convention embedded within this template.
 */
@BeanDefinition
public final class FxSwapTemplate
    implements TradeTemplate, ImmutableBean, Serializable {

  /**
   * The period between the spot value date and the near date.
   * <p>
   * For example, a '3M x 6M' FX swap has a period from spot to the near date of 3 months
   */
  @PropertyDefinition(validate = "notNull")
  private final Period periodToNear;
  /**
   * The period between the spot value date and the far date.
   * <p>
   * For example, a '3M x 6M' FX swap has a period from spot to the far date of 6 months
   */
  @PropertyDefinition(validate = "notNull")
  private final Period periodToFar;
  /**
   * The underlying FX Swap convention.
   * <p>
   * This specifies the market convention of the FX Swap to be created.
   */
  @PropertyDefinition(validate = "notNull")
  private final FxSwapConvention convention;

  //-------------------------------------------------------------------------
  /**
   * Obtains a template based on the specified period and convention.
   * <p>
   * The near date is equal to the spot date.
   * The period from the spot date to the far date is specified
   * <p>
   * For example, a '6M' FX swap has a near leg on the spot date and a period from spot to the far date of 6 months
   * 
   * @param periodToFar  the period between the spot date and the far date
   * @param convention  the market convention
   * @return the template
   */
  public static FxSwapTemplate of(Period periodToFar, FxSwapConvention convention) {
    return FxSwapTemplate.builder()
        .periodToNear(Period.ZERO)
        .periodToFar(periodToFar)
        .convention(convention).build();
  }

  /**
   * Obtains a template based on the specified periods and convention.
   * <p>
   * Both the period from the spot date to the near date and far date are specified.
   * <p>
   * For example, a '3M x 6M' FX swap has a period from spot to the start date of 3 months and 
   * a period from spot to the far date of 6 months
   * 
   * @param periodToNear  the period between the spot date and the near date
   * @param periodToFar  the period between the spot date and the far date
   * @param convention  the market convention
   * @return the template
   */
  public static FxSwapTemplate of(Period periodToNear, Period periodToFar, FxSwapConvention convention) {
    return FxSwapTemplate.builder()
        .periodToNear(periodToNear)
        .periodToFar(periodToFar)
        .convention(convention).build();
  }

  @ImmutableValidator
  private void validate() {
    ArgChecker.isFalse(periodToNear.isNegative(), "Period to start must not be negative");
    ArgChecker.isFalse(periodToFar.isNegative(), "Period to end must not be negative");
  }

  //-------------------------------------------------------------------------
  /**
   * Gets the currency pair of the template.
   * 
   * @return the currency pair
   */
  public CurrencyPair getCurrencyPair() {
    return convention.getCurrencyPair();
  }

  /**
   * Creates a trade based on this template.
   * <p>
   * This returns a trade based on the specified date.
   * The notional is unsigned, with buy/sell determining the direction of the trade.
   * If buying the FX Swap, the amount in the first currency of the pair is received
   * in the near leg and paid in the far leg, while the second currency is paid in the
   * near leg and received in the far leg.
   * 
   * @param tradeDate  the date of the trade
   * @param buySell  the buy/sell flag
   * @param notional  the notional amount, in the first currency of the currency pair
   * @param nearFxRate  the FX rate for the near leg
   * @param forwardPoints  the FX points to be added to the FX rate at the far leg
   * @param refData  the reference data, used to resolve the trade dates
   * @return the trade
   * @throws ReferenceDataNotFoundException if an identifier cannot be resolved in the reference data
   */
  public FxSwapTrade createTrade(
      LocalDate tradeDate,
      BuySell buySell,
      double notional,
      double nearFxRate,
      double forwardPoints,
      ReferenceData refData) {

    return convention.createTrade(
        tradeDate, periodToNear, periodToFar, buySell, notional, nearFxRate, forwardPoints, refData);
  }

  //------------------------- AUTOGENERATED START -------------------------
  ///CLOVER:OFF
  /**
   * The meta-bean for {@code FxSwapTemplate}.
   * @return the meta-bean, not null
   */
  public static FxSwapTemplate.Meta meta() {
    return FxSwapTemplate.Meta.INSTANCE;
  }

  static {
    JodaBeanUtils.registerMetaBean(FxSwapTemplate.Meta.INSTANCE);
  }

  /**
   * The serialization version id.
   */
  private static final long serialVersionUID = 1L;

  /**
   * Returns a builder used to create an instance of the bean.
   * @return the builder, not null
   */
  public static FxSwapTemplate.Builder builder() {
    return new FxSwapTemplate.Builder();
  }

  private FxSwapTemplate(
      Period periodToNear,
      Period periodToFar,
      FxSwapConvention convention) {
    JodaBeanUtils.notNull(periodToNear, "periodToNear");
    JodaBeanUtils.notNull(periodToFar, "periodToFar");
    JodaBeanUtils.notNull(convention, "convention");
    this.periodToNear = periodToNear;
    this.periodToFar = periodToFar;
    this.convention = convention;
    validate();
  }

  @Override
  public FxSwapTemplate.Meta metaBean() {
    return FxSwapTemplate.Meta.INSTANCE;
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
   * Gets the period between the spot value date and the near date.
   * <p>
   * For example, a '3M x 6M' FX swap has a period from spot to the near date of 3 months
   * @return the value of the property, not null
   */
  public Period getPeriodToNear() {
    return periodToNear;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the period between the spot value date and the far date.
   * <p>
   * For example, a '3M x 6M' FX swap has a period from spot to the far date of 6 months
   * @return the value of the property, not null
   */
  public Period getPeriodToFar() {
    return periodToFar;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the underlying FX Swap convention.
   * <p>
   * This specifies the market convention of the FX Swap to be created.
   * @return the value of the property, not null
   */
  public FxSwapConvention getConvention() {
    return convention;
  }

  //-----------------------------------------------------------------------
  /**
   * Returns a builder that allows this bean to be mutated.
   * @return the mutable builder, not null
   */
  public Builder toBuilder() {
    return new Builder(this);
  }

  @Override
  public boolean equals(Object obj) {
    if (obj == this) {
      return true;
    }
    if (obj != null && obj.getClass() == this.getClass()) {
      FxSwapTemplate other = (FxSwapTemplate) obj;
      return JodaBeanUtils.equal(periodToNear, other.periodToNear) &&
          JodaBeanUtils.equal(periodToFar, other.periodToFar) &&
          JodaBeanUtils.equal(convention, other.convention);
    }
    return false;
  }

  @Override
  public int hashCode() {
    int hash = getClass().hashCode();
    hash = hash * 31 + JodaBeanUtils.hashCode(periodToNear);
    hash = hash * 31 + JodaBeanUtils.hashCode(periodToFar);
    hash = hash * 31 + JodaBeanUtils.hashCode(convention);
    return hash;
  }

  @Override
  public String toString() {
    StringBuilder buf = new StringBuilder(128);
    buf.append("FxSwapTemplate{");
    buf.append("periodToNear").append('=').append(periodToNear).append(',').append(' ');
    buf.append("periodToFar").append('=').append(periodToFar).append(',').append(' ');
    buf.append("convention").append('=').append(JodaBeanUtils.toString(convention));
    buf.append('}');
    return buf.toString();
  }

  //-----------------------------------------------------------------------
  /**
   * The meta-bean for {@code FxSwapTemplate}.
   */
  public static final class Meta extends DirectMetaBean {
    /**
     * The singleton instance of the meta-bean.
     */
    static final Meta INSTANCE = new Meta();

    /**
     * The meta-property for the {@code periodToNear} property.
     */
    private final MetaProperty<Period> periodToNear = DirectMetaProperty.ofImmutable(
        this, "periodToNear", FxSwapTemplate.class, Period.class);
    /**
     * The meta-property for the {@code periodToFar} property.
     */
    private final MetaProperty<Period> periodToFar = DirectMetaProperty.ofImmutable(
        this, "periodToFar", FxSwapTemplate.class, Period.class);
    /**
     * The meta-property for the {@code convention} property.
     */
    private final MetaProperty<FxSwapConvention> convention = DirectMetaProperty.ofImmutable(
        this, "convention", FxSwapTemplate.class, FxSwapConvention.class);
    /**
     * The meta-properties.
     */
    private final Map<String, MetaProperty<?>> metaPropertyMap$ = new DirectMetaPropertyMap(
        this, null,
        "periodToNear",
        "periodToFar",
        "convention");

    /**
     * Restricted constructor.
     */
    private Meta() {
    }

    @Override
    protected MetaProperty<?> metaPropertyGet(String propertyName) {
      switch (propertyName.hashCode()) {
        case -18701724:  // periodToNear
          return periodToNear;
        case -970442405:  // periodToFar
          return periodToFar;
        case 2039569265:  // convention
          return convention;
      }
      return super.metaPropertyGet(propertyName);
    }

    @Override
    public FxSwapTemplate.Builder builder() {
      return new FxSwapTemplate.Builder();
    }

    @Override
    public Class<? extends FxSwapTemplate> beanType() {
      return FxSwapTemplate.class;
    }

    @Override
    public Map<String, MetaProperty<?>> metaPropertyMap() {
      return metaPropertyMap$;
    }

    //-----------------------------------------------------------------------
    /**
     * The meta-property for the {@code periodToNear} property.
     * @return the meta-property, not null
     */
    public MetaProperty<Period> periodToNear() {
      return periodToNear;
    }

    /**
     * The meta-property for the {@code periodToFar} property.
     * @return the meta-property, not null
     */
    public MetaProperty<Period> periodToFar() {
      return periodToFar;
    }

    /**
     * The meta-property for the {@code convention} property.
     * @return the meta-property, not null
     */
    public MetaProperty<FxSwapConvention> convention() {
      return convention;
    }

    //-----------------------------------------------------------------------
    @Override
    protected Object propertyGet(Bean bean, String propertyName, boolean quiet) {
      switch (propertyName.hashCode()) {
        case -18701724:  // periodToNear
          return ((FxSwapTemplate) bean).getPeriodToNear();
        case -970442405:  // periodToFar
          return ((FxSwapTemplate) bean).getPeriodToFar();
        case 2039569265:  // convention
          return ((FxSwapTemplate) bean).getConvention();
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
   * The bean-builder for {@code FxSwapTemplate}.
   */
  public static final class Builder extends DirectFieldsBeanBuilder<FxSwapTemplate> {

    private Period periodToNear;
    private Period periodToFar;
    private FxSwapConvention convention;

    /**
     * Restricted constructor.
     */
    private Builder() {
    }

    /**
     * Restricted copy constructor.
     * @param beanToCopy  the bean to copy from, not null
     */
    private Builder(FxSwapTemplate beanToCopy) {
      this.periodToNear = beanToCopy.getPeriodToNear();
      this.periodToFar = beanToCopy.getPeriodToFar();
      this.convention = beanToCopy.getConvention();
    }

    //-----------------------------------------------------------------------
    @Override
    public Object get(String propertyName) {
      switch (propertyName.hashCode()) {
        case -18701724:  // periodToNear
          return periodToNear;
        case -970442405:  // periodToFar
          return periodToFar;
        case 2039569265:  // convention
          return convention;
        default:
          throw new NoSuchElementException("Unknown property: " + propertyName);
      }
    }

    @Override
    public Builder set(String propertyName, Object newValue) {
      switch (propertyName.hashCode()) {
        case -18701724:  // periodToNear
          this.periodToNear = (Period) newValue;
          break;
        case -970442405:  // periodToFar
          this.periodToFar = (Period) newValue;
          break;
        case 2039569265:  // convention
          this.convention = (FxSwapConvention) newValue;
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
    public FxSwapTemplate build() {
      return new FxSwapTemplate(
          periodToNear,
          periodToFar,
          convention);
    }

    //-----------------------------------------------------------------------
    /**
     * Sets the period between the spot value date and the near date.
     * <p>
     * For example, a '3M x 6M' FX swap has a period from spot to the near date of 3 months
     * @param periodToNear  the new value, not null
     * @return this, for chaining, not null
     */
    public Builder periodToNear(Period periodToNear) {
      JodaBeanUtils.notNull(periodToNear, "periodToNear");
      this.periodToNear = periodToNear;
      return this;
    }

    /**
     * Sets the period between the spot value date and the far date.
     * <p>
     * For example, a '3M x 6M' FX swap has a period from spot to the far date of 6 months
     * @param periodToFar  the new value, not null
     * @return this, for chaining, not null
     */
    public Builder periodToFar(Period periodToFar) {
      JodaBeanUtils.notNull(periodToFar, "periodToFar");
      this.periodToFar = periodToFar;
      return this;
    }

    /**
     * Sets the underlying FX Swap convention.
     * <p>
     * This specifies the market convention of the FX Swap to be created.
     * @param convention  the new value, not null
     * @return this, for chaining, not null
     */
    public Builder convention(FxSwapConvention convention) {
      JodaBeanUtils.notNull(convention, "convention");
      this.convention = convention;
      return this;
    }

    //-----------------------------------------------------------------------
    @Override
    public String toString() {
      StringBuilder buf = new StringBuilder(128);
      buf.append("FxSwapTemplate.Builder{");
      buf.append("periodToNear").append('=').append(JodaBeanUtils.toString(periodToNear)).append(',').append(' ');
      buf.append("periodToFar").append('=').append(JodaBeanUtils.toString(periodToFar)).append(',').append(' ');
      buf.append("convention").append('=').append(JodaBeanUtils.toString(convention));
      buf.append('}');
      return buf.toString();
    }

  }

  ///CLOVER:ON
  //-------------------------- AUTOGENERATED END --------------------------
}
