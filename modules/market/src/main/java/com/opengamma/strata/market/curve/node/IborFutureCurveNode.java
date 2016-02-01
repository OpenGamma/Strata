/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.market.curve.node;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;

import org.joda.beans.Bean;
import org.joda.beans.BeanDefinition;
import org.joda.beans.ImmutableBean;
import org.joda.beans.ImmutableDefaults;
import org.joda.beans.ImmutableValidator;
import org.joda.beans.JodaBeanUtils;
import org.joda.beans.MetaProperty;
import org.joda.beans.Property;
import org.joda.beans.PropertyDefinition;
import org.joda.beans.impl.direct.DirectFieldsBeanBuilder;
import org.joda.beans.impl.direct.DirectMetaBean;
import org.joda.beans.impl.direct.DirectMetaProperty;
import org.joda.beans.impl.direct.DirectMetaPropertyMap;

import com.google.common.collect.ImmutableSet;
import com.opengamma.strata.basics.market.MarketData;
import com.opengamma.strata.basics.market.ObservableKey;
import com.opengamma.strata.collect.ArgChecker;
import com.opengamma.strata.collect.Messages;
import com.opengamma.strata.market.ValueType;
import com.opengamma.strata.market.curve.CurveNode;
import com.opengamma.strata.market.curve.DatedCurveParameterMetadata;
import com.opengamma.strata.market.curve.meta.SimpleCurveNodeMetadata;
import com.opengamma.strata.market.curve.meta.YearMonthCurveNodeMetadata;
import com.opengamma.strata.product.index.IborFutureTrade;
import com.opengamma.strata.product.index.type.IborFutureTemplate;

/**
 * A curve node whose instrument is an Ibor Future.
 */
@BeanDefinition
public final class IborFutureCurveNode
    implements CurveNode, ImmutableBean, Serializable {

  /**
   * The template for the Ibor Futures associated with this node.
   */
  @PropertyDefinition(validate = "notNull")
  private final IborFutureTemplate template;
  /**
   * The key identifying the market data value which provides the price.
   */
  @PropertyDefinition(validate = "notNull")
  private final ObservableKey rateKey;
  /**
   * The additional spread added to the price.
   */
  @PropertyDefinition
  private final double additionalSpread;
  /**
   * The label to use for the node, may be empty.
   * <p>
   * If empty, a default label will be created when the metadata is built.
   * The default label depends on the valuation date, so cannot be created in the node.
   */
  @PropertyDefinition(validate = "notNull", overrideGet = true)
  private final String label;
  /**
   * The method by which the date of the node is calculated, defaulted to 'LastPaymentDate'.
   */
  @PropertyDefinition
  private final NodeDateType nodeDateType;
  /**
   * The fixed date to be used on the node, only used when the type is 'FixedDate'.
   */
  @PropertyDefinition(get = "field")
  private final LocalDate nodeDate;

  //-------------------------------------------------------------------------
  /**
   * Obtains a curve node for an Ibor Future using the specified template and rate key.
   *
   * @param template  the template used for building the instrument for the node
   * @param rateKey  the key identifying the market rate used when building the instrument for the node
   * @return a node whose instrument is built from the template using a market rate
   */
  public static IborFutureCurveNode of(IborFutureTemplate template, ObservableKey rateKey) {
    return of(template, rateKey, 0d);
  }

  /**
   * Obtains a curve node for an Ibor Future using the specified template, rate key and spread.
   *
   * @param template  the template defining the node instrument
   * @param rateKey  the key identifying the market data providing the rate for the node instrument
   * @param additionalSpread  the additional spread amount added to the rate
   * @return a node whose instrument is built from the template using a market rate
   */
  public static IborFutureCurveNode of(
      IborFutureTemplate template,
      ObservableKey rateKey,
      double additionalSpread) {

    return of(template, rateKey, additionalSpread, "");
  }

  /**
   * Obtains a curve node for an Ibor Future using the specified template, rate key, spread and label.
   *
   * @param template  the template defining the node instrument
   * @param rateKey  the key identifying the market data providing the rate for the node instrument
   * @param additionalSpread  the additional spread amount added to the rate
   * @param label  the label to use for the node, if empty an appropriate default label will be generated
   * @return a node whose instrument is built from the template using a market rate
   */
  public static IborFutureCurveNode of(
      IborFutureTemplate template,
      ObservableKey rateKey,
      double additionalSpread,
      String label) {

    return new IborFutureCurveNode(template, rateKey, additionalSpread, label, NodeDateType.LAST_PAYMENT_DATE, null);
  }

  //-------------------------------------------------------------------------
  @Override
  public Set<ObservableKey> requirements() {
    return ImmutableSet.of(rateKey);
  }

  @Override
  public DatedCurveParameterMetadata metadata(LocalDate valuationDate) {
    if (nodeDateType.equals(NodeDateType.FIXED_DATE)) {
      return SimpleCurveNodeMetadata.of(nodeDate, label);
    }
    LocalDate referenceDate = template.referenceDate(valuationDate);
    if (nodeDateType.equals(NodeDateType.LAST_PAYMENT_DATE)) {
      LocalDate maturityDate = template.getConvention().getIndex().calculateMaturityFromEffective(referenceDate);
      if (label.isEmpty()) {
        return YearMonthCurveNodeMetadata.of(maturityDate, YearMonth.from(referenceDate));
      }
      return YearMonthCurveNodeMetadata.of(maturityDate, YearMonth.from(referenceDate), label);
    }
    if (nodeDateType.equals(NodeDateType.LAST_FIXING_DATE)) {
      IborFutureTrade trade = template.toTrade(valuationDate, 1, 1.0d, 0.0d);
      LocalDate fixingDate = trade.getProduct().getFixingDate();
      if (label.isEmpty()) {
        return YearMonthCurveNodeMetadata.of(fixingDate, YearMonth.from(referenceDate));
      }
      return YearMonthCurveNodeMetadata.of(fixingDate, YearMonth.from(referenceDate), label);
    }
    throw new UnsupportedOperationException("Node date type " + nodeDateType.toString());
  }

  @Override
  public IborFutureTrade trade(LocalDate valuationDate, MarketData marketData) {
    double price = marketData.getValue(rateKey) + additionalSpread;
    return template.toTrade(valuationDate, 1L, 1d, price);
  }

  @Override
  public double initialGuess(LocalDate valuationDate, MarketData marketData, ValueType valueType) {
    if (ValueType.ZERO_RATE.equals(valueType) || ValueType.FORWARD_RATE.equals(valueType)) {
      return 1d - marketData.getValue(rateKey);
    }
    if (ValueType.DISCOUNT_FACTOR.equals(valueType)) {
      double approximateMaturity = template.getMinimumPeriod()
          .plus(template.getConvention().getIndex().getTenor()).toTotalMonths() / 12d;
      return Math.exp(-approximateMaturity * (1d - marketData.getValue(rateKey)));
    }
    return 0d;
  }

  /**
   * Checks if the type is 'FixedDate'.
   * <p>
   * 
   * @return true if the type is 'FixedDate'
   */
  public boolean isFixedDate() {
    return (nodeDateType == NodeDateType.FIXED_DATE);
  }

  /**
   * Gets the node date if the type is 'FixedDate'.
   * <p>
   * If the type is 'FixedDate', this returns the node date.
   * Otherwise, this throws an exception.
   * 
   * @return the node date, only available if the type is 'FixedDate'
   * @throws IllegalStateException if called on a failure result
   */
  public LocalDate getNodeDate() {
    if (!isFixedDate()) {
      throw new IllegalStateException(Messages.format("No currency available for type '{}'", nodeDateType));
    }
    return nodeDate;
  }

  @ImmutableValidator
  private void validate() {
    if (nodeDateType.equals(NodeDateType.FIXED_DATE)) {
      ArgChecker.isTrue(nodeDate != null, "Node date must be present when node date type is FIXED_DATE");
    } else {
      ArgChecker.isTrue(nodeDate == null, "Node date must be null when node date type is not FIXED_DATE");
    }
  }

  @ImmutableDefaults
  private static void applyDefaults(Builder builder) {
    builder.nodeDateType = NodeDateType.LAST_PAYMENT_DATE;
    builder.nodeDate = null;
  }

  //------------------------- AUTOGENERATED START -------------------------
  ///CLOVER:OFF
  /**
   * The meta-bean for {@code IborFutureCurveNode}.
   * @return the meta-bean, not null
   */
  public static IborFutureCurveNode.Meta meta() {
    return IborFutureCurveNode.Meta.INSTANCE;
  }

  static {
    JodaBeanUtils.registerMetaBean(IborFutureCurveNode.Meta.INSTANCE);
  }

  /**
   * The serialization version id.
   */
  private static final long serialVersionUID = 1L;

  /**
   * Returns a builder used to create an instance of the bean.
   * @return the builder, not null
   */
  public static IborFutureCurveNode.Builder builder() {
    return new IborFutureCurveNode.Builder();
  }

  private IborFutureCurveNode(
      IborFutureTemplate template,
      ObservableKey rateKey,
      double additionalSpread,
      String label,
      NodeDateType nodeDateType,
      LocalDate nodeDate) {
    JodaBeanUtils.notNull(template, "template");
    JodaBeanUtils.notNull(rateKey, "rateKey");
    JodaBeanUtils.notNull(label, "label");
    this.template = template;
    this.rateKey = rateKey;
    this.additionalSpread = additionalSpread;
    this.label = label;
    this.nodeDateType = nodeDateType;
    this.nodeDate = nodeDate;
    validate();
  }

  @Override
  public IborFutureCurveNode.Meta metaBean() {
    return IborFutureCurveNode.Meta.INSTANCE;
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
   * Gets the template for the Ibor Futures associated with this node.
   * @return the value of the property, not null
   */
  public IborFutureTemplate getTemplate() {
    return template;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the key identifying the market data value which provides the price.
   * @return the value of the property, not null
   */
  public ObservableKey getRateKey() {
    return rateKey;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the additional spread added to the price.
   * @return the value of the property
   */
  public double getAdditionalSpread() {
    return additionalSpread;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the label to use for the node, may be empty.
   * <p>
   * If empty, a default label will be created when the metadata is built.
   * The default label depends on the valuation date, so cannot be created in the node.
   * @return the value of the property, not null
   */
  @Override
  public String getLabel() {
    return label;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the method by which the date of the node is calculated, defaulted to 'LastPaymentDate'.
   * @return the value of the property
   */
  public NodeDateType getNodeDateType() {
    return nodeDateType;
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
      IborFutureCurveNode other = (IborFutureCurveNode) obj;
      return JodaBeanUtils.equal(template, other.template) &&
          JodaBeanUtils.equal(rateKey, other.rateKey) &&
          JodaBeanUtils.equal(additionalSpread, other.additionalSpread) &&
          JodaBeanUtils.equal(label, other.label) &&
          JodaBeanUtils.equal(nodeDateType, other.nodeDateType) &&
          JodaBeanUtils.equal(nodeDate, other.nodeDate);
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
    hash = hash * 31 + JodaBeanUtils.hashCode(nodeDateType);
    hash = hash * 31 + JodaBeanUtils.hashCode(nodeDate);
    return hash;
  }

  @Override
  public String toString() {
    StringBuilder buf = new StringBuilder(224);
    buf.append("IborFutureCurveNode{");
    buf.append("template").append('=').append(template).append(',').append(' ');
    buf.append("rateKey").append('=').append(rateKey).append(',').append(' ');
    buf.append("additionalSpread").append('=').append(additionalSpread).append(',').append(' ');
    buf.append("label").append('=').append(label).append(',').append(' ');
    buf.append("nodeDateType").append('=').append(nodeDateType).append(',').append(' ');
    buf.append("nodeDate").append('=').append(JodaBeanUtils.toString(nodeDate));
    buf.append('}');
    return buf.toString();
  }

  //-----------------------------------------------------------------------
  /**
   * The meta-bean for {@code IborFutureCurveNode}.
   */
  public static final class Meta extends DirectMetaBean {
    /**
     * The singleton instance of the meta-bean.
     */
    static final Meta INSTANCE = new Meta();

    /**
     * The meta-property for the {@code template} property.
     */
    private final MetaProperty<IborFutureTemplate> template = DirectMetaProperty.ofImmutable(
        this, "template", IborFutureCurveNode.class, IborFutureTemplate.class);
    /**
     * The meta-property for the {@code rateKey} property.
     */
    private final MetaProperty<ObservableKey> rateKey = DirectMetaProperty.ofImmutable(
        this, "rateKey", IborFutureCurveNode.class, ObservableKey.class);
    /**
     * The meta-property for the {@code additionalSpread} property.
     */
    private final MetaProperty<Double> additionalSpread = DirectMetaProperty.ofImmutable(
        this, "additionalSpread", IborFutureCurveNode.class, Double.TYPE);
    /**
     * The meta-property for the {@code label} property.
     */
    private final MetaProperty<String> label = DirectMetaProperty.ofImmutable(
        this, "label", IborFutureCurveNode.class, String.class);
    /**
     * The meta-property for the {@code nodeDateType} property.
     */
    private final MetaProperty<NodeDateType> nodeDateType = DirectMetaProperty.ofImmutable(
        this, "nodeDateType", IborFutureCurveNode.class, NodeDateType.class);
    /**
     * The meta-property for the {@code nodeDate} property.
     */
    private final MetaProperty<LocalDate> nodeDate = DirectMetaProperty.ofImmutable(
        this, "nodeDate", IborFutureCurveNode.class, LocalDate.class);
    /**
     * The meta-properties.
     */
    private final Map<String, MetaProperty<?>> metaPropertyMap$ = new DirectMetaPropertyMap(
        this, null,
        "template",
        "rateKey",
        "additionalSpread",
        "label",
        "nodeDateType",
        "nodeDate");

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
        case 937712682:  // nodeDateType
          return nodeDateType;
        case 1122582736:  // nodeDate
          return nodeDate;
      }
      return super.metaPropertyGet(propertyName);
    }

    @Override
    public IborFutureCurveNode.Builder builder() {
      return new IborFutureCurveNode.Builder();
    }

    @Override
    public Class<? extends IborFutureCurveNode> beanType() {
      return IborFutureCurveNode.class;
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
    public MetaProperty<IborFutureTemplate> template() {
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

    /**
     * The meta-property for the {@code nodeDateType} property.
     * @return the meta-property, not null
     */
    public MetaProperty<NodeDateType> nodeDateType() {
      return nodeDateType;
    }

    /**
     * The meta-property for the {@code nodeDate} property.
     * @return the meta-property, not null
     */
    public MetaProperty<LocalDate> nodeDate() {
      return nodeDate;
    }

    //-----------------------------------------------------------------------
    @Override
    protected Object propertyGet(Bean bean, String propertyName, boolean quiet) {
      switch (propertyName.hashCode()) {
        case -1321546630:  // template
          return ((IborFutureCurveNode) bean).getTemplate();
        case 983444831:  // rateKey
          return ((IborFutureCurveNode) bean).getRateKey();
        case 291232890:  // additionalSpread
          return ((IborFutureCurveNode) bean).getAdditionalSpread();
        case 102727412:  // label
          return ((IborFutureCurveNode) bean).getLabel();
        case 937712682:  // nodeDateType
          return ((IborFutureCurveNode) bean).getNodeDateType();
        case 1122582736:  // nodeDate
          return ((IborFutureCurveNode) bean).nodeDate;
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
   * The bean-builder for {@code IborFutureCurveNode}.
   */
  public static final class Builder extends DirectFieldsBeanBuilder<IborFutureCurveNode> {

    private IborFutureTemplate template;
    private ObservableKey rateKey;
    private double additionalSpread;
    private String label;
    private NodeDateType nodeDateType;
    private LocalDate nodeDate;

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
    private Builder(IborFutureCurveNode beanToCopy) {
      this.template = beanToCopy.getTemplate();
      this.rateKey = beanToCopy.getRateKey();
      this.additionalSpread = beanToCopy.getAdditionalSpread();
      this.label = beanToCopy.getLabel();
      this.nodeDateType = beanToCopy.getNodeDateType();
      this.nodeDate = beanToCopy.nodeDate;
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
        case 937712682:  // nodeDateType
          return nodeDateType;
        case 1122582736:  // nodeDate
          return nodeDate;
        default:
          throw new NoSuchElementException("Unknown property: " + propertyName);
      }
    }

    @Override
    public Builder set(String propertyName, Object newValue) {
      switch (propertyName.hashCode()) {
        case -1321546630:  // template
          this.template = (IborFutureTemplate) newValue;
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
        case 937712682:  // nodeDateType
          this.nodeDateType = (NodeDateType) newValue;
          break;
        case 1122582736:  // nodeDate
          this.nodeDate = (LocalDate) newValue;
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
    public IborFutureCurveNode build() {
      return new IborFutureCurveNode(
          template,
          rateKey,
          additionalSpread,
          label,
          nodeDateType,
          nodeDate);
    }

    //-----------------------------------------------------------------------
    /**
     * Sets the template for the Ibor Futures associated with this node.
     * @param template  the new value, not null
     * @return this, for chaining, not null
     */
    public Builder template(IborFutureTemplate template) {
      JodaBeanUtils.notNull(template, "template");
      this.template = template;
      return this;
    }

    /**
     * Sets the key identifying the market data value which provides the price.
     * @param rateKey  the new value, not null
     * @return this, for chaining, not null
     */
    public Builder rateKey(ObservableKey rateKey) {
      JodaBeanUtils.notNull(rateKey, "rateKey");
      this.rateKey = rateKey;
      return this;
    }

    /**
     * Sets the additional spread added to the price.
     * @param additionalSpread  the new value
     * @return this, for chaining, not null
     */
    public Builder additionalSpread(double additionalSpread) {
      this.additionalSpread = additionalSpread;
      return this;
    }

    /**
     * Sets the label to use for the node, may be empty.
     * <p>
     * If empty, a default label will be created when the metadata is built.
     * The default label depends on the valuation date, so cannot be created in the node.
     * @param label  the new value, not null
     * @return this, for chaining, not null
     */
    public Builder label(String label) {
      JodaBeanUtils.notNull(label, "label");
      this.label = label;
      return this;
    }

    /**
     * Sets the method by which the date of the node is calculated, defaulted to 'LastPaymentDate'.
     * @param nodeDateType  the new value
     * @return this, for chaining, not null
     */
    public Builder nodeDateType(NodeDateType nodeDateType) {
      this.nodeDateType = nodeDateType;
      return this;
    }

    /**
     * Sets the fixed date to be used on the node, only used when the type is 'FixedDate'.
     * @param nodeDate  the new value
     * @return this, for chaining, not null
     */
    public Builder nodeDate(LocalDate nodeDate) {
      this.nodeDate = nodeDate;
      return this;
    }

    //-----------------------------------------------------------------------
    @Override
    public String toString() {
      StringBuilder buf = new StringBuilder(224);
      buf.append("IborFutureCurveNode.Builder{");
      buf.append("template").append('=').append(JodaBeanUtils.toString(template)).append(',').append(' ');
      buf.append("rateKey").append('=').append(JodaBeanUtils.toString(rateKey)).append(',').append(' ');
      buf.append("additionalSpread").append('=').append(JodaBeanUtils.toString(additionalSpread)).append(',').append(' ');
      buf.append("label").append('=').append(JodaBeanUtils.toString(label)).append(',').append(' ');
      buf.append("nodeDateType").append('=').append(JodaBeanUtils.toString(nodeDateType)).append(',').append(' ');
      buf.append("nodeDate").append('=').append(JodaBeanUtils.toString(nodeDate));
      buf.append('}');
      return buf.toString();
    }

  }

  ///CLOVER:ON
  //-------------------------- AUTOGENERATED END --------------------------
}
