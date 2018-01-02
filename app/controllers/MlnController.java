package controllers;

import com.googlecode.rockit.app.RockItAPI;
import com.googlecode.rockit.app.result.RockItResult;
import models.DataTablesResponse;
import models.Reasoner;
import models.Statement;
import org.apache.commons.lang3.StringUtils;
import play.cache.SyncCacheApi;
import play.libs.Json;
import play.mvc.Controller;
import play.mvc.Http;
import play.mvc.Result;

import javax.inject.Inject;
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

public class MlnController extends Controller {

    private static final String KEY_FILE_MLN = "mln";
    private static final String KEY_FILE_RULES = "rules";
    private static final String KEY_FILE_CONSTRAINTS = "constraints";
    private static final String KEY_FILE_DATA = "data";

    private static final String KEY_DATA = "data";
    private static final String KEY_CONSISTENT = "consistent";
    private static final String KEY_CONFLICTING = "conflicting";
    private static final String KEY_RUNTIME = "runtime";

    private static final String RULE = "!pinst(x, \"%s\", y, i1, i2, valid) v !pinst(x, \"%s\", z, i3, i4, valid) v %s(i1,i2,i3,i4) v sameAs(y,z) .";

    private SyncCacheApi cache;

    @Inject
    public MlnController(SyncCacheApi cache) {
        this.cache = cache;
    }

    public Result index() {
        return ok(views.html.mlnData.render());
    }

    public Result editor() throws Exception {
        Http.MultipartFormData<File> body = request().body().asMultipartFormData();
        Http.MultipartFormData.FilePart<File> dataPart = body.getFile("data");
        Http.MultipartFormData.FilePart<File> constraintsPart = body.getFile("constraints");
        Http.MultipartFormData.FilePart<File> rulesPart = body.getFile("rules");
        if (dataPart != null && rulesPart != null) {
            // Make a copy of all files to have them available after download
            File rulesFile = Files.createTempFile("tecore-", ".rules").toFile();
            copy(rulesFile, rulesPart.getFile());
            File constraintsFile = Files.createTempFile("tecore-", ".cstr").toFile();
            copy(constraintsFile, constraintsPart.getFile());
            File dataFile = Files.createTempFile("tecore-", ".db").toFile();
            copy(dataFile, dataPart.getFile());

            // Merge the rules and the constraints file
            File mlnFile = Files.createTempFile("tecore-", ".mln").toFile();
            copy(mlnFile, rulesPart.getFile(), constraintsPart.getFile());

            System.out.println(mlnFile.getAbsolutePath());

            Pattern pattern = Pattern.compile("pinstConf\\(\"(.*?)\", \"(.*?)\", \"(.*?)\", \"(.*?)\", \"(.*?)\", \"(.*?)\", (.*?)\\)");
            List<Statement> statements = Files.lines(dataFile.toPath())
                    .filter(StringUtils::isNoneBlank)
                    .map(pattern::matcher)
                    .filter(Matcher::find)
                    .map(matcher -> new Statement(
                            matcher.group(1),
                            matcher.group(2),
                            matcher.group(3),
                            matcher.group(4).substring(0, 4),
                            matcher.group(5).substring(0, 4))
                    ).collect(Collectors.toList());

            typeAheadLabels.clear();
            if (keyLabelMap.containsKey(statements.get(0).predicate)) {
                typeAheadLabels.addAll(keyLabelMap.values());
            } else {
                for (Statement stmt : statements) {
                    typeAheadLabels.add(stmt.predicate);
                }
            }

            cache.set(KEY_DATA, statements, 30 * 60);
            session(KEY_FILE_MLN, mlnFile.getAbsolutePath());
            session(KEY_FILE_RULES, rulesFile.getAbsolutePath());
            session(KEY_FILE_CONSTRAINTS, constraintsFile.getAbsolutePath());
            session(KEY_FILE_DATA, dataFile.getAbsolutePath());
        } else {
            flash("error", "Data and rules must be provided");
            return badRequest("Data and rules must be provided");
        }
        return ok(views.html.editor.render(Reasoner.MLN));
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

    public Result result() throws Exception {
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
                    String rule = String.format(RULE, subj, obj, pred);
                    writer.append(rule);
                    writer.newLine();
                }
            }
        }

        long start = System.nanoTime();
        List<RockItResult> rockitResults = new RockItAPI().doMapState(session().get(KEY_FILE_MLN), session().get(KEY_FILE_DATA));
        long duration = System.nanoTime() - start;
        Pattern pattern = Pattern.compile("pinst\\(\"(.*?)\", \"(.*?)\", \"(.*?)\", \"(.*?)\", \"(.*?)\", \"(.*?)\"\\)");
        List<Statement> consistent = rockitResults.stream()
                .map(RockItResult::getStatement)
                .map(pattern::matcher)
                .map(matcher -> {
                    matcher.find();
                    return new Statement(
                            matcher.group(1),
                            matcher.group(2),
                            matcher.group(3),
                            matcher.group(4).substring(0, 4),
                            matcher.group(5).substring(0, 4));
                }).collect(Collectors.toList());

        List<Statement> all = cache.getOrElseUpdate(KEY_DATA, Collections::emptyList);
        List<Statement> conflicting = new ArrayList<>(all);
        conflicting.removeAll(consistent);

        cache.set(KEY_CONSISTENT, consistent, 30 * 60);
        cache.set(KEY_CONFLICTING, conflicting, 30 * 60);
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
