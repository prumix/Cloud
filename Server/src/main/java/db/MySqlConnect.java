package db;

import lombok.Getter;

import java.sql.*;

public class MySqlConnect {
    private static MySqlConnect instance;
    private static final String DRIVER = "com.mysql.cj.jdbc.Driver";
    private static final String DATABASE_URL = "jdbc:mysql://localhost:3306/cloud";
    private static final String USERNAME = "root";
    private static final String PASSWORD = "9647";

    @Getter
    private Connection connection;


    private MySqlConnect() {
        try {
            Class.forName(DRIVER);
            connection = DriverManager.getConnection(DATABASE_URL, USERNAME, PASSWORD);
        } catch (ClassNotFoundException | SQLException e) {
            e.printStackTrace();
        }
    }


    public static MySqlConnect getInstance() {
        if (instance == null) {
            instance = new MySqlConnect();
        }
        return instance;
    }

  public String getUsername(String username, String password){
        String user="null";
        String sql = "select user_name from users where user_name=? AND password=?";
        try (PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
            preparedStatement.setString(1,username);
            preparedStatement.setString(2,password);
            ResultSet resultSet = preparedStatement.executeQuery();
            resultSet.next();
            user = resultSet.getString(1);
            System.out.println("RESULT: " + user);
        } catch (SQLException throwables) {
            throwables.printStackTrace();
            System.out.println("RESULT USER: " + user);
        }
        return user;
  }
}
