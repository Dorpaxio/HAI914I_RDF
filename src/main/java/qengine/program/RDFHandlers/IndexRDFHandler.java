package qengine.program.RDFHandlers;

import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.rio.RDFHandlerException;
import org.eclipse.rdf4j.rio.helpers.AbstractRDFHandler;
import qengine.program.Models.Dictionary;
import qengine.program.Models.Index;

public class IndexRDFHandler extends AbstractRDFHandler {

    private final Index index;
    private final Dictionary dictionary;

    public IndexRDFHandler(Index index, Dictionary dictionary) {
        this.index = index;
        this.dictionary = dictionary;
    }

    @Override
    public void handleStatement(Statement st) throws RDFHandlerException {
        super.handleStatement(st);

        int subjectId = dictionary.get(st.getSubject().stringValue());
        int predicatedId = dictionary.get(st.getPredicate().stringValue());
        int objectId = dictionary.get(st.getObject().stringValue());

        this.index.put(subjectId, predicatedId, objectId);
    }
}
