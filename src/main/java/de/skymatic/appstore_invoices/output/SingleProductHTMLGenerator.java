package de.skymatic.appstore_invoices.output;

import de.skymatic.appstore_invoices.model.Invoice;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;

public class SingleProductHTMLGenerator {

	private static final String PLACEHOLDER_START = "{{";
	private static final String PLACEHOLDER_END = "}}";
	private static final DateTimeFormatter formatter = DateTimeFormatter.ofLocalizedDate(FormatStyle.SHORT);
	private static final NumberFormat numFormatter;

	static {
		NumberFormat tmp = NumberFormat.getInstance();
		if(tmp instanceof DecimalFormat){
			((DecimalFormat) tmp).applyPattern("#,##0.0#;(#)");
			numFormatter = tmp;
		} else {
			numFormatter = new DecimalFormat("#,##0.0#;(#)");
		}
	}

	public Map<String, StringBuilder> createHTMLInvoices(Path templatePath, Collection<Invoice> invoices) throws IOException, MalformedTemplateException {
		assert invoices.stream().allMatch(i -> i.size() == 1);

		Map<String, StringBuilder> htmlInvoices = new HashMap<>();
		invoices.forEach(i -> htmlInvoices.put(i.getId(), new StringBuilder()));
		ParseState state = ParseState.REGULAR;
		try (BufferedReader br = Files.newBufferedReader(templatePath)) {
			//dependent on the state do something
			br.mark(2);
			while (br.read() != -1) {
				br.reset();
				Map<String, StringBuilder> intermediate;
				switch (state) {
					case REGULAR -> {
						intermediate = seekAndReplace(br, invoices);
						state = ParseState.GLOBAL_ITEM_TEMPLATE;
					}
					case GLOBAL_ITEM_TEMPLATE -> {
						intermediate = devourDigestDischarge(br, invoices);
						state = ParseState.REGULAR;
					}
					default -> throw new IllegalStateException("Not a valid state"); //TODO: better error description
				}
				intermediate.forEach((id, sb) -> htmlInvoices.get(id).append(sb));
				br.mark(2);
			}
		}

		return htmlInvoices;
	}

	private Map<String, StringBuilder> devourDigestDischarge(BufferedReader br, Collection<Invoice> invoices) throws IOException, MalformedTemplateException {
		Map<String, StringBuilder> replacements = new HashMap<>();
		invoices.forEach(i -> replacements.put(i.getId(), new StringBuilder()));

		//read in template
		StringBuilder template = devour(br);

		//digest
		String descriptionPlaceholder = PLACEHOLDER_START + " " + Placeholder.GLOBAL_ENTRY_DESCRIPTION.toString() + " " + PLACEHOLDER_END;
		String valuePlaceholder = PLACEHOLDER_START + " " + Placeholder.GLOBAL_ENTRY_VALUE.toString() + " " + PLACEHOLDER_END;
		int startPosDescription = template.indexOf(descriptionPlaceholder);
		int startPosValue = template.indexOf(valuePlaceholder);

		Placeholder first;
		Placeholder second;
		int firstReplaceStart;
		int firstReplaceEnd;
		int secondReplaceStart;
		int secondReplaceEnd;
		if (startPosDescription < startPosValue) {
			first = Placeholder.GLOBAL_ENTRY_DESCRIPTION;
			firstReplaceStart = startPosDescription;
			firstReplaceEnd = startPosDescription + descriptionPlaceholder.length();
			second = Placeholder.GLOBAL_ENTRY_VALUE;
			secondReplaceStart = startPosValue;
			secondReplaceEnd = startPosValue + valuePlaceholder.length();
		} else {
			first = Placeholder.GLOBAL_ENTRY_VALUE;
			firstReplaceStart = startPosValue;
			firstReplaceEnd = startPosValue + valuePlaceholder.length();
			second = Placeholder.GLOBAL_ENTRY_DESCRIPTION;
			secondReplaceStart = startPosDescription;
			secondReplaceEnd = startPosDescription + descriptionPlaceholder.length();
		}

		for (var invoice : invoices) {
			invoice.getGlobalItems().forEach((desc, val) -> {
						replacements.get(invoice.getId())
								.append(template, 0, firstReplaceStart)
								.append(first == Placeholder.GLOBAL_ENTRY_DESCRIPTION ? desc : numFormatter.format(val))
								.append(template, firstReplaceEnd, secondReplaceStart)
								.append(second == Placeholder.GLOBAL_ENTRY_VALUE ? numFormatter.format(val) : desc)
								.append(template, secondReplaceEnd + 1, template.length())
								.append("\n");
					}
			);
		}

		//discharge
		return replacements;
	}

	private StringBuilder devour(BufferedReader br) throws IOException, MalformedTemplateException {
		StringBuilder template = new StringBuilder();
		String line = br.readLine();
		while (line != null) {
			if (line.contains(PLACEHOLDER_START + " " + Placeholder.GLOBAL_ENTRY_TEMPLATE_END + " " + PLACEHOLDER_END)) {
				//we stop
				return template;
			} else {
				template.append(line).append("\n");
			}
			line = br.readLine();
		}
		//if we reach this line, the template section did not get closed
		throw new MalformedTemplateException("HTML template for global items is not closed."); //TODO: meaningful message
	}

	private Map<String, StringBuilder> seekAndReplace(BufferedReader br, Collection<Invoice> invoices) throws IOException, MalformedTemplateException {
		Map<String, StringBuilder> replacements = new HashMap<>();
		invoices.forEach(i -> replacements.put(i.getId(), new StringBuilder()));

		String line = br.readLine();
		while (line != null) {
			//consume all lines as long as you don't find GLOBAL_ENTRY_START
			if (line.contains(PLACEHOLDER_START + " " + Placeholder.GLOBAL_ENTRY_TEMPLATE_START.toString() + " " + PLACEHOLDER_END)) {
				//line contains global entry start, so we return
				break;
			} else {
				//check if line contains placeholder
				int posOfPlaceholderStart = line.indexOf(PLACEHOLDER_START);
				int posOfPlaceholderEnd = line.indexOf(PLACEHOLDER_END);
				int posOfLastPlaceholderEnd = 0;
				while (posOfPlaceholderStart > -1 && posOfPlaceholderEnd > -1) {
					//check if placeholder is one of ours
					try {
						Placeholder p = parsePlaceholder(line.substring(posOfPlaceholderStart + 2, posOfPlaceholderEnd).trim(), ParseState.REGULAR);
						//replace placeholder for each invoice with regarding value
						for (var invoice : invoices) {
							replacements.get(invoice.getId())
									.append(line, posOfLastPlaceholderEnd, posOfPlaceholderStart)
									.append(getReplacementForRegular(p, invoice));
						}
					} catch (IllegalStateException e) {
						//wrong Placeholder. Throw some shit
						throw new MalformedTemplateException(e);
					} catch (IllegalArgumentException e) {
						//not our shit. Skip only this entry.
					}
					//update position of placeholders
					posOfLastPlaceholderEnd = posOfPlaceholderEnd + 2;
					posOfPlaceholderStart = line.indexOf(PLACEHOLDER_START, posOfLastPlaceholderEnd);
					posOfPlaceholderEnd = line.indexOf(PLACEHOLDER_END, posOfPlaceholderStart);
					//repeat
				}
				//Just append rest of line
				for (var invoice : invoices) {
					replacements.get(invoice.getId())
							.append(line, posOfLastPlaceholderEnd, line.length())
							.append("\n");
				}
			}
			line = br.readLine();
		}
		return replacements;
	}

	private Placeholder parsePlaceholder(String placeholderName, ParseState state) {
		Placeholder p = Placeholder.valueOf(placeholderName);
		if (state.getPlaceholders().contains(p)) {
			//everything cool, proceed
			return p;
		} else {
			//throw shiat
			throw new IllegalStateException("Shiat, u mad?"); //TODO: better message
		}
	}

	private String getReplacementForRegular(Placeholder placeholder, Invoice invoice) {
		switch (placeholder) {
			case SUBSIDIARY_INFORMATION:
				return Arrays.stream(invoice.getRecipient().getAddress()) //
						.reduce("", (address, address_entry) -> address + "<br>" + address_entry);
			case PRODUCT_AMOUNT:
				return String.valueOf(invoice.totalUnits());
			case INVOICE_NUMBER:
				return String.valueOf(invoice.getId());
			case PRODUCT_PROCEEDS:
				return numFormatter.format(invoice.proceeds());
			case ISSUE_DATE:
				return invoice.getIssueDate().format(formatter);
			case SALES_PERIOD_START:
				return invoice.getStartOfPeriod().format(formatter);
			case SALES_PERIOD_END:
				return invoice.getEndOfPeriod().format(formatter);
			default:
				throw new IllegalArgumentException(); //NO-OP
		}
	}

	private enum ParseState {

		GLOBAL_ITEM_TEMPLATE(Placeholder.GLOBAL_ENTRY_DESCRIPTION,
				Placeholder.GLOBAL_ENTRY_VALUE),
		REGULAR(Placeholder.INVOICE_NUMBER,
				Placeholder.ISSUE_DATE,
				Placeholder.PRODUCT_AMOUNT,
				Placeholder.PRODUCT_PROCEEDS,
				Placeholder.SALES_PERIOD_END,
				Placeholder.SALES_PERIOD_START,
				Placeholder.SUBSIDIARY_INFORMATION);

		private final EnumSet<Placeholder> validPlaceholders;

		ParseState(Placeholder... validPlaceholders) {
			if (validPlaceholders.length == 0) {
				this.validPlaceholders = EnumSet.noneOf(Placeholder.class);
			} else {
				this.validPlaceholders = EnumSet.of(validPlaceholders[0], validPlaceholders);
			}
		}

		Collection<Placeholder> getPlaceholders() {
			return Collections.unmodifiableSet(validPlaceholders);
		}
	}

}