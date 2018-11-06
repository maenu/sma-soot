package ch.unibe.scg.sma.soot;

import java.io.File;
import java.io.IOException;
import java.util.BitSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.io.FileUtils;

import com.google.common.base.Charsets;
import com.google.common.io.Files;

import soot.Body;
import soot.BodyTransformer;
import soot.Local;
import soot.Main;
import soot.PackManager;
import soot.Transform;
import soot.Unit;
import soot.jimple.DefinitionStmt;
import soot.tagkit.StringTag;
import soot.toolkits.graph.BriefUnitGraph;
import soot.toolkits.graph.UnitGraph;
import soot.toolkits.scalar.ForwardFlowAnalysis;
import soot.util.cfgcmd.CFGToDotGraph;

public class ReachingDefinitionsAnalysis
		extends ForwardFlowAnalysis<Unit, ReachingDefinitionsAnalysis.ReachingDefinitions> {

	public static void main(String[] args) throws IOException, InterruptedException {
		// @formatter:off
		// @see https://www.sable.mcgill.ca/soot/tutorial/usage/ for all command line options
		 args = new String[] {
				"-allow-phantom-refs", 					// use stubs for unresolved classes
				"-print-tags-in-output",				// statements can have tags, e.g. line numbers, print
				//"--keep-line-number",					// keep line numbers from original sources
				"-p", "jb", "use-original-names",		// whenever possible use original local variable names in the jimple body builder phase
				"-output-format", "none",				// output nothing
				"--soot-class-path", "src/test/java",	// lookup classes from JAR/directory
				"ch.unibe.scg.sma.soot.TestClass"		// analyze class
		};
		// @formatter:on
		run(args);
	}

	public static void run(String[] args) throws IOException, InterruptedException {
		PackManager.v().getPack("jtp").add(new Transform("jtp.analyze", new BodyTransformer() {
			@Override
			protected void internalTransform(Body body, String phase, @SuppressWarnings("rawtypes") Map options) {
				try {
					run(body);
				} catch (IOException e) {
					throw new RuntimeException(e);
				}
			}
		}));
		// clean output
		File directory = new File("reaching-definitions");
		FileUtils.deleteDirectory(directory);
		directory.mkdirs();
		// run
		Main.v().run(args);
		// create pdfs
		ProcessBuilder processBuilder = new ProcessBuilder("/bin/bash", "--login", "-c",
				"find reaching-definitions/ -name '*.dot' -exec dot -Tpdf \"{}\" -o \"{}.pdf\" \\;");
		processBuilder.inheritIO();
		processBuilder.start().waitFor();
	}

	public static void run(Body body) throws IOException {
		/*
		 * unit graph is CFG created from jimple body
		 * 
		 * @see subclasses of UnitGraph for alternatives
		 */
		BriefUnitGraph unitGraph = new BriefUnitGraph(body);
		ReachingDefinitionsAnalysis analysis = new ReachingDefinitionsAnalysis(unitGraph);
		analysis.start();
		// dump cfg
		CFGToDotGraph cfgToDot = new CFGToDotGraph();
		cfgToDot.drawCFG(unitGraph, body).plot("reaching-definitions/" + body.getMethod().getSignature() + ".dot");
		// dump raw body
		for (Unit unit : body.getUnits()) {
			unit.removeAllTags();
		}
		Files.write(body.toString().replaceAll("\n+", "\n"),
				new File("reaching-definitions/" + body.getMethod().getSignature() + "-raw.jimple"), Charsets.UTF_8);
		// dump reaching definitions body
		for (Unit unit : body.getUnits()) {
			List<String> reachingDefinitions = analysis.getReachingDefinitionsAt(unit).stream().map(Object::toString)
					.collect(Collectors.toList());
			reachingDefinitions.sort((a, b) -> a.compareTo(b));
			String output = String.join("\n    ", reachingDefinitions.toArray(new String[0]));
			unit.addTag(new StringTag("\n    " + output + "\n"));
		}
		Files.write(body.toString().replaceAll("\n+", "\n"),
				new File("reaching-definitions/" + body.getMethod().getSignature() + "-reaching-definitions.jimple"),
				Charsets.UTF_8);
	}

	/**
	 * In the lattice, bottom is false, and top is true. This bit set is passed
	 * around, copied, and mutated by the flow analysis.
	 */
	@SuppressWarnings("serial")
	protected class ReachingDefinitions extends BitSet {

		public ReachingDefinitions() {
			super(used);
		}

		public ReachingDefinitions(ReachingDefinitions other) {
			super(used);
			or(other);
		}

		public boolean get(DefinitionStmt key) {
			int index = definitionIndex.get(key);
			return get(index);
		}

		public void put(DefinitionStmt key, boolean value) {
			int index;
			if (!definitionIndex.containsKey(key)) {
				index = used;
				used = used + 1;
				definitionIndex.put(key, index);
			} else {
				index = definitionIndex.get(key);
			}
			set(index, value);
		}
	}

	protected final HashMap<DefinitionStmt, Integer> definitionIndex;
	protected int used;

	public ReachingDefinitionsAnalysis(UnitGraph graph) {
		super(graph);
		definitionIndex = new HashMap<DefinitionStmt, Integer>();
		used = 0;
	}

	public void start() {
		doAnalysis();
	}

	public Set<DefinitionStmt> getReachingDefinitionsAt(Unit unit) {
		ReachingDefinitions reachingDefinitions = this.getFlowBefore(unit);
		return definitionIndex.keySet().stream().filter(reachingDefinitions::get).collect(Collectors.toSet());
	}

	@Override
	protected ReachingDefinitions newInitialFlow() {
		return new ReachingDefinitions();
	}

	@Override
	protected void copy(ReachingDefinitions in, ReachingDefinitions out) {
		out.clear();
		out.or(in);
	}

	/**
	 * REACH(in) = U REACH(out(p))
	 */
	@Override
	protected void merge(ReachingDefinitions in1, ReachingDefinitions in2, ReachingDefinitions out) {
		out.clear();
		out.or(in1);
		out.or(in2);
	}

	@Override
	protected void flowThrough(ReachingDefinitions in, Unit unit, ReachingDefinitions out_) {
		// do not modify in, it is read-only, instead create copy to modify
		ReachingDefinitions out = new ReachingDefinitions(in);
		if (isLocalDefinition(unit)) {
			Local local = getLocalDefinedIn(unit);
			Set<DefinitionStmt> kill = getDefinitionsDefining(local);
			DefinitionStmt gen = (DefinitionStmt) unit;
			// in - kill
			for (DefinitionStmt stmt : kill) {
				out.put(stmt, false);
			}
			// gen U (in - kill)
			out.put(gen, true);
		}
		// ensure out is exactly as the potentially modified in
		copy(out, out_);
	}

	private boolean isLocalDefinition(Unit unit) {
		if (!(unit instanceof DefinitionStmt)) {
			return false;
		}
		DefinitionStmt stmt = (DefinitionStmt) unit;
		return stmt.getLeftOp() instanceof Local;
	}

	private Local getLocalDefinedIn(Unit unit) {
		assert isLocalDefinition(unit);
		return (Local) ((DefinitionStmt) unit).getLeftOp();
	}

	private Set<DefinitionStmt> getDefinitionsDefining(Local local) {
		return definitionIndex.keySet().stream().filter(d -> getLocalDefinedIn(d) == local).collect(Collectors.toSet());
	}

}
