package nl.recognize.dwh.application.model;

import lombok.Builder;
import lombok.Getter;
import nl.recognize.dwh.application.security.Role;

import java.util.Collections;
import java.util.List;

@Getter
@Builder
public class DwhUser {
    private String username;

    private String password;

    /**
     * @return array
     */
    public List<String> getRoles() {
        return Collections.singletonList(Role.ROLE_DWH_BRIDGE);
    }

    public String getUuid() {
        return username;
    }
}
