package qengine.program;

import com.opencsv.CSVWriter;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.query.algebra.Projection;
import org.eclipse.rdf4j.query.algebra.StatementPattern;
import org.eclipse.rdf4j.query.algebra.Var;
import org.eclipse.rdf4j.query.algebra.helpers.AbstractQueryModelVisitor;
import org.eclipse.rdf4j.query.algebra.helpers.StatementPatternCollector;
import org.eclipse.rdf4j.query.parser.ParsedQuery;
import org.eclipse.rdf4j.query.parser.sparql.SPARQLParser;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.RDFParser;
import org.eclipse.rdf4j.rio.Rio;
import org.eclipse.rdf4j.rio.helpers.AbstractRDFHandler;
import qengine.program.Models.Dictionary;
import qengine.program.Models.Index;
import qengine.program.RDFHandlers.DictionaryRDFHandler;
import qengine.program.RDFHandlers.IndexRDFHandler;
import qengine.program.RDFHandlers.MainRDFHandler;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.IntFunction;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Programme simple lisant un fichier de requête et un fichier de données.
 *
 * <p>
 * Les entrées sont données ici de manière statique,
 * à vous de programmer les entrées par passage d'arguments en ligne de commande comme demandé dans l'énoncé.
 * </p>
 *
 * <p>
 * Le présent programme se contente de vous montrer la voie pour lire les triples et requêtes
 * depuis les fichiers ; ce sera à vous d'adapter/réécrire le code pour finalement utiliser les requêtes et interroger les données.
 * On ne s'attend pas forcémment à ce que vous gardiez la même structure de code, vous pouvez tout réécrire.
 * </p>
 *
 * @author Olivier Rodriguez <olivier.rodriguez1@umontpellier.fr>
 */
final class Main {
    static final String baseURI = null;

    /**
     * Votre répertoire de travail où vont se trouver les fichiers à lire
     */
    static final String workingDir = "data/";

    static final String outputDir = "output/";

    /**
     * Fichier contenant les requêtes sparql
     */
    static final String queryFile = workingDir + "STAR_ALL_workload.queryset";

    /**
     * Fichier contenant des données rdf
     */
    static final String dataFile = workingDir + "100K.nt";

    static final String outputFile = outputDir + "export.csv";

    static final List<List<Integer>> allResults = new ArrayList<>();

    // ========================================================================

    /**
     * Méthode utilisée ici lors du parsing de requête sparql pour agir sur l'objet obtenu.
     */
    public static void processAQuery(ParsedQuery query, Dictionary dictionary, Index index) {
        final List<StatementPattern> patterns = StatementPatternCollector.process(query.getTupleExpr());

        // {vO: [23923, 20323], v1: [2322, 2, 999]} -> Solutions possible pour chaque élément
        final Map<String, Set<Integer>> projectionElems = new HashMap<>();
        query.getTupleExpr().visit(new AbstractQueryModelVisitor<RuntimeException>() {
            public void meet(Projection projection) {
                projection.getProjectionElemList().getElements().forEach(projectionElem ->
                        projectionElems.computeIfAbsent(projectionElem.getSourceName(), el -> new HashSet<>()));
            }
        });

        final List<Integer> results = join(patterns, dictionary, index);

        projectionElems.get("v0").addAll(results);
        allResults.add(results);

        System.out.println(projectionElems);

        // Benchmarking
        /*long start = System.currentTimeMillis();
        for (int i = 0; i < 1000000; i++) {
            projectionElems.get("v0").addAll(join(patterns, dictionary, index));
            projectionElems.get("v0").clear();
        }
        System.out.println("Sort-Merge-Join : " + (System.currentTimeMillis() - start) + "ms");

        start = System.currentTimeMillis();
        for (int i = 0; i < 1000000; i++) {
            projectionElems.get("v0").addAll(intersect(patterns, dictionary, index));
            projectionElems.get("v0").clear();
        }
        System.out.println("Intersect : " + (System.currentTimeMillis() - start) + "ms");*/
    }

    private static List<Integer> intersect(List<StatementPattern> patterns, Dictionary dictionary, Index index) {
        final List<Integer> results = getResultsFromPattern(patterns.get(0), dictionary, index).getValue();

        if (patterns.size() > 1) {
            for (StatementPattern pattern : patterns.subList(1, patterns.size())) {
                results.retainAll(getResultsFromPattern(pattern, dictionary, index).getValue());
            }
        }

        return results;
    }

    private static List<Integer> join(List<StatementPattern> patterns, Dictionary dictionary, Index index) {
        final List<Integer> results = getResultsFromPattern(patterns.get(patterns.size() - 1), dictionary, index).getValue();
        if (results.size() == 0 || patterns.size() < 2) return results;

        return merge(results, join(patterns.subList(0, patterns.size() - 1), dictionary, index));
    }

    private static List<Integer> merge(List<Integer> list1, List<Integer> list2) {
        final List<Integer> output = new ArrayList<>();
        final int size1 = list1.size(), size2 = list2.size();
        int id1 = 0, id2 = 0;

        while (id1 < size1 && id2 < size2) {
            int value1 = list1.get(id1);
            int value2 = list2.get(id2);
            if (value1 == value2) {
                output.add(value1);
                id1++;
                id2++;
            } else if (value1 < value2) {
                id1++;
            } else {
                id2++;
            }
        }

        return output;
    }

    private static Map.Entry<String, List<Integer>> getResultsFromPattern(StatementPattern pattern, Dictionary dictionary, Index index) {
        final Var subjectVar = pattern.getSubjectVar();
        final Var predicateVar = pattern.getPredicateVar();
        final Var objectVar = pattern.getObjectVar();

        final int subject = getIdFromValue(subjectVar.getValue(), dictionary);
        final int predicate = getIdFromValue(predicateVar.getValue(), dictionary);
        final int object = getIdFromValue(objectVar.getValue(), dictionary);

        final String varName;

        Index.Order order = Index.Order.getBestOrder(subject, predicate, object);
        final List<Integer> results = new ArrayList<>(index.getResults(subject, predicate, object, order));

        if (subject == -1) varName = subjectVar.getName();
        else if (predicate == -1) varName = predicateVar.getName();
        else varName = objectVar.getName();

        return new AbstractMap.SimpleEntry<>(varName, results);
    }

    private static int getIdFromValue(Value value, Dictionary dictionary) {
        if (value == null) return -1;
        return Optional.ofNullable(dictionary.get(value.stringValue())).orElse(-1);
    }

    final static Map<Option, BiConsumer<String, Map<String, String>>> optionsConsumers = new HashMap<>();

    static {
        optionsConsumers.put(Option.DATA, (arg, map) -> map.put("data", arg));
        optionsConsumers.put(Option.QUERIES, (arg, map) -> map.put("queries", arg));
        optionsConsumers.put(Option.OUTPUT, (arg, map) -> map.put("output", arg));
    }

    /**
     * Entrée du programme
     */
    public static void main(String[] args) throws Exception {
        final Map<String, String> executionDatas = new HashMap<>();
        for (Option option : Option.values()) {
            for (int i = 0; i < args.length; i++) {
                final String arg = args[i];
                if (arg.startsWith("-") && arg.substring(1).equals(option.getName())) {
                    final BiConsumer<String, Map<String, String>> consumer = optionsConsumers.get(option);
                    if (consumer != null) {
                        if (option.isRequireArgument() && i >= args.length - 1) {
                            throw new Exception("Il manque un argument à l'option " + option.getName());
                        }
                        consumer.accept(option.isRequireArgument() ? args[i + 1] : null, executionDatas);
                    }
                }
            }
        }

        long debut = System.currentTimeMillis();
        Dictionary dictionary = new Dictionary();
        Index index = new Index();

        parseData(Optional.ofNullable(executionDatas.get("data")).orElse(dataFile), dictionary, index);
        parseQueries(Optional.ofNullable(executionDatas.get("queries")).orElse(queryFile), dictionary, index);
        System.out.println("Temps d'exécution : " + (System.currentTimeMillis() - debut) + "ms");

        export(Optional.ofNullable(executionDatas.get("output")).orElse(outputFile));
    }

    // ========================================================================

    private static void parseQueries(String queryFile, Dictionary dictionary, Index index) throws IOException {
        /*
         * On utilise un stream pour lire les lignes une par une, sans avoir à toutes les stocker
         * entièrement dans une collection.
         */
        try (Stream<String> lineStream = Files.lines(Paths.get(queryFile))) {
            SPARQLParser sparqlParser = new SPARQLParser();
            Iterator<String> lineIterator = lineStream.iterator();
            StringBuilder queryString = new StringBuilder();

            while (lineIterator.hasNext())
                /*
                 * On stocke plusieurs lignes jusqu'à ce que l'une d'entre elles se termine par un '}'
                 * On considère alors que c'est la fin d'une requête
                 */ {
                String line = lineIterator.next();
                queryString.append(line);

                if (line.trim().endsWith("}")) {
                    ParsedQuery query = sparqlParser.parseQuery(queryString.toString(), baseURI);

                    processAQuery(query, dictionary, index); // Traitement de la requête, à adapter/réécrire pour votre programme

                    queryString.setLength(0); // Reset le buffer de la requête en chaine vide
                }
            }
        }
    }

    /**
     * Traite chaque triple lu dans {@link #dataFile} avec {@link MainRDFHandler}.
     */
    private static void parseData(String dataFile, Dictionary dictionary, Index index) throws IOException {
        parse(dataFile, new DictionaryRDFHandler(dictionary));
        parse(dataFile, new IndexRDFHandler(index, dictionary));
    }

    private static void parse(String dataFile, AbstractRDFHandler abstractRDFHandler) throws IOException {
        try (Reader dataReader = new FileReader(dataFile)) {
            // On va parser des données au format ntriples
            RDFParser rdfParser = Rio.createParser(RDFFormat.NTRIPLES);

            // On utilise notre implémentation de handler
            rdfParser.setRDFHandler(abstractRDFHandler);

            // Parsing et traitement de chaque triple par le handler
            rdfParser.parse(dataReader, baseURI);
        }
    }

    private static void export(String exportFile) throws IOException {
        final File file = new File(exportFile);
        file.getParentFile().mkdirs();
        file.createNewFile();
        final CSVWriter writer = new CSVWriter(new OutputStreamWriter(new FileOutputStream(exportFile),
                StandardCharsets.UTF_8),
                ';',
                CSVWriter.DEFAULT_QUOTE_CHARACTER,
                CSVWriter.DEFAULT_ESCAPE_CHARACTER,
                CSVWriter.DEFAULT_LINE_END
        );

        final List<String[]> allResultsString = allResults.stream().map(results ->
                        results.stream().map(Object::toString).toArray(String[]::new))
                .collect(Collectors.toList());

        writer.writeAll(allResultsString);
        writer.close();
    }
}
