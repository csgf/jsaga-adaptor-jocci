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

package it.infn.ct.jsaga.adaptor.jocci.data;

import fr.in2p3.jsaga.adaptor.ssh3.data.SFTPDataAdaptor;
import it.infn.ct.jsaga.adaptor.jocci.security.jOCCISecurityCredential;

import java.util.Map;

import org.ogf.saga.error.AuthenticationFailedException;
import org.ogf.saga.error.AuthorizationFailedException;
import org.ogf.saga.error.BadParameterException;
import org.ogf.saga.error.NoSuccessException;
import org.ogf.saga.error.NotImplementedException;
import org.ogf.saga.error.TimeoutException;

import ch.ethz.ssh2.Connection;

/* ***************************************************
* *** Centre de Calcul de l'IN2P3 - Lyon (France) ***
* ***             http://cc.in2p3.fr/             ***
* ***************************************************
* File:   jOCCIDataAdaptor
* Author: Lionel Schwarz (lionel.schwarz@in2p3.fr)
* Date:   22 oct 2013
* ***************************************************/

public class jOCCIDataAdaptor extends SFTPDataAdaptor 
{
    
  @Override
  public String getType() { return "jocci"; }
  
  @Override
  public Class[] getSupportedSecurityCredentialClasses() 
  {
        return new Class[]{ jOCCISecurityCredential.class };
  }

  @Override
  public void connect(String userInfo, String host, 
                      int port, String basePath, Map attributes)
                throws NotImplementedException, AuthenticationFailedException, 
                       AuthorizationFailedException, BadParameterException, 
                       TimeoutException, NoSuccessException 
  {
	try {
        	m_conn = new Connection(host, port);
                m_conn.connect(null);
                
                String userId = ((jOCCISecurityCredential) credential)
                        .getSSHCredential()
                        .getUserId();
                
                String passPhrase = ((jOCCISecurityCredential) credential)
                        .getSSHCredential()
                        .getUserPass();
                
                // clone private key because the object will be reset
                byte[] privateKey = ((jOCCISecurityCredential) credential)
                        .getSSHCredential()
                        .getPrivateKey()
                        .clone();
                
                char[] pemPrivateKey = new String(privateKey).toCharArray();
                if (!m_conn.authenticateWithPublicKey(userId, pemPrivateKey, passPhrase)) 
		{
                        m_conn.close();
                        throw new AuthenticationFailedException("Auth fail");
                }
        } catch (Exception e) {
            m_conn.close();
	    if ("Auth fail".equals(e.getMessage()))
            throw new AuthenticationFailedException(e);
            throw new NoSuccessException("Unable to connect to server", e);
        }
  }  
}
