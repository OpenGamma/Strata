/*
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.product.swap;

import java.io.Serializable;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;

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
import com.opengamma.strata.basics.ReferenceData;
import com.opengamma.strata.basics.ReferenceDataNotFoundException;
import com.opengamma.strata.basics.currency.Currency;
import com.opengamma.strata.basics.currency.CurrencyAmount;
import com.opengamma.strata.basics.currency.Payment;
import com.opengamma.strata.basics.date.BusinessDayAdjustment;
import com.opengamma.strata.basics.date.DateAdjuster;
import com.opengamma.strata.basics.date.DayCount;
import com.opengamma.strata.basics.date.DaysAdjustment;
import com.opengamma.strata.basics.schedule.Frequency;
import com.opengamma.strata.basics.schedule.Schedule;
import com.opengamma.strata.basics.schedule.SchedulePeriod;
import com.opengamma.strata.collect.array.DoubleArray;
import com.opengamma.strata.product.common.PayReceive;

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
 * <p>
 * A different business day adjustment may be specified for the payment schedule to that
 * used for the accrual schedule. When resolving the swap, the adjustment will be applied
 * as part of the process that creates the payment date. Note that the start and end dates
 * of the payment period, as defined by the payment schedule, cannot be observed on the
 * resulting {@link RatePaymentPeriod} instance.
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
   * The business day adjustment to apply, optional.
   * <p>
   * Each date in the calculated schedule is determined relative to the accrual schedule.
   * Normally, the accrual schedule is adjusted ensuring each date is not a holiday.
   * As such, there is typically no reason to adjust the date before applying the payment date offset.
   * <p>
   * If the accrual dates are unadjusted, or for some other reason, it may be
   * desirable to adjust the schedule dates before applying the payment date offset.
   * This optional property allows that to happen.
   * Note that the payment date offset itself provides the ability to adjust dates
   * after the offset is applied.
   */
  @PropertyDefinition(get = "optional")
  private final BusinessDayAdjustment businessDayAdjustment;
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
  private final DaysAdjustment paymentDateOffset;
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
   * @param refData  the reference data to use when resolving
   * @return the payment schedule
   * @throws ReferenceDataNotFoundException if an identifier cannot be resolved in the reference data
   * @throws IllegalArgumentException if the accrual frequency does not divide evenly into the payment frequency
   */
  public Schedule createSchedule(Schedule accrualSchedule, ReferenceData refData) {
    // payment frequency of Term absorbs everything
    if (paymentFrequency.equals(Frequency.TERM)) {
      return accrualSchedule.mergeToTerm();
    }
    // derive schedule, retaining stubs as payment periods
    int accrualPeriodsPerPayment = paymentFrequency.exactDivide(accrualSchedule.getFrequency());
    boolean rollForwards = !accrualSchedule.getInitialStub().isPresent();
    Schedule paySchedule = accrualSchedule.mergeRegular(accrualPeriodsPerPayment, rollForwards);
    if (businessDayAdjustment != null) {
      return paySchedule.toAdjusted(businessDayAdjustment.resolve(refData));
    }
    return paySchedule;
  }

  //-------------------------------------------------------------------------
  /**
   * Builds the list of payment periods from the list of accrual periods.
   * <p>
   * This applies the payment schedule.
   * 
   * @param accrualSchedule  the accrual schedule
   * @param paymentSchedule  the payment schedule
   * @param accrualPeriods  the list of accrual periods
   * @param dayCount  the day count
   * @param notionalSchedule  the schedule of notionals
   * @param payReceive  the pay-receive flag
   * @param refData  the reference data to use when resolving
   * @return the list of payment periods
   */
  ImmutableList<NotionalPaymentPeriod> createPaymentPeriods(
      Schedule accrualSchedule,
      Schedule paymentSchedule,
      List<RateAccrualPeriod> accrualPeriods,
      DayCount dayCount,
      NotionalSchedule notionalSchedule,
      PayReceive payReceive,
      ReferenceData refData) {

    DoubleArray notionals = notionalSchedule.getAmount().resolveValues(paymentSchedule);
    // resolve against reference data once
    DateAdjuster paymentDateAdjuster = paymentDateOffset.resolve(refData);
    Function<SchedulePeriod, FxReset> fxResetFn =
        notionalSchedule.getFxReset().map(calc -> calc.resolve(refData)).orElse(p -> null);
    // build up payment periods using schedule
    Currency currency = notionalSchedule.getCurrency();
    ImmutableList.Builder<NotionalPaymentPeriod> paymentPeriods = ImmutableList.builder();
    // compare using == as Schedule.mergeRegular() will return same schedule
    if (accrualSchedule == paymentSchedule) {
      // same schedule means one accrual period per payment period
      for (int index = 0; index < paymentSchedule.size(); index++) {
        SchedulePeriod period = paymentSchedule.getPeriod(index);
        double notional = payReceive.normalize(notionals.get(index));
        ImmutableList<RateAccrualPeriod> paymentAccrualPeriods = ImmutableList.of(accrualPeriods.get(index));
        paymentPeriods.add(createPaymentPeriod(
            period, paymentAccrualPeriods, paymentDateAdjuster, fxResetFn, dayCount, currency, notional));
      }
    } else {
      // multiple accrual periods per payment period, or accrual/payment schedules differ
      int accrualIndex = 0;
      for (int paymentIndex = 0; paymentIndex < paymentSchedule.size(); paymentIndex++) {
        SchedulePeriod payPeriod = paymentSchedule.getPeriod(paymentIndex);
        double notional = payReceive.normalize(notionals.get(paymentIndex));
        int accrualStartIndex = accrualIndex;
        RateAccrualPeriod accrual = accrualPeriods.get(accrualIndex);
        while (accrual.getUnadjustedEndDate().isBefore(payPeriod.getUnadjustedEndDate())) {
          accrual = accrualPeriods.get(++accrualIndex);
        }
        List<RateAccrualPeriod> paymentAccrualPeriods = accrualPeriods.subList(accrualStartIndex, accrualIndex + 1);
        paymentPeriods.add(createPaymentPeriod(
            payPeriod, paymentAccrualPeriods, paymentDateAdjuster, fxResetFn, dayCount, currency, notional));
        accrualIndex++;
      }
    }
    return paymentPeriods.build();
  }

  // create the payment period
  private NotionalPaymentPeriod createPaymentPeriod(
      SchedulePeriod paymentPeriod,
      List<RateAccrualPeriod> periods,
      DateAdjuster paymentDateAdjuster,
      Function<SchedulePeriod, FxReset> fxResetFn,
      DayCount dayCount,
      Currency currency,
      double notional) {

    // FpML cash flow example 3 shows payment offset calculated from adjusted accrual date (not unadjusted)
    LocalDate paymentDate = paymentDateAdjuster.adjust(paymentRelativeTo.selectBaseDate(paymentPeriod));

    // extract FX reset information
    FxReset fxReset = fxResetFn.apply(paymentPeriod);

    // handle special case where amount is known
    if (periods.size() == 1 && periods.get(0).getRateComputation() instanceof KnownAmountRateComputation) {
      CurrencyAmount amount = ((KnownAmountRateComputation) periods.get(0).getRateComputation()).getAmount();
      Payment payment = Payment.of(amount, paymentDate);
      if (fxReset != null) {
        CurrencyAmount notionalAmount = CurrencyAmount.of(fxReset.getReferenceCurrency(), notional);
        return KnownAmountNotionalSwapPaymentPeriod.of(payment, paymentPeriod, notionalAmount, fxReset.getObservation());
      } else {
        CurrencyAmount notionalAmount = CurrencyAmount.of(currency, notional);
        return KnownAmountNotionalSwapPaymentPeriod.of(payment, paymentPeriod, notionalAmount);
      }
    }
    // rate based computation
    return new RatePaymentPeriod(paymentDate, periods, dayCount, currency, fxReset, notional, compoundingMethod);
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
      BusinessDayAdjustment businessDayAdjustment,
      PaymentRelativeTo paymentRelativeTo,
      DaysAdjustment paymentDateOffset,
      CompoundingMethod compoundingMethod) {
    JodaBeanUtils.notNull(paymentFrequency, "paymentFrequency");
    JodaBeanUtils.notNull(paymentRelativeTo, "paymentRelativeTo");
    JodaBeanUtils.notNull(paymentDateOffset, "paymentDateOffset");
    JodaBeanUtils.notNull(compoundingMethod, "compoundingMethod");
    this.paymentFrequency = paymentFrequency;
    this.businessDayAdjustment = businessDayAdjustment;
    this.paymentRelativeTo = paymentRelativeTo;
    this.paymentDateOffset = paymentDateOffset;
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
   * Gets the business day adjustment to apply, optional.
   * <p>
   * Each date in the calculated schedule is determined relative to the accrual schedule.
   * Normally, the accrual schedule is adjusted ensuring each date is not a holiday.
   * As such, there is typically no reason to adjust the date before applying the payment date offset.
   * <p>
   * If the accrual dates are unadjusted, or for some other reason, it may be
   * desirable to adjust the schedule dates before applying the payment date offset.
   * This optional property allows that to happen.
   * Note that the payment date offset itself provides the ability to adjust dates
   * after the offset is applied.
   * @return the optional value of the property, not null
   */
  public Optional<BusinessDayAdjustment> getBusinessDayAdjustment() {
    return Optional.ofNullable(businessDayAdjustment);
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
  public DaysAdjustment getPaymentDateOffset() {
    return paymentDateOffset;
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
      return JodaBeanUtils.equal(paymentFrequency, other.paymentFrequency) &&
          JodaBeanUtils.equal(businessDayAdjustment, other.businessDayAdjustment) &&
          JodaBeanUtils.equal(paymentRelativeTo, other.paymentRelativeTo) &&
          JodaBeanUtils.equal(paymentDateOffset, other.paymentDateOffset) &&
          JodaBeanUtils.equal(compoundingMethod, other.compoundingMethod);
    }
    return false;
  }

  @Override
  public int hashCode() {
    int hash = getClass().hashCode();
    hash = hash * 31 + JodaBeanUtils.hashCode(paymentFrequency);
    hash = hash * 31 + JodaBeanUtils.hashCode(businessDayAdjustment);
    hash = hash * 31 + JodaBeanUtils.hashCode(paymentRelativeTo);
    hash = hash * 31 + JodaBeanUtils.hashCode(paymentDateOffset);
    hash = hash * 31 + JodaBeanUtils.hashCode(compoundingMethod);
    return hash;
  }

  @Override
  public String toString() {
    StringBuilder buf = new StringBuilder(192);
    buf.append("PaymentSchedule{");
    buf.append("paymentFrequency").append('=').append(paymentFrequency).append(',').append(' ');
    buf.append("businessDayAdjustment").append('=').append(businessDayAdjustment).append(',').append(' ');
    buf.append("paymentRelativeTo").append('=').append(paymentRelativeTo).append(',').append(' ');
    buf.append("paymentDateOffset").append('=').append(paymentDateOffset).append(',').append(' ');
    buf.append("compoundingMethod").append('=').append(JodaBeanUtils.toString(compoundingMethod));
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
     * The meta-property for the {@code businessDayAdjustment} property.
     */
    private final MetaProperty<BusinessDayAdjustment> businessDayAdjustment = DirectMetaProperty.ofImmutable(
        this, "businessDayAdjustment", PaymentSchedule.class, BusinessDayAdjustment.class);
    /**
     * The meta-property for the {@code paymentRelativeTo} property.
     */
    private final MetaProperty<PaymentRelativeTo> paymentRelativeTo = DirectMetaProperty.ofImmutable(
        this, "paymentRelativeTo", PaymentSchedule.class, PaymentRelativeTo.class);
    /**
     * The meta-property for the {@code paymentDateOffset} property.
     */
    private final MetaProperty<DaysAdjustment> paymentDateOffset = DirectMetaProperty.ofImmutable(
        this, "paymentDateOffset", PaymentSchedule.class, DaysAdjustment.class);
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
        "businessDayAdjustment",
        "paymentRelativeTo",
        "paymentDateOffset",
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
        case -1065319863:  // businessDayAdjustment
          return businessDayAdjustment;
        case -1357627123:  // paymentRelativeTo
          return paymentRelativeTo;
        case -716438393:  // paymentDateOffset
          return paymentDateOffset;
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
     * The meta-property for the {@code businessDayAdjustment} property.
     * @return the meta-property, not null
     */
    public MetaProperty<BusinessDayAdjustment> businessDayAdjustment() {
      return businessDayAdjustment;
    }

    /**
     * The meta-property for the {@code paymentRelativeTo} property.
     * @return the meta-property, not null
     */
    public MetaProperty<PaymentRelativeTo> paymentRelativeTo() {
      return paymentRelativeTo;
    }

    /**
     * The meta-property for the {@code paymentDateOffset} property.
     * @return the meta-property, not null
     */
    public MetaProperty<DaysAdjustment> paymentDateOffset() {
      return paymentDateOffset;
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
        case -1065319863:  // businessDayAdjustment
          return ((PaymentSchedule) bean).businessDayAdjustment;
        case -1357627123:  // paymentRelativeTo
          return ((PaymentSchedule) bean).getPaymentRelativeTo();
        case -716438393:  // paymentDateOffset
          return ((PaymentSchedule) bean).getPaymentDateOffset();
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
    private BusinessDayAdjustment businessDayAdjustment;
    private PaymentRelativeTo paymentRelativeTo;
    private DaysAdjustment paymentDateOffset;
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
      this.businessDayAdjustment = beanToCopy.businessDayAdjustment;
      this.paymentRelativeTo = beanToCopy.getPaymentRelativeTo();
      this.paymentDateOffset = beanToCopy.getPaymentDateOffset();
      this.compoundingMethod = beanToCopy.getCompoundingMethod();
    }

    //-----------------------------------------------------------------------
    @Override
    public Object get(String propertyName) {
      switch (propertyName.hashCode()) {
        case 863656438:  // paymentFrequency
          return paymentFrequency;
        case -1065319863:  // businessDayAdjustment
          return businessDayAdjustment;
        case -1357627123:  // paymentRelativeTo
          return paymentRelativeTo;
        case -716438393:  // paymentDateOffset
          return paymentDateOffset;
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
        case -1065319863:  // businessDayAdjustment
          this.businessDayAdjustment = (BusinessDayAdjustment) newValue;
          break;
        case -1357627123:  // paymentRelativeTo
          this.paymentRelativeTo = (PaymentRelativeTo) newValue;
          break;
        case -716438393:  // paymentDateOffset
          this.paymentDateOffset = (DaysAdjustment) newValue;
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
    public PaymentSchedule build() {
      return new PaymentSchedule(
          paymentFrequency,
          businessDayAdjustment,
          paymentRelativeTo,
          paymentDateOffset,
          compoundingMethod);
    }

    //-----------------------------------------------------------------------
    /**
     * Sets the periodic frequency of payments.
     * <p>
     * Regular payments will be made at the specified periodic frequency.
     * The frequency must be the same as, or a multiple of, the accrual periodic frequency.
     * <p>
     * Compounding applies if the payment frequency does not equal the accrual frequency.
     * @param paymentFrequency  the new value, not null
     * @return this, for chaining, not null
     */
    public Builder paymentFrequency(Frequency paymentFrequency) {
      JodaBeanUtils.notNull(paymentFrequency, "paymentFrequency");
      this.paymentFrequency = paymentFrequency;
      return this;
    }

    /**
     * Sets the business day adjustment to apply, optional.
     * <p>
     * Each date in the calculated schedule is determined relative to the accrual schedule.
     * Normally, the accrual schedule is adjusted ensuring each date is not a holiday.
     * As such, there is typically no reason to adjust the date before applying the payment date offset.
     * <p>
     * If the accrual dates are unadjusted, or for some other reason, it may be
     * desirable to adjust the schedule dates before applying the payment date offset.
     * This optional property allows that to happen.
     * Note that the payment date offset itself provides the ability to adjust dates
     * after the offset is applied.
     * @param businessDayAdjustment  the new value
     * @return this, for chaining, not null
     */
    public Builder businessDayAdjustment(BusinessDayAdjustment businessDayAdjustment) {
      this.businessDayAdjustment = businessDayAdjustment;
      return this;
    }

    /**
     * Sets the base date that each payment is made relative to, defaulted to 'PeriodEnd'.
     * <p>
     * The payment date is relative to either the start or end of the payment period.
     * @param paymentRelativeTo  the new value, not null
     * @return this, for chaining, not null
     */
    public Builder paymentRelativeTo(PaymentRelativeTo paymentRelativeTo) {
      JodaBeanUtils.notNull(paymentRelativeTo, "paymentRelativeTo");
      this.paymentRelativeTo = paymentRelativeTo;
      return this;
    }

    /**
     * Sets the offset of payment from the base calculation period date.
     * <p>
     * The offset is applied to the unadjusted date specified by {@code paymentRelativeTo}.
     * Offset can be based on calendar days or business days.
     * @param paymentDateOffset  the new value, not null
     * @return this, for chaining, not null
     */
    public Builder paymentDateOffset(DaysAdjustment paymentDateOffset) {
      JodaBeanUtils.notNull(paymentDateOffset, "paymentDateOffset");
      this.paymentDateOffset = paymentDateOffset;
      return this;
    }

    /**
     * Sets the compounding method to use when there is more than one accrual period, defaulted to 'None'.
     * <p>
     * Compounding is used when combining accrual periods.
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
      StringBuilder buf = new StringBuilder(192);
      buf.append("PaymentSchedule.Builder{");
      buf.append("paymentFrequency").append('=').append(JodaBeanUtils.toString(paymentFrequency)).append(',').append(' ');
      buf.append("businessDayAdjustment").append('=').append(JodaBeanUtils.toString(businessDayAdjustment)).append(',').append(' ');
      buf.append("paymentRelativeTo").append('=').append(JodaBeanUtils.toString(paymentRelativeTo)).append(',').append(' ');
      buf.append("paymentDateOffset").append('=').append(JodaBeanUtils.toString(paymentDateOffset)).append(',').append(' ');
      buf.append("compoundingMethod").append('=').append(JodaBeanUtils.toString(compoundingMethod));
      buf.append('}');
      return buf.toString();
    }

  }

  ///CLOVER:ON
  //-------------------------- AUTOGENERATED END --------------------------
}
