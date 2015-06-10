/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 * <p>
 * Please see distribution for license.
 */
package com.opengamma.strata.finance.credit;

import com.opengamma.strata.basics.BuySell;
import com.opengamma.strata.basics.currency.Currency;
import com.opengamma.strata.basics.currency.CurrencyAmount;
import com.opengamma.strata.basics.date.BusinessDayAdjustment;
import com.opengamma.strata.basics.date.BusinessDayConvention;
import com.opengamma.strata.basics.date.DayCount;
import com.opengamma.strata.basics.date.HolidayCalendar;
import com.opengamma.strata.basics.schedule.StubConvention;
import com.opengamma.strata.finance.Expandable;
import com.opengamma.strata.finance.ProductTrade;
import com.opengamma.strata.finance.TradeInfo;
import com.opengamma.strata.finance.credit.fee.SinglePayment;
import org.joda.beans.Bean;
import org.joda.beans.BeanDefinition;
import org.joda.beans.ImmutableBean;
import org.joda.beans.ImmutableDefaults;
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

/**
 * A trade in a credit default swap.
 * <p>
 * An Over-The-Counter (OTC) trade in a {@link Cds}.
 * <p>
 */
@BeanDefinition
public final class CdsTrade
    implements ProductTrade<Cds>, Expandable<ExpandedCdsTrade>, ImmutableBean, Serializable {

  /**
   * contains tradeDate:
   * The trade date or 'today', this is the date other times are measured from (i.e. t = 0)
   * <p>
   * contains settleDate:
   * The date that values are PVed to at inception. It is normally today + 3 business days.  Aka cash-settle date.
   */
  @PropertyDefinition(overrideGet = true)
  private final TradeInfo tradeInfo;
  /**
   * The swap product that was agreed when the trade occurred.
   * <p>
   * The product captures the contracted financial details of the trade.
   */
  @PropertyDefinition(validate = "notNull", overrideGet = true)
  private final Cds product;

  /**
   * Value that the ISDA Standard model needs but that does not occur in the FpML
   * <p>
   * (aka Protection Effective date or assignment date). Date when party assumes ownership.
   * This is usually T+1. This is when protection (and risk) starts in terms of the model.
   * Note, this is sometimes just called the Effective Date, however this can cause
   * confusion with the legal effective date which is T-60 or T-90.
   */
  @PropertyDefinition(validate = "notNull")
  private final LocalDate stepInDate;

  /**
   * Value that the ISDA Standard model needs but that does not occur in the FpML
   */
  @PropertyDefinition(validate = "notNull")
  private final boolean payAccOnDefault;

  //-------------------------------------------------------------------------
  @SuppressWarnings({"rawtypes", "unchecked"})
  @ImmutableDefaults
  private static void applyDefaults(Builder builder) {
    builder.tradeInfo = TradeInfo.EMPTY;
  }

  public static CdsTrade of(
      TradeInfo tradeInfo,
      Cds product,
      LocalDate stepInDate,
      boolean payAccOnDefault
  ) {
    return builder()
        .tradeInfo(tradeInfo)
        .product(product)
        .stepInDate(stepInDate)
        .payAccOnDefault(payAccOnDefault)
        .build();
  }

  @Override
  public ExpandedCdsTrade expand() {
    LocalDate tradeDate = getTradeInfo().getTradeDate().get();
    LocalDate stepInDate = getStepInDate();
    LocalDate cashSettleDate = getTradeInfo().getSettlementDate().get();
    BusinessDayConvention businessdayAdjustmentConvention = getProduct().getGeneralTerms().getBusinessDayAdjustment().getConvention();
    HolidayCalendar calendar = getProduct().getGeneralTerms().getBusinessDayAdjustment().getCalendar();
    BusinessDayAdjustment businessDayAdjustment = BusinessDayAdjustment.of(
        businessdayAdjustmentConvention,
        calendar
    );
    LocalDate accStartDate = businessDayAdjustment.adjust(
        getProduct().getGeneralTerms().getStartDate()
    );
    LocalDate endDate = getProduct().getGeneralTerms().getEndDate();
    boolean payAccOnDefault = isPayAccOnDefault();
    Period paymentInterval = getProduct().getFeeLeg().getPeriodicPayments().getPaymentFrequency().getPeriod();
    StubConvention stubConvention = getProduct().getFeeLeg().getPeriodicPayments().getStubConvention();
    DayCount accrualDayCount = getProduct().getFeeLeg().getPeriodicPayments().getFixedAmountCalculation().getDayCountFraction();
    BuySell buySellProtection = getProduct().getGeneralTerms().getBuySellProtection();
    double upfrontFeeAmount = getProduct().getFeeLeg().getUpfrontFee().getFixedAmount().getAmount();
    LocalDate upfrontFeePaymentDate = getProduct().getFeeLeg().getUpfrontFee().getPaymentDate();
    double coupon = getProduct().getFeeLeg().getPeriodicPayments().getFixedAmountCalculation().getFixedRate();
    double notional = getProduct().getFeeLeg().getPeriodicPayments().getFixedAmountCalculation().getCalculationAmount().getAmount();
    Currency currency = getProduct().getFeeLeg().getPeriodicPayments().getFixedAmountCalculation().getCalculationAmount().getCurrency();

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
   * The meta-bean for {@code CdsTrade}.
   * @return the meta-bean, not null
   */
  public static CdsTrade.Meta meta() {
    return CdsTrade.Meta.INSTANCE;
  }

  static {
    JodaBeanUtils.registerMetaBean(CdsTrade.Meta.INSTANCE);
  }

  /**
   * The serialization version id.
   */
  private static final long serialVersionUID = 1L;

  /**
   * Returns a builder used to create an instance of the bean.
   * @return the builder, not null
   */
  public static CdsTrade.Builder builder() {
    return new CdsTrade.Builder();
  }

  private CdsTrade(
      TradeInfo tradeInfo,
      Cds product,
      LocalDate stepInDate,
      boolean payAccOnDefault) {
    JodaBeanUtils.notNull(product, "product");
    JodaBeanUtils.notNull(stepInDate, "stepInDate");
    JodaBeanUtils.notNull(payAccOnDefault, "payAccOnDefault");
    this.tradeInfo = tradeInfo;
    this.product = product;
    this.stepInDate = stepInDate;
    this.payAccOnDefault = payAccOnDefault;
  }

  @Override
  public CdsTrade.Meta metaBean() {
    return CdsTrade.Meta.INSTANCE;
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
   * Gets contains tradeDate:
   * The trade date or 'today', this is the date other times are measured from (i.e. t = 0)
   * <p>
   * contains settleDate:
   * The date that values are PVed to at inception. It is normally today + 3 business days.  Aka cash-settle date.
   * @return the value of the property
   */
  @Override
  public TradeInfo getTradeInfo() {
    return tradeInfo;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the swap product that was agreed when the trade occurred.
   * <p>
   * The product captures the contracted financial details of the trade.
   * @return the value of the property, not null
   */
  @Override
  public Cds getProduct() {
    return product;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets value that the ISDA Standard model needs but that does not occur in the FpML
   * <p>
   * (aka Protection Effective date or assignment date). Date when party assumes ownership.
   * This is usually T+1. This is when protection (and risk) starts in terms of the model.
   * Note, this is sometimes just called the Effective Date, however this can cause
   * confusion with the legal effective date which is T-60 or T-90.
   * @return the value of the property, not null
   */
  public LocalDate getStepInDate() {
    return stepInDate;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets value that the ISDA Standard model needs but that does not occur in the FpML
   * @return the value of the property, not null
   */
  public boolean isPayAccOnDefault() {
    return payAccOnDefault;
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
      CdsTrade other = (CdsTrade) obj;
      return JodaBeanUtils.equal(getTradeInfo(), other.getTradeInfo()) &&
          JodaBeanUtils.equal(getProduct(), other.getProduct()) &&
          JodaBeanUtils.equal(getStepInDate(), other.getStepInDate()) &&
          (isPayAccOnDefault() == other.isPayAccOnDefault());
    }
    return false;
  }

  @Override
  public int hashCode() {
    int hash = getClass().hashCode();
    hash = hash * 31 + JodaBeanUtils.hashCode(getTradeInfo());
    hash = hash * 31 + JodaBeanUtils.hashCode(getProduct());
    hash = hash * 31 + JodaBeanUtils.hashCode(getStepInDate());
    hash = hash * 31 + JodaBeanUtils.hashCode(isPayAccOnDefault());
    return hash;
  }

  @Override
  public String toString() {
    StringBuilder buf = new StringBuilder(160);
    buf.append("CdsTrade{");
    buf.append("tradeInfo").append('=').append(getTradeInfo()).append(',').append(' ');
    buf.append("product").append('=').append(getProduct()).append(',').append(' ');
    buf.append("stepInDate").append('=').append(getStepInDate()).append(',').append(' ');
    buf.append("payAccOnDefault").append('=').append(JodaBeanUtils.toString(isPayAccOnDefault()));
    buf.append('}');
    return buf.toString();
  }

  //-----------------------------------------------------------------------
  /**
   * The meta-bean for {@code CdsTrade}.
   */
  public static final class Meta extends DirectMetaBean {
    /**
     * The singleton instance of the meta-bean.
     */
    static final Meta INSTANCE = new Meta();

    /**
     * The meta-property for the {@code tradeInfo} property.
     */
    private final MetaProperty<TradeInfo> tradeInfo = DirectMetaProperty.ofImmutable(
        this, "tradeInfo", CdsTrade.class, TradeInfo.class);
    /**
     * The meta-property for the {@code product} property.
     */
    private final MetaProperty<Cds> product = DirectMetaProperty.ofImmutable(
        this, "product", CdsTrade.class, Cds.class);
    /**
     * The meta-property for the {@code stepInDate} property.
     */
    private final MetaProperty<LocalDate> stepInDate = DirectMetaProperty.ofImmutable(
        this, "stepInDate", CdsTrade.class, LocalDate.class);
    /**
     * The meta-property for the {@code payAccOnDefault} property.
     */
    private final MetaProperty<Boolean> payAccOnDefault = DirectMetaProperty.ofImmutable(
        this, "payAccOnDefault", CdsTrade.class, Boolean.TYPE);
    /**
     * The meta-properties.
     */
    private final Map<String, MetaProperty<?>> metaPropertyMap$ = new DirectMetaPropertyMap(
        this, null,
        "tradeInfo",
        "product",
        "stepInDate",
        "payAccOnDefault");

    /**
     * Restricted constructor.
     */
    private Meta() {
    }

    @Override
    protected MetaProperty<?> metaPropertyGet(String propertyName) {
      switch (propertyName.hashCode()) {
        case 752580658:  // tradeInfo
          return tradeInfo;
        case -309474065:  // product
          return product;
        case -1890516897:  // stepInDate
          return stepInDate;
        case -988493655:  // payAccOnDefault
          return payAccOnDefault;
      }
      return super.metaPropertyGet(propertyName);
    }

    @Override
    public CdsTrade.Builder builder() {
      return new CdsTrade.Builder();
    }

    @Override
    public Class<? extends CdsTrade> beanType() {
      return CdsTrade.class;
    }

    @Override
    public Map<String, MetaProperty<?>> metaPropertyMap() {
      return metaPropertyMap$;
    }

    //-----------------------------------------------------------------------
    /**
     * The meta-property for the {@code tradeInfo} property.
     * @return the meta-property, not null
     */
    public MetaProperty<TradeInfo> tradeInfo() {
      return tradeInfo;
    }

    /**
     * The meta-property for the {@code product} property.
     * @return the meta-property, not null
     */
    public MetaProperty<Cds> product() {
      return product;
    }

    /**
     * The meta-property for the {@code stepInDate} property.
     * @return the meta-property, not null
     */
    public MetaProperty<LocalDate> stepInDate() {
      return stepInDate;
    }

    /**
     * The meta-property for the {@code payAccOnDefault} property.
     * @return the meta-property, not null
     */
    public MetaProperty<Boolean> payAccOnDefault() {
      return payAccOnDefault;
    }

    //-----------------------------------------------------------------------
    @Override
    protected Object propertyGet(Bean bean, String propertyName, boolean quiet) {
      switch (propertyName.hashCode()) {
        case 752580658:  // tradeInfo
          return ((CdsTrade) bean).getTradeInfo();
        case -309474065:  // product
          return ((CdsTrade) bean).getProduct();
        case -1890516897:  // stepInDate
          return ((CdsTrade) bean).getStepInDate();
        case -988493655:  // payAccOnDefault
          return ((CdsTrade) bean).isPayAccOnDefault();
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
   * The bean-builder for {@code CdsTrade}.
   */
  public static final class Builder extends DirectFieldsBeanBuilder<CdsTrade> {

    private TradeInfo tradeInfo;
    private Cds product;
    private LocalDate stepInDate;
    private boolean payAccOnDefault;

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
    private Builder(CdsTrade beanToCopy) {
      this.tradeInfo = beanToCopy.getTradeInfo();
      this.product = beanToCopy.getProduct();
      this.stepInDate = beanToCopy.getStepInDate();
      this.payAccOnDefault = beanToCopy.isPayAccOnDefault();
    }

    //-----------------------------------------------------------------------
    @Override
    public Object get(String propertyName) {
      switch (propertyName.hashCode()) {
        case 752580658:  // tradeInfo
          return tradeInfo;
        case -309474065:  // product
          return product;
        case -1890516897:  // stepInDate
          return stepInDate;
        case -988493655:  // payAccOnDefault
          return payAccOnDefault;
        default:
          throw new NoSuchElementException("Unknown property: " + propertyName);
      }
    }

    @Override
    public Builder set(String propertyName, Object newValue) {
      switch (propertyName.hashCode()) {
        case 752580658:  // tradeInfo
          this.tradeInfo = (TradeInfo) newValue;
          break;
        case -309474065:  // product
          this.product = (Cds) newValue;
          break;
        case -1890516897:  // stepInDate
          this.stepInDate = (LocalDate) newValue;
          break;
        case -988493655:  // payAccOnDefault
          this.payAccOnDefault = (Boolean) newValue;
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
    public CdsTrade build() {
      return new CdsTrade(
          tradeInfo,
          product,
          stepInDate,
          payAccOnDefault);
    }

    //-----------------------------------------------------------------------
    /**
     * Sets the {@code tradeInfo} property in the builder.
     * @param tradeInfo  the new value
     * @return this, for chaining, not null
     */
    public Builder tradeInfo(TradeInfo tradeInfo) {
      this.tradeInfo = tradeInfo;
      return this;
    }

    /**
     * Sets the {@code product} property in the builder.
     * @param product  the new value, not null
     * @return this, for chaining, not null
     */
    public Builder product(Cds product) {
      JodaBeanUtils.notNull(product, "product");
      this.product = product;
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
     * Sets the {@code payAccOnDefault} property in the builder.
     * @param payAccOnDefault  the new value, not null
     * @return this, for chaining, not null
     */
    public Builder payAccOnDefault(boolean payAccOnDefault) {
      JodaBeanUtils.notNull(payAccOnDefault, "payAccOnDefault");
      this.payAccOnDefault = payAccOnDefault;
      return this;
    }

    //-----------------------------------------------------------------------
    @Override
    public String toString() {
      StringBuilder buf = new StringBuilder(160);
      buf.append("CdsTrade.Builder{");
      buf.append("tradeInfo").append('=').append(JodaBeanUtils.toString(tradeInfo)).append(',').append(' ');
      buf.append("product").append('=').append(JodaBeanUtils.toString(product)).append(',').append(' ');
      buf.append("stepInDate").append('=').append(JodaBeanUtils.toString(stepInDate)).append(',').append(' ');
      buf.append("payAccOnDefault").append('=').append(JodaBeanUtils.toString(payAccOnDefault));
      buf.append('}');
      return buf.toString();
    }

  }

  ///CLOVER:ON
  //-------------------------- AUTOGENERATED END --------------------------
}
