package nl.recognize.dwh.application.util;

import org.springframework.util.StringUtils;

import java.util.Arrays;
import java.util.List;

public class NameHelper
{
    public static String camelToSnake(String subject) {
        String firstChar = subject.substring(0, 1);
        String remainder = (subject.length() > 1 ? subject.substring(1) : "");
        return firstChar.toLowerCase() + remainder;
    }

    public static String dashToCamel(String subject) {
        return StringUtils.capitalize(subject).replace("-", "");
    }

    public static List<String> splitPluralName(String name) {
        String pluralName = name;
        String singularName = pluralName;

        if (!name.endsWith("s")) {
            pluralName += "List";
        } else {
            singularName = name.substring(0, name.length() -1);
        }

        return Arrays.asList(pluralName, singularName);
    }
}
