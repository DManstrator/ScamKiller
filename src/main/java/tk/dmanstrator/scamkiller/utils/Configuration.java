package tk.dmanstrator.scamkiller.utils;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import org.json.JSONObject;

public class Configuration {
	
	private final static Logger LOGGER = Logger.getLogger(Configuration.class.getName());
    
    private final String token;
    
    public Configuration(String pathToConfig) {
        InputStream resourceStream = ClassLoader.getSystemClassLoader().getResourceAsStream(pathToConfig);
        
        if (resourceStream == null) {
        	LOGGER.log(Level.SEVERE, "Couldn't get configuration file!");
            token = null;
            return;
        }
        
        BufferedReader resReader = new BufferedReader(new InputStreamReader(resourceStream));
        String json = resReader.lines().collect(Collectors.joining());
        
        JSONObject jsonObject = new JSONObject(json);
        this.token = jsonObject.getString("token");
    }

    public String getToken() {
        return token;
    }
    
}