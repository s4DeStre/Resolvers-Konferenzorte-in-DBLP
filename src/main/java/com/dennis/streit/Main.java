package com.dennis.streit;

import java.sql.*;

public class Main
{


    public static void main(String[] args)
    {
        try {
            Connection con = getConnection();
            //DatabaseCreator.createCountryList();
            //DatabaseCreator.resultsFromLänder();
            //DatabaseCreator.createTableSQL(con);
            DatabaseCreator.townsInChina();
            //DatabaseCreator.insertIntoSQL(con, "Allcountries.json");
            DatabaseCreator.insertIntoSQL(con,"TownsInChina");
            //Resolver.parseCSV(con);



        } catch (java.lang.Exception ex) {
            ex.printStackTrace();
        }


    }


    //Verbindung zur Datenbank "locations "über MYSQL Java Driver
    public static Connection getConnection() throws Exception
    {
        try {
            String driver = "com.mysql.cj.jdbc.Driver";
            String url = "jdbc:mysql://127.0.0.1:3306/locations?useSSL=false&useLegacyDatetimeCode=false&serverTimezone=Europe/Berlin&allowPublicKeyRetrieval=true";
            String username = "user";
            String password = "user";
            Class.forName(driver);
            Connection con = DriverManager.getConnection(url, username, password);
            return con;


        } catch (Exception e) {
            System.out.println(e);
        }
        return null;

    }


}