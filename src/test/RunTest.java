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

import org.apache.log4j.Logger;

/* *********************************************
 * *** Istituto Nazionale di Fisica Nucleare ***
 * ***      Sezione di Catania (Italy)       ***
 * ***        http://www.ct.infn.it/         ***
 * *********************************************
 * File:    rOCCIJobControlAdaptor.java
 * Authors: Giuseppe LA ROCCA, Diego SCARDACI
 * Email:   <giuseppe.larocca, diego.scardaci>@ct.infn.it
 * Ver.:    1.0.4
 * Date:    23 September 2014
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
    private static String OCCI_PREFIX = "";
    
    // Adding FedCloud Contextualisation options here
    private static String OCCI_CONTEXT_USER_DATA = "";    
    
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
        
        // Setting the EGI FedCloud Contextualisation options
        /*OCCI_CONTEXT_USER_DATA = 
                "file://" +
                System.getProperty("user.home") + 
                System.getProperty("file.separator") +
                "jsaga-adaptor-jocci" +
                System.getProperty("file.separator") +
                "tmpfedcloud.login";*/
        
        // OCCI_PROXY_PATH (fedcloud.egi.vo)
        /*OCCI_PROXY_PATH = System.getProperty("user.home") + 
                          System.getProperty("file.separator") +
                          "jsaga-adaptor-jocci" +
                          System.getProperty("file.separator") +
                          "x509up_u512";*/
        
        // OCCI_PROXY_PATH (vo.chain-project.eu)
        OCCI_PROXY_PATH = System.getProperty("user.home") + 
                          System.getProperty("file.separator") +
                          "jsaga-adaptor-jocci" +
                          System.getProperty("file.separator") +
                          "x509up_u501";
        
        // OCCI_PROXY_PATH (bes)
        /*OCCI_PROXY_PATH = System.getProperty("user.home") + 
                          System.getProperty("file.separator") +
                          "jsaga-adaptor-jocci" +
                          System.getProperty("file.separator") +
                          "x509up_u500";*/
        
        try {
            //Create an empty SAGA session            
            log.info("\nInitialize the security context for the rOCCI JSAGA adaptor");
            session = SessionFactory.createSession(false);
            
            //Modifiy this section according to the A&A schema of your middleware
            //In this example the rocci A&A schema is used            
            context = ContextFactory.createContext("jocci");
            
            // Set teh user proxy
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
            context.setAttribute(Context.USERID, "root");
            
            session.addContext(context);
            
            if (Integer.parseInt (context.getAttribute(Context.LIFETIME))/3600 > 0) 
            {
                log.info("");
                log.info("Initializing the security context for the rOCCI JSAGA adaptor [ SUCCESS ] ");
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
        OCCI_ENDPOINT_HOST = "jocci://carach5.ics.muni.cz";
        OCCI_ENDPOINT_PORT = "11443";
        // Possible OCCI_OS values: 'generic_vm', 'octave' and 'r' 
        // os_tpl#uuid_chain_reds_generic_vm_fedcloud_dukan_100 => generic_vm
        // os_tpl#uuid_chain_reds_octave_fedcloud_dukan_101 => octave
	// os_tpl#uuid_chain_reds_r_fedcloud_dukan_102 => r
        // os_tpl#uuid_chain_reds_generic_www_fedcloud_dukan_110 => 'generic_www'
        OCCI_OS = "uuid_chain_reds_generic_vm_fedcloud_dukan_100";
        OCCI_FLAVOR = "small";
        
        // === SETTINGS for the CESGA CLOUD RESOURCE === //
        //OCCI_ENDPOINT_HOST = "rocci://cloud.cesga.es";        
        //OCCI_ENDPOINT_PORT = "3202";
        // Possible OCCI_OS values: 'generic_vm', 'octave' and 'r'        
        // os_tpl#uuid_chain_reds_generic_vm_sl6_4_364 => generic_vm
        // os_tpl#uuid_chain_reds_octave_sl6_4_365 => octave
	// os_tpl#uuid_chain_reds_r_sl6_4_366 => r
        //OCCI_OS = "uuid_chain_reds_generic_vm_sl6_4_364";
        //OCCI_FLAVOR = "small";
        
        // === OCCI SETTINGS for the KTH CLOUD RESOURCE === //
        //OCCI_ENDPOINT_HOST = "rocci://egi.cloud.pdc.kth.se";
        //OCCI_ENDPOINT_PORT = "443";
        // Possible OCCI_OS values: 'generic_vm', 'octave' and 'r'
        //OCCI_OS = "egi_debian";
        //OCCI_FLAVOR = "small";
        
        // === OCCI SETTINGS for the FZ Jülich CLOUD RESOURCE === //
        //OCCI_ENDPOINT_HOST = "rocci://egi-cloud.zam.kfa-juelich.de";
        //OCCI_ENDPOINT_PORT = "8787";
        // Possible OCCI_OS values: 'generic_vm', 'octave' and 'r'
        // 053236c6-1eaa-43ec-b738-10dc4969d091 => 'chain-generic'
        // 464f2315-9f8b-4c8d-9235-1b7a8aa520ed => 'chain-octave'
        // dc87c573-8f09-46b1-ac0f-8be9743bd74f => 'chain-r'
        //OCCI_OS = "464f2315-9f8b-4c8d-9235-1b7a8aa520ed";
        //OCCI_OS = "7ea08f66-04ac-46b0-914c-ae76d0450b00";
        //OCCI_FLAVOR = "small";
        
        // === OCCI SETTINGS for the GRNET CLOUD RESOURCE === //
        //OCCI_ENDPOINT_HOST = "rocci://okeanos-occi2.hellasgrid.gr";
        //OCCI_ENDPOINT_PORT = "9000";
        // Possible OCCI_OS values: 'generic_vm', 'octave' and 'r'
        // vmcatcher-nkoz0e-virt-octave-3-6-3 => 'octave'
        // vmcatcher-nkp6v5-generic_www => 'generic_www'
        // vmcatcher-nkp125-virt-r_2-15-2 => 'r'
        // vmcatcher-nkoy5z-generic-vm => 'generic-vm'
        //OCCI_OS = "vmcatcher-nkoy5z-generic-vm";        
        //OCCI_FLAVOR = "c1r2048d10drbd";
        
        // === OCCI SETTINGS for the GWDG CLOUD RESOURCE === //
        //OCCI_ENDPOINT_HOST = "rocci://occi.cloud.gwdg.de";
        //OCCI_ENDPOINT_PORT = "3100";
        // Possible OCCI_OS values: 'generic_vm', 'octave' and 'r'
        //os_tpl#uuid_fctf_nagios_49
	//os_tpl#uuid_gwdg_centos_6_5_50
	//os_tpl#uuid_gwdg_scientific_linux_6_5_54
	//os_tpl#uuid_gwdg_ubuntu_12_04_4_55
	//os_tpl#uuid_gwdg_ubuntu_14_04_56
        //OCCI_OS = "uuid_generic_vm_46";
        //OCCI_OS = "uuid_gwdg_scientific_linux_6_5_54";
        //OCCI_FLAVOR = "small";
        
        // === OCCI SETTINGS for the VESPA (OpenStack) CLOUD RESOURCE === //
        //OCCI_ENDPOINT_HOST = "jocci://stack-server-01.cloud.dfa.unict.it";
        //OCCI_ENDPOINT_PORT = "8787";        
        // Possible OCCI_OS values: 'generic_vm', 'octave' and 'r'
        // os_tpl#5949cec9-dce0-4d6f-9c55-8e46a9c78ee9 ==> generic_vm
	// os_tpl#0645246b-2937-43c0-9a32-aefaf4123b9c ==> octave
	// os_tpl#978edc01-b944-4b05-b096-6fc951161a1d ==> r
        // os_tpl#e2e16362-093d-427e-861a-4102be127112 ==> generic_www
        //OCCI_OS = "e2e16362-093d-427e-861a-4102be127112";
        //OCCI_FLAVOR = "m1-medium";
        
        // === OCCI SETTINGS for the CATANIA (OpenNebula) CLOUD RESOURCE === //
        //OCCI_ENDPOINT_HOST = "jocci://nebula-server-01.ct.infn.it";
        //OCCI_ENDPOINT_PORT = "9000";
        // Possible OCCI_OS values: 'generic_vm', 'octave' and 'r'
        // os_tpl#uuid_generic_vm_19 ==> generic_vm
	// os_tpl#uuid_octave_20 ==> octave
	// os_tpl#uuid_r_7 ==> r                
        // os_tpl#uuid_appwrf_51 ==> wrf
        // os_tpl#uuid_blender4_61 ==> blender
        // os_tpl#uuid_tthread_72 ==> tthread_72
        // os_tpl#uuid_generic_www_35 ==> generic_www
        //OCCI_OS = "uuid_generic_vm_19";
        //OCCI_FLAVOR = "small";
        
        // === OCCI SETTINGS for the CATANIA (OpenStack) CLOUD RESOURCE === //
        //OCCI_ENDPOINT_HOST = "jocci://stack-server-01.ct.infn.it";
        //OCCI_ENDPOINT_PORT = "8787";
        // Possible OCCI_OS values: 'generic_vm', 'octave' and 'r'
        // f36b8eb8-8247-4b4f-a101-18c7834009e0 ==> generic_vm
	// bb623e1c-e693-4c7d-a90f-4e5bf96b4787 ==> octave
	// 91632086-39ef-4e52-a6d1-0e4f1bf95a7b ==> r        
        // 6ee0e31b-e066-4d39-86fd-059b1de8c52f ==> WRF
        // 4ba7c3d0-569e-4b8b-884c-23a5588329a7 ==> TreeThreader        
        //OCCI_OS = "f36b8eb8-8247-4b4f-a101-18c7834009e0";
        //OCCI_FLAVOR = "small";
        
        // === OCCI SETTINGS for the INFN-BARI (OpenStack) CLOUD RESOURCE === //
        //OCCI_ENDPOINT_HOST = "jocci://prisma-cloud.ba.infn.it";
        //OCCI_ENDPOINT_PORT = "8787";
        // Possible OCCI_OS values: 'generic_vm', 'octave', 'r', 'WRF', 'treethreader', 'aleph2000'
        // 623a86f7-f5f9-4bc7-816a-80e7bd6603ed => 'generic-vm'
        // 4aca9ee4-8638-4f95-824f-5128e8b0e90f => 'octave'
        // 217535d6-7315-4cb7-bc40-2aa20cfef60b => 'r'
        // a82fb047-5932-4b70-9099-67865e8b88f0 -> 'generic_www'
        // 30d841c8-fbf5-44d2-bdc9-f49df1bba2dd => 'treethreader'
        // b0df0319-5b5b-41fb-9453-2b578ee875fd => 'WRF'
        // 5f29ab3e-61f3-4f94-815f-3d6bf7a90704 => 'aleph2000'
        // 56c11ccb-c696-4fe6-b061-b5df24913580 => 'generic_www'
        //OCCI_OS = "623a86f7-f5f9-4bc7-816a-80e7bd6603ed";
        //OCCI_FLAVOR = "small";
        
        // === OCCI SETTINGS for the IHEP (OpenStack) CLOUD RESOURCE === //
        //OCCI_ENDPOINT_HOST = "jocci://sched02.ihep.ac.cn";
        //OCCI_ENDPOINT_PORT = "8787";
        // Possible OCCI_OS values: 'generic_vm', 'octave', 'r', etc.
        // 361d991f-fed3-4fb5-b267-5df3913d2b68 => 'generic-vm'
        // 3ade7193-5db3-4e9c-87fb-3f9b059620f9 => 'octave'
        // f1514748-66f0-4c1e-a169-2526f5f953d3 => 'r'
        // 37824c4f-1945-4ff7-b809-2c275f4396aa => 'generic_www'
        // f46671e2-bb12-425c-8ead-ecc9cf2112d1 => 'WRF'        
        //OCCI_OS = "361d991f-fed3-4fb5-b267-5df3913d2b68";
        //OCCI_FLAVOR = "small";
        
        // === OCCI SETTINGS for the ALGERIAN (OpenNebula) CLOUD RESOURCE === //
        //OCCI_ENDPOINT_HOST = "jocci://rocci.grid.arn.dz";
        //OCCI_ENDPOINT_PORT = "11443";
        // Possible OCCI_OS values: 'generic_vm', 'octave' and 'r'        
        // os_tpl#uuid_generic_vm_32 ==> uuid_generic_vm_32
	// os_tpl#uuid_virt_octave_34 ==> octave
	// os_tpl#uuid_virt_r_33 ==> r                
        // os_tpl#uuid_appwrf_36 ==> wrf
        // os_tpl#uuid_aleph2k_35 ==> aleph
        // os_tpl#uuid_generic_vm_32 ==> tthread_72
        // os_tpl#uuid_generic_www_31 ==> generic_www
        //OCCI_OS = "uuid_generic_vm_32";
        //OCCI_FLAVOR = "small";
        
        // === OCCI SETTINGS for the CIEMAT (OpenNebula) CLOUD RESOURCE === //
        //OCCI_ENDPOINT_HOST = "jocci://one01.ciemat.es";
        //OCCI_ENDPOINT_PORT = "11443";
        // Possible OCCI_OS values: 'generic_vm', 'octave' and 'r'
        // os_tpl#uuid_chain_reds_generic_vm_25 ==> generic_vm        
	// os_tpl#uuid_chain_reds_octave_28 ==> octave
	// os_tpl#uuid_chain_reds_r_27 ==> r     
        // os_tpl#uuid_chain_reds_wrf_24 ==> WRF
        // os_tpl#uuid_chain_reds_generic_www_29 ==> WWW
        //OCCI_OS = "uuid_chain_reds_generic_www_29";
        //OCCI_FLAVOR = "small";
                        
        //OCCI_RESOURCE_ID = "https://nebula-server-01.ct.infn.it:9000/compute/5463";
        OCCI_VM_TITLE = "jOCCI";

        // Possible ACTION values: 'list', 'describe', 'create' and 'delete'
        OCCI_ACTION = "create";
        
        try {    
            log.info("");
            log.info("Initialize the JobService context... ");
            
            // Start OCCI Actions ...
            if (OCCI_ACTION.equals("list")) {

                ServiceURL = OCCI_ENDPOINT_HOST + ":" + 
                             OCCI_ENDPOINT_PORT + 
                             System.getProperty("file.separator") + "?" +
                             "prefix=" + OCCI_PREFIX +
                             "&user_data=" + OCCI_CONTEXT_USER_DATA +
                             "&proxy_path=" + OCCI_PROXY_PATH;
            }
            
            else if (OCCI_ACTION.equals("create")) {                
                        
                        ServiceURL = OCCI_ENDPOINT_HOST + ":" + 
                                     OCCI_ENDPOINT_PORT + 
                                     System.getProperty("file.separator") + "?" +
                                     "prefix=" + OCCI_PREFIX +
                                     "&attributes_title=" + OCCI_VM_TITLE +
                                     "&mixin_os_tpl=" + OCCI_OS +
                                     "&mixin_resource_tpl=" + OCCI_FLAVOR +
                                     "&user_data=" + OCCI_CONTEXT_USER_DATA +                                     
                                     "&proxy_path=" + OCCI_PROXY_PATH;
            }
            
            else if (OCCI_ACTION.equals("describe")) {
                        
			ServiceURL = OCCI_ENDPOINT_HOST + ":" + 
                                     OCCI_ENDPOINT_PORT + 
                                     System.getProperty("file.separator") + "?" +
                                     "prefix=" + OCCI_PREFIX +
                                     "&resourceID=" + OCCI_RESOURCE_ID +
                                     "&user_data=" + OCCI_CONTEXT_USER_DATA +                                     
                                     "&proxy_path=" + OCCI_PROXY_PATH;
            }
            
            else if (OCCI_ACTION.equals("delete")) {

			ServiceURL = OCCI_ENDPOINT_HOST + ":" + 
                                     OCCI_ENDPOINT_PORT + 
                                     System.getProperty("file.separator") + "?" +
                                     "prefix=" + OCCI_PREFIX +
                                     "&resourceID=" + OCCI_RESOURCE_ID +
                                     "&user_data=" + OCCI_CONTEXT_USER_DATA +                                     
                                     "&proxy_path=" + OCCI_PROXY_PATH;
            }            
                                
            URL serviceURL = URLFactory.createURL(ServiceURL);
            log.info("serviceURL = " + serviceURL);            
            service = JobFactory.createJobService(session, serviceURL);  
                        
            // ========================================== //
            // === SUBMITTING VM using rOCCI standard === //
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
                log.info("Re-initialize the security context for the rOCCI JSAGA adaptor");
                session = SessionFactory.createSession(false);
            
                //Modifiy this section according to the A&A schema of your middleware
                //In this example the rocci A&A schema is used
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
                context.setAttribute(Context.USERID, "root");
            
                session.addContext(context);
            
                ServiceURL = OCCI_ENDPOINT_HOST + ":" + 
                             OCCI_ENDPOINT_PORT + 
                             System.getProperty("file.separator") + "?" +
                             "prefix=" + OCCI_PREFIX +
                             "&action=" + OCCI_ACTION + 
                             "&attributes_title=" + OCCI_VM_TITLE +
                             "&mixin_os_tpl=" + OCCI_OS +
                             "&mixin_resource_tpl=" + OCCI_FLAVOR +
                             "&user_data=" + OCCI_CONTEXT_USER_DATA +
                             "&proxy_path=" + OCCI_PROXY_PATH;
                
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