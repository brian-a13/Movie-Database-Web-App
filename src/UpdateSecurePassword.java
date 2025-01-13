import java.sql.*;
import java.util.ArrayList;
import java.util.Objects;

import org.jasypt.util.password.PasswordEncryptor;
import org.jasypt.util.password.StrongPasswordEncryptor;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.sql.DataSource;


/*
 * This is NOT a Servlet, which is why we can't look up the servlet config.
 * This program updates your existing moviedb customers table to change the
 * plain text passwords to encrypted passwords.
 *
 * You should only run this program **once**, because this program uses the
 * existing passwords as real passwords, then replace them. If you run it more
 * than once, it will treat the encrypted passwords as real passwords and
 * generate wrong values.
 *
 */
public class UpdateSecurePassword {
    public static void main(String[] args) throws Exception {
        String loginUser = "mytestuser";
        String loginPasswd = "mypassword";
        String loginUrl = "jdbc:mysql://18.188.27.136:3306/moviedb";
        Class.forName("com.mysql.jdbc.Driver").newInstance();
        try (Connection connection = DriverManager.getConnection(loginUrl, loginUser, loginPasswd);) {
            modifyAndEncryptCustomersTablePasswords(connection);
            modifyAndEncryptEmployeesTablePasswords(connection);
            System.out.println("finished both");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void modifyAndEncryptCustomersTablePasswords(Connection connection) throws SQLException {
        Statement statement = connection.createStatement();
        // change the customers table password column from VARCHAR(20) to VARCHAR(128)
        String alterQuery = "ALTER TABLE customers MODIFY COLUMN password VARCHAR(128)";
        int alterResult = statement.executeUpdate(alterQuery);
        System.out.println("altering customers table schema completed, " + alterResult + " rows affected");

        // get the ID and password for each customer
        String query = "SELECT id, password from customers";
        ResultSet resultSet = statement.executeQuery(query);

        // use the StrongPasswordEncryptor from jasypt library (Java Simplified Encryption)
        // it internally use SHA-256 algorithm and 10,000 iterations to calculate the encrypted password
        PasswordEncryptor passwordEncryptor = new StrongPasswordEncryptor();
        ArrayList<String> updateQueryList = new ArrayList<>();

        System.out.println("encrypting customer passwords (this might take a while)");
        addUpdateQueriesToList(resultSet, passwordEncryptor, updateQueryList, "customers");
        resultSet.close();

        // execute the update queries to update the password
        System.out.println("updating customer password");
        int count = 0;
        for (String updateQuery : updateQueryList) {
            int updateResult = statement.executeUpdate(updateQuery);
            count += updateResult;
        }
        statement.close();
        System.out.println("updating customer passwords completed, " + count + " rows affected");
    }

    private static void modifyAndEncryptEmployeesTablePasswords(Connection connection) throws SQLException {
        Statement statement = connection.createStatement();
        // change the employees table password column from VARCHAR(50) to VARCHAR(128)
        String alterQuery = "ALTER TABLE employees MODIFY COLUMN password VARCHAR(128)";
        int alterResult = statement.executeUpdate(alterQuery);
        System.out.println("altering employees table schema completed, " + alterResult + " rows affected");

        // get the email and password for each employee
        String query = "SELECT email, password from employees";
        ResultSet resultSet = statement.executeQuery(query);

        // use the StrongPasswordEncryptor from jasypt library (Java Simplified Encryption)
        // it internally use SHA-256 algorithm and 10,000 iterations to calculate the encrypted password
        PasswordEncryptor passwordEncryptor = new StrongPasswordEncryptor();
        ArrayList<String> updateQueryList = new ArrayList<>();

        System.out.println("encrypting employee passwords (this might take a while)");
        addUpdateQueriesToList(resultSet, passwordEncryptor, updateQueryList, "employees");
        resultSet.close();

        // execute the update queries to update the password
        System.out.println("updating employee password");
        int count = 0;
        for (String updateQuery : updateQueryList) {
            int updateResult = statement.executeUpdate(updateQuery);
            count += updateResult;
        }
        statement.close();
        System.out.println("updating employee passwords completed, " + count + " rows affected");
    }

    private static void addUpdateQueriesToList(ResultSet resultSet,
                                               PasswordEncryptor passwordEncryptor,
                                               ArrayList<String> updateQueryList,
                                               String typeTable) throws SQLException {
        while (resultSet.next()) {
            // get plain text password from current table
            String password = resultSet.getString("password");
            // encrypt the password using StrongPasswordEncryptor
            String encryptedPassword = passwordEncryptor.encryptPassword(password);
            // generate the update query
            String updateQuery;
            if (Objects.equals(typeTable, "customers")) {
                String id = resultSet.getString("id");
                updateQuery = String.format("UPDATE %s SET password='%s' WHERE id=%s;",
                        typeTable,
                        encryptedPassword,
                        id);
            }
            else {
                // employees update
                String email = resultSet.getString("email");
                updateQuery = String.format("UPDATE %s SET password='%s' WHERE email='%s';",
                        typeTable,
                        encryptedPassword,
                        email);
            }
            updateQueryList.add(updateQuery);
        }
    }
}
