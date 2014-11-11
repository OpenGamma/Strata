/**
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.platform.finance.swap;

import java.io.Serializable;
import java.time.LocalDate;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;

import org.joda.beans.Bean;
import org.joda.beans.BeanDefinition;
import org.joda.beans.ImmutableBean;
import org.joda.beans.JodaBeanUtils;
import org.joda.beans.MetaProperty;
import org.joda.beans.Property;
import org.joda.beans.PropertyDefinition;
import org.joda.beans.impl.direct.DirectFieldsBeanBuilder;
import org.joda.beans.impl.direct.DirectMetaBean;
import org.joda.beans.impl.direct.DirectMetaProperty;
import org.joda.beans.impl.direct.DirectMetaPropertyMap;

import com.google.common.collect.ImmutableList;
import com.opengamma.basics.currency.Currency;
import com.opengamma.basics.schedule.PeriodicSchedule;
import com.opengamma.basics.schedule.Schedule;

/**
 * A floating rate swap leg based on an IBOR-like interest rate.
 * <p>
 * This defines a single swap leg paying an IBOR-like interest rate.
 * The amount is based on the observed value of an index such as 'GBP-LIBOR-3M' or 'EURIBOR-1M'.
 * <p>
 * Interest is calculated based on <i>accrual periods</i> which follow a regular schedule
 * with optional initial and final stubs.
 * Coupon payment is made based on <i>payment periods</i> with are typically the same as the accrual periods.
 * If the payment period is longer than the accrual period then compounding may apply.
 * The actual floating rate calculation, including details of the notional and index, is
 * defined in {@link IborRateCalculation}.
 * <p>
 * The following concepts are supported:
 * <ul>
 * <li>Regular accrual periods with initial/final stub
 * <li>Accrual periods based on a specific roll convention, such as IMM 3rd Wednesday
 * <li>Payment periods longer than accrual periods with compounding
 * <li>Payment offset from start or end of each accrual period
 * <li>Optional rate averaging across multiple reset periods
 * <li>Notionals that change at a payment period boundary
 * <li>Optional FX reset notionals
 * <li>One IBOR-like index, optional linear interpolation in stubs
 * <li>Gearing and spread that change at an accrual period boundary
 * </ul>
 * <p>
 * See {@link OvernightRateSwapLeg} for overnight interest rates.
 */
@BeanDefinition
public final class IborRateSwapLeg
    implements SwapLeg, ImmutableBean, Serializable {

  /** Serialization version. */
  private static final long serialVersionUID = 1L;

  /**
   * The accrual periods.
   * <p>
   * This is used to define the accrual periods of the swap.
   * These are used directly or indirectly to determine other dates in the swap.
   */
  @PropertyDefinition(validate = "notNull")
  private final PeriodicSchedule accrualPeriods;
  /**
   * The payment periods.
   * <p>
   * This is used to define the payment periods, including any compounding.
   * The payment period dates are based on the accrual schedule.
   */
  @PropertyDefinition(validate = "notNull")
  private final PaymentSchedule paymentPeriods;
  /**
   * The primary calculation of the swap leg based on an IBOR-like index.
   * <p>
   * The floating rate is calculated by observing a market value at specific dates.
   * The schedule of observation is known as the reset frequency and is smaller than
   * or equal to the accrual frequency. The actual dates of observation are known as
   * the fixing dates and are relative to the reset schedule.
   * <p>
   * The calculation definition includes the index, notional and day count.
   */
  @PropertyDefinition(validate = "notNull")
  private final IborRateCalculation calculation;

  //-------------------------------------------------------------------------
  /**
   * Gets the start date of the leg.
   * <p>
   * This is the first accrual date in the leg, often known as the effective date.
   * This date has been adjusted to be a valid business day.
   * 
   * @return the start date of the period
   */
  @Override
  public LocalDate getStartDate() {
    return accrualPeriods.getStartDate();
  }

  /**
   * Gets the end date of the leg.
   * <p>
   * This is the last accrual date in the leg, often known as the maturity date.
   * This date has been adjusted to be a valid business day.
   * 
   * @return the end date of the period
   */
  @Override
  public LocalDate getEndDate() {
    return accrualPeriods.getEndDate();
  }

  /**
   * Gets the currency of the swap leg.
   * 
   * @return the currency
   */
  @Override
  public Currency getCurrency() {
    return calculation.getNotional().getCurrency();
  }

  /**
   * Converts this swap leg to the equivalent expanded swap leg.
   * 
   * @return the equivalent expanded swap leg
   * @throws RuntimeException if the swap leg is invalid
   */
  @Override
  public ExpandedSwapLeg toExpanded() {
    Schedule schedule = accrualPeriods.createSchedule();
    ImmutableList<RateAccrualPeriod> accrualPeriods = calculation.createAccrualPeriods(schedule);
    return ExpandedSwapLeg.builder()
        .paymentPeriods(paymentPeriods.createPaymentPeriods(accrualPeriods, schedule, calculation))
        .notionalExchange(NotionalExchange.NO_EXCHANGE)  // TODO
        .build();
  }

  //------------------------- AUTOGENERATED START -------------------------
  ///CLOVER:OFF
  /**
   * The meta-bean for {@code IborRateSwapLeg}.
   * @return the meta-bean, not null
   */
  public static IborRateSwapLeg.Meta meta() {
    return IborRateSwapLeg.Meta.INSTANCE;
  }

  static {
    JodaBeanUtils.registerMetaBean(IborRateSwapLeg.Meta.INSTANCE);
  }

  /**
   * Returns a builder used to create an instance of the bean.
   * @return the builder, not null
   */
  public static IborRateSwapLeg.Builder builder() {
    return new IborRateSwapLeg.Builder();
  }

  private IborRateSwapLeg(
      PeriodicSchedule accrualPeriods,
      PaymentSchedule paymentPeriods,
      IborRateCalculation calculation) {
    JodaBeanUtils.notNull(accrualPeriods, "accrualPeriods");
    JodaBeanUtils.notNull(paymentPeriods, "paymentPeriods");
    JodaBeanUtils.notNull(calculation, "calculation");
    this.accrualPeriods = accrualPeriods;
    this.paymentPeriods = paymentPeriods;
    this.calculation = calculation;
  }

  @Override
  public IborRateSwapLeg.Meta metaBean() {
    return IborRateSwapLeg.Meta.INSTANCE;
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
   * Gets the accrual periods.
   * <p>
   * This is used to define the accrual periods of the swap.
   * These are used directly or indirectly to determine other dates in the swap.
   * @return the value of the property, not null
   */
  public PeriodicSchedule getAccrualPeriods() {
    return accrualPeriods;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the payment periods.
   * <p>
   * This is used to define the payment periods, including any compounding.
   * The payment period dates are based on the accrual schedule.
   * @return the value of the property, not null
   */
  public PaymentSchedule getPaymentPeriods() {
    return paymentPeriods;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the primary calculation of the swap leg based on an IBOR-like index.
   * <p>
   * The floating rate is calculated by observing a market value at specific dates.
   * The schedule of observation is known as the reset frequency and is smaller than
   * or equal to the accrual frequency. The actual dates of observation are known as
   * the fixing dates and are relative to the reset schedule.
   * <p>
   * The calculation definition includes the index, notional and day count.
   * @return the value of the property, not null
   */
  public IborRateCalculation getCalculation() {
    return calculation;
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
      IborRateSwapLeg other = (IborRateSwapLeg) obj;
      return JodaBeanUtils.equal(getAccrualPeriods(), other.getAccrualPeriods()) &&
          JodaBeanUtils.equal(getPaymentPeriods(), other.getPaymentPeriods()) &&
          JodaBeanUtils.equal(getCalculation(), other.getCalculation());
    }
    return false;
  }

  @Override
  public int hashCode() {
    int hash = getClass().hashCode();
    hash += hash * 31 + JodaBeanUtils.hashCode(getAccrualPeriods());
    hash += hash * 31 + JodaBeanUtils.hashCode(getPaymentPeriods());
    hash += hash * 31 + JodaBeanUtils.hashCode(getCalculation());
    return hash;
  }

  @Override
  public String toString() {
    StringBuilder buf = new StringBuilder(128);
    buf.append("IborRateSwapLeg{");
    buf.append("accrualPeriods").append('=').append(getAccrualPeriods()).append(',').append(' ');
    buf.append("paymentPeriods").append('=').append(getPaymentPeriods()).append(',').append(' ');
    buf.append("calculation").append('=').append(JodaBeanUtils.toString(getCalculation()));
    buf.append('}');
    return buf.toString();
  }

  //-----------------------------------------------------------------------
  /**
   * The meta-bean for {@code IborRateSwapLeg}.
   */
  public static final class Meta extends DirectMetaBean {
    /**
     * The singleton instance of the meta-bean.
     */
    static final Meta INSTANCE = new Meta();

    /**
     * The meta-property for the {@code accrualPeriods} property.
     */
    private final MetaProperty<PeriodicSchedule> accrualPeriods = DirectMetaProperty.ofImmutable(
        this, "accrualPeriods", IborRateSwapLeg.class, PeriodicSchedule.class);
    /**
     * The meta-property for the {@code paymentPeriods} property.
     */
    private final MetaProperty<PaymentSchedule> paymentPeriods = DirectMetaProperty.ofImmutable(
        this, "paymentPeriods", IborRateSwapLeg.class, PaymentSchedule.class);
    /**
     * The meta-property for the {@code calculation} property.
     */
    private final MetaProperty<IborRateCalculation> calculation = DirectMetaProperty.ofImmutable(
        this, "calculation", IborRateSwapLeg.class, IborRateCalculation.class);
    /**
     * The meta-properties.
     */
    private final Map<String, MetaProperty<?>> metaPropertyMap$ = new DirectMetaPropertyMap(
        this, null,
        "accrualPeriods",
        "paymentPeriods",
        "calculation");

    /**
     * Restricted constructor.
     */
    private Meta() {
    }

    @Override
    protected MetaProperty<?> metaPropertyGet(String propertyName) {
      switch (propertyName.hashCode()) {
        case -92208605:  // accrualPeriods
          return accrualPeriods;
        case -1674414612:  // paymentPeriods
          return paymentPeriods;
        case -934682935:  // calculation
          return calculation;
      }
      return super.metaPropertyGet(propertyName);
    }

    @Override
    public IborRateSwapLeg.Builder builder() {
      return new IborRateSwapLeg.Builder();
    }

    @Override
    public Class<? extends IborRateSwapLeg> beanType() {
      return IborRateSwapLeg.class;
    }

    @Override
    public Map<String, MetaProperty<?>> metaPropertyMap() {
      return metaPropertyMap$;
    }

    //-----------------------------------------------------------------------
    /**
     * The meta-property for the {@code accrualPeriods} property.
     * @return the meta-property, not null
     */
    public MetaProperty<PeriodicSchedule> accrualPeriods() {
      return accrualPeriods;
    }

    /**
     * The meta-property for the {@code paymentPeriods} property.
     * @return the meta-property, not null
     */
    public MetaProperty<PaymentSchedule> paymentPeriods() {
      return paymentPeriods;
    }

    /**
     * The meta-property for the {@code calculation} property.
     * @return the meta-property, not null
     */
    public MetaProperty<IborRateCalculation> calculation() {
      return calculation;
    }

    //-----------------------------------------------------------------------
    @Override
    protected Object propertyGet(Bean bean, String propertyName, boolean quiet) {
      switch (propertyName.hashCode()) {
        case -92208605:  // accrualPeriods
          return ((IborRateSwapLeg) bean).getAccrualPeriods();
        case -1674414612:  // paymentPeriods
          return ((IborRateSwapLeg) bean).getPaymentPeriods();
        case -934682935:  // calculation
          return ((IborRateSwapLeg) bean).getCalculation();
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
   * The bean-builder for {@code IborRateSwapLeg}.
   */
  public static final class Builder extends DirectFieldsBeanBuilder<IborRateSwapLeg> {

    private PeriodicSchedule accrualPeriods;
    private PaymentSchedule paymentPeriods;
    private IborRateCalculation calculation;

    /**
     * Restricted constructor.
     */
    private Builder() {
    }

    /**
     * Restricted copy constructor.
     * @param beanToCopy  the bean to copy from, not null
     */
    private Builder(IborRateSwapLeg beanToCopy) {
      this.accrualPeriods = beanToCopy.getAccrualPeriods();
      this.paymentPeriods = beanToCopy.getPaymentPeriods();
      this.calculation = beanToCopy.getCalculation();
    }

    //-----------------------------------------------------------------------
    @Override
    public Object get(String propertyName) {
      switch (propertyName.hashCode()) {
        case -92208605:  // accrualPeriods
          return accrualPeriods;
        case -1674414612:  // paymentPeriods
          return paymentPeriods;
        case -934682935:  // calculation
          return calculation;
        default:
          throw new NoSuchElementException("Unknown property: " + propertyName);
      }
    }

    @Override
    public Builder set(String propertyName, Object newValue) {
      switch (propertyName.hashCode()) {
        case -92208605:  // accrualPeriods
          this.accrualPeriods = (PeriodicSchedule) newValue;
          break;
        case -1674414612:  // paymentPeriods
          this.paymentPeriods = (PaymentSchedule) newValue;
          break;
        case -934682935:  // calculation
          this.calculation = (IborRateCalculation) newValue;
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
    public IborRateSwapLeg build() {
      return new IborRateSwapLeg(
          accrualPeriods,
          paymentPeriods,
          calculation);
    }

    //-----------------------------------------------------------------------
    /**
     * Sets the {@code accrualPeriods} property in the builder.
     * @param accrualPeriods  the new value, not null
     * @return this, for chaining, not null
     */
    public Builder accrualPeriods(PeriodicSchedule accrualPeriods) {
      JodaBeanUtils.notNull(accrualPeriods, "accrualPeriods");
      this.accrualPeriods = accrualPeriods;
      return this;
    }

    /**
     * Sets the {@code paymentPeriods} property in the builder.
     * @param paymentPeriods  the new value, not null
     * @return this, for chaining, not null
     */
    public Builder paymentPeriods(PaymentSchedule paymentPeriods) {
      JodaBeanUtils.notNull(paymentPeriods, "paymentPeriods");
      this.paymentPeriods = paymentPeriods;
      return this;
    }

    /**
     * Sets the {@code calculation} property in the builder.
     * @param calculation  the new value, not null
     * @return this, for chaining, not null
     */
    public Builder calculation(IborRateCalculation calculation) {
      JodaBeanUtils.notNull(calculation, "calculation");
      this.calculation = calculation;
      return this;
    }

    //-----------------------------------------------------------------------
    @Override
    public String toString() {
      StringBuilder buf = new StringBuilder(128);
      buf.append("IborRateSwapLeg.Builder{");
      buf.append("accrualPeriods").append('=').append(JodaBeanUtils.toString(accrualPeriods)).append(',').append(' ');
      buf.append("paymentPeriods").append('=').append(JodaBeanUtils.toString(paymentPeriods)).append(',').append(' ');
      buf.append("calculation").append('=').append(JodaBeanUtils.toString(calculation));
      buf.append('}');
      return buf.toString();
    }

  }

  ///CLOVER:ON
  //-------------------------- AUTOGENERATED END --------------------------
}
