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
package org.jboss.test.ws.jaxws.samples.advanced.retail.cc;

import javax.jws.WebMethod;
import javax.jws.WebParam;
import javax.jws.WebResult;
import javax.jws.WebService;
import javax.xml.ws.RequestWrapper;
import javax.xml.ws.Response;
import javax.xml.ws.ResponseWrapper;


/**
 * This class was generated by the JAXWS SI.
 * JAX-WS RI 2.1-10/21/2006 12:56 AM(vivek)-EA2
 * Generated source version: 2.0
 *
 */
@WebService(name = "CCVerification", targetNamespace = "http://org.jboss.ws/samples/retail/cc")
public interface CCVerification {


    /**
     *
     * @param creditCardNumber
     * @return
     *     returns boolean
     */
    @WebMethod
    @WebResult(name = "verified", targetNamespace = "")
    @RequestWrapper(localName = "verify", targetNamespace = "http://org.jboss.ws/samples/retail/cc", className = "org.jboss.test.ws.jaxws.samples.advanced.retail.cc.VerificationRequest")
    @ResponseWrapper(localName = "verifyResponse", targetNamespace = "http://org.jboss.ws/samples/retail/cc", className = "org.jboss.test.ws.jaxws.samples.advanced.retail.cc.VerificationResponse")
    public Boolean verify(
        @WebParam(name = "creditCardNumber", targetNamespace = "")
        String creditCardNumber);

    //Response<Boolean> verifyAsync(String creditCardNumber);

}
