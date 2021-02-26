package nl.recognize.dwh.application.service;

import nl.recognize.dwh.application.model.DataTransformation;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class DataPipelineService {

    public Object apply(Object input, List<DataTransformation> transformations) {
        Object output = input;

        for (DataTransformation transformation : transformations) {
            output = transformation.transform(output);
        }

        return output;
    }
}
