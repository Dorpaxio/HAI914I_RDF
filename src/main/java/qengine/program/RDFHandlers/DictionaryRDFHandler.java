package qengine.program.RDFHandlers;

import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.rio.RDFHandlerException;
import org.eclipse.rdf4j.rio.helpers.AbstractRDFHandler;
import qengine.program.Models.Dictionary;

public class DictionaryRDFHandler extends AbstractRDFHandler {

    private final Dictionary dictionary;

    public DictionaryRDFHandler(Dictionary dictionary) {
        this.dictionary = dictionary;
    }

    @Override
    public void handleStatement(Statement st) throws RDFHandlerException {
        super.handleStatement(st);

        this.dictionary.put(st.getSubject().stringValue());
        this.dictionary.put(st.getPredicate().stringValue());
        this.dictionary.put(st.getObject().stringValue());
    }
}
