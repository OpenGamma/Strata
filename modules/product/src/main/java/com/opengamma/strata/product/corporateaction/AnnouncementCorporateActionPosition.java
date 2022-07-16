/*
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.product.corporateaction;

import com.opengamma.strata.basics.ReferenceData;
import com.opengamma.strata.basics.currency.Currency;
import com.opengamma.strata.collect.ArgChecker;
import com.opengamma.strata.product.*;
import com.opengamma.strata.product.common.SummarizerUtils;
import org.joda.beans.*;
import org.joda.beans.gen.BeanDefinition;
import org.joda.beans.gen.DerivedProperty;
import org.joda.beans.gen.ImmutableDefaults;
import org.joda.beans.gen.PropertyDefinition;
import org.joda.beans.impl.direct.DirectFieldsBeanBuilder;
import org.joda.beans.impl.direct.DirectMetaBean;
import org.joda.beans.impl.direct.DirectMetaProperty;
import org.joda.beans.impl.direct.DirectMetaPropertyMap;

import java.io.Serializable;
import java.util.Map;
import java.util.NoSuchElementException;
import org.joda.beans.Bean;
import org.joda.beans.JodaBeanUtils;
import org.joda.beans.MetaBean;
import org.joda.beans.MetaProperty;

/**
 * A position in a security, where the security is embedded ready for mark-to-market pricing.
 * <p>
 * This represents a position in a security, defined by long and short quantity.
 * The security is embedded directly, however the underlying product model is not available.
 * The security may be of any kind, including equities, bonds and exchange traded derivatives (ETD).
 * <p>
 * The net quantity of the position is stored using two fields - {@code longQuantity} and {@code shortQuantity}.
 * These two fields must not be negative.
 * In many cases, only a long quantity or short quantity will be present with the other set to zero.
 * However it is also possible for both to be non-zero, allowing long and short positions to be treated separately.
 * The net quantity is available via {@link #getQuantity()}.
 */
@BeanDefinition(constructorScope = "package")
public final class AnnouncementCorporateActionPosition
    implements SecuritizedProductPosition<AnnouncementCorporateAction>, ResolvableSecurityPosition, ImmutableBean, Serializable{

  /**
   * The additional position information, defaulted to an empty instance.
   * <p>
   * This allows additional information to be attached to the position.
   */
  @PropertyDefinition(validate = "notNull", overrideGet = true)
  private final PositionInfo info;
  /**
   * The underlying security.
   */
  @PropertyDefinition(validate = "notNull", overrideGet = true)
  private final AnnouncementCorporateAction product;
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
   * Obtains an instance from the security and net quantity.
   * <p>
   * The net quantity is the long quantity minus the short quantity, which may be negative.
   * If the quantity is positive it is treated as a long quantity.
   * Otherwise it is treated as a short quantity.
   *
   * @param security  the underlying security
   * @param netQuantity  the net quantity of the underlying security
   * @return the position
   */
  public static AnnouncementCorporateActionPosition ofNet(AnnouncementCorporateAction announcementCorporateAction, double netQuantity) {
    return ofNet(PositionInfo.empty(), announcementCorporateAction, netQuantity);
  }

  /**
   * Obtains an instance from position information, security and net quantity.
   * <p>
   * The net quantity is the long quantity minus the short quantity, which may be negative.
   * If the quantity is positive it is treated as a long quantity.
   * Otherwise it is treated as a short quantity.
   *
   * @param positionInfo  the position information
   * @param security  the underlying security
   * @param netQuantity  the net quantity of the underlying security
   * @return the position
   */
  public static AnnouncementCorporateActionPosition ofNet(PositionInfo positionInfo, AnnouncementCorporateAction announcementCorporateAction, double netQuantity) {
    double longQuantity = netQuantity >= 0 ? netQuantity : 0;
    double shortQuantity = netQuantity >= 0 ? 0 : -netQuantity;
    return new AnnouncementCorporateActionPosition(positionInfo, announcementCorporateAction, longQuantity, shortQuantity);
  }

  /**
   * Obtains an instance from the security, long quantity and short quantity.
   * <p>
   * The long quantity and short quantity must be zero or positive, not negative.
   * In many cases, only a long quantity or short quantity will be present with the other set to zero.
   * However it is also possible for both to be non-zero, allowing long and short positions to be treated separately.
   *
   * @param security  the underlying security
   * @param longQuantity  the long quantity of the underlying security
   * @param shortQuantity  the short quantity of the underlying security
   * @return the position
   */
  public static AnnouncementCorporateActionPosition ofLongShort(AnnouncementCorporateAction announcementCorporateAction, double longQuantity, double shortQuantity) {
    return ofLongShort(PositionInfo.empty(), announcementCorporateAction, longQuantity, shortQuantity);
  }

  /**
   * Obtains an instance from position information, security, long quantity and short quantity.
   * <p>
   * The long quantity and short quantity must be zero or positive, not negative.
   * In many cases, only a long quantity or short quantity will be present with the other set to zero.
   * However it is also possible for both to be non-zero, allowing long and short positions to be treated separately.
   *
   * @param positionInfo  the position information
   * @param security  the underlying security
   * @param longQuantity  the long quantity of the underlying security
   * @param shortQuantity  the short quantity of the underlying security
   * @return the position
   */
  public static AnnouncementCorporateActionPosition ofLongShort(
      PositionInfo positionInfo,
      AnnouncementCorporateAction announcementCorporateAction,
      double longQuantity,
      double shortQuantity) {

    return new AnnouncementCorporateActionPosition(positionInfo, announcementCorporateAction, longQuantity, shortQuantity);
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

  @Override public SecuritizedProductPosition<?> resolveTarget(ReferenceData refData) {
    return this;
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
  public AnnouncementCorporateActionPosition withInfo(PortfolioItemInfo info) {
    return new AnnouncementCorporateActionPosition(PositionInfo.from(info), product, longQuantity, shortQuantity);
  }

  @Override
  public AnnouncementCorporateActionPosition withQuantity(double quantity) {
    return AnnouncementCorporateActionPosition.ofNet(info, product, quantity);
  }

  //-------------------------------------------------------------------------
  @Override
  public PortfolioItemSummary summarize() {
    // ID x 200 //DPDPDP
    String description = getSecurityId().getStandardId().getValue() + " x " + SummarizerUtils.value(getQuantity());
    return SummarizerUtils.summary(this, ProductType.SECURITY, description, getCurrency());
  }

  //------------------------- AUTOGENERATED START -------------------------
  /**
   * The meta-bean for {@code AnnouncementCorporateActionPosition}.
   * @return the meta-bean, not null
   */
  public static AnnouncementCorporateActionPosition.Meta meta() {
    return AnnouncementCorporateActionPosition.Meta.INSTANCE;
  }

  static {
    MetaBean.register(AnnouncementCorporateActionPosition.Meta.INSTANCE);
  }

  /**
   * The serialization version id.
   */
  private static final long serialVersionUID = 1L;

  /**
   * Returns a builder used to create an instance of the bean.
   * @return the builder, not null
   */
  public static AnnouncementCorporateActionPosition.Builder builder() {
    return new AnnouncementCorporateActionPosition.Builder();
  }

  /**
   * Creates an instance.
   * @param info  the value of the property, not null
   * @param product  the value of the property, not null
   * @param longQuantity  the value of the property
   * @param shortQuantity  the value of the property
   */
  AnnouncementCorporateActionPosition(
      PositionInfo info,
      AnnouncementCorporateAction product,
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
  public AnnouncementCorporateActionPosition.Meta metaBean() {
    return AnnouncementCorporateActionPosition.Meta.INSTANCE;
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
   * Gets the underlying security.
   * @return the value of the property, not null
   */
  @Override
  public AnnouncementCorporateAction getProduct() {
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
      AnnouncementCorporateActionPosition other = (AnnouncementCorporateActionPosition) obj;
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
    StringBuilder buf = new StringBuilder(192);
    buf.append("AnnouncementCorporateActionPosition{");
    buf.append("info").append('=').append(JodaBeanUtils.toString(info)).append(',').append(' ');
    buf.append("product").append('=').append(JodaBeanUtils.toString(product)).append(',').append(' ');
    buf.append("longQuantity").append('=').append(JodaBeanUtils.toString(longQuantity)).append(',').append(' ');
    buf.append("shortQuantity").append('=').append(JodaBeanUtils.toString(shortQuantity)).append(',').append(' ');
    buf.append("quantity").append('=').append(JodaBeanUtils.toString(getQuantity()));
    buf.append('}');
    return buf.toString();
  }

  //-----------------------------------------------------------------------
  /**
   * The meta-bean for {@code AnnouncementCorporateActionPosition}.
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
        this, "info", AnnouncementCorporateActionPosition.class, PositionInfo.class);
    /**
     * The meta-property for the {@code product} property.
     */
    private final MetaProperty<AnnouncementCorporateAction> product = DirectMetaProperty.ofImmutable(
        this, "product", AnnouncementCorporateActionPosition.class, AnnouncementCorporateAction.class);
    /**
     * The meta-property for the {@code longQuantity} property.
     */
    private final MetaProperty<Double> longQuantity = DirectMetaProperty.ofImmutable(
        this, "longQuantity", AnnouncementCorporateActionPosition.class, Double.TYPE);
    /**
     * The meta-property for the {@code shortQuantity} property.
     */
    private final MetaProperty<Double> shortQuantity = DirectMetaProperty.ofImmutable(
        this, "shortQuantity", AnnouncementCorporateActionPosition.class, Double.TYPE);
    /**
     * The meta-property for the {@code quantity} property.
     */
    private final MetaProperty<Double> quantity = DirectMetaProperty.ofDerived(
        this, "quantity", AnnouncementCorporateActionPosition.class, Double.TYPE);
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
    public AnnouncementCorporateActionPosition.Builder builder() {
      return new AnnouncementCorporateActionPosition.Builder();
    }

    @Override
    public Class<? extends AnnouncementCorporateActionPosition> beanType() {
      return AnnouncementCorporateActionPosition.class;
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
    public MetaProperty<AnnouncementCorporateAction> product() {
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
          return ((AnnouncementCorporateActionPosition) bean).getInfo();
        case -309474065:  // product
          return ((AnnouncementCorporateActionPosition) bean).getProduct();
        case 611668775:  // longQuantity
          return ((AnnouncementCorporateActionPosition) bean).getLongQuantity();
        case -2094395097:  // shortQuantity
          return ((AnnouncementCorporateActionPosition) bean).getShortQuantity();
        case -1285004149:  // quantity
          return ((AnnouncementCorporateActionPosition) bean).getQuantity();
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
   * The bean-builder for {@code AnnouncementCorporateActionPosition}.
   */
  public static final class Builder extends DirectFieldsBeanBuilder<AnnouncementCorporateActionPosition> {

    private PositionInfo info;
    private AnnouncementCorporateAction product;
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
    private Builder(AnnouncementCorporateActionPosition beanToCopy) {
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
          this.product = (AnnouncementCorporateAction) newValue;
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
    public AnnouncementCorporateActionPosition build() {
      return new AnnouncementCorporateActionPosition(
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
     * Sets the underlying security.
     * @param product  the new value, not null
     * @return this, for chaining, not null
     */
    public Builder product(AnnouncementCorporateAction product) {
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
      StringBuilder buf = new StringBuilder(192);
      buf.append("AnnouncementCorporateActionPosition.Builder{");
      buf.append("info").append('=').append(JodaBeanUtils.toString(info)).append(',').append(' ');
      buf.append("product").append('=').append(JodaBeanUtils.toString(product)).append(',').append(' ');
      buf.append("longQuantity").append('=').append(JodaBeanUtils.toString(longQuantity)).append(',').append(' ');
      buf.append("shortQuantity").append('=').append(JodaBeanUtils.toString(shortQuantity)).append(',').append(' ');
      buf.append("quantity").append('=').append(JodaBeanUtils.toString(null));
      buf.append('}');
      return buf.toString();
    }

  }

  //-------------------------- AUTOGENERATED END --------------------------
}
