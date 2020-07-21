package cn.com.fabric.sdk;

import com.google.protobuf.ByteString;
import org.hyperledger.fabric.sdk.*;
import org.hyperledger.fabric.sdk.exception.*;
import org.hyperledger.fabric.sdk.security.CryptoSuite;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.util.*;


/**
 * @author Deng Zhiwen
 * @date 2020/7/20 11:16
 */

public class FabricClient {

    private static final Logger log = LoggerFactory.getLogger(FabricClient.class);

    private HFClient hfClient;

    public FabricClient(UserContext userContext) throws IllegalAccessException, InvocationTargetException, InvalidArgumentException, InstantiationException, NoSuchMethodException, CryptoException, ClassNotFoundException {
        hfClient = HFClient.createNewInstance();
        CryptoSuite cryptoSuite = CryptoSuite.Factory.getCryptoSuite(); // 设置加密算法
        hfClient.setCryptoSuite(cryptoSuite);
        hfClient.setUserContext(userContext);
    }

    /**
     * 创建通道
     * @param channelName 通道名
     * @param orderer   orderer 节点
     * @param txPath    通道配置文件，xxx.tx 格式，在网络中先用命令行创好
     * @return chanenl 通道
     */
    public Channel createChannel(String channelName, Orderer orderer, String txPath) throws Exception {
        ChannelConfiguration channelConfiguration = new ChannelConfiguration(new File(txPath));
        return hfClient.newChannel(channelName, orderer, channelConfiguration,
                hfClient.getChannelConfigurationSignature(channelConfiguration, hfClient.getUserContext()));
    }

    /**
     * 安装链码
     * @param lang 语言
     * @param chaincodeName 链码名称
     * @param chaincodeVersion 版本
     * @param chaincodePath 路径
     * @param peers 要安装的 Peer 节点
     */
    public void installChaincode(TransactionRequest.Type lang, String chaincodeName, String chaincodeVersion, String chaincodeLocation, String chaincodePath, List<Peer> peers) throws Exception {
        InstallProposalRequest installProposalRequest = hfClient.newInstallProposalRequest();

        ChaincodeID chaincodeID = ChaincodeID.newBuilder()
                                .setName(chaincodeName)
                                .setVersion(chaincodeVersion)  // 不写的话默认使用最新版本
                                .build();

        installProposalRequest.setChaincodeLanguage(lang);
        installProposalRequest.setChaincodeID(chaincodeID);
        installProposalRequest.setChaincodeSourceLocation(new File(chaincodeLocation));
        installProposalRequest.setChaincodePath(chaincodePath);

        Collection<ProposalResponse> responses = hfClient.sendInstallProposal(installProposalRequest, peers);
        for (ProposalResponse response : responses) {
            if (response.getStatus().getStatus() == 200) {
                log.info("{} installed success", response.getPeer().getName());
            } else {
                log.info("{} installed failed", response.getPeer().getName());
            }
        }
    }

    /**
     * 实例化链码（这里没有指定背书策略）；默认的是 OR ，只要有一个人背书就认为是合法
     * 如果要指定背书策略，看下面升级合约的代码
     * @param channelName
     * @param lang
     * @param chaincodeName
     * @param chaincodeVersion
     * @param orderer
     * @param peer
     * @param funcName
     * @param args
     */
    public void initChaincode(String channelName, TransactionRequest.Type lang, String chaincodeName, String chaincodeVersion, Orderer orderer, Peer peer, String funcName, String... args) throws Exception {
        Channel channel = getChannel(channelName);
        channel.addPeer(peer);
        channel.addOrderer(orderer);
        channel.initialize();

        ChaincodeID chaincodeID = ChaincodeID.newBuilder()
                .setName(chaincodeName)
                .setVersion(chaincodeVersion)  // 不写的话默认使用最新版本
                .build();

        InstantiateProposalRequest instantiateProposalRequest = hfClient.newInstantiationProposalRequest();
        instantiateProposalRequest.setFcn(funcName);
        instantiateProposalRequest.setChaincodeLanguage(lang);  // 不写的话默认是go
        instantiateProposalRequest.setChaincodeID(chaincodeID);

        // 返回提案
        Collection<ProposalResponse> responses = channel.sendInstantiationProposal(instantiateProposalRequest);

        for (ProposalResponse response : responses) {
            if (response.getStatus().getStatus() == 200) {
                log.info("{} Instantiate success", response.getPeer().getName());
            } else {
                log.info("{} Instantiate failed", response.getPeer().getName());
            }
        }

        channel.sendTransaction(responses); // 上块

    }


    /**
     * 升级合约（前置条件是先安装 2.0）
     * 1. 改动原来的链码后，再次安装，版本设置为 2.0，安装完成后通道有 1.0 和 2.0 两个版本的合约。
     * 2. 这个时候再升级，升级完后再查看 instantiated 就只有 2.0 版本了
     * @param channelName
     * @param lang
     * @param chaincodeName
     * @param chaincodeVersion
     * @param orderer
     * @param peer
     * @param funcName
     * @param args
     */
    public void upgradeChaincode(String channelName, TransactionRequest.Type lang, String chaincodeName, String chaincodeVersion, Orderer orderer, Peer peer, String funcName, String... args) throws Exception {
        Channel channel = getChannel(channelName);
        channel.addPeer(peer);
        channel.addOrderer(orderer);
        channel.initialize();

        ChaincodeID chaincodeID = ChaincodeID.newBuilder()
                .setName(chaincodeName)
                .setVersion(chaincodeVersion)  // 不写的话默认使用最新版本
                .build();

        UpgradeProposalRequest upgradeProposalRequest = hfClient.newUpgradeProposalRequest();
        upgradeProposalRequest.setFcn(funcName);
        upgradeProposalRequest.setChaincodeLanguage(lang);  // 不写的话默认是go
        upgradeProposalRequest.setChaincodeID(chaincodeID);

        // 指定背书策略
        ChaincodeEndorsementPolicy chaincodeEndorsementPolicy = new ChaincodeEndorsementPolicy();
        chaincodeEndorsementPolicy.fromYamlFile(new File("xxx.yaml"));
        upgradeProposalRequest.setChaincodeEndorsementPolicy(chaincodeEndorsementPolicy);

        // 返回提案
        Collection<ProposalResponse> responses = channel.sendUpgradeProposal(upgradeProposalRequest);

        for (ProposalResponse response : responses) {
            if (response.getStatus().getStatus() == 200) {
                log.info("{} upgrade success", response.getPeer().getName());
            } else {
                log.info("{} upgrade failed", response.getPeer().getName());
            }
        }

        channel.sendTransaction(responses); // 上块

    }

    /**
     * 触发合约，需要背书
     * @param channelName
     * @param lang
     * @param chaincodeName
     * @param chaincodeVersion
     * @param orderer
     * @param peers
     * @param funcName
     * @param args
     * @throws Exception
     */
    public void invokeChaincode(String channelName, TransactionRequest.Type lang, String chaincodeName, String chaincodeVersion, Orderer orderer, List<Peer> peers, String funcName, String... args) throws Exception{
        Channel channel = getChannel(channelName);
        channel.addOrderer(orderer);
        for (Peer peer : peers) {
            channel.addPeer(peer);
        }
        channel.initialize();

        ChaincodeID chaincodeID = ChaincodeID.newBuilder()
                .setName(chaincodeName)
                .setVersion(chaincodeVersion)   // 不写的话默认使用最新版本
                .build();

        TransactionProposalRequest transactionProposalRequest = hfClient.newTransactionProposalRequest();
        transactionProposalRequest.setFcn(funcName);
        transactionProposalRequest.setChaincodeLanguage(lang);  // 不写的话默认是go
        transactionProposalRequest.setChaincodeID(chaincodeID);
        transactionProposalRequest.setArgs(args);
        // transactionProposalRequest.setUserContext();

        Collection<ProposalResponse> responses = channel.sendTransactionProposal(transactionProposalRequest, peers);

        for (ProposalResponse response : responses) {
            if (response.getStatus().getStatus() == 200) {
                log.info("{} invoke proposal {} success", response.getPeer().getName(), funcName);
            } else {
                log.info("{} invoke proposal {} failed", response.getPeer().getName(), funcName);
            }
        }

        channel.sendTransaction(responses); // 上块
    }


    public Map queryChaincode(String channelName, TransactionRequest.Type lang, String chaincodeName, String chaincodeVersion, List<Peer> peers, String funcName, String... args) throws Exception {
        Channel channel = getChannel(channelName);
        for (Peer peer : peers) {
            channel.addPeer(peer);
        }
        channel.initialize();

        ChaincodeID chaincodeID = ChaincodeID.newBuilder()
                .setName(chaincodeName)
                .setVersion(chaincodeVersion)
                .build();

        QueryByChaincodeRequest queryByChaincodeRequest = hfClient.newQueryProposalRequest();
        queryByChaincodeRequest.setChaincodeID(chaincodeID);
        queryByChaincodeRequest.setChaincodeName(chaincodeName);
        queryByChaincodeRequest.setChaincodeVersion(chaincodeVersion);
        queryByChaincodeRequest.setChaincodeLanguage(lang);
        queryByChaincodeRequest.setFcn(funcName);
        queryByChaincodeRequest.setArgs(args);

        Collection<ProposalResponse> responses = channel.queryByChaincode(queryByChaincodeRequest);

        HashMap map = new HashMap<>();
        for (ProposalResponse response : responses) {
            if (response.getStatus().getStatus() == 200) {
                // getPayload 返回的是 ByteString
                String payloadStr = new String(response.getProposalResponse().getResponse().getPayload().toByteArray());

                /*ByteString payload = response.getProposalResponse().getPayload();
                byte[] payloadBytes = payload.toByteArray();
                String payloadStr = Base64.getEncoder().encodeToString(payloadBytes);*/

                log.info("data is {}", payloadStr);
                map.put(response.getStatus().getStatus(), payloadStr);
                return map;
            } else {
                String message = response.getMessage();
                log.info("data get error {}", message);
                map.put(response.getStatus().getStatus(), message);
                return map;
            }
        }
        map.put("code","404");
        return map;
    }




    /////////////////////////////////////////////////////////////////////////

    public Orderer getOrderer(String name, String grpcUrl, String tlsFilePath) throws Exception {
        Properties properties = new Properties();
        properties.setProperty("pemFile", tlsFilePath);
        return hfClient.newOrderer(name, grpcUrl, properties);  // 不是创建是连接
    }

    public Peer getPeer(String name, String grpcUrl, String tlsFilePath) throws Exception {
        Properties properties = new Properties();
        properties.setProperty("pemFile", tlsFilePath);
        return hfClient.newPeer(name, grpcUrl, properties); // 连接
    }

    public Channel getChannel(String channelName) throws Exception {
        return hfClient.newChannel(channelName);
    }
}
