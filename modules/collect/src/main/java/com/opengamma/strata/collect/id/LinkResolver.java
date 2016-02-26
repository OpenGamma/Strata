/**
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.collect.id;

import static com.opengamma.strata.collect.Guavate.toImmutableList;

import java.util.List;
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
   * Obtains a link resolver that is unable to resolve any links.
   * <p>
   * This is a special implementation of {@code LinkResolver} that will be used when
   * it is assumed that all targets have already been resolved.
   * Any attempt to resolve a link will throw {@code LinkResolutionException}
   *
   * @return the link resolver
   */
  public static LinkResolver none() {
    // cannot use a lambda for LinkResolver
    return new LinkResolver() {
      @Override
      public <T extends IdentifiableBean> T resolve(StandardId identifier, TypeToken<T> targetType) {
        throw new LinkResolutionException("Unable to resolve link to: " + identifier + ", using LinkResolver.none()");
      }
    };
  }

  //-------------------------------------------------------------------------
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
   * <pre>
   *  new TypeToken&lt;Trade&lt;Swap&gt;&gt;() {};
   * </pre>
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
   * Resolves all the links within the specified list of beans.
   * <p>
   * This takes the specified list of beans and resolves any links.
   * Each bean in the list is checked to see if it implements {@link LinkResolvable}.
   * If the target is not resolvable, or the target is already resolved,
   * then the input bean will be returned.
   * <p>
   * This method is primarily useful where the type of the input objects is not known to be resolvable.
   * For example, this might occur when processing a {@code List<Object>}.
   * 
   * @param <B>  the type of the beans
   * @param beans  the list of target beans
   * @return a new list of resolved beans
   */
  public default <B> List<B> resolveLinksIn(List<B> beans) {
    return beans.stream()
        .map(this::resolveLinksIn)
        .collect(toImmutableList());
  }

  /**
   * Resolves all the links within the specified bean.
   * <p>
   * This takes the specified bean and resolves any links if the object implements {@link LinkResolvable}.
   * If the target is not resolvable, or the target is already resolved,
   * then the specified input bean will be returned.
   * <p>
   * This method is primarily useful where the type of the input object is not known to be resolvable.
   * For example, this might occur when processing a {@code List<Object>}.
   * 
   * @param <B>  the type of the bean
   * @param bean  the target bean
   * @return the resolved bean
   */
  @SuppressWarnings("unchecked")
  public default <B> B resolveLinksIn(B bean) {
    return (bean instanceof LinkResolvable ? ((LinkResolvable<B>) bean).resolveLinks(this) : bean);
  }

  /**
   * Resolves all the links within one property of a bean.
   * <p>
   * This takes the specified bean and replaces the target object.
   * The target must be a property of the bean and the update function must be able to replace the target.
   * The update must return a new bean, leaving the original unaltered.
   * <p>
   * If the target is not resolvable, is null, or is already resolved,
   * then the specified input bean will be returned.
   * <p>
   * For example, this method might be used as follows:
   * <pre>
   *  resolver.resolveLinksIn(bean, bean.getFoo(), resolved -> bean.toBuilder().foo(resolved).build());
   * </pre>
   * <p>
   * This method is typically invoked from implementations of {@link LinkResolvable#resolveLinks(LinkResolver)}.
   * In that case, the above example would use {@code this} instead of {@code bean}.
   * 
   * @param <B>  the type of the beans
   * @param <T>  the type of the target within the bean
   * @param bean  the target bean
   * @param target  the target object within the bean, may be null
   * @param updateFn  the update function
   * @return the updated bean
   */
  public default <B, T> B resolveLinksIn(B bean, T target, Function<T, B> updateFn) {
    if (target instanceof LinkResolvable) {
      @SuppressWarnings("unchecked")
      LinkResolvable<T> resolvableTarget = (LinkResolvable<T>) target;
      T resolved = resolvableTarget.resolveLinks(this);
      if (resolved != target) {
        return updateFn.apply(resolved);
      }
    }
    return bean;
  }

}
