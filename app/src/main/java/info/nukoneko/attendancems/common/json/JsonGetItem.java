package info.nukoneko.attendancems.common.json;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * Created by Atsumi on 2014/12/03.
 */
public class JsonGetItem {
    private JsonNode json;
    public JsonGetItem(JsonNode jsonNode){
        this.json = jsonNode;
    }
    public String getString(String item){
        return json.get(item) == null ? "" : json.get(item).asText();
    }
    public Integer getInteger(String item){
        return json.get(item) == null ? 0: json.get(item).asInt();
    }
    public Long getLong(String item){
        return json.get(item) == null ? 0: json.get(item).asLong();
    }
    public Boolean getBoolean(String item){
        return json.get(item) != null && json.get(item).asBoolean();
    }
}
