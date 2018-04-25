package com.agosense.fidelia.plugin.word.server;

import org.xml.sax.Attributes;

public class SaxStackElement {

    private final transient String qName;
    private final transient Attributes attributes;

    public SaxStackElement(String qName, Attributes attributes) {
        this.qName = qName;
        this.attributes = attributes;
    }

    public String getQName() {
        return qName;
    }

    public Attributes getAttributes() {
        return attributes;
    }
}

