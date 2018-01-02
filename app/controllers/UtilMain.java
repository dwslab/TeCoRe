package controllers;

import org.apache.commons.lang3.StringUtils;
import org.jooq.lambda.Unchecked;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.DecimalFormat;
import java.util.HashSet;
import java.util.Set;
import java.util.StringJoiner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class UtilMain {


    public static void main(String[] args) throws Exception {
        String f0 = "conf/resources/psl/football/player_team_year_rockit_0.csv";
        String f0_out = "conf/resources/psl/football/player_team_year_rockit_0_new.csv";
        String f10 = "conf/resources/psl/football/player_team_year_rockit_10.csv";
        String f10_out = "conf/resources/psl/football/player_team_year_rockit_10_new.csv";
        String f25 = "conf/resources/psl/football/player_team_year_rockit_25.csv";
        String f25_out = "conf/resources/psl/football/player_team_year_rockit_25_new.csv";
        String f50 = "conf/resources/psl/football/player_team_year_rockit_50.csv";
        String f50_out = "conf/resources/psl/football/player_team_year_rockit_50_new.csv";
        String f75 = "conf/resources/psl/football/player_team_year_rockit_75.csv";
        String f75_out = "conf/resources/psl/football/player_team_year_rockit_75_new.csv";
        String f100 = "conf/resources/psl/football/player_team_year_rockit_100.csv";
        String f100_out = "conf/resources/psl/football/player_team_year_rockit_100_new.csv";

//        convert(f0, f0_out);
//        convert(f10, f10_out);
//        convert(f25, f25_out);
//        convert(f50, f50_out);
//        convert(f75, f75_out);
//        convert(f100, f100_out);

        filter();

    }


    public static void convertWiki() throws IOException {
        Files.walk(Paths.get("conf/resources/rockit/wikidata/"))
                .map(Path::toFile)
                .filter(File::isFile)
                .map(File::toPath)
                .forEach(Unchecked.consumer(file -> doConvert(file)));
    }

    public static void doConvert(Path file) throws IOException {
        String name = file.toFile().getName();
        String outPath = "conf/resources/psl/wikidata/";

        Pattern pattern = Pattern.compile("pinstConf\\(\"(.*)\", \"(.*)\", \"(.*)\", \"(.*)\", \"(.*)\", \".*\", (.*)\\)");

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(outPath + name))) {
            Files.lines(file, StandardCharsets.ISO_8859_1)
                    .filter(StringUtils::isNoneBlank)
                    .forEach(Unchecked.consumer(l -> {
                        Matcher m = pattern.matcher(l);
                        if (m.find()) {
                            String subj = m.group(1);
                            String pred = m.group(2);
                            String obj = m.group(3);
                            String begin = m.group(4);
                            String end = m.group(5);
                            String weight = m.group(6);

                            double conf = 1 / (1 + Math.exp(Double.parseDouble(weight)));
                            DecimalFormat formatter = new DecimalFormat("#.##");


                            writer.write(subj + "," + pred + "," + obj + "," + begin + "," + end + "," + formatter.format(conf).replaceAll(",", "."));
                            writer.newLine();
                        } else {
                            if (!l.contains("sameAs")) {
                                System.err.println("not matching " + l);
                            }
                        }
                    }));
        }
    }


    public static void filter() throws IOException {
        Files.walk(Paths.get("conf/resources/psl/wikidata/"))
                .map(Path::toFile)
                .filter(File::isFile)
                .map(File::toPath)
                .forEach(Unchecked.consumer(file -> {
                    String in  = file.toFile().getAbsolutePath();
                    String out = file.toFile().getAbsolutePath().replace(".csv", "_new.csv");
                    convert(in, out);
                }));
    }

    public static void convert(String in, String out) throws IOException {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(out))) {
            Set<String> lines = new HashSet<>();
            Files.lines(Paths.get(in))
                    .map(s -> s.split(","))
                    .forEach(Unchecked.consumer(s -> {
                        if (!lines.contains(s[0] + s[1] + s[2] + s[3] + s[4])) {
                            lines.add(s[0] + s[1] + s[2] + s[3] + s[4]);
                            StringJoiner joiner = new StringJoiner(",", "", "\n");
                            joiner.add(s[0]);
                            joiner.add(s[1]);
                            joiner.add(s[2]);
                            joiner.add(s[3]);
                            joiner.add(s[4]);
                            joiner.add(s[5]);
                            writer.write(joiner.toString());
                        }
                    }));
        }
    }

}
