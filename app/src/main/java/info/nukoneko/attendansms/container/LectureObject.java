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
public class LectureObject {
    private String lectureID;
    private String gradingName;
    private String name;
    private String teacherUserID;
    private String teacherName;
    private String aYear;
    private String semester;
    private String wDay;
    private String time;
    public LectureObject(Object argsJson){
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
            this.lectureID = json.getString("lectureID");
            this.gradingName = json.getString("grandingName");
            this.name = json.getString("name");
            this.teacherUserID = json.getString("teacherUserID");
            this.teacherName = json.getString("teacherName");
            this.aYear = json.getString("ayear");
            this.semester = json.getString("semester");
            this.wDay = json.getString("wday");
            this.time = json.getString("time");
        } catch (JsonMappingException e) {
            e.printStackTrace();
        } catch (JsonParseException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public String getName() {
        return name;
    }

    public String getTeacherName() {
        return teacherName;
    }

    public String getLectureID() {
        return lectureID;
    }

    public String getGradingName() {
        return gradingName;
    }

    public String getTeacherUserID() {
        return teacherUserID;
    }

    public String getSemester() {
        return semester;
    }

    public String getTime() {
        return time;
    }

    public String getaYear() {
        return aYear;
    }

    public String getwDay() {
        return wDay;
    }
}
