/**
 * User: alesj
 * Date: 18.4.2006
 * Time: 12:42:33
 * 
 * (C) Genera Lynx d.o.o.
 */

package org.jboss.spring.kernel;

/**
 * @author <a href="mailto:ales.justin@genera-lynx.com">Ales Justin</a>
 */
public class NullLocator implements Locator
{

   public Object locateBean(String beanName)
   {
      return null;
   }

}
