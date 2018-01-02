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

m.add predicate: "hasWikipediaAnchorText",  types: [ArgumentType.String, ArgumentType.String]
m.add predicate: "hasImport",  types: [ArgumentType.String, ArgumentType.String]
m.add predicate: "linksTo",  types: [ArgumentType.String, ArgumentType.String]
m.add predicate: "playsFor",  types: [ArgumentType.String, ArgumentType.String]
m.add predicate: "wasBornOnDate",  types: [ArgumentType.String, ArgumentType.String]
m.add predicate: "objectStartRelation",  types: [ArgumentType.String, ArgumentType.String]
m.add predicate: "relationLocatedByObject",  types: [ArgumentType.String, ArgumentType.String]
m.add predicate: "occursUntil",  types: [ArgumentType.String, ArgumentType.String]
m.add predicate: "placedIn",  types: [ArgumentType.String, ArgumentType.String]
m.add predicate: "wasCreatedOnDate",  types: [ArgumentType.String, ArgumentType.String]
m.add predicate: "wroteMusicFor",  types: [ArgumentType.String, ArgumentType.String]
m.add predicate: "endsExistingOnDate",  types: [ArgumentType.String, ArgumentType.String]
m.add predicate: "permanentRelationToSubject",  types: [ArgumentType.String, ArgumentType.String]
m.add predicate: "hasCitationTitle",  types: [ArgumentType.String, ArgumentType.String]
m.add predicate: "hasGloss",  types: [ArgumentType.String, ArgumentType.String]
m.add predicate: "isMarriedTo",  types: [ArgumentType.String, ArgumentType.String]
m.add predicate: "isInterestedIn",  types: [ArgumentType.String, ArgumentType.String]
m.add predicate: "influences",  types: [ArgumentType.String, ArgumentType.String]
m.add predicate: "hasAirportCode",  types: [ArgumentType.String, ArgumentType.String]
m.add predicate: "worksAt",  types: [ArgumentType.String, ArgumentType.String]
m.add predicate: "hasContext",  types: [ArgumentType.String, ArgumentType.String]
m.add predicate: "hasContextPrecedingAnchor",  types: [ArgumentType.String, ArgumentType.String]
m.add predicate: "hasWikipediaArticleLength",  types: [ArgumentType.String, ArgumentType.String]
m.add predicate: "objectEndRelation",  types: [ArgumentType.String, ArgumentType.String]
m.add predicate: "hasTitleText",  types: [ArgumentType.String, ArgumentType.String]
m.add predicate: "hasWikipediaCategory",  types: [ArgumentType.String, ArgumentType.String]
m.add predicate: "permanentRelationToObject",  types: [ArgumentType.String, ArgumentType.String]
m.add predicate: "subjectEndRelation",  types: [ArgumentType.String, ArgumentType.String]
m.add predicate: "actedIn",  types: [ArgumentType.String, ArgumentType.String]
m.add predicate: "participatedIn",  types: [ArgumentType.String, ArgumentType.String]
m.add predicate: "directed",  types: [ArgumentType.String, ArgumentType.String]
m.add predicate: "hasCurrency",  types: [ArgumentType.String, ArgumentType.String]
m.add predicate: "startsExistingOnDate",  types: [ArgumentType.String, ArgumentType.String]
m.add predicate: "hasContextSucceedingAnchor",  types: [ArgumentType.String, ArgumentType.String]
m.add predicate: "hasFamilyName",  types: [ArgumentType.String, ArgumentType.String]
m.add predicate: "startedOnDate",  types: [ArgumentType.String, ArgumentType.String]
m.add predicate: "subjectStartRelation",  types: [ArgumentType.String, ArgumentType.String]
m.add predicate: "diedOnDate",  types: [ArgumentType.String, ArgumentType.String]
m.add predicate: "hasGDP",  types: [ArgumentType.String, ArgumentType.String]
m.add predicate: "hasAnchorText",  types: [ArgumentType.String, ArgumentType.String]
m.add predicate: "wasDestroyedOnDate",  types: [ArgumentType.String, ArgumentType.String]
m.add predicate: "hasAcademicAdvisor",  types: [ArgumentType.String, ArgumentType.String]
m.add predicate: "dealsWith",  types: [ArgumentType.String, ArgumentType.String]
m.add predicate: "hasEconomicGrowth",  types: [ArgumentType.String, ArgumentType.String]
m.add predicate: "endedOnDate",  types: [ArgumentType.String, ArgumentType.String]
m.add predicate: "hasExport",  types: [ArgumentType.String, ArgumentType.String]
m.add predicate: "hasChild",  types: [ArgumentType.String, ArgumentType.String]
m.add predicate: "hasThreeLetterLanguageCode",  types: [ArgumentType.String, ArgumentType.String]
m.add predicate: "hasInflation",  types: [ArgumentType.String, ArgumentType.String]
m.add predicate: "graduatedFrom",  types: [ArgumentType.String, ArgumentType.String]
m.add predicate: "hasWikipediaAbstract",  types: [ArgumentType.String, ArgumentType.String]
m.add predicate: "hasCapital",  types: [ArgumentType.String, ArgumentType.String]
m.add predicate: "holdsPoliticalPosition",  types: [ArgumentType.String, ArgumentType.String]
m.add predicate: "occursSince",  types: [ArgumentType.String, ArgumentType.String]
m.add predicate: "exports",  types: [ArgumentType.String, ArgumentType.String]
m.add predicate: "hasGivenName",  types: [ArgumentType.String, ArgumentType.String]
m.add predicate: "hasPoverty",  types: [ArgumentType.String, ArgumentType.String]
m.add predicate: "hasRevenue",  types: [ArgumentType.String, ArgumentType.String]
m.add predicate: "hasLatitude",  types: [ArgumentType.String, ArgumentType.String]
m.add predicate: "hasUnemployment",  types: [ArgumentType.String, ArgumentType.String]
m.add predicate: "hasNeighbor",  types: [ArgumentType.String, ArgumentType.String]
m.add predicate: "isConnectedTo",  types: [ArgumentType.String, ArgumentType.String]
m.add predicate: "wasBornIn",  types: [ArgumentType.String, ArgumentType.String]
m.add predicate: "diedIn",  types: [ArgumentType.String, ArgumentType.String]
m.add predicate: "hasGeonamesClassId",  types: [ArgumentType.String, ArgumentType.String]
m.add predicate: "extractionTechnique",  types: [ArgumentType.String, ArgumentType.String]
m.add predicate: "livesIn",  types: [ArgumentType.String, ArgumentType.String]
m.add predicate: "edited",  types: [ArgumentType.String, ArgumentType.String]
m.add predicate: "hasLongitude",  types: [ArgumentType.String, ArgumentType.String]
m.add predicate: "happenedOnDate",  types: [ArgumentType.String, ArgumentType.String]
m.add predicate: "hasPredecessor",  types: [ArgumentType.String, ArgumentType.String]
m.add predicate: "isCitizenOf",  types: [ArgumentType.String, ArgumentType.String]
m.add predicate: "hasNumberOfWikipediaLinks",  types: [ArgumentType.String, ArgumentType.String]
m.add predicate: "hasGeonamesEntityId",  types: [ArgumentType.String, ArgumentType.String]
m.add predicate: "hasLanguageCode",  types: [ArgumentType.String, ArgumentType.String]
m.add predicate: "hasSuccessor",  types: [ArgumentType.String, ArgumentType.String]
m.add predicate: "created",  types: [ArgumentType.String, ArgumentType.String]
m.add predicate: "isKnownFor",  types: [ArgumentType.String, ArgumentType.String]
m.add predicate: "imports",  types: [ArgumentType.String, ArgumentType.String]
m.add predicate: "owns",  types: [ArgumentType.String, ArgumentType.String]
m.add predicate: "hasExpenses",  types: [ArgumentType.String, ArgumentType.String]
m.add predicate: "hasBudget",  types: [ArgumentType.String, ArgumentType.String]



/*
// #1. A person cannot have two birth dates
m.add rule :  ( tf(X, "born", Y, Begin1, End1)  &  tf(X, "born", Z, Begin2, End2)  & notEqual(Begin1, Begin2) ) >> conflict(X, "born", Y), weight : 400.0
// #2. A person cannot have two death dates
m.add rule :  ( tf(X, "died", Y, Begin1, End1)  &  tf(X, "died", Z, Begin2, End2)  & notEqual(Begin1, Begin2) ) >> conflict(X, "died", Y), weight : 400.0
*/

m.add rule : ( hasWikipediaAnchorText(X, Y) ) >> hasTitleText(X,Y), weight : 0.643134
m.add rule : ( hasImport(X, Y) ) >> hasGDP(X,Y), weight : 0.858857
m.add rule : ( linksTo(X, Y) ) >> objectEndRelation(X,Y), weight : 0.0162865
m.add rule : ( hasWikipediaAnchorText(X, Y) ) >> hasWikipediaCategory(X,Y), weight : 0.328359
m.add rule : ( playsFor(X, Y) ) >> worksAt(X,Y), weight : 0.248993
m.add rule : ( wasBornOnDate(X, Y) ) >> endsExistingOnDate(X,Y), weight : 0.688117
m.add rule : ( objectStartRelation(X, Y) ) >> permanentRelationToSubject(X,Y), weight : 0.769215
m.add rule : ( relationLocatedByObject(X, Y) ) >> objectEndRelation(X,Y), weight : 0.515045
m.add rule : ( occursUntil(X, Y) ) >> occursSince(X,Y), weight : 0.826418
m.add rule : ( placedIn(X, Y) ) >> objectEndRelation(X,Y), weight : 0.447391
m.add rule : ( wasCreatedOnDate(X, Y) ) >> endsExistingOnDate(X,Y), weight : 0.0113152
m.add rule : ( wroteMusicFor(X, Y) ) >> edited(X,Y), weight : 0.956683
m.add rule : ( endsExistingOnDate(X, Y) ) >> diedOnDate(X,Y), weight : 0.777915
m.add rule : ( permanentRelationToSubject(X, Y) ) >> isInterestedIn(X,Y), weight : 0.762804
m.add rule : ( placedIn(X, Y) ) >> hasNeighbor(X,Y), weight : 0.238082
m.add rule : ( hasCitationTitle(X, Y) ) >> hasWikipediaAbstract(X,Y), weight : 0.870817
m.add rule : ( hasGloss(X, Y) ) >> hasFamilyName(X,Y), weight : 0.554067
m.add rule : ( objectStartRelation(X, Y) ) >> isInterestedIn(X,Y), weight : 0.0822929
m.add rule : ( linksTo(X, Y) ) >> permanentRelationToObject(X,Y), weight : 0.75745
m.add rule : ( isMarriedTo(X, Y) ) >> isInterestedIn(X,Y), weight : 0.286655
m.add rule : ( isInterestedIn(X, Y) ) >> permanentRelationToSubject(X,Y), weight : 0.886551
m.add rule : ( influences(X, Y) ) >> holdsPoliticalPosition(X,Y), weight : 0.674464
m.add rule : ( hasCitationTitle(X, Y) ) >> hasWikipediaAnchorText(X,Y), weight : 0.599767
m.add rule : ( hasAirportCode(X, Y) ) >> hasGloss(X,Y), weight : 0.117318
m.add rule : ( wroteMusicFor(X, Y) ) >> actedIn(X,Y), weight : 0.709204
m.add rule : ( worksAt(X, Y) ) >> graduatedFrom(X,Y), weight : 0.422934
m.add rule : ( hasContext(X, Y) ) >> hasAirportCode(X,Y), weight : 0.407478
m.add rule : ( placedIn(X, Y) ) >> permanentRelationToObject(X,Y), weight : 0.867594
m.add rule : ( relationLocatedByObject(X, Y) ) >> permanentRelationToObject(X,Y), weight : 0.950451
m.add rule : ( hasContextPrecedingAnchor(X, Y) ) >> extractionTechnique(X,Y), weight : 0.253501
m.add rule : ( isInterestedIn(X, Y) ) >> wasBornIn(X,Y), weight : 0.612473
m.add rule : ( isInterestedIn(X, Y) ) >> objectStartRelation(X,Y), weight : 0.447959
m.add rule : ( hasWikipediaArticleLength(X, Y) ) >> hasNumberOfWikipediaLinks(X,Y), weight : 0.291071
m.add rule : ( objectEndRelation(X, Y) ) >> subjectEndRelation(X,Y), weight : 0.30087
m.add rule : ( relationLocatedByObject(X, Y) ) >> hasCurrency(X,Y), weight : 0.217678
m.add rule : ( hasTitleText(X, Y) ) >> hasCitationTitle(X,Y), weight : 0.134925
m.add rule : ( placedIn(X, Y) ) >> hasCurrency(X,Y), weight : 0.644681
m.add rule : ( hasWikipediaCategory(X, Y) ) >> hasCitationTitle(X,Y), weight : 0.271596
m.add rule : ( wasCreatedOnDate(X, Y) ) >> diedOnDate(X,Y), weight : 0.800248
m.add rule : ( isInterestedIn(X, Y) ) >> graduatedFrom(X,Y), weight : 0.623176
m.add rule : ( permanentRelationToObject(X, Y) ) >> subjectEndRelation(X,Y), weight : 0.382373
m.add rule : ( subjectEndRelation(X, Y) ) >> linksTo(X,Y), weight : 0.195974
m.add rule : ( actedIn(X, Y) ) >> directed(X,Y), weight : 0.95305
m.add rule : ( wasBornOnDate(X, Y) ) >> diedOnDate(X,Y), weight : 0.925823
m.add rule : ( permanentRelationToSubject(X, Y) ) >> objectStartRelation(X,Y), weight : 0.0343145
m.add rule : ( participatedIn(X, Y) ) >> owns(X,Y), weight : 0.453869
m.add rule : ( directed(X, Y) ) >> edited(X,Y), weight : 0.115669
m.add rule : ( hasCurrency(X, Y) ) >> dealsWith(X,Y), weight : 0.495273
m.add rule : ( linksTo(X, Y) ) >> hasCurrency(X,Y), weight : 0.856105
m.add rule : ( startsExistingOnDate(X, Y) ) >> wasBornOnDate(X,Y), weight : 0.163672
m.add rule : ( hasContextSucceedingAnchor(X, Y) ) >> hasAnchorText(X,Y), weight : 0.939287
m.add rule : ( worksAt(X, Y) ) >> isInterestedIn(X,Y), weight : 0.146441
m.add rule : ( linksTo(X, Y) ) >> subjectStartRelation(X,Y), weight : 0.863115
m.add rule : ( hasFamilyName(X, Y) ) >> hasGivenName(X,Y), weight : 0.492079
m.add rule : ( hasCurrency(X, Y) ) >> subjectEndRelation(X,Y), weight : 0.19636
m.add rule : ( isInterestedIn(X, Y) ) >> isMarriedTo(X,Y), weight : 0.0213454
m.add rule : ( startedOnDate(X, Y) ) >> endedOnDate(X,Y), weight : 0.129612
m.add rule : ( startsExistingOnDate(X, Y) ) >> wasCreatedOnDate(X,Y), weight : 0.170051
m.add rule : ( relationLocatedByObject(X, Y) ) >> subjectStartRelation(X,Y), weight : 0.735063
m.add rule : ( directed(X, Y) ) >> actedIn(X,Y), weight : 0.0732447
m.add rule : ( placedIn(X, Y) ) >> subjectStartRelation(X,Y), weight : 0.108774
m.add rule : ( subjectEndRelation(X, Y) ) >> placedIn(X,Y), weight : 0.0100369
m.add rule : ( subjectStartRelation(X, Y) ) >> subjectEndRelation(X,Y), weight : 0.686497
m.add rule : ( diedOnDate(X, Y) ) >> endsExistingOnDate(X,Y), weight : 0.32963
m.add rule : ( subjectEndRelation(X, Y) ) >> relationLocatedByObject(X,Y), weight : 0.188488
m.add rule : ( isInterestedIn(X, Y) ) >> diedIn(X,Y), weight : 0.204774
m.add rule : ( hasGloss(X, Y) ) >> hasContext(X,Y), weight : 0.533134
m.add rule : ( hasGDP(X, Y) ) >> hasImport(X,Y), weight : 0.782126
m.add rule : ( isInterestedIn(X, Y) ) >> subjectEndRelation(X,Y), weight : 0.470243
m.add rule : ( wasCreatedOnDate(X, Y) ) >> wasBornOnDate(X,Y), weight : 0.239457
m.add rule : ( objectEndRelation(X, Y) ) >> objectStartRelation(X,Y), weight : 0.754502
m.add rule : ( hasAnchorText(X, Y) ) >> hasContextSucceedingAnchor(X,Y), weight : 0.58092
m.add rule : ( isInterestedIn(X, Y) ) >> livesIn(X,Y), weight : 0.0283118
m.add rule : ( placedIn(X, Y) ) >> linksTo(X,Y), weight : 0.039627
m.add rule : ( wasDestroyedOnDate(X, Y) ) >> startsExistingOnDate(X,Y), weight : 0.99631
m.add rule : ( wasBornOnDate(X, Y) ) >> wasCreatedOnDate(X,Y), weight : 0.774226
m.add rule : ( hasContextSucceedingAnchor(X, Y) ) >> hasContextPrecedingAnchor(X,Y), weight : 0.53703
m.add rule : ( hasAcademicAdvisor(X, Y) ) >> isInterestedIn(X,Y), weight : 0.775112
m.add rule : ( dealsWith(X, Y) ) >> hasCurrency(X,Y), weight : 0.645929
m.add rule : ( relationLocatedByObject(X, Y) ) >> linksTo(X,Y), weight : 0.199996
m.add rule : ( influences(X, Y) ) >> hasChild(X,Y), weight : 0.282289
m.add rule : ( permanentRelationToObject(X, Y) ) >> objectStartRelation(X,Y), weight : 0.0397387
m.add rule : ( hasEconomicGrowth(X, Y) ) >> hasPoverty(X,Y), weight : 0.326393
m.add rule : ( playsFor(X, Y) ) >> graduatedFrom(X,Y), weight : 0.212944
m.add rule : ( subjectEndRelation(X, Y) ) >> hasCurrency(X,Y), weight : 0.887408
m.add rule : ( endedOnDate(X, Y) ) >> happenedOnDate(X,Y), weight : 0.487176
m.add rule : ( hasExport(X, Y) ) >> hasImport(X,Y), weight : 0.604494
m.add rule : ( permanentRelationToSubject(X, Y) ) >> subjectEndRelation(X,Y), weight : 0.313698
m.add rule : ( hasChild(X, Y) ) >> isInterestedIn(X,Y), weight : 0.736633
m.add rule : ( hasThreeLetterLanguageCode(X, Y) ) >> hasLanguageCode(X,Y), weight : 0.144111
m.add rule : ( hasWikipediaAnchorText(X, Y) ) >> hasCitationTitle(X,Y), weight : 0.0117043
m.add rule : ( placedIn(X, Y) ) >> relationLocatedByObject(X,Y), weight : 0.962156
m.add rule : ( hasInflation(X, Y) ) >> hasUnemployment(X,Y), weight : 0.215657
m.add rule : ( objectStartRelation(X, Y) ) >> subjectEndRelation(X,Y), weight : 0.82813
m.add rule : ( graduatedFrom(X, Y) ) >> playsFor(X,Y), weight : 0.276089
m.add rule : ( hasWikipediaAbstract(X, Y) ) >> hasWikipediaAnchorText(X,Y), weight : 0.56716
m.add rule : ( hasCapital(X, Y) ) >> dealsWith(X,Y), weight : 0.86803
m.add rule : ( holdsPoliticalPosition(X, Y) ) >> isMarriedTo(X,Y), weight : 0.0857079
m.add rule : ( hasCurrency(X, Y) ) >> objectStartRelation(X,Y), weight : 0.220632
m.add rule : ( occursSince(X, Y) ) >> occursUntil(X,Y), weight : 0.865313
m.add rule : ( relationLocatedByObject(X, Y) ) >> placedIn(X,Y), weight : 0.13691
m.add rule : ( subjectEndRelation(X, Y) ) >> subjectStartRelation(X,Y), weight : 0.937157
m.add rule : ( hasEconomicGrowth(X, Y) ) >> hasInflation(X,Y), weight : 0.560333
m.add rule : ( exports(X, Y) ) >> imports(X,Y), weight : 0.942706
m.add rule : ( linksTo(X, Y) ) >> relationLocatedByObject(X,Y), weight : 0.13868
m.add rule : ( startsExistingOnDate(X, Y) ) >> diedOnDate(X,Y), weight : 0.0917293
m.add rule : ( subjectStartRelation(X, Y) ) >> objectStartRelation(X,Y), weight : 0.0175522
m.add rule : ( hasGivenName(X, Y) ) >> hasContext(X,Y), weight : 0.0518667
m.add rule : ( influences(X, Y) ) >> hasAcademicAdvisor(X,Y), weight : 0.505736
m.add rule : ( hasPoverty(X, Y) ) >> hasUnemployment(X,Y), weight : 0.621405
m.add rule : ( linksTo(X, Y) ) >> placedIn(X,Y), weight : 0.116678
m.add rule : ( playsFor(X, Y) ) >> isInterestedIn(X,Y), weight : 0.972783
m.add rule : ( objectEndRelation(X, Y) ) >> isInterestedIn(X,Y), weight : 0.136455
m.add rule : ( placedIn(X, Y) ) >> isConnectedTo(X,Y), weight : 0.075742
m.add rule : ( endsExistingOnDate(X, Y) ) >> wasCreatedOnDate(X,Y), weight : 0.222183
m.add rule : ( hasExport(X, Y) ) >> hasGDP(X,Y), weight : 0.085298
m.add rule : ( subjectStartRelation(X, Y) ) >> permanentRelationToSubject(X,Y), weight : 0.577377
m.add rule : ( isInterestedIn(X, Y) ) >> worksAt(X,Y), weight : 0.773737
m.add rule : ( holdsPoliticalPosition(X, Y) ) >> isInterestedIn(X,Y), weight : 0.795082
m.add rule : ( hasCurrency(X, Y) ) >> hasCapital(X,Y), weight : 0.924694
m.add rule : ( subjectEndRelation(X, Y) ) >> objectEndRelation(X,Y), weight : 0.0947455
m.add rule : ( hasFamilyName(X, Y) ) >> hasContext(X,Y), weight : 0.829809
m.add rule : ( endsExistingOnDate(X, Y) ) >> wasBornOnDate(X,Y), weight : 0.903054
m.add rule : ( actedIn(X, Y) ) >> wroteMusicFor(X,Y), weight : 0.011828
m.add rule : ( isInterestedIn(X, Y) ) >> isCitizenOf(X,Y), weight : 0.0218649
m.add rule : ( hasAnchorText(X, Y) ) >> hasContextPrecedingAnchor(X,Y), weight : 0.708361
m.add rule : ( hasWikipediaCategory(X, Y) ) >> hasTitleText(X,Y), weight : 0.0379918
m.add rule : ( hasGloss(X, Y) ) >> hasGivenName(X,Y), weight : 0.226479
m.add rule : ( permanentRelationToObject(X, Y) ) >> isInterestedIn(X,Y), weight : 0.431254
m.add rule : ( hasContext(X, Y) ) >> hasGloss(X,Y), weight : 0.964387
m.add rule : ( hasCurrency(X, Y) ) >> permanentRelationToSubject(X,Y), weight : 0.746513
m.add rule : ( hasTitleText(X, Y) ) >> hasWikipediaCategory(X,Y), weight : 0.216756
m.add rule : ( hasAcademicAdvisor(X, Y) ) >> isMarriedTo(X,Y), weight : 0.456214
m.add rule : ( permanentRelationToObject(X, Y) ) >> permanentRelationToSubject(X,Y), weight : 0.210716
m.add rule : ( subjectEndRelation(X, Y) ) >> permanentRelationToObject(X,Y), weight : 0.791636
m.add rule : ( diedOnDate(X, Y) ) >> wasCreatedOnDate(X,Y), weight : 0.819948
m.add rule : ( endedOnDate(X, Y) ) >> startedOnDate(X,Y), weight : 0.859575
m.add rule : ( startedOnDate(X, Y) ) >> happenedOnDate(X,Y), weight : 0.855886
m.add rule : ( hasRevenue(X, Y) ) >> hasBudget(X,Y), weight : 0.630111
m.add rule : ( diedOnDate(X, Y) ) >> wasBornOnDate(X,Y), weight : 0.167141
m.add rule : ( hasGivenName(X, Y) ) >> hasFamilyName(X,Y), weight : 0.942253
m.add rule : ( subjectStartRelation(X, Y) ) >> isInterestedIn(X,Y), weight : 0.588181
m.add rule : ( hasLatitude(X, Y) ) >> hasLongitude(X,Y), weight : 0.788177
m.add rule : ( startsExistingOnDate(X, Y) ) >> endsExistingOnDate(X,Y), weight : 0.070466
m.add rule : ( hasRevenue(X, Y) ) >> hasExpenses(X,Y), weight : 0.110205
m.add rule : ( hasChild(X, Y) ) >> isMarriedTo(X,Y), weight : 0.436598
m.add rule : ( objectEndRelation(X, Y) ) >> permanentRelationToSubject(X,Y), weight : 0.649542
m.add rule : ( subjectEndRelation(X, Y) ) >> objectStartRelation(X,Y), weight : 0.536951
m.add rule : ( hasChild(X, Y) ) >> holdsPoliticalPosition(X,Y), weight : 0.0241261
m.add rule : ( hasWikipediaAbstract(X, Y) ) >> hasWikipediaCategory(X,Y), weight : 0.62862
m.add rule : ( hasUnemployment(X, Y) ) >> hasInflation(X,Y), weight : 0.942318
m.add rule : ( permanentRelationToObject(X, Y) ) >> hasCurrency(X,Y), weight : 0.678951
m.add rule : ( endsExistingOnDate(X, Y) ) >> startsExistingOnDate(X,Y), weight : 0.823062
m.add rule : ( hasCurrency(X, Y) ) >> subjectStartRelation(X,Y), weight : 0.834766
m.add rule : ( linksTo(X, Y) ) >> subjectEndRelation(X,Y), weight : 0.796922
m.add rule : ( hasWikipediaAbstract(X, Y) ) >> hasTitleText(X,Y), weight : 0.0125785
m.add rule : ( permanentRelationToSubject(X, Y) ) >> linksTo(X,Y), weight : 0.840708
m.add rule : ( wasBornOnDate(X, Y) ) >> wasDestroyedOnDate(X,Y), weight : 0.116797
m.add rule : ( hasNeighbor(X, Y) ) >> hasCurrency(X,Y), weight : 0.683957
m.add rule : ( wasCreatedOnDate(X, Y) ) >> wasDestroyedOnDate(X,Y), weight : 0.551987
m.add rule : ( isMarriedTo(X, Y) ) >> hasChild(X,Y), weight : 0.637695
m.add rule : ( hasAirportCode(X, Y) ) >> hasContext(X,Y), weight : 0.858328
m.add rule : ( isInterestedIn(X, Y) ) >> linksTo(X,Y), weight : 0.723641
m.add rule : ( isInterestedIn(X, Y) ) >> hasAcademicAdvisor(X,Y), weight : 0.860551
m.add rule : ( objectEndRelation(X, Y) ) >> hasCurrency(X,Y), weight : 0.797708
m.add rule : ( isConnectedTo(X, Y) ) >> hasNeighbor(X,Y), weight : 0.358041
m.add rule : ( hasContextSucceedingAnchor(X, Y) ) >> extractionTechnique(X,Y), weight : 0.300747
m.add rule : ( objectStartRelation(X, Y) ) >> placedIn(X,Y), weight : 0.439427
m.add rule : ( hasAcademicAdvisor(X, Y) ) >> holdsPoliticalPosition(X,Y), weight : 0.531156
m.add rule : ( objectStartRelation(X, Y) ) >> relationLocatedByObject(X,Y), weight : 0.548708
m.add rule : ( placedIn(X, Y) ) >> subjectEndRelation(X,Y), weight : 0.600575
m.add rule : ( relationLocatedByObject(X, Y) ) >> subjectEndRelation(X,Y), weight : 0.106311
m.add rule : ( subjectStartRelation(X, Y) ) >> hasCurrency(X,Y), weight : 0.727716
m.add rule : ( permanentRelationToSubject(X, Y) ) >> relationLocatedByObject(X,Y), weight : 0.844394
m.add rule : ( isMarriedTo(X, Y) ) >> hasAcademicAdvisor(X,Y), weight : 0.817177
m.add rule : ( diedOnDate(X, Y) ) >> startsExistingOnDate(X,Y), weight : 0.953632
m.add rule : ( wasBornIn(X, Y) ) >> isInterestedIn(X,Y), weight : 0.029374
m.add rule : ( objectEndRelation(X, Y) ) >> subjectStartRelation(X,Y), weight : 0.251557
m.add rule : ( graduatedFrom(X, Y) ) >> isInterestedIn(X,Y), weight : 0.336855
m.add rule : ( permanentRelationToSubject(X, Y) ) >> placedIn(X,Y), weight : 0.914232
m.add rule : ( directed(X, Y) ) >> wroteMusicFor(X,Y), weight : 0.687968
m.add rule : ( objectStartRelation(X, Y) ) >> linksTo(X,Y), weight : 0.483051
m.add rule : ( isInterestedIn(X, Y) ) >> placedIn(X,Y), weight : 0.407745
m.add rule : ( isInterestedIn(X, Y) ) >> relationLocatedByObject(X,Y), weight : 0.50249
m.add rule : ( isInterestedIn(X, Y) ) >> hasChild(X,Y), weight : 0.332299
m.add rule : ( hasUnemployment(X, Y) ) >> hasPoverty(X,Y), weight : 0.235353
m.add rule : ( diedIn(X, Y) ) >> isInterestedIn(X,Y), weight : 0.247181
m.add rule : ( hasGeonamesClassId(X, Y) ) >> hasGeonamesEntityId(X,Y), weight : 0.269045
m.add rule : ( extractionTechnique(X, Y) ) >> hasContextSucceedingAnchor(X,Y), weight : 0.977407
m.add rule : ( permanentRelationToObject(X, Y) ) >> subjectStartRelation(X,Y), weight : 0.0153987
m.add rule : ( endsExistingOnDate(X, Y) ) >> wasDestroyedOnDate(X,Y), weight : 0.241878
m.add rule : ( wasDestroyedOnDate(X, Y) ) >> wasBornOnDate(X,Y), weight : 0.673132
m.add rule : ( livesIn(X, Y) ) >> isInterestedIn(X,Y), weight : 0.637519
m.add rule : ( subjectEndRelation(X, Y) ) >> isInterestedIn(X,Y), weight : 0.384032
m.add rule : ( edited(X, Y) ) >> directed(X,Y), weight : 0.600789
m.add rule : ( hasAnchorText(X, Y) ) >> extractionTechnique(X,Y), weight : 0.0570022
m.add rule : ( hasContext(X, Y) ) >> hasGivenName(X,Y), weight : 0.267718
m.add rule : ( hasCurrency(X, Y) ) >> permanentRelationToObject(X,Y), weight : 0.0593546
m.add rule : ( hasContextPrecedingAnchor(X, Y) ) >> hasAnchorText(X,Y), weight : 0.879303
m.add rule : ( wasCreatedOnDate(X, Y) ) >> startsExistingOnDate(X,Y), weight : 0.738878
m.add rule : ( wasDestroyedOnDate(X, Y) ) >> wasCreatedOnDate(X,Y), weight : 0.594764
m.add rule : ( wasBornOnDate(X, Y) ) >> startsExistingOnDate(X,Y), weight : 0.224875
m.add rule : ( hasLongitude(X, Y) ) >> hasLatitude(X,Y), weight : 0.392016
m.add rule : ( happenedOnDate(X, Y) ) >> endedOnDate(X,Y), weight : 0.334269
m.add rule : ( isInterestedIn(X, Y) ) >> influences(X,Y), weight : 0.92245
m.add rule : ( hasCurrency(X, Y) ) >> hasNeighbor(X,Y), weight : 0.710627
m.add rule : ( subjectStartRelation(X, Y) ) >> permanentRelationToObject(X,Y), weight : 0.781093
m.add rule : ( hasPredecessor(X, Y) ) >> hasSuccessor(X,Y), weight : 0.891298
m.add rule : ( wasBornIn(X, Y) ) >> diedIn(X,Y), weight : 0.327896
m.add rule : ( permanentRelationToObject(X, Y) ) >> objectEndRelation(X,Y), weight : 0.977438
m.add rule : ( diedOnDate(X, Y) ) >> wasDestroyedOnDate(X,Y), weight : 0.514389
m.add rule : ( isMarriedTo(X, Y) ) >> influences(X,Y), weight : 0.538515
m.add rule : ( hasCurrency(X, Y) ) >> objectEndRelation(X,Y), weight : 0.167135
m.add rule : ( hasUnemployment(X, Y) ) >> hasEconomicGrowth(X,Y), weight : 0.109453
m.add rule : ( objectEndRelation(X, Y) ) >> permanentRelationToObject(X,Y), weight : 0.788404
m.add rule : ( hasTitleText(X, Y) ) >> hasWikipediaAbstract(X,Y), weight : 0.611466
m.add rule : ( extractionTechnique(X, Y) ) >> hasContextPrecedingAnchor(X,Y), weight : 0.446232
m.add rule : ( hasWikipediaCategory(X, Y) ) >> hasWikipediaAbstract(X,Y), weight : 0.243154
m.add rule : ( isCitizenOf(X, Y) ) >> isInterestedIn(X,Y), weight : 0.255733
m.add rule : ( subjectEndRelation(X, Y) ) >> permanentRelationToSubject(X,Y), weight : 0.0964407
m.add rule : ( subjectStartRelation(X, Y) ) >> objectEndRelation(X,Y), weight : 0.213238
m.add rule : ( hasNumberOfWikipediaLinks(X, Y) ) >> hasWikipediaArticleLength(X,Y), weight : 0.897195
m.add rule : ( isConnectedTo(X, Y) ) >> hasCurrency(X,Y), weight : 0.449182
m.add rule : ( hasTitleText(X, Y) ) >> hasWikipediaAnchorText(X,Y), weight : 0.0868772
m.add rule : ( dealsWith(X, Y) ) >> hasCapital(X,Y), weight : 0.945205
m.add rule : ( diedIn(X, Y) ) >> wasBornIn(X,Y), weight : 0.668846
m.add rule : ( hasWikipediaCategory(X, Y) ) >> hasWikipediaAnchorText(X,Y), weight : 0.529397
m.add rule : ( hasPoverty(X, Y) ) >> hasInflation(X,Y), weight : 0.327105
m.add rule : ( objectStartRelation(X, Y) ) >> permanentRelationToObject(X,Y), weight : 0.685146
m.add rule : ( hasGeonamesEntityId(X, Y) ) >> hasGeonamesClassId(X,Y), weight : 0.985893
m.add rule : ( hasNeighbor(X, Y) ) >> isConnectedTo(X,Y), weight : 0.42532
m.add rule : ( extractionTechnique(X, Y) ) >> hasAnchorText(X,Y), weight : 0.956476
m.add rule : ( hasChild(X, Y) ) >> hasAcademicAdvisor(X,Y), weight : 0.505184
m.add rule : ( hasGivenName(X, Y) ) >> hasGloss(X,Y), weight : 0.105759
m.add rule : ( hasLanguageCode(X, Y) ) >> hasThreeLetterLanguageCode(X,Y), weight : 0.21207
m.add rule : ( isInterestedIn(X, Y) ) >> objectEndRelation(X,Y), weight : 0.939785
m.add rule : ( linksTo(X, Y) ) >> isInterestedIn(X,Y), weight : 0.784179
m.add rule : ( isInterestedIn(X, Y) ) >> playsFor(X,Y), weight : 0.601356
m.add rule : ( wasDestroyedOnDate(X, Y) ) >> diedOnDate(X,Y), weight : 0.554988
m.add rule : ( hasEconomicGrowth(X, Y) ) >> hasUnemployment(X,Y), weight : 0.584362
m.add rule : ( hasCitationTitle(X, Y) ) >> hasWikipediaCategory(X,Y), weight : 0.835918
m.add rule : ( hasWikipediaAbstract(X, Y) ) >> hasCitationTitle(X,Y), weight : 0.172773
m.add rule : ( permanentRelationToSubject(X, Y) ) >> objectEndRelation(X,Y), weight : 0.0870048
m.add rule : ( hasCitationTitle(X, Y) ) >> hasTitleText(X,Y), weight : 0.774973
m.add rule : ( holdsPoliticalPosition(X, Y) ) >> influences(X,Y), weight : 0.258024
m.add rule : ( isConnectedTo(X, Y) ) >> placedIn(X,Y), weight : 0.665769
m.add rule : ( relationLocatedByObject(X, Y) ) >> isInterestedIn(X,Y), weight : 0.168259
m.add rule : ( isInterestedIn(X, Y) ) >> holdsPoliticalPosition(X,Y), weight : 0.500558
m.add rule : ( placedIn(X, Y) ) >> isInterestedIn(X,Y), weight : 0.735911
m.add rule : ( isInterestedIn(X, Y) ) >> permanentRelationToObject(X,Y), weight : 0.983091
m.add rule : ( hasGloss(X, Y) ) >> hasAirportCode(X,Y), weight : 0.252137
m.add rule : ( hasInflation(X, Y) ) >> hasPoverty(X,Y), weight : 0.229544
m.add rule : ( happenedOnDate(X, Y) ) >> startedOnDate(X,Y), weight : 0.244942
m.add rule : ( hasSuccessor(X, Y) ) >> hasPredecessor(X,Y), weight : 0.486821
m.add rule : ( isMarriedTo(X, Y) ) >> holdsPoliticalPosition(X,Y), weight : 0.159952
m.add rule : ( placedIn(X, Y) ) >> permanentRelationToSubject(X,Y), weight : 0.797471
m.add rule : ( relationLocatedByObject(X, Y) ) >> permanentRelationToSubject(X,Y), weight : 0.181503
m.add rule : ( influences(X, Y) ) >> isInterestedIn(X,Y), weight : 0.782292
m.add rule : ( objectStartRelation(X, Y) ) >> objectEndRelation(X,Y), weight : 0.839294
m.add rule : ( hasCurrency(X, Y) ) >> isConnectedTo(X,Y), weight : 0.107012
m.add rule : ( hasAcademicAdvisor(X, Y) ) >> hasChild(X,Y), weight : 0.166367
m.add rule : ( permanentRelationToSubject(X, Y) ) >> permanentRelationToObject(X,Y), weight : 0.0456697
m.add rule : ( linksTo(X, Y) ) >> permanentRelationToSubject(X,Y), weight : 0.784548
m.add rule : ( created(X, Y) ) >> isKnownFor(X,Y), weight : 0.379312
m.add rule : ( hasContext(X, Y) ) >> hasFamilyName(X,Y), weight : 0.604187
m.add rule : ( hasWikipediaAnchorText(X, Y) ) >> hasWikipediaAbstract(X,Y), weight : 0.996203
m.add rule : ( subjectStartRelation(X, Y) ) >> placedIn(X,Y), weight : 0.330472
m.add rule : ( permanentRelationToObject(X, Y) ) >> linksTo(X,Y), weight : 0.252922
m.add rule : ( influences(X, Y) ) >> isMarriedTo(X,Y), weight : 0.963549
m.add rule : ( hasCapital(X, Y) ) >> hasCurrency(X,Y), weight : 0.744642
m.add rule : ( edited(X, Y) ) >> wroteMusicFor(X,Y), weight : 0.63594
m.add rule : ( hasChild(X, Y) ) >> influences(X,Y), weight : 0.963836
m.add rule : ( linksTo(X, Y) ) >> objectStartRelation(X,Y), weight : 0.941275
m.add rule : ( objectStartRelation(X, Y) ) >> subjectStartRelation(X,Y), weight : 0.455663
m.add rule : ( permanentRelationToSubject(X, Y) ) >> hasCurrency(X,Y), weight : 0.994178
m.add rule : ( subjectStartRelation(X, Y) ) >> relationLocatedByObject(X,Y), weight : 0.161313
m.add rule : ( hasImport(X, Y) ) >> hasExport(X,Y), weight : 0.270767
m.add rule : ( hasCurrency(X, Y) ) >> relationLocatedByObject(X,Y), weight : 0.059171
m.add rule : ( isKnownFor(X, Y) ) >> created(X,Y), weight : 0.670637
m.add rule : ( objectEndRelation(X, Y) ) >> linksTo(X,Y), weight : 0.116869
m.add rule : ( wasDestroyedOnDate(X, Y) ) >> endsExistingOnDate(X,Y), weight : 0.360024
m.add rule : ( placedIn(X, Y) ) >> objectStartRelation(X,Y), weight : 0.615756
m.add rule : ( imports(X, Y) ) >> exports(X,Y), weight : 0.712197
m.add rule : ( hasCurrency(X, Y) ) >> placedIn(X,Y), weight : 0.925435
m.add rule : ( holdsPoliticalPosition(X, Y) ) >> hasAcademicAdvisor(X,Y), weight : 0.822629
m.add rule : ( relationLocatedByObject(X, Y) ) >> objectStartRelation(X,Y), weight : 0.271811
m.add rule : ( hasAcademicAdvisor(X, Y) ) >> influences(X,Y), weight : 0.358689
m.add rule : ( owns(X, Y) ) >> participatedIn(X,Y), weight : 0.303893
m.add rule : ( hasExpenses(X, Y) ) >> hasRevenue(X,Y), weight : 0.972739
m.add rule : ( hasExpenses(X, Y) ) >> hasBudget(X,Y), weight : 0.502136
m.add rule : ( graduatedFrom(X, Y) ) >> worksAt(X,Y), weight : 0.829241
m.add rule : ( subjectStartRelation(X, Y) ) >> linksTo(X,Y), weight : 0.514387
m.add rule : ( hasPoverty(X, Y) ) >> hasEconomicGrowth(X,Y), weight : 0.50028
m.add rule : ( permanentRelationToObject(X, Y) ) >> placedIn(X,Y), weight : 0.9256
m.add rule : ( hasNeighbor(X, Y) ) >> placedIn(X,Y), weight : 0.882076
m.add rule : ( worksAt(X, Y) ) >> playsFor(X,Y), weight : 0.38726
m.add rule : ( permanentRelationToObject(X, Y) ) >> relationLocatedByObject(X,Y), weight : 0.493019
m.add rule : ( hasBudget(X, Y) ) >> hasRevenue(X,Y), weight : 0.705089
m.add rule : ( isInterestedIn(X, Y) ) >> subjectStartRelation(X,Y), weight : 0.644874
m.add rule : ( startsExistingOnDate(X, Y) ) >> wasDestroyedOnDate(X,Y), weight : 0.429053
m.add rule : ( wroteMusicFor(X, Y) ) >> directed(X,Y), weight : 0.0304094
m.add rule : ( objectEndRelation(X, Y) ) >> placedIn(X,Y), weight : 0.585397
m.add rule : ( hasContextPrecedingAnchor(X, Y) ) >> hasContextSucceedingAnchor(X,Y), weight : 0.169759
m.add rule : ( objectStartRelation(X, Y) ) >> hasCurrency(X,Y), weight : 0.00567773
m.add rule : ( holdsPoliticalPosition(X, Y) ) >> hasChild(X,Y), weight : 0.178451
m.add rule : ( permanentRelationToSubject(X, Y) ) >> subjectStartRelation(X,Y), weight : 0.265456
m.add rule : ( hasBudget(X, Y) ) >> hasExpenses(X,Y), weight : 0.0404289
m.add rule : ( hasFamilyName(X, Y) ) >> hasGloss(X,Y), weight : 0.298453
m.add rule : ( hasCurrency(X, Y) ) >> linksTo(X,Y), weight : 0.964221
m.add rule : ( hasGDP(X, Y) ) >> hasExport(X,Y), weight : 0.13248
m.add rule : ( hasInflation(X, Y) ) >> hasEconomicGrowth(X,Y), weight : 0.633038
m.add rule : ( objectEndRelation(X, Y) ) >> relationLocatedByObject(X,Y), weight : 0.368949




def evidencePartition = new Partition(0);
def insert = data.getInserter(actedIn, evidencePartition);
/*
def dir = 'data'+java.io.File.separator+'footballdb'+java.io.File.separator;
InserterUtils.loadDelimitedDataTruth(insert, dir+"player_team_year_psl.txt");
*/
// accept input filename 
//InserterUtils.loadDelimitedDataTruth(insert, this.args[0]);
//InserterUtils.loadDelimitedDataTruth(insert, dir+"test_psl.txt");
/*
//Rule test data
insert.insertValue(0.6593, "Zoltan Mesko", "playsFor",	"A",	2010,	2012);
insert.insertValue(0.6593, "Zoltan Mesko", "playsFor",	"A",	2013,	2013);
insert.insertValue(0.6593, "Zoltan Mesko", "born",	1965,	1965,	1965);
*/
insert.insertValue(0.133222, "VictorMoore",	"DangerousNanMcGrew");	
insert.insertValue(0.800683, "KatiaLoritz",	"Atracoalastres");	
insert.insertValue(0.27548, "BillOwenactor","TheShipThatDiedofShame");	
insert.insertValue(0.859237, "LeoGregory",	"TheBigIAm");	
insert.insertValue(0.890958, "ChinmoyRoy",	"GoopyGyneBaghaByne");	
insert.insertValue(0.228988, "Sukanyaactress",	"PudhuNelluPudhuNaathu");	
insert.insertValue(0.278879, "BobbyAndrews",	"WalaNaBangPagibig");	
insert.insertValue(0.779761, "ParinyaCharoenphol",	"MercuryManfilm");	
insert.insertValue(0.398604, "YoichiNumata",	"ThePrincessBlade");	
insert.insertValue(0.151184, "JordiMoll",	"TheMakingofPlusOne");	



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
for (GroundAtom atom : Queries.getAllAtoms(db, person)) {
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
	
System.out.println("Average run time over 1 runs = " +  overall );//(end - start));
	
	
