package uk.gov.ida.notification.exceptions;

import org.slf4j.MDC;
import uk.gov.ida.notification.shared.ProxyNodeMDCKey;

public class SessionAlreadyExistsException extends RuntimeException {

    private String sessionId;

    public SessionAlreadyExistsException(String sessionId, String hubRequestId, String eidasRequestId) {
        super("Session already exists for session_id: " + sessionId);
        MDC.put(ProxyNodeMDCKey.SESSION_ID.name(), sessionId);
        MDC.put(ProxyNodeMDCKey.HUB_REQUEST_ID.name(), hubRequestId);
        MDC.put(ProxyNodeMDCKey.EIDAS_REQUEST_ID.name(), eidasRequestId);    }
}