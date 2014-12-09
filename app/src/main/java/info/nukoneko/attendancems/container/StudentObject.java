package info.nukoneko.attendancems.container;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;

import info.nukoneko.attendancems.common.json.JsonGetItem;

/**
 * Created by Telneko on 2014/12/04.
 */
public class StudentObject {
    private String userID;
    private String fullName;
    private String furiGana;
    private String gender;
    private String logName;
    public StudentObject(Object argsJson){
        try {
            JsonNode jsonNode = null;
            if (argsJson instanceof JsonNode) {
                jsonNode = (JsonNode) argsJson;
            } else if (argsJson instanceof String) {
                ObjectMapper objectMapper = new ObjectMapper();
                jsonNode = objectMapper.readValue((String) argsJson, JsonNode.class);
            } else {
                throw new IllegalArgumentException();
            }
            JsonGetItem json = new JsonGetItem(jsonNode);
            this.userID = json.getString("userID");
            this.fullName = json.getString("fullname");
            this.furiGana = json.getString("furigana");
            this.gender = json.getString("gender");
            this.logName = json.getString("logname");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    public String getFullName() {
        return fullName;
    }
    public String getFuriGana() {
        return furiGana;
    }

    public String getUserID() {
        return userID;
    }

    public String getGender() {
        return gender;
    }

    public String getLogname() {
        return logName;
    }
}
