/**
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.product.bond;

import static com.opengamma.strata.collect.Guavate.ensureOnlyOne;

import java.io.Serializable;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.Set;
import java.util.stream.Collectors;

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

import com.google.common.collect.ImmutableList;
import com.opengamma.strata.basics.ReferenceData;
import com.opengamma.strata.basics.StandardId;
import com.opengamma.strata.basics.currency.Currency;
import com.opengamma.strata.basics.date.DayCount;
import com.opengamma.strata.basics.date.DayCount.ScheduleInfo;
import com.opengamma.strata.basics.date.DayCounts;
import com.opengamma.strata.basics.date.DaysAdjustment;
import com.opengamma.strata.basics.schedule.Frequency;
import com.opengamma.strata.basics.schedule.RollConvention;
import com.opengamma.strata.basics.schedule.RollConventions;
import com.opengamma.strata.collect.ArgChecker;
import com.opengamma.strata.product.ResolvedProduct;
import com.opengamma.strata.product.SecurityId;
import com.opengamma.strata.product.swap.InflationRateCalculation;

/**
 * A capital indexed bond.
 * <p>
 * This is the resolved form of {@link CapitalIndexedBond} and is an input to the pricers.
 * Applications will typically create a {@code ResolvedCapitalIndexedBond} from a {@code CapitalIndexedBond}
 * using {@link CapitalIndexedBond#resolve(ReferenceData)}.
 * <p>
 * The periodic coupon payments are defined in {@code periodicPayments},
 * whereas {@code nominalPayment} separately represents the nominal payments.
 * <p>
 * The legal entity of this bond is identified by {@code legalEntityId}.
 * The enum, {@code yieldConvention}, specifies the yield computation convention.
 * The accrued interest must be computed with {@code dayCount}.
 * <p>
 * A {@code ResolvedCapitalIndexedBond} is bound to data that changes over time, such as holiday calendars.
 * If the data changes, such as the addition of a new holiday, the resolved form will not be updated.
 * Care must be taken when placing the resolved form in a cache or persistence layer.
 * 
 * <h4>Price</h4>
 * Strata uses <i>decimal prices</i> for bonds in the trade model, pricers and market data.
 * For example, a price of 99.32% is represented in Strata by 0.9932.
 */
@BeanDefinition
public final class ResolvedCapitalIndexedBond
    implements ResolvedProduct, ImmutableBean, Serializable {

  /**
   * The security identifier.
   * <p>
   * This identifier uniquely identifies the security within the system.
   */
  @PropertyDefinition(validate = "notNull")
  private final SecurityId securityId;
  /**
   * The nominal payment of the product.
   * <p>
   * The payment date of the nominal payment agrees with the final coupon payment date of the periodic payments.
   */
  @PropertyDefinition(validate = "notNull")
  private final CapitalIndexedBondPaymentPeriod nominalPayment;
  /**
   * The periodic payments of the product.
   * <p>
   * Each payment period represents part of the life-time of the product.
   * The start date and end date of the leg are determined from the first and last period.
   * As such, the periods should be sorted.
   */
  @PropertyDefinition(validate = "notNull")
  private final ImmutableList<CapitalIndexedBondPaymentPeriod> periodicPayments;
  /**
   * The frequency of the bond payments.
   * <p>
   * This must match the frequency used to generate the payment schedule.
   */
  @PropertyDefinition(validate = "notNull")
  private final Frequency frequency;
  /**
   * The roll convention of the bond payments.
   * <p>
   * This must match the convention used to generate the payment schedule.
   */
  @PropertyDefinition(validate = "notNull")
  private final RollConvention rollConvention;
  /**
   * The day count convention applicable.
   * <p>
   * The conversion from dates to a numerical value is made based on this day count.
   * For the inflation-indexed bond, the day count convention is used to compute accrued interest.
   */
  @PropertyDefinition(validate = "notNull")
  private final DayCount dayCount;
  /**
   * Yield convention.
   * <p>
   * The convention defines how to convert from yield to price and inversely.
   */
  @PropertyDefinition(validate = "notNull")
  private final CapitalIndexedBondYieldConvention yieldConvention;
  /**
   * The legal entity identifier.
   * <p>
   * This identifier is used for the legal entity that issues the bond.
   */
  @PropertyDefinition(validate = "notNull")
  private final StandardId legalEntityId;
  /**
   * The number of days between valuation date and settlement date.
   * <p>
   * This is used to compute clean price.
   * The clean price is the relative price to be paid at the standard settlement date in exchange for the bond.
   */
  @PropertyDefinition(validate = "notNull")
  private final DaysAdjustment settlementDateOffset;
  /**
   * The inflation rate calculation.
   * <p>
   * The reference index is interpolated index or monthly index.
   * Real coupons are represented by {@code gearing} in the calculation.
   * The price index value at the start of the bond is represented by {@code firstIndexValue} in the calculation.
   */
  @PropertyDefinition(validate = "notNull")
  private final InflationRateCalculation rateCalculation;

  //-------------------------------------------------------------------------
  @ImmutableValidator
  private void validate() {
    Currency currencyNominal = nominalPayment.getCurrency();
    Set<Currency> currencies =
        periodicPayments.stream().map(CapitalIndexedBondPaymentPeriod::getCurrency).collect(Collectors.toSet());
    currencies.add(currencyNominal);
    ArgChecker.isTrue(currencies.size() == 1, "Product must have a single currency, found: " + currencies);
    ArgChecker.isTrue(rateCalculation.getFirstIndexValue().isPresent(), "Rate calculation must specify first index value");
  }

  //-------------------------------------------------------------------------
  /**
   * Gets the start date of the product.
   * <p>
   * This is the first coupon period date of the bond, often known as the effective date.
   * This date has been adjusted to be a valid business day.
   * 
   * @return the start date
   */
  public LocalDate getStartDate() {
    return periodicPayments.get(0).getStartDate();
  }

  /**
   * Gets the end date of the product.
   * <p>
   * This is the last coupon period date of the bond, often known as the maturity date.
   * This date has been adjusted to be a valid business day.
   * 
   * @return the end date
   */
  public LocalDate getEndDate() {
    return periodicPayments.get(periodicPayments.size() - 1).getEndDate();
  }

  /**
   * The unadjusted start date.
   * <p>
   * This is the unadjusted first coupon period date of the bond.
   * 
   * @return the unadjusted start date
   */
  public LocalDate getUnadjustedStartDate() {
    return periodicPayments.get(0).getUnadjustedStartDate();
  }

  /**
   * The unadjusted end date.
   * <p>
   * This is the unadjusted last coupon period date of the bond.
   * 
   * @return the unadjusted end date
   */
  public LocalDate getUnadjustedEndDate() {
    return periodicPayments.get(periodicPayments.size() - 1).getUnadjustedEndDate();
  }

  /**
   * Gets the currency of the product.
   * <p>
   * All payments in the bond will have this currency.
   * 
   * @return the currency
   */
  public Currency getCurrency() {
    return nominalPayment.getCurrency();
  }

  /**
   * Gets the notional amount, must be positive.
   * <p>
   * The notional expressed here must be positive.
   * The currency of the notional is specified by {@link #getCurrency()}.
   * 
   * @return the notional amount
   */
  public double getNotional() {
    return periodicPayments.get(0).getNotional();
  }

  /**
   * Checks if there is an ex-coupon period.
   * 
   * @return true if has an ex-coupon period
   */
  public boolean hasExCouponPeriod() {
    return periodicPayments.get(0).hasExCouponPeriod();
  }

  /**
   * Gets the first index value
   * <p>
   * This is the price index value at the start of the bond.
   * 
   * @return the first index value
   */
  public double getFirstIndexValue() {
    return rateCalculation.getFirstIndexValue().getAsDouble();  // validated in constructor
  }

  //-------------------------------------------------------------------------
  /**
   * Finds the period that contains the specified date.
   * <p>
   * The search is performed using unadjusted dates.
   * 
   * @param date  the date to find the period for
   * @return the period, empty if not found
   * @throws IllegalArgumentException if more than one period matches
   */
  public Optional<CapitalIndexedBondPaymentPeriod> findPeriod(LocalDate date) {
    return periodicPayments.stream()
        .filter(p -> p.contains(date))
        .reduce(ensureOnlyOne());
  }

  /**
   * Finds the period that contains the specified date.
   * <p>
   * The search is performed using unadjusted dates.
   * 
   * @param date  the date to find the period for
   * @return the period, empty if not found
   * @throws IllegalArgumentException if more than one period matches
   */
  public OptionalInt findPeriodIndex(LocalDate date) {
    for (int i = 0; i < periodicPayments.size(); i++) {
      if (periodicPayments.get(i).contains(date)) {
        return OptionalInt.of(i);
      }
    }
    return OptionalInt.empty();
  }

  /**
   * Calculates the year fraction within the specified period.
   * <p>
   * Year fractions on bonds are calculated on unadjusted dates.
   * 
   * @param startDate  the start date
   * @param endDate  the end date
   * @return the year fraction
   * @throws IllegalArgumentException if the dates are outside the range of the bond or start is after end
   */
  public double yearFraction(LocalDate startDate, LocalDate endDate) {
    return yearFraction(startDate, endDate, dayCount);
  }

  /**
   * Calculates the year fraction within the specified period and day count.
   * <p>
   * Year fractions on bonds are calculated on unadjusted dates.
   * 
   * @param startDate  the start date
   * @param endDate  the end date
   * @param dayCount the day count
   * @return the year fraction
   * @throws IllegalArgumentException if the dates are outside the range of the bond or start is after end
   */
  public double yearFraction(LocalDate startDate, LocalDate endDate, DayCount dayCount) {
    ArgChecker.inOrderOrEqual(getUnadjustedStartDate(), startDate, "bond.unadjustedStartDate", "startDate");
    ArgChecker.inOrderOrEqual(startDate, endDate, "startDate", "endDate");
    ArgChecker.inOrderOrEqual(endDate, getUnadjustedEndDate(), "endDate", "bond.unadjustedEndDate");
    // bond day counts often need ScheduleInfo
    ScheduleInfo info = new ScheduleInfo() {
      @Override
      public LocalDate getStartDate() {
        return getUnadjustedStartDate();
      }

      @Override
      public LocalDate getEndDate() {
        return getUnadjustedEndDate();
      }

      @Override
      public Frequency getFrequency() {
        return frequency;
      }

      @Override
      public LocalDate getPeriodEndDate(LocalDate date) {
        return periodicPayments.stream()
            .filter(p -> p.contains(date))
            .map(p -> p.getUnadjustedEndDate())
            .findFirst()
            .orElseThrow(() -> new IllegalArgumentException("Date is not contained in any period"));
      }

      @Override
      public boolean isEndOfMonthConvention() {
        return rollConvention == RollConventions.EOM;
      }
    };
    return dayCount.yearFraction(startDate, endDate, info);
  }

  //-------------------------------------------------------------------------
  /**
   * Calculates the settlement date from the valuation date.
   * 
   * @param valuationDate  the valuation date
   * @param refData  the reference data to use
   * @return the settlement date
   */
  public LocalDate calculateSettlementDateFromValuation(LocalDate valuationDate, ReferenceData refData) {
    return settlementDateOffset.adjust(valuationDate, refData);
  }

  //-------------------------------------------------------------------------
  /**
   * Calculates the accrued interest of the bond with the specified date.
   * 
   * @param referenceDate  the reference date
   * @return the accrued interest of the product 
   */
  public double accruedInterest(LocalDate referenceDate) {
    if (getUnadjustedStartDate().isAfter(referenceDate)) {
      return 0d;
    }
    double notional = getNotional();
    CapitalIndexedBondPaymentPeriod period = findPeriod(referenceDate)
        .orElseThrow(() -> new IllegalArgumentException("Date outside range of bond"));
    LocalDate previousAccrualDate = period.getUnadjustedStartDate();
    double realCoupon = period.getRealCoupon();
    double couponPerYear = getFrequency().eventsPerYear();
    double rate = realCoupon * couponPerYear;
    double accruedInterest = yieldConvention.equals(CapitalIndexedBondYieldConvention.JP_IL_COMPOUND) ||
        yieldConvention.equals(CapitalIndexedBondYieldConvention.JP_IL_SIMPLE) ?
            yearFraction(previousAccrualDate, referenceDate, DayCounts.ACT_365F) * rate * notional :
            yearFraction(previousAccrualDate, referenceDate) * rate * notional;
    double result = 0d;
    if (hasExCouponPeriod() && !referenceDate.isBefore(period.getDetachmentDate())) {
      result = accruedInterest - notional * rate * yearFraction(previousAccrualDate, period.getUnadjustedEndDate());
    } else {
      result = accruedInterest;
    }
    return result;
  }

  //------------------------- AUTOGENERATED START -------------------------
  ///CLOVER:OFF
  /**
   * The meta-bean for {@code ResolvedCapitalIndexedBond}.
   * @return the meta-bean, not null
   */
  public static ResolvedCapitalIndexedBond.Meta meta() {
    return ResolvedCapitalIndexedBond.Meta.INSTANCE;
  }

  static {
    JodaBeanUtils.registerMetaBean(ResolvedCapitalIndexedBond.Meta.INSTANCE);
  }

  /**
   * The serialization version id.
   */
  private static final long serialVersionUID = 1L;

  /**
   * Returns a builder used to create an instance of the bean.
   * @return the builder, not null
   */
  public static ResolvedCapitalIndexedBond.Builder builder() {
    return new ResolvedCapitalIndexedBond.Builder();
  }

  private ResolvedCapitalIndexedBond(
      SecurityId securityId,
      CapitalIndexedBondPaymentPeriod nominalPayment,
      List<CapitalIndexedBondPaymentPeriod> periodicPayments,
      Frequency frequency,
      RollConvention rollConvention,
      DayCount dayCount,
      CapitalIndexedBondYieldConvention yieldConvention,
      StandardId legalEntityId,
      DaysAdjustment settlementDateOffset,
      InflationRateCalculation rateCalculation) {
    JodaBeanUtils.notNull(securityId, "securityId");
    JodaBeanUtils.notNull(nominalPayment, "nominalPayment");
    JodaBeanUtils.notNull(periodicPayments, "periodicPayments");
    JodaBeanUtils.notNull(frequency, "frequency");
    JodaBeanUtils.notNull(rollConvention, "rollConvention");
    JodaBeanUtils.notNull(dayCount, "dayCount");
    JodaBeanUtils.notNull(yieldConvention, "yieldConvention");
    JodaBeanUtils.notNull(legalEntityId, "legalEntityId");
    JodaBeanUtils.notNull(settlementDateOffset, "settlementDateOffset");
    JodaBeanUtils.notNull(rateCalculation, "rateCalculation");
    this.securityId = securityId;
    this.nominalPayment = nominalPayment;
    this.periodicPayments = ImmutableList.copyOf(periodicPayments);
    this.frequency = frequency;
    this.rollConvention = rollConvention;
    this.dayCount = dayCount;
    this.yieldConvention = yieldConvention;
    this.legalEntityId = legalEntityId;
    this.settlementDateOffset = settlementDateOffset;
    this.rateCalculation = rateCalculation;
    validate();
  }

  @Override
  public ResolvedCapitalIndexedBond.Meta metaBean() {
    return ResolvedCapitalIndexedBond.Meta.INSTANCE;
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
   * Gets the security identifier.
   * <p>
   * This identifier uniquely identifies the security within the system.
   * @return the value of the property, not null
   */
  public SecurityId getSecurityId() {
    return securityId;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the nominal payment of the product.
   * <p>
   * The payment date of the nominal payment agrees with the final coupon payment date of the periodic payments.
   * @return the value of the property, not null
   */
  public CapitalIndexedBondPaymentPeriod getNominalPayment() {
    return nominalPayment;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the periodic payments of the product.
   * <p>
   * Each payment period represents part of the life-time of the product.
   * The start date and end date of the leg are determined from the first and last period.
   * As such, the periods should be sorted.
   * @return the value of the property, not null
   */
  public ImmutableList<CapitalIndexedBondPaymentPeriod> getPeriodicPayments() {
    return periodicPayments;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the frequency of the bond payments.
   * <p>
   * This must match the frequency used to generate the payment schedule.
   * @return the value of the property, not null
   */
  public Frequency getFrequency() {
    return frequency;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the roll convention of the bond payments.
   * <p>
   * This must match the convention used to generate the payment schedule.
   * @return the value of the property, not null
   */
  public RollConvention getRollConvention() {
    return rollConvention;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the day count convention applicable.
   * <p>
   * The conversion from dates to a numerical value is made based on this day count.
   * For the inflation-indexed bond, the day count convention is used to compute accrued interest.
   * @return the value of the property, not null
   */
  public DayCount getDayCount() {
    return dayCount;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets yield convention.
   * <p>
   * The convention defines how to convert from yield to price and inversely.
   * @return the value of the property, not null
   */
  public CapitalIndexedBondYieldConvention getYieldConvention() {
    return yieldConvention;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the legal entity identifier.
   * <p>
   * This identifier is used for the legal entity that issues the bond.
   * @return the value of the property, not null
   */
  public StandardId getLegalEntityId() {
    return legalEntityId;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the number of days between valuation date and settlement date.
   * <p>
   * This is used to compute clean price.
   * The clean price is the relative price to be paid at the standard settlement date in exchange for the bond.
   * @return the value of the property, not null
   */
  public DaysAdjustment getSettlementDateOffset() {
    return settlementDateOffset;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the inflation rate calculation.
   * <p>
   * The reference index is interpolated index or monthly index.
   * Real coupons are represented by {@code gearing} in the calculation.
   * The price index value at the start of the bond is represented by {@code firstIndexValue} in the calculation.
   * @return the value of the property, not null
   */
  public InflationRateCalculation getRateCalculation() {
    return rateCalculation;
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
      ResolvedCapitalIndexedBond other = (ResolvedCapitalIndexedBond) obj;
      return JodaBeanUtils.equal(securityId, other.securityId) &&
          JodaBeanUtils.equal(nominalPayment, other.nominalPayment) &&
          JodaBeanUtils.equal(periodicPayments, other.periodicPayments) &&
          JodaBeanUtils.equal(frequency, other.frequency) &&
          JodaBeanUtils.equal(rollConvention, other.rollConvention) &&
          JodaBeanUtils.equal(dayCount, other.dayCount) &&
          JodaBeanUtils.equal(yieldConvention, other.yieldConvention) &&
          JodaBeanUtils.equal(legalEntityId, other.legalEntityId) &&
          JodaBeanUtils.equal(settlementDateOffset, other.settlementDateOffset) &&
          JodaBeanUtils.equal(rateCalculation, other.rateCalculation);
    }
    return false;
  }

  @Override
  public int hashCode() {
    int hash = getClass().hashCode();
    hash = hash * 31 + JodaBeanUtils.hashCode(securityId);
    hash = hash * 31 + JodaBeanUtils.hashCode(nominalPayment);
    hash = hash * 31 + JodaBeanUtils.hashCode(periodicPayments);
    hash = hash * 31 + JodaBeanUtils.hashCode(frequency);
    hash = hash * 31 + JodaBeanUtils.hashCode(rollConvention);
    hash = hash * 31 + JodaBeanUtils.hashCode(dayCount);
    hash = hash * 31 + JodaBeanUtils.hashCode(yieldConvention);
    hash = hash * 31 + JodaBeanUtils.hashCode(legalEntityId);
    hash = hash * 31 + JodaBeanUtils.hashCode(settlementDateOffset);
    hash = hash * 31 + JodaBeanUtils.hashCode(rateCalculation);
    return hash;
  }

  @Override
  public String toString() {
    StringBuilder buf = new StringBuilder(352);
    buf.append("ResolvedCapitalIndexedBond{");
    buf.append("securityId").append('=').append(securityId).append(',').append(' ');
    buf.append("nominalPayment").append('=').append(nominalPayment).append(',').append(' ');
    buf.append("periodicPayments").append('=').append(periodicPayments).append(',').append(' ');
    buf.append("frequency").append('=').append(frequency).append(',').append(' ');
    buf.append("rollConvention").append('=').append(rollConvention).append(',').append(' ');
    buf.append("dayCount").append('=').append(dayCount).append(',').append(' ');
    buf.append("yieldConvention").append('=').append(yieldConvention).append(',').append(' ');
    buf.append("legalEntityId").append('=').append(legalEntityId).append(',').append(' ');
    buf.append("settlementDateOffset").append('=').append(settlementDateOffset).append(',').append(' ');
    buf.append("rateCalculation").append('=').append(JodaBeanUtils.toString(rateCalculation));
    buf.append('}');
    return buf.toString();
  }

  //-----------------------------------------------------------------------
  /**
   * The meta-bean for {@code ResolvedCapitalIndexedBond}.
   */
  public static final class Meta extends DirectMetaBean {
    /**
     * The singleton instance of the meta-bean.
     */
    static final Meta INSTANCE = new Meta();

    /**
     * The meta-property for the {@code securityId} property.
     */
    private final MetaProperty<SecurityId> securityId = DirectMetaProperty.ofImmutable(
        this, "securityId", ResolvedCapitalIndexedBond.class, SecurityId.class);
    /**
     * The meta-property for the {@code nominalPayment} property.
     */
    private final MetaProperty<CapitalIndexedBondPaymentPeriod> nominalPayment = DirectMetaProperty.ofImmutable(
        this, "nominalPayment", ResolvedCapitalIndexedBond.class, CapitalIndexedBondPaymentPeriod.class);
    /**
     * The meta-property for the {@code periodicPayments} property.
     */
    @SuppressWarnings({"unchecked", "rawtypes" })
    private final MetaProperty<ImmutableList<CapitalIndexedBondPaymentPeriod>> periodicPayments = DirectMetaProperty.ofImmutable(
        this, "periodicPayments", ResolvedCapitalIndexedBond.class, (Class) ImmutableList.class);
    /**
     * The meta-property for the {@code frequency} property.
     */
    private final MetaProperty<Frequency> frequency = DirectMetaProperty.ofImmutable(
        this, "frequency", ResolvedCapitalIndexedBond.class, Frequency.class);
    /**
     * The meta-property for the {@code rollConvention} property.
     */
    private final MetaProperty<RollConvention> rollConvention = DirectMetaProperty.ofImmutable(
        this, "rollConvention", ResolvedCapitalIndexedBond.class, RollConvention.class);
    /**
     * The meta-property for the {@code dayCount} property.
     */
    private final MetaProperty<DayCount> dayCount = DirectMetaProperty.ofImmutable(
        this, "dayCount", ResolvedCapitalIndexedBond.class, DayCount.class);
    /**
     * The meta-property for the {@code yieldConvention} property.
     */
    private final MetaProperty<CapitalIndexedBondYieldConvention> yieldConvention = DirectMetaProperty.ofImmutable(
        this, "yieldConvention", ResolvedCapitalIndexedBond.class, CapitalIndexedBondYieldConvention.class);
    /**
     * The meta-property for the {@code legalEntityId} property.
     */
    private final MetaProperty<StandardId> legalEntityId = DirectMetaProperty.ofImmutable(
        this, "legalEntityId", ResolvedCapitalIndexedBond.class, StandardId.class);
    /**
     * The meta-property for the {@code settlementDateOffset} property.
     */
    private final MetaProperty<DaysAdjustment> settlementDateOffset = DirectMetaProperty.ofImmutable(
        this, "settlementDateOffset", ResolvedCapitalIndexedBond.class, DaysAdjustment.class);
    /**
     * The meta-property for the {@code rateCalculation} property.
     */
    private final MetaProperty<InflationRateCalculation> rateCalculation = DirectMetaProperty.ofImmutable(
        this, "rateCalculation", ResolvedCapitalIndexedBond.class, InflationRateCalculation.class);
    /**
     * The meta-properties.
     */
    private final Map<String, MetaProperty<?>> metaPropertyMap$ = new DirectMetaPropertyMap(
        this, null,
        "securityId",
        "nominalPayment",
        "periodicPayments",
        "frequency",
        "rollConvention",
        "dayCount",
        "yieldConvention",
        "legalEntityId",
        "settlementDateOffset",
        "rateCalculation");

    /**
     * Restricted constructor.
     */
    private Meta() {
    }

    @Override
    protected MetaProperty<?> metaPropertyGet(String propertyName) {
      switch (propertyName.hashCode()) {
        case 1574023291:  // securityId
          return securityId;
        case -44199542:  // nominalPayment
          return nominalPayment;
        case -367345944:  // periodicPayments
          return periodicPayments;
        case -70023844:  // frequency
          return frequency;
        case -10223666:  // rollConvention
          return rollConvention;
        case 1905311443:  // dayCount
          return dayCount;
        case -1895216418:  // yieldConvention
          return yieldConvention;
        case 866287159:  // legalEntityId
          return legalEntityId;
        case 135924714:  // settlementDateOffset
          return settlementDateOffset;
        case -521703991:  // rateCalculation
          return rateCalculation;
      }
      return super.metaPropertyGet(propertyName);
    }

    @Override
    public ResolvedCapitalIndexedBond.Builder builder() {
      return new ResolvedCapitalIndexedBond.Builder();
    }

    @Override
    public Class<? extends ResolvedCapitalIndexedBond> beanType() {
      return ResolvedCapitalIndexedBond.class;
    }

    @Override
    public Map<String, MetaProperty<?>> metaPropertyMap() {
      return metaPropertyMap$;
    }

    //-----------------------------------------------------------------------
    /**
     * The meta-property for the {@code securityId} property.
     * @return the meta-property, not null
     */
    public MetaProperty<SecurityId> securityId() {
      return securityId;
    }

    /**
     * The meta-property for the {@code nominalPayment} property.
     * @return the meta-property, not null
     */
    public MetaProperty<CapitalIndexedBondPaymentPeriod> nominalPayment() {
      return nominalPayment;
    }

    /**
     * The meta-property for the {@code periodicPayments} property.
     * @return the meta-property, not null
     */
    public MetaProperty<ImmutableList<CapitalIndexedBondPaymentPeriod>> periodicPayments() {
      return periodicPayments;
    }

    /**
     * The meta-property for the {@code frequency} property.
     * @return the meta-property, not null
     */
    public MetaProperty<Frequency> frequency() {
      return frequency;
    }

    /**
     * The meta-property for the {@code rollConvention} property.
     * @return the meta-property, not null
     */
    public MetaProperty<RollConvention> rollConvention() {
      return rollConvention;
    }

    /**
     * The meta-property for the {@code dayCount} property.
     * @return the meta-property, not null
     */
    public MetaProperty<DayCount> dayCount() {
      return dayCount;
    }

    /**
     * The meta-property for the {@code yieldConvention} property.
     * @return the meta-property, not null
     */
    public MetaProperty<CapitalIndexedBondYieldConvention> yieldConvention() {
      return yieldConvention;
    }

    /**
     * The meta-property for the {@code legalEntityId} property.
     * @return the meta-property, not null
     */
    public MetaProperty<StandardId> legalEntityId() {
      return legalEntityId;
    }

    /**
     * The meta-property for the {@code settlementDateOffset} property.
     * @return the meta-property, not null
     */
    public MetaProperty<DaysAdjustment> settlementDateOffset() {
      return settlementDateOffset;
    }

    /**
     * The meta-property for the {@code rateCalculation} property.
     * @return the meta-property, not null
     */
    public MetaProperty<InflationRateCalculation> rateCalculation() {
      return rateCalculation;
    }

    //-----------------------------------------------------------------------
    @Override
    protected Object propertyGet(Bean bean, String propertyName, boolean quiet) {
      switch (propertyName.hashCode()) {
        case 1574023291:  // securityId
          return ((ResolvedCapitalIndexedBond) bean).getSecurityId();
        case -44199542:  // nominalPayment
          return ((ResolvedCapitalIndexedBond) bean).getNominalPayment();
        case -367345944:  // periodicPayments
          return ((ResolvedCapitalIndexedBond) bean).getPeriodicPayments();
        case -70023844:  // frequency
          return ((ResolvedCapitalIndexedBond) bean).getFrequency();
        case -10223666:  // rollConvention
          return ((ResolvedCapitalIndexedBond) bean).getRollConvention();
        case 1905311443:  // dayCount
          return ((ResolvedCapitalIndexedBond) bean).getDayCount();
        case -1895216418:  // yieldConvention
          return ((ResolvedCapitalIndexedBond) bean).getYieldConvention();
        case 866287159:  // legalEntityId
          return ((ResolvedCapitalIndexedBond) bean).getLegalEntityId();
        case 135924714:  // settlementDateOffset
          return ((ResolvedCapitalIndexedBond) bean).getSettlementDateOffset();
        case -521703991:  // rateCalculation
          return ((ResolvedCapitalIndexedBond) bean).getRateCalculation();
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
   * The bean-builder for {@code ResolvedCapitalIndexedBond}.
   */
  public static final class Builder extends DirectFieldsBeanBuilder<ResolvedCapitalIndexedBond> {

    private SecurityId securityId;
    private CapitalIndexedBondPaymentPeriod nominalPayment;
    private List<CapitalIndexedBondPaymentPeriod> periodicPayments = ImmutableList.of();
    private Frequency frequency;
    private RollConvention rollConvention;
    private DayCount dayCount;
    private CapitalIndexedBondYieldConvention yieldConvention;
    private StandardId legalEntityId;
    private DaysAdjustment settlementDateOffset;
    private InflationRateCalculation rateCalculation;

    /**
     * Restricted constructor.
     */
    private Builder() {
    }

    /**
     * Restricted copy constructor.
     * @param beanToCopy  the bean to copy from, not null
     */
    private Builder(ResolvedCapitalIndexedBond beanToCopy) {
      this.securityId = beanToCopy.getSecurityId();
      this.nominalPayment = beanToCopy.getNominalPayment();
      this.periodicPayments = beanToCopy.getPeriodicPayments();
      this.frequency = beanToCopy.getFrequency();
      this.rollConvention = beanToCopy.getRollConvention();
      this.dayCount = beanToCopy.getDayCount();
      this.yieldConvention = beanToCopy.getYieldConvention();
      this.legalEntityId = beanToCopy.getLegalEntityId();
      this.settlementDateOffset = beanToCopy.getSettlementDateOffset();
      this.rateCalculation = beanToCopy.getRateCalculation();
    }

    //-----------------------------------------------------------------------
    @Override
    public Object get(String propertyName) {
      switch (propertyName.hashCode()) {
        case 1574023291:  // securityId
          return securityId;
        case -44199542:  // nominalPayment
          return nominalPayment;
        case -367345944:  // periodicPayments
          return periodicPayments;
        case -70023844:  // frequency
          return frequency;
        case -10223666:  // rollConvention
          return rollConvention;
        case 1905311443:  // dayCount
          return dayCount;
        case -1895216418:  // yieldConvention
          return yieldConvention;
        case 866287159:  // legalEntityId
          return legalEntityId;
        case 135924714:  // settlementDateOffset
          return settlementDateOffset;
        case -521703991:  // rateCalculation
          return rateCalculation;
        default:
          throw new NoSuchElementException("Unknown property: " + propertyName);
      }
    }

    @SuppressWarnings("unchecked")
    @Override
    public Builder set(String propertyName, Object newValue) {
      switch (propertyName.hashCode()) {
        case 1574023291:  // securityId
          this.securityId = (SecurityId) newValue;
          break;
        case -44199542:  // nominalPayment
          this.nominalPayment = (CapitalIndexedBondPaymentPeriod) newValue;
          break;
        case -367345944:  // periodicPayments
          this.periodicPayments = (List<CapitalIndexedBondPaymentPeriod>) newValue;
          break;
        case -70023844:  // frequency
          this.frequency = (Frequency) newValue;
          break;
        case -10223666:  // rollConvention
          this.rollConvention = (RollConvention) newValue;
          break;
        case 1905311443:  // dayCount
          this.dayCount = (DayCount) newValue;
          break;
        case -1895216418:  // yieldConvention
          this.yieldConvention = (CapitalIndexedBondYieldConvention) newValue;
          break;
        case 866287159:  // legalEntityId
          this.legalEntityId = (StandardId) newValue;
          break;
        case 135924714:  // settlementDateOffset
          this.settlementDateOffset = (DaysAdjustment) newValue;
          break;
        case -521703991:  // rateCalculation
          this.rateCalculation = (InflationRateCalculation) newValue;
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
    public ResolvedCapitalIndexedBond build() {
      return new ResolvedCapitalIndexedBond(
          securityId,
          nominalPayment,
          periodicPayments,
          frequency,
          rollConvention,
          dayCount,
          yieldConvention,
          legalEntityId,
          settlementDateOffset,
          rateCalculation);
    }

    //-----------------------------------------------------------------------
    /**
     * Sets the security identifier.
     * <p>
     * This identifier uniquely identifies the security within the system.
     * @param securityId  the new value, not null
     * @return this, for chaining, not null
     */
    public Builder securityId(SecurityId securityId) {
      JodaBeanUtils.notNull(securityId, "securityId");
      this.securityId = securityId;
      return this;
    }

    /**
     * Sets the nominal payment of the product.
     * <p>
     * The payment date of the nominal payment agrees with the final coupon payment date of the periodic payments.
     * @param nominalPayment  the new value, not null
     * @return this, for chaining, not null
     */
    public Builder nominalPayment(CapitalIndexedBondPaymentPeriod nominalPayment) {
      JodaBeanUtils.notNull(nominalPayment, "nominalPayment");
      this.nominalPayment = nominalPayment;
      return this;
    }

    /**
     * Sets the periodic payments of the product.
     * <p>
     * Each payment period represents part of the life-time of the product.
     * The start date and end date of the leg are determined from the first and last period.
     * As such, the periods should be sorted.
     * @param periodicPayments  the new value, not null
     * @return this, for chaining, not null
     */
    public Builder periodicPayments(List<CapitalIndexedBondPaymentPeriod> periodicPayments) {
      JodaBeanUtils.notNull(periodicPayments, "periodicPayments");
      this.periodicPayments = periodicPayments;
      return this;
    }

    /**
     * Sets the {@code periodicPayments} property in the builder
     * from an array of objects.
     * @param periodicPayments  the new value, not null
     * @return this, for chaining, not null
     */
    public Builder periodicPayments(CapitalIndexedBondPaymentPeriod... periodicPayments) {
      return periodicPayments(ImmutableList.copyOf(periodicPayments));
    }

    /**
     * Sets the frequency of the bond payments.
     * <p>
     * This must match the frequency used to generate the payment schedule.
     * @param frequency  the new value, not null
     * @return this, for chaining, not null
     */
    public Builder frequency(Frequency frequency) {
      JodaBeanUtils.notNull(frequency, "frequency");
      this.frequency = frequency;
      return this;
    }

    /**
     * Sets the roll convention of the bond payments.
     * <p>
     * This must match the convention used to generate the payment schedule.
     * @param rollConvention  the new value, not null
     * @return this, for chaining, not null
     */
    public Builder rollConvention(RollConvention rollConvention) {
      JodaBeanUtils.notNull(rollConvention, "rollConvention");
      this.rollConvention = rollConvention;
      return this;
    }

    /**
     * Sets the day count convention applicable.
     * <p>
     * The conversion from dates to a numerical value is made based on this day count.
     * For the inflation-indexed bond, the day count convention is used to compute accrued interest.
     * @param dayCount  the new value, not null
     * @return this, for chaining, not null
     */
    public Builder dayCount(DayCount dayCount) {
      JodaBeanUtils.notNull(dayCount, "dayCount");
      this.dayCount = dayCount;
      return this;
    }

    /**
     * Sets yield convention.
     * <p>
     * The convention defines how to convert from yield to price and inversely.
     * @param yieldConvention  the new value, not null
     * @return this, for chaining, not null
     */
    public Builder yieldConvention(CapitalIndexedBondYieldConvention yieldConvention) {
      JodaBeanUtils.notNull(yieldConvention, "yieldConvention");
      this.yieldConvention = yieldConvention;
      return this;
    }

    /**
     * Sets the legal entity identifier.
     * <p>
     * This identifier is used for the legal entity that issues the bond.
     * @param legalEntityId  the new value, not null
     * @return this, for chaining, not null
     */
    public Builder legalEntityId(StandardId legalEntityId) {
      JodaBeanUtils.notNull(legalEntityId, "legalEntityId");
      this.legalEntityId = legalEntityId;
      return this;
    }

    /**
     * Sets the number of days between valuation date and settlement date.
     * <p>
     * This is used to compute clean price.
     * The clean price is the relative price to be paid at the standard settlement date in exchange for the bond.
     * @param settlementDateOffset  the new value, not null
     * @return this, for chaining, not null
     */
    public Builder settlementDateOffset(DaysAdjustment settlementDateOffset) {
      JodaBeanUtils.notNull(settlementDateOffset, "settlementDateOffset");
      this.settlementDateOffset = settlementDateOffset;
      return this;
    }

    /**
     * Sets the inflation rate calculation.
     * <p>
     * The reference index is interpolated index or monthly index.
     * Real coupons are represented by {@code gearing} in the calculation.
     * The price index value at the start of the bond is represented by {@code firstIndexValue} in the calculation.
     * @param rateCalculation  the new value, not null
     * @return this, for chaining, not null
     */
    public Builder rateCalculation(InflationRateCalculation rateCalculation) {
      JodaBeanUtils.notNull(rateCalculation, "rateCalculation");
      this.rateCalculation = rateCalculation;
      return this;
    }

    //-----------------------------------------------------------------------
    @Override
    public String toString() {
      StringBuilder buf = new StringBuilder(352);
      buf.append("ResolvedCapitalIndexedBond.Builder{");
      buf.append("securityId").append('=').append(JodaBeanUtils.toString(securityId)).append(',').append(' ');
      buf.append("nominalPayment").append('=').append(JodaBeanUtils.toString(nominalPayment)).append(',').append(' ');
      buf.append("periodicPayments").append('=').append(JodaBeanUtils.toString(periodicPayments)).append(',').append(' ');
      buf.append("frequency").append('=').append(JodaBeanUtils.toString(frequency)).append(',').append(' ');
      buf.append("rollConvention").append('=').append(JodaBeanUtils.toString(rollConvention)).append(',').append(' ');
      buf.append("dayCount").append('=').append(JodaBeanUtils.toString(dayCount)).append(',').append(' ');
      buf.append("yieldConvention").append('=').append(JodaBeanUtils.toString(yieldConvention)).append(',').append(' ');
      buf.append("legalEntityId").append('=').append(JodaBeanUtils.toString(legalEntityId)).append(',').append(' ');
      buf.append("settlementDateOffset").append('=').append(JodaBeanUtils.toString(settlementDateOffset)).append(',').append(' ');
      buf.append("rateCalculation").append('=').append(JodaBeanUtils.toString(rateCalculation));
      buf.append('}');
      return buf.toString();
    }

  }

  ///CLOVER:ON
  //-------------------------- AUTOGENERATED END --------------------------
}
