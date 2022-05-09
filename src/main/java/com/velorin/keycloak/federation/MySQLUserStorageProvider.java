package com.velorin.keycloak.federation;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;

import org.apache.commons.codec.digest.DigestUtils;
import org.jboss.logging.Logger;
import org.keycloak.component.ComponentModel;
import org.keycloak.credential.CredentialInput;
import org.keycloak.credential.CredentialInputUpdater;
import org.keycloak.credential.CredentialInputValidator;
import org.keycloak.credential.CredentialModel;
import org.keycloak.models.*;
import org.keycloak.storage.ReadOnlyException;
import org.keycloak.storage.StorageId;
import org.keycloak.storage.UserStorageProvider;
import org.keycloak.storage.adapter.AbstractUserAdapter;
import org.keycloak.storage.user.UserLookupProvider;

public class MySQLUserStorageProvider
        implements UserStorageProvider, UserLookupProvider, CredentialInputValidator
        , CredentialInputUpdater {
    protected KeycloakSession session;
    protected Connection conn;
    protected ComponentModel config;


    //TODO: read from properties file
    private static final String clientId = "my-microservice";



    private static final Logger logger = Logger.getLogger(MySQLUserStorageProvider.class);

    public MySQLUserStorageProvider(KeycloakSession session, ComponentModel config, Connection conn) {
        this.session = session;
        this.config = config;
        this.conn = conn;
    }

    @Override
    public UserModel getUserByUsername(String username, RealmModel realm) {

        PreparedStatement userPreparedStatement = null;
        ResultSet userTebleResult = null;
        UserModel adapter = null;
        try {
            String query = "SELECT * FROM "
                    + this.config.getConfig().getFirst("table") + " WHERE "
                    + this.config.getConfig().getFirst("usernamecol") + "=?;";

            userPreparedStatement = conn.prepareStatement(query);
            userPreparedStatement.setString(1, username);
            userTebleResult = userPreparedStatement.executeQuery();
            Integer users_role_id = null;
            if(userTebleResult.next()) {
                users_role_id = userTebleResult.getInt("role_model_id");
            }

            ResultSet role_result;
            PreparedStatement rolePreparedStatement = null;
            String roleName = null;
            try {
                rolePreparedStatement = conn.prepareStatement("SELECT * FROM role WHERE id =?;");
                rolePreparedStatement.setInt(1, users_role_id);
                role_result = rolePreparedStatement.executeQuery();
                if (role_result.next()) {
                    roleName = role_result.getString("role_name");
                }
            } catch (SQLException e) {
                //TODO: handle exception based on your needs
                e.printStackTrace();
            }
            try{
            String pword = null;
            pword = userTebleResult.getString(this.config.getConfig().getFirst("passwordcol"));
            if (pword != null) {
                adapter = createAdapter(realm, username, roleName);
            }}catch (NullPointerException ex){
                ex.printStackTrace();
            }
            // Now do something with the ResultSet ....
        } catch (SQLException ex) {
            //TODO: handle exception based on your needs
            System.out.println("SQLException: " + ex.getMessage());
            System.out.println("SQLState: " + ex.getSQLState());
            System.out.println("VendorError: " + ex.getErrorCode());
        }
     catch (NullPointerException ex) {
         //TODO: handle exception based on your needs
         System.out.println("NULL Pointer Exception : user and password not found " );

    }finally {
            // it is a good idea to release
            // resources in a finally{} block
            // in reverse-order of their creation
            // if they are no-longer needed

            if (userTebleResult != null) {
                try {
                    userTebleResult.close();
                } catch (SQLException sqlEx) {
                } // ignore

                userTebleResult = null;
            }

            if (userPreparedStatement != null) {
                try {
                    userPreparedStatement.close();
                } catch (SQLException sqlEx) {
                } // ignore

                userPreparedStatement = null;
            }
        }
        return adapter;
    }

    protected UserModel createAdapter(RealmModel realm, String username, String roleName) {
        return new AbstractUserAdapter(session, realm, config) {

            @Override
            public String getUsername() {
                return username;
            }

            @Override
            public Set<RoleModel> getRoleMappings() {
                Set<RoleModel> roleModelMap = new HashSet<>();

                RoleModel roleModel = new RoleModel() {

                    @Override
                    public String getName() {
                        return roleName;
                    }

                    @Override
                    public String getDescription() {
                        return "${role_"+roleName+"}";
                    }

                    @Override
                    public void setDescription(String s) {

                    }

                    @Override
                    public String getId() {
                        return roleName;
                    }

                    @Override
                    public void setName(String s) {

                    }

                    @Override
                    public boolean isComposite() {
                        return false;
                    }

                    @Override
                    public void addCompositeRole(RoleModel roleModel) {

                    }

                    @Override
                    public void removeCompositeRole(RoleModel roleModel) {

                    }

                    @Override
                    public Set<RoleModel> getComposites() {
                        return null;
                    }

                    @Override
                    public boolean isClientRole() {
                        return true;
                    }

                    //TODO: change client id here
                    @Override
                    public String getContainerId() {
                        return isClientRole() ? clientId : realm.getId();
                    }


                    //TODO: check
                    @Override
                    public RoleContainerModel getContainer() {
                        return isClientRole() ? /*realm.getClientById("role.getClientId()")*/ realm.getClientByClientId(clientId) : realm;
                    }

                    @Override
                    public boolean hasRole(RoleModel roleModel) {
                        return true;
                    }

                    @Override
                    public void setSingleAttribute(String s, String s1) {

                    }

                    @Override
                    public void setAttribute(String s, Collection<String> collection) {

                    }

                    @Override
                    public void removeAttribute(String s) {

                    }

                    @Override
                    public String getFirstAttribute(String s) {
                        return null;
                    }

                    @Override
                    public List<String> getAttribute(String s) {
                        /*List<String> attStrings = new ArrayList<>();
                        attStrings.add("getAttributes1");
                        return attStrings*/;
                        return null;
                    }

                    @Override
                    public Map<String, List<String>> getAttributes() {
                        return null;
                    }
                };

                roleModelMap.add(roleModel);
                return roleModelMap;
            }
        };
    }

    @Override
    public UserModel getUserById(String id, RealmModel realm) {
        StorageId storageId = new StorageId(id);
        String username = storageId.getExternalId();
        return getUserByUsername(username, realm);
    }

    @Override
    public UserModel getUserByEmail(String email, RealmModel realm) {
        return null;
    }

    @Override
    public boolean isConfiguredFor(RealmModel realm, UserModel user, String credentialType) {
        String password = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        try {
            String query = "SELECT " + this.config.getConfig().getFirst("usernamecol") + ", "
                    + this.config.getConfig().getFirst("passwordcol") + " FROM "
                    + this.config.getConfig().getFirst("table") + " WHERE "
                    + this.config.getConfig().getFirst("usernamecol") + "=?;";
            pstmt = conn.prepareStatement(query);
            pstmt.setString(1, user.getUsername());
            rs = pstmt.executeQuery();
            if (rs.next()) {
                password = rs.getString(this.config.getConfig().getFirst("passwordcol"));
            }
            // Now do something with the ResultSet ....
        } catch (SQLException ex) {
            //TODO: handle exception based on your needs
            System.out.println("SQLException: " + ex.getMessage());
            System.out.println("SQLState: " + ex.getSQLState());
            System.out.println("VendorError: " + ex.getErrorCode());
        } finally {
            // it is a good idea to release
            // resources in a finally{} block
            // in reverse-order of their creation
            // if they are no-longer needed

            if (rs != null) {
                try {
                    rs.close();
                } catch (SQLException sqlEx) {
                } // ignore

                rs = null;
            }

            if (pstmt != null) {
                try {
                    pstmt.close();
                } catch (SQLException sqlEx) {
                } // ignore

                pstmt = null;
            }
        }
        return credentialType.equals(CredentialModel.PASSWORD) && password != null;
    }

    @Override
    public boolean supportsCredentialType(String credentialType) {
        return credentialType.equals(CredentialModel.PASSWORD);
    }

    @Override
    public boolean isValid(RealmModel realm, UserModel user, CredentialInput input) {
        if (!supportsCredentialType(input.getType()))
            return false;
        String password = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        try {
            String query = "SELECT " + this.config.getConfig().getFirst("usernamecol") + ", "
                    + this.config.getConfig().getFirst("passwordcol") + " FROM "
                    + this.config.getConfig().getFirst("table") + " WHERE "
                    + this.config.getConfig().getFirst("usernamecol") + "=?;";
            pstmt = conn.prepareStatement(query);
            pstmt.setString(1, user.getUsername());
            rs = pstmt.executeQuery();
            if (rs.next()) {
                password = rs.getString(this.config.getConfig().getFirst("passwordcol"));
            }
            // Now do something with the ResultSet ....
        } catch (SQLException ex ) {
            // handle any errors
            System.out.println("line 207 isValid");
            System.out.println("SQLException: " + ex.getMessage());
            System.out.println("SQLState: " + ex.getSQLState());
            System.out.println("VendorError: " + ex.getErrorCode());
        } finally {
            // it is a good idea to release
            // resources in a finally{} block
            // in reverse-order of their creation
            // if they are no-longer needed

            if (rs != null) {
                try {
                    rs.close();
                } catch (SQLException sqlEx) {
                } // ignore

                rs = null;
            }

            if (pstmt != null) {
                try {
                    pstmt.close();
                } catch (SQLException sqlEx) {
                } // ignore

                pstmt = null;
            }
        }

        if (password == null)
            return false;

        String hex = null;
        if (this.config.getConfig().getFirst("hash").equalsIgnoreCase("SHA1")) {
            hex = DigestUtils.sha1Hex(input.getChallengeResponse());
        } else {
            hex = DigestUtils.md5Hex(input.getChallengeResponse());
        }
        return password.equalsIgnoreCase(hex);
    }

    @Override
    public boolean updateCredential(RealmModel realm, UserModel user, CredentialInput input) {
        if (input.getType().equals(CredentialModel.PASSWORD))
            throw new ReadOnlyException("user is read only for this update");

        return false;
    }

    @Override
    public void disableCredentialType(RealmModel realm, UserModel user, String credentialType) {

    }

    @Override
    public Set<String> getDisableableCredentialTypes(RealmModel realm, UserModel user) {
        return Collections.EMPTY_SET;
    }

    @Override
    public void close() {
        if (conn != null) {
            try {
                conn.close();
            } catch (SQLException sqlEx) {
                logger.error(sqlEx.getMessage());
            } // ignore
            conn = null;
        }
    }

}
