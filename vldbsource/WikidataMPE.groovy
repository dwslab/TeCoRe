/*
 * This file is part of the PSL software.
 * Copyright 2011-2013 University of Maryland
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package edu.umd.cs.example;

import java.text.DecimalFormat;

import edu.umd.cs.psl.application.inference.LazyMPEInference
import edu.umd.cs.psl.application.inference.MPEInference;
import edu.umd.cs.psl.application.learning.weight.maxlikelihood.MaxLikelihoodMPE;
import edu.umd.cs.psl.config.*
import edu.umd.cs.psl.database.DataStore
import edu.umd.cs.psl.database.Database;
import edu.umd.cs.psl.database.DatabasePopulator;
import edu.umd.cs.psl.database.Partition;
import edu.umd.cs.psl.database.ReadOnlyDatabase;
import edu.umd.cs.psl.database.rdbms.RDBMSDataStore
import edu.umd.cs.psl.database.rdbms.driver.H2DatabaseDriver
import edu.umd.cs.psl.database.rdbms.driver.H2DatabaseDriver.Type
import edu.umd.cs.psl.groovy.PSLModel;
import edu.umd.cs.psl.groovy.PredicateConstraint;
import edu.umd.cs.psl.groovy.SetComparison;
import edu.umd.cs.psl.model.argument.ArgumentType;
import edu.umd.cs.psl.model.argument.GroundTerm;
import edu.umd.cs.psl.model.argument.UniqueID;
import edu.umd.cs.psl.model.argument.Variable;
import edu.umd.cs.psl.model.atom.GroundAtom;
import edu.umd.cs.psl.model.function.ExternalFunction;
import edu.umd.cs.psl.ui.functions.textsimilarity.*
import edu.umd.cs.psl.ui.loading.InserterUtils;
import edu.umd.cs.psl.util.database.Queries;

/*
 * The first thing we need to do is initialize a ConfigBundle and a DataStore
 */

/*
 * A ConfigBundle is a set of key-value pairs containing configuration options.
 * One place these can be defined is in psl-example/src/main/resources/psl.properties
 */
ConfigManager cm = ConfigManager.getManager()
ConfigBundle config = cm.getBundle("basic-example")

/* Uses H2 as a DataStore and stores it in a temp. directory by default */
def defaultPath = System.getProperty("java.io.tmpdir")
String dbpath = config.getString("dbpath", defaultPath + File.separator + "basic-example")
DataStore data = new RDBMSDataStore(new H2DatabaseDriver(Type.Disk, dbpath, true), config)

/*
 * Now we can initialize a PSLModel, which is the core component of PSL.
 * The first constructor argument is the context in which the PSLModel is defined.
 * The second argument is the DataStore we will be using.
 */

def withErrorTag = {
	PSLModel m = new PSLModel(this, data)
	
	m.add predicate: "triple",       types: [ArgumentType.String, ArgumentType.String, ArgumentType.String]
	m.add predicate: "conflict",       types: [ArgumentType.String, ArgumentType.String, ArgumentType.String]
	m.add predicate: "conflictExtended",       types: [ArgumentType.String, ArgumentType.String, ArgumentType.Long, ArgumentType.Long, ArgumentType.String, ArgumentType.Long, ArgumentType.Long]
	m.add predicate: "conflictEvtn",       types: [ArgumentType.Long, ArgumentType.Long]
	m.add predicate: "tf",       types: [ArgumentType.String, ArgumentType.String, ArgumentType.String, ArgumentType.Long, ArgumentType.Long, ArgumentType.String]
	m.add predicate: "person",  types:  [ArgumentType.String]
	
	m.add function: "bnFiftteenNineteen" , implementation: new BNFiftteenNineteenW()  //
	m.add function: "belowTwentyOne" , implementation: new BelowTwentyOneW()  //
	m.add function: "disjoint" , implementation: new AllenDisjointW()  //
	m.add function: "belowFifty" , implementation: new BelowFiftyW()  //
	m.add function: "aboveFifty" , implementation: new AboveFiftyW()  //
	m.add function: "aboveSixteen" , implementation: new AboveSixteenOrEqual()  //
	m.add function: "superiorThan40" , implementation: new InequalityGreaterThanW()  // comparisons of the form X > 20
	m.add function: "validLifeSpan" , implementation: new ValidLifeSpanW()    //comparisons of the form X - Y  < 150
	m.add function: "notEqual" , implementation: new NotEqual()    //comparisons of the form X != Y
	m.add function: "strCMP", implementation: new CMPStringSimilarity()  // compare strings "aaa" = "bb"
	m.add function: "greaterThan", implementation: new GreaterThan()  // compare X > Y
	m.add function: "lessThan", implementation: new LessThan()  // compare X < Y
	m.add function: "overlaps", implementation: new AllenOverlaps()  // Allen's overlap
	m.add function: "before", implementation: new AllenBeforeW()  // Allen's overlap inverse
	
	
	
	
	// #1. A person cannot have two birth dates
	m.add rule :  ( tf(X, '"P569"', Y, Begin1, End1, Original)  &  tf(X, '"P569"', Z, Begin2, End2, Original)  & 
		notEqual(Begin1, Begin2) ) >> conflict(X, "P569", Y), weight : 400.0
	// #2. A person cannot have two death dates
	m.add rule :  ( tf(X, '"P570"', Y, Begin1, End1, Original)  &  tf(X, '"P570"', Z, Begin2, End2, Original)  & 
		notEqual(Begin1, Begin2) ) >> conflict(X, '"P570"', Y), weight : 400.0
	// #3. A person's birth date is before his/her death date
	m.add rule :  ( tf(X, '"P569"', Y, Begin1, End1, Original)  &  tf(X, '"P570"', Z, Begin2, End2, Original)  & 
		~lessThan(Begin1, Begin2) ) >> conflict(X, "borndied", Y), weight : 400.0
	m.add rule :  ( tf(X, '"P569"', Y, Begin1, End1, Original)  &  tf(X, '"P570"', Z, Begin2, End2, Original)  & 
		~validLifeSpan(Begin1, Begin2) ) >> conflict(X, "validSpan", Y), weight : 400.0
	// #4. A person must be born before playing for a team.
	m.add rule :  ( tf(X, '"P569"', Y, Begin1, End1, Original)  &  tf(X, '"P54"', Z, Begin2, End2, Original)  & 
		~before(Begin1, End1, Begin2, End2) ) >> conflict(X, "playsForBeforeBorn", Z), weight : 400.0
	// #5. A person must be alive to play for a team.
	m.add rule :  ( tf(X, '"P570"', Y, Begin1, End1, Original)  &  tf(X, '"P54"', Z, Begin2, End2, Original)  & 
		~before(Begin2, End2, Begin1, End1) ) >> conflict(X, "playsForAfterDeath", Z), weight : 400.0
	// #6. A person must be atleast 16 before playing for a premier league club.  [SOFT CONSTRAINT]
	m.add rule :  ( tf(X, '"P569"', Y, Begin1, End1, Original)  &  tf(X, '"P54"', Z, Begin2, End2, Original)  & 
		~aboveSixteen(Begin1, Begin2) ) >> conflict(X, "aboveSixteen", Z), weight : 4.0                   //conflict(X, "aboveSixteen", Z), weight : 4.0
	// #7. Someone who is older than 50 years does not play in a club.  [SOFT]
	m.add rule :  ( tf(X, '"P569"', Y, Begin1, End1, Original)  &  tf(X, '"P54"', Z, Begin2, End2, Original)  & 
		~belowFifty(Begin1, Begin2) ) >> conflict(X, "belowFifty", Z), weight : 4.0
	// #8. A footballer cannot play for two different clubs at the same time/period.  [correponds to Allen's DISJOINT]
	m.add rule : ( tf(X, '"P54"', Y, Begin1, End1, Original)  &  tf(X, '"P54"', Z, Begin2, End2, Original) & 
		~strCMP(Y, Z) & ~disjoint(Begin1, End1, Begin2, End2) ) >> conflictExtended(X, Y, Begin1, End1, Z, Begin2, End2), weight : 400.0
	
	// #9. A retired player is someone, that played for a club and is older 50 years
	m.add rule : ( tf(X, '"P569"', Y, Begin1, End1, Original)  & tf(X, '"P54"', Z, Begin2, End2, Original) &
		 aboveFifty(Begin1) ) >> triple(X, "is", "RetiredPlayer"), weight : 4.0
	// #10. A young player in a team is someone who is at most 21 years old.
	m.add rule : ( tf(X, '"P569"', Y, Begin1, End1, Original)  & tf(X, '"P54"', Z, Begin2, End2, Original) & 
		belowTwentyOne(Begin1) ) >> triple(X, "is", "YoungPlayer"), weight : 4.0
	// #11. A teen player is someone who is at least 15 and at most 19 years old.
	m.add rule : ( tf(X, '"P569"', Y, Begin1, End1, Original)  & tf(X, '"P54"', Z, Begin2, End2, Original) & 
		bnFiftteenNineteen(Begin1) ) >> triple(X, "is", "TeenPlayer"), weight : 4.0
	// #12. DivorcedPerson ? [Refine this constraint]
	//Long thisYear = new Long(2016);
	//m.add rule : ( tf(X, "P26", Y, Begin1, End1)  &  ~tf(X, "P26", Z, Begin1, thisYear) & ~strCMP(Y, Z) ) >> triple(X, "is", "DivorcedPerson"), weight : 4.0
	// #13. The inverse of spouse relation holds
	m.add rule : ( tf(X, '"P26"', Y, Begin1, End1, Original) ) >> triple(X, '"P26"', Y), weight : 4.0
	
	// #14. A person cannot be married to two distinct individuals.
	m.add rule : ( tf(X, '"P26"', Y, Begin1, End1, Original)  &  tf(X, '"P26"', Z, Begin2, End2, Original) & 
		~strCMP(Y, Z) & ~disjoint(Begin1, End1, Begin2, End2) ) >> conflict(X, "oneSpouseMan", Z), weight : 400.0
	// #15. Constraint #13 with exchanged arguments.
	m.add rule : ( tf(Y, '"P26"', X, Begin1, End1, Original)  &  tf(Z, '"P26"', X, Begin2, End2, Original) & 
		~strCMP(Y, Z) & ~disjoint(Begin1, End1, Begin2, End2) ) >> conflict(X, "oneSpouseWoman", Z), weight : 400.0
	// #16. A person must be born before getting married.
	m.add rule :  ( tf(X, '"P569"', Y, Begin1, End1, Original)  &  tf(X, '"P26"', Z, Begin2, End2, Original)  & 
		~before(Begin1, End1, Begin2, End2) ) >> conflict(X, "spouseBeforeBorn", Z), weight : 400.0
	// #17. A person must be alive to be married.
	m.add rule :  ( tf(X, '"P570"', Y, Begin1, End1, Original)  &  tf(X, '"P26"', Z, Begin2, End2, Original)  & 
		~before(Begin2, End2, Begin1, End1) ) >> conflict(X, "spouseAfterDeath", Z), weight : 400.0
	// #18. A person cannot be both a player and a coach at the same time.   [SOFT CONSTRAINT]
	m.add rule : ( tf(X, '"P54"', Y, Begin1, End1, Original)  &  tf(X, '"P286"', Z, Begin2, End2, Original) & 
		~strCMP(Y, Z) & ~disjoint(Begin1, End1, Begin2, End2) ) >> conflict(X, "playerAndCoach", Z), weight : 4.0
	// #19. A person cannot work for two companies at the same time.     [SOFT]
	m.add rule : ( tf(X,  '"P108"', Y, Begin1, End1, Original)  &  tf(X,  '"P108"', Z, Begin2, End2, Original) & 
		~strCMP(Y, Z) & ~disjoint(Begin1, End1, Begin2, End2) ) >> conflict(X, "multiWorker", Z), weight : 4.0
	
	
	
	def evidencePartition = new Partition(0);
	def insert = data.getInserter(tf, evidencePartition);
	
//	def dir = 'data'+java.io.File.separator+'footballdb'+java.io.File.separator;
//	InserterUtils.loadDelimitedDataTruth(insert, dir+"wikidata_psl.txt");
	InserterUtils.loadDelimitedDataTruth(insert, this.args[0]);
	
	def targetPartition = new Partition(1);
	//Database db = data.getDatabase(targetPartition, [tf] as Set, evidencePartition);
	Database db = data.getDatabase(targetPartition,  evidencePartition);
	
	
	/** 
	 *  average over 10 runs
	 *   
	 */
	long start, end, overall = 0;
	 for (int i = 0; i < 10; i++) {
		 start = System.currentTimeMillis();
		 LazyMPEInference results = new LazyMPEInference(m, db, config);
		 results.mpeInference();
		 end = System.currentTimeMillis();
		 overall += end - start;
	 }
	
	/*
	 * Let's see the results
	 */
	//println "Inference results with hand-defined weights:"
	DecimalFormat formatter = new DecimalFormat("#.##");
	/*
	for (GroundAtom atom : Queries.getAllAtoms(db, triple))
		println atom.toString() + "\t" + formatter.format(atom.getValue());
	for (GroundAtom atom : Queries.getAllAtoms(db, conflict))
		println atom.toString() + "\t" + formatter.format(atom.getValue());
	
	*/
	
	ArrayList<String> mpeState = new ArrayList<String>();
	for (GroundAtom atom : Queries.getAllAtoms(db, conflictExtended)) {
		if (Double.parseDouble(formatter.format(atom.getValue())) != 0) {
			mpeState.add(atom.toString() + "\t" + formatter.format(atom.getValue()));
		}
	}	
	for (GroundAtom atom : Queries.getAllAtoms(db, triple)) {
		if (Double.parseDouble(formatter.format(atom.getValue())) != 0) {
			mpeState.add(atom.toString() + "\t" + formatter.format(atom.getValue()));
		}
	}	
	for (GroundAtom atom : Queries.getAllAtoms(db, conflict)){
		if (Double.parseDouble(formatter.format(atom.getValue())) != 0) {
			mpeState.add(atom.toString() + "\t" + formatter.format(atom.getValue()));
		}
	}	
	/*
	try {
		Files.write(Paths.get(this.args[1]), mpeState);
		System.out.println("[SUCCESS]");
	}catch (IOException e) {
		//exception handling left as an exercise for the reader
	} */
		
	try {
		Writer writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(this.args[1]), "utf-8")); 
		for (String predicate : mpeState)
			writer.write(predicate + "\n");
		writer.close();
	} catch (IOException e) {
			System.err.println("Cannot write to file!")
	} finally {
		//writer.close();
	}
		
	//System.out.println("Average run time over 10 runs = " +  overall/10 );//(end - start));
	System.out.println(overall/10 );//(end - start));
}



// Without error tag 
def withoutErrorTag = {
	PSLModel m = new PSLModel(this, data)
	
	m.add predicate: "triple",       types: [ArgumentType.String, ArgumentType.String, ArgumentType.String]
	m.add predicate: "conflict",       types: [ArgumentType.String, ArgumentType.String, ArgumentType.String]
	m.add predicate: "wrongInterval",       types: [ArgumentType.String, ArgumentType.String, ArgumentType.String]	
	m.add predicate: "conflictExtended",       types: [ArgumentType.String, ArgumentType.String, ArgumentType.Long, ArgumentType.Long, ArgumentType.String, ArgumentType.Long, ArgumentType.Long]
	m.add predicate: "conflictEvtn",       types: [ArgumentType.Long, ArgumentType.Long]
	m.add predicate: "tf",       types: [ArgumentType.String, ArgumentType.String, ArgumentType.String, ArgumentType.Long, ArgumentType.Long]
	m.add predicate: "person",  types:  [ArgumentType.String]
	
	m.add function: "bnFiftteenNineteen" , implementation: new BNFiftteenNineteenW()  //
	m.add function: "belowTwentyOne" , implementation: new BelowTwentyOneW()  //
	m.add function: "disjoint" , implementation: new AllenDisjointW()  //
	m.add function: "belowFifty" , implementation: new BelowFiftyW()  //
	m.add function: "aboveFifty" , implementation: new AboveFiftyW()  //
	m.add function: "aboveSixteen" , implementation: new AboveSixteenOrEqual()  //
	m.add function: "superiorThan40" , implementation: new InequalityGreaterThanW()  // comparisons of the form X > 20
	m.add function: "validLifeSpan" , implementation: new ValidLifeSpanW()    //comparisons of the form X - Y  < 150
	m.add function: "notEqual" , implementation: new NotEqual()    //comparisons of the form X != Y
	m.add function: "strCMP", implementation: new CMPStringSimilarity()  // compare strings "aaa" = "bb"
	m.add function: "greaterThan", implementation: new GreaterThan()  // compare X > Y
	m.add function: "lessThan", implementation: new LessThan()  // compare X < Y
	m.add function: "overlaps", implementation: new AllenOverlaps()  // Allen's overlap
	m.add function: "before", implementation: new AllenBeforeW()  // Allen's overlap inverse
	m.add function: "checkInterval", implementation: new CheckIntervalW() // check interval
	
	
	
	// wrong intervals
	m.add rule :  ( tf(X, '"P54"', Y, Begin1, End1)   &
		lessThan(End1, Begin1) ) >> wrongInterval(X, "wrongInterval", Y), weight : 400.0
	// #1. A person cannot have two birth dates
	m.add rule :  ( tf(X, '"P569"', Y, Begin1, End1)  &  tf(X, '"P569"', Z, Begin2, End2)  &
		notEqual(Begin1, Begin2) ) >> conflict(X, "P569", Y), weight : 400.0
	// #2. A person cannot have two death dates
	m.add rule :  ( tf(X, '"P570"', Y, Begin1, End1)  &  tf(X, '"P570"', Z, Begin2, End2)  &
		notEqual(Begin1, Begin2) ) >> conflict(X, '"P570"', Y), weight : 400.0
	// #3. A person's birth date is before his/her death date
	m.add rule :  ( tf(X, '"P569"', Y, Begin1, End1)  &  tf(X, '"P570"', Z, Begin2, End2)  &
		~lessThan(Begin1, Begin2) ) >> conflict(X, "diedBFborn", Y), weight : 400.0
	m.add rule :  ( tf(X, '"P569"', Y, Begin1, End1)  &  tf(X, '"P570"', Z, Begin2, End2)  &
		~validLifeSpan(Begin1, Begin2) ) >> conflict(X, "validSpan", Y), weight : 400.0
	// #4. A person must be born before playing for a team.
	m.add rule :  ( tf(X, '"P569"', Y, Begin1, End1)  &  tf(X, '"P54"', Z, Begin2, End2)  &
		~before(Begin1, End1, Begin2, End2) ) >> conflict(X, "playsForBeforeBorn", Z), weight : 400.0
	// #5. A person must be alive to play for a team.
	m.add rule :  ( tf(X, '"P570"', Y, Begin1, End1)  &  tf(X, '"P54"', Z, Begin2, End2)  &
		~before(Begin2, End2, Begin1, End1) ) >> conflict(X, "playsForAfterDeath", Z), weight : 400.0
	// #6. A person must be atleast 16 before playing for a premier league club.  [SOFT CONSTRAINT]
	m.add rule :  ( tf(X, '"P569"', Y, Begin1, End1)  &  tf(X, '"P54"', Z, Begin2, End2)  &
		~aboveSixteen(Begin1, Begin2) ) >> conflict(X, "aboveSixteen", Z), weight : 4.0                   //conflict(X, "aboveSixteen", Z), weight : 4.0
	// #7. Someone who is older than 50 years does not play in a club.  [SOFT]
	m.add rule :  ( tf(X, '"P569"', Y, Begin1, End1)  &  tf(X, '"P54"', Z, Begin2, End2)  &
		~belowFifty(Begin1, Begin2) ) >> conflict(X, "aboveFifty", Z), weight : 4.0
	// #8. A footballer cannot play for two different clubs at the same time/period.  [correponds to Allen's DISJOINT]
	m.add rule : ( tf(X, '"P54"', Y, Begin1, End1)  &  tf(X, '"P54"', Z, Begin2, End2) &
		~strCMP(Y, Z) & ~disjoint(Begin1, End1, Begin2, End2) & checkInterval(Begin1, End1, Begin2, End2)  ) >> conflictExtended(X, Y, Begin1, End1, Z, Begin2, End2), weight : 400.0
	
	// #9. A retired player is someone, that played for a club and is older 50 years
	m.add rule : ( tf(X, '"P569"', Y, Begin1, End1)  & tf(X, '"P54"', Z, Begin2, End2) &
		 aboveFifty(Begin1) ) >> triple(X, "is", "RetiredPlayer"), weight : 4.0
	// #10. A young player in a team is someone who is at most 21 years old.
	m.add rule : ( tf(X, '"P569"', Y, Begin1, End1)  & tf(X, '"P54"', Z, Begin2, End2) &
		belowTwentyOne(Begin1) ) >> triple(X, "is", "YoungPlayer"), weight : 4.0
	// #11. A teen player is someone who is at least 15 and at most 19 years old.
	m.add rule : ( tf(X, '"P569"', Y, Begin1, End1)  & tf(X, '"P54"', Z, Begin2, End2) &
		bnFiftteenNineteen(Begin1) ) >> triple(X, "is", "TeenPlayer"), weight : 4.0
	// #12. DivorcedPerson ? [Refine this constraint]
	//Long thisYear = new Long(2016);
	//m.add rule : ( tf(X, "P26", Y, Begin1, End1)  &  ~tf(X, "P26", Z, Begin1, thisYear) & ~strCMP(Y, Z) ) >> triple(X, "is", "DivorcedPerson"), weight : 4.0
	// #13. The inverse of spouse relation holds
	m.add rule : ( tf(X, '"P26"', Y, Begin1, End1) ) >> triple(X, '"P26"', Y), weight : 4.0
	
	// #14. A person cannot be married to two distinct individuals.
	m.add rule : ( tf(X, '"P26"', Y, Begin1, End1)  &  tf(X, '"P26"', Z, Begin2, End2) &
		~strCMP(Y, Z) & ~disjoint(Begin1, End1, Begin2, End2) & checkInterval(Begin1, End1, Begin2, End2) ) >> conflict(X, "oneSpouseMan", Z), weight : 400.0
	// #15. Constraint #13 with exchanged arguments.
	m.add rule : ( tf(Y, '"P26"', X, Begin1, End1)  &  tf(Z, '"P26"', X, Begin2, End2) &
		~strCMP(Y, Z) & ~disjoint(Begin1, End1, Begin2, End2) & checkInterval(Begin1, End1, Begin2, End2) ) >> conflict(X, "oneSpouseWoman", Z), weight : 400.0
	// #16. A person must be born before getting married.
	m.add rule :  ( tf(X, '"P569"', Y, Begin1, End1)  &  tf(X, '"P26"', Z, Begin2, End2)  &
		~before(Begin1, End1, Begin2, End2) & checkInterval(Begin1, End1, Begin2, End2) ) >> conflict(X, "spouseBeforeBorn", Z), weight : 400.0
	// #17. A person must be alive to be married.
	m.add rule :  ( tf(X, '"P570"', Y, Begin1, End1)  &  tf(X, '"P26"', Z, Begin2, End2)  &
		~before(Begin2, End2, Begin1, End1) & checkInterval(Begin1, End1, Begin2, End2) ) >> conflict(X, "spouseAfterDeath", Z), weight : 400.0
	// #18. A person cannot be both a player and a coach at the same time.   [SOFT CONSTRAINT]
	m.add rule : ( tf(X, '"P54"', Y, Begin1, End1)  &  tf(X, '"P286"', Z, Begin2, End2) &
		~strCMP(Y, Z) & ~disjoint(Begin1, End1, Begin2, End2) ) >> conflict(X, "playerAndCoach", Z), weight : 4.0
	// #19. A person cannot work for two companies at the same time.     [SOFT]
	m.add rule : ( tf(X,  '"P108"', Y, Begin1, End1)  &  tf(X,  '"P108"', Z, Begin2, End2) &
		~strCMP(Y, Z) & ~disjoint(Begin1, End1, Begin2, End2) & checkInterval(Begin1, End1, Begin2, End2) ) >> conflict(X, "multiWorker", Z), weight : 4.0
	
	
	
	def evidencePartition = new Partition(0);
	def insert = data.getInserter(tf, evidencePartition);
	
//	def dir = 'data'+java.io.File.separator+'footballdb'+java.io.File.separator;
//	InserterUtils.loadDelimitedDataTruth(insert, dir+"without_tag_wikidata_psl.txt");
	
	// accept input filename
	InserterUtils.loadDelimitedDataTruth(insert, this.args[0]);
	//InserterUtils.loadDelimitedDataTruth(insert, dir+"test_psl.txt");
	//Rule test data
	/*insert.insertValue(0.6593, "Zoltan Mesko", "P54",	"A",	201001,	201201, "true");
	insert.insertValue(0.6593, "Zoltan Mesko", "P54",	"A",	201301,	201301, "true");
	insert.insertValue(0.6593, "Zoltan Mesko", "P569",	1965,	196501,	196501, "false");
	insert.insertValue(0.6593, "Zoltan Mesko2", "P569",	1965,	196501,	196501, "false");
	insert.insertValue(0.6593, "Zoltan Mesko3", "P569",	1965,	196501,	196501, "true");
	insert.insertValue(0.6593, "Zoltan Mesko3", "P569",	1969,	196901,	196501, "true");
	insert.insertValue(0.8114, "Q1000002",	"P569",	"19321204",	193212,	193212,	"true");
	insert.insertValue(0.9097, "Q1000002",	"P569",	"-19321204",	-193212,	-193212,	"true");*/
	
	/*
	def dir = 'data'+java.io.File.separator+'footballdb'+java.io.File.separator;
	
	Scanner sc=new Scanner(new FileReader(dir+"wikidata_psl.txt"));
	while (sc.hasNextLine()){
		String [] tf = sc.nextLine().split("\t");
		System.out.println(tf);
		insert.insertValue(Double.parseDouble(tf[6].trim()),tf[0].trim(), tf[1].trim(), tf[2].trim(), tf[3].trim(), tf[4].trim(), tf[5].trim());
		//System.out.println(Double.parseDouble(tf[3].trim()) + " "  +tf[0].trim()  + " "  + tf[1].trim()  + " "  + tf[2].trim());
	}
	
	*/
	def targetPartition = new Partition(1);
	//Database db = data.getDatabase(targetPartition, [tf] as Set, evidencePartition);
	Database db = data.getDatabase(targetPartition,  evidencePartition);
	
	/*
	MPEInference inferenceApp = new MPEInference(m, db, config);
	inferenceApp.mpeInference();
	inferenceApp.close();
	
	long start = System.currentTimeMillis();
	LazyMPEInference results = new LazyMPEInference(m, db, config);
	results.mpeInference();
	long end = System.currentTimeMillis();
	*/
	//println m
	
	/**
	 *  average over 10 runs
	 *
	 */
	long start, end, overall = 0;
	// for (int i = 0; i < 10; i++) {
		 start = System.currentTimeMillis();
		 LazyMPEInference results = new LazyMPEInference(m, db, config);
		 results.mpeInference();
		 end = System.currentTimeMillis();
		 overall += end - start;
	 //}
	
	/*
	 * Let's see the results
	 */
	//println "Inference results with hand-defined weights:"
	DecimalFormat formatter = new DecimalFormat("#.##");
	/*
	for (GroundAtom atom : Queries.getAllAtoms(db, triple))
		println atom.toString() + "\t" + formatter.format(atom.getValue());
	for (GroundAtom atom : Queries.getAllAtoms(db, conflict))
		println atom.toString() + "\t" + formatter.format(atom.getValue());
	
	*/
	
	ArrayList<String> mpeState = new ArrayList<String>();
//	for (GroundAtom atom : Queries.getAllAtoms(db, conflictExtended)) {
//		if (Double.parseDouble(formatter.format(atom.getValue())) > 0) {
//			mpeState.add(atom.toString() + "\t" + formatter.format(atom.getValue()));
//		}
//	}
//	for (GroundAtom atom : Queries.getAllAtoms(db, triple)) {
//		if (Double.parseDouble(formatter.format(atom.getValue())) > 0) {
//			mpeState.add(atom.toString() + "\t" + formatter.format(atom.getValue()));
//		}
//	}
	for (GroundAtom atom : Queries.getAllAtoms(db, conflict)){
		if (Double.parseDouble(formatter.format(atom.getValue())) > 0) {
			mpeState.add(atom.toString() + "\t" + formatter.format(atom.getValue()));
		}
	}
	/*
	try {
		Files.write(Paths.get(this.args[1]), mpeState);
		System.out.println("[SUCCESS]");
	}catch (IOException e) {
		//exception handling left as an exercise for the reader
	} */
		
	try {
//		Writer writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream("out.txt"), "utf-8"));
		Writer writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(this.args[1]), "utf-8"));
		for (String predicate : mpeState)
			writer.write(predicate + "\n");
		writer.close();
	} catch (IOException e) {
			System.err.println("Cannot write to file!")
	} finally {
		//writer.close();
	}
		
	//System.out.println("Average run time over 10 runs = " +  overall/10 );//(end - start));
	System.out.println(overall);//(end - start));
}


//withErrorTag()
withoutErrorTag();
