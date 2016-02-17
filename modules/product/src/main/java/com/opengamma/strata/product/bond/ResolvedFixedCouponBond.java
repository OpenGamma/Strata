/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.product.bond;

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
import com.opengamma.strata.basics.currency.Currency;
import com.opengamma.strata.basics.currency.Payment;
import com.opengamma.strata.basics.date.DayCount;
import com.opengamma.strata.basics.date.DaysAdjustment;
import com.opengamma.strata.basics.market.ReferenceData;
import com.opengamma.strata.basics.schedule.PeriodicSchedule;
import com.opengamma.strata.collect.id.StandardId;
import com.opengamma.strata.product.ResolvedProduct;

/**
 * A fixed coupon bond, resolved for pricing.
 * <p>
 * This is the resolved form of {@link FixedCouponBond} and is an input to the pricers.
 * Applications will typically create a {@code ResolvedFixedCouponBond} from a {@code FixedCouponBond}
 * using {@link FixedCouponBond#resolve(ReferenceData)}.
 * <p>
 * The list of {@link FixedCouponBondPaymentPeriod} represents the periodic coupon payments,  
 * whereas the nominal payment is defined by {@link Payment}.
 * <p>
 * The legal entity of this fixed coupon bond is identified by {@link StandardId}.
 * The enum, {@link YieldConvention}, specifies the yield computation convention.
 * <p>
 * A {@code ResolvedFixedCouponBond} is bound to data that changes over time, such as holiday calendars.
 * If the data changes, such as the addition of a new holiday, the resolved form will not be updated.
 * Care must be taken when placing the resolved form in a cache or persistence layer.
 */
@BeanDefinition
public final class ResolvedFixedCouponBond
    implements ResolvedProduct, ImmutableBean, Serializable {
  // TODO: periodicSchedule, fixedRate, dayCount, exCouponPeriod copied here from FixedCouponBond

  /**
   * The nominal payment of the product.
   * <p>
   * The payment date of the nominal payment agrees with the final coupon payment date of the periodic payments.
   */
  @PropertyDefinition(validate = "notNull")
  private final Payment nominalPayment;
  /**
   * The periodic payments of the product.
   * <p>
   * Each payment period represents part of the life-time of the product.
   * The start date and end date of the leg are determined from the first and last period.
   * As such, the periods should be sorted.
   */
  @PropertyDefinition(validate = "notNull")
  private final ImmutableList<FixedCouponBondPaymentPeriod> periodicPayments;
  /**
   * The accrual schedule.
   * <p>
   * This is used to define the accrual periods.
   * These are used directly or indirectly to determine other dates in the product.
   */
  @PropertyDefinition(validate = "notNull")
  private final PeriodicSchedule periodicSchedule;
  /**
   * The fixed coupon rate. 
   * <p>
   * The periodic payments are based on this fixed coupon rate.
   */
  @PropertyDefinition
  private final double fixedRate;
  /**
   * The day count convention applicable. 
   * <p>
   * The conversion from dates to a numerical value is made based on this day count. 
   * For the fixed bond, the day count convention is used to compute accrued interest.
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
  private final YieldConvention yieldConvention;
  /**
   * The legal entity identifier.
   * <p>
   * This identifier is used for the legal entity which issues the fixed coupon bond product. 
   */
  @PropertyDefinition(validate = "notNull")
  private final StandardId legalEntityId;
  /**
   * The number of days between valuation date and settlement date. 
   * <p>
   * This is used to compute clean price. 
   * The clean price is the relative price to be paid at the standard settlement date in exchange for the bond.
   * <p>
   * It is usually one business day for US treasuries and UK Gilts and three days for Euroland government bonds. 
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
    return nominalPayment.getAmount();
  }

  //------------------------- AUTOGENERATED START -------------------------
  ///CLOVER:OFF
  /**
   * The meta-bean for {@code ResolvedFixedCouponBond}.
   * @return the meta-bean, not null
   */
  public static ResolvedFixedCouponBond.Meta meta() {
    return ResolvedFixedCouponBond.Meta.INSTANCE;
  }

  static {
    JodaBeanUtils.registerMetaBean(ResolvedFixedCouponBond.Meta.INSTANCE);
  }

  /**
   * The serialization version id.
   */
  private static final long serialVersionUID = 1L;

  /**
   * Returns a builder used to create an instance of the bean.
   * @return the builder, not null
   */
  public static ResolvedFixedCouponBond.Builder builder() {
    return new ResolvedFixedCouponBond.Builder();
  }

  private ResolvedFixedCouponBond(
      Payment nominalPayment,
      List<FixedCouponBondPaymentPeriod> periodicPayments,
      PeriodicSchedule periodicSchedule,
      double fixedRate,
      DayCount dayCount,
      YieldConvention yieldConvention,
      StandardId legalEntityId,
      DaysAdjustment settlementDateOffset,
      DaysAdjustment exCouponPeriod) {
    JodaBeanUtils.notNull(nominalPayment, "nominalPayment");
    JodaBeanUtils.notNull(periodicPayments, "periodicPayments");
    JodaBeanUtils.notNull(periodicSchedule, "periodicSchedule");
    JodaBeanUtils.notNull(dayCount, "dayCount");
    JodaBeanUtils.notNull(yieldConvention, "yieldConvention");
    JodaBeanUtils.notNull(legalEntityId, "legalEntityId");
    JodaBeanUtils.notNull(settlementDateOffset, "settlementDateOffset");
    JodaBeanUtils.notNull(exCouponPeriod, "exCouponPeriod");
    this.nominalPayment = nominalPayment;
    this.periodicPayments = ImmutableList.copyOf(periodicPayments);
    this.periodicSchedule = periodicSchedule;
    this.fixedRate = fixedRate;
    this.dayCount = dayCount;
    this.yieldConvention = yieldConvention;
    this.legalEntityId = legalEntityId;
    this.settlementDateOffset = settlementDateOffset;
    this.exCouponPeriod = exCouponPeriod;
  }

  @Override
  public ResolvedFixedCouponBond.Meta metaBean() {
    return ResolvedFixedCouponBond.Meta.INSTANCE;
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
   * Gets the nominal payment of the product.
   * <p>
   * The payment date of the nominal payment agrees with the final coupon payment date of the periodic payments.
   * @return the value of the property, not null
   */
  public Payment getNominalPayment() {
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
  public ImmutableList<FixedCouponBondPaymentPeriod> getPeriodicPayments() {
    return periodicPayments;
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
   * Gets the fixed coupon rate.
   * <p>
   * The periodic payments are based on this fixed coupon rate.
   * @return the value of the property
   */
  public double getFixedRate() {
    return fixedRate;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the day count convention applicable.
   * <p>
   * The conversion from dates to a numerical value is made based on this day count.
   * For the fixed bond, the day count convention is used to compute accrued interest.
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
  public YieldConvention getYieldConvention() {
    return yieldConvention;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the legal entity identifier.
   * <p>
   * This identifier is used for the legal entity which issues the fixed coupon bond product.
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
   * <p>
   * It is usually one business day for US treasuries and UK Gilts and three days for Euroland government bonds.
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
      ResolvedFixedCouponBond other = (ResolvedFixedCouponBond) obj;
      return JodaBeanUtils.equal(nominalPayment, other.nominalPayment) &&
          JodaBeanUtils.equal(periodicPayments, other.periodicPayments) &&
          JodaBeanUtils.equal(periodicSchedule, other.periodicSchedule) &&
          JodaBeanUtils.equal(fixedRate, other.fixedRate) &&
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
    hash = hash * 31 + JodaBeanUtils.hashCode(nominalPayment);
    hash = hash * 31 + JodaBeanUtils.hashCode(periodicPayments);
    hash = hash * 31 + JodaBeanUtils.hashCode(periodicSchedule);
    hash = hash * 31 + JodaBeanUtils.hashCode(fixedRate);
    hash = hash * 31 + JodaBeanUtils.hashCode(dayCount);
    hash = hash * 31 + JodaBeanUtils.hashCode(yieldConvention);
    hash = hash * 31 + JodaBeanUtils.hashCode(legalEntityId);
    hash = hash * 31 + JodaBeanUtils.hashCode(settlementDateOffset);
    hash = hash * 31 + JodaBeanUtils.hashCode(exCouponPeriod);
    return hash;
  }

  @Override
  public String toString() {
    StringBuilder buf = new StringBuilder(320);
    buf.append("ResolvedFixedCouponBond{");
    buf.append("nominalPayment").append('=').append(nominalPayment).append(',').append(' ');
    buf.append("periodicPayments").append('=').append(periodicPayments).append(',').append(' ');
    buf.append("periodicSchedule").append('=').append(periodicSchedule).append(',').append(' ');
    buf.append("fixedRate").append('=').append(fixedRate).append(',').append(' ');
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
   * The meta-bean for {@code ResolvedFixedCouponBond}.
   */
  public static final class Meta extends DirectMetaBean {
    /**
     * The singleton instance of the meta-bean.
     */
    static final Meta INSTANCE = new Meta();

    /**
     * The meta-property for the {@code nominalPayment} property.
     */
    private final MetaProperty<Payment> nominalPayment = DirectMetaProperty.ofImmutable(
        this, "nominalPayment", ResolvedFixedCouponBond.class, Payment.class);
    /**
     * The meta-property for the {@code periodicPayments} property.
     */
    @SuppressWarnings({"unchecked", "rawtypes" })
    private final MetaProperty<ImmutableList<FixedCouponBondPaymentPeriod>> periodicPayments = DirectMetaProperty.ofImmutable(
        this, "periodicPayments", ResolvedFixedCouponBond.class, (Class) ImmutableList.class);
    /**
     * The meta-property for the {@code periodicSchedule} property.
     */
    private final MetaProperty<PeriodicSchedule> periodicSchedule = DirectMetaProperty.ofImmutable(
        this, "periodicSchedule", ResolvedFixedCouponBond.class, PeriodicSchedule.class);
    /**
     * The meta-property for the {@code fixedRate} property.
     */
    private final MetaProperty<Double> fixedRate = DirectMetaProperty.ofImmutable(
        this, "fixedRate", ResolvedFixedCouponBond.class, Double.TYPE);
    /**
     * The meta-property for the {@code dayCount} property.
     */
    private final MetaProperty<DayCount> dayCount = DirectMetaProperty.ofImmutable(
        this, "dayCount", ResolvedFixedCouponBond.class, DayCount.class);
    /**
     * The meta-property for the {@code yieldConvention} property.
     */
    private final MetaProperty<YieldConvention> yieldConvention = DirectMetaProperty.ofImmutable(
        this, "yieldConvention", ResolvedFixedCouponBond.class, YieldConvention.class);
    /**
     * The meta-property for the {@code legalEntityId} property.
     */
    private final MetaProperty<StandardId> legalEntityId = DirectMetaProperty.ofImmutable(
        this, "legalEntityId", ResolvedFixedCouponBond.class, StandardId.class);
    /**
     * The meta-property for the {@code settlementDateOffset} property.
     */
    private final MetaProperty<DaysAdjustment> settlementDateOffset = DirectMetaProperty.ofImmutable(
        this, "settlementDateOffset", ResolvedFixedCouponBond.class, DaysAdjustment.class);
    /**
     * The meta-property for the {@code exCouponPeriod} property.
     */
    private final MetaProperty<DaysAdjustment> exCouponPeriod = DirectMetaProperty.ofImmutable(
        this, "exCouponPeriod", ResolvedFixedCouponBond.class, DaysAdjustment.class);
    /**
     * The meta-properties.
     */
    private final Map<String, MetaProperty<?>> metaPropertyMap$ = new DirectMetaPropertyMap(
        this, null,
        "nominalPayment",
        "periodicPayments",
        "periodicSchedule",
        "fixedRate",
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
        case -44199542:  // nominalPayment
          return nominalPayment;
        case -367345944:  // periodicPayments
          return periodicPayments;
        case 1847018066:  // periodicSchedule
          return periodicSchedule;
        case 747425396:  // fixedRate
          return fixedRate;
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
    public ResolvedFixedCouponBond.Builder builder() {
      return new ResolvedFixedCouponBond.Builder();
    }

    @Override
    public Class<? extends ResolvedFixedCouponBond> beanType() {
      return ResolvedFixedCouponBond.class;
    }

    @Override
    public Map<String, MetaProperty<?>> metaPropertyMap() {
      return metaPropertyMap$;
    }

    //-----------------------------------------------------------------------
    /**
     * The meta-property for the {@code nominalPayment} property.
     * @return the meta-property, not null
     */
    public MetaProperty<Payment> nominalPayment() {
      return nominalPayment;
    }

    /**
     * The meta-property for the {@code periodicPayments} property.
     * @return the meta-property, not null
     */
    public MetaProperty<ImmutableList<FixedCouponBondPaymentPeriod>> periodicPayments() {
      return periodicPayments;
    }

    /**
     * The meta-property for the {@code periodicSchedule} property.
     * @return the meta-property, not null
     */
    public MetaProperty<PeriodicSchedule> periodicSchedule() {
      return periodicSchedule;
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
     * The meta-property for the {@code yieldConvention} property.
     * @return the meta-property, not null
     */
    public MetaProperty<YieldConvention> yieldConvention() {
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
        case -44199542:  // nominalPayment
          return ((ResolvedFixedCouponBond) bean).getNominalPayment();
        case -367345944:  // periodicPayments
          return ((ResolvedFixedCouponBond) bean).getPeriodicPayments();
        case 1847018066:  // periodicSchedule
          return ((ResolvedFixedCouponBond) bean).getPeriodicSchedule();
        case 747425396:  // fixedRate
          return ((ResolvedFixedCouponBond) bean).getFixedRate();
        case 1905311443:  // dayCount
          return ((ResolvedFixedCouponBond) bean).getDayCount();
        case -1895216418:  // yieldConvention
          return ((ResolvedFixedCouponBond) bean).getYieldConvention();
        case 866287159:  // legalEntityId
          return ((ResolvedFixedCouponBond) bean).getLegalEntityId();
        case 135924714:  // settlementDateOffset
          return ((ResolvedFixedCouponBond) bean).getSettlementDateOffset();
        case 1408037338:  // exCouponPeriod
          return ((ResolvedFixedCouponBond) bean).getExCouponPeriod();
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
   * The bean-builder for {@code ResolvedFixedCouponBond}.
   */
  public static final class Builder extends DirectFieldsBeanBuilder<ResolvedFixedCouponBond> {

    private Payment nominalPayment;
    private List<FixedCouponBondPaymentPeriod> periodicPayments = ImmutableList.of();
    private PeriodicSchedule periodicSchedule;
    private double fixedRate;
    private DayCount dayCount;
    private YieldConvention yieldConvention;
    private StandardId legalEntityId;
    private DaysAdjustment settlementDateOffset;
    private DaysAdjustment exCouponPeriod;

    /**
     * Restricted constructor.
     */
    private Builder() {
    }

    /**
     * Restricted copy constructor.
     * @param beanToCopy  the bean to copy from, not null
     */
    private Builder(ResolvedFixedCouponBond beanToCopy) {
      this.nominalPayment = beanToCopy.getNominalPayment();
      this.periodicPayments = beanToCopy.getPeriodicPayments();
      this.periodicSchedule = beanToCopy.getPeriodicSchedule();
      this.fixedRate = beanToCopy.getFixedRate();
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
        case -44199542:  // nominalPayment
          return nominalPayment;
        case -367345944:  // periodicPayments
          return periodicPayments;
        case 1847018066:  // periodicSchedule
          return periodicSchedule;
        case 747425396:  // fixedRate
          return fixedRate;
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

    @SuppressWarnings("unchecked")
    @Override
    public Builder set(String propertyName, Object newValue) {
      switch (propertyName.hashCode()) {
        case -44199542:  // nominalPayment
          this.nominalPayment = (Payment) newValue;
          break;
        case -367345944:  // periodicPayments
          this.periodicPayments = (List<FixedCouponBondPaymentPeriod>) newValue;
          break;
        case 1847018066:  // periodicSchedule
          this.periodicSchedule = (PeriodicSchedule) newValue;
          break;
        case 747425396:  // fixedRate
          this.fixedRate = (Double) newValue;
          break;
        case 1905311443:  // dayCount
          this.dayCount = (DayCount) newValue;
          break;
        case -1895216418:  // yieldConvention
          this.yieldConvention = (YieldConvention) newValue;
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
    public ResolvedFixedCouponBond build() {
      return new ResolvedFixedCouponBond(
          nominalPayment,
          periodicPayments,
          periodicSchedule,
          fixedRate,
          dayCount,
          yieldConvention,
          legalEntityId,
          settlementDateOffset,
          exCouponPeriod);
    }

    //-----------------------------------------------------------------------
    /**
     * Sets the nominal payment of the product.
     * <p>
     * The payment date of the nominal payment agrees with the final coupon payment date of the periodic payments.
     * @param nominalPayment  the new value, not null
     * @return this, for chaining, not null
     */
    public Builder nominalPayment(Payment nominalPayment) {
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
    public Builder periodicPayments(List<FixedCouponBondPaymentPeriod> periodicPayments) {
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
    public Builder periodicPayments(FixedCouponBondPaymentPeriod... periodicPayments) {
      return periodicPayments(ImmutableList.copyOf(periodicPayments));
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
     * Sets the fixed coupon rate.
     * <p>
     * The periodic payments are based on this fixed coupon rate.
     * @param fixedRate  the new value
     * @return this, for chaining, not null
     */
    public Builder fixedRate(double fixedRate) {
      this.fixedRate = fixedRate;
      return this;
    }

    /**
     * Sets the day count convention applicable.
     * <p>
     * The conversion from dates to a numerical value is made based on this day count.
     * For the fixed bond, the day count convention is used to compute accrued interest.
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
    public Builder yieldConvention(YieldConvention yieldConvention) {
      JodaBeanUtils.notNull(yieldConvention, "yieldConvention");
      this.yieldConvention = yieldConvention;
      return this;
    }

    /**
     * Sets the legal entity identifier.
     * <p>
     * This identifier is used for the legal entity which issues the fixed coupon bond product.
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
     * <p>
     * It is usually one business day for US treasuries and UK Gilts and three days for Euroland government bonds.
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
      StringBuilder buf = new StringBuilder(320);
      buf.append("ResolvedFixedCouponBond.Builder{");
      buf.append("nominalPayment").append('=').append(JodaBeanUtils.toString(nominalPayment)).append(',').append(' ');
      buf.append("periodicPayments").append('=').append(JodaBeanUtils.toString(periodicPayments)).append(',').append(' ');
      buf.append("periodicSchedule").append('=').append(JodaBeanUtils.toString(periodicSchedule)).append(',').append(' ');
      buf.append("fixedRate").append('=').append(JodaBeanUtils.toString(fixedRate)).append(',').append(' ');
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
