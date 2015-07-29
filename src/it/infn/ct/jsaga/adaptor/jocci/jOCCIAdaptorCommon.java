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

package it.infn.ct.jsaga.adaptor.jocci;

import fr.in2p3.jsaga.adaptor.ClientAdaptor;
import fr.in2p3.jsaga.adaptor.base.defaults.Default;
import fr.in2p3.jsaga.adaptor.base.usage.U;
import fr.in2p3.jsaga.adaptor.base.usage.UAnd;
import fr.in2p3.jsaga.adaptor.base.usage.Usage;
import fr.in2p3.jsaga.adaptor.security.SecurityCredential;

import org.ogf.saga.context.Context;

import org.ogf.saga.error.*;

import it.infn.ct.jsaga.adaptor.jocci.security.jOCCISecurityCredential;

import java.util.Map;
       
/* *********************************************
 * *** Istituto Nazionale di Fisica Nucleare ***
 * ***      Sezione di Catania (Italy)       ***
 * ***        http://www.ct.infn.it/         ***
 * *********************************************
 * File:    jOCCIJobControlAdaptor.java
 * Authors: Giuseppe LA ROCCA, Diego SCARDACI
 * Email:   <giuseppe.larocca, diego.scardaci>@ct.infn.it
 * Ver.:    1.0.4
 * Date:    24 September 2014
 * *********************************************/

public class jOCCIAdaptorCommon extends Object implements ClientAdaptor {

  protected jOCCISecurityCredential credential = null;            
  protected String sshHost = null;
  protected String user_cred = "";
  protected String ca_path = "";
  
  protected static final String AUTH = "auth";
  protected static final String RESOURCE = "compute";
  protected static final String ACTION = "create";
  
  @Override
  public Class[] getSupportedSecurityCredentialClasses() 
  {    
      return new Class[] {         
	  jOCCISecurityCredential.class
      };
  }

  @Override
  public void setSecurityCredential(SecurityCredential sc) 
  {
      credential = (jOCCISecurityCredential)sc;
      
      try {
                user_cred = (String)credential.getProxy().getAttribute(Context.USERPROXY);
                ca_path = (String)credential.getProxy().getCertRepository().getPath();
      } catch (NotImplementedException e) { e.printStackTrace(); } 
        catch (NoSuccessException e) { e.printStackTrace(); }
  }
  
  @Override
  public String getType() { return "jocci"; }

  @Override
  public int getDefaultPort() { return 11443; }
  
  @Override
  public void connect (String userInfo, String host, int port, String basePath, Map attributes) 
         throws NotImplementedException, 
                AuthenticationFailedException, 
                AuthorizationFailedException, 
                IncorrectURLException, 
                BadParameterException, 
                TimeoutException, 
                NoSuccessException 
  { }
  
  @Override
  public void disconnect() throws NoSuccessException {  } 

  @Override
  public Usage getUsage() 
  { 
    return new UAnd.Builder()
                     .and(new U(AUTH))
                     .and(new U(RESOURCE))
                     .and(new U(ACTION))
                     .build();      
  }

  @Override
  public Default[] getDefaults(Map map) throws IncorrectStateException 
  {
    return new Default[] {
        new Default (AUTH, "x509"),
        // Possible RESOURCE values: 'os_tpl', 'resource_tpl', 'compute' (default)
        new Default (RESOURCE, "compute"),
        new Default (ACTION, "create")
    };
  }
}
