/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.market.curve.node;

import java.io.Serializable;
import java.time.LocalDate;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;

import org.joda.beans.Bean;
import org.joda.beans.BeanDefinition;
import org.joda.beans.ImmutableBean;
import org.joda.beans.ImmutablePreBuild;
import org.joda.beans.JodaBeanUtils;
import org.joda.beans.MetaProperty;
import org.joda.beans.Property;
import org.joda.beans.PropertyDefinition;
import org.joda.beans.impl.direct.DirectFieldsBeanBuilder;
import org.joda.beans.impl.direct.DirectMetaBean;
import org.joda.beans.impl.direct.DirectMetaProperty;
import org.joda.beans.impl.direct.DirectMetaPropertyMap;

import com.google.common.collect.ImmutableSet;
import com.opengamma.strata.basics.BuySell;
import com.opengamma.strata.basics.market.MarketData;
import com.opengamma.strata.basics.market.ObservableKey;
import com.opengamma.strata.market.ValueType;
import com.opengamma.strata.market.curve.CurveNode;
import com.opengamma.strata.market.curve.DatedCurveParameterMetadata;
import com.opengamma.strata.market.curve.meta.TenorCurveNodeMetadata;
import com.opengamma.strata.product.swap.SwapTrade;
import com.opengamma.strata.product.swap.type.ThreeLegBasisSwapTemplate;

/**
 * A curve node whose instrument is a three leg basis swap.
 */
@BeanDefinition
public final class ThreeLegBasisSwapCurveNode
    implements CurveNode, ImmutableBean, Serializable {

  /**
   * The template for the swap associated with this node.
   */
  @PropertyDefinition(validate = "notNull")
  private final ThreeLegBasisSwapTemplate template;
  /**
   * The key identifying the market data value which provides the rate.
   */
  @PropertyDefinition(validate = "notNull")
  private final ObservableKey rateKey;
  /**
   * The additional spread added to the market quote.
   */
  @PropertyDefinition
  private final double additionalSpread;
  /**
   * The label to use for the node, defaulted.
   * <p>
   * When building, this will default based on the tenor if not specified.
   */
  @PropertyDefinition(validate = "notEmpty")
  private final String label;

  //-------------------------------------------------------------------------
  /**
   * Returns a curve node for a three leg basis swap using the
   * specified instrument template and rate.
   * <p>
   * A suitable default label will be created.
   *
   * @param template  the template used for building the instrument for the node
   * @param rateKey  the key identifying the market rate used when building the instrument for the node
   * @return a node whose instrument is built from the template using a market rate
   */
  public static ThreeLegBasisSwapCurveNode of(ThreeLegBasisSwapTemplate template, ObservableKey rateKey) {
    return of(template, rateKey, 0d);
  }

  /**
   * Returns a curve node for a three leg basis swap using the specified instrument template, rate key and spread.
   * <p>
   * A suitable default label will be created.
   *
   * @param template  the template defining the node instrument
   * @param rateKey  the key identifying the market data providing the rate for the node instrument
   * @param additionalSpread  the additional spread amount added to the market quote
   * @return a node whose instrument is built from the template using a market rate
   */
  public static ThreeLegBasisSwapCurveNode of(ThreeLegBasisSwapTemplate template, ObservableKey rateKey,
      double additionalSpread) {
    return builder()
        .template(template)
        .rateKey(rateKey)
        .additionalSpread(additionalSpread)
        .build();
  }

  /**
   * Returns a curve node for a three leg basis swap using the specified instrument template, rate key, spread and label.
   *
   * @param template  the template defining the node instrument
   * @param rateKey  the key identifying the market data providing the rate for the node instrument
   * @param additionalSpread  the additional spread amount added to the market quote
   * @param label  the label to use for the node, if null or empty an appropriate default label will be used
   * @return a node whose instrument is built from the template using a market rate
   */
  public static ThreeLegBasisSwapCurveNode of(
      ThreeLegBasisSwapTemplate template,
      ObservableKey rateKey,
      double additionalSpread,
      String label) {

    return new ThreeLegBasisSwapCurveNode(template, rateKey, additionalSpread, label);
  }

  @ImmutablePreBuild
  private static void preBuild(Builder builder) {
    if (builder.label == null && builder.template != null) {
      builder.label = builder.template.getTenor().toString();
    }
  }

  //-------------------------------------------------------------------------
  @Override
  public Set<ObservableKey> requirements() {
    return ImmutableSet.of(rateKey);
  }

  @Override
  public DatedCurveParameterMetadata metadata(LocalDate valuationDate) {
    SwapTrade trade = template.toTrade(valuationDate, BuySell.BUY, 1, 1);
    return TenorCurveNodeMetadata.of(trade.getProduct().getEndDate(), template.getTenor(), label);
  }

  @Override
  public SwapTrade trade(LocalDate valuationDate, MarketData marketData) {
    double marketQuote = marketData.getValue(rateKey) + additionalSpread;
    return template.toTrade(valuationDate, BuySell.BUY, 1, marketQuote);
  }

  @Override
  public double initialGuess(LocalDate valuationDate, MarketData marketData, ValueType valueType) {
    if (ValueType.DISCOUNT_FACTOR.equals(valueType)) {
      return 1d;
    }
    return 0d;
  }

  //------------------------- AUTOGENERATED START -------------------------
  ///CLOVER:OFF
  /**
   * The meta-bean for {@code ThreeLegBasisSwapCurveNode}.
   * @return the meta-bean, not null
   */
  public static ThreeLegBasisSwapCurveNode.Meta meta() {
    return ThreeLegBasisSwapCurveNode.Meta.INSTANCE;
  }

  static {
    JodaBeanUtils.registerMetaBean(ThreeLegBasisSwapCurveNode.Meta.INSTANCE);
  }

  /**
   * The serialization version id.
   */
  private static final long serialVersionUID = 1L;

  /**
   * Returns a builder used to create an instance of the bean.
   * @return the builder, not null
   */
  public static ThreeLegBasisSwapCurveNode.Builder builder() {
    return new ThreeLegBasisSwapCurveNode.Builder();
  }

  private ThreeLegBasisSwapCurveNode(
      ThreeLegBasisSwapTemplate template,
      ObservableKey rateKey,
      double additionalSpread,
      String label) {
    JodaBeanUtils.notNull(template, "template");
    JodaBeanUtils.notNull(rateKey, "rateKey");
    JodaBeanUtils.notEmpty(label, "label");
    this.template = template;
    this.rateKey = rateKey;
    this.additionalSpread = additionalSpread;
    this.label = label;
  }

  @Override
  public ThreeLegBasisSwapCurveNode.Meta metaBean() {
    return ThreeLegBasisSwapCurveNode.Meta.INSTANCE;
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
   * Gets the template for the swap associated with this node.
   * @return the value of the property, not null
   */
  public ThreeLegBasisSwapTemplate getTemplate() {
    return template;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the key identifying the market data value which provides the rate.
   * @return the value of the property, not null
   */
  public ObservableKey getRateKey() {
    return rateKey;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the additional spread added to the market quote.
   * @return the value of the property
   */
  public double getAdditionalSpread() {
    return additionalSpread;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the label to use for the node, defaulted.
   * <p>
   * When building, this will default based on the tenor if not specified.
   * @return the value of the property, not empty
   */
  public String getLabel() {
    return label;
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
      ThreeLegBasisSwapCurveNode other = (ThreeLegBasisSwapCurveNode) obj;
      return JodaBeanUtils.equal(template, other.template) &&
          JodaBeanUtils.equal(rateKey, other.rateKey) &&
          JodaBeanUtils.equal(additionalSpread, other.additionalSpread) &&
          JodaBeanUtils.equal(label, other.label);
    }
    return false;
  }

  @Override
  public int hashCode() {
    int hash = getClass().hashCode();
    hash = hash * 31 + JodaBeanUtils.hashCode(template);
    hash = hash * 31 + JodaBeanUtils.hashCode(rateKey);
    hash = hash * 31 + JodaBeanUtils.hashCode(additionalSpread);
    hash = hash * 31 + JodaBeanUtils.hashCode(label);
    return hash;
  }

  @Override
  public String toString() {
    StringBuilder buf = new StringBuilder(160);
    buf.append("ThreeLegBasisSwapCurveNode{");
    buf.append("template").append('=').append(template).append(',').append(' ');
    buf.append("rateKey").append('=').append(rateKey).append(',').append(' ');
    buf.append("additionalSpread").append('=').append(additionalSpread).append(',').append(' ');
    buf.append("label").append('=').append(JodaBeanUtils.toString(label));
    buf.append('}');
    return buf.toString();
  }

  //-----------------------------------------------------------------------
  /**
   * The meta-bean for {@code ThreeLegBasisSwapCurveNode}.
   */
  public static final class Meta extends DirectMetaBean {
    /**
     * The singleton instance of the meta-bean.
     */
    static final Meta INSTANCE = new Meta();

    /**
     * The meta-property for the {@code template} property.
     */
    private final MetaProperty<ThreeLegBasisSwapTemplate> template = DirectMetaProperty.ofImmutable(
        this, "template", ThreeLegBasisSwapCurveNode.class, ThreeLegBasisSwapTemplate.class);
    /**
     * The meta-property for the {@code rateKey} property.
     */
    private final MetaProperty<ObservableKey> rateKey = DirectMetaProperty.ofImmutable(
        this, "rateKey", ThreeLegBasisSwapCurveNode.class, ObservableKey.class);
    /**
     * The meta-property for the {@code additionalSpread} property.
     */
    private final MetaProperty<Double> additionalSpread = DirectMetaProperty.ofImmutable(
        this, "additionalSpread", ThreeLegBasisSwapCurveNode.class, Double.TYPE);
    /**
     * The meta-property for the {@code label} property.
     */
    private final MetaProperty<String> label = DirectMetaProperty.ofImmutable(
        this, "label", ThreeLegBasisSwapCurveNode.class, String.class);
    /**
     * The meta-properties.
     */
    private final Map<String, MetaProperty<?>> metaPropertyMap$ = new DirectMetaPropertyMap(
        this, null,
        "template",
        "rateKey",
        "additionalSpread",
        "label");

    /**
     * Restricted constructor.
     */
    private Meta() {
    }

    @Override
    protected MetaProperty<?> metaPropertyGet(String propertyName) {
      switch (propertyName.hashCode()) {
        case -1321546630:  // template
          return template;
        case 983444831:  // rateKey
          return rateKey;
        case 291232890:  // additionalSpread
          return additionalSpread;
        case 102727412:  // label
          return label;
      }
      return super.metaPropertyGet(propertyName);
    }

    @Override
    public ThreeLegBasisSwapCurveNode.Builder builder() {
      return new ThreeLegBasisSwapCurveNode.Builder();
    }

    @Override
    public Class<? extends ThreeLegBasisSwapCurveNode> beanType() {
      return ThreeLegBasisSwapCurveNode.class;
    }

    @Override
    public Map<String, MetaProperty<?>> metaPropertyMap() {
      return metaPropertyMap$;
    }

    //-----------------------------------------------------------------------
    /**
     * The meta-property for the {@code template} property.
     * @return the meta-property, not null
     */
    public MetaProperty<ThreeLegBasisSwapTemplate> template() {
      return template;
    }

    /**
     * The meta-property for the {@code rateKey} property.
     * @return the meta-property, not null
     */
    public MetaProperty<ObservableKey> rateKey() {
      return rateKey;
    }

    /**
     * The meta-property for the {@code additionalSpread} property.
     * @return the meta-property, not null
     */
    public MetaProperty<Double> additionalSpread() {
      return additionalSpread;
    }

    /**
     * The meta-property for the {@code label} property.
     * @return the meta-property, not null
     */
    public MetaProperty<String> label() {
      return label;
    }

    //-----------------------------------------------------------------------
    @Override
    protected Object propertyGet(Bean bean, String propertyName, boolean quiet) {
      switch (propertyName.hashCode()) {
        case -1321546630:  // template
          return ((ThreeLegBasisSwapCurveNode) bean).getTemplate();
        case 983444831:  // rateKey
          return ((ThreeLegBasisSwapCurveNode) bean).getRateKey();
        case 291232890:  // additionalSpread
          return ((ThreeLegBasisSwapCurveNode) bean).getAdditionalSpread();
        case 102727412:  // label
          return ((ThreeLegBasisSwapCurveNode) bean).getLabel();
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
   * The bean-builder for {@code ThreeLegBasisSwapCurveNode}.
   */
  public static final class Builder extends DirectFieldsBeanBuilder<ThreeLegBasisSwapCurveNode> {

    private ThreeLegBasisSwapTemplate template;
    private ObservableKey rateKey;
    private double additionalSpread;
    private String label;

    /**
     * Restricted constructor.
     */
    private Builder() {
    }

    /**
     * Restricted copy constructor.
     * @param beanToCopy  the bean to copy from, not null
     */
    private Builder(ThreeLegBasisSwapCurveNode beanToCopy) {
      this.template = beanToCopy.getTemplate();
      this.rateKey = beanToCopy.getRateKey();
      this.additionalSpread = beanToCopy.getAdditionalSpread();
      this.label = beanToCopy.getLabel();
    }

    //-----------------------------------------------------------------------
    @Override
    public Object get(String propertyName) {
      switch (propertyName.hashCode()) {
        case -1321546630:  // template
          return template;
        case 983444831:  // rateKey
          return rateKey;
        case 291232890:  // additionalSpread
          return additionalSpread;
        case 102727412:  // label
          return label;
        default:
          throw new NoSuchElementException("Unknown property: " + propertyName);
      }
    }

    @Override
    public Builder set(String propertyName, Object newValue) {
      switch (propertyName.hashCode()) {
        case -1321546630:  // template
          this.template = (ThreeLegBasisSwapTemplate) newValue;
          break;
        case 983444831:  // rateKey
          this.rateKey = (ObservableKey) newValue;
          break;
        case 291232890:  // additionalSpread
          this.additionalSpread = (Double) newValue;
          break;
        case 102727412:  // label
          this.label = (String) newValue;
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
    public ThreeLegBasisSwapCurveNode build() {
      preBuild(this);
      return new ThreeLegBasisSwapCurveNode(
          template,
          rateKey,
          additionalSpread,
          label);
    }

    //-----------------------------------------------------------------------
    /**
     * Sets the template for the swap associated with this node.
     * @param template  the new value, not null
     * @return this, for chaining, not null
     */
    public Builder template(ThreeLegBasisSwapTemplate template) {
      JodaBeanUtils.notNull(template, "template");
      this.template = template;
      return this;
    }

    /**
     * Sets the key identifying the market data value which provides the rate.
     * @param rateKey  the new value, not null
     * @return this, for chaining, not null
     */
    public Builder rateKey(ObservableKey rateKey) {
      JodaBeanUtils.notNull(rateKey, "rateKey");
      this.rateKey = rateKey;
      return this;
    }

    /**
     * Sets the additional spread added to the market quote.
     * @param additionalSpread  the new value
     * @return this, for chaining, not null
     */
    public Builder additionalSpread(double additionalSpread) {
      this.additionalSpread = additionalSpread;
      return this;
    }

    /**
     * Sets the label to use for the node, defaulted.
     * <p>
     * When building, this will default based on the tenor if not specified.
     * @param label  the new value, not empty
     * @return this, for chaining, not null
     */
    public Builder label(String label) {
      JodaBeanUtils.notEmpty(label, "label");
      this.label = label;
      return this;
    }

    //-----------------------------------------------------------------------
    @Override
    public String toString() {
      StringBuilder buf = new StringBuilder(160);
      buf.append("ThreeLegBasisSwapCurveNode.Builder{");
      buf.append("template").append('=').append(JodaBeanUtils.toString(template)).append(',').append(' ');
      buf.append("rateKey").append('=').append(JodaBeanUtils.toString(rateKey)).append(',').append(' ');
      buf.append("additionalSpread").append('=').append(JodaBeanUtils.toString(additionalSpread)).append(',').append(' ');
      buf.append("label").append('=').append(JodaBeanUtils.toString(label));
      buf.append('}');
      return buf.toString();
    }

  }

  ///CLOVER:ON
  //-------------------------- AUTOGENERATED END --------------------------
}
