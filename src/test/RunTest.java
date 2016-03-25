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

package test;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.ogf.saga.context.Context;
import org.ogf.saga.context.ContextFactory;

import org.ogf.saga.error.NotImplementedException;
import org.ogf.saga.error.IncorrectStateException;
import org.ogf.saga.error.NoSuccessException;
import org.ogf.saga.error.PermissionDeniedException;
import org.ogf.saga.error.SagaException;

import org.ogf.saga.session.Session;
import org.ogf.saga.session.SessionFactory;

import org.ogf.saga.job.JobDescription;
import org.ogf.saga.job.JobService;
import org.ogf.saga.job.JobFactory;
import org.ogf.saga.job.Job;

import org.ogf.saga.task.State;

import org.ogf.saga.url.URL;
import org.ogf.saga.url.URLFactory;

import fr.in2p3.jsaga.impl.job.instance.JobImpl;
import fr.in2p3.jsaga.impl.job.service.JobServiceImpl;

import java.math.BigInteger;
import java.util.Random;
import org.apache.log4j.Logger;

/* *********************************************
 * *** Istituto Nazionale di Fisica Nucleare ***
 * ***      Sezione di Catania (Italy)       ***
 * ***        http://www.ct.infn.it/         ***
 * *********************************************
 * File:    jOCCIJobControlAdaptor.java
 * Authors: Giuseppe LA ROCCA
 * Email:   <giuseppe.larocca>@ct.infn.it
 * Ver.:    1.0.4
 * Date:    27 October 2015
 * *********************************************/

public class RunTest 
{        
    private static String OCCI_ENDPOINT_HOST = "";
    private static String OCCI_ENDPOINT_PORT = "";
    private static String OCCI_OS = "";
    private static String OCCI_FLAVOR = "";
    private static String OCCI_ACTION = "";
    //private static String OCCI_RESOURCE = "";
    private static String OCCI_RESOURCE_ID = "";
    private static String OCCI_VM_TITLE = "";    
    private static String OCCI_PROXY_PATH = "";
    //private static String OCCI_PREFIX = "/usr/local/rvm/gems/ruby-1.9.3-p429/bin";    
    private static String OCCI_PROTOCOL = "";
    
    // Adding FedCloud Contextualisation options here
    private static String OCCI_CONTEXT_PUBLICKEY = "";
    private static String OCCI_CONTEXT_PUBLICKEY_NAME = "";
    private static String OCCI_PUBLIC_NETWORK_ID = "";
    
    private static Logger log = Logger.getLogger(RunTest.class);
        
    public static String getNativeJobId(String jobId) 
    {
        String nativeJobId = "";
        Pattern pattern = Pattern.compile("\\[(.*)\\]-\\[(.*)\\]");
        Matcher matcher = pattern.matcher(jobId);
    
        try {
            if (matcher.find()) nativeJobId = matcher.group(2);                
            else return null;               
        } catch (Exception ex) { 
            System.out.println(ex.toString());
            return null;
        }

        return nativeJobId;
    }
                
    public static void main(String[] args) throws NotImplementedException 
    {                
        System.setProperty("saga.factory", "fr.in2p3.jsaga.impl.SagaFactoryImpl");
                        
        Session session = null;
        Context context = null;
        
        String ServiceURL = "";
        JobService service = null;
        
        Job job = null;
        String jobId = "";
        
        // Possible values: 'true' and 'false' 
       OCCI_CONTEXT_PUBLICKEY = "true";
       // Possible values: 'centos', 'ubuntu', 'root', 'cloud-user', ...
       OCCI_CONTEXT_PUBLICKEY_NAME = "centos";
       // Public Network ID
       OCCI_PUBLIC_NETWORK_ID = "public";
        
        // OCCI_PROXY_PATH (fedcloud.egi.vo)
        OCCI_PROXY_PATH = System.getProperty("user.home") + 
                          System.getProperty("file.separator") +
                          "jsaga-adaptor-jocci" +
                          System.getProperty("file.separator") +
                          "x509up_u512";
        
        // OCCI_PROXY_PATH (vo.chain-project.eu)
        /*OCCI_PROXY_PATH = System.getProperty("user.home") + 
                          System.getProperty("file.separator") +
                          "jsaga-adaptor-jocci" +
                          System.getProperty("file.separator") +
                          "x509up_u501";*/
        
        // OCCI_PROXY_PATH (trainig.egi.eu)
        /*OCCI_PROXY_PATH = System.getProperty("user.home") + 
                          System.getProperty("file.separator") +
                          "jsaga-adaptor-jocci" +
                          System.getProperty("file.separator") +
                          "x509up_u500";*/
        
        try {
            //Create an empty SAGA session            
            log.info("\nInitialize the security context for the jOCCI JSAGA adaptor");
            session = SessionFactory.createSession(false);
            
            //Modifiy this section according to the A&A schema of your middleware
            //In this example the jocci A&A schema is used            
            context = ContextFactory.createContext("jocci");
            
            // Set the user proxy
            context.setAttribute(Context.USERPROXY, OCCI_PROXY_PATH);            
            
            //Set the public key for SSH connections
            context.setAttribute(Context.USERCERT,
                    System.getProperty("user.home") + 
                    System.getProperty("file.separator") + 
                    ".ssh/id_rsa.pub");
            
            //Set the private key for SSH connections
            context.setAttribute(Context.USERKEY,
                    System.getProperty("user.home") + 
                    System.getProperty("file.separator") + 
                    ".ssh/id_rsa");
            
            // Set the userID for SSH connections
            //context.setAttribute(Context.USERID, "root");
            context.setAttribute(Context.USERID, OCCI_CONTEXT_PUBLICKEY_NAME);
            
            session.addContext(context);
            
            if (Integer.parseInt (context.getAttribute(Context.LIFETIME))/3600 > 0) 
            {
                log.info("");
                log.info("Initializing the security context for the jOCCI JSAGA adaptor [ SUCCESS ] ");
                log.info("See below security context details... ");
                log.info("User DN  = " 
                        + context.getAttribute(Context.USERID));            
                log.info("Proxy    = " 
                        + context.getAttribute(Context.USERPROXY));
                log.info("Lifetime = " 
                        + Integer.parseInt (context.getAttribute(Context.LIFETIME)) / 3600 
                        + "h.");
                log.info("CA Repos = " 
                        + context.getAttribute(Context.CERTREPOSITORY));
                log.info("Type     = "
                        + context.getAttribute(Context.TYPE));
                log.info("VO name  = "
                        + context.getAttribute(Context.USERVO));                
                                
            } else throw new RuntimeException ("Your credentials have expired!");         
                                    
         } catch (Exception ex) {
            log.error("");
            log.error("Initialize the Security context [ FAILED ] ");
            log.error("See below the stack trace... ");
            ex.printStackTrace(System.out);
            System.exit(-1);
         }                
        
        // === OCCI SETTINGS for the CESNET CLOUD RESOURCE === //        
        //OCCI_ENDPOINT_HOST = "jocci://carach5.ics.muni.cz";
        //OCCI_ENDPOINT_PORT = "11443";
        //OCCI_PROTOCOL = "https://";
        // vo.chain-project.eu
        // os_tpl#uuid_chain_reds_tthreader_fedcloud_dukan_104
        // os_tpl#uuid_chain_reds_aleph2000_fedcloud_dukan_105        
        // os_tpl#uuid_chain_reds_generic_vm_fedcloud_dukan_100
        // os_tpl#uuid_chain_reds_octave_fedcloud_dukan_101        
	// os_tpl#uuid_chain_reds_r_fedcloud_dukan_102
        // os_tpl#uuid_chain_reds_generic_www_fedcloud_dukan_110
        // os_tpl#uuid_chain_reds_wrf_fedcloud_dukan_103
        //OCCI_OS = "uuid_chain_reds_octave_fedcloud_dukan_101";
        // fedcloud.egi.eu               
        //OCCI_OS = "uuid_egi_centos_6_fedcloud_warg_130";        
        //OCCI_FLAVOR = "small";                       
                
        // === OCCI SETTINGS for the CATANIA CLOUD RESOURCE === //        
        //OCCI_ENDPOINT_HOST = "jocci://nebula-server-01.ct.infn.it";
        //OCCI_ENDPOINT_PORT = "9000";
        //OCCI_PROTOCOL = "https://";
        // vo.chain-project.eu
        // os_tpl#uuid_chain_reds_generic_vm_centos_6_6_kvm_103
	// os_tpl#uuid_chain_reds_octave_centos_6_6_kvm_102
	// os_tpl#uuid_chain_reds_octave_centos_6_6_kvm_102
        // os_tpl#uuid_chain_reds_wrf_centos_6_6_kvm_105        
        // os_tpl#uuid_chain_reds_tthreader_scientific_linux_6_5_kvm_108
        // os_tpl#uuid_chain_reds_generic_www_centos_6_6_kvm_106
        // os_tpl#uuid_chain_reds_aleph2000_scientific_linux_slc5_11_kvm_104
        //OCCI_OS = "uuid_chain_reds_generic_vm_centos_6_6_kvm_103";
        // fedcloud.egi.eu
        // os_tpl#uuid_centos6_minimal_centos_6_x_kvm_130
        // os_tpl#uuid_cernvm_scientificlinux_6_0_kvm_119
        //OCCI_OS = "uuid_cernvm_scientificlinux_6_0_kvm_119";
        //OCCI_OS = "uuid_centos6_minimal_centos_6_x_kvm_130";
        //OCCI_FLAVOR = "medium";
        
        // === OCCI SETTINGS for the INFN-BARI CLOUD RESOURCE === //        
        //OCCI_ENDPOINT_HOST = "jocci://prisma-cloud.ba.infn.it";        
        //OCCI_ENDPOINT_PORT = "8787";
        //OCCI_PROTOCOL = "http://";
        // Possible OCCI_OS values: 'generic_vm', 'octave', 'r', 'WRF', 'treethreader', 'aleph2000'
        // 623a86f7-f5f9-4bc7-816a-80e7bd6603ed => 'generic-vm'
        // 4aca9ee4-8638-4f95-824f-5128e8b0e90f => 'octave'
        // 217535d6-7315-4cb7-bc40-2aa20cfef60b => 'r'
        // a82fb047-5932-4b70-9099-67865e8b88f0 -> 'generic_www'
        // 30d841c8-fbf5-44d2-bdc9-f49df1bba2dd => 'treethreader'
        // b0df0319-5b5b-41fb-9453-2b578ee875fd => 'WRF'
        // 5f29ab3e-61f3-4f94-815f-3d6bf7a90704 => 'aleph2000'
        // 56c11ccb-c696-4fe6-b061-b5df24913580 => 'generic_www'
        //OCCI_OS = "38e758ec-0f2c-4cd2-8f2c-40e48c3ed62e";
        //OCCI_OS = "623a86f7-f5f9-4bc7-816a-80e7bd6603ed";
        //OCCI_FLAVOR = "small";
                       
        // === OCCI SETTINGS for the CIEMAT CLOUD RESOURCE === //        
        OCCI_ENDPOINT_HOST = "jocci://cloud.cesga.es";
        OCCI_ENDPOINT_PORT = "3202";
        OCCI_PROTOCOL = "https://";
        //OCCI_OS = "uuid_basic_centos_6_minimal_271";
        OCCI_OS = "uuid_centos5_7_90";
        OCCI_FLAVOR = "small";
        
        // === OCCI SETTINGS for INFN-PADOVA-STACK CLOUD RESOURCE === //       
        /*OCCI_ENDPOINT_HOST = "jocci://egi-cloud.pd.infn.it"; 
        OCCI_ENDPOINT_PORT = "8787";      
        OCCI_PROTOCOL = "https://";
        //OCCI_OS = "55f18599-e863-491a-83d4-28823b0345c0"; // [Debian/7/KVM]_fctf
        OCCI_OS = "556b231f-1daf-4bbf-b172-fb950da9c330"; //[CentOS/6/KVM]_fctf
        OCCI_FLAVOR = "m1-small";*/
                
        /*OCCI_ENDPOINT_HOST = "jocci://controller.ceta-ciemat.es";
        OCCI_ENDPOINT_PORT = "8787";
        OCCI_PROTOCOL = "https://";
        OCCI_OS = "d8145afb-f820-44d6-96a9-f491939868da";
        OCCI_FLAVOR = "m1-small";*/
        /* [ controller.ceta-ciemat.es ]
         * CHAIN-REDS
         *  225b8e1b-7403-402c-a63f-1ecbbb747db0 => generic_vm
         *  e0ec1fce-7ff0-410d-b923-bafe90818fe4 => r
         *  6c4328f9-71db-457f-81d1-1b775c721a23 => octave
         * FEDCLOUD
         *  d8145afb-f820-44d6-96a9-f491939868da => Ubuntu/12.04
         */                         
                
       /*OCCI_ENDPOINT_HOST = "jocci://stack-server-02.ct.infn.it";
       OCCI_ENDPOINT_PORT = "8787";
       OCCI_PROTOCOL = "http://";
       // - [CentOS/6/KVM]_EGI_fedcloud (OK), OCCI_PUBLICKEY_NAME=centos
       OCCI_OS = "2a612491-d544-4b9c-af3f-994ac7f61d2a"; 
       // - [Ubuntu/14.04/KVM]_EGI_fedcloud (OK), OCCI_PUBLICKEY_NAME=ubuntu
       //OCCI_OS = "74f88b3c-a70f-4c2d-9df6-8b2ba766701f";
       OCCI_FLAVOR = "m1-medium";*/
                                
       OCCI_VM_TITLE = "jOCCI_";         
       OCCI_ACTION = "create";
                
       BigInteger result = new BigInteger(30, new Random());       
       OCCI_VM_TITLE += result;
        
        try {    
            log.info("");
            log.info("Initialize the JobService context... ");
            
            // Start OCCI Actions ...
            if (OCCI_ACTION.equals("list")) {

                ServiceURL = OCCI_ENDPOINT_HOST + ":" 
                            + OCCI_ENDPOINT_PORT 
                            + System.getProperty("file.separator") + "?"   
                            + "protocol=" + OCCI_PROTOCOL
                            + "&network=" + OCCI_PUBLIC_NETWORK_ID
                            + "&credentials_publickey=" + OCCI_CONTEXT_PUBLICKEY 
                            + "&credentials_publickey_name=" + OCCI_CONTEXT_PUBLICKEY_NAME 
                            + "&proxy_path=" + OCCI_PROXY_PATH;
            }
            
            else if (OCCI_ACTION.equals("create")) {                
                        
                        ServiceURL = OCCI_ENDPOINT_HOST + ":" 
                                    + OCCI_ENDPOINT_PORT 
                                    + System.getProperty("file.separator") + "?"
                                    + "protocol=" + OCCI_PROTOCOL
                                    + "&network=" + OCCI_PUBLIC_NETWORK_ID
                                    + "&attributes_title=" + OCCI_VM_TITLE 
                                    + "&mixin_os_tpl=" + OCCI_OS 
                                    + "&mixin_resource_tpl=" + OCCI_FLAVOR 
                                    + "&credentials_publickey=" + OCCI_CONTEXT_PUBLICKEY
                                    + "&credentials_publickey_name=" + OCCI_CONTEXT_PUBLICKEY_NAME 
                                    + "&proxy_path=" + OCCI_PROXY_PATH;
            }
            
            else if (OCCI_ACTION.equals("describe")) {
                        
			ServiceURL = OCCI_ENDPOINT_HOST + ":" 
                                    + OCCI_ENDPOINT_PORT 
                                    + System.getProperty("file.separator") + "?"
                                    + "protocol=" + OCCI_PROTOCOL
                                    + "&network=" + OCCI_PUBLIC_NETWORK_ID
                                    + "&resourceID=" + OCCI_RESOURCE_ID 
                                    + "&credentials_publickey=" + OCCI_CONTEXT_PUBLICKEY 
                                    + "&credentials_publickey_name=" + OCCI_CONTEXT_PUBLICKEY_NAME 
                                    + "&proxy_path=" + OCCI_PROXY_PATH;
            }
            
            else if (OCCI_ACTION.equals("delete")) {

			ServiceURL = OCCI_ENDPOINT_HOST + ":" 
                                + OCCI_ENDPOINT_PORT 
                                + System.getProperty("file.separator") + "?"  
                                + "protocol=" + OCCI_PROTOCOL
                                + "&network=" + OCCI_PUBLIC_NETWORK_ID
                                + "&resourceID=" + OCCI_RESOURCE_ID 
                                + "&credentials_publickey=" + OCCI_CONTEXT_PUBLICKEY 
                                + "&credentials_publickey_name=" + OCCI_CONTEXT_PUBLICKEY_NAME 
                                + "&proxy_path=" + OCCI_PROXY_PATH;
            }            
                                
            URL serviceURL = URLFactory.createURL(ServiceURL);
            log.info("serviceURL = " + serviceURL);            
            service = JobFactory.createJobService(session, serviceURL);  
                        
            // ========================================== //
            // === SUBMITTING VM using jOCCI standard === //
            // ========================================== //
            
            if (OCCI_ACTION.equals("create")) 
            {                                                
                // Create the job description
                JobDescription desc = JobFactory.createJobDescription();
                desc.setAttribute(JobDescription.EXECUTABLE, "/bin/bash");                
	           
                desc.setAttribute(JobDescription.OUTPUT, "output.txt");
                desc.setAttribute(JobDescription.ERROR, "error.txt");
                  
                desc.setVectorAttribute(JobDescription.ARGUMENTS, 
                    new String[]{"job-generic.sh"});
                    
                desc.setVectorAttribute(
                    JobDescription.FILETRANSFER,
                    new String[]{
                        System.getProperty("user.home") + 
                        System.getProperty("file.separator") +
                        "jsaga-adaptor-jocci" +
                        System.getProperty("file.separator") +
                        "job-generic.sh>job-generic.sh",
                        
                        System.getProperty("user.home") + 
                        System.getProperty("file.separator") +
                        "jsaga-adaptor-jocci" +
                        System.getProperty("file.separator") +
                        "output.txt<output.txt",
                        
                        System.getProperty("user.home") + 
                        System.getProperty("file.separator") +
                        "jsaga-adaptor-jocci" +
                        System.getProperty("file.separator") +
                        "error.txt<error.txt"}
                    );                
                
                // ================================= //
                // === CREATE a new job instance === //
                // ================================= //
                job = service.createJob(desc);
                job.run();                                
                            
                // Getting the jobId
                jobId = job.getAttribute(Job.JOBID);
                log.info("");
                log.info("Job instance created: ");
                log.info(jobId);
                
                try {
			((JobServiceImpl)service).disconnect();                        
		} catch (NoSuccessException ex) {
                        log.error("See below the stack trace... ");
                        ex.printStackTrace(System.out);					
		}
                log.info("");
                log.info("Closing session...");
                session.close();
                                                
                // =========================== //
                // === CHECKING job status === //
                // =========================== //                
                
                //Create an empty SAGA session
                log.info("");
                log.info("Re-initialize the security context for the jOCCI JSAGA adaptor");
                session = SessionFactory.createSession(false);
            
                //Modifiy this section according to the A&A schema of your middleware
                //In this example the jocci A&A schema is used
                context = ContextFactory.createContext("jocci");
                
                // Set the user proxy
                context.setAttribute(Context.USERPROXY, OCCI_PROXY_PATH);                                
            
                //Set the public key for SSH connections
                context.setAttribute(Context.USERCERT,
                        System.getProperty("user.home") + 
                        System.getProperty("file.separator") + 
                        ".ssh/id_rsa.pub");
            
                //Set the private key for SSH connections
                context.setAttribute(Context.USERKEY,
                        System.getProperty("user.home") + 
                        System.getProperty("file.separator") + 
                        ".ssh/id_rsa");
                
                // Set the userID for SSH connections
                //context.setAttribute(Context.USERID, "root");
                context.setAttribute(Context.USERID, OCCI_CONTEXT_PUBLICKEY_NAME);
            
                session.addContext(context);
            
                ServiceURL = OCCI_ENDPOINT_HOST + ":" 
                        + OCCI_ENDPOINT_PORT 
                        + System.getProperty("file.separator") + "?"                                                                       
                        + "protocol=" + OCCI_PROTOCOL
                        + "&network=" + OCCI_PUBLIC_NETWORK_ID
                        + "&action=" + OCCI_ACTION 
                        + "&attributes_title=" + OCCI_VM_TITLE 
                        + "&mixin_os_tpl=" + OCCI_OS 
                        + "&mixin_resource_tpl=" + OCCI_FLAVOR 
                        + "&credentials_publickey=" + OCCI_CONTEXT_PUBLICKEY
                        + "&credentials_publickey_name=" + OCCI_CONTEXT_PUBLICKEY_NAME 
                        + "&proxy_path=" + OCCI_PROXY_PATH;
                
                serviceURL = URLFactory.createURL(ServiceURL);                
                JobService service1 = JobFactory.createJobService(session, serviceURL);  
                
                Job job1 = service1.getJob(getNativeJobId(jobId));                                
                
                log.info("");
                log.info("Fetching the status of the job ");
                log.info ("[ " + getNativeJobId(jobId) + " ] ");
                
                log.info("");
                log.info("JobID [ " 
                        + jobId 
                        + " ] ");
                
                boolean jobIsDone = false;
                //String nativeJobId = "";
                
                while(!jobIsDone) 
                {
                    // display final state
                    State state = null;
                    
                    try { 
                        state = job1.getState();
                        log.info("Current Status = " + state.name());
                        
                        String executionHosts[];
                        executionHosts = job1.getVectorAttribute(Job.EXECUTIONHOSTS);
                        log.info("Execution Host = " + executionHosts[0]);
                        
                    } catch (Exception ex) {
                        log.error("");
			log.error("Error in getting job status... [ FAILED ] ");
			log.error(ex.toString());
			log.error("Cause :" + ex.getCause());
                    }
                    
                    if (State.CANCELED.compareTo(state) == 0) {
                        log.info("");
			log.info("Job Status = CANCELED ");
                    } else {
                        
                        if (State.DONE.compareTo(state) == 0) 
                        {
                            jobIsDone = true;
                            
                            String exitCode = job1.getAttribute(Job.EXITCODE);
                            log.info("");
                            log.info("Final Job Status = DONE");
                            if (Integer.parseInt (exitCode) == 0)
                                 log.info("Exit Code (0) [ SUCCESS ] ");
                            else log.info("Exit Code [ " + exitCode + " ] ");
                        
                            log.info("");
                            log.info("Retrieving job results.");
                            log.info("This operation may take a few minutes to complete...");
                            
                            // ========================================== //
                            // === EXECUTING post-staging and cleanup === //
                            // ========================================== //
                            try { 
                                ((JobImpl)job1).postStagingAndCleanup();
                                                        
                            } catch (NotImplementedException ex) { ex.printStackTrace(System.out); }
                              catch (PermissionDeniedException ex) { ex.printStackTrace(System.out); }
                              catch (IncorrectStateException ex) { ex.printStackTrace(System.out); }
                              catch (NoSuccessException ex) { ex.printStackTrace(System.out); }
                        
                            log.info("Job outputs retrieved [ SUCCESS ] ");
                            try { ((JobServiceImpl)service1).disconnect();
                            } catch (NoSuccessException ex) { 
                                log.error("Job outputs retrieved [ FAILED ] ");
                                log.error("See below the stack trace... ");
                                ex.printStackTrace(System.out); 
                            }
                            session.close();
                            break;
                        } // end Sate.DONE
                    
                        else if (State.FAILED.compareTo(state) == 0) 
                        {
                            try {
                                    String exitCode = job1.getAttribute(Job.EXITCODE);
                                    log.info("");
                                    log.info("Job Status = FAILED");
                                    log.info("Exit Code [ " + exitCode + " ] ");
                            } catch (SagaException e) { log.error("Job failed."); }
                        }
                    
                        else {
                            log.info("");
                            log.info("Unexpected job status: " + state);
                        }
                    }
                
                    try { Thread.sleep(10000);
                    } catch (InterruptedException ex) { ex.printStackTrace(System.out); }
                } // end while                                
            } // end if (OCCI_ACTION.equals("create"))
            
            log.info("");
            log.info("Initialize the JobService context [ SUCCESS ] ");
                
	} catch (Exception ex) {
                log.error("");
		log.error("Initialize the JobService context [ FAILED ] ");
		log.error("See below the stack trace... ");                
                ex.printStackTrace(System.out);
        }                
    }    
}
