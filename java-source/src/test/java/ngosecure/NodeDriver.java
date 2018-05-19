package ngosecure;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import net.corda.core.identity.CordaX500Name;
import net.corda.core.utilities.NetworkHostAndPort;
import net.corda.testing.driver.DriverParameters;
import net.corda.testing.driver.NodeHandle;
import net.corda.testing.driver.NodeParameters;
import net.corda.testing.node.User;

import static net.corda.testing.driver.Driver.driver;

public class NodeDriver {
    public static void main(String[] args) {
        final User user = new User("user1", "test", ImmutableSet.of("ALL"));
        final User nodeAUser = new User("user2", "test", ImmutableSet.of("InvokeRpc.nodeInfo"));

        /*List<NotarySpec> notarySpecs = new ArrayList<NotarySpec>();
        NotarySpec notarySpec = new NotarySpec();*/

        DriverParameters driverParameters = new DriverParameters()
                .withExtraCordappPackagesToScan(ImmutableList.of("net.corda.finance.contracts.asset", "net.corda.finance.schemas"))
                .withIsDebug(true).withStartNodesInProcess(true)
                .withWaitForAllNodesToFinish(true);

/*
        List<NotarySpec> notarySpecs = driverParameters.getNotarySpecs();
        System.out.println("Notary Spec Name: "  + notarySpecs.get(0).getName());*/

        driver(driverParameters
                , dsl -> {
            try {
                NodeHandle nodeA = dsl.startNode(new NodeParameters()
                        .withProvidedName(new CordaX500Name("PartyA", "London", "GB"))
                        .withRpcUsers(ImmutableList.of(user))).get();
                NodeHandle nodeB = dsl.startNode(new NodeParameters()
                        .withProvidedName(new CordaX500Name("PartyB", "New York", "US"))
                        .withRpcUsers(ImmutableList.of(user))).get();
                NodeHandle nodeC = dsl.startNode(new NodeParameters()
                        .withProvidedName(new CordaX500Name("PartyC", "Paris", "FR"))
                        .withRpcUsers(ImmutableList.of(user))).get();
                NodeHandle nodeD = dsl.startNode(new NodeParameters()
                        .withProvidedName(new CordaX500Name("PartyD", "Chennai", "IN"))
                        .withRpcUsers(ImmutableList.of(user))).get();



                System.out.println("Notary Count: " + dsl.getNotaryHandles().size() + "|First notary name" +
                        dsl.getNotaryHandles().get(0).getIdentity().getName());

                NodeHandle notaryHandle = dsl.getDefaultNotaryNode().get();
                NetworkHostAndPort notaryNetwork = notaryHandle.getNodeInfo().getAddresses().get(0);

                System.out.println("Default Notary handle host: " + notaryNetwork.getHost() +
                        "Default Notary handle port: " + notaryNetwork.getPort() +
                        "Default Notary handle component1: " + notaryNetwork.component1() +
                        "Default Notary handle component2: " + notaryNetwork.component2() +
                        "Default Notary handle name: " + dsl.getDefaultNotaryHandle().getIdentity().getName()
                );

                dsl.startWebserver(notaryHandle);
                dsl.startWebserver(nodeA);
                dsl.startWebserver(nodeB);
                dsl.startWebserver(nodeC);
                dsl.startWebserver(nodeD);
            } catch (Throwable e) {
                System.err.println("Encountered exception in node startup: " + e.getMessage());
                e.printStackTrace();
            }
            return null;
        });
    }
}