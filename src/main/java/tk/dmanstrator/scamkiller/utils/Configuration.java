package tk.dmanstrator.scamkiller.utils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONObject;

public class Configuration {
    
    private final String token;
	private final List<Long> ids = new ArrayList<>();
	private final List<String> names = new ArrayList<>();
	
    public Configuration(String pathToConfig) {
    	String json = null;
		try {
			json = FileUtils.getContentFromFile(pathToConfig);
		} catch (IOException e) {
			e.printStackTrace();
		}
    	
    	if (json == null)  {
    		this.token = null;
    		return;
    	}
        
        JSONObject jsonObject = new JSONObject(json);
        this.token = jsonObject.getString("token");
        
        JSONArray idArr = jsonObject.getJSONArray("ids");
        for (Object obj : idArr)  {
        	if (!(obj instanceof Long))  {
        		continue;
        	}
        	long id = (Long) obj;
        	if (!ids.contains(id))  {
        		ids.add(id);
        	}
        }
        
        JSONArray nameArr = jsonObject.getJSONArray("names");
        for (Object obj : nameArr)  {
        	if (!(obj instanceof String))  {
        		continue;
        	}
        	String name = (String) obj;
        	if (!names.contains(name))  {
        		names.add(name);
        	}
        }
    }

	public String getToken() {
        return token;
    }
    
	public List<Long> getIds() {
		return ids;
	}
	
	public List<String> getNames() {
		return names;
	}
}