/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2006, Red Hat Middleware LLC, and individual contributors
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
package org.jboss.test.naming.test;

import java.util.Properties;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;

import org.jboss.test.JBossTestCase;

/** 
 * Tests of HA-JNDI auto-discovery.
 */
public class HAJndiAutoDiscoveryUnitTestCase extends JBossTestCase
{
   private static final String DISCOVERY_TTL = System.getProperty("jbosstest.udp.ip_ttl", "1");
   private static final String DISCOVERY_GROUP = System.getProperty("jbosstest.udpGroup", "");
   
   /**
    * Constructor for the SimpleUnitTestCase object
    *
    * @param name  Test name
    */
   public HAJndiAutoDiscoveryUnitTestCase(String name)
   {
      super(name);
   }

   /** Test discovery with the partition name specified
    *
    * @throws Exception
    */
   public void testHaPartitionName() throws Exception
   {
      getLog().debug("+++ testHaPartitionName");
      Properties env = new Properties();
      String serverHost = getServerHost();
      env.setProperty(Context.PROVIDER_URL, "jnp://" + serverHost + ":65535/");
      env.setProperty("jnp.localAddress", serverHost);      
      env.setProperty("jnp.partitionName", "DefaultPartition");
      // Don't let the discovery packet off the test server so we don't
      // get spurious responses from other servers on the network
      env.setProperty("jnp.discoveryTTL", DISCOVERY_TTL);
      if (DISCOVERY_GROUP != null && "".equals(DISCOVERY_GROUP) == false)
      {
         // Server is not listening for discovery on std multicast address
         // so we need to use the correct one
         env.setProperty("jnp.discoveryGroup", DISCOVERY_GROUP);
      }
      getLog().debug("Creating InitialContext with env="+env);
      InitialContext ctx = new InitialContext(env);
      getLog().debug("Created InitialContext");
      Object obj = ctx.lookup("invokers");
      getLog().debug("lookup(invokers) : "+obj);
      Context invokersCtx = (Context) obj;
      NamingEnumeration list = invokersCtx.list("");
      while( list.hasMore() )
      {
         Object entry = list.next();
         getLog().debug(" + "+entry);
      }
      ctx.close();

      // Now test discovery with a non-existent partition name
      env.setProperty(Context.PROVIDER_URL, "jnp://" + getServerHost() + ":65535/");
      env.setProperty("jnp.partitionName", "__NotTheDefaultPartition__");
      try
      {
         ctx = new InitialContext(env);
         getLog().debug("Created InitialContext");
         obj = ctx.lookup("invokers");
         fail("Was able to lookup(invokers): "+obj);
      }
      catch(NamingException e)
      {
         getLog().debug("Partition specific discovery failed as expected", e);
      }
   }

   /** Test naming discovery with an explicit port
    * 
    * @throws Exception
    */ 
   public void testDiscoveryPort() throws Exception
   {
      getLog().debug("+++ testDiscoveryPort");
      Properties env = new Properties();
      String serverHost = getServerHost();
      env.setProperty(Context.PROVIDER_URL, "jnp://" + serverHost + ":65535/");
      env.setProperty("jnp.localAddress", serverHost);      
      env.setProperty("jnp.discoveryPort", "1102");
      // Don't let the discovery packet off the test server so we don't
      // get spurious responses from other servers on the network
      env.setProperty("jnp.discoveryTTL", DISCOVERY_TTL);
      if (DISCOVERY_GROUP != null && "".equals(DISCOVERY_GROUP) == false)
      {
         // Server is not listening for discovery on std multicast address
         // so we need to use the correct one
         env.setProperty("jnp.discoveryGroup", DISCOVERY_GROUP);
      }
      getLog().debug("Creating InitialContext with env="+env);
      InitialContext ctx = new InitialContext(env);
      getLog().debug("Created InitialContext");
      Object obj = ctx.lookup("invokers");
      getLog().debug("lookup(invokers) : "+obj);
      Context invokersCtx = (Context) obj;
      NamingEnumeration list = invokersCtx.list("");
      while( list.hasMore() )
      {
         Object entry = list.next();
         getLog().debug(" + "+entry);
      }
      ctx.close();
   }

}
