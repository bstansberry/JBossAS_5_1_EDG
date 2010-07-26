/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2008, Red Hat Middleware LLC, and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.jboss.test.cmp2.fkmapping.ejb;


import javax.ejb.EntityBean;
import javax.ejb.EntityContext;
import javax.ejb.RemoveException;
import javax.ejb.CreateException;


/**
 * @ejb.bean
 *    name="Child"
 *    type="CMP"
 *    cmp-version="2.x"
 *    view-type="local"
 *    reentrant="false"
 *    primkey-field="id"
 * @ejb.pk generate="false"
 * @ejb.util  generate="physical"
 * @ejb.persistence  table-name="CHILD"
 * @jboss.persistence
 *    create-table="true"
 *    remove-table="true"
 * @ejb:transaction-type  type="Container"
 * @jboss.container-configuration name="INSERT after ejbPostCreate Container"
 *
 * @author <a href="mailto:alex@jboss.org">Alex Loubyansky</a>
 */
public abstract class ChildCMPBean
   implements EntityBean
{
   // Attributes -----------------------------------------------
   private EntityContext ctx;

   // CMP accessors --------------------------------------------
   /**
    * @ejb.pk-field
    * @ejb.persistent-field
    * @ejb.interface-method
    * @ejb.persistence  column-name="CHILD_ID"
    */
   public abstract Long getId();
   public abstract void setId(Long id);

   /**
    * @ejb.persistent-field
    * @ejb.interface-method
    * @ejb.persistence column-name="FIRST_NAME"
    */
   public abstract String getFirstName();
   public abstract void setFirstName(String name);

   /**
    * Non-null CMP field mapped to the foreign key field
    * Used as a read-only field to verify correctness of INSERT
    *
    * @ejb.persistent-field
    * @ejb.interface-method
    * @ejb.persistence column-name="MOTHER_ID"
    * @jboss.persistence not-null="true"
    */
   public abstract Long getMotherId();
   public abstract void setMotherId(Long id);

   /**
    * Non-null CMP field mapped to the foreign key field
    * Used as a read-only field to verify correctness of INSERT
    *
    * @ejb.persistent-field
    * @ejb.interface-method
    * @ejb.persistence column-name="MOTHER_NAME"
    * @jboss.persistence not-null="true"
    */
   public abstract String getMotherName();
   public abstract void setMotherName(String name);

   /**
    * @ejb.interface-method
    * @ejb.relation
    *    name="Mother-Child"
    *    role-name="Child-has-Mother"
    *    target-ejb="Parent"
    *    target-role-name="Mother-has-Child"
    *    target-multiple="yes"
    *    cascade-delete="no"
    * @jboss.relation
    *    related-pk-field="id"
    *    fk-column="MOTHER_ID"
    * @jboss.relation
    *    related-pk-field="firstName"
    *    fk-column="MOTHER_NAME"
    */
   public abstract ParentLocal getMother();
   /**
    * @ejb.interface-method
    */
   public abstract void setMother(ParentLocal parent);

   // EntityBean implementation -------------------------------------
   /**
    * @ejb.create-method
    */
   public Long ejbCreate(Long childId, String firstName)
      throws CreateException
   {
      setId(childId);
      setFirstName(firstName);
      return null;
   }

   public void ejbPostCreate(Long id, String firstName) {}

   /**
    * @ejb.create-method
    */
   public Long ejbCreate(Long childId, String firstName, Long parentId, String parentName)
      throws CreateException
   {
      setId(childId);
      setFirstName(firstName);
      return null;
   }

   public void ejbPostCreate(Long id, String firstName, Long parentId, String parentName)
      throws CreateException
   {
      ParentLocal parent = null;
      try
      {
         // this will trigger synchronization with the store and this, not yet
         // created, child should not be updated
         parent = ParentUtil.getLocalHome().findByPrimaryKey(new ParentPK(parentId, parentName));
      }
      catch(Exception e)
      {
         throw new CreateException("Could not create relationship: " + e.getMessage());
      }

      setMother(parent);

      if(!id.equals(ctx.getPrimaryKey()))
         throw new IllegalStateException("Primary key is not available in ejbPostCreate.");

      if(ctx.getEJBLocalObject() == null)
         throw new IllegalStateException("Local object is not available in ejbPostCreate.");
   }

   /**
    * @param  ctx The new entityContext value
    */
   public void setEntityContext(EntityContext ctx)
   {
      this.ctx = ctx;
   }

   /**
    * Unset the associated entity context.
    */
   public void unsetEntityContext()
   {
      this.ctx = null;
   }

   public void ejbActivate() {}
   public void ejbLoad() {}
   public void ejbPassivate() {}
   public void ejbRemove() throws RemoveException {}
   public void ejbStore() {}
}
