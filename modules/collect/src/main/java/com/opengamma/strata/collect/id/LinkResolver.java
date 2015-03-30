/**
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.collect.id;

import java.util.function.Function;

import com.google.common.reflect.TypeToken;

/**
 * A resolver that can provide the target when resolving links.
 * <p>
 * A {@link Link} provides loose coupling between different parts of the object model.
 * When the target of a link is needed, it is resolved by passing in a link resolver.
 * <p>
 * Link resolution will typically be implemented to access an underlying data store.
 * If the link specifies an identifier that is not resolvable, or the declared target
 * type is incorrect, an exception is thrown.
 */
public interface LinkResolver {

  /**
   * Resolves the supplied link, returning the realized target of the link.
   * <p>
   * The implementation of this interface may perform any thread-safe action to obtain
   * the link target. Typically this will involve accessing an underlying data store.
   * If the link cannot be resolved then a {@code LinkResolutionException} will be thrown.
   * <p>
   * The type is expressed as a standard {@link Class} object.
   *
   * @param <T>  the type of the target of the link
   * @param identifier  the identifier to be resolved
   * @param targetType  the target type of the link
   * @return the resolved target of the link
   * @throws LinkResolutionException if the link cannot be resolved
   */
  public default <T extends IdentifiableBean> T resolve(StandardId identifier, Class<T> targetType) {
    return resolve(identifier, TypeToken.of(targetType));
  }

  /**
   * Resolves the supplied link, returning the realized target of the link.
   * <p>
   * The implementation of this interface may perform any thread-safe action to obtain
   * the link target. Typically this will involve accessing an underlying data store.
   * If the link cannot be resolved then a {@code LinkResolutionException} will be thrown.
   * <p>
   * The type is expressed as a {@link TypeToken}, which allows types like
   * {@code Trade<Swap>} to be expressed:
   * <p>
   * <pre>{@code
   *  new TypeToken<Trade<Swap>>() {};
   * }</pre>
   *
   * @param <T>  the type of the target of the link
   * @param identifier  the identifier to be resolved
   * @param targetType  the target type of the link
   * @return the resolved target of the link
   * @throws LinkResolutionException if the link cannot be resolved
   */
  public abstract <T extends IdentifiableBean> T resolve(StandardId identifier, TypeToken<T> targetType);

  //-------------------------------------------------------------------------
  /**
   * Resolves all the links within one property of a bean.
   * <p>
   * This takes the specified bean and replaces the target object.
   * The target must be a property of the bean and the update function must be able to replace the target.
   * The update must return a new bean, leaving the original unaltered.
   * <p>
   * If the target is not resolvable, or the target is already resolved,
   * then the specified input bean will be returned.
   * <p>
   * For example, this method might be used as follows:
   * <pre>
   *  resolver.resolveLinksIn(bean, bean.getFoo(), resolved -> bean.toBuilder().foo(resolved).build());
   * </pre>
   * <p>
   * This method is typically invoked from implementations of {@link Resolvable#resolveLinks(LinkResolver)}.
   * In that case, the above example would use {@code this} instead of {@code bean}.
   * 
   * @param bean  the target bean
   * @param target  the target object within the bean
   * @param updateFn  the update function
   * @return the updated bean
   */
  public default <B, T> B resolveLinksIn(B bean, T target, Function<T, B> updateFn) {
    if (target instanceof Resolvable) {
      @SuppressWarnings("unchecked")
      Resolvable<T> resolvableTarget = (Resolvable<T>) target;
      T resolved = resolvableTarget.resolveLinks(this);
      if (resolved != target) {
        return updateFn.apply(resolved);
      }
    }
    return bean;
  }

}
