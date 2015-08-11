/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.finance.rate.bond;

import java.io.Serializable;
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
import com.opengamma.strata.basics.currency.CurrencyAmount;
import com.opengamma.strata.basics.currency.Payment;
import com.opengamma.strata.basics.date.DayCount;
import com.opengamma.strata.basics.date.DaysAdjustment;
import com.opengamma.strata.basics.schedule.PeriodicSchedule;
import com.opengamma.strata.basics.schedule.Schedule;
import com.opengamma.strata.basics.schedule.SchedulePeriod;
import com.opengamma.strata.collect.id.StandardId;

/**
 * A fixed coupon bond.
 * <p>
 * A fixed coupon bond is a financial instrument that represents a stream of fixed payments. 
 * The payments consist two types: periodic coupon payments and nominal payment.
 * The periodic payments are made {@code n} times a year with a fixed coupon rate at individual coupon dates.   
 * The nominal payment is the unique payment at the final coupon date.
 * <p>
 * The periodic coupon payment schedule is defined using {@link PeriodicSchedule}. 
 * The payment amount is computed with {@code fixedRate} and {@code notionalAmount}. 
 * The nominal payment is defined from the last period of the periodic coupon payment schedule and {@code notionalAmount}. 
 * <p>
 * The accrual factor between two dates is computed {@code dayCount}. 
 * The legal entity of this fixed coupon bond is identified by {@link StandardId}. The enum, {@link YieldConvention}, 
 * specifies the yield computation convention, and {@link DaysAdjustment} does the number of days between valuation 
 * date and settlement date. 
 * </ul>
 */
@BeanDefinition
public final class FixedCouponBond
    implements FixedCouponBondProduct, ImmutableBean, Serializable {

  /**
   * The notional amount of the product.
   * <p>
   * The amount is signed.
   * A positive amount indicates the payments are to be received.
   * A negative amount indicates the payments are to be paid.
   * <p>
   * This must be specified in one of the two currencies of the forward.
   */
  @PropertyDefinition(validate = "notNull")
  private final CurrencyAmount notionalAmount;
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
   */
  @PropertyDefinition(validate = "notNull")
  private final DayCount dayCount;
  /**
   * Yield convention.
   * <p>
   * The convention defines accrued interest calculation type of the product.  
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

  @Override
  public ExpandedFixedCouponBond expand() {
    Schedule accrualSchedule = periodicSchedule.createSchedule();
    ImmutableList.Builder<FixedCouponBondPaymentPeriod> accrualPeriods = ImmutableList.builder();
    for (int i = 0; i < accrualSchedule.size(); i++) {
      SchedulePeriod period = accrualSchedule.getPeriod(i);
      accrualPeriods.add(FixedCouponBondPaymentPeriod.builder()
          .unadjustedStartDate(period.getUnadjustedStartDate())
          .unadjustedEndDate(period.getUnadjustedEndDate())
          .startDate(period.getStartDate())
          .endDate(period.getEndDate())
          .notional(notionalAmount.getAmount())
          .currency(notionalAmount.getCurrency())
          .fixedRate(fixedRate)
          .build());
    }
    ImmutableList<FixedCouponBondPaymentPeriod> periodicPayments = accrualPeriods.build();
    FixedCouponBondPaymentPeriod lastPeriod = periodicPayments.get(periodicPayments.size() - 1);
    Payment nominalPayment = Payment.of(notionalAmount, lastPeriod.getPaymentDate());
    return ExpandedFixedCouponBond.builder()
        .legalEntityId(legalEntityId)
        .nominalPayment(nominalPayment)
        .periodicPayments(ImmutableList.copyOf(periodicPayments))
        .dayCount(dayCount)
        .yieldConvention(yieldConvention)
        .settlementDateOffset(settlementDateOffset)
        .build();
  }

  //------------------------- AUTOGENERATED START -------------------------
  ///CLOVER:OFF
  /**
   * The meta-bean for {@code FixedCouponBond}.
   * @return the meta-bean, not null
   */
  public static FixedCouponBond.Meta meta() {
    return FixedCouponBond.Meta.INSTANCE;
  }

  static {
    JodaBeanUtils.registerMetaBean(FixedCouponBond.Meta.INSTANCE);
  }

  /**
   * The serialization version id.
   */
  private static final long serialVersionUID = 1L;

  /**
   * Returns a builder used to create an instance of the bean.
   * @return the builder, not null
   */
  public static FixedCouponBond.Builder builder() {
    return new FixedCouponBond.Builder();
  }

  private FixedCouponBond(
      CurrencyAmount notionalAmount,
      PeriodicSchedule periodicSchedule,
      double fixedRate,
      DayCount dayCount,
      YieldConvention yieldConvention,
      StandardId legalEntityId,
      DaysAdjustment settlementDateOffset) {
    JodaBeanUtils.notNull(notionalAmount, "notionalAmount");
    JodaBeanUtils.notNull(periodicSchedule, "periodicSchedule");
    JodaBeanUtils.notNull(dayCount, "dayCount");
    JodaBeanUtils.notNull(yieldConvention, "yieldConvention");
    JodaBeanUtils.notNull(legalEntityId, "legalEntityId");
    JodaBeanUtils.notNull(settlementDateOffset, "settlementDateOffset");
    this.notionalAmount = notionalAmount;
    this.periodicSchedule = periodicSchedule;
    this.fixedRate = fixedRate;
    this.dayCount = dayCount;
    this.yieldConvention = yieldConvention;
    this.legalEntityId = legalEntityId;
    this.settlementDateOffset = settlementDateOffset;
  }

  @Override
  public FixedCouponBond.Meta metaBean() {
    return FixedCouponBond.Meta.INSTANCE;
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
   * Gets the notional amount of the product.
   * <p>
   * The amount is signed.
   * A positive amount indicates the payments are to be received.
   * A negative amount indicates the payments are to be paid.
   * <p>
   * This must be specified in one of the two currencies of the forward.
   * @return the value of the property, not null
   */
  public CurrencyAmount getNotionalAmount() {
    return notionalAmount;
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
   * @return the value of the property, not null
   */
  public DayCount getDayCount() {
    return dayCount;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets yield convention.
   * <p>
   * The convention defines accrued interest calculation type of the product.
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
      FixedCouponBond other = (FixedCouponBond) obj;
      return JodaBeanUtils.equal(getNotionalAmount(), other.getNotionalAmount()) &&
          JodaBeanUtils.equal(getPeriodicSchedule(), other.getPeriodicSchedule()) &&
          JodaBeanUtils.equal(getFixedRate(), other.getFixedRate()) &&
          JodaBeanUtils.equal(getDayCount(), other.getDayCount()) &&
          JodaBeanUtils.equal(getYieldConvention(), other.getYieldConvention()) &&
          JodaBeanUtils.equal(getLegalEntityId(), other.getLegalEntityId()) &&
          JodaBeanUtils.equal(getSettlementDateOffset(), other.getSettlementDateOffset());
    }
    return false;
  }

  @Override
  public int hashCode() {
    int hash = getClass().hashCode();
    hash = hash * 31 + JodaBeanUtils.hashCode(getNotionalAmount());
    hash = hash * 31 + JodaBeanUtils.hashCode(getPeriodicSchedule());
    hash = hash * 31 + JodaBeanUtils.hashCode(getFixedRate());
    hash = hash * 31 + JodaBeanUtils.hashCode(getDayCount());
    hash = hash * 31 + JodaBeanUtils.hashCode(getYieldConvention());
    hash = hash * 31 + JodaBeanUtils.hashCode(getLegalEntityId());
    hash = hash * 31 + JodaBeanUtils.hashCode(getSettlementDateOffset());
    return hash;
  }

  @Override
  public String toString() {
    StringBuilder buf = new StringBuilder(256);
    buf.append("FixedCouponBond{");
    buf.append("notionalAmount").append('=').append(getNotionalAmount()).append(',').append(' ');
    buf.append("periodicSchedule").append('=').append(getPeriodicSchedule()).append(',').append(' ');
    buf.append("fixedRate").append('=').append(getFixedRate()).append(',').append(' ');
    buf.append("dayCount").append('=').append(getDayCount()).append(',').append(' ');
    buf.append("yieldConvention").append('=').append(getYieldConvention()).append(',').append(' ');
    buf.append("legalEntityId").append('=').append(getLegalEntityId()).append(',').append(' ');
    buf.append("settlementDateOffset").append('=').append(JodaBeanUtils.toString(getSettlementDateOffset()));
    buf.append('}');
    return buf.toString();
  }

  //-----------------------------------------------------------------------
  /**
   * The meta-bean for {@code FixedCouponBond}.
   */
  public static final class Meta extends DirectMetaBean {
    /**
     * The singleton instance of the meta-bean.
     */
    static final Meta INSTANCE = new Meta();

    /**
     * The meta-property for the {@code notionalAmount} property.
     */
    private final MetaProperty<CurrencyAmount> notionalAmount = DirectMetaProperty.ofImmutable(
        this, "notionalAmount", FixedCouponBond.class, CurrencyAmount.class);
    /**
     * The meta-property for the {@code periodicSchedule} property.
     */
    private final MetaProperty<PeriodicSchedule> periodicSchedule = DirectMetaProperty.ofImmutable(
        this, "periodicSchedule", FixedCouponBond.class, PeriodicSchedule.class);
    /**
     * The meta-property for the {@code fixedRate} property.
     */
    private final MetaProperty<Double> fixedRate = DirectMetaProperty.ofImmutable(
        this, "fixedRate", FixedCouponBond.class, Double.TYPE);
    /**
     * The meta-property for the {@code dayCount} property.
     */
    private final MetaProperty<DayCount> dayCount = DirectMetaProperty.ofImmutable(
        this, "dayCount", FixedCouponBond.class, DayCount.class);
    /**
     * The meta-property for the {@code yieldConvention} property.
     */
    private final MetaProperty<YieldConvention> yieldConvention = DirectMetaProperty.ofImmutable(
        this, "yieldConvention", FixedCouponBond.class, YieldConvention.class);
    /**
     * The meta-property for the {@code legalEntityId} property.
     */
    private final MetaProperty<StandardId> legalEntityId = DirectMetaProperty.ofImmutable(
        this, "legalEntityId", FixedCouponBond.class, StandardId.class);
    /**
     * The meta-property for the {@code settlementDateOffset} property.
     */
    private final MetaProperty<DaysAdjustment> settlementDateOffset = DirectMetaProperty.ofImmutable(
        this, "settlementDateOffset", FixedCouponBond.class, DaysAdjustment.class);
    /**
     * The meta-properties.
     */
    private final Map<String, MetaProperty<?>> metaPropertyMap$ = new DirectMetaPropertyMap(
        this, null,
        "notionalAmount",
        "periodicSchedule",
        "fixedRate",
        "dayCount",
        "yieldConvention",
        "legalEntityId",
        "settlementDateOffset");

    /**
     * Restricted constructor.
     */
    private Meta() {
    }

    @Override
    protected MetaProperty<?> metaPropertyGet(String propertyName) {
      switch (propertyName.hashCode()) {
        case -902123592:  // notionalAmount
          return notionalAmount;
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
      }
      return super.metaPropertyGet(propertyName);
    }

    @Override
    public FixedCouponBond.Builder builder() {
      return new FixedCouponBond.Builder();
    }

    @Override
    public Class<? extends FixedCouponBond> beanType() {
      return FixedCouponBond.class;
    }

    @Override
    public Map<String, MetaProperty<?>> metaPropertyMap() {
      return metaPropertyMap$;
    }

    //-----------------------------------------------------------------------
    /**
     * The meta-property for the {@code notionalAmount} property.
     * @return the meta-property, not null
     */
    public MetaProperty<CurrencyAmount> notionalAmount() {
      return notionalAmount;
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

    //-----------------------------------------------------------------------
    @Override
    protected Object propertyGet(Bean bean, String propertyName, boolean quiet) {
      switch (propertyName.hashCode()) {
        case -902123592:  // notionalAmount
          return ((FixedCouponBond) bean).getNotionalAmount();
        case 1847018066:  // periodicSchedule
          return ((FixedCouponBond) bean).getPeriodicSchedule();
        case 747425396:  // fixedRate
          return ((FixedCouponBond) bean).getFixedRate();
        case 1905311443:  // dayCount
          return ((FixedCouponBond) bean).getDayCount();
        case -1895216418:  // yieldConvention
          return ((FixedCouponBond) bean).getYieldConvention();
        case 866287159:  // legalEntityId
          return ((FixedCouponBond) bean).getLegalEntityId();
        case 135924714:  // settlementDateOffset
          return ((FixedCouponBond) bean).getSettlementDateOffset();
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
   * The bean-builder for {@code FixedCouponBond}.
   */
  public static final class Builder extends DirectFieldsBeanBuilder<FixedCouponBond> {

    private CurrencyAmount notionalAmount;
    private PeriodicSchedule periodicSchedule;
    private double fixedRate;
    private DayCount dayCount;
    private YieldConvention yieldConvention;
    private StandardId legalEntityId;
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
    private Builder(FixedCouponBond beanToCopy) {
      this.notionalAmount = beanToCopy.getNotionalAmount();
      this.periodicSchedule = beanToCopy.getPeriodicSchedule();
      this.fixedRate = beanToCopy.getFixedRate();
      this.dayCount = beanToCopy.getDayCount();
      this.yieldConvention = beanToCopy.getYieldConvention();
      this.legalEntityId = beanToCopy.getLegalEntityId();
      this.settlementDateOffset = beanToCopy.getSettlementDateOffset();
    }

    //-----------------------------------------------------------------------
    @Override
    public Object get(String propertyName) {
      switch (propertyName.hashCode()) {
        case -902123592:  // notionalAmount
          return notionalAmount;
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
        default:
          throw new NoSuchElementException("Unknown property: " + propertyName);
      }
    }

    @Override
    public Builder set(String propertyName, Object newValue) {
      switch (propertyName.hashCode()) {
        case -902123592:  // notionalAmount
          this.notionalAmount = (CurrencyAmount) newValue;
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
    public FixedCouponBond build() {
      return new FixedCouponBond(
          notionalAmount,
          periodicSchedule,
          fixedRate,
          dayCount,
          yieldConvention,
          legalEntityId,
          settlementDateOffset);
    }

    //-----------------------------------------------------------------------
    /**
     * Sets the notional amount of the product.
     * <p>
     * The amount is signed.
     * A positive amount indicates the payments are to be received.
     * A negative amount indicates the payments are to be paid.
     * <p>
     * This must be specified in one of the two currencies of the forward.
     * @param notionalAmount  the new value, not null
     * @return this, for chaining, not null
     */
    public Builder notionalAmount(CurrencyAmount notionalAmount) {
      JodaBeanUtils.notNull(notionalAmount, "notionalAmount");
      this.notionalAmount = notionalAmount;
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
     * The convention defines accrued interest calculation type of the product.
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

    //-----------------------------------------------------------------------
    @Override
    public String toString() {
      StringBuilder buf = new StringBuilder(256);
      buf.append("FixedCouponBond.Builder{");
      buf.append("notionalAmount").append('=').append(JodaBeanUtils.toString(notionalAmount)).append(',').append(' ');
      buf.append("periodicSchedule").append('=').append(JodaBeanUtils.toString(periodicSchedule)).append(',').append(' ');
      buf.append("fixedRate").append('=').append(JodaBeanUtils.toString(fixedRate)).append(',').append(' ');
      buf.append("dayCount").append('=').append(JodaBeanUtils.toString(dayCount)).append(',').append(' ');
      buf.append("yieldConvention").append('=').append(JodaBeanUtils.toString(yieldConvention)).append(',').append(' ');
      buf.append("legalEntityId").append('=').append(JodaBeanUtils.toString(legalEntityId)).append(',').append(' ');
      buf.append("settlementDateOffset").append('=').append(JodaBeanUtils.toString(settlementDateOffset));
      buf.append('}');
      return buf.toString();
    }

  }

  ///CLOVER:ON
  //-------------------------- AUTOGENERATED END --------------------------
}
