package qengine.program;

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

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
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

    /**
     * Fichier contenant les requêtes sparql
     */
    static final String queryFile = workingDir + "STAR_ALL_workload.queryset";

    /**
     * Fichier contenant des données rdf
     */
    static final String dataFile = workingDir + "100K.nt";

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
                projection.getProjectionElemList().getElements().forEach(projectionElem -> {
                    projectionElems.computeIfAbsent(projectionElem.getSourceName(), el -> new HashSet<>());
                });
            }
        });

        for (int i = 0; i < patterns.size(); i++) {
            final Map.Entry<String, Set<Integer>> resultMap = getResultsFromPattern(patterns.get(i), dictionary, index);
            if (i == 0) {
                projectionElems.get(resultMap.getKey()).addAll(resultMap.getValue());
            } else {
                projectionElems.get(resultMap.getKey()).retainAll(resultMap.getValue());
            }
        }

        System.out.println(projectionElems);

    }

    private static Map.Entry<String, Set<Integer>> getResultsFromPattern(StatementPattern pattern, Dictionary dictionary, Index index) {
        final Set<Integer> results = new HashSet<>();
        final Var subjectVar = pattern.getSubjectVar();
        final Var predicateVar = pattern.getPredicateVar();
        final Var objectVar = pattern.getObjectVar();

        final int subject = getIdFromValue(subjectVar.getValue(), dictionary);
        final int predicate = getIdFromValue(predicateVar.getValue(), dictionary);
        final int object = getIdFromValue(objectVar.getValue(), dictionary);

        final String varName;

        Index.Order order = Index.Order.getBestOrder(subject, predicate, object);
        results.addAll(index.getResults(subject, predicate, object, order));
        if (subject == -1) varName = subjectVar.getName();
        else if (predicate == -1) varName = predicateVar.getName();
        else varName = objectVar.getName();

        return new AbstractMap.SimpleEntry<>(varName, results);
    }

    private static int getIdFromValue(Value value, Dictionary dictionary) {
        if (value == null) return -1;
        return Optional.ofNullable(dictionary.get(value.stringValue())).orElse(-1);
    }

    /**
     * Entrée du programme
     */
    public static void main(String[] args) throws Exception {
        Dictionary dictionary = new Dictionary();
        Index index = new Index();

        parseData(dictionary, index);
        parseQueries(dictionary, index);
    }

    // ========================================================================

    /**
     * Traite chaque requête lue dans {@link #queryFile} avec {@link #processAQuery(ParsedQuery)}.
     */
    private static void parseQueries(Dictionary dictionary, Index index) throws FileNotFoundException, IOException {
        /**
         * Try-with-resources
         *
         * @see <a href="https://docs.oracle.com/javase/tutorial/essential/exceptions/tryResourceClose.html">Try-with-resources</a>
         */
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
    private static void parseData(Dictionary dictionary, Index index) throws IOException {
        parse(new DictionaryRDFHandler(dictionary));
        parse(new IndexRDFHandler(index, dictionary));
    }

    private static void parse(AbstractRDFHandler abstractRDFHandler) throws FileNotFoundException, IOException {
        try (Reader dataReader = new FileReader(dataFile)) {
            // On va parser des données au format ntriples
            RDFParser rdfParser = Rio.createParser(RDFFormat.NTRIPLES);

            // On utilise notre implémentation de handler
            rdfParser.setRDFHandler(abstractRDFHandler);

            // Parsing et traitement de chaque triple par le handler
            rdfParser.parse(dataReader, baseURI);
        }
    }
}
