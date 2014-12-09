package info.nukoneko.attendancems.container;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Map;

import info.nukoneko.attendancems.common.json.JsonGetItem;

/**
 * Created by Telneko on 2014/12/04.
 */
public class OnStartUpObject {
    private Integer numEnrollments;
    private LectureObject lecture;
    private ArrayList<StudentObject> enrollmentTable = new ArrayList<StudentObject>();
    private ArrayList<EntryObject> resumeEntryList = new ArrayList<EntryObject>();

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
                Iterator<Map.Entry<String, JsonNode>> enroll = enrollment.fields();
                while (enroll.hasNext()){
                    enrollmentTable.add(new StudentObject(enroll.next().getValue()));
                    enroll.remove();
                }
            }
            JsonNode resumeEntryList = jsonNode.get("resumeEntryList");
            if(resumeEntryList != null) {
                for (int i = 0; resumeEntryList.get(i) != null; i++) {
                    this.resumeEntryList.add(new EntryObject(resumeEntryList.get(i)));
                }
            }
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

    public ArrayList<EntryObject> getResumeEntryList() {
        return resumeEntryList;
    }
}
