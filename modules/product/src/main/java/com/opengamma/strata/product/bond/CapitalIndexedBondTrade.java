/*
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.product.bond;

import java.io.Serializable;
import java.time.LocalDate;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;

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

import com.opengamma.strata.basics.ReferenceData;
import com.opengamma.strata.basics.currency.Payment;
import com.opengamma.strata.basics.schedule.SchedulePeriod;
import com.opengamma.strata.collect.ArgChecker;
import com.opengamma.strata.product.ResolvableTrade;
import com.opengamma.strata.product.SecuritizedProductTrade;
import com.opengamma.strata.product.TradeInfo;
import com.opengamma.strata.product.rate.RateComputation;

/**
 * A trade representing a capital indexed bond.
 * <p>
 * A trade in an underlying {@link CapitalIndexedBond}.
 * 
 * <h4>Price</h4>
 * Strata uses <i>decimal prices</i> for bonds in the trade model, pricers and market data.
 * For example, a price of 99.32% is represented in Strata by 0.9932.
 */
@BeanDefinition(constructorScope = "package")
public final class CapitalIndexedBondTrade
    implements SecuritizedProductTrade, ResolvableTrade<ResolvedCapitalIndexedBondTrade>, ImmutableBean, Serializable {

  /**
   * The additional trade information, defaulted to an empty instance.
   * <p>
   * This allows additional information to be attached to the trade.
   * Either the trade or settlement date is required when calling {@link CapitalIndexedBondTrade#resolve(ReferenceData)}.
   */
  @PropertyDefinition(overrideGet = true)
  private final TradeInfo info;
  /**
   * The bond that was traded.
   * <p>
   * The product captures the contracted financial details of the trade.
   */
  @PropertyDefinition(validate = "notNull", overrideGet = true)
  private final CapitalIndexedBond product;
  /**
   * The quantity that was traded.
   * <p>
   * This will be positive if buying and negative if selling.
   */
  @PropertyDefinition(overrideGet = true)
  private final double quantity;
  /**
   * The <i>clean</i> price at which the bond was traded.
   * <p>
   * The "clean" price excludes any accrued interest.
   * <p>
   * Strata uses <i>decimal prices</i> for bonds in the trade model, pricers and market data.
   * For example, a price of 99.32% is represented in Strata by 0.9932.
   */
  @PropertyDefinition(validate = "ArgChecker.notNegative", overrideGet = true)
  private final double price;

  //-------------------------------------------------------------------------
  @ImmutableDefaults
  private static void applyDefaults(Builder builder) {
    builder.info = TradeInfo.empty();
  }

  //-------------------------------------------------------------------------
  @Override
  public ResolvedCapitalIndexedBondTrade resolve(ReferenceData refData) {
    ResolvedCapitalIndexedBond resolvedProduct = product.resolve(refData);
    TradeInfo completedInfo = calculateSettlementDate(refData);
    LocalDate settlementDate = completedInfo.getSettlementDate().get();

    double accruedInterest = resolvedProduct.accruedInterest(settlementDate) / product.getNotional();
    if (settlementDate.isBefore(resolvedProduct.getStartDate())) {
      throw new IllegalArgumentException("Settlement date must not be before bond starts");
    }
    BondPaymentPeriod settlement;
    if (product.getYieldConvention().equals(CapitalIndexedBondYieldConvention.GB_IL_FLOAT)) {
      settlement = KnownAmountBondPaymentPeriod.of(
          Payment.of(product.getCurrency(),
              -product.getNotional() * quantity * (price + accruedInterest), settlementDate),
          SchedulePeriod.of(
              resolvedProduct.getStartDate(),
              settlementDate,
              product.getAccrualSchedule().getStartDate(),
              settlementDate));
    } else {
      RateComputation rateComputation = product.getRateCalculation().createRateComputation(settlementDate);
      settlement = CapitalIndexedBondPaymentPeriod.builder()
          .startDate(resolvedProduct.getStartDate())
          .unadjustedStartDate(product.getAccrualSchedule().getStartDate())
          .endDate(settlementDate)
          .rateComputation(rateComputation)
          .currency(product.getCurrency())
          .notional(-product.getNotional() * quantity * (price + accruedInterest))
          .realCoupon(1d)
          .build();
    }

    return ResolvedCapitalIndexedBondTrade.builder()
        .info(completedInfo)
        .product(resolvedProduct)
        .quantity(quantity)
        .price(price)
        .settlement(settlement)
        .build();
  }

  // calculates the settlement date from the trade date if necessary
  private TradeInfo calculateSettlementDate(ReferenceData refData) {
    if (info.getSettlementDate().isPresent()) {
      return info;
    }
    if (info.getTradeDate().isPresent()) {
      LocalDate tradeDate = info.getTradeDate().get();
      LocalDate settlementDate = product.getSettlementDateOffset().adjust(tradeDate, refData);
      return info.toBuilder().settlementDate(settlementDate).build();
    }
    throw new IllegalStateException("CapitalIndexedBondTrade.resolve() requires either trade date or settlement date");
  }

  //------------------------- AUTOGENERATED START -------------------------
  ///CLOVER:OFF
  /**
   * The meta-bean for {@code CapitalIndexedBondTrade}.
   * @return the meta-bean, not null
   */
  public static CapitalIndexedBondTrade.Meta meta() {
    return CapitalIndexedBondTrade.Meta.INSTANCE;
  }

  static {
    JodaBeanUtils.registerMetaBean(CapitalIndexedBondTrade.Meta.INSTANCE);
  }

  /**
   * The serialization version id.
   */
  private static final long serialVersionUID = 1L;

  /**
   * Returns a builder used to create an instance of the bean.
   * @return the builder, not null
   */
  public static CapitalIndexedBondTrade.Builder builder() {
    return new CapitalIndexedBondTrade.Builder();
  }

  /**
   * Creates an instance.
   * @param info  the value of the property
   * @param product  the value of the property, not null
   * @param quantity  the value of the property
   * @param price  the value of the property
   */
  CapitalIndexedBondTrade(
      TradeInfo info,
      CapitalIndexedBond product,
      double quantity,
      double price) {
    JodaBeanUtils.notNull(product, "product");
    ArgChecker.notNegative(price, "price");
    this.info = info;
    this.product = product;
    this.quantity = quantity;
    this.price = price;
  }

  @Override
  public CapitalIndexedBondTrade.Meta metaBean() {
    return CapitalIndexedBondTrade.Meta.INSTANCE;
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
   * Gets the additional trade information, defaulted to an empty instance.
   * <p>
   * This allows additional information to be attached to the trade.
   * Either the trade or settlement date is required when calling {@link CapitalIndexedBondTrade#resolve(ReferenceData)}.
   * @return the value of the property
   */
  @Override
  public TradeInfo getInfo() {
    return info;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the bond that was traded.
   * <p>
   * The product captures the contracted financial details of the trade.
   * @return the value of the property, not null
   */
  @Override
  public CapitalIndexedBond getProduct() {
    return product;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the quantity that was traded.
   * <p>
   * This will be positive if buying and negative if selling.
   * @return the value of the property
   */
  @Override
  public double getQuantity() {
    return quantity;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the <i>clean</i> price at which the bond was traded.
   * <p>
   * The "clean" price excludes any accrued interest.
   * <p>
   * Strata uses <i>decimal prices</i> for bonds in the trade model, pricers and market data.
   * For example, a price of 99.32% is represented in Strata by 0.9932.
   * @return the value of the property
   */
  @Override
  public double getPrice() {
    return price;
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
      CapitalIndexedBondTrade other = (CapitalIndexedBondTrade) obj;
      return JodaBeanUtils.equal(info, other.info) &&
          JodaBeanUtils.equal(product, other.product) &&
          JodaBeanUtils.equal(quantity, other.quantity) &&
          JodaBeanUtils.equal(price, other.price);
    }
    return false;
  }

  @Override
  public int hashCode() {
    int hash = getClass().hashCode();
    hash = hash * 31 + JodaBeanUtils.hashCode(info);
    hash = hash * 31 + JodaBeanUtils.hashCode(product);
    hash = hash * 31 + JodaBeanUtils.hashCode(quantity);
    hash = hash * 31 + JodaBeanUtils.hashCode(price);
    return hash;
  }

  @Override
  public String toString() {
    StringBuilder buf = new StringBuilder(160);
    buf.append("CapitalIndexedBondTrade{");
    buf.append("info").append('=').append(info).append(',').append(' ');
    buf.append("product").append('=').append(product).append(',').append(' ');
    buf.append("quantity").append('=').append(quantity).append(',').append(' ');
    buf.append("price").append('=').append(JodaBeanUtils.toString(price));
    buf.append('}');
    return buf.toString();
  }

  //-----------------------------------------------------------------------
  /**
   * The meta-bean for {@code CapitalIndexedBondTrade}.
   */
  public static final class Meta extends DirectMetaBean {
    /**
     * The singleton instance of the meta-bean.
     */
    static final Meta INSTANCE = new Meta();

    /**
     * The meta-property for the {@code info} property.
     */
    private final MetaProperty<TradeInfo> info = DirectMetaProperty.ofImmutable(
        this, "info", CapitalIndexedBondTrade.class, TradeInfo.class);
    /**
     * The meta-property for the {@code product} property.
     */
    private final MetaProperty<CapitalIndexedBond> product = DirectMetaProperty.ofImmutable(
        this, "product", CapitalIndexedBondTrade.class, CapitalIndexedBond.class);
    /**
     * The meta-property for the {@code quantity} property.
     */
    private final MetaProperty<Double> quantity = DirectMetaProperty.ofImmutable(
        this, "quantity", CapitalIndexedBondTrade.class, Double.TYPE);
    /**
     * The meta-property for the {@code price} property.
     */
    private final MetaProperty<Double> price = DirectMetaProperty.ofImmutable(
        this, "price", CapitalIndexedBondTrade.class, Double.TYPE);
    /**
     * The meta-properties.
     */
    private final Map<String, MetaProperty<?>> metaPropertyMap$ = new DirectMetaPropertyMap(
        this, null,
        "info",
        "product",
        "quantity",
        "price");

    /**
     * Restricted constructor.
     */
    private Meta() {
    }

    @Override
    protected MetaProperty<?> metaPropertyGet(String propertyName) {
      switch (propertyName.hashCode()) {
        case 3237038:  // info
          return info;
        case -309474065:  // product
          return product;
        case -1285004149:  // quantity
          return quantity;
        case 106934601:  // price
          return price;
      }
      return super.metaPropertyGet(propertyName);
    }

    @Override
    public CapitalIndexedBondTrade.Builder builder() {
      return new CapitalIndexedBondTrade.Builder();
    }

    @Override
    public Class<? extends CapitalIndexedBondTrade> beanType() {
      return CapitalIndexedBondTrade.class;
    }

    @Override
    public Map<String, MetaProperty<?>> metaPropertyMap() {
      return metaPropertyMap$;
    }

    //-----------------------------------------------------------------------
    /**
     * The meta-property for the {@code info} property.
     * @return the meta-property, not null
     */
    public MetaProperty<TradeInfo> info() {
      return info;
    }

    /**
     * The meta-property for the {@code product} property.
     * @return the meta-property, not null
     */
    public MetaProperty<CapitalIndexedBond> product() {
      return product;
    }

    /**
     * The meta-property for the {@code quantity} property.
     * @return the meta-property, not null
     */
    public MetaProperty<Double> quantity() {
      return quantity;
    }

    /**
     * The meta-property for the {@code price} property.
     * @return the meta-property, not null
     */
    public MetaProperty<Double> price() {
      return price;
    }

    //-----------------------------------------------------------------------
    @Override
    protected Object propertyGet(Bean bean, String propertyName, boolean quiet) {
      switch (propertyName.hashCode()) {
        case 3237038:  // info
          return ((CapitalIndexedBondTrade) bean).getInfo();
        case -309474065:  // product
          return ((CapitalIndexedBondTrade) bean).getProduct();
        case -1285004149:  // quantity
          return ((CapitalIndexedBondTrade) bean).getQuantity();
        case 106934601:  // price
          return ((CapitalIndexedBondTrade) bean).getPrice();
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
   * The bean-builder for {@code CapitalIndexedBondTrade}.
   */
  public static final class Builder extends DirectFieldsBeanBuilder<CapitalIndexedBondTrade> {

    private TradeInfo info;
    private CapitalIndexedBond product;
    private double quantity;
    private double price;

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
    private Builder(CapitalIndexedBondTrade beanToCopy) {
      this.info = beanToCopy.getInfo();
      this.product = beanToCopy.getProduct();
      this.quantity = beanToCopy.getQuantity();
      this.price = beanToCopy.getPrice();
    }

    //-----------------------------------------------------------------------
    @Override
    public Object get(String propertyName) {
      switch (propertyName.hashCode()) {
        case 3237038:  // info
          return info;
        case -309474065:  // product
          return product;
        case -1285004149:  // quantity
          return quantity;
        case 106934601:  // price
          return price;
        default:
          throw new NoSuchElementException("Unknown property: " + propertyName);
      }
    }

    @Override
    public Builder set(String propertyName, Object newValue) {
      switch (propertyName.hashCode()) {
        case 3237038:  // info
          this.info = (TradeInfo) newValue;
          break;
        case -309474065:  // product
          this.product = (CapitalIndexedBond) newValue;
          break;
        case -1285004149:  // quantity
          this.quantity = (Double) newValue;
          break;
        case 106934601:  // price
          this.price = (Double) newValue;
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

    /**
     * @deprecated Use Joda-Convert in application code
     */
    @Override
    @Deprecated
    public Builder setString(String propertyName, String value) {
      setString(meta().metaProperty(propertyName), value);
      return this;
    }

    /**
     * @deprecated Use Joda-Convert in application code
     */
    @Override
    @Deprecated
    public Builder setString(MetaProperty<?> property, String value) {
      super.setString(property, value);
      return this;
    }

    /**
     * @deprecated Loop in application code
     */
    @Override
    @Deprecated
    public Builder setAll(Map<String, ? extends Object> propertyValueMap) {
      super.setAll(propertyValueMap);
      return this;
    }

    @Override
    public CapitalIndexedBondTrade build() {
      return new CapitalIndexedBondTrade(
          info,
          product,
          quantity,
          price);
    }

    //-----------------------------------------------------------------------
    /**
     * Sets the additional trade information, defaulted to an empty instance.
     * <p>
     * This allows additional information to be attached to the trade.
     * Either the trade or settlement date is required when calling {@link CapitalIndexedBondTrade#resolve(ReferenceData)}.
     * @param info  the new value
     * @return this, for chaining, not null
     */
    public Builder info(TradeInfo info) {
      this.info = info;
      return this;
    }

    /**
     * Sets the bond that was traded.
     * <p>
     * The product captures the contracted financial details of the trade.
     * @param product  the new value, not null
     * @return this, for chaining, not null
     */
    public Builder product(CapitalIndexedBond product) {
      JodaBeanUtils.notNull(product, "product");
      this.product = product;
      return this;
    }

    /**
     * Sets the quantity that was traded.
     * <p>
     * This will be positive if buying and negative if selling.
     * @param quantity  the new value
     * @return this, for chaining, not null
     */
    public Builder quantity(double quantity) {
      this.quantity = quantity;
      return this;
    }

    /**
     * Sets the <i>clean</i> price at which the bond was traded.
     * <p>
     * The "clean" price excludes any accrued interest.
     * <p>
     * Strata uses <i>decimal prices</i> for bonds in the trade model, pricers and market data.
     * For example, a price of 99.32% is represented in Strata by 0.9932.
     * @param price  the new value
     * @return this, for chaining, not null
     */
    public Builder price(double price) {
      ArgChecker.notNegative(price, "price");
      this.price = price;
      return this;
    }

    //-----------------------------------------------------------------------
    @Override
    public String toString() {
      StringBuilder buf = new StringBuilder(160);
      buf.append("CapitalIndexedBondTrade.Builder{");
      buf.append("info").append('=').append(JodaBeanUtils.toString(info)).append(',').append(' ');
      buf.append("product").append('=').append(JodaBeanUtils.toString(product)).append(',').append(' ');
      buf.append("quantity").append('=').append(JodaBeanUtils.toString(quantity)).append(',').append(' ');
      buf.append("price").append('=').append(JodaBeanUtils.toString(price));
      buf.append('}');
      return buf.toString();
    }

  }

  ///CLOVER:ON
  //-------------------------- AUTOGENERATED END --------------------------
}
