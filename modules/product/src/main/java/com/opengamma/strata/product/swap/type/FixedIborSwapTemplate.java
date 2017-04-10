/*
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.product.swap.type;

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
import com.opengamma.strata.basics.date.Tenor;
import com.opengamma.strata.collect.ArgChecker;
import com.opengamma.strata.product.TradeTemplate;
import com.opengamma.strata.product.common.BuySell;
import com.opengamma.strata.product.swap.SwapTrade;

/**
 * A template for creating Fixed-Ibor swap trades.
 * <p>
 * This defines almost all the data necessary to create a Fixed-Ibor single currency {@link SwapTrade}.
 * The trade date, notional and fixed rate are required to complete the template and create the trade.
 * As such, it is often possible to get a market price for a trade based on the template.
 * The market price is typically quoted as a bid/ask on the fixed rate.
 * <p>
 * The template references four dates.
 * <ul>
 * <li>Trade date, the date that the trade is agreed
 * <li>Spot date, the base for date calculations, typically 2 business days after the trade date
 * <li>Start date, the date on which accrual starts
 * <li>End date, the date on which accrual ends
 * </ul>
 * Some of these dates are specified by the convention embedded within this template.
 */
@BeanDefinition
public final class FixedIborSwapTemplate
    implements TradeTemplate, ImmutableBean, Serializable {

  /**
   * The period between the spot value date and the start date.
   * <p>
   * This is often zero, but can be greater if the swap if <i>forward starting</i>.
   * This must not be negative.
   */
  @PropertyDefinition(validate = "notNull")
  private final Period periodToStart;
  /**
   * The tenor of the swap.
   * <p>
   * This is the period from the first accrual date to the last accrual date.
   */
  @PropertyDefinition(validate = "notNull")
  private final Tenor tenor;
  /**
   * The market convention of the swap.
   */
  @PropertyDefinition(validate = "notNull")
  private final FixedIborSwapConvention convention;

  //-------------------------------------------------------------------------
  @ImmutableValidator
  private void validate() {
    ArgChecker.isFalse(periodToStart.isNegative(), "Period to start must not be negative");
  }

  //-------------------------------------------------------------------------
  /**
   * Obtains a template based on the specified tenor and convention.
   * <p>
   * The swap will start on the spot date.
   * 
   * @param tenor  the tenor of the swap
   * @param convention  the market convention
   * @return the template
   */
  public static FixedIborSwapTemplate of(Tenor tenor, FixedIborSwapConvention convention) {
    return of(Period.ZERO, tenor, convention);
  }

  /**
   * Creates a template based on the specified period, tenor and convention.
   * <p>
   * The period from the spot date to the start date is specified.
   * 
   * @param periodToStart  the period between the spot date and the start date
   * @param tenor  the tenor of the swap
   * @param convention  the market convention
   * @return the template
   */
  public static FixedIborSwapTemplate of(Period periodToStart, Tenor tenor, FixedIborSwapConvention convention) {
    return FixedIborSwapTemplate.builder()
        .periodToStart(periodToStart)
        .tenor(tenor)
        .convention(convention)
        .build();
  }

  //-------------------------------------------------------------------------
  /**
   * Creates a trade based on this template.
   * <p>
   * This returns a trade based on the specified trade date.
   * <p>
   * The notional is unsigned, with buy/sell determining the direction of the trade.
   * If buying the swap, the floating rate is received from the counterparty, with the fixed rate being paid.
   * If selling the swap, the floating rate is paid to the counterparty, with the fixed rate being received.
   * 
   * @param tradeDate  the date of the trade
   * @param buySell  the buy/sell flag
   * @param notional  the notional amount, in the payment currency of the template
   * @param fixedRate  the fixed rate, typically derived from the market
   * @param refData  the reference data, used to resolve the trade dates
   * @return the trade
   * @throws ReferenceDataNotFoundException if an identifier cannot be resolved in the reference data
   */
  public SwapTrade createTrade(
      LocalDate tradeDate,
      BuySell buySell,
      double notional,
      double fixedRate,
      ReferenceData refData) {

    return convention.createTrade(tradeDate, periodToStart, tenor, buySell, notional, fixedRate, refData);
  }

  //------------------------- AUTOGENERATED START -------------------------
  ///CLOVER:OFF
  /**
   * The meta-bean for {@code FixedIborSwapTemplate}.
   * @return the meta-bean, not null
   */
  public static FixedIborSwapTemplate.Meta meta() {
    return FixedIborSwapTemplate.Meta.INSTANCE;
  }

  static {
    JodaBeanUtils.registerMetaBean(FixedIborSwapTemplate.Meta.INSTANCE);
  }

  /**
   * The serialization version id.
   */
  private static final long serialVersionUID = 1L;

  /**
   * Returns a builder used to create an instance of the bean.
   * @return the builder, not null
   */
  public static FixedIborSwapTemplate.Builder builder() {
    return new FixedIborSwapTemplate.Builder();
  }

  private FixedIborSwapTemplate(
      Period periodToStart,
      Tenor tenor,
      FixedIborSwapConvention convention) {
    JodaBeanUtils.notNull(periodToStart, "periodToStart");
    JodaBeanUtils.notNull(tenor, "tenor");
    JodaBeanUtils.notNull(convention, "convention");
    this.periodToStart = periodToStart;
    this.tenor = tenor;
    this.convention = convention;
    validate();
  }

  @Override
  public FixedIborSwapTemplate.Meta metaBean() {
    return FixedIborSwapTemplate.Meta.INSTANCE;
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
   * Gets the period between the spot value date and the start date.
   * <p>
   * This is often zero, but can be greater if the swap if <i>forward starting</i>.
   * This must not be negative.
   * @return the value of the property, not null
   */
  public Period getPeriodToStart() {
    return periodToStart;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the tenor of the swap.
   * <p>
   * This is the period from the first accrual date to the last accrual date.
   * @return the value of the property, not null
   */
  public Tenor getTenor() {
    return tenor;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the market convention of the swap.
   * @return the value of the property, not null
   */
  public FixedIborSwapConvention getConvention() {
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
      FixedIborSwapTemplate other = (FixedIborSwapTemplate) obj;
      return JodaBeanUtils.equal(periodToStart, other.periodToStart) &&
          JodaBeanUtils.equal(tenor, other.tenor) &&
          JodaBeanUtils.equal(convention, other.convention);
    }
    return false;
  }

  @Override
  public int hashCode() {
    int hash = getClass().hashCode();
    hash = hash * 31 + JodaBeanUtils.hashCode(periodToStart);
    hash = hash * 31 + JodaBeanUtils.hashCode(tenor);
    hash = hash * 31 + JodaBeanUtils.hashCode(convention);
    return hash;
  }

  @Override
  public String toString() {
    StringBuilder buf = new StringBuilder(128);
    buf.append("FixedIborSwapTemplate{");
    buf.append("periodToStart").append('=').append(periodToStart).append(',').append(' ');
    buf.append("tenor").append('=').append(tenor).append(',').append(' ');
    buf.append("convention").append('=').append(JodaBeanUtils.toString(convention));
    buf.append('}');
    return buf.toString();
  }

  //-----------------------------------------------------------------------
  /**
   * The meta-bean for {@code FixedIborSwapTemplate}.
   */
  public static final class Meta extends DirectMetaBean {
    /**
     * The singleton instance of the meta-bean.
     */
    static final Meta INSTANCE = new Meta();

    /**
     * The meta-property for the {@code periodToStart} property.
     */
    private final MetaProperty<Period> periodToStart = DirectMetaProperty.ofImmutable(
        this, "periodToStart", FixedIborSwapTemplate.class, Period.class);
    /**
     * The meta-property for the {@code tenor} property.
     */
    private final MetaProperty<Tenor> tenor = DirectMetaProperty.ofImmutable(
        this, "tenor", FixedIborSwapTemplate.class, Tenor.class);
    /**
     * The meta-property for the {@code convention} property.
     */
    private final MetaProperty<FixedIborSwapConvention> convention = DirectMetaProperty.ofImmutable(
        this, "convention", FixedIborSwapTemplate.class, FixedIborSwapConvention.class);
    /**
     * The meta-properties.
     */
    private final Map<String, MetaProperty<?>> metaPropertyMap$ = new DirectMetaPropertyMap(
        this, null,
        "periodToStart",
        "tenor",
        "convention");

    /**
     * Restricted constructor.
     */
    private Meta() {
    }

    @Override
    protected MetaProperty<?> metaPropertyGet(String propertyName) {
      switch (propertyName.hashCode()) {
        case -574688858:  // periodToStart
          return periodToStart;
        case 110246592:  // tenor
          return tenor;
        case 2039569265:  // convention
          return convention;
      }
      return super.metaPropertyGet(propertyName);
    }

    @Override
    public FixedIborSwapTemplate.Builder builder() {
      return new FixedIborSwapTemplate.Builder();
    }

    @Override
    public Class<? extends FixedIborSwapTemplate> beanType() {
      return FixedIborSwapTemplate.class;
    }

    @Override
    public Map<String, MetaProperty<?>> metaPropertyMap() {
      return metaPropertyMap$;
    }

    //-----------------------------------------------------------------------
    /**
     * The meta-property for the {@code periodToStart} property.
     * @return the meta-property, not null
     */
    public MetaProperty<Period> periodToStart() {
      return periodToStart;
    }

    /**
     * The meta-property for the {@code tenor} property.
     * @return the meta-property, not null
     */
    public MetaProperty<Tenor> tenor() {
      return tenor;
    }

    /**
     * The meta-property for the {@code convention} property.
     * @return the meta-property, not null
     */
    public MetaProperty<FixedIborSwapConvention> convention() {
      return convention;
    }

    //-----------------------------------------------------------------------
    @Override
    protected Object propertyGet(Bean bean, String propertyName, boolean quiet) {
      switch (propertyName.hashCode()) {
        case -574688858:  // periodToStart
          return ((FixedIborSwapTemplate) bean).getPeriodToStart();
        case 110246592:  // tenor
          return ((FixedIborSwapTemplate) bean).getTenor();
        case 2039569265:  // convention
          return ((FixedIborSwapTemplate) bean).getConvention();
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
   * The bean-builder for {@code FixedIborSwapTemplate}.
   */
  public static final class Builder extends DirectFieldsBeanBuilder<FixedIborSwapTemplate> {

    private Period periodToStart;
    private Tenor tenor;
    private FixedIborSwapConvention convention;

    /**
     * Restricted constructor.
     */
    private Builder() {
    }

    /**
     * Restricted copy constructor.
     * @param beanToCopy  the bean to copy from, not null
     */
    private Builder(FixedIborSwapTemplate beanToCopy) {
      this.periodToStart = beanToCopy.getPeriodToStart();
      this.tenor = beanToCopy.getTenor();
      this.convention = beanToCopy.getConvention();
    }

    //-----------------------------------------------------------------------
    @Override
    public Object get(String propertyName) {
      switch (propertyName.hashCode()) {
        case -574688858:  // periodToStart
          return periodToStart;
        case 110246592:  // tenor
          return tenor;
        case 2039569265:  // convention
          return convention;
        default:
          throw new NoSuchElementException("Unknown property: " + propertyName);
      }
    }

    @Override
    public Builder set(String propertyName, Object newValue) {
      switch (propertyName.hashCode()) {
        case -574688858:  // periodToStart
          this.periodToStart = (Period) newValue;
          break;
        case 110246592:  // tenor
          this.tenor = (Tenor) newValue;
          break;
        case 2039569265:  // convention
          this.convention = (FixedIborSwapConvention) newValue;
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

    /**
     * @deprecated Use Joda-Convert in application code
     */
    @Override
    @Deprecated
    public Builder setString(String propertyName, String value) {
      setString(meta().metaProperty(propertyName), value);
      return this;
    }

    /**
     * @deprecated Use Joda-Convert in application code
     */
    @Override
    @Deprecated
    public Builder setString(MetaProperty<?> property, String value) {
      super.setString(property, value);
      return this;
    }

    /**
     * @deprecated Loop in application code
     */
    @Override
    @Deprecated
    public Builder setAll(Map<String, ? extends Object> propertyValueMap) {
      super.setAll(propertyValueMap);
      return this;
    }

    @Override
    public FixedIborSwapTemplate build() {
      return new FixedIborSwapTemplate(
          periodToStart,
          tenor,
          convention);
    }

    //-----------------------------------------------------------------------
    /**
     * Sets the period between the spot value date and the start date.
     * <p>
     * This is often zero, but can be greater if the swap if <i>forward starting</i>.
     * This must not be negative.
     * @param periodToStart  the new value, not null
     * @return this, for chaining, not null
     */
    public Builder periodToStart(Period periodToStart) {
      JodaBeanUtils.notNull(periodToStart, "periodToStart");
      this.periodToStart = periodToStart;
      return this;
    }

    /**
     * Sets the tenor of the swap.
     * <p>
     * This is the period from the first accrual date to the last accrual date.
     * @param tenor  the new value, not null
     * @return this, for chaining, not null
     */
    public Builder tenor(Tenor tenor) {
      JodaBeanUtils.notNull(tenor, "tenor");
      this.tenor = tenor;
      return this;
    }

    /**
     * Sets the market convention of the swap.
     * @param convention  the new value, not null
     * @return this, for chaining, not null
     */
    public Builder convention(FixedIborSwapConvention convention) {
      JodaBeanUtils.notNull(convention, "convention");
      this.convention = convention;
      return this;
    }

    //-----------------------------------------------------------------------
    @Override
    public String toString() {
      StringBuilder buf = new StringBuilder(128);
      buf.append("FixedIborSwapTemplate.Builder{");
      buf.append("periodToStart").append('=').append(JodaBeanUtils.toString(periodToStart)).append(',').append(' ');
      buf.append("tenor").append('=').append(JodaBeanUtils.toString(tenor)).append(',').append(' ');
      buf.append("convention").append('=').append(JodaBeanUtils.toString(convention));
      buf.append('}');
      return buf.toString();
    }

  }

  ///CLOVER:ON
  //-------------------------- AUTOGENERATED END --------------------------
}
