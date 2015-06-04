package com.opengamma.strata.finance.credit;

import com.opengamma.strata.basics.BuySell;
import com.opengamma.strata.basics.currency.Currency;
import com.opengamma.strata.basics.date.BusinessDayAdjustment;
import com.opengamma.strata.basics.date.BusinessDayConvention;
import com.opengamma.strata.basics.date.DayCount;
import com.opengamma.strata.basics.date.HolidayCalendar;
import com.opengamma.strata.basics.schedule.StubConvention;
import com.opengamma.strata.finance.credit.fee.SinglePayment;
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

import java.io.Serializable;
import java.time.LocalDate;
import java.time.Period;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.Set;

@BeanDefinition
public class ExpandedCdsTrade implements ModelCdsTrade, ImmutableBean, Serializable {

  /**
   * tradeDate The trade date
   */
  @PropertyDefinition(validate = "notNull", overrideGet = true)
  private final LocalDate tradeDate;

  /**
   * Typically T+1 unadjusted. Required by the model.
   */
  @PropertyDefinition(validate = "notNull", overrideGet = true)
  public final LocalDate stepInDate;

  /**
   * The date that values are PVed to. Is is normally today + 3 business days.  Aka cash-settle date.
   */
  @PropertyDefinition(validate = "notNull", overrideGet = true)
  public final LocalDate cashSettleDate;

  /**
   * accStartDate This is when the CDS nominally starts in terms of premium payments.
   * i.e. the number of days in the first period (and thus the amount of the first premium payment)
   * is counted from this date.
   * <p>
   * This should be adjusted according business day and holidays
   */
  @PropertyDefinition(validate = "notNull", overrideGet = true)
  public final LocalDate accStartDate;


  /**
   * endDate (aka maturity date) This is when the contract expires and protection ends -
   * any default after this date does not trigger a payment. (the protection ends at end of day)
   * <p>
   * This is an adjusted date and can fall on a holiday or weekend.
   */
  @PropertyDefinition(validate = "notNull", overrideGet = true)
  public final LocalDate endDate;


  /**
   * payAccOnDefault Is the accrued premium paid in the event of a default
   */
  @PropertyDefinition(validate = "notNull", overrideGet = true)
  public final boolean payAccOnDefault;

  /**
   * paymentInterval The nominal step between premium payments (e.g. 3 months, 6 months).
   */
  @PropertyDefinition(validate = "notNull", overrideGet = true)
  public final Period paymentInterval;

  /**
   * stubType stubType Options are FRONTSHORT, FRONTLONG, BACKSHORT, BACKLONG or NONE
   * - <b>Note</b> in this code NONE is not allowed
   */
  @PropertyDefinition(validate = "notNull", overrideGet = true)
  public final StubConvention stubConvention;

  /**
   * businessdayAdjustmentConvention How are adjustments for non-business days made
   */
  @PropertyDefinition(validate = "notNull", overrideGet = true)
  public final BusinessDayConvention businessdayAdjustmentConvention;

  /**
   * calendar HolidayCalendar defining what is a non-business day
   */
  @PropertyDefinition(validate = "notNull", overrideGet = true)
  public final HolidayCalendar calendar;


  /**
   * accrualDayCount Day count used for accrual
   */
  @PropertyDefinition(validate = "notNull", overrideGet = true)
  public final DayCount accrualDayCount;

  /**
   * are we buying protection and paying fees or are we selling protection and receiving fees
   */
  @PropertyDefinition(validate = "notNull", overrideGet = true)
  public final BuySell buySellProtection;

  /**
   * optional upfront fee amount, will be NaN if there is no fee
   */
  @PropertyDefinition(validate = "notNull", overrideGet = true)
  public final double upfrontFeeAmount;

  /**
   * optional upfront fee date, will return null if called and there is no fee
   * check the fee amount first before calling
   */
  @PropertyDefinition(overrideGet = true)
  public final LocalDate upfrontFeePaymentDate;

  /**
   * coupon used to calc fee payments
   */
  @PropertyDefinition(validate = "notNull", overrideGet = true)
  public final double coupon;

  /**
   * notional amount used to calc fee payments
   */
  @PropertyDefinition(validate = "notNull", overrideGet = true)
  public final double notional;

  /**
   * currency fees are paid in
   */
  @PropertyDefinition(validate = "notNull", overrideGet = true)
  public final Currency currency;

  public static ExpandedCdsTrade from(CdsTrade _trade) {
    // See comments on each property above to understand the expansion mappings below
    LocalDate tradeDate = _trade.getTradeInfo().getTradeDate().get();
    LocalDate stepInDate = _trade.getStepInDate();
    LocalDate cashSettleDate = _trade.getTradeInfo().getSettlementDate().get();
    BusinessDayConvention businessdayAdjustmentConvention = _trade.getProduct().getGeneralTerms().getDateAdjustments().getConvention();
    HolidayCalendar calendar = _trade.getProduct().getGeneralTerms().getDateAdjustments().getCalendar();
    BusinessDayAdjustment businessDayAdjustment = BusinessDayAdjustment.of(
        businessdayAdjustmentConvention,
        calendar
    );
    LocalDate accStartDate = businessDayAdjustment.adjust(
        _trade.getProduct().getGeneralTerms().getEffectiveDate()
    );
    LocalDate endDate = _trade.getProduct().getGeneralTerms().getScheduledTerminationDate();
    boolean payAccOnDefault = _trade.isPayAccOnDefault();
    Period paymentInterval = _trade.getProduct().getFeeLeg().getPeriodicPayments().getPaymentFrequency().getPeriod();
    StubConvention stubConvention = _trade.getProduct().getFeeLeg().getPeriodicPayments().getStubConvention();
    DayCount accrualDayCount = _trade.getProduct().getFeeLeg().getPeriodicPayments().getFixedAmountCalculation().getDayCountFraction();
    BuySell buySellProtection = _trade.getProduct().getGeneralTerms().getBuySellProtection();
    Optional<SinglePayment> fee = _trade.getProduct().getFeeLeg().getSinglePayment();
    final Double upfrontFeeAmount;
    final LocalDate upfrontFeePaymentDate;
    if (fee.isPresent()) {
      upfrontFeeAmount = fee.get().getFixedAmount();
      upfrontFeePaymentDate = fee.get().getPaymentDate();

    } else {
      upfrontFeeAmount = Double.NaN;
      upfrontFeePaymentDate = null;

    }
    double coupon = _trade.getProduct().getFeeLeg().getPeriodicPayments().getFixedAmountCalculation().getFixedRate();
    double notional = _trade.getProduct().getFeeLeg().getPeriodicPayments().getFixedAmountCalculation().getCalculationAmount().getAmount();
    Currency currency = _trade.getProduct().getFeeLeg().getPeriodicPayments().getFixedAmountCalculation().getCalculationAmount().getCurrency();

    return ExpandedCdsTrade
        .builder()
        .tradeDate(tradeDate)
        .stepInDate(stepInDate)
        .cashSettleDate(cashSettleDate)
        .accStartDate(accStartDate)
        .endDate(endDate)
        .payAccOnDefault(payAccOnDefault)
        .paymentInterval(paymentInterval)
        .stubConvention(stubConvention)
        .businessdayAdjustmentConvention(businessdayAdjustmentConvention)
        .calendar(calendar)
        .accrualDayCount(accrualDayCount)
        .buySellProtection(buySellProtection)
        .accrualDayCount(accrualDayCount)
        .upfrontFeeAmount(upfrontFeeAmount)
        .upfrontFeePaymentDate(upfrontFeePaymentDate)
        .coupon(coupon)
        .notional(notional)
        .currency(currency)
        .build();
  }

  //------------------------- AUTOGENERATED START -------------------------
  ///CLOVER:OFF
  /**
   * The meta-bean for {@code ExpandedCdsTrade}.
   * @return the meta-bean, not null
   */
  public static ExpandedCdsTrade.Meta meta() {
    return ExpandedCdsTrade.Meta.INSTANCE;
  }

  static {
    JodaBeanUtils.registerMetaBean(ExpandedCdsTrade.Meta.INSTANCE);
  }

  /**
   * The serialization version id.
   */
  private static final long serialVersionUID = 1L;

  /**
   * Returns a builder used to create an instance of the bean.
   * @return the builder, not null
   */
  public static ExpandedCdsTrade.Builder builder() {
    return new ExpandedCdsTrade.Builder();
  }

  /**
   * Restricted constructor.
   * @param builder  the builder to copy from, not null
   */
  protected ExpandedCdsTrade(ExpandedCdsTrade.Builder builder) {
    JodaBeanUtils.notNull(builder.tradeDate, "tradeDate");
    JodaBeanUtils.notNull(builder.stepInDate, "stepInDate");
    JodaBeanUtils.notNull(builder.cashSettleDate, "cashSettleDate");
    JodaBeanUtils.notNull(builder.accStartDate, "accStartDate");
    JodaBeanUtils.notNull(builder.endDate, "endDate");
    JodaBeanUtils.notNull(builder.payAccOnDefault, "payAccOnDefault");
    JodaBeanUtils.notNull(builder.paymentInterval, "paymentInterval");
    JodaBeanUtils.notNull(builder.stubConvention, "stubConvention");
    JodaBeanUtils.notNull(builder.businessdayAdjustmentConvention, "businessdayAdjustmentConvention");
    JodaBeanUtils.notNull(builder.calendar, "calendar");
    JodaBeanUtils.notNull(builder.accrualDayCount, "accrualDayCount");
    JodaBeanUtils.notNull(builder.buySellProtection, "buySellProtection");
    JodaBeanUtils.notNull(builder.upfrontFeeAmount, "upfrontFeeAmount");
    JodaBeanUtils.notNull(builder.coupon, "coupon");
    JodaBeanUtils.notNull(builder.notional, "notional");
    JodaBeanUtils.notNull(builder.currency, "currency");
    this.tradeDate = builder.tradeDate;
    this.stepInDate = builder.stepInDate;
    this.cashSettleDate = builder.cashSettleDate;
    this.accStartDate = builder.accStartDate;
    this.endDate = builder.endDate;
    this.payAccOnDefault = builder.payAccOnDefault;
    this.paymentInterval = builder.paymentInterval;
    this.stubConvention = builder.stubConvention;
    this.businessdayAdjustmentConvention = builder.businessdayAdjustmentConvention;
    this.calendar = builder.calendar;
    this.accrualDayCount = builder.accrualDayCount;
    this.buySellProtection = builder.buySellProtection;
    this.upfrontFeeAmount = builder.upfrontFeeAmount;
    this.upfrontFeePaymentDate = builder.upfrontFeePaymentDate;
    this.coupon = builder.coupon;
    this.notional = builder.notional;
    this.currency = builder.currency;
  }

  @Override
  public ExpandedCdsTrade.Meta metaBean() {
    return ExpandedCdsTrade.Meta.INSTANCE;
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
   * Gets tradeDate The trade date
   * @return the value of the property, not null
   */
  @Override
  public LocalDate getTradeDate() {
    return tradeDate;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets typically T+1 unadjusted. Required by the model.
   * @return the value of the property, not null
   */
  @Override
  public LocalDate getStepInDate() {
    return stepInDate;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the date that values are PVed to. Is is normally today + 3 business days.  Aka cash-settle date.
   * @return the value of the property, not null
   */
  @Override
  public LocalDate getCashSettleDate() {
    return cashSettleDate;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets accStartDate This is when the CDS nominally starts in terms of premium payments.
   * i.e. the number of days in the first period (and thus the amount of the first premium payment)
   * is counted from this date.
   * <p>
   * This should be adjusted according business day and holidays
   * @return the value of the property, not null
   */
  @Override
  public LocalDate getAccStartDate() {
    return accStartDate;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets endDate (aka maturity date) This is when the contract expires and protection ends -
   * any default after this date does not trigger a payment. (the protection ends at end of day)
   * <p>
   * This is an adjusted date and can fall on a holiday or weekend.
   * @return the value of the property, not null
   */
  @Override
  public LocalDate getEndDate() {
    return endDate;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets payAccOnDefault Is the accrued premium paid in the event of a default
   * @return the value of the property, not null
   */
  @Override
  public boolean isPayAccOnDefault() {
    return payAccOnDefault;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets paymentInterval The nominal step between premium payments (e.g. 3 months, 6 months).
   * @return the value of the property, not null
   */
  @Override
  public Period getPaymentInterval() {
    return paymentInterval;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets stubType stubType Options are FRONTSHORT, FRONTLONG, BACKSHORT, BACKLONG or NONE
   * - <b>Note</b> in this code NONE is not allowed
   * @return the value of the property, not null
   */
  @Override
  public StubConvention getStubConvention() {
    return stubConvention;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets businessdayAdjustmentConvention How are adjustments for non-business days made
   * @return the value of the property, not null
   */
  @Override
  public BusinessDayConvention getBusinessdayAdjustmentConvention() {
    return businessdayAdjustmentConvention;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets calendar HolidayCalendar defining what is a non-business day
   * @return the value of the property, not null
   */
  @Override
  public HolidayCalendar getCalendar() {
    return calendar;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets accrualDayCount Day count used for accrual
   * @return the value of the property, not null
   */
  @Override
  public DayCount getAccrualDayCount() {
    return accrualDayCount;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets are we buying protection and paying fees or are we selling protection and receiving fees
   * @return the value of the property, not null
   */
  @Override
  public BuySell getBuySellProtection() {
    return buySellProtection;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets optional upfront fee amount, will be NaN if there is no fee
   * @return the value of the property, not null
   */
  @Override
  public double getUpfrontFeeAmount() {
    return upfrontFeeAmount;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets optional upfront fee date, will return null if called and there is no fee
   * check the fee amount first before calling
   * @return the value of the property
   */
  @Override
  public LocalDate getUpfrontFeePaymentDate() {
    return upfrontFeePaymentDate;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets coupon used to calc fee payments
   * @return the value of the property, not null
   */
  @Override
  public double getCoupon() {
    return coupon;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets notional amount used to calc fee payments
   * @return the value of the property, not null
   */
  @Override
  public double getNotional() {
    return notional;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets currency fees are paid in
   * @return the value of the property, not null
   */
  @Override
  public Currency getCurrency() {
    return currency;
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
      ExpandedCdsTrade other = (ExpandedCdsTrade) obj;
      return JodaBeanUtils.equal(getTradeDate(), other.getTradeDate()) &&
          JodaBeanUtils.equal(getStepInDate(), other.getStepInDate()) &&
          JodaBeanUtils.equal(getCashSettleDate(), other.getCashSettleDate()) &&
          JodaBeanUtils.equal(getAccStartDate(), other.getAccStartDate()) &&
          JodaBeanUtils.equal(getEndDate(), other.getEndDate()) &&
          (isPayAccOnDefault() == other.isPayAccOnDefault()) &&
          JodaBeanUtils.equal(getPaymentInterval(), other.getPaymentInterval()) &&
          JodaBeanUtils.equal(getStubConvention(), other.getStubConvention()) &&
          JodaBeanUtils.equal(getBusinessdayAdjustmentConvention(), other.getBusinessdayAdjustmentConvention()) &&
          JodaBeanUtils.equal(getCalendar(), other.getCalendar()) &&
          JodaBeanUtils.equal(getAccrualDayCount(), other.getAccrualDayCount()) &&
          JodaBeanUtils.equal(getBuySellProtection(), other.getBuySellProtection()) &&
          JodaBeanUtils.equal(getUpfrontFeeAmount(), other.getUpfrontFeeAmount()) &&
          JodaBeanUtils.equal(getUpfrontFeePaymentDate(), other.getUpfrontFeePaymentDate()) &&
          JodaBeanUtils.equal(getCoupon(), other.getCoupon()) &&
          JodaBeanUtils.equal(getNotional(), other.getNotional()) &&
          JodaBeanUtils.equal(getCurrency(), other.getCurrency());
    }
    return false;
  }

  @Override
  public int hashCode() {
    int hash = getClass().hashCode();
    hash = hash * 31 + JodaBeanUtils.hashCode(getTradeDate());
    hash = hash * 31 + JodaBeanUtils.hashCode(getStepInDate());
    hash = hash * 31 + JodaBeanUtils.hashCode(getCashSettleDate());
    hash = hash * 31 + JodaBeanUtils.hashCode(getAccStartDate());
    hash = hash * 31 + JodaBeanUtils.hashCode(getEndDate());
    hash = hash * 31 + JodaBeanUtils.hashCode(isPayAccOnDefault());
    hash = hash * 31 + JodaBeanUtils.hashCode(getPaymentInterval());
    hash = hash * 31 + JodaBeanUtils.hashCode(getStubConvention());
    hash = hash * 31 + JodaBeanUtils.hashCode(getBusinessdayAdjustmentConvention());
    hash = hash * 31 + JodaBeanUtils.hashCode(getCalendar());
    hash = hash * 31 + JodaBeanUtils.hashCode(getAccrualDayCount());
    hash = hash * 31 + JodaBeanUtils.hashCode(getBuySellProtection());
    hash = hash * 31 + JodaBeanUtils.hashCode(getUpfrontFeeAmount());
    hash = hash * 31 + JodaBeanUtils.hashCode(getUpfrontFeePaymentDate());
    hash = hash * 31 + JodaBeanUtils.hashCode(getCoupon());
    hash = hash * 31 + JodaBeanUtils.hashCode(getNotional());
    hash = hash * 31 + JodaBeanUtils.hashCode(getCurrency());
    return hash;
  }

  @Override
  public String toString() {
    StringBuilder buf = new StringBuilder(576);
    buf.append("ExpandedCdsTrade{");
    int len = buf.length();
    toString(buf);
    if (buf.length() > len) {
      buf.setLength(buf.length() - 2);
    }
    buf.append('}');
    return buf.toString();
  }

  protected void toString(StringBuilder buf) {
    buf.append("tradeDate").append('=').append(JodaBeanUtils.toString(getTradeDate())).append(',').append(' ');
    buf.append("stepInDate").append('=').append(JodaBeanUtils.toString(getStepInDate())).append(',').append(' ');
    buf.append("cashSettleDate").append('=').append(JodaBeanUtils.toString(getCashSettleDate())).append(',').append(' ');
    buf.append("accStartDate").append('=').append(JodaBeanUtils.toString(getAccStartDate())).append(',').append(' ');
    buf.append("endDate").append('=').append(JodaBeanUtils.toString(getEndDate())).append(',').append(' ');
    buf.append("payAccOnDefault").append('=').append(JodaBeanUtils.toString(isPayAccOnDefault())).append(',').append(' ');
    buf.append("paymentInterval").append('=').append(JodaBeanUtils.toString(getPaymentInterval())).append(',').append(' ');
    buf.append("stubConvention").append('=').append(JodaBeanUtils.toString(getStubConvention())).append(',').append(' ');
    buf.append("businessdayAdjustmentConvention").append('=').append(JodaBeanUtils.toString(getBusinessdayAdjustmentConvention())).append(',').append(' ');
    buf.append("calendar").append('=').append(JodaBeanUtils.toString(getCalendar())).append(',').append(' ');
    buf.append("accrualDayCount").append('=').append(JodaBeanUtils.toString(getAccrualDayCount())).append(',').append(' ');
    buf.append("buySellProtection").append('=').append(JodaBeanUtils.toString(getBuySellProtection())).append(',').append(' ');
    buf.append("upfrontFeeAmount").append('=').append(JodaBeanUtils.toString(getUpfrontFeeAmount())).append(',').append(' ');
    buf.append("upfrontFeePaymentDate").append('=').append(JodaBeanUtils.toString(getUpfrontFeePaymentDate())).append(',').append(' ');
    buf.append("coupon").append('=').append(JodaBeanUtils.toString(getCoupon())).append(',').append(' ');
    buf.append("notional").append('=').append(JodaBeanUtils.toString(getNotional())).append(',').append(' ');
    buf.append("currency").append('=').append(JodaBeanUtils.toString(getCurrency())).append(',').append(' ');
  }

  //-----------------------------------------------------------------------
  /**
   * The meta-bean for {@code ExpandedCdsTrade}.
   */
  public static class Meta extends DirectMetaBean {
    /**
     * The singleton instance of the meta-bean.
     */
    static final Meta INSTANCE = new Meta();

    /**
     * The meta-property for the {@code tradeDate} property.
     */
    private final MetaProperty<LocalDate> tradeDate = DirectMetaProperty.ofImmutable(
        this, "tradeDate", ExpandedCdsTrade.class, LocalDate.class);
    /**
     * The meta-property for the {@code stepInDate} property.
     */
    private final MetaProperty<LocalDate> stepInDate = DirectMetaProperty.ofImmutable(
        this, "stepInDate", ExpandedCdsTrade.class, LocalDate.class);
    /**
     * The meta-property for the {@code cashSettleDate} property.
     */
    private final MetaProperty<LocalDate> cashSettleDate = DirectMetaProperty.ofImmutable(
        this, "cashSettleDate", ExpandedCdsTrade.class, LocalDate.class);
    /**
     * The meta-property for the {@code accStartDate} property.
     */
    private final MetaProperty<LocalDate> accStartDate = DirectMetaProperty.ofImmutable(
        this, "accStartDate", ExpandedCdsTrade.class, LocalDate.class);
    /**
     * The meta-property for the {@code endDate} property.
     */
    private final MetaProperty<LocalDate> endDate = DirectMetaProperty.ofImmutable(
        this, "endDate", ExpandedCdsTrade.class, LocalDate.class);
    /**
     * The meta-property for the {@code payAccOnDefault} property.
     */
    private final MetaProperty<Boolean> payAccOnDefault = DirectMetaProperty.ofImmutable(
        this, "payAccOnDefault", ExpandedCdsTrade.class, Boolean.TYPE);
    /**
     * The meta-property for the {@code paymentInterval} property.
     */
    private final MetaProperty<Period> paymentInterval = DirectMetaProperty.ofImmutable(
        this, "paymentInterval", ExpandedCdsTrade.class, Period.class);
    /**
     * The meta-property for the {@code stubConvention} property.
     */
    private final MetaProperty<StubConvention> stubConvention = DirectMetaProperty.ofImmutable(
        this, "stubConvention", ExpandedCdsTrade.class, StubConvention.class);
    /**
     * The meta-property for the {@code businessdayAdjustmentConvention} property.
     */
    private final MetaProperty<BusinessDayConvention> businessdayAdjustmentConvention = DirectMetaProperty.ofImmutable(
        this, "businessdayAdjustmentConvention", ExpandedCdsTrade.class, BusinessDayConvention.class);
    /**
     * The meta-property for the {@code calendar} property.
     */
    private final MetaProperty<HolidayCalendar> calendar = DirectMetaProperty.ofImmutable(
        this, "calendar", ExpandedCdsTrade.class, HolidayCalendar.class);
    /**
     * The meta-property for the {@code accrualDayCount} property.
     */
    private final MetaProperty<DayCount> accrualDayCount = DirectMetaProperty.ofImmutable(
        this, "accrualDayCount", ExpandedCdsTrade.class, DayCount.class);
    /**
     * The meta-property for the {@code buySellProtection} property.
     */
    private final MetaProperty<BuySell> buySellProtection = DirectMetaProperty.ofImmutable(
        this, "buySellProtection", ExpandedCdsTrade.class, BuySell.class);
    /**
     * The meta-property for the {@code upfrontFeeAmount} property.
     */
    private final MetaProperty<Double> upfrontFeeAmount = DirectMetaProperty.ofImmutable(
        this, "upfrontFeeAmount", ExpandedCdsTrade.class, Double.TYPE);
    /**
     * The meta-property for the {@code upfrontFeePaymentDate} property.
     */
    private final MetaProperty<LocalDate> upfrontFeePaymentDate = DirectMetaProperty.ofImmutable(
        this, "upfrontFeePaymentDate", ExpandedCdsTrade.class, LocalDate.class);
    /**
     * The meta-property for the {@code coupon} property.
     */
    private final MetaProperty<Double> coupon = DirectMetaProperty.ofImmutable(
        this, "coupon", ExpandedCdsTrade.class, Double.TYPE);
    /**
     * The meta-property for the {@code notional} property.
     */
    private final MetaProperty<Double> notional = DirectMetaProperty.ofImmutable(
        this, "notional", ExpandedCdsTrade.class, Double.TYPE);
    /**
     * The meta-property for the {@code currency} property.
     */
    private final MetaProperty<Currency> currency = DirectMetaProperty.ofImmutable(
        this, "currency", ExpandedCdsTrade.class, Currency.class);
    /**
     * The meta-properties.
     */
    private final Map<String, MetaProperty<?>> metaPropertyMap$ = new DirectMetaPropertyMap(
        this, null,
        "tradeDate",
        "stepInDate",
        "cashSettleDate",
        "accStartDate",
        "endDate",
        "payAccOnDefault",
        "paymentInterval",
        "stubConvention",
        "businessdayAdjustmentConvention",
        "calendar",
        "accrualDayCount",
        "buySellProtection",
        "upfrontFeeAmount",
        "upfrontFeePaymentDate",
        "coupon",
        "notional",
        "currency");

    /**
     * Restricted constructor.
     */
    protected Meta() {
    }

    @Override
    protected MetaProperty<?> metaPropertyGet(String propertyName) {
      switch (propertyName.hashCode()) {
        case 752419634:  // tradeDate
          return tradeDate;
        case -1890516897:  // stepInDate
          return stepInDate;
        case 625347372:  // cashSettleDate
          return cashSettleDate;
        case -274214993:  // accStartDate
          return accStartDate;
        case -1607727319:  // endDate
          return endDate;
        case -988493655:  // payAccOnDefault
          return payAccOnDefault;
        case -230746901:  // paymentInterval
          return paymentInterval;
        case -31408449:  // stubConvention
          return stubConvention;
        case -283944262:  // businessdayAdjustmentConvention
          return businessdayAdjustmentConvention;
        case -178324674:  // calendar
          return calendar;
        case -1387075166:  // accrualDayCount
          return accrualDayCount;
        case -405622799:  // buySellProtection
          return buySellProtection;
        case 2020904624:  // upfrontFeeAmount
          return upfrontFeeAmount;
        case 508500860:  // upfrontFeePaymentDate
          return upfrontFeePaymentDate;
        case -1354573786:  // coupon
          return coupon;
        case 1585636160:  // notional
          return notional;
        case 575402001:  // currency
          return currency;
      }
      return super.metaPropertyGet(propertyName);
    }

    @Override
    public ExpandedCdsTrade.Builder builder() {
      return new ExpandedCdsTrade.Builder();
    }

    @Override
    public Class<? extends ExpandedCdsTrade> beanType() {
      return ExpandedCdsTrade.class;
    }

    @Override
    public Map<String, MetaProperty<?>> metaPropertyMap() {
      return metaPropertyMap$;
    }

    //-----------------------------------------------------------------------
    /**
     * The meta-property for the {@code tradeDate} property.
     * @return the meta-property, not null
     */
    public final MetaProperty<LocalDate> tradeDate() {
      return tradeDate;
    }

    /**
     * The meta-property for the {@code stepInDate} property.
     * @return the meta-property, not null
     */
    public final MetaProperty<LocalDate> stepInDate() {
      return stepInDate;
    }

    /**
     * The meta-property for the {@code cashSettleDate} property.
     * @return the meta-property, not null
     */
    public final MetaProperty<LocalDate> cashSettleDate() {
      return cashSettleDate;
    }

    /**
     * The meta-property for the {@code accStartDate} property.
     * @return the meta-property, not null
     */
    public final MetaProperty<LocalDate> accStartDate() {
      return accStartDate;
    }

    /**
     * The meta-property for the {@code endDate} property.
     * @return the meta-property, not null
     */
    public final MetaProperty<LocalDate> endDate() {
      return endDate;
    }

    /**
     * The meta-property for the {@code payAccOnDefault} property.
     * @return the meta-property, not null
     */
    public final MetaProperty<Boolean> payAccOnDefault() {
      return payAccOnDefault;
    }

    /**
     * The meta-property for the {@code paymentInterval} property.
     * @return the meta-property, not null
     */
    public final MetaProperty<Period> paymentInterval() {
      return paymentInterval;
    }

    /**
     * The meta-property for the {@code stubConvention} property.
     * @return the meta-property, not null
     */
    public final MetaProperty<StubConvention> stubConvention() {
      return stubConvention;
    }

    /**
     * The meta-property for the {@code businessdayAdjustmentConvention} property.
     * @return the meta-property, not null
     */
    public final MetaProperty<BusinessDayConvention> businessdayAdjustmentConvention() {
      return businessdayAdjustmentConvention;
    }

    /**
     * The meta-property for the {@code calendar} property.
     * @return the meta-property, not null
     */
    public final MetaProperty<HolidayCalendar> calendar() {
      return calendar;
    }

    /**
     * The meta-property for the {@code accrualDayCount} property.
     * @return the meta-property, not null
     */
    public final MetaProperty<DayCount> accrualDayCount() {
      return accrualDayCount;
    }

    /**
     * The meta-property for the {@code buySellProtection} property.
     * @return the meta-property, not null
     */
    public final MetaProperty<BuySell> buySellProtection() {
      return buySellProtection;
    }

    /**
     * The meta-property for the {@code upfrontFeeAmount} property.
     * @return the meta-property, not null
     */
    public final MetaProperty<Double> upfrontFeeAmount() {
      return upfrontFeeAmount;
    }

    /**
     * The meta-property for the {@code upfrontFeePaymentDate} property.
     * @return the meta-property, not null
     */
    public final MetaProperty<LocalDate> upfrontFeePaymentDate() {
      return upfrontFeePaymentDate;
    }

    /**
     * The meta-property for the {@code coupon} property.
     * @return the meta-property, not null
     */
    public final MetaProperty<Double> coupon() {
      return coupon;
    }

    /**
     * The meta-property for the {@code notional} property.
     * @return the meta-property, not null
     */
    public final MetaProperty<Double> notional() {
      return notional;
    }

    /**
     * The meta-property for the {@code currency} property.
     * @return the meta-property, not null
     */
    public final MetaProperty<Currency> currency() {
      return currency;
    }

    //-----------------------------------------------------------------------
    @Override
    protected Object propertyGet(Bean bean, String propertyName, boolean quiet) {
      switch (propertyName.hashCode()) {
        case 752419634:  // tradeDate
          return ((ExpandedCdsTrade) bean).getTradeDate();
        case -1890516897:  // stepInDate
          return ((ExpandedCdsTrade) bean).getStepInDate();
        case 625347372:  // cashSettleDate
          return ((ExpandedCdsTrade) bean).getCashSettleDate();
        case -274214993:  // accStartDate
          return ((ExpandedCdsTrade) bean).getAccStartDate();
        case -1607727319:  // endDate
          return ((ExpandedCdsTrade) bean).getEndDate();
        case -988493655:  // payAccOnDefault
          return ((ExpandedCdsTrade) bean).isPayAccOnDefault();
        case -230746901:  // paymentInterval
          return ((ExpandedCdsTrade) bean).getPaymentInterval();
        case -31408449:  // stubConvention
          return ((ExpandedCdsTrade) bean).getStubConvention();
        case -283944262:  // businessdayAdjustmentConvention
          return ((ExpandedCdsTrade) bean).getBusinessdayAdjustmentConvention();
        case -178324674:  // calendar
          return ((ExpandedCdsTrade) bean).getCalendar();
        case -1387075166:  // accrualDayCount
          return ((ExpandedCdsTrade) bean).getAccrualDayCount();
        case -405622799:  // buySellProtection
          return ((ExpandedCdsTrade) bean).getBuySellProtection();
        case 2020904624:  // upfrontFeeAmount
          return ((ExpandedCdsTrade) bean).getUpfrontFeeAmount();
        case 508500860:  // upfrontFeePaymentDate
          return ((ExpandedCdsTrade) bean).getUpfrontFeePaymentDate();
        case -1354573786:  // coupon
          return ((ExpandedCdsTrade) bean).getCoupon();
        case 1585636160:  // notional
          return ((ExpandedCdsTrade) bean).getNotional();
        case 575402001:  // currency
          return ((ExpandedCdsTrade) bean).getCurrency();
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
   * The bean-builder for {@code ExpandedCdsTrade}.
   */
  public static class Builder extends DirectFieldsBeanBuilder<ExpandedCdsTrade> {

    private LocalDate tradeDate;
    private LocalDate stepInDate;
    private LocalDate cashSettleDate;
    private LocalDate accStartDate;
    private LocalDate endDate;
    private boolean payAccOnDefault;
    private Period paymentInterval;
    private StubConvention stubConvention;
    private BusinessDayConvention businessdayAdjustmentConvention;
    private HolidayCalendar calendar;
    private DayCount accrualDayCount;
    private BuySell buySellProtection;
    private double upfrontFeeAmount;
    private LocalDate upfrontFeePaymentDate;
    private double coupon;
    private double notional;
    private Currency currency;

    /**
     * Restricted constructor.
     */
    protected Builder() {
    }

    /**
     * Restricted copy constructor.
     * @param beanToCopy  the bean to copy from, not null
     */
    protected Builder(ExpandedCdsTrade beanToCopy) {
      this.tradeDate = beanToCopy.getTradeDate();
      this.stepInDate = beanToCopy.getStepInDate();
      this.cashSettleDate = beanToCopy.getCashSettleDate();
      this.accStartDate = beanToCopy.getAccStartDate();
      this.endDate = beanToCopy.getEndDate();
      this.payAccOnDefault = beanToCopy.isPayAccOnDefault();
      this.paymentInterval = beanToCopy.getPaymentInterval();
      this.stubConvention = beanToCopy.getStubConvention();
      this.businessdayAdjustmentConvention = beanToCopy.getBusinessdayAdjustmentConvention();
      this.calendar = beanToCopy.getCalendar();
      this.accrualDayCount = beanToCopy.getAccrualDayCount();
      this.buySellProtection = beanToCopy.getBuySellProtection();
      this.upfrontFeeAmount = beanToCopy.getUpfrontFeeAmount();
      this.upfrontFeePaymentDate = beanToCopy.getUpfrontFeePaymentDate();
      this.coupon = beanToCopy.getCoupon();
      this.notional = beanToCopy.getNotional();
      this.currency = beanToCopy.getCurrency();
    }

    //-----------------------------------------------------------------------
    @Override
    public Object get(String propertyName) {
      switch (propertyName.hashCode()) {
        case 752419634:  // tradeDate
          return tradeDate;
        case -1890516897:  // stepInDate
          return stepInDate;
        case 625347372:  // cashSettleDate
          return cashSettleDate;
        case -274214993:  // accStartDate
          return accStartDate;
        case -1607727319:  // endDate
          return endDate;
        case -988493655:  // payAccOnDefault
          return payAccOnDefault;
        case -230746901:  // paymentInterval
          return paymentInterval;
        case -31408449:  // stubConvention
          return stubConvention;
        case -283944262:  // businessdayAdjustmentConvention
          return businessdayAdjustmentConvention;
        case -178324674:  // calendar
          return calendar;
        case -1387075166:  // accrualDayCount
          return accrualDayCount;
        case -405622799:  // buySellProtection
          return buySellProtection;
        case 2020904624:  // upfrontFeeAmount
          return upfrontFeeAmount;
        case 508500860:  // upfrontFeePaymentDate
          return upfrontFeePaymentDate;
        case -1354573786:  // coupon
          return coupon;
        case 1585636160:  // notional
          return notional;
        case 575402001:  // currency
          return currency;
        default:
          throw new NoSuchElementException("Unknown property: " + propertyName);
      }
    }

    @Override
    public Builder set(String propertyName, Object newValue) {
      switch (propertyName.hashCode()) {
        case 752419634:  // tradeDate
          this.tradeDate = (LocalDate) newValue;
          break;
        case -1890516897:  // stepInDate
          this.stepInDate = (LocalDate) newValue;
          break;
        case 625347372:  // cashSettleDate
          this.cashSettleDate = (LocalDate) newValue;
          break;
        case -274214993:  // accStartDate
          this.accStartDate = (LocalDate) newValue;
          break;
        case -1607727319:  // endDate
          this.endDate = (LocalDate) newValue;
          break;
        case -988493655:  // payAccOnDefault
          this.payAccOnDefault = (Boolean) newValue;
          break;
        case -230746901:  // paymentInterval
          this.paymentInterval = (Period) newValue;
          break;
        case -31408449:  // stubConvention
          this.stubConvention = (StubConvention) newValue;
          break;
        case -283944262:  // businessdayAdjustmentConvention
          this.businessdayAdjustmentConvention = (BusinessDayConvention) newValue;
          break;
        case -178324674:  // calendar
          this.calendar = (HolidayCalendar) newValue;
          break;
        case -1387075166:  // accrualDayCount
          this.accrualDayCount = (DayCount) newValue;
          break;
        case -405622799:  // buySellProtection
          this.buySellProtection = (BuySell) newValue;
          break;
        case 2020904624:  // upfrontFeeAmount
          this.upfrontFeeAmount = (Double) newValue;
          break;
        case 508500860:  // upfrontFeePaymentDate
          this.upfrontFeePaymentDate = (LocalDate) newValue;
          break;
        case -1354573786:  // coupon
          this.coupon = (Double) newValue;
          break;
        case 1585636160:  // notional
          this.notional = (Double) newValue;
          break;
        case 575402001:  // currency
          this.currency = (Currency) newValue;
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
    public ExpandedCdsTrade build() {
      return new ExpandedCdsTrade(this);
    }

    //-----------------------------------------------------------------------
    /**
     * Sets the {@code tradeDate} property in the builder.
     * @param tradeDate  the new value, not null
     * @return this, for chaining, not null
     */
    public Builder tradeDate(LocalDate tradeDate) {
      JodaBeanUtils.notNull(tradeDate, "tradeDate");
      this.tradeDate = tradeDate;
      return this;
    }

    /**
     * Sets the {@code stepInDate} property in the builder.
     * @param stepInDate  the new value, not null
     * @return this, for chaining, not null
     */
    public Builder stepInDate(LocalDate stepInDate) {
      JodaBeanUtils.notNull(stepInDate, "stepInDate");
      this.stepInDate = stepInDate;
      return this;
    }

    /**
     * Sets the {@code cashSettleDate} property in the builder.
     * @param cashSettleDate  the new value, not null
     * @return this, for chaining, not null
     */
    public Builder cashSettleDate(LocalDate cashSettleDate) {
      JodaBeanUtils.notNull(cashSettleDate, "cashSettleDate");
      this.cashSettleDate = cashSettleDate;
      return this;
    }

    /**
     * Sets the {@code accStartDate} property in the builder.
     * @param accStartDate  the new value, not null
     * @return this, for chaining, not null
     */
    public Builder accStartDate(LocalDate accStartDate) {
      JodaBeanUtils.notNull(accStartDate, "accStartDate");
      this.accStartDate = accStartDate;
      return this;
    }

    /**
     * Sets the {@code endDate} property in the builder.
     * @param endDate  the new value, not null
     * @return this, for chaining, not null
     */
    public Builder endDate(LocalDate endDate) {
      JodaBeanUtils.notNull(endDate, "endDate");
      this.endDate = endDate;
      return this;
    }

    /**
     * Sets the {@code payAccOnDefault} property in the builder.
     * @param payAccOnDefault  the new value, not null
     * @return this, for chaining, not null
     */
    public Builder payAccOnDefault(boolean payAccOnDefault) {
      JodaBeanUtils.notNull(payAccOnDefault, "payAccOnDefault");
      this.payAccOnDefault = payAccOnDefault;
      return this;
    }

    /**
     * Sets the {@code paymentInterval} property in the builder.
     * @param paymentInterval  the new value, not null
     * @return this, for chaining, not null
     */
    public Builder paymentInterval(Period paymentInterval) {
      JodaBeanUtils.notNull(paymentInterval, "paymentInterval");
      this.paymentInterval = paymentInterval;
      return this;
    }

    /**
     * Sets the {@code stubConvention} property in the builder.
     * @param stubConvention  the new value, not null
     * @return this, for chaining, not null
     */
    public Builder stubConvention(StubConvention stubConvention) {
      JodaBeanUtils.notNull(stubConvention, "stubConvention");
      this.stubConvention = stubConvention;
      return this;
    }

    /**
     * Sets the {@code businessdayAdjustmentConvention} property in the builder.
     * @param businessdayAdjustmentConvention  the new value, not null
     * @return this, for chaining, not null
     */
    public Builder businessdayAdjustmentConvention(BusinessDayConvention businessdayAdjustmentConvention) {
      JodaBeanUtils.notNull(businessdayAdjustmentConvention, "businessdayAdjustmentConvention");
      this.businessdayAdjustmentConvention = businessdayAdjustmentConvention;
      return this;
    }

    /**
     * Sets the {@code calendar} property in the builder.
     * @param calendar  the new value, not null
     * @return this, for chaining, not null
     */
    public Builder calendar(HolidayCalendar calendar) {
      JodaBeanUtils.notNull(calendar, "calendar");
      this.calendar = calendar;
      return this;
    }

    /**
     * Sets the {@code accrualDayCount} property in the builder.
     * @param accrualDayCount  the new value, not null
     * @return this, for chaining, not null
     */
    public Builder accrualDayCount(DayCount accrualDayCount) {
      JodaBeanUtils.notNull(accrualDayCount, "accrualDayCount");
      this.accrualDayCount = accrualDayCount;
      return this;
    }

    /**
     * Sets the {@code buySellProtection} property in the builder.
     * @param buySellProtection  the new value, not null
     * @return this, for chaining, not null
     */
    public Builder buySellProtection(BuySell buySellProtection) {
      JodaBeanUtils.notNull(buySellProtection, "buySellProtection");
      this.buySellProtection = buySellProtection;
      return this;
    }

    /**
     * Sets the {@code upfrontFeeAmount} property in the builder.
     * @param upfrontFeeAmount  the new value, not null
     * @return this, for chaining, not null
     */
    public Builder upfrontFeeAmount(double upfrontFeeAmount) {
      JodaBeanUtils.notNull(upfrontFeeAmount, "upfrontFeeAmount");
      this.upfrontFeeAmount = upfrontFeeAmount;
      return this;
    }

    /**
     * Sets the {@code upfrontFeePaymentDate} property in the builder.
     * @param upfrontFeePaymentDate  the new value
     * @return this, for chaining, not null
     */
    public Builder upfrontFeePaymentDate(LocalDate upfrontFeePaymentDate) {
      this.upfrontFeePaymentDate = upfrontFeePaymentDate;
      return this;
    }

    /**
     * Sets the {@code coupon} property in the builder.
     * @param coupon  the new value, not null
     * @return this, for chaining, not null
     */
    public Builder coupon(double coupon) {
      JodaBeanUtils.notNull(coupon, "coupon");
      this.coupon = coupon;
      return this;
    }

    /**
     * Sets the {@code notional} property in the builder.
     * @param notional  the new value, not null
     * @return this, for chaining, not null
     */
    public Builder notional(double notional) {
      JodaBeanUtils.notNull(notional, "notional");
      this.notional = notional;
      return this;
    }

    /**
     * Sets the {@code currency} property in the builder.
     * @param currency  the new value, not null
     * @return this, for chaining, not null
     */
    public Builder currency(Currency currency) {
      JodaBeanUtils.notNull(currency, "currency");
      this.currency = currency;
      return this;
    }

    //-----------------------------------------------------------------------
    @Override
    public String toString() {
      StringBuilder buf = new StringBuilder(576);
      buf.append("ExpandedCdsTrade.Builder{");
      int len = buf.length();
      toString(buf);
      if (buf.length() > len) {
        buf.setLength(buf.length() - 2);
      }
      buf.append('}');
      return buf.toString();
    }

    protected void toString(StringBuilder buf) {
      buf.append("tradeDate").append('=').append(JodaBeanUtils.toString(tradeDate)).append(',').append(' ');
      buf.append("stepInDate").append('=').append(JodaBeanUtils.toString(stepInDate)).append(',').append(' ');
      buf.append("cashSettleDate").append('=').append(JodaBeanUtils.toString(cashSettleDate)).append(',').append(' ');
      buf.append("accStartDate").append('=').append(JodaBeanUtils.toString(accStartDate)).append(',').append(' ');
      buf.append("endDate").append('=').append(JodaBeanUtils.toString(endDate)).append(',').append(' ');
      buf.append("payAccOnDefault").append('=').append(JodaBeanUtils.toString(payAccOnDefault)).append(',').append(' ');
      buf.append("paymentInterval").append('=').append(JodaBeanUtils.toString(paymentInterval)).append(',').append(' ');
      buf.append("stubConvention").append('=').append(JodaBeanUtils.toString(stubConvention)).append(',').append(' ');
      buf.append("businessdayAdjustmentConvention").append('=').append(JodaBeanUtils.toString(businessdayAdjustmentConvention)).append(',').append(' ');
      buf.append("calendar").append('=').append(JodaBeanUtils.toString(calendar)).append(',').append(' ');
      buf.append("accrualDayCount").append('=').append(JodaBeanUtils.toString(accrualDayCount)).append(',').append(' ');
      buf.append("buySellProtection").append('=').append(JodaBeanUtils.toString(buySellProtection)).append(',').append(' ');
      buf.append("upfrontFeeAmount").append('=').append(JodaBeanUtils.toString(upfrontFeeAmount)).append(',').append(' ');
      buf.append("upfrontFeePaymentDate").append('=').append(JodaBeanUtils.toString(upfrontFeePaymentDate)).append(',').append(' ');
      buf.append("coupon").append('=').append(JodaBeanUtils.toString(coupon)).append(',').append(' ');
      buf.append("notional").append('=').append(JodaBeanUtils.toString(notional)).append(',').append(' ');
      buf.append("currency").append('=').append(JodaBeanUtils.toString(currency)).append(',').append(' ');
    }

  }

  ///CLOVER:ON
  //-------------------------- AUTOGENERATED END --------------------------
}
