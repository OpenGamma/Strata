/*
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.product.swap;

import static com.google.common.base.MoreObjects.firstNonNull;
import static com.opengamma.strata.basics.value.ValueSchedule.ALWAYS_0;
import static com.opengamma.strata.basics.value.ValueSchedule.ALWAYS_1;
import static com.opengamma.strata.product.swap.IborRateResetMethod.UNWEIGHTED;

import java.io.Serializable;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.OptionalDouble;
import java.util.function.Function;

import org.joda.beans.Bean;
import org.joda.beans.ImmutableBean;
import org.joda.beans.JodaBeanUtils;
import org.joda.beans.MetaBean;
import org.joda.beans.MetaProperty;
import org.joda.beans.gen.BeanDefinition;
import org.joda.beans.gen.ImmutableDefaults;
import org.joda.beans.gen.ImmutablePreBuild;
import org.joda.beans.gen.PropertyDefinition;
import org.joda.beans.impl.direct.DirectFieldsBeanBuilder;
import org.joda.beans.impl.direct.DirectMetaBean;
import org.joda.beans.impl.direct.DirectMetaProperty;
import org.joda.beans.impl.direct.DirectMetaPropertyMap;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.opengamma.strata.basics.ReferenceData;
import com.opengamma.strata.basics.currency.Currency;
import com.opengamma.strata.basics.date.DateAdjuster;
import com.opengamma.strata.basics.date.DayCount;
import com.opengamma.strata.basics.date.DaysAdjustment;
import com.opengamma.strata.basics.index.IborIndex;
import com.opengamma.strata.basics.index.IborIndexObservation;
import com.opengamma.strata.basics.index.Index;
import com.opengamma.strata.basics.schedule.RollConventions;
import com.opengamma.strata.basics.schedule.Schedule;
import com.opengamma.strata.basics.schedule.SchedulePeriod;
import com.opengamma.strata.basics.value.ValueSchedule;
import com.opengamma.strata.collect.array.DoubleArray;
import com.opengamma.strata.collect.tuple.Pair;
import com.opengamma.strata.product.rate.FixedRateComputation;
import com.opengamma.strata.product.rate.IborAveragedFixing;
import com.opengamma.strata.product.rate.IborAveragedRateComputation;
import com.opengamma.strata.product.rate.IborRateComputation;
import com.opengamma.strata.product.rate.RateComputation;

/**
 * Defines the calculation of a floating rate swap leg based on an Ibor index.
 * <p>
 * This defines the data necessary to calculate the amount payable on the leg.
 * The amount is based on the observed value of an Ibor index such as 'GBP-LIBOR-3M' or 'EUR-EURIBOR-1M'.
 * <p>
 * The index is observed once for each <i>reset period</i> and referred to as a <i>fixing</i>.
 * The actual date of observation is the <i>fixing date</i>, which is relative to either
 * the start or end of the reset period.
 * <p>
 * The reset period is typically the same as the accrual period.
 * In this case, the rate for the accrual period is based directly on the fixing.
 * If the reset period is a subdivision of the accrual period then there are multiple fixings,
 * one for each reset period.
 * In that case, the rate for the accrual period is based on an average of the fixings.
 */
@BeanDefinition
public final class IborRateCalculation
    implements RateCalculation, ImmutableBean, Serializable {

  /**
   * The day count convention.
   * <p>
   * This is used to convert dates to a numerical value.
   * <p>
   * When building, this will default to the day count of the index if not specified.
   */
  @PropertyDefinition(validate = "notNull", overrideGet = true)
  private final DayCount dayCount;
  /**
   * The Ibor index.
   * <p>
   * The rate to be paid is based on this index
   * It will be a well known market index such as 'GBP-LIBOR-3M'.
   */
  @PropertyDefinition(validate = "notNull")
  private final IborIndex index;
  /**
   * The reset schedule, used when averaging rates, optional.
   * <p>
   * Most swaps have a single fixing for each accrual period.
   * This property allows multiple fixings to be defined by dividing the accrual periods into reset periods.
   * <p>
   * If this property is not present, then the reset period is the same as the accrual period.
   * If this property is present, then the accrual period is divided as per the information
   * in the reset schedule, multiple fixing dates are calculated, and rate averaging performed.
   */
  @PropertyDefinition(get = "optional")
  private final ResetSchedule resetPeriods;
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
   * <p>
   * When building, this will default to the fixing offset of the index if not specified.
   */
  @PropertyDefinition(validate = "notNull")
  private final DaysAdjustment fixingDateOffset;
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
   * The rate of the first regular reset period, optional.
   * A 5% rate will be expressed as 0.05.
   * <p>
   * In certain circumstances two counterparties agree the rate of the first fixing
   * when the contract starts, and it is used in place of one observed fixing.
   * For all other fixings, the rate is observed via the normal fixing process.
   * <p>
   * This property allows the rate of the first reset period of the first <i>regular</i> accrual period
   * to be controlled. Note that if there is an initial stub, this will be the second reset period.
   * Other calculation elements, such as gearing or spread, still apply to the rate specified here.
   * <p>
   * If the first rate applies to the initial stub rather than the regular accrual periods
   * it must be specified using {@code initialStub}. Alternatively, {@code firstRate} can be used.
   * <p>
   * This property follows the definition in FpML. See also {@code firstRate}.
   */
  @PropertyDefinition(get = "optional")
  private final Double firstRegularRate;
  /**
   * The rate of the first reset period, which may be a stub, optional.
   * A 5% rate will be expressed as 0.05.
   * <p>
   * In certain circumstances two counterparties agree the rate of the first fixing
   * when the contract starts, and it is used in place of one observed fixing.
   * For all other fixings, the rate is observed via the normal fixing process.
   * <p>
   * This property allows the rate of the first reset period to be controlled,
   * irrespective of whether that is an initial stub or a regular period.
   * Other calculation elements, such as gearing or spread, still apply to the rate specified here.
   * <p>
   * This property is similar to {@code firstRegularRate}.
   * This property operates on the first reset period, whether that is an initial stub or a regular period.
   * By contrast, {@code firstRegularRate} operates on the first regular period, and never on a stub.
   * <p>
   * If either {@code firstRegularRate} or {@code initialStub} are present, this property is ignored.
   * <p>
   * If this property is not present, then the first rate is observed via the normal fixing process.
   */
  @PropertyDefinition(get = "optional")
  private final Double firstRate;
  /**
   * The offset of the first fixing date from the first adjusted reset date, optional.
   * <p>
   * If present, this offset is used instead of {@code fixingDateOffset} for the first
   * reset period of the swap, which will be either an initial stub or the first reset
   * period of the first <i>regular</i> accrual period.
   * <p>
   * The offset is applied to the base date specified by {@code fixingRelativeTo}.
   * The offset is typically a negative number of business days.
   * <p>
   * If this property is not present, then the {@code fixingDateOffset} applies to all fixings.
   */
  @PropertyDefinition(get = "optional")
  private final DaysAdjustment firstFixingDateOffset;
  /**
   * The rate to be used in initial stub, optional.
   * <p>
   * The initial stub of a swap may have different rate rules to the regular accrual periods.
   * A fixed rate may be specified, a different floating rate or a linearly interpolated floating rate.
   * This may not be present if there is no initial stub, or if the index during the stub is the same
   * as the main floating rate index.
   * <p>
   * If this property is not present, then the main index applies during any initial stub.
   * If this property is present and there is no initial stub, it is ignored.
   */
  @PropertyDefinition(get = "optional")
  private final IborRateStubCalculation initialStub;
  /**
   * The rate to be used in final stub, optional.
   * <p>
   * The final stub of a swap may have different rate rules to the regular accrual periods.
   * A fixed rate may be specified, a different floating rate or a linearly interpolated floating rate.
   * This may not be present if there is no final stub, or if the index during the stub is the same
   * as the main floating rate index.
   * <p>
   * If this property is not present, then the main index applies during any final stub.
   * If this property is present and there is no final stub, it is ignored.
   */
  @PropertyDefinition(get = "optional")
  private final IborRateStubCalculation finalStub;
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
   * If this property is not present, then no gearing applies.
   * <p>
   * Gearing is also known as <i>leverage</i>.
   */
  @PropertyDefinition(get = "optional")
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
   * If this property is not present, then no spread applies.
   * <p>
   * Defined by the 2006 ISDA definitions article 6.2e.
   */
  @PropertyDefinition(get = "optional")
  private final ValueSchedule spread;

  //-------------------------------------------------------------------------
  /**
   * Obtains a rate calculation for the specified index.
   * <p>
   * The calculation will use the day count and fixing offset of the index.
   * All optional fields will be set to their default values.
   * Thus, fixing will be in advance, with no spread, gearing or reset periods.
   * If this method provides insufficient control, use the {@linkplain #builder() builder}.
   * 
   * @param index  the index
   * @return the calculation
   */
  public static IborRateCalculation of(IborIndex index) {
    return IborRateCalculation.builder().index(index).build();
  }

  //-------------------------------------------------------------------------
  @ImmutableDefaults
  private static void applyDefaults(Builder builder) {
    builder.fixingRelativeTo(FixingRelativeTo.PERIOD_START);
    builder.negativeRateMethod(NegativeRateMethod.ALLOW_NEGATIVE);
  }

  @ImmutablePreBuild
  private static void preBuild(Builder builder) {
    if (builder.index != null) {
      if (builder.dayCount == null) {
        builder.dayCount = builder.index.getDayCount();
      }
      if (builder.fixingDateOffset == null) {
        builder.fixingDateOffset = builder.index.getFixingDateOffset();
      }
    }
  }

  //-------------------------------------------------------------------------
  @Override
  public SwapLegType getType() {
    return SwapLegType.IBOR;
  }

  @Override
  public void collectCurrencies(ImmutableSet.Builder<Currency> builder) {
    builder.add(index.getCurrency());
    getInitialStub().ifPresent(stub -> stub.collectCurrencies(builder));
    getFinalStub().ifPresent(stub -> stub.collectCurrencies(builder));
  }

  @Override
  public void collectIndices(ImmutableSet.Builder<Index> builder) {
    builder.add(index);
    getInitialStub().ifPresent(stub -> stub.collectIndices(builder));
    getFinalStub().ifPresent(stub -> stub.collectIndices(builder));
  }

  @Override
  public ImmutableList<RateAccrualPeriod> createAccrualPeriods(
      Schedule accrualSchedule,
      Schedule paymentSchedule,
      ReferenceData refData) {

    // resolve data by schedule
    DoubleArray resolvedGearings = firstNonNull(gearing, ALWAYS_1).resolveValues(accrualSchedule);
    DoubleArray resolvedSpreads = firstNonNull(spread, ALWAYS_0).resolveValues(accrualSchedule);
    // resolve against reference data once
    DateAdjuster fixingDateAdjuster = fixingDateOffset.resolve(refData);
    Function<SchedulePeriod, Schedule> resetScheduleFn =
        getResetPeriods().map(rp ->
            accrualSchedule.getFrequency().isMonthBased() && rp.getResetFrequency().isWeekBased() ?
                rp.createSchedule(RollConventions.NONE, refData, true) :
                rp.createSchedule(accrualSchedule.getRollConvention(), refData, false))
            .orElse(null);
    Function<LocalDate, IborIndexObservation> iborObservationFn = index.resolve(refData);

    // need to use getStubs(boolean) and not getInitialStub()/getFinalStub() to ensure correct stub allocation
    Pair<Optional<SchedulePeriod>, Optional<SchedulePeriod>> scheduleStubs =
        accrualSchedule.getStubs(initialStub == null && finalStub != null);
    Optional<SchedulePeriod> scheduleInitialStub = scheduleStubs.getFirst();
    Optional<SchedulePeriod> scheduleFinalStub = scheduleStubs.getSecond();

    // build accrual periods
    ImmutableList.Builder<RateAccrualPeriod> accrualPeriods = ImmutableList.builder();
    for (int i = 0; i < accrualSchedule.size(); i++) {
      SchedulePeriod period = accrualSchedule.getPeriod(i);
      RateComputation rateComputation = createRateComputation(
          period, fixingDateAdjuster, resetScheduleFn, iborObservationFn, i, scheduleInitialStub, scheduleFinalStub, refData);
      double yearFraction = period.yearFraction(dayCount, accrualSchedule);
      accrualPeriods.add(new RateAccrualPeriod(
          period, yearFraction, rateComputation, resolvedGearings.get(i), resolvedSpreads.get(i), negativeRateMethod));
    }
    return accrualPeriods.build();
  }

  // creates the rate computation
  private RateComputation createRateComputation(
      SchedulePeriod period,
      DateAdjuster fixingDateAdjuster,
      Function<SchedulePeriod, Schedule> resetScheduleFn,
      Function<LocalDate, IborIndexObservation> iborObservationFn,
      int scheduleIndex,
      Optional<SchedulePeriod> scheduleInitialStub,
      Optional<SchedulePeriod> scheduleFinalStub,
      ReferenceData refData) {

    LocalDate fixingDate = fixingDateAdjuster.adjust(fixingRelativeTo.selectBaseDate(period));
    if (scheduleIndex == 0 && firstFixingDateOffset != null) {
      fixingDate = firstFixingDateOffset.resolve(refData).adjust(fixingRelativeTo.selectBaseDate(period));
    }
    // initial stub
    if (scheduleInitialStub.isPresent() && scheduleIndex == 0) {
      if (firstRate != null &&
          firstRegularRate == null &&
          (initialStub == null || IborRateStubCalculation.NONE.equals(initialStub))) {
        return FixedRateComputation.of(firstRate);
      }
      return firstNonNull(initialStub, IborRateStubCalculation.NONE).createRateComputation(fixingDate, index, refData);
    }
    // final stub
    if (scheduleFinalStub.isPresent() && scheduleFinalStub.get() == period) {
      return firstNonNull(finalStub, IborRateStubCalculation.NONE).createRateComputation(fixingDate, index, refData);
    }
    // override rate
    Double overrideFirstRate = null;
    if (firstRegularRate != null) {
      if (isFirstRegularPeriod(scheduleIndex, scheduleInitialStub.isPresent())) {
        overrideFirstRate = firstRegularRate;
      }
    } else if (firstRate != null && scheduleIndex == 0) {
      overrideFirstRate = firstRate;
    }
    // handle explicit reset periods, possible averaging
    if (resetScheduleFn != null) {
      return createRateComputationWithResetPeriods(
          resetScheduleFn.apply(period),
          fixingDateAdjuster,
          iborObservationFn,
          scheduleIndex,
          overrideFirstRate,
          refData);
    }
    // handle possible fixed rate
    if (overrideFirstRate != null) {
      return FixedRateComputation.of(overrideFirstRate);
    }
    // simple Ibor
    return IborRateComputation.of(iborObservationFn.apply(fixingDate));
  }

  // reset periods have been specified, which may or may not imply averaging
  private RateComputation createRateComputationWithResetPeriods(
      Schedule resetSchedule,
      DateAdjuster fixingDateAdjuster,
      Function<LocalDate, IborIndexObservation> iborObservationFn,
      int scheduleIndex,
      Double overrideFirstRate,
      ReferenceData refData) {

    List<IborAveragedFixing> fixings = new ArrayList<>();
    for (int i = 0; i < resetSchedule.size(); i++) {
      SchedulePeriod resetPeriod = resetSchedule.getPeriod(i);
      LocalDate fixingDate = fixingDateAdjuster.adjust(fixingRelativeTo.selectBaseDate(resetPeriod));
      if (scheduleIndex == 0 && i == 0 && firstFixingDateOffset != null) {
        fixingDate = firstFixingDateOffset.resolve(refData).adjust(fixingRelativeTo.selectBaseDate(resetPeriod));
      }
      fixings.add(IborAveragedFixing.builder()
          .observation(iborObservationFn.apply(fixingDate))
          .fixedRate(overrideFirstRate != null && i == 0 ? overrideFirstRate : null)
          .weight(resetPeriods.getResetMethod() == UNWEIGHTED ? 1 : resetPeriod.lengthInDays())
          .build());
    }
    return IborAveragedRateComputation.of(fixings);
  }

  // is the period the first regular period
  private boolean isFirstRegularPeriod(int scheduleIndex, boolean hasInitialStub) {
    if (hasInitialStub) {
      return scheduleIndex == 1;
    } else {
      return scheduleIndex == 0;
    }
  }

  //------------------------- AUTOGENERATED START -------------------------
  /**
   * The meta-bean for {@code IborRateCalculation}.
   * @return the meta-bean, not null
   */
  public static IborRateCalculation.Meta meta() {
    return IborRateCalculation.Meta.INSTANCE;
  }

  static {
    MetaBean.register(IborRateCalculation.Meta.INSTANCE);
  }

  /**
   * The serialization version id.
   */
  private static final long serialVersionUID = 1L;

  /**
   * Returns a builder used to create an instance of the bean.
   * @return the builder, not null
   */
  public static IborRateCalculation.Builder builder() {
    return new IborRateCalculation.Builder();
  }

  private IborRateCalculation(
      DayCount dayCount,
      IborIndex index,
      ResetSchedule resetPeriods,
      FixingRelativeTo fixingRelativeTo,
      DaysAdjustment fixingDateOffset,
      NegativeRateMethod negativeRateMethod,
      Double firstRegularRate,
      Double firstRate,
      DaysAdjustment firstFixingDateOffset,
      IborRateStubCalculation initialStub,
      IborRateStubCalculation finalStub,
      ValueSchedule gearing,
      ValueSchedule spread) {
    JodaBeanUtils.notNull(dayCount, "dayCount");
    JodaBeanUtils.notNull(index, "index");
    JodaBeanUtils.notNull(fixingRelativeTo, "fixingRelativeTo");
    JodaBeanUtils.notNull(fixingDateOffset, "fixingDateOffset");
    JodaBeanUtils.notNull(negativeRateMethod, "negativeRateMethod");
    this.dayCount = dayCount;
    this.index = index;
    this.resetPeriods = resetPeriods;
    this.fixingRelativeTo = fixingRelativeTo;
    this.fixingDateOffset = fixingDateOffset;
    this.negativeRateMethod = negativeRateMethod;
    this.firstRegularRate = firstRegularRate;
    this.firstRate = firstRate;
    this.firstFixingDateOffset = firstFixingDateOffset;
    this.initialStub = initialStub;
    this.finalStub = finalStub;
    this.gearing = gearing;
    this.spread = spread;
  }

  @Override
  public IborRateCalculation.Meta metaBean() {
    return IborRateCalculation.Meta.INSTANCE;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the day count convention.
   * <p>
   * This is used to convert dates to a numerical value.
   * <p>
   * When building, this will default to the day count of the index if not specified.
   * @return the value of the property, not null
   */
  @Override
  public DayCount getDayCount() {
    return dayCount;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the Ibor index.
   * <p>
   * The rate to be paid is based on this index
   * It will be a well known market index such as 'GBP-LIBOR-3M'.
   * @return the value of the property, not null
   */
  public IborIndex getIndex() {
    return index;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the reset schedule, used when averaging rates, optional.
   * <p>
   * Most swaps have a single fixing for each accrual period.
   * This property allows multiple fixings to be defined by dividing the accrual periods into reset periods.
   * <p>
   * If this property is not present, then the reset period is the same as the accrual period.
   * If this property is present, then the accrual period is divided as per the information
   * in the reset schedule, multiple fixing dates are calculated, and rate averaging performed.
   * @return the optional value of the property, not null
   */
  public Optional<ResetSchedule> getResetPeriods() {
    return Optional.ofNullable(resetPeriods);
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
   * <p>
   * When building, this will default to the fixing offset of the index if not specified.
   * @return the value of the property, not null
   */
  public DaysAdjustment getFixingDateOffset() {
    return fixingDateOffset;
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
   * Gets the rate of the first regular reset period, optional.
   * A 5% rate will be expressed as 0.05.
   * <p>
   * In certain circumstances two counterparties agree the rate of the first fixing
   * when the contract starts, and it is used in place of one observed fixing.
   * For all other fixings, the rate is observed via the normal fixing process.
   * <p>
   * This property allows the rate of the first reset period of the first <i>regular</i> accrual period
   * to be controlled. Note that if there is an initial stub, this will be the second reset period.
   * Other calculation elements, such as gearing or spread, still apply to the rate specified here.
   * <p>
   * If the first rate applies to the initial stub rather than the regular accrual periods
   * it must be specified using {@code initialStub}. Alternatively, {@code firstRate} can be used.
   * <p>
   * This property follows the definition in FpML. See also {@code firstRate}.
   * @return the optional value of the property, not null
   */
  public OptionalDouble getFirstRegularRate() {
    return firstRegularRate != null ? OptionalDouble.of(firstRegularRate) : OptionalDouble.empty();
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the rate of the first reset period, which may be a stub, optional.
   * A 5% rate will be expressed as 0.05.
   * <p>
   * In certain circumstances two counterparties agree the rate of the first fixing
   * when the contract starts, and it is used in place of one observed fixing.
   * For all other fixings, the rate is observed via the normal fixing process.
   * <p>
   * This property allows the rate of the first reset period to be controlled,
   * irrespective of whether that is an initial stub or a regular period.
   * Other calculation elements, such as gearing or spread, still apply to the rate specified here.
   * <p>
   * This property is similar to {@code firstRegularRate}.
   * This property operates on the first reset period, whether that is an initial stub or a regular period.
   * By contrast, {@code firstRegularRate} operates on the first regular period, and never on a stub.
   * <p>
   * If either {@code firstRegularRate} or {@code initialStub} are present, this property is ignored.
   * <p>
   * If this property is not present, then the first rate is observed via the normal fixing process.
   * @return the optional value of the property, not null
   */
  public OptionalDouble getFirstRate() {
    return firstRate != null ? OptionalDouble.of(firstRate) : OptionalDouble.empty();
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the offset of the first fixing date from the first adjusted reset date, optional.
   * <p>
   * If present, this offset is used instead of {@code fixingDateOffset} for the first
   * reset period of the swap, which will be either an initial stub or the first reset
   * period of the first <i>regular</i> accrual period.
   * <p>
   * The offset is applied to the base date specified by {@code fixingRelativeTo}.
   * The offset is typically a negative number of business days.
   * <p>
   * If this property is not present, then the {@code fixingDateOffset} applies to all fixings.
   * @return the optional value of the property, not null
   */
  public Optional<DaysAdjustment> getFirstFixingDateOffset() {
    return Optional.ofNullable(firstFixingDateOffset);
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the rate to be used in initial stub, optional.
   * <p>
   * The initial stub of a swap may have different rate rules to the regular accrual periods.
   * A fixed rate may be specified, a different floating rate or a linearly interpolated floating rate.
   * This may not be present if there is no initial stub, or if the index during the stub is the same
   * as the main floating rate index.
   * <p>
   * If this property is not present, then the main index applies during any initial stub.
   * If this property is present and there is no initial stub, it is ignored.
   * @return the optional value of the property, not null
   */
  public Optional<IborRateStubCalculation> getInitialStub() {
    return Optional.ofNullable(initialStub);
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the rate to be used in final stub, optional.
   * <p>
   * The final stub of a swap may have different rate rules to the regular accrual periods.
   * A fixed rate may be specified, a different floating rate or a linearly interpolated floating rate.
   * This may not be present if there is no final stub, or if the index during the stub is the same
   * as the main floating rate index.
   * <p>
   * If this property is not present, then the main index applies during any final stub.
   * If this property is present and there is no final stub, it is ignored.
   * @return the optional value of the property, not null
   */
  public Optional<IborRateStubCalculation> getFinalStub() {
    return Optional.ofNullable(finalStub);
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
   * If this property is not present, then no gearing applies.
   * <p>
   * Gearing is also known as <i>leverage</i>.
   * @return the optional value of the property, not null
   */
  public Optional<ValueSchedule> getGearing() {
    return Optional.ofNullable(gearing);
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
   * If this property is not present, then no spread applies.
   * <p>
   * Defined by the 2006 ISDA definitions article 6.2e.
   * @return the optional value of the property, not null
   */
  public Optional<ValueSchedule> getSpread() {
    return Optional.ofNullable(spread);
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
      IborRateCalculation other = (IborRateCalculation) obj;
      return JodaBeanUtils.equal(dayCount, other.dayCount) &&
          JodaBeanUtils.equal(index, other.index) &&
          JodaBeanUtils.equal(resetPeriods, other.resetPeriods) &&
          JodaBeanUtils.equal(fixingRelativeTo, other.fixingRelativeTo) &&
          JodaBeanUtils.equal(fixingDateOffset, other.fixingDateOffset) &&
          JodaBeanUtils.equal(negativeRateMethod, other.negativeRateMethod) &&
          JodaBeanUtils.equal(firstRegularRate, other.firstRegularRate) &&
          JodaBeanUtils.equal(firstRate, other.firstRate) &&
          JodaBeanUtils.equal(firstFixingDateOffset, other.firstFixingDateOffset) &&
          JodaBeanUtils.equal(initialStub, other.initialStub) &&
          JodaBeanUtils.equal(finalStub, other.finalStub) &&
          JodaBeanUtils.equal(gearing, other.gearing) &&
          JodaBeanUtils.equal(spread, other.spread);
    }
    return false;
  }

  @Override
  public int hashCode() {
    int hash = getClass().hashCode();
    hash = hash * 31 + JodaBeanUtils.hashCode(dayCount);
    hash = hash * 31 + JodaBeanUtils.hashCode(index);
    hash = hash * 31 + JodaBeanUtils.hashCode(resetPeriods);
    hash = hash * 31 + JodaBeanUtils.hashCode(fixingRelativeTo);
    hash = hash * 31 + JodaBeanUtils.hashCode(fixingDateOffset);
    hash = hash * 31 + JodaBeanUtils.hashCode(negativeRateMethod);
    hash = hash * 31 + JodaBeanUtils.hashCode(firstRegularRate);
    hash = hash * 31 + JodaBeanUtils.hashCode(firstRate);
    hash = hash * 31 + JodaBeanUtils.hashCode(firstFixingDateOffset);
    hash = hash * 31 + JodaBeanUtils.hashCode(initialStub);
    hash = hash * 31 + JodaBeanUtils.hashCode(finalStub);
    hash = hash * 31 + JodaBeanUtils.hashCode(gearing);
    hash = hash * 31 + JodaBeanUtils.hashCode(spread);
    return hash;
  }

  @Override
  public String toString() {
    StringBuilder buf = new StringBuilder(448);
    buf.append("IborRateCalculation{");
    buf.append("dayCount").append('=').append(JodaBeanUtils.toString(dayCount)).append(',').append(' ');
    buf.append("index").append('=').append(JodaBeanUtils.toString(index)).append(',').append(' ');
    buf.append("resetPeriods").append('=').append(JodaBeanUtils.toString(resetPeriods)).append(',').append(' ');
    buf.append("fixingRelativeTo").append('=').append(JodaBeanUtils.toString(fixingRelativeTo)).append(',').append(' ');
    buf.append("fixingDateOffset").append('=').append(JodaBeanUtils.toString(fixingDateOffset)).append(',').append(' ');
    buf.append("negativeRateMethod").append('=').append(JodaBeanUtils.toString(negativeRateMethod)).append(',').append(' ');
    buf.append("firstRegularRate").append('=').append(JodaBeanUtils.toString(firstRegularRate)).append(',').append(' ');
    buf.append("firstRate").append('=').append(JodaBeanUtils.toString(firstRate)).append(',').append(' ');
    buf.append("firstFixingDateOffset").append('=').append(JodaBeanUtils.toString(firstFixingDateOffset)).append(',').append(' ');
    buf.append("initialStub").append('=').append(JodaBeanUtils.toString(initialStub)).append(',').append(' ');
    buf.append("finalStub").append('=').append(JodaBeanUtils.toString(finalStub)).append(',').append(' ');
    buf.append("gearing").append('=').append(JodaBeanUtils.toString(gearing)).append(',').append(' ');
    buf.append("spread").append('=').append(JodaBeanUtils.toString(spread));
    buf.append('}');
    return buf.toString();
  }

  //-----------------------------------------------------------------------
  /**
   * The meta-bean for {@code IborRateCalculation}.
   */
  public static final class Meta extends DirectMetaBean {
    /**
     * The singleton instance of the meta-bean.
     */
    static final Meta INSTANCE = new Meta();

    /**
     * The meta-property for the {@code dayCount} property.
     */
    private final MetaProperty<DayCount> dayCount = DirectMetaProperty.ofImmutable(
        this, "dayCount", IborRateCalculation.class, DayCount.class);
    /**
     * The meta-property for the {@code index} property.
     */
    private final MetaProperty<IborIndex> index = DirectMetaProperty.ofImmutable(
        this, "index", IborRateCalculation.class, IborIndex.class);
    /**
     * The meta-property for the {@code resetPeriods} property.
     */
    private final MetaProperty<ResetSchedule> resetPeriods = DirectMetaProperty.ofImmutable(
        this, "resetPeriods", IborRateCalculation.class, ResetSchedule.class);
    /**
     * The meta-property for the {@code fixingRelativeTo} property.
     */
    private final MetaProperty<FixingRelativeTo> fixingRelativeTo = DirectMetaProperty.ofImmutable(
        this, "fixingRelativeTo", IborRateCalculation.class, FixingRelativeTo.class);
    /**
     * The meta-property for the {@code fixingDateOffset} property.
     */
    private final MetaProperty<DaysAdjustment> fixingDateOffset = DirectMetaProperty.ofImmutable(
        this, "fixingDateOffset", IborRateCalculation.class, DaysAdjustment.class);
    /**
     * The meta-property for the {@code negativeRateMethod} property.
     */
    private final MetaProperty<NegativeRateMethod> negativeRateMethod = DirectMetaProperty.ofImmutable(
        this, "negativeRateMethod", IborRateCalculation.class, NegativeRateMethod.class);
    /**
     * The meta-property for the {@code firstRegularRate} property.
     */
    private final MetaProperty<Double> firstRegularRate = DirectMetaProperty.ofImmutable(
        this, "firstRegularRate", IborRateCalculation.class, Double.class);
    /**
     * The meta-property for the {@code firstRate} property.
     */
    private final MetaProperty<Double> firstRate = DirectMetaProperty.ofImmutable(
        this, "firstRate", IborRateCalculation.class, Double.class);
    /**
     * The meta-property for the {@code firstFixingDateOffset} property.
     */
    private final MetaProperty<DaysAdjustment> firstFixingDateOffset = DirectMetaProperty.ofImmutable(
        this, "firstFixingDateOffset", IborRateCalculation.class, DaysAdjustment.class);
    /**
     * The meta-property for the {@code initialStub} property.
     */
    private final MetaProperty<IborRateStubCalculation> initialStub = DirectMetaProperty.ofImmutable(
        this, "initialStub", IborRateCalculation.class, IborRateStubCalculation.class);
    /**
     * The meta-property for the {@code finalStub} property.
     */
    private final MetaProperty<IborRateStubCalculation> finalStub = DirectMetaProperty.ofImmutable(
        this, "finalStub", IborRateCalculation.class, IborRateStubCalculation.class);
    /**
     * The meta-property for the {@code gearing} property.
     */
    private final MetaProperty<ValueSchedule> gearing = DirectMetaProperty.ofImmutable(
        this, "gearing", IborRateCalculation.class, ValueSchedule.class);
    /**
     * The meta-property for the {@code spread} property.
     */
    private final MetaProperty<ValueSchedule> spread = DirectMetaProperty.ofImmutable(
        this, "spread", IborRateCalculation.class, ValueSchedule.class);
    /**
     * The meta-properties.
     */
    private final Map<String, MetaProperty<?>> metaPropertyMap$ = new DirectMetaPropertyMap(
        this, null,
        "dayCount",
        "index",
        "resetPeriods",
        "fixingRelativeTo",
        "fixingDateOffset",
        "negativeRateMethod",
        "firstRegularRate",
        "firstRate",
        "firstFixingDateOffset",
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
        case 1905311443:  // dayCount
          return dayCount;
        case 100346066:  // index
          return index;
        case -1272973693:  // resetPeriods
          return resetPeriods;
        case 232554996:  // fixingRelativeTo
          return fixingRelativeTo;
        case 873743726:  // fixingDateOffset
          return fixingDateOffset;
        case 1969081334:  // negativeRateMethod
          return negativeRateMethod;
        case 570227148:  // firstRegularRate
          return firstRegularRate;
        case 132955056:  // firstRate
          return firstRate;
        case 2022439998:  // firstFixingDateOffset
          return firstFixingDateOffset;
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
    public IborRateCalculation.Builder builder() {
      return new IborRateCalculation.Builder();
    }

    @Override
    public Class<? extends IborRateCalculation> beanType() {
      return IborRateCalculation.class;
    }

    @Override
    public Map<String, MetaProperty<?>> metaPropertyMap() {
      return metaPropertyMap$;
    }

    //-----------------------------------------------------------------------
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
    public MetaProperty<IborIndex> index() {
      return index;
    }

    /**
     * The meta-property for the {@code resetPeriods} property.
     * @return the meta-property, not null
     */
    public MetaProperty<ResetSchedule> resetPeriods() {
      return resetPeriods;
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
     * The meta-property for the {@code negativeRateMethod} property.
     * @return the meta-property, not null
     */
    public MetaProperty<NegativeRateMethod> negativeRateMethod() {
      return negativeRateMethod;
    }

    /**
     * The meta-property for the {@code firstRegularRate} property.
     * @return the meta-property, not null
     */
    public MetaProperty<Double> firstRegularRate() {
      return firstRegularRate;
    }

    /**
     * The meta-property for the {@code firstRate} property.
     * @return the meta-property, not null
     */
    public MetaProperty<Double> firstRate() {
      return firstRate;
    }

    /**
     * The meta-property for the {@code firstFixingDateOffset} property.
     * @return the meta-property, not null
     */
    public MetaProperty<DaysAdjustment> firstFixingDateOffset() {
      return firstFixingDateOffset;
    }

    /**
     * The meta-property for the {@code initialStub} property.
     * @return the meta-property, not null
     */
    public MetaProperty<IborRateStubCalculation> initialStub() {
      return initialStub;
    }

    /**
     * The meta-property for the {@code finalStub} property.
     * @return the meta-property, not null
     */
    public MetaProperty<IborRateStubCalculation> finalStub() {
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
        case 1905311443:  // dayCount
          return ((IborRateCalculation) bean).getDayCount();
        case 100346066:  // index
          return ((IborRateCalculation) bean).getIndex();
        case -1272973693:  // resetPeriods
          return ((IborRateCalculation) bean).resetPeriods;
        case 232554996:  // fixingRelativeTo
          return ((IborRateCalculation) bean).getFixingRelativeTo();
        case 873743726:  // fixingDateOffset
          return ((IborRateCalculation) bean).getFixingDateOffset();
        case 1969081334:  // negativeRateMethod
          return ((IborRateCalculation) bean).getNegativeRateMethod();
        case 570227148:  // firstRegularRate
          return ((IborRateCalculation) bean).firstRegularRate;
        case 132955056:  // firstRate
          return ((IborRateCalculation) bean).firstRate;
        case 2022439998:  // firstFixingDateOffset
          return ((IborRateCalculation) bean).firstFixingDateOffset;
        case 1233359378:  // initialStub
          return ((IborRateCalculation) bean).initialStub;
        case 355242820:  // finalStub
          return ((IborRateCalculation) bean).finalStub;
        case -91774989:  // gearing
          return ((IborRateCalculation) bean).gearing;
        case -895684237:  // spread
          return ((IborRateCalculation) bean).spread;
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
   * The bean-builder for {@code IborRateCalculation}.
   */
  public static final class Builder extends DirectFieldsBeanBuilder<IborRateCalculation> {

    private DayCount dayCount;
    private IborIndex index;
    private ResetSchedule resetPeriods;
    private FixingRelativeTo fixingRelativeTo;
    private DaysAdjustment fixingDateOffset;
    private NegativeRateMethod negativeRateMethod;
    private Double firstRegularRate;
    private Double firstRate;
    private DaysAdjustment firstFixingDateOffset;
    private IborRateStubCalculation initialStub;
    private IborRateStubCalculation finalStub;
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
    private Builder(IborRateCalculation beanToCopy) {
      this.dayCount = beanToCopy.getDayCount();
      this.index = beanToCopy.getIndex();
      this.resetPeriods = beanToCopy.resetPeriods;
      this.fixingRelativeTo = beanToCopy.getFixingRelativeTo();
      this.fixingDateOffset = beanToCopy.getFixingDateOffset();
      this.negativeRateMethod = beanToCopy.getNegativeRateMethod();
      this.firstRegularRate = beanToCopy.firstRegularRate;
      this.firstRate = beanToCopy.firstRate;
      this.firstFixingDateOffset = beanToCopy.firstFixingDateOffset;
      this.initialStub = beanToCopy.initialStub;
      this.finalStub = beanToCopy.finalStub;
      this.gearing = beanToCopy.gearing;
      this.spread = beanToCopy.spread;
    }

    //-----------------------------------------------------------------------
    @Override
    public Object get(String propertyName) {
      switch (propertyName.hashCode()) {
        case 1905311443:  // dayCount
          return dayCount;
        case 100346066:  // index
          return index;
        case -1272973693:  // resetPeriods
          return resetPeriods;
        case 232554996:  // fixingRelativeTo
          return fixingRelativeTo;
        case 873743726:  // fixingDateOffset
          return fixingDateOffset;
        case 1969081334:  // negativeRateMethod
          return negativeRateMethod;
        case 570227148:  // firstRegularRate
          return firstRegularRate;
        case 132955056:  // firstRate
          return firstRate;
        case 2022439998:  // firstFixingDateOffset
          return firstFixingDateOffset;
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
        case 1905311443:  // dayCount
          this.dayCount = (DayCount) newValue;
          break;
        case 100346066:  // index
          this.index = (IborIndex) newValue;
          break;
        case -1272973693:  // resetPeriods
          this.resetPeriods = (ResetSchedule) newValue;
          break;
        case 232554996:  // fixingRelativeTo
          this.fixingRelativeTo = (FixingRelativeTo) newValue;
          break;
        case 873743726:  // fixingDateOffset
          this.fixingDateOffset = (DaysAdjustment) newValue;
          break;
        case 1969081334:  // negativeRateMethod
          this.negativeRateMethod = (NegativeRateMethod) newValue;
          break;
        case 570227148:  // firstRegularRate
          this.firstRegularRate = (Double) newValue;
          break;
        case 132955056:  // firstRate
          this.firstRate = (Double) newValue;
          break;
        case 2022439998:  // firstFixingDateOffset
          this.firstFixingDateOffset = (DaysAdjustment) newValue;
          break;
        case 1233359378:  // initialStub
          this.initialStub = (IborRateStubCalculation) newValue;
          break;
        case 355242820:  // finalStub
          this.finalStub = (IborRateStubCalculation) newValue;
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
    public IborRateCalculation build() {
      preBuild(this);
      return new IborRateCalculation(
          dayCount,
          index,
          resetPeriods,
          fixingRelativeTo,
          fixingDateOffset,
          negativeRateMethod,
          firstRegularRate,
          firstRate,
          firstFixingDateOffset,
          initialStub,
          finalStub,
          gearing,
          spread);
    }

    //-----------------------------------------------------------------------
    /**
     * Sets the day count convention.
     * <p>
     * This is used to convert dates to a numerical value.
     * <p>
     * When building, this will default to the day count of the index if not specified.
     * @param dayCount  the new value, not null
     * @return this, for chaining, not null
     */
    public Builder dayCount(DayCount dayCount) {
      JodaBeanUtils.notNull(dayCount, "dayCount");
      this.dayCount = dayCount;
      return this;
    }

    /**
     * Sets the Ibor index.
     * <p>
     * The rate to be paid is based on this index
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
     * Sets the reset schedule, used when averaging rates, optional.
     * <p>
     * Most swaps have a single fixing for each accrual period.
     * This property allows multiple fixings to be defined by dividing the accrual periods into reset periods.
     * <p>
     * If this property is not present, then the reset period is the same as the accrual period.
     * If this property is present, then the accrual period is divided as per the information
     * in the reset schedule, multiple fixing dates are calculated, and rate averaging performed.
     * @param resetPeriods  the new value
     * @return this, for chaining, not null
     */
    public Builder resetPeriods(ResetSchedule resetPeriods) {
      this.resetPeriods = resetPeriods;
      return this;
    }

    /**
     * Sets the base date that each fixing is made relative to, defaulted to 'PeriodStart'.
     * <p>
     * The fixing date is relative to either the start or end of each reset period.
     * <p>
     * Note that in most cases, the reset frequency matches the accrual frequency
     * and thus there is only one fixing for the accrual period.
     * @param fixingRelativeTo  the new value, not null
     * @return this, for chaining, not null
     */
    public Builder fixingRelativeTo(FixingRelativeTo fixingRelativeTo) {
      JodaBeanUtils.notNull(fixingRelativeTo, "fixingRelativeTo");
      this.fixingRelativeTo = fixingRelativeTo;
      return this;
    }

    /**
     * Sets the offset of the fixing date from each adjusted reset date.
     * <p>
     * The offset is applied to the base date specified by {@code fixingRelativeTo}.
     * The offset is typically a negative number of business days.
     * <p>
     * Note that in most cases, the reset frequency matches the accrual frequency
     * and thus there is only one fixing for the accrual period.
     * <p>
     * When building, this will default to the fixing offset of the index if not specified.
     * @param fixingDateOffset  the new value, not null
     * @return this, for chaining, not null
     */
    public Builder fixingDateOffset(DaysAdjustment fixingDateOffset) {
      JodaBeanUtils.notNull(fixingDateOffset, "fixingDateOffset");
      this.fixingDateOffset = fixingDateOffset;
      return this;
    }

    /**
     * Sets the negative rate method, defaulted to 'AllowNegative'.
     * <p>
     * This is used when the interest rate, observed or calculated, goes negative.
     * It does not apply if the rate is fixed, such as in a stub or using {@code firstRegularRate}.
     * <p>
     * Defined by the 2006 ISDA definitions article 6.4.
     * @param negativeRateMethod  the new value, not null
     * @return this, for chaining, not null
     */
    public Builder negativeRateMethod(NegativeRateMethod negativeRateMethod) {
      JodaBeanUtils.notNull(negativeRateMethod, "negativeRateMethod");
      this.negativeRateMethod = negativeRateMethod;
      return this;
    }

    /**
     * Sets the rate of the first regular reset period, optional.
     * A 5% rate will be expressed as 0.05.
     * <p>
     * In certain circumstances two counterparties agree the rate of the first fixing
     * when the contract starts, and it is used in place of one observed fixing.
     * For all other fixings, the rate is observed via the normal fixing process.
     * <p>
     * This property allows the rate of the first reset period of the first <i>regular</i> accrual period
     * to be controlled. Note that if there is an initial stub, this will be the second reset period.
     * Other calculation elements, such as gearing or spread, still apply to the rate specified here.
     * <p>
     * If the first rate applies to the initial stub rather than the regular accrual periods
     * it must be specified using {@code initialStub}. Alternatively, {@code firstRate} can be used.
     * <p>
     * This property follows the definition in FpML. See also {@code firstRate}.
     * @param firstRegularRate  the new value
     * @return this, for chaining, not null
     */
    public Builder firstRegularRate(Double firstRegularRate) {
      this.firstRegularRate = firstRegularRate;
      return this;
    }

    /**
     * Sets the rate of the first reset period, which may be a stub, optional.
     * A 5% rate will be expressed as 0.05.
     * <p>
     * In certain circumstances two counterparties agree the rate of the first fixing
     * when the contract starts, and it is used in place of one observed fixing.
     * For all other fixings, the rate is observed via the normal fixing process.
     * <p>
     * This property allows the rate of the first reset period to be controlled,
     * irrespective of whether that is an initial stub or a regular period.
     * Other calculation elements, such as gearing or spread, still apply to the rate specified here.
     * <p>
     * This property is similar to {@code firstRegularRate}.
     * This property operates on the first reset period, whether that is an initial stub or a regular period.
     * By contrast, {@code firstRegularRate} operates on the first regular period, and never on a stub.
     * <p>
     * If either {@code firstRegularRate} or {@code initialStub} are present, this property is ignored.
     * <p>
     * If this property is not present, then the first rate is observed via the normal fixing process.
     * @param firstRate  the new value
     * @return this, for chaining, not null
     */
    public Builder firstRate(Double firstRate) {
      this.firstRate = firstRate;
      return this;
    }

    /**
     * Sets the offset of the first fixing date from the first adjusted reset date, optional.
     * <p>
     * If present, this offset is used instead of {@code fixingDateOffset} for the first
     * reset period of the swap, which will be either an initial stub or the first reset
     * period of the first <i>regular</i> accrual period.
     * <p>
     * The offset is applied to the base date specified by {@code fixingRelativeTo}.
     * The offset is typically a negative number of business days.
     * <p>
     * If this property is not present, then the {@code fixingDateOffset} applies to all fixings.
     * @param firstFixingDateOffset  the new value
     * @return this, for chaining, not null
     */
    public Builder firstFixingDateOffset(DaysAdjustment firstFixingDateOffset) {
      this.firstFixingDateOffset = firstFixingDateOffset;
      return this;
    }

    /**
     * Sets the rate to be used in initial stub, optional.
     * <p>
     * The initial stub of a swap may have different rate rules to the regular accrual periods.
     * A fixed rate may be specified, a different floating rate or a linearly interpolated floating rate.
     * This may not be present if there is no initial stub, or if the index during the stub is the same
     * as the main floating rate index.
     * <p>
     * If this property is not present, then the main index applies during any initial stub.
     * If this property is present and there is no initial stub, it is ignored.
     * @param initialStub  the new value
     * @return this, for chaining, not null
     */
    public Builder initialStub(IborRateStubCalculation initialStub) {
      this.initialStub = initialStub;
      return this;
    }

    /**
     * Sets the rate to be used in final stub, optional.
     * <p>
     * The final stub of a swap may have different rate rules to the regular accrual periods.
     * A fixed rate may be specified, a different floating rate or a linearly interpolated floating rate.
     * This may not be present if there is no final stub, or if the index during the stub is the same
     * as the main floating rate index.
     * <p>
     * If this property is not present, then the main index applies during any final stub.
     * If this property is present and there is no final stub, it is ignored.
     * @param finalStub  the new value
     * @return this, for chaining, not null
     */
    public Builder finalStub(IborRateStubCalculation finalStub) {
      this.finalStub = finalStub;
      return this;
    }

    /**
     * Sets the gearing multiplier, optional.
     * <p>
     * This defines the gearing as an initial value and a list of adjustments.
     * The gearing is only permitted to change at accrual period boundaries.
     * <p>
     * When calculating the rate, the fixing rate is multiplied by the gearing.
     * A gearing of 1 has no effect.
     * If both gearing and spread exist, then the gearing is applied first.
     * <p>
     * If this property is not present, then no gearing applies.
     * <p>
     * Gearing is also known as <i>leverage</i>.
     * @param gearing  the new value
     * @return this, for chaining, not null
     */
    public Builder gearing(ValueSchedule gearing) {
      this.gearing = gearing;
      return this;
    }

    /**
     * Sets the spread rate, with a 5% rate expressed as 0.05, optional.
     * <p>
     * This defines the spread as an initial value and a list of adjustments.
     * The spread is only permitted to change at accrual period boundaries.
     * Spread is a per annum rate.
     * <p>
     * When calculating the rate, the spread is added to the fixing rate.
     * A spread of 0 has no effect.
     * If both gearing and spread exist, then the gearing is applied first.
     * <p>
     * If this property is not present, then no spread applies.
     * <p>
     * Defined by the 2006 ISDA definitions article 6.2e.
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
      buf.append("IborRateCalculation.Builder{");
      buf.append("dayCount").append('=').append(JodaBeanUtils.toString(dayCount)).append(',').append(' ');
      buf.append("index").append('=').append(JodaBeanUtils.toString(index)).append(',').append(' ');
      buf.append("resetPeriods").append('=').append(JodaBeanUtils.toString(resetPeriods)).append(',').append(' ');
      buf.append("fixingRelativeTo").append('=').append(JodaBeanUtils.toString(fixingRelativeTo)).append(',').append(' ');
      buf.append("fixingDateOffset").append('=').append(JodaBeanUtils.toString(fixingDateOffset)).append(',').append(' ');
      buf.append("negativeRateMethod").append('=').append(JodaBeanUtils.toString(negativeRateMethod)).append(',').append(' ');
      buf.append("firstRegularRate").append('=').append(JodaBeanUtils.toString(firstRegularRate)).append(',').append(' ');
      buf.append("firstRate").append('=').append(JodaBeanUtils.toString(firstRate)).append(',').append(' ');
      buf.append("firstFixingDateOffset").append('=').append(JodaBeanUtils.toString(firstFixingDateOffset)).append(',').append(' ');
      buf.append("initialStub").append('=').append(JodaBeanUtils.toString(initialStub)).append(',').append(' ');
      buf.append("finalStub").append('=').append(JodaBeanUtils.toString(finalStub)).append(',').append(' ');
      buf.append("gearing").append('=').append(JodaBeanUtils.toString(gearing)).append(',').append(' ');
      buf.append("spread").append('=').append(JodaBeanUtils.toString(spread));
      buf.append('}');
      return buf.toString();
    }

  }

  //-------------------------- AUTOGENERATED END --------------------------
}
