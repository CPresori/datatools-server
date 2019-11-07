package com.conveyal.datatools.manager.jobs;

import com.amazonaws.services.ec2.model.Instance;
import com.conveyal.datatools.DatatoolsTest;
import com.conveyal.datatools.manager.auth.Auth0UserProfile;
import com.conveyal.datatools.manager.models.Deployment;
import com.conveyal.datatools.manager.models.EC2Info;
import com.conveyal.datatools.manager.models.OtpServer;
import com.conveyal.datatools.manager.models.Project;
import com.conveyal.datatools.manager.persistence.Persistence;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Date;
import java.util.List;

import static com.conveyal.datatools.manager.controllers.api.ServerController.getIds;
import static com.conveyal.datatools.manager.controllers.api.ServerController.terminateInstances;

/**
 * This contains a few helpful tests for quickly spinning up a deployment using any bundle of GTFS + OSM or pre-built
 * graph that already exists on S3.
 *
 * Note: these tests require credentials on the IBI AWS account, which is why the class is tagged with Ignore (so that it is
 * not run with the rest of the test suite)
 */
@Ignore
public class DeployJobTest {
    private static final Logger LOG = LoggerFactory.getLogger(DeployJobTest.class);
    private static Project project;
    private static OtpServer server;
    private static Deployment deployment;

    /**
     * Add project, server, and deployment to prepare for tests.
     */
    @BeforeClass
    public static void setUp() throws IOException {
        // start server if it isn't already running
        DatatoolsTest.setUp();
        // Create a project, feed sources, and feed versions to merge.
        project = new Project();
        project.name = String.format("Test %s", new Date().toString());
        Persistence.projects.create(project);
        server = new OtpServer();
        server.projectId = project.id;
        server.s3Bucket = "datatools-dev";
        server.ec2Info = new EC2Info();
        server.ec2Info.instanceCount = 2;
        server.ec2Info.instanceType = "r5d.xlarge";
        // FIXME: How should we include these private-ish values??
        server.ec2Info.securityGroupId = "YOUR_VALUE";
        server.ec2Info.subnetId = "YOUR_VALUE";
        server.ec2Info.keyName = "YOUR_VALUE";
        server.ec2Info.targetGroupArn = "YOUR_VALUE";
        server.ec2Info.iamInstanceProfileArn = "YOUR_VALUE";
        Persistence.servers.create(server);
        deployment = new Deployment();
        deployment.projectId = project.id;
        deployment.name = "Test deployment";
        Persistence.deployments.create(deployment);
    }

    /**
     * Tests that Data Tools can run an ELB deployment.
     *
     * Note: this requires changing server.yml#modules.deployment.ec2.enabled to true
     * @throws InterruptedException
     */
    @Test
    public void canDeployFromPreloadedBundle () throws InterruptedException {
        DeployJob deployJob = new DeployJob(deployment, Auth0UserProfile.createTestAdminUser(), server, "test-deploy", DeployJob.DeployType.USE_PRELOADED_BUNDLE);
        deployJob.run();
        Thread.sleep(60 * 1000);
        List<Instance> instances = server.retrieveEC2Instances();
        List<String> ids = getIds(instances);
        terminateInstances(ids);
    }

    /**
     * Tests that Data Tools can run an ELB deployment from a pre-built graph.
     * @throws InterruptedException
     */
    @Test
    public void canDeployFromPrebuiltGraph () throws InterruptedException {
        DeployJob deployJob = new DeployJob(deployment, Auth0UserProfile.createTestAdminUser(), server, "test-deploy", DeployJob.DeployType.USE_PREBUILT_GRAPH);
        deployJob.run();
        Thread.sleep(60 * 1000);
        List<Instance> instances = server.retrieveEC2Instances();
        List<String> ids = getIds(instances);
        terminateInstances(ids);
    }

}
