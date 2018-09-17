package com.dennis.streit;

import com.opencsv.CSVReader;

import java.io.FileReader;
import java.io.FileWriter;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.List;

public class Resolver
{
    public static void parseCSV(Connection con)
    {
        try {
            String output = "";
            int bothFound = 0;
            int countriesFound =0;
            int citiesFound = 0;
            int nothingFound =0;

            CSVReader reader = new CSVReader(new FileReader("files/proceedings.csv"));
            List<String[]> proceedings = reader.readAll();
            //durch csv Dokument laufen und index+key entfernen
            // i=1 statt 0 um Header zu entfernen
            for (int i = 1; i < /*proceedings.size()*/10; i++) {
                //Konferenztitel in Token aufteilen
                String[] tokens = proceedings.get(i)[1].split("\\s*,\\s*");
                String[] prepTitle = tokens;
                //Boolean Werte zur Fallunterscheidung, resetten mit neuem Eintrag
                boolean foundCountry = false;
                boolean foundPair = false;
                boolean foundCity = false;
                for (int j = 0; j < tokens.length; j++) {
                    //nach aktuellem Token als Land in DB suchen
                    String[] currentCountry = searchForCountryinSQL(con, tokens[j]);
                    if (currentCountry != null) {
                        foundCountry = true;
                        //noch einmal Tokens durchlaufen um Stadt zu finden
                        for (int k = 0; k < tokens.length; k++) {
                            //j ist aktuelles Land und kann nicht Stadt sein
                            if (k != j) {
                                String[] pair = searchForCityWithCountry(con, tokens[j], tokens[k]);
                                if (pair != null) {
                                    foundPair = true;
                                    bothFound++;
                                    //Ausgabe vorbereiten
                                    for (int m = 0; m < prepTitle.length; m++) {
                                        if (m == j) prepTitle[m] ="<country countryId =\"Q"+ pair[1]+"\">"+prepTitle[m]+"</country>";
                                        if (m == k) prepTitle[m] ="<city cityId=\"Q"+ pair[3]+"\" country=\""+pair[0]+"\" countryId=\"Q"+pair[1]+">" + prepTitle[m]+"</city>";
                                    }

                                }
                            }
                        }
                        //Fall: nur Land gefunden und keine Stadt
                        if (!foundPair) {
                            countriesFound++;
                            for (int m = 0; m < prepTitle.length; m++) {
                                if (m == j) prepTitle[m] = "<country countryId =\"Q"+ currentCountry[1]+"\">"+prepTitle[m]+"</country>";
                            }
                        }
                    }
                }
                //falls: kein Land im String gefunden wurde
                if (!foundCountry) {
                    if (!foundPair) {
                        for (int j = 0; j < tokens.length; j++) {
                            //nach aktuellem Token als Land suchen
                            String[] currentCity = searchJustCityInSQL(con, tokens[j]);
                            if (currentCity != null) {
                                foundCity = true;
                                citiesFound++;
                                for (int m = 0; m < tokens.length; m++) {
                                    if (m == j)
                                        prepTitle[m] = "<city cityId=\"Q"+ currentCity[3]
                                                +"\" country=\""+currentCity[0]+"\" countryId=\"Q"
                                                +currentCity[1]+">" + prepTitle[m]+"</city>";
                                }
                            }
                        }
                    }
                    //falls: kein Land und keine Stadt gefunden
                    if (!foundCity) {
                        nothingFound++;
                        for (int m = 0; m < tokens.length; m++) {
                        }
                    }

                }
                output += Arrays.toString(prepTitle);
                output += "\n";
            }
            //Statistik
            output+="Paare gefunden: "+bothFound+", Einzelne Länder gefunden: "+countriesFound
                    +", Einzelne Städte gefunden: "+citiesFound+", Keins von beidem gefunden: "+nothingFound+"\n";
            FileWriter file = new FileWriter("files/parsedProceedings.csv");
            file.write(output);
            file.flush();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static String[] searchForCountryinSQL(Connection con, String länderToken)
    {
        try {
            PreparedStatement ps = con.prepareStatement("SELECT DISTINCT countryEntity, countryLabel FROM cities WHERE countryLabel = ? ");
            //gesuchtes Token eventuell substituieren
            ps.setString(1, checkCountrySynonyms(länderToken));
            ResultSet result = ps.executeQuery();
            if (result.next()) {
                String[] data = new String[]{result.getString("countryLabel"), result.getString("countryEntity")};
                return data;
            } else return null;
        } catch (SQLException e) {
            e.printStackTrace();
            return null;
        }

    }

    public static String[] searchForCityWithCountry(Connection con, String länderToken, String städteToken)
    {
        try {
            PreparedStatement ps = con.prepareStatement("SELECT  countryLabel, countryEntity, cityLabel, cityEntity  FROM cities WHERE countryLabel= ? and (cityLabel = ? or nativeLabel=?) ORDER BY population DESC;");
            ps.setString(1, checkCountrySynonyms(länderToken));
            ps.setString(2, städteToken);
            ps.setString(3, städteToken);
            ResultSet result = ps.executeQuery();
            if (result.next()) {
                String[] data = new String[]{result.getString("countryLabel"), result.getString("countryEntity"), result.getString("cityLabel"), result.getString("cityEntity")};
                return data;
            } else return null;
        } catch (Exception e) {
            System.out.println(e);
            return null;
        }
    }

    public static String[] searchJustCityInSQL(Connection con, String cityToken)
    {
        try {
            PreparedStatement ps = con.prepareStatement("SELECT  countryLabel, countryEntity, cityLabel, cityEntity  FROM cities WHERE cityLabel = ? or nativeLabel=? ORDER BY population DESC;");
            ps.setString(1, cityToken);
            ps.setString(2, cityToken);
            ResultSet result = ps.executeQuery();
            if (result.next()) {
                String[] data = new String[]{result.getString("countryLabel"), result.getString("countryEntity"), result.getString("cityLabel"), result.getString("cityEntity")};
                return data;
            } else return null;
        } catch (Exception e) {
            System.out.println(e);
            return null;
        }
    }
    //Aliase durch Wert in Datenbank ersetzen
    public static String checkCountrySynonyms(String country)
    {
        switch (country) {
            case "USA":
                country = "United States of America";
                break;
            case "USA.":
                country="United States of America";
                break;
            case "UK":
                country = "United Kingdom";
                break;
            case "China":
                country = "People's Republic of China";
                break;
            case "The Netherlands":
                country = "Netherlands";
                break;
            case "Korea":
                country = "South Korea";
                break;
            case "Republic of Korea":
                country = "South Korea";
                break;
            case "Korea (South)":
                country = "South Korea";
                break;
            case "Brasil":
                country = "Brazil";
                break;
            case "CSFR":
                country="Czech Republic";
                break;
            case "The Bahamas":
                country="Bahamas";
                break;
        }
        return country;
    }
}
