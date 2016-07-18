package com.opengamma.strata.product.credit.cds;

import java.io.Serializable;
import java.time.LocalDate;
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
import com.opengamma.strata.basics.date.DayCount;
import com.opengamma.strata.basics.date.DaysAdjustment;
import com.opengamma.strata.basics.schedule.Frequency;
import com.opengamma.strata.basics.schedule.PeriodicSchedule;
import com.opengamma.strata.basics.schedule.RollConventions;
import com.opengamma.strata.basics.schedule.Schedule;
import com.opengamma.strata.basics.schedule.SchedulePeriod;
import com.opengamma.strata.basics.schedule.StubConvention;
import com.opengamma.strata.collect.ArgChecker;
import com.opengamma.strata.product.common.BuySell;

@BeanDefinition
public final class Cds
    implements Resolvable<ResolvedCds>, ImmutableBean, Serializable {

  /**
   * The accrual schedule.
   * <p>
   * This is used to define the accrual periods.
   * These are used directly or indirectly to determine other dates in the product.
   */
  @PropertyDefinition(validate = "notNull")
  private final PeriodicSchedule accrualSchedule;
  /**
   * The currency of the leg associated with the notional.
   * <p>
   * This is the currency of the leg and the currency that swap rate calculation is made in.
   * The amounts of the notional are expressed in terms of this currency.
   */
  @PropertyDefinition(validate = "notNull")
  private final Currency currency;
  /**
   * The notional amount, must be non-negative.
   * <p>
   * The notional amount applicable during the period.
   * The currency of the notional is specified by {@code currency}.
   */
  @PropertyDefinition(validate = "ArgChecker.notNegativeOrZero")
  private final double notional;
  /**
   * The day count convention.
   * <p>
   * This is used to convert dates to a numerical value.
   * <p>
   * When building, this will default to the day count of the swap convention in the swap index if not specified.
   */
  @PropertyDefinition(validate = "notNull")
  private final DayCount dayCount;

  @PropertyDefinition(validate = "ArgChecker.notNegative")
  private final double coupon;
  /**
   * Whether the accrued premium is paid in the event of a default.
   */
  @PropertyDefinition(validate = "notNull")
  private final boolean payAccruedOnDefault;

  //  @PropertyDefinition(validate = "ArgChecker.notNegative")  // TODO should be in market data
  //  private final double recoveryRate;

  @PropertyDefinition(validate = "notNull")
  private final boolean protectStart; // TODO only applied to protection leg and accrued on default
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
   * The legal entity identifier.
   * <p>
   * This identifier is used for the legal entity that issues the bond.
   */
  @PropertyDefinition(validate = "notNull")
  private final StandardId legalEntityId;

  @PropertyDefinition(validate = "notNull")
  private final BuySell buySell;

  //-------------------------------------------------------------------------
  public static Cds of(LocalDate startDate, LocalDate endDate, Frequency paymentFrequency,
      BusinessDayAdjustment businessDayAdjustment, StubConvention stubConvention, Currency currency,
      double notional, DayCount dayCount, double coupon, boolean payAccruedOnDefault, boolean protectStart,
      DaysAdjustment settlementDateOffset, StandardId legalEntityId, BuySell buySell) {

    PeriodicSchedule accrualSchedule = PeriodicSchedule.builder()
        .businessDayAdjustment(businessDayAdjustment)
        .startDate(startDate)
        .endDate(endDate)
        .startDateBusinessDayAdjustment(BusinessDayAdjustment.NONE) // TODO do we need this flexibility?
        .endDateBusinessDayAdjustment(BusinessDayAdjustment.NONE)// TODO do we need this flexibility?
        .frequency(paymentFrequency)
        .rollConvention(RollConventions.NONE)
        .stubConvention(stubConvention)
        .build();

    return new Cds(accrualSchedule, currency, notional, dayCount, coupon, payAccruedOnDefault, protectStart, settlementDateOffset,
        legalEntityId, buySell);
  }

  //-------------------------------------------------------------------------
  @Override
  public ResolvedCds resolve(ReferenceData refData) {
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
          .paymentDate(period.getEndDate())
          .notional(notional)
          .currency(currency)
          .coupon(coupon)
          .yearFraction(period.yearFraction(dayCount, adjustedSchedule))
          .build());
    }
    SchedulePeriod lastPeriod = adjustedSchedule.getPeriod(nPeriods - 1);
    accrualPeriods.add(CreditCouponPaymentPeriod.builder()
        .startDate(lastPeriod.getStartDate())
        .endDate(lastPeriod.getUnadjustedEndDate())  // TODO flexibility for adjusting??
        .unadjustedStartDate(lastPeriod.getUnadjustedStartDate())
        .unadjustedEndDate(lastPeriod.getUnadjustedEndDate())
        .paymentDate(lastPeriod.getEndDate())
        .notional(notional)
        .currency(currency)
        .coupon(coupon)
        .yearFraction(lastPeriod.yearFraction(dayCount, adjustedSchedule))
        .build());
    ImmutableList<CreditCouponPaymentPeriod> periodicPayments = accrualPeriods.build();

    return ResolvedCds.builder()
        .buySell(buySell)
        .legalEntityId(legalEntityId)
        .protectStart(protectStart)
        .payAccruedOnDefault(payAccruedOnDefault)
        .periodicPayments(periodicPayments)
        .settlementDateOffset(settlementDateOffset)
        .dayCount(dayCount)
        .build();
  }

  //------------------------- AUTOGENERATED START -------------------------
  ///CLOVER:OFF
  /**
   * The meta-bean for {@code Cds}.
   * @return the meta-bean, not null
   */
  public static Cds.Meta meta() {
    return Cds.Meta.INSTANCE;
  }

  static {
    JodaBeanUtils.registerMetaBean(Cds.Meta.INSTANCE);
  }

  /**
   * The serialization version id.
   */
  private static final long serialVersionUID = 1L;

  /**
   * Returns a builder used to create an instance of the bean.
   * @return the builder, not null
   */
  public static Cds.Builder builder() {
    return new Cds.Builder();
  }

  private Cds(
      PeriodicSchedule accrualSchedule,
      Currency currency,
      double notional,
      DayCount dayCount,
      double coupon,
      boolean payAccruedOnDefault,
      boolean protectStart,
      DaysAdjustment settlementDateOffset,
      StandardId legalEntityId,
      BuySell buySell) {
    JodaBeanUtils.notNull(accrualSchedule, "accrualSchedule");
    JodaBeanUtils.notNull(currency, "currency");
    ArgChecker.notNegativeOrZero(notional, "notional");
    JodaBeanUtils.notNull(dayCount, "dayCount");
    ArgChecker.notNegative(coupon, "coupon");
    JodaBeanUtils.notNull(payAccruedOnDefault, "payAccruedOnDefault");
    JodaBeanUtils.notNull(protectStart, "protectStart");
    JodaBeanUtils.notNull(settlementDateOffset, "settlementDateOffset");
    JodaBeanUtils.notNull(legalEntityId, "legalEntityId");
    JodaBeanUtils.notNull(buySell, "buySell");
    this.accrualSchedule = accrualSchedule;
    this.currency = currency;
    this.notional = notional;
    this.dayCount = dayCount;
    this.coupon = coupon;
    this.payAccruedOnDefault = payAccruedOnDefault;
    this.protectStart = protectStart;
    this.settlementDateOffset = settlementDateOffset;
    this.legalEntityId = legalEntityId;
    this.buySell = buySell;
  }

  @Override
  public Cds.Meta metaBean() {
    return Cds.Meta.INSTANCE;
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
   * Gets the currency of the leg associated with the notional.
   * <p>
   * This is the currency of the leg and the currency that swap rate calculation is made in.
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
   * The notional amount applicable during the period.
   * The currency of the notional is specified by {@code currency}.
   * @return the value of the property
   */
  public double getNotional() {
    return notional;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the day count convention.
   * <p>
   * This is used to convert dates to a numerical value.
   * <p>
   * When building, this will default to the day count of the swap convention in the swap index if not specified.
   * @return the value of the property, not null
   */
  public DayCount getDayCount() {
    return dayCount;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the coupon.
   * @return the value of the property
   */
  public double getCoupon() {
    return coupon;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets whether the accrued premium is paid in the event of a default.
   * @return the value of the property, not null
   */
  public boolean isPayAccruedOnDefault() {
    return payAccruedOnDefault;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the protectStart.
   * @return the value of the property, not null
   */
  public boolean isProtectStart() {
    return protectStart;
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
   * Gets the buySell.
   * @return the value of the property, not null
   */
  public BuySell getBuySell() {
    return buySell;
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
      Cds other = (Cds) obj;
      return JodaBeanUtils.equal(accrualSchedule, other.accrualSchedule) &&
          JodaBeanUtils.equal(currency, other.currency) &&
          JodaBeanUtils.equal(notional, other.notional) &&
          JodaBeanUtils.equal(dayCount, other.dayCount) &&
          JodaBeanUtils.equal(coupon, other.coupon) &&
          (payAccruedOnDefault == other.payAccruedOnDefault) &&
          (protectStart == other.protectStart) &&
          JodaBeanUtils.equal(settlementDateOffset, other.settlementDateOffset) &&
          JodaBeanUtils.equal(legalEntityId, other.legalEntityId) &&
          JodaBeanUtils.equal(buySell, other.buySell);
    }
    return false;
  }

  @Override
  public int hashCode() {
    int hash = getClass().hashCode();
    hash = hash * 31 + JodaBeanUtils.hashCode(accrualSchedule);
    hash = hash * 31 + JodaBeanUtils.hashCode(currency);
    hash = hash * 31 + JodaBeanUtils.hashCode(notional);
    hash = hash * 31 + JodaBeanUtils.hashCode(dayCount);
    hash = hash * 31 + JodaBeanUtils.hashCode(coupon);
    hash = hash * 31 + JodaBeanUtils.hashCode(payAccruedOnDefault);
    hash = hash * 31 + JodaBeanUtils.hashCode(protectStart);
    hash = hash * 31 + JodaBeanUtils.hashCode(settlementDateOffset);
    hash = hash * 31 + JodaBeanUtils.hashCode(legalEntityId);
    hash = hash * 31 + JodaBeanUtils.hashCode(buySell);
    return hash;
  }

  @Override
  public String toString() {
    StringBuilder buf = new StringBuilder(352);
    buf.append("Cds{");
    buf.append("accrualSchedule").append('=').append(accrualSchedule).append(',').append(' ');
    buf.append("currency").append('=').append(currency).append(',').append(' ');
    buf.append("notional").append('=').append(notional).append(',').append(' ');
    buf.append("dayCount").append('=').append(dayCount).append(',').append(' ');
    buf.append("coupon").append('=').append(coupon).append(',').append(' ');
    buf.append("payAccruedOnDefault").append('=').append(payAccruedOnDefault).append(',').append(' ');
    buf.append("protectStart").append('=').append(protectStart).append(',').append(' ');
    buf.append("settlementDateOffset").append('=').append(settlementDateOffset).append(',').append(' ');
    buf.append("legalEntityId").append('=').append(legalEntityId).append(',').append(' ');
    buf.append("buySell").append('=').append(JodaBeanUtils.toString(buySell));
    buf.append('}');
    return buf.toString();
  }

  //-----------------------------------------------------------------------
  /**
   * The meta-bean for {@code Cds}.
   */
  public static final class Meta extends DirectMetaBean {
    /**
     * The singleton instance of the meta-bean.
     */
    static final Meta INSTANCE = new Meta();

    /**
     * The meta-property for the {@code accrualSchedule} property.
     */
    private final MetaProperty<PeriodicSchedule> accrualSchedule = DirectMetaProperty.ofImmutable(
        this, "accrualSchedule", Cds.class, PeriodicSchedule.class);
    /**
     * The meta-property for the {@code currency} property.
     */
    private final MetaProperty<Currency> currency = DirectMetaProperty.ofImmutable(
        this, "currency", Cds.class, Currency.class);
    /**
     * The meta-property for the {@code notional} property.
     */
    private final MetaProperty<Double> notional = DirectMetaProperty.ofImmutable(
        this, "notional", Cds.class, Double.TYPE);
    /**
     * The meta-property for the {@code dayCount} property.
     */
    private final MetaProperty<DayCount> dayCount = DirectMetaProperty.ofImmutable(
        this, "dayCount", Cds.class, DayCount.class);
    /**
     * The meta-property for the {@code coupon} property.
     */
    private final MetaProperty<Double> coupon = DirectMetaProperty.ofImmutable(
        this, "coupon", Cds.class, Double.TYPE);
    /**
     * The meta-property for the {@code payAccruedOnDefault} property.
     */
    private final MetaProperty<Boolean> payAccruedOnDefault = DirectMetaProperty.ofImmutable(
        this, "payAccruedOnDefault", Cds.class, Boolean.TYPE);
    /**
     * The meta-property for the {@code protectStart} property.
     */
    private final MetaProperty<Boolean> protectStart = DirectMetaProperty.ofImmutable(
        this, "protectStart", Cds.class, Boolean.TYPE);
    /**
     * The meta-property for the {@code settlementDateOffset} property.
     */
    private final MetaProperty<DaysAdjustment> settlementDateOffset = DirectMetaProperty.ofImmutable(
        this, "settlementDateOffset", Cds.class, DaysAdjustment.class);
    /**
     * The meta-property for the {@code legalEntityId} property.
     */
    private final MetaProperty<StandardId> legalEntityId = DirectMetaProperty.ofImmutable(
        this, "legalEntityId", Cds.class, StandardId.class);
    /**
     * The meta-property for the {@code buySell} property.
     */
    private final MetaProperty<BuySell> buySell = DirectMetaProperty.ofImmutable(
        this, "buySell", Cds.class, BuySell.class);
    /**
     * The meta-properties.
     */
    private final Map<String, MetaProperty<?>> metaPropertyMap$ = new DirectMetaPropertyMap(
        this, null,
        "accrualSchedule",
        "currency",
        "notional",
        "dayCount",
        "coupon",
        "payAccruedOnDefault",
        "protectStart",
        "settlementDateOffset",
        "legalEntityId",
        "buySell");

    /**
     * Restricted constructor.
     */
    private Meta() {
    }

    @Override
    protected MetaProperty<?> metaPropertyGet(String propertyName) {
      switch (propertyName.hashCode()) {
        case 304659814:  // accrualSchedule
          return accrualSchedule;
        case 575402001:  // currency
          return currency;
        case 1585636160:  // notional
          return notional;
        case 1905311443:  // dayCount
          return dayCount;
        case -1354573786:  // coupon
          return coupon;
        case -43782841:  // payAccruedOnDefault
          return payAccruedOnDefault;
        case 33810131:  // protectStart
          return protectStart;
        case 135924714:  // settlementDateOffset
          return settlementDateOffset;
        case 866287159:  // legalEntityId
          return legalEntityId;
        case 244977400:  // buySell
          return buySell;
      }
      return super.metaPropertyGet(propertyName);
    }

    @Override
    public Cds.Builder builder() {
      return new Cds.Builder();
    }

    @Override
    public Class<? extends Cds> beanType() {
      return Cds.class;
    }

    @Override
    public Map<String, MetaProperty<?>> metaPropertyMap() {
      return metaPropertyMap$;
    }

    //-----------------------------------------------------------------------
    /**
     * The meta-property for the {@code accrualSchedule} property.
     * @return the meta-property, not null
     */
    public MetaProperty<PeriodicSchedule> accrualSchedule() {
      return accrualSchedule;
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
     * The meta-property for the {@code dayCount} property.
     * @return the meta-property, not null
     */
    public MetaProperty<DayCount> dayCount() {
      return dayCount;
    }

    /**
     * The meta-property for the {@code coupon} property.
     * @return the meta-property, not null
     */
    public MetaProperty<Double> coupon() {
      return coupon;
    }

    /**
     * The meta-property for the {@code payAccruedOnDefault} property.
     * @return the meta-property, not null
     */
    public MetaProperty<Boolean> payAccruedOnDefault() {
      return payAccruedOnDefault;
    }

    /**
     * The meta-property for the {@code protectStart} property.
     * @return the meta-property, not null
     */
    public MetaProperty<Boolean> protectStart() {
      return protectStart;
    }

    /**
     * The meta-property for the {@code settlementDateOffset} property.
     * @return the meta-property, not null
     */
    public MetaProperty<DaysAdjustment> settlementDateOffset() {
      return settlementDateOffset;
    }

    /**
     * The meta-property for the {@code legalEntityId} property.
     * @return the meta-property, not null
     */
    public MetaProperty<StandardId> legalEntityId() {
      return legalEntityId;
    }

    /**
     * The meta-property for the {@code buySell} property.
     * @return the meta-property, not null
     */
    public MetaProperty<BuySell> buySell() {
      return buySell;
    }

    //-----------------------------------------------------------------------
    @Override
    protected Object propertyGet(Bean bean, String propertyName, boolean quiet) {
      switch (propertyName.hashCode()) {
        case 304659814:  // accrualSchedule
          return ((Cds) bean).getAccrualSchedule();
        case 575402001:  // currency
          return ((Cds) bean).getCurrency();
        case 1585636160:  // notional
          return ((Cds) bean).getNotional();
        case 1905311443:  // dayCount
          return ((Cds) bean).getDayCount();
        case -1354573786:  // coupon
          return ((Cds) bean).getCoupon();
        case -43782841:  // payAccruedOnDefault
          return ((Cds) bean).isPayAccruedOnDefault();
        case 33810131:  // protectStart
          return ((Cds) bean).isProtectStart();
        case 135924714:  // settlementDateOffset
          return ((Cds) bean).getSettlementDateOffset();
        case 866287159:  // legalEntityId
          return ((Cds) bean).getLegalEntityId();
        case 244977400:  // buySell
          return ((Cds) bean).getBuySell();
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
   * The bean-builder for {@code Cds}.
   */
  public static final class Builder extends DirectFieldsBeanBuilder<Cds> {

    private PeriodicSchedule accrualSchedule;
    private Currency currency;
    private double notional;
    private DayCount dayCount;
    private double coupon;
    private boolean payAccruedOnDefault;
    private boolean protectStart;
    private DaysAdjustment settlementDateOffset;
    private StandardId legalEntityId;
    private BuySell buySell;

    /**
     * Restricted constructor.
     */
    private Builder() {
    }

    /**
     * Restricted copy constructor.
     * @param beanToCopy  the bean to copy from, not null
     */
    private Builder(Cds beanToCopy) {
      this.accrualSchedule = beanToCopy.getAccrualSchedule();
      this.currency = beanToCopy.getCurrency();
      this.notional = beanToCopy.getNotional();
      this.dayCount = beanToCopy.getDayCount();
      this.coupon = beanToCopy.getCoupon();
      this.payAccruedOnDefault = beanToCopy.isPayAccruedOnDefault();
      this.protectStart = beanToCopy.isProtectStart();
      this.settlementDateOffset = beanToCopy.getSettlementDateOffset();
      this.legalEntityId = beanToCopy.getLegalEntityId();
      this.buySell = beanToCopy.getBuySell();
    }

    //-----------------------------------------------------------------------
    @Override
    public Object get(String propertyName) {
      switch (propertyName.hashCode()) {
        case 304659814:  // accrualSchedule
          return accrualSchedule;
        case 575402001:  // currency
          return currency;
        case 1585636160:  // notional
          return notional;
        case 1905311443:  // dayCount
          return dayCount;
        case -1354573786:  // coupon
          return coupon;
        case -43782841:  // payAccruedOnDefault
          return payAccruedOnDefault;
        case 33810131:  // protectStart
          return protectStart;
        case 135924714:  // settlementDateOffset
          return settlementDateOffset;
        case 866287159:  // legalEntityId
          return legalEntityId;
        case 244977400:  // buySell
          return buySell;
        default:
          throw new NoSuchElementException("Unknown property: " + propertyName);
      }
    }

    @Override
    public Builder set(String propertyName, Object newValue) {
      switch (propertyName.hashCode()) {
        case 304659814:  // accrualSchedule
          this.accrualSchedule = (PeriodicSchedule) newValue;
          break;
        case 575402001:  // currency
          this.currency = (Currency) newValue;
          break;
        case 1585636160:  // notional
          this.notional = (Double) newValue;
          break;
        case 1905311443:  // dayCount
          this.dayCount = (DayCount) newValue;
          break;
        case -1354573786:  // coupon
          this.coupon = (Double) newValue;
          break;
        case -43782841:  // payAccruedOnDefault
          this.payAccruedOnDefault = (Boolean) newValue;
          break;
        case 33810131:  // protectStart
          this.protectStart = (Boolean) newValue;
          break;
        case 135924714:  // settlementDateOffset
          this.settlementDateOffset = (DaysAdjustment) newValue;
          break;
        case 866287159:  // legalEntityId
          this.legalEntityId = (StandardId) newValue;
          break;
        case 244977400:  // buySell
          this.buySell = (BuySell) newValue;
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
    public Cds build() {
      return new Cds(
          accrualSchedule,
          currency,
          notional,
          dayCount,
          coupon,
          payAccruedOnDefault,
          protectStart,
          settlementDateOffset,
          legalEntityId,
          buySell);
    }

    //-----------------------------------------------------------------------
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
     * Sets the currency of the leg associated with the notional.
     * <p>
     * This is the currency of the leg and the currency that swap rate calculation is made in.
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
     * The notional amount applicable during the period.
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
     * Sets the day count convention.
     * <p>
     * This is used to convert dates to a numerical value.
     * <p>
     * When building, this will default to the day count of the swap convention in the swap index if not specified.
     * @param dayCount  the new value, not null
     * @return this, for chaining, not null
     */
    public Builder dayCount(DayCount dayCount) {
      JodaBeanUtils.notNull(dayCount, "dayCount");
      this.dayCount = dayCount;
      return this;
    }

    /**
     * Sets the coupon.
     * @param coupon  the new value
     * @return this, for chaining, not null
     */
    public Builder coupon(double coupon) {
      ArgChecker.notNegative(coupon, "coupon");
      this.coupon = coupon;
      return this;
    }

    /**
     * Sets whether the accrued premium is paid in the event of a default.
     * @param payAccruedOnDefault  the new value, not null
     * @return this, for chaining, not null
     */
    public Builder payAccruedOnDefault(boolean payAccruedOnDefault) {
      JodaBeanUtils.notNull(payAccruedOnDefault, "payAccruedOnDefault");
      this.payAccruedOnDefault = payAccruedOnDefault;
      return this;
    }

    /**
     * Sets the protectStart.
     * @param protectStart  the new value, not null
     * @return this, for chaining, not null
     */
    public Builder protectStart(boolean protectStart) {
      JodaBeanUtils.notNull(protectStart, "protectStart");
      this.protectStart = protectStart;
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
     * Sets the buySell.
     * @param buySell  the new value, not null
     * @return this, for chaining, not null
     */
    public Builder buySell(BuySell buySell) {
      JodaBeanUtils.notNull(buySell, "buySell");
      this.buySell = buySell;
      return this;
    }

    //-----------------------------------------------------------------------
    @Override
    public String toString() {
      StringBuilder buf = new StringBuilder(352);
      buf.append("Cds.Builder{");
      buf.append("accrualSchedule").append('=').append(JodaBeanUtils.toString(accrualSchedule)).append(',').append(' ');
      buf.append("currency").append('=').append(JodaBeanUtils.toString(currency)).append(',').append(' ');
      buf.append("notional").append('=').append(JodaBeanUtils.toString(notional)).append(',').append(' ');
      buf.append("dayCount").append('=').append(JodaBeanUtils.toString(dayCount)).append(',').append(' ');
      buf.append("coupon").append('=').append(JodaBeanUtils.toString(coupon)).append(',').append(' ');
      buf.append("payAccruedOnDefault").append('=').append(JodaBeanUtils.toString(payAccruedOnDefault)).append(',').append(' ');
      buf.append("protectStart").append('=').append(JodaBeanUtils.toString(protectStart)).append(',').append(' ');
      buf.append("settlementDateOffset").append('=').append(JodaBeanUtils.toString(settlementDateOffset)).append(',').append(' ');
      buf.append("legalEntityId").append('=').append(JodaBeanUtils.toString(legalEntityId)).append(',').append(' ');
      buf.append("buySell").append('=').append(JodaBeanUtils.toString(buySell));
      buf.append('}');
      return buf.toString();
    }

  }

  ///CLOVER:ON
  //-------------------------- AUTOGENERATED END --------------------------
}
