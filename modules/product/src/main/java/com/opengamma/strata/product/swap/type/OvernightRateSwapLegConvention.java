/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.product.swap.type;

import static com.opengamma.strata.basics.date.BusinessDayConventions.MODIFIED_FOLLOWING;

import java.io.Serializable;
import java.time.LocalDate;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.Set;

import org.joda.beans.Bean;
import org.joda.beans.BeanDefinition;
import org.joda.beans.ImmutableBean;
import org.joda.beans.ImmutableDefaults;
import org.joda.beans.ImmutableValidator;
import org.joda.beans.JodaBeanUtils;
import org.joda.beans.MetaProperty;
import org.joda.beans.Property;
import org.joda.beans.PropertyDefinition;
import org.joda.beans.impl.direct.DirectFieldsBeanBuilder;
import org.joda.beans.impl.direct.DirectMetaBean;
import org.joda.beans.impl.direct.DirectMetaProperty;
import org.joda.beans.impl.direct.DirectMetaPropertyMap;

import com.opengamma.strata.basics.currency.Currency;
import com.opengamma.strata.basics.date.BusinessDayAdjustment;
import com.opengamma.strata.basics.date.DayCount;
import com.opengamma.strata.basics.date.DaysAdjustment;
import com.opengamma.strata.basics.index.OvernightIndex;
import com.opengamma.strata.basics.schedule.Frequency;
import com.opengamma.strata.basics.schedule.PeriodicSchedule;
import com.opengamma.strata.basics.schedule.RollConvention;
import com.opengamma.strata.basics.schedule.RollConventions;
import com.opengamma.strata.basics.schedule.StubConvention;
import com.opengamma.strata.basics.value.ValueSchedule;
import com.opengamma.strata.collect.ArgChecker;
import com.opengamma.strata.product.common.PayReceive;
import com.opengamma.strata.product.swap.CompoundingMethod;
import com.opengamma.strata.product.swap.NotionalSchedule;
import com.opengamma.strata.product.swap.OvernightAccrualMethod;
import com.opengamma.strata.product.swap.OvernightRateCalculation;
import com.opengamma.strata.product.swap.PaymentSchedule;
import com.opengamma.strata.product.swap.RateCalculationSwapLeg;

/**
 * A market convention for the floating leg of rate swap trades based on an Overnight index.
 * <p>
 * This defines the market convention for a floating leg based on the observed value
 * of an Overnight index such as 'GBP-SONIA' or 'EUR-EONIA'.
 * In most cases, the index contains sufficient information to fully define the convention.
 * As such, no other fields need to be specified when creating an instance.
 * The getters will default any missing information on the fly, avoiding both null and {@link Optional}.
 * <p>
 * There are two methods of accruing interest on an Overnight index - 'Compounded' and 'Averaged'.
 * Averaging is primarily related to the 'USD-FED-FUND' index.
 */
@BeanDefinition
public final class OvernightRateSwapLegConvention
    implements SwapLegConvention, ImmutableBean, Serializable {

  /**
   * The Overnight index.
   * <p>
   * The floating rate to be paid is based on this index
   * It will be a well known market index such as 'GBP-SONIA'.
   */
  @PropertyDefinition(validate = "notNull")
  private final OvernightIndex index;
  /**
   * The method of accruing overnight interest, defaulted to 'Compounded'.
   * <p>
   * Two methods of accrual are supported - 'Compounded' and 'Averaged'.
   * Averaging is primarily related to the 'USD-FED-FUND' index.
   */
  @PropertyDefinition(validate = "notNull")
  private final OvernightAccrualMethod accrualMethod;

  /**
   * The number of business days before the end of the period that the rate is cut off.
   * <p>
   * When a rate cut-off applies, the final daily rate is determined this number of days
   * before the end of the period, with any subsequent days having the same rate.
   * <p>
   * The amount must be zero or positive.
   * A value of zero or one will have no effect on the standard calculation.
   * The fixing holiday calendar of the index is used to determine business days.
   * <p>
   * For example, a value of {@code 3} means that the rate observed on
   * {@code (periodEndDate - 3 business days)} is also to be used on
   * {@code (periodEndDate - 2 business days)} and {@code (periodEndDate - 1 business day)}.
   * <p>
   * If there are multiple accrual periods in the payment period, then this
   * will only apply to the last accrual period in the payment period.
   * <p>
   * This will default to the zero if not specified.
   */
  @PropertyDefinition(get = "field")
  private final Integer rateCutOffDays;
  /**
   * The leg currency, optional with defaulting getter.
   * <p>
   * This is the currency of the swap leg and the currency that payment is made in.
   * The data model permits this currency to differ from that of the index,
   * however the two are typically the same.
   * <p>
   * This will default to the currency of the index if not specified.
   */
  @PropertyDefinition(get = "field")
  private final Currency currency;
  /**
   * The day count convention applicable, optional with defaulting getter.
   * <p>
   * This is used to convert dates to a numerical value.
   * The data model permits the day count to differ from that of the index,
   * however the two are typically the same.
   * <p>
   * This will default to the day count of the index if not specified.
   */
  @PropertyDefinition(get = "field")
  private final DayCount dayCount;
  /**
   * The periodic frequency of accrual.
   * <p>
   * Interest will be accrued over periods at the specified periodic frequency, such as every 3 months.
   * <p>
   * This will default to the term frequency if not specified.
   */
  @PropertyDefinition(get = "field")
  private final Frequency accrualFrequency;
  /**
   * The business day adjustment to apply to accrual schedule dates.
   * <p>
   * Each date in the calculated schedule is determined without taking into account weekends and holidays.
   * The adjustment specified here is used to convert those dates to valid business days.
   * <p>
   * The start date and end date may have their own business day adjustment rules.
   * If those are not present, then this adjustment is used instead.
   * <p>
   * This will default to 'ModifiedFollowing' using the index fixing calendar if not specified.
   */
  @PropertyDefinition(get = "field")
  private final BusinessDayAdjustment accrualBusinessDayAdjustment;
  /**
   * The business day adjustment to apply to the start date, optional with defaulting getter.
   * <p>
   * The start date property is an unadjusted date and as such might be a weekend or holiday.
   * The adjustment specified here is used to convert the start date to a valid business day.
   * <p>
   * This will default to the {@code accrualDatesBusinessDayAdjustment} if not specified.
   */
  @PropertyDefinition(get = "field")
  private final BusinessDayAdjustment startDateBusinessDayAdjustment;
  /**
   * The business day adjustment to apply to the end date, optional with defaulting getter.
   * <p>
   * The end date property is an unadjusted date and as such might be a weekend or holiday.
   * The adjustment specified here is used to convert the end date to a valid business day.
   * <p>
   * This will default to the {@code accrualDatesBusinessDayAdjustment} if not specified.
   */
  @PropertyDefinition(get = "field")
  private final BusinessDayAdjustment endDateBusinessDayAdjustment;
  /**
   * The convention defining how to handle stubs, optional with defaulting getter.
   * <p>
   * The stub convention is used during schedule construction to determine whether the irregular
   * remaining period occurs at the start or end of the schedule.
   * It also determines whether the irregular period is shorter or longer than the regular period.
   * <p>
   * This will default to 'ShortInitial' if not specified.
   */
  @PropertyDefinition(get = "field")
  private final StubConvention stubConvention;
  /**
   * The convention defining how to roll dates, optional with defaulting getter.
   * <p>
   * The schedule periods are determined at the high level by repeatedly adding
   * the frequency to the start date, or subtracting it from the end date.
   * The roll convention provides the detailed rule to adjust the day-of-month or day-of-week.
   * <p>
   * This will default to 'None' if not specified.
   */
  @PropertyDefinition(get = "field")
  private final RollConvention rollConvention;
  /**
   * The periodic frequency of payments, optional with defaulting getter.
   * <p>
   * Regular payments will be made at the specified periodic frequency.
   * The frequency must be the same as, or a multiple of, the accrual periodic frequency.
   * <p>
   * Compounding applies if the payment frequency does not equal the accrual frequency.
   * <p>
   * This will default to the accrual frequency if not specified.
   */
  @PropertyDefinition(get = "field")
  private final Frequency paymentFrequency;
  /**
   * The offset of payment from the base date, optional with defaulting getter.
   * <p>
   * The offset is applied to the unadjusted date specified by {@code paymentRelativeTo}.
   * Offset can be based on calendar days or business days.
   * <p>
   * This will default to 'None' if not specified.
   */
  @PropertyDefinition(get = "field")
  private final DaysAdjustment paymentDateOffset;
  /**
   * The compounding method to use when there is more than one accrual period
   * in each payment period, optional with defaulting getter.
   * <p>
   * Compounding is used when combining accrual periods.
   * <p>
   * This will default to 'None' if not specified.
   */
  @PropertyDefinition(get = "field")
  private final CompoundingMethod compoundingMethod;

  //-------------------------------------------------------------------------
  /**
   * Obtains a convention based on the specified index, using the 'Compounded' accrual method.
   * <p>
   * The standard market convention for an Overnight rate leg is based on the index,
   * frequency and payment offset, with the accrual method set to 'Compounded' and the
   * stub convention set to 'ShortInitial'.
   * Use the {@linkplain #builder() builder} for unusual conventions.
   * 
   * @param index  the index, the market convention values are extracted from the index
   * @param frequency  the frequency of payment, which is also the frequency of accrual
   * @param paymentOffsetDays  the lag in days of payment from the end of the accrual period using the fixing calendar
   * @return the convention
   */
  public static OvernightRateSwapLegConvention of(
      OvernightIndex index,
      Frequency frequency,
      int paymentOffsetDays) {

    return of(index, frequency, paymentOffsetDays, OvernightAccrualMethod.COMPOUNDED);
  }

  /**
   * Creates a convention based on the specified index, specifying the accrual method.
   * <p>
   * The standard market convention for an Overnight rate leg is based on the index,
   * frequency, payment offset and accrual type, with the stub convention set to 'ShortInitial'.
   * Use the {@linkplain #builder() builder} for unusual conventions.
   * <p>
   * The accrual method is usually 'Compounded'.
   * The 'Averaged' method is primarily related to the 'USD-FED-FUND' index.
   * 
   * @param index  the index, the market convention values are extracted from the index
   * @param frequency  the frequency of payment, which is also the frequency of accrual
   * @param paymentOffsetDays  the lag in days of payment from the end of the accrual period using the fixing calendar
   * @param accrualMethod  the method of accruing overnight interest
   * @return the convention
   */
  public static OvernightRateSwapLegConvention of(
      OvernightIndex index,
      Frequency frequency,
      int paymentOffsetDays,
      OvernightAccrualMethod accrualMethod) {

    return OvernightRateSwapLegConvention.builder()
        .index(index)
        .accrualMethod(accrualMethod)
        .accrualFrequency(frequency)
        .paymentFrequency(frequency)
        .paymentDateOffset(DaysAdjustment.ofBusinessDays(paymentOffsetDays, index.getFixingCalendar()))
        .stubConvention(StubConvention.SHORT_INITIAL)
        .build();
  }

  @ImmutableDefaults
  private static void applyDefaults(Builder builder) {
    builder.accrualMethod = OvernightAccrualMethod.COMPOUNDED;
  }

  @ImmutableValidator
  private void validate() {
    if (rateCutOffDays != null) {
      ArgChecker.notNegative(rateCutOffDays.intValue(), "rateCutOffDays");
    }
  }

  //-------------------------------------------------------------------------
  /**
   * Gets the number of business days before the end of the period that the rate is cut off, defaulted to zero.
   * <p>
   * When a rate cut-off applies, the final daily rate is determined this number of days
   * before the end of the period, with any subsequent days having the same rate.
   * <p>
   * The amount must be zero or positive.
   * A value of zero or one will have no effect on the standard calculation.
   * The fixing holiday calendar of the index is used to determine business days.
   * <p>
   * For example, a value of {@code 3} means that the rate observed on
   * {@code (periodEndDate - 3 business days)} is also to be used on
   * {@code (periodEndDate - 2 business days)} and {@code (periodEndDate - 1 business day)}.
   * <p>
   * If there are multiple accrual periods in the payment period, then this
   * will only apply to the last accrual period in the payment period.
   * <p>
   * This will default to zero if not specified.
   * 
   * @return the rate cut off
   */
  public int getRateCutOffDays() {
    return rateCutOffDays != null ? rateCutOffDays : 0;
  }

  /**
   * Gets the leg currency, optional with defaulting getter.
   * <p>
   * This is the currency of the swap leg and the currency that payment is made in.
   * The data model permits this currency to differ from that of the index,
   * however the two are typically the same.
   * <p>
   * This will default to the currency of the index if not specified.
   * 
   * @return the start date business day adjustment, not null
   */
  public Currency getCurrency() {
    return currency != null ? currency : index.getCurrency();
  }

  /**
   * Gets the day count convention applicable,
   * providing a default result if no override specified.
   * <p>
   * This is used to convert dates to a numerical value.
   * The data model permits the day count to differ from that of the index,
   * however the two are typically the same.
   * <p>
   * This will default to the day count of the index if not specified.
   * 
   * @return the day count, not null
   */
  public DayCount getDayCount() {
    return dayCount != null ? dayCount : index.getDayCount();
  }

  /**
   * Gets the periodic frequency of accrual.
   * <p>
   * Interest will be accrued over periods at the specified periodic frequency, such as every 3 months.
   * <p>
   * This will default to the term frequency if not specified.
   * 
   * @return the accrual frequency, not null
   */
  public Frequency getAccrualFrequency() {
    return accrualFrequency != null ? accrualFrequency : Frequency.TERM;
  }

  /**
   * Gets the business day adjustment to apply to accrual schedule dates,
   * providing a default result if no override specified.
   * <p>
   * Each date in the calculated schedule is determined without taking into account weekends and holidays.
   * The adjustment specified here is used to convert those dates to valid business days.
   * The start date and end date have their own business day adjustment rules.
   * <p>
   * This will default to 'ModifiedFollowing' using the index fixing calendar if not specified.
   * 
   * @return the business day adjustment, not null
   */
  public BusinessDayAdjustment getAccrualBusinessDayAdjustment() {
    return accrualBusinessDayAdjustment != null ?
        accrualBusinessDayAdjustment :
        BusinessDayAdjustment.of(MODIFIED_FOLLOWING, index.getFixingCalendar());
  }

  /**
   * Gets the business day adjustment to apply to the start date,
   * providing a default result if no override specified.
   * <p>
   * The start date property is an unadjusted date and as such might be a weekend or holiday.
   * The adjustment specified here is used to convert the start date to a valid business day.
   * <p>
   * This will default to the {@code accrualDatesBusinessDayAdjustment} if not specified.
   * 
   * @return the start date business day adjustment, not null
   */
  public BusinessDayAdjustment getStartDateBusinessDayAdjustment() {
    return startDateBusinessDayAdjustment != null ? startDateBusinessDayAdjustment : getAccrualBusinessDayAdjustment();
  }

  /**
   * Gets the business day adjustment to apply to the end date,
   * providing a default result if no override specified.
   * <p>
   * The end date property is an unadjusted date and as such might be a weekend or holiday.
   * The adjustment specified here is used to convert the end date to a valid business day.
   * <p>
   * This will default to the {@code accrualDatesBusinessDayAdjustment} if not specified.
   * 
   * @return the end date business day adjustment, not null
   */
  public BusinessDayAdjustment getEndDateBusinessDayAdjustment() {
    return endDateBusinessDayAdjustment != null ? endDateBusinessDayAdjustment : getAccrualBusinessDayAdjustment();
  }

  /**
   * Gets the convention defining how to handle stubs,
   * providing a default result if no override specified.
   * <p>
   * The stub convention is used during schedule construction to determine whether the irregular
   * remaining period occurs at the start or end of the schedule.
   * It also determines whether the irregular period is shorter or longer than the regular period.
   * <p>
   * This will default to 'ShortInitial' if not specified.
   * 
   * @return the stub convention, not null
   */
  public StubConvention getStubConvention() {
    return stubConvention != null ? stubConvention : StubConvention.SHORT_INITIAL;
  }

  /**
   * Gets the convention defining how to roll dates,
   * providing a default result if no override specified.
   * <p>
   * The schedule periods are determined at the high level by repeatedly adding
   * the frequency to the start date, or subtracting it from the end date.
   * The roll convention provides the detailed rule to adjust the day-of-month or day-of-week.
   * <p>
   * This will default to 'None' if not specified.
   * 
   * @return the roll convention, not null
   */
  public RollConvention getRollConvention() {
    return rollConvention != null ? rollConvention : RollConventions.NONE;
  }

  /**
   * Gets the periodic frequency of payments,
   * providing a default result if no override specified.
   * <p>
   * Regular payments will be made at the specified periodic frequency.
   * The frequency must be the same as, or a multiple of, the accrual periodic frequency.
   * <p>
   * Compounding applies if the payment frequency does not equal the accrual frequency.
   * <p>
   * This will default to the accrual frequency if not specified.
   * 
   * @return the payment frequency, not null
   */
  public Frequency getPaymentFrequency() {
    return paymentFrequency != null ? paymentFrequency : getAccrualFrequency();
  }

  /**
   * Gets the offset of payment from the base date,
   * providing a default result if no override specified.
   * <p>
   * The offset is applied to the unadjusted date specified by {@code paymentRelativeTo}.
   * Offset can be based on calendar days or business days.
   * 
   * @return the payment date offset, not null
   */
  public DaysAdjustment getPaymentDateOffset() {
    return paymentDateOffset != null ? paymentDateOffset : DaysAdjustment.NONE;
  }

  /**
   * Gets the compounding method to use when there is more than one accrual period
   * in each payment period, providing a default result if no override specified.
   * <p>
   * Compounding is used when combining accrual periods.
   * 
   * @return the compounding method, not null
   */
  public CompoundingMethod getCompoundingMethod() {
    return compoundingMethod != null ? compoundingMethod : CompoundingMethod.NONE;
  }

  //-------------------------------------------------------------------------
  /**
   * Creates a leg based on this convention.
   * <p>
   * This returns a leg based on the specified date.
   * The notional is unsigned, with pay/receive determining the direction of the leg.
   * If the leg is 'Pay', the fixed rate is paid to the counterparty.
   * If the leg is 'Receive', the fixed rate is received from the counterparty.
   *
   * @param startDate  the start date
   * @param endDate  the end date
   * @param payReceive  determines if the leg is to be paid or received
   * @param notional  the notional
   * @return the leg
   */
  public RateCalculationSwapLeg toLeg(
      LocalDate startDate,
      LocalDate endDate,
      PayReceive payReceive,
      double notional) {

    return toLeg(startDate, endDate, payReceive, notional, 0d);
  }

  /**
   * Creates a leg based on this convention.
   * <p>
   * This returns a leg based on the specified date.
   * The notional is unsigned, with pay/receive determining the direction of the leg.
   * If the leg is 'Pay', the fixed rate is paid to the counterparty.
   * If the leg is 'Receive', the fixed rate is received from the counterparty.
   *
   * @param startDate  the start date
   * @param endDate  the end date
   * @param payReceive  determines if the leg is to be paid or received
   * @param notional  the notional
   * @param spread  the spread to apply
   * @return the leg
   */
  public RateCalculationSwapLeg toLeg(
      LocalDate startDate,
      LocalDate endDate,
      PayReceive payReceive,
      double notional,
      double spread) {

    return RateCalculationSwapLeg
        .builder()
        .payReceive(payReceive)
        .accrualSchedule(PeriodicSchedule.builder()
            .startDate(startDate)
            .endDate(endDate)
            .frequency(getAccrualFrequency())
            .businessDayAdjustment(getAccrualBusinessDayAdjustment())
            .startDateBusinessDayAdjustment(startDateBusinessDayAdjustment)
            .endDateBusinessDayAdjustment(endDateBusinessDayAdjustment)
            .stubConvention(stubConvention)
            .rollConvention(rollConvention)
            .build())
        .paymentSchedule(PaymentSchedule.builder()
            .paymentFrequency(getPaymentFrequency())
            .paymentDateOffset(getPaymentDateOffset())
            .compoundingMethod(getCompoundingMethod())
            .build())
        .notionalSchedule(NotionalSchedule.of(getCurrency(), notional))
        .calculation(OvernightRateCalculation.builder()
            .index(index)
            .dayCount(getDayCount())
            .accrualMethod(getAccrualMethod())
            .rateCutOffDays(getRateCutOffDays())
            .spread(spread != 0 ? ValueSchedule.of(spread) : null)
            .build())
        .build();
  }

  //------------------------- AUTOGENERATED START -------------------------
  ///CLOVER:OFF
  /**
   * The meta-bean for {@code OvernightRateSwapLegConvention}.
   * @return the meta-bean, not null
   */
  public static OvernightRateSwapLegConvention.Meta meta() {
    return OvernightRateSwapLegConvention.Meta.INSTANCE;
  }

  static {
    JodaBeanUtils.registerMetaBean(OvernightRateSwapLegConvention.Meta.INSTANCE);
  }

  /**
   * The serialization version id.
   */
  private static final long serialVersionUID = 1L;

  /**
   * Returns a builder used to create an instance of the bean.
   * @return the builder, not null
   */
  public static OvernightRateSwapLegConvention.Builder builder() {
    return new OvernightRateSwapLegConvention.Builder();
  }

  private OvernightRateSwapLegConvention(
      OvernightIndex index,
      OvernightAccrualMethod accrualMethod,
      Integer rateCutOffDays,
      Currency currency,
      DayCount dayCount,
      Frequency accrualFrequency,
      BusinessDayAdjustment accrualBusinessDayAdjustment,
      BusinessDayAdjustment startDateBusinessDayAdjustment,
      BusinessDayAdjustment endDateBusinessDayAdjustment,
      StubConvention stubConvention,
      RollConvention rollConvention,
      Frequency paymentFrequency,
      DaysAdjustment paymentDateOffset,
      CompoundingMethod compoundingMethod) {
    JodaBeanUtils.notNull(index, "index");
    JodaBeanUtils.notNull(accrualMethod, "accrualMethod");
    this.index = index;
    this.accrualMethod = accrualMethod;
    this.rateCutOffDays = rateCutOffDays;
    this.currency = currency;
    this.dayCount = dayCount;
    this.accrualFrequency = accrualFrequency;
    this.accrualBusinessDayAdjustment = accrualBusinessDayAdjustment;
    this.startDateBusinessDayAdjustment = startDateBusinessDayAdjustment;
    this.endDateBusinessDayAdjustment = endDateBusinessDayAdjustment;
    this.stubConvention = stubConvention;
    this.rollConvention = rollConvention;
    this.paymentFrequency = paymentFrequency;
    this.paymentDateOffset = paymentDateOffset;
    this.compoundingMethod = compoundingMethod;
    validate();
  }

  @Override
  public OvernightRateSwapLegConvention.Meta metaBean() {
    return OvernightRateSwapLegConvention.Meta.INSTANCE;
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
   * Gets the Overnight index.
   * <p>
   * The floating rate to be paid is based on this index
   * It will be a well known market index such as 'GBP-SONIA'.
   * @return the value of the property, not null
   */
  public OvernightIndex getIndex() {
    return index;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the method of accruing overnight interest, defaulted to 'Compounded'.
   * <p>
   * Two methods of accrual are supported - 'Compounded' and 'Averaged'.
   * Averaging is primarily related to the 'USD-FED-FUND' index.
   * @return the value of the property, not null
   */
  public OvernightAccrualMethod getAccrualMethod() {
    return accrualMethod;
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
      OvernightRateSwapLegConvention other = (OvernightRateSwapLegConvention) obj;
      return JodaBeanUtils.equal(index, other.index) &&
          JodaBeanUtils.equal(accrualMethod, other.accrualMethod) &&
          JodaBeanUtils.equal(rateCutOffDays, other.rateCutOffDays) &&
          JodaBeanUtils.equal(currency, other.currency) &&
          JodaBeanUtils.equal(dayCount, other.dayCount) &&
          JodaBeanUtils.equal(accrualFrequency, other.accrualFrequency) &&
          JodaBeanUtils.equal(accrualBusinessDayAdjustment, other.accrualBusinessDayAdjustment) &&
          JodaBeanUtils.equal(startDateBusinessDayAdjustment, other.startDateBusinessDayAdjustment) &&
          JodaBeanUtils.equal(endDateBusinessDayAdjustment, other.endDateBusinessDayAdjustment) &&
          JodaBeanUtils.equal(stubConvention, other.stubConvention) &&
          JodaBeanUtils.equal(rollConvention, other.rollConvention) &&
          JodaBeanUtils.equal(paymentFrequency, other.paymentFrequency) &&
          JodaBeanUtils.equal(paymentDateOffset, other.paymentDateOffset) &&
          JodaBeanUtils.equal(compoundingMethod, other.compoundingMethod);
    }
    return false;
  }

  @Override
  public int hashCode() {
    int hash = getClass().hashCode();
    hash = hash * 31 + JodaBeanUtils.hashCode(index);
    hash = hash * 31 + JodaBeanUtils.hashCode(accrualMethod);
    hash = hash * 31 + JodaBeanUtils.hashCode(rateCutOffDays);
    hash = hash * 31 + JodaBeanUtils.hashCode(currency);
    hash = hash * 31 + JodaBeanUtils.hashCode(dayCount);
    hash = hash * 31 + JodaBeanUtils.hashCode(accrualFrequency);
    hash = hash * 31 + JodaBeanUtils.hashCode(accrualBusinessDayAdjustment);
    hash = hash * 31 + JodaBeanUtils.hashCode(startDateBusinessDayAdjustment);
    hash = hash * 31 + JodaBeanUtils.hashCode(endDateBusinessDayAdjustment);
    hash = hash * 31 + JodaBeanUtils.hashCode(stubConvention);
    hash = hash * 31 + JodaBeanUtils.hashCode(rollConvention);
    hash = hash * 31 + JodaBeanUtils.hashCode(paymentFrequency);
    hash = hash * 31 + JodaBeanUtils.hashCode(paymentDateOffset);
    hash = hash * 31 + JodaBeanUtils.hashCode(compoundingMethod);
    return hash;
  }

  @Override
  public String toString() {
    StringBuilder buf = new StringBuilder(480);
    buf.append("OvernightRateSwapLegConvention{");
    buf.append("index").append('=').append(index).append(',').append(' ');
    buf.append("accrualMethod").append('=').append(accrualMethod).append(',').append(' ');
    buf.append("rateCutOffDays").append('=').append(rateCutOffDays).append(',').append(' ');
    buf.append("currency").append('=').append(currency).append(',').append(' ');
    buf.append("dayCount").append('=').append(dayCount).append(',').append(' ');
    buf.append("accrualFrequency").append('=').append(accrualFrequency).append(',').append(' ');
    buf.append("accrualBusinessDayAdjustment").append('=').append(accrualBusinessDayAdjustment).append(',').append(' ');
    buf.append("startDateBusinessDayAdjustment").append('=').append(startDateBusinessDayAdjustment).append(',').append(' ');
    buf.append("endDateBusinessDayAdjustment").append('=').append(endDateBusinessDayAdjustment).append(',').append(' ');
    buf.append("stubConvention").append('=').append(stubConvention).append(',').append(' ');
    buf.append("rollConvention").append('=').append(rollConvention).append(',').append(' ');
    buf.append("paymentFrequency").append('=').append(paymentFrequency).append(',').append(' ');
    buf.append("paymentDateOffset").append('=').append(paymentDateOffset).append(',').append(' ');
    buf.append("compoundingMethod").append('=').append(JodaBeanUtils.toString(compoundingMethod));
    buf.append('}');
    return buf.toString();
  }

  //-----------------------------------------------------------------------
  /**
   * The meta-bean for {@code OvernightRateSwapLegConvention}.
   */
  public static final class Meta extends DirectMetaBean {
    /**
     * The singleton instance of the meta-bean.
     */
    static final Meta INSTANCE = new Meta();

    /**
     * The meta-property for the {@code index} property.
     */
    private final MetaProperty<OvernightIndex> index = DirectMetaProperty.ofImmutable(
        this, "index", OvernightRateSwapLegConvention.class, OvernightIndex.class);
    /**
     * The meta-property for the {@code accrualMethod} property.
     */
    private final MetaProperty<OvernightAccrualMethod> accrualMethod = DirectMetaProperty.ofImmutable(
        this, "accrualMethod", OvernightRateSwapLegConvention.class, OvernightAccrualMethod.class);
    /**
     * The meta-property for the {@code rateCutOffDays} property.
     */
    private final MetaProperty<Integer> rateCutOffDays = DirectMetaProperty.ofImmutable(
        this, "rateCutOffDays", OvernightRateSwapLegConvention.class, Integer.class);
    /**
     * The meta-property for the {@code currency} property.
     */
    private final MetaProperty<Currency> currency = DirectMetaProperty.ofImmutable(
        this, "currency", OvernightRateSwapLegConvention.class, Currency.class);
    /**
     * The meta-property for the {@code dayCount} property.
     */
    private final MetaProperty<DayCount> dayCount = DirectMetaProperty.ofImmutable(
        this, "dayCount", OvernightRateSwapLegConvention.class, DayCount.class);
    /**
     * The meta-property for the {@code accrualFrequency} property.
     */
    private final MetaProperty<Frequency> accrualFrequency = DirectMetaProperty.ofImmutable(
        this, "accrualFrequency", OvernightRateSwapLegConvention.class, Frequency.class);
    /**
     * The meta-property for the {@code accrualBusinessDayAdjustment} property.
     */
    private final MetaProperty<BusinessDayAdjustment> accrualBusinessDayAdjustment = DirectMetaProperty.ofImmutable(
        this, "accrualBusinessDayAdjustment", OvernightRateSwapLegConvention.class, BusinessDayAdjustment.class);
    /**
     * The meta-property for the {@code startDateBusinessDayAdjustment} property.
     */
    private final MetaProperty<BusinessDayAdjustment> startDateBusinessDayAdjustment = DirectMetaProperty.ofImmutable(
        this, "startDateBusinessDayAdjustment", OvernightRateSwapLegConvention.class, BusinessDayAdjustment.class);
    /**
     * The meta-property for the {@code endDateBusinessDayAdjustment} property.
     */
    private final MetaProperty<BusinessDayAdjustment> endDateBusinessDayAdjustment = DirectMetaProperty.ofImmutable(
        this, "endDateBusinessDayAdjustment", OvernightRateSwapLegConvention.class, BusinessDayAdjustment.class);
    /**
     * The meta-property for the {@code stubConvention} property.
     */
    private final MetaProperty<StubConvention> stubConvention = DirectMetaProperty.ofImmutable(
        this, "stubConvention", OvernightRateSwapLegConvention.class, StubConvention.class);
    /**
     * The meta-property for the {@code rollConvention} property.
     */
    private final MetaProperty<RollConvention> rollConvention = DirectMetaProperty.ofImmutable(
        this, "rollConvention", OvernightRateSwapLegConvention.class, RollConvention.class);
    /**
     * The meta-property for the {@code paymentFrequency} property.
     */
    private final MetaProperty<Frequency> paymentFrequency = DirectMetaProperty.ofImmutable(
        this, "paymentFrequency", OvernightRateSwapLegConvention.class, Frequency.class);
    /**
     * The meta-property for the {@code paymentDateOffset} property.
     */
    private final MetaProperty<DaysAdjustment> paymentDateOffset = DirectMetaProperty.ofImmutable(
        this, "paymentDateOffset", OvernightRateSwapLegConvention.class, DaysAdjustment.class);
    /**
     * The meta-property for the {@code compoundingMethod} property.
     */
    private final MetaProperty<CompoundingMethod> compoundingMethod = DirectMetaProperty.ofImmutable(
        this, "compoundingMethod", OvernightRateSwapLegConvention.class, CompoundingMethod.class);
    /**
     * The meta-properties.
     */
    private final Map<String, MetaProperty<?>> metaPropertyMap$ = new DirectMetaPropertyMap(
        this, null,
        "index",
        "accrualMethod",
        "rateCutOffDays",
        "currency",
        "dayCount",
        "accrualFrequency",
        "accrualBusinessDayAdjustment",
        "startDateBusinessDayAdjustment",
        "endDateBusinessDayAdjustment",
        "stubConvention",
        "rollConvention",
        "paymentFrequency",
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
        case 100346066:  // index
          return index;
        case -1335729296:  // accrualMethod
          return accrualMethod;
        case -92095804:  // rateCutOffDays
          return rateCutOffDays;
        case 575402001:  // currency
          return currency;
        case 1905311443:  // dayCount
          return dayCount;
        case 945206381:  // accrualFrequency
          return accrualFrequency;
        case 896049114:  // accrualBusinessDayAdjustment
          return accrualBusinessDayAdjustment;
        case 429197561:  // startDateBusinessDayAdjustment
          return startDateBusinessDayAdjustment;
        case -734327136:  // endDateBusinessDayAdjustment
          return endDateBusinessDayAdjustment;
        case -31408449:  // stubConvention
          return stubConvention;
        case -10223666:  // rollConvention
          return rollConvention;
        case 863656438:  // paymentFrequency
          return paymentFrequency;
        case -716438393:  // paymentDateOffset
          return paymentDateOffset;
        case -1376171496:  // compoundingMethod
          return compoundingMethod;
      }
      return super.metaPropertyGet(propertyName);
    }

    @Override
    public OvernightRateSwapLegConvention.Builder builder() {
      return new OvernightRateSwapLegConvention.Builder();
    }

    @Override
    public Class<? extends OvernightRateSwapLegConvention> beanType() {
      return OvernightRateSwapLegConvention.class;
    }

    @Override
    public Map<String, MetaProperty<?>> metaPropertyMap() {
      return metaPropertyMap$;
    }

    //-----------------------------------------------------------------------
    /**
     * The meta-property for the {@code index} property.
     * @return the meta-property, not null
     */
    public MetaProperty<OvernightIndex> index() {
      return index;
    }

    /**
     * The meta-property for the {@code accrualMethod} property.
     * @return the meta-property, not null
     */
    public MetaProperty<OvernightAccrualMethod> accrualMethod() {
      return accrualMethod;
    }

    /**
     * The meta-property for the {@code rateCutOffDays} property.
     * @return the meta-property, not null
     */
    public MetaProperty<Integer> rateCutOffDays() {
      return rateCutOffDays;
    }

    /**
     * The meta-property for the {@code currency} property.
     * @return the meta-property, not null
     */
    public MetaProperty<Currency> currency() {
      return currency;
    }

    /**
     * The meta-property for the {@code dayCount} property.
     * @return the meta-property, not null
     */
    public MetaProperty<DayCount> dayCount() {
      return dayCount;
    }

    /**
     * The meta-property for the {@code accrualFrequency} property.
     * @return the meta-property, not null
     */
    public MetaProperty<Frequency> accrualFrequency() {
      return accrualFrequency;
    }

    /**
     * The meta-property for the {@code accrualBusinessDayAdjustment} property.
     * @return the meta-property, not null
     */
    public MetaProperty<BusinessDayAdjustment> accrualBusinessDayAdjustment() {
      return accrualBusinessDayAdjustment;
    }

    /**
     * The meta-property for the {@code startDateBusinessDayAdjustment} property.
     * @return the meta-property, not null
     */
    public MetaProperty<BusinessDayAdjustment> startDateBusinessDayAdjustment() {
      return startDateBusinessDayAdjustment;
    }

    /**
     * The meta-property for the {@code endDateBusinessDayAdjustment} property.
     * @return the meta-property, not null
     */
    public MetaProperty<BusinessDayAdjustment> endDateBusinessDayAdjustment() {
      return endDateBusinessDayAdjustment;
    }

    /**
     * The meta-property for the {@code stubConvention} property.
     * @return the meta-property, not null
     */
    public MetaProperty<StubConvention> stubConvention() {
      return stubConvention;
    }

    /**
     * The meta-property for the {@code rollConvention} property.
     * @return the meta-property, not null
     */
    public MetaProperty<RollConvention> rollConvention() {
      return rollConvention;
    }

    /**
     * The meta-property for the {@code paymentFrequency} property.
     * @return the meta-property, not null
     */
    public MetaProperty<Frequency> paymentFrequency() {
      return paymentFrequency;
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
        case 100346066:  // index
          return ((OvernightRateSwapLegConvention) bean).getIndex();
        case -1335729296:  // accrualMethod
          return ((OvernightRateSwapLegConvention) bean).getAccrualMethod();
        case -92095804:  // rateCutOffDays
          return ((OvernightRateSwapLegConvention) bean).rateCutOffDays;
        case 575402001:  // currency
          return ((OvernightRateSwapLegConvention) bean).currency;
        case 1905311443:  // dayCount
          return ((OvernightRateSwapLegConvention) bean).dayCount;
        case 945206381:  // accrualFrequency
          return ((OvernightRateSwapLegConvention) bean).accrualFrequency;
        case 896049114:  // accrualBusinessDayAdjustment
          return ((OvernightRateSwapLegConvention) bean).accrualBusinessDayAdjustment;
        case 429197561:  // startDateBusinessDayAdjustment
          return ((OvernightRateSwapLegConvention) bean).startDateBusinessDayAdjustment;
        case -734327136:  // endDateBusinessDayAdjustment
          return ((OvernightRateSwapLegConvention) bean).endDateBusinessDayAdjustment;
        case -31408449:  // stubConvention
          return ((OvernightRateSwapLegConvention) bean).stubConvention;
        case -10223666:  // rollConvention
          return ((OvernightRateSwapLegConvention) bean).rollConvention;
        case 863656438:  // paymentFrequency
          return ((OvernightRateSwapLegConvention) bean).paymentFrequency;
        case -716438393:  // paymentDateOffset
          return ((OvernightRateSwapLegConvention) bean).paymentDateOffset;
        case -1376171496:  // compoundingMethod
          return ((OvernightRateSwapLegConvention) bean).compoundingMethod;
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
   * The bean-builder for {@code OvernightRateSwapLegConvention}.
   */
  public static final class Builder extends DirectFieldsBeanBuilder<OvernightRateSwapLegConvention> {

    private OvernightIndex index;
    private OvernightAccrualMethod accrualMethod;
    private Integer rateCutOffDays;
    private Currency currency;
    private DayCount dayCount;
    private Frequency accrualFrequency;
    private BusinessDayAdjustment accrualBusinessDayAdjustment;
    private BusinessDayAdjustment startDateBusinessDayAdjustment;
    private BusinessDayAdjustment endDateBusinessDayAdjustment;
    private StubConvention stubConvention;
    private RollConvention rollConvention;
    private Frequency paymentFrequency;
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
    private Builder(OvernightRateSwapLegConvention beanToCopy) {
      this.index = beanToCopy.getIndex();
      this.accrualMethod = beanToCopy.getAccrualMethod();
      this.rateCutOffDays = beanToCopy.rateCutOffDays;
      this.currency = beanToCopy.currency;
      this.dayCount = beanToCopy.dayCount;
      this.accrualFrequency = beanToCopy.accrualFrequency;
      this.accrualBusinessDayAdjustment = beanToCopy.accrualBusinessDayAdjustment;
      this.startDateBusinessDayAdjustment = beanToCopy.startDateBusinessDayAdjustment;
      this.endDateBusinessDayAdjustment = beanToCopy.endDateBusinessDayAdjustment;
      this.stubConvention = beanToCopy.stubConvention;
      this.rollConvention = beanToCopy.rollConvention;
      this.paymentFrequency = beanToCopy.paymentFrequency;
      this.paymentDateOffset = beanToCopy.paymentDateOffset;
      this.compoundingMethod = beanToCopy.compoundingMethod;
    }

    //-----------------------------------------------------------------------
    @Override
    public Object get(String propertyName) {
      switch (propertyName.hashCode()) {
        case 100346066:  // index
          return index;
        case -1335729296:  // accrualMethod
          return accrualMethod;
        case -92095804:  // rateCutOffDays
          return rateCutOffDays;
        case 575402001:  // currency
          return currency;
        case 1905311443:  // dayCount
          return dayCount;
        case 945206381:  // accrualFrequency
          return accrualFrequency;
        case 896049114:  // accrualBusinessDayAdjustment
          return accrualBusinessDayAdjustment;
        case 429197561:  // startDateBusinessDayAdjustment
          return startDateBusinessDayAdjustment;
        case -734327136:  // endDateBusinessDayAdjustment
          return endDateBusinessDayAdjustment;
        case -31408449:  // stubConvention
          return stubConvention;
        case -10223666:  // rollConvention
          return rollConvention;
        case 863656438:  // paymentFrequency
          return paymentFrequency;
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
        case 100346066:  // index
          this.index = (OvernightIndex) newValue;
          break;
        case -1335729296:  // accrualMethod
          this.accrualMethod = (OvernightAccrualMethod) newValue;
          break;
        case -92095804:  // rateCutOffDays
          this.rateCutOffDays = (Integer) newValue;
          break;
        case 575402001:  // currency
          this.currency = (Currency) newValue;
          break;
        case 1905311443:  // dayCount
          this.dayCount = (DayCount) newValue;
          break;
        case 945206381:  // accrualFrequency
          this.accrualFrequency = (Frequency) newValue;
          break;
        case 896049114:  // accrualBusinessDayAdjustment
          this.accrualBusinessDayAdjustment = (BusinessDayAdjustment) newValue;
          break;
        case 429197561:  // startDateBusinessDayAdjustment
          this.startDateBusinessDayAdjustment = (BusinessDayAdjustment) newValue;
          break;
        case -734327136:  // endDateBusinessDayAdjustment
          this.endDateBusinessDayAdjustment = (BusinessDayAdjustment) newValue;
          break;
        case -31408449:  // stubConvention
          this.stubConvention = (StubConvention) newValue;
          break;
        case -10223666:  // rollConvention
          this.rollConvention = (RollConvention) newValue;
          break;
        case 863656438:  // paymentFrequency
          this.paymentFrequency = (Frequency) newValue;
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
    public OvernightRateSwapLegConvention build() {
      return new OvernightRateSwapLegConvention(
          index,
          accrualMethod,
          rateCutOffDays,
          currency,
          dayCount,
          accrualFrequency,
          accrualBusinessDayAdjustment,
          startDateBusinessDayAdjustment,
          endDateBusinessDayAdjustment,
          stubConvention,
          rollConvention,
          paymentFrequency,
          paymentDateOffset,
          compoundingMethod);
    }

    //-----------------------------------------------------------------------
    /**
     * Sets the Overnight index.
     * <p>
     * The floating rate to be paid is based on this index
     * It will be a well known market index such as 'GBP-SONIA'.
     * @param index  the new value, not null
     * @return this, for chaining, not null
     */
    public Builder index(OvernightIndex index) {
      JodaBeanUtils.notNull(index, "index");
      this.index = index;
      return this;
    }

    /**
     * Sets the method of accruing overnight interest, defaulted to 'Compounded'.
     * <p>
     * Two methods of accrual are supported - 'Compounded' and 'Averaged'.
     * Averaging is primarily related to the 'USD-FED-FUND' index.
     * @param accrualMethod  the new value, not null
     * @return this, for chaining, not null
     */
    public Builder accrualMethod(OvernightAccrualMethod accrualMethod) {
      JodaBeanUtils.notNull(accrualMethod, "accrualMethod");
      this.accrualMethod = accrualMethod;
      return this;
    }

    /**
     * Sets the number of business days before the end of the period that the rate is cut off.
     * <p>
     * When a rate cut-off applies, the final daily rate is determined this number of days
     * before the end of the period, with any subsequent days having the same rate.
     * <p>
     * The amount must be zero or positive.
     * A value of zero or one will have no effect on the standard calculation.
     * The fixing holiday calendar of the index is used to determine business days.
     * <p>
     * For example, a value of {@code 3} means that the rate observed on
     * {@code (periodEndDate - 3 business days)} is also to be used on
     * {@code (periodEndDate - 2 business days)} and {@code (periodEndDate - 1 business day)}.
     * <p>
     * If there are multiple accrual periods in the payment period, then this
     * will only apply to the last accrual period in the payment period.
     * <p>
     * This will default to the zero if not specified.
     * @param rateCutOffDays  the new value
     * @return this, for chaining, not null
     */
    public Builder rateCutOffDays(Integer rateCutOffDays) {
      this.rateCutOffDays = rateCutOffDays;
      return this;
    }

    /**
     * Sets the leg currency, optional with defaulting getter.
     * <p>
     * This is the currency of the swap leg and the currency that payment is made in.
     * The data model permits this currency to differ from that of the index,
     * however the two are typically the same.
     * <p>
     * This will default to the currency of the index if not specified.
     * @param currency  the new value
     * @return this, for chaining, not null
     */
    public Builder currency(Currency currency) {
      this.currency = currency;
      return this;
    }

    /**
     * Sets the day count convention applicable, optional with defaulting getter.
     * <p>
     * This is used to convert dates to a numerical value.
     * The data model permits the day count to differ from that of the index,
     * however the two are typically the same.
     * <p>
     * This will default to the day count of the index if not specified.
     * @param dayCount  the new value
     * @return this, for chaining, not null
     */
    public Builder dayCount(DayCount dayCount) {
      this.dayCount = dayCount;
      return this;
    }

    /**
     * Sets the periodic frequency of accrual.
     * <p>
     * Interest will be accrued over periods at the specified periodic frequency, such as every 3 months.
     * <p>
     * This will default to the term frequency if not specified.
     * @param accrualFrequency  the new value
     * @return this, for chaining, not null
     */
    public Builder accrualFrequency(Frequency accrualFrequency) {
      this.accrualFrequency = accrualFrequency;
      return this;
    }

    /**
     * Sets the business day adjustment to apply to accrual schedule dates.
     * <p>
     * Each date in the calculated schedule is determined without taking into account weekends and holidays.
     * The adjustment specified here is used to convert those dates to valid business days.
     * <p>
     * The start date and end date may have their own business day adjustment rules.
     * If those are not present, then this adjustment is used instead.
     * <p>
     * This will default to 'ModifiedFollowing' using the index fixing calendar if not specified.
     * @param accrualBusinessDayAdjustment  the new value
     * @return this, for chaining, not null
     */
    public Builder accrualBusinessDayAdjustment(BusinessDayAdjustment accrualBusinessDayAdjustment) {
      this.accrualBusinessDayAdjustment = accrualBusinessDayAdjustment;
      return this;
    }

    /**
     * Sets the business day adjustment to apply to the start date, optional with defaulting getter.
     * <p>
     * The start date property is an unadjusted date and as such might be a weekend or holiday.
     * The adjustment specified here is used to convert the start date to a valid business day.
     * <p>
     * This will default to the {@code accrualDatesBusinessDayAdjustment} if not specified.
     * @param startDateBusinessDayAdjustment  the new value
     * @return this, for chaining, not null
     */
    public Builder startDateBusinessDayAdjustment(BusinessDayAdjustment startDateBusinessDayAdjustment) {
      this.startDateBusinessDayAdjustment = startDateBusinessDayAdjustment;
      return this;
    }

    /**
     * Sets the business day adjustment to apply to the end date, optional with defaulting getter.
     * <p>
     * The end date property is an unadjusted date and as such might be a weekend or holiday.
     * The adjustment specified here is used to convert the end date to a valid business day.
     * <p>
     * This will default to the {@code accrualDatesBusinessDayAdjustment} if not specified.
     * @param endDateBusinessDayAdjustment  the new value
     * @return this, for chaining, not null
     */
    public Builder endDateBusinessDayAdjustment(BusinessDayAdjustment endDateBusinessDayAdjustment) {
      this.endDateBusinessDayAdjustment = endDateBusinessDayAdjustment;
      return this;
    }

    /**
     * Sets the convention defining how to handle stubs, optional with defaulting getter.
     * <p>
     * The stub convention is used during schedule construction to determine whether the irregular
     * remaining period occurs at the start or end of the schedule.
     * It also determines whether the irregular period is shorter or longer than the regular period.
     * <p>
     * This will default to 'ShortInitial' if not specified.
     * @param stubConvention  the new value
     * @return this, for chaining, not null
     */
    public Builder stubConvention(StubConvention stubConvention) {
      this.stubConvention = stubConvention;
      return this;
    }

    /**
     * Sets the convention defining how to roll dates, optional with defaulting getter.
     * <p>
     * The schedule periods are determined at the high level by repeatedly adding
     * the frequency to the start date, or subtracting it from the end date.
     * The roll convention provides the detailed rule to adjust the day-of-month or day-of-week.
     * <p>
     * This will default to 'None' if not specified.
     * @param rollConvention  the new value
     * @return this, for chaining, not null
     */
    public Builder rollConvention(RollConvention rollConvention) {
      this.rollConvention = rollConvention;
      return this;
    }

    /**
     * Sets the periodic frequency of payments, optional with defaulting getter.
     * <p>
     * Regular payments will be made at the specified periodic frequency.
     * The frequency must be the same as, or a multiple of, the accrual periodic frequency.
     * <p>
     * Compounding applies if the payment frequency does not equal the accrual frequency.
     * <p>
     * This will default to the accrual frequency if not specified.
     * @param paymentFrequency  the new value
     * @return this, for chaining, not null
     */
    public Builder paymentFrequency(Frequency paymentFrequency) {
      this.paymentFrequency = paymentFrequency;
      return this;
    }

    /**
     * Sets the offset of payment from the base date, optional with defaulting getter.
     * <p>
     * The offset is applied to the unadjusted date specified by {@code paymentRelativeTo}.
     * Offset can be based on calendar days or business days.
     * <p>
     * This will default to 'None' if not specified.
     * @param paymentDateOffset  the new value
     * @return this, for chaining, not null
     */
    public Builder paymentDateOffset(DaysAdjustment paymentDateOffset) {
      this.paymentDateOffset = paymentDateOffset;
      return this;
    }

    /**
     * Sets the compounding method to use when there is more than one accrual period
     * in each payment period, optional with defaulting getter.
     * <p>
     * Compounding is used when combining accrual periods.
     * <p>
     * This will default to 'None' if not specified.
     * @param compoundingMethod  the new value
     * @return this, for chaining, not null
     */
    public Builder compoundingMethod(CompoundingMethod compoundingMethod) {
      this.compoundingMethod = compoundingMethod;
      return this;
    }

    //-----------------------------------------------------------------------
    @Override
    public String toString() {
      StringBuilder buf = new StringBuilder(480);
      buf.append("OvernightRateSwapLegConvention.Builder{");
      buf.append("index").append('=').append(JodaBeanUtils.toString(index)).append(',').append(' ');
      buf.append("accrualMethod").append('=').append(JodaBeanUtils.toString(accrualMethod)).append(',').append(' ');
      buf.append("rateCutOffDays").append('=').append(JodaBeanUtils.toString(rateCutOffDays)).append(',').append(' ');
      buf.append("currency").append('=').append(JodaBeanUtils.toString(currency)).append(',').append(' ');
      buf.append("dayCount").append('=').append(JodaBeanUtils.toString(dayCount)).append(',').append(' ');
      buf.append("accrualFrequency").append('=').append(JodaBeanUtils.toString(accrualFrequency)).append(',').append(' ');
      buf.append("accrualBusinessDayAdjustment").append('=').append(JodaBeanUtils.toString(accrualBusinessDayAdjustment)).append(',').append(' ');
      buf.append("startDateBusinessDayAdjustment").append('=').append(JodaBeanUtils.toString(startDateBusinessDayAdjustment)).append(',').append(' ');
      buf.append("endDateBusinessDayAdjustment").append('=').append(JodaBeanUtils.toString(endDateBusinessDayAdjustment)).append(',').append(' ');
      buf.append("stubConvention").append('=').append(JodaBeanUtils.toString(stubConvention)).append(',').append(' ');
      buf.append("rollConvention").append('=').append(JodaBeanUtils.toString(rollConvention)).append(',').append(' ');
      buf.append("paymentFrequency").append('=').append(JodaBeanUtils.toString(paymentFrequency)).append(',').append(' ');
      buf.append("paymentDateOffset").append('=').append(JodaBeanUtils.toString(paymentDateOffset)).append(',').append(' ');
      buf.append("compoundingMethod").append('=').append(JodaBeanUtils.toString(compoundingMethod));
      buf.append('}');
      return buf.toString();
    }

  }

  ///CLOVER:ON
  //-------------------------- AUTOGENERATED END --------------------------
}
