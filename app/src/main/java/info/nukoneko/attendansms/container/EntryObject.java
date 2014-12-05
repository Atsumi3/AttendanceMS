package info.nukoneko.attendansms.container;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;

import info.nukoneko.attendansms.common.json.JsonGetItem;

/**
 * Created by Telneko on 2014/12/04.
 */
public class EntryObject {
    private String time;
    private StudentObject student;
    private Integer groupID;
    private String result;
    private Boolean sound;
    // only use on adapter;
    private Boolean wasAnim = false;
    public EntryObject(Object argsJson){
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
            this.result = json.getString("result");
            this.time = json.getString("time");
            this.time = this.time.substring(0, 10);
            this.student = new StudentObject(jsonNode.get("student"));
            this.groupID = json.getInteger("groupID");
            this.sound = json.getBoolean("sound");
        } catch (JsonMappingException e) {
            e.printStackTrace();
        } catch (JsonParseException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public String getResult() {
        return result;
    }

    public StudentObject getStudent() {
        return student;
    }

    public Boolean getSound() {
        return sound;
    }

    public String getTime() {
        return time;
    }

    public Integer getGroupID() {
        return groupID;
    }

    public Boolean getWasAnim() {
        return wasAnim;
    }

    public void setWasAnim(Boolean wasAnim) {
        this.wasAnim = wasAnim;
    }
}
