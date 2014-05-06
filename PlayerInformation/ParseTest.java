/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package parsetest;
import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Iterator;
import java.io.File;
import java.io.OutputStreamWriter;
import java.io.FileOutputStream;
import java.io.Writer;

import org.json.simple.JSONArray;
import org.json.simple.JSONValue;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

/**
 *
 * @author vyew
 */
public class ParseTest {

    /**
     * @param args the command line arguments
     */
    public static JSONObject getFromAPI(long playerID){
        JSONObject returnResults = new JSONObject();
        String url = "http://stats.nba.com/feeds/players/profile/"+playerID+"_Profile.js";
        try{
        InputStream inst = new URL(url).openStream();
        InputStreamReader isr = new InputStreamReader(inst);
        BufferedReader rd = new BufferedReader(isr);
        String temp;
        StringBuilder b = new StringBuilder();
        
        while ((temp = rd.readLine()) != null) {b.append(temp);}
        temp = b.toString();
        returnResults = (JSONObject) JSONValue.parse(temp);
        }catch (MalformedURLException e){
            System.out.print("Check uri...");
            e.printStackTrace();
        }catch (IOException e){
            e.printStackTrace();
        }
        
        return returnResults;
    }
    
    public static StringBuilder stringifyRow(JSONObject jo, String[] a){
        StringBuilder sb = new StringBuilder();
        sb.append(jo.get(a[0]));
        for(int i = 1; i < a.length; i++){
            sb.append(",");
            sb.append(jo.get(a[i]));
        }
        return sb;
    }
    
    public static void main(String[] args) {
       JSONParser parser = new JSONParser();
       String dir = "C:\\Users\\vyew\\Documents\\infsci2711\\dbb\\rawSave";
       String[] pb = {"Birthdate", "School", "Height", "Weight", "LastSeason", "FirstSeason", "DraftTeam", "DraftPick", "DraftRound"};
       String[] pts= {"TeamId", "City", "Nickname", "SeasonStart", "SeasonEnd"};
       StringBuilder playerBio = new StringBuilder();
       StringBuilder playerTeams = new StringBuilder();
       try {
		Object obj = parser.parse(new FileReader(dir+"\\allplayers.json"));
		JSONObject jsonObject = (JSONObject) obj;
		JSONArray resultSets = (JSONArray) jsonObject.get("resultSets");
		JSONObject resultSetsObject = (JSONObject) resultSets.get(0);
                JSONArray headers = (JSONArray) resultSetsObject.get("headers");

                playerBio.append("Person_ID,LastName,FirstName,Birthday,Birthyear");
                for (int i = 1; i < pb.length;i++) {
                    playerBio.append(",").append(pb[i]);
                }
                playerBio.append("\n");
                
                playerTeams.append("Person_ID");
                for (int i = 0; i < pts.length;i++) {
                    playerTeams.append(",").append(pts[i]);
                }
                playerTeams.append("\n");
                
                JSONArray rowSet = (JSONArray) resultSetsObject.get("rowSet");
                JSONArray rse;
                Iterator it;
                long pID;
                JSONObject jo = new JSONObject();
                JSONObject jo2, jo3, jo4,jo5;
                JSONArray ja = new JSONArray(), ja2, ja3;
                String pn;
                for (int i = 0; i < rowSet.size(); i++){
                    System.out.println (""+i+"/"+rowSet.size());
                    rse = (JSONArray)rowSet.get(i);
                    it = rse.iterator();
                    pID = (long)rse.get(0);
                    pn = (String)rse.get(1);
                    jo = getFromAPI(pID);
                    ja = (JSONArray)jo.get("PlayerProfile");
                    jo2 = (JSONObject)ja.get(0);
                    ja2 = (JSONArray) jo2.get("PlayerBio");
                    jo3 = (JSONObject) ja2.get(0);
                    playerBio.append(rse.get(0))
                             .append(",")
                             .append(pn.replaceAll("\\s", ""))
                             .append(",")
                             .append(stringifyRow(jo3, pb))
                             .append("\n");
                   
                    jo4 = (JSONObject)ja.get(1);
                    ja3 = (JSONArray) jo4.get("PlayerTeamSeasons");
                    for(int j = 0; j < ja3.size(); j++){
                        jo5 = (JSONObject) ja3.get(j);
                        playerTeams.append(pID)
                                   .append(",")
                                   .append(stringifyRow(jo5, pts))
                                   .append("\n");
                    }
                }
                File file = new File("C:\\Users\\vyew\\Documents\\infsci2711\\dbb\\rawSave\\pts.csv");
                Writer writer = new OutputStreamWriter(new FileOutputStream(file));
                writer.write(playerTeams.toString());
                writer.close();
                file = new File("C:\\Users\\vyew\\Documents\\infsci2711\\dbb\\rawSave\\pb.csv");
                writer = new OutputStreamWriter(new FileOutputStream(file));
                writer.write(playerBio.toString());
                writer.close();
                
	} catch (FileNotFoundException e) {
		e.printStackTrace();
	} catch (IOException e) {
		e.printStackTrace();
	} catch (ParseException e) {
		e.printStackTrace();
	}
    }
}
