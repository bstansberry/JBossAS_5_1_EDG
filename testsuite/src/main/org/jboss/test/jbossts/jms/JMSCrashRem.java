/*
 * JBoss, Home of Professional Open Source
 * Copyright 2008, Red Hat Middleware LLC, and individual contributors
 * as indicated by the @author tags.
 * See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 * This copyrighted material is made available to anyone wishing to use,
 * modify, copy, or redistribute it subject to the terms and conditions
 * of the GNU Lesser General Public License, v. 2.1.
 * This program is distributed in the hope that it will be useful, but WITHOUT A
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE.  See the GNU Lesser General Public License for more details.
 * You should have received a copy of the GNU Lesser General Public License,
 * v.2.1 along with this distribution; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,
 * MA  02110-1301, USA.
 *
 * (C) 2008,
 * @author JBoss Inc.
 */
package org.jboss.test.jbossts.jms;

import org.jboss.test.jbossts.recovery.ASFailureSpec;

import javax.ejb.Remote;

@Remote
public interface JMSCrashRem {
    // JNDI names of the fault injection beans
    String BMT_JNDI_NAME = "JMSCrashBMTBean/remote";
    String CMT_JNDI_NAME = "JMSCrashCMTBean/remote";

    String testXA(String message, boolean reverseOrder, ASFailureSpec... specs);
}
