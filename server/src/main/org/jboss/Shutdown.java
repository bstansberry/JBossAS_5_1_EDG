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
package org.jboss;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.lang.reflect.UndeclaredThrowableException;
import java.util.Hashtable;
import java.util.ArrayList;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import javax.management.MBeanServerConnection;
import javax.management.ObjectName;
import javax.naming.Context;
import javax.naming.InitialContext;


import gnu.getopt.Getopt;
import gnu.getopt.LongOpt;

import org.jboss.bootstrap.spi.Server;
import org.jboss.bootstrap.spi.ServerProcess;
import org.jboss.naming.HttpNamingContextFactory;
import org.jboss.system.server.ServerImplMBean;
import org.jboss.security.SecurityAssociation;
import org.jboss.security.SimplePrincipal;
import org.jnp.interfaces.NamingContext;

/**
 * A JMX client that uses an MBeanServerConnection to shutdown a remote JBoss
 * server.
 *
 * @version <tt>$Revision: 81030 $</tt>
 * @author  <a href="mailto:jason@planet57.com">Jason Dillon</a>
 * @author  Scott.Stark@jboss.org
 */
public class Shutdown
{
   /////////////////////////////////////////////////////////////////////////
   //                         Command Line Support                        //
   /////////////////////////////////////////////////////////////////////////
   
   public static final String PROGRAM_NAME = System.getProperty("program.name", "shutdown");
   
   protected static void displayUsage()
   {
      System.out.println("A JMX client to shutdown (exit or halt) a remote JBoss server.");
      System.out.println();
      System.out.println("usage: " + PROGRAM_NAME + " [options] <operation>");
      System.out.println();
      System.out.println("options:");
      System.out.println("    -h, --help                Show this help message (default)");
      System.out.println("    -D<name>[=<value>]        Set a system property");
      System.out.println("    --                        Stop processing options");
      System.out.println("    -s, --server=<url>        Specify the JNDI URL of the remote server");
      System.out.println("    -n, --serverName=<url>    Specify the JMX name of the ServerImpl");
      System.out.println("    -a, --adapter=<name>      Specify JNDI name of the MBeanServerConnection to use");
      System.out.println("    -u, --user=<name>         Specify the username for authentication");
      System.out.println("    -p, --password=<name>     Specify the password for authentication");
      System.out.println();
      System.out.println("operations:");
      System.out.println("    -S, --shutdown            Shutdown the server");
      System.out.println("    -e, --exit=<code>         Force the VM to exit with a status code");
      System.out.println("    -H, --halt=<code>         Force the VM to halt with a status code");
      System.out.println();
   }

   public static void main(final String[] args) throws Exception
   {
      if (args.length == 0)
      {
         displayUsage();
         System.exit(0);
      }
      
      String sopts = "-:hD:s:n:a:u:p:Se:H:";
      LongOpt[] lopts =
      {
         new LongOpt("help", LongOpt.NO_ARGUMENT, null, 'h'),
         new LongOpt("server", LongOpt.REQUIRED_ARGUMENT, null, 's'),
         new LongOpt("adapter", LongOpt.REQUIRED_ARGUMENT, null, 'a'),
         new LongOpt("serverName", LongOpt.REQUIRED_ARGUMENT, null, 'n'),
         new LongOpt("shutdown", LongOpt.NO_ARGUMENT, null, 'S'),
         new LongOpt("exit", LongOpt.REQUIRED_ARGUMENT, null, 'e'),
         new LongOpt("halt", LongOpt.REQUIRED_ARGUMENT, null, 'H'),
         new LongOpt("user", LongOpt.REQUIRED_ARGUMENT, null, 'u'),
         new LongOpt("password", LongOpt.REQUIRED_ARGUMENT, null, 'p'),
      };

      Getopt getopt = new Getopt(PROGRAM_NAME, args, sopts, lopts);
      int code;
      String arg;

      String serverURL = null;
      String adapterName = "jmx/rmi/RMIAdaptor";
      String username = null;
      String password = null;
      ObjectName serverJMXName = ServerImplMBean.OBJECT_NAME;
      boolean exit = false;
      boolean halt = false;
      int exitcode = -1;

      while ((code = getopt.getopt()) != -1)
      {
         switch (code)
         {
            case ':':
            case '?':
               // for now both of these should exit with error status
               System.exit(1);
               break;

            case 1:
               // this will catch non-option arguments
               // (which we don't currently care about)
               System.err.println(PROGRAM_NAME + ": unused non-option argument: " +
               getopt.getOptarg());
               break;
            case 'h':
               displayUsage();
               System.exit(0);
               break;
            case 'D':
            {
               // set a system property
               arg = getopt.getOptarg();
               String name, value;
               int i = arg.indexOf("=");
               if (i == -1)
               {
                  name = arg;
                  value = "true";
               }
               else
               {
                  name = arg.substring(0, i);
                  value = arg.substring(i + 1, arg.length());
               }
               System.setProperty(name, value);
               break;
            }
            case 's':
               serverURL = getopt.getOptarg();
               break;
            case 'n':
               serverJMXName = new ObjectName(getopt.getOptarg());
               break;
            case 'S':
               // nothing...
               break;
            case 'a':
               adapterName = getopt.getOptarg();
               break;
            case 'u':
               username = getopt.getOptarg();
               SecurityAssociation.setPrincipal(new SimplePrincipal(username));
               break;
            case 'p':
               password = getopt.getOptarg();
               SecurityAssociation.setCredential(password);
               break;
            case 'e':
               exitcode = Integer.parseInt(getopt.getOptarg());
               exit = true;
               break;
            case 'H':
               exitcode = Integer.parseInt(getopt.getOptarg());
               halt = true;
               break;
         }
      }
      
      InitialContext ctx;

      // If there was a username specified, but no password prompt for it
      if( username != null && password == null )
      {
         System.out.print("Enter password for "+username+": ");
         BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
         password = br.readLine();
         SecurityAssociation.setCredential(password);
      }

      if (serverURL == null)
      {
         ctx = new InitialContext();
      }
      else
      {
         Hashtable env = new Hashtable();
         env.put(Context.PROVIDER_URL, serverURL);
         env.put(NamingContext.JNP_DISABLE_DISCOVERY, "true");
         // This is a hack
         if (serverURL.startsWith("http:") || serverURL.startsWith("https:"))
            env.put(Context.INITIAL_CONTEXT_FACTORY, HttpNamingContextFactory.class.getName());
         ctx  = new InitialContext(env);
      }

      Object obj = ctx.lookup(adapterName);
      if (!(obj instanceof MBeanServerConnection))
      {
         throw new RuntimeException("Object not of type: MBeanServerConnection, but: " +
            (obj == null ? "not found" : obj.getClass().getName()));
      }

      MBeanServerConnection adaptor = (MBeanServerConnection) obj;
      ServerProxyHandler handler = new ServerProxyHandler(adaptor, serverJMXName);
      Class<?>[] ifaces = {ServerProcess.class};
      ClassLoader tcl = Thread.currentThread().getContextClassLoader();
      ServerProcess server = (ServerProcess) Proxy.newProxyInstance(tcl, ifaces, handler);

      if (exit)
      {
         server.exit(exitcode);
      }
      else if (halt)
      {
         server.halt(exitcode);
      }
      else
      {
         server.shutdown();
      }
      System.out.println("Shutdown message has been posted to the server.");
      System.out.println("Server shutdown may take a while - check logfiles for completion");
   }

   private static class ServerProxyHandler implements InvocationHandler
   {
      ObjectName serverName;
      MBeanServerConnection server;
      ServerProxyHandler(MBeanServerConnection server, ObjectName serverName)
      {
         this.server = server;
         this.serverName = serverName;
      }

      public Object invoke(Object proxy, Method method, Object[] args)
            throws Throwable
      {
         String methodName = method.getName();
         Class[] sigTypes = method.getParameterTypes();
         ArrayList sigStrings = new ArrayList();
         for(int s = 0; s < sigTypes.length; s ++)
            sigStrings.add(sigTypes[s].getName());
         String[] sig = new String[sigTypes.length];
         sigStrings.toArray(sig);
         Object value = null;
         try
         {
            value = server.invoke(serverName, methodName, args, sig);
         }
         catch(UndeclaredThrowableException e)
         {
            System.out.println("getUndeclaredThrowable: "+e.getUndeclaredThrowable());
            throw e.getUndeclaredThrowable();
         }
         return value;
      }
   }
}
