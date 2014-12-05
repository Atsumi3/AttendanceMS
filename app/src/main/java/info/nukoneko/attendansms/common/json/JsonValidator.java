package info.nukoneko.attendansms.common.json;

/**
 * Created by TEJNEK on 2014/11/05.
 */
public class JsonValidator {
    private String json = new String();

    public JsonValidator setText(String text){
        this.json = text;
        return this;
    }
    public String validate(){
        String base = this.json.replace("\n", "").replace("\r\n", "").replace("\r", "").replace(" ", "");
        String crlf = System.getProperty("line.separator");
        String out = new String();
        int hierarchy = 0;
        for(int i = 1; i <= base.length(); i++){
            String tabs = new String();

            String a = base.substring(i-1,i);
            if(a.equals("{")|| a.equals("[") || a.equals("}") || a.equals("]") || a.equals(",")){
                if(a.equals("{") || a.equals("[")){
                    hierarchy++;
                }else if(a.equals("}") || a.equals("]")){
                    hierarchy--;
                }else if(a.equals(",")){
                }
                for(int j = 0;j < hierarchy; j++) tabs += "\t";

                if(a.equals("}")){
                    a = crlf + tabs + a;
                }else{
                    a = a + crlf + tabs;
                }
            }
            out += a;
        }
        return out;
    }
}
