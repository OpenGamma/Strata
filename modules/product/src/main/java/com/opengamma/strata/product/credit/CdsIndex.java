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
 * The contract periodically pays fixed coupons to the index buyer until the expiry, 
 * and in return, the index buyer receives the bond of a defaulted constituent legal entity for par.  
 */
@BeanDefinition
public final class CdsIndex
    implements Resolvable<ResolvedCdsIndex>, ImmutableBean, Serializable {

  /**
   * Whether the CDS index is buy or sell.
   * <p>
   * A value of 'Buy' implies buying credit risk, where the fixed coupon is received
   * and the protection is paid  in the event of default.
   * A value of 'Sell' implies selling credit risk, where the fixed coupon is paid
   * and the protection is received in the event of default. 
   */
  @PropertyDefinition(validate = "notNull")
  private final BuySell buySell;
  /**
   * The CDS index identifier.
   * <p>
   * This identifier is used for referring this CDS index product.
   */
  @PropertyDefinition(validate = "notNull")
  private final StandardId cdsIndexId;
  /**
   * The legal entity identifiers.
   * <p>
   * This identifiers are used for the reference legal entities of the CDS index.
   */
  @PropertyDefinition(validate = "notNull")
  private final ImmutableList<StandardId> referenceEntityIds;
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
   * The accrual schedule.
   * <p>
   * This is used to define the accrual periods.
   */
  @PropertyDefinition(validate = "notNull")
  private final PeriodicSchedule accrualSchedule;
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
   */
  @PropertyDefinition(validate = "notNull")
  private final DayCount dayCount;
  /**
   * The payment on default.
   * <p>
   * Whether the accrued premium is paid in the event of a default.
   */
  @PropertyDefinition(validate = "notNull")
  private final PaymentOnDefault paymentOnDefault;
  /**
   * The protection start of the day.
   * <p>
   * When the protection starts on the start date.
   */
  @PropertyDefinition(validate = "notNull")
  private final ProtectionStartOfDay protectionStart;
  /**
   * The number of days between valuation date and step-in date.
   * <p>
   * The step-in date is also called protection effective date. 
   * It is usually 1 calendar day for standardized CDS index contracts. 
   */
  @PropertyDefinition(validate = "notNull")
  private final DaysAdjustment stepinDateOffset;
  /**
   * The number of days between valuation date and settlement date.
   * <p>
   * It is usually 3 business days for standardized CDS index contracts.
   */
  @PropertyDefinition(validate = "notNull")
  private final DaysAdjustment settlementDateOffset;

  //-------------------------------------------------------------------------
  /**
   * Creates an instance.
   * <p>
   * The start date adjustment, end date adjustment, and roll convention are switched off. 
   * Use {@link #builder()} for the full flexibility.
   * 
   * @param buySell  buy or sell
   * @param cdsIndexId  the CDS index ID
   * @param referenceEntityIds  the legal entity IDs
   * @param currency  the currency
   * @param notional  the notional 
   * @param startDate  the start date
   * @param endDate  the end date
   * @param paymentFrequency  the coupon frequency
   * @param businessDayAdjustment  the business day adjustment
   * @param stubConvention  the stub convention
   * @param fixedRate  the fixed coupon rate
   * @param dayCount  the day count convention
   * @param paymentOnDefault  the payment on default
   * @param protectStart  the protection start of the day
   * @param stepinDateOffset  the step-in date offset
   * @param settlementDateOffset  the settlement date offset
   * @return the instance
   */
  public static CdsIndex of(
      BuySell buySell,
      StandardId cdsIndexId,
      List<StandardId> referenceEntityIds,
      Currency currency,
      double notional,
      LocalDate startDate,
      LocalDate endDate,
      Frequency paymentFrequency,
      BusinessDayAdjustment businessDayAdjustment,
      StubConvention stubConvention,
      double fixedRate,
      DayCount dayCount,
      PaymentOnDefault paymentOnDefault,
      ProtectionStartOfDay protectStart,
      DaysAdjustment stepinDateOffset,
      DaysAdjustment settlementDateOffset) {

    PeriodicSchedule accrualSchedule = PeriodicSchedule.builder()
        .businessDayAdjustment(businessDayAdjustment)
        .startDate(startDate)
        .endDate(endDate)
        .startDateBusinessDayAdjustment(BusinessDayAdjustment.NONE)
        .endDateBusinessDayAdjustment(BusinessDayAdjustment.NONE)
        .frequency(paymentFrequency)
        .rollConvention(RollConventions.NONE)
        .stubConvention(stubConvention)
        .build();
    return new CdsIndex(buySell, cdsIndexId, referenceEntityIds, currency, notional, accrualSchedule, fixedRate, dayCount,
        paymentOnDefault, protectStart, stepinDateOffset, settlementDateOffset);
  }

  /**
   * Creates an instance of standardized CDS index.
   * 
   * @param buySell  buy or sell
   * @param cdsIndexId  the CDS index ID
   * @param referenceEntityIds  the legal entity IDs
   * @param currency  the currency
   * @param notional  the notional
   * @param startDate  the start date
   * @param endDate  the end date
   * @param calendar  the calendar
   * @param fixedRate  the fixed coupon rate
   * @return the instance
   */
  public static CdsIndex of(
      BuySell buySell,
      StandardId cdsIndexId,
      List<StandardId> referenceEntityIds,
      Currency currency,
      double notional,
      LocalDate startDate,
      LocalDate endDate,
      HolidayCalendarId calendar,
      double fixedRate) {

    return of(buySell, cdsIndexId, referenceEntityIds, currency, notional, startDate, endDate, Frequency.P3M,
        BusinessDayAdjustment.of(BusinessDayConventions.FOLLOWING, calendar), StubConvention.SHORT_INITIAL,
        fixedRate, DayCounts.ACT_360, PaymentOnDefault.ACCRUED_PREMIUM, ProtectionStartOfDay.BEGINNING,
        DaysAdjustment.ofCalendarDays(1), DaysAdjustment.ofBusinessDays(3, calendar));
  }

  //-------------------------------------------------------------------------
  @Override
  public ResolvedCdsIndex resolve(ReferenceData refData) {
    Schedule adjustedSchedule = accrualSchedule.createSchedule(refData);
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
        .paymentDate(accrualSchedule.getBusinessDayAdjustment().adjust(lastPeriod.getEndDate(), refData))
        .notional(notional)
        .currency(currency)
        .fixedRate(fixedRate)
        .yearFraction(modifiedPeriod.yearFraction(dayCount, adjustedSchedule))
        .build());
    ImmutableList<CreditCouponPaymentPeriod> periodicPayments = accrualPeriods.build();

    return ResolvedCdsIndex.builder()
        .buySell(buySell)
        .cdsIndexId(cdsIndexId)
        .referenceEntityIds(referenceEntityIds)
        .protectionStart(protectionStart)
        .paymentOnDefault(paymentOnDefault)
        .periodicPayments(periodicPayments)
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
      List<StandardId> referenceEntityIds,
      Currency currency,
      double notional,
      PeriodicSchedule accrualSchedule,
      double fixedRate,
      DayCount dayCount,
      PaymentOnDefault paymentOnDefault,
      ProtectionStartOfDay protectionStart,
      DaysAdjustment stepinDateOffset,
      DaysAdjustment settlementDateOffset) {
    JodaBeanUtils.notNull(buySell, "buySell");
    JodaBeanUtils.notNull(cdsIndexId, "cdsIndexId");
    JodaBeanUtils.notNull(referenceEntityIds, "referenceEntityIds");
    JodaBeanUtils.notNull(currency, "currency");
    ArgChecker.notNegativeOrZero(notional, "notional");
    JodaBeanUtils.notNull(accrualSchedule, "accrualSchedule");
    ArgChecker.notNegative(fixedRate, "fixedRate");
    JodaBeanUtils.notNull(dayCount, "dayCount");
    JodaBeanUtils.notNull(paymentOnDefault, "paymentOnDefault");
    JodaBeanUtils.notNull(protectionStart, "protectionStart");
    JodaBeanUtils.notNull(stepinDateOffset, "stepinDateOffset");
    JodaBeanUtils.notNull(settlementDateOffset, "settlementDateOffset");
    this.buySell = buySell;
    this.cdsIndexId = cdsIndexId;
    this.referenceEntityIds = ImmutableList.copyOf(referenceEntityIds);
    this.currency = currency;
    this.notional = notional;
    this.accrualSchedule = accrualSchedule;
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
   * A value of 'Buy' implies buying credit risk, where the fixed coupon is received
   * and the protection is paid  in the event of default.
   * A value of 'Sell' implies selling credit risk, where the fixed coupon is paid
   * and the protection is received in the event of default.
   * @return the value of the property, not null
   */
  public BuySell getBuySell() {
    return buySell;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the CDS index identifier.
   * <p>
   * This identifier is used for referring this CDS index product.
   * @return the value of the property, not null
   */
  public StandardId getCdsIndexId() {
    return cdsIndexId;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the legal entity identifiers.
   * <p>
   * This identifiers are used for the reference legal entities of the CDS index.
   * @return the value of the property, not null
   */
  public ImmutableList<StandardId> getReferenceEntityIds() {
    return referenceEntityIds;
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
   * Gets the accrual schedule.
   * <p>
   * This is used to define the accrual periods.
   * @return the value of the property, not null
   */
  public PeriodicSchedule getAccrualSchedule() {
    return accrualSchedule;
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
          JodaBeanUtils.equal(referenceEntityIds, other.referenceEntityIds) &&
          JodaBeanUtils.equal(currency, other.currency) &&
          JodaBeanUtils.equal(notional, other.notional) &&
          JodaBeanUtils.equal(accrualSchedule, other.accrualSchedule) &&
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
    hash = hash * 31 + JodaBeanUtils.hashCode(referenceEntityIds);
    hash = hash * 31 + JodaBeanUtils.hashCode(currency);
    hash = hash * 31 + JodaBeanUtils.hashCode(notional);
    hash = hash * 31 + JodaBeanUtils.hashCode(accrualSchedule);
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
    buf.append("referenceEntityIds").append('=').append(referenceEntityIds).append(',').append(' ');
    buf.append("currency").append('=').append(currency).append(',').append(' ');
    buf.append("notional").append('=').append(notional).append(',').append(' ');
    buf.append("accrualSchedule").append('=').append(accrualSchedule).append(',').append(' ');
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
     * The meta-property for the {@code referenceEntityIds} property.
     */
    @SuppressWarnings({"unchecked", "rawtypes" })
    private final MetaProperty<ImmutableList<StandardId>> referenceEntityIds = DirectMetaProperty.ofImmutable(
        this, "referenceEntityIds", CdsIndex.class, (Class) ImmutableList.class);
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
     * The meta-property for the {@code accrualSchedule} property.
     */
    private final MetaProperty<PeriodicSchedule> accrualSchedule = DirectMetaProperty.ofImmutable(
        this, "accrualSchedule", CdsIndex.class, PeriodicSchedule.class);
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
        "referenceEntityIds",
        "currency",
        "notional",
        "accrualSchedule",
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
        case -315789110:  // referenceEntityIds
          return referenceEntityIds;
        case 575402001:  // currency
          return currency;
        case 1585636160:  // notional
          return notional;
        case 304659814:  // accrualSchedule
          return accrualSchedule;
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
     * The meta-property for the {@code referenceEntityIds} property.
     * @return the meta-property, not null
     */
    public MetaProperty<ImmutableList<StandardId>> referenceEntityIds() {
      return referenceEntityIds;
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
        case -315789110:  // referenceEntityIds
          return ((CdsIndex) bean).getReferenceEntityIds();
        case 575402001:  // currency
          return ((CdsIndex) bean).getCurrency();
        case 1585636160:  // notional
          return ((CdsIndex) bean).getNotional();
        case 304659814:  // accrualSchedule
          return ((CdsIndex) bean).getAccrualSchedule();
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
    private List<StandardId> referenceEntityIds = ImmutableList.of();
    private Currency currency;
    private double notional;
    private PeriodicSchedule accrualSchedule;
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
    }

    /**
     * Restricted copy constructor.
     * @param beanToCopy  the bean to copy from, not null
     */
    private Builder(CdsIndex beanToCopy) {
      this.buySell = beanToCopy.getBuySell();
      this.cdsIndexId = beanToCopy.getCdsIndexId();
      this.referenceEntityIds = beanToCopy.getReferenceEntityIds();
      this.currency = beanToCopy.getCurrency();
      this.notional = beanToCopy.getNotional();
      this.accrualSchedule = beanToCopy.getAccrualSchedule();
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
        case -315789110:  // referenceEntityIds
          return referenceEntityIds;
        case 575402001:  // currency
          return currency;
        case 1585636160:  // notional
          return notional;
        case 304659814:  // accrualSchedule
          return accrualSchedule;
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
        case -315789110:  // referenceEntityIds
          this.referenceEntityIds = (List<StandardId>) newValue;
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
      return new CdsIndex(
          buySell,
          cdsIndexId,
          referenceEntityIds,
          currency,
          notional,
          accrualSchedule,
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
     * A value of 'Buy' implies buying credit risk, where the fixed coupon is received
     * and the protection is paid  in the event of default.
     * A value of 'Sell' implies selling credit risk, where the fixed coupon is paid
     * and the protection is received in the event of default.
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
     * This identifier is used for referring this CDS index product.
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
     * This identifiers are used for the reference legal entities of the CDS index.
     * @param referenceEntityIds  the new value, not null
     * @return this, for chaining, not null
     */
    public Builder referenceEntityIds(List<StandardId> referenceEntityIds) {
      JodaBeanUtils.notNull(referenceEntityIds, "referenceEntityIds");
      this.referenceEntityIds = referenceEntityIds;
      return this;
    }

    /**
     * Sets the {@code referenceEntityIds} property in the builder
     * from an array of objects.
     * @param referenceEntityIds  the new value, not null
     * @return this, for chaining, not null
     */
    public Builder referenceEntityIds(StandardId... referenceEntityIds) {
      return referenceEntityIds(ImmutableList.copyOf(referenceEntityIds));
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
     * Sets the accrual schedule.
     * <p>
     * This is used to define the accrual periods.
     * @param accrualSchedule  the new value, not null
     * @return this, for chaining, not null
     */
    public Builder accrualSchedule(PeriodicSchedule accrualSchedule) {
      JodaBeanUtils.notNull(accrualSchedule, "accrualSchedule");
      this.accrualSchedule = accrualSchedule;
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
      buf.append("referenceEntityIds").append('=').append(JodaBeanUtils.toString(referenceEntityIds)).append(',').append(' ');
      buf.append("currency").append('=').append(JodaBeanUtils.toString(currency)).append(',').append(' ');
      buf.append("notional").append('=').append(JodaBeanUtils.toString(notional)).append(',').append(' ');
      buf.append("accrualSchedule").append('=').append(JodaBeanUtils.toString(accrualSchedule)).append(',').append(' ');
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
