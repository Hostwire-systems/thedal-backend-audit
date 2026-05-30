package com.thedal.thedal_app;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class MessageUtil {
    private static Properties properties = new Properties();

    static {
        try (InputStream input = MessageUtil.class.getClassLoader().getResourceAsStream("messages.properties")) {
            if (input == null) {
                throw new IOException("Unable to find messages.properties");
            }
            properties.load(input);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static String getMessage(String key) {
        return properties.getProperty(key);
    }
}

