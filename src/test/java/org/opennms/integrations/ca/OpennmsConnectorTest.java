/*******************************************************************************
 * This file is part of OpenNMS(R).
 *
 * Copyright (C) 2018 The OpenNMS Group, Inc.
 * OpenNMS(R) is Copyright (C) 1999-2018 The OpenNMS Group, Inc.
 *
 * OpenNMS(R) is a registered trademark of The OpenNMS Group, Inc.
 *
 * OpenNMS(R) is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published
 * by the Free Software Foundation, either version 3 of the License,
 * or (at your option) any later version.
 *
 * OpenNMS(R) is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with OpenNMS(R).  If not, see:
 *      http://www.gnu.org/licenses/
 *
 * For more information contact:
 *     OpenNMS(R) Licensing <license@opennms.org>
 *     http://www.opennms.org/
 *     http://www.opennms.com/
 *******************************************************************************/

package org.opennms.integrations.ca;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.Matchers.isIn;
import static org.hamcrest.Matchers.not;

import java.util.Arrays;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.Before;
import org.junit.Test;
import org.opennms.features.kafka.producer.model.OpennmsModelProtos;

import com.ca.ucf.api.InvalidParameterException;
import com.ca.ucf.api.UCFException;
import com.ca.usm.ucf.utils.USMSiloDataObjectType;

import commonj.sdo.DataObject;

public class OpennmsConnectorTest {

    private OpennmsConnector connector;

    @Before
    public void setUp() {
        // Create the connector before each test to ensure
        // that the USM classes are properly initialized
        connector = new OpennmsConnector();
    }

    @Test(expected = UCFException.class)
    public void failsToLoadWithEmptyConfig() throws UCFException {
        connector.initialize(Collections.emptyMap());
    }

    @Test
    public void canMapAlarmSeverity() throws InvalidParameterException {
        // Build a set containing the valid string values: Normal, Minor, Major, Critical, Down.
        final Set<String> validSeverities =  Arrays.stream(SOISeverity.values())
                .map(SOISeverity::getStringValue)
                .collect(Collectors.toSet());

        // Build an alarm with each severity and
        for (OpennmsModelProtos.Severity severity : OpennmsModelProtos.Severity.values()) {
            if (OpennmsModelProtos.Severity.UNRECOGNIZED.equals(severity)) {
                continue;
            }
            OpennmsModelProtos.Alarm alarm = OpennmsModelProtos.Alarm.newBuilder()
                    .setSeverity(severity)
                    .build();
            DataObject alarmEntity = OpennmsConnector.createAlertEntityForAlarm(alarm);
            // Verify that the mapped entity contains a valid severity
            Map<String,String> alarmEntityMap = USMSiloDataObjectType.convertToMap(alarmEntity);
            assertThat(alarmEntityMap.get(OpennmsConnector.ALARM_ENTITY_SEVERITY_KEY), isIn(validSeverities));
        }

    }

    @Test
    public void canMapEventParameters() throws InvalidParameterException {
        // Build an alarm with some event params
        OpennmsModelProtos.Alarm alarm = OpennmsModelProtos.Alarm.newBuilder()
                .setSeverity(OpennmsModelProtos.Severity.MINOR)
                .setLastEvent(OpennmsModelProtos.Event.newBuilder()
                        .addParameter(OpennmsModelProtos.EventParameter.newBuilder()) // no key, no value
                        .addParameter(OpennmsModelProtos.EventParameter.newBuilder().setName("key1")) // no value
                        .addParameter(OpennmsModelProtos.EventParameter.newBuilder() // both a key and value
                                .setName("key2")
                                .setValue("value2")))
                .build();

        DataObject alarmEntity = OpennmsConnector.createAlertEntityForAlarm(alarm);
        // Verify that the mapped entity contains the expected parameters
        Map<String,String> alarmEntityMap = USMSiloDataObjectType.convertToMap(alarmEntity);
        assertThat(alarmEntityMap, not(hasKey("key1")));
        assertThat(alarmEntityMap.get(OpennmsConnector.ALARM_ENTITY_EVENT_PARM_PREFIX_KEY + "key2"),
                equalTo("value2"));
    }
}
