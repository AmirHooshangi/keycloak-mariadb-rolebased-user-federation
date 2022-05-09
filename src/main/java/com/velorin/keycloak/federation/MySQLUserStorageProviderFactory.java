package com.velorin.keycloak.federation;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.List;
import org.jboss.logging.Logger;
import org.keycloak.component.ComponentModel;
import org.keycloak.component.ComponentValidationException;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.provider.ProviderConfigProperty;
import org.keycloak.provider.ProviderConfigurationBuilder;
import org.keycloak.storage.UserStorageProviderFactory;

public class MySQLUserStorageProviderFactory implements UserStorageProviderFactory<MySQLUserStorageProvider> {
    private static final Logger logger = Logger.getLogger(MySQLUserStorageProviderFactory.class);

    protected static final List<ProviderConfigProperty> configMetadata;

    public static final String PROVIDER_NAME = "mysql-users";

    static {
        configMetadata = ProviderConfigurationBuilder.create().property().name("mysql")
                .type(ProviderConfigProperty.STRING_TYPE).label("MySQL URI")
                .defaultValue("jdbc:mysql://localhost/db?user=root").helpText("MySQL URI").add().property()
                .name("table").type(ProviderConfigProperty.STRING_TYPE).label("Users Table").defaultValue("user")
                .helpText("Table where users are stored").add().property().name("usernamecol")
                .type(ProviderConfigProperty.STRING_TYPE).label("Username Column").defaultValue("username")
                .helpText("Column name that holds the usernames").add().property().name("passwordcol")
                .type(ProviderConfigProperty.STRING_TYPE).label("Password Column").defaultValue("password")
                .helpText("Column name that holds the passwords").add().property().name("hash")
                .type(ProviderConfigProperty.LIST_TYPE).label("Hash Algorithm").defaultValue("SHA1")
                .options(Arrays.asList("SHA1", "MD5")).helpText("Algorithm used for hashing").add().build();
    }

    @Override
    public List<ProviderConfigProperty> getConfigProperties() {
        return configMetadata;
    }

    @Override
    public String getId() {
        return PROVIDER_NAME;
    }

    @Override
    public void validateConfiguration(KeycloakSession session, RealmModel realm, ComponentModel config)
            throws ComponentValidationException {
        String uri = config.getConfig().getFirst("mysql");
        if (uri == null)
            throw new ComponentValidationException("MySQL connection URI not present");
        Connection conn = null;
        try {
            conn = DriverManager.getConnection(uri);
            conn.isValid(1000);
        } catch (SQLException ex) {
            // handle any errors
            logger.error("SQLException: " + ex.getMessage());
            logger.error("SQLState: " + ex.getSQLState());
            logger.error("VendorError: " + ex.getErrorCode());
            throw new ComponentValidationException(ex.getMessage());
        }
    }

    @Override
    public MySQLUserStorageProvider create(KeycloakSession session, ComponentModel config) {
        String uri = config.getConfig().getFirst("mysql");

        Connection conn = null;
        try {
            conn = DriverManager.getConnection(uri);
        } catch (SQLException ex) {
            // handle any errors
            logger.error("SQLException: " + ex.getMessage());
            logger.error("SQLState: " + ex.getSQLState());
            logger.error("VendorError: " + ex.getErrorCode());
            throw new ComponentValidationException(ex.getMessage());
        }

        return new MySQLUserStorageProvider(session, config, conn);
    }

}
