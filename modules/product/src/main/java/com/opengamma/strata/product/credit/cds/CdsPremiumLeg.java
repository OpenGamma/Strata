package com.opengamma.strata.product.credit.cds;

import java.io.Serializable;
import java.time.LocalDate;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;

import org.joda.beans.Bean;
import org.joda.beans.BeanBuilder;
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
import com.opengamma.strata.basics.currency.Currency;
import com.opengamma.strata.basics.date.BusinessDayAdjustment;
import com.opengamma.strata.basics.date.DayCount;
import com.opengamma.strata.basics.schedule.Frequency;
import com.opengamma.strata.basics.schedule.PeriodicSchedule;
import com.opengamma.strata.basics.schedule.RollConventions;
import com.opengamma.strata.basics.schedule.Schedule;
import com.opengamma.strata.basics.schedule.SchedulePeriod;
import com.opengamma.strata.basics.schedule.StubConvention;
import com.opengamma.strata.collect.ArgChecker;
import com.opengamma.strata.product.common.PayReceive;

@BeanDefinition(builderScope = "private")
public final class CdsPremiumLeg
    implements Resolvable<ResolvedCdsPremiumLeg>, ImmutableBean, Serializable {

  /**
   * Whether the leg is pay or receive.
   * <p>
   * A value of 'Pay' implies that the resulting amount is paid to the counterparty.
   * A value of 'Receive' implies that the resulting amount is received from the counterparty.
   * Note that negative swap rates can result in a payment in the opposite direction
   * to that implied by this indicator.
   */
  @PropertyDefinition(validate = "notNull")
  private final PayReceive payReceive;
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

  // TODO consider stepinDate and accStartDate (in pricer?)

  public static CdsPremiumLeg of(LocalDate startDate, LocalDate endDate, Frequency paymentFrequency,
      BusinessDayAdjustment businessDayAdjustment, StubConvention stubConvention, PayReceive payReceive, Currency currency,
      double notional, DayCount dayCount, double coupon, boolean payAccruedOnDefault) {

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
    return new CdsPremiumLeg(payReceive, accrualSchedule, currency, notional, dayCount, coupon, payAccruedOnDefault);
  }

  public ResolvedCdsPremiumLeg resolve(ReferenceData refData) {
    Schedule adjustedSchedule = accrualSchedule.createSchedule(refData);
    ImmutableList.Builder<CreditCouponPaymentPeriod> accrualPeriods = ImmutableList.builder();
    int nPeriods = adjustedSchedule.size();
    for (int i = 0; i < nPeriods - 1; i++) {
      SchedulePeriod period = adjustedSchedule.getPeriod(i);
      accrualPeriods.add(CreditCouponPaymentPeriod.builder()
          .startDate(period.getStartDate())
          .endDate(period.getEndDate())
          .paymentDate(period.getEndDate())
          .notional(notional)
          .currency(currency)
          .fixedRate(coupon)
          .yearFraction(period.yearFraction(dayCount, adjustedSchedule))
          .build());
    }
    SchedulePeriod lastPeriod = adjustedSchedule.getPeriod(nPeriods - 1);
    accrualPeriods.add(CreditCouponPaymentPeriod.builder()
        .startDate(lastPeriod.getStartDate())
        .endDate(lastPeriod.getEndDate())
        .paymentDate(accrualSchedule.getBusinessDayAdjustment().adjust(lastPeriod.getEndDate(), refData))
        .notional(notional)
        .currency(currency)
        .fixedRate(coupon)
        .yearFraction(lastPeriod.yearFraction(dayCount, adjustedSchedule))
        .build());
    ImmutableList<CreditCouponPaymentPeriod> periodicPayments = accrualPeriods.build();
    return ResolvedCdsPremiumLeg.builder()
        .periodicPayments(periodicPayments)
        .dayCount(dayCount)
        .payAccruedOnDefault(payAccruedOnDefault)
        .payReceive(payReceive)
        .build();
  }

  //------------------------- AUTOGENERATED START -------------------------
  ///CLOVER:OFF
  /**
   * The meta-bean for {@code CdsPremiumLeg}.
   * @return the meta-bean, not null
   */
  public static CdsPremiumLeg.Meta meta() {
    return CdsPremiumLeg.Meta.INSTANCE;
  }

  static {
    JodaBeanUtils.registerMetaBean(CdsPremiumLeg.Meta.INSTANCE);
  }

  /**
   * The serialization version id.
   */
  private static final long serialVersionUID = 1L;

  private CdsPremiumLeg(
      PayReceive payReceive,
      PeriodicSchedule accrualSchedule,
      Currency currency,
      double notional,
      DayCount dayCount,
      double coupon,
      boolean payAccruedOnDefault) {
    JodaBeanUtils.notNull(payReceive, "payReceive");
    JodaBeanUtils.notNull(accrualSchedule, "accrualSchedule");
    JodaBeanUtils.notNull(currency, "currency");
    ArgChecker.notNegativeOrZero(notional, "notional");
    JodaBeanUtils.notNull(dayCount, "dayCount");
    ArgChecker.notNegative(coupon, "coupon");
    JodaBeanUtils.notNull(payAccruedOnDefault, "payAccruedOnDefault");
    this.payReceive = payReceive;
    this.accrualSchedule = accrualSchedule;
    this.currency = currency;
    this.notional = notional;
    this.dayCount = dayCount;
    this.coupon = coupon;
    this.payAccruedOnDefault = payAccruedOnDefault;
  }

  @Override
  public CdsPremiumLeg.Meta metaBean() {
    return CdsPremiumLeg.Meta.INSTANCE;
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
   * Gets whether the leg is pay or receive.
   * <p>
   * A value of 'Pay' implies that the resulting amount is paid to the counterparty.
   * A value of 'Receive' implies that the resulting amount is received from the counterparty.
   * Note that negative swap rates can result in a payment in the opposite direction
   * to that implied by this indicator.
   * @return the value of the property, not null
   */
  public PayReceive getPayReceive() {
    return payReceive;
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
  @Override
  public boolean equals(Object obj) {
    if (obj == this) {
      return true;
    }
    if (obj != null && obj.getClass() == this.getClass()) {
      CdsPremiumLeg other = (CdsPremiumLeg) obj;
      return JodaBeanUtils.equal(payReceive, other.payReceive) &&
          JodaBeanUtils.equal(accrualSchedule, other.accrualSchedule) &&
          JodaBeanUtils.equal(currency, other.currency) &&
          JodaBeanUtils.equal(notional, other.notional) &&
          JodaBeanUtils.equal(dayCount, other.dayCount) &&
          JodaBeanUtils.equal(coupon, other.coupon) &&
          (payAccruedOnDefault == other.payAccruedOnDefault);
    }
    return false;
  }

  @Override
  public int hashCode() {
    int hash = getClass().hashCode();
    hash = hash * 31 + JodaBeanUtils.hashCode(payReceive);
    hash = hash * 31 + JodaBeanUtils.hashCode(accrualSchedule);
    hash = hash * 31 + JodaBeanUtils.hashCode(currency);
    hash = hash * 31 + JodaBeanUtils.hashCode(notional);
    hash = hash * 31 + JodaBeanUtils.hashCode(dayCount);
    hash = hash * 31 + JodaBeanUtils.hashCode(coupon);
    hash = hash * 31 + JodaBeanUtils.hashCode(payAccruedOnDefault);
    return hash;
  }

  @Override
  public String toString() {
    StringBuilder buf = new StringBuilder(256);
    buf.append("CdsPremiumLeg{");
    buf.append("payReceive").append('=').append(payReceive).append(',').append(' ');
    buf.append("accrualSchedule").append('=').append(accrualSchedule).append(',').append(' ');
    buf.append("currency").append('=').append(currency).append(',').append(' ');
    buf.append("notional").append('=').append(notional).append(',').append(' ');
    buf.append("dayCount").append('=').append(dayCount).append(',').append(' ');
    buf.append("coupon").append('=').append(coupon).append(',').append(' ');
    buf.append("payAccruedOnDefault").append('=').append(JodaBeanUtils.toString(payAccruedOnDefault));
    buf.append('}');
    return buf.toString();
  }

  //-----------------------------------------------------------------------
  /**
   * The meta-bean for {@code CdsPremiumLeg}.
   */
  public static final class Meta extends DirectMetaBean {
    /**
     * The singleton instance of the meta-bean.
     */
    static final Meta INSTANCE = new Meta();

    /**
     * The meta-property for the {@code payReceive} property.
     */
    private final MetaProperty<PayReceive> payReceive = DirectMetaProperty.ofImmutable(
        this, "payReceive", CdsPremiumLeg.class, PayReceive.class);
    /**
     * The meta-property for the {@code accrualSchedule} property.
     */
    private final MetaProperty<PeriodicSchedule> accrualSchedule = DirectMetaProperty.ofImmutable(
        this, "accrualSchedule", CdsPremiumLeg.class, PeriodicSchedule.class);
    /**
     * The meta-property for the {@code currency} property.
     */
    private final MetaProperty<Currency> currency = DirectMetaProperty.ofImmutable(
        this, "currency", CdsPremiumLeg.class, Currency.class);
    /**
     * The meta-property for the {@code notional} property.
     */
    private final MetaProperty<Double> notional = DirectMetaProperty.ofImmutable(
        this, "notional", CdsPremiumLeg.class, Double.TYPE);
    /**
     * The meta-property for the {@code dayCount} property.
     */
    private final MetaProperty<DayCount> dayCount = DirectMetaProperty.ofImmutable(
        this, "dayCount", CdsPremiumLeg.class, DayCount.class);
    /**
     * The meta-property for the {@code coupon} property.
     */
    private final MetaProperty<Double> coupon = DirectMetaProperty.ofImmutable(
        this, "coupon", CdsPremiumLeg.class, Double.TYPE);
    /**
     * The meta-property for the {@code payAccruedOnDefault} property.
     */
    private final MetaProperty<Boolean> payAccruedOnDefault = DirectMetaProperty.ofImmutable(
        this, "payAccruedOnDefault", CdsPremiumLeg.class, Boolean.TYPE);
    /**
     * The meta-properties.
     */
    private final Map<String, MetaProperty<?>> metaPropertyMap$ = new DirectMetaPropertyMap(
        this, null,
        "payReceive",
        "accrualSchedule",
        "currency",
        "notional",
        "dayCount",
        "coupon",
        "payAccruedOnDefault");

    /**
     * Restricted constructor.
     */
    private Meta() {
    }

    @Override
    protected MetaProperty<?> metaPropertyGet(String propertyName) {
      switch (propertyName.hashCode()) {
        case -885469925:  // payReceive
          return payReceive;
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
      }
      return super.metaPropertyGet(propertyName);
    }

    @Override
    public BeanBuilder<? extends CdsPremiumLeg> builder() {
      return new CdsPremiumLeg.Builder();
    }

    @Override
    public Class<? extends CdsPremiumLeg> beanType() {
      return CdsPremiumLeg.class;
    }

    @Override
    public Map<String, MetaProperty<?>> metaPropertyMap() {
      return metaPropertyMap$;
    }

    //-----------------------------------------------------------------------
    /**
     * The meta-property for the {@code payReceive} property.
     * @return the meta-property, not null
     */
    public MetaProperty<PayReceive> payReceive() {
      return payReceive;
    }

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

    //-----------------------------------------------------------------------
    @Override
    protected Object propertyGet(Bean bean, String propertyName, boolean quiet) {
      switch (propertyName.hashCode()) {
        case -885469925:  // payReceive
          return ((CdsPremiumLeg) bean).getPayReceive();
        case 304659814:  // accrualSchedule
          return ((CdsPremiumLeg) bean).getAccrualSchedule();
        case 575402001:  // currency
          return ((CdsPremiumLeg) bean).getCurrency();
        case 1585636160:  // notional
          return ((CdsPremiumLeg) bean).getNotional();
        case 1905311443:  // dayCount
          return ((CdsPremiumLeg) bean).getDayCount();
        case -1354573786:  // coupon
          return ((CdsPremiumLeg) bean).getCoupon();
        case -43782841:  // payAccruedOnDefault
          return ((CdsPremiumLeg) bean).isPayAccruedOnDefault();
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
   * The bean-builder for {@code CdsPremiumLeg}.
   */
  private static final class Builder extends DirectFieldsBeanBuilder<CdsPremiumLeg> {

    private PayReceive payReceive;
    private PeriodicSchedule accrualSchedule;
    private Currency currency;
    private double notional;
    private DayCount dayCount;
    private double coupon;
    private boolean payAccruedOnDefault;

    /**
     * Restricted constructor.
     */
    private Builder() {
    }

    //-----------------------------------------------------------------------
    @Override
    public Object get(String propertyName) {
      switch (propertyName.hashCode()) {
        case -885469925:  // payReceive
          return payReceive;
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
        default:
          throw new NoSuchElementException("Unknown property: " + propertyName);
      }
    }

    @Override
    public Builder set(String propertyName, Object newValue) {
      switch (propertyName.hashCode()) {
        case -885469925:  // payReceive
          this.payReceive = (PayReceive) newValue;
          break;
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
    public CdsPremiumLeg build() {
      return new CdsPremiumLeg(
          payReceive,
          accrualSchedule,
          currency,
          notional,
          dayCount,
          coupon,
          payAccruedOnDefault);
    }

    //-----------------------------------------------------------------------
    @Override
    public String toString() {
      StringBuilder buf = new StringBuilder(256);
      buf.append("CdsPremiumLeg.Builder{");
      buf.append("payReceive").append('=').append(JodaBeanUtils.toString(payReceive)).append(',').append(' ');
      buf.append("accrualSchedule").append('=').append(JodaBeanUtils.toString(accrualSchedule)).append(',').append(' ');
      buf.append("currency").append('=').append(JodaBeanUtils.toString(currency)).append(',').append(' ');
      buf.append("notional").append('=').append(JodaBeanUtils.toString(notional)).append(',').append(' ');
      buf.append("dayCount").append('=').append(JodaBeanUtils.toString(dayCount)).append(',').append(' ');
      buf.append("coupon").append('=').append(JodaBeanUtils.toString(coupon)).append(',').append(' ');
      buf.append("payAccruedOnDefault").append('=').append(JodaBeanUtils.toString(payAccruedOnDefault));
      buf.append('}');
      return buf.toString();
    }

  }

  ///CLOVER:ON
  //-------------------------- AUTOGENERATED END --------------------------
}
