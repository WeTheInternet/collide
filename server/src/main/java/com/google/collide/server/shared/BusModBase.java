package com.google.collide.server.shared;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

/**
 * Created by James X. Nelson (james @wetheinter.net) on 8/28/16.
 */
public abstract class BusModBase extends AbstractVerticle {

    protected static final Logger logger = LoggerFactory.getLogger(BusModBase.class);
    protected EventBus eb;
    protected JsonObject config;


    /**
     * Start the busmod
     */
    public void start() {
        eb = vertx.eventBus();
        config = config();
    }

    protected void sendOK(Message<JsonObject> message) {
        sendOK(message, null);
    }

    protected void sendStatus(String status, Message<JsonObject> message) {
        sendStatus(status, message, null);
    }

    protected void sendStatus(String status, Message<JsonObject> message, JsonObject json) {
        if (json == null) {
            json = new JsonObject();
        }
        json.put("status", status);
        message.reply(json);
    }

    protected void sendOK(Message<JsonObject> message, JsonObject json) {
        sendStatus("ok", message, json);
    }

    protected void sendError(Message<JsonObject> message, String error) {
        sendError(message, error, null);
    }

    protected void sendError(Message<JsonObject> message, String error, Exception e) {
        logger.error(error, e);
        JsonObject json = new JsonObject().put("status", "error").put("message", error);
        message.reply(json);
    }

    protected String getMandatoryString(String field, Message<JsonObject> message) {
        String val = message.body().getString(field);
        if (val == null) {
            sendError(message, field + " must be specified");
        }
        return val;
    }

    protected JsonObject getMandatoryObject(String field, Message<JsonObject> message) {
        JsonObject val = message.body().getJsonObject(field);
        if (val == null) {
            sendError(message, field + " must be specified");
        }
        return val;
    }

    protected boolean getOptionalBooleanConfig(String fieldName, boolean defaultValue) {
        Boolean b = config.getBoolean(fieldName);
        return b == null ? defaultValue : b.booleanValue();
    }

    protected String getOptionalStringConfig(String fieldName, String defaultValue) {
        String b = config.getString(fieldName);
        return b == null ? defaultValue : b;
    }

    protected int getOptionalIntConfig(String fieldName, int defaultValue) {
        Integer b = config.getInteger(fieldName);
        return b == null ? defaultValue : b.intValue();
    }

    protected long getOptionalLongConfig(String fieldName, long defaultValue) {
        Long l = config.getLong(fieldName);
        return l == null ? defaultValue : l.longValue();
    }

    protected JsonObject getOptionalObjectConfig(String fieldName, JsonObject defaultValue) {
        JsonObject o = config.getJsonObject(fieldName);
        return o == null ? defaultValue : o;
    }

    protected JsonArray getOptionalArrayConfig(String fieldName, JsonArray defaultValue) {
        JsonArray a = config.getJsonArray(fieldName);
        return a == null ? defaultValue : a;
    }

    protected boolean getMandatoryBooleanConfig(String fieldName) {
        Boolean b = config.getBoolean(fieldName);
        if (b == null) {
            throw new IllegalArgumentException(fieldName + " must be specified in config for busmod");
        }
        return b;
    }

    protected String getMandatoryStringConfig(String fieldName) {
        String s = config.getString(fieldName);
        if (s == null) {
            throw new IllegalArgumentException(fieldName + " must be specified in config for busmod");
        }
        return s;
    }

    protected int getMandatoryIntConfig(String fieldName) {
        Integer i = config.getInteger(fieldName);
        if (i == null) {
            throw new IllegalArgumentException(fieldName + " must be specified in config for busmod");
        }
        return i;
    }

    protected long getMandatoryLongConfig(String fieldName) {
        Long l = config.getLong(fieldName);
        if (l == null) {
            throw new IllegalArgumentException(fieldName + " must be specified in config for busmod");
        }
        return l;
    }

}
