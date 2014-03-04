package ixa.pipe.convert;



/**
 * Filter is an interface for predicate objects which respond to the
 * <code>accept</code> method.
 *
 */
public interface Filter <T>  {

  /**
   * Checks if the given object passes the filter.
   *
   * @param obj an object to test
   * @return Whether the object should be accepted (for some processing)
   */
  public boolean accept(T obj);

}