/*
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.product.bond;

import static com.opengamma.strata.basics.value.ValueSchedule.ALWAYS_1;

import java.io.Serializable;
import java.util.Map;
import java.util.NoSuchElementException;
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

import com.google.common.collect.ImmutableList;
import com.opengamma.strata.basics.ReferenceData;
import com.opengamma.strata.basics.Resolvable;
import com.opengamma.strata.basics.StandardId;
import com.opengamma.strata.basics.currency.Currency;
import com.opengamma.strata.basics.date.DateAdjuster;
import com.opengamma.strata.basics.date.DayCount;
import com.opengamma.strata.basics.date.DaysAdjustment;
import com.opengamma.strata.basics.schedule.PeriodicSchedule;
import com.opengamma.strata.basics.schedule.Schedule;
import com.opengamma.strata.basics.schedule.SchedulePeriod;
import com.opengamma.strata.collect.ArgChecker;
import com.opengamma.strata.collect.array.DoubleArray;
import com.opengamma.strata.product.SecuritizedProduct;
import com.opengamma.strata.product.SecurityId;
import com.opengamma.strata.product.rate.RateComputation;
import com.opengamma.strata.product.swap.InflationRateCalculation;

/**
 * A capital indexed bond.
 * <p>
 * A capital indexed bond is a financial instrument that represents a stream of inflation-adjusted payments.
 * The payments consist two types: periodic coupon payments and nominal payment.
 * All of the payments are adjusted for inflation.
 * <p>
 * The periodic coupon payment schedule is defined using {@code periodicSchedule}.
 * The payment amount will be computed based on this schedule and {@link RateComputation} of {@code InflationRateCalculation}.
 * The nominal payment is defined from the last period of the periodic coupon payment schedule.
 * <p>
 * The legal entity of this bond is identified by {@code legalEntityId}.
 * The enum, {@code yieldConvention}, specifies the yield computation convention.
 * The accrued interest must be computed with {@code dayCount}.
 * 
 * <h4>Price</h4>
 * Strata uses <i>decimal prices</i> for bonds in the trade model, pricers and market data.
 * For example, a price of 99.32% is represented in Strata by 0.9932.
 */
@BeanDefinition(constructorScope = "package")
public final class CapitalIndexedBond
    implements SecuritizedProduct, Resolvable<ResolvedCapitalIndexedBond>, ImmutableBean, Serializable {

  /**
   * The security identifier.
   * <p>
   * This identifier uniquely identifies the security within the system.
   */
  @PropertyDefinition(validate = "notNull", overrideGet = true)
  private final SecurityId securityId;
  /**
   * The currency that the bond is traded in.
   */
  @PropertyDefinition(validate = "notNull", overrideGet = true)
  private final Currency currency;
  /**
   * The notional amount, must be positive.
   * <p>
   * The notional expressed here must be positive.
   * The currency of the notional is specified by {@code currency}.
   */
  @PropertyDefinition(validate = "ArgChecker.notNegativeOrZero")
  private final double notional;
  /**
   * The accrual schedule.
   * <p>
   * This is used to define the accrual periods.
   * These are used directly or indirectly to determine other dates in the product.
   */
  @PropertyDefinition(validate = "notNull")
  private final PeriodicSchedule accrualSchedule;
  /**
   * The inflation rate calculation.
   * <p>
   * The reference index is interpolated index or monthly index.
   * Real coupons are represented by {@code gearing} in the calculation.
   * The price index value at the start of the bond is represented by {@code firstIndexValue} in the calculation.
   */
  @PropertyDefinition(validate = "notNull")
  private final InflationRateCalculation rateCalculation;
  /**
   * The day count convention applicable.
   * <p>
   * The conversion from dates to a numerical value is made based on this day count.
   * For the inflation-indexed bond, the day count convention is used to compute accrued interest.
   * <p>
   * Note that the year fraction of a coupon payment is computed based on the unadjusted
   * dates in the schedule.
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
   * Ex-coupon period.
   * <p>
   * Some bonds trade ex-coupons before the coupon payment. The coupon is paid not to the
   * owner of the bond on the payment date but to the owner of the bond on the detachment date.
   * The difference between the two is the ex-coupon period (measured in days).
   * <p>
   * Because the detachment date is not after the coupon date, the number of days
   * stored in this field should be zero or negative.
   */
  @PropertyDefinition(validate = "notNull")
  private final DaysAdjustment exCouponPeriod;

  //-------------------------------------------------------------------------
  @ImmutableDefaults
  private static void applyDefaults(Builder builder) {
    builder.exCouponPeriod = DaysAdjustment.NONE;
  }

  @ImmutableValidator
  private void validate() {
    ArgChecker.isTrue(settlementDateOffset.getDays() >= 0, "The settlement date offset must be non-negative");
    ArgChecker.isTrue(exCouponPeriod.getDays() <= 0,
        "The ex-coupon period is measured from the payment date, thus the days must be non-positive");
    ArgChecker.isTrue(rateCalculation.getFirstIndexValue().isPresent(), "Rate calculation must specify first index value");
  }

  //-------------------------------------------------------------------------
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
  @Override
  public ResolvedCapitalIndexedBond resolve(ReferenceData refData) {
    Schedule adjustedSchedule = accrualSchedule.createSchedule(refData);
    DateAdjuster exCouponPeriodAdjuster = exCouponPeriod.resolve(refData);
    DoubleArray resolvedGearings =
        rateCalculation.getGearing().orElse(ALWAYS_1).resolveValues(adjustedSchedule);
    ImmutableList.Builder<CapitalIndexedBondPaymentPeriod> bondPeriodsBuilder = ImmutableList.builder();
    // coupon payments
    for (int i = 0; i < adjustedSchedule.size(); i++) {
      SchedulePeriod period = adjustedSchedule.getPeriod(i);
      bondPeriodsBuilder.add(CapitalIndexedBondPaymentPeriod.builder()
          .unadjustedStartDate(period.getUnadjustedStartDate())
          .unadjustedEndDate(period.getUnadjustedEndDate())
          .startDate(period.getStartDate())
          .endDate(period.getEndDate())
          .detachmentDate(exCouponPeriodAdjuster.adjust(period.getEndDate()))
          .notional(notional)
          .currency(currency)
          .rateComputation(rateCalculation.createRateComputation(period.getEndDate()))
          .realCoupon(resolvedGearings.get(i))
          .build());
    }
    ImmutableList<CapitalIndexedBondPaymentPeriod> bondPeriods = bondPeriodsBuilder.build();
    // nominal payment
    CapitalIndexedBondPaymentPeriod nominalPayment = bondPeriods.get(bondPeriods.size() - 1)
        .withUnitCoupon(bondPeriods.get(0).getStartDate(), bondPeriods.get(0).getUnadjustedStartDate());
    return ResolvedCapitalIndexedBond.builder()
        .securityId(securityId)
        .periodicPayments(ImmutableList.copyOf(bondPeriods))
        .frequency(accrualSchedule.getFrequency())
        .rollConvention(accrualSchedule.calculatedRollConvention())
        .dayCount(dayCount)
        .yieldConvention(yieldConvention)
        .settlementDateOffset(settlementDateOffset)
        .legalEntityId(legalEntityId)
        .nominalPayment(nominalPayment)
        .rateCalculation(rateCalculation)
        .build();
  }

  //------------------------- AUTOGENERATED START -------------------------
  ///CLOVER:OFF
  /**
   * The meta-bean for {@code CapitalIndexedBond}.
   * @return the meta-bean, not null
   */
  public static CapitalIndexedBond.Meta meta() {
    return CapitalIndexedBond.Meta.INSTANCE;
  }

  static {
    JodaBeanUtils.registerMetaBean(CapitalIndexedBond.Meta.INSTANCE);
  }

  /**
   * The serialization version id.
   */
  private static final long serialVersionUID = 1L;

  /**
   * Returns a builder used to create an instance of the bean.
   * @return the builder, not null
   */
  public static CapitalIndexedBond.Builder builder() {
    return new CapitalIndexedBond.Builder();
  }

  /**
   * Creates an instance.
   * @param securityId  the value of the property, not null
   * @param currency  the value of the property, not null
   * @param notional  the value of the property
   * @param accrualSchedule  the value of the property, not null
   * @param rateCalculation  the value of the property, not null
   * @param dayCount  the value of the property, not null
   * @param yieldConvention  the value of the property, not null
   * @param legalEntityId  the value of the property, not null
   * @param settlementDateOffset  the value of the property, not null
   * @param exCouponPeriod  the value of the property, not null
   */
  CapitalIndexedBond(
      SecurityId securityId,
      Currency currency,
      double notional,
      PeriodicSchedule accrualSchedule,
      InflationRateCalculation rateCalculation,
      DayCount dayCount,
      CapitalIndexedBondYieldConvention yieldConvention,
      StandardId legalEntityId,
      DaysAdjustment settlementDateOffset,
      DaysAdjustment exCouponPeriod) {
    JodaBeanUtils.notNull(securityId, "securityId");
    JodaBeanUtils.notNull(currency, "currency");
    ArgChecker.notNegativeOrZero(notional, "notional");
    JodaBeanUtils.notNull(accrualSchedule, "accrualSchedule");
    JodaBeanUtils.notNull(rateCalculation, "rateCalculation");
    JodaBeanUtils.notNull(dayCount, "dayCount");
    JodaBeanUtils.notNull(yieldConvention, "yieldConvention");
    JodaBeanUtils.notNull(legalEntityId, "legalEntityId");
    JodaBeanUtils.notNull(settlementDateOffset, "settlementDateOffset");
    JodaBeanUtils.notNull(exCouponPeriod, "exCouponPeriod");
    this.securityId = securityId;
    this.currency = currency;
    this.notional = notional;
    this.accrualSchedule = accrualSchedule;
    this.rateCalculation = rateCalculation;
    this.dayCount = dayCount;
    this.yieldConvention = yieldConvention;
    this.legalEntityId = legalEntityId;
    this.settlementDateOffset = settlementDateOffset;
    this.exCouponPeriod = exCouponPeriod;
    validate();
  }

  @Override
  public CapitalIndexedBond.Meta metaBean() {
    return CapitalIndexedBond.Meta.INSTANCE;
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
  @Override
  public SecurityId getSecurityId() {
    return securityId;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the currency that the bond is traded in.
   * @return the value of the property, not null
   */
  @Override
  public Currency getCurrency() {
    return currency;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the notional amount, must be positive.
   * <p>
   * The notional expressed here must be positive.
   * The currency of the notional is specified by {@code currency}.
   * @return the value of the property
   */
  public double getNotional() {
    return notional;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the accrual schedule.
   * <p>
   * This is used to define the accrual periods.
   * These are used directly or indirectly to determine other dates in the product.
   * @return the value of the property, not null
   */
  public PeriodicSchedule getAccrualSchedule() {
    return accrualSchedule;
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
   * Gets the day count convention applicable.
   * <p>
   * The conversion from dates to a numerical value is made based on this day count.
   * For the inflation-indexed bond, the day count convention is used to compute accrued interest.
   * <p>
   * Note that the year fraction of a coupon payment is computed based on the unadjusted
   * dates in the schedule.
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
   * Gets ex-coupon period.
   * <p>
   * Some bonds trade ex-coupons before the coupon payment. The coupon is paid not to the
   * owner of the bond on the payment date but to the owner of the bond on the detachment date.
   * The difference between the two is the ex-coupon period (measured in days).
   * <p>
   * Because the detachment date is not after the coupon date, the number of days
   * stored in this field should be zero or negative.
   * @return the value of the property, not null
   */
  public DaysAdjustment getExCouponPeriod() {
    return exCouponPeriod;
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
      CapitalIndexedBond other = (CapitalIndexedBond) obj;
      return JodaBeanUtils.equal(securityId, other.securityId) &&
          JodaBeanUtils.equal(currency, other.currency) &&
          JodaBeanUtils.equal(notional, other.notional) &&
          JodaBeanUtils.equal(accrualSchedule, other.accrualSchedule) &&
          JodaBeanUtils.equal(rateCalculation, other.rateCalculation) &&
          JodaBeanUtils.equal(dayCount, other.dayCount) &&
          JodaBeanUtils.equal(yieldConvention, other.yieldConvention) &&
          JodaBeanUtils.equal(legalEntityId, other.legalEntityId) &&
          JodaBeanUtils.equal(settlementDateOffset, other.settlementDateOffset) &&
          JodaBeanUtils.equal(exCouponPeriod, other.exCouponPeriod);
    }
    return false;
  }

  @Override
  public int hashCode() {
    int hash = getClass().hashCode();
    hash = hash * 31 + JodaBeanUtils.hashCode(securityId);
    hash = hash * 31 + JodaBeanUtils.hashCode(currency);
    hash = hash * 31 + JodaBeanUtils.hashCode(notional);
    hash = hash * 31 + JodaBeanUtils.hashCode(accrualSchedule);
    hash = hash * 31 + JodaBeanUtils.hashCode(rateCalculation);
    hash = hash * 31 + JodaBeanUtils.hashCode(dayCount);
    hash = hash * 31 + JodaBeanUtils.hashCode(yieldConvention);
    hash = hash * 31 + JodaBeanUtils.hashCode(legalEntityId);
    hash = hash * 31 + JodaBeanUtils.hashCode(settlementDateOffset);
    hash = hash * 31 + JodaBeanUtils.hashCode(exCouponPeriod);
    return hash;
  }

  @Override
  public String toString() {
    StringBuilder buf = new StringBuilder(352);
    buf.append("CapitalIndexedBond{");
    buf.append("securityId").append('=').append(securityId).append(',').append(' ');
    buf.append("currency").append('=').append(currency).append(',').append(' ');
    buf.append("notional").append('=').append(notional).append(',').append(' ');
    buf.append("accrualSchedule").append('=').append(accrualSchedule).append(',').append(' ');
    buf.append("rateCalculation").append('=').append(rateCalculation).append(',').append(' ');
    buf.append("dayCount").append('=').append(dayCount).append(',').append(' ');
    buf.append("yieldConvention").append('=').append(yieldConvention).append(',').append(' ');
    buf.append("legalEntityId").append('=').append(legalEntityId).append(',').append(' ');
    buf.append("settlementDateOffset").append('=').append(settlementDateOffset).append(',').append(' ');
    buf.append("exCouponPeriod").append('=').append(JodaBeanUtils.toString(exCouponPeriod));
    buf.append('}');
    return buf.toString();
  }

  //-----------------------------------------------------------------------
  /**
   * The meta-bean for {@code CapitalIndexedBond}.
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
        this, "securityId", CapitalIndexedBond.class, SecurityId.class);
    /**
     * The meta-property for the {@code currency} property.
     */
    private final MetaProperty<Currency> currency = DirectMetaProperty.ofImmutable(
        this, "currency", CapitalIndexedBond.class, Currency.class);
    /**
     * The meta-property for the {@code notional} property.
     */
    private final MetaProperty<Double> notional = DirectMetaProperty.ofImmutable(
        this, "notional", CapitalIndexedBond.class, Double.TYPE);
    /**
     * The meta-property for the {@code accrualSchedule} property.
     */
    private final MetaProperty<PeriodicSchedule> accrualSchedule = DirectMetaProperty.ofImmutable(
        this, "accrualSchedule", CapitalIndexedBond.class, PeriodicSchedule.class);
    /**
     * The meta-property for the {@code rateCalculation} property.
     */
    private final MetaProperty<InflationRateCalculation> rateCalculation = DirectMetaProperty.ofImmutable(
        this, "rateCalculation", CapitalIndexedBond.class, InflationRateCalculation.class);
    /**
     * The meta-property for the {@code dayCount} property.
     */
    private final MetaProperty<DayCount> dayCount = DirectMetaProperty.ofImmutable(
        this, "dayCount", CapitalIndexedBond.class, DayCount.class);
    /**
     * The meta-property for the {@code yieldConvention} property.
     */
    private final MetaProperty<CapitalIndexedBondYieldConvention> yieldConvention = DirectMetaProperty.ofImmutable(
        this, "yieldConvention", CapitalIndexedBond.class, CapitalIndexedBondYieldConvention.class);
    /**
     * The meta-property for the {@code legalEntityId} property.
     */
    private final MetaProperty<StandardId> legalEntityId = DirectMetaProperty.ofImmutable(
        this, "legalEntityId", CapitalIndexedBond.class, StandardId.class);
    /**
     * The meta-property for the {@code settlementDateOffset} property.
     */
    private final MetaProperty<DaysAdjustment> settlementDateOffset = DirectMetaProperty.ofImmutable(
        this, "settlementDateOffset", CapitalIndexedBond.class, DaysAdjustment.class);
    /**
     * The meta-property for the {@code exCouponPeriod} property.
     */
    private final MetaProperty<DaysAdjustment> exCouponPeriod = DirectMetaProperty.ofImmutable(
        this, "exCouponPeriod", CapitalIndexedBond.class, DaysAdjustment.class);
    /**
     * The meta-properties.
     */
    private final Map<String, MetaProperty<?>> metaPropertyMap$ = new DirectMetaPropertyMap(
        this, null,
        "securityId",
        "currency",
        "notional",
        "accrualSchedule",
        "rateCalculation",
        "dayCount",
        "yieldConvention",
        "legalEntityId",
        "settlementDateOffset",
        "exCouponPeriod");

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
        case 575402001:  // currency
          return currency;
        case 1585636160:  // notional
          return notional;
        case 304659814:  // accrualSchedule
          return accrualSchedule;
        case -521703991:  // rateCalculation
          return rateCalculation;
        case 1905311443:  // dayCount
          return dayCount;
        case -1895216418:  // yieldConvention
          return yieldConvention;
        case 866287159:  // legalEntityId
          return legalEntityId;
        case 135924714:  // settlementDateOffset
          return settlementDateOffset;
        case 1408037338:  // exCouponPeriod
          return exCouponPeriod;
      }
      return super.metaPropertyGet(propertyName);
    }

    @Override
    public CapitalIndexedBond.Builder builder() {
      return new CapitalIndexedBond.Builder();
    }

    @Override
    public Class<? extends CapitalIndexedBond> beanType() {
      return CapitalIndexedBond.class;
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
     * The meta-property for the {@code currency} property.
     * @return the meta-property, not null
     */
    public MetaProperty<Currency> currency() {
      return currency;
    }

    /**
     * The meta-property for the {@code notional} property.
     * @return the meta-property, not null
     */
    public MetaProperty<Double> notional() {
      return notional;
    }

    /**
     * The meta-property for the {@code accrualSchedule} property.
     * @return the meta-property, not null
     */
    public MetaProperty<PeriodicSchedule> accrualSchedule() {
      return accrualSchedule;
    }

    /**
     * The meta-property for the {@code rateCalculation} property.
     * @return the meta-property, not null
     */
    public MetaProperty<InflationRateCalculation> rateCalculation() {
      return rateCalculation;
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
     * The meta-property for the {@code exCouponPeriod} property.
     * @return the meta-property, not null
     */
    public MetaProperty<DaysAdjustment> exCouponPeriod() {
      return exCouponPeriod;
    }

    //-----------------------------------------------------------------------
    @Override
    protected Object propertyGet(Bean bean, String propertyName, boolean quiet) {
      switch (propertyName.hashCode()) {
        case 1574023291:  // securityId
          return ((CapitalIndexedBond) bean).getSecurityId();
        case 575402001:  // currency
          return ((CapitalIndexedBond) bean).getCurrency();
        case 1585636160:  // notional
          return ((CapitalIndexedBond) bean).getNotional();
        case 304659814:  // accrualSchedule
          return ((CapitalIndexedBond) bean).getAccrualSchedule();
        case -521703991:  // rateCalculation
          return ((CapitalIndexedBond) bean).getRateCalculation();
        case 1905311443:  // dayCount
          return ((CapitalIndexedBond) bean).getDayCount();
        case -1895216418:  // yieldConvention
          return ((CapitalIndexedBond) bean).getYieldConvention();
        case 866287159:  // legalEntityId
          return ((CapitalIndexedBond) bean).getLegalEntityId();
        case 135924714:  // settlementDateOffset
          return ((CapitalIndexedBond) bean).getSettlementDateOffset();
        case 1408037338:  // exCouponPeriod
          return ((CapitalIndexedBond) bean).getExCouponPeriod();
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
   * The bean-builder for {@code CapitalIndexedBond}.
   */
  public static final class Builder extends DirectFieldsBeanBuilder<CapitalIndexedBond> {

    private SecurityId securityId;
    private Currency currency;
    private double notional;
    private PeriodicSchedule accrualSchedule;
    private InflationRateCalculation rateCalculation;
    private DayCount dayCount;
    private CapitalIndexedBondYieldConvention yieldConvention;
    private StandardId legalEntityId;
    private DaysAdjustment settlementDateOffset;
    private DaysAdjustment exCouponPeriod;

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
    private Builder(CapitalIndexedBond beanToCopy) {
      this.securityId = beanToCopy.getSecurityId();
      this.currency = beanToCopy.getCurrency();
      this.notional = beanToCopy.getNotional();
      this.accrualSchedule = beanToCopy.getAccrualSchedule();
      this.rateCalculation = beanToCopy.getRateCalculation();
      this.dayCount = beanToCopy.getDayCount();
      this.yieldConvention = beanToCopy.getYieldConvention();
      this.legalEntityId = beanToCopy.getLegalEntityId();
      this.settlementDateOffset = beanToCopy.getSettlementDateOffset();
      this.exCouponPeriod = beanToCopy.getExCouponPeriod();
    }

    //-----------------------------------------------------------------------
    @Override
    public Object get(String propertyName) {
      switch (propertyName.hashCode()) {
        case 1574023291:  // securityId
          return securityId;
        case 575402001:  // currency
          return currency;
        case 1585636160:  // notional
          return notional;
        case 304659814:  // accrualSchedule
          return accrualSchedule;
        case -521703991:  // rateCalculation
          return rateCalculation;
        case 1905311443:  // dayCount
          return dayCount;
        case -1895216418:  // yieldConvention
          return yieldConvention;
        case 866287159:  // legalEntityId
          return legalEntityId;
        case 135924714:  // settlementDateOffset
          return settlementDateOffset;
        case 1408037338:  // exCouponPeriod
          return exCouponPeriod;
        default:
          throw new NoSuchElementException("Unknown property: " + propertyName);
      }
    }

    @Override
    public Builder set(String propertyName, Object newValue) {
      switch (propertyName.hashCode()) {
        case 1574023291:  // securityId
          this.securityId = (SecurityId) newValue;
          break;
        case 575402001:  // currency
          this.currency = (Currency) newValue;
          break;
        case 1585636160:  // notional
          this.notional = (Double) newValue;
          break;
        case 304659814:  // accrualSchedule
          this.accrualSchedule = (PeriodicSchedule) newValue;
          break;
        case -521703991:  // rateCalculation
          this.rateCalculation = (InflationRateCalculation) newValue;
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
        case 1408037338:  // exCouponPeriod
          this.exCouponPeriod = (DaysAdjustment) newValue;
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
    public CapitalIndexedBond build() {
      return new CapitalIndexedBond(
          securityId,
          currency,
          notional,
          accrualSchedule,
          rateCalculation,
          dayCount,
          yieldConvention,
          legalEntityId,
          settlementDateOffset,
          exCouponPeriod);
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
     * Sets the currency that the bond is traded in.
     * @param currency  the new value, not null
     * @return this, for chaining, not null
     */
    public Builder currency(Currency currency) {
      JodaBeanUtils.notNull(currency, "currency");
      this.currency = currency;
      return this;
    }

    /**
     * Sets the notional amount, must be positive.
     * <p>
     * The notional expressed here must be positive.
     * The currency of the notional is specified by {@code currency}.
     * @param notional  the new value
     * @return this, for chaining, not null
     */
    public Builder notional(double notional) {
      ArgChecker.notNegativeOrZero(notional, "notional");
      this.notional = notional;
      return this;
    }

    /**
     * Sets the accrual schedule.
     * <p>
     * This is used to define the accrual periods.
     * These are used directly or indirectly to determine other dates in the product.
     * @param accrualSchedule  the new value, not null
     * @return this, for chaining, not null
     */
    public Builder accrualSchedule(PeriodicSchedule accrualSchedule) {
      JodaBeanUtils.notNull(accrualSchedule, "accrualSchedule");
      this.accrualSchedule = accrualSchedule;
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

    /**
     * Sets the day count convention applicable.
     * <p>
     * The conversion from dates to a numerical value is made based on this day count.
     * For the inflation-indexed bond, the day count convention is used to compute accrued interest.
     * <p>
     * Note that the year fraction of a coupon payment is computed based on the unadjusted
     * dates in the schedule.
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
     * Sets ex-coupon period.
     * <p>
     * Some bonds trade ex-coupons before the coupon payment. The coupon is paid not to the
     * owner of the bond on the payment date but to the owner of the bond on the detachment date.
     * The difference between the two is the ex-coupon period (measured in days).
     * <p>
     * Because the detachment date is not after the coupon date, the number of days
     * stored in this field should be zero or negative.
     * @param exCouponPeriod  the new value, not null
     * @return this, for chaining, not null
     */
    public Builder exCouponPeriod(DaysAdjustment exCouponPeriod) {
      JodaBeanUtils.notNull(exCouponPeriod, "exCouponPeriod");
      this.exCouponPeriod = exCouponPeriod;
      return this;
    }

    //-----------------------------------------------------------------------
    @Override
    public String toString() {
      StringBuilder buf = new StringBuilder(352);
      buf.append("CapitalIndexedBond.Builder{");
      buf.append("securityId").append('=').append(JodaBeanUtils.toString(securityId)).append(',').append(' ');
      buf.append("currency").append('=').append(JodaBeanUtils.toString(currency)).append(',').append(' ');
      buf.append("notional").append('=').append(JodaBeanUtils.toString(notional)).append(',').append(' ');
      buf.append("accrualSchedule").append('=').append(JodaBeanUtils.toString(accrualSchedule)).append(',').append(' ');
      buf.append("rateCalculation").append('=').append(JodaBeanUtils.toString(rateCalculation)).append(',').append(' ');
      buf.append("dayCount").append('=').append(JodaBeanUtils.toString(dayCount)).append(',').append(' ');
      buf.append("yieldConvention").append('=').append(JodaBeanUtils.toString(yieldConvention)).append(',').append(' ');
      buf.append("legalEntityId").append('=').append(JodaBeanUtils.toString(legalEntityId)).append(',').append(' ');
      buf.append("settlementDateOffset").append('=').append(JodaBeanUtils.toString(settlementDateOffset)).append(',').append(' ');
      buf.append("exCouponPeriod").append('=').append(JodaBeanUtils.toString(exCouponPeriod));
      buf.append('}');
      return buf.toString();
    }

  }

  ///CLOVER:ON
  //-------------------------- AUTOGENERATED END --------------------------
}
