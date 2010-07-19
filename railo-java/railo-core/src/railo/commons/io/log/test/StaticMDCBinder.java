package railo.commons.io.log.test;

import org.slf4j.helpers.BasicMDCAdapter;
import org.slf4j.spi.MDCAdapter;

import railo.print;


/**
 * This implementation is bound to {@link BasicMDCAdapter}.
 *
 * @author Ceki G&uuml;lc&uuml;
 */
public class StaticMDCBinder {

  
  /**
   * The unique instance of this class.
   */
  public static final StaticMDCBinder SINGLETON = new StaticMDCBinder();

  private StaticMDCBinder() {
  }
  
  /**
   * Currently this method always returns an instance of 
   * {@link BasicMDCAdapter}.
   */
  public MDCAdapter getMDCA() {
	  //print.o("getMDCA:"+BasicMDCAdapter.class.getName());
	  // note that this method is invoked only from within the static initializer of 
    // the org.slf4j.MDC class.
    return new BasicMDCAdapter();
  }
  
  public String  getMDCAdapterClassStr() {
	  //print.o("getMDCAdapterClassStr:"+BasicMDCAdapter.class.getName());
    return BasicMDCAdapter.class.getName();
  }
}
