package nl.recognize.dwh.application.model;

import java.util.Map;

public class ProtocolResponse<BODY extends Object> {
    private Metadata metadata;

    private BODY body;

    public ProtocolResponse(Metadata metadata, BODY body) {
        this.metadata = metadata;
        this.body = body;
    }

    public Metadata getMetadata() {
        return metadata;
    }

    public BODY getBody() {
        return body;
    }
}
