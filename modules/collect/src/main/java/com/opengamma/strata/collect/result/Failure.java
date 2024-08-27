/*
 * Copyright (C) 2013 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.collect.result;

import static com.opengamma.strata.collect.Guavate.toImmutableSet;

import java.io.Serializable;
import java.util.Collection;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.joda.beans.Bean;
import org.joda.beans.BeanBuilder;
import org.joda.beans.ImmutableBean;
import org.joda.beans.JodaBeanUtils;
import org.joda.beans.MetaBean;
import org.joda.beans.MetaProperty;
import org.joda.beans.gen.BeanDefinition;
import org.joda.beans.gen.PropertyDefinition;
import org.joda.beans.impl.direct.DirectMetaBean;
import org.joda.beans.impl.direct.DirectMetaProperty;
import org.joda.beans.impl.direct.DirectMetaPropertyMap;
import org.joda.beans.impl.direct.DirectPrivateBeanBuilder;

import com.google.common.collect.ImmutableSet;
import com.opengamma.strata.collect.ArgChecker;
import com.opengamma.strata.collect.Messages;

/**
 * Description of a failed result.
 * <p>
 * If calculation of a result fails this class provides details of the failure.
 * There is a single reason and message and a set of detailed failure items.
 * Each {@link FailureItem} has details of the actual cause.
 * <p>
 * In most cases, instances of {@code Failure} should be created using one of the
 * {@code failure} methods on {@link Result}.
 */
@BeanDefinition(builderScope = "private")
public final class Failure
    implements ImmutableBean, Serializable {

  /**
   * The reason associated with the failure.
   */
  @PropertyDefinition(validate = "notNull")
  private final FailureReason reason;
  /**
   * The error message associated with the failure.
   */
  @PropertyDefinition(validate = "notEmpty")
  private final String message;
  /**
   * The set of failure items.
   * There will be at least one failure item.
   */
  @PropertyDefinition(validate = "notEmpty")
  private final ImmutableSet<FailureItem> items;

  //-------------------------------------------------------------------------
  /**
   * Obtains a failure from a reason and message.
   * <p>
   * The message is produced using a template that contains zero to many "{}" placeholders.
   * Each placeholder is replaced by the next available argument.
   * If there are too few arguments, then the message will be left with placeholders.
   * If there are too many arguments, then the excess arguments are appended to the
   * end of the message. No attempt is made to format the arguments.
   * See {@link Messages#format(String, Object...)} for more details.
   * <p>
   * An exception will be created internally to obtain a stack trace.
   * The cause type will not be present in the resulting failure.
   * 
   * @param reason  the reason
   * @param message  a message explaining the failure, not empty, uses "{}" for inserting {@code messageArgs}
   * @param messageArgs  the arguments for the message
   * @return the failure
   */
  public static Failure of(FailureReason reason, String message, Object... messageArgs) {
    return Failure.of(FailureItem.ofAutoStackTrace(1, reason, message, messageArgs));
  }

  /**
   * Obtains a failure from a reason, message and throwable.
   * <p>
   * The message is produced using a template that contains zero to many "{}" placeholders.
   * Each placeholder is replaced by the next available argument.
   * If there are too few arguments, then the message will be left with placeholders.
   * If there are too many arguments, then the excess arguments are appended to the
   * end of the message. No attempt is made to format the arguments.
   * See {@link Messages#format(String, Object...)} for more details.
   * 
   * @param reason  the reason
   * @param cause  the cause
   * @param message  the failure message, possibly containing placeholders, formatted using {@link Messages#format}
   * @param messageArgs  arguments used to create the failure message
   * @return the failure
   */
  public static Failure of(FailureReason reason, Throwable cause, String message, Object... messageArgs) {
    return Failure.of(FailureItem.of(reason, cause, message, messageArgs));
  }

  /**
   * Obtains a failure from a reason, message and exception.
   * <p>
   * The message is produced using a template that contains zero to many "{}" placeholders.
   * Each placeholder is replaced by the next available argument.
   * If there are too few arguments, then the message will be left with placeholders.
   * If there are too many arguments, then the excess arguments are appended to the
   * end of the message. No attempt is made to format the arguments.
   * See {@link Messages#format(String, Object...)} for more details.
   * 
   * @param reason  the reason
   * @param cause  the cause
   * @param message  the failure message, possibly containing placeholders, formatted using {@link Messages#format}
   * @param messageArgs  arguments used to create the failure message
   * @return the failure
   */
  public static Failure of(FailureReason reason, Exception cause, String message, Object... messageArgs) {
    // this method is retained to ensure binary compatibility
    return Failure.of(FailureItem.of(reason, cause, message, messageArgs));
  }

  /**
   * Obtains a failure from a reason and throwable.
   * 
   * @param reason  the reason
   * @param cause  the cause
   * @return the failure
   */
  public static Failure of(FailureReason reason, Throwable cause) {
    return Failure.of(FailureItem.of(reason, cause));
  }

  /**
   * Obtains a failure from a reason and exception.
   * 
   * @param reason  the reason
   * @param cause  the cause
   * @return the failure
   */
  public static Failure of(FailureReason reason, Exception cause) {
    // this method is retained to ensure binary compatibility
    return Failure.of(FailureItem.of(reason, cause));
  }

  /**
   * Obtains a failure for a single failure item.
   * 
   * @param item  the failure item
   * @return the failure
   */
  public static Failure of(FailureItem item) {
    return new Failure(item.getReason(), item.getMessage(), ImmutableSet.of(item));
  }

  /**
   * Obtains a failure for multiple failure items.
   *
   * @param item  the first failure item
   * @param additionalItems  additional failure items
   * @return the failure
   */
  public static Failure of(FailureItem item, FailureItem... additionalItems) {
    return of(ImmutableSet.<FailureItem>builder().add(item).add(additionalItems).build());
  }

  /**
   * Obtains a failure for a non-empty collection of failure items.
   * 
   * @param items  the failures, not empty
   * @return the failure
   */
  public static Failure of(Collection<FailureItem> items) {
    ArgChecker.notEmpty(items, "items");
    Set<FailureItem> itemSet = ImmutableSet.copyOf(items);
    String message = itemSet.stream()
        .map(FailureItem::getMessage)
        .collect(Collectors.joining(", "));
    FailureReason reason = itemSet.stream()
        .map(FailureItem::getReason)
        .reduce((s1, s2) -> s1 == s2 ? s1 : FailureReason.MULTIPLE).get();
    return new Failure(reason, message, itemSet);
  }

  /**
   * Creates a failure from the throwable.
   * <p>
   * This recognizes {@link FailureException} and {@link FailureItemProvider}.
   *
   * @param th  the throwable to be processed
   * @return the failure
   */
  public static Failure from(Throwable th) {
    try {
      throw th;
    } catch (FailureException ex) {
      return ex.getFailure();
    } catch (Throwable ex) {
      if (ex instanceof FailureItemProvider) {
        return of(((FailureItemProvider) ex).getFailureItem());
      }
      return of(FailureReason.ERROR, ex);
    }
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the first failure item.
   * <p>
   * There will be at least one failure item, so this always succeeds.
   * 
   * @return the failure item
   */
  public FailureItem getFirstItem() {
    return items.iterator().next();
  }

  /**
   * Processes the failure by applying a function that alters the items.
   * <p>
   * This operation allows wrapping a failure item with additional information that may have not been available
   * to the code that created the original failure.
   *
   * @param function  the function to transform the failure items with
   * @return the transformed instance
   */
  public Failure mapItems(Function<FailureItem, FailureItem> function) {
    return new Failure(reason, message, items.stream().map(function).collect(toImmutableSet()));
  }

  //------------------------- AUTOGENERATED START -------------------------
  /**
   * The meta-bean for {@code Failure}.
   * @return the meta-bean, not null
   */
  public static Failure.Meta meta() {
    return Failure.Meta.INSTANCE;
  }

  static {
    MetaBean.register(Failure.Meta.INSTANCE);
  }

  /**
   * The serialization version id.
   */
  private static final long serialVersionUID = 1L;

  private Failure(
      FailureReason reason,
      String message,
      Set<FailureItem> items) {
    JodaBeanUtils.notNull(reason, "reason");
    JodaBeanUtils.notEmpty(message, "message");
    JodaBeanUtils.notEmpty(items, "items");
    this.reason = reason;
    this.message = message;
    this.items = ImmutableSet.copyOf(items);
  }

  @Override
  public Failure.Meta metaBean() {
    return Failure.Meta.INSTANCE;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the reason associated with the failure.
   * @return the value of the property, not null
   */
  public FailureReason getReason() {
    return reason;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the error message associated with the failure.
   * @return the value of the property, not empty
   */
  public String getMessage() {
    return message;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the set of failure items.
   * There will be at least one failure item.
   * @return the value of the property, not empty
   */
  public ImmutableSet<FailureItem> getItems() {
    return items;
  }

  //-----------------------------------------------------------------------
  @Override
  public boolean equals(Object obj) {
    if (obj == this) {
      return true;
    }
    if (obj != null && obj.getClass() == this.getClass()) {
      Failure other = (Failure) obj;
      return JodaBeanUtils.equal(reason, other.reason) &&
          JodaBeanUtils.equal(message, other.message) &&
          JodaBeanUtils.equal(items, other.items);
    }
    return false;
  }

  @Override
  public int hashCode() {
    int hash = getClass().hashCode();
    hash = hash * 31 + JodaBeanUtils.hashCode(reason);
    hash = hash * 31 + JodaBeanUtils.hashCode(message);
    hash = hash * 31 + JodaBeanUtils.hashCode(items);
    return hash;
  }

  @Override
  public String toString() {
    StringBuilder buf = new StringBuilder(128);
    buf.append("Failure{");
    buf.append("reason").append('=').append(JodaBeanUtils.toString(reason)).append(',').append(' ');
    buf.append("message").append('=').append(JodaBeanUtils.toString(message)).append(',').append(' ');
    buf.append("items").append('=').append(JodaBeanUtils.toString(items));
    buf.append('}');
    return buf.toString();
  }

  //-----------------------------------------------------------------------
  /**
   * The meta-bean for {@code Failure}.
   */
  public static final class Meta extends DirectMetaBean {
    /**
     * The singleton instance of the meta-bean.
     */
    static final Meta INSTANCE = new Meta();

    /**
     * The meta-property for the {@code reason} property.
     */
    private final MetaProperty<FailureReason> reason = DirectMetaProperty.ofImmutable(
        this, "reason", Failure.class, FailureReason.class);
    /**
     * The meta-property for the {@code message} property.
     */
    private final MetaProperty<String> message = DirectMetaProperty.ofImmutable(
        this, "message", Failure.class, String.class);
    /**
     * The meta-property for the {@code items} property.
     */
    @SuppressWarnings({"unchecked", "rawtypes" })
    private final MetaProperty<ImmutableSet<FailureItem>> items = DirectMetaProperty.ofImmutable(
        this, "items", Failure.class, (Class) ImmutableSet.class);
    /**
     * The meta-properties.
     */
    private final Map<String, MetaProperty<?>> metaPropertyMap$ = new DirectMetaPropertyMap(
        this, null,
        "reason",
        "message",
        "items");

    /**
     * Restricted constructor.
     */
    private Meta() {
    }

    @Override
    protected MetaProperty<?> metaPropertyGet(String propertyName) {
      switch (propertyName.hashCode()) {
        case -934964668:  // reason
          return reason;
        case 954925063:  // message
          return message;
        case 100526016:  // items
          return items;
      }
      return super.metaPropertyGet(propertyName);
    }

    @Override
    public BeanBuilder<? extends Failure> builder() {
      return new Failure.Builder();
    }

    @Override
    public Class<? extends Failure> beanType() {
      return Failure.class;
    }

    @Override
    public Map<String, MetaProperty<?>> metaPropertyMap() {
      return metaPropertyMap$;
    }

    //-----------------------------------------------------------------------
    /**
     * The meta-property for the {@code reason} property.
     * @return the meta-property, not null
     */
    public MetaProperty<FailureReason> reason() {
      return reason;
    }

    /**
     * The meta-property for the {@code message} property.
     * @return the meta-property, not null
     */
    public MetaProperty<String> message() {
      return message;
    }

    /**
     * The meta-property for the {@code items} property.
     * @return the meta-property, not null
     */
    public MetaProperty<ImmutableSet<FailureItem>> items() {
      return items;
    }

    //-----------------------------------------------------------------------
    @Override
    protected Object propertyGet(Bean bean, String propertyName, boolean quiet) {
      switch (propertyName.hashCode()) {
        case -934964668:  // reason
          return ((Failure) bean).getReason();
        case 954925063:  // message
          return ((Failure) bean).getMessage();
        case 100526016:  // items
          return ((Failure) bean).getItems();
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
   * The bean-builder for {@code Failure}.
   */
  private static final class Builder extends DirectPrivateBeanBuilder<Failure> {

    private FailureReason reason;
    private String message;
    private Set<FailureItem> items = ImmutableSet.of();

    /**
     * Restricted constructor.
     */
    private Builder() {
    }

    //-----------------------------------------------------------------------
    @Override
    public Object get(String propertyName) {
      switch (propertyName.hashCode()) {
        case -934964668:  // reason
          return reason;
        case 954925063:  // message
          return message;
        case 100526016:  // items
          return items;
        default:
          throw new NoSuchElementException("Unknown property: " + propertyName);
      }
    }

    @SuppressWarnings("unchecked")
    @Override
    public Builder set(String propertyName, Object newValue) {
      switch (propertyName.hashCode()) {
        case -934964668:  // reason
          this.reason = (FailureReason) newValue;
          break;
        case 954925063:  // message
          this.message = (String) newValue;
          break;
        case 100526016:  // items
          this.items = (Set<FailureItem>) newValue;
          break;
        default:
          throw new NoSuchElementException("Unknown property: " + propertyName);
      }
      return this;
    }

    @Override
    public Failure build() {
      return new Failure(
          reason,
          message,
          items);
    }

    //-----------------------------------------------------------------------
    @Override
    public String toString() {
      StringBuilder buf = new StringBuilder(128);
      buf.append("Failure.Builder{");
      buf.append("reason").append('=').append(JodaBeanUtils.toString(reason)).append(',').append(' ');
      buf.append("message").append('=').append(JodaBeanUtils.toString(message)).append(',').append(' ');
      buf.append("items").append('=').append(JodaBeanUtils.toString(items));
      buf.append('}');
      return buf.toString();
    }

  }

  //-------------------------- AUTOGENERATED END --------------------------
}
