/*
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

import org.joda.beans.Bean;
import org.joda.beans.ImmutableBean;
import org.joda.beans.JodaBeanUtils;
import org.joda.beans.MetaBean;
import org.joda.beans.MetaProperty;
import org.joda.beans.gen.BeanDefinition;
import org.joda.beans.gen.PropertyDefinition;
import org.joda.beans.impl.direct.DirectFieldsBeanBuilder;
import org.joda.beans.impl.direct.DirectMetaBean;
import org.joda.beans.impl.direct.DirectMetaProperty;
import org.joda.beans.impl.direct.DirectMetaPropertyMap;

import com.opengamma.strata.basics.currency.Currency;
import com.opengamma.strata.basics.date.BusinessDayAdjustment;
import com.opengamma.strata.basics.date.DayCount;
import com.opengamma.strata.basics.date.DaysAdjustment;
import com.opengamma.strata.basics.index.IborIndex;
import com.opengamma.strata.basics.schedule.Frequency;
import com.opengamma.strata.basics.schedule.PeriodicSchedule;
import com.opengamma.strata.basics.schedule.RollConvention;
import com.opengamma.strata.basics.schedule.RollConventions;
import com.opengamma.strata.basics.schedule.StubConvention;
import com.opengamma.strata.basics.value.ValueSchedule;
import com.opengamma.strata.product.common.PayReceive;
import com.opengamma.strata.product.swap.CompoundingMethod;
import com.opengamma.strata.product.swap.FixingRelativeTo;
import com.opengamma.strata.product.swap.IborRateCalculation;
import com.opengamma.strata.product.swap.NotionalSchedule;
import com.opengamma.strata.product.swap.PaymentSchedule;
import com.opengamma.strata.product.swap.RateCalculationSwapLeg;

/**
 * A market convention for the floating leg of rate swap trades based on an Ibor index.
 * <p>
 * This defines the market convention for a floating leg based on the observed value
 * of an Ibor index such as 'GBP-LIBOR-3M' or 'EUR-EURIBOR-1M'.
 * In most cases, the index contains sufficient information to fully define the convention.
 * As such, no other fields need to be specified when creating an instance.
 * The getters will default any missing information on the fly, avoiding both null and {@link Optional}.
 */
@BeanDefinition
public final class IborRateSwapLegConvention
    implements FloatRateSwapLegConvention, ImmutableBean, Serializable {

  /**
   * The Ibor index.
   * <p>
   * The floating rate to be paid is based on this index
   * It will be a well known market index such as 'GBP-LIBOR-3M'.
   */
  @PropertyDefinition(validate = "notNull", overrideGet = true)
  private final IborIndex index;

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
   * This will default to the tenor of the index if not specified.
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
   * This will default to 'SmartInitial' if not specified.
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
   * The base date that each fixing is made relative to, optional with defaulting getter.
   * <p>
   * The fixing date is relative to either the start or end of each reset period.
   * <p>
   * Note that in most cases, the reset frequency matches the accrual frequency
   * and thus there is only one fixing for the accrual period.
   * <p>
   * This will default to 'PeriodStart' if not specified.
   */
  @PropertyDefinition(get = "field")
  private final FixingRelativeTo fixingRelativeTo;
  /**
   * The offset of the fixing date from each adjusted reset date.
   * <p>
   * The offset is applied to the base date specified by {@code fixingRelativeTo}.
   * The offset is typically a negative number of business days.
   * The data model permits the offset to differ from that of the index,
   * however the two are typically the same.
   * <p>
   * This will default to the fixing date offset of the index if not specified.
   */
  @PropertyDefinition(get = "field")
  private final DaysAdjustment fixingDateOffset;
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
  /**
   * The flag indicating whether to exchange the notional.
   * <p>
   * If 'true', the notional there is both an initial exchange and a final exchange of notional.
   * <p>
   * This will default to 'false' if not specified.
   */
  @PropertyDefinition
  private final boolean notionalExchange;

  //-------------------------------------------------------------------------
  /**
   * Obtains a convention based on the specified index.
   * <p>
   * The standard market convention for an Ibor rate leg is based on the index,
   * with the stub convention set to 'SmartInitial'.
   * Use the {@linkplain #builder() builder} for unusual conventions.
   * 
   * @param index  the index, the market convention values are extracted from the index
   * @return the convention
   */
  public static IborRateSwapLegConvention of(IborIndex index) {
    return IborRateSwapLegConvention.builder()
        .index(index)
        .stubConvention(StubConvention.SMART_INITIAL)
        .build();
  }

  //-------------------------------------------------------------------------
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
  @Override
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
  @Override
  public DayCount getDayCount() {
    return dayCount != null ? dayCount : index.getDayCount();
  }

  /**
   * Gets the periodic frequency of accrual.
   * <p>
   * Interest will be accrued over periods at the specified periodic frequency, such as every 3 months.
   * <p>
   * This will default to the tenor of the index if not specified.
   * 
   * @return the accrual frequency, not null
   */
  public Frequency getAccrualFrequency() {
    return accrualFrequency != null ? accrualFrequency : Frequency.of(index.getTenor().getPeriod());
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
  @Override
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
  @Override
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
  @Override
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
   * This will default to 'SmartInitial' if not specified.
   * 
   * @return the stub convention, not null
   */
  public StubConvention getStubConvention() {
    return stubConvention != null ? stubConvention : StubConvention.SMART_INITIAL;
  }

  /**
   * Gets the convention defining how to roll dates,
   * providing a default result if no override specified.
   * <p>
   * The schedule periods are determined at the high level by repeatedly adding
   * the frequency to the start date, or subtracting it from the end date.
   * The roll convention provides the detailed rule to adjust the day-of-month or day-of-week.
   * <p>
   * This will default to 'EOM' if not specified.
   * 
   * @return the roll convention, not null
   */
  public RollConvention getRollConvention() {
    return rollConvention != null ? rollConvention : RollConventions.EOM;
  }

  /**
   * Gets the base date that each fixing is made relative to, optional with defaulting getter.
   * <p>
   * The fixing date is relative to either the start or end of each reset period.
   * <p>
   * Note that in most cases, the reset frequency matches the accrual frequency
   * and thus there is only one fixing for the accrual period.
   * <p>
   * This will default to 'PeriodStart' if not specified.
   * 
   * @return the fixing relative to, not null
   */
  public FixingRelativeTo getFixingRelativeTo() {
    return fixingRelativeTo != null ? fixingRelativeTo : FixingRelativeTo.PERIOD_START;
  }

  /**
   * The offset of the fixing date from each adjusted reset date,
   * providing a default result if no override specified.
   * <p>
   * The offset is applied to the base date specified by {@code fixingRelativeTo}.
   * The offset is typically a negative number of business days.
   * The data model permits the offset to differ from that of the index,
   * however the two are typically the same.
   * <p>
   * This will default to the fixing date offset of the index if not specified.
   * 
   * @return the fixing date offset, not null
   */
  public DaysAdjustment getFixingDateOffset() {
    return fixingDateOffset != null ? fixingDateOffset : index.getFixingDateOffset();
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
  @Override
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
            .stubConvention(getStubConvention())
            .rollConvention(rollConvention)
            .build())
        .paymentSchedule(PaymentSchedule.builder()
            .paymentFrequency(getPaymentFrequency())
            .paymentDateOffset(getPaymentDateOffset())
            .compoundingMethod(getCompoundingMethod())
            .build())
        .notionalSchedule(NotionalSchedule.builder()
            .currency(getCurrency())
            .finalExchange(notionalExchange)
            .initialExchange(notionalExchange)
            .amount(ValueSchedule.of(notional)).build())
        .calculation(IborRateCalculation.builder()
            .index(index)
            .dayCount(getDayCount())
            .fixingRelativeTo(getFixingRelativeTo())
            .fixingDateOffset(getFixingDateOffset())
            .spread(spread != 0 ? ValueSchedule.of(spread) : null)
            .build())
        .build();
  }

  //------------------------- AUTOGENERATED START -------------------------
  /**
   * The meta-bean for {@code IborRateSwapLegConvention}.
   * @return the meta-bean, not null
   */
  public static IborRateSwapLegConvention.Meta meta() {
    return IborRateSwapLegConvention.Meta.INSTANCE;
  }

  static {
    MetaBean.register(IborRateSwapLegConvention.Meta.INSTANCE);
  }

  /**
   * The serialization version id.
   */
  private static final long serialVersionUID = 1L;

  /**
   * Returns a builder used to create an instance of the bean.
   * @return the builder, not null
   */
  public static IborRateSwapLegConvention.Builder builder() {
    return new IborRateSwapLegConvention.Builder();
  }

  private IborRateSwapLegConvention(
      IborIndex index,
      Currency currency,
      DayCount dayCount,
      Frequency accrualFrequency,
      BusinessDayAdjustment accrualBusinessDayAdjustment,
      BusinessDayAdjustment startDateBusinessDayAdjustment,
      BusinessDayAdjustment endDateBusinessDayAdjustment,
      StubConvention stubConvention,
      RollConvention rollConvention,
      FixingRelativeTo fixingRelativeTo,
      DaysAdjustment fixingDateOffset,
      Frequency paymentFrequency,
      DaysAdjustment paymentDateOffset,
      CompoundingMethod compoundingMethod,
      boolean notionalExchange) {
    JodaBeanUtils.notNull(index, "index");
    this.index = index;
    this.currency = currency;
    this.dayCount = dayCount;
    this.accrualFrequency = accrualFrequency;
    this.accrualBusinessDayAdjustment = accrualBusinessDayAdjustment;
    this.startDateBusinessDayAdjustment = startDateBusinessDayAdjustment;
    this.endDateBusinessDayAdjustment = endDateBusinessDayAdjustment;
    this.stubConvention = stubConvention;
    this.rollConvention = rollConvention;
    this.fixingRelativeTo = fixingRelativeTo;
    this.fixingDateOffset = fixingDateOffset;
    this.paymentFrequency = paymentFrequency;
    this.paymentDateOffset = paymentDateOffset;
    this.compoundingMethod = compoundingMethod;
    this.notionalExchange = notionalExchange;
  }

  @Override
  public IborRateSwapLegConvention.Meta metaBean() {
    return IborRateSwapLegConvention.Meta.INSTANCE;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the Ibor index.
   * <p>
   * The floating rate to be paid is based on this index
   * It will be a well known market index such as 'GBP-LIBOR-3M'.
   * @return the value of the property, not null
   */
  @Override
  public IborIndex getIndex() {
    return index;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the flag indicating whether to exchange the notional.
   * <p>
   * If 'true', the notional there is both an initial exchange and a final exchange of notional.
   * <p>
   * This will default to 'false' if not specified.
   * @return the value of the property
   */
  public boolean isNotionalExchange() {
    return notionalExchange;
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
      IborRateSwapLegConvention other = (IborRateSwapLegConvention) obj;
      return JodaBeanUtils.equal(index, other.index) &&
          JodaBeanUtils.equal(currency, other.currency) &&
          JodaBeanUtils.equal(dayCount, other.dayCount) &&
          JodaBeanUtils.equal(accrualFrequency, other.accrualFrequency) &&
          JodaBeanUtils.equal(accrualBusinessDayAdjustment, other.accrualBusinessDayAdjustment) &&
          JodaBeanUtils.equal(startDateBusinessDayAdjustment, other.startDateBusinessDayAdjustment) &&
          JodaBeanUtils.equal(endDateBusinessDayAdjustment, other.endDateBusinessDayAdjustment) &&
          JodaBeanUtils.equal(stubConvention, other.stubConvention) &&
          JodaBeanUtils.equal(rollConvention, other.rollConvention) &&
          JodaBeanUtils.equal(fixingRelativeTo, other.fixingRelativeTo) &&
          JodaBeanUtils.equal(fixingDateOffset, other.fixingDateOffset) &&
          JodaBeanUtils.equal(paymentFrequency, other.paymentFrequency) &&
          JodaBeanUtils.equal(paymentDateOffset, other.paymentDateOffset) &&
          JodaBeanUtils.equal(compoundingMethod, other.compoundingMethod) &&
          (notionalExchange == other.notionalExchange);
    }
    return false;
  }

  @Override
  public int hashCode() {
    int hash = getClass().hashCode();
    hash = hash * 31 + JodaBeanUtils.hashCode(index);
    hash = hash * 31 + JodaBeanUtils.hashCode(currency);
    hash = hash * 31 + JodaBeanUtils.hashCode(dayCount);
    hash = hash * 31 + JodaBeanUtils.hashCode(accrualFrequency);
    hash = hash * 31 + JodaBeanUtils.hashCode(accrualBusinessDayAdjustment);
    hash = hash * 31 + JodaBeanUtils.hashCode(startDateBusinessDayAdjustment);
    hash = hash * 31 + JodaBeanUtils.hashCode(endDateBusinessDayAdjustment);
    hash = hash * 31 + JodaBeanUtils.hashCode(stubConvention);
    hash = hash * 31 + JodaBeanUtils.hashCode(rollConvention);
    hash = hash * 31 + JodaBeanUtils.hashCode(fixingRelativeTo);
    hash = hash * 31 + JodaBeanUtils.hashCode(fixingDateOffset);
    hash = hash * 31 + JodaBeanUtils.hashCode(paymentFrequency);
    hash = hash * 31 + JodaBeanUtils.hashCode(paymentDateOffset);
    hash = hash * 31 + JodaBeanUtils.hashCode(compoundingMethod);
    hash = hash * 31 + JodaBeanUtils.hashCode(notionalExchange);
    return hash;
  }

  @Override
  public String toString() {
    StringBuilder buf = new StringBuilder(512);
    buf.append("IborRateSwapLegConvention{");
    buf.append("index").append('=').append(JodaBeanUtils.toString(index)).append(',').append(' ');
    buf.append("currency").append('=').append(JodaBeanUtils.toString(currency)).append(',').append(' ');
    buf.append("dayCount").append('=').append(JodaBeanUtils.toString(dayCount)).append(',').append(' ');
    buf.append("accrualFrequency").append('=').append(JodaBeanUtils.toString(accrualFrequency)).append(',').append(' ');
    buf.append("accrualBusinessDayAdjustment").append('=').append(JodaBeanUtils.toString(accrualBusinessDayAdjustment)).append(',').append(' ');
    buf.append("startDateBusinessDayAdjustment").append('=').append(JodaBeanUtils.toString(startDateBusinessDayAdjustment)).append(',').append(' ');
    buf.append("endDateBusinessDayAdjustment").append('=').append(JodaBeanUtils.toString(endDateBusinessDayAdjustment)).append(',').append(' ');
    buf.append("stubConvention").append('=').append(JodaBeanUtils.toString(stubConvention)).append(',').append(' ');
    buf.append("rollConvention").append('=').append(JodaBeanUtils.toString(rollConvention)).append(',').append(' ');
    buf.append("fixingRelativeTo").append('=').append(JodaBeanUtils.toString(fixingRelativeTo)).append(',').append(' ');
    buf.append("fixingDateOffset").append('=').append(JodaBeanUtils.toString(fixingDateOffset)).append(',').append(' ');
    buf.append("paymentFrequency").append('=').append(JodaBeanUtils.toString(paymentFrequency)).append(',').append(' ');
    buf.append("paymentDateOffset").append('=').append(JodaBeanUtils.toString(paymentDateOffset)).append(',').append(' ');
    buf.append("compoundingMethod").append('=').append(JodaBeanUtils.toString(compoundingMethod)).append(',').append(' ');
    buf.append("notionalExchange").append('=').append(JodaBeanUtils.toString(notionalExchange));
    buf.append('}');
    return buf.toString();
  }

  //-----------------------------------------------------------------------
  /**
   * The meta-bean for {@code IborRateSwapLegConvention}.
   */
  public static final class Meta extends DirectMetaBean {
    /**
     * The singleton instance of the meta-bean.
     */
    static final Meta INSTANCE = new Meta();

    /**
     * The meta-property for the {@code index} property.
     */
    private final MetaProperty<IborIndex> index = DirectMetaProperty.ofImmutable(
        this, "index", IborRateSwapLegConvention.class, IborIndex.class);
    /**
     * The meta-property for the {@code currency} property.
     */
    private final MetaProperty<Currency> currency = DirectMetaProperty.ofImmutable(
        this, "currency", IborRateSwapLegConvention.class, Currency.class);
    /**
     * The meta-property for the {@code dayCount} property.
     */
    private final MetaProperty<DayCount> dayCount = DirectMetaProperty.ofImmutable(
        this, "dayCount", IborRateSwapLegConvention.class, DayCount.class);
    /**
     * The meta-property for the {@code accrualFrequency} property.
     */
    private final MetaProperty<Frequency> accrualFrequency = DirectMetaProperty.ofImmutable(
        this, "accrualFrequency", IborRateSwapLegConvention.class, Frequency.class);
    /**
     * The meta-property for the {@code accrualBusinessDayAdjustment} property.
     */
    private final MetaProperty<BusinessDayAdjustment> accrualBusinessDayAdjustment = DirectMetaProperty.ofImmutable(
        this, "accrualBusinessDayAdjustment", IborRateSwapLegConvention.class, BusinessDayAdjustment.class);
    /**
     * The meta-property for the {@code startDateBusinessDayAdjustment} property.
     */
    private final MetaProperty<BusinessDayAdjustment> startDateBusinessDayAdjustment = DirectMetaProperty.ofImmutable(
        this, "startDateBusinessDayAdjustment", IborRateSwapLegConvention.class, BusinessDayAdjustment.class);
    /**
     * The meta-property for the {@code endDateBusinessDayAdjustment} property.
     */
    private final MetaProperty<BusinessDayAdjustment> endDateBusinessDayAdjustment = DirectMetaProperty.ofImmutable(
        this, "endDateBusinessDayAdjustment", IborRateSwapLegConvention.class, BusinessDayAdjustment.class);
    /**
     * The meta-property for the {@code stubConvention} property.
     */
    private final MetaProperty<StubConvention> stubConvention = DirectMetaProperty.ofImmutable(
        this, "stubConvention", IborRateSwapLegConvention.class, StubConvention.class);
    /**
     * The meta-property for the {@code rollConvention} property.
     */
    private final MetaProperty<RollConvention> rollConvention = DirectMetaProperty.ofImmutable(
        this, "rollConvention", IborRateSwapLegConvention.class, RollConvention.class);
    /**
     * The meta-property for the {@code fixingRelativeTo} property.
     */
    private final MetaProperty<FixingRelativeTo> fixingRelativeTo = DirectMetaProperty.ofImmutable(
        this, "fixingRelativeTo", IborRateSwapLegConvention.class, FixingRelativeTo.class);
    /**
     * The meta-property for the {@code fixingDateOffset} property.
     */
    private final MetaProperty<DaysAdjustment> fixingDateOffset = DirectMetaProperty.ofImmutable(
        this, "fixingDateOffset", IborRateSwapLegConvention.class, DaysAdjustment.class);
    /**
     * The meta-property for the {@code paymentFrequency} property.
     */
    private final MetaProperty<Frequency> paymentFrequency = DirectMetaProperty.ofImmutable(
        this, "paymentFrequency", IborRateSwapLegConvention.class, Frequency.class);
    /**
     * The meta-property for the {@code paymentDateOffset} property.
     */
    private final MetaProperty<DaysAdjustment> paymentDateOffset = DirectMetaProperty.ofImmutable(
        this, "paymentDateOffset", IborRateSwapLegConvention.class, DaysAdjustment.class);
    /**
     * The meta-property for the {@code compoundingMethod} property.
     */
    private final MetaProperty<CompoundingMethod> compoundingMethod = DirectMetaProperty.ofImmutable(
        this, "compoundingMethod", IborRateSwapLegConvention.class, CompoundingMethod.class);
    /**
     * The meta-property for the {@code notionalExchange} property.
     */
    private final MetaProperty<Boolean> notionalExchange = DirectMetaProperty.ofImmutable(
        this, "notionalExchange", IborRateSwapLegConvention.class, Boolean.TYPE);
    /**
     * The meta-properties.
     */
    private final Map<String, MetaProperty<?>> metaPropertyMap$ = new DirectMetaPropertyMap(
        this, null,
        "index",
        "currency",
        "dayCount",
        "accrualFrequency",
        "accrualBusinessDayAdjustment",
        "startDateBusinessDayAdjustment",
        "endDateBusinessDayAdjustment",
        "stubConvention",
        "rollConvention",
        "fixingRelativeTo",
        "fixingDateOffset",
        "paymentFrequency",
        "paymentDateOffset",
        "compoundingMethod",
        "notionalExchange");

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
        case 232554996:  // fixingRelativeTo
          return fixingRelativeTo;
        case 873743726:  // fixingDateOffset
          return fixingDateOffset;
        case 863656438:  // paymentFrequency
          return paymentFrequency;
        case -716438393:  // paymentDateOffset
          return paymentDateOffset;
        case -1376171496:  // compoundingMethod
          return compoundingMethod;
        case -159410813:  // notionalExchange
          return notionalExchange;
      }
      return super.metaPropertyGet(propertyName);
    }

    @Override
    public IborRateSwapLegConvention.Builder builder() {
      return new IborRateSwapLegConvention.Builder();
    }

    @Override
    public Class<? extends IborRateSwapLegConvention> beanType() {
      return IborRateSwapLegConvention.class;
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
    public MetaProperty<IborIndex> index() {
      return index;
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
     * The meta-property for the {@code fixingRelativeTo} property.
     * @return the meta-property, not null
     */
    public MetaProperty<FixingRelativeTo> fixingRelativeTo() {
      return fixingRelativeTo;
    }

    /**
     * The meta-property for the {@code fixingDateOffset} property.
     * @return the meta-property, not null
     */
    public MetaProperty<DaysAdjustment> fixingDateOffset() {
      return fixingDateOffset;
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

    /**
     * The meta-property for the {@code notionalExchange} property.
     * @return the meta-property, not null
     */
    public MetaProperty<Boolean> notionalExchange() {
      return notionalExchange;
    }

    //-----------------------------------------------------------------------
    @Override
    protected Object propertyGet(Bean bean, String propertyName, boolean quiet) {
      switch (propertyName.hashCode()) {
        case 100346066:  // index
          return ((IborRateSwapLegConvention) bean).getIndex();
        case 575402001:  // currency
          return ((IborRateSwapLegConvention) bean).currency;
        case 1905311443:  // dayCount
          return ((IborRateSwapLegConvention) bean).dayCount;
        case 945206381:  // accrualFrequency
          return ((IborRateSwapLegConvention) bean).accrualFrequency;
        case 896049114:  // accrualBusinessDayAdjustment
          return ((IborRateSwapLegConvention) bean).accrualBusinessDayAdjustment;
        case 429197561:  // startDateBusinessDayAdjustment
          return ((IborRateSwapLegConvention) bean).startDateBusinessDayAdjustment;
        case -734327136:  // endDateBusinessDayAdjustment
          return ((IborRateSwapLegConvention) bean).endDateBusinessDayAdjustment;
        case -31408449:  // stubConvention
          return ((IborRateSwapLegConvention) bean).stubConvention;
        case -10223666:  // rollConvention
          return ((IborRateSwapLegConvention) bean).rollConvention;
        case 232554996:  // fixingRelativeTo
          return ((IborRateSwapLegConvention) bean).fixingRelativeTo;
        case 873743726:  // fixingDateOffset
          return ((IborRateSwapLegConvention) bean).fixingDateOffset;
        case 863656438:  // paymentFrequency
          return ((IborRateSwapLegConvention) bean).paymentFrequency;
        case -716438393:  // paymentDateOffset
          return ((IborRateSwapLegConvention) bean).paymentDateOffset;
        case -1376171496:  // compoundingMethod
          return ((IborRateSwapLegConvention) bean).compoundingMethod;
        case -159410813:  // notionalExchange
          return ((IborRateSwapLegConvention) bean).isNotionalExchange();
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
   * The bean-builder for {@code IborRateSwapLegConvention}.
   */
  public static final class Builder extends DirectFieldsBeanBuilder<IborRateSwapLegConvention> {

    private IborIndex index;
    private Currency currency;
    private DayCount dayCount;
    private Frequency accrualFrequency;
    private BusinessDayAdjustment accrualBusinessDayAdjustment;
    private BusinessDayAdjustment startDateBusinessDayAdjustment;
    private BusinessDayAdjustment endDateBusinessDayAdjustment;
    private StubConvention stubConvention;
    private RollConvention rollConvention;
    private FixingRelativeTo fixingRelativeTo;
    private DaysAdjustment fixingDateOffset;
    private Frequency paymentFrequency;
    private DaysAdjustment paymentDateOffset;
    private CompoundingMethod compoundingMethod;
    private boolean notionalExchange;

    /**
     * Restricted constructor.
     */
    private Builder() {
    }

    /**
     * Restricted copy constructor.
     * @param beanToCopy  the bean to copy from, not null
     */
    private Builder(IborRateSwapLegConvention beanToCopy) {
      this.index = beanToCopy.getIndex();
      this.currency = beanToCopy.currency;
      this.dayCount = beanToCopy.dayCount;
      this.accrualFrequency = beanToCopy.accrualFrequency;
      this.accrualBusinessDayAdjustment = beanToCopy.accrualBusinessDayAdjustment;
      this.startDateBusinessDayAdjustment = beanToCopy.startDateBusinessDayAdjustment;
      this.endDateBusinessDayAdjustment = beanToCopy.endDateBusinessDayAdjustment;
      this.stubConvention = beanToCopy.stubConvention;
      this.rollConvention = beanToCopy.rollConvention;
      this.fixingRelativeTo = beanToCopy.fixingRelativeTo;
      this.fixingDateOffset = beanToCopy.fixingDateOffset;
      this.paymentFrequency = beanToCopy.paymentFrequency;
      this.paymentDateOffset = beanToCopy.paymentDateOffset;
      this.compoundingMethod = beanToCopy.compoundingMethod;
      this.notionalExchange = beanToCopy.isNotionalExchange();
    }

    //-----------------------------------------------------------------------
    @Override
    public Object get(String propertyName) {
      switch (propertyName.hashCode()) {
        case 100346066:  // index
          return index;
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
        case 232554996:  // fixingRelativeTo
          return fixingRelativeTo;
        case 873743726:  // fixingDateOffset
          return fixingDateOffset;
        case 863656438:  // paymentFrequency
          return paymentFrequency;
        case -716438393:  // paymentDateOffset
          return paymentDateOffset;
        case -1376171496:  // compoundingMethod
          return compoundingMethod;
        case -159410813:  // notionalExchange
          return notionalExchange;
        default:
          throw new NoSuchElementException("Unknown property: " + propertyName);
      }
    }

    @Override
    public Builder set(String propertyName, Object newValue) {
      switch (propertyName.hashCode()) {
        case 100346066:  // index
          this.index = (IborIndex) newValue;
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
        case 232554996:  // fixingRelativeTo
          this.fixingRelativeTo = (FixingRelativeTo) newValue;
          break;
        case 873743726:  // fixingDateOffset
          this.fixingDateOffset = (DaysAdjustment) newValue;
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
        case -159410813:  // notionalExchange
          this.notionalExchange = (Boolean) newValue;
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
    public IborRateSwapLegConvention build() {
      return new IborRateSwapLegConvention(
          index,
          currency,
          dayCount,
          accrualFrequency,
          accrualBusinessDayAdjustment,
          startDateBusinessDayAdjustment,
          endDateBusinessDayAdjustment,
          stubConvention,
          rollConvention,
          fixingRelativeTo,
          fixingDateOffset,
          paymentFrequency,
          paymentDateOffset,
          compoundingMethod,
          notionalExchange);
    }

    //-----------------------------------------------------------------------
    /**
     * Sets the Ibor index.
     * <p>
     * The floating rate to be paid is based on this index
     * It will be a well known market index such as 'GBP-LIBOR-3M'.
     * @param index  the new value, not null
     * @return this, for chaining, not null
     */
    public Builder index(IborIndex index) {
      JodaBeanUtils.notNull(index, "index");
      this.index = index;
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
     * This will default to the tenor of the index if not specified.
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
     * This will default to 'SmartInitial' if not specified.
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
     * Sets the base date that each fixing is made relative to, optional with defaulting getter.
     * <p>
     * The fixing date is relative to either the start or end of each reset period.
     * <p>
     * Note that in most cases, the reset frequency matches the accrual frequency
     * and thus there is only one fixing for the accrual period.
     * <p>
     * This will default to 'PeriodStart' if not specified.
     * @param fixingRelativeTo  the new value
     * @return this, for chaining, not null
     */
    public Builder fixingRelativeTo(FixingRelativeTo fixingRelativeTo) {
      this.fixingRelativeTo = fixingRelativeTo;
      return this;
    }

    /**
     * Sets the offset of the fixing date from each adjusted reset date.
     * <p>
     * The offset is applied to the base date specified by {@code fixingRelativeTo}.
     * The offset is typically a negative number of business days.
     * The data model permits the offset to differ from that of the index,
     * however the two are typically the same.
     * <p>
     * This will default to the fixing date offset of the index if not specified.
     * @param fixingDateOffset  the new value
     * @return this, for chaining, not null
     */
    public Builder fixingDateOffset(DaysAdjustment fixingDateOffset) {
      this.fixingDateOffset = fixingDateOffset;
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

    /**
     * Sets the flag indicating whether to exchange the notional.
     * <p>
     * If 'true', the notional there is both an initial exchange and a final exchange of notional.
     * <p>
     * This will default to 'false' if not specified.
     * @param notionalExchange  the new value
     * @return this, for chaining, not null
     */
    public Builder notionalExchange(boolean notionalExchange) {
      this.notionalExchange = notionalExchange;
      return this;
    }

    //-----------------------------------------------------------------------
    @Override
    public String toString() {
      StringBuilder buf = new StringBuilder(512);
      buf.append("IborRateSwapLegConvention.Builder{");
      buf.append("index").append('=').append(JodaBeanUtils.toString(index)).append(',').append(' ');
      buf.append("currency").append('=').append(JodaBeanUtils.toString(currency)).append(',').append(' ');
      buf.append("dayCount").append('=').append(JodaBeanUtils.toString(dayCount)).append(',').append(' ');
      buf.append("accrualFrequency").append('=').append(JodaBeanUtils.toString(accrualFrequency)).append(',').append(' ');
      buf.append("accrualBusinessDayAdjustment").append('=').append(JodaBeanUtils.toString(accrualBusinessDayAdjustment)).append(',').append(' ');
      buf.append("startDateBusinessDayAdjustment").append('=').append(JodaBeanUtils.toString(startDateBusinessDayAdjustment)).append(',').append(' ');
      buf.append("endDateBusinessDayAdjustment").append('=').append(JodaBeanUtils.toString(endDateBusinessDayAdjustment)).append(',').append(' ');
      buf.append("stubConvention").append('=').append(JodaBeanUtils.toString(stubConvention)).append(',').append(' ');
      buf.append("rollConvention").append('=').append(JodaBeanUtils.toString(rollConvention)).append(',').append(' ');
      buf.append("fixingRelativeTo").append('=').append(JodaBeanUtils.toString(fixingRelativeTo)).append(',').append(' ');
      buf.append("fixingDateOffset").append('=').append(JodaBeanUtils.toString(fixingDateOffset)).append(',').append(' ');
      buf.append("paymentFrequency").append('=').append(JodaBeanUtils.toString(paymentFrequency)).append(',').append(' ');
      buf.append("paymentDateOffset").append('=').append(JodaBeanUtils.toString(paymentDateOffset)).append(',').append(' ');
      buf.append("compoundingMethod").append('=').append(JodaBeanUtils.toString(compoundingMethod)).append(',').append(' ');
      buf.append("notionalExchange").append('=').append(JodaBeanUtils.toString(notionalExchange));
      buf.append('}');
      return buf.toString();
    }

  }

  //-------------------------- AUTOGENERATED END --------------------------
}
