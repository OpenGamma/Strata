/**
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.product.credit;

import java.io.Serializable;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;

import org.joda.beans.Bean;
import org.joda.beans.BeanDefinition;
import org.joda.beans.ImmutableBean;
import org.joda.beans.ImmutableDefaults;
import org.joda.beans.ImmutablePreBuild;
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
import com.opengamma.strata.basics.date.BusinessDayAdjustment;
import com.opengamma.strata.basics.date.BusinessDayConventions;
import com.opengamma.strata.basics.date.DayCount;
import com.opengamma.strata.basics.date.DayCounts;
import com.opengamma.strata.basics.date.DaysAdjustment;
import com.opengamma.strata.basics.date.HolidayCalendarId;
import com.opengamma.strata.basics.schedule.Frequency;
import com.opengamma.strata.basics.schedule.PeriodicSchedule;
import com.opengamma.strata.basics.schedule.RollConventions;
import com.opengamma.strata.basics.schedule.Schedule;
import com.opengamma.strata.basics.schedule.SchedulePeriod;
import com.opengamma.strata.basics.schedule.StubConvention;
import com.opengamma.strata.collect.ArgChecker;
import com.opengamma.strata.product.common.BuySell;

/**
 * A CDS (portfolio) index product. 
 * <p>
 * A CDS index is a portofolio of single name credit default swaps. 
 * The protection buyer periodically pays fixed coupons to the protection seller until the expiry, 
 * and in return, the protection buyer receives the bond of a defaulted constituent legal entity for par.  
 */
@BeanDefinition
public final class CdsIndex
    implements Resolvable<ResolvedCdsIndex>, ImmutableBean, Serializable {

  /**
   * Whether the CDS index is buy or sell.
   * <p>
   * A value of 'Buy' implies buying protection, where the fixed coupon is paid
   * and the protection is received  in the event of default.
   * A value of 'Sell' implies selling protection, where the fixed coupon is received
   * and the protection is paid in the event of default.
   */
  @PropertyDefinition(validate = "notNull")
  private final BuySell buySell;
  /**
   * The CDS index identifier.
   * <p>
   * This identifier is used to refer this CDS index product.
   */
  @PropertyDefinition(validate = "notNull")
  private final StandardId cdsIndexId;
  /**
   * The legal entity identifiers.
   * <p>
   * These identifiers refer to the reference legal entities of the CDS index.
   */
  @PropertyDefinition(validate = "notNull")
  private final ImmutableList<StandardId> legalEntityIds;
  /**
   * The currency of the CDS index.
   * <p>
   * The amounts of the notional are expressed in terms of this currency.
   */
  @PropertyDefinition(validate = "notNull")
  private final Currency currency;
  /**
   * The notional amount, must be non-negative.
   * <p>
   * The fixed notional amount applicable during the lifetime of the CDS.
   * The currency of the notional is specified by {@code currency}.
   */
  @PropertyDefinition(validate = "ArgChecker.notNegativeOrZero")
  private final double notional;
  /**
   * The payment schedule.
   * <p>
   * This is used to define the payment periods.
   */
  @PropertyDefinition(validate = "notNull")
  private final PeriodicSchedule paymentSchedule;
  /**
   * The fixed coupon rate.
   * <p>
   * This must be represented in decimal form.
   */
  @PropertyDefinition(validate = "ArgChecker.notNegative")
  private final double fixedRate;
  /**
   * The day count convention.
   * <p>
   * This is used to convert dates to a numerical value.
   * <p>
   * When building, this will default to 'Act/360'.
   */
  @PropertyDefinition(validate = "notNull")
  private final DayCount dayCount;
  /**
   * The payment on default.
   * <p>
   * Whether the accrued premium is paid in the event of a default.
   * <p>
   * When building, this will default to 'AccruedPremium'.
   */
  @PropertyDefinition(validate = "notNull")
  private final PaymentOnDefault paymentOnDefault;
  /**
   * The protection start of the day.
   * <p>
   * When the protection starts on the start date.
   * <p>
   * When building, this will default to 'Beginning'.
   */
  @PropertyDefinition(validate = "notNull")
  private final ProtectionStartOfDay protectionStart;
  /**
   * The number of days between valuation date and step-in date.
   * <p>
   * The step-in date is also called protection effective date.
   * It is usually 1 calendar day for standardized CDS index contracts.
   * <p>
   * When building, this will default to 1 calendar day.
   */
  @PropertyDefinition(validate = "notNull")
  private final DaysAdjustment stepinDateOffset;
  /**
   * The number of days between valuation date and settlement date.
   * <p>
   * It is usually 3 business days for standardized CDS index contracts.
   * <p>
   * When building, this will default to 3 business days in the calendar of the payment schedule.
   */
  @PropertyDefinition(validate = "notNull")
  private final DaysAdjustment settlementDateOffset;

  //-------------------------------------------------------------------------
  /**
   * Creates an instance of a standardized CDS index.
   * 
   * @param buySell  buy or sell
   * @param cdsIndexId  the CDS index ID
   * @param legalEntityIds  the legal entity IDs
   * @param currency  the currency
   * @param notional  the notional
   * @param startDate  the start date
   * @param endDate  the end date
   * @param calendar  the calendar
   * @param paymentFrequency  the payment frequency
   * @param fixedRate  the fixed coupon rate
   * @return the instance
   */
  public static CdsIndex of(
      BuySell buySell,
      StandardId cdsIndexId,
      List<StandardId> legalEntityIds,
      Currency currency,
      double notional,
      LocalDate startDate,
      LocalDate endDate,
      Frequency paymentFrequency,
      HolidayCalendarId calendar,
      double fixedRate) {

    PeriodicSchedule accrualSchedule = PeriodicSchedule.builder()
        .businessDayAdjustment(BusinessDayAdjustment.of(BusinessDayConventions.FOLLOWING, calendar))
        .startDate(startDate)
        .endDate(endDate)
        .startDateBusinessDayAdjustment(BusinessDayAdjustment.NONE)
        .endDateBusinessDayAdjustment(BusinessDayAdjustment.NONE)
        .frequency(paymentFrequency)
        .rollConvention(RollConventions.NONE)
        .stubConvention(StubConvention.SHORT_INITIAL)
        .build();
    return new CdsIndex(
        buySell,
        cdsIndexId,
        legalEntityIds,
        currency,
        notional,
        accrualSchedule,
        fixedRate,
        DayCounts.ACT_360,
        PaymentOnDefault.ACCRUED_PREMIUM,
        ProtectionStartOfDay.BEGINNING,
        DaysAdjustment.ofCalendarDays(1),
        DaysAdjustment.ofBusinessDays(3, calendar));
  }

  @ImmutableDefaults
  private static void applyDefaults(Builder builder) {
    builder.dayCount = DayCounts.ACT_360;
    builder.paymentOnDefault = PaymentOnDefault.ACCRUED_PREMIUM;
    builder.protectionStart = ProtectionStartOfDay.BEGINNING;
    builder.stepinDateOffset = DaysAdjustment.ofCalendarDays(1);
  }

  @ImmutablePreBuild
  private static void preBuild(Builder builder) {
    if (builder.settlementDateOffset == null && builder.paymentSchedule != null) {
      builder.settlementDateOffset =
          DaysAdjustment.ofBusinessDays(3, builder.paymentSchedule.getBusinessDayAdjustment().getCalendar());
    }
  }

  //-------------------------------------------------------------------------
  @Override
  public ResolvedCdsIndex resolve(ReferenceData refData) {
    Schedule adjustedSchedule = paymentSchedule.createSchedule(refData);
    ImmutableList.Builder<CreditCouponPaymentPeriod> accrualPeriods = ImmutableList.builder();
    int nPeriods = adjustedSchedule.size();
    for (int i = 0; i < nPeriods - 1; i++) {
      SchedulePeriod period = adjustedSchedule.getPeriod(i);
      accrualPeriods.add(CreditCouponPaymentPeriod.builder()
          .startDate(period.getStartDate())
          .endDate(period.getEndDate())
          .unadjustedStartDate(period.getUnadjustedStartDate())
          .unadjustedEndDate(period.getUnadjustedEndDate())
          .effectiveStartDate(protectionStart.isBeginning() ? period.getStartDate().minusDays(1) : period.getStartDate())
          .effectiveEndDate(protectionStart.isBeginning() ? period.getEndDate().minusDays(1) : period.getEndDate())
          .paymentDate(period.getEndDate())
          .notional(notional)
          .currency(currency)
          .fixedRate(fixedRate)
          .yearFraction(period.yearFraction(dayCount, adjustedSchedule))
          .build());
    }
    // last period - accrual end is modified
    SchedulePeriod lastPeriod = adjustedSchedule.getPeriod(nPeriods - 1);
    LocalDate accEndDate = protectionStart.isBeginning() ? lastPeriod.getEndDate().plusDays(1) : lastPeriod.getEndDate();
    SchedulePeriod modifiedPeriod = lastPeriod.toBuilder().endDate(accEndDate).build();
    accrualPeriods.add(CreditCouponPaymentPeriod.builder()
        .startDate(modifiedPeriod.getStartDate())
        .endDate(modifiedPeriod.getEndDate())
        .unadjustedStartDate(modifiedPeriod.getUnadjustedStartDate())
        .unadjustedEndDate(modifiedPeriod.getUnadjustedEndDate())
        .effectiveStartDate(protectionStart.isBeginning() ? lastPeriod.getStartDate().minusDays(1) : lastPeriod.getStartDate())
        .effectiveEndDate(lastPeriod.getEndDate())
        .paymentDate(paymentSchedule.getBusinessDayAdjustment().adjust(lastPeriod.getEndDate(), refData))
        .notional(notional)
        .currency(currency)
        .fixedRate(fixedRate)
        .yearFraction(modifiedPeriod.yearFraction(dayCount, adjustedSchedule))
        .build());
    ImmutableList<CreditCouponPaymentPeriod> paymentPeriods = accrualPeriods.build();

    return ResolvedCdsIndex.builder()
        .buySell(buySell)
        .cdsIndexId(cdsIndexId)
        .legalEntityIds(legalEntityIds)
        .protectionStart(protectionStart)
        .paymentOnDefault(paymentOnDefault)
        .paymentPeriods(paymentPeriods)
        .protectionEndDate(lastPeriod.getEndDate())
        .settlementDateOffset(settlementDateOffset)
        .stepinDateOffset(stepinDateOffset)
        .dayCount(dayCount)
        .build();
  }

  //------------------------- AUTOGENERATED START -------------------------
  ///CLOVER:OFF
  /**
   * The meta-bean for {@code CdsIndex}.
   * @return the meta-bean, not null
   */
  public static CdsIndex.Meta meta() {
    return CdsIndex.Meta.INSTANCE;
  }

  static {
    JodaBeanUtils.registerMetaBean(CdsIndex.Meta.INSTANCE);
  }

  /**
   * The serialization version id.
   */
  private static final long serialVersionUID = 1L;

  /**
   * Returns a builder used to create an instance of the bean.
   * @return the builder, not null
   */
  public static CdsIndex.Builder builder() {
    return new CdsIndex.Builder();
  }

  private CdsIndex(
      BuySell buySell,
      StandardId cdsIndexId,
      List<StandardId> legalEntityIds,
      Currency currency,
      double notional,
      PeriodicSchedule paymentSchedule,
      double fixedRate,
      DayCount dayCount,
      PaymentOnDefault paymentOnDefault,
      ProtectionStartOfDay protectionStart,
      DaysAdjustment stepinDateOffset,
      DaysAdjustment settlementDateOffset) {
    JodaBeanUtils.notNull(buySell, "buySell");
    JodaBeanUtils.notNull(cdsIndexId, "cdsIndexId");
    JodaBeanUtils.notNull(legalEntityIds, "legalEntityIds");
    JodaBeanUtils.notNull(currency, "currency");
    ArgChecker.notNegativeOrZero(notional, "notional");
    JodaBeanUtils.notNull(paymentSchedule, "paymentSchedule");
    ArgChecker.notNegative(fixedRate, "fixedRate");
    JodaBeanUtils.notNull(dayCount, "dayCount");
    JodaBeanUtils.notNull(paymentOnDefault, "paymentOnDefault");
    JodaBeanUtils.notNull(protectionStart, "protectionStart");
    JodaBeanUtils.notNull(stepinDateOffset, "stepinDateOffset");
    JodaBeanUtils.notNull(settlementDateOffset, "settlementDateOffset");
    this.buySell = buySell;
    this.cdsIndexId = cdsIndexId;
    this.legalEntityIds = ImmutableList.copyOf(legalEntityIds);
    this.currency = currency;
    this.notional = notional;
    this.paymentSchedule = paymentSchedule;
    this.fixedRate = fixedRate;
    this.dayCount = dayCount;
    this.paymentOnDefault = paymentOnDefault;
    this.protectionStart = protectionStart;
    this.stepinDateOffset = stepinDateOffset;
    this.settlementDateOffset = settlementDateOffset;
  }

  @Override
  public CdsIndex.Meta metaBean() {
    return CdsIndex.Meta.INSTANCE;
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
   * Gets whether the CDS index is buy or sell.
   * <p>
   * A value of 'Buy' implies buying protection, where the fixed coupon is paid
   * and the protection is received  in the event of default.
   * A value of 'Sell' implies selling protection, where the fixed coupon is received
   * and the protection is paid in the event of default.
   * @return the value of the property, not null
   */
  public BuySell getBuySell() {
    return buySell;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the CDS index identifier.
   * <p>
   * This identifier is used to refer this CDS index product.
   * @return the value of the property, not null
   */
  public StandardId getCdsIndexId() {
    return cdsIndexId;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the legal entity identifiers.
   * <p>
   * These identifiers refer to the reference legal entities of the CDS index.
   * @return the value of the property, not null
   */
  public ImmutableList<StandardId> getLegalEntityIds() {
    return legalEntityIds;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the currency of the CDS index.
   * <p>
   * The amounts of the notional are expressed in terms of this currency.
   * @return the value of the property, not null
   */
  public Currency getCurrency() {
    return currency;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the notional amount, must be non-negative.
   * <p>
   * The fixed notional amount applicable during the lifetime of the CDS.
   * The currency of the notional is specified by {@code currency}.
   * @return the value of the property
   */
  public double getNotional() {
    return notional;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the payment schedule.
   * <p>
   * This is used to define the payment periods.
   * @return the value of the property, not null
   */
  public PeriodicSchedule getPaymentSchedule() {
    return paymentSchedule;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the fixed coupon rate.
   * <p>
   * This must be represented in decimal form.
   * @return the value of the property
   */
  public double getFixedRate() {
    return fixedRate;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the day count convention.
   * <p>
   * This is used to convert dates to a numerical value.
   * <p>
   * When building, this will default to 'Act/360'.
   * @return the value of the property, not null
   */
  public DayCount getDayCount() {
    return dayCount;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the payment on default.
   * <p>
   * Whether the accrued premium is paid in the event of a default.
   * <p>
   * When building, this will default to 'AccruedPremium'.
   * @return the value of the property, not null
   */
  public PaymentOnDefault getPaymentOnDefault() {
    return paymentOnDefault;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the protection start of the day.
   * <p>
   * When the protection starts on the start date.
   * <p>
   * When building, this will default to 'Beginning'.
   * @return the value of the property, not null
   */
  public ProtectionStartOfDay getProtectionStart() {
    return protectionStart;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the number of days between valuation date and step-in date.
   * <p>
   * The step-in date is also called protection effective date.
   * It is usually 1 calendar day for standardized CDS index contracts.
   * <p>
   * When building, this will default to 1 calendar day.
   * @return the value of the property, not null
   */
  public DaysAdjustment getStepinDateOffset() {
    return stepinDateOffset;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the number of days between valuation date and settlement date.
   * <p>
   * It is usually 3 business days for standardized CDS index contracts.
   * <p>
   * When building, this will default to 3 business days in the calendar of the payment schedule.
   * @return the value of the property, not null
   */
  public DaysAdjustment getSettlementDateOffset() {
    return settlementDateOffset;
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
      CdsIndex other = (CdsIndex) obj;
      return JodaBeanUtils.equal(buySell, other.buySell) &&
          JodaBeanUtils.equal(cdsIndexId, other.cdsIndexId) &&
          JodaBeanUtils.equal(legalEntityIds, other.legalEntityIds) &&
          JodaBeanUtils.equal(currency, other.currency) &&
          JodaBeanUtils.equal(notional, other.notional) &&
          JodaBeanUtils.equal(paymentSchedule, other.paymentSchedule) &&
          JodaBeanUtils.equal(fixedRate, other.fixedRate) &&
          JodaBeanUtils.equal(dayCount, other.dayCount) &&
          JodaBeanUtils.equal(paymentOnDefault, other.paymentOnDefault) &&
          JodaBeanUtils.equal(protectionStart, other.protectionStart) &&
          JodaBeanUtils.equal(stepinDateOffset, other.stepinDateOffset) &&
          JodaBeanUtils.equal(settlementDateOffset, other.settlementDateOffset);
    }
    return false;
  }

  @Override
  public int hashCode() {
    int hash = getClass().hashCode();
    hash = hash * 31 + JodaBeanUtils.hashCode(buySell);
    hash = hash * 31 + JodaBeanUtils.hashCode(cdsIndexId);
    hash = hash * 31 + JodaBeanUtils.hashCode(legalEntityIds);
    hash = hash * 31 + JodaBeanUtils.hashCode(currency);
    hash = hash * 31 + JodaBeanUtils.hashCode(notional);
    hash = hash * 31 + JodaBeanUtils.hashCode(paymentSchedule);
    hash = hash * 31 + JodaBeanUtils.hashCode(fixedRate);
    hash = hash * 31 + JodaBeanUtils.hashCode(dayCount);
    hash = hash * 31 + JodaBeanUtils.hashCode(paymentOnDefault);
    hash = hash * 31 + JodaBeanUtils.hashCode(protectionStart);
    hash = hash * 31 + JodaBeanUtils.hashCode(stepinDateOffset);
    hash = hash * 31 + JodaBeanUtils.hashCode(settlementDateOffset);
    return hash;
  }

  @Override
  public String toString() {
    StringBuilder buf = new StringBuilder(416);
    buf.append("CdsIndex{");
    buf.append("buySell").append('=').append(buySell).append(',').append(' ');
    buf.append("cdsIndexId").append('=').append(cdsIndexId).append(',').append(' ');
    buf.append("legalEntityIds").append('=').append(legalEntityIds).append(',').append(' ');
    buf.append("currency").append('=').append(currency).append(',').append(' ');
    buf.append("notional").append('=').append(notional).append(',').append(' ');
    buf.append("paymentSchedule").append('=').append(paymentSchedule).append(',').append(' ');
    buf.append("fixedRate").append('=').append(fixedRate).append(',').append(' ');
    buf.append("dayCount").append('=').append(dayCount).append(',').append(' ');
    buf.append("paymentOnDefault").append('=').append(paymentOnDefault).append(',').append(' ');
    buf.append("protectionStart").append('=').append(protectionStart).append(',').append(' ');
    buf.append("stepinDateOffset").append('=').append(stepinDateOffset).append(',').append(' ');
    buf.append("settlementDateOffset").append('=').append(JodaBeanUtils.toString(settlementDateOffset));
    buf.append('}');
    return buf.toString();
  }

  //-----------------------------------------------------------------------
  /**
   * The meta-bean for {@code CdsIndex}.
   */
  public static final class Meta extends DirectMetaBean {
    /**
     * The singleton instance of the meta-bean.
     */
    static final Meta INSTANCE = new Meta();

    /**
     * The meta-property for the {@code buySell} property.
     */
    private final MetaProperty<BuySell> buySell = DirectMetaProperty.ofImmutable(
        this, "buySell", CdsIndex.class, BuySell.class);
    /**
     * The meta-property for the {@code cdsIndexId} property.
     */
    private final MetaProperty<StandardId> cdsIndexId = DirectMetaProperty.ofImmutable(
        this, "cdsIndexId", CdsIndex.class, StandardId.class);
    /**
     * The meta-property for the {@code legalEntityIds} property.
     */
    @SuppressWarnings({"unchecked", "rawtypes" })
    private final MetaProperty<ImmutableList<StandardId>> legalEntityIds = DirectMetaProperty.ofImmutable(
        this, "legalEntityIds", CdsIndex.class, (Class) ImmutableList.class);
    /**
     * The meta-property for the {@code currency} property.
     */
    private final MetaProperty<Currency> currency = DirectMetaProperty.ofImmutable(
        this, "currency", CdsIndex.class, Currency.class);
    /**
     * The meta-property for the {@code notional} property.
     */
    private final MetaProperty<Double> notional = DirectMetaProperty.ofImmutable(
        this, "notional", CdsIndex.class, Double.TYPE);
    /**
     * The meta-property for the {@code paymentSchedule} property.
     */
    private final MetaProperty<PeriodicSchedule> paymentSchedule = DirectMetaProperty.ofImmutable(
        this, "paymentSchedule", CdsIndex.class, PeriodicSchedule.class);
    /**
     * The meta-property for the {@code fixedRate} property.
     */
    private final MetaProperty<Double> fixedRate = DirectMetaProperty.ofImmutable(
        this, "fixedRate", CdsIndex.class, Double.TYPE);
    /**
     * The meta-property for the {@code dayCount} property.
     */
    private final MetaProperty<DayCount> dayCount = DirectMetaProperty.ofImmutable(
        this, "dayCount", CdsIndex.class, DayCount.class);
    /**
     * The meta-property for the {@code paymentOnDefault} property.
     */
    private final MetaProperty<PaymentOnDefault> paymentOnDefault = DirectMetaProperty.ofImmutable(
        this, "paymentOnDefault", CdsIndex.class, PaymentOnDefault.class);
    /**
     * The meta-property for the {@code protectionStart} property.
     */
    private final MetaProperty<ProtectionStartOfDay> protectionStart = DirectMetaProperty.ofImmutable(
        this, "protectionStart", CdsIndex.class, ProtectionStartOfDay.class);
    /**
     * The meta-property for the {@code stepinDateOffset} property.
     */
    private final MetaProperty<DaysAdjustment> stepinDateOffset = DirectMetaProperty.ofImmutable(
        this, "stepinDateOffset", CdsIndex.class, DaysAdjustment.class);
    /**
     * The meta-property for the {@code settlementDateOffset} property.
     */
    private final MetaProperty<DaysAdjustment> settlementDateOffset = DirectMetaProperty.ofImmutable(
        this, "settlementDateOffset", CdsIndex.class, DaysAdjustment.class);
    /**
     * The meta-properties.
     */
    private final Map<String, MetaProperty<?>> metaPropertyMap$ = new DirectMetaPropertyMap(
        this, null,
        "buySell",
        "cdsIndexId",
        "legalEntityIds",
        "currency",
        "notional",
        "paymentSchedule",
        "fixedRate",
        "dayCount",
        "paymentOnDefault",
        "protectionStart",
        "stepinDateOffset",
        "settlementDateOffset");

    /**
     * Restricted constructor.
     */
    private Meta() {
    }

    @Override
    protected MetaProperty<?> metaPropertyGet(String propertyName) {
      switch (propertyName.hashCode()) {
        case 244977400:  // buySell
          return buySell;
        case -464117509:  // cdsIndexId
          return cdsIndexId;
        case 1085098268:  // legalEntityIds
          return legalEntityIds;
        case 575402001:  // currency
          return currency;
        case 1585636160:  // notional
          return notional;
        case -1499086147:  // paymentSchedule
          return paymentSchedule;
        case 747425396:  // fixedRate
          return fixedRate;
        case 1905311443:  // dayCount
          return dayCount;
        case -480203780:  // paymentOnDefault
          return paymentOnDefault;
        case 2103482633:  // protectionStart
          return protectionStart;
        case 852621746:  // stepinDateOffset
          return stepinDateOffset;
        case 135924714:  // settlementDateOffset
          return settlementDateOffset;
      }
      return super.metaPropertyGet(propertyName);
    }

    @Override
    public CdsIndex.Builder builder() {
      return new CdsIndex.Builder();
    }

    @Override
    public Class<? extends CdsIndex> beanType() {
      return CdsIndex.class;
    }

    @Override
    public Map<String, MetaProperty<?>> metaPropertyMap() {
      return metaPropertyMap$;
    }

    //-----------------------------------------------------------------------
    /**
     * The meta-property for the {@code buySell} property.
     * @return the meta-property, not null
     */
    public MetaProperty<BuySell> buySell() {
      return buySell;
    }

    /**
     * The meta-property for the {@code cdsIndexId} property.
     * @return the meta-property, not null
     */
    public MetaProperty<StandardId> cdsIndexId() {
      return cdsIndexId;
    }

    /**
     * The meta-property for the {@code legalEntityIds} property.
     * @return the meta-property, not null
     */
    public MetaProperty<ImmutableList<StandardId>> legalEntityIds() {
      return legalEntityIds;
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
     * The meta-property for the {@code paymentSchedule} property.
     * @return the meta-property, not null
     */
    public MetaProperty<PeriodicSchedule> paymentSchedule() {
      return paymentSchedule;
    }

    /**
     * The meta-property for the {@code fixedRate} property.
     * @return the meta-property, not null
     */
    public MetaProperty<Double> fixedRate() {
      return fixedRate;
    }

    /**
     * The meta-property for the {@code dayCount} property.
     * @return the meta-property, not null
     */
    public MetaProperty<DayCount> dayCount() {
      return dayCount;
    }

    /**
     * The meta-property for the {@code paymentOnDefault} property.
     * @return the meta-property, not null
     */
    public MetaProperty<PaymentOnDefault> paymentOnDefault() {
      return paymentOnDefault;
    }

    /**
     * The meta-property for the {@code protectionStart} property.
     * @return the meta-property, not null
     */
    public MetaProperty<ProtectionStartOfDay> protectionStart() {
      return protectionStart;
    }

    /**
     * The meta-property for the {@code stepinDateOffset} property.
     * @return the meta-property, not null
     */
    public MetaProperty<DaysAdjustment> stepinDateOffset() {
      return stepinDateOffset;
    }

    /**
     * The meta-property for the {@code settlementDateOffset} property.
     * @return the meta-property, not null
     */
    public MetaProperty<DaysAdjustment> settlementDateOffset() {
      return settlementDateOffset;
    }

    //-----------------------------------------------------------------------
    @Override
    protected Object propertyGet(Bean bean, String propertyName, boolean quiet) {
      switch (propertyName.hashCode()) {
        case 244977400:  // buySell
          return ((CdsIndex) bean).getBuySell();
        case -464117509:  // cdsIndexId
          return ((CdsIndex) bean).getCdsIndexId();
        case 1085098268:  // legalEntityIds
          return ((CdsIndex) bean).getLegalEntityIds();
        case 575402001:  // currency
          return ((CdsIndex) bean).getCurrency();
        case 1585636160:  // notional
          return ((CdsIndex) bean).getNotional();
        case -1499086147:  // paymentSchedule
          return ((CdsIndex) bean).getPaymentSchedule();
        case 747425396:  // fixedRate
          return ((CdsIndex) bean).getFixedRate();
        case 1905311443:  // dayCount
          return ((CdsIndex) bean).getDayCount();
        case -480203780:  // paymentOnDefault
          return ((CdsIndex) bean).getPaymentOnDefault();
        case 2103482633:  // protectionStart
          return ((CdsIndex) bean).getProtectionStart();
        case 852621746:  // stepinDateOffset
          return ((CdsIndex) bean).getStepinDateOffset();
        case 135924714:  // settlementDateOffset
          return ((CdsIndex) bean).getSettlementDateOffset();
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
   * The bean-builder for {@code CdsIndex}.
   */
  public static final class Builder extends DirectFieldsBeanBuilder<CdsIndex> {

    private BuySell buySell;
    private StandardId cdsIndexId;
    private List<StandardId> legalEntityIds = ImmutableList.of();
    private Currency currency;
    private double notional;
    private PeriodicSchedule paymentSchedule;
    private double fixedRate;
    private DayCount dayCount;
    private PaymentOnDefault paymentOnDefault;
    private ProtectionStartOfDay protectionStart;
    private DaysAdjustment stepinDateOffset;
    private DaysAdjustment settlementDateOffset;

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
    private Builder(CdsIndex beanToCopy) {
      this.buySell = beanToCopy.getBuySell();
      this.cdsIndexId = beanToCopy.getCdsIndexId();
      this.legalEntityIds = beanToCopy.getLegalEntityIds();
      this.currency = beanToCopy.getCurrency();
      this.notional = beanToCopy.getNotional();
      this.paymentSchedule = beanToCopy.getPaymentSchedule();
      this.fixedRate = beanToCopy.getFixedRate();
      this.dayCount = beanToCopy.getDayCount();
      this.paymentOnDefault = beanToCopy.getPaymentOnDefault();
      this.protectionStart = beanToCopy.getProtectionStart();
      this.stepinDateOffset = beanToCopy.getStepinDateOffset();
      this.settlementDateOffset = beanToCopy.getSettlementDateOffset();
    }

    //-----------------------------------------------------------------------
    @Override
    public Object get(String propertyName) {
      switch (propertyName.hashCode()) {
        case 244977400:  // buySell
          return buySell;
        case -464117509:  // cdsIndexId
          return cdsIndexId;
        case 1085098268:  // legalEntityIds
          return legalEntityIds;
        case 575402001:  // currency
          return currency;
        case 1585636160:  // notional
          return notional;
        case -1499086147:  // paymentSchedule
          return paymentSchedule;
        case 747425396:  // fixedRate
          return fixedRate;
        case 1905311443:  // dayCount
          return dayCount;
        case -480203780:  // paymentOnDefault
          return paymentOnDefault;
        case 2103482633:  // protectionStart
          return protectionStart;
        case 852621746:  // stepinDateOffset
          return stepinDateOffset;
        case 135924714:  // settlementDateOffset
          return settlementDateOffset;
        default:
          throw new NoSuchElementException("Unknown property: " + propertyName);
      }
    }

    @SuppressWarnings("unchecked")
    @Override
    public Builder set(String propertyName, Object newValue) {
      switch (propertyName.hashCode()) {
        case 244977400:  // buySell
          this.buySell = (BuySell) newValue;
          break;
        case -464117509:  // cdsIndexId
          this.cdsIndexId = (StandardId) newValue;
          break;
        case 1085098268:  // legalEntityIds
          this.legalEntityIds = (List<StandardId>) newValue;
          break;
        case 575402001:  // currency
          this.currency = (Currency) newValue;
          break;
        case 1585636160:  // notional
          this.notional = (Double) newValue;
          break;
        case -1499086147:  // paymentSchedule
          this.paymentSchedule = (PeriodicSchedule) newValue;
          break;
        case 747425396:  // fixedRate
          this.fixedRate = (Double) newValue;
          break;
        case 1905311443:  // dayCount
          this.dayCount = (DayCount) newValue;
          break;
        case -480203780:  // paymentOnDefault
          this.paymentOnDefault = (PaymentOnDefault) newValue;
          break;
        case 2103482633:  // protectionStart
          this.protectionStart = (ProtectionStartOfDay) newValue;
          break;
        case 852621746:  // stepinDateOffset
          this.stepinDateOffset = (DaysAdjustment) newValue;
          break;
        case 135924714:  // settlementDateOffset
          this.settlementDateOffset = (DaysAdjustment) newValue;
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
    public CdsIndex build() {
      preBuild(this);
      return new CdsIndex(
          buySell,
          cdsIndexId,
          legalEntityIds,
          currency,
          notional,
          paymentSchedule,
          fixedRate,
          dayCount,
          paymentOnDefault,
          protectionStart,
          stepinDateOffset,
          settlementDateOffset);
    }

    //-----------------------------------------------------------------------
    /**
     * Sets whether the CDS index is buy or sell.
     * <p>
     * A value of 'Buy' implies buying protection, where the fixed coupon is paid
     * and the protection is received  in the event of default.
     * A value of 'Sell' implies selling protection, where the fixed coupon is received
     * and the protection is paid in the event of default.
     * @param buySell  the new value, not null
     * @return this, for chaining, not null
     */
    public Builder buySell(BuySell buySell) {
      JodaBeanUtils.notNull(buySell, "buySell");
      this.buySell = buySell;
      return this;
    }

    /**
     * Sets the CDS index identifier.
     * <p>
     * This identifier is used to refer this CDS index product.
     * @param cdsIndexId  the new value, not null
     * @return this, for chaining, not null
     */
    public Builder cdsIndexId(StandardId cdsIndexId) {
      JodaBeanUtils.notNull(cdsIndexId, "cdsIndexId");
      this.cdsIndexId = cdsIndexId;
      return this;
    }

    /**
     * Sets the legal entity identifiers.
     * <p>
     * These identifiers refer to the reference legal entities of the CDS index.
     * @param legalEntityIds  the new value, not null
     * @return this, for chaining, not null
     */
    public Builder legalEntityIds(List<StandardId> legalEntityIds) {
      JodaBeanUtils.notNull(legalEntityIds, "legalEntityIds");
      this.legalEntityIds = legalEntityIds;
      return this;
    }

    /**
     * Sets the {@code legalEntityIds} property in the builder
     * from an array of objects.
     * @param legalEntityIds  the new value, not null
     * @return this, for chaining, not null
     */
    public Builder legalEntityIds(StandardId... legalEntityIds) {
      return legalEntityIds(ImmutableList.copyOf(legalEntityIds));
    }

    /**
     * Sets the currency of the CDS index.
     * <p>
     * The amounts of the notional are expressed in terms of this currency.
     * @param currency  the new value, not null
     * @return this, for chaining, not null
     */
    public Builder currency(Currency currency) {
      JodaBeanUtils.notNull(currency, "currency");
      this.currency = currency;
      return this;
    }

    /**
     * Sets the notional amount, must be non-negative.
     * <p>
     * The fixed notional amount applicable during the lifetime of the CDS.
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
     * Sets the payment schedule.
     * <p>
     * This is used to define the payment periods.
     * @param paymentSchedule  the new value, not null
     * @return this, for chaining, not null
     */
    public Builder paymentSchedule(PeriodicSchedule paymentSchedule) {
      JodaBeanUtils.notNull(paymentSchedule, "paymentSchedule");
      this.paymentSchedule = paymentSchedule;
      return this;
    }

    /**
     * Sets the fixed coupon rate.
     * <p>
     * This must be represented in decimal form.
     * @param fixedRate  the new value
     * @return this, for chaining, not null
     */
    public Builder fixedRate(double fixedRate) {
      ArgChecker.notNegative(fixedRate, "fixedRate");
      this.fixedRate = fixedRate;
      return this;
    }

    /**
     * Sets the day count convention.
     * <p>
     * This is used to convert dates to a numerical value.
     * <p>
     * When building, this will default to 'Act/360'.
     * @param dayCount  the new value, not null
     * @return this, for chaining, not null
     */
    public Builder dayCount(DayCount dayCount) {
      JodaBeanUtils.notNull(dayCount, "dayCount");
      this.dayCount = dayCount;
      return this;
    }

    /**
     * Sets the payment on default.
     * <p>
     * Whether the accrued premium is paid in the event of a default.
     * <p>
     * When building, this will default to 'AccruedPremium'.
     * @param paymentOnDefault  the new value, not null
     * @return this, for chaining, not null
     */
    public Builder paymentOnDefault(PaymentOnDefault paymentOnDefault) {
      JodaBeanUtils.notNull(paymentOnDefault, "paymentOnDefault");
      this.paymentOnDefault = paymentOnDefault;
      return this;
    }

    /**
     * Sets the protection start of the day.
     * <p>
     * When the protection starts on the start date.
     * <p>
     * When building, this will default to 'Beginning'.
     * @param protectionStart  the new value, not null
     * @return this, for chaining, not null
     */
    public Builder protectionStart(ProtectionStartOfDay protectionStart) {
      JodaBeanUtils.notNull(protectionStart, "protectionStart");
      this.protectionStart = protectionStart;
      return this;
    }

    /**
     * Sets the number of days between valuation date and step-in date.
     * <p>
     * The step-in date is also called protection effective date.
     * It is usually 1 calendar day for standardized CDS index contracts.
     * <p>
     * When building, this will default to 1 calendar day.
     * @param stepinDateOffset  the new value, not null
     * @return this, for chaining, not null
     */
    public Builder stepinDateOffset(DaysAdjustment stepinDateOffset) {
      JodaBeanUtils.notNull(stepinDateOffset, "stepinDateOffset");
      this.stepinDateOffset = stepinDateOffset;
      return this;
    }

    /**
     * Sets the number of days between valuation date and settlement date.
     * <p>
     * It is usually 3 business days for standardized CDS index contracts.
     * <p>
     * When building, this will default to 3 business days in the calendar of the payment schedule.
     * @param settlementDateOffset  the new value, not null
     * @return this, for chaining, not null
     */
    public Builder settlementDateOffset(DaysAdjustment settlementDateOffset) {
      JodaBeanUtils.notNull(settlementDateOffset, "settlementDateOffset");
      this.settlementDateOffset = settlementDateOffset;
      return this;
    }

    //-----------------------------------------------------------------------
    @Override
    public String toString() {
      StringBuilder buf = new StringBuilder(416);
      buf.append("CdsIndex.Builder{");
      buf.append("buySell").append('=').append(JodaBeanUtils.toString(buySell)).append(',').append(' ');
      buf.append("cdsIndexId").append('=').append(JodaBeanUtils.toString(cdsIndexId)).append(',').append(' ');
      buf.append("legalEntityIds").append('=').append(JodaBeanUtils.toString(legalEntityIds)).append(',').append(' ');
      buf.append("currency").append('=').append(JodaBeanUtils.toString(currency)).append(',').append(' ');
      buf.append("notional").append('=').append(JodaBeanUtils.toString(notional)).append(',').append(' ');
      buf.append("paymentSchedule").append('=').append(JodaBeanUtils.toString(paymentSchedule)).append(',').append(' ');
      buf.append("fixedRate").append('=').append(JodaBeanUtils.toString(fixedRate)).append(',').append(' ');
      buf.append("dayCount").append('=').append(JodaBeanUtils.toString(dayCount)).append(',').append(' ');
      buf.append("paymentOnDefault").append('=').append(JodaBeanUtils.toString(paymentOnDefault)).append(',').append(' ');
      buf.append("protectionStart").append('=').append(JodaBeanUtils.toString(protectionStart)).append(',').append(' ');
      buf.append("stepinDateOffset").append('=').append(JodaBeanUtils.toString(stepinDateOffset)).append(',').append(' ');
      buf.append("settlementDateOffset").append('=').append(JodaBeanUtils.toString(settlementDateOffset));
      buf.append('}');
      return buf.toString();
    }

  }

  ///CLOVER:ON
  //-------------------------- AUTOGENERATED END --------------------------
}
