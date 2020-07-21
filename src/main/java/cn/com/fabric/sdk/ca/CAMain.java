package cn.com.fabric.sdk.ca;

import cn.com.fabric.sdk.UserContext;
import org.hyperledger.fabric.sdk.Enrollment;

import javax.management.Attribute;
import java.util.Properties;

/**
 * @author Deng Zhiwen
 * @date 2020/7/21 14:09
 */
public class CAMain {

    private static final String URL = "https://47.115.37.243:7054";

    private static final String org1CAKey = "E:\\Project\\Myfabric\\fabric-SDK-learn\\src\\main\\resources\\crypto-config\\peerOrganizations\\org1.example.com\\tlsca\\priv_sk";
    private static final String org1CACert = "E:\\Project\\Myfabric\\fabric-SDK-learn\\src\\main\\resources\\crypto-config\\peerOrganizations\\org1.example.com\\tlsca\\tlsca.org1.example.com-cert.pem";
    private static final String org1CACert2 = "E:\\Project\\Myfabric\\fabric-SDK-learn\\src\\main\\resources\\crypto-config\\peerOrganizations\\org1.example.com\\ca\\ca.org1.example.com-cert.pem";
    private static final String org1CACert3 = "E:\\Project\\Myfabric\\fabric-SDK-learn\\src\\main\\resources\\crypto-config\\peerOrganizations\\org1.example.com\\users\\Admin@org1.example.com\\tls\\ca.crt";

    public static void main(String[] args) throws Exception{

        Properties properties = new Properties();
        properties.setProperty("pemFile", org1CACert2);

        // 错误2：这里把 TLS 证书加上去了，为什么还会报错
        // unable to find valid certification path to requested target
        FabricCAClient caClient = new FabricCAClient(URL, properties);

        // 错误1：这里注册有一些问题 是因为 TLS 证书问题，网络是开启 TLS 的，这里没有
        // Exception in thread "main" org.hyperledger.fabric_ca.sdk.exception.EnrollmentException: Url:http://47.115.37.243:7054, Failed to enroll user admin
        String registerSecret = register(caClient);
        System.out.println(registerSecret);
    }


    private static String register(FabricCAClient caClient) throws Exception {
        // 待注册用户
        UserContext register = new UserContext();
        register.setName("lihua");
        register.setAffiliation("org1");

        // 注册商
        UserContext registrar = new UserContext();
        registrar.setName("admin");
        registrar.setAffiliation("org1");
        Enrollment enroll = caClient.enroll("admin", "adminpw");// 登记
        registrar.setEnrollment(enroll);

        String registerSecret = caClient.register(registrar, register);
        return registerSecret;
    }
}
