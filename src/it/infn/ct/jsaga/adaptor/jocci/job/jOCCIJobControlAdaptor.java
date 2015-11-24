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
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.URI;
import java.net.UnknownHostException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
 * Ver.:    1.0.7
 * Date:    20 November 2015
 * *********************************************/

public class jOCCIJobControlAdaptor extends jOCCIAdaptorCommon
                                    implements JobControlAdaptor, 
                                               StagingJobAdaptorTwoPhase, 
                                               CleanableJobAdaptor
{     
    
  protected static final String ATTRIBUTES_TITLE = "attributes_title";
  protected static final String MIXIN_OS_TPL = "mixin_os_tpl";
  protected static final String MIXIN_RESOURCE_TPL = "mixin_resource_tpl";
  protected static final String CREDENTIALS_PUBLICKEY = "credentials_publickey";
  protected static final String CREDENTIALS_PUBLICKEY_NAME = "credentials_publickey_name";
  //protected static final String PREFIX = "prefix";  
    
  // MAX tentatives before to gave up to connect the VM server.
  private final int MAX_CONNECTIONS = 10;
  
  private static final Logger log = 
          Logger.getLogger(jOCCIJobControlAdaptor.class);
      
  private jOCCIJobMonitorAdaptor jOCCIJobMonitorAdaptor = 
            new jOCCIJobMonitorAdaptor();
    
  private SSHJobControlAdaptor sshControlAdaptor = 
            new SSHJobControlAdaptor();
  
  //private String prefix = "";
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
  private String context_publickey = "";  
  private String context_publickey_name = "";  
    
  enum ACTION_TYPE { list, delete, describe, create; }
    
  String[] IP = new String[2];
  String networkInterfaceLocation = "";
  String networkInterfaceLocation_stripped = "";
  Resource vm_resource = null;
  
  public boolean testIpAddress(byte[] testAddress)
  {
        Inet4Address inet4Address;
        boolean result=false;

        try {
            inet4Address = (Inet4Address) InetAddress.getByAddress(testAddress);
            result = inet4Address.isSiteLocalAddress();
        } catch (UnknownHostException ex) {             
            java.util.logging.Logger
                    .getLogger(jOCCIJobControlAdaptor.class.getName())
                    .log(Level.SEVERE, null, ex); 
        }

        return result;
  }
  
  /* Check if the IP is public or not */
  public String checkIP (String _ip)
  {
    String publicIP = null;
    String tmp  = "";
    int k=0;
    boolean check = false;

    Pattern patternID =
        Pattern.compile("^(\\d{1,3})\\.(\\d{1,3})\\.(\\d{1,3})\\.(\\d{1,3})$");
        
    Matcher matcher = patternID.matcher(_ip);
    while (matcher.find()) {
        String _IP0 = matcher.group(1).replace(".","");
        String _IP1 = matcher.group(2).replace(".","");
        String _IP2 = matcher.group(3).replace(".","");
        String _IP3 = matcher.group(4).replace(".","");
            
        //CHECK if IP[k] is PRIVATE or PUBLIC
        byte[] rawAddress = {
            (byte) Integer.parseInt(_IP0),
            (byte) Integer.parseInt(_IP1),
            (byte) Integer.parseInt(_IP2),
            (byte) Integer.parseInt(_IP3)
        };

        if (!testIpAddress(rawAddress)) {
            // Saving the public IP
            publicIP = tmp;
            check = true;
        }
                  
        k++;    
    } // end-while
        
    return publicIP;
  }    
  
  public String getPublicKey (String file)
  {
        FileInputStream fis = null;
        String _publicKey = "";
        
        try {           
            File f = new File(file);
            fis = new FileInputStream(f);
            DataInputStream dis = new DataInputStream(fis);
            byte[] keyBytes = new byte[(int) f.length()];
            dis.readFully(keyBytes);
            dis.close();
            _publicKey = new String (keyBytes).trim();            
        } catch (IOException ex) {
            java.util.logging.Logger
                    .getLogger(jOCCIJobControlAdaptor.class.getName())
                    .log(Level.SEVERE, null, ex);
        } finally {
            try {
                fis.close();
            } catch (IOException ex) {
                java.util.logging.Logger
                        .getLogger(jOCCIJobControlAdaptor.class.getName())
                        .log(Level.SEVERE, null, ex);
            }
        }
        
        return (_publicKey);
  }
  
  public String get_publicNet (Client client)
  {
    String public_network = "";
        
    try {                
        List<URI> uris = client.list("network");
               
        if (!uris.isEmpty()) {
            // Listing networks
            for (URI uri : uris) {
                if ((uri.toString()).contains("public"))
                    public_network = uri.toString();

                log.info("NetworkID = " + uri.toString());
            }
        }
            
    } catch (CommunicationException ex) {
        java.util.logging.Logger
            .getLogger(jOCCIJobControlAdaptor.class.getName())
            .log(Level.SEVERE, null, ex);
    }
        
    return public_network;
  }
    
  public String[] getNetworkLocation (List<Entity> entities)
  {
    // This method retrieves the deafult IP address assigned to the 
    // computing resource and the private networkID to be unlinked
    String[] result = new String [2];
    for (Entity entity : entities)
    {
        Resource resource = (Resource) entity;                
        Set<Link> links = resource.getLinks(NetworkInterface.TERM_DEFAULT);
        for (Link link : links)
        {
            result[0] = link.getValue(IPNetworkInterface.ADDRESS_ATTRIBUTE_NAME);
            if (checkIP(result[0]) == null) {
                networkInterfaceLocation = link.getKind().getLocation() + link.getId();
                networkInterfaceLocation_stripped =
                    networkInterfaceLocation
                    .replace("/network/interface/","");
                                
                networkInterfaceLocation = "/network/interface/" 
                    + networkInterfaceLocation_stripped;
                                
                result[1] = networkInterfaceLocation;
            }
        }
    }
        
    return (result);
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
       
    //prefix = (String) attributes.get(PREFIX);       
    action = (String) attributes.get(ACTION);
    auth = (String) attributes.get(AUTH);
    resource = (String) attributes.get(RESOURCE);
    attributes_title = (String) attributes.get(ATTRIBUTES_TITLE);
    mixin_os_tpl = (String) attributes.get(MIXIN_OS_TPL);
    mixin_resource_tpl = (String) attributes.get(MIXIN_RESOURCE_TPL);
      
    //context_publickey = (String) attributes.get("credentials_publickey");
    context_publickey = (String) attributes.get(CREDENTIALS_PUBLICKEY);
    //context_publickey_name = (String) attributes.get("credentials_publickey_name");
    context_publickey_name = (String) attributes.get(CREDENTIALS_PUBLICKEY_NAME);
       
    // Check if OCCI path is set                
    /*if ((prefix != null) && (new File((prefix)).exists()))
        prefix += System.getProperty("file.separator");
    else prefix = "";*/
              
    Endpoint = "https://" 
        + host + ":" + port 
        + System.getProperty("file.separator");
       
    OCCI_ENDPOINT_HOST = host;
    OCCI_ENDPOINT_PORT = port;
      
    log.info("");
    log.info("See below the details: ");
    log.info("");
    //log.info("PREFIX    = " + prefix);
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
                  
    if (context_publickey.equals("true")) {        
        log.info("");
        log.info("EGI FedCloud Contextualisation options:");
        log.info("PKEY CONT.  = " + context_publickey);
        log.info("PKEY NAME   = " + context_publickey_name);
    }
    
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
        
    String _nativeJobId = 
            nativeJobId.substring(0, nativeJobId.indexOf("@"));
        
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
        
        log.info("");
        log.info("Stopping the SSH process on the remote compute"); 
        
        try {            
            sshControlAdaptor.connect(null, _publicIP, 22, null, new HashMap());            
            sshControlAdaptor.clean(_nativeJobId);                    
                    
        } catch (NotImplementedException ex) { throw new NoSuccessException(ex); } 
          catch (AuthenticationFailedException ex) { throw new PermissionDeniedException(ex); } 
          catch (AuthorizationFailedException ex) { throw new PermissionDeniedException(ex); } 
          catch (BadParameterException ex) { throw new NoSuccessException(ex); }
                
        // Stop and delete resource
        log.info("");log.info("[ STOP & DELETE ]");
        log.info("- Trigger a 'stop' action to the resource");                
               
        HTTPAuthentication authentication =
            new VOMSAuthentication(user_cred);
                                
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
                        
    } catch (EntityBuildingException ex) {
            java.util.logging.Logger
                .getLogger(jOCCIJobControlAdaptor.class.getName())
                .log(Level.SEVERE, null, ex); 
    } catch (CommunicationException ex) {
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
    //String networkInterfaceLocation = "";
    //String networkInterfaceLocation_stripped = "";
    //Resource vm_resource = null;
    String publicIP = "";
        
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
                           
            log.info("");
            if (context_publickey.equals("true")) {                
                log.info("EGI FedCloud Contextualisation options:");                                
                
                log.info("org.openstack.credentials.publickey.data = " 
                    + getPublicKey(credential
                        .getSSHCredential()
                        .getPublicKeyFile()
                        .getPath()));
                
                log.info("org.openstack.credentials.publickey.name = " 
                    + context_publickey_name);
            } else log.info("No contextualization set for the resource.");
                                
            HTTPAuthentication authentication =
            new VOMSAuthentication(user_cred);
                
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
            log.info("[ TEMPLATES ]");
            log.info("- Available os templates mixins ...");
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
                if (context_publickey.equals("true")) {
                // Add SSH public key 
                compute.addMixin(model.findMixin(URI.create("http://schemas.openstack.org/instance/credentials#public_key")));
                compute.addAttribute("org.openstack.credentials.publickey.name", 
                        context_publickey_name);
                
                compute.addAttribute("org.openstack.credentials.publickey.data", 
                        getPublicKey(credential
                        .getSSHCredential()
                        .getPublicKeyFile()
                        .getPath()));
                }
                
                compute.setTitle(attributes_title);
                log.info(mixin.toText());
                    
                log.info("");
                // Creating a new VM resource
                URI location = client.create(compute);                    
                    
                // Getting the list of available running resources
                List<URI> list = client.list();                                        
                                                                                
                // Listing all the active resources
                URI uri_location = null;
                log.info("[ LIST ]");
                log.info("- Retieve the list of VMs running on the cloud site");

                for (URI uri : list) {
                    if (uri.toString().contains("compute")) {
                        log.info("~ " + uri);
                        uri_location = uri;                                
                    }
                 }

                 log.info("");
                 log.info("================= [ R E P O R T ] =================");
                    
                 // 3.) Describe resource
                 log.info("");log.info("[ DESCRIPTION ]");
                 if (location != null) {
                    log.info("- Getting VM settings for the resource");
                    log.info(uri_location);
                 } else log.error("Some errors occurred during the creation of a new resource.");
                                        
                 List<Entity> entities = client.describe(uri_location);
                 Resource resource = (Resource) entities.get(0);
                 vm_resource = resource;
                 log.info(entities.get(0).toText());                    
                    
                 SimpleDateFormat ft = 
                    new SimpleDateFormat ("E yyyy.MM.dd 'at' hh:mm:ss a zzz");
           
                 log.info("");
                 log.info("Waiting the remote VM finishes the boot!");
                 log.info(ft.format(date));
                    
                 log.info("");
                 log.info("Waiting the status of the following resource becomes ACTIVE");
                 log.info(uri_location);
                 log.info("This operation may take few minutes to complete. Please wait!");
                 log.info("");
                 
                 try {
                    Thread.sleep(6000);
                 } catch (InterruptedException ex) { 
                    java.util.logging.Logger
                        .getLogger(jOCCIJobControlAdaptor.class.getName())
                        .log(Level.SEVERE, null, ex);
                 }
                                        
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
                                
                            // Getting IP address and NetworkLocation 
                            // of the resource
                            String IP = getNetworkLocation(entities)[0];
                            
                            log.info("");
                            log.info("The compute resource is now active!"); 
                            log.info("URI = " + uri_location);
                            log.info("IP  = " + IP);
                            log.info("");
                            log.info("Check whether the compute resource has a public IP or not ");
                                
                            if (checkIP(IP) != null) {
                                log.info("The compute resource has a *public* IP [ " + IP + " ]");
                                publicIP=IP;
                            } else {                                    
                                log.info("The compute resource has a *private* IP [ " + IP + " ]");
                                    
                                // Find a network resource that provides public IPs 
                                String public_network = "";
                                List<URI> uris = client.list("network");
                                log.info("Listing available networks for the given resource");
                                if (!uris.isEmpty()) {
                                    // Listing networks
                                    for (URI uri : uris) {
                                        if ((uri.toString()).contains("public"))
                                            public_network = uri.toString();
                                        
                                        log.info("NetworkID = " + uri.toString());
                                    }
                                }
                                    
                                if (public_network != null && !public_network.isEmpty()) 
                                {
                                    log.info("");
                                    log.info("Public Network = " + public_network);
                                    log.info("Unlink the network interface that doesn't have public IPs");                                        
                                    log.info("NetworkID = " + getNetworkLocation(entities)[1]);                                        
                                    client.delete(URI.create((getNetworkLocation(entities)[1])));

                                    IPNetworkInterface ipni = eb.getIPNetworkInterface();
                                    ipni.setSource(vm_resource);
                                    ipni.setTarget(public_network);
                                    location = client.create(ipni);
                                        
                                    // Sleeping for a while
                                    try { 
                                        Thread.sleep(5000);
                                    } catch (InterruptedException ex) { 
                                        ex.printStackTrace(System.out); 
                                    }

                                    log.info("");log.info("[ DESCRIPTION ]");
                                    entities = client.describe(uri_location);
                                    log.info(entities.get(0).toText());
                                        
                                    String tmp = "";
                                    log.info("");
                                    log.info("- Get the available IPs for the given VM");
                                    for (Entity entity : entities) {
                                        resource = (Resource) entity;
                                        Set<Link> links = resource
                                            .getLinks(NetworkInterface.TERM_DEFAULT);
                                                    
                                        for (Link link : links) {
                                            tmp = link.getValue(IPNetworkInterface.ADDRESS_ATTRIBUTE_NAME);
                                            log.info("IP = " + tmp);
                                            if (checkIP(tmp) != null)                           
                                                publicIP=tmp;
                                        }
                                    } // end-for
                                } // end-if
                            } // end-else
                                
                            log.info("Public IP address = " + publicIP);                                
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
                                    tc.connect(publicIP, 22);
                                    InputStream instr = tc.getInputStream();
                                                    
                                    ret_read = instr.read(buff);                            
                                    if (ret_read > 0)
                                    {
                                        log.info("SSH daemon has started [ OK ] ");
                                        tc.disconnect();
                                        flag=false;
                                    }
                                 } catch (IOException e) {
                                    log.info("The destination host is unreachable. Sleeping for a while... ");
                                    try {
                                        Thread.sleep(60000);
                                    } catch (InterruptedException ex) { 
                                        java.util.logging.Logger
                                            .getLogger(jOCCIJobControlAdaptor.class.getName())
                                            .log(Level.SEVERE, null, ex);
                                    }
                                        
                                    MAX++;
                                 }
                            } // end-while
                                
                            date = new Date();
                            log.info(ft.format(date));
                    
                            jOCCIJobMonitorAdaptor.setSSHHost(publicIP);
        
                            try {            
                                sshControlAdaptor.connect(null, publicIP, 22, null, new HashMap());            
                            } catch (NotImplementedException ex) { throw new NoSuccessException(ex); } 
                              catch (AuthenticationFailedException ex) { throw new PermissionDeniedException(ex); } 
                              catch (AuthorizationFailedException ex) { throw new PermissionDeniedException(ex); } 
                              catch (BadParameterException ex) { throw new NoSuccessException(ex); }
                
                            result = sshControlAdaptor.submit(jobDesc, checkMatch, uniqId) 
                                    + "@" + publicIP + "#" + uri_location;
                                
                            break;
                     } else {                                                        
                        try {
                            // Sleeping for a while ...
                            Thread.sleep(5000);
                        } catch (InterruptedException ex) {
                            java.util.logging.Logger
                                .getLogger(jOCCIJobControlAdaptor.class.getName())
                                .log(Level.SEVERE, null, ex);
                        }                                                
                      } //end-else
                    } //end-while

            } else log.error("No OS template mixins available!");
                
        } catch (RenderingException ex) {
            java.util.logging.Logger
                .getLogger(jOCCIJobControlAdaptor.class.getName())
                .log(Level.SEVERE, null, ex);
        } catch (InvalidAttributeValueException ex) {
            java.util.logging.Logger
                .getLogger(jOCCIJobControlAdaptor.class.getName())
                .log(Level.SEVERE, null, ex);
        } catch (EntityBuildingException ex) {
            java.util.logging.Logger
                .getLogger(jOCCIJobControlAdaptor.class.getName())
                .log(Level.SEVERE, null, ex);
        } catch (AmbiguousIdentifierException ex) {
            java.util.logging.Logger
                .getLogger(jOCCIJobControlAdaptor.class.getName())
                .log(Level.SEVERE, null, ex);
        } catch (CommunicationException ex) {
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
             
    // change URL sftp:// to jocci://
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
        .and(new U(CREDENTIALS_PUBLICKEY))
        .and(new U(CREDENTIALS_PUBLICKEY_NAME))
        //.and(new UOptional(PREFIX))
        .build();
  }  
}
