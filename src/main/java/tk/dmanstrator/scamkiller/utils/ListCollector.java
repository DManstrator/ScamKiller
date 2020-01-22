package tk.dmanstrator.scamkiller.utils;

import java.util.List;

@FunctionalInterface
public interface ListCollector<T>  {
	public List<T> getValues(String content);
}