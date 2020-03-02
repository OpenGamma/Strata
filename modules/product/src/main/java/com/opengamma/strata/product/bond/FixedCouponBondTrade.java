/*
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.product.bond;

import java.io.Serializable;
import java.time.LocalDate;
import java.util.Map;
import java.util.NoSuchElementException;

import org.joda.beans.Bean;
import org.joda.beans.ImmutableBean;
import org.joda.beans.JodaBeanUtils;
import org.joda.beans.MetaBean;
import org.joda.beans.MetaProperty;
import org.joda.beans.gen.BeanDefinition;
import org.joda.beans.gen.ImmutableDefaults;
import org.joda.beans.gen.PropertyDefinition;
import org.joda.beans.impl.direct.DirectFieldsBeanBuilder;
import org.joda.beans.impl.direct.DirectMetaBean;
import org.joda.beans.impl.direct.DirectMetaProperty;
import org.joda.beans.impl.direct.DirectMetaPropertyMap;

import com.opengamma.strata.basics.ReferenceData;
import com.opengamma.strata.collect.ArgChecker;
import com.opengamma.strata.product.PortfolioItemInfo;
import com.opengamma.strata.product.PortfolioItemSummary;
import com.opengamma.strata.product.ProductType;
import com.opengamma.strata.product.ResolvableTrade;
import com.opengamma.strata.product.SecuritizedProductTrade;
import com.opengamma.strata.product.TradeInfo;
import com.opengamma.strata.product.common.SummarizerUtils;

/**
 * A trade representing a fixed coupon bond.
 * <p>
 * A trade in an underlying {@link FixedCouponBond}.
 * 
 * <h4>Price</h4>
 * Strata uses <i>decimal prices</i> for bonds in the trade model, pricers and market data.
 * For example, a price of 99.32% is represented in Strata by 0.9932.
 */
@BeanDefinition(constructorScope = "package")
public final class FixedCouponBondTrade
    implements
    SecuritizedProductTrade<FixedCouponBond>, ResolvableTrade<ResolvedFixedCouponBondTrade>, ImmutableBean, Serializable {

  /**
   * The additional trade information, defaulted to an empty instance.
   * <p>
   * This allows additional information to be attached to the trade.
   * Either the trade or settlement date is required when calling {@link FixedCouponBondTrade#resolve(ReferenceData)}.
   */
  @PropertyDefinition(validate = "notNull", overrideGet = true)
  private final TradeInfo info;
  /**
   * The bond that was traded.
   * <p>
   * The product captures the contracted financial details of the trade.
   */
  @PropertyDefinition(validate = "notNull", overrideGet = true)
  private final FixedCouponBond product;
  /**
   * The quantity that was traded.
   * <p>
   * This will be positive if buying and negative if selling.
   */
  @PropertyDefinition(overrideGet = true)
  private final double quantity;
  /**
   * The <i>clean</i> price at which the bond was traded, in decimal form.
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
  public FixedCouponBondTrade withInfo(PortfolioItemInfo info) {
    return new FixedCouponBondTrade(TradeInfo.from(info), product, quantity, price);
  }

  @Override
  public FixedCouponBondTrade withQuantity(double quantity) {
    return new FixedCouponBondTrade(info, product, quantity, price);
  }

  @Override
  public FixedCouponBondTrade withPrice(double price) {
    return new FixedCouponBondTrade(info, product, quantity, price);
  }

  //-------------------------------------------------------------------------
  @Override
  public PortfolioItemSummary summarize() {
    // ID x 200
    String description = getSecurityId().getStandardId().getValue() + " x " + SummarizerUtils.value(getQuantity());
    return SummarizerUtils.summary(this, ProductType.BOND, description, getCurrency());
  }

  @Override
  public ResolvedFixedCouponBondTrade resolve(ReferenceData refData) {
    ResolvedFixedCouponBond resolved = getProduct().resolve(refData);
    LocalDate settlementDate = calculateSettlementDate(refData);
    return ResolvedFixedCouponBondTrade.builder()
        .info(info)
        .product(resolved)
        .quantity(quantity)
        .settlement(ResolvedFixedCouponBondSettlement.of(settlementDate, price))
        .build();
  }

  // calculates the settlement date from the trade date if necessary
  private LocalDate calculateSettlementDate(ReferenceData refData) {
    if (info.getSettlementDate().isPresent()) {
      return info.getSettlementDate().get();
    }
    if (info.getTradeDate().isPresent()) {
      LocalDate tradeDate = info.getTradeDate().get();
      return product.getSettlementDateOffset().adjust(tradeDate, refData);
    }
    throw new IllegalStateException("FixedCouponBondTrade.resolve() requires either trade date or settlement date");
  }

  //------------------------- AUTOGENERATED START -------------------------
  /**
   * The meta-bean for {@code FixedCouponBondTrade}.
   * @return the meta-bean, not null
   */
  public static FixedCouponBondTrade.Meta meta() {
    return FixedCouponBondTrade.Meta.INSTANCE;
  }

  static {
    MetaBean.register(FixedCouponBondTrade.Meta.INSTANCE);
  }

  /**
   * The serialization version id.
   */
  private static final long serialVersionUID = 1L;

  /**
   * Returns a builder used to create an instance of the bean.
   * @return the builder, not null
   */
  public static FixedCouponBondTrade.Builder builder() {
    return new FixedCouponBondTrade.Builder();
  }

  /**
   * Creates an instance.
   * @param info  the value of the property, not null
   * @param product  the value of the property, not null
   * @param quantity  the value of the property
   * @param price  the value of the property
   */
  FixedCouponBondTrade(
      TradeInfo info,
      FixedCouponBond product,
      double quantity,
      double price) {
    JodaBeanUtils.notNull(info, "info");
    JodaBeanUtils.notNull(product, "product");
    ArgChecker.notNegative(price, "price");
    this.info = info;
    this.product = product;
    this.quantity = quantity;
    this.price = price;
  }

  @Override
  public FixedCouponBondTrade.Meta metaBean() {
    return FixedCouponBondTrade.Meta.INSTANCE;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the additional trade information, defaulted to an empty instance.
   * <p>
   * This allows additional information to be attached to the trade.
   * Either the trade or settlement date is required when calling {@link FixedCouponBondTrade#resolve(ReferenceData)}.
   * @return the value of the property, not null
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
  public FixedCouponBond getProduct() {
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
   * Gets the <i>clean</i> price at which the bond was traded, in decimal form.
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
      FixedCouponBondTrade other = (FixedCouponBondTrade) obj;
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
    buf.append("FixedCouponBondTrade{");
    buf.append("info").append('=').append(JodaBeanUtils.toString(info)).append(',').append(' ');
    buf.append("product").append('=').append(JodaBeanUtils.toString(product)).append(',').append(' ');
    buf.append("quantity").append('=').append(JodaBeanUtils.toString(quantity)).append(',').append(' ');
    buf.append("price").append('=').append(JodaBeanUtils.toString(price));
    buf.append('}');
    return buf.toString();
  }

  //-----------------------------------------------------------------------
  /**
   * The meta-bean for {@code FixedCouponBondTrade}.
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
        this, "info", FixedCouponBondTrade.class, TradeInfo.class);
    /**
     * The meta-property for the {@code product} property.
     */
    private final MetaProperty<FixedCouponBond> product = DirectMetaProperty.ofImmutable(
        this, "product", FixedCouponBondTrade.class, FixedCouponBond.class);
    /**
     * The meta-property for the {@code quantity} property.
     */
    private final MetaProperty<Double> quantity = DirectMetaProperty.ofImmutable(
        this, "quantity", FixedCouponBondTrade.class, Double.TYPE);
    /**
     * The meta-property for the {@code price} property.
     */
    private final MetaProperty<Double> price = DirectMetaProperty.ofImmutable(
        this, "price", FixedCouponBondTrade.class, Double.TYPE);
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
    public FixedCouponBondTrade.Builder builder() {
      return new FixedCouponBondTrade.Builder();
    }

    @Override
    public Class<? extends FixedCouponBondTrade> beanType() {
      return FixedCouponBondTrade.class;
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
    public MetaProperty<FixedCouponBond> product() {
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
          return ((FixedCouponBondTrade) bean).getInfo();
        case -309474065:  // product
          return ((FixedCouponBondTrade) bean).getProduct();
        case -1285004149:  // quantity
          return ((FixedCouponBondTrade) bean).getQuantity();
        case 106934601:  // price
          return ((FixedCouponBondTrade) bean).getPrice();
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
   * The bean-builder for {@code FixedCouponBondTrade}.
   */
  public static final class Builder extends DirectFieldsBeanBuilder<FixedCouponBondTrade> {

    private TradeInfo info;
    private FixedCouponBond product;
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
    private Builder(FixedCouponBondTrade beanToCopy) {
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
          this.product = (FixedCouponBond) newValue;
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

    @Override
    public FixedCouponBondTrade build() {
      return new FixedCouponBondTrade(
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
     * Either the trade or settlement date is required when calling {@link FixedCouponBondTrade#resolve(ReferenceData)}.
     * @param info  the new value, not null
     * @return this, for chaining, not null
     */
    public Builder info(TradeInfo info) {
      JodaBeanUtils.notNull(info, "info");
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
    public Builder product(FixedCouponBond product) {
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
     * Sets the <i>clean</i> price at which the bond was traded, in decimal form.
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
      buf.append("FixedCouponBondTrade.Builder{");
      buf.append("info").append('=').append(JodaBeanUtils.toString(info)).append(',').append(' ');
      buf.append("product").append('=').append(JodaBeanUtils.toString(product)).append(',').append(' ');
      buf.append("quantity").append('=').append(JodaBeanUtils.toString(quantity)).append(',').append(' ');
      buf.append("price").append('=').append(JodaBeanUtils.toString(price));
      buf.append('}');
      return buf.toString();
    }

  }

  //-------------------------- AUTOGENERATED END --------------------------
}
