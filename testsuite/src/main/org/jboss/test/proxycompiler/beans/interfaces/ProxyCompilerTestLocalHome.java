/*
 * Generated by XDoclet - Do not edit!
 */
package org.jboss.test.proxycompiler.beans.interfaces;

/**
 * Local home interface for ProxyCompilerTest.
 */
public interface ProxyCompilerTestLocalHome
   extends javax.ejb.EJBLocalHome
{
   public static final String COMP_NAME="java:comp/env/ejb/ProxyCompilerTestLocal";
   public static final String JNDI_NAME="LocalProxyCompilerTest";

   public org.jboss.test.proxycompiler.beans.interfaces.ProxyCompilerTestLocal create(java.lang.Integer pk)
      throws javax.ejb.CreateException;

   public java.util.Collection findAll()
      throws javax.ejb.FinderException;

   public org.jboss.test.proxycompiler.beans.interfaces.ProxyCompilerTestLocal findByPrimaryKey(java.lang.Integer pk)
      throws javax.ejb.FinderException;

}
