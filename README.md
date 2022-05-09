# Basic implementation for Keycloak role-based user federation

### Role-based user federation for Keykcloak using Mariadb and Mysql:

This implementation is based on [https://github.com/kyrcha/keycloak-mysql-user-federation#readme](kychra-user-federation).
For installation process follow the link. For database table creation <ins>*mysql-scripts.txt* </ins> can be used.

In this implelentation Keycloak user federation considers users credentials as well as its role. 
This role is returned to keycloak and keycloak puts it in the user's token.
You Keycloak JWT should includ this property with your custom role name in the database:

```
  "resource_access": {
    "yourClientId": {
      "roles": [
        "admin"
      ]
    }
  }
```
