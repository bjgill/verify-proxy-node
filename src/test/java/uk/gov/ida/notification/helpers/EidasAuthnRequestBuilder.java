package uk.gov.ida.notification.helpers;

import org.opensaml.saml.saml2.core.AuthnRequest;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import uk.gov.ida.notification.saml.SamlParser;

import javax.xml.transform.TransformerException;
import javax.xml.xpath.XPathExpressionException;
import java.io.IOException;
import java.util.HashMap;

public class EidasAuthnRequestBuilder {
    private final String EIDAS_AUTHN_REQUEST_XML = "eidas_authn_request.xml";
    private final SamlParser parser;
    private final Document authnRequestDocument;
    private final String saml2 = "urn:oasis:names:tc:SAML:2.0:assertion";
    private final String saml2p = "urn:oasis:names:tc:SAML:2.0:protocol";
    private final String ds = "http://www.w3.org/2000/09/xmldsig#";
    private final String eidas = "http://eidas.europa.eu/saml-extensions";
    private HashMap<String, String> namespaceMap;

    public EidasAuthnRequestBuilder() throws Exception {
        parser = new SamlParser();
        authnRequestDocument = XmlHelpers.readDocumentFromFile(EIDAS_AUTHN_REQUEST_XML);
        namespaceMap = new HashMap<String, String>(){{
            put("saml2", saml2);
            put("saml2p", saml2p);
            put("ds", ds);
            put("eidas", eidas);
        }};
    }

    public AuthnRequest build() throws IOException, TransformerException {
        String authnRequestString = XmlHelpers.serializeDomElementToString(authnRequestDocument.getDocumentElement());
        return parser.parseSamlString(authnRequestString);
    }

    public EidasAuthnRequestBuilder withNoIssuer() throws XPathExpressionException {
        Node node = findNode("//saml2:Issuer");
        node.getParentNode().removeChild(node);
        return this;
    }

    public EidasAuthnRequestBuilder withSpType(String spType) throws XPathExpressionException {
        findNode("//eidas:SPType").setTextContent(spType);
        return this;
    }

    private Node findNode(String xPathExpression) throws XPathExpressionException {
        return XmlHelpers.findNodeInDocument(authnRequestDocument, xPathExpression, namespaceMap);
    }
}
