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

import org.joda.beans.Bean;
import org.joda.beans.ImmutableBean;
import org.joda.beans.JodaBeanUtils;
import org.joda.beans.MetaBean;
import org.joda.beans.MetaProperty;
import org.joda.beans.gen.BeanDefinition;
import org.joda.beans.gen.DerivedProperty;
import org.joda.beans.gen.PropertyDefinition;
import org.joda.beans.impl.direct.DirectFieldsBeanBuilder;
import org.joda.beans.impl.direct.DirectMetaBean;
import org.joda.beans.impl.direct.DirectMetaProperty;
import org.joda.beans.impl.direct.DirectMetaPropertyMap;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.opengamma.strata.basics.ReferenceData;
import com.opengamma.strata.basics.ReferenceDataNotFoundException;
import com.opengamma.strata.basics.currency.Currency;
import com.opengamma.strata.basics.date.AdjustableDate;
import com.opengamma.strata.basics.date.BusinessDayAdjustment;
import com.opengamma.strata.basics.date.DayCount;
import com.opengamma.strata.basics.date.DaysAdjustment;
import com.opengamma.strata.basics.index.Index;
import com.opengamma.strata.basics.schedule.PeriodicSchedule;
import com.opengamma.strata.basics.schedule.Schedule;
import com.opengamma.strata.basics.schedule.SchedulePeriod;
import com.opengamma.strata.basics.schedule.StubConvention;
import com.opengamma.strata.product.common.PayReceive;

/**
 * A rate swap leg defined using a parameterized schedule and calculation.
 * <p>
 * This defines a single swap leg paying a rate, such as an interest rate.
 * The rate may be fixed or floating, see {@link FixedRateCalculation},
 * {@link IborRateCalculation} and {@link OvernightRateCalculation}.
 * <p>
 * Interest is calculated based on <i>accrual periods</i> which follow a regular schedule
 * with optional initial and final stubs. Coupon payments are based on <i>payment periods</i>
 * which are typically the same as the accrual periods.
 * If the payment period is longer than the accrual period then compounding may apply.
 * The schedule of periods is defined using {@link PeriodicSchedule}, {@link PaymentSchedule},
 * {@link NotionalSchedule} and {@link ResetSchedule}.
 * <p>
 * If the schedule needs to be manually specified, or there are other unusual calculation
 * rules then the {@link RatePeriodSwapLeg} class should be used instead.
 */
@BeanDefinition
public final class RateCalculationSwapLeg
    implements ScheduledSwapLeg, ImmutableBean, Serializable {

  /**
   * Whether the leg is pay or receive.
   * <p>
   * A value of 'Pay' implies that the resulting amount is paid to the counterparty.
   * A value of 'Receive' implies that the resulting amount is received from the counterparty.
   * Note that negative interest rates can result in a payment in the opposite
   * direction to that implied by this indicator.
   */
  @PropertyDefinition(validate = "notNull", overrideGet = true)
  private final PayReceive payReceive;
  /**
   * The accrual schedule.
   * <p>
   * This is used to define the accrual periods.
   * These are used directly or indirectly to determine other dates in the swap.
   */
  @PropertyDefinition(validate = "notNull", overrideGet = true)
  private final PeriodicSchedule accrualSchedule;

  /**
   * The accrual schedule offset.
   * <p>
   * This is used to define offset of all accrual periods.
   */
  @PropertyDefinition
  private final DaysAdjustment accrualScheduleOffset;
  /**
   * The payment schedule.
   * <p>
   * This is used to define the payment periods, including any compounding.
   * The payment period dates are based on the accrual schedule.
   */
  @PropertyDefinition(validate = "notNull", overrideGet = true)
  private final PaymentSchedule paymentSchedule;
  /**
   * The notional schedule.
   * <p>
   * The notional amount schedule, which can vary during the lifetime of the swap.
   * In most cases, the notional amount is not exchanged, with only the net difference being exchanged.
   * However, in certain cases, initial, final or intermediate amounts are exchanged.
   */
  @PropertyDefinition(validate = "notNull")
  private final NotionalSchedule notionalSchedule;
  /**
   * The interest rate accrual calculation.
   * <p>
   * Different kinds of swap leg are determined by the subclass used here.
   * See {@link FixedRateCalculation}, {@link IborRateCalculation} and {@link OvernightRateCalculation}.
   */
  @PropertyDefinition(validate = "notNull")
  private final RateCalculation calculation;

  //-------------------------------------------------------------------------
  @Override
  @DerivedProperty
  public SwapLegType getType() {
    return calculation.getType();
  }

  @Override
  @DerivedProperty
  public AdjustableDate getStartDate() {
    return accrualSchedule.calculatedStartDate();
  }

  @Override
  @DerivedProperty
  public AdjustableDate getEndDate() {
    return accrualSchedule.calculatedEndDate();
  }

  @Override
  @DerivedProperty
  public Currency getCurrency() {
    return notionalSchedule.getCurrency();
  }

  @Override
  public void collectCurrencies(ImmutableSet.Builder<Currency> builder) {
    builder.add(getCurrency());
    calculation.collectCurrencies(builder);
    notionalSchedule.getFxReset().ifPresent(fxReset -> builder.add(fxReset.getReferenceCurrency()));
  }

  @Override
  public void collectIndices(ImmutableSet.Builder<Index> builder) {
    calculation.collectIndices(builder);
    notionalSchedule.getFxReset().ifPresent(fxReset -> builder.add(fxReset.getIndex()));
  }

  //-------------------------------------------------------------------------

  /**
   * Returns an instance based on this leg with the start date replaced.
   * <p>
   * This uses {@link PeriodicSchedule#replaceStartDate(LocalDate)}.
   *
   * @throws IllegalArgumentException if the start date cannot be replaced with the proposed start date
   */
  @Override
  public RateCalculationSwapLeg replaceStartDate(LocalDate adjustedStartDate) {
    return toBuilder().accrualSchedule(accrualSchedule.replaceStartDate(adjustedStartDate)).build();
  }

  /**
   * Converts this swap leg to the equivalent {@code ResolvedSwapLeg}.
   * <p>
   * An {@link ResolvedSwapLeg} represents the same data as this leg, but with
   * a complete schedule of dates defined using {@link RatePaymentPeriod}.
   *
   * @return the equivalent resolved swap leg
   * @throws ReferenceDataNotFoundException if an identifier cannot be resolved in the reference data
   * @throws RuntimeException if unable to resolve due to an invalid swap schedule or definition
   */
  @Override
  public ResolvedSwapLeg resolve(ReferenceData refData) {
    // Must check if pay/accr freq combination is acceptable by standard approach (accr -> pay schedules).
    // If combination is not acceptable (pay freq not multiple of accr freq), fallback to pay -> accr approach.
    if (!validPaymentAndAccrualFrequencyCombination()) {
      return resolveWithPaymentFrequencyDrivenPaymentPeriods(refData);
    }
    DayCount dayCount = calculation.getDayCount();
    Schedule resolvedAccruals = accrualSchedule.createSchedule(refData);
    if (accrualScheduleOffset != null) {
      resolvedAccruals = resolvedAccruals.toAdjusted(accrualScheduleOffset.resolve(refData));
    }

    Schedule resolvedPayments = paymentSchedule.createSchedule(resolvedAccruals, refData);
    List<RateAccrualPeriod> accrualPeriods =
        calculation.createAccrualPeriods(resolvedAccruals, resolvedPayments, refData);
    List<NotionalPaymentPeriod> payPeriods = paymentSchedule.createPaymentPeriods(
        resolvedAccruals, resolvedPayments, accrualPeriods, dayCount, notionalSchedule, payReceive, refData);
    LocalDate startDate = accrualPeriods.get(0).getStartDate();
    ImmutableList<SwapPaymentEvent> payEvents = notionalSchedule.createEvents(payPeriods, startDate, refData);
    return new ResolvedSwapLeg(getType(), payReceive, payPeriods, payEvents, getCurrency());
  }

  private boolean validPaymentAndAccrualFrequencyCombination() {
    try {
      paymentSchedule.getPaymentFrequency().exactDivide(accrualSchedule.getFrequency());
      return true;
    } catch (IllegalArgumentException ex) {
      boolean isTermPayFrequency = paymentSchedule.getPaymentFrequency().isTerm();
      boolean isWeekBased = accrualSchedule.getFrequency().isWeekBased();

      // short-circuit if accrual frequency is week-based, since payment frequency driven approach
      // should be used, even when payment freq == TERM
      if (isWeekBased) {
        return false;
      }

      // return false, unless payment frequency is TERM (where stubs can be used)
      return isTermPayFrequency;
    }
  }

  /**
   * Converts this swap leg to the equivalent {@code ResolvedSwapLeg} using payment driven approach.
   * When payment frequency is not a multiple of accrual frequency, whole accrual range is split into sub-ranges
   * driven by payment frequency. E.g. if accrual freq is 1W, payment freq is 1M and swap length is 3M, then
   * whole 3M accrual schedule is split into 3 sub-schedules (due to payment 1M freq):
   *    1. 0-1M
   *    2. 1-2M
   *    3. 2-3M
   * Each sub-schedule then should only have one TERM payment. When each sub-schedule is
   * resolved (payments with accruals created), then all are merged and result is returned.
   *
   * @return the equivalent resolved swap leg
   * @throws ReferenceDataNotFoundException if an identifier cannot be resolved in the reference data
   * @throws RuntimeException if unable to resolve due to an invalid swap schedule or definition
   */
  private ResolvedSwapLeg resolveWithPaymentFrequencyDrivenPaymentPeriods(ReferenceData refData) {
    PeriodicSchedule paymentPeriodicSchedule = PeriodicSchedule.builder()
        .startDate(accrualSchedule.getStartDate())
        .endDate(accrualSchedule.getEndDate())
        .frequency(paymentSchedule.getPaymentFrequency())
        .businessDayAdjustment(accrualSchedule.getBusinessDayAdjustment())
        .stubConvention(accrualSchedule.getStubConvention().orElse(StubConvention.SMART_INITIAL))
        .build();
    Schedule schedule = paymentPeriodicSchedule.createSchedule(refData);
    ImmutableList.Builder<NotionalPaymentPeriod> payPeriodsBuilder = ImmutableList.builder();
    for (SchedulePeriod p : schedule.getPeriods()) {
      PeriodicSchedule bucketSchedule =
          accrualSchedule.toBuilder().stubConvention(StubConvention.SHORT_FINAL).startDate(p.getStartDate())
              .endDate(p.getEndDate()).businessDayAdjustment(BusinessDayAdjustment.NONE)
              .build();
      payPeriodsBuilder.addAll(resolvePayPeriodsPerBucket(bucketSchedule, refData));
    }
    ImmutableList<NotionalPaymentPeriod> payPeriods = payPeriodsBuilder.build();
    LocalDate startDate = payPeriods.get(0).getStartDate();
    ImmutableList<SwapPaymentEvent> payEvents = notionalSchedule.createEvents(payPeriods, startDate, refData);
    return new ResolvedSwapLeg(getType(), payReceive, payPeriods, payEvents, getCurrency());
  }

  private List<NotionalPaymentPeriod> resolvePayPeriodsPerBucket(PeriodicSchedule bucketSchedule, ReferenceData refData) {
    DayCount dayCount = calculation.getDayCount();
    Schedule resolvedAccruals = bucketSchedule.createSchedule(refData, true);
    Schedule resolvedPayments = resolvedAccruals.mergeToTerm();
    List<RateAccrualPeriod> accrualPeriods =
        calculation.createAccrualPeriods(resolvedAccruals, resolvedPayments, refData);
    return paymentSchedule.createPaymentPeriods(resolvedAccruals, resolvedPayments, accrualPeriods, dayCount,
        notionalSchedule, payReceive, refData);
  }

  //------------------------- AUTOGENERATED START -------------------------
  /**
   * The meta-bean for {@code RateCalculationSwapLeg}.
   * @return the meta-bean, not null
   */
  public static RateCalculationSwapLeg.Meta meta() {
    return RateCalculationSwapLeg.Meta.INSTANCE;
  }

  static {
    MetaBean.register(RateCalculationSwapLeg.Meta.INSTANCE);
  }

  /**
   * The serialization version id.
   */
  private static final long serialVersionUID = 1L;

  /**
   * Returns a builder used to create an instance of the bean.
   * @return the builder, not null
   */
  public static RateCalculationSwapLeg.Builder builder() {
    return new RateCalculationSwapLeg.Builder();
  }

  private RateCalculationSwapLeg(
      PayReceive payReceive,
      PeriodicSchedule accrualSchedule,
      DaysAdjustment accrualScheduleOffset,
      PaymentSchedule paymentSchedule,
      NotionalSchedule notionalSchedule,
      RateCalculation calculation) {
    JodaBeanUtils.notNull(payReceive, "payReceive");
    JodaBeanUtils.notNull(accrualSchedule, "accrualSchedule");
    JodaBeanUtils.notNull(paymentSchedule, "paymentSchedule");
    JodaBeanUtils.notNull(notionalSchedule, "notionalSchedule");
    JodaBeanUtils.notNull(calculation, "calculation");
    this.payReceive = payReceive;
    this.accrualSchedule = accrualSchedule;
    this.accrualScheduleOffset = accrualScheduleOffset;
    this.paymentSchedule = paymentSchedule;
    this.notionalSchedule = notionalSchedule;
    this.calculation = calculation;
  }

  @Override
  public RateCalculationSwapLeg.Meta metaBean() {
    return RateCalculationSwapLeg.Meta.INSTANCE;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets whether the leg is pay or receive.
   * <p>
   * A value of 'Pay' implies that the resulting amount is paid to the counterparty.
   * A value of 'Receive' implies that the resulting amount is received from the counterparty.
   * Note that negative interest rates can result in a payment in the opposite
   * direction to that implied by this indicator.
   * @return the value of the property, not null
   */
  @Override
  public PayReceive getPayReceive() {
    return payReceive;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the accrual schedule.
   * <p>
   * This is used to define the accrual periods.
   * These are used directly or indirectly to determine other dates in the swap.
   * @return the value of the property, not null
   */
  @Override
  public PeriodicSchedule getAccrualSchedule() {
    return accrualSchedule;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the accrual schedule offset.
   * <p>
   * This is used to define offset of all accrual periods.
   * @return the value of the property
   */
  public DaysAdjustment getAccrualScheduleOffset() {
    return accrualScheduleOffset;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the payment schedule.
   * <p>
   * This is used to define the payment periods, including any compounding.
   * The payment period dates are based on the accrual schedule.
   * @return the value of the property, not null
   */
  @Override
  public PaymentSchedule getPaymentSchedule() {
    return paymentSchedule;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the notional schedule.
   * <p>
   * The notional amount schedule, which can vary during the lifetime of the swap.
   * In most cases, the notional amount is not exchanged, with only the net difference being exchanged.
   * However, in certain cases, initial, final or intermediate amounts are exchanged.
   * @return the value of the property, not null
   */
  public NotionalSchedule getNotionalSchedule() {
    return notionalSchedule;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the interest rate accrual calculation.
   * <p>
   * Different kinds of swap leg are determined by the subclass used here.
   * See {@link FixedRateCalculation}, {@link IborRateCalculation} and {@link OvernightRateCalculation}.
   * @return the value of the property, not null
   */
  public RateCalculation getCalculation() {
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
      RateCalculationSwapLeg other = (RateCalculationSwapLeg) obj;
      return JodaBeanUtils.equal(payReceive, other.payReceive) &&
          JodaBeanUtils.equal(accrualSchedule, other.accrualSchedule) &&
          JodaBeanUtils.equal(accrualScheduleOffset, other.accrualScheduleOffset) &&
          JodaBeanUtils.equal(paymentSchedule, other.paymentSchedule) &&
          JodaBeanUtils.equal(notionalSchedule, other.notionalSchedule) &&
          JodaBeanUtils.equal(calculation, other.calculation);
    }
    return false;
  }

  @Override
  public int hashCode() {
    int hash = getClass().hashCode();
    hash = hash * 31 + JodaBeanUtils.hashCode(payReceive);
    hash = hash * 31 + JodaBeanUtils.hashCode(accrualSchedule);
    hash = hash * 31 + JodaBeanUtils.hashCode(accrualScheduleOffset);
    hash = hash * 31 + JodaBeanUtils.hashCode(paymentSchedule);
    hash = hash * 31 + JodaBeanUtils.hashCode(notionalSchedule);
    hash = hash * 31 + JodaBeanUtils.hashCode(calculation);
    return hash;
  }

  @Override
  public String toString() {
    StringBuilder buf = new StringBuilder(352);
    buf.append("RateCalculationSwapLeg{");
    buf.append("payReceive").append('=').append(JodaBeanUtils.toString(payReceive)).append(',').append(' ');
    buf.append("accrualSchedule").append('=').append(JodaBeanUtils.toString(accrualSchedule)).append(',').append(' ');
    buf.append("accrualScheduleOffset").append('=').append(JodaBeanUtils.toString(accrualScheduleOffset)).append(',').append(' ');
    buf.append("paymentSchedule").append('=').append(JodaBeanUtils.toString(paymentSchedule)).append(',').append(' ');
    buf.append("notionalSchedule").append('=').append(JodaBeanUtils.toString(notionalSchedule)).append(',').append(' ');
    buf.append("calculation").append('=').append(JodaBeanUtils.toString(calculation)).append(',').append(' ');
    buf.append("type").append('=').append(JodaBeanUtils.toString(getType())).append(',').append(' ');
    buf.append("startDate").append('=').append(JodaBeanUtils.toString(getStartDate())).append(',').append(' ');
    buf.append("endDate").append('=').append(JodaBeanUtils.toString(getEndDate())).append(',').append(' ');
    buf.append("currency").append('=').append(JodaBeanUtils.toString(getCurrency()));
    buf.append('}');
    return buf.toString();
  }

  //-----------------------------------------------------------------------
  /**
   * The meta-bean for {@code RateCalculationSwapLeg}.
   */
  public static final class Meta extends DirectMetaBean {
    /**
     * The singleton instance of the meta-bean.
     */
    static final Meta INSTANCE = new Meta();

    /**
     * The meta-property for the {@code payReceive} property.
     */
    private final MetaProperty<PayReceive> payReceive = DirectMetaProperty.ofImmutable(
        this, "payReceive", RateCalculationSwapLeg.class, PayReceive.class);
    /**
     * The meta-property for the {@code accrualSchedule} property.
     */
    private final MetaProperty<PeriodicSchedule> accrualSchedule = DirectMetaProperty.ofImmutable(
        this, "accrualSchedule", RateCalculationSwapLeg.class, PeriodicSchedule.class);
    /**
     * The meta-property for the {@code accrualScheduleOffset} property.
     */
    private final MetaProperty<DaysAdjustment> accrualScheduleOffset = DirectMetaProperty.ofImmutable(
        this, "accrualScheduleOffset", RateCalculationSwapLeg.class, DaysAdjustment.class);
    /**
     * The meta-property for the {@code paymentSchedule} property.
     */
    private final MetaProperty<PaymentSchedule> paymentSchedule = DirectMetaProperty.ofImmutable(
        this, "paymentSchedule", RateCalculationSwapLeg.class, PaymentSchedule.class);
    /**
     * The meta-property for the {@code notionalSchedule} property.
     */
    private final MetaProperty<NotionalSchedule> notionalSchedule = DirectMetaProperty.ofImmutable(
        this, "notionalSchedule", RateCalculationSwapLeg.class, NotionalSchedule.class);
    /**
     * The meta-property for the {@code calculation} property.
     */
    private final MetaProperty<RateCalculation> calculation = DirectMetaProperty.ofImmutable(
        this, "calculation", RateCalculationSwapLeg.class, RateCalculation.class);
    /**
     * The meta-property for the {@code type} property.
     */
    private final MetaProperty<SwapLegType> type = DirectMetaProperty.ofDerived(
        this, "type", RateCalculationSwapLeg.class, SwapLegType.class);
    /**
     * The meta-property for the {@code startDate} property.
     */
    private final MetaProperty<AdjustableDate> startDate = DirectMetaProperty.ofDerived(
        this, "startDate", RateCalculationSwapLeg.class, AdjustableDate.class);
    /**
     * The meta-property for the {@code endDate} property.
     */
    private final MetaProperty<AdjustableDate> endDate = DirectMetaProperty.ofDerived(
        this, "endDate", RateCalculationSwapLeg.class, AdjustableDate.class);
    /**
     * The meta-property for the {@code currency} property.
     */
    private final MetaProperty<Currency> currency = DirectMetaProperty.ofDerived(
        this, "currency", RateCalculationSwapLeg.class, Currency.class);
    /**
     * The meta-properties.
     */
    private final Map<String, MetaProperty<?>> metaPropertyMap$ = new DirectMetaPropertyMap(
        this, null,
        "payReceive",
        "accrualSchedule",
        "accrualScheduleOffset",
        "paymentSchedule",
        "notionalSchedule",
        "calculation",
        "type",
        "startDate",
        "endDate",
        "currency");

    /**
     * Restricted constructor.
     */
    private Meta() {
    }

    @Override
    protected MetaProperty<?> metaPropertyGet(String propertyName) {
      switch (propertyName.hashCode()) {
        case -885469925:  // payReceive
          return payReceive;
        case 304659814:  // accrualSchedule
          return accrualSchedule;
        case 1849616793:  // accrualScheduleOffset
          return accrualScheduleOffset;
        case -1499086147:  // paymentSchedule
          return paymentSchedule;
        case 1447860727:  // notionalSchedule
          return notionalSchedule;
        case -934682935:  // calculation
          return calculation;
        case 3575610:  // type
          return type;
        case -2129778896:  // startDate
          return startDate;
        case -1607727319:  // endDate
          return endDate;
        case 575402001:  // currency
          return currency;
      }
      return super.metaPropertyGet(propertyName);
    }

    @Override
    public RateCalculationSwapLeg.Builder builder() {
      return new RateCalculationSwapLeg.Builder();
    }

    @Override
    public Class<? extends RateCalculationSwapLeg> beanType() {
      return RateCalculationSwapLeg.class;
    }

    @Override
    public Map<String, MetaProperty<?>> metaPropertyMap() {
      return metaPropertyMap$;
    }

    //-----------------------------------------------------------------------
    /**
     * The meta-property for the {@code payReceive} property.
     * @return the meta-property, not null
     */
    public MetaProperty<PayReceive> payReceive() {
      return payReceive;
    }

    /**
     * The meta-property for the {@code accrualSchedule} property.
     * @return the meta-property, not null
     */
    public MetaProperty<PeriodicSchedule> accrualSchedule() {
      return accrualSchedule;
    }

    /**
     * The meta-property for the {@code accrualScheduleOffset} property.
     * @return the meta-property, not null
     */
    public MetaProperty<DaysAdjustment> accrualScheduleOffset() {
      return accrualScheduleOffset;
    }

    /**
     * The meta-property for the {@code paymentSchedule} property.
     * @return the meta-property, not null
     */
    public MetaProperty<PaymentSchedule> paymentSchedule() {
      return paymentSchedule;
    }

    /**
     * The meta-property for the {@code notionalSchedule} property.
     * @return the meta-property, not null
     */
    public MetaProperty<NotionalSchedule> notionalSchedule() {
      return notionalSchedule;
    }

    /**
     * The meta-property for the {@code calculation} property.
     * @return the meta-property, not null
     */
    public MetaProperty<RateCalculation> calculation() {
      return calculation;
    }

    /**
     * The meta-property for the {@code type} property.
     * @return the meta-property, not null
     */
    public MetaProperty<SwapLegType> type() {
      return type;
    }

    /**
     * The meta-property for the {@code startDate} property.
     * @return the meta-property, not null
     */
    public MetaProperty<AdjustableDate> startDate() {
      return startDate;
    }

    /**
     * The meta-property for the {@code endDate} property.
     * @return the meta-property, not null
     */
    public MetaProperty<AdjustableDate> endDate() {
      return endDate;
    }

    /**
     * The meta-property for the {@code currency} property.
     * @return the meta-property, not null
     */
    public MetaProperty<Currency> currency() {
      return currency;
    }

    //-----------------------------------------------------------------------
    @Override
    protected Object propertyGet(Bean bean, String propertyName, boolean quiet) {
      switch (propertyName.hashCode()) {
        case -885469925:  // payReceive
          return ((RateCalculationSwapLeg) bean).getPayReceive();
        case 304659814:  // accrualSchedule
          return ((RateCalculationSwapLeg) bean).getAccrualSchedule();
        case 1849616793:  // accrualScheduleOffset
          return ((RateCalculationSwapLeg) bean).getAccrualScheduleOffset();
        case -1499086147:  // paymentSchedule
          return ((RateCalculationSwapLeg) bean).getPaymentSchedule();
        case 1447860727:  // notionalSchedule
          return ((RateCalculationSwapLeg) bean).getNotionalSchedule();
        case -934682935:  // calculation
          return ((RateCalculationSwapLeg) bean).getCalculation();
        case 3575610:  // type
          return ((RateCalculationSwapLeg) bean).getType();
        case -2129778896:  // startDate
          return ((RateCalculationSwapLeg) bean).getStartDate();
        case -1607727319:  // endDate
          return ((RateCalculationSwapLeg) bean).getEndDate();
        case 575402001:  // currency
          return ((RateCalculationSwapLeg) bean).getCurrency();
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
   * The bean-builder for {@code RateCalculationSwapLeg}.
   */
  public static final class Builder extends DirectFieldsBeanBuilder<RateCalculationSwapLeg> {

    private PayReceive payReceive;
    private PeriodicSchedule accrualSchedule;
    private DaysAdjustment accrualScheduleOffset;
    private PaymentSchedule paymentSchedule;
    private NotionalSchedule notionalSchedule;
    private RateCalculation calculation;

    /**
     * Restricted constructor.
     */
    private Builder() {
    }

    /**
     * Restricted copy constructor.
     * @param beanToCopy  the bean to copy from, not null
     */
    private Builder(RateCalculationSwapLeg beanToCopy) {
      this.payReceive = beanToCopy.getPayReceive();
      this.accrualSchedule = beanToCopy.getAccrualSchedule();
      this.accrualScheduleOffset = beanToCopy.getAccrualScheduleOffset();
      this.paymentSchedule = beanToCopy.getPaymentSchedule();
      this.notionalSchedule = beanToCopy.getNotionalSchedule();
      this.calculation = beanToCopy.getCalculation();
    }

    //-----------------------------------------------------------------------
    @Override
    public Object get(String propertyName) {
      switch (propertyName.hashCode()) {
        case -885469925:  // payReceive
          return payReceive;
        case 304659814:  // accrualSchedule
          return accrualSchedule;
        case 1849616793:  // accrualScheduleOffset
          return accrualScheduleOffset;
        case -1499086147:  // paymentSchedule
          return paymentSchedule;
        case 1447860727:  // notionalSchedule
          return notionalSchedule;
        case -934682935:  // calculation
          return calculation;
        default:
          throw new NoSuchElementException("Unknown property: " + propertyName);
      }
    }

    @Override
    public Builder set(String propertyName, Object newValue) {
      switch (propertyName.hashCode()) {
        case -885469925:  // payReceive
          this.payReceive = (PayReceive) newValue;
          break;
        case 304659814:  // accrualSchedule
          this.accrualSchedule = (PeriodicSchedule) newValue;
          break;
        case 1849616793:  // accrualScheduleOffset
          this.accrualScheduleOffset = (DaysAdjustment) newValue;
          break;
        case -1499086147:  // paymentSchedule
          this.paymentSchedule = (PaymentSchedule) newValue;
          break;
        case 1447860727:  // notionalSchedule
          this.notionalSchedule = (NotionalSchedule) newValue;
          break;
        case -934682935:  // calculation
          this.calculation = (RateCalculation) newValue;
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
    public RateCalculationSwapLeg build() {
      return new RateCalculationSwapLeg(
          payReceive,
          accrualSchedule,
          accrualScheduleOffset,
          paymentSchedule,
          notionalSchedule,
          calculation);
    }

    //-----------------------------------------------------------------------
    /**
     * Sets whether the leg is pay or receive.
     * <p>
     * A value of 'Pay' implies that the resulting amount is paid to the counterparty.
     * A value of 'Receive' implies that the resulting amount is received from the counterparty.
     * Note that negative interest rates can result in a payment in the opposite
     * direction to that implied by this indicator.
     * @param payReceive  the new value, not null
     * @return this, for chaining, not null
     */
    public Builder payReceive(PayReceive payReceive) {
      JodaBeanUtils.notNull(payReceive, "payReceive");
      this.payReceive = payReceive;
      return this;
    }

    /**
     * Sets the accrual schedule.
     * <p>
     * This is used to define the accrual periods.
     * These are used directly or indirectly to determine other dates in the swap.
     * @param accrualSchedule  the new value, not null
     * @return this, for chaining, not null
     */
    public Builder accrualSchedule(PeriodicSchedule accrualSchedule) {
      JodaBeanUtils.notNull(accrualSchedule, "accrualSchedule");
      this.accrualSchedule = accrualSchedule;
      return this;
    }

    /**
     * Sets the accrual schedule offset.
     * <p>
     * This is used to define offset of all accrual periods.
     * @param accrualScheduleOffset  the new value
     * @return this, for chaining, not null
     */
    public Builder accrualScheduleOffset(DaysAdjustment accrualScheduleOffset) {
      this.accrualScheduleOffset = accrualScheduleOffset;
      return this;
    }

    /**
     * Sets the payment schedule.
     * <p>
     * This is used to define the payment periods, including any compounding.
     * The payment period dates are based on the accrual schedule.
     * @param paymentSchedule  the new value, not null
     * @return this, for chaining, not null
     */
    public Builder paymentSchedule(PaymentSchedule paymentSchedule) {
      JodaBeanUtils.notNull(paymentSchedule, "paymentSchedule");
      this.paymentSchedule = paymentSchedule;
      return this;
    }

    /**
     * Sets the notional schedule.
     * <p>
     * The notional amount schedule, which can vary during the lifetime of the swap.
     * In most cases, the notional amount is not exchanged, with only the net difference being exchanged.
     * However, in certain cases, initial, final or intermediate amounts are exchanged.
     * @param notionalSchedule  the new value, not null
     * @return this, for chaining, not null
     */
    public Builder notionalSchedule(NotionalSchedule notionalSchedule) {
      JodaBeanUtils.notNull(notionalSchedule, "notionalSchedule");
      this.notionalSchedule = notionalSchedule;
      return this;
    }

    /**
     * Sets the interest rate accrual calculation.
     * <p>
     * Different kinds of swap leg are determined by the subclass used here.
     * See {@link FixedRateCalculation}, {@link IborRateCalculation} and {@link OvernightRateCalculation}.
     * @param calculation  the new value, not null
     * @return this, for chaining, not null
     */
    public Builder calculation(RateCalculation calculation) {
      JodaBeanUtils.notNull(calculation, "calculation");
      this.calculation = calculation;
      return this;
    }

    //-----------------------------------------------------------------------
    @Override
    public String toString() {
      StringBuilder buf = new StringBuilder(352);
      buf.append("RateCalculationSwapLeg.Builder{");
      buf.append("payReceive").append('=').append(JodaBeanUtils.toString(payReceive)).append(',').append(' ');
      buf.append("accrualSchedule").append('=').append(JodaBeanUtils.toString(accrualSchedule)).append(',').append(' ');
      buf.append("accrualScheduleOffset").append('=').append(JodaBeanUtils.toString(accrualScheduleOffset)).append(',').append(' ');
      buf.append("paymentSchedule").append('=').append(JodaBeanUtils.toString(paymentSchedule)).append(',').append(' ');
      buf.append("notionalSchedule").append('=').append(JodaBeanUtils.toString(notionalSchedule)).append(',').append(' ');
      buf.append("calculation").append('=').append(JodaBeanUtils.toString(calculation)).append(',').append(' ');
      buf.append("type").append('=').append(JodaBeanUtils.toString(null)).append(',').append(' ');
      buf.append("startDate").append('=').append(JodaBeanUtils.toString(null)).append(',').append(' ');
      buf.append("endDate").append('=').append(JodaBeanUtils.toString(null)).append(',').append(' ');
      buf.append("currency").append('=').append(JodaBeanUtils.toString(null));
      buf.append('}');
      return buf.toString();
    }

  }

  //-------------------------- AUTOGENERATED END --------------------------
}
