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
ConfigBundle config = cm.getBundle("mpe-yago")

/* Uses H2 as a DataStore and stores it in a temp. directory by default */
def defaultPath = System.getProperty("java.io.tmpdir")
String dbpath = config.getString("dbpath", defaultPath + File.separator + "mpe-yago")
DataStore data = new RDBMSDataStore(new H2DatabaseDriver(Type.Disk, dbpath, true), config)

/*
 * Now we can initialize a PSLModel, which is the core component of PSL.
 * The first constructor argument is the context in which the PSLModel is defined.
 * The second argument is the DataStore we will be using.
 */
PSLModel m = new PSLModel(this, data)

/*
m.add predicate: "triple",       types: [ArgumentType.String, ArgumentType.String, ArgumentType.String]
m.add predicate: "conflict",       types: [ArgumentType.String, ArgumentType.String, ArgumentType.String]
m.add predicate: "conflictExtended",       types: [ArgumentType.String, ArgumentType.String, ArgumentType.Long, ArgumentType.Long, ArgumentType.String, ArgumentType.Long, ArgumentType.Long]
m.add predicate: "conflictEvtn",       types: [ArgumentType.Long, ArgumentType.Long]
m.add predicate: "tf",       types: [ArgumentType.String, ArgumentType.String, ArgumentType.String, ArgumentType.Long, ArgumentType.Long]
m.add predicate: "person",  types:  [ArgumentType.String]
*/


m.add predicate: "triple",       types: [ArgumentType.String, ArgumentType.String, ArgumentType.String]
/*
// #1. A person cannot have two birth dates
m.add rule :  ( tf(X, "born", Y, Begin1, End1)  &  tf(X, "born", Z, Begin2, End2)  & notEqual(Begin1, Begin2) ) >> conflict(X, "born", Y), weight : 400.0
// #2. A person cannot have two death dates
m.add rule :  ( tf(X, "died", Y, Begin1, End1)  &  tf(X, "died", Z, Begin2, End2)  & notEqual(Begin1, Begin2) ) >> conflict(X, "died", Y), weight : 400.0
*/

m.add rule : ( triple(X, "hasWikipediaAnchorText", Y) ) >> triple(X, "hasTitleText", Y) 
m.add rule : ( triple(X, "hasImport", Y) ) >> triple(X, "hasGDP", Y) 
m.add rule : ( triple(X, "linksTo", Y) ) >> triple(X, "objectEndRelation", Y) 
m.add rule : ( triple(X, "hasWikipediaAnchorText", Y) ) >> triple(X, "hasWikipediaCategory", Y) 
m.add rule : ( triple(X, "playsFor", Y) ) >> triple(X, "worksAt", Y) 
m.add rule : ( triple(X, "wasBornOnDate", Y) ) >> triple(X, "endsExistingOnDate", Y) 
m.add rule : ( triple(X, "objectStartRelation", Y) ) >> triple(X, "permanentRelationToSubject", Y) 
m.add rule : ( triple(X, "relationLocatedByObject", Y) ) >> triple(X, "objectEndRelation", Y) 
m.add rule : ( triple(X, "occursUntil", Y) ) >> triple(X, "occursSince", Y) 
m.add rule : ( triple(X, "placedIn", Y) ) >> triple(X, "objectEndRelation", Y) 
m.add rule : ( triple(X, "wasCreatedOnDate", Y) ) >> triple(X, "endsExistingOnDate", Y) 
m.add rule : ( triple(X, "wroteMusicFor", Y) ) >> triple(X, "edited", Y) 
m.add rule : ( triple(X, "endsExistingOnDate", Y) ) >> triple(X, "diedOnDate", Y) 
m.add rule : ( triple(X, "permanentRelationToSubject", Y) ) >> triple(X, "isInterestedIn", Y) 
m.add rule : ( triple(X, "placedIn", Y) ) >> triple(X, "hasNeighbor", Y) 
m.add rule : ( triple(X, "hasCitationTitle", Y) ) >> triple(X, "hasWikipediaAbstract", Y) 
m.add rule : ( triple(X, "hasGloss", Y) ) >> triple(X, "hasFamilyName", Y) 
m.add rule : ( triple(X, "objectStartRelation", Y) ) >> triple(X, "isInterestedIn", Y) 
m.add rule : ( triple(X, "linksTo", Y) ) >> triple(X, "permanentRelationToObject", Y) 
m.add rule : ( triple(X, "isMarriedTo", Y) ) >> triple(X, "isInterestedIn", Y) 
m.add rule : ( triple(X, "isInterestedIn", Y) ) >> triple(X, "permanentRelationToSubject", Y) 
m.add rule : ( triple(X, "influences", Y) ) >> triple(X, "holdsPoliticalPosition", Y) 
m.add rule : ( triple(X, "hasCitationTitle", Y) ) >> triple(X, "hasWikipediaAnchorText", Y) 
m.add rule : ( triple(X, "hasAirportCode", Y) ) >> triple(X, "hasGloss", Y) 
m.add rule : ( triple(X, "wroteMusicFor", Y) ) >> triple(X, "actedIn", Y) 
m.add rule : ( triple(X, "worksAt", Y) ) >> triple(X, "graduatedFrom", Y) 
m.add rule : ( triple(X, "hasContext", Y) ) >> triple(X, "hasAirportCode", Y) 
m.add rule : ( triple(X, "placedIn", Y) ) >> triple(X, "permanentRelationToObject", Y) 
m.add rule : ( triple(X, "relationLocatedByObject", Y) ) >> triple(X, "permanentRelationToObject", Y) 
m.add rule : ( triple(X, "hasContextPrecedingAnchor", Y) ) >> triple(X, "extractionTechnique", Y) 
m.add rule : ( triple(X, "isInterestedIn", Y) ) >> triple(X, "wasBornIn", Y) 
m.add rule : ( triple(X, "isInterestedIn", Y) ) >> triple(X, "objectStartRelation", Y) 
m.add rule : ( triple(X, "hasWikipediaArticleLength", Y) ) >> triple(X, "hasNumberOfWikipediaLinks", Y) 
m.add rule : ( triple(X, "objectEndRelation", Y) ) >> triple(X, "subjectEndRelation", Y) 
m.add rule : ( triple(X, "relationLocatedByObject", Y) ) >> triple(X, "hasCurrency", Y) 
m.add rule : ( triple(X, "hasTitleText", Y) ) >> triple(X, "hasCitationTitle", Y) 
m.add rule : ( triple(X, "placedIn", Y) ) >> triple(X, "hasCurrency", Y) 
m.add rule : ( triple(X, "hasWikipediaCategory", Y) ) >> triple(X, "hasCitationTitle", Y) 
m.add rule : ( triple(X, "wasCreatedOnDate", Y) ) >> triple(X, "diedOnDate", Y) 
m.add rule : ( triple(X, "isInterestedIn", Y) ) >> triple(X, "graduatedFrom", Y) 
m.add rule : ( triple(X, "permanentRelationToObject", Y) ) >> triple(X, "subjectEndRelation", Y) 
m.add rule : ( triple(X, "subjectEndRelation", Y) ) >> triple(X, "linksTo", Y) 
m.add rule : ( triple(X, "actedIn", Y) ) >> triple(X, "directed", Y) 
m.add rule : ( triple(X, "wasBornOnDate", Y) ) >> triple(X, "diedOnDate", Y) 
m.add rule : ( triple(X, "permanentRelationToSubject", Y) ) >> triple(X, "objectStartRelation", Y) 
m.add rule : ( triple(X, "participatedIn", Y) ) >> triple(X, "owns", Y) 
m.add rule : ( triple(X, "directed", Y) ) >> triple(X, "edited", Y) 
m.add rule : ( triple(X, "hasCurrency", Y) ) >> triple(X, "dealsWith", Y) 
m.add rule : ( triple(X, "linksTo", Y) ) >> triple(X, "hasCurrency", Y) 
m.add rule : ( triple(X, "startsExistingOnDate", Y) ) >> triple(X, "wasBornOnDate", Y) 
m.add rule : ( triple(X, "hasContextSucceedingAnchor", Y) ) >> triple(X, "hasAnchorText", Y) 
m.add rule : ( triple(X, "worksAt", Y) ) >> triple(X, "isInterestedIn", Y) 
m.add rule : ( triple(X, "linksTo", Y) ) >> triple(X, "subjectStartRelation", Y) 
m.add rule : ( triple(X, "hasFamilyName", Y) ) >> triple(X, "hasGivenName", Y) 
m.add rule : ( triple(X, "hasCurrency", Y) ) >> triple(X, "subjectEndRelation", Y) 
m.add rule : ( triple(X, "isInterestedIn", Y) ) >> triple(X, "isMarriedTo", Y) 
m.add rule : ( triple(X, "startedOnDate", Y) ) >> triple(X, "endedOnDate", Y) 
m.add rule : ( triple(X, "startsExistingOnDate", Y) ) >> triple(X, "wasCreatedOnDate", Y) 
m.add rule : ( triple(X, "relationLocatedByObject", Y) ) >> triple(X, "subjectStartRelation", Y) 
m.add rule : ( triple(X, "directed", Y) ) >> triple(X, "actedIn", Y) 
m.add rule : ( triple(X, "placedIn", Y) ) >> triple(X, "subjectStartRelation", Y) 
m.add rule : ( triple(X, "subjectEndRelation", Y) ) >> triple(X, "placedIn", Y) 
m.add rule : ( triple(X, "subjectStartRelation", Y) ) >> triple(X, "subjectEndRelation", Y) 
m.add rule : ( triple(X, "diedOnDate", Y) ) >> triple(X, "endsExistingOnDate", Y) 
m.add rule : ( triple(X, "subjectEndRelation", Y) ) >> triple(X, "relationLocatedByObject", Y) 
m.add rule : ( triple(X, "isInterestedIn", Y) ) >> triple(X, "diedIn", Y) 
m.add rule : ( triple(X, "hasGloss", Y) ) >> triple(X, "hasContext", Y) 
m.add rule : ( triple(X, "hasGDP", Y) ) >> triple(X, "hasImport", Y) 
m.add rule : ( triple(X, "isInterestedIn", Y) ) >> triple(X, "subjectEndRelation", Y) 
m.add rule : ( triple(X, "wasCreatedOnDate", Y) ) >> triple(X, "wasBornOnDate", Y) 
m.add rule : ( triple(X, "objectEndRelation", Y) ) >> triple(X, "objectStartRelation", Y) 
m.add rule : ( triple(X, "hasAnchorText", Y) ) >> triple(X, "hasContextSucceedingAnchor", Y) 
m.add rule : ( triple(X, "isInterestedIn", Y) ) >> triple(X, "livesIn", Y) 
m.add rule : ( triple(X, "placedIn", Y) ) >> triple(X, "linksTo", Y) 
m.add rule : ( triple(X, "wasDestroyedOnDate", Y) ) >> triple(X, "startsExistingOnDate", Y) 
m.add rule : ( triple(X, "wasBornOnDate", Y) ) >> triple(X, "wasCreatedOnDate", Y) 
m.add rule : ( triple(X, "hasContextSucceedingAnchor", Y) ) >> triple(X, "hasContextPrecedingAnchor", Y) 
m.add rule : ( triple(X, "hasAcademicAdvisor", Y) ) >> triple(X, "isInterestedIn", Y) 
m.add rule : ( triple(X, "dealsWith", Y) ) >> triple(X, "hasCurrency", Y) 
m.add rule : ( triple(X, "relationLocatedByObject", Y) ) >> triple(X, "linksTo", Y) 
m.add rule : ( triple(X, "influences", Y) ) >> triple(X, "hasChild", Y) 
m.add rule : ( triple(X, "permanentRelationToObject", Y) ) >> triple(X, "objectStartRelation", Y) 
m.add rule : ( triple(X, "hasEconomicGrowth", Y) ) >> triple(X, "hasPoverty", Y) 
m.add rule : ( triple(X, "playsFor", Y) ) >> triple(X, "graduatedFrom", Y) 
m.add rule : ( triple(X, "subjectEndRelation", Y) ) >> triple(X, "hasCurrency", Y) 
m.add rule : ( triple(X, "endedOnDate", Y) ) >> triple(X, "happenedOnDate", Y) 
m.add rule : ( triple(X, "hasExport", Y) ) >> triple(X, "hasImport", Y) 
m.add rule : ( triple(X, "permanentRelationToSubject", Y) ) >> triple(X, "subjectEndRelation", Y) 
m.add rule : ( triple(X, "hasChild", Y) ) >> triple(X, "isInterestedIn", Y) 
m.add rule : ( triple(X, "hasThreeLetterLanguageCode", Y) ) >> triple(X, "hasLanguageCode", Y) 
m.add rule : ( triple(X, "hasWikipediaAnchorText", Y) ) >> triple(X, "hasCitationTitle", Y) 
m.add rule : ( triple(X, "placedIn", Y) ) >> triple(X, "relationLocatedByObject", Y) 
m.add rule : ( triple(X, "hasInflation", Y) ) >> triple(X, "hasUnemployment", Y) 
m.add rule : ( triple(X, "objectStartRelation", Y) ) >> triple(X, "subjectEndRelation", Y) 
m.add rule : ( triple(X, "graduatedFrom", Y) ) >> triple(X, "playsFor", Y) 
m.add rule : ( triple(X, "hasWikipediaAbstract", Y) ) >> triple(X, "hasWikipediaAnchorText", Y) 
m.add rule : ( triple(X, "hasCapital", Y) ) >> triple(X, "dealsWith", Y) 
m.add rule : ( triple(X, "holdsPoliticalPosition", Y) ) >> triple(X, "isMarriedTo", Y) 
m.add rule : ( triple(X, "hasCurrency", Y) ) >> triple(X, "objectStartRelation", Y) 
m.add rule : ( triple(X, "occursSince", Y) ) >> triple(X, "occursUntil", Y) 
m.add rule : ( triple(X, "relationLocatedByObject", Y) ) >> triple(X, "placedIn", Y) 
m.add rule : ( triple(X, "subjectEndRelation", Y) ) >> triple(X, "subjectStartRelation", Y) 
m.add rule : ( triple(X, "hasEconomicGrowth", Y) ) >> triple(X, "hasInflation", Y) 
m.add rule : ( triple(X, "exports", Y) ) >> triple(X, "imports", Y) 
m.add rule : ( triple(X, "linksTo", Y) ) >> triple(X, "relationLocatedByObject", Y) 
m.add rule : ( triple(X, "startsExistingOnDate", Y) ) >> triple(X, "diedOnDate", Y) 
m.add rule : ( triple(X, "subjectStartRelation", Y) ) >> triple(X, "objectStartRelation", Y) 
m.add rule : ( triple(X, "hasGivenName", Y) ) >> triple(X, "hasContext", Y) 
m.add rule : ( triple(X, "influences", Y) ) >> triple(X, "hasAcademicAdvisor", Y) 
m.add rule : ( triple(X, "hasPoverty", Y) ) >> triple(X, "hasUnemployment", Y) 
m.add rule : ( triple(X, "linksTo", Y) ) >> triple(X, "placedIn", Y) 
m.add rule : ( triple(X, "playsFor", Y) ) >> triple(X, "isInterestedIn", Y) 
m.add rule : ( triple(X, "objectEndRelation", Y) ) >> triple(X, "isInterestedIn", Y) 
m.add rule : ( triple(X, "placedIn", Y) ) >> triple(X, "isConnectedTo", Y) 
m.add rule : ( triple(X, "endsExistingOnDate", Y) ) >> triple(X, "wasCreatedOnDate", Y) 
m.add rule : ( triple(X, "hasExport", Y) ) >> triple(X, "hasGDP", Y) 
m.add rule : ( triple(X, "subjectStartRelation", Y) ) >> triple(X, "permanentRelationToSubject", Y) 
m.add rule : ( triple(X, "isInterestedIn", Y) ) >> triple(X, "worksAt", Y) 
m.add rule : ( triple(X, "holdsPoliticalPosition", Y) ) >> triple(X, "isInterestedIn", Y) 
m.add rule : ( triple(X, "hasCurrency", Y) ) >> triple(X, "hasCapital", Y) 
m.add rule : ( triple(X, "subjectEndRelation", Y) ) >> triple(X, "objectEndRelation", Y) 
m.add rule : ( triple(X, "hasFamilyName", Y) ) >> triple(X, "hasContext", Y) 
m.add rule : ( triple(X, "endsExistingOnDate", Y) ) >> triple(X, "wasBornOnDate", Y) 
m.add rule : ( triple(X, "actedIn", Y) ) >> triple(X, "wroteMusicFor", Y) 
m.add rule : ( triple(X, "isInterestedIn", Y) ) >> triple(X, "isCitizenOf", Y) 
m.add rule : ( triple(X, "hasAnchorText", Y) ) >> triple(X, "hasContextPrecedingAnchor", Y) 
m.add rule : ( triple(X, "hasWikipediaCategory", Y) ) >> triple(X, "hasTitleText", Y) 
m.add rule : ( triple(X, "hasGloss", Y) ) >> triple(X, "hasGivenName", Y) 
m.add rule : ( triple(X, "permanentRelationToObject", Y) ) >> triple(X, "isInterestedIn", Y) 
m.add rule : ( triple(X, "hasContext", Y) ) >> triple(X, "hasGloss", Y) 
m.add rule : ( triple(X, "hasCurrency", Y) ) >> triple(X, "permanentRelationToSubject", Y) 
m.add rule : ( triple(X, "hasTitleText", Y) ) >> triple(X, "hasWikipediaCategory", Y) 
m.add rule : ( triple(X, "hasAcademicAdvisor", Y) ) >> triple(X, "isMarriedTo", Y) 
m.add rule : ( triple(X, "permanentRelationToObject", Y) ) >> triple(X, "permanentRelationToSubject", Y) 
m.add rule : ( triple(X, "subjectEndRelation", Y) ) >> triple(X, "permanentRelationToObject", Y) 
m.add rule : ( triple(X, "diedOnDate", Y) ) >> triple(X, "wasCreatedOnDate", Y) 
m.add rule : ( triple(X, "endedOnDate", Y) ) >> triple(X, "startedOnDate", Y) 
m.add rule : ( triple(X, "startedOnDate", Y) ) >> triple(X, "happenedOnDate", Y) 
m.add rule : ( triple(X, "hasRevenue", Y) ) >> triple(X, "hasBudget", Y) 
m.add rule : ( triple(X, "diedOnDate", Y) ) >> triple(X, "wasBornOnDate", Y) 
m.add rule : ( triple(X, "hasGivenName", Y) ) >> triple(X, "hasFamilyName", Y) 
m.add rule : ( triple(X, "subjectStartRelation", Y) ) >> triple(X, "isInterestedIn", Y) 
m.add rule : ( triple(X, "hasLatitude", Y) ) >> triple(X, "hasLongitude", Y) 
m.add rule : ( triple(X, "startsExistingOnDate", Y) ) >> triple(X, "endsExistingOnDate", Y) 
m.add rule : ( triple(X, "hasRevenue", Y) ) >> triple(X, "hasExpenses", Y) 
m.add rule : ( triple(X, "hasChild", Y) ) >> triple(X, "isMarriedTo", Y) 
m.add rule : ( triple(X, "objectEndRelation", Y) ) >> triple(X, "permanentRelationToSubject", Y) 
m.add rule : ( triple(X, "subjectEndRelation", Y) ) >> triple(X, "objectStartRelation", Y) 
m.add rule : ( triple(X, "hasChild", Y) ) >> triple(X, "holdsPoliticalPosition", Y) 
m.add rule : ( triple(X, "hasWikipediaAbstract", Y) ) >> triple(X, "hasWikipediaCategory", Y) 
m.add rule : ( triple(X, "hasUnemployment", Y) ) >> triple(X, "hasInflation", Y) 
m.add rule : ( triple(X, "permanentRelationToObject", Y) ) >> triple(X, "hasCurrency", Y) 
m.add rule : ( triple(X, "endsExistingOnDate", Y) ) >> triple(X, "startsExistingOnDate", Y) 
m.add rule : ( triple(X, "hasCurrency", Y) ) >> triple(X, "subjectStartRelation", Y) 
m.add rule : ( triple(X, "linksTo", Y) ) >> triple(X, "subjectEndRelation", Y) 
m.add rule : ( triple(X, "hasWikipediaAbstract", Y) ) >> triple(X, "hasTitleText", Y) 
m.add rule : ( triple(X, "permanentRelationToSubject", Y) ) >> triple(X, "linksTo", Y) 
m.add rule : ( triple(X, "wasBornOnDate", Y) ) >> triple(X, "wasDestroyedOnDate", Y) 
m.add rule : ( triple(X, "hasNeighbor", Y) ) >> triple(X, "hasCurrency", Y) 
m.add rule : ( triple(X, "wasCreatedOnDate", Y) ) >> triple(X, "wasDestroyedOnDate", Y) 
m.add rule : ( triple(X, "isMarriedTo", Y) ) >> triple(X, "hasChild", Y) 
m.add rule : ( triple(X, "hasAirportCode", Y) ) >> triple(X, "hasContext", Y) 
m.add rule : ( triple(X, "isInterestedIn", Y) ) >> triple(X, "linksTo", Y) 
m.add rule : ( triple(X, "isInterestedIn", Y) ) >> triple(X, "hasAcademicAdvisor", Y) 
m.add rule : ( triple(X, "objectEndRelation", Y) ) >> triple(X, "hasCurrency", Y) 
m.add rule : ( triple(X, "isConnectedTo", Y) ) >> triple(X, "hasNeighbor", Y) 
m.add rule : ( triple(X, "hasContextSucceedingAnchor", Y) ) >> triple(X, "extractionTechnique", Y) 
m.add rule : ( triple(X, "objectStartRelation", Y) ) >> triple(X, "placedIn", Y) 
m.add rule : ( triple(X, "hasAcademicAdvisor", Y) ) >> triple(X, "holdsPoliticalPosition", Y) 
m.add rule : ( triple(X, "objectStartRelation", Y) ) >> triple(X, "relationLocatedByObject", Y) 
m.add rule : ( triple(X, "placedIn", Y) ) >> triple(X, "subjectEndRelation", Y) 
m.add rule : ( triple(X, "relationLocatedByObject", Y) ) >> triple(X, "subjectEndRelation", Y) 
m.add rule : ( triple(X, "subjectStartRelation", Y) ) >> triple(X, "hasCurrency", Y) 
m.add rule : ( triple(X, "permanentRelationToSubject", Y) ) >> triple(X, "relationLocatedByObject", Y) 
m.add rule : ( triple(X, "isMarriedTo", Y) ) >> triple(X, "hasAcademicAdvisor", Y) 
m.add rule : ( triple(X, "diedOnDate", Y) ) >> triple(X, "startsExistingOnDate", Y) 
m.add rule : ( triple(X, "wasBornIn", Y) ) >> triple(X, "isInterestedIn", Y) 
m.add rule : ( triple(X, "objectEndRelation", Y) ) >> triple(X, "subjectStartRelation", Y) 
m.add rule : ( triple(X, "graduatedFrom", Y) ) >> triple(X, "isInterestedIn", Y) 
m.add rule : ( triple(X, "permanentRelationToSubject", Y) ) >> triple(X, "placedIn", Y) 
m.add rule : ( triple(X, "directed", Y) ) >> triple(X, "wroteMusicFor", Y) 
m.add rule : ( triple(X, "objectStartRelation", Y) ) >> triple(X, "linksTo", Y) 
m.add rule : ( triple(X, "isInterestedIn", Y) ) >> triple(X, "placedIn", Y) 
m.add rule : ( triple(X, "isInterestedIn", Y) ) >> triple(X, "relationLocatedByObject", Y) 
m.add rule : ( triple(X, "isInterestedIn", Y) ) >> triple(X, "hasChild", Y) 
m.add rule : ( triple(X, "hasUnemployment", Y) ) >> triple(X, "hasPoverty", Y) 
m.add rule : ( triple(X, "diedIn", Y) ) >> triple(X, "isInterestedIn", Y) 
m.add rule : ( triple(X, "hasGeonamesClassId", Y) ) >> triple(X, "hasGeonamesEntityId", Y) 
m.add rule : ( triple(X, "extractionTechnique", Y) ) >> triple(X, "hasContextSucceedingAnchor", Y) 
m.add rule : ( triple(X, "permanentRelationToObject", Y) ) >> triple(X, "subjectStartRelation", Y) 
m.add rule : ( triple(X, "endsExistingOnDate", Y) ) >> triple(X, "wasDestroyedOnDate", Y) 
m.add rule : ( triple(X, "wasDestroyedOnDate", Y) ) >> triple(X, "wasBornOnDate", Y) 
m.add rule : ( triple(X, "livesIn", Y) ) >> triple(X, "isInterestedIn", Y) 
m.add rule : ( triple(X, "subjectEndRelation", Y) ) >> triple(X, "isInterestedIn", Y) 
m.add rule : ( triple(X, "edited", Y) ) >> triple(X, "directed", Y) 
m.add rule : ( triple(X, "hasAnchorText", Y) ) >> triple(X, "extractionTechnique", Y) 
m.add rule : ( triple(X, "hasContext", Y) ) >> triple(X, "hasGivenName", Y) 
m.add rule : ( triple(X, "hasCurrency", Y) ) >> triple(X, "permanentRelationToObject", Y) 
m.add rule : ( triple(X, "hasContextPrecedingAnchor", Y) ) >> triple(X, "hasAnchorText", Y) 
m.add rule : ( triple(X, "wasCreatedOnDate", Y) ) >> triple(X, "startsExistingOnDate", Y) 
m.add rule : ( triple(X, "wasDestroyedOnDate", Y) ) >> triple(X, "wasCreatedOnDate", Y) 
m.add rule : ( triple(X, "wasBornOnDate", Y) ) >> triple(X, "startsExistingOnDate", Y) 
m.add rule : ( triple(X, "hasLongitude", Y) ) >> triple(X, "hasLatitude", Y) 
m.add rule : ( triple(X, "happenedOnDate", Y) ) >> triple(X, "endedOnDate", Y) 
m.add rule : ( triple(X, "isInterestedIn", Y) ) >> triple(X, "influences", Y) 
m.add rule : ( triple(X, "hasCurrency", Y) ) >> triple(X, "hasNeighbor", Y) 
m.add rule : ( triple(X, "subjectStartRelation", Y) ) >> triple(X, "permanentRelationToObject", Y) 
m.add rule : ( triple(X, "hasPredecessor", Y) ) >> triple(X, "hasSuccessor", Y) 
m.add rule : ( triple(X, "wasBornIn", Y) ) >> triple(X, "diedIn", Y) 
m.add rule : ( triple(X, "permanentRelationToObject", Y) ) >> triple(X, "objectEndRelation", Y) 
m.add rule : ( triple(X, "diedOnDate", Y) ) >> triple(X, "wasDestroyedOnDate", Y) 
m.add rule : ( triple(X, "isMarriedTo", Y) ) >> triple(X, "influences", Y) 
m.add rule : ( triple(X, "hasCurrency", Y) ) >> triple(X, "objectEndRelation", Y) 
m.add rule : ( triple(X, "hasUnemployment", Y) ) >> triple(X, "hasEconomicGrowth", Y) 
m.add rule : ( triple(X, "objectEndRelation", Y) ) >> triple(X, "permanentRelationToObject", Y) 
m.add rule : ( triple(X, "hasTitleText", Y) ) >> triple(X, "hasWikipediaAbstract", Y) 
m.add rule : ( triple(X, "extractionTechnique", Y) ) >> triple(X, "hasContextPrecedingAnchor", Y) 
m.add rule : ( triple(X, "hasWikipediaCategory", Y) ) >> triple(X, "hasWikipediaAbstract", Y) 
m.add rule : ( triple(X, "isCitizenOf", Y) ) >> triple(X, "isInterestedIn", Y) 
m.add rule : ( triple(X, "subjectEndRelation", Y) ) >> triple(X, "permanentRelationToSubject", Y) 
m.add rule : ( triple(X, "subjectStartRelation", Y) ) >> triple(X, "objectEndRelation", Y) 
m.add rule : ( triple(X, "hasNumberOfWikipediaLinks", Y) ) >> triple(X, "hasWikipediaArticleLength", Y) 
m.add rule : ( triple(X, "isConnectedTo", Y) ) >> triple(X, "hasCurrency", Y) 
m.add rule : ( triple(X, "hasTitleText", Y) ) >> triple(X, "hasWikipediaAnchorText", Y) 
m.add rule : ( triple(X, "dealsWith", Y) ) >> triple(X, "hasCapital", Y) 
m.add rule : ( triple(X, "diedIn", Y) ) >> triple(X, "wasBornIn", Y) 
m.add rule : ( triple(X, "hasWikipediaCategory", Y) ) >> triple(X, "hasWikipediaAnchorText", Y) 
m.add rule : ( triple(X, "hasPoverty", Y) ) >> triple(X, "hasInflation", Y) 
m.add rule : ( triple(X, "objectStartRelation", Y) ) >> triple(X, "permanentRelationToObject", Y) 
m.add rule : ( triple(X, "hasGeonamesEntityId", Y) ) >> triple(X, "hasGeonamesClassId", Y) 
m.add rule : ( triple(X, "hasNeighbor", Y) ) >> triple(X, "isConnectedTo", Y) 
m.add rule : ( triple(X, "extractionTechnique", Y) ) >> triple(X, "hasAnchorText", Y) 
m.add rule : ( triple(X, "hasChild", Y) ) >> triple(X, "hasAcademicAdvisor", Y) 
m.add rule : ( triple(X, "hasGivenName", Y) ) >> triple(X, "hasGloss", Y) 
m.add rule : ( triple(X, "hasLanguageCode", Y) ) >> triple(X, "hasThreeLetterLanguageCode", Y) 
m.add rule : ( triple(X, "isInterestedIn", Y) ) >> triple(X, "objectEndRelation", Y) 
m.add rule : ( triple(X, "linksTo", Y) ) >> triple(X, "isInterestedIn", Y) 
m.add rule : ( triple(X, "isInterestedIn", Y) ) >> triple(X, "playsFor", Y) 
m.add rule : ( triple(X, "wasDestroyedOnDate", Y) ) >> triple(X, "diedOnDate", Y) 
m.add rule : ( triple(X, "hasEconomicGrowth", Y) ) >> triple(X, "hasUnemployment", Y) 
m.add rule : ( triple(X, "hasCitationTitle", Y) ) >> triple(X, "hasWikipediaCategory", Y) 
m.add rule : ( triple(X, "hasWikipediaAbstract", Y) ) >> triple(X, "hasCitationTitle", Y) 
m.add rule : ( triple(X, "permanentRelationToSubject", Y) ) >> triple(X, "objectEndRelation", Y) 
m.add rule : ( triple(X, "hasCitationTitle", Y) ) >> triple(X, "hasTitleText", Y) 
m.add rule : ( triple(X, "holdsPoliticalPosition", Y) ) >> triple(X, "influences", Y) 
m.add rule : ( triple(X, "isConnectedTo", Y) ) >> triple(X, "placedIn", Y) 
m.add rule : ( triple(X, "relationLocatedByObject", Y) ) >> triple(X, "isInterestedIn", Y) 
m.add rule : ( triple(X, "isInterestedIn", Y) ) >> triple(X, "holdsPoliticalPosition", Y) 
m.add rule : ( triple(X, "placedIn", Y) ) >> triple(X, "isInterestedIn", Y) 
m.add rule : ( triple(X, "isInterestedIn", Y) ) >> triple(X, "permanentRelationToObject", Y) 
m.add rule : ( triple(X, "hasGloss", Y) ) >> triple(X, "hasAirportCode", Y) 
m.add rule : ( triple(X, "hasInflation", Y) ) >> triple(X, "hasPoverty", Y) 
m.add rule : ( triple(X, "happenedOnDate", Y) ) >> triple(X, "startedOnDate", Y) 
m.add rule : ( triple(X, "hasSuccessor", Y) ) >> triple(X, "hasPredecessor", Y) 
m.add rule : ( triple(X, "isMarriedTo", Y) ) >> triple(X, "holdsPoliticalPosition", Y) 
m.add rule : ( triple(X, "placedIn", Y) ) >> triple(X, "permanentRelationToSubject", Y) 
m.add rule : ( triple(X, "relationLocatedByObject", Y) ) >> triple(X, "permanentRelationToSubject", Y) 
m.add rule : ( triple(X, "influences", Y) ) >> triple(X, "isInterestedIn", Y) 
m.add rule : ( triple(X, "objectStartRelation", Y) ) >> triple(X, "objectEndRelation", Y) 
m.add rule : ( triple(X, "hasCurrency", Y) ) >> triple(X, "isConnectedTo", Y) 
m.add rule : ( triple(X, "hasAcademicAdvisor", Y) ) >> triple(X, "hasChild", Y) 
m.add rule : ( triple(X, "permanentRelationToSubject", Y) ) >> triple(X, "permanentRelationToObject", Y) 
m.add rule : ( triple(X, "linksTo", Y) ) >> triple(X, "permanentRelationToSubject", Y) 
m.add rule : ( triple(X, "created", Y) ) >> triple(X, "isKnownFor", Y) 
m.add rule : ( triple(X, "hasContext", Y) ) >> triple(X, "hasFamilyName", Y) 
m.add rule : ( triple(X, "hasWikipediaAnchorText", Y) ) >> triple(X, "hasWikipediaAbstract", Y) 
m.add rule : ( triple(X, "subjectStartRelation", Y) ) >> triple(X, "placedIn", Y) 
m.add rule : ( triple(X, "permanentRelationToObject", Y) ) >> triple(X, "linksTo", Y) 
m.add rule : ( triple(X, "influences", Y) ) >> triple(X, "isMarriedTo", Y) 
m.add rule : ( triple(X, "hasCapital", Y) ) >> triple(X, "hasCurrency", Y) 
m.add rule : ( triple(X, "edited", Y) ) >> triple(X, "wroteMusicFor", Y) 
m.add rule : ( triple(X, "hasChild", Y) ) >> triple(X, "influences", Y) 
m.add rule : ( triple(X, "linksTo", Y) ) >> triple(X, "objectStartRelation", Y) 
m.add rule : ( triple(X, "objectStartRelation", Y) ) >> triple(X, "subjectStartRelation", Y) 
m.add rule : ( triple(X, "permanentRelationToSubject", Y) ) >> triple(X, "hasCurrency", Y) 
m.add rule : ( triple(X, "subjectStartRelation", Y) ) >> triple(X, "relationLocatedByObject", Y) 
m.add rule : ( triple(X, "hasImport", Y) ) >> triple(X, "hasExport", Y) 
m.add rule : ( triple(X, "hasCurrency", Y) ) >> triple(X, "relationLocatedByObject", Y) 
m.add rule : ( triple(X, "isKnownFor", Y) ) >> triple(X, "created", Y) 
m.add rule : ( triple(X, "objectEndRelation", Y) ) >> triple(X, "linksTo", Y) 
m.add rule : ( triple(X, "wasDestroyedOnDate", Y) ) >> triple(X, "endsExistingOnDate", Y) 
m.add rule : ( triple(X, "placedIn", Y) ) >> triple(X, "objectStartRelation", Y) 
m.add rule : ( triple(X, "imports", Y) ) >> triple(X, "exports", Y) 
m.add rule : ( triple(X, "hasCurrency", Y) ) >> triple(X, "placedIn", Y) 
m.add rule : ( triple(X, "holdsPoliticalPosition", Y) ) >> triple(X, "hasAcademicAdvisor", Y) 
m.add rule : ( triple(X, "relationLocatedByObject", Y) ) >> triple(X, "objectStartRelation", Y) 
m.add rule : ( triple(X, "hasAcademicAdvisor", Y) ) >> triple(X, "influences", Y) 
m.add rule : ( triple(X, "owns", Y) ) >> triple(X, "participatedIn", Y) 
m.add rule : ( triple(X, "hasExpenses", Y) ) >> triple(X, "hasRevenue", Y) 
m.add rule : ( triple(X, "hasExpenses", Y) ) >> triple(X, "hasBudget", Y) 
m.add rule : ( triple(X, "graduatedFrom", Y) ) >> triple(X, "worksAt", Y) 
m.add rule : ( triple(X, "subjectStartRelation", Y) ) >> triple(X, "linksTo", Y) 
m.add rule : ( triple(X, "hasPoverty", Y) ) >> triple(X, "hasEconomicGrowth", Y) 
m.add rule : ( triple(X, "permanentRelationToObject", Y) ) >> triple(X, "placedIn", Y) 
m.add rule : ( triple(X, "hasNeighbor", Y) ) >> triple(X, "placedIn", Y) 
m.add rule : ( triple(X, "worksAt", Y) ) >> triple(X, "playsFor", Y) 
m.add rule : ( triple(X, "permanentRelationToObject", Y) ) >> triple(X, "relationLocatedByObject", Y) 
m.add rule : ( triple(X, "hasBudget", Y) ) >> triple(X, "hasRevenue", Y) 
m.add rule : ( triple(X, "isInterestedIn", Y) ) >> triple(X, "subjectStartRelation", Y) 
m.add rule : ( triple(X, "startsExistingOnDate", Y) ) >> triple(X, "wasDestroyedOnDate", Y) 
m.add rule : ( triple(X, "wroteMusicFor", Y) ) >> triple(X, "directed", Y) 
m.add rule : ( triple(X, "objectEndRelation", Y) ) >> triple(X, "placedIn", Y) 
m.add rule : ( triple(X, "hasContextPrecedingAnchor", Y) ) >> triple(X, "hasContextSucceedingAnchor", Y) 
m.add rule : ( triple(X, "objectStartRelation", Y) ) >> triple(X, "hasCurrency", Y) 
m.add rule : ( triple(X, "holdsPoliticalPosition", Y) ) >> triple(X, "hasChild", Y) 
m.add rule : ( triple(X, "permanentRelationToSubject", Y) ) >> triple(X, "subjectStartRelation", Y) 
m.add rule : ( triple(X, "hasBudget", Y) ) >> triple(X, "hasExpenses", Y) 
m.add rule : ( triple(X, "hasFamilyName", Y) ) >> triple(X, "hasGloss", Y) 
m.add rule : ( triple(X, "hasCurrency", Y) ) >> triple(X, "linksTo", Y) 
m.add rule : ( triple(X, "hasGDP", Y) ) >> triple(X, "hasExport", Y) 
m.add rule : ( triple(X, "hasInflation", Y) ) >> triple(X, "hasEconomicGrowth", Y) 
m.add rule : ( triple(X, "objectEndRelation", Y) ) >> triple(X, "relationLocatedByObject", Y) 


def evidencePartition = new Partition(0);
def insert = data.getInserter(triple, evidencePartition);
InserterUtils.loadDelimitedDataTruth(insert, this.args[0]);

/*
insert.insertValue(0.133222, "VictorMoore",	"actedIn", "DangerousNanMcGrew");	
insert.insertValue(0.800683, "KatiaLoritz",	"actedIn", "Atracoalastres");	
insert.insertValue(0.27548, "BillOwenactor","actedIn", "TheShipThatDiedofShame");	
insert.insertValue(0.859237, "LeoGregory","actedIn", 	"TheBigIAm");	
insert.insertValue(0.890958, "ChinmoyRoy","actedIn", 	"GoopyGyneBaghaByne");	
insert.insertValue(0.228988, "Sukanyaactress","actedIn", 	"PudhuNelluPudhuNaathu");	
insert.insertValue(0.278879, "BobbyAndrews","actedIn", 	"WalaNaBangPagibig");	
insert.insertValue(0.779761, "ParinyaCharoenphol","actedIn", 	"MercuryManfilm");	
insert.insertValue(0.398604, "YoichiNumata","actedIn", 	"ThePrincessBlade");	
insert.insertValue(0.151184, "JordiMoll","actedIn", 	"TheMakingofPlusOne");	
*/


def targetPartition = new Partition(1);
//Database db = data.getDatabase(targetPartition, [tt] as Set, evidencePartition);
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
 for (int i = 0; i < 1; i++) {
	 start = System.currentTimeMillis();
	 LazyMPEInference results = new LazyMPEInference(m, db, config);
	 results.mpeInference();
	 end = System.currentTimeMillis();
	 overall += end - start;
 }

/*
 * Let's see the results
 */
println "Inference results with hand-defined weights:"
DecimalFormat formatter = new DecimalFormat("#.##");
/*
for (GroundAtom atom : Queries.getAllAtoms(db, person))
	println atom.toString() + "\t" + formatter.format(atom.getValue());
for (GroundAtom atom : Queries.getAllAtoms(db, triple))
	println atom.toString() + "\t" + formatter.format(atom.getValue());
for (GroundAtom atom : Queries.getAllAtoms(db, conflict))
	println atom.toString() + "\t" + formatter.format(atom.getValue());
*/

/*ArrayList<String> mpeState = new ArrayList<String>();
for (GroundAtom atom : Queries.getAllAtoms(db, triple)) {
	//if (Double.parseDouble(formatter.format(atom.getValue())) != 0) {
		//mpeState.add(atom.toString() + "\t" + formatter.format(atom.getValue()));
	   println atom.toString() + "\t" + formatter.format(atom.getValue());
	//}
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
for (GroundAtom atom : Queries.getAllAtoms(db, conflictExtended)){
	if (Double.parseDouble(formatter.format(atom.getValue())) != 0) {
		mpeState.add(atom.toString() + "\t" + formatter.format(atom.getValue()));
	}
}*/
/*
try {
	Files.write(Paths.get(this.args[1]), mpeState);
	System.out.println("[SUCCESS]");
}catch (IOException e) {
	//exception handling left as an exercise for the reader
} */
	/*
try {
	Writer writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(this.args[1]), "utf-8")); 
	for (String predicate : mpeState)
		writer.write(predicate + "\n");
	writer.close();
} catch (IOException e) {
		System.err.println("Cannot read file!")
} finally {
	//writer.close();
} */
	
System.out.println("Average runtime over 1 runs = " +  overall );//(end - start));
	
	
