/*
 * Copyright (c) 2026 cparedes. Todos los derechos reservados.
 */
package com.cparedesr.dockia.agents.extractmetadata;

import org.apache.tika.exception.TikaException;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.parser.AutoDetectParser;
import org.apache.tika.parser.ParseContext;
import org.apache.tika.parser.Parser;
import org.apache.tika.sax.BodyContentHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.io.InputStream;

@Component
public class DocumentTextExtractor {

    private static final Logger LOGGER = LoggerFactory.getLogger(DocumentTextExtractor.class);

    private final Parser parser = new AutoDetectParser();
    private final AgentProperties properties;

    public DocumentTextExtractor(AgentProperties properties) {
        this.properties = properties;
    }

    public String extract(InputStream inputStream) throws IOException, TikaException, SAXException {
        int maxChars = Math.max(1000, properties.getContentMaxChars());
        BodyContentHandler handler = new BodyContentHandler(maxChars + 1);
        Metadata metadata = new Metadata();
        ParseContext context = new ParseContext();

        try {
            parser.parse(inputStream, handler, metadata, context);
        } catch (SAXException e) {
            if (handler.toString().isBlank()) {
                throw e;
            }
            LOGGER.debug("Extraccion de texto truncada por limite de caracteres", e);
        }

        String text = handler.toString().trim();
        return text.length() > maxChars ? text.substring(0, maxChars) : text;
    }
}
