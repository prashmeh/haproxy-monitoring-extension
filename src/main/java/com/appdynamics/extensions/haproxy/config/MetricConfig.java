/*
 *
 *   Copyright 2018. AppDynamics LLC and its affiliates.
 *   All Rights Reserved.
 *   This is unpublished proprietary source code of AppDynamics LLC and its affiliates.
 *   The copyright notice above does not evidence any actual or intended publication of such source code.
 *
 */

package com.appdynamics.extensions.haproxy.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;

@XmlAccessorType(XmlAccessType.FIELD)
public class MetricConfig {
    public static final Logger logger = LoggerFactory.getLogger(MetricConfig.class);

    @XmlAttribute
    private String attr;
    @XmlAttribute
    private String alias;
    @XmlAttribute
    private int column;
    @XmlAttribute
    private String delta;
    @XmlAttribute
    private String aggregationType;
    @XmlAttribute
    private String timeRollUpType;
    @XmlAttribute
    private String clusterRollUpType;
    @XmlAttribute
    private String character;
    @XmlElement(name="isBoolean")
    private String isBoolean= "false";

    public String getAttr() { return attr; }

    public void setAttr(String attr) {
        this.attr = attr;
    }

    public String getAlias() {
        return alias;
    }

    public void setAlias(String alias) {
        this.alias = alias;
    }

    public String isBoolean() {
        return isBoolean;
    }

    public void setBoolean(String isBoolean) {
        this.isBoolean = isBoolean;
    }

    public String getCharacter() {
        return character;
    }

    public void setCharacter(String character) {
        this.character = character;
    }

    public String getDelta() {
        return delta;
    }

    public void setDelta(String delta) {
        this.delta = delta;
    }

    public String getAggregationType() {
        return aggregationType;
    }

    public void setAggregationType(String aggregationType) {
        this.aggregationType = aggregationType;
    }

    public String getTimeRollUpType() {
        return timeRollUpType;
    }

    public void setTimeRollUpType(String timeRollUpType) {
        this.timeRollUpType = timeRollUpType;
    }

    public String getClusterRollUpType() {
        return clusterRollUpType;
    }

    public void setClusterRollUpType(String clusterRollUpType) {
        this.clusterRollUpType = clusterRollUpType;
    }

    public int getColumn(){return column;}

}