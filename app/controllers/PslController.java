package controllers;

import akka.japi.pf.Match;
import groovy.lang.Binding;
import groovy.lang.GroovyShell;
import groovy.lang.Script;
import groovy.util.ResourceException;
import groovy.util.ScriptException;
import models.DataTablesResponse;
import models.Reasoner;
import models.Statement;
import org.apache.commons.lang3.StringUtils;
import play.cache.SyncCacheApi;
import play.libs.Json;
import play.mvc.Controller;
import play.mvc.Http;
import play.mvc.Result;
import play.mvc.Results;

import javax.inject.Inject;
import javax.swing.plaf.nimbus.State;
import java.io.*;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static controllers.EditorController.keyLabelMap;
import static controllers.EditorController.typeAheadLabels;

public class PslController extends Controller {

    private static final String KEY_FILE_MLN = "mln";
    private static final String KEY_FILE_RULES = "rules";
    private static final String KEY_FILE_CONSTRAINTS = "constraints";
    private static final String KEY_FILE_DATA = "data";

    private static final String KEY_DATA = "data";
    private static final String KEY_CONSISTENT = "consistent";
    private static final String KEY_CONFLICTING = "conflicting";
    private static final String KEY_RUNTIME = "runtime";

    private static final String RULE = "m.add rule :  ( " +
            " tf(X, '%s', Y, Begin1, End1) " +
            " & tf(X, '%s', Z, Begin2, End2) " +
            " & ~%s(Begin1, End1, Begin2, End2) ) " +
            " >> conflict('%s', X, '%s', Y, Begin1, End1, X, '%s', Z, Begin2, End2), weight : 400.0";

    private static boolean fubar = false;

    private SyncCacheApi cache;

    @Inject
    public PslController(SyncCacheApi cache) {
        this.cache = cache;
    }

    public Result index() {
        return Results.ok(views.html.pslData.render());
    }

    public Result editor() throws IOException, ResourceException, ScriptException {
        Http.MultipartFormData<File> body = request().body().asMultipartFormData();
        Http.MultipartFormData.FilePart<File> dataPart = body.getFile("data");
        Http.MultipartFormData.FilePart<File> constraintsPart = body.getFile("constraints");
        if (dataPart != null) {
            // rules + contraints und daten kopieren
            File constraintsFile = Files.createTempFile("tecore-", ".cstr").toFile();
            copy(constraintsFile, constraintsPart.getFile());
            File dataFile = Files.createTempFile("tecore-", ".db").toFile();
            copy(dataFile, dataPart.getFile());

            List<Statement> statements = Files.lines(dataFile.toPath())
                    .filter(StringUtils::isNoneBlank)
                    .map(s -> s.split(","))
                    .map(s -> new Statement(s[0], s[1], s[2], s[3], s[4]))
                    .collect(Collectors.toList());

            typeAheadLabels.clear();
            if (keyLabelMap.containsKey(statements.get(0).predicate)) {
                typeAheadLabels.addAll(keyLabelMap.values());
            } else {
                for (Statement stmt : statements) {
                    typeAheadLabels.add(stmt.predicate);
                }
            }

            cache.set(KEY_DATA, statements, 30 * 60);
            session(KEY_FILE_CONSTRAINTS, constraintsFile.getAbsolutePath());
            session(KEY_FILE_DATA, dataFile.getAbsolutePath());
        }

        return ok(views.html.editor.render(Reasoner.PSL));
    }

    private void copy(File outFile, File... inFiles) throws IOException {
        try (FileChannel outChannel = new FileOutputStream(outFile, true).getChannel()) {
            for (File inFile : inFiles) {
                try (FileChannel inChannel = new FileInputStream(inFile).getChannel()) {
                    outChannel.transferFrom(inChannel, outChannel.size(), inChannel.size());
                }
            }
        }
    }

    public Result result() throws IOException {
        // add the constraints from the editor, write them to a file and copy everything together
        File constraintsFile = new File(session(KEY_FILE_CONSTRAINTS));
        Map<String, String[]> params = request().body().asFormUrlEncoded();
        String constraints = params.get("uploadedConstraints")[0]
                .replaceAll("\r", "\n")
                .replaceAll("\n\n", "\n")
                .replaceAll("[<>]", "");
        if (StringUtils.isNotBlank(constraints)) {
            File cstrFile = new File(session(KEY_FILE_CONSTRAINTS));
            try (BufferedWriter writer = new BufferedWriter(new FileWriter(cstrFile, true))) {
                writer.newLine();
                writer.append("// === Rules created in the GUI ===\n");
                for (String cstr : constraints.split("\n")) {
                    String[] cstrArr = cstr.split("\t");
                    String subj = cstrArr[0];
                    String pred = cstrArr[1];
                    String obj = cstrArr[2];
                    String rule = String.format(RULE, subj, obj, pred, pred, subj, obj);
                    writer.append(rule);
                    writer.newLine();
                }
            }
        }

        // for whatever reason, groovy/psl remembers the added functions...
        // so we work around this by using a differnet script on the first run
        File rulesStartFile;
        if (!fubar) {
            rulesStartFile = new File("conf/resources/psl/psl.rules.start_first");
            fubar = true;
        } else {
            rulesStartFile = new File("conf/resources/psl/psl.rules.start");
        }
        File rulesEndFile = new File("conf/resources/psl/psl.rules.end");
        File mlnFile = Files.createTempFile("tecore-", ".mln").toFile();
        copy(mlnFile, rulesStartFile, constraintsFile, rulesEndFile);

        File dataFile = new File(session(KEY_FILE_DATA));

        // groovy script ausführen
        File outputFile = Files.createTempFile("tecore-psl-", ".out").toFile();
        System.out.println(mlnFile.getAbsolutePath());

        Binding binding = new Binding();
        String[] args = {dataFile.getAbsolutePath(), outputFile.getAbsolutePath()};
        binding.setProperty("args", args);

        // Creating a new ClassLoader results in duplicate classes and thus ClassCastExceptions,
        // so this did also not function as workaround
//        Set<URL> urls = new HashSet<>();
//        ClassLoader classLoader = getClass().getClassLoader();
//        while (classLoader != null) {
//            if (classLoader instanceof  URLClassLoader) {
//                urls.addAll(Arrays.asList(((URLClassLoader) classLoader).getURLs()));
//            }
//            classLoader = classLoader.getParent();
//        }
//
//        System.out.println(urls);
//
//        GroovyShell shell = new GroovyShell(new URLClassLoader(urls.toArray(new URL[urls.size()])));
        GroovyShell shell = new GroovyShell();
        Script script = shell.parse(mlnFile);
        script.setBinding(binding);
        long start = System.nanoTime();
        script.run();
        long duration = System.nanoTime() - start;
        shell.resetLoadedClasses();

        System.out.println(outputFile.getAbsolutePath());

        Pattern patternTf = Pattern.compile("TF\\('(.*)', '(.*)', '(.*)', '(.*)', '(.*)'\\)\t(\\d?,?\\d?\\d?)");
        Pattern patternConflict = Pattern.compile("CONFLICT\\('(.*)', '(.*)', '(.*)', '(.*)', '(.*)', '(.*)', '(.*)', '(.*)', '(.*)', '(.*)', '(.*)'\\)\t(\\d,?\\d?\\d?)");
        Pattern patternWrong = Pattern.compile("WRONGINTERVAL\\('(.*)', '(.*)', '(.*)', '(.*)', '(.*)'\\)");

        Map<Statement, Double> stmtConfMap = new HashMap<>();

        Set<Statement> all = new HashSet<>();
        Set<Statement> conflicting = new HashSet<>();
        Files.lines(outputFile.toPath())
                .forEach(line -> {
                    if (line.startsWith("TF")) {
                        Matcher m = patternTf.matcher(line);
                        if (m.find()) {
                            String subj = m.group(1);
                            String pred = m.group(2);
                            String obj = m.group(3);
                            String begin = m.group(4);
                            String end = m.group(5);
                            double conf = Double.parseDouble(m.group(6).replaceAll(",", "."));
                            Statement stmt = new Statement(subj, pred, obj, begin, end);
                            stmtConfMap.put(stmt, conf);
                            all.add(stmt);
                        } else {
                            throw new RuntimeException("no match" + line);
                        }
                    } else if (line.startsWith("WRONGINTERVAL")) { // wrongInterval
                        Matcher m = patternWrong.matcher(line);
                        if (m.find()) {
                            String subj = m.group(1);
                            String pred = m.group(2);
                            String obj = m.group(3);
                            String begin = m.group(4);
                            String end = m.group(5);
                            Statement stmt = new Statement(subj, pred, obj, begin, end);
                            conflicting.add(stmt);
                        } else {
                            throw new RuntimeException("no match");
                        }
                    }
                });
        Files.lines(outputFile.toPath())
                .forEach(line -> {
                    if (line.startsWith("CONFLICT")) {
                        Matcher m = patternConflict.matcher(line);
                        if (m.find()) {
                            String subj1 = m.group(2);
                            String pred1 = m.group(3);
                            String obj1 = m.group(4);
                            String begin1 = m.group(5);
                            String end1 = m.group(6);
                            String subj2 = m.group(7);
                            String pred2 = m.group(8);
                            String obj2 = m.group(9);
                            String begin2 = m.group(10);
                            String end2 = m.group(11);
                            String conf = m.group(12);
                            Statement stmt1 = new Statement(subj1, pred1, obj1, begin1, end1);
                            Statement stmt2 = new Statement(subj2, pred2, obj2, begin2, end2);
                            if (!"0".equals(conf) && !conflicting.contains(stmt1) && !conflicting.contains(stmt2)) {
                                double conf1 = stmtConfMap.get(stmt1);
                                double conf2 = stmtConfMap.get(stmt2);
                                if (conf1 < conf2) {
                                    conflicting.add(stmt1);
                                } else {
                                    conflicting.add(stmt2);
                                }
                            }
                        } else {
                            throw new RuntimeException("no match");
                        }
                    }
                });

        Set<Statement> consistent = new HashSet<>(all);
        consistent.removeAll(conflicting);

        cache.set(KEY_CONSISTENT, new ArrayList<>(consistent), 30 * 60);
        cache.set(KEY_CONFLICTING, new ArrayList<>(conflicting), 30 * 60);
        cache.set(KEY_RUNTIME, duration, 30 * 60);

        return ok(views.html.result.render(consistent.size(), conflicting.size(), TimeUnit.NANOSECONDS.toMillis(duration)));
    }

    public Result resultConsistent() throws Exception {
        List<Statement> statements = cache.get(KEY_CONSISTENT);
        if (statements == null) {
            result();
            statements = cache.get(KEY_CONSISTENT);
        }
        DataTablesResponse response = processDataTablesRequest(statements, statements.size());
        return ok(Json.toJson(response));
    }

    public Result resultConflicting() throws Exception {
        List<Statement> statements = cache.get(KEY_CONFLICTING);
        if (statements == null) {
            result();
            statements = cache.get(KEY_CONFLICTING);
        }
        DataTablesResponse response = processDataTablesRequest(statements, statements.size());
        return ok(Json.toJson(response));
    }

    private DataTablesResponse processDataTablesRequest(List<Statement> stmts, int totalSize) {
        try {
            Map<String, String[]> params = request().body().asFormUrlEncoded();
            int draw = Integer.parseInt(params.get("draw")[0]);
            int start = Integer.parseInt(params.get("start")[0]);
            int length = Integer.parseInt(params.get("length")[0]);
            String[] searchValue = params.get("search[value]");
            String[] orderColumn = params.get("order[0][column]");
            String[] orderDir = params.get("order[0][dir]");

            if (length < 0) length = Integer.MAX_VALUE;
            Stream<Statement> stream = stmts.stream();
            if (searchValue != null && StringUtils.isNotBlank(searchValue[0])) {
                stream = stream.filter(s -> contains(s, searchValue[0]));
            }
            if (orderColumn != null && StringUtils.isNotBlank(orderColumn[0])) {
                stream = stream.sorted(comparator(Integer.parseInt(orderColumn[0]), orderDir[0]));
            }
            stream = stream.skip(start)
                    .limit(length);
            List<Statement> statements = stream.collect(Collectors.toList());
            DataTablesResponse response = new DataTablesResponse(draw, totalSize, totalSize, statements, null);
            return response;
        } catch (Exception e) {
            e.printStackTrace();
            Map<String, String[]> params = request().body().asFormUrlEncoded();
            int draw = Integer.parseInt(params.get("draw")[0]);
            DataTablesResponse response = new DataTablesResponse(draw, 0, 0, null, e.getMessage());
            return response;
        }
    }

    private boolean contains(Statement stmt, String searchValue) {
        return stmt.subject.contains(searchValue)
                || stmt.predicate.contains(searchValue)
                || stmt.obj.contains(searchValue)
                || stmt.from.contains(searchValue)
                || stmt.to.contains(searchValue);
    }

    private Comparator<Statement> comparator(int column, String dir) {
        int sign = ("asc".equals(dir.toLowerCase()) ? 1 : -1);
        switch (column) {
            case 0:
                return (s1, s2) -> sign * s1.subject.compareTo(s2.subject);
            case 1:
                return (s1, s2) -> sign * s1.predicate.compareTo(s2.predicate);
            case 2:
                return (s1, s2) -> sign * s1.obj.compareTo(s2.obj);
            case 3:
                return (s1, s2) -> sign * s1.from.compareTo(s2.from);
            case 4:
                return (s1, s2) -> sign * s1.to.compareTo(s2.to);
            default:
                throw new RuntimeException();
        }
    }

    public Result getDataFile() {
        return ok(new File(session(KEY_FILE_DATA)));
    }

    public Result getRulesFile() {
        return ok(new File(session(KEY_FILE_RULES)));
    }

    public Result getConstraintsFile() {
        return ok(new File(session(KEY_FILE_CONSTRAINTS)));
    }

}
