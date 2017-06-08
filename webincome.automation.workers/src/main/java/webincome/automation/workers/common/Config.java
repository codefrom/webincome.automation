package webincome.automation.workers.common;

import java.util.HashMap;

public class Config {
	private HashMap<String, Object> _map = new HashMap<String, Object>();
	
	public void setProperty(String name, Object value) {
		_map.put(name, value);
	}
	
	public <T> T getProperty(String name) {
		if(!_map.containsKey(name))
			return null;
		return (T)_map.get(name);
	}
}
