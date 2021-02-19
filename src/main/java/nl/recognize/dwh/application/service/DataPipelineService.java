package nl.recognize.dwh.application.service;

import nl.recognize.dwh.application.model.DataTransformation;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public class DataPipelineService {

    public Map<String, Object> apply(Map<String, Object> input, List<DataTransformation> transformations) {
        Map<String, Object> output = input;

        for (DataTransformation transformation : transformations) {
            output = transformation.transform(output);
        }

        return output;
    }
}
