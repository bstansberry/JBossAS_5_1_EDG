/*
 * Generated by XDoclet - Do not edit!
 */
package org.jboss.test.cmp2.fkmapping.ejb;

/**
 * Local home interface for Parent.
 */
public interface ParentLocalHome
   extends javax.ejb.EJBLocalHome
{
   public static final String COMP_NAME="java:comp/env/ejb/ParentLocal";
   public static final String JNDI_NAME="ParentLocal";

   public org.jboss.test.cmp2.fkmapping.ejb.ParentLocal create(java.lang.Long parentId , java.lang.String firstName)
      throws javax.ejb.CreateException;

   public org.jboss.test.cmp2.fkmapping.ejb.ParentLocal findByPrimaryKey(org.jboss.test.cmp2.fkmapping.ejb.ParentPK pk)
      throws javax.ejb.FinderException;

}
