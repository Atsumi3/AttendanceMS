package info.nukoneko.attendansms.container;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.util.ArrayList;

import info.nukoneko.attendansms.common.json.JsonGetItem;

/**
 * Created by Telneko on 2014/12/04.
 */
public class OnStartUpObject {
    private Integer numEnrollments;
    private LectureObject lecture;
    private ArrayList<StudentObject> enrollmentTable = new ArrayList<StudentObject>();
    private EntryObject resumeEntryList;

    public OnStartUpObject(Object argsJson){
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
            this.numEnrollments = json.getInteger("numEnrollments");
            this.lecture = new LectureObject(jsonNode.get("lecture"));
            JsonNode enrollment = jsonNode.get("enrollmentTable");
            if(enrollment != null) {
                for (int i = 0; enrollment.get(i) != null; i++) {
                    enrollmentTable.add(new StudentObject(enrollment.get(i)));
                }
            }
            this.resumeEntryList = new EntryObject(jsonNode.get("resumeEntryList"));
        } catch (JsonMappingException e) {
            e.printStackTrace();
        } catch (JsonParseException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public Integer getNumEnrollments() {
        return numEnrollments;
    }

    public LectureObject getLecture() {
        return lecture;
    }

    public ArrayList<StudentObject> getEnrollmentTable() {
        return enrollmentTable;
    }

    public EntryObject getResumeEntryList() {
        return resumeEntryList;
    }
}
