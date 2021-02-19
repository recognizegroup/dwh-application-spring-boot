package nl.recognize.dwh.application.model;

import lombok.Getter;
import lombok.experimental.SuperBuilder;

@Getter
@SuperBuilder
public class DetailOptions extends BaseOptions {
    private String identifier;
}
