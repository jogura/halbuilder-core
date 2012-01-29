package com.theoryinpractise.halbuilder.json;

import com.theoryinpractise.halbuilder.ReadableResource;
import com.theoryinpractise.halbuilder.ResourceException;
import com.theoryinpractise.halbuilder.ResourceFactory;
import com.theoryinpractise.halbuilder.ResourceReader;
import com.theoryinpractise.halbuilder.resources.MutableResource;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.ObjectMapper;

import java.io.Reader;
import java.util.Iterator;
import java.util.Map;

public class JsonResourceReader implements ResourceReader {
    private ResourceFactory resourceFactory;

    public JsonResourceReader(ResourceFactory resourceFactory) {
        this.resourceFactory = resourceFactory;
    }

    public ReadableResource read(Reader reader) {
        try {
            ObjectMapper mapper = new ObjectMapper();

            JsonNode rootNode = mapper.readValue(reader, JsonNode.class);

            MutableResource resource = readResource(rootNode);


            return resource.asImmutableResource();
        } catch (Exception e) {
            throw new ResourceException(e);
        }

    }

    private MutableResource readResource(JsonNode rootNode) {
        String href = rootNode.get("_href").asText();
        MutableResource resource = new MutableResource(resourceFactory, href);

        readNamespaces(resource, rootNode);
        readLinks(resource, rootNode);
        readProperties(resource, rootNode);
        readResources(resource, rootNode);
        return resource;
    }

    private void readNamespaces(MutableResource resource, JsonNode rootNode) {
        if (rootNode.has("_curies")) {
            Iterator<Map.Entry<String, JsonNode>> fields = rootNode.get("_curies").getFields();
            while (fields.hasNext()) {
                Map.Entry<String, JsonNode> next = fields.next();
                resource.withNamespace(next.getKey(), next.getValue().asText());
            }
        }
    }

    private void readLinks(MutableResource resource, JsonNode rootNode) {
        if (rootNode.has("_links")) {
            Iterator<Map.Entry<String, JsonNode>> fields = rootNode.get("_links").getFields();
            while (fields.hasNext()) {
                Map.Entry<String, JsonNode> keyNode = fields.next();
                if (keyNode.getValue().isArray()) {
                    Iterator<JsonNode> values = keyNode.getValue().getElements();
                    while (values.hasNext()) {
                        JsonNode valueNode = values.next();
                        resource.withLink(valueNode.get("_href").asText(), keyNode.getKey());
                    }
                } else {
                    resource.withLink(keyNode.getValue().get("_href").asText(), keyNode.getKey());
                }
            }
        }
    }

    private void readProperties(MutableResource resource, JsonNode rootNode) {

        Iterator<String> fieldNames = rootNode.getFieldNames();
        while (fieldNames.hasNext()) {
            String fieldName = fieldNames.next();
            if (!fieldName.startsWith("_")) {
                JsonNode field = rootNode.get(fieldName);
                resource.withProperty(fieldName, field.asText());
            }
        }

    }

    private void readResources(MutableResource resource, JsonNode rootNode) {
        if (rootNode.has("_embedded")) {
            Iterator<Map.Entry<String, JsonNode>> fields = rootNode.get("_embedded").getFields();
            while (fields.hasNext()) {
                Map.Entry<String, JsonNode> keyNode = fields.next();
                if (keyNode.getValue().isArray()) {
                    Iterator<JsonNode> values = keyNode.getValue().getElements();
                    while (values.hasNext()) {
                        JsonNode valueNode = values.next();
                        resource.withSubresource(keyNode.getKey(), readResource(valueNode));
                    }
                } else {
                    resource.withSubresource(keyNode.getKey(), readResource(keyNode.getValue()));
                }

            }
        }
    }
}
