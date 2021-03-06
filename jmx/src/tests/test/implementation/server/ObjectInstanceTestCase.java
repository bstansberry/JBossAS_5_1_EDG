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
package test.implementation.server;

import junit.framework.TestCase;

import test.implementation.server.support.Trivial;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import javax.management.MBeanServer;
import javax.management.MBeanServerFactory;
import javax.management.ObjectName;
import javax.management.ObjectInstance;

import org.jboss.mx.server.ServerObjectInstance;

/**
 * Tests the ObjectInstance handling which is a bit brain-dead in the RI.<p>
 *
 * Maybe one-day these will be part of the compliance testsuite.
 *
 * @author  <a href="mailto:Adrian.Brock@HappeningTimes.com">Adrian Brock</a>.
 */
public class ObjectInstanceTestCase
  extends TestCase
{
   // Attributes ----------------------------------------------------------------

   // Constructor ---------------------------------------------------------------

   /**
    * Construct the test
    */
   public ObjectInstanceTestCase(String s)
   {
      super(s);
   }

   // Tests that should work in the RI ------------------------------------------

   /**
    * Test default domain
    */
   public void testDefaultDomain()
   {
      MBeanServer server =null;
      ObjectName unqualifiedName = null;
      ObjectName qualifiedName = null;
      ObjectInstance instance1 = null;
      ObjectInstance instance2 = null;
      try
      {
         server = MBeanServerFactory.createMBeanServer();
         unqualifiedName = new ObjectName(":property=1");
         qualifiedName = new ObjectName("DefaultDomain:property=1");
         instance1 = server.registerMBean(new Trivial(), qualifiedName);
         instance2 = server.getObjectInstance(unqualifiedName);
      }
      catch (Exception e)
      {
         fail(e.toString());
      }

      assertEquals(instance1.getObjectName(),qualifiedName);
      assertEquals(instance1, instance2);

      if (server != null)
         MBeanServerFactory.releaseMBeanServer(server);
   }

   /**
    * Test different servers
    */
   public void testDifferentServers()
   {
      MBeanServer server =null;
      ObjectName name = null;
      ObjectInstance instance1 = null;
      ObjectInstance instance2 = null;
      try
      {
         server = MBeanServerFactory.createMBeanServer();
         name = new ObjectName(":property=1");
         instance1 = server.registerMBean(new Trivial(), name);
         MBeanServerFactory.releaseMBeanServer(server);
         server = MBeanServerFactory.createMBeanServer();
         instance2 = server.registerMBean(new Trivial(), name);
      }
      catch (Exception e)
      {
         fail(e.toString());
      }

      if (instance1.equals(instance2) == true)
         fail("Instances in different servers are the same");

      if (server != null)
         MBeanServerFactory.releaseMBeanServer(server);
   }

   // Tests that need to work in JBossMX  because of the extra agent id --------

   /**
    * Test ObjectInstance/ServerObjectInstance Equals
    */
   public void testEquals()
   {
      // Create the object instances
      ObjectInstance oi = null;
      ServerObjectInstance soi = null;
      ObjectName name = null;
      String className = "org.jboss.AClass";
      try
      {
         name = new ObjectName(":a=a");
         oi = new ObjectInstance(name, className);
         soi = new ServerObjectInstance(name, className, "agentid");
      }
      catch (Exception e)
      {
        fail(e.toString());
      }
      assertEquals(oi, soi);
   }
   /**
    * Test serialization. For moving between implementations, this HAS
    * to produce an ObjectInstance.
    */
   public void testSerialization()
   {
      // Create the new object Instance
      ServerObjectInstance original = null;
      ObjectInstance result = null;
      ObjectName name = null;
      String className = "org.jboss.AClassName";
      try
      {
         name = new ObjectName(":a=a");
         original = new ServerObjectInstance(name, className, "agentid");
      }
      catch (Exception e)
      {
         fail(e.toString());
      }

      try
      {
         // Serialize it
         ByteArrayOutputStream baos = new ByteArrayOutputStream();
         ObjectOutputStream oos = new ObjectOutputStream(baos);
         oos.writeObject(original);
    
         // Deserialize it
         ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());
         ObjectInputStream ois = new ObjectInputStream(bais);
         result = (ObjectInstance) ois.readObject();
      }
      catch (IOException ioe)
      {
         fail(ioe.toString());
      }
      catch (ClassNotFoundException cnfe)
      {
         fail(cnfe.toString());
      }

      // Did it work?
      assertEquals("javax.management.ObjectInstance", result.getClass().getName());
      assertEquals(name, result.getObjectName());
      assertEquals(className, result.getClassName());
   }
}
