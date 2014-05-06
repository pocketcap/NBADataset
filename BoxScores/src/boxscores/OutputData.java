package boxscores;

import java.util.HashMap;

/**
 *
 * @author Yahui Wang
 */
public class OutputData {

    public static void main(String[] args){
        
        for(int k = 0; k <= 13; k++){
            int j = k*100000;
            
            for(int i = 1; i <= 1240; i++){
                String GameID = "00" + (20000000 + j + i); // 0020000001 is first one
            
                BoxScoresParser bsp = new BoxScoresParser(GameID);
                bsp.outputBoxScores();
            
                String season = bsp.getSeason();
                String home = bsp.getHome();
                String visitor = bsp.getVisitor();
                HashMap<String, Object> team_h_members = bsp.getTeamH();
                HashMap<String, Object> team_v_members = bsp.getTeamV();
            
                if(season.isEmpty() || home.isEmpty() || visitor.isEmpty() || (team_h_members == null) || (team_v_members == null))
                    continue;
                
                PlayByPlayParser pbpp = new PlayByPlayParser(GameID, season, home, visitor);
                pbpp.setTeamH(team_h_members);
                pbpp.setTeamV(team_v_members);
                pbpp.outputPlayByPlay();
            
                System.out.println(GameID + " finished");
            }
        }
    }
}
