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
import java.util.Iterator;
import java.util.Map;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

/**
 *
 * @author Yahui Wang
 */
public class PlayByPlayParser {
    
    public String GameID;
    public String season;
    public String home;
    public String visitor;
    
    public Map<String, String> row_map = null;
    //public Set<String> etype = null;
    public HashMap<String, Object> team_h_members = null;
    public HashMap<String, Object> team_v_members = null;
    public Set<String> foul_types = null;
    public Set<String> shot_types = null;
    public int score_margin = 0;
    public Set<String> turnover_reasons = null;
    
    // Constructor
    public PlayByPlayParser(String GameID, String season, String home, String visitor){
        this.GameID = GameID;
        this.season = season;
        this.home = home;
        this.visitor = visitor;
    }
    
    public void outputPlayByPlay() {
        
        //fillEtype();
        //fillTeamH();
        //fillTeamV();
        fillFoulType();
        fillShotType();
        fillTurnoverReason();
        
        URL ppURL = null;
        URLConnection ppURLConnection = null;
        
        InputStreamReader isr = null;
        BufferedWriter bw = null;
 
	try {
            ppURL = new URL("http://stats.nba.com/stats/playbyplay?GameID=" + GameID + "&StartPeriod=0&EndPeriod=10");
            ppURLConnection = ppURL.openConnection();
            ppURLConnection.connect();
            
            InputStream fr = ppURLConnection.getInputStream();
            isr = new InputStreamReader(fr);
            
            File output_directory = new File("/Users/Kuroba/Desktop/NBA/" + season);
            
            if(!output_directory.exists()){
                output_directory.mkdir();
            }
            
            bw = new BufferedWriter(new FileWriter(output_directory + File.separator + GameID + "_" + visitor + home + "_play_by_play.csv"));
            
            JSONParser parser = new JSONParser();
	    Object obj = parser.parse(isr);
            JSONObject jsonObject = (JSONObject) obj;
            
            /**
            JSONObject parameters = (JSONObject) jsonObject.get("parameters");
            String GameID = (String)parameters.get("GameID");
            long StartPeriod = (Long)parameters.get("StartPeriod");
            long EndPeriod = (Long)parameters.get("EndPeriod");
            */
                
            JSONArray resultSets = (JSONArray) jsonObject.get("resultSets");
            Iterator<JSONObject> iterator = resultSets.iterator();
            
            while(iterator.hasNext()){
                JSONObject _jo = (JSONObject)iterator.next();
                    
                if(_jo.get("name").equals("PlayByPlay")){
                    
                    outputHeader(bw);
                    
                    //JSONArray headers = (JSONArray)_jo.get("headers");
                    
                    JSONArray rowSet = (JSONArray)_jo.get("rowSet");   
                    Iterator<JSONArray> iterator1 = rowSet.iterator();
                    
                    while(iterator1.hasNext()){
                        JSONArray _rs = (JSONArray)iterator1.next();
                        
                        buildRowMap();
                        
                        // attribute: period, time
                        row_map.put("period", Long.toString((Long)_rs.get(4)));
                        row_map.put("time", (String)_rs.get(6));
                        
                        if((_rs.get(7) != null) || (_rs.get(9) != null)){
                            
                            String _description = _rs.get(7) != null ? _rs.get(7).toString() : _rs.get(9).toString();
                            
                            String _second_description = null;
                            
                            if(_rs.get(9) != null){ // in case homedescription and visitordescription both exist
                                _second_description = _rs.get(9).toString();
                            }
                            
                            // attribute: team, player, assist, block, steal
                            String _action = null;
                            String _assist = null;
                            
                            if(_description.contains("(")){
                                _action = _description.split("\\(")[0];
                                for(String _each_part : _description.split("[\\(||//)]")){
                                    
                                    if(_each_part.contains("AST"))
                                        _assist = _each_part;
                                }
                            }
                            
                            if(_rs.get(7) != null){ // HOMEDESCRIPTION exists
                                row_map.put("team", home);
                                
                                Map<String, Object> team_members = team_h_members;
                                Map<String, Object> second_team_members = team_v_members;
                                
                                if(_second_description != null){ // homedescription and visitordescription both exist
                                          
                                    if((!_description.contains("MISS")) && (!_description.contains("Turnover"))){
                                        row_map.put("team", visitor);
                                            
                                        if(_second_description.contains("MISS") || _second_description.contains("Turnover")){
                                            String tmp_description = _description;
                                            _description = _second_description;
                                            _second_description = tmp_description;
                                            
                                            team_members = team_v_members;
                                            second_team_members = team_h_members;
                                                
                                            if(_description.contains("(")){
                                                _action = _description.split("\\(")[0];
                                                for(String _each_part : _description.split("[\\(||//)]")){
                                                    
                                                    if(_each_part.contains("AST"))
                                                        _assist = _each_part;
                                                }
                                            }
                                        }
                                    }
                                    
                                    if(_second_description.contains("BLOCK") || _second_description.contains("STEAL")){
                                        for(String player_name : second_team_members.keySet()){
                                            String player_last_name = "";
                                            
                                            if(player_name.split(" ").length > 1)
                                                player_last_name = player_name.split(" ")[1]; // keep last name
                                        
                                            if(_second_description.contains(player_last_name)){
                                                
                                                if(_second_description.contains("BLOCK"))
                                                    row_map.put("block", player_name);
                                                else if(_second_description.contains("STEAL"))
                                                    row_map.put("steal", player_name);
                                            }
                                        }
                                    }
                                }
                                
                                for(String player_name : team_members.keySet()){
                                    String player_last_name = "";
                                    
                                    if(player_name.split(" ").length > 1)
                                        player_last_name = player_name.split(" ")[1]; // keep last name
                                
                                    if((_action != null) && (_assist != null)){
                                        if(_action.contains(player_last_name))
                                            row_map.put("player", player_name);
                                    
                                        if(_assist.contains(player_last_name))
                                            row_map.put("assist", player_name);
                                    }
                                    else{
                                        if(_description.contains(player_last_name))
                                            row_map.put("player", player_name);
                                    }
                                }
                            }
                            else { // VISITORDESCRIPTION exists
                                row_map.put("team", visitor);
                                for(String player_name : team_v_members.keySet()){
                                    String player_last_name = "";
                                    
                                    if(player_name.split(" ").length > 1)
                                        player_last_name = player_name.split(" ")[1]; // keep last name
                                
                                    if((_action != null) && (_assist != null)){
                                        if(_action.contains(player_last_name))
                                            row_map.put("player", player_name);
                                    
                                        if(_assist.contains(player_last_name))
                                            row_map.put("assist", player_name);
                                    }
                                    else{
                                        if(_description.contains(player_last_name))
                                            row_map.put("player", player_name);
                                    }
                                }
                            }
                            
                            // attribute: etype(foul), type
                            for(String foul_type : foul_types){
                                
                                if(_description.contains(foul_type)){
                                    row_map.put("etype", "foul");
                                    switch(foul_type){
                                        case "S.FOUL":
                                            row_map.put("type", "shooting");
                                            break;
                                        case "P.FOUL":
                                            row_map.put("type", "personal");
                                            break;
                                        case "L.B.FOUL":
                                            row_map.put("type", "loose ball");
                                            break;
                                        case "OFF.Foul":
                                            row_map.put("type", "offensive");
                                            break;
                                        case "T.Foul":
                                            row_map.put("type", "team");
                                            break;
                                        case "C.P.FOUL":
                                            row_map.put("type", "clear path");
                                            break;
                                        default:
                                            break;
                                    }
                                }
                            }
                            
                            // attribute: etype(free throw), num, outof, reason, result
                            if(_description.contains("Free Throw")){
                                row_map.put("etype", "free throw");
                                
                                Pattern num_outof = Pattern.compile("[0-9] of [0-9]");
                                Matcher m_num_outof = num_outof.matcher(_description);
                                
                                if(m_num_outof.find()){
                                    row_map.put("num", Character.toString(_description.charAt(m_num_outof.start())));
                                    row_map.put("outof", Character.toString(_description.charAt(m_num_outof.end()-1)));
                                }
                                
                                row_map.put("reason", "foul"); // !!need check!!
                                
                                if(_description.contains("PTS")){ // means "made"
                                    row_map.put("result", "made");
                                }
                                else if (_description.contains("MISS")){ // means "missed"
                                    row_map.put("result", "missed");
                                }
                            }
                            
                            // attribute: team, etype(jump ball), away, home, possession
                            if(_description.contains("Jump Ball")){
                                row_map.put("team", "OFF");
                                row_map.put("etype", "jump ball");
                                
                                if(!row_map.get("player").isEmpty())
                                    row_map.put("player", "");
                                
                                String home_part = null;
                                String away_part = null;
                                String tip_to = null;
                                
                                if((_description.contains(" vs. ")) || (_description.contains(": Tip to "))){
                                    home_part = _description.split(" vs. ")[0];
                                    away_part = (_description.split(" vs. ")[1]).split(": Tip to ")[0];
                                    
                                    if((_description.split(" vs. ")[1]).split(": Tip to ").length > 1)
                                        tip_to = (_description.split(" vs. ")[1]).split(": Tip to ")[1];
                                    
                                    for(String home_player_name : team_h_members.keySet()){
                                        String home_player_last_name = "";
                                        
                                        if(home_player_name.split(" ").length > 1)
                                            home_player_last_name = home_player_name.split(" ")[1]; // keep last name
                                        
                                        if(home_part.contains(home_player_last_name))
                                            row_map.put("home", home_player_name);
                                        
                                        if(tip_to != null){
                                            
                                            if(tip_to.contains(home_player_last_name))
                                                row_map.put("possession", home_player_name);
                                        }
                                    }
                                    
                                    for(String away_player_name : team_v_members.keySet()){
                                        
                                        String away_player_last_name = "";
                                        
                                        if(away_player_name.split(" ").length > 1)
                                            away_player_last_name = away_player_name.split(" ")[1]; // keep last name
                                        
                                        if(away_part.contains(away_player_last_name))
                                            row_map.put("away", away_player_name);
                                        
                                        if(tip_to != null){
                                            
                                            if(tip_to.contains(away_player_last_name))
                                                row_map.put("possession", away_player_name);
                                        }
                                    }
                                }
                                
                                
                            }
                            
                            // attribute: etype(rebound), type
                            if(_description.contains("REBOUND") || _description.contains("Rebound")){
                                row_map.put("etype", "rebound");
                                
                                if(_description.contains("Rebound")){
                                    
                                    if(!row_map.get("player").isEmpty())
                                        row_map.put("player", "");
                                    
                                    if(!row_map.get("type").isEmpty())
                                        row_map.put("type", "");
                                }
                                else if(_description.contains("REBOUND") && _description.contains("Off:") && _description.contains("Def:")){
                                    
                                    int _rb_off = Integer.parseInt((_description.split("Off:")[1]).split(" ")[0]);
                                    int _rb_def = Integer.parseInt((_description.split("Def:")[1]).split("\\)")[0]);
                                    
                                    if((!row_map.get("player").isEmpty()) && (!row_map.get("team").isEmpty())){
                                        
                                        if(row_map.get("team").equals(home)){
                                            
                                            if(team_h_members.get(row_map.get("player")) != null){
                                                Map<String, Integer> a_member = (Map<String, Integer>)team_h_members.get(row_map.get("player"));
                                            
                                                if(a_member.get("rb_off") == null)
                                                    a_member.put("rb_off", 0);
                                                    
                                                if(a_member.get("rb_def") == null)
                                                    a_member.put("rb_def", 0);
                                                
                                                
                                                if(_rb_off > a_member.get("rb_off")){
                                                    row_map.put("type", "off");
                                                    a_member.put("rb_off", a_member.get("rb_off") + 1);
                                                }
                                                
                                                else if(_rb_def > a_member.get("rb_def")){
                                                    row_map.put("type", "def");
                                                    a_member.put("rb_def", a_member.get("rb_def") + 1);
                                                }
                                            }
                                        }
                                        else if(row_map.get("team").equals(visitor)){
                                            
                                            if(team_v_members.get(row_map.get("player")) != null){
                                                Map<String, Integer> a_member = (Map<String, Integer>)team_v_members.get(row_map.get("player"));
                                            
                                                if(a_member.get("rb_off") == null)
                                                    a_member.put("rb_off", 0);
                                                    
                                                if(a_member.get("rb_def") == null)
                                                    a_member.put("rb_def", 0);
                                                
                                                
                                                if(_rb_off > a_member.get("rb_off")){
                                                    row_map.put("type", "off");
                                                    a_member.put("rb_off", a_member.get("rb_off") + 1);
                                                }
                                                
                                                else if(_rb_def > a_member.get("rb_def")){
                                                    row_map.put("type", "def");
                                                    a_member.put("rb_def", a_member.get("rb_def") + 1);
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                            
                            // attribute: etype(shot), points, result, type
                            // !!!need modify!!!
                            if((_description.contains("Shot")) || (_description.contains("Dunk")) || (_description.contains("Layup")) || (_description.contains("Roll"))){
                                row_map.put("etype", "shot");
                                
                                if(_description.contains("Shot"))
                                    row_map.put("type", "shot");
                                else if(_description.contains("Dunk"))
                                    row_map.put("type", "dunk");
                                else if(_description.contains("Layup"))
                                    row_map.put("type", "layup");
                                else if(_description.contains("Roll"))
                                    row_map.put("type", "roll");
                                
                                for(String shot_type : shot_types){
                                    
                                    if(_description.contains(shot_type)){
                                        row_map.put("type", shot_type.toLowerCase());
                                        
                                        int cur_score_margin = score_margin;
                                        
                                        if(_rs.get(11) != null){
                                    
                                            if(_rs.get(11).equals("TIE"))
                                                cur_score_margin = 0;
                                            else
                                                cur_score_margin = Integer.parseInt((String)_rs.get(11));
                                        }   
                                
                                        int points = cur_score_margin - score_margin;
                                        int is_positive = 0;
                                        
                                        if(row_map.get("team").equals(home))
                                            is_positive = 1;
                                        else if(row_map.get("team").equals(visitor))
                                            is_positive = -1;
                                        
                                        points = points * is_positive;
                                        
                                        if(points > 0){ // means "made"
                                            row_map.put("points", Integer.toString(points));
                                            row_map.put("result", "made");
                                        }
                                        else // means "missed"
                                            row_map.put("result", "missed");
                                    }
                                }
                            }
                            
                            if(_rs.get(11) != null){
                                
                                if(_rs.get(11).equals("TIE"))
                                    score_margin = 0;
                                else
                                    score_margin = Integer.parseInt((String)_rs.get(11));
                            }
                            
                            // attribute: etype(sub), entered, left
                            if(_description.contains("SUB:") && _description.contains("FOR")){
                                
                                row_map.put("etype", "sub");
                                
                                if(!row_map.get("player").isEmpty())
                                    row_map.put("player", "");
                                
                                String entered = (_description.split("SUB: ")[1]).split(" FOR ")[0];
                                String left = (_description.split("SUB: ")[1]).split(" FOR ")[1];
                                   
                                if(row_map.get("team").equals(home)){   
                                    for(String player_name : team_h_members.keySet()){
                                        String player_last_name = "";
                                        
                                        if(player_name.split(" ").length > 1)
                                            player_last_name = player_name.split(" ")[1]; // keep last name
                                        
                                        if(entered.contains(player_last_name))
                                            row_map.put("entered", player_name);
                                        
                                        if(left.contains(player_last_name))
                                            row_map.put("left", player_name);
                                    }
                                }
                                else if(row_map.get("team").equals(visitor)){
                                    for(String player_name : team_v_members.keySet()){
                                        String player_last_name = "";
                                        
                                        if(player_name.split(" ").length > 1)
                                            player_last_name = player_name.split(" ")[1]; // keep last name
                                        
                                        if(entered.contains(player_last_name))
                                            row_map.put("entered", player_name);
                                        
                                        if(left.contains(player_last_name))
                                            row_map.put("left", player_name);
                                    }
                                }
                                
                            }
                            
                            // attribute: etype(timeout), type
                            if(_description.contains("Timeout")){
                                row_map.put("etype", "timeout");
                                
                                if(_description.contains("Regular"))
                                    row_map.put("type", "regular");
                                else if(_description.contains("Short"))
                                    row_map.put("type", "short");
                            }
                            
                            // attribute: etype(turnover), reason
                            
                            if(_description.contains("Turnover")){
                                row_map.put("etype", "turnover");
                                
                                for(String turnover_reason : turnover_reasons){
                                    
                                    if(_description.contains(turnover_reason)){
                                        row_map.put("reason", turnover_reason.toLowerCase());
                                    }
                                }
                            }
                            
                            // attribute: etype(violation), type
                            if(_description.contains("Violation:")){
                                row_map.put("etype", "violation");
                                
                                String vio_type = _description.split("Violation:")[1];
                                row_map.put("type", vio_type.toLowerCase());
                            }
                        }
                        
                        // attribute: team, etype(timeout), type
                        else{ // NEUTRALDESCRIPTION exists
                            row_map.put("team", "OFF");
                            
                            String _description = (String)_rs.get(8);
                            if(_description.contains("Timeout")){
                                row_map.put("team", home);
                                row_map.put("etype", "timeout");
                                row_map.put("type", "official");
                            }
                        }
                        
                        outputRowMap(bw, row_map);
                        
                        /**
                        for(int i = 0; i < _rs.size(); i++){
                            if(i == _rs.size() - 1)
                                System.out.println(_rs.get(i));
                            else
                                System.out.print(_rs.get(i) + " ");
                        }
                        */
                    } // end of while(iterator1.hasNext())
                } 
            } // end of while(iterator.hasNext())
	}
        catch(FileNotFoundException e){
            e.printStackTrace();
	}
        catch(IOException e) {
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
    
    /**
    // !!!need modify!!!
    public void fillEtype(){
        
        if(etype == null)
            etype = new HashSet<String>();
        etype.add("foul");
        etype.add("free throw");
        etype.add("jump ball");
        etype.add("rebound");
        etype.add("shot");
        etype.add("sub");
        etype.add("timeout");
        etype.add("turnover");
        etype.add("violation");
    }
    */
    
    public void setTeamH(HashMap<String, Object> teamH){
        
        team_h_members = (HashMap<String, Object>)teamH.clone();
    }
    
    public void setTeamV(HashMap<String, Object> teamV){
        
        team_v_members = (HashMap<String, Object>)teamV.clone();
    }
    
    /**
    public void fillTeamH(){
        
        if(team_h_members == null)
            team_h_members = new HashMap<String, Object>();
        team_h_members.put("Larry Johnson", new HashMap<String, Integer>());
        team_h_members.put("Latrell Sprewell", new HashMap<String, Integer>());
        team_h_members.put("Marcus Camby", new HashMap<String, Integer>());
        team_h_members.put("Allan Houston", new HashMap<String, Integer>());
        team_h_members.put("Charlie Ward", new HashMap<String, Integer>());
        team_h_members.put("Chris Childs", new HashMap<String, Integer>());
        team_h_members.put("Glen Rice", new HashMap<String, Integer>());
        team_h_members.put("Kurt Thomas", new HashMap<String, Integer>());
        team_h_members.put("Erick Strickland", new HashMap<String, Integer>());
        team_h_members.put("Lavor Postell", new HashMap<String, Integer>());
        team_h_members.put("Felton Spencer", new HashMap<String, Integer>());
        team_h_members.put("Travis Knight", new HashMap<String, Integer>());
    }
    
    public void fillTeamV(){
        
        if(team_v_members == null)
            team_v_members = new HashMap<String, Object>();
        team_v_members.put("Tyrone Hill", new HashMap<String, Integer>());
        team_v_members.put("George Lynch", new HashMap<String, Integer>());
        team_v_members.put("Theo Ratliff", new HashMap<String, Integer>());
        team_v_members.put("Allen Iverson", new HashMap<String, Integer>());
        team_v_members.put("Eric Snow", new HashMap<String, Integer>());
        team_v_members.put("Aaron McKie", new HashMap<String, Integer>());
        team_v_members.put("Vernon Maxwell", new HashMap<String, Integer>());
        team_v_members.put("Todd MacCulloch", new HashMap<String, Integer>());
        team_v_members.put("Toni Kukoc", new HashMap<String, Integer>());
        team_v_members.put("Jumaine Jones", new HashMap<String, Integer>());
        team_v_members.put("Juan Sanchez", new HashMap<String, Integer>());
        team_v_members.put("Nazr Mohammed", new HashMap<String, Integer>());
    }
    */
    
    // !!!need modify!!!
    public void fillFoulType(){
        
        if(foul_types == null)
            foul_types = new HashSet<String>();
        foul_types.add("S.FOUL"); // shooting
        foul_types.add("P.FOUL"); // personal
        foul_types.add("L.B.FOUL"); // loose ball
        foul_types.add("OFF.Foul"); // offensive
        foul_types.add("T.Foul"); // team
        foul_types.add("C.P.FOUL"); // clear path
    }
    
    // !!!need modify!!!
    public void fillShotType(){
        
        if(shot_types == null)
            shot_types = new HashSet<String>();
        shot_types.add("Turnaround Jump");
        shot_types.add("Turnaround Hook");
        shot_types.add("Turnaround Fade Away");
        shot_types.add("Turnaround Bank");
        shot_types.add("Turnaround Bank Hook");
        shot_types.add("Tip");
        shot_types.add("Step Back");
        shot_types.add("Slam");
        shot_types.add("Running Slam");
        shot_types.add("Running");
        shot_types.add("Running Jump");
        shot_types.add("Runing");
        shot_types.add("Runing Bank");
        shot_types.add("Reverse");
        shot_types.add("Putback");
        shot_types.add("Pullup Jump");
        shot_types.add("Jump Hook");
        shot_types.add("Jump");
        shot_types.add("Floating Jump");
        shot_types.add("Finger Rool");
        shot_types.add("Driving Reverse Layup");
        shot_types.add("Driving Layup");
        shot_types.add("Driving Finger Roll");
        shot_types.add("3PT");
        shot_types.add("Finger Roll");
    }
    
    // !!!need modify!!!
    public void fillTurnoverReason(){
        if(turnover_reasons == null)
            turnover_reasons = new HashSet<String>();
        turnover_reasons.add("Lost Ball");
        turnover_reasons.add("Bad Pass");
        turnover_reasons.add("Short Clock");
        turnover_reasons.add("Foul");
        turnover_reasons.add("Step Out of Bounds");
        turnover_reasons.add("Violation");
    }
    
    public void buildRowMap(){
        row_map = new HashMap<String, String>();
        row_map.put("period", "");
        row_map.put("time", "");
        row_map.put("team", "");
        row_map.put("etype", "");
        row_map.put("assist", "");
        row_map.put("away", "");
        row_map.put("block", "");
        row_map.put("entered", "");
        row_map.put("home", "");
        row_map.put("left", "");
        row_map.put("num", "");
        row_map.put("outof", "");
        row_map.put("player", "");
        row_map.put("points", "");
        row_map.put("possession", "");
        row_map.put("reason", "");
        row_map.put("result", "");
        row_map.put("steal", "");
        row_map.put("type", "");
    }
    
    public void outputHeader(BufferedWriter bw) throws IOException{
        bw.write("period" + ",");
        bw.write("time" + ",");
        bw.write("team" + ",");
        bw.write("etype" + ",");
        bw.write("assist" + ",");
        bw.write("away" + ",");
        bw.write("block" + ",");
        bw.write("entered" + ",");
        bw.write("home" + ",");
        bw.write("left" + ",");
        bw.write("num" + ",");
        bw.write("outof" + ",");
        bw.write("player" + ",");
        bw.write("points" + ",");
        bw.write("possession" + ",");
        bw.write("reason" + ",");
        bw.write("result" + ",");
        bw.write("steal" + ",");
        bw.write("type" + "\n");
    }
    
    public void outputRowMap(BufferedWriter bw, Map<String, String> row_map) throws IOException{
        bw.write(row_map.get("period") + ",");
        bw.write(row_map.get("time") + ",");
        bw.write(row_map.get("team") + ",");
        bw.write(row_map.get("etype") + ",");
        bw.write(row_map.get("assist") + ",");
        bw.write(row_map.get("away") + ",");
        bw.write(row_map.get("block") + ",");
        bw.write(row_map.get("entered") + ",");
        bw.write(row_map.get("home") + ",");
        bw.write(row_map.get("left") + ",");
        bw.write(row_map.get("num") + ",");
        bw.write(row_map.get("outof") + ",");
        bw.write(row_map.get("player") + ",");
        bw.write(row_map.get("points") + ",");
        bw.write(row_map.get("possession") + ",");
        bw.write(row_map.get("reason") + ",");
        bw.write(row_map.get("result") + ",");
        bw.write(row_map.get("steal") + ",");
        bw.write(row_map.get("type") + "\n");
    }
}
