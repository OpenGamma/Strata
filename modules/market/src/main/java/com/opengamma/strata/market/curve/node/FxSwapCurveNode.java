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
import org.joda.beans.ImmutableDefaults;
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
import com.opengamma.strata.basics.currency.FxRate;
import com.opengamma.strata.basics.date.Tenor;
import com.opengamma.strata.basics.market.FxRateKey;
import com.opengamma.strata.basics.market.MarketData;
import com.opengamma.strata.basics.market.ObservableKey;
import com.opengamma.strata.basics.market.SimpleMarketDataKey;
import com.opengamma.strata.market.ValueType;
import com.opengamma.strata.market.curve.CurveNode;
import com.opengamma.strata.market.curve.DatedCurveParameterMetadata;
import com.opengamma.strata.market.curve.meta.SimpleCurveNodeMetadata;
import com.opengamma.strata.market.curve.meta.TenorCurveNodeMetadata;
import com.opengamma.strata.product.fx.FxSwapTrade;
import com.opengamma.strata.product.fx.type.FxSwapTemplate;

/**
 * A curve node whose instrument is an FX Swap.
 */
@BeanDefinition
public final class FxSwapCurveNode
    implements CurveNode, ImmutableBean, Serializable {

  /**
   * The template for the FX Swap associated with this node.
   */
  @PropertyDefinition(validate = "notNull")
  private final FxSwapTemplate template;
  /**
   * The key identifying the market data value which provides the FX forward points.
   */
  @PropertyDefinition(validate = "notNull")
  private final ObservableKey farForwardPointsKey;
  /**
   * The label to use for the node, defaulted.
   * <p>
   * When building, this will default based on the far period if not specified.
   */
  @PropertyDefinition(validate = "notEmpty", overrideGet = true)
  private final String label;
  /**
   * The method by which the date of the node is calculated, defaulted to 'End'.
   */
  @PropertyDefinition
  private final CurveNodeDate date;

  //-------------------------------------------------------------------------
  /**
   * Returns a curve node for an FX Swap using the specified instrument template and keys.
   * <p>
   * A suitable default label will be created.
   *
   * @param template  the template used for building the instrument for the node
   * @param farForwardPointsKey  the key identifying the FX points at the far date
   * @return a node whose instrument is built from the template using a market rate
   */
  public static FxSwapCurveNode of(FxSwapTemplate template, ObservableKey farForwardPointsKey) {
    return builder()
        .template(template)
        .farForwardPointsKey(farForwardPointsKey)
        .build();
  }

  /**
   * Returns a curve node for an FX Swap using the specified instrument template and keys and label.
   *
   * @param template  the template used for building the instrument for the node
   * @param farForwardPointsKey  the key identifying the FX points at the far date
   * @param label  the label to use for the node
   * @return a node whose instrument is built from the template using a market rate
   */
  public static FxSwapCurveNode of(FxSwapTemplate template, ObservableKey farForwardPointsKey, String label) {
    return builder()
        .template(template)
        .farForwardPointsKey(farForwardPointsKey)
        .label(label)
        .build();
  }

  @ImmutableDefaults
  private static void applyDefaults(Builder builder) {
    builder.date = CurveNodeDate.END;
  }

  @ImmutablePreBuild
  private static void preBuild(Builder builder) {
    if (builder.label == null && builder.template != null) {
      builder.label = Tenor.of(builder.template.getPeriodToFar()).toString();
    }
  }

  //-------------------------------------------------------------------------
  @Override
  public Set<? extends SimpleMarketDataKey<?>> requirements() {
    // TODO: extra key for near forward points
    return ImmutableSet.of(farForwardPointsKey, fxKey());
  }

  @Override
  public DatedCurveParameterMetadata metadata(LocalDate valuationDate) {
    LocalDate nodeDate = date.calculate(
        () -> calculateEnd(valuationDate),
        () -> calculateLastFixingDate(valuationDate));
    if (date.isFixed()) {
      return SimpleCurveNodeMetadata.of(nodeDate, label);
    }
    Tenor tenor = Tenor.of(template.getPeriodToFar());
    return TenorCurveNodeMetadata.of(nodeDate, tenor, label);
  }

  // calculate the end date
  private LocalDate calculateEnd(LocalDate valuationDate) {
    FxSwapTrade trade = template.createTrade(valuationDate, BuySell.BUY, 1, 1, 0);
    return trade.getProduct().getFarLeg().getPaymentDate();
  }

  // calculate the last fixing date
  private LocalDate calculateLastFixingDate(LocalDate valuationDate) {
    throw new UnsupportedOperationException("Node date of 'LastFixing' is not supported for FxSwap");
  }

  @Override
  public FxSwapTrade trade(LocalDate valuationDate, MarketData marketData) {
    FxRate fxRate = marketData.getValue(fxKey());
    double rate = fxRate.fxRate(template.getCurrencyPair());
    double fxPts = marketData.getValue(farForwardPointsKey);
    return template.createTrade(valuationDate, BuySell.BUY, 1d, rate, fxPts);
  }

  @Override
  public double initialGuess(LocalDate valuationDate, MarketData marketData, ValueType valueType) {
    if (ValueType.DISCOUNT_FACTOR.equals(valueType)) {
      return 1d;
    }
    return 0d;
  }

  private FxRateKey fxKey() {
    return FxRateKey.of(template.getCurrencyPair());
  }

  //-------------------------------------------------------------------------
  /**
   * Returns a copy of this node with the specified date.
   * 
   * @param date  the date to use
   * @return the node based on this node with the specified date
   */
  public FxSwapCurveNode withDate(CurveNodeDate date) {
    return new FxSwapCurveNode(template, farForwardPointsKey, label, date);
  }

  //------------------------- AUTOGENERATED START -------------------------
  ///CLOVER:OFF
  /**
   * The meta-bean for {@code FxSwapCurveNode}.
   * @return the meta-bean, not null
   */
  public static FxSwapCurveNode.Meta meta() {
    return FxSwapCurveNode.Meta.INSTANCE;
  }

  static {
    JodaBeanUtils.registerMetaBean(FxSwapCurveNode.Meta.INSTANCE);
  }

  /**
   * The serialization version id.
   */
  private static final long serialVersionUID = 1L;

  /**
   * Returns a builder used to create an instance of the bean.
   * @return the builder, not null
   */
  public static FxSwapCurveNode.Builder builder() {
    return new FxSwapCurveNode.Builder();
  }

  private FxSwapCurveNode(
      FxSwapTemplate template,
      ObservableKey farForwardPointsKey,
      String label,
      CurveNodeDate date) {
    JodaBeanUtils.notNull(template, "template");
    JodaBeanUtils.notNull(farForwardPointsKey, "farForwardPointsKey");
    JodaBeanUtils.notEmpty(label, "label");
    this.template = template;
    this.farForwardPointsKey = farForwardPointsKey;
    this.label = label;
    this.date = date;
  }

  @Override
  public FxSwapCurveNode.Meta metaBean() {
    return FxSwapCurveNode.Meta.INSTANCE;
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
   * Gets the template for the FX Swap associated with this node.
   * @return the value of the property, not null
   */
  public FxSwapTemplate getTemplate() {
    return template;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the key identifying the market data value which provides the FX forward points.
   * @return the value of the property, not null
   */
  public ObservableKey getFarForwardPointsKey() {
    return farForwardPointsKey;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the label to use for the node, defaulted.
   * <p>
   * When building, this will default based on the far period if not specified.
   * @return the value of the property, not empty
   */
  @Override
  public String getLabel() {
    return label;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the method by which the date of the node is calculated, defaulted to 'End'.
   * @return the value of the property
   */
  public CurveNodeDate getDate() {
    return date;
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
      FxSwapCurveNode other = (FxSwapCurveNode) obj;
      return JodaBeanUtils.equal(template, other.template) &&
          JodaBeanUtils.equal(farForwardPointsKey, other.farForwardPointsKey) &&
          JodaBeanUtils.equal(label, other.label) &&
          JodaBeanUtils.equal(date, other.date);
    }
    return false;
  }

  @Override
  public int hashCode() {
    int hash = getClass().hashCode();
    hash = hash * 31 + JodaBeanUtils.hashCode(template);
    hash = hash * 31 + JodaBeanUtils.hashCode(farForwardPointsKey);
    hash = hash * 31 + JodaBeanUtils.hashCode(label);
    hash = hash * 31 + JodaBeanUtils.hashCode(date);
    return hash;
  }

  @Override
  public String toString() {
    StringBuilder buf = new StringBuilder(160);
    buf.append("FxSwapCurveNode{");
    buf.append("template").append('=').append(template).append(',').append(' ');
    buf.append("farForwardPointsKey").append('=').append(farForwardPointsKey).append(',').append(' ');
    buf.append("label").append('=').append(label).append(',').append(' ');
    buf.append("date").append('=').append(JodaBeanUtils.toString(date));
    buf.append('}');
    return buf.toString();
  }

  //-----------------------------------------------------------------------
  /**
   * The meta-bean for {@code FxSwapCurveNode}.
   */
  public static final class Meta extends DirectMetaBean {
    /**
     * The singleton instance of the meta-bean.
     */
    static final Meta INSTANCE = new Meta();

    /**
     * The meta-property for the {@code template} property.
     */
    private final MetaProperty<FxSwapTemplate> template = DirectMetaProperty.ofImmutable(
        this, "template", FxSwapCurveNode.class, FxSwapTemplate.class);
    /**
     * The meta-property for the {@code farForwardPointsKey} property.
     */
    private final MetaProperty<ObservableKey> farForwardPointsKey = DirectMetaProperty.ofImmutable(
        this, "farForwardPointsKey", FxSwapCurveNode.class, ObservableKey.class);
    /**
     * The meta-property for the {@code label} property.
     */
    private final MetaProperty<String> label = DirectMetaProperty.ofImmutable(
        this, "label", FxSwapCurveNode.class, String.class);
    /**
     * The meta-property for the {@code date} property.
     */
    private final MetaProperty<CurveNodeDate> date = DirectMetaProperty.ofImmutable(
        this, "date", FxSwapCurveNode.class, CurveNodeDate.class);
    /**
     * The meta-properties.
     */
    private final Map<String, MetaProperty<?>> metaPropertyMap$ = new DirectMetaPropertyMap(
        this, null,
        "template",
        "farForwardPointsKey",
        "label",
        "date");

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
        case -367520146:  // farForwardPointsKey
          return farForwardPointsKey;
        case 102727412:  // label
          return label;
        case 3076014:  // date
          return date;
      }
      return super.metaPropertyGet(propertyName);
    }

    @Override
    public FxSwapCurveNode.Builder builder() {
      return new FxSwapCurveNode.Builder();
    }

    @Override
    public Class<? extends FxSwapCurveNode> beanType() {
      return FxSwapCurveNode.class;
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
    public MetaProperty<FxSwapTemplate> template() {
      return template;
    }

    /**
     * The meta-property for the {@code farForwardPointsKey} property.
     * @return the meta-property, not null
     */
    public MetaProperty<ObservableKey> farForwardPointsKey() {
      return farForwardPointsKey;
    }

    /**
     * The meta-property for the {@code label} property.
     * @return the meta-property, not null
     */
    public MetaProperty<String> label() {
      return label;
    }

    /**
     * The meta-property for the {@code date} property.
     * @return the meta-property, not null
     */
    public MetaProperty<CurveNodeDate> date() {
      return date;
    }

    //-----------------------------------------------------------------------
    @Override
    protected Object propertyGet(Bean bean, String propertyName, boolean quiet) {
      switch (propertyName.hashCode()) {
        case -1321546630:  // template
          return ((FxSwapCurveNode) bean).getTemplate();
        case -367520146:  // farForwardPointsKey
          return ((FxSwapCurveNode) bean).getFarForwardPointsKey();
        case 102727412:  // label
          return ((FxSwapCurveNode) bean).getLabel();
        case 3076014:  // date
          return ((FxSwapCurveNode) bean).getDate();
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
   * The bean-builder for {@code FxSwapCurveNode}.
   */
  public static final class Builder extends DirectFieldsBeanBuilder<FxSwapCurveNode> {

    private FxSwapTemplate template;
    private ObservableKey farForwardPointsKey;
    private String label;
    private CurveNodeDate date;

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
    private Builder(FxSwapCurveNode beanToCopy) {
      this.template = beanToCopy.getTemplate();
      this.farForwardPointsKey = beanToCopy.getFarForwardPointsKey();
      this.label = beanToCopy.getLabel();
      this.date = beanToCopy.getDate();
    }

    //-----------------------------------------------------------------------
    @Override
    public Object get(String propertyName) {
      switch (propertyName.hashCode()) {
        case -1321546630:  // template
          return template;
        case -367520146:  // farForwardPointsKey
          return farForwardPointsKey;
        case 102727412:  // label
          return label;
        case 3076014:  // date
          return date;
        default:
          throw new NoSuchElementException("Unknown property: " + propertyName);
      }
    }

    @Override
    public Builder set(String propertyName, Object newValue) {
      switch (propertyName.hashCode()) {
        case -1321546630:  // template
          this.template = (FxSwapTemplate) newValue;
          break;
        case -367520146:  // farForwardPointsKey
          this.farForwardPointsKey = (ObservableKey) newValue;
          break;
        case 102727412:  // label
          this.label = (String) newValue;
          break;
        case 3076014:  // date
          this.date = (CurveNodeDate) newValue;
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
    public FxSwapCurveNode build() {
      preBuild(this);
      return new FxSwapCurveNode(
          template,
          farForwardPointsKey,
          label,
          date);
    }

    //-----------------------------------------------------------------------
    /**
     * Sets the template for the FX Swap associated with this node.
     * @param template  the new value, not null
     * @return this, for chaining, not null
     */
    public Builder template(FxSwapTemplate template) {
      JodaBeanUtils.notNull(template, "template");
      this.template = template;
      return this;
    }

    /**
     * Sets the key identifying the market data value which provides the FX forward points.
     * @param farForwardPointsKey  the new value, not null
     * @return this, for chaining, not null
     */
    public Builder farForwardPointsKey(ObservableKey farForwardPointsKey) {
      JodaBeanUtils.notNull(farForwardPointsKey, "farForwardPointsKey");
      this.farForwardPointsKey = farForwardPointsKey;
      return this;
    }

    /**
     * Sets the label to use for the node, defaulted.
     * <p>
     * When building, this will default based on the far period if not specified.
     * @param label  the new value, not empty
     * @return this, for chaining, not null
     */
    public Builder label(String label) {
      JodaBeanUtils.notEmpty(label, "label");
      this.label = label;
      return this;
    }

    /**
     * Sets the method by which the date of the node is calculated, defaulted to 'End'.
     * @param date  the new value
     * @return this, for chaining, not null
     */
    public Builder date(CurveNodeDate date) {
      this.date = date;
      return this;
    }

    //-----------------------------------------------------------------------
    @Override
    public String toString() {
      StringBuilder buf = new StringBuilder(160);
      buf.append("FxSwapCurveNode.Builder{");
      buf.append("template").append('=').append(JodaBeanUtils.toString(template)).append(',').append(' ');
      buf.append("farForwardPointsKey").append('=').append(JodaBeanUtils.toString(farForwardPointsKey)).append(',').append(' ');
      buf.append("label").append('=').append(JodaBeanUtils.toString(label)).append(',').append(' ');
      buf.append("date").append('=').append(JodaBeanUtils.toString(date));
      buf.append('}');
      return buf.toString();
    }

  }

  ///CLOVER:ON
  //-------------------------- AUTOGENERATED END --------------------------
}
