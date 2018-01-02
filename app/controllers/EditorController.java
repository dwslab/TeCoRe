package controllers;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import org.apache.commons.lang3.StringUtils;
import play.libs.Json;
import play.mvc.Controller;
import play.mvc.Result;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.Set;

public class EditorController extends Controller {

    public static final BiMap<String, String> keyLabelMap = HashBiMap.create();
    public static final Set<String> typeAheadLabels = new HashSet<>();

    static {
        try {
            Files.lines(Paths.get("conf/resources/wikidata_properties.csv"))
                    .skip(1)
                    .map(l -> l.split(","))
                    .forEach(r -> keyLabelMap.put(StringUtils.substringAfterLast(r[0].trim(), "/"), r[1].trim()));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

    }

    public Result getPredicates() {
        return ok(Json.toJson(typeAheadLabels));
    }

}
