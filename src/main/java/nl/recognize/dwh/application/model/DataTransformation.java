package nl.recognize.dwh.application.model;

import java.util.Map;

public interface DataTransformation {
    /**
     * Method that applies data transformations, after which a new output is generated
     * @return
     */
    public Map<String, Object> transform(Object input);
}
