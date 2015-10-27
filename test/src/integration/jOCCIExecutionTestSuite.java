package integration;

import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;
import org.ogf.saga.job.description.DescriptionTest;
import org.ogf.saga.job.run.InfoTest;
import org.ogf.saga.job.run.InteractiveTest;
import org.ogf.saga.job.run.MinimalTest;
import org.ogf.saga.job.run.OptionalTest;
import org.ogf.saga.job.run.RequiredTest;
import org.ogf.saga.job.run.SandboxTest;


/* ***************************************************
* *** Centre de Calcul de l'IN2P3 - Lyon (France) ***
* ***             http://cc.in2p3.fr/             ***
* ***************************************************
* File:   jOCCIExecutionTestSuite
* Author: Lionel Schwarz (lionel.schwarz@in2p3.fr)
* Date:   30 sept 2013
****************************************************/

@RunWith(Suite.class)
@SuiteClasses({
    jOCCIExecutionTestSuite.jOCCIJobDescriptionTest.class,
    jOCCIExecutionTestSuite.jOCCIJobRunRequiredTest.class,
    jOCCIExecutionTestSuite.jOCCIJobRunOptionalTest.class,
    jOCCIExecutionTestSuite.jOCCIJobRunSandboxTest.class,
    jOCCIExecutionTestSuite.jOCCIJobRunInteractiveTest.class,
    jOCCIExecutionTestSuite.jOCCIJobRunInfoTest.class
})
public class jOCCIExecutionTestSuite {
    
    private final static String TYPE = "jocci";

    // test cases
    public static class jOCCIJobDescriptionTest extends DescriptionTest {
        public jOCCIJobDescriptionTest() throws Exception {super(TYPE);}
        public void test_spmdVariation() {  }
        public void test_totalCPUCount() {  }
        public void test_numberOfProcesses() {  }
        public void test_processesPerHost() {  }
        public void test_threadsPerProcess() {  }
//        public void test_input() {  }
//        public void test_fileTransfer() {  }
        public void test_cleanup() {  }
        public void test_totalCPUTime() {  }
        public void test_totalPhysicalMemory() {  }
        public void test_cpuArchitecture() {  }
        public void test_operatingSystemType() {  }
        public void test_candidateHosts() {  }
        public void test_queue() {  }
        public void test_wallTimeLimit() {  }
     }
    
    // test cases
    public static class jOCCIJobRunMinimalTest extends MinimalTest {
        public jOCCIJobRunMinimalTest() throws Exception {super(TYPE);}
    }
    
    // test cases
    public static class jOCCIJobRunRequiredTest extends RequiredTest {
        public jOCCIJobRunRequiredTest() throws Exception {super(TYPE);}
    }

    // test cases
    public static class jOCCIJobRunSandboxTest extends SandboxTest {
        public jOCCIJobRunSandboxTest() throws Exception {super(TYPE);}
        @Override @Test @Ignore("Not supported")
        public void test_output_workingDirectory() {  }
    }
    
    // test cases
    public static class jOCCIJobRunOptionalTest extends OptionalTest {
        public jOCCIJobRunOptionalTest() throws Exception {super(TYPE);}
        @Override @Test @Ignore("Not supported")
        public void test_resume_done() {  }
        @Override @Test @Ignore("Not supported")
        public void test_suspend_done() {  }
        @Override @Test @Ignore("Not supported")
        public void test_suspend_running() {  }
    }
    
     // test cases
//    public static class jOCCIJobRunDescriptionTest extends RequirementsTest {
//        public jOCCIJobRunDescriptionTest() throws Exception {super(TYPE);}
//        public void test_run_queueRequirement() {  }
//        public void test_run_cpuTimeRequirement() {  }
//        public void test_run_memoryRequirement() {  }
//    }
    
    // test cases
    public static class jOCCIJobRunInteractiveTest extends InteractiveTest {
        public jOCCIJobRunInteractiveTest() throws Exception {super(TYPE);}
        @Override @Test @Ignore("Not supported")
        public void test_simultaneousStdin()  { }
    }

    // test cases
    public static class jOCCIJobRunInfoTest extends InfoTest {
        public jOCCIJobRunInfoTest() throws Exception {super(TYPE);}
    }
}
