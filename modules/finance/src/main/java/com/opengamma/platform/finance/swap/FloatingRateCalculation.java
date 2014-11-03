/**
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.platform.finance.swap;

import static com.google.common.base.Objects.firstNonNull;

import java.io.Serializable;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
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
import com.opengamma.basics.currency.Currency;
import com.opengamma.basics.date.DayCount;
import com.opengamma.basics.date.DaysAdjustment;
import com.opengamma.basics.index.RateIndex;
import com.opengamma.basics.schedule.Schedule;
import com.opengamma.basics.schedule.SchedulePeriod;
import com.opengamma.basics.value.ValueSchedule;

/**
 * Defines the calculation of a floating rate swap leg.
 * <p>
 * This defines the data necessary to calculate the amount payable on the leg.
 * The amount is based on the observed value of a floating rate index.
 * <p>
 * The rate is observed once for each <i>reset period</i> and referred to as a <i>fixing</i>.
 * The actual date of observation is the <i>fixing date</i>, which is relative to either
 * the start or end of the reset period.
 * <p>
 * The reset period is typically the same as the accrual period.
 * In this case, the floating rate for the accrual period is based directly on the fixing.
 * <p>
 * In some swaps, the reset period is a subdivision of the accrual period.
 * In this case, there are multiple fixings for each accrual period, and the floating rate
 * for the accrual period is based on an average of the fixings.
 * <p>
 * This class contains a number of mandatory elements, supplemented by optional ones.
 * The optional elements allow the specification of gearing, spread, averaging, stub rates
 * and a fixed first rate.
 */
@BeanDefinition
public final class FloatingRateCalculation
    implements ImmutableBean, Serializable {

  /** Serialization version. */
  private static final long serialVersionUID = 1L;

  /**
   * Whether the calculation is pay or receive.
   * <p>
   * A pay value implies that the resulting amount is paid to the counterparty.
   * A receive value implies that the resulting amount is received from the counterparty.
   * Note that negative interest rates can result in a payment in the opposite
   * direction to that implied by this indicator.
   */
  @PropertyDefinition(validate = "notNull")
  private final PayReceive payReceive;
  /**
   * The notional amount, always positive.
   * <p>
   * The notional amount of the swap leg, which can vary during the lifetime of the swap.
   * In most cases, the notional amount is not exchanged, with only the net difference being exchanged.
   * However, in certain cases, initial, final or intermediate amounts are exchanged.
   * <p>
   * The notional expressed here is always positive, see {@code payReceive}.
   */
  @PropertyDefinition(validate = "notNull")
  private final NotionalAmount notional;
  /**
   * The day count convention applicable.
   * <p>
   * This is used to convert dates to a numerical value.
   */
  @PropertyDefinition(validate = "notNull")
  private final DayCount dayCount;
  /**
   * The floating rate index to be used.
   * <p>
   * The rate to be paid is based on this index
   * It will be a well known market rate such as 'GBP-LIBOR-3M'.
   */
  @PropertyDefinition(validate = "notNull")
  private final RateIndex index;
  /**
   * The base date that each fixing is made relative to, defaulted to 'PeriodStart'.
   * <p>
   * The fixing date is relative to either the start or end of each reset period.
   * <p>
   * Note that in most cases, the reset frequency matches the accrual frequency
   * and thus there is only one fixing for the accrual period.
   */
  @PropertyDefinition(validate = "notNull")
  private final FixingRelativeTo fixingRelativeTo;
  /**
   * The offset of the fixing date from each adjusted reset date.
   * <p>
   * The offset is applied to the base date specified by {@code fixingRelativeTo}.
   * The offset is typically a negative number of business days.
   * <p>
   * Note that in most cases, the reset frequency matches the accrual frequency
   * and thus there is only one fixing for the accrual period.
   */
  @PropertyDefinition(validate = "notNull")
  private final DaysAdjustment fixingOffset;
  /**
   * The negative rate method, defaulted to 'AllowNegative'.
   * <p>
   * This is used when the interest rate, observed or calculated, goes negative.
   * It does not apply if the rate is fixed, such as in a stub or using {@code firstRegularRate}.
   * <p>
   * Defined by the 2006 ISDA definitions article 6.4.
   */
  @PropertyDefinition(validate = "notNull")
  private final NegativeRateMethod negativeRateMethod;

  /**
   * The reset schedule, used when averaging rates, optional.
   * <p>
   * Most swaps have a single fixing for each accrual period.
   * This property allows multiple fixings to be defined by dividing the accrual periods into reset periods.
   * <p>
   * If this property is null, then the reset period is the same as the accrual period.
   * If this property is non-null, then the accrual period is divided as per the information
   * in the reset schedule, multiple fixing dates are calculated, and rate averaging performed.
   */
  @PropertyDefinition
  private final ResetSchedule resetPeriods;
  /**
   * The first floating rate of the first regular reset period, with a 5% rate expressed as 0.05, optional.
   * <p>
   * In certain circumstances two counterparties agree the rate of the first fixing when the contract starts.
   * The rate is applicable for the first reset period of the first <i>regular</i> accrual period.
   * It is used in place of an observed fixing.
   * Other calculation elements, such as gearing or spread, still apply.
   * After the first reset period, the rate is calculated via the normal fixing process.
   * <p>
   * If the first floating rate applies to the initial stub rather than the regular accrual periods
   * it must be specified using {@code initialStub}.
   * <p>
   * If this property is null, then the first floating rate is calculated via the normal fixing process.
   */
  @PropertyDefinition
  private final Double firstRegularRate;
  /**
   * The rate to be used in initial stub, optional.
   * <p>
   * The initial stub of a swap may have different rate rules to the regular accrual periods.
   * A fixed rate may be specified, a different floating rate or a linearly interpolated floating rate.
   * This may be null if there is no initial stub, or if the index during the stub is the same
   * as the main floating rate index.
   * <p>
   * If this property is null, then the main index applies during any initial stub.
   * If this property is non-null and there is no initial stub, it is ignored.
   */
  @PropertyDefinition
  private final FloatingRateStub initialStub;
  /**
   * The rate to be used in final stub, optional.
   * <p>
   * The final stub of a swap may have different rate rules to the regular accrual periods.
   * A fixed rate may be specified, a different floating rate or a linearly interpolated floating rate.
   * This may be null if there is no final stub, or if the index during the stub is the same
   * as the main floating rate index.
   * <p>
   * If this property is null, then the main index applies during any final stub.
   * If this property is non-null and there is no final stub, it is ignored.
   */
  @PropertyDefinition
  private final FloatingRateStub finalStub;
  /**
   * The gearing multiplier, optional.
   * <p>
   * This defines the gearing as an initial value and a list of adjustments.
   * The gearing is only permitted to change at accrual period boundaries.
   * <p>
   * When calculating the rate, the fixing rate is multiplied by the gearing.
   * A gearing of 1 has no effect.
   * If both gearing and spread exist, then the gearing is applied first.
   * <p>
   * If this property is null, then no gearing applies.
   * <p>
   * Gearing is also known as <i>leverage</i>.
   */
  @PropertyDefinition
  private final ValueSchedule gearing;
  /**
   * The spread rate, with a 5% rate expressed as 0.05, optional.
   * <p>
   * This defines the spread as an initial value and a list of adjustments.
   * The spread is only permitted to change at accrual period boundaries.
   * Spread is a per annum rate.
   * <p>
   * When calculating the rate, the spread is added to the fixing rate.
   * A spread of 0 has no effect.
   * If both gearing and spread exist, then the gearing is applied first.
   * <p>
   * If this property is null, then no spread applies.
   * <p>
   * Defined by the 2006 ISDA definitions article 6.2e.
   */
  @PropertyDefinition
  private final ValueSchedule spread;

  //-------------------------------------------------------------------------
  @ImmutableDefaults
  private static void applyDefaults(Builder builder) {
    builder.fixingRelativeTo(FixingRelativeTo.PERIOD_START);
    builder.negativeRateMethod(NegativeRateMethod.ALLOW_NEGATIVE);
  }

  //-------------------------------------------------------------------------
  /**
   * Creates the matching accrual periods based on this calculation.
   * 
   * @param schedule  the schedule
   * @return the expanded accrual periods
   * @throws RuntimeException if the swap calculation is invalid
   */
  ImmutableList<AccrualPeriod> createAccrualPeriods(Schedule schedule) {
    if (notional.getFxReset() != null ||
        notional.isInitialExchange() ||
        notional.isFinalExchange() ||
        notional.isIntermediateExchange() ||
        resetPeriods != null) {
      throw new UnsupportedOperationException();
    }
    // avoid null stub definitions if there are stubs
    boolean hasInitialStub = schedule.getFirstPeriod().isStub();
    boolean hasFinalStub = schedule.getLastPeriod().isStub();
    if ((hasInitialStub && initialStub == null) ||
        (hasFinalStub && finalStub == null)) {
      return toBuilder()
          .initialStub(firstNonNull(initialStub, FloatingRateStub.NONE))
          .finalStub(firstNonNull(finalStub, FloatingRateStub.NONE))
          .build()
          .createAccrualPeriods(schedule);
    }
    // resolve data by schedule
    ImmutableList.Builder<AccrualPeriod> accrualPeriods = ImmutableList.builder();
    List<Double> resolvedNotionals = notional.getAmount().resolveValues(schedule.getPeriods());
    List<Double> resolvedGearings = firstNonNull(gearing, ValueSchedule.of(1)).resolveValues(schedule.getPeriods());
    List<Double> resolvedSpreads = firstNonNull(spread, ValueSchedule.of(0)).resolveValues(schedule.getPeriods());
    List<Double> resolvedRates = resolveFixedRates(schedule.size(), hasInitialStub, hasFinalStub);
    List<RateIndex> resolvedIndices = resolveIndices(schedule.size(), hasInitialStub, hasFinalStub);
    List<RateIndex> resolveInterpolated = resolveInterpolatedIndices(schedule.size(), hasInitialStub, hasFinalStub);
    Currency currency = notional.getCurrency();
    // build accrual periods
    for (int i = 0; i < schedule.size(); i++) {
      SchedulePeriod period = schedule.getPeriod(i);
      Double fixedRate = resolvedRates.get(i);
      if (fixedRate != null) {
        // floating rate overridden by fixed rate
        accrualPeriods.add(FixedRateAccrualPeriod.builder()
            .startDate(period.getStartDate())
            .endDate(period.getEndDate())
            .yearFraction(createYearFraction(period))
            .currency(currency)
            .notional(payReceive.normalize(resolvedNotionals.get(i)))
            .rate(negativeRateMethod.adjust(fixedRate * resolvedGearings.get(i) + resolvedSpreads.get(i)))
            .build());
      } else {
        // floating rate needed
        accrualPeriods.add(FloatingRateAccrualPeriod.builder()
            .startDate(period.getStartDate())
            .endDate(period.getEndDate())
            .yearFraction(createYearFraction(period))
            .currency(currency)
            .notional(payReceive.normalize(resolvedNotionals.get(i)))
            .index(resolvedIndices.get(i))
            .indexInterpolated(resolveInterpolated.get(i))
            .fixingDate(createFixingDate(period))
            .negativeRateMethod(negativeRateMethod)
            .gearing(resolvedGearings.get(i))
            .spread(resolvedSpreads.get(i))
            .build());
      }
    }
    return accrualPeriods.build();
  }

  // resolves any fixed rates, list contains null indicating no fixed rate
  private List<Double> resolveFixedRates(int size, boolean hasInitialStub, boolean hasFinalStub) {
    // initialStub and finalStub are not-null at this point
    if ((hasInitialStub && initialStub.isFixedRate()) ||
        (hasFinalStub && finalStub.isFixedRate()) ||
        firstRegularRate != null) {
      Double[] rates = new Double[size];
      if (hasInitialStub) {
        rates[0] = initialStub.getRate();
        rates[1] = firstRegularRate;
      } else {
        rates[0] = firstRegularRate;
      }
      if (hasFinalStub) {
        // this will overwrite firstRegularRate if size = 2 (intentional behavior)
        rates[rates.length - 1] = finalStub.getRate();
      }
      return Arrays.asList(rates);
    }
    return Collections.nCopies(size, null);
  }

  // resolves any varying indices, list contains no nulls
  private List<RateIndex> resolveIndices(int size, boolean hasInitialStub, boolean hasFinalStub) {
    // initialStub and finalStub are not-null at this point
    List<RateIndex> result = Collections.nCopies(size, index);
    if ((hasInitialStub && initialStub.isFloatingRate()) ||
        (hasFinalStub && finalStub.isFloatingRate())) {
      result = new ArrayList<>(result);
      if (hasInitialStub && initialStub.isFloatingRate()) {
        result.set(0, initialStub.getStartIndex());
      }
      if (hasFinalStub && finalStub.isFloatingRate()) {
        result.set(size - 1, finalStub.getStartIndex());
      }
    }
    return result;
  }

  // resolves any varying linear interpolated indices, list contains null indicating no interpolation
  private List<RateIndex> resolveInterpolatedIndices(int size, boolean hasInitialStub, boolean hasFinalStub) {
    // initialStub and finalStub are not-null at this point
    if ((hasInitialStub && initialStub.isInterpolated()) ||
        (hasFinalStub && finalStub.isInterpolated())) {
      RateIndex[] rates = new RateIndex[size];
      if (hasInitialStub && initialStub.isInterpolated()) {
        rates[0] = initialStub.getEndIndex();
      }
      if (hasFinalStub && finalStub.isInterpolated()) {
        rates[rates.length - 1] = finalStub.getEndIndex();
      }
      return Arrays.asList(rates);
    }
    return Collections.nCopies(size, null);
  }

  // determine the year fraction
  private double createYearFraction(SchedulePeriod period) {
    return dayCount.getDayCountFraction(period.getStartDate(), period.getEndDate(), period);
  }

  // determine the fixing date
  private LocalDate createFixingDate(SchedulePeriod period) {
    switch (fixingRelativeTo) {
      case PERIOD_END:
        return fixingOffset.adjust(period.getEndDate());
      case PERIOD_START:
      default:
        return fixingOffset.adjust(period.getStartDate());
    }
  }

  //------------------------- AUTOGENERATED START -------------------------
  ///CLOVER:OFF
  /**
   * The meta-bean for {@code FloatingRateCalculation}.
   * @return the meta-bean, not null
   */
  public static FloatingRateCalculation.Meta meta() {
    return FloatingRateCalculation.Meta.INSTANCE;
  }

  static {
    JodaBeanUtils.registerMetaBean(FloatingRateCalculation.Meta.INSTANCE);
  }

  /**
   * Returns a builder used to create an instance of the bean.
   * @return the builder, not null
   */
  public static FloatingRateCalculation.Builder builder() {
    return new FloatingRateCalculation.Builder();
  }

  private FloatingRateCalculation(
      PayReceive payReceive,
      NotionalAmount notional,
      DayCount dayCount,
      RateIndex index,
      FixingRelativeTo fixingRelativeTo,
      DaysAdjustment fixingOffset,
      NegativeRateMethod negativeRateMethod,
      ResetSchedule resetPeriods,
      Double firstRegularRate,
      FloatingRateStub initialStub,
      FloatingRateStub finalStub,
      ValueSchedule gearing,
      ValueSchedule spread) {
    JodaBeanUtils.notNull(payReceive, "payReceive");
    JodaBeanUtils.notNull(notional, "notional");
    JodaBeanUtils.notNull(dayCount, "dayCount");
    JodaBeanUtils.notNull(index, "index");
    JodaBeanUtils.notNull(fixingRelativeTo, "fixingRelativeTo");
    JodaBeanUtils.notNull(fixingOffset, "fixingOffset");
    JodaBeanUtils.notNull(negativeRateMethod, "negativeRateMethod");
    this.payReceive = payReceive;
    this.notional = notional;
    this.dayCount = dayCount;
    this.index = index;
    this.fixingRelativeTo = fixingRelativeTo;
    this.fixingOffset = fixingOffset;
    this.negativeRateMethod = negativeRateMethod;
    this.resetPeriods = resetPeriods;
    this.firstRegularRate = firstRegularRate;
    this.initialStub = initialStub;
    this.finalStub = finalStub;
    this.gearing = gearing;
    this.spread = spread;
  }

  @Override
  public FloatingRateCalculation.Meta metaBean() {
    return FloatingRateCalculation.Meta.INSTANCE;
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
   * Gets whether the calculation is pay or receive.
   * <p>
   * A pay value implies that the resulting amount is paid to the counterparty.
   * A receive value implies that the resulting amount is received from the counterparty.
   * Note that negative interest rates can result in a payment in the opposite
   * direction to that implied by this indicator.
   * @return the value of the property, not null
   */
  public PayReceive getPayReceive() {
    return payReceive;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the notional amount, always positive.
   * <p>
   * The notional amount of the swap leg, which can vary during the lifetime of the swap.
   * In most cases, the notional amount is not exchanged, with only the net difference being exchanged.
   * However, in certain cases, initial, final or intermediate amounts are exchanged.
   * <p>
   * The notional expressed here is always positive, see {@code payReceive}.
   * @return the value of the property, not null
   */
  public NotionalAmount getNotional() {
    return notional;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the day count convention applicable.
   * <p>
   * This is used to convert dates to a numerical value.
   * @return the value of the property, not null
   */
  public DayCount getDayCount() {
    return dayCount;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the floating rate index to be used.
   * <p>
   * The rate to be paid is based on this index
   * It will be a well known market rate such as 'GBP-LIBOR-3M'.
   * @return the value of the property, not null
   */
  public RateIndex getIndex() {
    return index;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the base date that each fixing is made relative to, defaulted to 'PeriodStart'.
   * <p>
   * The fixing date is relative to either the start or end of each reset period.
   * <p>
   * Note that in most cases, the reset frequency matches the accrual frequency
   * and thus there is only one fixing for the accrual period.
   * @return the value of the property, not null
   */
  public FixingRelativeTo getFixingRelativeTo() {
    return fixingRelativeTo;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the offset of the fixing date from each adjusted reset date.
   * <p>
   * The offset is applied to the base date specified by {@code fixingRelativeTo}.
   * The offset is typically a negative number of business days.
   * <p>
   * Note that in most cases, the reset frequency matches the accrual frequency
   * and thus there is only one fixing for the accrual period.
   * @return the value of the property, not null
   */
  public DaysAdjustment getFixingOffset() {
    return fixingOffset;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the negative rate method, defaulted to 'AllowNegative'.
   * <p>
   * This is used when the interest rate, observed or calculated, goes negative.
   * It does not apply if the rate is fixed, such as in a stub or using {@code firstRegularRate}.
   * <p>
   * Defined by the 2006 ISDA definitions article 6.4.
   * @return the value of the property, not null
   */
  public NegativeRateMethod getNegativeRateMethod() {
    return negativeRateMethod;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the reset schedule, used when averaging rates, optional.
   * <p>
   * Most swaps have a single fixing for each accrual period.
   * This property allows multiple fixings to be defined by dividing the accrual periods into reset periods.
   * <p>
   * If this property is null, then the reset period is the same as the accrual period.
   * If this property is non-null, then the accrual period is divided as per the information
   * in the reset schedule, multiple fixing dates are calculated, and rate averaging performed.
   * @return the value of the property
   */
  public ResetSchedule getResetPeriods() {
    return resetPeriods;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the first floating rate of the first regular reset period, with a 5% rate expressed as 0.05, optional.
   * <p>
   * In certain circumstances two counterparties agree the rate of the first fixing when the contract starts.
   * The rate is applicable for the first reset period of the first <i>regular</i> accrual period.
   * It is used in place of an observed fixing.
   * Other calculation elements, such as gearing or spread, still apply.
   * After the first reset period, the rate is calculated via the normal fixing process.
   * <p>
   * If the first floating rate applies to the initial stub rather than the regular accrual periods
   * it must be specified using {@code initialStub}.
   * <p>
   * If this property is null, then the first floating rate is calculated via the normal fixing process.
   * @return the value of the property
   */
  public Double getFirstRegularRate() {
    return firstRegularRate;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the rate to be used in initial stub, optional.
   * <p>
   * The initial stub of a swap may have different rate rules to the regular accrual periods.
   * A fixed rate may be specified, a different floating rate or a linearly interpolated floating rate.
   * This may be null if there is no initial stub, or if the index during the stub is the same
   * as the main floating rate index.
   * <p>
   * If this property is null, then the main index applies during any initial stub.
   * If this property is non-null and there is no initial stub, it is ignored.
   * @return the value of the property
   */
  public FloatingRateStub getInitialStub() {
    return initialStub;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the rate to be used in final stub, optional.
   * <p>
   * The final stub of a swap may have different rate rules to the regular accrual periods.
   * A fixed rate may be specified, a different floating rate or a linearly interpolated floating rate.
   * This may be null if there is no final stub, or if the index during the stub is the same
   * as the main floating rate index.
   * <p>
   * If this property is null, then the main index applies during any final stub.
   * If this property is non-null and there is no final stub, it is ignored.
   * @return the value of the property
   */
  public FloatingRateStub getFinalStub() {
    return finalStub;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the gearing multiplier, optional.
   * <p>
   * This defines the gearing as an initial value and a list of adjustments.
   * The gearing is only permitted to change at accrual period boundaries.
   * <p>
   * When calculating the rate, the fixing rate is multiplied by the gearing.
   * A gearing of 1 has no effect.
   * If both gearing and spread exist, then the gearing is applied first.
   * <p>
   * If this property is null, then no gearing applies.
   * <p>
   * Gearing is also known as <i>leverage</i>.
   * @return the value of the property
   */
  public ValueSchedule getGearing() {
    return gearing;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the spread rate, with a 5% rate expressed as 0.05, optional.
   * <p>
   * This defines the spread as an initial value and a list of adjustments.
   * The spread is only permitted to change at accrual period boundaries.
   * Spread is a per annum rate.
   * <p>
   * When calculating the rate, the spread is added to the fixing rate.
   * A spread of 0 has no effect.
   * If both gearing and spread exist, then the gearing is applied first.
   * <p>
   * If this property is null, then no spread applies.
   * <p>
   * Defined by the 2006 ISDA definitions article 6.2e.
   * @return the value of the property
   */
  public ValueSchedule getSpread() {
    return spread;
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
      FloatingRateCalculation other = (FloatingRateCalculation) obj;
      return JodaBeanUtils.equal(getPayReceive(), other.getPayReceive()) &&
          JodaBeanUtils.equal(getNotional(), other.getNotional()) &&
          JodaBeanUtils.equal(getDayCount(), other.getDayCount()) &&
          JodaBeanUtils.equal(getIndex(), other.getIndex()) &&
          JodaBeanUtils.equal(getFixingRelativeTo(), other.getFixingRelativeTo()) &&
          JodaBeanUtils.equal(getFixingOffset(), other.getFixingOffset()) &&
          JodaBeanUtils.equal(getNegativeRateMethod(), other.getNegativeRateMethod()) &&
          JodaBeanUtils.equal(getResetPeriods(), other.getResetPeriods()) &&
          JodaBeanUtils.equal(getFirstRegularRate(), other.getFirstRegularRate()) &&
          JodaBeanUtils.equal(getInitialStub(), other.getInitialStub()) &&
          JodaBeanUtils.equal(getFinalStub(), other.getFinalStub()) &&
          JodaBeanUtils.equal(getGearing(), other.getGearing()) &&
          JodaBeanUtils.equal(getSpread(), other.getSpread());
    }
    return false;
  }

  @Override
  public int hashCode() {
    int hash = getClass().hashCode();
    hash += hash * 31 + JodaBeanUtils.hashCode(getPayReceive());
    hash += hash * 31 + JodaBeanUtils.hashCode(getNotional());
    hash += hash * 31 + JodaBeanUtils.hashCode(getDayCount());
    hash += hash * 31 + JodaBeanUtils.hashCode(getIndex());
    hash += hash * 31 + JodaBeanUtils.hashCode(getFixingRelativeTo());
    hash += hash * 31 + JodaBeanUtils.hashCode(getFixingOffset());
    hash += hash * 31 + JodaBeanUtils.hashCode(getNegativeRateMethod());
    hash += hash * 31 + JodaBeanUtils.hashCode(getResetPeriods());
    hash += hash * 31 + JodaBeanUtils.hashCode(getFirstRegularRate());
    hash += hash * 31 + JodaBeanUtils.hashCode(getInitialStub());
    hash += hash * 31 + JodaBeanUtils.hashCode(getFinalStub());
    hash += hash * 31 + JodaBeanUtils.hashCode(getGearing());
    hash += hash * 31 + JodaBeanUtils.hashCode(getSpread());
    return hash;
  }

  @Override
  public String toString() {
    StringBuilder buf = new StringBuilder(448);
    buf.append("FloatingRateCalculation{");
    buf.append("payReceive").append('=').append(getPayReceive()).append(',').append(' ');
    buf.append("notional").append('=').append(getNotional()).append(',').append(' ');
    buf.append("dayCount").append('=').append(getDayCount()).append(',').append(' ');
    buf.append("index").append('=').append(getIndex()).append(',').append(' ');
    buf.append("fixingRelativeTo").append('=').append(getFixingRelativeTo()).append(',').append(' ');
    buf.append("fixingOffset").append('=').append(getFixingOffset()).append(',').append(' ');
    buf.append("negativeRateMethod").append('=').append(getNegativeRateMethod()).append(',').append(' ');
    buf.append("resetPeriods").append('=').append(getResetPeriods()).append(',').append(' ');
    buf.append("firstRegularRate").append('=').append(getFirstRegularRate()).append(',').append(' ');
    buf.append("initialStub").append('=').append(getInitialStub()).append(',').append(' ');
    buf.append("finalStub").append('=').append(getFinalStub()).append(',').append(' ');
    buf.append("gearing").append('=').append(getGearing()).append(',').append(' ');
    buf.append("spread").append('=').append(JodaBeanUtils.toString(getSpread()));
    buf.append('}');
    return buf.toString();
  }

  //-----------------------------------------------------------------------
  /**
   * The meta-bean for {@code FloatingRateCalculation}.
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
        this, "payReceive", FloatingRateCalculation.class, PayReceive.class);
    /**
     * The meta-property for the {@code notional} property.
     */
    private final MetaProperty<NotionalAmount> notional = DirectMetaProperty.ofImmutable(
        this, "notional", FloatingRateCalculation.class, NotionalAmount.class);
    /**
     * The meta-property for the {@code dayCount} property.
     */
    private final MetaProperty<DayCount> dayCount = DirectMetaProperty.ofImmutable(
        this, "dayCount", FloatingRateCalculation.class, DayCount.class);
    /**
     * The meta-property for the {@code index} property.
     */
    private final MetaProperty<RateIndex> index = DirectMetaProperty.ofImmutable(
        this, "index", FloatingRateCalculation.class, RateIndex.class);
    /**
     * The meta-property for the {@code fixingRelativeTo} property.
     */
    private final MetaProperty<FixingRelativeTo> fixingRelativeTo = DirectMetaProperty.ofImmutable(
        this, "fixingRelativeTo", FloatingRateCalculation.class, FixingRelativeTo.class);
    /**
     * The meta-property for the {@code fixingOffset} property.
     */
    private final MetaProperty<DaysAdjustment> fixingOffset = DirectMetaProperty.ofImmutable(
        this, "fixingOffset", FloatingRateCalculation.class, DaysAdjustment.class);
    /**
     * The meta-property for the {@code negativeRateMethod} property.
     */
    private final MetaProperty<NegativeRateMethod> negativeRateMethod = DirectMetaProperty.ofImmutable(
        this, "negativeRateMethod", FloatingRateCalculation.class, NegativeRateMethod.class);
    /**
     * The meta-property for the {@code resetPeriods} property.
     */
    private final MetaProperty<ResetSchedule> resetPeriods = DirectMetaProperty.ofImmutable(
        this, "resetPeriods", FloatingRateCalculation.class, ResetSchedule.class);
    /**
     * The meta-property for the {@code firstRegularRate} property.
     */
    private final MetaProperty<Double> firstRegularRate = DirectMetaProperty.ofImmutable(
        this, "firstRegularRate", FloatingRateCalculation.class, Double.class);
    /**
     * The meta-property for the {@code initialStub} property.
     */
    private final MetaProperty<FloatingRateStub> initialStub = DirectMetaProperty.ofImmutable(
        this, "initialStub", FloatingRateCalculation.class, FloatingRateStub.class);
    /**
     * The meta-property for the {@code finalStub} property.
     */
    private final MetaProperty<FloatingRateStub> finalStub = DirectMetaProperty.ofImmutable(
        this, "finalStub", FloatingRateCalculation.class, FloatingRateStub.class);
    /**
     * The meta-property for the {@code gearing} property.
     */
    private final MetaProperty<ValueSchedule> gearing = DirectMetaProperty.ofImmutable(
        this, "gearing", FloatingRateCalculation.class, ValueSchedule.class);
    /**
     * The meta-property for the {@code spread} property.
     */
    private final MetaProperty<ValueSchedule> spread = DirectMetaProperty.ofImmutable(
        this, "spread", FloatingRateCalculation.class, ValueSchedule.class);
    /**
     * The meta-properties.
     */
    private final Map<String, MetaProperty<?>> metaPropertyMap$ = new DirectMetaPropertyMap(
        this, null,
        "payReceive",
        "notional",
        "dayCount",
        "index",
        "fixingRelativeTo",
        "fixingOffset",
        "negativeRateMethod",
        "resetPeriods",
        "firstRegularRate",
        "initialStub",
        "finalStub",
        "gearing",
        "spread");

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
        case 1585636160:  // notional
          return notional;
        case 1905311443:  // dayCount
          return dayCount;
        case 100346066:  // index
          return index;
        case 232554996:  // fixingRelativeTo
          return fixingRelativeTo;
        case -317508960:  // fixingOffset
          return fixingOffset;
        case 1969081334:  // negativeRateMethod
          return negativeRateMethod;
        case -1272973693:  // resetPeriods
          return resetPeriods;
        case 570227148:  // firstRegularRate
          return firstRegularRate;
        case 1233359378:  // initialStub
          return initialStub;
        case 355242820:  // finalStub
          return finalStub;
        case -91774989:  // gearing
          return gearing;
        case -895684237:  // spread
          return spread;
      }
      return super.metaPropertyGet(propertyName);
    }

    @Override
    public FloatingRateCalculation.Builder builder() {
      return new FloatingRateCalculation.Builder();
    }

    @Override
    public Class<? extends FloatingRateCalculation> beanType() {
      return FloatingRateCalculation.class;
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
     * The meta-property for the {@code notional} property.
     * @return the meta-property, not null
     */
    public MetaProperty<NotionalAmount> notional() {
      return notional;
    }

    /**
     * The meta-property for the {@code dayCount} property.
     * @return the meta-property, not null
     */
    public MetaProperty<DayCount> dayCount() {
      return dayCount;
    }

    /**
     * The meta-property for the {@code index} property.
     * @return the meta-property, not null
     */
    public MetaProperty<RateIndex> index() {
      return index;
    }

    /**
     * The meta-property for the {@code fixingRelativeTo} property.
     * @return the meta-property, not null
     */
    public MetaProperty<FixingRelativeTo> fixingRelativeTo() {
      return fixingRelativeTo;
    }

    /**
     * The meta-property for the {@code fixingOffset} property.
     * @return the meta-property, not null
     */
    public MetaProperty<DaysAdjustment> fixingOffset() {
      return fixingOffset;
    }

    /**
     * The meta-property for the {@code negativeRateMethod} property.
     * @return the meta-property, not null
     */
    public MetaProperty<NegativeRateMethod> negativeRateMethod() {
      return negativeRateMethod;
    }

    /**
     * The meta-property for the {@code resetPeriods} property.
     * @return the meta-property, not null
     */
    public MetaProperty<ResetSchedule> resetPeriods() {
      return resetPeriods;
    }

    /**
     * The meta-property for the {@code firstRegularRate} property.
     * @return the meta-property, not null
     */
    public MetaProperty<Double> firstRegularRate() {
      return firstRegularRate;
    }

    /**
     * The meta-property for the {@code initialStub} property.
     * @return the meta-property, not null
     */
    public MetaProperty<FloatingRateStub> initialStub() {
      return initialStub;
    }

    /**
     * The meta-property for the {@code finalStub} property.
     * @return the meta-property, not null
     */
    public MetaProperty<FloatingRateStub> finalStub() {
      return finalStub;
    }

    /**
     * The meta-property for the {@code gearing} property.
     * @return the meta-property, not null
     */
    public MetaProperty<ValueSchedule> gearing() {
      return gearing;
    }

    /**
     * The meta-property for the {@code spread} property.
     * @return the meta-property, not null
     */
    public MetaProperty<ValueSchedule> spread() {
      return spread;
    }

    //-----------------------------------------------------------------------
    @Override
    protected Object propertyGet(Bean bean, String propertyName, boolean quiet) {
      switch (propertyName.hashCode()) {
        case -885469925:  // payReceive
          return ((FloatingRateCalculation) bean).getPayReceive();
        case 1585636160:  // notional
          return ((FloatingRateCalculation) bean).getNotional();
        case 1905311443:  // dayCount
          return ((FloatingRateCalculation) bean).getDayCount();
        case 100346066:  // index
          return ((FloatingRateCalculation) bean).getIndex();
        case 232554996:  // fixingRelativeTo
          return ((FloatingRateCalculation) bean).getFixingRelativeTo();
        case -317508960:  // fixingOffset
          return ((FloatingRateCalculation) bean).getFixingOffset();
        case 1969081334:  // negativeRateMethod
          return ((FloatingRateCalculation) bean).getNegativeRateMethod();
        case -1272973693:  // resetPeriods
          return ((FloatingRateCalculation) bean).getResetPeriods();
        case 570227148:  // firstRegularRate
          return ((FloatingRateCalculation) bean).getFirstRegularRate();
        case 1233359378:  // initialStub
          return ((FloatingRateCalculation) bean).getInitialStub();
        case 355242820:  // finalStub
          return ((FloatingRateCalculation) bean).getFinalStub();
        case -91774989:  // gearing
          return ((FloatingRateCalculation) bean).getGearing();
        case -895684237:  // spread
          return ((FloatingRateCalculation) bean).getSpread();
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
   * The bean-builder for {@code FloatingRateCalculation}.
   */
  public static final class Builder extends DirectFieldsBeanBuilder<FloatingRateCalculation> {

    private PayReceive payReceive;
    private NotionalAmount notional;
    private DayCount dayCount;
    private RateIndex index;
    private FixingRelativeTo fixingRelativeTo;
    private DaysAdjustment fixingOffset;
    private NegativeRateMethod negativeRateMethod;
    private ResetSchedule resetPeriods;
    private Double firstRegularRate;
    private FloatingRateStub initialStub;
    private FloatingRateStub finalStub;
    private ValueSchedule gearing;
    private ValueSchedule spread;

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
    private Builder(FloatingRateCalculation beanToCopy) {
      this.payReceive = beanToCopy.getPayReceive();
      this.notional = beanToCopy.getNotional();
      this.dayCount = beanToCopy.getDayCount();
      this.index = beanToCopy.getIndex();
      this.fixingRelativeTo = beanToCopy.getFixingRelativeTo();
      this.fixingOffset = beanToCopy.getFixingOffset();
      this.negativeRateMethod = beanToCopy.getNegativeRateMethod();
      this.resetPeriods = beanToCopy.getResetPeriods();
      this.firstRegularRate = beanToCopy.getFirstRegularRate();
      this.initialStub = beanToCopy.getInitialStub();
      this.finalStub = beanToCopy.getFinalStub();
      this.gearing = beanToCopy.getGearing();
      this.spread = beanToCopy.getSpread();
    }

    //-----------------------------------------------------------------------
    @Override
    public Object get(String propertyName) {
      switch (propertyName.hashCode()) {
        case -885469925:  // payReceive
          return payReceive;
        case 1585636160:  // notional
          return notional;
        case 1905311443:  // dayCount
          return dayCount;
        case 100346066:  // index
          return index;
        case 232554996:  // fixingRelativeTo
          return fixingRelativeTo;
        case -317508960:  // fixingOffset
          return fixingOffset;
        case 1969081334:  // negativeRateMethod
          return negativeRateMethod;
        case -1272973693:  // resetPeriods
          return resetPeriods;
        case 570227148:  // firstRegularRate
          return firstRegularRate;
        case 1233359378:  // initialStub
          return initialStub;
        case 355242820:  // finalStub
          return finalStub;
        case -91774989:  // gearing
          return gearing;
        case -895684237:  // spread
          return spread;
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
        case 1585636160:  // notional
          this.notional = (NotionalAmount) newValue;
          break;
        case 1905311443:  // dayCount
          this.dayCount = (DayCount) newValue;
          break;
        case 100346066:  // index
          this.index = (RateIndex) newValue;
          break;
        case 232554996:  // fixingRelativeTo
          this.fixingRelativeTo = (FixingRelativeTo) newValue;
          break;
        case -317508960:  // fixingOffset
          this.fixingOffset = (DaysAdjustment) newValue;
          break;
        case 1969081334:  // negativeRateMethod
          this.negativeRateMethod = (NegativeRateMethod) newValue;
          break;
        case -1272973693:  // resetPeriods
          this.resetPeriods = (ResetSchedule) newValue;
          break;
        case 570227148:  // firstRegularRate
          this.firstRegularRate = (Double) newValue;
          break;
        case 1233359378:  // initialStub
          this.initialStub = (FloatingRateStub) newValue;
          break;
        case 355242820:  // finalStub
          this.finalStub = (FloatingRateStub) newValue;
          break;
        case -91774989:  // gearing
          this.gearing = (ValueSchedule) newValue;
          break;
        case -895684237:  // spread
          this.spread = (ValueSchedule) newValue;
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
    public FloatingRateCalculation build() {
      return new FloatingRateCalculation(
          payReceive,
          notional,
          dayCount,
          index,
          fixingRelativeTo,
          fixingOffset,
          negativeRateMethod,
          resetPeriods,
          firstRegularRate,
          initialStub,
          finalStub,
          gearing,
          spread);
    }

    //-----------------------------------------------------------------------
    /**
     * Sets the {@code payReceive} property in the builder.
     * @param payReceive  the new value, not null
     * @return this, for chaining, not null
     */
    public Builder payReceive(PayReceive payReceive) {
      JodaBeanUtils.notNull(payReceive, "payReceive");
      this.payReceive = payReceive;
      return this;
    }

    /**
     * Sets the {@code notional} property in the builder.
     * @param notional  the new value, not null
     * @return this, for chaining, not null
     */
    public Builder notional(NotionalAmount notional) {
      JodaBeanUtils.notNull(notional, "notional");
      this.notional = notional;
      return this;
    }

    /**
     * Sets the {@code dayCount} property in the builder.
     * @param dayCount  the new value, not null
     * @return this, for chaining, not null
     */
    public Builder dayCount(DayCount dayCount) {
      JodaBeanUtils.notNull(dayCount, "dayCount");
      this.dayCount = dayCount;
      return this;
    }

    /**
     * Sets the {@code index} property in the builder.
     * @param index  the new value, not null
     * @return this, for chaining, not null
     */
    public Builder index(RateIndex index) {
      JodaBeanUtils.notNull(index, "index");
      this.index = index;
      return this;
    }

    /**
     * Sets the {@code fixingRelativeTo} property in the builder.
     * @param fixingRelativeTo  the new value, not null
     * @return this, for chaining, not null
     */
    public Builder fixingRelativeTo(FixingRelativeTo fixingRelativeTo) {
      JodaBeanUtils.notNull(fixingRelativeTo, "fixingRelativeTo");
      this.fixingRelativeTo = fixingRelativeTo;
      return this;
    }

    /**
     * Sets the {@code fixingOffset} property in the builder.
     * @param fixingOffset  the new value, not null
     * @return this, for chaining, not null
     */
    public Builder fixingOffset(DaysAdjustment fixingOffset) {
      JodaBeanUtils.notNull(fixingOffset, "fixingOffset");
      this.fixingOffset = fixingOffset;
      return this;
    }

    /**
     * Sets the {@code negativeRateMethod} property in the builder.
     * @param negativeRateMethod  the new value, not null
     * @return this, for chaining, not null
     */
    public Builder negativeRateMethod(NegativeRateMethod negativeRateMethod) {
      JodaBeanUtils.notNull(negativeRateMethod, "negativeRateMethod");
      this.negativeRateMethod = negativeRateMethod;
      return this;
    }

    /**
     * Sets the {@code resetPeriods} property in the builder.
     * @param resetPeriods  the new value
     * @return this, for chaining, not null
     */
    public Builder resetPeriods(ResetSchedule resetPeriods) {
      this.resetPeriods = resetPeriods;
      return this;
    }

    /**
     * Sets the {@code firstRegularRate} property in the builder.
     * @param firstRegularRate  the new value
     * @return this, for chaining, not null
     */
    public Builder firstRegularRate(Double firstRegularRate) {
      this.firstRegularRate = firstRegularRate;
      return this;
    }

    /**
     * Sets the {@code initialStub} property in the builder.
     * @param initialStub  the new value
     * @return this, for chaining, not null
     */
    public Builder initialStub(FloatingRateStub initialStub) {
      this.initialStub = initialStub;
      return this;
    }

    /**
     * Sets the {@code finalStub} property in the builder.
     * @param finalStub  the new value
     * @return this, for chaining, not null
     */
    public Builder finalStub(FloatingRateStub finalStub) {
      this.finalStub = finalStub;
      return this;
    }

    /**
     * Sets the {@code gearing} property in the builder.
     * @param gearing  the new value
     * @return this, for chaining, not null
     */
    public Builder gearing(ValueSchedule gearing) {
      this.gearing = gearing;
      return this;
    }

    /**
     * Sets the {@code spread} property in the builder.
     * @param spread  the new value
     * @return this, for chaining, not null
     */
    public Builder spread(ValueSchedule spread) {
      this.spread = spread;
      return this;
    }

    //-----------------------------------------------------------------------
    @Override
    public String toString() {
      StringBuilder buf = new StringBuilder(448);
      buf.append("FloatingRateCalculation.Builder{");
      buf.append("payReceive").append('=').append(JodaBeanUtils.toString(payReceive)).append(',').append(' ');
      buf.append("notional").append('=').append(JodaBeanUtils.toString(notional)).append(',').append(' ');
      buf.append("dayCount").append('=').append(JodaBeanUtils.toString(dayCount)).append(',').append(' ');
      buf.append("index").append('=').append(JodaBeanUtils.toString(index)).append(',').append(' ');
      buf.append("fixingRelativeTo").append('=').append(JodaBeanUtils.toString(fixingRelativeTo)).append(',').append(' ');
      buf.append("fixingOffset").append('=').append(JodaBeanUtils.toString(fixingOffset)).append(',').append(' ');
      buf.append("negativeRateMethod").append('=').append(JodaBeanUtils.toString(negativeRateMethod)).append(',').append(' ');
      buf.append("resetPeriods").append('=').append(JodaBeanUtils.toString(resetPeriods)).append(',').append(' ');
      buf.append("firstRegularRate").append('=').append(JodaBeanUtils.toString(firstRegularRate)).append(',').append(' ');
      buf.append("initialStub").append('=').append(JodaBeanUtils.toString(initialStub)).append(',').append(' ');
      buf.append("finalStub").append('=').append(JodaBeanUtils.toString(finalStub)).append(',').append(' ');
      buf.append("gearing").append('=').append(JodaBeanUtils.toString(gearing)).append(',').append(' ');
      buf.append("spread").append('=').append(JodaBeanUtils.toString(spread));
      buf.append('}');
      return buf.toString();
    }

  }

  ///CLOVER:ON
  //-------------------------- AUTOGENERATED END --------------------------
}
