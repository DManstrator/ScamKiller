package tk.dmanstrator.scamkiller.utils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public class FileUtils {
	
	private final static Logger LOGGER = Logger.getLogger(Configuration.class.getName());
	
	private FileUtils()  {}
	
    public static String getContentFromFile(String pathToFile) throws IOException {
        InputStream resourceStream = ClassLoader.getSystemClassLoader().getResourceAsStream(pathToFile);
        
        if (resourceStream == null) {
        	LOGGER.log(Level.SEVERE, String.format("Couldn't get file %s from resources!", pathToFile));
        	throw new IOException("The resource could not be found, the resource is in a package that is not opened unconditionally"
        			+ "or access to the resource is denied by the security manager");
        }
        
        BufferedReader resReader = new BufferedReader(new InputStreamReader(resourceStream));
        String input = resReader.lines().collect(Collectors.joining());
		return input;
	}
    
    public static boolean writeToFile(String pathToFile, String content)  {
    	return writeToFile(pathToFile, content, true);
    }
    
    public static boolean writeToFile(String pathToFile, String content, boolean append)  {
    	// TODO impl
    	return false;
    }
}