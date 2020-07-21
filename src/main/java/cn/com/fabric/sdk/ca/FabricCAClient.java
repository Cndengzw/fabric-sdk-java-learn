package cn.com.fabric.sdk.ca;

import cn.com.fabric.sdk.UserContext;
import org.hyperledger.fabric.sdk.Enrollment;
import org.hyperledger.fabric.sdk.exception.CryptoException;
import org.hyperledger.fabric.sdk.exception.InvalidArgumentException;
import org.hyperledger.fabric.sdk.security.CryptoSuite;
import org.hyperledger.fabric_ca.sdk.Attribute;
import org.hyperledger.fabric_ca.sdk.HFCAClient;
import org.hyperledger.fabric_ca.sdk.RegistrationRequest;

import java.lang.reflect.InvocationTargetException;
import java.net.MalformedURLException;
import java.util.Properties;

/**
 * @author Deng Zhiwen
 * @date 2020/7/21 13:47
 */
public class FabricCAClient {

    private HFCAClient hfcaClient;

    public FabricCAClient(String url, Properties properties) throws MalformedURLException, IllegalAccessException, InvocationTargetException, InvalidArgumentException, InstantiationException, NoSuchMethodException, CryptoException, ClassNotFoundException {
        hfcaClient = HFCAClient.createNewInstance(url, properties);
        CryptoSuite cryptoSuite = CryptoSuite.Factory.getCryptoSuite();
        hfcaClient.setCryptoSuite(cryptoSuite);
    }

    /**
     * 注册用户
     * @param registrar 注册商
     * @param register 被注册用户
     * @return register_secret 被注册用户密码
     */
    public String register(UserContext registrar, UserContext register) throws Exception {
        RegistrationRequest registrationRequest = new RegistrationRequest(register.getName(), register.getAffiliation());

        // 设置可以该用户可以注册的属性，就是他被注册后可以用来注册 client 和 peer 的用户
        Attribute attribute = new Attribute("hf.Registar.Roles", "client,peer");
        registrationRequest.addAttribute(attribute);

        String registerSecret = hfcaClient.register(registrationRequest, registrar); // 返回的是注册完的密码
        return registerSecret;
    }

    /**
     * 登记用户
     * @param username
     * @param password
     * @return
     * @throws Exception
     */
    public Enrollment enroll(String username, String password) throws Exception {
        Enrollment enroll = hfcaClient.enroll(username, password);
        return enroll;
    }

}
