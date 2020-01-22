package tk.dmanstrator.scamkiller.utils;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class LongCollector implements ListCollector<Long> {

	@Override
	public List<Long> getValues(String content) {
		String[] csv = content.split(",");
		List<Long> list = Arrays.stream(csv)
			.map(elem ->  {
				try  {
					long id = Long.parseLong(elem);
					return id;
				}  catch (NumberFormatException ex) {
					return -1L;
				}
			})
			.filter(elem -> elem != -1)
			.collect(Collectors.toList());
		return list;
	}
}