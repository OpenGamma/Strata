/**
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.product.bond;

import static com.opengamma.strata.basics.value.ValueSchedule.ALWAYS_1;

import java.io.Serializable;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;

import org.joda.beans.Bean;
import org.joda.beans.BeanDefinition;
import org.joda.beans.ImmutableBean;
import org.joda.beans.ImmutablePreBuild;
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
import com.opengamma.strata.basics.currency.Currency;
import com.opengamma.strata.basics.date.DayCount;
import com.opengamma.strata.basics.date.DaysAdjustment;
import com.opengamma.strata.basics.schedule.PeriodicSchedule;
import com.opengamma.strata.basics.schedule.Schedule;
import com.opengamma.strata.basics.schedule.SchedulePeriod;
import com.opengamma.strata.collect.ArgChecker;
import com.opengamma.strata.collect.id.StandardId;
import com.opengamma.strata.product.rate.RateObservation;
import com.opengamma.strata.product.swap.InflationRateCalculation;

/**
 * A capital indexed bond.
 * <p>
 * A capital indexed bond is a financial instrument that represents a stream of inflation-adjusted payments. 
 * The payments consist two types: periodic coupon payments and nominal payment.
 * All of the payments are adjusted for inflation. 
 * <p>
 * The periodic coupon payment schedule is defined using {@code periodicSchedule}. 
 * The payment amount will be computed based on this schedule and {@link RateObservation} of {@code InflationRateCalculation}. 
 * The nominal payment is defined from the last period of the periodic coupon payment schedule. 
 * <p>
 * The legal entity of this bond is identified by {@code legalEntityId}.
 * The enum, {@code yieldConvention}, specifies the yield computation convention.
 * The accrued interest must be computed with {@code dayCount}. 
 */
@BeanDefinition
public final class CapitalIndexedBond
    implements CapitalIndexedBondProduct, ImmutableBean, Serializable {

  /**
   * The primary currency of the product.
   * <p>
   * The amounts of the notional are usually expressed in terms of this currency,
   * however they can be converted from amounts in a different currency.
   */
  @PropertyDefinition(validate = "notNull")
  private final Currency currency;
  /**
   * The notional amount, must be positive. 
   * <p>
   * The notional expressed here must be positive.
   * The currency of the notional is specified by {@code currency}.
   */
  @PropertyDefinition(validate = "ArgChecker.notNegative")
  private final double notional;
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
   * The accrual schedule.
   * <p>
   * This is used to define the accrual periods.
   * These are used directly or indirectly to determine other dates in the product.
   */
  @PropertyDefinition(validate = "notNull")
  private final PeriodicSchedule periodicSchedule;
  /**
   * The inflation rate calculation.
   * <p>
   * The reference index is interpolated index or monthly index.
   * Real coupons are represented by {@code gearing} in this field.
   */
  @PropertyDefinition(validate = "notNull")
  private final InflationRateCalculation rateCalculation;
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
   * Because the detachment date is not after the coupon date, the number of days stored in this field 
   * should be zero or negative. 
   */
  @PropertyDefinition(validate = "notNull")
  private final DaysAdjustment exCouponPeriod;
  /**
   * The legal entity identifier.
   * <p>
   * This identifier is used for the legal entity which issues the coupon bond product. 
   */
  @PropertyDefinition(validate = "notNull")
  private final StandardId legalEntityId;
  /**
   * Yield convention.
   * <p>
   * The convention defines how to convert from yield to price and inversely.  
   */
  @PropertyDefinition(validate = "notNull")
  private final YieldConvention yieldConvention;
  /**
   * Start index value. 
   * <p>
   * The price index value at the start of the bond. 
   */
  @PropertyDefinition(validate = "ArgChecker.notNegativeOrZero")
  private final double startIndexValue;

  //-------------------------------------------------------------------------
  @ImmutablePreBuild
  private static void preBuild(Builder builder) {
    if (builder.exCouponPeriod == null) {
      builder.exCouponPeriod = DaysAdjustment.NONE;
    }
  }

  @ImmutableValidator
  private void validate() {
    ArgChecker.isTrue(settlementDateOffset.getDays() >= 0d, "The settlement date offset must be non-negative");
    ArgChecker.isTrue(exCouponPeriod.getDays() <= 0d,
        "The ex-coupon period is measured from the payment date, thus the days must be non-positive");
  }

  //-------------------------------------------------------------------------
  @Override
  public ExpandedCapitalIndexedBond expand() {
    Schedule schedule = periodicSchedule.createSchedule();
    List<Double> resolvedGearings =
        rateCalculation.getGearing().orElse(ALWAYS_1).resolveValues(schedule.getPeriods());
    ImmutableList.Builder<CapitalIndexedBondPaymentPeriod> bondPeriodsBuilder = ImmutableList.builder();
    // coupon payments
    for (int i = 0; i < schedule.size(); i++) {
      SchedulePeriod period = schedule.getPeriod(i);
      bondPeriodsBuilder.add(CapitalIndexedBondPaymentPeriod.builder()
          .unadjustedStartDate(period.getUnadjustedStartDate())
          .unadjustedEndDate(period.getUnadjustedEndDate())
          .startDate(period.getStartDate())
          .endDate(period.getEndDate())
          .detachmentDate(exCouponPeriod.adjust(period.getEndDate()))
          .notional(notional)
          .currency(currency)
          .rateObservation(rateCalculation.createRateObservation(period.getEndDate(), startIndexValue))
          .realCoupon(resolvedGearings.get(i))
          .build());
    }
    ImmutableList<CapitalIndexedBondPaymentPeriod> bondPeriods = bondPeriodsBuilder.build();
    // nominal payment
    CapitalIndexedBondPaymentPeriod nominalPayment = bondPeriods.get(bondPeriods.size() - 1)
        .withUnitCoupon(bondPeriods.get(0).getStartDate(), bondPeriods.get(0).getUnadjustedStartDate());
    return ExpandedCapitalIndexedBond.builder()
        .periodicPayments(ImmutableList.copyOf(bondPeriods))
        .dayCount(dayCount)
        .yieldConvention(yieldConvention)
        .settlementDateOffset(settlementDateOffset)
        .legalEntityId(legalEntityId)
        .nominalPayment(nominalPayment)
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

  private CapitalIndexedBond(
      Currency currency,
      double notional,
      DayCount dayCount,
      PeriodicSchedule periodicSchedule,
      InflationRateCalculation rateCalculation,
      DaysAdjustment settlementDateOffset,
      DaysAdjustment exCouponPeriod,
      StandardId legalEntityId,
      YieldConvention yieldConvention,
      double startIndexValue) {
    JodaBeanUtils.notNull(currency, "currency");
    ArgChecker.notNegative(notional, "notional");
    JodaBeanUtils.notNull(dayCount, "dayCount");
    JodaBeanUtils.notNull(periodicSchedule, "periodicSchedule");
    JodaBeanUtils.notNull(rateCalculation, "rateCalculation");
    JodaBeanUtils.notNull(settlementDateOffset, "settlementDateOffset");
    JodaBeanUtils.notNull(exCouponPeriod, "exCouponPeriod");
    JodaBeanUtils.notNull(legalEntityId, "legalEntityId");
    JodaBeanUtils.notNull(yieldConvention, "yieldConvention");
    ArgChecker.notNegativeOrZero(startIndexValue, "startIndexValue");
    this.currency = currency;
    this.notional = notional;
    this.dayCount = dayCount;
    this.periodicSchedule = periodicSchedule;
    this.rateCalculation = rateCalculation;
    this.settlementDateOffset = settlementDateOffset;
    this.exCouponPeriod = exCouponPeriod;
    this.legalEntityId = legalEntityId;
    this.yieldConvention = yieldConvention;
    this.startIndexValue = startIndexValue;
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
   * Gets the primary currency of the product.
   * <p>
   * The amounts of the notional are usually expressed in terms of this currency,
   * however they can be converted from amounts in a different currency.
   * @return the value of the property, not null
   */
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
   * Gets the accrual schedule.
   * <p>
   * This is used to define the accrual periods.
   * These are used directly or indirectly to determine other dates in the product.
   * @return the value of the property, not null
   */
  public PeriodicSchedule getPeriodicSchedule() {
    return periodicSchedule;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the inflation rate calculation.
   * <p>
   * The reference index is interpolated index or monthly index.
   * Real coupons are represented by {@code gearing} in this field.
   * @return the value of the property, not null
   */
  public InflationRateCalculation getRateCalculation() {
    return rateCalculation;
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
   * Because the detachment date is not after the coupon date, the number of days stored in this field
   * should be zero or negative.
   * @return the value of the property, not null
   */
  public DaysAdjustment getExCouponPeriod() {
    return exCouponPeriod;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the legal entity identifier.
   * <p>
   * This identifier is used for the legal entity which issues the coupon bond product.
   * @return the value of the property, not null
   */
  public StandardId getLegalEntityId() {
    return legalEntityId;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets yield convention.
   * <p>
   * The convention defines how to convert from yield to price and inversely.
   * @return the value of the property, not null
   */
  public YieldConvention getYieldConvention() {
    return yieldConvention;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets start index value.
   * <p>
   * The price index value at the start of the bond.
   * @return the value of the property
   */
  public double getStartIndexValue() {
    return startIndexValue;
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
      return JodaBeanUtils.equal(currency, other.currency) &&
          JodaBeanUtils.equal(notional, other.notional) &&
          JodaBeanUtils.equal(dayCount, other.dayCount) &&
          JodaBeanUtils.equal(periodicSchedule, other.periodicSchedule) &&
          JodaBeanUtils.equal(rateCalculation, other.rateCalculation) &&
          JodaBeanUtils.equal(settlementDateOffset, other.settlementDateOffset) &&
          JodaBeanUtils.equal(exCouponPeriod, other.exCouponPeriod) &&
          JodaBeanUtils.equal(legalEntityId, other.legalEntityId) &&
          JodaBeanUtils.equal(yieldConvention, other.yieldConvention) &&
          JodaBeanUtils.equal(startIndexValue, other.startIndexValue);
    }
    return false;
  }

  @Override
  public int hashCode() {
    int hash = getClass().hashCode();
    hash = hash * 31 + JodaBeanUtils.hashCode(currency);
    hash = hash * 31 + JodaBeanUtils.hashCode(notional);
    hash = hash * 31 + JodaBeanUtils.hashCode(dayCount);
    hash = hash * 31 + JodaBeanUtils.hashCode(periodicSchedule);
    hash = hash * 31 + JodaBeanUtils.hashCode(rateCalculation);
    hash = hash * 31 + JodaBeanUtils.hashCode(settlementDateOffset);
    hash = hash * 31 + JodaBeanUtils.hashCode(exCouponPeriod);
    hash = hash * 31 + JodaBeanUtils.hashCode(legalEntityId);
    hash = hash * 31 + JodaBeanUtils.hashCode(yieldConvention);
    hash = hash * 31 + JodaBeanUtils.hashCode(startIndexValue);
    return hash;
  }

  @Override
  public String toString() {
    StringBuilder buf = new StringBuilder(352);
    buf.append("CapitalIndexedBond{");
    buf.append("currency").append('=').append(currency).append(',').append(' ');
    buf.append("notional").append('=').append(notional).append(',').append(' ');
    buf.append("dayCount").append('=').append(dayCount).append(',').append(' ');
    buf.append("periodicSchedule").append('=').append(periodicSchedule).append(',').append(' ');
    buf.append("rateCalculation").append('=').append(rateCalculation).append(',').append(' ');
    buf.append("settlementDateOffset").append('=').append(settlementDateOffset).append(',').append(' ');
    buf.append("exCouponPeriod").append('=').append(exCouponPeriod).append(',').append(' ');
    buf.append("legalEntityId").append('=').append(legalEntityId).append(',').append(' ');
    buf.append("yieldConvention").append('=').append(yieldConvention).append(',').append(' ');
    buf.append("startIndexValue").append('=').append(JodaBeanUtils.toString(startIndexValue));
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
     * The meta-property for the {@code dayCount} property.
     */
    private final MetaProperty<DayCount> dayCount = DirectMetaProperty.ofImmutable(
        this, "dayCount", CapitalIndexedBond.class, DayCount.class);
    /**
     * The meta-property for the {@code periodicSchedule} property.
     */
    private final MetaProperty<PeriodicSchedule> periodicSchedule = DirectMetaProperty.ofImmutable(
        this, "periodicSchedule", CapitalIndexedBond.class, PeriodicSchedule.class);
    /**
     * The meta-property for the {@code rateCalculation} property.
     */
    private final MetaProperty<InflationRateCalculation> rateCalculation = DirectMetaProperty.ofImmutable(
        this, "rateCalculation", CapitalIndexedBond.class, InflationRateCalculation.class);
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
     * The meta-property for the {@code legalEntityId} property.
     */
    private final MetaProperty<StandardId> legalEntityId = DirectMetaProperty.ofImmutable(
        this, "legalEntityId", CapitalIndexedBond.class, StandardId.class);
    /**
     * The meta-property for the {@code yieldConvention} property.
     */
    private final MetaProperty<YieldConvention> yieldConvention = DirectMetaProperty.ofImmutable(
        this, "yieldConvention", CapitalIndexedBond.class, YieldConvention.class);
    /**
     * The meta-property for the {@code startIndexValue} property.
     */
    private final MetaProperty<Double> startIndexValue = DirectMetaProperty.ofImmutable(
        this, "startIndexValue", CapitalIndexedBond.class, Double.TYPE);
    /**
     * The meta-properties.
     */
    private final Map<String, MetaProperty<?>> metaPropertyMap$ = new DirectMetaPropertyMap(
        this, null,
        "currency",
        "notional",
        "dayCount",
        "periodicSchedule",
        "rateCalculation",
        "settlementDateOffset",
        "exCouponPeriod",
        "legalEntityId",
        "yieldConvention",
        "startIndexValue");

    /**
     * Restricted constructor.
     */
    private Meta() {
    }

    @Override
    protected MetaProperty<?> metaPropertyGet(String propertyName) {
      switch (propertyName.hashCode()) {
        case 575402001:  // currency
          return currency;
        case 1585636160:  // notional
          return notional;
        case 1905311443:  // dayCount
          return dayCount;
        case 1847018066:  // periodicSchedule
          return periodicSchedule;
        case -521703991:  // rateCalculation
          return rateCalculation;
        case 135924714:  // settlementDateOffset
          return settlementDateOffset;
        case 1408037338:  // exCouponPeriod
          return exCouponPeriod;
        case 866287159:  // legalEntityId
          return legalEntityId;
        case -1895216418:  // yieldConvention
          return yieldConvention;
        case -1656407615:  // startIndexValue
          return startIndexValue;
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
     * The meta-property for the {@code dayCount} property.
     * @return the meta-property, not null
     */
    public MetaProperty<DayCount> dayCount() {
      return dayCount;
    }

    /**
     * The meta-property for the {@code periodicSchedule} property.
     * @return the meta-property, not null
     */
    public MetaProperty<PeriodicSchedule> periodicSchedule() {
      return periodicSchedule;
    }

    /**
     * The meta-property for the {@code rateCalculation} property.
     * @return the meta-property, not null
     */
    public MetaProperty<InflationRateCalculation> rateCalculation() {
      return rateCalculation;
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

    /**
     * The meta-property for the {@code legalEntityId} property.
     * @return the meta-property, not null
     */
    public MetaProperty<StandardId> legalEntityId() {
      return legalEntityId;
    }

    /**
     * The meta-property for the {@code yieldConvention} property.
     * @return the meta-property, not null
     */
    public MetaProperty<YieldConvention> yieldConvention() {
      return yieldConvention;
    }

    /**
     * The meta-property for the {@code startIndexValue} property.
     * @return the meta-property, not null
     */
    public MetaProperty<Double> startIndexValue() {
      return startIndexValue;
    }

    //-----------------------------------------------------------------------
    @Override
    protected Object propertyGet(Bean bean, String propertyName, boolean quiet) {
      switch (propertyName.hashCode()) {
        case 575402001:  // currency
          return ((CapitalIndexedBond) bean).getCurrency();
        case 1585636160:  // notional
          return ((CapitalIndexedBond) bean).getNotional();
        case 1905311443:  // dayCount
          return ((CapitalIndexedBond) bean).getDayCount();
        case 1847018066:  // periodicSchedule
          return ((CapitalIndexedBond) bean).getPeriodicSchedule();
        case -521703991:  // rateCalculation
          return ((CapitalIndexedBond) bean).getRateCalculation();
        case 135924714:  // settlementDateOffset
          return ((CapitalIndexedBond) bean).getSettlementDateOffset();
        case 1408037338:  // exCouponPeriod
          return ((CapitalIndexedBond) bean).getExCouponPeriod();
        case 866287159:  // legalEntityId
          return ((CapitalIndexedBond) bean).getLegalEntityId();
        case -1895216418:  // yieldConvention
          return ((CapitalIndexedBond) bean).getYieldConvention();
        case -1656407615:  // startIndexValue
          return ((CapitalIndexedBond) bean).getStartIndexValue();
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

    private Currency currency;
    private double notional;
    private DayCount dayCount;
    private PeriodicSchedule periodicSchedule;
    private InflationRateCalculation rateCalculation;
    private DaysAdjustment settlementDateOffset;
    private DaysAdjustment exCouponPeriod;
    private StandardId legalEntityId;
    private YieldConvention yieldConvention;
    private double startIndexValue;

    /**
     * Restricted constructor.
     */
    private Builder() {
    }

    /**
     * Restricted copy constructor.
     * @param beanToCopy  the bean to copy from, not null
     */
    private Builder(CapitalIndexedBond beanToCopy) {
      this.currency = beanToCopy.getCurrency();
      this.notional = beanToCopy.getNotional();
      this.dayCount = beanToCopy.getDayCount();
      this.periodicSchedule = beanToCopy.getPeriodicSchedule();
      this.rateCalculation = beanToCopy.getRateCalculation();
      this.settlementDateOffset = beanToCopy.getSettlementDateOffset();
      this.exCouponPeriod = beanToCopy.getExCouponPeriod();
      this.legalEntityId = beanToCopy.getLegalEntityId();
      this.yieldConvention = beanToCopy.getYieldConvention();
      this.startIndexValue = beanToCopy.getStartIndexValue();
    }

    //-----------------------------------------------------------------------
    @Override
    public Object get(String propertyName) {
      switch (propertyName.hashCode()) {
        case 575402001:  // currency
          return currency;
        case 1585636160:  // notional
          return notional;
        case 1905311443:  // dayCount
          return dayCount;
        case 1847018066:  // periodicSchedule
          return periodicSchedule;
        case -521703991:  // rateCalculation
          return rateCalculation;
        case 135924714:  // settlementDateOffset
          return settlementDateOffset;
        case 1408037338:  // exCouponPeriod
          return exCouponPeriod;
        case 866287159:  // legalEntityId
          return legalEntityId;
        case -1895216418:  // yieldConvention
          return yieldConvention;
        case -1656407615:  // startIndexValue
          return startIndexValue;
        default:
          throw new NoSuchElementException("Unknown property: " + propertyName);
      }
    }

    @Override
    public Builder set(String propertyName, Object newValue) {
      switch (propertyName.hashCode()) {
        case 575402001:  // currency
          this.currency = (Currency) newValue;
          break;
        case 1585636160:  // notional
          this.notional = (Double) newValue;
          break;
        case 1905311443:  // dayCount
          this.dayCount = (DayCount) newValue;
          break;
        case 1847018066:  // periodicSchedule
          this.periodicSchedule = (PeriodicSchedule) newValue;
          break;
        case -521703991:  // rateCalculation
          this.rateCalculation = (InflationRateCalculation) newValue;
          break;
        case 135924714:  // settlementDateOffset
          this.settlementDateOffset = (DaysAdjustment) newValue;
          break;
        case 1408037338:  // exCouponPeriod
          this.exCouponPeriod = (DaysAdjustment) newValue;
          break;
        case 866287159:  // legalEntityId
          this.legalEntityId = (StandardId) newValue;
          break;
        case -1895216418:  // yieldConvention
          this.yieldConvention = (YieldConvention) newValue;
          break;
        case -1656407615:  // startIndexValue
          this.startIndexValue = (Double) newValue;
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
    public CapitalIndexedBond build() {
      preBuild(this);
      return new CapitalIndexedBond(
          currency,
          notional,
          dayCount,
          periodicSchedule,
          rateCalculation,
          settlementDateOffset,
          exCouponPeriod,
          legalEntityId,
          yieldConvention,
          startIndexValue);
    }

    //-----------------------------------------------------------------------
    /**
     * Sets the primary currency of the product.
     * <p>
     * The amounts of the notional are usually expressed in terms of this currency,
     * however they can be converted from amounts in a different currency.
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
      ArgChecker.notNegative(notional, "notional");
      this.notional = notional;
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
     * Sets the accrual schedule.
     * <p>
     * This is used to define the accrual periods.
     * These are used directly or indirectly to determine other dates in the product.
     * @param periodicSchedule  the new value, not null
     * @return this, for chaining, not null
     */
    public Builder periodicSchedule(PeriodicSchedule periodicSchedule) {
      JodaBeanUtils.notNull(periodicSchedule, "periodicSchedule");
      this.periodicSchedule = periodicSchedule;
      return this;
    }

    /**
     * Sets the inflation rate calculation.
     * <p>
     * The reference index is interpolated index or monthly index.
     * Real coupons are represented by {@code gearing} in this field.
     * @param rateCalculation  the new value, not null
     * @return this, for chaining, not null
     */
    public Builder rateCalculation(InflationRateCalculation rateCalculation) {
      JodaBeanUtils.notNull(rateCalculation, "rateCalculation");
      this.rateCalculation = rateCalculation;
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
     * Because the detachment date is not after the coupon date, the number of days stored in this field
     * should be zero or negative.
     * @param exCouponPeriod  the new value, not null
     * @return this, for chaining, not null
     */
    public Builder exCouponPeriod(DaysAdjustment exCouponPeriod) {
      JodaBeanUtils.notNull(exCouponPeriod, "exCouponPeriod");
      this.exCouponPeriod = exCouponPeriod;
      return this;
    }

    /**
     * Sets the legal entity identifier.
     * <p>
     * This identifier is used for the legal entity which issues the coupon bond product.
     * @param legalEntityId  the new value, not null
     * @return this, for chaining, not null
     */
    public Builder legalEntityId(StandardId legalEntityId) {
      JodaBeanUtils.notNull(legalEntityId, "legalEntityId");
      this.legalEntityId = legalEntityId;
      return this;
    }

    /**
     * Sets yield convention.
     * <p>
     * The convention defines how to convert from yield to price and inversely.
     * @param yieldConvention  the new value, not null
     * @return this, for chaining, not null
     */
    public Builder yieldConvention(YieldConvention yieldConvention) {
      JodaBeanUtils.notNull(yieldConvention, "yieldConvention");
      this.yieldConvention = yieldConvention;
      return this;
    }

    /**
     * Sets start index value.
     * <p>
     * The price index value at the start of the bond.
     * @param startIndexValue  the new value
     * @return this, for chaining, not null
     */
    public Builder startIndexValue(double startIndexValue) {
      ArgChecker.notNegativeOrZero(startIndexValue, "startIndexValue");
      this.startIndexValue = startIndexValue;
      return this;
    }

    //-----------------------------------------------------------------------
    @Override
    public String toString() {
      StringBuilder buf = new StringBuilder(352);
      buf.append("CapitalIndexedBond.Builder{");
      buf.append("currency").append('=').append(JodaBeanUtils.toString(currency)).append(',').append(' ');
      buf.append("notional").append('=').append(JodaBeanUtils.toString(notional)).append(',').append(' ');
      buf.append("dayCount").append('=').append(JodaBeanUtils.toString(dayCount)).append(',').append(' ');
      buf.append("periodicSchedule").append('=').append(JodaBeanUtils.toString(periodicSchedule)).append(',').append(' ');
      buf.append("rateCalculation").append('=').append(JodaBeanUtils.toString(rateCalculation)).append(',').append(' ');
      buf.append("settlementDateOffset").append('=').append(JodaBeanUtils.toString(settlementDateOffset)).append(',').append(' ');
      buf.append("exCouponPeriod").append('=').append(JodaBeanUtils.toString(exCouponPeriod)).append(',').append(' ');
      buf.append("legalEntityId").append('=').append(JodaBeanUtils.toString(legalEntityId)).append(',').append(' ');
      buf.append("yieldConvention").append('=').append(JodaBeanUtils.toString(yieldConvention)).append(',').append(' ');
      buf.append("startIndexValue").append('=').append(JodaBeanUtils.toString(startIndexValue));
      buf.append('}');
      return buf.toString();
    }

  }

  ///CLOVER:ON
  //-------------------------- AUTOGENERATED END --------------------------
}
