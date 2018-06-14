/*
 * Copyright (C) 2018 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.product.dsf;

import java.io.Serializable;
import java.util.Map;
import java.util.NoSuchElementException;

import org.joda.beans.Bean;
import org.joda.beans.ImmutableBean;
import org.joda.beans.JodaBeanUtils;
import org.joda.beans.MetaBean;
import org.joda.beans.MetaProperty;
import org.joda.beans.gen.BeanDefinition;
import org.joda.beans.gen.DerivedProperty;
import org.joda.beans.gen.ImmutableDefaults;
import org.joda.beans.gen.PropertyDefinition;
import org.joda.beans.impl.direct.DirectFieldsBeanBuilder;
import org.joda.beans.impl.direct.DirectMetaBean;
import org.joda.beans.impl.direct.DirectMetaProperty;
import org.joda.beans.impl.direct.DirectMetaPropertyMap;

import com.opengamma.strata.basics.ReferenceData;
import com.opengamma.strata.basics.Resolvable;
import com.opengamma.strata.basics.currency.Currency;
import com.opengamma.strata.collect.ArgChecker;
import com.opengamma.strata.product.PortfolioItemSummary;
import com.opengamma.strata.product.PositionInfo;
import com.opengamma.strata.product.ProductType;
import com.opengamma.strata.product.SecuritizedProductPosition;
import com.opengamma.strata.product.SecurityId;
import com.opengamma.strata.product.common.SummarizerUtils;

/**
 * A position in a DSF.
 * <p>
 * A position in an underlying {@link Dsf}.
 * <p>
 * The net quantity of the position is stored using two fields - {@code longQuantity} and {@code shortQuantity}.
 * These two fields must not be negative.
 * In many cases, only a long quantity or short quantity will be present with the other set to zero.
 * However it is also possible for both to be non-zero, allowing long and short positions to be treated separately.
 * The net quantity is available via {@link #getQuantity()}.
 * 
 * <h4>Price</h4>
 * Strata uses <i>decimal prices</i> for DSFs in the trade model, pricers and market data.
 * The decimal price is based on the decimal multiplier equivalent to the implied percentage.
 * Thus the market price of 100.182 is represented in Strata by 1.00182.
 */
@BeanDefinition(constructorScope = "package")
public final class DsfPosition
    implements
    SecuritizedProductPosition<Dsf>, Resolvable<ResolvedDsfTrade>, ImmutableBean, Serializable {

  /**
   * The additional position information, defaulted to an empty instance.
   * <p>
   * This allows additional information to be attached to the position.
   */
  @PropertyDefinition(validate = "notNull", overrideGet = true)
  private final PositionInfo info;
  /**
   * The DSF that was traded.
   * <p>
   * The product captures the contracted financial details.
   */
  @PropertyDefinition(validate = "notNull", overrideGet = true)
  private final Dsf product;
  /**
   * The long quantity of the security.
   * <p>
   * This is the quantity of the underlying security that is held.
   * The quantity cannot be negative, as that would imply short selling.
   */
  @PropertyDefinition(validate = "ArgChecker.notNegative")
  private final double longQuantity;
  /**
   * The short quantity of the security.
   * <p>
   * This is the quantity of the underlying security that has been short sold.
   * The quantity cannot be negative, as that would imply the position is long.
   */
  @PropertyDefinition(validate = "ArgChecker.notNegative")
  private final double shortQuantity;

  //-------------------------------------------------------------------------
  /**
   * Obtains an instance from position information, product and net quantity.
   * <p>
   * The net quantity is the long quantity minus the short quantity, which may be negative.
   * If the quantity is positive it is treated as a long quantity.
   * Otherwise it is treated as a short quantity.
   *
   * @param positionInfo  the position information
   * @param product  the underlying product
   * @param netQuantity  the net quantity of the underlying security
   * @return the position
   */
  public static DsfPosition ofNet(PositionInfo positionInfo, Dsf product, double netQuantity) {
    double longQuantity = netQuantity >= 0 ? netQuantity : 0;
    double shortQuantity = netQuantity >= 0 ? 0 : -netQuantity;
    return new DsfPosition(positionInfo, product, longQuantity, shortQuantity);
  }

  /**
   * Obtains an instance from position information, product, long quantity and short quantity.
   * <p>
   * The long quantity and short quantity must be zero or positive, not negative.
   * In many cases, only a long quantity or short quantity will be present with the other set to zero.
   * However it is also possible for both to be non-zero, allowing long and short positions to be treated separately.
   *
   * @param positionInfo  the position information
   * @param product  the underlying product
   * @param longQuantity  the long quantity of the underlying security
   * @param shortQuantity  the short quantity of the underlying security
   * @return the position
   */
  public static DsfPosition ofLongShort(
      PositionInfo positionInfo,
      Dsf product,
      double longQuantity,
      double shortQuantity) {

    return new DsfPosition(positionInfo, product, longQuantity, shortQuantity);
  }

  @ImmutableDefaults
  private static void applyDefaults(Builder builder) {
    builder.info = PositionInfo.empty();
  }

  //-------------------------------------------------------------------------
  @Override
  public SecurityId getSecurityId() {
    return product.getSecurityId();
  }

  @Override
  public Currency getCurrency() {
    return product.getCurrency();
  }

  @Override
  @DerivedProperty
  public double getQuantity() {
    return longQuantity - shortQuantity;
  }

  //-------------------------------------------------------------------------
  @Override
  public DsfPosition withInfo(PositionInfo info) {
    return new DsfPosition(info, product, longQuantity, shortQuantity);
  }

  @Override
  public DsfPosition withQuantity(double quantity) {
    return DsfPosition.ofNet(info, product, quantity);
  }

  //-------------------------------------------------------------------------
  @Override
  public PortfolioItemSummary summarize() {
    // ID x 200
    String description = getSecurityId().getStandardId().getValue() + " x " + SummarizerUtils.value(getQuantity());
    return SummarizerUtils.summary(this, ProductType.DSF, description, getCurrency());
  }

  @Override
  public ResolvedDsfTrade resolve(ReferenceData refData) {
    ResolvedDsf resolved = product.resolve(refData);
    return new ResolvedDsfTrade(info, resolved, getQuantity(), null);
  }

  //------------------------- AUTOGENERATED START -------------------------
  /**
   * The meta-bean for {@code DsfPosition}.
   * @return the meta-bean, not null
   */
  public static DsfPosition.Meta meta() {
    return DsfPosition.Meta.INSTANCE;
  }

  static {
    MetaBean.register(DsfPosition.Meta.INSTANCE);
  }

  /**
   * The serialization version id.
   */
  private static final long serialVersionUID = 1L;

  /**
   * Returns a builder used to create an instance of the bean.
   * @return the builder, not null
   */
  public static DsfPosition.Builder builder() {
    return new DsfPosition.Builder();
  }

  /**
   * Creates an instance.
   * @param info  the value of the property, not null
   * @param product  the value of the property, not null
   * @param longQuantity  the value of the property
   * @param shortQuantity  the value of the property
   */
  DsfPosition(
      PositionInfo info,
      Dsf product,
      double longQuantity,
      double shortQuantity) {
    JodaBeanUtils.notNull(info, "info");
    JodaBeanUtils.notNull(product, "product");
    ArgChecker.notNegative(longQuantity, "longQuantity");
    ArgChecker.notNegative(shortQuantity, "shortQuantity");
    this.info = info;
    this.product = product;
    this.longQuantity = longQuantity;
    this.shortQuantity = shortQuantity;
  }

  @Override
  public DsfPosition.Meta metaBean() {
    return DsfPosition.Meta.INSTANCE;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the additional position information, defaulted to an empty instance.
   * <p>
   * This allows additional information to be attached to the position.
   * @return the value of the property, not null
   */
  @Override
  public PositionInfo getInfo() {
    return info;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the DSF that was traded.
   * <p>
   * The product captures the contracted financial details.
   * @return the value of the property, not null
   */
  @Override
  public Dsf getProduct() {
    return product;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the long quantity of the security.
   * <p>
   * This is the quantity of the underlying security that is held.
   * The quantity cannot be negative, as that would imply short selling.
   * @return the value of the property
   */
  public double getLongQuantity() {
    return longQuantity;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the short quantity of the security.
   * <p>
   * This is the quantity of the underlying security that has been short sold.
   * The quantity cannot be negative, as that would imply the position is long.
   * @return the value of the property
   */
  public double getShortQuantity() {
    return shortQuantity;
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
      DsfPosition other = (DsfPosition) obj;
      return JodaBeanUtils.equal(info, other.info) &&
          JodaBeanUtils.equal(product, other.product) &&
          JodaBeanUtils.equal(longQuantity, other.longQuantity) &&
          JodaBeanUtils.equal(shortQuantity, other.shortQuantity);
    }
    return false;
  }

  @Override
  public int hashCode() {
    int hash = getClass().hashCode();
    hash = hash * 31 + JodaBeanUtils.hashCode(info);
    hash = hash * 31 + JodaBeanUtils.hashCode(product);
    hash = hash * 31 + JodaBeanUtils.hashCode(longQuantity);
    hash = hash * 31 + JodaBeanUtils.hashCode(shortQuantity);
    return hash;
  }

  @Override
  public String toString() {
    StringBuilder buf = new StringBuilder(160);
    buf.append("DsfPosition{");
    buf.append("info").append('=').append(info).append(',').append(' ');
    buf.append("product").append('=').append(product).append(',').append(' ');
    buf.append("longQuantity").append('=').append(longQuantity).append(',').append(' ');
    buf.append("shortQuantity").append('=').append(JodaBeanUtils.toString(shortQuantity));
    buf.append('}');
    return buf.toString();
  }

  //-----------------------------------------------------------------------
  /**
   * The meta-bean for {@code DsfPosition}.
   */
  public static final class Meta extends DirectMetaBean {
    /**
     * The singleton instance of the meta-bean.
     */
    static final Meta INSTANCE = new Meta();

    /**
     * The meta-property for the {@code info} property.
     */
    private final MetaProperty<PositionInfo> info = DirectMetaProperty.ofImmutable(
        this, "info", DsfPosition.class, PositionInfo.class);
    /**
     * The meta-property for the {@code product} property.
     */
    private final MetaProperty<Dsf> product = DirectMetaProperty.ofImmutable(
        this, "product", DsfPosition.class, Dsf.class);
    /**
     * The meta-property for the {@code longQuantity} property.
     */
    private final MetaProperty<Double> longQuantity = DirectMetaProperty.ofImmutable(
        this, "longQuantity", DsfPosition.class, Double.TYPE);
    /**
     * The meta-property for the {@code shortQuantity} property.
     */
    private final MetaProperty<Double> shortQuantity = DirectMetaProperty.ofImmutable(
        this, "shortQuantity", DsfPosition.class, Double.TYPE);
    /**
     * The meta-property for the {@code quantity} property.
     */
    private final MetaProperty<Double> quantity = DirectMetaProperty.ofDerived(
        this, "quantity", DsfPosition.class, Double.TYPE);
    /**
     * The meta-properties.
     */
    private final Map<String, MetaProperty<?>> metaPropertyMap$ = new DirectMetaPropertyMap(
        this, null,
        "info",
        "product",
        "longQuantity",
        "shortQuantity",
        "quantity");

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
        case 611668775:  // longQuantity
          return longQuantity;
        case -2094395097:  // shortQuantity
          return shortQuantity;
        case -1285004149:  // quantity
          return quantity;
      }
      return super.metaPropertyGet(propertyName);
    }

    @Override
    public DsfPosition.Builder builder() {
      return new DsfPosition.Builder();
    }

    @Override
    public Class<? extends DsfPosition> beanType() {
      return DsfPosition.class;
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
    public MetaProperty<PositionInfo> info() {
      return info;
    }

    /**
     * The meta-property for the {@code product} property.
     * @return the meta-property, not null
     */
    public MetaProperty<Dsf> product() {
      return product;
    }

    /**
     * The meta-property for the {@code longQuantity} property.
     * @return the meta-property, not null
     */
    public MetaProperty<Double> longQuantity() {
      return longQuantity;
    }

    /**
     * The meta-property for the {@code shortQuantity} property.
     * @return the meta-property, not null
     */
    public MetaProperty<Double> shortQuantity() {
      return shortQuantity;
    }

    /**
     * The meta-property for the {@code quantity} property.
     * @return the meta-property, not null
     */
    public MetaProperty<Double> quantity() {
      return quantity;
    }

    //-----------------------------------------------------------------------
    @Override
    protected Object propertyGet(Bean bean, String propertyName, boolean quiet) {
      switch (propertyName.hashCode()) {
        case 3237038:  // info
          return ((DsfPosition) bean).getInfo();
        case -309474065:  // product
          return ((DsfPosition) bean).getProduct();
        case 611668775:  // longQuantity
          return ((DsfPosition) bean).getLongQuantity();
        case -2094395097:  // shortQuantity
          return ((DsfPosition) bean).getShortQuantity();
        case -1285004149:  // quantity
          return ((DsfPosition) bean).getQuantity();
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
   * The bean-builder for {@code DsfPosition}.
   */
  public static final class Builder extends DirectFieldsBeanBuilder<DsfPosition> {

    private PositionInfo info;
    private Dsf product;
    private double longQuantity;
    private double shortQuantity;

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
    private Builder(DsfPosition beanToCopy) {
      this.info = beanToCopy.getInfo();
      this.product = beanToCopy.getProduct();
      this.longQuantity = beanToCopy.getLongQuantity();
      this.shortQuantity = beanToCopy.getShortQuantity();
    }

    //-----------------------------------------------------------------------
    @Override
    public Object get(String propertyName) {
      switch (propertyName.hashCode()) {
        case 3237038:  // info
          return info;
        case -309474065:  // product
          return product;
        case 611668775:  // longQuantity
          return longQuantity;
        case -2094395097:  // shortQuantity
          return shortQuantity;
        default:
          throw new NoSuchElementException("Unknown property: " + propertyName);
      }
    }

    @Override
    public Builder set(String propertyName, Object newValue) {
      switch (propertyName.hashCode()) {
        case 3237038:  // info
          this.info = (PositionInfo) newValue;
          break;
        case -309474065:  // product
          this.product = (Dsf) newValue;
          break;
        case 611668775:  // longQuantity
          this.longQuantity = (Double) newValue;
          break;
        case -2094395097:  // shortQuantity
          this.shortQuantity = (Double) newValue;
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
    public DsfPosition build() {
      return new DsfPosition(
          info,
          product,
          longQuantity,
          shortQuantity);
    }

    //-----------------------------------------------------------------------
    /**
     * Sets the additional position information, defaulted to an empty instance.
     * <p>
     * This allows additional information to be attached to the position.
     * @param info  the new value, not null
     * @return this, for chaining, not null
     */
    public Builder info(PositionInfo info) {
      JodaBeanUtils.notNull(info, "info");
      this.info = info;
      return this;
    }

    /**
     * Sets the DSF that was traded.
     * <p>
     * The product captures the contracted financial details.
     * @param product  the new value, not null
     * @return this, for chaining, not null
     */
    public Builder product(Dsf product) {
      JodaBeanUtils.notNull(product, "product");
      this.product = product;
      return this;
    }

    /**
     * Sets the long quantity of the security.
     * <p>
     * This is the quantity of the underlying security that is held.
     * The quantity cannot be negative, as that would imply short selling.
     * @param longQuantity  the new value
     * @return this, for chaining, not null
     */
    public Builder longQuantity(double longQuantity) {
      ArgChecker.notNegative(longQuantity, "longQuantity");
      this.longQuantity = longQuantity;
      return this;
    }

    /**
     * Sets the short quantity of the security.
     * <p>
     * This is the quantity of the underlying security that has been short sold.
     * The quantity cannot be negative, as that would imply the position is long.
     * @param shortQuantity  the new value
     * @return this, for chaining, not null
     */
    public Builder shortQuantity(double shortQuantity) {
      ArgChecker.notNegative(shortQuantity, "shortQuantity");
      this.shortQuantity = shortQuantity;
      return this;
    }

    //-----------------------------------------------------------------------
    @Override
    public String toString() {
      StringBuilder buf = new StringBuilder(160);
      buf.append("DsfPosition.Builder{");
      buf.append("info").append('=').append(JodaBeanUtils.toString(info)).append(',').append(' ');
      buf.append("product").append('=').append(JodaBeanUtils.toString(product)).append(',').append(' ');
      buf.append("longQuantity").append('=').append(JodaBeanUtils.toString(longQuantity)).append(',').append(' ');
      buf.append("shortQuantity").append('=').append(JodaBeanUtils.toString(shortQuantity));
      buf.append('}');
      return buf.toString();
    }

  }

  //-------------------------- AUTOGENERATED END --------------------------
}
