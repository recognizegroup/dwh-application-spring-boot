package nl.recognize.dwh.application.model;

import lombok.Getter;
import java.util.Map;

@Getter
public class ProtocolResponse<BODY extends Object> {
    private Map<String, String> metadata;

    private BODY body;

    public ProtocolResponse(Map<String, String> metadata, BODY body) {
        this.metadata = metadata;
        this.body = body;
    }

}
