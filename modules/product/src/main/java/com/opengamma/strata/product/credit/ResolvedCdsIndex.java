/**
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.product.credit;

import static com.opengamma.strata.collect.Guavate.ensureOnlyOne;

import java.io.Serializable;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;
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
import com.opengamma.strata.basics.StandardId;
import com.opengamma.strata.basics.currency.Currency;
import com.opengamma.strata.basics.date.DayCount;
import com.opengamma.strata.basics.date.DaysAdjustment;
import com.opengamma.strata.product.ResolvedProduct;
import com.opengamma.strata.product.common.BuySell;

/**
 * A CDS (portfolio) index, resolved for pricing.
 * <p>
 * This is the resolved form of {@link CdsIndex} and is an input to the pricers.
 * Applications will typically create a {@code ResolvedCdsIndex} from a {@code CdsIndex}
 * using {@link CdsIndex#resolve(ReferenceData)}.
 */
@BeanDefinition
public final class ResolvedCdsIndex
    implements ResolvedProduct, ImmutableBean, Serializable {

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
   * The periodic payments based on the fixed rate.
   * <p>
   * Each payment period represents part of the life-time of the leg.
   * In most cases, the periods do not overlap. However, since each payment period
   * is essentially independent the data model allows overlapping periods.
   */
  @PropertyDefinition(validate = "notEmpty")
  private final ImmutableList<CreditCouponPaymentPeriod> periodicPayments;
  /**
   * The protection end date.
   * <p>
   * This may be different from the accrual end date of the last payment period in {@code periodicPayments}.
   */
  @PropertyDefinition(validate = "notNull")
  private final LocalDate protectionEndDate;
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
   * Obtains the effective start date from the step-in date. 
   * 
   * @param stepinDate  the step-in date
   * @return the effective start date
   */
  public LocalDate calculateEffectiveStartDate(LocalDate stepinDate) {
    LocalDate startDate = stepinDate.isAfter(getAccrualStartDate()) ? stepinDate : getAccrualStartDate();
    return protectionStart.isBeginning() ? startDate.minusDays(1) : startDate;
  }

  /**
   * Obtains the accrual start date.
   * <p>
   * In general this is different from the protection start date. 
   * Use {@code stepinDateOffset} to compute the protection start date.
   * 
   * @return the accrual start date
   */
  public LocalDate getAccrualStartDate() {
    return periodicPayments.get(0).getStartDate();
  }

  /**
   * Obtains the accrual end date.
   * 
   * @return the accrual end date
   */
  public LocalDate getAccrualEndDate() {
    return periodicPayments.get(periodicPayments.size() - 1).getEndDate();
  }

  /**
   * Obtains the notional.
   * 
   * @return the notional
   */
  public double getNotional() {
    return periodicPayments.get(0).getNotional();
  }

  /**
   * Obtains the currency.
   * 
   * @return the currency
   */
  public Currency getCurrency() {
    return periodicPayments.get(0).getCurrency();
  }

  /**
   * Obtains the fixed coupon rate.
   * 
   * @return the fixed rate
   */
  public double getFixedRate() {
    return periodicPayments.get(0).getFixedRate();
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
  public Optional<CreditCouponPaymentPeriod> findPeriod(LocalDate date) {
    return periodicPayments.stream()
        .filter(p -> p.contains(date))
        .reduce(ensureOnlyOne());
  }

  /**
   * Calculates the accrued premium per fractional spread for unit notional.
   * 
   * @param stepinDate  the step-in date
   * @return the accrued year fraction
   */
  public double accruedYearFraction(LocalDate stepinDate) {
    if (stepinDate.isBefore(getAccrualStartDate())) {
      return 0d;
    }
    if (stepinDate.isEqual(getAccrualEndDate())) {
      return periodicPayments.get(periodicPayments.size() - 1).getYearFraction();
    }
    CreditCouponPaymentPeriod period = findPeriod(stepinDate)
        .orElseThrow(() -> new IllegalArgumentException("Date outside range"));
    return dayCount.relativeYearFraction(period.getStartDate(), stepinDate);
  }

  //------------------------- AUTOGENERATED START -------------------------
  ///CLOVER:OFF
  /**
   * The meta-bean for {@code ResolvedCdsIndex}.
   * @return the meta-bean, not null
   */
  public static ResolvedCdsIndex.Meta meta() {
    return ResolvedCdsIndex.Meta.INSTANCE;
  }

  static {
    JodaBeanUtils.registerMetaBean(ResolvedCdsIndex.Meta.INSTANCE);
  }

  /**
   * The serialization version id.
   */
  private static final long serialVersionUID = 1L;

  /**
   * Returns a builder used to create an instance of the bean.
   * @return the builder, not null
   */
  public static ResolvedCdsIndex.Builder builder() {
    return new ResolvedCdsIndex.Builder();
  }

  private ResolvedCdsIndex(
      BuySell buySell,
      StandardId cdsIndexId,
      List<StandardId> referenceEntityIds,
      List<CreditCouponPaymentPeriod> periodicPayments,
      LocalDate protectionEndDate,
      DayCount dayCount,
      PaymentOnDefault paymentOnDefault,
      ProtectionStartOfDay protectionStart,
      DaysAdjustment stepinDateOffset,
      DaysAdjustment settlementDateOffset) {
    JodaBeanUtils.notNull(buySell, "buySell");
    JodaBeanUtils.notNull(cdsIndexId, "cdsIndexId");
    JodaBeanUtils.notNull(referenceEntityIds, "referenceEntityIds");
    JodaBeanUtils.notEmpty(periodicPayments, "periodicPayments");
    JodaBeanUtils.notNull(protectionEndDate, "protectionEndDate");
    JodaBeanUtils.notNull(dayCount, "dayCount");
    JodaBeanUtils.notNull(paymentOnDefault, "paymentOnDefault");
    JodaBeanUtils.notNull(protectionStart, "protectionStart");
    JodaBeanUtils.notNull(stepinDateOffset, "stepinDateOffset");
    JodaBeanUtils.notNull(settlementDateOffset, "settlementDateOffset");
    this.buySell = buySell;
    this.cdsIndexId = cdsIndexId;
    this.referenceEntityIds = ImmutableList.copyOf(referenceEntityIds);
    this.periodicPayments = ImmutableList.copyOf(periodicPayments);
    this.protectionEndDate = protectionEndDate;
    this.dayCount = dayCount;
    this.paymentOnDefault = paymentOnDefault;
    this.protectionStart = protectionStart;
    this.stepinDateOffset = stepinDateOffset;
    this.settlementDateOffset = settlementDateOffset;
  }

  @Override
  public ResolvedCdsIndex.Meta metaBean() {
    return ResolvedCdsIndex.Meta.INSTANCE;
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
   * Gets the periodic payments based on the fixed rate.
   * <p>
   * Each payment period represents part of the life-time of the leg.
   * In most cases, the periods do not overlap. However, since each payment period
   * is essentially independent the data model allows overlapping periods.
   * @return the value of the property, not empty
   */
  public ImmutableList<CreditCouponPaymentPeriod> getPeriodicPayments() {
    return periodicPayments;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the protection end date.
   * <p>
   * This may be different from the accrual end date of the last payment period in {@code periodicPayments}.
   * @return the value of the property, not null
   */
  public LocalDate getProtectionEndDate() {
    return protectionEndDate;
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
      ResolvedCdsIndex other = (ResolvedCdsIndex) obj;
      return JodaBeanUtils.equal(buySell, other.buySell) &&
          JodaBeanUtils.equal(cdsIndexId, other.cdsIndexId) &&
          JodaBeanUtils.equal(referenceEntityIds, other.referenceEntityIds) &&
          JodaBeanUtils.equal(periodicPayments, other.periodicPayments) &&
          JodaBeanUtils.equal(protectionEndDate, other.protectionEndDate) &&
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
    hash = hash * 31 + JodaBeanUtils.hashCode(periodicPayments);
    hash = hash * 31 + JodaBeanUtils.hashCode(protectionEndDate);
    hash = hash * 31 + JodaBeanUtils.hashCode(dayCount);
    hash = hash * 31 + JodaBeanUtils.hashCode(paymentOnDefault);
    hash = hash * 31 + JodaBeanUtils.hashCode(protectionStart);
    hash = hash * 31 + JodaBeanUtils.hashCode(stepinDateOffset);
    hash = hash * 31 + JodaBeanUtils.hashCode(settlementDateOffset);
    return hash;
  }

  @Override
  public String toString() {
    StringBuilder buf = new StringBuilder(352);
    buf.append("ResolvedCdsIndex{");
    buf.append("buySell").append('=').append(buySell).append(',').append(' ');
    buf.append("cdsIndexId").append('=').append(cdsIndexId).append(',').append(' ');
    buf.append("referenceEntityIds").append('=').append(referenceEntityIds).append(',').append(' ');
    buf.append("periodicPayments").append('=').append(periodicPayments).append(',').append(' ');
    buf.append("protectionEndDate").append('=').append(protectionEndDate).append(',').append(' ');
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
   * The meta-bean for {@code ResolvedCdsIndex}.
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
        this, "buySell", ResolvedCdsIndex.class, BuySell.class);
    /**
     * The meta-property for the {@code cdsIndexId} property.
     */
    private final MetaProperty<StandardId> cdsIndexId = DirectMetaProperty.ofImmutable(
        this, "cdsIndexId", ResolvedCdsIndex.class, StandardId.class);
    /**
     * The meta-property for the {@code referenceEntityIds} property.
     */
    @SuppressWarnings({"unchecked", "rawtypes" })
    private final MetaProperty<ImmutableList<StandardId>> referenceEntityIds = DirectMetaProperty.ofImmutable(
        this, "referenceEntityIds", ResolvedCdsIndex.class, (Class) ImmutableList.class);
    /**
     * The meta-property for the {@code periodicPayments} property.
     */
    @SuppressWarnings({"unchecked", "rawtypes" })
    private final MetaProperty<ImmutableList<CreditCouponPaymentPeriod>> periodicPayments = DirectMetaProperty.ofImmutable(
        this, "periodicPayments", ResolvedCdsIndex.class, (Class) ImmutableList.class);
    /**
     * The meta-property for the {@code protectionEndDate} property.
     */
    private final MetaProperty<LocalDate> protectionEndDate = DirectMetaProperty.ofImmutable(
        this, "protectionEndDate", ResolvedCdsIndex.class, LocalDate.class);
    /**
     * The meta-property for the {@code dayCount} property.
     */
    private final MetaProperty<DayCount> dayCount = DirectMetaProperty.ofImmutable(
        this, "dayCount", ResolvedCdsIndex.class, DayCount.class);
    /**
     * The meta-property for the {@code paymentOnDefault} property.
     */
    private final MetaProperty<PaymentOnDefault> paymentOnDefault = DirectMetaProperty.ofImmutable(
        this, "paymentOnDefault", ResolvedCdsIndex.class, PaymentOnDefault.class);
    /**
     * The meta-property for the {@code protectionStart} property.
     */
    private final MetaProperty<ProtectionStartOfDay> protectionStart = DirectMetaProperty.ofImmutable(
        this, "protectionStart", ResolvedCdsIndex.class, ProtectionStartOfDay.class);
    /**
     * The meta-property for the {@code stepinDateOffset} property.
     */
    private final MetaProperty<DaysAdjustment> stepinDateOffset = DirectMetaProperty.ofImmutable(
        this, "stepinDateOffset", ResolvedCdsIndex.class, DaysAdjustment.class);
    /**
     * The meta-property for the {@code settlementDateOffset} property.
     */
    private final MetaProperty<DaysAdjustment> settlementDateOffset = DirectMetaProperty.ofImmutable(
        this, "settlementDateOffset", ResolvedCdsIndex.class, DaysAdjustment.class);
    /**
     * The meta-properties.
     */
    private final Map<String, MetaProperty<?>> metaPropertyMap$ = new DirectMetaPropertyMap(
        this, null,
        "buySell",
        "cdsIndexId",
        "referenceEntityIds",
        "periodicPayments",
        "protectionEndDate",
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
        case -367345944:  // periodicPayments
          return periodicPayments;
        case -1193325040:  // protectionEndDate
          return protectionEndDate;
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
    public ResolvedCdsIndex.Builder builder() {
      return new ResolvedCdsIndex.Builder();
    }

    @Override
    public Class<? extends ResolvedCdsIndex> beanType() {
      return ResolvedCdsIndex.class;
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
     * The meta-property for the {@code periodicPayments} property.
     * @return the meta-property, not null
     */
    public MetaProperty<ImmutableList<CreditCouponPaymentPeriod>> periodicPayments() {
      return periodicPayments;
    }

    /**
     * The meta-property for the {@code protectionEndDate} property.
     * @return the meta-property, not null
     */
    public MetaProperty<LocalDate> protectionEndDate() {
      return protectionEndDate;
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
          return ((ResolvedCdsIndex) bean).getBuySell();
        case -464117509:  // cdsIndexId
          return ((ResolvedCdsIndex) bean).getCdsIndexId();
        case -315789110:  // referenceEntityIds
          return ((ResolvedCdsIndex) bean).getReferenceEntityIds();
        case -367345944:  // periodicPayments
          return ((ResolvedCdsIndex) bean).getPeriodicPayments();
        case -1193325040:  // protectionEndDate
          return ((ResolvedCdsIndex) bean).getProtectionEndDate();
        case 1905311443:  // dayCount
          return ((ResolvedCdsIndex) bean).getDayCount();
        case -480203780:  // paymentOnDefault
          return ((ResolvedCdsIndex) bean).getPaymentOnDefault();
        case 2103482633:  // protectionStart
          return ((ResolvedCdsIndex) bean).getProtectionStart();
        case 852621746:  // stepinDateOffset
          return ((ResolvedCdsIndex) bean).getStepinDateOffset();
        case 135924714:  // settlementDateOffset
          return ((ResolvedCdsIndex) bean).getSettlementDateOffset();
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
   * The bean-builder for {@code ResolvedCdsIndex}.
   */
  public static final class Builder extends DirectFieldsBeanBuilder<ResolvedCdsIndex> {

    private BuySell buySell;
    private StandardId cdsIndexId;
    private List<StandardId> referenceEntityIds = ImmutableList.of();
    private List<CreditCouponPaymentPeriod> periodicPayments = ImmutableList.of();
    private LocalDate protectionEndDate;
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
    private Builder(ResolvedCdsIndex beanToCopy) {
      this.buySell = beanToCopy.getBuySell();
      this.cdsIndexId = beanToCopy.getCdsIndexId();
      this.referenceEntityIds = beanToCopy.getReferenceEntityIds();
      this.periodicPayments = beanToCopy.getPeriodicPayments();
      this.protectionEndDate = beanToCopy.getProtectionEndDate();
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
        case -367345944:  // periodicPayments
          return periodicPayments;
        case -1193325040:  // protectionEndDate
          return protectionEndDate;
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
        case -367345944:  // periodicPayments
          this.periodicPayments = (List<CreditCouponPaymentPeriod>) newValue;
          break;
        case -1193325040:  // protectionEndDate
          this.protectionEndDate = (LocalDate) newValue;
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
    public ResolvedCdsIndex build() {
      return new ResolvedCdsIndex(
          buySell,
          cdsIndexId,
          referenceEntityIds,
          periodicPayments,
          protectionEndDate,
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
     * Sets the periodic payments based on the fixed rate.
     * <p>
     * Each payment period represents part of the life-time of the leg.
     * In most cases, the periods do not overlap. However, since each payment period
     * is essentially independent the data model allows overlapping periods.
     * @param periodicPayments  the new value, not empty
     * @return this, for chaining, not null
     */
    public Builder periodicPayments(List<CreditCouponPaymentPeriod> periodicPayments) {
      JodaBeanUtils.notEmpty(periodicPayments, "periodicPayments");
      this.periodicPayments = periodicPayments;
      return this;
    }

    /**
     * Sets the {@code periodicPayments} property in the builder
     * from an array of objects.
     * @param periodicPayments  the new value, not empty
     * @return this, for chaining, not null
     */
    public Builder periodicPayments(CreditCouponPaymentPeriod... periodicPayments) {
      return periodicPayments(ImmutableList.copyOf(periodicPayments));
    }

    /**
     * Sets the protection end date.
     * <p>
     * This may be different from the accrual end date of the last payment period in {@code periodicPayments}.
     * @param protectionEndDate  the new value, not null
     * @return this, for chaining, not null
     */
    public Builder protectionEndDate(LocalDate protectionEndDate) {
      JodaBeanUtils.notNull(protectionEndDate, "protectionEndDate");
      this.protectionEndDate = protectionEndDate;
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
      StringBuilder buf = new StringBuilder(352);
      buf.append("ResolvedCdsIndex.Builder{");
      buf.append("buySell").append('=').append(JodaBeanUtils.toString(buySell)).append(',').append(' ');
      buf.append("cdsIndexId").append('=').append(JodaBeanUtils.toString(cdsIndexId)).append(',').append(' ');
      buf.append("referenceEntityIds").append('=').append(JodaBeanUtils.toString(referenceEntityIds)).append(',').append(' ');
      buf.append("periodicPayments").append('=').append(JodaBeanUtils.toString(periodicPayments)).append(',').append(' ');
      buf.append("protectionEndDate").append('=').append(JodaBeanUtils.toString(protectionEndDate)).append(',').append(' ');
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
