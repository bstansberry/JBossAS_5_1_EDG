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
package org.jboss.console.twiddle.command;

import java.io.PrintWriter;

import javax.management.ObjectName;

import gnu.getopt.Getopt;
import gnu.getopt.LongOpt;

import org.jboss.util.Strings;

/**
 * Query the server for a list of matching MBeans.
 *
 * @version <tt>$Revision: 81010 $</tt>
 * @author <a href="mailto:jason@planet57.com">Jason Dillon</a>
 * @author Scott.Stark@jboss.org
 */
public class QueryCommand
   extends MBeanServerCommand
{
   private String query;

   private boolean displayCount;

   public QueryCommand()
   {
      super("query", "Query the server for a list of matching MBeans");
   }

   public void displayHelp()
   {
      PrintWriter out = context.getWriter();

      out.println(desc);
      out.println();
      out.println("usage: " + name + " [options] <query>");
      out.println("options:");
      out.println("    -c, --count    Display the matching MBean count");
      out.println("    --             Stop processing options");
      out.println("Examples:");
      out.println(" query all mbeans: "+name+" '*:*'");
      out.println(" query all mbeans in the jboss.j2ee domain: "+name+" 'jboss.j2ee:*'");

      out.flush();
   }

   private void processArguments(final String[] args)
      throws CommandException
   {
      log.debug("processing arguments: " + Strings.join(args, ","));

      if (args.length == 0)
      {
         throw new CommandException("Command requires arguments");
      }

      String sopts = "-:c";
      LongOpt[] lopts =
         {
            new LongOpt("count", LongOpt.NO_ARGUMENT, null, 'c'),
         };

      Getopt getopt = new Getopt(null, args, sopts, lopts);
      getopt.setOpterr(false);

      int code;
      int argidx = 0;

      while ((code = getopt.getopt()) != -1)
      {
         switch (code)
         {
            case ':':
               throw new CommandException
                  ("Option requires an argument: " + args[getopt.getOptind() - 1]);

            case '?':
               throw new CommandException
                  ("Invalid (or ambiguous) option: " + args[getopt.getOptind() - 1]);

               // non-option arguments
            case 1:
               {
                  String arg = getopt.getOptarg();

                  switch (argidx++)
                  {
                     case 0:
                        query = arg;
                        log.debug("query: " + query);
                        break;

                     default:
                        throw new CommandException("Unused argument: " + arg);
                  }
                  break;
               }

               // Show count
            case 'c':
               displayCount = true;
               break;
         }
      }
   }

   public void execute(String[] args) throws Exception
   {
      processArguments(args);

      if (query == null)
         throw new CommandException("Missing MBean query");
      
      
      // get the list of object names to work with
      ObjectName[] names = queryMBeans(query);

      PrintWriter out = context.getWriter();

      if (displayCount)
      {
         out.println(names.length);
      }
      else
      {
         for (int i = 0; i < names.length; i++)
         {
            out.println(names[i]);
         }
      }

      out.flush();
   }
}
