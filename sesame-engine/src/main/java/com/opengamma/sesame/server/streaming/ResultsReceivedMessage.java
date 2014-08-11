/**
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.sesame.server.streaming;

import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;

import org.joda.beans.Bean;
import org.joda.beans.BeanDefinition;
import org.joda.beans.ImmutableBean;
import org.joda.beans.ImmutableConstructor;
import org.joda.beans.JodaBeanUtils;
import org.joda.beans.MetaProperty;
import org.joda.beans.Property;
import org.joda.beans.PropertyDefinition;
import org.joda.beans.impl.direct.DirectFieldsBeanBuilder;
import org.joda.beans.impl.direct.DirectMetaBean;
import org.joda.beans.impl.direct.DirectMetaProperty;
import org.joda.beans.impl.direct.DirectMetaPropertyMap;

import com.google.common.base.Function;
import com.opengamma.sesame.engine.Results;
import com.opengamma.util.ArgumentChecker;

/**
 * Immutable bean indicating that results have been received.
 */
@BeanDefinition
public final class ResultsReceivedMessage implements Function<StreamingClientResultListener, Object>, ImmutableBean {

  /**
   * The results that were received.
   */
  @PropertyDefinition(validate = "notNull")
  private final Results _results;

  /**
   * Constructs a message with the received results.
   *
   * @param results the results that have been received
   */
  @ImmutableConstructor
  public ResultsReceivedMessage(Results results) {
    _results = ArgumentChecker.notNull(results, "results");
  }

  @Override
  public Object apply(StreamingClientResultListener input) {
    input.resultsReceived(_results);
    return null;
  }

  //------------------------- AUTOGENERATED START -------------------------
  ///CLOVER:OFF
  /**
   * The meta-bean for {@code ResultsReceivedMessage}.
   * @return the meta-bean, not null
   */
  public static ResultsReceivedMessage.Meta meta() {
    return ResultsReceivedMessage.Meta.INSTANCE;
  }

  static {
    JodaBeanUtils.registerMetaBean(ResultsReceivedMessage.Meta.INSTANCE);
  }

  /**
   * Returns a builder used to create an instance of the bean.
   * @return the builder, not null
   */
  public static ResultsReceivedMessage.Builder builder() {
    return new ResultsReceivedMessage.Builder();
  }

  @Override
  public ResultsReceivedMessage.Meta metaBean() {
    return ResultsReceivedMessage.Meta.INSTANCE;
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
   * Gets the results that were received.
   * @return the value of the property, not null
   */
  public Results getResults() {
    return _results;
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
      ResultsReceivedMessage other = (ResultsReceivedMessage) obj;
      return JodaBeanUtils.equal(getResults(), other.getResults());
    }
    return false;
  }

  @Override
  public int hashCode() {
    int hash = getClass().hashCode();
    hash += hash * 31 + JodaBeanUtils.hashCode(getResults());
    return hash;
  }

  @Override
  public String toString() {
    StringBuilder buf = new StringBuilder(64);
    buf.append("ResultsReceivedMessage{");
    buf.append("results").append('=').append(JodaBeanUtils.toString(getResults()));
    buf.append('}');
    return buf.toString();
  }

  //-----------------------------------------------------------------------
  /**
   * The meta-bean for {@code ResultsReceivedMessage}.
   */
  public static final class Meta extends DirectMetaBean {
    /**
     * The singleton instance of the meta-bean.
     */
    static final Meta INSTANCE = new Meta();

    /**
     * The meta-property for the {@code results} property.
     */
    private final MetaProperty<Results> _results = DirectMetaProperty.ofImmutable(
        this, "results", ResultsReceivedMessage.class, Results.class);
    /**
     * The meta-properties.
     */
    private final Map<String, MetaProperty<?>> _metaPropertyMap$ = new DirectMetaPropertyMap(
        this, null,
        "results");

    /**
     * Restricted constructor.
     */
    private Meta() {
    }

    @Override
    protected MetaProperty<?> metaPropertyGet(String propertyName) {
      switch (propertyName.hashCode()) {
        case 1097546742:  // results
          return _results;
      }
      return super.metaPropertyGet(propertyName);
    }

    @Override
    public ResultsReceivedMessage.Builder builder() {
      return new ResultsReceivedMessage.Builder();
    }

    @Override
    public Class<? extends ResultsReceivedMessage> beanType() {
      return ResultsReceivedMessage.class;
    }

    @Override
    public Map<String, MetaProperty<?>> metaPropertyMap() {
      return _metaPropertyMap$;
    }

    //-----------------------------------------------------------------------
    /**
     * The meta-property for the {@code results} property.
     * @return the meta-property, not null
     */
    public MetaProperty<Results> results() {
      return _results;
    }

    //-----------------------------------------------------------------------
    @Override
    protected Object propertyGet(Bean bean, String propertyName, boolean quiet) {
      switch (propertyName.hashCode()) {
        case 1097546742:  // results
          return ((ResultsReceivedMessage) bean).getResults();
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
   * The bean-builder for {@code ResultsReceivedMessage}.
   */
  public static final class Builder extends DirectFieldsBeanBuilder<ResultsReceivedMessage> {

    private Results _results;

    /**
     * Restricted constructor.
     */
    private Builder() {
    }

    /**
     * Restricted copy constructor.
     * @param beanToCopy  the bean to copy from, not null
     */
    private Builder(ResultsReceivedMessage beanToCopy) {
      this._results = beanToCopy.getResults();
    }

    //-----------------------------------------------------------------------
    @Override
    public Object get(String propertyName) {
      switch (propertyName.hashCode()) {
        case 1097546742:  // results
          return _results;
        default:
          throw new NoSuchElementException("Unknown property: " + propertyName);
      }
    }

    @Override
    public Builder set(String propertyName, Object newValue) {
      switch (propertyName.hashCode()) {
        case 1097546742:  // results
          this._results = (Results) newValue;
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
    public ResultsReceivedMessage build() {
      return new ResultsReceivedMessage(
          _results);
    }

    //-----------------------------------------------------------------------
    /**
     * Sets the {@code results} property in the builder.
     * @param results  the new value, not null
     * @return this, for chaining, not null
     */
    public Builder results(Results results) {
      JodaBeanUtils.notNull(results, "results");
      this._results = results;
      return this;
    }

    //-----------------------------------------------------------------------
    @Override
    public String toString() {
      StringBuilder buf = new StringBuilder(64);
      buf.append("ResultsReceivedMessage.Builder{");
      buf.append("results").append('=').append(JodaBeanUtils.toString(_results));
      buf.append('}');
      return buf.toString();
    }

  }

  ///CLOVER:ON
  //-------------------------- AUTOGENERATED END --------------------------
}
