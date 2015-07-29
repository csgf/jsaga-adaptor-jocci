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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.ogf.saga.error.IncorrectStateException;
import org.ogf.saga.error.NoSuccessException;
import org.ogf.saga.error.TimeoutException;

import fr.in2p3.jsaga.adaptor.base.defaults.Default;
import fr.in2p3.jsaga.adaptor.base.usage.UAnd;
import fr.in2p3.jsaga.adaptor.base.usage.Usage;
import fr.in2p3.jsaga.adaptor.ssh3.security.SSHSecurityAdaptor;
import fr.in2p3.jsaga.adaptor.security.SecurityCredential;
import fr.in2p3.jsaga.adaptor.security.VOMSSecurityAdaptor;
import fr.in2p3.jsaga.adaptor.security.VOMSSecurityCredential;
import fr.in2p3.jsaga.adaptor.security.impl.SSHSecurityCredential;

/* ***************************************************
* *** Centre de Calcul de l'IN2P3 - Lyon (France) ***
* ***             http://cc.in2p3.fr/             ***
* ***************************************************
* File:   jOCCISecurityAdaptor
* Author: Lionel Schwarz (lionel.schwarz@in2p3.fr)
* Date:   22 oct 2013
* ***************************************************/

public class jOCCISecurityAdaptor extends VOMSSecurityAdaptor 
{
    private SSHSecurityAdaptor m_sshAdaptor;

    public jOCCISecurityAdaptor() 
    {
        super();
        m_sshAdaptor = new SSHSecurityAdaptor();
    }
    
    @Override
    public String getType() { return "jocci"; }

    @Override
    public Usage getUsage() {
        return new UAnd.Builder()
                   .and(super.getUsage())
                   .and(new SSHSecurityAdaptor().getUsage())
                   .build();
    }

    @Override
    public Default[] getDefaults(Map attributes) 
            throws IncorrectStateException 
    {            
        Default[] vomsDefault = super.getDefaults(attributes);
        Default[] sshDefault = m_sshAdaptor.getDefaults(attributes);
            
        List<Default> both = 
        new ArrayList<Default>(vomsDefault.length + sshDefault.length);
            
        Collections.addAll(both, vomsDefault);
        Collections.addAll(both, sshDefault);
            
        return both.toArray(new Default[both.size()]);
     }

    @Override
    public Class getSecurityCredentialClass() 
    {
        return jOCCISecurityCredential.class;
    }

    @Override
    public SecurityCredential createSecurityCredential(int usage,
    			Map attributes, String contextId) 
           throws IncorrectStateException,
                  TimeoutException, NoSuccessException 
    {                
	VOMSSecurityCredential proxy = 
            (VOMSSecurityCredential)super
            .createSecurityCredential(usage, attributes, contextId);
                
	SSHSecurityCredential sshCred = 
            (SSHSecurityCredential)new SSHSecurityAdaptor()
            .createSecurityCredential(usage, attributes, contextId);
                
	return new jOCCISecurityCredential(proxy, sshCred);
    }
}
