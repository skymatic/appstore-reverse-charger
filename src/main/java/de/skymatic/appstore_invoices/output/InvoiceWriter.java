package de.skymatic.appstore_invoices.output;

import de.skymatic.appstore_invoices.model.Invoice;
import de.skymatic.appstore_invoices.template.Template;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Collection;

public interface InvoiceWriter {

	default void write(Path outputDirectory, Template template, Collection<? extends Invoice> invoices) throws IOException {
		for(var invoice : invoices) {
			StringBuilder sb = template.fillWithData(invoice); //TODO
			Path p = outputDirectory.resolve("invoice-" + invoice.getId() + ".html");
			Files.writeString(p,template.fillWithData(invoice), StandardOpenOption.CREATE, StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING);
		}
	}

	static InvoiceWriter createInvoiceGenerator(OutputFormat o) {
		switch (o) {
			case HTML:
				return new HTMLWriter();
			default:
				throw new IllegalArgumentException("Unknown Outputformat.");
		}
	}

	enum OutputFormat {
		HTML;
	}

}
