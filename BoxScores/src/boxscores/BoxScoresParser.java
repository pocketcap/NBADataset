package boxscores;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

/**
 *
 * @author Yahui Wang
 */
public class BoxScoresParser {
    
    public String GameID;
    
    public String season = "";
    public String home = "";
    public String visitor = "";
    
    public HashMap<String, Object> team_h_members = null;
    public HashMap<String, Object> team_v_members = null;
    
    public String[] headers = {"GAME_ID","TEAM_ID","TEAM_ABBREVIATION","TEAM_CITY","PLAYER_ID","PLAYER_NAME","START_POSITION","COMMENT","MIN","FGM","FGA","FG_PCT","FG3M","FG3A","FG3_PCT","FTM","FTA","FT_PCT","OREB","DREB","REB","AST","STL","BLK","TO","PF","PTS","PLUS_MINUS"};
    public Map<String, Object> row_map = null;
    public String game_date_est = "";
    public long home_team_id = -1;
    public long visitor_team_id = -1;
    
    // Construtor
    public BoxScoresParser(String GameID){
        this.GameID = GameID;
    }
    
    public String getSeason(){
        return season;
    }
    
    public String getHome(){
        return home;
    }
    
    public String getVisitor(){
        return visitor;
    }
    
    public HashMap<String, Object> getTeamH(){
        return team_h_members;
    }
    
    public HashMap<String, Object> getTeamV(){
        return team_v_members;
    }
    
    public void outputBoxScores(){
        
        URL bcURL = null;
        URLConnection bcURLConnection = null;
        
        InputStreamReader isr = null;
        BufferedWriter bw = null;
        
        try{
            
            bcURL = new URL("http://stats.nba.com/stats/boxscore?GameID=" + GameID + "&RangeType=0&StartPeriod=0&EndPeriod=0&StartRange=0&EndRange=0&tabView=playbyplay");
            bcURLConnection = bcURL.openConnection();
            bcURLConnection.connect();
            
            InputStream fr = bcURLConnection.getInputStream();
            isr = new InputStreamReader(fr);
            
            JSONParser parser = new JSONParser();
	    Object obj = parser.parse(isr);
            JSONObject jsonObject = (JSONObject) obj;
            
            JSONArray resultSets = (JSONArray) jsonObject.get("resultSets");
            Iterator<JSONObject> iterator = resultSets.iterator();
            
            while(iterator.hasNext()){
                JSONObject _jo = (JSONObject)iterator.next();
                
                if(_jo.get("name").equals("GameSummary")){
                    
                    JSONArray _r_s = (JSONArray)_jo.get("rowSet");
                    JSONArray _r_s1 = (JSONArray)_r_s.get(0);
                    
                    season = (String)_r_s1.get(8);
                    
                    if(((String)_r_s1.get(5)).split("/").length > 1){
                        game_date_est = ((String)_r_s1.get(5)).split("/")[0];
                        home= ((String)_r_s1.get(5)).split("/")[1].substring(3, 6);
                        visitor = ((String)_r_s1.get(5)).split("/")[1].substring(0, 3);
                    }
                    
                    home_team_id = (long)_r_s1.get(6);
                    visitor_team_id = (long)_r_s1.get(7);
                }
                
                if(_jo.get("name").equals("PlayerStats")){
                    
                    if((!season.isEmpty()) && (!home.isEmpty()) && (!visitor.isEmpty())){
                        File output_directory = new File("/Users/Kuroba/Desktop/NBA/" + season);
                    
                        if(!output_directory.exists()){
                            output_directory.mkdir();
                        }
                        bw = new BufferedWriter(new FileWriter(output_directory + File.separator + GameID + "_" + visitor + home + "_box_scores.csv"));
                        outputHeader(bw);
                    }
                    
                    JSONArray _rowSet = (JSONArray)_jo.get("rowSet");
                    Iterator<JSONArray> iterator1 = _rowSet.iterator();
                    
                    while(iterator1.hasNext()){
                        
                        JSONArray _rs = (JSONArray)iterator1.next();
                        
                        row_map = new HashMap<String, Object>();
                        row_map.put("GAME_DATE_EST", game_date_est);
                        row_map.put("SEASON", season);
                        
                        for(int k = 0; k < headers.length; k++){
                            row_map.put(headers[k], _rs.get(k));
                        }
                        
                        if((long)_rs.get(1) == home_team_id){
                            
                            if(team_h_members == null)
                                team_h_members = new HashMap<String, Object>();
        
                            team_h_members.put((String)_rs.get(5), new HashMap<String, Integer>());
                        }
                        else if((long)_rs.get(1) == visitor_team_id){
                            
                            if(team_v_members == null)
                                team_v_members = new HashMap<String, Object>();
        
                            team_v_members.put((String)_rs.get(5), new HashMap<String, Integer>());
                        }
                        
                        outputRowMap(bw, row_map);
                    }
                }
            }
        }
        catch(FileNotFoundException e){
            e.printStackTrace();
	}
        catch(IOException e){
            System.out.println(GameID + " not existed");
	}
        catch(ParseException e){
            e.printStackTrace();
	}
        finally{
            try{
                
                if(isr != null)
                    isr.close();
                
                if(bw != null){
                    bw.flush();
                    bw.close();
                }
            }
            catch(IOException ex){
                ex.printStackTrace();
            }
        }
    }
    
    public void outputHeader(BufferedWriter bw) throws IOException{
        bw.write("GAME_DATE_EST" + ",");
        bw.write("SEASON" + ",");
        
        for(int k = 0; k < headers.length; k++){
            
            if(k == headers.length-1)
                bw.write(headers[k] + "\n");
            else
                bw.write(headers[k] + ",");
        }
                           
    }
    
    public void outputRowMap(BufferedWriter bw, Map<String, Object> row_map) throws IOException{
        bw.write(row_map.get("GAME_DATE_EST") + ",");
        bw.write(row_map.get("SEASON") + ",");
        
        for(int k = 0; k < headers.length; k++){
            
            if(k == headers.length-1)
                bw.write(row_map.get(headers[k]) + "\n");
            else
                bw.write(row_map.get(headers[k]) + ",");
        }
    }
    
}
