package uk.gov.ida.notification.resources;

import io.dropwizard.views.View;
import net.shibboleth.utilities.java.support.resolver.ResolverException;
import org.opensaml.saml.saml2.core.Response;
import uk.gov.ida.notification.EidasResponseGenerator;
import uk.gov.ida.notification.SamlFormViewBuilder;
import uk.gov.ida.notification.pki.CredentialBuilder;
import uk.gov.ida.notification.pki.EncryptionCredential;
import uk.gov.ida.notification.saml.ResponseAssertionDecrypter;
import uk.gov.ida.notification.saml.ResponseAssertionEncrypter;
import uk.gov.ida.notification.saml.SamlFormMessageType;
import uk.gov.ida.notification.saml.metadata.ConnectorNodeMetadata;
import uk.gov.ida.notification.saml.translation.HubResponseContainer;

import javax.ws.rs.Consumes;
import javax.ws.rs.FormParam;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.core.MediaType;
import java.util.logging.Logger;

@Path("/SAML2/SSO/Response")
public class HubResponseResource {
    private static final Logger LOG = Logger.getLogger(HubResponseResource.class.getName());

    private final EidasResponseGenerator eidasResponseGenerator;
    private final SamlFormViewBuilder samlFormViewBuilder;
    private final ResponseAssertionDecrypter assertionDecrypter;
    private final String connectorNodeUrl;
    private ConnectorNodeMetadata connectorNodeMetadata;

    public HubResponseResource(EidasResponseGenerator eidasResponseGenerator, SamlFormViewBuilder samlFormViewBuilder, ResponseAssertionDecrypter assertionDecrypter, String connectorNodeUrl, ConnectorNodeMetadata connectorNodeMetadata) {
        this.assertionDecrypter = assertionDecrypter;
        this.connectorNodeUrl = connectorNodeUrl;
        this.eidasResponseGenerator = eidasResponseGenerator;
        this.samlFormViewBuilder = samlFormViewBuilder;
        this.connectorNodeMetadata = connectorNodeMetadata;
    }

    @POST
    @Path("/POST")
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    public View hubResponse(
            @FormParam(SamlFormMessageType.SAML_RESPONSE) Response encryptedHubResponse,
            @FormParam("RelayState") String relayState) throws ResolverException {
        Response decryptedHubResponse = assertionDecrypter.decrypt(encryptedHubResponse);

        HubResponseContainer hubResponseContainer = HubResponseContainer.fromResponse(decryptedHubResponse);
        logHubResponse(hubResponseContainer);

        ResponseAssertionEncrypter assertionEncrypter = createAssertionEncrypter();

        Response securedEidasResponse = eidasResponseGenerator.generate(hubResponseContainer, assertionEncrypter);
        logEidasResponse(securedEidasResponse);

        return samlFormViewBuilder.buildResponse(connectorNodeUrl, securedEidasResponse, "Post eIDAS Response SAML to Connector Node", relayState);
    }

    private ResponseAssertionEncrypter createAssertionEncrypter() throws ResolverException {
        EncryptionCredential connectorNodeEncryptingCredential = CredentialBuilder
                .withPublicKey(connectorNodeMetadata.getEncryptionPublicKey())
                .buildEncryptionCredential();
        return new ResponseAssertionEncrypter(connectorNodeEncryptingCredential);
    }

    private void logHubResponse(HubResponseContainer hubResponseContainer) {
        LOG.info("[Hub Response] ID: " + hubResponseContainer.getHubResponse().getResponseId());
        LOG.info("[Hub Response] In response to: " + hubResponseContainer.getHubResponse().getInResponseTo());
        LOG.info("[Hub Response] Provided level of assurance: " + hubResponseContainer.getAuthnStatement().getProvidedLoa());
    }

    private void logEidasResponse(Response eidasResponse) {
        LOG.info("[eIDAS Response] ID: " + eidasResponse.getID());
        LOG.info("[eIDAS Response] In response to: " + eidasResponse.getInResponseTo());
    }
}
