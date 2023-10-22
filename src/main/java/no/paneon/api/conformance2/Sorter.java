package no.paneon.api.conformance2;

import static java.util.stream.Collectors.toList;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import no.paneon.api.utils.Out;

public class Sorter {

	static final Logger LOG = LogManager.getLogger(Sorter.class);

	public static List<String> sortedTreeView(List<String> input) {
		List<String> res = new LinkedList<>();

		if (input.isEmpty())
			return res;

		String pivot = selectPivotElement(input);

		List<String> first = input.stream().filter(s -> s.startsWith(pivot)).collect(toList());

		Map<Integer, List<String>> grouped = first.stream().sorted()
				.collect(Collectors.groupingBy(Sorter::numberOfParts));

		LOG.debug("## sorted:: first=" + first);

		if (grouped.keySet().size() == 1) {
			res.addAll(first);
		} else {
			Integer key = grouped.keySet().iterator().next();
			List<String> group = grouped.get(key);

			LOG.debug("## sorted:: group=" + group );

			List<String> subsequentGroup = group.stream().filter(s -> isNotContaining(first, s)).collect(toList());

			subsequentGroup.remove(pivot);

			LOG.debug("## sorted:: subsequentGroup=" + subsequentGroup);

			if (subsequentGroup.size() == 0) {
				res.addAll(group);

				first.removeAll(group);

				res.addAll(sortedTreeView(first));

			} else {

				group.removeAll(subsequentGroup);
				res.addAll(sortedTreeView(group));

				first.removeAll(group);
				res.addAll(sortedTreeView(first));

			}

		}

		List<String> remaining = input.stream().filter(s -> !s.startsWith(pivot)).collect(toList());

		LOG.debug("## sorted:: remaining=" + remaining );

		res.addAll(sortedTreeView(remaining));

		LOG.debug("## sorted:: res=" + res );

		return res;

	}

	private static String selectPivotElement(List<String> input) {
		String pivot = "";
		boolean foundCore=false;
		Iterator<String> iter = input.iterator();
		while (!foundCore && iter.hasNext()) {
			String candidate = iter.next();
			boolean isPrefix = input.stream().anyMatch(s -> !s.equals(candidate) && s.startsWith(candidate));

			LOG.debug("## sorted:: candidate={} isPrefix={}", candidate, isPrefix );

			if (!isPrefix) {
				foundCore = true;
				pivot = candidate.replaceAll("\\.[^\\.]+$", "");
			}
		}

		if (!foundCore) {
			pivot = input.get(0);
		}
		return pivot;
	}

	public static int numberOfParts(String s) {
		return s.replaceAll("[^.]", "").length();
	}

	private static boolean isNotContaining(List<String> list, String pivot) {
		return list.stream().anyMatch(s -> s.startsWith(pivot + "."));
	}

}
