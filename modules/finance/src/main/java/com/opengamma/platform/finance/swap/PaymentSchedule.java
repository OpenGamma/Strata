/**
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.platform.finance.swap;

import java.io.Serializable;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;

import org.joda.beans.Bean;
import org.joda.beans.BeanDefinition;
import org.joda.beans.ImmutableBean;
import org.joda.beans.ImmutableDefaults;
import org.joda.beans.JodaBeanUtils;
import org.joda.beans.MetaProperty;
import org.joda.beans.Property;
import org.joda.beans.PropertyDefinition;
import org.joda.beans.impl.direct.DirectFieldsBeanBuilder;
import org.joda.beans.impl.direct.DirectMetaBean;
import org.joda.beans.impl.direct.DirectMetaProperty;
import org.joda.beans.impl.direct.DirectMetaPropertyMap;

import com.google.common.collect.ImmutableList;
import com.opengamma.basics.PayReceive;
import com.opengamma.basics.date.DaysAdjustment;
import com.opengamma.basics.schedule.Frequency;
import com.opengamma.basics.schedule.Schedule;
import com.opengamma.basics.schedule.SchedulePeriod;
import com.opengamma.collect.Guavate;

/**
 * Defines the schedule of payment dates relative to the accrual periods.
 * <p>
 * This defines the data necessary to create a schedule of payment periods.
 * Each payment period contains one or more accrual periods.
 * If a payment period contains more than one accrual period then the compounding
 * method will be used to combine the amounts.
 * <p>
 * This class defines payment periods using a periodic frequency.
 * The frequency must match or be a multiple of the accrual periodic frequency.
 * <p>
 * If the payment frequency is 'Term' then there is only one payment.
 * As such, a 'Term' payment frequency causes stubs to be treated solely as accrual periods.
 * In all other cases, stubs are treated as payment periods in their own right.
 * <p>
 * When applying the frequency, it is converted into an integer value, representing the
 * number of accrual periods per payment period. The accrual periods are allocated by rolling
 * forwards or backwards, applying the same direction as accrual schedule generation.
 */
@BeanDefinition
public final class PaymentSchedule
    implements ImmutableBean, Serializable {

  /**
   * The periodic frequency of payments.
   * <p>
   * Regular payments will be made at the specified periodic frequency.
   * The frequency must be the same as, or a multiple of, the accrual periodic frequency.
   * <p>
   * Compounding applies if the payment frequency does not equal the accrual frequency.
   */
  @PropertyDefinition(validate = "notNull")
  private final Frequency paymentFrequency;
  /**
   * The base date that each payment is made relative to, defaulted to 'PeriodEnd'.
   * <p>
   * The payment date is relative to either the start or end of the payment period.
   */
  @PropertyDefinition(validate = "notNull")
  private final PaymentRelativeTo paymentRelativeTo;
  /**
   * The offset of payment from the base calculation period date.
   * <p>
   * The offset is applied to the unadjusted date specified by {@code paymentRelativeTo}.
   * Offset can be based on calendar days or business days.
   */
  @PropertyDefinition(validate = "notNull")
  private final DaysAdjustment paymentOffset;
  /**
   * The compounding method to use when there is more than one accrual period, defaulted to 'None'.
   * <p>
   * Compounding is used when combining accrual periods.
   */
  @PropertyDefinition(validate = "notNull")
  private final CompoundingMethod compoundingMethod;

  //-------------------------------------------------------------------------
  @ImmutableDefaults
  private static void applyDefaults(Builder builder) {
    builder.paymentRelativeTo(PaymentRelativeTo.PERIOD_END);
    builder.compoundingMethod(CompoundingMethod.NONE);
  }

  //-------------------------------------------------------------------------
  /**
   * Creates the payment schedule based on the accrual schedule.
   * <p>
   * If the payment frequency matches the accrual frequency, or if there is
   * only one period in the accrual schedule, the input schedule is returned.
   * <p>
   * Only the regular part of the accrual schedule is grouped into payment periods.
   * Any initial or final stub will be returned unaltered in the new schedule.
   * <p>
   * The grouping is determined by rolling forwards or backwards through the regular accrual periods
   * Rolling is backwards if there is an initial stub, otherwise rolling is forwards.
   * Grouping involves merging the existing accrual periods, thus the roll convention
   * of the accrual periods is implicitly applied.
   * 
   * @param accrualSchedule  the accrual schedule
   * @return the payment schedule
   * @throws IllegalArgumentException if the accrual frequency does not divide evenly into the payment frequency
   */
  public Schedule createSchedule(Schedule accrualSchedule) {
    // payment frequency of Term absorbs everything
    if (paymentFrequency.equals(Frequency.TERM)) {
      return accrualSchedule.mergeToTerm();
    }
    // derive schedule, retaining stubs as payment periods
    int accrualPeriodsPerPayment = paymentFrequency.exactDivide(accrualSchedule.getFrequency());
    boolean rollForwards = !accrualSchedule.getInitialStub().isPresent();
    return accrualSchedule.mergeRegular(accrualPeriodsPerPayment, rollForwards);
  }

  //-------------------------------------------------------------------------
  /**
   * Builds the list of payment periods from the list of accrual periods.
   * <p>
   * This applies the payment schedule.
   * 
   * @param accrualSchedule  the accrual schedule
   * @param accrualPeriods  the list of accrual periods
   * @return the list of payment periods
   */
  ImmutableList<RatePaymentPeriod> createPaymentPeriods(
      Schedule paymentSchedule,
      List<RateAccrualPeriod> accrualPeriods,
      NotionalSchedule notionalSchedule,
      PayReceive payReceive) {
    List<Double> notionals = notionalSchedule.getAmount().resolveValues(paymentSchedule.getPeriods());
    // build up payment periods using schedule
    ImmutableList.Builder<RatePaymentPeriod> paymentPeriods = ImmutableList.builder();
    for (int paymentIndex = 0; paymentIndex < paymentSchedule.size(); paymentIndex++) {
      SchedulePeriod period = paymentSchedule.getPeriod(paymentIndex);
      double notional = payReceive.normalize(notionals.get(paymentIndex));
      List<RateAccrualPeriod> paymentAccrualPeriods = accrualPeriods.stream()
          .filter(p -> !p.getStartDate().isBefore(period.getStartDate()))
          .filter(p -> !p.getEndDate().isAfter(period.getEndDate()))
          .collect(Guavate.toImmutableList());
      paymentPeriods.add(createPaymentPeriod(period, paymentAccrualPeriods, notionalSchedule, notional));
    }
    return paymentPeriods.build();
  }

  // create the payment period
  private RatePaymentPeriod createPaymentPeriod(
      SchedulePeriod paymentPeriod,
      List<RateAccrualPeriod> periods,
      NotionalSchedule notionalAmount,
      double notional) {
    return RatePaymentPeriod.builder()
        .paymentDate(paymentOffset.adjust(paymentRelativeTo.selectBaseDate(paymentPeriod)))
        .accrualPeriods(periods)
        .currency(notionalAmount.getCurrency())
        .fxReset(createFxReset(notionalAmount, paymentPeriod))
        .notional(notional)
        .compoundingMethod(compoundingMethod)
        .build();
  }

  // determine the FX reset
  private FxReset createFxReset(NotionalSchedule notionalSchedule, SchedulePeriod period) {
    return notionalSchedule.getFxReset()
        .map(calc -> calc.applyToPeriod(period))
        .orElse(null);
  }

  //------------------------- AUTOGENERATED START -------------------------
  ///CLOVER:OFF
  /**
   * The meta-bean for {@code PaymentSchedule}.
   * @return the meta-bean, not null
   */
  public static PaymentSchedule.Meta meta() {
    return PaymentSchedule.Meta.INSTANCE;
  }

  static {
    JodaBeanUtils.registerMetaBean(PaymentSchedule.Meta.INSTANCE);
  }

  /**
   * The serialization version id.
   */
  private static final long serialVersionUID = 1L;

  /**
   * Returns a builder used to create an instance of the bean.
   * @return the builder, not null
   */
  public static PaymentSchedule.Builder builder() {
    return new PaymentSchedule.Builder();
  }

  private PaymentSchedule(
      Frequency paymentFrequency,
      PaymentRelativeTo paymentRelativeTo,
      DaysAdjustment paymentOffset,
      CompoundingMethod compoundingMethod) {
    JodaBeanUtils.notNull(paymentFrequency, "paymentFrequency");
    JodaBeanUtils.notNull(paymentRelativeTo, "paymentRelativeTo");
    JodaBeanUtils.notNull(paymentOffset, "paymentOffset");
    JodaBeanUtils.notNull(compoundingMethod, "compoundingMethod");
    this.paymentFrequency = paymentFrequency;
    this.paymentRelativeTo = paymentRelativeTo;
    this.paymentOffset = paymentOffset;
    this.compoundingMethod = compoundingMethod;
  }

  @Override
  public PaymentSchedule.Meta metaBean() {
    return PaymentSchedule.Meta.INSTANCE;
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
   * Gets the periodic frequency of payments.
   * <p>
   * Regular payments will be made at the specified periodic frequency.
   * The frequency must be the same as, or a multiple of, the accrual periodic frequency.
   * <p>
   * Compounding applies if the payment frequency does not equal the accrual frequency.
   * @return the value of the property, not null
   */
  public Frequency getPaymentFrequency() {
    return paymentFrequency;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the base date that each payment is made relative to, defaulted to 'PeriodEnd'.
   * <p>
   * The payment date is relative to either the start or end of the payment period.
   * @return the value of the property, not null
   */
  public PaymentRelativeTo getPaymentRelativeTo() {
    return paymentRelativeTo;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the offset of payment from the base calculation period date.
   * <p>
   * The offset is applied to the unadjusted date specified by {@code paymentRelativeTo}.
   * Offset can be based on calendar days or business days.
   * @return the value of the property, not null
   */
  public DaysAdjustment getPaymentOffset() {
    return paymentOffset;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the compounding method to use when there is more than one accrual period, defaulted to 'None'.
   * <p>
   * Compounding is used when combining accrual periods.
   * @return the value of the property, not null
   */
  public CompoundingMethod getCompoundingMethod() {
    return compoundingMethod;
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
      PaymentSchedule other = (PaymentSchedule) obj;
      return JodaBeanUtils.equal(getPaymentFrequency(), other.getPaymentFrequency()) &&
          JodaBeanUtils.equal(getPaymentRelativeTo(), other.getPaymentRelativeTo()) &&
          JodaBeanUtils.equal(getPaymentOffset(), other.getPaymentOffset()) &&
          JodaBeanUtils.equal(getCompoundingMethod(), other.getCompoundingMethod());
    }
    return false;
  }

  @Override
  public int hashCode() {
    int hash = getClass().hashCode();
    hash = hash * 31 + JodaBeanUtils.hashCode(getPaymentFrequency());
    hash = hash * 31 + JodaBeanUtils.hashCode(getPaymentRelativeTo());
    hash = hash * 31 + JodaBeanUtils.hashCode(getPaymentOffset());
    hash = hash * 31 + JodaBeanUtils.hashCode(getCompoundingMethod());
    return hash;
  }

  @Override
  public String toString() {
    StringBuilder buf = new StringBuilder(160);
    buf.append("PaymentSchedule{");
    buf.append("paymentFrequency").append('=').append(getPaymentFrequency()).append(',').append(' ');
    buf.append("paymentRelativeTo").append('=').append(getPaymentRelativeTo()).append(',').append(' ');
    buf.append("paymentOffset").append('=').append(getPaymentOffset()).append(',').append(' ');
    buf.append("compoundingMethod").append('=').append(JodaBeanUtils.toString(getCompoundingMethod()));
    buf.append('}');
    return buf.toString();
  }

  //-----------------------------------------------------------------------
  /**
   * The meta-bean for {@code PaymentSchedule}.
   */
  public static final class Meta extends DirectMetaBean {
    /**
     * The singleton instance of the meta-bean.
     */
    static final Meta INSTANCE = new Meta();

    /**
     * The meta-property for the {@code paymentFrequency} property.
     */
    private final MetaProperty<Frequency> paymentFrequency = DirectMetaProperty.ofImmutable(
        this, "paymentFrequency", PaymentSchedule.class, Frequency.class);
    /**
     * The meta-property for the {@code paymentRelativeTo} property.
     */
    private final MetaProperty<PaymentRelativeTo> paymentRelativeTo = DirectMetaProperty.ofImmutable(
        this, "paymentRelativeTo", PaymentSchedule.class, PaymentRelativeTo.class);
    /**
     * The meta-property for the {@code paymentOffset} property.
     */
    private final MetaProperty<DaysAdjustment> paymentOffset = DirectMetaProperty.ofImmutable(
        this, "paymentOffset", PaymentSchedule.class, DaysAdjustment.class);
    /**
     * The meta-property for the {@code compoundingMethod} property.
     */
    private final MetaProperty<CompoundingMethod> compoundingMethod = DirectMetaProperty.ofImmutable(
        this, "compoundingMethod", PaymentSchedule.class, CompoundingMethod.class);
    /**
     * The meta-properties.
     */
    private final Map<String, MetaProperty<?>> metaPropertyMap$ = new DirectMetaPropertyMap(
        this, null,
        "paymentFrequency",
        "paymentRelativeTo",
        "paymentOffset",
        "compoundingMethod");

    /**
     * Restricted constructor.
     */
    private Meta() {
    }

    @Override
    protected MetaProperty<?> metaPropertyGet(String propertyName) {
      switch (propertyName.hashCode()) {
        case 863656438:  // paymentFrequency
          return paymentFrequency;
        case -1357627123:  // paymentRelativeTo
          return paymentRelativeTo;
        case 1303406137:  // paymentOffset
          return paymentOffset;
        case -1376171496:  // compoundingMethod
          return compoundingMethod;
      }
      return super.metaPropertyGet(propertyName);
    }

    @Override
    public PaymentSchedule.Builder builder() {
      return new PaymentSchedule.Builder();
    }

    @Override
    public Class<? extends PaymentSchedule> beanType() {
      return PaymentSchedule.class;
    }

    @Override
    public Map<String, MetaProperty<?>> metaPropertyMap() {
      return metaPropertyMap$;
    }

    //-----------------------------------------------------------------------
    /**
     * The meta-property for the {@code paymentFrequency} property.
     * @return the meta-property, not null
     */
    public MetaProperty<Frequency> paymentFrequency() {
      return paymentFrequency;
    }

    /**
     * The meta-property for the {@code paymentRelativeTo} property.
     * @return the meta-property, not null
     */
    public MetaProperty<PaymentRelativeTo> paymentRelativeTo() {
      return paymentRelativeTo;
    }

    /**
     * The meta-property for the {@code paymentOffset} property.
     * @return the meta-property, not null
     */
    public MetaProperty<DaysAdjustment> paymentOffset() {
      return paymentOffset;
    }

    /**
     * The meta-property for the {@code compoundingMethod} property.
     * @return the meta-property, not null
     */
    public MetaProperty<CompoundingMethod> compoundingMethod() {
      return compoundingMethod;
    }

    //-----------------------------------------------------------------------
    @Override
    protected Object propertyGet(Bean bean, String propertyName, boolean quiet) {
      switch (propertyName.hashCode()) {
        case 863656438:  // paymentFrequency
          return ((PaymentSchedule) bean).getPaymentFrequency();
        case -1357627123:  // paymentRelativeTo
          return ((PaymentSchedule) bean).getPaymentRelativeTo();
        case 1303406137:  // paymentOffset
          return ((PaymentSchedule) bean).getPaymentOffset();
        case -1376171496:  // compoundingMethod
          return ((PaymentSchedule) bean).getCompoundingMethod();
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
   * The bean-builder for {@code PaymentSchedule}.
   */
  public static final class Builder extends DirectFieldsBeanBuilder<PaymentSchedule> {

    private Frequency paymentFrequency;
    private PaymentRelativeTo paymentRelativeTo;
    private DaysAdjustment paymentOffset;
    private CompoundingMethod compoundingMethod;

    /**
     * Restricted constructor.
     */
    private Builder() {
      applyDefaults(this);
    }

    /**
     * Restricted copy constructor.
     * @param beanToCopy  the bean to copy from, not null
     */
    private Builder(PaymentSchedule beanToCopy) {
      this.paymentFrequency = beanToCopy.getPaymentFrequency();
      this.paymentRelativeTo = beanToCopy.getPaymentRelativeTo();
      this.paymentOffset = beanToCopy.getPaymentOffset();
      this.compoundingMethod = beanToCopy.getCompoundingMethod();
    }

    //-----------------------------------------------------------------------
    @Override
    public Object get(String propertyName) {
      switch (propertyName.hashCode()) {
        case 863656438:  // paymentFrequency
          return paymentFrequency;
        case -1357627123:  // paymentRelativeTo
          return paymentRelativeTo;
        case 1303406137:  // paymentOffset
          return paymentOffset;
        case -1376171496:  // compoundingMethod
          return compoundingMethod;
        default:
          throw new NoSuchElementException("Unknown property: " + propertyName);
      }
    }

    @Override
    public Builder set(String propertyName, Object newValue) {
      switch (propertyName.hashCode()) {
        case 863656438:  // paymentFrequency
          this.paymentFrequency = (Frequency) newValue;
          break;
        case -1357627123:  // paymentRelativeTo
          this.paymentRelativeTo = (PaymentRelativeTo) newValue;
          break;
        case 1303406137:  // paymentOffset
          this.paymentOffset = (DaysAdjustment) newValue;
          break;
        case -1376171496:  // compoundingMethod
          this.compoundingMethod = (CompoundingMethod) newValue;
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
    public PaymentSchedule build() {
      return new PaymentSchedule(
          paymentFrequency,
          paymentRelativeTo,
          paymentOffset,
          compoundingMethod);
    }

    //-----------------------------------------------------------------------
    /**
     * Sets the {@code paymentFrequency} property in the builder.
     * @param paymentFrequency  the new value, not null
     * @return this, for chaining, not null
     */
    public Builder paymentFrequency(Frequency paymentFrequency) {
      JodaBeanUtils.notNull(paymentFrequency, "paymentFrequency");
      this.paymentFrequency = paymentFrequency;
      return this;
    }

    /**
     * Sets the {@code paymentRelativeTo} property in the builder.
     * @param paymentRelativeTo  the new value, not null
     * @return this, for chaining, not null
     */
    public Builder paymentRelativeTo(PaymentRelativeTo paymentRelativeTo) {
      JodaBeanUtils.notNull(paymentRelativeTo, "paymentRelativeTo");
      this.paymentRelativeTo = paymentRelativeTo;
      return this;
    }

    /**
     * Sets the {@code paymentOffset} property in the builder.
     * @param paymentOffset  the new value, not null
     * @return this, for chaining, not null
     */
    public Builder paymentOffset(DaysAdjustment paymentOffset) {
      JodaBeanUtils.notNull(paymentOffset, "paymentOffset");
      this.paymentOffset = paymentOffset;
      return this;
    }

    /**
     * Sets the {@code compoundingMethod} property in the builder.
     * @param compoundingMethod  the new value, not null
     * @return this, for chaining, not null
     */
    public Builder compoundingMethod(CompoundingMethod compoundingMethod) {
      JodaBeanUtils.notNull(compoundingMethod, "compoundingMethod");
      this.compoundingMethod = compoundingMethod;
      return this;
    }

    //-----------------------------------------------------------------------
    @Override
    public String toString() {
      StringBuilder buf = new StringBuilder(160);
      buf.append("PaymentSchedule.Builder{");
      buf.append("paymentFrequency").append('=').append(JodaBeanUtils.toString(paymentFrequency)).append(',').append(' ');
      buf.append("paymentRelativeTo").append('=').append(JodaBeanUtils.toString(paymentRelativeTo)).append(',').append(' ');
      buf.append("paymentOffset").append('=').append(JodaBeanUtils.toString(paymentOffset)).append(',').append(' ');
      buf.append("compoundingMethod").append('=').append(JodaBeanUtils.toString(compoundingMethod));
      buf.append('}');
      return buf.toString();
    }

  }

  ///CLOVER:ON
  //-------------------------- AUTOGENERATED END --------------------------
}
