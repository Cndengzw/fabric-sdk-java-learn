package cn.com.fabric.sdk;

import org.hyperledger.fabric.sdk.*;
import org.hyperledger.fabric.sdk.exception.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * @author Deng Zhiwen
 * @date 2020/7/20 11:24
 */
public class SdkMain {

    private static final String keyFolderPath = "E:\\Project\\fabric-SDK-learn\\src\\main\\resources\\crypto-config\\peerOrganizations\\org1.example.com\\users\\Admin@org1.example.com\\msp\\keystore";
    private static final String keyFileName = "priv_sk";
    private static final String certFolderPath = "E:\\Project\\fabric-SDK-learn\\src\\main\\resources\\crypto-config\\peerOrganizations\\org1.example.com\\users\\Admin@org1.example.com\\msp\\signcerts";
    private static final String certFileName = "Admin@org1.example.com-cert.pem";

    private static final String ordererTlsFilePath = "E:\\Project\\fabric-SDK-learn\\src\\main\\resources\\crypto-config\\ordererOrganizations\\example.com\\tlsca\\tlsca.example.com-cert.pem";
    private static final String org1peerTlsFilePath = "E:\\Project\\fabric-SDK-learn\\src\\main\\resources\\crypto-config\\peerOrganizations\\org1.example.com\\peers\\peer0.org1.example.com\\msp\\tlscacerts\\tlsca.org1.example.com-cert.pem";
    private static final String org2peerTlsFilePath = "E:\\Project\\fabric-SDK-learn\\src\\main\\resources\\crypto-config\\peerOrganizations\\org2.example.com\\peers\\peer0.org2.example.com\\msp\\tlscacerts\\tlsca.org2.example.com-cert.pem";
    private static final String txFilePath = "E:\\Project\\fabric-SDK-learn\\src\\main\\resources\\test.tx";

    public static void main(String[] args) throws Exception {

        UserContext userContext = new UserContext();
        userContext.setAffiliation("Org1");
        userContext.setMspId("Org1MSP");
        userContext.setAccount("李伟");
        userContext.setName("admin");

        Enrollment enrollment = UserUtils.getEnrollment(keyFolderPath, keyFileName, certFolderPath, certFileName);
        userContext.setEnrollment(enrollment);

        FabricClient fabricClient = new FabricClient(userContext);

        // createChannelAndJoinPeer(fabricClient);

        // installChaincode(fabricClient);

        // initChaincode(fabricClient);

        // upgradeChaincode(fabricClient);

    }

    // 创建通道并将 Peer 加入到通道
    public static void createChannelAndJoinPeer(FabricClient fabricClient) throws Exception {
        Orderer orderer = fabricClient.getOrderer("orderer.example.com", "grpcs://orderer.example.com:7050", ordererTlsFilePath);
        Peer peer = fabricClient.getPeer("peer0.org1.example.com", "grpcs://peer0.org1.example.com:7051", org1peerTlsFilePath);

        // 这里用 test 是因为先在网络中生成另一个 test 的通道配置文件
        Channel channel = fabricClient.createChannel("test", orderer, txFilePath);
        channel.initialize();

        channel.joinPeer(peer);// 把 peer 加入到通道中
    }

    // 安装合约
    // ERROR：cannot find package "github.com/hyperledger/fabric/core/chaincode/shim" in any of:......
    public static void installChaincode(FabricClient fabricClient) throws Exception {
        Peer peer = fabricClient.getPeer("peer0.org1.example.com", "grpcs://peer0.org1.example.com:7051", org1peerTlsFilePath);
        Peer peer1 = fabricClient.getPeer("peer1.org1.example.com", "grpcs://peer1.org1.example.com:8051", org1peerTlsFilePath);

        List<Peer> peers = new ArrayList<>();
        peers.add(peer);
        peers.add(peer1);

        fabricClient.installChaincode(TransactionRequest.Type.GO_LANG,
                                    "basicinfo",
                                    "1.0",
                                    "F:\\chaincode",
                                    "basicinfo",
                                    peers);

    }

    // 实例化合约
    public static void initChaincode(FabricClient fabricClient) throws Exception, TransactionException {
        Orderer orderer = fabricClient.getOrderer("orderer.example.com", "grpcs://orderer.example.com:7050", ordererTlsFilePath);
        Peer peer = fabricClient.getPeer("peer0.org1.example.com", "grpcs://peer0.org1.example.com:7051", org1peerTlsFilePath);

        fabricClient.initChaincode("mychannel", TransactionRequest.Type.GO_LANG, "basicinfo", "1.0", orderer, peer, "init", "");
    }

    // 升级合约
    public static void upgradeChaincode(FabricClient fabricClient) throws Exception {
        Orderer orderer = fabricClient.getOrderer("orderer.example.com", "grpcs://orderer.example.com:7050", ordererTlsFilePath);
        Peer peer = fabricClient.getPeer("peer0.org1.example.com", "grpcs://peer0.org1.example.com:7051", org1peerTlsFilePath);

        fabricClient.upgradeChaincode("mychannel", TransactionRequest.Type.GO_LANG, "basicinfo", "2.0", orderer, peer, "init", "");
    }

    // 触发合约，需要背书
    public static void invokeChaincode(FabricClient fabricClient) throws Exception {
        Orderer orderer = fabricClient.getOrderer("orderer.example.com", "grpcs://orderer.example.com:7050", ordererTlsFilePath);
        Peer peer01 = fabricClient.getPeer("peer0.org1.example.com", "grpcs://peer0.org1.example.com:7051", org1peerTlsFilePath);
        Peer peer02 = fabricClient.getPeer("peer0.org2.example.com", "grpcs://peer0.org2.example.com:9051", org2peerTlsFilePath);

        ArrayList<Peer> peers = new ArrayList<>();
        peers.add(peer01);
        peers.add(peer02);  // 两个节点都要在通道安装了此合约

        String[] args = {"xxxxxx"};
        fabricClient.invokeChaincode("mychannel", TransactionRequest.Type.GO_LANG, "basicinfo", "2.0", orderer, peers, "save", args);
    }

    // 查询合约，不需要背书
    public static Map queryChaincode(FabricClient fabricClient) throws Exception {
        Peer peer01 = fabricClient.getPeer("peer0.org1.example.com", "grpcs://peer0.org1.example.com:7051", org1peerTlsFilePath);
        Peer peer02 = fabricClient.getPeer("peer0.org2.example.com", "grpcs://peer0.org2.example.com:9051", org2peerTlsFilePath);

        ArrayList<Peer> peers = new ArrayList<>();
        peers.add(peer01);
        peers.add(peer02);  // 两个节点都要在通道安装了此合约

        String[] args = {"xxxxxx"};
        return fabricClient.queryChaincode("mychannel", TransactionRequest.Type.GO_LANG, "basicinfo", "2.0", peers, "query", args);
    }
}
