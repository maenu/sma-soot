package ch.unibe.scg.sma.soot;

import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.stream.Stream;

import org.jboss.util.NotImplementedException;

import soot.Body;
import soot.BodyTransformer;
import soot.Main;
import soot.PackManager;
import soot.SootClass;
import soot.SootMethod;
import soot.Transform;

public class InternalInvocationAnalysis {

	public static void main(String[] args) throws IOException, InterruptedException {
		String jar = Stream.of(System.getProperty("java.class.path").split(System.getProperty("path.separator")))
				.filter(s -> s.endsWith("guava-18.0.jar")).findFirst().get();
		System.out.println("Analyzing " + jar);
		// @formatter:off
		// @see https://www.sable.mcgill.ca/soot/tutorial/usage/ for all command line options
		 args = new String[] {
				"-allow-phantom-refs", 					// use stubs for unresolved classes
				"-output-format", "none",				// output nothing
				"--soot-class-path", jar,				// lookup classes from JAR/directory
				"-process-dir", jar						// analyze JAR/directory
		};
		// @formatter:on
		run(args);
	}

	public static void run(String[] args) throws IOException, InterruptedException {
		InternalInvocationAnalysis analysis = new InternalInvocationAnalysis();
		PackManager.v().getPack("jtp").add(new Transform("jtp.analyze", new BodyTransformer() {

			/**
			 * Synchronized as analysis is not thread-safe.
			 */
			@Override
			protected synchronized void internalTransform(Body body, String phase,
					@SuppressWarnings("rawtypes") Map options) {
				analysis.process(body);
			}

		}));
		Main.v().run(args);
		// print top 10 methods
		Iterator<Entry<SootMethod, Integer>> iterator = analysis.getMostUsedMethods().entrySet().iterator();
		for (int i = 0; i < 10; i = i + 1) {
			if (!iterator.hasNext()) {
				break;
			}
			Entry<SootMethod, Integer> entry = iterator.next();
			System.out.println(String.format("%d\t%s", entry.getValue(), entry.getKey().getSignature()));
		}
	}

	private Map<SootMethod, Integer> internalInvocations;

	public InternalInvocationAnalysis() {
		internalInvocations = new HashMap<>();
	}

	public SortedMap<SootMethod, Integer> getMostUsedMethods() {
		TreeMap<SootMethod, Integer> map = new TreeMap<>((a, b) -> {
			return -internalInvocations.getOrDefault(a, 0).compareTo(internalInvocations.getOrDefault(b, 0));
		});
		map.putAll(internalInvocations);
		return map;
	}

	public void process(Body body) {
		// FIXME Implement this method using the internalInvocations map to count
		// invocations to methods of guava itself. Use isInternal to check if the
		// declaring class of a method is actually a method defined by the analyzed
		// application itself. Pseudo code: iterate over the units in the body, cast
		// each Unit to a Stmt, check if it contains an invocation, check if the invoked
		// method's declaring class is internal and increase the counter in the
		// internalInvocations map
		throw new NotImplementedException();
	}

	private boolean isInternal(SootClass clazz) {
		return clazz.isApplicationClass();
	}

}
