/*
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.product.deposit.type;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.Period;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;

import org.joda.beans.Bean;
import org.joda.beans.BeanDefinition;
import org.joda.beans.ImmutableBean;
import org.joda.beans.ImmutablePreBuild;
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
import com.opengamma.strata.basics.index.IborIndex;
import com.opengamma.strata.collect.ArgChecker;
import com.opengamma.strata.product.TradeTemplate;
import com.opengamma.strata.product.common.BuySell;
import com.opengamma.strata.product.deposit.IborFixingDeposit;
import com.opengamma.strata.product.deposit.IborFixingDepositTrade;

/**
 * A template for creating an Ibor fixing deposit trade.
 * <p>
 * This defines almost all the data necessary to create a {@link IborFixingDepositTrade}.
 * The trade date, notional and fixed rate are required to complete the template and create the trade.
 * As such, it is often possible to get a market price for a trade based on the template.
 * <p>
 * The convention is defined by four dates.
 * <ul>
 * <li>Trade date, the date that the trade is agreed
 * <li>Start date or spot date, the date on which the deposit starts, typically 2 business days after the trade date
 * <li>End date, the date on which deposit ends, typically a number of months after the start date
 * <li>Fixing date, the date on which the index is to be observed, typically 2 business days before the start date
 * </ul>
 * Some of these dates are specified by the convention embedded within this template.
 */
@BeanDefinition
public final class IborFixingDepositTemplate
    implements TradeTemplate, ImmutableBean, Serializable {

  /**
   * The period between the start date and the end date.
   * <p>
   * The difference between the start date and the end date typically matches the tenor of the index,
   * however this is not validated.
   */
  @PropertyDefinition(validate = "notNull")
  private final Period depositPeriod;
  /**
   * The underlying Ibor fixing deposit convention.
   * <p>
   * This specifies the standard convention of the Ibor fixing deposit to be created.
   */
  @PropertyDefinition(validate = "notNull")
  private final IborFixingDepositConvention convention;

  //-------------------------------------------------------------------------
  @ImmutablePreBuild
  private static void preBuild(Builder builder) {
    if (builder.depositPeriod == null && builder.convention != null) {
      builder.depositPeriod = builder.convention.getIndex().getTenor().getPeriod();
    }
  }

  @ImmutableValidator
  private void validate() {
    ArgChecker.isFalse(depositPeriod.isNegative(), "Deposit Period must not be negative");
  }

  //-------------------------------------------------------------------------
  /**
   * Obtains a template based on the specified index.
   * <p>
   * The period from the start date to the end date will be the tenor of the index.
   * The convention will be created based on the index.
   * 
   * @param index  the index that defines the market convention
   * @return the template
   */
  public static IborFixingDepositTemplate of(IborIndex index) {
    return of(index.getTenor().getPeriod(), IborFixingDepositConvention.of(index));
  }

  /**
   * Obtains a template based on the specified period and index.
   * <p>
   * The period from the start date to the end is specified.
   * The convention will be created based on the index.
   * 
   * @param depositPeriod  the period between the start date and the end date
   * @param index  the index that defines the market convention
   * @return the template
   */
  public static IborFixingDepositTemplate of(Period depositPeriod, IborIndex index) {
    return of(depositPeriod, IborFixingDepositConvention.of(index));
  }

  /**
   * Obtains a template based on the specified periods and convention.
   * 
   * @param depositPeriod  the period between the start date and the end date
   * @param convention  the market convention
   * @return the template
   */
  public static IborFixingDepositTemplate of(Period depositPeriod, IborFixingDepositConvention convention) {
    ArgChecker.notNull(depositPeriod, "depositPeriod");
    ArgChecker.notNull(convention, "convention");
    return IborFixingDepositTemplate.builder()
        .depositPeriod(depositPeriod)
        .convention(convention)
        .build();
  }

  //-------------------------------------------------------------------------
  /**
   * Creates a trade based on this template.
   * <p>
   * This returns a trade based on the specified date.
   * The notional is unsigned, with buy/sell determining the direction of the trade.
   * If buying the Ibor fixing deposit, the floating rate is paid from the counterparty, with the fixed rate being received.
   * If selling the Ibor fixing deposit, the floating received is paid to the counterparty, with the fixed rate being paid.
   * 
   * @param tradeDate  the date of the trade
   * @param buySell  the buy/sell flag, see {@link IborFixingDeposit#getBuySell()}
   * @param notional  the notional amount, in the payment currency of the template
   * @param fixedRate  the fixed rate, typically derived from the market
   * @param refData  the reference data, used to resolve the trade dates
   * @return the trade
   * @throws ReferenceDataNotFoundException if an identifier cannot be resolved in the reference data
   */
  public IborFixingDepositTrade createTrade(
      LocalDate tradeDate,
      BuySell buySell,
      double notional,
      double fixedRate,
      ReferenceData refData) {

    return convention.createTrade(tradeDate, depositPeriod, buySell, notional, fixedRate, refData);
  }

  //------------------------- AUTOGENERATED START -------------------------
  ///CLOVER:OFF
  /**
   * The meta-bean for {@code IborFixingDepositTemplate}.
   * @return the meta-bean, not null
   */
  public static IborFixingDepositTemplate.Meta meta() {
    return IborFixingDepositTemplate.Meta.INSTANCE;
  }

  static {
    JodaBeanUtils.registerMetaBean(IborFixingDepositTemplate.Meta.INSTANCE);
  }

  /**
   * The serialization version id.
   */
  private static final long serialVersionUID = 1L;

  /**
   * Returns a builder used to create an instance of the bean.
   * @return the builder, not null
   */
  public static IborFixingDepositTemplate.Builder builder() {
    return new IborFixingDepositTemplate.Builder();
  }

  private IborFixingDepositTemplate(
      Period depositPeriod,
      IborFixingDepositConvention convention) {
    JodaBeanUtils.notNull(depositPeriod, "depositPeriod");
    JodaBeanUtils.notNull(convention, "convention");
    this.depositPeriod = depositPeriod;
    this.convention = convention;
    validate();
  }

  @Override
  public IborFixingDepositTemplate.Meta metaBean() {
    return IborFixingDepositTemplate.Meta.INSTANCE;
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
   * Gets the period between the start date and the end date.
   * <p>
   * The difference between the start date and the end date typically matches the tenor of the index,
   * however this is not validated.
   * @return the value of the property, not null
   */
  public Period getDepositPeriod() {
    return depositPeriod;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the underlying Ibor fixing deposit convention.
   * <p>
   * This specifies the standard convention of the Ibor fixing deposit to be created.
   * @return the value of the property, not null
   */
  public IborFixingDepositConvention getConvention() {
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
      IborFixingDepositTemplate other = (IborFixingDepositTemplate) obj;
      return JodaBeanUtils.equal(depositPeriod, other.depositPeriod) &&
          JodaBeanUtils.equal(convention, other.convention);
    }
    return false;
  }

  @Override
  public int hashCode() {
    int hash = getClass().hashCode();
    hash = hash * 31 + JodaBeanUtils.hashCode(depositPeriod);
    hash = hash * 31 + JodaBeanUtils.hashCode(convention);
    return hash;
  }

  @Override
  public String toString() {
    StringBuilder buf = new StringBuilder(96);
    buf.append("IborFixingDepositTemplate{");
    buf.append("depositPeriod").append('=').append(depositPeriod).append(',').append(' ');
    buf.append("convention").append('=').append(JodaBeanUtils.toString(convention));
    buf.append('}');
    return buf.toString();
  }

  //-----------------------------------------------------------------------
  /**
   * The meta-bean for {@code IborFixingDepositTemplate}.
   */
  public static final class Meta extends DirectMetaBean {
    /**
     * The singleton instance of the meta-bean.
     */
    static final Meta INSTANCE = new Meta();

    /**
     * The meta-property for the {@code depositPeriod} property.
     */
    private final MetaProperty<Period> depositPeriod = DirectMetaProperty.ofImmutable(
        this, "depositPeriod", IborFixingDepositTemplate.class, Period.class);
    /**
     * The meta-property for the {@code convention} property.
     */
    private final MetaProperty<IborFixingDepositConvention> convention = DirectMetaProperty.ofImmutable(
        this, "convention", IborFixingDepositTemplate.class, IborFixingDepositConvention.class);
    /**
     * The meta-properties.
     */
    private final Map<String, MetaProperty<?>> metaPropertyMap$ = new DirectMetaPropertyMap(
        this, null,
        "depositPeriod",
        "convention");

    /**
     * Restricted constructor.
     */
    private Meta() {
    }

    @Override
    protected MetaProperty<?> metaPropertyGet(String propertyName) {
      switch (propertyName.hashCode()) {
        case 14649855:  // depositPeriod
          return depositPeriod;
        case 2039569265:  // convention
          return convention;
      }
      return super.metaPropertyGet(propertyName);
    }

    @Override
    public IborFixingDepositTemplate.Builder builder() {
      return new IborFixingDepositTemplate.Builder();
    }

    @Override
    public Class<? extends IborFixingDepositTemplate> beanType() {
      return IborFixingDepositTemplate.class;
    }

    @Override
    public Map<String, MetaProperty<?>> metaPropertyMap() {
      return metaPropertyMap$;
    }

    //-----------------------------------------------------------------------
    /**
     * The meta-property for the {@code depositPeriod} property.
     * @return the meta-property, not null
     */
    public MetaProperty<Period> depositPeriod() {
      return depositPeriod;
    }

    /**
     * The meta-property for the {@code convention} property.
     * @return the meta-property, not null
     */
    public MetaProperty<IborFixingDepositConvention> convention() {
      return convention;
    }

    //-----------------------------------------------------------------------
    @Override
    protected Object propertyGet(Bean bean, String propertyName, boolean quiet) {
      switch (propertyName.hashCode()) {
        case 14649855:  // depositPeriod
          return ((IborFixingDepositTemplate) bean).getDepositPeriod();
        case 2039569265:  // convention
          return ((IborFixingDepositTemplate) bean).getConvention();
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
   * The bean-builder for {@code IborFixingDepositTemplate}.
   */
  public static final class Builder extends DirectFieldsBeanBuilder<IborFixingDepositTemplate> {

    private Period depositPeriod;
    private IborFixingDepositConvention convention;

    /**
     * Restricted constructor.
     */
    private Builder() {
    }

    /**
     * Restricted copy constructor.
     * @param beanToCopy  the bean to copy from, not null
     */
    private Builder(IborFixingDepositTemplate beanToCopy) {
      this.depositPeriod = beanToCopy.getDepositPeriod();
      this.convention = beanToCopy.getConvention();
    }

    //-----------------------------------------------------------------------
    @Override
    public Object get(String propertyName) {
      switch (propertyName.hashCode()) {
        case 14649855:  // depositPeriod
          return depositPeriod;
        case 2039569265:  // convention
          return convention;
        default:
          throw new NoSuchElementException("Unknown property: " + propertyName);
      }
    }

    @Override
    public Builder set(String propertyName, Object newValue) {
      switch (propertyName.hashCode()) {
        case 14649855:  // depositPeriod
          this.depositPeriod = (Period) newValue;
          break;
        case 2039569265:  // convention
          this.convention = (IborFixingDepositConvention) newValue;
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
    public IborFixingDepositTemplate build() {
      preBuild(this);
      return new IborFixingDepositTemplate(
          depositPeriod,
          convention);
    }

    //-----------------------------------------------------------------------
    /**
     * Sets the period between the start date and the end date.
     * <p>
     * The difference between the start date and the end date typically matches the tenor of the index,
     * however this is not validated.
     * @param depositPeriod  the new value, not null
     * @return this, for chaining, not null
     */
    public Builder depositPeriod(Period depositPeriod) {
      JodaBeanUtils.notNull(depositPeriod, "depositPeriod");
      this.depositPeriod = depositPeriod;
      return this;
    }

    /**
     * Sets the underlying Ibor fixing deposit convention.
     * <p>
     * This specifies the standard convention of the Ibor fixing deposit to be created.
     * @param convention  the new value, not null
     * @return this, for chaining, not null
     */
    public Builder convention(IborFixingDepositConvention convention) {
      JodaBeanUtils.notNull(convention, "convention");
      this.convention = convention;
      return this;
    }

    //-----------------------------------------------------------------------
    @Override
    public String toString() {
      StringBuilder buf = new StringBuilder(96);
      buf.append("IborFixingDepositTemplate.Builder{");
      buf.append("depositPeriod").append('=').append(JodaBeanUtils.toString(depositPeriod)).append(',').append(' ');
      buf.append("convention").append('=').append(JodaBeanUtils.toString(convention));
      buf.append('}');
      return buf.toString();
    }

  }

  ///CLOVER:ON
  //-------------------------- AUTOGENERATED END --------------------------
}
