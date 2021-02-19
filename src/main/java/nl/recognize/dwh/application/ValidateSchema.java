package nl.recognize.dwh.application;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import nl.recognize.dwh.application.service.ValidationService;
import org.apache.logging.log4j.util.Strings;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.ExitCodeGenerator;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;

import java.util.List;

@SpringBootApplication
@Slf4j
@RequiredArgsConstructor
public class ValidateSchema implements ExitCodeGenerator
{
    private final ValidationService validationService;
    private int exitCode;

    public static void main(String[] args) {
        System.exit(
                SpringApplication.exit(
                        SpringApplication.run(ValidateSchema.class, args)
                )
        );
    }

    @Bean
    public CommandLineRunner commandLineRunner(ApplicationContext context) {
        return args -> {

            log.info("[Recognize DWH] Validating mapping...");
            List<String> errors = validationService.validate();
            if (errors.isEmpty()) {
                log.info("[Recognize DWH] Validation ok");
                exitCode = 0;
            } else {
                log.warn("The entity mapping does not match the current data model: \n{}", Strings.join(errors, '\n'));
                exitCode = 1;
            }
        };
    }

    @Override
    public int getExitCode() {
        return exitCode;
    }
}
