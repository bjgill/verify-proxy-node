package uk.gov.ida.notification.configuration;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.apache.xml.security.algorithms.JCEMapper;
import org.opensaml.security.x509.BasicX509Credential;
import org.opensaml.security.x509.X509Support;
import uk.gov.ida.common.shared.configuration.DeserializablePublicKeyConfiguration;

import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.Provider;
import java.security.Security;
import java.security.cert.X509Certificate;

@JsonIgnoreProperties(ignoreUnknown = true)
public class CloudHsmCredentialConfiguration extends CredentialConfiguration {
    @JsonCreator
    public CloudHsmCredentialConfiguration(
        @JsonProperty("publicKey") DeserializablePublicKeyConfiguration publicKey,
        @JsonProperty("hsmKeyLabel") String hsmKeyLabel
    ) throws CredentialConfigurationException {
        try {
            X509Certificate certificate = X509Support.decodeCertificate(publicKey.getCert().getBytes());
            Provider caviumProvider = (Provider) ClassLoader.getSystemClassLoader()
                .loadClass("com.cavium.provider.CaviumProvider")
                .getConstructor()
                .newInstance();
            Security.addProvider(caviumProvider);
            JCEMapper.setProviderId("Cavium");
            KeyStore cloudHsmStore = KeyStore.getInstance("Cavium");
            cloudHsmStore.load(null, null);
            BasicX509Credential credential = new BasicX509Credential(
                certificate,
                (PrivateKey) cloudHsmStore.getKey(hsmKeyLabel, null));
            setCredential(credential);
        } catch(Exception e) {
            throw new CredentialConfigurationException(e);
        }
    }
}