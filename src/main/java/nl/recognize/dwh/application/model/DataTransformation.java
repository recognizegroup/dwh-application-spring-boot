package nl.recognize.dwh.application.model;

public interface DataTransformation {
    /**
     * Method that applies data transformations, after which a new output is generated
     *
     * @return
     */
    public Object transform(Object input);
}
