package org.ambraproject.wombat.config.site;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableTable;

/**
 * A global bean that looks up patterns that have been mapped to request handlers. Ordinarily those patterns are set up
 * as part of Spring configuration; we want to capture those patterns in order to use them later, for building links.
 * <p/>
 * This bean has two roles: intercepting the patterns before they get passed to Spring config (which, as far as we know,
 * doesn't have any built-in way to extract them later), and looking them up later. It would be nice if we could have
 * one bean to do the first job, which produces another, immutable bean to do the second job when it's done.
 * Unfortunately, we're not aware of a Spring hook that allows us to set up a separate bean only after all request
 * handlers have been mapped. For the next-best thing, we statefully freeze this object the first time it is read and
 * throw an {@code IllegalStateException} rather than allow any further writes, making it effectively immutable.
 * <p/>
 * This class is intended to be thread-safe. Writes are synchronized, and the object is immutable while being read.
 */
public final class RequestMappingContextDictionary {

  // While false, this object is in a state to accept writes. Permanently set to true on first call to getPattern.
  private boolean isFrozen = false;

  private final Object writeLock = new Object();

  private final ImmutableTable.Builder<String, Site, RequestMappingContext> siteTableBuilder;
  private final ImmutableMap.Builder<String, RequestMappingContext> globalTableBuilder;

  // By convention, assign to each of these fields only once, when isFrozen is set to true.
  private ImmutableTable<String, Site, RequestMappingContext> siteTable = null;
  private ImmutableMap<String, RequestMappingContext> globalTable = null;

  public RequestMappingContextDictionary() {
    siteTableBuilder = ImmutableTable.builder();
    globalTableBuilder = ImmutableMap.builder();
  }

  private static String getHandlerName(RequestMappingContext mapping) {
    return Preconditions.checkNotNull(mapping.getAnnotation().name());
  }

  /**
   * Register the pattern that is associated with a handler on a particular site.
   * <p/>
   * All registrations must be completed before the first call to {@link #getPattern}.
   *
   * @param mapping the mapping to the handler
   * @param site    the site associated with the handler
   * @throws IllegalStateException if {@link #getPattern} has been called once or more on this object
   */
  public void registerSiteMapping(RequestMappingContext mapping, Site site) {
    Preconditions.checkNotNull(site);
    Preconditions.checkArgument(!mapping.isSiteless());
    String handlerName = getHandlerName(mapping);

    synchronized (writeLock) {
      if (isFrozen) {
        throw new IllegalStateException("Cannot register more methods after directory has been read");
      }
      siteTableBuilder.put(handlerName, site, mapping);
    }
  }

  /**
   * Register the pattern that is associated with a siteless handler.
   * <p/>
   * All registrations must be completed before the first call to {@link #getPattern}.
   *
   * @param mapping the mapping to the handler
   * @throws IllegalStateException if {@link #getPattern} has been called once or more on this object
   */
  public void registerGlobalMapping(RequestMappingContext mapping) {
    Preconditions.checkArgument(mapping.isSiteless());
    String handlerName = getHandlerName(mapping);

    synchronized (writeLock) {
      if (isFrozen) {
        throw new IllegalStateException("Cannot register more methods after directory has been read");
      }
      globalTableBuilder.put(handlerName, mapping);
    }
  }

  /**
   * Look up a registered pattern.
   * <p/>
   * This method should only be called when all registrations are complete. The first time this method is called for
   * this object, it has the side effect of freezing the object and invalidating any future calls to {@link
   * #registerSiteMapping} and {@link #registerGlobalMapping}.
   *
   * @param handlerName the name of the handler
   * @param site        the site associated with the request to map
   * @return the pattern, or {@code null} if no handler exists for the given name on the given site
   */
  public RequestMappingContext getPattern(String handlerName, Site site) {
    /*
     * We permit a harmless race condition on the `if (!isFrozen)` block: if the first two calls to this method happen
     * concurrently, we might invoke the builders more than once. This is safe because they will return identical data.
     */
    if (!isFrozen) {
      synchronized (writeLock) {
        isFrozen = true;
        siteTable = siteTableBuilder.build();
        globalTable = globalTableBuilder.build();
      }
    }

    Preconditions.checkNotNull(handlerName);
    Preconditions.checkNotNull(site);
    RequestMappingContext siteMapping = siteTable.get(handlerName, site);
    return (siteMapping != null) ? siteMapping : globalTable.get(handlerName);
  }

}
