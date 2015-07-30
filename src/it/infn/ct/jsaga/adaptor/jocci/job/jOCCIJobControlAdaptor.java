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

package it.infn.ct.jsaga.adaptor.jocci.job;

import it.infn.ct.jsaga.adaptor.jocci.jOCCIAdaptorCommon;

import cz.cesnet.cloud.occi.Model;
import cz.cesnet.cloud.occi.api.Client;
import cz.cesnet.cloud.occi.api.EntityBuilder;
import cz.cesnet.cloud.occi.api.exception.CommunicationException;
import cz.cesnet.cloud.occi.api.exception.EntityBuildingException;
import cz.cesnet.cloud.occi.api.http.HTTPClient;
import cz.cesnet.cloud.occi.api.http.auth.HTTPAuthentication;
import cz.cesnet.cloud.occi.api.http.auth.VOMSAuthentication;
import cz.cesnet.cloud.occi.core.ActionInstance;
import cz.cesnet.cloud.occi.core.Entity;
import cz.cesnet.cloud.occi.core.Link;
import cz.cesnet.cloud.occi.core.Mixin;
import cz.cesnet.cloud.occi.core.Resource;
import cz.cesnet.cloud.occi.exception.AmbiguousIdentifierException;
import cz.cesnet.cloud.occi.exception.InvalidAttributeValueException;
import cz.cesnet.cloud.occi.exception.RenderingException;
import cz.cesnet.cloud.occi.infrastructure.Compute;
import cz.cesnet.cloud.occi.infrastructure.IPNetworkInterface;
import cz.cesnet.cloud.occi.infrastructure.NetworkInterface;
import cz.cesnet.cloud.occi.parser.MediaType;

import fr.in2p3.jsaga.adaptor.base.usage.U;
import fr.in2p3.jsaga.adaptor.base.usage.UAnd;
import fr.in2p3.jsaga.adaptor.base.usage.UOptional;
import fr.in2p3.jsaga.adaptor.base.usage.Usage;
import fr.in2p3.jsaga.adaptor.job.control.description.JobDescriptionTranslator;
import fr.in2p3.jsaga.adaptor.job.control.advanced.CleanableJobAdaptor;
import fr.in2p3.jsaga.adaptor.job.control.staging.StagingJobAdaptorTwoPhase;
import fr.in2p3.jsaga.adaptor.job.control.staging.StagingTransfer;
import fr.in2p3.jsaga.adaptor.job.control.JobControlAdaptor;
import fr.in2p3.jsaga.adaptor.job.monitor.JobMonitorAdaptor;
import fr.in2p3.jsaga.adaptor.job.BadResource;
import fr.in2p3.jsaga.adaptor.ssh3.job.SSHJobControlAdaptor;

import org.ogf.saga.error.NoSuccessException;
import org.ogf.saga.error.NotImplementedException;
import org.ogf.saga.error.AuthenticationFailedException;
import org.ogf.saga.error.AuthorizationFailedException;
import org.ogf.saga.error.BadParameterException;
import org.ogf.saga.error.IncorrectURLException;
import org.ogf.saga.error.TimeoutException;
import org.ogf.saga.error.PermissionDeniedException;

import java.io.IOException;
import java.io.BufferedReader;
import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;

import org.apache.log4j.Logger;
import org.apache.commons.net.telnet.TelnetClient;

/* *********************************************
 * *** Istituto Nazionale di Fisica Nucleare ***
 * ***      Sezione di Catania (Italy)       ***
 * ***        http://www.ct.infn.it/         ***
 * *********************************************
 * File:    jOCCIJobControlAdaptor.java
 * Authors: Giuseppe LA ROCCA
 * Email:   giuseppe.larocca@ct.infn.it
 * Ver.:    0.0.1
 * Date:    29 July 2015
 * *********************************************/

public class jOCCIJobControlAdaptor extends jOCCIAdaptorCommon
                                    implements JobControlAdaptor, 
                                               StagingJobAdaptorTwoPhase, 
                                               CleanableJobAdaptor
{     
    
  protected static final String ATTRIBUTES_TITLE = "attributes_title";
  protected static final String MIXIN_OS_TPL = "mixin_os_tpl";
  protected static final String MIXIN_RESOURCE_TPL = "mixin_resource_tpl";
  protected static final String PREFIX = "prefix";  
    
  // MAX tentatives before to gave up to connect the VM server.
  private final int MAX_CONNECTIONS = 10;
  
  private static final Logger log = 
          Logger.getLogger(jOCCIJobControlAdaptor.class);
      
  private jOCCIJobMonitorAdaptor jOCCIJobMonitorAdaptor = 
            new jOCCIJobMonitorAdaptor();
    
  private SSHJobControlAdaptor sshControlAdaptor = 
            new SSHJobControlAdaptor();
  
  private String prefix = "";
  private String action = "";
  private String resource = "";  
  private String auth = "";
  private String attributes_title = "";
  private String mixin_os_tpl = "";
  private String mixin_resource_tpl = "";  
  private String Endpoint = "";
  private String OCCI_ENDPOINT_HOST = "";
  private int OCCI_ENDPOINT_PORT;
  
  // Adding FedCloud Contextualisation options here
  private String context_user_data = "";  
    
  enum ACTION_TYPE { list, delete, describe, create; }
    
  String[] IP = new String[2];
  
  private List<String> run_OCCI (String action_type, String action)            
  {
      String line;
      Integer cmdExitCode;
      //List<String> list_jOCCI = new ArrayList();
      List<String> list_jOCCI = new ArrayList<String>();      
            
      try
      {            
        Process p = Runtime.getRuntime().exec(action);
        cmdExitCode = p.waitFor();
        log.info("EXIT CODE = " + cmdExitCode);

        if (cmdExitCode==0)
        {
            BufferedReader in = new BufferedReader(
                            new InputStreamReader(p.getInputStream()));

            ACTION_TYPE type = ACTION_TYPE.valueOf(action_type);
             
            while ((line = in.readLine()) != null) 
            {         
                // Skip blank lines.
                if (line.trim().length() > 0) {
                    
                switch (type) {
                    case list:
                        list_jOCCI.add(line.trim());
                        break;

                    case create:
                        list_jOCCI.add(line.trim());
                        log.info("");
                        log.info("A new OCCI computeID has been created:");
                        break;

                    case describe:
                        list_jOCCI.add(line.trim());
                        break;
                
                    case delete:
                        break;
                } // end switch
                } // end if
            } // end while

            in.close();
             
            if (action_type.equals("describe") || 
                action_type.equals("list") ||
                action_type.equals("delete")) 
                log.info("\n");         
                             
            for (int i = 0; i < list_jOCCI.size(); i++)         
                log.info(list_jOCCI.get(i));
        }
                                             
        } catch (InterruptedException ex) {
            java.util.logging.Logger.getLogger(jOCCIJobControlAdaptor.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException ex) { log.error(ex); }
        
        return list_jOCCI;
    }
                        
    @Override
    public void connect (String userInfo, String host, int port, String basePath, Map attributes) 
           throws NotImplementedException, 
                  AuthenticationFailedException, 
                  AuthorizationFailedException, 
                  IncorrectURLException, 
                  BadParameterException, 
                  TimeoutException, 
                  NoSuccessException 
    {      
        
       //List<String> results = new ArrayList();
        List<String> results = new ArrayList<String>();
                 
       log.info("");
       log.info("Trying to connect to the cloud host [ " + host + " ] ");
       
       prefix = (String) attributes.get(PREFIX);
       action = (String) attributes.get(ACTION);
       auth = (String) attributes.get(AUTH);
       resource = (String) attributes.get(RESOURCE);
       attributes_title = (String) attributes.get(ATTRIBUTES_TITLE);
       mixin_os_tpl = (String) attributes.get(MIXIN_OS_TPL);
       mixin_resource_tpl = (String) attributes.get(MIXIN_RESOURCE_TPL);
       
       context_user_data = (String) attributes.get("user_data");
       
       // Check if OCCI path is set                
       if ((prefix != null) && (new File((prefix)).exists()))
            prefix += System.getProperty("file.separator");
       else prefix = "";
              
       Endpoint = "https://" 
                  + host + ":" + port 
                  + System.getProperty("file.separator");
       
       OCCI_ENDPOINT_HOST = host;
       OCCI_ENDPOINT_PORT = port;
       
       log.info("");
       log.info("See below the details: ");
       log.info("");
       log.info("PREFIX    = " + prefix);
       log.info("ACTION    = " + action);
       log.info("RESOURCE  = " + resource);
       
       log.info("");
       log.info("AUTH       = " + auth);       
       log.info("PROXY_PATH = " + user_cred);       
       log.info("CA_PATH    = " + ca_path);
       
       log.info("");
       log.info("HOST        = " + host);
       log.info("PORT        = " + port);
       log.info("ENDPOINT    = " + Endpoint);
       log.info("PUBLIC KEY  = " + credential.getSSHCredential().getPublicKeyFile().getPath());
       log.info("PRIVATE KEY = " + credential.getSSHCredential().getPrivateKeyFile().getPath());
       
       log.info("");
       log.info("EGI FedCLoud Contextualisation options:");       
       log.info("USER DATA  = " + context_user_data);
       log.info("");
             
       sshControlAdaptor.setSecurityCredential(credential.getSSHCredential());
    }
            
    @Override
    public void start(String nativeJobId) throws PermissionDeniedException, 
                                                 TimeoutException, 
                                                 NoSuccessException 
    {
        String _publicIP = 
                nativeJobId.substring(nativeJobId.indexOf("@")+1, 
                                      nativeJobId.indexOf("#"));
        
        String _nativeJobId = nativeJobId.substring(0, nativeJobId.indexOf("@"));
        
        try {                        
            sshControlAdaptor.connect(null, _publicIP, 22, null, new HashMap());            
            sshControlAdaptor.start(_nativeJobId);                         
            
        } catch (NotImplementedException ex) { throw new NoSuccessException(ex); } 
          catch (AuthenticationFailedException ex) { throw new PermissionDeniedException(ex); } 
          catch (AuthorizationFailedException ex) { throw new PermissionDeniedException(ex); } 
          catch (BadParameterException ex) { throw new NoSuccessException(ex); }
    }
    
    @Override
    public void cancel(String nativeJobId) throws PermissionDeniedException, 
                                                  TimeoutException, 
                                                  NoSuccessException 
    {   
        String _publicIP = nativeJobId.substring(nativeJobId.indexOf("@")+1, 
                                                 nativeJobId.indexOf("#"));
        
        String _nativeJobId = nativeJobId.substring(0, nativeJobId.indexOf("@"));
        
        try {                        
            sshControlAdaptor.connect(null, _publicIP, 22, null, new HashMap());            
            sshControlAdaptor.cancel(_nativeJobId);
        } catch (NotImplementedException ex) { throw new NoSuccessException(ex); } 
          catch (AuthenticationFailedException ex) { throw new PermissionDeniedException(ex); } 
          catch (AuthorizationFailedException ex) { throw new PermissionDeniedException(ex); } 
          catch (BadParameterException ex) { throw new NoSuccessException(ex); }
        
        log.info("Calling the cancel() method");        
    }
    
    @Override
    public void clean (String nativeJobId) throws PermissionDeniedException, 
                                                  TimeoutException, 
                                                  NoSuccessException 
    {    
            
            try {    
              
                List<String> results = new ArrayList();            
                
                String _publicIP = nativeJobId.substring(nativeJobId.indexOf("@")+1, 
                                                         nativeJobId.indexOf("#"));
                
                String _nativeJobId = nativeJobId.substring(0, nativeJobId.indexOf("@"));
                String _resourceId = nativeJobId.substring(nativeJobId.indexOf("#")+1);
                
                // Stop and delete resource
                log.info("");log.info("[ STOP & DELETE ]");
                log.info("- Trigger a 'stop' action to the resource");                
                
                HTTPAuthentication authentication =
                new VOMSAuthentication(user_cred);
                        
                //set custom certificates if needed
                authentication.setCAPath(ca_path);
                Client client = new HTTPClient(URI.create("https://"
                                + OCCI_ENDPOINT_HOST + ":"
                                + OCCI_ENDPOINT_PORT),
                                authentication, MediaType.TEXT_PLAIN, false);
                        
                //connect client
                client.connect();

                Model model = client.getModel();
                EntityBuilder eb = new EntityBuilder(model);
                        
                ActionInstance actionInstance =
                eb.getActionInstance(URI.create("http://schemas.ogf.org/occi/infrastructure/compute/action#stop"));             
                boolean status = client.trigger(URI.create(_resourceId), actionInstance);
                if (status) log.info("Triggered: OK");
                else log.error("Triggered: FAIL");

                log.info("- Delete the resource");
                status = client.delete(URI.create(_resourceId));
                if (status) log.info("Delete: OK");
                else log.error("Delete: FAIL");                
                        
                log.info("");
                log.info("Stopping the VM [ " + _publicIP + " ]");                       
                
                try {            
                    sshControlAdaptor.connect(null, _publicIP, 22, null, new HashMap());            
                    sshControlAdaptor.clean(_nativeJobId);                    
                    
                } catch (NotImplementedException ex) { throw new NoSuccessException(ex); } 
                  catch (AuthenticationFailedException ex) { throw new PermissionDeniedException(ex); } 
                  catch (AuthorizationFailedException ex) { throw new PermissionDeniedException(ex); } 
                  catch (BadParameterException ex) { throw new NoSuccessException(ex); }                
        } 
        catch (EntityBuildingException ex) {
            java.util.logging.Logger
                    .getLogger(jOCCIJobControlAdaptor.class.getName())
                    .log(Level.SEVERE, null, ex); 
        }           
        catch (CommunicationException ex) {
            java.util.logging.Logger
                    .getLogger(jOCCIJobControlAdaptor.class.getName())
                    .log(Level.SEVERE, null, ex); 
        }
    }        
    
    @Override
    public String submit (String jobDesc, boolean checkMatch, String uniqId) 
                  throws PermissionDeniedException, 
                         TimeoutException, 
                         NoSuccessException, 
                         BadResource 
    {        
        String result = "";
        
        if (action.equals("create")) 
        {
            try 
            {
                log.info("Creating a new resource using jOCCI-api. Please wait!");
                
                if (attributes_title.trim().length() > 0)
                    log.info("VM Title     = " + attributes_title);
                    
                if (mixin_os_tpl.trim().length() > 0)
                    log.info("OS           = " + mixin_os_tpl);
                    
                if (mixin_resource_tpl.trim().length() > 0)
                    log.info("Flavour      = " + mixin_resource_tpl);
                                
                HTTPAuthentication authentication =
                new VOMSAuthentication(user_cred);
                
                //set custom certificates if needed
                authentication.setCAPath(ca_path);
                Client client = new HTTPClient(URI.create("https://"
                                + OCCI_ENDPOINT_HOST + ":"
                                + OCCI_ENDPOINT_PORT),
                                authentication, MediaType.TEXT_PLAIN, false);
                
                //connect client
                client.connect();

                Model model = client.getModel();
                EntityBuilder eb = new EntityBuilder(model);
                Date date = new Date();
                
                log.info("");
                log.info("[ TEMPLATE ]");
                log.info("- Available os template mixins ...");
                List<Mixin> mixins = model.findRelatedMixins("os_tpl");
                
                if (!mixins.isEmpty())
                {
                    for (Mixin entry : mixins)
                        log.info(entry);
                    
                    // 1.) Create a new compute resource
                    log.info("");log.info("[ CREATE ]");
                    Resource compute = eb.getResource("compute");
                    Mixin mixin = model.findMixin(mixin_os_tpl);
                    compute.addMixin(mixin);
                    compute.addMixin(model.findMixin(mixin_os_tpl, "os_tpl"));
                    compute.addMixin(model.findMixin(mixin_resource_tpl, "resource_tpl"));
                    compute.setTitle(attributes_title);
                    log.info(mixin.toText());
                    
                    log.info("");
                    // Creating a new VM resource
                    URI location = client.create(compute);
                    // Getting the list of available running resources
                    List<URI> list = client.list();
                    URI uri_location = list.get(0);
                    
                    log.info("=============== [ R E P O R T ] ===============");
                    if (location != null) log.info(uri_location);
                    else log.error("Some errors occurred during the creation of a new resource.");
                    
                    // 3.) Describe resource
                    log.info("");log.info("[ DESCRIPTION ]");
                    log.info("- Getting VM settings");
                    List<Entity> entities = client.describe(uri_location);
                    log.info(entities.get(0).toText());
                    
                    // Getting the IP address of the resource                   
                    String _publicIP = "";
                    for (Entity entity : entities) 
                    {
                        Resource resource = (Resource) entity;
                        
                        Set<Link> links = 
                        resource.getLinks(NetworkInterface.TERM_DEFAULT);
                        
                        for (Link link : links)
                        _publicIP = link.getValue(IPNetworkInterface.ADDRESS_ATTRIBUTE_NAME);
                    }
                                        
                    SimpleDateFormat ft = 
                       new SimpleDateFormat ("E yyyy.MM.dd 'at' hh:mm:ss a zzz");
           
                    log.info("");
                    log.info("Waiting the remote VM finishes the boot!");
                    log.info(ft.format(date));
                    
                    log.info("");log.info("Waiting [ " + uri_location + " ] becomes ACTIVE! ");
                    log.info("Starting VM [ " + _publicIP + " ] in progress...");
                    log.info("This operation may take few minutes to complete. Please wait!");
                    log.info("");
                    
                    while (!entities.get(0)
                            .getValue(Compute.STATE_ATTRIBUTE_NAME)
                            .equals("active"))
                    {
                        log.info("[ STATUS ] = "
                                + entities.get(0)
                                .getValue(Compute.STATE_ATTRIBUTE_NAME));

                        entities = client.describe(uri_location);
                        
                        if (entities.get(0)
                                .getValue(Compute.STATE_ATTRIBUTE_NAME)
                                .equals("active"))
                        {
                                log.info("[ STATUS ] = "
                                        + entities.get(0)
                                        .getValue(Compute.STATE_ATTRIBUTE_NAME));

                                log.info("");
                                log.info("Compute [ " + uri_location + " ] is active.");
                                log.info("IP address = " + _publicIP);                                
                                log.info("Checking for connectivity. Please wait! ");
                                
                                byte[] buff = new byte[1024];
                                int ret_read = 0;
                                boolean flag = true;                                        
                                int MAX = 0;
                                TelnetClient tc = null;
                    
                                while ((flag) && (MAX < MAX_CONNECTIONS))
                                {                        
                                    try
                                    {
                                        tc = new TelnetClient();
                                        tc.connect(_publicIP, 22);
                                        InputStream instr = tc.getInputStream();
                                                    
                                        ret_read = instr.read(buff);                            
                                        if (ret_read > 0)
                                        {
                                            log.info("SSH daemon has started [ OK ] ");
                                            tc.disconnect();
                                            flag=false;
                                        }
                                    } catch (IOException e) {
                                        try {
                                            Thread.sleep(60000);
                                        } catch (InterruptedException ex) 
                                        { 
                                            java.util.logging.Logger
                                                     .getLogger(jOCCIJobControlAdaptor.class.getName())
                                                     .log(Level.SEVERE, null, ex);
                                        }
                                        
                                        MAX++;
                                    }
                                }
                                
                                date = new Date();
                                log.info(ft.format(date));
                    
                                jOCCIJobMonitorAdaptor.setSSHHost(_publicIP);
        
                                try {            
                                    sshControlAdaptor.connect(null, _publicIP, 22, null, new HashMap());            
                                } catch (NotImplementedException ex) { throw new NoSuccessException(ex); } 
                                  catch (AuthenticationFailedException ex) { throw new PermissionDeniedException(ex); } 
                                  catch (AuthorizationFailedException ex) { throw new PermissionDeniedException(ex); } 
                                  catch (BadParameterException ex) { throw new NoSuccessException(ex); }
                
                                result = sshControlAdaptor.submit(jobDesc, checkMatch, uniqId) 
                                    + "@" + _publicIP + "#" + uri_location;
                                
                                break;
                        } else {                            
                            // Sleeping for a while ...
                            try {
                                Thread.sleep(5000);
                            } catch (InterruptedException ex) {
                            java.util.logging.Logger
                                  .getLogger(jOCCIJobControlAdaptor.class.getName())
                                  .log(Level.SEVERE, null, ex);
                            }                                                
                        }
                    }

                } else log.error("No os template mixins available. Quiting.");                
                
            } // end creating
            catch (RenderingException ex) {
                java.util.logging.Logger
                        .getLogger(jOCCIJobControlAdaptor.class.getName())
                        .log(Level.SEVERE, null, ex);
            }            
            catch (InvalidAttributeValueException ex) {
                java.util.logging.Logger
                        .getLogger(jOCCIJobControlAdaptor.class.getName())
                        .log(Level.SEVERE, null, ex);
            } 
            catch (EntityBuildingException ex) {
                java.util.logging.Logger
                        .getLogger(jOCCIJobControlAdaptor.class.getName())
                        .log(Level.SEVERE, null, ex);
            }            
            catch (AmbiguousIdentifierException ex) {
                java.util.logging.Logger
                        .getLogger(jOCCIJobControlAdaptor.class.getName())
                        .log(Level.SEVERE, null, ex);
            }   
            catch (CommunicationException ex) {
                java.util.logging.Logger
                        .getLogger(jOCCIJobControlAdaptor.class.getName())
                        .log(Level.SEVERE, null, ex);
            }
            } // end creating
                
        return result;
    }
    
    @Override
    public StagingTransfer[] getInputStagingTransfer(String nativeJobId) 
                             throws PermissionDeniedException, 
                                    TimeoutException, 
                                    NoSuccessException 
    {        
        StagingTransfer[] result = null;
        String _publicIP = nativeJobId.substring(nativeJobId.indexOf("@")+1, 
                                                 nativeJobId.indexOf("#"));
        
        String _nativeJobId = nativeJobId.substring(0, nativeJobId.indexOf("@"));
        
        try {            	
	    sshControlAdaptor.setSecurityCredential(credential.getSSHCredential());
            sshControlAdaptor.connect(null, _publicIP, 22, null, new HashMap());
            result = sshControlAdaptor.getInputStagingTransfer(_nativeJobId);
                        
        } catch (NotImplementedException ex) { throw new NoSuccessException(ex); } 
          catch (AuthenticationFailedException ex) { throw new PermissionDeniedException(ex); } 
          catch (AuthorizationFailedException ex) { throw new PermissionDeniedException(ex); } 
          catch (BadParameterException ex) { throw new NoSuccessException(ex); }
             
        // change URL sftp:// tp jocci://
        return sftp2jocci(result);
    }
    
    @Override
    public StagingTransfer[] getOutputStagingTransfer(String nativeJobId) 
                             throws PermissionDeniedException, 
                                    TimeoutException, 
                                    NoSuccessException 
    {
        
        StagingTransfer[] result = null;
        String _publicIP = nativeJobId.substring(nativeJobId.indexOf("@")+1, 
                                                 nativeJobId.indexOf("#"));
        
        String _nativeJobId = nativeJobId.substring(0, nativeJobId.indexOf("@"));
        
        try {            
            sshControlAdaptor.connect(null, _publicIP, 22, null, new HashMap());            
            result = sshControlAdaptor.getOutputStagingTransfer(_nativeJobId);
        } catch (NotImplementedException ex) { throw new NoSuccessException(ex); } 
          catch (AuthenticationFailedException ex) { throw new PermissionDeniedException(ex); } 
          catch (AuthorizationFailedException ex) { throw new PermissionDeniedException(ex); } 
          catch (BadParameterException ex) { throw new NoSuccessException(ex); }
                        
        // change URL sftp:// tp jocci://
        return sftp2jocci(result);
    }

    private StagingTransfer[] sftp2jocci(StagingTransfer[] transfers) 
    {
        int index=0;
        StagingTransfer[] newTransfers = new StagingTransfer[transfers.length];
        
        for (StagingTransfer tr: transfers) 
        {
            StagingTransfer newTr = 
                    new StagingTransfer(
                        tr.getFrom().replace("sftp://", "jocci://"),
                        tr.getTo().replace("sftp://", "jocci://"),
                        tr.isAppend());
                
            newTransfers[index++] = newTr;
        }
        
        return newTransfers;
    }
    
    @Override
    public String getStagingDirectory(String nativeJobId) 
                  throws PermissionDeniedException, 
                         TimeoutException, 
                         NoSuccessException 
    {               
        String result = null;
        String _publicIP = nativeJobId.substring(nativeJobId.indexOf("@")+1, 
                                                 nativeJobId.indexOf("#"));
        
        String _nativeJobId = nativeJobId.substring(0, nativeJobId.indexOf("@"));
        
        try {            
            sshControlAdaptor.connect(null, _publicIP, 22, null, new HashMap());            
            result = sshControlAdaptor.getStagingDirectory(_nativeJobId);
        } catch (NotImplementedException ex) { throw new NoSuccessException(ex); } 
          catch (AuthenticationFailedException ex) { throw new PermissionDeniedException(ex); } 
          catch (AuthorizationFailedException ex) { throw new PermissionDeniedException(ex); } 
          catch (BadParameterException ex) { throw new NoSuccessException(ex); }
                
        return result;
    }
    
    @Override
    public JobMonitorAdaptor getDefaultJobMonitor() 
    {        
        return jOCCIJobMonitorAdaptor;
    }
    
    @Override
    public JobDescriptionTranslator getJobDescriptionTranslator() 
            throws NoSuccessException 
    {        
        return sshControlAdaptor.getJobDescriptionTranslator();        
    }
    
    @Override
    public Usage getUsage()
    {
        return new UAnd.Builder()
            .and(super.getUsage())
            .and(new U(ATTRIBUTES_TITLE))
            .and(new U(MIXIN_OS_TPL))
            .and(new U(MIXIN_RESOURCE_TPL))
            .and(new UOptional(PREFIX))
            .build();
    }  
}
