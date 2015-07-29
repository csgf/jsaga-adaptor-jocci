/*
 * ====================================================================
 *
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 * ====================================================================
 *
 * This software consists of voluntary contributions made by many
 * individuals on behalf of the Apache Software Foundation.  For more
 * information on the Apache Software Foundation, please see
 * <http://www.apache.org/>.
 *
 */

package it.infn.ct.jsaga.adaptor.jocci.security;

import java.io.PrintStream;

import org.ogf.saga.error.NoSuccessException;
import org.ogf.saga.error.NotImplementedException;

import fr.in2p3.jsaga.adaptor.security.SecurityCredential;
import fr.in2p3.jsaga.adaptor.security.VOMSSecurityCredential;
import fr.in2p3.jsaga.adaptor.security.impl.SSHSecurityCredential;

/* ***************************************************
* *** Centre de Calcul de l'IN2P3 - Lyon (France) ***
* ***             http://cc.in2p3.fr/             ***
* ***************************************************
* File:   jOCCISecurityCredential
* Author: Lionel Schwarz (lionel.schwarz@in2p3.fr)
* Date:   22 oct 2013
* ***************************************************/

public class jOCCISecurityCredential implements SecurityCredential 
{
    private SSHSecurityCredential m_sshCred;
    private VOMSSecurityCredential m_proxy;
	
    public jOCCISecurityCredential(VOMSSecurityCredential p, 
                                   SSHSecurityCredential s) 
    {
        this.m_sshCred = s;
	this.m_proxy = p;
    }
	
    @Override
    public String getUserID() throws Exception 
    {
        return m_proxy.getUserID();
    }

    @Override
    public String getAttribute(String key) 
            throws NotImplementedException, NoSuccessException 
    {
        return m_proxy.getAttribute(key);
    }

    @Override
    public void close() throws Exception 
    {
        m_proxy.close();
	m_sshCred.close();
    }

    @Override
    public void dump(PrintStream out) throws Exception 
    {
        out.println("VOMS Proxy to access Cloud");
	m_proxy.dump(out);
	out.flush();
	out.println("SSH Credential to access VM");
	m_sshCred.dump(out);
	out.flush();
    }

    public SSHSecurityCredential getSSHCredential() 
    {
        return this.m_sshCred;
    }
        
    public VOMSSecurityCredential getProxy() 
    {        
        return this.m_proxy;
    }
}
